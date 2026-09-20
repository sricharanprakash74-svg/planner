package com.example.plannerapp.credits

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.stripe.android.PaymentConfiguration
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.PaymentSheetResult
import com.stripe.android.paymentsheet.rememberPaymentSheet

data class CreditPack(
    val id: String,
    val name: String,
    val credits: Int,
    val bonusCredits: Int = 0,
    val priceUsd: String,
    val tag: String? = null
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreditHubSheet(
    viewModel: CreditViewModel,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    remember {
        PaymentConfiguration.init(context, "YOUR_STRIPE_PUBLISHABLE_KEY")
        true
    }

    val snackbarHostState = remember { SnackbarHostState() }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val clientSecret by viewModel.stripeClientSecret.collectAsState()
    val uiState by viewModel.uiState.collectAsState()

    val paymentSheet = rememberPaymentSheet { paymentResult ->
        when (paymentResult) {
            is PaymentSheetResult.Completed -> viewModel.onStripePaymentSuccess()
            is PaymentSheetResult.Canceled -> viewModel.onStripePaymentCanceledOrFailed("Payment canceled")
            is PaymentSheetResult.Failed -> viewModel.onStripePaymentCanceledOrFailed("Payment failed: ${paymentResult.error.message}")
        }
    }

    LaunchedEffect(clientSecret) {
        if (clientSecret != null) {
            paymentSheet.presentWithPaymentIntent(
                clientSecret!!,
                PaymentSheet.Configuration(merchantDisplayName = "PlannerApp Store")
            )
        }
    }

    LaunchedEffect(uiState.message) {
        uiState.message?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessage()
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        modifier = modifier
    ) {
        Scaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) },
            containerColor = MaterialTheme.colorScheme.surface
        ) { innerPadding ->
            PlannerStoreContent(
                viewModel = viewModel,
                snackbarHostState = snackbarHostState,
                modifier = Modifier.padding(innerPadding)
            )
        }
    }
}
