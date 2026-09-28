package com.example.plannerapp.ui.detail

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.outlined.Checklist
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.EditNote
import androidx.compose.material.icons.outlined.Forum
import androidx.compose.material.icons.outlined.NewReleases
import androidx.compose.material.icons.outlined.Public
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.plannerapp.data.JoinedCommunityEntity
import com.example.plannerapp.data.social.PlanVersionUpdate
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlanDetailTopBar(
    planHeading: String?,
    startDate: String?,
    endDate: String?,
    onBack: () -> Unit,
    onShareStory: () -> Unit,
    onEditPlan: () -> Unit,
    onDeletePlan: () -> Unit,
    onPublishPlan: () -> Unit = {}
) {
    var planMenuExpanded by remember { mutableStateOf(false) }

    TopAppBar(
        title = {
            Column {
                Text(
                    text = planHeading ?: "Plan Details",
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (startDate != null && endDate != null) {
                    Text(
                        text = "$startDate to $endDate",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
        },
        actions = {
            IconButton(onClick = onShareStory) {
                Icon(Icons.Outlined.Share, contentDescription = "Share Story Card")
            }
            Box {
                IconButton(onClick = { planMenuExpanded = true }) {
                    Icon(Icons.Filled.MoreVert, contentDescription = "Plan Options")
                }
                DropdownMenu(
                    expanded = planMenuExpanded,
                    onDismissRequest = { planMenuExpanded = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Publish to Community") },
                        leadingIcon = { Icon(Icons.Outlined.Public, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                        onClick = {
                            planMenuExpanded = false
                            onPublishPlan()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Export Story Card") },
                        leadingIcon = { Icon(Icons.Outlined.Share, contentDescription = null) },
                        onClick = {
                            planMenuExpanded = false
                            onShareStory()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Edit Plan & Duration") },
                        leadingIcon = { Icon(Icons.Outlined.Edit, contentDescription = null) },
                        onClick = {
                            planMenuExpanded = false
                            onEditPlan()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Delete Plan", color = MaterialTheme.colorScheme.error) },
                        leadingIcon = { Icon(Icons.Outlined.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                        onClick = {
                            planMenuExpanded = false
                            onDeletePlan()
                        }
                    )
                }
            }
        }
    )
}

@Composable
fun CommunityPlanBanner(
    community: JoinedCommunityEntity,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
        border = BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
        )
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Outlined.Forum,
                contentDescription = "Community",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Community Plan",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "${community.communityTitle} • by ${community.creatorName}",
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                contentDescription = "View Discussion",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(14.dp)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddOptionsSheet(
    selectedDate: LocalDate,
    isDayLocked: Boolean,
    onDismiss: () -> Unit,
    onAddTask: () -> Unit,
    onOpenJournal: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val haptic = LocalHapticFeedback.current

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 36.dp, top = 8.dp)
        ) {
            Text(
                text = "Add to Plan",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = selectedDate.format(DateTimeFormatter.ofPattern("EEEE, MMMM d, yyyy")),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Option 1: Add New Task
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = if (isDayLocked) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                else MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(enabled = !isDayLocked) {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onAddTask()
                    }
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(16.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Checklist,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(26.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Add New Task",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = if (isDayLocked) "Day is locked (tasks cannot be added)" else "Create a task, habit, or checklist routine",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Option 2: Write Journal / Diary
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onOpenJournal()
                    }
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(16.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.EditNote,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(26.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Write Journal / Diary",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Record thoughts, lessons, reflections, or daily notes",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun PlanVersionUpdateBanner(
    update: PlanVersionUpdate,
    onAdoptTasks: () -> Unit,
    onCloneAsNew: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.7f)
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.4f))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Outlined.NewReleases,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Plan Update Available: v${update.latestVersionTag}",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }

            if (update.changelog.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = update.changelog,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.85f)
                )
            }

            if (update.newTasks.isNotEmpty()) {
                Spacer(modifier = Modifier.height(6.dp))
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.6f)
                ) {
                    Text(
                        text = "+${update.newTasks.size} new curriculum tasks",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = onAdoptTasks,
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                ) {
                    Text("Adopt Tasks", style = MaterialTheme.typography.labelMedium)
                }

                OutlinedButton(
                    onClick = onCloneAsNew,
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                ) {
                    Text("Clone As New", style = MaterialTheme.typography.labelMedium)
                }

                TextButton(
                    onClick = onDismiss,
                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 6.dp)
                ) {
                    Text("Dismiss", style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    }
}

