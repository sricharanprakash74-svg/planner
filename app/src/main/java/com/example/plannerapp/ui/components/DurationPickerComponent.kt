package com.example.plannerapp.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccessTime
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

sealed class DurationOption(val label: String, val days: Int) {
    object OneDay : DurationOption("1 day", 1)
    object TwoDays : DurationOption("2 days", 2)
    object OneWeek : DurationOption("1 week", 7)
    class Custom(val customDays: Int, val customEndDate: LocalDate) : DurationOption("Custom", customDays)
}

@Composable
fun DurationPickerRow(
    durationDays: Int,
    baseDate: LocalDate = LocalDate.now(),
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val targetDate = baseDate.plusDays((durationDays - 1).coerceAtLeast(0).toLong())
    val formatter = DateTimeFormatter.ofPattern("EEE, d MMM")
    val untilText = if (durationDays == 1) {
        "1 day (Until ${targetDate.format(formatter)})"
    } else {
        "$durationDays days (Until ${targetDate.format(formatter)})"
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Outlined.AccessTime,
                contentDescription = "Duration",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Duration",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = untilText,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DurationPickerDialog(
    initialDurationDays: Int,
    baseDate: LocalDate = LocalDate.now(),
    showMakeDefaultOption: Boolean = false,
    initialMakeDefault: Boolean = false,
    onDismiss: () -> Unit,
    onSave: (durationDays: Int, makeDefault: Boolean) -> Unit
) {
    // 0 = 1 day, 1 = 2 days, 2 = 1 week, 3 = Custom
    val selectedOptionIndex = remember(initialDurationDays) {
        mutableIntStateOf(
            when (initialDurationDays) {
                1 -> 0
                2 -> 1
                7 -> 2
                else -> 3
            }
        )
    }

    var customDays by remember { mutableIntStateOf(initialDurationDays.coerceAtLeast(1)) }
    var customEndDate by remember { mutableStateOf(baseDate.plusDays((initialDurationDays - 1).coerceAtLeast(0).toLong())) }
    var showCustomDatePicker by remember { mutableStateOf(false) }
    var makeDefault by remember { mutableStateOf(initialMakeDefault) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Duration",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Option 1: 1 day
                DurationRadioRow(
                    label = "1 day",
                    isSelected = selectedOptionIndex.intValue == 0,
                    onClick = { selectedOptionIndex.intValue = 0 }
                )

                // Option 2: 2 days
                DurationRadioRow(
                    label = "2 days",
                    isSelected = selectedOptionIndex.intValue == 1,
                    onClick = { selectedOptionIndex.intValue = 1 }
                )

                // Option 3: 1 week
                DurationRadioRow(
                    label = "1 week",
                    isSelected = selectedOptionIndex.intValue == 2,
                    onClick = { selectedOptionIndex.intValue = 2 }
                )

                // Option 4: Custom
                val customLabel = if (selectedOptionIndex.intValue == 3) {
                    "Custom (${customDays}d until ${customEndDate.format(DateTimeFormatter.ofPattern("d MMM"))})"
                } else {
                    "Custom"
                }

                DurationRadioRow(
                    label = customLabel,
                    isSelected = selectedOptionIndex.intValue == 3,
                    onClick = {
                        selectedOptionIndex.intValue = 3
                        showCustomDatePicker = true
                    }
                )

                if (showMakeDefaultOption) {
                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { makeDefault = !makeDefault }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = makeDefault,
                            onCheckedChange = { makeDefault = it }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Make this default for every task in this plan",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val finalDays = when (selectedOptionIndex.intValue) {
                        0 -> 1
                        1 -> 2
                        2 -> 7
                        else -> customDays
                    }
                    onSave(finalDays, makeDefault)
                }
            ) {
                Text("Save", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )

    if (showCustomDatePicker) {
        val initialEpoch = customEndDate.atStartOfDay(ZoneId.of("UTC")).toInstant().toEpochMilli()
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = initialEpoch)

        DatePickerDialog(
            onDismissRequest = { showCustomDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            val pickedDate = Instant.ofEpochMilli(millis).atZone(ZoneId.of("UTC")).toLocalDate()
                            val diffDays = (ChronoUnit.DAYS.between(baseDate, pickedDate) + 1).coerceAtLeast(1).toInt()
                            customDays = diffDays
                            customEndDate = pickedDate
                        }
                        showCustomDatePicker = false
                    }
                ) {
                    Text("OK", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showCustomDatePicker = false }) {
                    Text("Cancel")
                }
            }
        ) {
            DatePicker(
                state = datePickerState
            )
        }
    }
}

@Composable
private fun DurationRadioRow(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(
            selected = isSelected,
            onClick = onClick
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
