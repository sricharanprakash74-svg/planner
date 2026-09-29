package com.example.plannerapp.ui.billing

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.plannerapp.billing.BillingConfig
import com.example.plannerapp.billing.BillingViewModel
import com.example.plannerapp.theme.AppAccent
import com.example.plannerapp.theme.AppDimens
import com.example.plannerapp.theme.AppDisabledBgLight
import com.example.plannerapp.theme.AppDisabledText
import com.example.plannerapp.theme.AppPrimaryButton
import com.example.plannerapp.theme.AppPrimaryButtonPressed
import com.example.plannerapp.theme.AppSuccess

/**
 * Modern Jetpack Compose Dummy Paywall Screen.
 *
 * 100% offline, zero external billing SDK dependencies.
 * Adheres strictly to the Zero Emojis guideline using Material 3 vector icons.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaywallScreen(
    viewModel: BillingViewModel,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = androidx.compose.ui.platform.LocalContext.current
    val activity = remember(context) { context as? android.app.Activity }

    LaunchedEffect(uiState.userFeedbackMessage) {
        uiState.userFeedbackMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearFeedback()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "taxent Pro",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.primaryContainer
                        ) {
                            Text(
                                text = "Pro Pass",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Outlined.Close,
                            contentDescription = "Close Paywall"
                        )
                    }
                },
                actions = {
                    if (uiState.isProActive) {
                        TextButton(onClick = { viewModel.resetMockStatus() }) {
                            Text("Reset", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        bottomBar = {
            Surface(
                tonalElevation = 6.dp,
                shadowElevation = 8.dp,
                color = MaterialTheme.colorScheme.surface
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 20.dp, vertical = 14.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (uiState.isProActive) {
                        Button(
                            onClick = onDismiss,
                            shape = RoundedCornerShape(AppDimens.CornerButton),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = Color.White
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(AppDimens.ButtonHeight)
                        ) {
                            Text(
                                text = "Continue to App",
                                style = MaterialTheme.typography.labelLarge
                            )
                        }
                    } else {
                        val purchaseSource = remember { MutableInteractionSource() }
                        val isPurchasePressed by purchaseSource.collectIsPressedAsState()
                        val purchaseBg = if (isPurchasePressed) AppPrimaryButtonPressed else AppPrimaryButton

                        Button(
                            onClick = { viewModel.purchase(activity) },
                            enabled = !uiState.isPurchasing && !uiState.isRestoring,
                            interactionSource = purchaseSource,
                            shape = RoundedCornerShape(AppDimens.CornerButton),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = purchaseBg,
                                contentColor = Color.White,
                                disabledContainerColor = AppDisabledBgLight,
                                disabledContentColor = AppDisabledText
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(AppDimens.ButtonHeight)
                        ) {
                            if (uiState.isPurchasing) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(22.dp),
                                    color = Color.White,
                                    strokeWidth = 2.dp
                                )
                            } else {
                                val ctaText = if (uiState.selectedTier == BillingConfig.PACKAGE_ANNUAL) {
                                    "Start 7-Day Free Trial — then \$39.99/year"
                                } else {
                                    "Start Monthly Membership — \$4.99/month"
                                }
                                Text(
                                    text = ctaText,
                                    style = MaterialTheme.typography.labelLarge
                                )
                            }
                        }

                        // Ghost button for Restore Purchases
                        TextButton(
                            onClick = { viewModel.restorePurchases() },
                            enabled = !uiState.isRestoring && !uiState.isPurchasing,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(AppDimens.ButtonHeight),
                            shape = RoundedCornerShape(AppDimens.CornerButton)
                        ) {
                            if (uiState.isRestoring) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                            }
                            Text(
                                text = "Restore Purchases",
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Medium),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // Compliance disclaimer footnote
                    Text(
                        text = "Recurring billing. Cancel anytime in your account settings. Terms of Service & Privacy Policy apply.",
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        modifier = modifier
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = AppDimens.ScreenHorizontalPadding),
            verticalArrangement = Arrangement.spacedBy(AppDimens.Space16)
        ) {
            // Success state banner if Pro is active
            if (uiState.isProActive) {
                item {
                    Spacer(modifier = Modifier.height(4.dp))
                    ProUnlockedBanner(onContinue = onDismiss)
                }
            }

            // Header Section: Unlock Pro Access
            item {
                Spacer(modifier = Modifier.height(4.dp))
                HeaderCard()
            }

            // Feature List with checkmarks
            item {
                ProFeaturesCard()
            }

            // Subscription Tiers Header
            item {
                Text(
                    text = "Choose Your Membership",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            // Annual Tier Card ($39.99/yr - Save 33%)
            item {
                TierCard(
                    title = "Annual Plan",
                    price = BillingConfig.PRICE_ANNUAL,
                    subtitle = "$3.33 / month · Billed annually",
                    savingsBadge = "Save 33%",
                    isSelected = uiState.selectedTier == BillingConfig.PACKAGE_ANNUAL,
                    onClick = { viewModel.selectTier(BillingConfig.PACKAGE_ANNUAL) }
                )
            }

            // Monthly Tier Card ($4.99/mo)
            item {
                TierCard(
                    title = "Monthly Plan",
                    price = BillingConfig.PRICE_MONTHLY,
                    subtitle = "Flexible recurring monthly subscription",
                    savingsBadge = null,
                    isSelected = uiState.selectedTier == BillingConfig.PACKAGE_MONTHLY,
                    onClick = { viewModel.selectTier(BillingConfig.PACKAGE_MONTHLY) }
                )
            }

            // Bottom Spacing
            item {
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun HeaderCard() {
    Surface(
        shape = RoundedCornerShape(AppDimens.CornerCard),
        color = MaterialTheme.colorScheme.primaryContainer,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(AppDimens.Space20),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.WorkspacePremium,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(32.dp)
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "Unlock Pro Access",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Supercharge your habit routines and unlock unrestricted productivity superpowers.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f),
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun ProFeaturesCard() {
    Surface(
        shape = RoundedCornerShape(AppDimens.CornerCard),
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(AppDimens.CardInternalPadding),
            verticalArrangement = Arrangement.spacedBy(AppDimens.Space16)
        ) {
            FeatureRow(
                title = "Unlimited Cloud Sync",
                subtitle = "Seamless real-time synchronization across all your devices."
            )
            FeatureRow(
                title = "Advanced Analytics",
                subtitle = "Consistency stock-market curves, streak projections & reports."
            )
            FeatureRow(
                title = "Ad-free Experience",
                subtitle = "Completely distraction-free planning without interruptions."
            )
            FeatureRow(
                title = "Streak Freeze Protection",
                subtitle = "Automatic streak shield saves your streaks on off-schedule days."
            )
        }
    }
}

@Composable
private fun FeatureRow(
    title: String,
    subtitle: String
) {
    Row(
        verticalAlignment = Alignment.Top,
        modifier = Modifier.fillMaxWidth()
    ) {
        Icon(
            imageVector = Icons.Filled.Check,
            contentDescription = null,
            tint = AppSuccess,
            modifier = Modifier
                .padding(top = 2.dp)
                .size(AppDimens.IconSizeMd)
        )
        Spacer(modifier = Modifier.width(AppDimens.Space12))
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge.copy(fontSize = 15.sp),
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp),
                fontWeight = FontWeight.Normal,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun TierCard(
    title: String,
    price: String,
    subtitle: String,
    savingsBadge: String?,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val borderColor = if (isSelected) primaryColor else MaterialTheme.colorScheme.outlineVariant

    OutlinedCard(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(if (isSelected) 2.dp else 1.dp, borderColor),
        colors = CardDefaults.outlinedCardColors(
            containerColor = if (isSelected) primaryColor.copy(alpha = 0.06f) else MaterialTheme.colorScheme.surface
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                RadioButton(
                    selected = isSelected,
                    onClick = onClick
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f, fill = false)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1
                        )
                        if (savingsBadge != null) {
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = MaterialTheme.colorScheme.primaryContainer
                            ) {
                                Text(
                                    text = savingsBadge,
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp),
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            Text(
                text = price,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = if (isSelected) primaryColor else MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun ProUnlockedBanner(onContinue: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.primaryContainer,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = Icons.Outlined.CheckCircle,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "Pro Unlocked",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        text = "All Pro features are active for this account.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                    )
                }
            }
            TextButton(onClick = onContinue) {
                Text(
                    text = "Done",
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}
