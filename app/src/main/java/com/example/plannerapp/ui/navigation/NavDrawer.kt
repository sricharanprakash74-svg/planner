package com.example.plannerapp.ui.navigation

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.plannerapp.data.PlanEntity
import com.example.plannerapp.theme.AppDimens

@Composable
fun AppNavDrawerSheet(
    isCreator: Boolean,
    isOnline: Boolean,
    plans: List<PlanEntity>,
    pinnedPlans: List<PlanEntity>,
    showAllRecentPlans: Boolean,
    onToggleShowAllRecent: () -> Unit,
    onPlanClick: (Long) -> Unit,
    onAddPlanClick: () -> Unit,
    onExploreClick: (String) -> Unit,
    onCreditStoreClick: () -> Unit,
    onCreatorProgramClick: () -> Unit,
    onUpgradeProClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onCloseDrawer: () -> Unit,
    modifier: Modifier = Modifier
) {
    ModalDrawerSheet(
        modifier = modifier.width(310.dp),
        drawerContainerColor = MaterialTheme.colorScheme.surface
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(vertical = 16.dp)
        ) {
            // Header Brand
            item {
                Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Folder,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "taxent",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold
                                )
                                if (isCreator) {
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Outlined.Verified,
                                                contentDescription = "Verified Creator",
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(12.dp)
                                            )
                                            Spacer(modifier = Modifier.width(3.dp))
                                            Text(
                                                text = "Creator",
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    }
                                }
                            }
                            Text(
                                text = "${plans.size} Active Plans",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
                Spacer(modifier = Modifier.height(8.dp))
            }

            // ── Recently Visited ─────────────────────────────────────────
            if (plans.isNotEmpty()) {
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Recently Visited",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        TextButton(
                            onClick = onToggleShowAllRecent,
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                        ) {
                            Text(
                                text = if (showAllRecentPlans) "Show less" else "See all",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }

                val recentList = if (showAllRecentPlans) plans else plans.take(3)
                items(recentList, key = { "recent_${it.planId}" }) { plan ->
                    val colorSeed = (plan.heading.hashCode() and 0x7FFFFFFF) % 4
                    val avatarColor = when (colorSeed) {
                        0 -> Color(0xFFE53935)
                        1 -> Color(0xFF1E88E5)
                        2 -> Color(0xFF43A047)
                        else -> Color(0xFFFB8C00)
                    }
                    NavigationDrawerItem(
                        label = {
                            Text(
                                text = plan.heading,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                fontWeight = FontWeight.Medium
                            )
                        },
                        icon = {
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(CircleShape)
                                    .background(avatarColor),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = plan.heading.firstOrNull()?.uppercase() ?: "P",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        },
                        selected = false,
                        onClick = {
                            onCloseDrawer()
                            onPlanClick(plan.planId)
                        },
                        modifier = Modifier.padding(horizontal = 12.dp)
                    )
                }

                item {
                    Spacer(modifier = Modifier.height(4.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
                    Spacer(modifier = Modifier.height(4.dp))
                }
            }

            // ── Offline Status Banner (when offline) ────────────────────
            if (!isOnline) {
                item {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        shape = RoundedCornerShape(AppDimens.CornerCard),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        border = BorderStroke(
                            1.dp,
                            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.CloudOff,
                                contentDescription = "Offline Mode",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Offline Mode",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Local storage active",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            // ── Upgrade to Pro Banner (online only) ──────────────────────
            if (isOnline && !isCreator) {
                item {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 4.dp)
                            .clickable {
                                onCloseDrawer()
                                onUpgradeProClick()
                            },
                        shape = RoundedCornerShape(AppDimens.CornerCard),
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
                        border = BorderStroke(
                            1.dp,
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.20f)
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(
                                horizontal = AppDimens.Space16,
                                vertical = AppDimens.Space12
                            ),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.WorkspacePremium,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(AppDimens.IconSizeLg)
                                )
                                Spacer(modifier = Modifier.width(AppDimens.Space12))
                                Column {
                                    Text(
                                        text = "Upgrade to Pro",
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "Cloud sync, streak freezes & more",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            Icon(
                                imageVector = Icons.AutoMirrored.Outlined.ArrowForward,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(AppDimens.IconSizeSm)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
                    Spacer(modifier = Modifier.height(4.dp))
                }
            }

            // ── Shortcuts / Quick Actions ──────────────────────────────
            item {
                Text(
                    text = "Quick Actions",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
                )

                NavigationDrawerItem(
                    label = { Text("New Plan Folder", fontWeight = FontWeight.SemiBold) },
                    icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                    selected = false,
                    onClick = {
                        onCloseDrawer()
                        onAddPlanClick()
                    },
                    modifier = Modifier.padding(horizontal = 12.dp)
                )

                NavigationDrawerItem(
                    label = { Text("Explore Plans", fontWeight = FontWeight.SemiBold) },
                    icon = { Icon(Icons.Outlined.Explore, contentDescription = null) },
                    selected = false,
                    onClick = {
                        onCloseDrawer()
                        onExploreClick("")
                    },
                    modifier = Modifier.padding(horizontal = 12.dp)
                )

                NavigationDrawerItem(
                    label = { Text("Credit Store", fontWeight = FontWeight.SemiBold) },
                    icon = { Icon(Icons.Outlined.Token, contentDescription = null) },
                    selected = false,
                    onClick = {
                        onCloseDrawer()
                        onCreditStoreClick()
                    },
                    modifier = Modifier.padding(horizontal = 12.dp)
                )

                Spacer(modifier = Modifier.height(4.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
                Spacer(modifier = Modifier.height(4.dp))
            }

            // ── Creator Program (Coming Soon) ──────────────────────────
            if (isOnline) {
                item {
                    Text(
                        text = "Creator Program",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
                    )

                    NavigationDrawerItem(
                        label = {
                            Column {
                                Text(
                                    text = "Creator Program",
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = "Coming Soon",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        },
                        icon = {
                            Icon(
                                imageVector = Icons.Outlined.AutoAwesome,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                        },
                        selected = false,
                        onClick = {
                            onCloseDrawer()
                            onCreatorProgramClick()
                        },
                        modifier = Modifier.padding(horizontal = 12.dp)
                    )

                    Spacer(modifier = Modifier.height(4.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
                    Spacer(modifier = Modifier.height(4.dp))
                }
            }

            // ── Pinned Plans Section (if any) ───────────────────────────
            if (pinnedPlans.isNotEmpty()) {
                item {
                    Text(
                        text = "Pinned Plans",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
                    )
                }

                items(pinnedPlans, key = { "pinned_${it.planId}" }) { plan ->
                    NavigationDrawerItem(
                        label = {
                            Text(
                                text = plan.heading,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        },
                        icon = {
                            Icon(
                                imageVector = Icons.Filled.PushPin,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                        },
                        selected = false,
                        onClick = {
                            onCloseDrawer()
                            onPlanClick(plan.planId)
                        },
                        modifier = Modifier.padding(horizontal = 12.dp)
                    )
                }

                item {
                    Spacer(modifier = Modifier.height(4.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
                    Spacer(modifier = Modifier.height(4.dp))
                }
            }

            // ── Preferences ────────────────────────────────────────────
            item {
                Text(
                    text = "Preferences",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
                )

                NavigationDrawerItem(
                    label = { Text("Settings", fontWeight = FontWeight.SemiBold) },
                    icon = { Icon(Icons.Outlined.Settings, contentDescription = null) },
                    selected = false,
                    onClick = {
                        onCloseDrawer()
                        onSettingsClick()
                    },
                    modifier = Modifier.padding(horizontal = 12.dp)
                )
            }
        }
    }
}
