package com.example.plannerapp.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
    onNotificationsClick: () -> Unit,
    onTimezoneClick: () -> Unit,
    onPublishPlanClick: () -> Unit,
    onBackupClick: () -> Unit,
    onExportClick: () -> Unit,
    onDeleteAccountClick: () -> Unit,
    onViewCreatorProfileClick: (String) -> Unit = {},
    onSignInClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var darkMode by remember { mutableStateOf(false) }
    val user by viewModel.currentUser.collectAsState()
    val isGuest = user?.cloudUserId == null

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back to Profile")
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
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            // Account section
            SettingsSectionHeader("Account")
            if (isGuest) {
                SettingsItem(
                    icon = Icons.Outlined.Person, 
                    label = "Sign In or Create Account", 
                    subtitle = "Sync your plans across devices",
                    labelColor = MaterialTheme.colorScheme.primary,
                    onClick = onSignInClick
                )
            } else {
                SettingsItem(
                    icon = Icons.Outlined.Person, 
                    label = "Edit Profile", 
                    subtitle = user?.email, 
                    onClick = onEditProfileClick
                )
            }
            SettingsItem(
                icon = Icons.Outlined.Notifications, 
                label = "Notifications", 
                subtitle = "Reminders & Alerts",
                onClick = onNotificationsClick
            )
            SettingsToggleItem(
                icon = Icons.Outlined.DarkMode,
                label = "Dark Mode",
                checked = darkMode,
                onCheckedChange = { darkMode = it }
            )
            SettingsItem(
                icon = Icons.Outlined.Language, 
                label = "Timezone", 
                subtitle = user?.userTimezone ?: "System Default", 
                onClick = onTimezoneClick
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Creator section
            SettingsSectionHeader("Creator")
            SettingsToggleItem(
                icon = Icons.Outlined.Star,
                label = "Creator Mode",
                checked = user?.isCreator == true,
                onCheckedChange = { viewModel.setCreatorStatus(it) }
            )
            if (user?.isCreator == true) {
                val creatorUserId = user?.cloudUserId ?: user?.userId?.toString() ?: "1"
                SettingsItem(
                    icon = Icons.Outlined.AccountCircle,
                    label = "View Public Creator Profile",
                    subtitle = "See how your profile looks to the community",
                    onClick = { onViewCreatorProfileClick(creatorUserId) }
                )
                SettingsItem(
                    icon = Icons.Outlined.Edit, 
                    label = "Publish a Plan", 
                    subtitle = "Share with the community",
                    onClick = onPublishPlanClick
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Data section
            SettingsSectionHeader("Data & Cloud")
            SettingsItem(
                icon = Icons.Outlined.Backup, 
                label = "Backup & Sync", 
                subtitle = "Manage cloud backups",
                onClick = onBackupClick
            )
            SettingsItem(
                icon = Icons.Outlined.FileDownload, 
                label = "Export Data", 
                subtitle = "Download JSON, CSV, or Markdown",
                onClick = onExportClick
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Danger zone
            SettingsSectionHeader("Danger Zone")
            SettingsItem(
                icon = Icons.Outlined.Delete,
                label = "Delete Account",
                subtitle = "Wipe all local and cloud data",
                labelColor = MaterialTheme.colorScheme.error,
                onClick = onDeleteAccountClick
            )

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "PlannerApp v1.0.0",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 16.dp)
            )
        }
    }
}

@Composable
private fun SettingsSectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(vertical = 8.dp)
    )
    HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
}

@Composable
private fun SettingsItem(
    icon: ImageVector,
    label: String,
    subtitle: String? = null,
    labelColor: Color = MaterialTheme.colorScheme.onSurface,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            modifier = Modifier.size(22.dp),
            tint = labelColor
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyLarge,
                color = labelColor
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Text(
            text = "›",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun SettingsToggleItem(
    icon: ImageVector,
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            modifier = Modifier.size(22.dp),
            tint = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f)
        )
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}
