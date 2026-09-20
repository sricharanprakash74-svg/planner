package com.example.plannerapp.ui.create

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import com.example.plannerapp.smartlink.SmartLinkParser
import com.example.plannerapp.smartlink.ui.SmartLinkCard
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
            com.example.plannerapp.ui.components.StaggeredEntrance(index = index) {
                Column {
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
            }
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

        val detectedTaskUrl = remember(task.description) {
            SmartLinkParser.findFirstUrl(task.description)
        }
        if (detectedTaskUrl != null) {
            Spacer(modifier = Modifier.height(8.dp))
            SmartLinkCard(
                url = detectedTaskUrl,
                modifier = Modifier.fillMaxWidth()
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "Active Days",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            val days = listOf(1 to "M", 2 to "T", 3 to "W", 4 to "T", 5 to "F", 6 to "S", 7 to "S")
            days.forEach { (dayInt, label) ->
                val isSelected = task.days.contains(dayInt)
                
                val bgColor by androidx.compose.animation.animateColorAsState(
                    targetValue = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    label = "dayBgColor"
                )
                
                val contentColor by androidx.compose.animation.animateColorAsState(
                    targetValue = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                    label = "dayContentColor"
                )

                Surface(
                    onClick = {
                        val newDays = if (isSelected) task.days - dayInt else task.days + dayInt
                        onTaskChange(task.copy(days = newDays))
                    },
                    shape = RoundedCornerShape(8.dp),
                    color = bgColor,
                    border = if (isSelected) null else BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
                    modifier = Modifier.size(36.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = contentColor
                        )
                    }
                }
            }
        }
    }
}
