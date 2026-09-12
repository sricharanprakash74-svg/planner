package com.example.plannerapp.billing

/**
 * Centralized configuration for RevenueCat in-app purchases and subscriptions.
 *
 * All entitlement identifiers, package keys, and offering IDs are managed here.
 * TODO: Fill with dashboard credentials before production submission.
 */
object BillingConfig {

    /**
     * Primary entitlement identifier configured in the RevenueCat dashboard.
     * Users with this entitlement active are granted Planner Pro / Premium access.
     *
     * TODO: Fill with dashboard credentials
     */
    const val ENTITLEMENT_PREMIUM = "premium_access"

    /**
     * Optional custom offering identifier.
     * If null, RevenueCat automatically serves the 'current' offering configured in your dashboard.
     *
     * TODO: Fill with dashboard credentials
     */
    val DEFAULT_OFFERING_ID: String? = null

    /**
     * Note on API Keys:
     * - Google Play API Key: Defined in build.gradle.kts (RC_API_KEY = "goog_placeholder_key")
     * - Samsung Galaxy Store API Key: Defined in build.gradle.kts (RC_API_KEY = "galx_placeholder_key")
     *
     * TODO: Replace placeholder keys in app/build.gradle.kts with live RevenueCat public API keys.
     */
}
