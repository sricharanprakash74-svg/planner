package com.example.plannerapp

import android.app.Application
import com.example.plannerapp.billing.StoreBillingInitializer

/**
 * Custom Application class for PlannerApp.
 * Initializes store-specific billing configuration early in the application lifecycle.
 */
class PlannerApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        StoreBillingInitializer.initialize(this)
    }
}
