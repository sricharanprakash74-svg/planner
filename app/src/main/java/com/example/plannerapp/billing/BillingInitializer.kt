package com.example.plannerapp.billing

import android.content.Context

/**
 * Contract for store-specific billing initialization.
 * Concrete implementations are provided via flavor-specific source sets
 * (src/googlePlay and src/galaxyStore) to keep billing dependencies completely decoupled.
 */
interface BillingInitializer {
    fun initialize(context: Context)
}
