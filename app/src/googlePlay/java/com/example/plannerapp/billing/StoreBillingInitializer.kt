package com.example.plannerapp.billing

import android.content.Context
import com.example.plannerapp.BuildConfig
import com.revenuecat.purchases.LogLevel
import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.PurchasesConfiguration

/**
 * Google Play flavor initialization for RevenueCat.
 * Configured using standard PurchasesConfiguration.Builder.
 */
object StoreBillingInitializer : BillingInitializer {
    override fun initialize(context: Context) {
        if (BuildConfig.DEBUG) {
            Purchases.logLevel = LogLevel.DEBUG
        }
        Purchases.configure(
            PurchasesConfiguration.Builder(context, BuildConfig.RC_API_KEY)
                .build()
        )
    }
}
