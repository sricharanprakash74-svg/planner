package com.example.plannerapp.ui.detail

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
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
    var subtasks by remember { mutableStateOf(if (initialSubtasks.isEmpty()) listOf("") else initialSubtasks) }
    var showDurationDialog by remember { mutableStateOf(false) }

    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val subtaskFocusRequesters = remember { mutableMapOf<Int, FocusRequester>() }
    fun getFocusRequester(index: Int): FocusRequester =
        subtaskFocusRequesters.getOrPut(index) { FocusRequester() }

    var requestedFocusIndex by remember { mutableStateOf<Int?>(null) }

    LaunchedEffect(requestedFocusIndex) {
        val target = requestedFocusIndex
        if (target != null) {
            kotlinx.coroutines.delay(60)
            try {
                getFocusRequester(target).requestFocus()
            } catch (_: Exception) {}
            requestedFocusIndex = null
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        modifier = Modifier.imePadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
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
                shape = RoundedCornerShape(12.dp),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                keyboardActions = KeyboardActions(
                    onNext = {
                        if (subtasks.isEmpty()) {
                            subtasks = listOf("")
                            requestedFocusIndex = 0
                        } else {
                            getFocusRequester(0).requestFocus()
                        }
                    }
                )
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
                val isLast = index == subtasks.lastIndex
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
                        modifier = Modifier
                            .weight(1f)
                            .focusRequester(getFocusRequester(index)),
                        placeholder = { Text("Subtask ${index + 1}") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        keyboardOptions = KeyboardOptions(
                            imeAction = if (isLast) ImeAction.Done else ImeAction.Next
                        ),
                        keyboardActions = KeyboardActions(
                            onNext = {
                                if (index + 1 < subtasks.size) {
                                    getFocusRequester(index + 1).requestFocus()
                                }
                            },
                            onDone = {
                                if (subtask.isNotBlank()) {
                                    val nextIndex = subtasks.size
                                    subtasks = subtasks + ""
                                    requestedFocusIndex = nextIndex
                                } else {
                                    focusManager.clearFocus()
                                    keyboardController?.hide()
                                }
                            }
                        )
                    )
                    if (subtasks.size > 1) {
                        IconButton(
                            onClick = {
                                val newSubtasks = subtasks.toMutableList()
                                newSubtasks.removeAt(index)
                                subtasks = newSubtasks
                                subtaskFocusRequesters.remove(index)
                            }
                        ) {
                            Icon(Icons.Filled.Close, contentDescription = "Remove subtask", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }

            TextButton(
                onClick = {
                    val nextIndex = subtasks.size
                    subtasks = subtasks + ""
                    requestedFocusIndex = nextIndex
                }
            ) {
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
