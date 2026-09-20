package com.example.plannerapp

import android.app.Application
import com.example.plannerapp.billing.AppBillingRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Custom Application class for PlannerApp.
 */
class PlannerApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // Non-blocking initialization of billing on IO dispatcher to preserve 0ms startup time
        CoroutineScope(Dispatchers.IO).launch {
            AppBillingRepository.getInstance(this@PlannerApplication)
        }
    }
}
