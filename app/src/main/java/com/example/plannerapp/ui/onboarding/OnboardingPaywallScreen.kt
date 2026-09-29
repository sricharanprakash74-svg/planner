package com.example.plannerapp.ui.onboarding

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.plannerapp.billing.BillingViewModel
import com.example.plannerapp.billing.BillingViewModelFactory
import com.example.plannerapp.ui.billing.PaywallScreen
import androidx.compose.ui.platform.LocalContext

/**
 * Thin wrapper that presents the full RevenueCat-backed PaywallScreen
 * immediately after onboarding completion (step 2 → paywall → Home).
 *
 * Reuses PaywallScreen so the onboarding flow and the in-app paywall
 * share an identical UI and billing path — no duplicated code.
 */
@Composable
fun OnboardingPaywallScreen(onDismiss: () -> Unit) {
    val context = LocalContext.current
    val billingViewModel: BillingViewModel = viewModel(
        key = "revenuecat_billing_vm_onboarding",
        factory = BillingViewModelFactory(context.applicationContext)
    )
    PaywallScreen(
        viewModel = billingViewModel,
        onDismiss = onDismiss
    )
}
