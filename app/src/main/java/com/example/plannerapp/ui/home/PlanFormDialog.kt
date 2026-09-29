package com.example.plannerapp.ui.home

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.plannerapp.data.social.VisibilitySelector
import com.example.plannerapp.data.social.VisibilityTier
import com.example.plannerapp.ui.components.DurationPickerDialog
import com.example.plannerapp.ui.components.DurationPickerRow
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.ui.graphics.graphicsLayer
import com.example.plannerapp.theme.PhysicsSpec
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlanFormDialog(
    title: String,
    initialName: String,
    initialDescription: String = "",
    initialDurationDays: Int,
    initialMakeDefault: Boolean,
    initialReminderEnabled: Boolean = false,
    initialReminderTime: String = "08:00",
    initialVisibility: VisibilityTier = VisibilityTier.PUBLIC,
    confirmText: String,
    onDismiss: () -> Unit,
    onSaveWithVisibility: ((name: String, description: String, durationDays: Int, defaultTaskDuration: Int, reminderEnabled: Boolean, reminderTime: String?, visibility: VisibilityTier) -> Unit)? = null,
    onSave: (name: String, description: String, durationDays: Int, defaultTaskDuration: Int, reminderEnabled: Boolean, reminderTime: String?) -> Unit = { _, _, _, _, _, _ -> }
) {
    var text by remember { mutableStateOf(initialName) }
    var description by remember { mutableStateOf(initialDescription) }
    var durationDays by remember { mutableIntStateOf(initialDurationDays) }
    var makeDefault by remember { mutableStateOf(initialMakeDefault) }
    var reminderEnabled by remember { mutableStateOf(initialReminderEnabled) }
    var reminderTime by remember { mutableStateOf(initialReminderTime) }
    var selectedVisibility by remember { mutableStateOf(initialVisibility) }
    var showDurationDialog by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = { BottomSheetDefaults.DragHandle() },
        modifier = Modifier.imePadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
        ) {
            // Pinned Action Header Toolbar with Physics Press Feedback
            val confirmInteractionSource = remember { MutableInteractionSource() }
            val isConfirmPressed by confirmInteractionSource.collectIsPressedAsState()
            val confirmScale by animateFloatAsState(
                targetValue = if (isConfirmPressed) 0.94f else 1f,
                animationSpec = PhysicsSpec.PressDown,
                label = "dialog_confirm_scale"
            )

            val cancelInteractionSource = remember { MutableInteractionSource() }
            val isCancelPressed by cancelInteractionSource.collectIsPressedAsState()
            val cancelScale by animateFloatAsState(
                targetValue = if (isCancelPressed) 0.94f else 1f,
                animationSpec = PhysicsSpec.PressDown,
                label = "dialog_cancel_scale"
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                TextButton(
                    onClick = onDismiss,
                    interactionSource = cancelInteractionSource,
                    modifier = Modifier.graphicsLayer {
                        scaleX = cancelScale
                        scaleY = cancelScale
                    }
                ) {
                    Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Button(
                    onClick = {
                        if (text.isNotBlank()) {
                            val defaultTaskDur = if (makeDefault) durationDays else 1
                            if (onSaveWithVisibility != null) {
                                onSaveWithVisibility(text.trim(), description.trim(), durationDays, defaultTaskDur, reminderEnabled, if (reminderEnabled) reminderTime else null, selectedVisibility)
                            } else {
                                onSave(text.trim(), description.trim(), durationDays, defaultTaskDur, reminderEnabled, if (reminderEnabled) reminderTime else null)
                            }
                        }
                    },
                    enabled = text.isNotBlank(),
                    interactionSource = confirmInteractionSource,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                        disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f)
                    ),
                    modifier = Modifier.graphicsLayer {
                        scaleX = confirmScale
                        scaleY = confirmScale
                    }
                ) {
                    Text(confirmText, fontWeight = FontWeight.SemiBold)
                }
            }

            HorizontalDivider(
                thickness = 0.5.dp,
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
            )

            // Scrollable Content
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 16.dp)
            ) {
                TextField(
                    value = text,
                    onValueChange = { text = it },
                    label = { Text("Plan Name") },
                    placeholder = { Text("e.g., Morning Routine") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                        focusedIndicatorColor = androidx.compose.ui.graphics.Color.Transparent,
                        unfocusedIndicatorColor = androidx.compose.ui.graphics.Color.Transparent,
                        disabledIndicatorColor = androidx.compose.ui.graphics.Color.Transparent
                    )
                )

                Spacer(modifier = Modifier.height(12.dp))

                TextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description (Optional)") },
                    placeholder = { Text("Add notes or context for this plan") },
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                        focusedIndicatorColor = androidx.compose.ui.graphics.Color.Transparent,
                        unfocusedIndicatorColor = androidx.compose.ui.graphics.Color.Transparent,
                        disabledIndicatorColor = androidx.compose.ui.graphics.Color.Transparent
                    )
                )

                Spacer(modifier = Modifier.height(16.dp))

                DurationPickerRow(
                    durationDays = durationDays,
                    baseDate = LocalDate.now(),
                    onClick = { showDurationDialog = true }
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Reminder Settings Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                    )
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
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
                                    tint = if (reminderEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = "Daily Reminder",
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        text = if (reminderEnabled) "Notify daily at ${formatReminderTime(reminderTime)}" else "Reminders off",
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
                            Spacer(modifier = Modifier.height(12.dp))
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                val presets = listOf("08:00" to "8:00 AM", "12:00" to "12:00 PM", "20:00" to "8:00 PM")
                                presets.forEach { (timeVal, label) ->
                                    FilterChip(
                                        selected = reminderTime == timeVal,
                                        onClick = { reminderTime = timeVal },
                                        label = { Text(label, fontSize = MaterialTheme.typography.labelSmall.fontSize) }
                                    )
                                }
                                val isCustom = presets.none { it.first == reminderTime }
                                FilterChip(
                                    selected = isCustom,
                                    onClick = { showTimePicker = true },
                                    label = {
                                        Text(if (isCustom) formatReminderTime(reminderTime) else "Custom", fontSize = MaterialTheme.typography.labelSmall.fontSize)
                                    }
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Content Visibility & Privacy Selector
                VisibilitySelector(
                    selectedTier = selectedVisibility,
                    onTierSelected = { selectedVisibility = it }
                )

                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }

    if (showDurationDialog) {
        DurationPickerDialog(
            initialDurationDays = durationDays,
            baseDate = LocalDate.now(),
            showMakeDefaultOption = true,
            initialMakeDefault = makeDefault,
            onDismiss = { showDurationDialog = false },
            onSave = { selectedDays, isDefault ->
                durationDays = selectedDays
                makeDefault = isDefault
                showDurationDialog = false
            }
        )
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

fun formatReminderTime(timeStr: String?): String {
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
