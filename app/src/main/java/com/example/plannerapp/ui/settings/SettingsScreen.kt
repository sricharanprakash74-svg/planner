package com.example.plannerapp.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.HelpOutline
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.automirrored.outlined.TrendingUp
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.plannerapp.theme.AppDimens

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onBack: () -> Unit,
    onEditProfileClick: () -> Unit,
    onManageAccountClick: () -> Unit = onEditProfileClick,
    onSavedPlansClick: () -> Unit,
    onActivityLogClick: () -> Unit,
    onNotificationsClick: () -> Unit,
    onTimeFocusClick: () -> Unit,
    onTimezoneClick: () -> Unit,
    onPlanPrivacyClick: () -> Unit,
    onPublishPlanClick: () -> Unit,
    onAppearanceClick: () -> Unit,
    onBackupClick: () -> Unit,
    onExportClick: () -> Unit,
    onAccessibilityClick: () -> Unit,
    onCreatorSetupClick: () -> Unit,
    onCreatorMonetizationClick: () -> Unit = {},
    onCreatorStudioClick: () -> Unit = onCreatorMonetizationClick,
    onSubscriptionClick: () -> Unit = {},
    onHelpClick: () -> Unit,
    onPrivacyPolicyClick: () -> Unit,
    onAboutClick: () -> Unit,
    onSignInClick: () -> Unit,
    onSignOutClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val user by viewModel.currentUser.collectAsState()
    val isGuest = user?.cloudUserId == null
    val isCreator = user?.isCreator == true

    val context = LocalContext.current
    val notifPrefs = remember { viewModel.loadNotifPrefs(context) }
    val notifLabel = if (notifPrefs["allow"] == true) "On" else "Off"
    val privacyPrefs = remember { viewModel.loadPlanPrivacyPrefs(context) }
    val defaultPrivacyLabel = when (privacyPrefs["visibility"] as? Int) {
        1 -> "Private"
        2 -> "Friends"
        else -> "Public"
    }
    val appearancePrefs = remember { viewModel.loadAppearancePrefs(context) }
    val appearanceLabel = if (appearancePrefs["dark_mode"] == true) "Dark" else "System"
    val backupPrefs = remember { context.getSharedPreferences("backup_prefs", android.content.Context.MODE_PRIVATE) }
    val backupLabel = if (backupPrefs.getBoolean("auto_backup", true)) "On" else "Off"

    var searchQuery by remember { mutableStateOf("") }
    var showSignOutDialog by remember { mutableStateOf(false) }
    var showCreatorWaitlistSheet by remember { mutableStateOf(false) }

    // Define all settings entries for search filtering
    data class SettingsEntry(
        val label: String,
        val subtitle: String? = null,
        val icon: ImageVector,
        val trailingText: String? = null,
        val labelColor: Color? = null,
        val onClick: () -> Unit
    )

    val allEntries = buildList {
        add(SettingsEntry("Profile Center", "Display name, bio, personal details", Icons.Outlined.Person, onClick = onEditProfileClick))
        add(SettingsEntry("Manage Account", "Email, password, deletion", Icons.Outlined.AccountCircle, onClick = onManageAccountClick))
        add(SettingsEntry("Saved Plans", null, Icons.Outlined.Bookmark, onClick = onSavedPlansClick))
        add(SettingsEntry("Activity Log", "Plans created, tasks completed, badges", Icons.Outlined.History, onClick = onActivityLogClick))
        add(SettingsEntry("Notifications", "Reminders & alerts", Icons.Outlined.Notifications, trailingText = notifLabel, onClick = onNotificationsClick))
        add(SettingsEntry("Time & Focus", "Focus mode, Pomodoro timer, daily limits", Icons.Outlined.Timer, onClick = onTimeFocusClick))
        add(SettingsEntry("Language & Timezone", user?.userTimezone ?: "System Default", Icons.Outlined.Language, trailingText = user?.userTimezone?.take(15) ?: "Default", onClick = onTimezoneClick))
        if (isCreator) {
            add(SettingsEntry("Plan Privacy", defaultPrivacyLabel, Icons.Outlined.Lock, trailingText = defaultPrivacyLabel, onClick = onPlanPrivacyClick))
            add(SettingsEntry("Publish a Plan", "Share with the community", Icons.Outlined.Public, onClick = onPublishPlanClick))
        }
        add(SettingsEntry("Appearance", "Theme, dark mode", Icons.Outlined.Palette, trailingText = appearanceLabel, onClick = onAppearanceClick))
        add(SettingsEntry("Backup & Sync", "Manage cloud backups", Icons.Outlined.Backup, trailingText = backupLabel, onClick = onBackupClick))
        add(SettingsEntry("Export Data", "JSON, CSV, or Markdown", Icons.Outlined.FileDownload, onClick = onExportClick))
        add(SettingsEntry("Accessibility", "Text size, contrast, motion", Icons.Outlined.Accessibility, onClick = onAccessibilityClick))
        add(SettingsEntry("Creator Program", "Share routines, inspire the community, and earn rewards (Coming Soon)", Icons.Outlined.AutoAwesome, trailingText = "Coming Soon", onClick = { showCreatorWaitlistSheet = true }))
        add(SettingsEntry("Planner Pro Pass", "Subscriptions & multi-store in-app purchases", Icons.Outlined.WorkspacePremium, onClick = onSubscriptionClick))
        add(SettingsEntry("Help & FAQ", null, Icons.AutoMirrored.Outlined.HelpOutline, onClick = onHelpClick))
        add(SettingsEntry("About PlannerApp", "v1.0.0", Icons.Outlined.Info, onClick = onAboutClick))
    }

    val filteredEntries = remember(searchQuery, allEntries) {
        if (searchQuery.isBlank()) emptyList()
        else allEntries.filter {
            it.label.contains(searchQuery, ignoreCase = true) ||
            it.subtitle?.contains(searchQuery, ignoreCase = true) == true
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        modifier = modifier
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
        ) {
            // Search bar — filled style, no ring
            TextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search settings") },
                leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = "Search") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = AppDimens.Space16, vertical = AppDimens.Space8),
                shape = RoundedCornerShape(AppDimens.CornerCompact),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor   = MaterialTheme.colorScheme.surfaceVariant,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                    focusedIndicatorColor   = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                )
            )

            if (searchQuery.isNotBlank()) {
                // Flat filtered search results
                if (filteredEntries.isEmpty()) {
                    Spacer(modifier = Modifier.height(AppDimens.Space24))
                    Text(
                        text = "No settings found for \"$searchQuery\"",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = AppDimens.Space16)
                    )
                } else {
                    Spacer(modifier = Modifier.height(AppDimens.Space8))
                    SettingsInsetCard {
                        filteredEntries.forEachIndexed { i, entry ->
                            SettingsItemRow(
                                icon = entry.icon,
                                label = entry.label,
                                subtitle = entry.subtitle,
                                trailingText = entry.trailingText,
                                labelColor = entry.labelColor ?: MaterialTheme.colorScheme.onSurface,
                                onClick = entry.onClick
                            )
                            if (i < filteredEntries.lastIndex) SettingsIntraCardDivider()
                        }
                    }
                }
            } else {
                // ── Full sectioned list — each section is an inset-grouped Card ──────────

                // --- Your account ---
                SettingsSectionLabel("Your account")
                SettingsInsetCard {
                    SettingsItemRow(
                        icon = Icons.Outlined.Person,
                        label = "Profile Center",
                        subtitle = "Display name, bio, personal details",
                        onClick = onEditProfileClick
                    )
                    SettingsIntraCardDivider()
                    SettingsItemRow(
                        icon = Icons.Outlined.AutoAwesome,
                        label = "Creator Program",
                        subtitle = "Share routines, inspire the community, and earn rewards",
                        trailingText = "Coming Soon",
                        onClick = { showCreatorWaitlistSheet = true }
                    )
                }

                // --- How you use PlannerApp ---
                SettingsSectionLabel("How you use PlannerApp")
                SettingsInsetCard {
                    SettingsItemRow(icon = Icons.Outlined.Bookmark, label = "Saved Plans", onClick = onSavedPlansClick)
                    SettingsIntraCardDivider()
                    SettingsItemRow(icon = Icons.Outlined.History, label = "Activity Log", subtitle = "Plans created, tasks completed, badges", onClick = onActivityLogClick)
                    SettingsIntraCardDivider()
                    SettingsItemRow(icon = Icons.Outlined.Notifications, label = "Notifications", subtitle = "Reminders & alerts", trailingText = notifLabel, onClick = onNotificationsClick)
                    SettingsIntraCardDivider()
                    SettingsItemRow(icon = Icons.Outlined.Timer, label = "Time & Focus", subtitle = "Focus mode, Pomodoro timer, daily limits", onClick = onTimeFocusClick)
                    SettingsIntraCardDivider()
                    SettingsItemRow(
                        icon = Icons.Outlined.Language,
                        label = "Language & Timezone",
                        trailingText = user?.userTimezone?.take(20) ?: "System Default",
                        onClick = onTimezoneClick
                    )
                }

                // --- Who can see your plans (creator only) ---
                if (isCreator) {
                    SettingsSectionLabel("Who can see your plans")
                    SettingsInsetCard {
                        SettingsItemRow(
                            icon = Icons.Outlined.Lock,
                            label = "Plan Privacy",
                            trailingText = defaultPrivacyLabel,
                            onClick = onPlanPrivacyClick
                        )
                        SettingsIntraCardDivider()
                        SettingsItemRow(
                            icon = Icons.Outlined.Public,
                            label = "Publish a Plan",
                            subtitle = "Share with the community",
                            onClick = onPublishPlanClick
                        )
                    }
                }

                // --- Account & Data ---
                SettingsSectionLabel("Account & Data")
                SettingsInsetCard {
                    val isOnline = com.example.plannerapp.data.LocalIsOnline.current
                    if (isOnline) {
                        SettingsItemRow(icon = Icons.Outlined.Backup, label = "Backup & Sync", subtitle = "Manage cloud backups", trailingText = backupLabel, onClick = onBackupClick)
                        SettingsIntraCardDivider()
                    }
                    SettingsItemRow(icon = Icons.Outlined.AccountCircle, label = "Manage Account", subtitle = "Email, password, deletion", onClick = onManageAccountClick)
                }

                // --- Your app & data ---
                SettingsSectionLabel("Your app & data")
                SettingsInsetCard {
                    SettingsItemRow(icon = Icons.Outlined.Palette, label = "Appearance", subtitle = "Theme, dark mode", trailingText = appearanceLabel, onClick = onAppearanceClick)
                    SettingsIntraCardDivider()
                    SettingsItemRow(icon = Icons.Outlined.FileDownload, label = "Export Data", subtitle = "JSON, CSV, or Markdown", onClick = onExportClick)
                    SettingsIntraCardDivider()
                    SettingsItemRow(icon = Icons.Outlined.Accessibility, label = "Accessibility", subtitle = "Text size, contrast, motion", onClick = onAccessibilityClick)
                }

                // --- Your insights & tools ---
                SettingsSectionLabel("Your insights & tools")
                SettingsInsetCard {
                    if (com.example.plannerapp.data.LocalIsOnline.current) {
                        SettingsItemRow(
                            icon = Icons.Outlined.AutoAwesome,
                            label = "Creator Program",
                            subtitle = "Tools for habit leaders and creators rolling out soon",
                            trailingText = "Coming Soon",
                            onClick = { showCreatorWaitlistSheet = true }
                        )
                        SettingsIntraCardDivider()
                    }
                    SettingsItemRow(
                        icon = Icons.Outlined.WorkspacePremium,
                        label = "Planner Pro Pass",
                        subtitle = "Manage membership, store subscriptions & restore purchases",
                        onClick = onSubscriptionClick
                    )
                }

                // --- More info & support ---
                SettingsSectionLabel("More info & support")
                SettingsInsetCard {
                    SettingsItemRow(icon = Icons.AutoMirrored.Outlined.HelpOutline, label = "Help & FAQ", onClick = onHelpClick)
                    SettingsIntraCardDivider()
                    SettingsItemRow(icon = Icons.Outlined.PrivacyTip, label = "Privacy Policy", onClick = onPrivacyPolicyClick)
                    SettingsIntraCardDivider()
                    SettingsItemRow(icon = Icons.Outlined.Info, label = "About PlannerApp", trailingText = "v1.0.0", onClick = onAboutClick)
                }

                // --- Login ---
                SettingsSectionLabel("Login")
                SettingsInsetCard {
                    if (isGuest) {
                        SettingsItemRow(
                            icon = Icons.Outlined.AccountCircle,
                            label = "Sign In or Create Account",
                            subtitle = "Sync your plans across devices",
                            labelColor = MaterialTheme.colorScheme.primary,
                            onClick = onSignInClick
                        )
                    } else {
                        SettingsItemRow(
                            icon = Icons.AutoMirrored.Outlined.Logout,
                            label = "Log Out",
                            labelColor = MaterialTheme.colorScheme.error,
                            onClick = { showSignOutDialog = true }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(AppDimens.Space32))
            }
        }
    }

    if (showSignOutDialog) {
        AlertDialog(
            onDismissRequest = { showSignOutDialog = false },
            title = { Text("Log Out") },
            text = { Text("You will be signed out and returned to the sign-in screen. Your local data will be preserved.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showSignOutDialog = false
                        viewModel.signOut { onSignOutClick() }
                    }
                ) {
                    Text("Log Out", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showSignOutDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showCreatorWaitlistSheet) {
        com.example.plannerapp.ui.creator.CreatorWaitlistSheet(
            onDismissRequest = { showCreatorWaitlistSheet = false }
        )
    }
}

/** Apple-style inset-grouped section card wrapping a set of settings rows. */
@Composable
private fun SettingsInsetCard(content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = AppDimens.Space16),
        shape = RoundedCornerShape(AppDimens.CornerCard),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = AppDimens.ElevationNone)
    ) {
        Column(content = content)
    }
    Spacer(modifier = Modifier.height(AppDimens.Space8))
}

/** Thin divider between rows inside an inset card — indented past the icon. */
@Composable
private fun SettingsIntraCardDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(start = 56.dp),
        thickness = 0.5.dp,
        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
    )
}

@Composable
private fun SettingsSectionLabel(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(
            start = AppDimens.Space16,
            top = AppDimens.Space24,
            bottom = AppDimens.Space8,
            end = AppDimens.Space16
        )
    )
}

@Composable
private fun SettingsItemRow(
    icon: ImageVector,
    label: String,
    subtitle: String? = null,
    trailingText: String? = null,
    labelColor: Color = Color.Unspecified,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = AppDimens.Space16, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            modifier = Modifier.size(22.dp),
            tint = if (labelColor != Color.Unspecified) labelColor else MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.width(AppDimens.Space16))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyLarge,
                color = if (labelColor != Color.Unspecified) labelColor else MaterialTheme.colorScheme.onSurface
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        if (trailingText != null) {
            Text(
                text = trailingText,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.width(AppDimens.Space4))
        }
        // Chevron recedes to alpha 0.45 — row label commands full visual attention
        Icon(
            imageVector = Icons.Outlined.ChevronRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f),
            modifier = Modifier.size(18.dp)
        )
    }
}
