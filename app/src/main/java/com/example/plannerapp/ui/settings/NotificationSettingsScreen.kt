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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationSettingsScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var allowNotifications by remember { mutableStateOf(true) }
    var morningReminder by remember { mutableStateOf(true) }
    var eveningReminder by remember { mutableStateOf(true) }
    var streakAlerts by remember { mutableStateOf(true) }
    var communityAlerts by remember { mutableStateOf(false) }
    var vibrate by remember { mutableStateOf(true) }
    var sound by remember { mutableStateOf(true) }

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

            // Master Toggle
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Allow Notifications",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "Receive alerts and daily planning reminders",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = allowNotifications,
                    onCheckedChange = { allowNotifications = it }
                )
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            if (allowNotifications) {
                Text(
                    text = "Daily Reminders",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(vertical = 8.dp)
                )

                NotificationToggleRow(
                    title = "Morning Kickoff (08:00 AM)",
                    subtitle = "Get an overview of today's planned tasks",
                    checked = morningReminder,
                    onCheckedChange = { morningReminder = it }
                )

                NotificationToggleRow(
                    title = "Evening Reflection (09:00 PM)",
                    subtitle = "Review what you accomplished today",
                    checked = eveningReminder,
                    onCheckedChange = { eveningReminder = it }
                )

                NotificationToggleRow(
                    title = "Streak Saver Warning",
                    subtitle = "Get notified if you're about to lose your daily streak",
                    checked = streakAlerts,
                    onCheckedChange = { streakAlerts = it }
                )

                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                Text(
                    text = "Social & Community",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(vertical = 8.dp)
                )

                NotificationToggleRow(
                    title = "Community Activity",
                    subtitle = "Upvotes, comments, and forks on your published plans",
                    checked = communityAlerts,
                    onCheckedChange = { communityAlerts = it }
                )

                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                Text(
                    text = "Preferences",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(vertical = 8.dp)
                )

                NotificationToggleRow(
                    title = "Sound",
                    subtitle = "Play notification sound",
                    checked = sound,
                    onCheckedChange = { sound = it }
                )

                NotificationToggleRow(
                    title = "Vibration",
                    subtitle = "Vibrate on notification arrival",
                    checked = vibrate,
                    onCheckedChange = { vibrate = it }
                )
            }
        }
    }
}

@Composable
private fun NotificationToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(modifier = Modifier.width(16.dp))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}
