package com.example.plannerapp.billing

/**
 * Centralized configuration for the Dummy Paywall.
 */
object BillingConfig {

    /**
     * Primary entitlement identifiers for Pro access.
     */
    const val ENTITLEMENT_PRO = "pro_access"
    const val ENTITLEMENT_PREMIUM = "premium"

    /**
     * Package identifiers for paywall subscription tiers.
     */
    const val PACKAGE_MONTHLY = "pro_monthly"
    const val PACKAGE_ANNUAL = "pro_annual"

    const val PRICE_MONTHLY = "\$9.99 / month"
    const val PRICE_ANNUAL = "$39.99 / year"
}
