package com.example.plannerapp.ui.detail

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import com.example.plannerapp.data.DailyTaskView
import com.example.plannerapp.sharing.SharePlanStoryDialog
import com.example.plannerapp.ui.home.PlanFormDialog
import com.example.plannerapp.ui.state.Resource
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlanDetailScreen(
    viewModel: PlanDetailViewModel,
    autoOpenAddTask: Boolean = false,
    onBack: () -> Unit,
    onOpenCommunityDiscussion: (String) -> Unit = {},
    onPlanCloned: (Long) -> Unit = {},
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
    var showShareStoryDialog by remember { mutableStateOf(false) }
    var showPublishPlanDialog by remember { mutableStateOf(false) }

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
            PlanDetailTopBar(
                planHeading = plan?.heading,
                startDate = plan?.startDate,
                endDate = plan?.endDate,
                onBack = onBack,
                onShareStory = { showShareStoryDialog = true },
                onEditPlan = { showEditPlanDialog = true },
                onDeletePlan = { showDeletePlanDialog = true },
                onPublishPlan = { showPublishPlanDialog = true }
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
                    CommunityPlanBanner(
                        community = community,
                        onClick = { onOpenCommunityDiscussion(community.postId) }
                    )
                }
            }

            // Plan Version Update Banner if creator released a new version
            uiState.planUpdate?.let { update ->
                item {
                    PlanVersionUpdateBanner(
                        update = update,
                        onAdoptTasks = {
                            viewModel.adoptPlanUpdate(update) { count ->
                                coroutineScope.launch {
                                    snackbarHostState.showSnackbar("Adopted $count new tasks. Your existing progress is preserved.")
                                }
                            }
                        },
                        onCloneAsNew = {
                            viewModel.cloneAsNewPlan(update) { newId ->
                                onPlanCloned(newId)
                            }
                        },
                        onDismiss = { viewModel.dismissPlanUpdate(update.latestVersionTag) }
                    )
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
                PlanTimelineHeader(
                    selectedDate = uiState.selectedDate,
                    onJumpToToday = { viewModel.selectDate(LocalDate.now()) }
                )
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
                PlanDetailTasksHeader(
                    selectedDate = uiState.selectedDate,
                    isDayLocked = isDayLocked,
                    isFutureDate = isFutureDate,
                    completedTasksCount = completedTasksCount,
                    totalTasksCount = totalTasksCount
                )
            }

            // 6. Tasks List or Empty State
            if (uiState.tasks.isEmpty()) {
                item {
                    PlanDetailEmptyTasksView(
                        isDayLocked = isDayLocked,
                        onAddTask = { showAddTaskSheet = true }
                    )
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
                PlanDetailDayCompletionCard(
                    isDayLocked = isDayLocked,
                    isFutureDate = isFutureDate,
                    totalTasksCount = totalTasksCount,
                    allTasksCompleted = allTasksCompleted,
                    completedTasksCount = completedTasksCount,
                    onMarkDayCompleted = { viewModel.markDayCompleted(completedTasksCount, totalTasksCount) },
                    onFinishEarly = { showEarlyFinishConfirmDialog = true }
                )
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
        EarlyFinishConfirmDialog(
            completedTasksCount = completedTasksCount,
            totalTasksCount = totalTasksCount,
            onConfirm = {
                viewModel.markDayCompleted(completedTasksCount, totalTasksCount)
                showEarlyFinishConfirmDialog = false
            },
            onDismiss = { showEarlyFinishConfirmDialog = false }
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
        AddOptionsSheet(
            selectedDate = uiState.selectedDate,
            isDayLocked = isDayLocked,
            onDismiss = { showAddOptionsSheet = false },
            onAddTask = {
                showAddOptionsSheet = false
                showAddTaskSheet = true
            },
            onOpenJournal = {
                showAddOptionsSheet = false
                showJournalSheet = true
            }
        )
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
        DeleteTaskConfirmDialog(
            taskDescription = task.taskDescription,
            onConfirm = {
                viewModel.deleteTask(task.templateId)
                taskToDelete = null
            },
            onDismiss = { taskToDelete = null }
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
        DeletePlanConfirmDialog(
            planHeading = plan!!.heading,
            onConfirm = {
                showDeletePlanDialog = false
                viewModel.deletePlan {
                    onBack()
                }
            },
            onDismiss = { showDeletePlanDialog = false }
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

    if (showPublishPlanDialog && plan != null) {
        val currentContext = LocalContext.current
        val db = remember(currentContext) { com.example.plannerapp.data.PlannerDatabase.getDatabase(currentContext) }
        val socialRepo = remember { com.example.plannerapp.data.social.SupabaseSocialRepository() }
        com.example.plannerapp.ui.social.PublishPlanDialog(
            plan = plan!!,
            plannerRepository = com.example.plannerapp.data.PlannerRepository(db.plannerDao()),
            socialRepository = socialRepo,
            userDao = db.userDao(),
            onDismiss = { showPublishPlanDialog = false },
            onPublished = { postId ->
                showPublishPlanDialog = false
                onOpenCommunityDiscussion(postId)
            }
        )
    }
}
