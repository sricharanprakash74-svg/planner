package com.example.plannerapp.ui.detail

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.example.plannerapp.data.DailyTaskView
import com.example.plannerapp.smartlink.ui.SmartLinkCard
import com.example.plannerapp.ui.components.SubtaskBranchConnector
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Composable
fun PlanDetailTasksHeader(
    selectedDate: LocalDate,
    isDayLocked: Boolean,
    isFutureDate: Boolean,
    completedTasksCount: Int,
    totalTasksCount: Int,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "Tasks for ${selectedDate.format(DateTimeFormatter.ofPattern("MMM d, yyyy"))}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            if (isDayLocked) {
                Spacer(modifier = Modifier.width(6.dp))
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFF10B981).copy(alpha = 0.15f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Lock,
                            contentDescription = "Locked",
                            tint = Color(0xFF10B981),
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = "Completed & Locked",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF10B981),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            } else if (isFutureDate) {
                Spacer(modifier = Modifier.width(6.dp))
                Icon(
                    imageVector = Icons.Filled.Lock,
                    contentDescription = "Future Date Locked",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
        Text(
            text = "$completedTasksCount/$totalTasksCount Done",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
fun PlanDetailEmptyTasksView(
    isDayLocked: Boolean,
    onAddTask: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "No tasks scheduled for this day!",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (!isDayLocked) {
                Spacer(modifier = Modifier.height(8.dp))
                TextButton(onClick = onAddTask) {
                    Text("+ Add a task for this day")
                }
            }
        }
    }
}

@Composable
fun PlanDetailDayCompletionCard(
    isDayLocked: Boolean,
    isFutureDate: Boolean,
    totalTasksCount: Int,
    allTasksCompleted: Boolean,
    completedTasksCount: Int,
    onMarkDayCompleted: () -> Unit,
    onFinishEarly: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.padding(horizontal = 16.dp)) {
        if (isDayLocked) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color(0xFF10B981).copy(alpha = 0.12f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Filled.CheckCircle,
                        contentDescription = null,
                        tint = Color(0xFF10B981),
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Day Finalized & Recorded",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF10B981)
                        )
                        Text(
                            text = "Tasks for this day are locked and contributed to your analytics.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else if (!isFutureDate && totalTasksCount > 0) {
            if (allTasksCompleted) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "All Tasks Completed for Today!",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Lock in your 100% score to seal this day in your streak & analytics.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = onMarkDayCompleted,
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Filled.Lock, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Mark Day as Completed")
                        }
                    }
                }
            } else {
                OutlinedButton(
                    onClick = onFinishEarly,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Outlined.Lock, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Finish Day Early (Lock Day)")
                }
            }
        }
    }
}

@Composable
fun TaskItemWithSubtasks(
    task: DailyTaskView,
    isFuture: Boolean,
    isLocked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    onSubtaskCheckedChange: (Int, Boolean) -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    val gson = Gson()
    val stringListType = object : TypeToken<List<String>>() {}.type
    val boolListType = object : TypeToken<List<Boolean>>() {}.type

    val subtasks: List<String> = try {
        gson.fromJson(task.subtasks, stringListType) ?: emptyList()
    } catch (e: Exception) { emptyList() }

    val completedSubtasks: List<Boolean> = try {
        gson.fromJson(task.completedSubtasks, boolListType) ?: emptyList()
    } catch (e: Exception) { emptyList() }

    val completedSubtaskCount = completedSubtasks.count { it }
    var menuExpanded by remember { mutableStateOf(false) }

    val targetContainerColor = if (task.isCompleted) {
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
    } else {
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
    }

    val animatedContainerColor by animateColorAsState(
        targetValue = targetContainerColor,
        label = "taskCardBg"
    )

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp)),
        color = animatedContainerColor
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(enabled = !isLocked) { 
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onCheckedChange(!task.isCompleted) 
                    }
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = task.isCompleted,
                    onCheckedChange = { 
                        if (!isLocked) {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onCheckedChange(it)
                        }
                    },
                    enabled = !isLocked
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = task.taskDescription,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = if (task.isCompleted) FontWeight.Normal else FontWeight.SemiBold,
                            color = if (task.isCompleted) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f) else MaterialTheme.colorScheme.onSurface,
                            textDecoration = if (task.isCompleted) TextDecoration.LineThrough else TextDecoration.None
                        )
                        if (isLocked) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Icon(
                                imageVector = Icons.Filled.Lock,
                                contentDescription = "Locked",
                                tint = Color(0xFF10B981),
                                modifier = Modifier.size(13.dp)
                            )
                        } else if (isFuture) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Icon(
                                imageVector = Icons.Filled.Lock,
                                contentDescription = "Future task locked",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(top = 2.dp)
                    ) {
                        if (task.durationDays > 1) {
                            Text(
                                text = "${task.durationDays}d duration",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        if (subtasks.isNotEmpty()) {
                            Text(
                                text = "$completedSubtaskCount/${subtasks.size} subtasks",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (completedSubtaskCount == subtasks.size && subtasks.isNotEmpty()) Color(0xFF10B981) else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                if (!isLocked) {
                    Box {
                        IconButton(
                            onClick = { menuExpanded = true },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.MoreVert,
                                contentDescription = "Task Menu",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        DropdownMenu(
                            expanded = menuExpanded,
                            onDismissRequest = { menuExpanded = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Edit Task") },
                                leadingIcon = { Icon(Icons.Outlined.Edit, contentDescription = null) },
                                onClick = {
                                    menuExpanded = false
                                    onEdit()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Delete Task", color = MaterialTheme.colorScheme.error) },
                                leadingIcon = { Icon(Icons.Outlined.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                                onClick = {
                                    menuExpanded = false
                                    onDelete()
                                }
                            )
                        }
                    }
                }
            }

            val detectedUrl = remember(task.taskDescription) {
                "https?://[\\w\\-\\.\\?%&=#+/]+".toRegex().find(task.taskDescription)?.value
            }
            if (detectedUrl != null) {
                Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
                    SmartLinkCard(url = detectedUrl, modifier = Modifier.fillMaxWidth())
                }
            }

            if (subtasks.isNotEmpty()) {
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
                Column(modifier = Modifier.padding(start = 24.dp, end = 16.dp, top = 8.dp, bottom = 12.dp)) {
                    subtasks.forEachIndexed { index, subtaskTitle ->
                        val isSubChecked = completedSubtasks.getOrElse(index) { false }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .clickable(enabled = !isLocked) { 
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    onSubtaskCheckedChange(index, !isSubChecked) 
                                }
                        ) {
                            SubtaskBranchConnector(
                                isLastChild = index == subtasks.lastIndex,
                                isCompleted = isSubChecked,
                                modifier = Modifier
                                    .width(18.dp)
                                    .height(24.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Checkbox(
                                checked = isSubChecked,
                                onCheckedChange = { 
                                    if (!isLocked) {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        onSubtaskCheckedChange(index, it)
                                    }
                                },
                                enabled = !isLocked,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = subtaskTitle,
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (isSubChecked) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f) else MaterialTheme.colorScheme.onSurface,
                                textDecoration = if (isSubChecked) TextDecoration.LineThrough else TextDecoration.None
                            )
                        }
                    }
                }
            }
        }
    }
}
