package com.example.plannerapp.ui.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.plannerapp.credits.CreditViewModel
import com.example.plannerapp.data.CreatorDashboardResponse
import com.example.plannerapp.data.PayoutHistoryItem
import com.example.plannerapp.data.PlanSaleItem
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreatorMonetizationScreen(
    creditViewModel: CreditViewModel,
    onBack: () -> Unit,
    onBecomeCreatorClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val activeUser by creditViewModel.activeUserFlow.collectAsState()
    val isCreator = activeUser?.isCreator == true

    val dashboard by creditViewModel.creatorDashboard.collectAsState()
    val isBusy by creditViewModel.uiState.collectAsState()

    var showPayoutDialog by remember { mutableStateOf(false) }
    var payoutCreditsInput by remember { mutableStateOf("500") }

    var selectedMethod by remember { mutableStateOf("PAYPAL") }
    var accountInput by remember { mutableStateOf("") }
    var hasInitializedSettings by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        creditViewModel.loadCreatorDashboard()
    }

    LaunchedEffect(dashboard) {
        if (!hasInitializedSettings && dashboard != null) {
            selectedMethod = dashboard?.payoutMethod ?: "PAYPAL"
            accountInput = dashboard?.payoutAccount ?: ""
            hasInitializedSettings = true
        }
    }

    val primaryColor = Color(0xFF1E88E5)
    val accentGreen = Color(0xFF2E7D32)

    val redeemable = dashboard?.redeemableCredits ?: 0
    val estimatedUsd = dashboard?.estimatedUsd ?: 0.0
    val minThreshold = dashboard?.minCashOutCredits ?: 500

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Creator Monetization & Payouts", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        modifier = modifier
    ) { innerPadding ->
        if (!isCreator) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Surface(
                    shape = CircleShape,
                    color = primaryColor.copy(alpha = 0.1f),
                    modifier = Modifier.size(80.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Outlined.Lock,
                            contentDescription = "Locked",
                            tint = primaryColor,
                            modifier = Modifier.size(40.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Text(
                    text = "Creator Privileges Required",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Monetization and cash-outs are available exclusively to verified creators. Publish your custom plans, earn 70% of credit revenue, and cash out real money directly to your account.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )

                Spacer(modifier = Modifier.height(24.dp))

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Outlined.MonetizationOn, contentDescription = null, tint = accentGreen, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(12.dp))
                            Text("Set custom credit prices for your plans", style = MaterialTheme.typography.bodyMedium)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Outlined.Paid, contentDescription = null, tint = primaryColor, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(12.dp))
                            Text("70% earnings split on all plan sales", style = MaterialTheme.typography.bodyMedium)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Outlined.AccountBalanceWallet, contentDescription = null, tint = primaryColor, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(12.dp))
                            Text("Cash out earnings via PayPal or Bank Transfer", style = MaterialTheme.typography.bodyMedium)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Outlined.Verified, contentDescription = null, tint = primaryColor, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(12.dp))
                            Text("Verified Creator badge across profile and community", style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                Button(
                    onClick = onBecomeCreatorClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Outlined.Star, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Become a Creator", fontWeight = FontWeight.SemiBold)
                }
            }
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // ── Top Earnings Card ───────────────────────────────────────
            item {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = primaryColor.copy(alpha = 0.08f),
                    border = BorderStroke(1.dp, primaryColor.copy(alpha = 0.2f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "AVAILABLE FOR CASH-OUT",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = primaryColor
                            )
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = accentGreen.copy(alpha = 0.12f)
                            ) {
                                Text(
                                    text = "70% Revenue Split",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = accentGreen,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            verticalAlignment = Alignment.Bottom,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "$redeemable",
                                        style = MaterialTheme.typography.headlineMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Credits",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Text(
                                    text = String.format(java.util.Locale.US, "Estimated Value: $%.2f USD", estimatedUsd),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = accentGreen
                                )
                            }

                            Button(
                                onClick = { showPayoutDialog = true },
                                enabled = redeemable >= minThreshold && !isBusy.isBusy,
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Payments,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Cash Out", fontWeight = FontWeight.Bold)
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                        HorizontalDivider(color = primaryColor.copy(alpha = 0.15f))
                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(
                                    text = "Lifetime Earned",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "${dashboard?.earnedCredits ?: 0} Credits",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Column {
                                Text(
                                    text = "Total Paid Out",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = String.format(java.util.Locale.US, "$%.2f USD", dashboard?.totalPaidUsd ?: 0.0),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Column {
                                Text(
                                    text = "Plans Sold",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "${dashboard?.salesCount ?: 0} Unlocks",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }

            // ── Payout Settings Card ────────────────────────────────────
            item {
                OutlinedCard(
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Outlined.AccountBalance,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Payout Destination",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Configure how and where you receive your creator earnings.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(14.dp))

                        // Method selection
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            listOf("PAYPAL" to "PayPal", "BANK" to "Bank Transfer", "UPI" to "UPI").forEach { (key, label) ->
                                FilterChip(
                                    selected = selectedMethod == key,
                                    onClick = { selectedMethod = key },
                                    label = { Text(label, style = MaterialTheme.typography.labelMedium) }
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        val placeholderText = when (selectedMethod) {
                            "PAYPAL" -> "Enter your PayPal email address"
                            "BANK" -> "Enter IBAN or Account Number with Routing Code"
                            else -> "Enter UPI ID (e.g., username@bank)"
                        }

                        OutlinedTextField(
                            value = accountInput,
                            onValueChange = { accountInput = it },
                            label = { Text(if (selectedMethod == "PAYPAL") "PayPal Email" else "Account Details") },
                            placeholder = { Text(placeholderText) },
                            singleLine = selectedMethod == "PAYPAL",
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp)
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        Button(
                            onClick = {
                                creditViewModel.updatePayoutSettings(selectedMethod, accountInput) { success, msg ->
                                    coroutineScope.launch { snackbarHostState.showSnackbar(msg) }
                                }
                            },
                            enabled = accountInput.isNotBlank() && !isBusy.isBusy,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.align(Alignment.End)
                        ) {
                            Text("Save Payout Method", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // ── Recent Plan Sales ───────────────────────────────────────
            val recentSales = dashboard?.recentSales ?: emptyList()
            item {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(top = 8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Storefront,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Recent Plan Sales",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            if (recentSales.isEmpty()) {
                item {
                    Text(
                        text = "No paid plan unlocks recorded yet. Publish plans to the Explore community to start earning.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }
            } else {
                items(recentSales, key = { "sale_${it.id}" }) { sale ->
                    PlanSaleRow(sale = sale)
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                }
            }

            // ── Payout Requests History ─────────────────────────────────
            val recentPayouts = dashboard?.recentPayouts ?: emptyList()
            item {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(top = 16.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.History,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Payout History",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            if (recentPayouts.isEmpty()) {
                item {
                    Text(
                        text = "No cash-out requests submitted yet.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }
            } else {
                items(recentPayouts, key = { "payout_${it.id}" }) { payout ->
                    PayoutRow(payout = payout)
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                }
            }
        }
    }

    // ── Cash Out Confirmation Dialog ────────────────────────────────────
    if (showPayoutDialog) {
        val creditsInt = payoutCreditsInput.toIntOrNull() ?: 0
        val payoutUsd = String.format(java.util.Locale.US, "%.2f", creditsInt * 0.007)

        AlertDialog(
            onDismissRequest = { showPayoutDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.Outlined.Payments,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(32.dp)
                )
            },
            title = {
                Text(
                    text = "Request Cash-Out",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "Convert your redeemable creator credits into real currency payout.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    OutlinedTextField(
                        value = payoutCreditsInput,
                        onValueChange = { input ->
                            if (input.all { it.isDigit() }) payoutCreditsInput = input
                        },
                        label = { Text("Credits to Redeem") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        supportingText = {
                            Text("Minimum: 500 Credits ($3.50 USD). Available: $redeemable")
                        }
                    )

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        listOf(500, 1000, 2000).forEach { amount ->
                            if (amount <= redeemable) {
                                FilterChip(
                                    selected = payoutCreditsInput == amount.toString(),
                                    onClick = { payoutCreditsInput = amount.toString() },
                                    label = { Text("$amount pts") }
                                )
                            }
                        }
                        if (redeemable >= 500) {
                            FilterChip(
                                selected = payoutCreditsInput == redeemable.toString(),
                                onClick = { payoutCreditsInput = redeemable.toString() },
                                label = { Text("All ($redeemable)") }
                            )
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Payout Amount:", style = MaterialTheme.typography.bodyMedium)
                                Text("$$payoutUsd USD", fontWeight = FontWeight.Bold, color = accentGreen)
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Destination:", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(
                                    text = if (accountInput.isNotBlank()) "$selectedMethod: $accountInput" else "None configured",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showPayoutDialog = false
                        creditViewModel.requestCashOut(creditsInt) { success, msg ->
                            coroutineScope.launch { snackbarHostState.showSnackbar(msg) }
                        }
                    },
                    enabled = creditsInt in minThreshold..redeemable && accountInput.isNotBlank()
                ) {
                    Text("Confirm Cash-Out")
                }
            },
            dismissButton = {
                TextButton(onClick = { showPayoutDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun PlanSaleRow(sale: PlanSaleItem) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = sale.planTitle,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = "Unlocked by ${sale.buyerName} · ${sale.createdAt.take(10)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = "+${sale.creatorEarning} pts",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1E88E5)
            )
            val usd = String.format(java.util.Locale.US, "$%.2f", sale.creatorEarning * 0.007)
            Text(
                text = usd,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun PayoutRow(payout: PayoutHistoryItem) {
    val statusColor = when (payout.status.uppercase()) {
        "COMPLETED" -> Color(0xFF2E7D32)
        "PROCESSING" -> Color(0xFFFB8C00)
        "REJECTED" -> MaterialTheme.colorScheme.error
        else -> Color(0xFF1E88E5)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = String.format(java.util.Locale.US, "$%.2f USD", payout.amountUsd),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.width(8.dp))
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = statusColor.copy(alpha = 0.12f)
                ) {
                    Text(
                        text = payout.status.uppercase(),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = statusColor,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
            Text(
                text = "${payout.payoutMethod}: ${payout.payoutAccount} · ${payout.createdAt.take(10)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "Ref: ${payout.referenceId}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.outline
            )
        }
        Text(
            text = "-${payout.creditsDeducted} pts",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.error
        )
    }
}
