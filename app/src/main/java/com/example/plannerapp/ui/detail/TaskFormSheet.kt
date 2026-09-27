package com.example.plannerapp.ui.detail

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.plannerapp.ui.components.DurationPickerDialog
import com.example.plannerapp.ui.components.DurationPickerRow
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskFormSheet(
    title: String,
    initialDescription: String,
    initialDurationDays: Int,
    initialSubtasks: List<String>,
    baseDate: LocalDate,
    onDismiss: () -> Unit,
    onSave: (description: String, durationDays: Int, subtasks: List<String>) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var text by remember { mutableStateOf(initialDescription) }
    var durationDays by remember { mutableIntStateOf(initialDurationDays) }
    var subtasks by remember { mutableStateOf(initialSubtasks) }
    var showDurationDialog by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
                .padding(bottom = 32.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(16.dp))
            
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Task description") },
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Duration Selection Row
            DurationPickerRow(
                durationDays = durationDays,
                baseDate = baseDate,
                onClick = { showDurationDialog = true }
            )

            Spacer(modifier = Modifier.height(16.dp))
            Text("Subtasks (Optional)", style = MaterialTheme.typography.titleSmall)
            Spacer(modifier = Modifier.height(8.dp))

            subtasks.forEachIndexed { index, subtask ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(bottom = 8.dp)
                ) {
                    OutlinedTextField(
                        value = subtask,
                        onValueChange = { newSubtask -> 
                            val newSubtasks = subtasks.toMutableList()
                            newSubtasks[index] = newSubtask
                            subtasks = newSubtasks
                        },
                        modifier = Modifier.weight(1f),
                        placeholder = { Text("Subtask ${index + 1}") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )
                    if (subtasks.size > 1) {
                        IconButton(
                            onClick = {
                                val newSubtasks = subtasks.toMutableList()
                                newSubtasks.removeAt(index)
                                subtasks = newSubtasks
                            }
                        ) {
                            Icon(Icons.Filled.Close, contentDescription = "Remove subtask", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }

            TextButton(onClick = { subtasks = subtasks + "" }) {
                Text("+ Add another subtask")
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = onDismiss) {
                    Text("Cancel")
                }
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = { 
                        if (text.isNotBlank()) {
                            onSave(text, durationDays, subtasks.filter { it.isNotBlank() })
                        }
                    },
                    enabled = text.isNotBlank()
                ) {
                    Text("Save")
                }
            }
        }
    }

    if (showDurationDialog) {
        DurationPickerDialog(
            initialDurationDays = durationDays,
            baseDate = baseDate,
            showMakeDefaultOption = false,
            onDismiss = { showDurationDialog = false },
            onSave = { selectedDays, _ ->
                durationDays = selectedDays
                showDurationDialog = false
            }
        )
    }
}
