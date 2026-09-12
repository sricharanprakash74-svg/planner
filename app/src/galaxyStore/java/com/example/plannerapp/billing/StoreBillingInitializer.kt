package com.example.plannerapp.billing

import android.content.Context
import com.example.plannerapp.BuildConfig
import com.revenuecat.purchases.LogLevel
import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.galaxy.GalaxyBillingMode
import com.revenuecat.purchases.galaxy.GalaxyConfiguration

/**
 * Samsung Galaxy Store flavor initialization for RevenueCat.
 * Configured using GalaxyConfiguration.Builder with GalaxyBillingMode.TEST
 * for debug builds and GalaxyBillingMode.PRODUCTION for release builds.
 */
object StoreBillingInitializer : BillingInitializer {
    override fun initialize(context: Context) {
        if (BuildConfig.DEBUG) {
            Purchases.logLevel = LogLevel.DEBUG
        }
        val billingMode = if (BuildConfig.DEBUG) {
            GalaxyBillingMode.TEST
        } else {
            GalaxyBillingMode.PRODUCTION
        }
        Purchases.configure(
            GalaxyConfiguration.Builder(context, BuildConfig.RC_API_KEY, billingMode)
                .build()
        )
    }
}
