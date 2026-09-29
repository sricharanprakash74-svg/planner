package com.example.plannerapp.billing

import android.app.Activity
import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.example.plannerapp.BuildConfig
import com.revenuecat.purchases.CustomerInfo
import com.revenuecat.purchases.LogLevel
import com.revenuecat.purchases.Offerings
import com.revenuecat.purchases.Package
import com.revenuecat.purchases.PurchaseParams
import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.PurchasesConfiguration
import com.revenuecat.purchases.PurchasesError
import com.revenuecat.purchases.PurchasesErrorCode
import com.revenuecat.purchases.interfaces.PurchaseCallback
import com.revenuecat.purchases.interfaces.ReceiveCustomerInfoCallback
import com.revenuecat.purchases.interfaces.ReceiveOfferingsCallback
import com.revenuecat.purchases.interfaces.UpdatedCustomerInfoListener
import com.revenuecat.purchases.models.StoreTransaction
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Clean Billing Repository interface supporting RevenueCat with instant, zero-latency local fallback.
 */
interface BillingRepository {
    /**
     * Observable stream indicating whether Pro access is unlocked.
     */
    val isProActive: StateFlow<Boolean>

    /**
     * Observable stream of loaded RevenueCat packages, or empty if using local packages.
     */
    val availablePackages: StateFlow<List<Package>>

    /**
     * Executes purchase for the specified package identifier using RevenueCat or mock fallback.
     */
    fun purchase(activity: Activity? = null, packageId: String, onComplete: (Boolean) -> Unit)

    /**
     * Restores previous purchases across devices and sessions.
     */
    fun restorePurchases(onComplete: (Boolean) -> Unit)

    /**
     * Resets purchase state back to free tier.
     */
    fun resetMockStatus()
}

/**
 * Hybrid production-ready BillingRepository:
 * 1. Zero performance degradation: all RevenueCat configuration and sync happens asynchronously in background IO.
 * 2. Instant cache reads: isProActive uses StateFlow and local SharedPreferences (< 1ms read).
 * 3. Graceful fallback: If test API key is missing or offline or in demo mode, mock billing acts transparently without crashes.
 */
class AppBillingRepository(
    private val context: Context,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.IO)
) : BillingRepository {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("billing_dummy_prefs", Context.MODE_PRIVATE)

    private val _isProActive = MutableStateFlow(prefs.getBoolean("is_pro_unlocked", false))
    override val isProActive: StateFlow<Boolean> = _isProActive.asStateFlow()

    private val _availablePackages = MutableStateFlow<List<Package>>(emptyList())
    override val availablePackages: StateFlow<List<Package>> = _availablePackages.asStateFlow()

    init {
        initRevenueCatAsync()
    }

    private fun initRevenueCatAsync() {
        scope.launch {
            try {
                val apiKey = BuildConfig.REVENUECAT_KEY.ifBlank { BuildConfig.REVENUECAT_API_KEY }
                if (apiKey.isNotBlank() && apiKey != "goog_placeholder_key" && !Purchases.isConfigured) {
                    Purchases.logLevel = LogLevel.DEBUG
                    Purchases.configure(
                        PurchasesConfiguration.Builder(context, apiKey).build()
                    )
                }

                if (Purchases.isConfigured) {
                    Purchases.sharedInstance.updatedCustomerInfoListener = UpdatedCustomerInfoListener { customerInfo ->
                        handleCustomerInfo(customerInfo)
                    }

                    // Background initial sync
                    Purchases.sharedInstance.getCustomerInfo(object : ReceiveCustomerInfoCallback {
                        override fun onReceived(customerInfo: CustomerInfo) {
                            handleCustomerInfo(customerInfo)
                        }

                        override fun onError(error: PurchasesError) {
                            Log.d("BillingRepository", "Offline or credentials notice: ${error.message}")
                        }
                    })

                    // Background offerings fetch
                    Purchases.sharedInstance.getOfferings(object : ReceiveOfferingsCallback {
                        override fun onReceived(offerings: Offerings) {
                            val current = offerings.current
                            if (current != null && current.availablePackages.isNotEmpty()) {
                                _availablePackages.value = current.availablePackages
                            }
                        }

                        override fun onError(error: PurchasesError) {
                            Log.d("BillingRepository", "Using local catalog fallback: ${error.message}")
                        }
                    })
                }
            } catch (e: Throwable) {
                Log.w("BillingRepository", "RevenueCat non-blocking initialization: ${e.message}")
            }
        }
    }

    private fun handleCustomerInfo(customerInfo: CustomerInfo) {
        val hasPro = customerInfo.entitlements[BillingConfig.ENTITLEMENT_PRO]?.isActive == true ||
            customerInfo.entitlements[BillingConfig.ENTITLEMENT_PREMIUM]?.isActive == true ||
            customerInfo.entitlements.active.isNotEmpty()

        setProUnlocked(hasPro)
    }

    private fun setProUnlocked(unlocked: Boolean) {
        prefs.edit().putBoolean("is_pro_unlocked", unlocked).apply()
        _isProActive.value = unlocked
    }

    override fun purchase(activity: Activity?, packageId: String, onComplete: (Boolean) -> Unit) {
        scope.launch {
            val rcPackage = _availablePackages.value.find { it.identifier == packageId }
                ?: _availablePackages.value.firstOrNull()

            if (activity != null && Purchases.isConfigured && rcPackage != null) {
                withContext(Dispatchers.Main) {
                    val params = PurchaseParams.Builder(activity, rcPackage).build()
                    Purchases.sharedInstance.purchase(
                        params,
                        object : PurchaseCallback {
                            override fun onCompleted(storeTransaction: StoreTransaction, customerInfo: CustomerInfo) {
                                handleCustomerInfo(customerInfo)
                                val hasPro = _isProActive.value
                                if (!hasPro) {
                                    Log.w("BillingRepository", "Purchase completed but Pro entitlement is not active in customer info.")
                                }
                                onComplete(hasPro)
                            }

                            override fun onError(error: PurchasesError, userCancelled: Boolean) {
                                if (userCancelled || error.code == PurchasesErrorCode.PurchaseCancelledError) {
                                    Log.d("BillingRepository", "Purchase cancelled by user.")
                                } else {
                                    Log.w("BillingRepository", "Purchase error (${error.code}): ${error.message}")
                                }
                                onComplete(false)
                            }
                        }
                    )
                }
            } else if (BuildConfig.USE_MOCK_BILLING) {
                // High-speed simulated checkout (500ms) with zero jank (mock mode only)
                delay(500)
                setProUnlocked(true)
                onComplete(true)
            } else {
                Log.e("BillingRepository", "Cannot purchase: Billing is not configured and mock billing is disabled.")
                onComplete(false)
            }
        }
    }

    override fun restorePurchases(onComplete: (Boolean) -> Unit) {
        scope.launch {
            if (Purchases.isConfigured && _availablePackages.value.isNotEmpty()) {
                withContext(Dispatchers.Main) {
                    Purchases.sharedInstance.restorePurchases(object : ReceiveCustomerInfoCallback {
                        override fun onReceived(customerInfo: CustomerInfo) {
                            val hasPro = customerInfo.entitlements[BillingConfig.ENTITLEMENT_PRO]?.isActive == true ||
                                customerInfo.entitlements[BillingConfig.ENTITLEMENT_PREMIUM]?.isActive == true ||
                                customerInfo.entitlements.active.isNotEmpty()
                            setProUnlocked(hasPro)
                            onComplete(hasPro)
                        }

                        override fun onError(error: PurchasesError) {
                            val cached = prefs.getBoolean("is_pro_unlocked", false)
                            _isProActive.value = cached
                            onComplete(cached)
                        }
                    })
                }
            } else if (BuildConfig.USE_MOCK_BILLING) {
                delay(400)
                val cached = prefs.getBoolean("is_pro_unlocked", false)
                _isProActive.value = cached
                onComplete(cached)
            } else {
                Log.e("BillingRepository", "Cannot restore purchases: Billing is not configured and mock billing is disabled.")
                onComplete(false)
            }
        }
    }

    override fun resetMockStatus() {
        prefs.edit().remove("is_pro_unlocked").apply()
        _isProActive.value = false
    }

    companion object {
        @Volatile
        private var INSTANCE: BillingRepository? = null

        fun getInstance(context: Context): BillingRepository {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: AppBillingRepository(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
}
