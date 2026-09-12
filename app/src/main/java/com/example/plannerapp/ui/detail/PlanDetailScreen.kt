package com.example.plannerapp.ui.detail

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Today
import androidx.compose.material.icons.outlined.AccessTime
import androidx.compose.material.icons.outlined.Book
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Checklist
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.EditNote
import androidx.compose.material.icons.outlined.Forum
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.*
import com.example.plannerapp.sharing.SharePlanStoryDialog
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.plannerapp.data.DailyTaskView
import com.example.plannerapp.ui.components.DurationPickerDialog
import com.example.plannerapp.ui.components.DurationPickerRow
import com.example.plannerapp.ui.components.TaskThreadBranch
import com.example.plannerapp.ui.home.PlanFormDialog
import com.example.plannerapp.ui.state.Resource
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlanDetailScreen(
    viewModel: PlanDetailViewModel,
    autoOpenAddTask: Boolean = false,
    onBack: () -> Unit,
    onOpenCommunityDiscussion: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val uiStateResource by viewModel.uiState.collectAsState()
    val plan by viewModel.currentPlan.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current

    var showAddOptionsSheet by remember { mutableStateOf(false) }
    var showJournalSheet by remember { mutableStateOf(false) }
    var showAddTaskSheet by remember { mutableStateOf(autoOpenAddTask) }
    var taskToEdit by remember { mutableStateOf<DailyTaskView?>(null) }
    var taskToDelete by remember { mutableStateOf<DailyTaskView?>(null) }

    var showEditPlanDialog by remember { mutableStateOf(false) }
    var showDeletePlanDialog by remember { mutableStateOf(false) }
    var planMenuExpanded by remember { mutableStateOf(false) }
    var showShareStoryDialog by remember { mutableStateOf(false) }

    var showFullCalendarDialog by remember { mutableStateOf(false) }
    var showEarlyFinishConfirmDialog by remember { mutableStateOf(false) }
    var futureAttemptCount by remember { mutableIntStateOf(0) }
    var showFutureBlockedDialog by remember { mutableStateOf(false) }

    val uiState = when (val state = uiStateResource) {
        is Resource.Loading -> {
            Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return
        }
        is Resource.Error -> {
            Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Error: ${state.message}", color = MaterialTheme.colorScheme.error)
            }
            return
        }
        is Resource.Success -> state.data
    }

    val isFutureDate = uiState.selectedDate.isAfter(LocalDate.now())
    val isDayLocked = uiState.isDayLocked
    val totalTasksCount = uiState.tasks.size
    val completedTasksCount = uiState.tasks.count { it.isCompleted }
    val allTasksCompleted = totalTasksCount > 0 && completedTasksCount == totalTasksCount

    val handleCheckinAction: (() -> Unit) -> Unit = { action ->
        if (isDayLocked) {
            coroutineScope.launch {
                snackbarHostState.showSnackbar(
                    message = "This day is marked as completed and locked.",
                    duration = SnackbarDuration.Short
                )
            }
        } else if (isFutureDate) {
            futureAttemptCount++
            if (futureAttemptCount >= 2) {
                showFutureBlockedDialog = true
            } else {
                coroutineScope.launch {
                    snackbarHostState.showSnackbar(
                        message = "Future tasks cannot be checked in yet.",
                        duration = SnackbarDuration.Short
                    )
                }
            }
        } else {
            futureAttemptCount = 0
            action()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = plan?.heading ?: "Plan Details",
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (plan != null) {
                            Text(
                                text = "${plan?.startDate} to ${plan?.endDate}",
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
                    IconButton(onClick = { showShareStoryDialog = true }) {
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
                                text = { Text("Export Story Card") },
                                leadingIcon = { Icon(Icons.Outlined.Share, contentDescription = null) },
                                onClick = {
                                    planMenuExpanded = false
                                    showShareStoryDialog = true
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Edit Plan & Duration") },
                                leadingIcon = { Icon(Icons.Outlined.Edit, contentDescription = null) },
                                onClick = {
                                    planMenuExpanded = false
                                    showEditPlanDialog = true
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Delete Plan", color = MaterialTheme.colorScheme.error) },
                                leadingIcon = { Icon(Icons.Outlined.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                                onClick = {
                                    planMenuExpanded = false
                                    showDeletePlanDialog = true
                                }
                            )
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    showAddOptionsSheet = true
                }
            ) {
                Icon(
                    imageVector = Icons.Filled.Add,
                    contentDescription = "Add to Plan"
                )
            }
        },
        containerColor = MaterialTheme.colorScheme.background,
        modifier = modifier
    ) { paddingValues ->
        // Unified scrollable LazyColumn: scrolling down naturally closes/scrolls off top calendar
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(bottom = 80.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Community Discussion Banner if this plan was joined from the community feed
            uiState.joinedCommunity?.let { community ->
                item {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                            .clickable { onOpenCommunityDiscussion(community.postId) },
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                        border = androidx.compose.foundation.BorderStroke(
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
            }

            // 1. Plan Progress Header (with Calendar trigger on days left)
            item {
                PlanProgressHeader(
                    currentDay = uiState.currentDayNumber,
                    totalDays = uiState.totalDays,
                    progressPercent = uiState.overallProgressPercent,
                    onCalendarClick = { showFullCalendarDialog = true }
                )
            }

            // 2. Plan Timeline Header with "Jump to Today" Button
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Plan Timeline",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    if (uiState.selectedDate != LocalDate.now()) {
                        OutlinedButton(
                            onClick = { viewModel.selectDate(LocalDate.now()) },
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.height(32.dp)
                        ) {
                            Icon(Icons.Filled.Today, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Jump to Today", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }

            // 3. Plan-Specific Timeline Calendar Strip
            item {
                PlanTimelineCalendar(
                    days = uiState.planDays,
                    onDateSelected = { 
                        viewModel.selectDate(it)
                        futureAttemptCount = 0
                    }
                )
            }

            // 4. Section Divider
            item {
                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
                )
            }

            // 5. Tasks Section Title & Status
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Tasks for ${uiState.selectedDate.format(DateTimeFormatter.ofPattern("MMM d, yyyy"))}",
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

            // 6. Tasks List or Empty State
            if (uiState.tasks.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
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
                                TextButton(onClick = { showAddTaskSheet = true }) {
                                    Text("+ Add a task for this day")
                                }
                            }
                        }
                    }
                }
            } else {
                items(uiState.tasks, key = { it.checkinId }) { task ->
                    Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                        TaskItemWithSubtasks(
                            task = task,
                            isFuture = isFutureDate,
                            isLocked = isDayLocked,
                            onCheckedChange = { isChecked -> 
                                handleCheckinAction {
                                    viewModel.onTaskChecked(task.checkinId, isChecked)
                                }
                            },
                            onSubtaskCheckedChange = { index, isChecked -> 
                                handleCheckinAction {
                                    viewModel.onSubtaskChecked(task, index, isChecked)
                                }
                            },
                            onEdit = { if (!isDayLocked) taskToEdit = task },
                            onDelete = { if (!isDayLocked) taskToDelete = task }
                        )
                    }
                }
            }

            // 7. Day Completion Prompts
            item {
                Box(modifier = Modifier.padding(horizontal = 16.dp)) {
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
                                        onClick = { viewModel.markDayCompleted(completedTasksCount, totalTasksCount) },
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
                                onClick = { showEarlyFinishConfirmDialog = true },
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

            // 8. Daily Journal & Reflections Section
            item {
                Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                    DailyJournalCard(
                        date = uiState.selectedDate,
                        initialNotes = uiState.journalNotes,
                        onNotesChanged = { newNotes -> viewModel.saveJournalNotes(newNotes) }
                    )
                }
            }
        }
    }

    if (showEarlyFinishConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showEarlyFinishConfirmDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.Filled.Lock,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(32.dp)
                )
            },
            title = {
                Text(
                    text = "Finish Day Early?",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleLarge
                )
            },
            text = {
                Column {
                    Text(
                        text = "You have completed $completedTasksCount of $totalTasksCount tasks for this day.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Locking this day early with incomplete tasks will record a lower consistency percentage in your Analytics and you won't be able to edit these tasks later.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.markDayCompleted(completedTasksCount, totalTasksCount)
                        showEarlyFinishConfirmDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Yes, Lock Day")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEarlyFinishConfirmDialog = false }) {
                    Text("Keep Working")
                }
            }
        )
    }

    if (showFullCalendarDialog && plan != null) {
        val currentPlan = plan!!
        val startDate = try { LocalDate.parse(currentPlan.startDate) } catch (e: Exception) { LocalDate.now() }
        val endDate = try { LocalDate.parse(currentPlan.endDate) } catch (e: Exception) { startDate.plusDays(29) }

        PlanFullCalendarDialog(
            planHeading = currentPlan.heading,
            startDate = startDate,
            endDate = endDate,
            selectedDate = uiState.selectedDate,
            planDays = uiState.planDays,
            onDateSelected = { date ->
                viewModel.selectDate(date)
                showFullCalendarDialog = false
            },
            onDismiss = { showFullCalendarDialog = false }
        )
    }

    if (showFutureBlockedDialog) {
        FutureTaskBlockedDialog(
            onDismiss = { showFutureBlockedDialog = false }
        )
    }

    if (showAddOptionsSheet) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { showAddOptionsSheet = false },
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
                    text = uiState.selectedDate.format(DateTimeFormatter.ofPattern("EEEE, MMMM d, yyyy")),
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
                            showAddOptionsSheet = false
                            showAddTaskSheet = true
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
                            showAddOptionsSheet = false
                            showJournalSheet = true
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

    if (showJournalSheet) {
        JournalEditorSheet(
            date = uiState.selectedDate,
            initialNotes = uiState.journalNotes,
            onDismiss = { showJournalSheet = false },
            onSave = { updatedNotes ->
                viewModel.saveJournalNotes(updatedNotes)
                showJournalSheet = false
            }
        )
    }

    if (showAddTaskSheet) {
        val defaultDuration = plan?.defaultTaskDurationDays ?: 1
        TaskFormSheet(
            title = "Add Task",
            initialDescription = "",
            initialDurationDays = defaultDuration,
            initialSubtasks = listOf(""),
            baseDate = uiState.selectedDate,
            onDismiss = { showAddTaskSheet = false },
            onSave = { desc, durationDays, subs ->
                viewModel.addTask(desc, durationDays, subs)
                showAddTaskSheet = false
            }
        )
    }

    taskToEdit?.let { task ->
        val stringListType = object : TypeToken<List<String>>() {}.type
        val existingSubtasks: List<String> = try {
            Gson().fromJson(task.subtasks, stringListType) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }

        TaskFormSheet(
            title = "Edit Task",
            initialDescription = task.taskDescription,
            initialDurationDays = task.durationDays,
            initialSubtasks = if (existingSubtasks.isEmpty()) listOf("") else existingSubtasks,
            baseDate = uiState.selectedDate,
            onDismiss = { taskToEdit = null },
            onSave = { desc, durationDays, subs ->
                viewModel.editTask(task.templateId, task.checkinId, desc, durationDays, subs)
                taskToEdit = null
            }
        )
    }

    taskToDelete?.let { task ->
        AlertDialog(
            onDismissRequest = { taskToDelete = null },
            title = { Text("Delete Task") },
            text = { Text("Are you sure you want to delete \"${task.taskDescription}\"?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteTask(task.templateId)
                        taskToDelete = null
                    }
                ) {
                    Text("Delete", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { taskToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showEditPlanDialog && plan != null) {
        val currentPlan = plan!!
        val startDate = try { LocalDate.parse(currentPlan.startDate) } catch (e: Exception) { LocalDate.now() }
        val endDate = try { LocalDate.parse(currentPlan.endDate) } catch (e: Exception) { startDate.plusDays(29) }
        val currentDuration = (ChronoUnit.DAYS.between(startDate, endDate) + 1).coerceAtLeast(1).toInt()

        PlanFormDialog(
            title = "Edit Plan",
            initialName = currentPlan.heading,
            initialDescription = currentPlan.description,
            initialDurationDays = currentDuration,
            initialMakeDefault = currentPlan.defaultTaskDurationDays == currentDuration,
            initialReminderEnabled = currentPlan.reminderEnabled,
            initialReminderTime = currentPlan.reminderTime ?: "08:00",
            confirmText = "Save",
            onDismiss = { showEditPlanDialog = false },
            onSave = { name, desc, durationDays, defaultTaskDuration, reminderEnabled, reminderTime ->
                val newStart = startDate.format(DateTimeFormatter.ISO_LOCAL_DATE)
                val newEnd = startDate.plusDays((durationDays - 1).coerceAtLeast(0).toLong()).format(DateTimeFormatter.ISO_LOCAL_DATE)
                viewModel.updatePlan(
                    heading = name,
                    description = desc,
                    startDate = newStart,
                    endDate = newEnd,
                    defaultTaskDurationDays = defaultTaskDuration,
                    reminderEnabled = reminderEnabled,
                    reminderTime = reminderTime
                )
                showEditPlanDialog = false
            }
        )
    }

    if (showDeletePlanDialog && plan != null) {
        AlertDialog(
            onDismissRequest = { showDeletePlanDialog = false },
            title = { Text("Delete Plan") },
            text = { Text("Are you sure you want to delete \"${plan?.heading}\"? All tasks inside will be permanently deleted.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeletePlanDialog = false
                        viewModel.deletePlan {
                            onBack()
                        }
                    }
                ) {
                    Text("Delete", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeletePlanDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showShareStoryDialog && plan != null) {
        SharePlanStoryDialog(
            planId = plan!!.planId,
            planTitle = plan!!.heading,
            durationDays = uiState.totalDays,
            streakDays = uiState.planDays.count { it.isCompleted },
            consistencyPercent = uiState.overallProgressPercent,
            completedTasksCount = uiState.tasks.count { it.isCompleted },
            onDismissRequest = { showShareStoryDialog = false },
            onShared = {
                coroutineScope.launch {
                    snackbarHostState.showSnackbar("Story Card exported! +25 credits awarded.")
                }
            }
        )
    }
}

@Composable
fun DailyJournalCard(
    date: LocalDate,
    initialNotes: String,
    onNotesChanged: (String) -> Unit
) {
    var notesText by remember(date, initialNotes) { mutableStateOf(initialNotes) }
    var isSaved by remember(date, initialNotes) { mutableStateOf(true) }

    // Guaranteed Autosave on leaving the page, switching dates, or component disposal
    DisposableEffect(date, notesText) {
        onDispose {
            if (!isSaved) {
                onNotesChanged(notesText)
            }
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Outlined.EditNote,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "Daily Journal & Notes",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Write your experiences, reflections, and thoughts",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Saved indicator badge
                if (isSaved && notesText.isNotBlank()) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFF10B981).copy(alpha = 0.15f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Check,
                                contentDescription = "Saved",
                                tint = Color(0xFF10B981),
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = "Saved",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFF10B981),
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = notesText,
                onValueChange = {
                    notesText = it
                    isSaved = false
                },
                placeholder = { Text("How was your day? Jot down what went well or what you learned...") },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 100.dp),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                    focusedContainerColor = MaterialTheme.colorScheme.surface
                )
            )

            // Show Save button only when there are unsaved changes
            if (!isSaved) {
                Spacer(modifier = Modifier.height(10.dp))
                Button(
                    onClick = {
                        onNotesChanged(notesText)
                        isSaved = true
                    },
                    modifier = Modifier.align(Alignment.End),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Check,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Save Notes", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun PlanFullCalendarDialog(
    planHeading: String,
    startDate: LocalDate,
    endDate: LocalDate,
    selectedDate: LocalDate,
    planDays: List<PlanDayUiModel>,
    onDateSelected: (LocalDate) -> Unit,
    onDismiss: () -> Unit
) {
    var displayedMonth by remember { mutableStateOf(YearMonth.from(selectedDate)) }
    val totalDays = (ChronoUnit.DAYS.between(startDate, endDate) + 1).coerceAtLeast(1)
    val completedDaysCount = planDays.count { it.isCompleted }

    val daysByDate = remember(planDays) {
        planDays.associateBy { it.date }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = planHeading,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Filled.Close, contentDescription = "Close")
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                ) {
                    Text(
                        text = "${startDate.format(DateTimeFormatter.ofPattern("d MMM"))} – ${endDate.format(DateTimeFormatter.ofPattern("d MMM, yyyy"))} ($totalDays Days • $completedDaysCount Completed)",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Month Selector
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { displayedMonth = displayedMonth.minusMonths(1) },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Previous Month")
                    }

                    Text(
                        text = displayedMonth.format(DateTimeFormatter.ofPattern("MMMM yyyy")),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    IconButton(
                        onClick = { displayedMonth = displayedMonth.plusMonths(1) },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Next Month")
                    }
                }

                // Days of week header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    val daysOfWeek = listOf("S", "M", "T", "W", "T", "F", "S")
                    daysOfWeek.forEach { dayName ->
                        Text(
                            text = dayName,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.width(36.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Monthly Grid
                val firstDayOfMonth = displayedMonth.atDay(1)
                val firstDayOfWeekIndex = firstDayOfMonth.dayOfWeek.value % 7 // Sunday = 0
                val daysInMonth = displayedMonth.lengthOfMonth()
                val totalGridCells = ((firstDayOfWeekIndex + daysInMonth + 6) / 7) * 7

                LazyVerticalGrid(
                    columns = GridCells.Fixed(7),
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(totalGridCells) { index ->
                        val dayNumber = index - firstDayOfWeekIndex + 1
                        if (dayNumber in 1..daysInMonth) {
                            val currentDate = displayedMonth.atDay(dayNumber)
                            val isWithinPlan = !currentDate.isBefore(startDate) && !currentDate.isAfter(endDate)
                            val isSelected = currentDate == selectedDate
                            val isToday = currentDate == LocalDate.now()
                            val planDayData = daysByDate[currentDate]

                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .then(
                                        when {
                                            isSelected -> Modifier.background(MaterialTheme.colorScheme.primary)
                                            isWithinPlan -> Modifier.background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f))
                                            else -> Modifier
                                        }
                                    )
                                    .then(
                                        if (isToday && !isSelected) {
                                            Modifier.border(1.5.dp, MaterialTheme.colorScheme.primary, CircleShape)
                                        } else Modifier
                                    )
                                    .clickable(enabled = isWithinPlan) {
                                        onDateSelected(currentDate)
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Text(
                                        text = "$dayNumber",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = if (isSelected || isWithinPlan) FontWeight.Bold else FontWeight.Normal,
                                        color = when {
                                            isSelected -> MaterialTheme.colorScheme.onPrimary
                                            isWithinPlan -> MaterialTheme.colorScheme.onSurface
                                            else -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                                        }
                                    )

                                    if (planDayData != null && planDayData.hasTasks) {
                                        Box(
                                            modifier = Modifier
                                                .size(4.dp)
                                                .clip(CircleShape)
                                                .background(
                                                    if (planDayData.isCompleted) Color(0xFF10B981)
                                                    else if (isSelected) MaterialTheme.colorScheme.onPrimary
                                                    else MaterialTheme.colorScheme.primary
                                                )
                                        )
                                    }
                                }
                            }
                        } else {
                            Spacer(modifier = Modifier.size(36.dp))
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}

@Composable
fun FutureTaskBlockedDialog(
    onDismiss: () -> Unit
) {
    val now = LocalTime.now()
    val midnight = LocalTime.MAX
    val secondsRemaining = ChronoUnit.SECONDS.between(now, midnight) + 1
    val hours = (secondsRemaining / 3600).coerceAtLeast(0)
    val minutes = ((secondsRemaining % 3600) / 60).coerceAtLeast(0)
    val timeFormatted = if (hours > 0) "$hours hours $minutes minutes" else "$minutes minutes"

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Filled.Lock,
                contentDescription = "Locked",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(36.dp)
            )
        },
        title = {
            Text(
                text = "Future Check-in Locked",
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "You cannot update future tasks until the present day is completed.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(16.dp))

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Time left to complete today",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Outlined.AccessTime,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = timeFormatted,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("Got it")
            }
        }
    )
}

@Composable
fun PlanProgressHeader(
    currentDay: Int,
    totalDays: Int,
    progressPercent: Int,
    onCalendarClick: () -> Unit
) {
    val animatedProgress by animateFloatAsState(
        targetValue = (progressPercent / 100f).coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 600),
        label = "planProgress"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = if (currentDay in 1..totalDays) "Day $currentDay of $totalDays" else "Selected Day",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "$progressPercent% Completed Overall",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Dedicated Calendar Button on the Days Left / Progress Badge
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                    modifier = Modifier.clickable(onClick = onCalendarClick)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.CalendarMonth,
                            contentDescription = "View Calendar",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "${totalDays - currentDay.coerceAtLeast(1)}d left",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            LinearProgressIndicator(
                progress = { animatedProgress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )
        }
    }
}

@Composable
fun PlanTimelineCalendar(
    days: List<PlanDayUiModel>,
    onDateSelected: (LocalDate) -> Unit
) {
    val listState = rememberLazyListState()

    // Auto-scroll to selected day
    LaunchedEffect(days) {
        val selectedIdx = days.indexOfFirst { it.isSelected }
        if (selectedIdx >= 0) {
            listState.animateScrollToItem(selectedIdx.coerceAtLeast(0))
        }
    }

    LazyRow(
        state = listState,
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items(days, key = { it.date.toString() }) { day ->
            PlanDayPill(
                day = day,
                onClick = { onDateSelected(day.date) }
            )
        }
    }
}

@Composable
fun PlanDayPill(
    day: PlanDayUiModel,
    onClick: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val isSelected = day.isSelected
    val isToday = day.isToday
    val isCompleted = day.isCompleted
    val isLocked = day.isLocked

    val containerColor = if (isSelected) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.surface
    }

    val contentColor = if (isSelected) {
        MaterialTheme.colorScheme.onPrimary
    } else {
        MaterialTheme.colorScheme.onSurface
    }

    Surface(
        modifier = Modifier
            .width(60.dp)
            .clip(RoundedCornerShape(20.dp))
            .then(
                if (isToday && !isSelected) {
                    Modifier.border(1.5.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(20.dp))
                } else Modifier
            )
            .clickable(onClick = {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onClick()
            }),
        shape = RoundedCornerShape(20.dp),
        color = containerColor,
        tonalElevation = if (isSelected) 4.dp else 1.dp
    ) {
        Column(
            modifier = Modifier.padding(vertical = 12.dp, horizontal = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = day.dayOfWeek,
                style = MaterialTheme.typography.labelSmall,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = contentColor.copy(alpha = if (isSelected) 0.85f else 0.6f)
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = day.dayOfMonth,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = contentColor
            )

            Spacer(modifier = Modifier.height(6.dp))

            // Completion / Task Status Dot
            if (isLocked) {
                Box(
                    modifier = Modifier
                        .size(16.dp)
                        .clip(CircleShape)
                        .background(if (isSelected) MaterialTheme.colorScheme.onPrimary else Color(0xFF10B981)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.Lock,
                        contentDescription = "Locked",
                        tint = if (isSelected) MaterialTheme.colorScheme.primary else Color.White,
                        modifier = Modifier.size(9.dp)
                    )
                }
            } else if (day.hasTasks) {
                if (isCompleted) {
                    Box(
                        modifier = Modifier
                            .size(16.dp)
                            .clip(CircleShape)
                            .background(if (isSelected) MaterialTheme.colorScheme.onPrimary else Color(0xFF10B981)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Check,
                            contentDescription = "Completed",
                            tint = if (isSelected) MaterialTheme.colorScheme.primary else Color.White,
                            modifier = Modifier.size(10.dp)
                        )
                    }
                } else {
                    // Partial / Pending Tasks dot
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(if (isSelected) MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f) else MaterialTheme.colorScheme.primary)
                    )
                }
            } else {
                // Empty day placeholder dot
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(contentColor.copy(alpha = 0.2f))
                )
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
    onDelete: () -> Unit
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
    var isSubtasksExpanded by remember { mutableStateOf(true) }

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
        modifier = Modifier
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
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                modifier = Modifier.clickable {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    isSubtasksExpanded = !isSubtasksExpanded
                                }
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "$completedSubtaskCount/${subtasks.size} subtasks",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (completedSubtaskCount == subtasks.size && subtasks.isNotEmpty()) Color(0xFF10B981) else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.width(2.dp))
                                    Icon(
                                        imageVector = if (isSubtasksExpanded) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                                        contentDescription = if (isSubtasksExpanded) "Collapse subtasks" else "Expand subtasks",
                                        modifier = Modifier.size(13.dp),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
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

            if (subtasks.isNotEmpty()) {
                AnimatedVisibility(
                    visible = isSubtasksExpanded,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 16.dp, end = 16.dp, top = 2.dp, bottom = 12.dp)
                    ) {
                        subtasks.forEachIndexed { index, subtaskTitle ->
                            val isSubChecked = completedSubtasks.getOrElse(index) { false }
                            val isLast = index == subtasks.lastIndex

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(IntrinsicSize.Min)
                                    .clickable(enabled = !isLocked) { 
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        onSubtaskCheckedChange(index, !isSubChecked) 
                                    }
                            ) {
                                // Reddit-style linking thread branch connecting parent to subtask
                                TaskThreadBranch(
                                    isLast = isLast,
                                    isCompleted = isSubChecked,
                                    width = 32.dp,
                                    trunkX = 12.dp,
                                    modifier = Modifier.fillMaxHeight()
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
                                    textDecoration = if (isSubChecked) TextDecoration.LineThrough else TextDecoration.None,
                                    modifier = Modifier
                                        .weight(1f)
                                        .padding(vertical = 6.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

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

            Column(modifier = Modifier.fillMaxWidth()) {
                subtasks.forEachIndexed { index, subtask ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(IntrinsicSize.Min)
                            .padding(bottom = 8.dp)
                    ) {
                        TaskThreadBranch(
                            isLast = false,
                            isCompleted = false,
                            width = 24.dp,
                            trunkX = 8.dp,
                            modifier = Modifier.fillMaxHeight()
                        )
                        Spacer(modifier = Modifier.width(4.dp))
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

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(IntrinsicSize.Min)
                ) {
                    TaskThreadBranch(
                        isLast = true,
                        isCompleted = false,
                        width = 24.dp,
                        trunkX = 8.dp,
                        modifier = Modifier.fillMaxHeight()
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    TextButton(onClick = { subtasks = subtasks + "" }) {
                        Text("+ Add another subtask")
                    }
                }
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JournalEditorSheet(
    date: LocalDate,
    initialNotes: String,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var text by remember(date, initialNotes) { mutableStateOf(initialNotes) }
    val haptic = LocalHapticFeedback.current

    ModalBottomSheet(
        onDismissRequest = {
            onSave(text)
            onDismiss()
        },
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
                .padding(bottom = 32.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Outlined.EditNote,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Daily Journal & Diary",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = date.format(DateTimeFormatter.ofPattern("EEEE, MMMM d, yyyy")),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                IconButton(onClick = {
                    onSave(text)
                    onDismiss()
                }) {
                    Icon(Icons.Filled.Close, contentDescription = "Close")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 180.dp, max = 320.dp),
                placeholder = {
                    Text("How was your day? Jot down what went well, what you learned, or personal reflections and diary notes...")
                },
                shape = RoundedCornerShape(14.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            val wordCount = if (text.isBlank()) 0 else text.trim().split("\\s+".toRegex()).size
            val charCount = text.length

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "$wordCount words • $charCount characters",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Row {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onSave(text)
                        },
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Save Notes", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
