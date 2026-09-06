package com.example.plannerapp.ui.create

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.plannerapp.ui.create.CreatePlanViewModel
import java.time.LocalDate
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

data class TaskInput(var description: String = "", var days: Set<Int> = setOf(1,2,3,4,5,6,7)) // 1=Mon, 7=Sun

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreatePlanScreen(
    viewModel: CreatePlanViewModel,
    onClose: () -> Unit,
    onPlanCreated: () -> Unit,
    modifier: Modifier = Modifier
) {
    var planName by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var tasks by remember { mutableStateOf(listOf(TaskInput())) }
    
    var showStartDatePicker by remember { mutableStateOf(false) }
    var showEndDatePicker by remember { mutableStateOf(false) }
    var startDate by remember { mutableStateOf(LocalDate.now()) }
    var endDate by remember { mutableStateOf(LocalDate.now().plusDays(30)) }

    var reminderEnabled by remember { mutableStateOf(false) }
    var reminderTime by remember { mutableStateOf("08:00") }
    var showTimePicker by remember { mutableStateOf(false) }
    
    val dateFormatter = remember { DateTimeFormatter.ofPattern("MMM dd, yyyy") }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("Create New Plan", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onClose) {
                        Icon(Icons.Filled.Close, contentDescription = "Close")
                    }
                },
                actions = {
                    TextButton(
                        onClick = { 
                            val validTasks = tasks.filter { it.description.isNotBlank() && it.days.isNotEmpty() }
                            viewModel.createNewPlan(
                                name = planName,
                                description = description,
                                startDate = startDate,
                                endDate = endDate,
                                tasksInput = validTasks.map { it.description to it.days },
                                reminderEnabled = reminderEnabled,
                                reminderTime = if (reminderEnabled) reminderTime else null
                            )
                            onPlanCreated()
                            onClose()
                        },
                        enabled = planName.isNotBlank() && tasks.any { it.description.isNotBlank() } && !startDate.isAfter(endDate)
                    ) {
                        Text("Done", fontWeight = FontWeight.Bold)
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            Text("Plan Name", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = planName,
                onValueChange = { planName = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("e.g. Morning Routine") }
            )

            Spacer(modifier = Modifier.height(24.dp))

            Text("Description", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Start the day right with healthy habits.") },
                minLines = 3
            )

            Spacer(modifier = Modifier.height(24.dp))
            
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Start Date", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedButton(onClick = { showStartDatePicker = true }, modifier = Modifier.fillMaxWidth()) {
                        Text(startDate.format(dateFormatter))
                    }
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text("End Date", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedButton(onClick = { showEndDatePicker = true }, modifier = Modifier.fillMaxWidth()) {
                        Text(endDate.format(dateFormatter))
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Reminder section
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = if (reminderEnabled) Icons.Outlined.NotificationsActive else Icons.Outlined.Notifications,
                                contentDescription = "Daily Reminder",
                                tint = if (reminderEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Daily Reminder",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = if (reminderEnabled) "Notify daily at ${formatReminderTime(reminderTime)}" else "Reminders disabled",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Switch(
                            checked = reminderEnabled,
                            onCheckedChange = { reminderEnabled = it }
                        )
                    }

                    if (reminderEnabled) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Reminder Time",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            val presets = listOf("08:00" to "8:00 AM", "12:00" to "12:00 PM", "20:00" to "8:00 PM")
                            presets.forEach { (timeVal, label) ->
                                FilterChip(
                                    selected = reminderTime == timeVal,
                                    onClick = { reminderTime = timeVal },
                                    label = { Text(label) }
                                )
                            }
                            val isCustom = presets.none { it.first == reminderTime }
                            FilterChip(
                                selected = isCustom,
                                onClick = { showTimePicker = true },
                                label = {
                                    Text(if (isCustom) formatReminderTime(reminderTime) else "Custom")
                                },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Outlined.Schedule,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(24.dp))

            Text("Tasks", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(16.dp))

            tasks.forEachIndexed { index, task ->
                TaskEntryRow(
                    task = task,
                    onTaskChange = { updatedTask ->
                        val newList = tasks.toMutableList()
                        newList[index] = updatedTask
                        tasks = newList
                    },
                    onRemove = {
                        if (tasks.size > 1) {
                            val newList = tasks.toMutableList()
                            newList.removeAt(index)
                            tasks = newList
                        }
                    }
                )
                Spacer(modifier = Modifier.height(24.dp))
            }

            OutlinedButton(
                onClick = { tasks = tasks + TaskInput() },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("+ Add Another Task")
            }
            
            Spacer(modifier = Modifier.height(32.dp))
        }
    }

    if (showStartDatePicker) {
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = startDate.atStartOfDay(ZoneId.of("UTC")).toInstant().toEpochMilli())
        DatePickerDialog(
            onDismissRequest = { showStartDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let {
                        startDate = Instant.ofEpochMilli(it).atZone(ZoneId.of("UTC")).toLocalDate()
                    }
                    showStartDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showStartDatePicker = false }) { Text("Cancel") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    if (showEndDatePicker) {
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = endDate.atStartOfDay(ZoneId.of("UTC")).toInstant().toEpochMilli())
        DatePickerDialog(
            onDismissRequest = { showEndDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let {
                        endDate = Instant.ofEpochMilli(it).atZone(ZoneId.of("UTC")).toLocalDate()
                    }
                    showEndDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showEndDatePicker = false }) { Text("Cancel") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    if (showTimePicker) {
        val initialHour = reminderTime.split(":").getOrNull(0)?.toIntOrNull() ?: 8
        val initialMinute = reminderTime.split(":").getOrNull(1)?.toIntOrNull() ?: 0
        val timePickerState = rememberTimePickerState(
            initialHour = initialHour,
            initialMinute = initialMinute,
            is24Hour = false
        )
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            title = { Text("Select Reminder Time", fontWeight = FontWeight.Bold) },
            confirmButton = {
                TextButton(onClick = {
                    reminderTime = String.format(java.util.Locale.US, "%02d:%02d", timePickerState.hour, timePickerState.minute)
                    showTimePicker = false
                }) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showTimePicker = false }) {
                    Text("Cancel")
                }
            },
            text = {
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    TimePicker(state = timePickerState)
                }
            }
        )
    }
}

private fun formatReminderTime(timeStr: String?): String {
    if (timeStr.isNullOrBlank()) return "Off"
    val parts = timeStr.split(":")
    if (parts.size != 2) return timeStr
    val hour = parts[0].toIntOrNull() ?: return timeStr
    val minute = parts[1].toIntOrNull() ?: return timeStr
    val period = if (hour >= 12) "PM" else "AM"
    val displayHour = when {
        hour == 0 -> 12
        hour > 12 -> hour - 12
        else -> hour
    }
    return String.format(java.util.Locale.US, "%d:%02d %s", displayHour, minute, period)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskEntryRow(
    task: TaskInput,
    onTaskChange: (TaskInput) -> Unit,
    onRemove: () -> Unit
) {
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = task.description,
                onValueChange = { onTaskChange(task.copy(description = it)) },
                modifier = Modifier.weight(1f),
                placeholder = { Text("Task (e.g. Drink Water)") }
            )
            IconButton(onClick = onRemove) {
                Icon(Icons.Outlined.Delete, contentDescription = "Remove", tint = MaterialTheme.colorScheme.error)
            }
        }
        
        Spacer(modifier = Modifier.height(12.dp))
        
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.fillMaxWidth()) {
            val days = listOf(1 to "M", 2 to "T", 3 to "W", 4 to "T", 5 to "F", 6 to "S", 7 to "S")
            days.forEach { (dayInt, label) ->
                val isSelected = task.days.contains(dayInt)
                FilterChip(
                    selected = isSelected,
                    onClick = {
                        val newDays = if (isSelected) task.days - dayInt else task.days + dayInt
                        onTaskChange(task.copy(days = newDays))
                    },
                    label = { Text(label) }
                )
            }
        }
    }
}
