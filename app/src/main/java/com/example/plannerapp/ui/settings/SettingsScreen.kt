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
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onBack: () -> Unit,
    onEditProfileClick: () -> Unit,
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
        add(SettingsEntry("Creator Studio", if (isCreator) "Manage your creator profile" else "Set up your creator profile", Icons.Outlined.Star, onClick = onCreatorSetupClick))
        if (isCreator) {
            add(SettingsEntry("Creator Monetization & Payouts", "Cash out earned credits, manage payout methods and sales", Icons.Outlined.Payments, onClick = onCreatorMonetizationClick))
        }
        add(SettingsEntry("Planner Pro Pass", "Subscriptions & multi-store in-app purchases", Icons.Outlined.WorkspacePremium, onClick = onSubscriptionClick))
        add(SettingsEntry("Help & FAQ", null, Icons.AutoMirrored.Outlined.HelpOutline, onClick = onHelpClick))
        add(SettingsEntry("About PlannerApp", "v1.0.0", Icons.Outlined.Info, onClick = onAboutClick))
    }

    val filteredEntries = remember(searchQuery, allEntries) {
        if (searchQuery.isBlank()) emptyList()
        else allEntries.filter { it.label.contains(searchQuery, ignoreCase = true) || it.subtitle?.contains(searchQuery, ignoreCase = true) == true }
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
            // Search bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search settings") },
                leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = "Search") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                shape = RoundedCornerShape(12.dp)
            )

            if (searchQuery.isNotBlank()) {
                // Flat filtered search results
                if (filteredEntries.isEmpty()) {
                    Spacer(modifier = Modifier.height(24.dp))
                    Text(
                        text = "No settings found for \"$searchQuery\"",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                } else {
                    Spacer(modifier = Modifier.height(8.dp))
                    filteredEntries.forEach { entry ->
                        SettingsItemRow(
                            icon = entry.icon,
                            label = entry.label,
                            subtitle = entry.subtitle,
                            trailingText = entry.trailingText,
                            labelColor = entry.labelColor ?: MaterialTheme.colorScheme.onSurface,
                            onClick = entry.onClick
                        )
                    }
                }
            } else {
                // Full sectioned list

                // --- Your account ---
                SettingsSectionLabel("Your account")
                SettingsItemRow(
                    icon = Icons.Outlined.Person,
                    label = "Profile Center",
                    subtitle = "Display name, bio, personal details",
                    onClick = onEditProfileClick
                )
                SettingsSectionDivider()

                // --- How you use PlannerApp ---
                SettingsSectionLabel("How you use PlannerApp")
                SettingsItemRow(icon = Icons.Outlined.Bookmark, label = "Saved Plans", onClick = onSavedPlansClick)
                SettingsItemRow(icon = Icons.Outlined.History, label = "Activity Log", subtitle = "Plans created, tasks completed, badges", onClick = onActivityLogClick)
                SettingsItemRow(icon = Icons.Outlined.Notifications, label = "Notifications", subtitle = "Reminders & alerts", trailingText = notifLabel, onClick = onNotificationsClick)
                SettingsItemRow(icon = Icons.Outlined.Timer, label = "Time & Focus", subtitle = "Focus mode, Pomodoro timer, daily limits", onClick = onTimeFocusClick)
                SettingsItemRow(
                    icon = Icons.Outlined.Language,
                    label = "Language & Timezone",
                    trailingText = user?.userTimezone?.take(20) ?: "System Default",
                    onClick = onTimezoneClick
                )
                SettingsSectionDivider()

                // --- Who can see your plans (creator only) ---
                if (isCreator) {
                    SettingsSectionLabel("Who can see your plans")
                    SettingsItemRow(
                        icon = Icons.Outlined.Lock,
                        label = "Plan Privacy",
                        trailingText = defaultPrivacyLabel,
                        onClick = onPlanPrivacyClick
                    )
                    SettingsItemRow(
                        icon = Icons.Outlined.Public,
                        label = "Publish a Plan",
                        subtitle = "Share with the community",
                        onClick = onPublishPlanClick
                    )
                    SettingsSectionDivider()
                }

                // --- Account Settings ---
                SettingsSectionLabel("Account & Data")
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    val isOnline = com.example.plannerapp.data.LocalIsOnline.current
                    if (isOnline) {
                        SettingsItemRow(icon = Icons.Outlined.Backup, label = "Backup & Sync", subtitle = "Manage cloud backups", trailingText = backupLabel, onClick = onBackupClick)
                        HorizontalDivider(modifier = Modifier.padding(start = 56.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    }
                    SettingsItemRow(icon = Icons.Outlined.AccountCircle, label = "Manage Account", subtitle = "Email, password, deletion", onClick = onEditProfileClick)
                }
                SettingsSectionDivider()

                // --- Your app & data ---
                SettingsSectionLabel("Your app & data")
                SettingsItemRow(icon = Icons.Outlined.Palette, label = "Appearance", subtitle = "Theme, dark mode", trailingText = appearanceLabel, onClick = onAppearanceClick)
                SettingsItemRow(icon = Icons.Outlined.FileDownload, label = "Export Data", subtitle = "JSON, CSV, or Markdown", onClick = onExportClick)
                SettingsItemRow(icon = Icons.Outlined.Accessibility, label = "Accessibility", subtitle = "Text size, contrast, motion", onClick = onAccessibilityClick)
                SettingsSectionDivider()

                // --- Your insights & tools ---
                SettingsSectionLabel("Your insights & tools")
                if (com.example.plannerapp.data.LocalIsOnline.current) {
                    SettingsItemRow(
                        icon = Icons.Outlined.Star,
                        label = "Creator Studio",
                        subtitle = if (isCreator) "Manage your creator profile" else "Set up your creator profile",
                        onClick = onCreatorSetupClick
                    )
                    if (isCreator) {
                        SettingsItemRow(
                            icon = Icons.Outlined.Payments,
                            label = "Creator Monetization & Payouts",
                            subtitle = "Cash out earned credits, manage payout methods and sales",
                            onClick = onCreatorMonetizationClick
                        )
                    }
                }
                SettingsItemRow(
                    icon = Icons.Outlined.WorkspacePremium,
                    label = "Planner Pro Pass",
                    subtitle = "Manage membership, store subscriptions & restore purchases",
                    onClick = onSubscriptionClick
                )
                SettingsSectionDivider()

                // --- More info & support ---
                SettingsSectionLabel("More info & support")
                SettingsItemRow(icon = Icons.AutoMirrored.Outlined.HelpOutline, label = "Help & FAQ", onClick = onHelpClick)
                SettingsItemRow(icon = Icons.Outlined.PrivacyTip, label = "Privacy Policy", onClick = onPrivacyPolicyClick)
                SettingsItemRow(icon = Icons.Outlined.Info, label = "About PlannerApp", trailingText = "v1.0.0", onClick = onAboutClick)
                SettingsSectionDivider()

                // --- Login ---
                SettingsSectionLabel("Login")
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

                Spacer(modifier = Modifier.height(32.dp))
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
}

@Composable
private fun SettingsSectionLabel(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(start = 16.dp, top = 20.dp, bottom = 4.dp, end = 16.dp)
    )
}

@Composable
private fun SettingsSectionDivider() {
    Spacer(modifier = Modifier.height(4.dp))
    HorizontalDivider(
        thickness = 1.dp,
        color = MaterialTheme.colorScheme.outlineVariant
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
            .padding(horizontal = 16.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            modifier = Modifier.size(22.dp),
            tint = if (labelColor != Color.Unspecified) labelColor else MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.width(16.dp))
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
            Spacer(modifier = Modifier.width(4.dp))
        }
        Icon(
            imageVector = Icons.Outlined.ChevronRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(18.dp)
        )
    }
}
