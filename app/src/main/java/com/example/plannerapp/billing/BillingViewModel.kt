package com.example.plannerapp.billing

import android.app.Activity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.plannerapp.BuildConfig
import com.revenuecat.purchases.CustomerInfo
import com.revenuecat.purchases.Offering
import com.revenuecat.purchases.Offerings
import com.revenuecat.purchases.Package
import com.revenuecat.purchases.PurchaseParams
import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.PurchasesError
import com.revenuecat.purchases.PurchasesErrorCode
import com.revenuecat.purchases.interfaces.PurchaseCallback
import com.revenuecat.purchases.interfaces.ReceiveCustomerInfoCallback
import com.revenuecat.purchases.interfaces.ReceiveOfferingsCallback
import com.revenuecat.purchases.interfaces.UpdatedCustomerInfoListener
import com.revenuecat.purchases.models.StoreTransaction
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * UI State for multi-store in-app purchases and subscriptions.
 */
data class BillingUiState(
    val isLoading: Boolean = true,
    val isPurchasing: Boolean = false,
    val isRestoring: Boolean = false,
    val isPremium: Boolean = false,
    val currentOffering: Offering? = null,
    val selectedPackage: Package? = null,
    val errorMessage: String? = null,
    val userFeedbackMessage: String? = null,
    val storeName: String = BuildConfig.STORE_NAME
)

/**
 * Unified, store-agnostic Billing ViewModel for RevenueCat.
 *
 * Handles:
 * - Fetching current offerings via `getOfferings`
 * - Checking active entitlement status (`BillingConfig.ENTITLEMENT_PREMIUM`)
 * - Executing purchases via `PurchaseParams`
 * - Restoring purchases via `restorePurchases`
 * - Listening to real-time customer info updates via `updatedCustomerInfoListener`
 */
class BillingViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(BillingUiState())
    val uiState: StateFlow<BillingUiState> = _uiState.asStateFlow()

    init {
        setupCustomerInfoListener()
        checkEntitlementStatus()
        fetchOfferings()
    }

    /**
     * Binds RevenueCat's real-time customer info listener to immediately react
     * when subscriptions are purchased, cancelled, or renewed outside the app.
     */
    private fun setupCustomerInfoListener() {
        if (!Purchases.isConfigured) return

        Purchases.sharedInstance.updatedCustomerInfoListener = UpdatedCustomerInfoListener { customerInfo ->
            updatePremiumStatus(customerInfo)
        }
    }

    /**
     * Checks if the active user has the premium entitlement granted.
     */
    fun checkEntitlementStatus() {
        if (!Purchases.isConfigured) {
            _uiState.update { it.copy(isLoading = false) }
            return
        }

        Purchases.sharedInstance.getCustomerInfo(object : ReceiveCustomerInfoCallback {
            override fun onReceived(customerInfo: CustomerInfo) {
                updatePremiumStatus(customerInfo)
            }

            override fun onError(error: PurchasesError) {
                _uiState.update {
                    it.copy(errorMessage = error.message)
                }
            }
        })
    }

    /**
     * Fetches current offerings from RevenueCat dashboard.
     */
    fun fetchOfferings() {
        if (!Purchases.isConfigured) {
            _uiState.update {
                it.copy(
                    isLoading = false,
                    errorMessage = "Billing SDK not initialized. Verify your RC_API_KEY in build configuration."
                )
            }
            return
        }

        _uiState.update { it.copy(isLoading = true, errorMessage = null) }

        Purchases.sharedInstance.getOfferings(object : ReceiveOfferingsCallback {
            override fun onReceived(offerings: Offerings) {
                val offering = if (BillingConfig.DEFAULT_OFFERING_ID != null) {
                    offerings.getOffering(BillingConfig.DEFAULT_OFFERING_ID) ?: offerings.current
                } else {
                    offerings.current
                }

                val defaultPackage = offering?.availablePackages?.firstOrNull()

                _uiState.update {
                    it.copy(
                        isLoading = false,
                        currentOffering = offering,
                        selectedPackage = defaultPackage,
                        errorMessage = if (offering == null) "No active offerings found in dashboard." else null
                    )
                }
            }

            override fun onError(error: PurchasesError) {
                val errorMsg = when {
                    error.code == PurchasesErrorCode.NetworkError ->
                        "Network drop: Unable to load plans. Please check your connection and tap Retry."
                    error.code == PurchasesErrorCode.ConfigurationError || error.code == PurchasesErrorCode.InvalidCredentialsError ->
                        "Configuration issue: Unable to load plans. Verify RevenueCat dashboard keys in build settings."
                    else -> error.message
                }
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = errorMsg
                    )
                }
            }
        })
    }

    /**
     * Allows the user to select a specific package tier from the offering.
     */
    fun selectPackage(packageToSelect: Package) {
        _uiState.update { it.copy(selectedPackage = packageToSelect) }
    }

    /**
     * Executes purchase of the selected or supplied package using PurchaseParams.
     * Full error handling for user cancellations, network drops, or configuration issues.
     */
    fun purchase(activity: Activity, packageToBuy: Package? = _uiState.value.selectedPackage) {
        if (packageToBuy == null) {
            _uiState.update { it.copy(errorMessage = "Please select a plan to continue.") }
            return
        }

        if (!Purchases.isConfigured) {
            _uiState.update { it.copy(errorMessage = "Billing service is not ready. Please try again later.") }
            return
        }

        _uiState.update { it.copy(isPurchasing = true, errorMessage = null, userFeedbackMessage = null) }

        val purchaseParams = PurchaseParams.Builder(activity, packageToBuy).build()

        Purchases.sharedInstance.purchase(
            purchaseParams,
            object : PurchaseCallback {
                override fun onCompleted(storeTransaction: StoreTransaction, customerInfo: CustomerInfo) {
                    _uiState.update { it.copy(isPurchasing = false) }
                    updatePremiumStatus(customerInfo)
                    _uiState.update {
                        it.copy(userFeedbackMessage = "Thank you! Premium access is now active.")
                    }
                }

                override fun onError(error: PurchasesError, userCancelled: Boolean) {
                    when {
                        // 1. User Cancellation
                        userCancelled || error.code == PurchasesErrorCode.PurchaseCancelledError -> {
                            _uiState.update {
                                it.copy(
                                    isPurchasing = false,
                                    errorMessage = null,
                                    userFeedbackMessage = "Purchase was cancelled."
                                )
                            }
                        }
                        // 2. Network Drops / Offline
                        error.code == PurchasesErrorCode.NetworkError -> {
                            _uiState.update {
                                it.copy(
                                    isPurchasing = false,
                                    errorMessage = "Network drop detected. Please check your internet connection and try again.",
                                    userFeedbackMessage = null
                                )
                            }
                        }
                        // 3. Configuration Issues
                        error.code == PurchasesErrorCode.ConfigurationError || error.code == PurchasesErrorCode.InvalidCredentialsError -> {
                            _uiState.update {
                                it.copy(
                                    isPurchasing = false,
                                    errorMessage = "Store configuration error. Please verify your RevenueCat dashboard credentials.",
                                    userFeedbackMessage = null
                                )
                            }
                        }
                        // 4. Product Already Subscribed
                        error.code == PurchasesErrorCode.ProductAlreadyPurchasedError -> {
                            _uiState.update {
                                it.copy(
                                    isPurchasing = false,
                                    errorMessage = null,
                                    userFeedbackMessage = "You already own this subscription. Try restoring purchases."
                                )
                            }
                        }
                        // 5. Payment Pending
                        error.code == PurchasesErrorCode.PaymentPendingError -> {
                            _uiState.update {
                                it.copy(
                                    isPurchasing = false,
                                    errorMessage = null,
                                    userFeedbackMessage = "Payment is pending store approval. Your subscription will activate once confirmed."
                                )
                            }
                        }
                        // 6. Generic / Other Store Errors
                        else -> {
                            _uiState.update {
                                it.copy(
                                    isPurchasing = false,
                                    errorMessage = error.message.ifBlank { "An unexpected error occurred during purchase." },
                                    userFeedbackMessage = null
                                )
                            }
                        }
                    }
                }
            }
        )
    }

    /**
     * Restores previous purchases across devices according to store guidelines.
     */
    fun restorePurchases() {
        if (!Purchases.isConfigured) {
            _uiState.update { it.copy(errorMessage = "Billing service is not ready.") }
            return
        }

        _uiState.update { it.copy(isRestoring = true, errorMessage = null, userFeedbackMessage = null) }

        Purchases.sharedInstance.restorePurchases(object : ReceiveCustomerInfoCallback {
            override fun onReceived(customerInfo: CustomerInfo) {
                val hasPremium = customerInfo.entitlements[BillingConfig.ENTITLEMENT_PREMIUM]?.isActive == true
                _uiState.update {
                    it.copy(
                        isRestoring = false,
                        isPremium = hasPremium,
                        userFeedbackMessage = if (hasPremium) {
                            "Purchases successfully restored!"
                        } else {
                            "No active subscriptions found to restore."
                        }
                    )
                }
            }

            override fun onError(error: PurchasesError) {
                _uiState.update {
                    it.copy(
                        isRestoring = false,
                        errorMessage = error.message
                    )
                }
            }
        })
    }

    private fun updatePremiumStatus(customerInfo: CustomerInfo) {
        val hasPremium = customerInfo.entitlements[BillingConfig.ENTITLEMENT_PREMIUM]?.isActive == true
        _uiState.update { it.copy(isPremium = hasPremium) }
    }

    fun clearFeedback() {
        _uiState.update { it.copy(errorMessage = null, userFeedbackMessage = null) }
    }
}

class BillingViewModelFactory : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(BillingViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return BillingViewModel() as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
