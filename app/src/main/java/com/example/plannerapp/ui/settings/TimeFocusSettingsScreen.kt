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
fun TimeFocusSettingsScreen(
    viewModel: SettingsViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val saved = remember { viewModel.loadTimeFocusPrefs(context) }
    val focusDurationOptions = listOf("25 min", "45 min", "60 min", "90 min")

    var focusModeEnabled by remember { mutableStateOf(saved["focus_mode"] as Boolean) }
    var pomodoroEnabled by remember { mutableStateOf(saved["pomodoro"] as Boolean) }
    var selectedFocusDuration by remember { mutableIntStateOf(saved["duration_index"] as Int) }
    var dailyPlanningReminder by remember { mutableStateOf(saved["daily_planning"] as Boolean) }

    fun persist() { viewModel.saveTimeFocusPrefs(context, focusModeEnabled, pomodoroEnabled, selectedFocusDuration, dailyPlanningReminder) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Time & Focus", fontWeight = FontWeight.Bold) },
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
            TimeFocusSectionHeader("Focus Mode")
            TimeFocusToggleRow("Enable Focus Mode", "Suppress distracting notifications during active tasks", focusModeEnabled) { focusModeEnabled = it; persist() }
            Spacer(modifier = Modifier.height(16.dp))
            TimeFocusSectionHeader("Pomodoro Timer")
            TimeFocusToggleRow("Pomodoro Sessions", "Break work into focused intervals with short breaks", pomodoroEnabled) { pomodoroEnabled = it; persist() }
            if (pomodoroEnabled) {
                Spacer(modifier = Modifier.height(12.dp))
                Text("Focus session duration", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                Spacer(modifier = Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    focusDurationOptions.forEachIndexed { index, option ->
                        FilterChip(
                            selected = selectedFocusDuration == index,
                            onClick = { selectedFocusDuration = index; persist() },
                            label = { Text(option) }
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            TimeFocusSectionHeader("Daily Planning")
            TimeFocusToggleRow("Daily Planning Reminder", "Prompt you each morning to review and plan your day", dailyPlanningReminder) { dailyPlanningReminder = it; persist() }
            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Spacer(modifier = Modifier.height(12.dp))
            Text("Advanced time management features coming in a future update.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun TimeFocusSectionHeader(title: String) {
    Text(text = title, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(vertical = 8.dp))
}

@Composable
private fun TimeFocusToggleRow(title: String, subtitle: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
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
