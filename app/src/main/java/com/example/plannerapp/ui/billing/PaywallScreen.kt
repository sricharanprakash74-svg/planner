package com.example.plannerapp.ui.billing

import android.app.Activity
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.plannerapp.billing.BillingViewModel
import com.revenuecat.purchases.Package
import com.revenuecat.purchases.PackageType

/**
 * Modern Jetpack Compose Paywall Screen for PlannerApp Pro Pass.
 *
 * Store-agnostic: adapts seamlessly to Google Play and Samsung Galaxy Store.
 * Strictly adheres to the zero-emoji guideline using Material 3 vector icons.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaywallScreen(
    viewModel: BillingViewModel,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val activity = context as? Activity
    val snackbarHostState = remember { SnackbarHostState() }

    val accentColor = MaterialTheme.colorScheme.primary

    LaunchedEffect(uiState.errorMessage, uiState.userFeedbackMessage) {
        val error = uiState.errorMessage
        val feedback = uiState.userFeedbackMessage
        if (error != null) {
            snackbarHostState.showSnackbar(error)
            viewModel.clearFeedback()
        } else if (feedback != null) {
            snackbarHostState.showSnackbar(feedback)
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
                            text = "PLANNER PRO",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.primaryContainer
                        ) {
                            Text(
                                text = uiState.storeName,
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
                    TextButton(
                        onClick = { viewModel.restorePurchases() },
                        enabled = !uiState.isRestoring && !uiState.isPurchasing
                    ) {
                        if (uiState.isRestoring) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text(
                                text = "Restore",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold
                            )
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
                        .padding(horizontal = 20.dp, vertical = 14.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (uiState.isPremium) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.CheckCircle,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Planner Pro is Active",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }
                    } else {
                        Button(
                            onClick = {
                                if (activity != null) {
                                    viewModel.purchase(activity)
                                }
                            },
                            enabled = !uiState.isPurchasing && !uiState.isRestoring && uiState.selectedPackage != null,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                        ) {
                            if (uiState.isPurchasing) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(22.dp),
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    strokeWidth = 2.dp
                                )
                            } else {
                                val pkg = uiState.selectedPackage
                                val buttonText = if (pkg != null) {
                                    "Continue with ${pkg.product.price.formatted}"
                                } else {
                                    "Select a Plan"
                                }
                                Text(
                                    text = buttonText,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    // Compliance disclaimer footnote
                    Text(
                        text = "Recurring billing. Cancel anytime in ${uiState.storeName} account settings. Terms and Privacy Policy apply.",
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
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Hero Value Proposition Card
            item {
                Spacer(modifier = Modifier.height(4.dp))
                HeroHeaderCard(storeName = uiState.storeName, isPremium = uiState.isPremium)
            }

            // Benefits Checklist
            item {
                ProFeaturesList()
            }

            // Packages Section Header
            item {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Choose Your Membership",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    if (uiState.isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp
                        )
                    }
                }
            }

            // Available Packages
            val packages = uiState.currentOffering?.availablePackages.orEmpty()
            if (packages.isNotEmpty()) {
                items(packages, key = { it.identifier }) { pkg ->
                    val isSelected = uiState.selectedPackage?.identifier == pkg.identifier
                    PackageTierCard(
                        rcPackage = pkg,
                        isSelected = isSelected,
                        onClick = { viewModel.selectPackage(pkg) }
                    )
                }
            } else if (!uiState.isLoading) {
                // Fallback state when RevenueCat API keys are placeholders, offline, or network dropped
                item {
                    PlaceholderOfferingNotice(
                        storeName = uiState.storeName,
                        errorMessage = uiState.errorMessage,
                        onRetry = { viewModel.fetchOfferings() }
                    )
                }
            }

            // Prominent Store Compliance Restore Section
            item {
                OutlinedCard(
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Already have a subscription?",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "Restore your previous purchases across devices.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        TextButton(
                            onClick = { viewModel.restorePurchases() },
                            enabled = !uiState.isRestoring && !uiState.isPurchasing
                        ) {
                            Text(
                                text = "Restore",
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun HeroHeaderCard(storeName: String, isPremium: Boolean) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(54.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.WorkspacePremium,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(30.dp)
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = if (isPremium) "Planner Pro Unlocked" else "Elevate Your Routine",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Full access to premium features, advanced analytics, creator tools, and cross-device sync via $storeName.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun ProFeaturesList() {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            ProFeatureRow(
                icon = Icons.Outlined.AllInclusive,
                title = "Unlimited Habit Plans",
                subtitle = "Create and track unlimited custom routines without restrictions."
            )
            ProFeatureRow(
                icon = Icons.Outlined.Insights,
                title = "Advanced AI Analytics",
                subtitle = "Deep consistency trends, streak projections, and performance insights."
            )
            ProFeatureRow(
                icon = Icons.Outlined.Bolt,
                title = "Streak Shield & Freezes",
                subtitle = "Complimentary monthly streak protection shields for off-schedule days."
            )
            ProFeatureRow(
                icon = Icons.Outlined.Storefront,
                title = "Verified Creator Privileges",
                subtitle = "Publish paid templates and monetize directly with 70% revenue share."
            )
        }
    }
}

@Composable
private fun ProFeatureRow(icon: ImageVector, title: String, subtitle: String) {
    Row(
        verticalAlignment = Alignment.Top,
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp)
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun PackageTierCard(
    rcPackage: Package,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val borderColor = if (isSelected) primaryColor else MaterialTheme.colorScheme.outlineVariant

    val periodTitle = when (rcPackage.packageType) {
        PackageType.ANNUAL -> "Annual Plan"
        PackageType.MONTHLY -> "Monthly Plan"
        PackageType.WEEKLY -> "Weekly Plan"
        PackageType.LIFETIME -> "Lifetime Access"
        else -> rcPackage.identifier.replaceFirstChar { it.uppercase() }
    }

    val badgeText = when (rcPackage.packageType) {
        PackageType.ANNUAL -> "Best Value · Save 50%"
        PackageType.LIFETIME -> "One-Time Payment"
        PackageType.MONTHLY -> "Most Flexible"
        else -> null
    }

    OutlinedCard(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(if (isSelected) 2.dp else 1.dp, borderColor),
        colors = CardDefaults.outlinedCardColors(
            containerColor = if (isSelected) primaryColor.copy(alpha = 0.05f) else MaterialTheme.colorScheme.surface
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
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = periodTitle,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        if (badgeText != null) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = MaterialTheme.colorScheme.primaryContainer
                            ) {
                                Text(
                                    text = badgeText,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                    Text(
                        text = rcPackage.product.description.ifBlank { "Unlocks full access to all features" },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = rcPackage.product.price.formatted,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (isSelected) primaryColor else MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

@Composable
private fun PlaceholderOfferingNotice(
    storeName: String,
    errorMessage: String?,
    onRetry: () -> Unit
) {
    OutlinedCard(
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Outlined.Info,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "$storeName Integration Ready",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
            }
            if (errorMessage != null) {
                Text(
                    text = errorMessage,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            } else {
                Text(
                    text = "RevenueCat is configured for $storeName. To test live purchases, provide your RevenueCat public API key in build.gradle.kts and configure products in the RevenueCat dashboard.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            OutlinedButton(
                onClick = onRetry,
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.align(Alignment.End)
            ) {
                Icon(
                    imageVector = Icons.Outlined.Refresh,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text("Retry Loading Plans")
            }
        }
    }
}
