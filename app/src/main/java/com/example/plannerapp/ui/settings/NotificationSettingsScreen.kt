package com.example.plannerapp.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationSettingsScreen(
    viewModel: SettingsViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val saved = remember { viewModel.loadNotifPrefs(context) }

    var allowNotifications by remember { mutableStateOf(saved["allow"] as Boolean) }
    var morningReminder by remember { mutableStateOf(saved["morning"] as Boolean) }
    var eveningReminder by remember { mutableStateOf(saved["evening"] as Boolean) }
    var streakAlerts by remember { mutableStateOf(saved["streak"] as Boolean) }
    var communityAlerts by remember { mutableStateOf(saved["community"] as Boolean) }
    var vibrate by remember { mutableStateOf(saved["vibrate"] as Boolean) }
    var sound by remember { mutableStateOf(saved["sound"] as Boolean) }

    fun persist() {
        viewModel.saveNotifPrefs(context, allowNotifications, morningReminder, eveningReminder, streakAlerts, communityAlerts, vibrate, sound)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Notifications", fontWeight = FontWeight.Bold) },
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
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Allow Notifications", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Text("Receive alerts and daily planning reminders", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Switch(checked = allowNotifications, onCheckedChange = { allowNotifications = it; persist() })
            }
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            if (allowNotifications) {
                Text("Daily Reminders", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold, modifier = Modifier.padding(vertical = 8.dp))
                NotificationToggleRow("Morning Kickoff (08:00 AM)", "Get an overview of today's planned tasks", morningReminder) { morningReminder = it; persist() }
                NotificationToggleRow("Evening Reflection (09:00 PM)", "Review what you accomplished today", eveningReminder) { eveningReminder = it; persist() }
                NotificationToggleRow("Streak Saver Warning", "Get notified if you're about to lose your daily streak", streakAlerts) { streakAlerts = it; persist() }
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                Text("Social & Community", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold, modifier = Modifier.padding(vertical = 8.dp))
                NotificationToggleRow("Community Activity", "Upvotes, comments, and forks on your published plans", communityAlerts) { communityAlerts = it; persist() }
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                Text("Preferences", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold, modifier = Modifier.padding(vertical = 8.dp))
                NotificationToggleRow("Sound", "Play notification sound", sound) { sound = it; persist() }
                NotificationToggleRow("Vibration", "Vibrate on notification arrival", vibrate) { vibrate = it; persist() }
            }
        }
    }
}

@Composable
private fun NotificationToggleRow(title: String, subtitle: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
            Text(text = subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(modifier = Modifier.width(16.dp))
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
