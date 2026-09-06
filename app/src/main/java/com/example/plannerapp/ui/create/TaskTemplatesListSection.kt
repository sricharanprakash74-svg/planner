package com.example.plannerapp.ui.create

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

data class TaskInput(
    var description: String = "",
    var days: Set<Int> = setOf(1, 2, 3, 4, 5, 6, 7) // 1=Mon, 7=Sun
)

@Composable
fun TaskTemplatesListSection(
    tasks: List<TaskInput>,
    onTasksChange: (List<TaskInput>) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = "Tasks",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(16.dp))

        tasks.forEachIndexed { index, task ->
            TaskEntryRow(
                task = task,
                onTaskChange = { updatedTask ->
                    val newList = tasks.toMutableList()
                    newList[index] = updatedTask
                    onTasksChange(newList)
                },
                onRemove = {
                    if (tasks.size > 1) {
                        val newList = tasks.toMutableList()
                        newList.removeAt(index)
                        onTasksChange(newList)
                    }
                }
            )
            Spacer(modifier = Modifier.height(24.dp))
        }

        OutlinedButton(
            onClick = { onTasksChange(tasks + TaskInput()) },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("+ Add Another Task")
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskEntryRow(
    task: TaskInput,
    onTaskChange: (TaskInput) -> Unit,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = task.description,
                onValueChange = { onTaskChange(task.copy(description = it)) },
                modifier = Modifier.weight(1f),
                placeholder = { Text("Task (e.g. Drink Water)") }
            )
            IconButton(onClick = onRemove) {
                Icon(
                    imageVector = Icons.Outlined.Delete,
                    contentDescription = "Remove",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
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
