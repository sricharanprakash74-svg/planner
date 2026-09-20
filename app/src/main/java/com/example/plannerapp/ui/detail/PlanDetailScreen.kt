package com.example.plannerapp.ui.detail

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
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
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.EditNote
import androidx.compose.material.icons.outlined.Forum
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.NewReleases
import androidx.compose.material.icons.outlined.Public
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.*
import com.example.plannerapp.sharing.SharePlanStoryDialog
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
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
import com.example.plannerapp.smartlink.SmartLinkParser
import com.example.plannerapp.smartlink.ui.SmartLinkCard
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.graphicsLayer
import com.example.plannerapp.theme.AppDimens
import com.example.plannerapp.theme.PhysicsSpec
import com.example.plannerapp.ui.components.SubtaskBranchConnector
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
    var planMenuExpanded by remember { mutableStateOf(false) }
    var showShareStoryDialog by remember { mutableStateOf(false) }

    var showFullCalendarDialog by remember { mutableStateOf(false) }
    var showEarlyFinishConfirmDialog by remember { mutableStateOf(false) }
    var showReflectionSection by remember { mutableStateOf(false) }
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
                    Text(
                        text = "Plan Overview",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    AssistChip(
                        onClick = {
                            val newPublic = !(plan?.isPublic ?: false)
                            viewModel.togglePlanVisibility(newPublic)
                            coroutineScope.launch {
                                snackbarHostState.showSnackbar(
                                    if (newPublic) "Plan is now Public and discoverable by the community."
                                    else "Plan is now Private."
                                )
                            }
                        },
                        label = {
                            Text(
                                text = if (plan?.isPublic == true) "Public" else "Private",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold
                            )
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = if (plan?.isPublic == true) Icons.Outlined.Public else Icons.Outlined.Lock,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp)
                            )
                        },
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = if (plan?.isPublic == true)
                                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
                            else
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            labelColor = if (plan?.isPublic == true)
                                MaterialTheme.colorScheme.onPrimaryContainer
                            else
                                MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        modifier = Modifier.padding(end = 4.dp)
                    )

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
                                text = { Text(if (plan?.isPublic == true) "Make Plan Private" else "Make Plan Public") },
                                leadingIcon = {
                                    Icon(
                                        imageVector = if (plan?.isPublic == true) Icons.Outlined.Lock else Icons.Outlined.Public,
                                        contentDescription = null
                                    )
                                },
                                onClick = {
                                    planMenuExpanded = false
                                    val newPublic = !(plan?.isPublic ?: false)
                                    viewModel.togglePlanVisibility(newPublic)
                                    coroutineScope.launch {
                                        snackbarHostState.showSnackbar(
                                            if (newPublic) "Plan is now Public." else "Plan is now Private."
                                        )
                                    }
                                }
                            )
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
                            if (!isDayLocked) {
                                DropdownMenuItem(
                                    text = { Text("Finish Day Early (Lock Day)") },
                                    leadingIcon = { Icon(Icons.Outlined.Lock, contentDescription = null) },
                                    onClick = {
                                        planMenuExpanded = false
                                        showEarlyFinishConfirmDialog = true
                                    }
                                )
                            }
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
            // Plan Version Update Banner (Non-destructive update propagation)
            uiState.planUpdate?.let { update ->
                item {
                    Card(
                        modifier = Modifier
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
                                    onClick = {
                                        viewModel.adoptPlanUpdate(update) { count ->
                                            coroutineScope.launch {
                                                snackbarHostState.showSnackbar("Adopted $count new tasks. Your existing progress is preserved.")
                                            }
                                        }
                                    },
                                    modifier = Modifier.weight(1f),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                                ) {
                                    Text("Adopt Tasks", style = MaterialTheme.typography.labelMedium)
                                }

                                OutlinedButton(
                                    onClick = {
                                        viewModel.cloneAsNewPlan(update) { newId ->
                                            onPlanCloned(newId)
                                        }
                                    },
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                                ) {
                                    Text("Clone As New", style = MaterialTheme.typography.labelMedium)
                                }

                                TextButton(
                                    onClick = { viewModel.dismissPlanUpdate(update.latestVersionTag) },
                                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 6.dp)
                                ) {
                                    Text("Dismiss", style = MaterialTheme.typography.labelSmall)
                                }
                            }
                        }
                    }
                }
            }

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

            // 1. Plan Overview Header (with Calendar trigger on days left)
            item {
                val headerDateFormatter = remember { DateTimeFormatter.ofPattern("MMM d") }
                val formattedDates = remember(plan?.startDate, plan?.endDate) {
                    try {
                        val start = LocalDate.parse(plan?.startDate ?: "").format(headerDateFormatter)
                        val end = LocalDate.parse(plan?.endDate ?: "").format(headerDateFormatter)
                        "$start – $end"
                    } catch (e: Exception) {
                        "${plan?.startDate} – ${plan?.endDate}"
                    }
                }

                PlanOverviewHeader(
                    planTitle = plan?.heading ?: "Plan Overview",
                    dateRange = formattedDates,
                    currentDay = uiState.currentDayNumber,
                    totalDays = uiState.totalDays,
                    progressPercent = uiState.overallProgressPercent,
                    isDayComplete = isDayLocked || allTasksCompleted,
                    selectedDate = uiState.selectedDate,
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

            if (isDayLocked || (totalTasksCount > 0 && allTasksCompleted)) {
                // ── COMPLETED DAY / JOURNAL READING VIEW ───────────────
                item {
                    CompletedDayJournalView(
                        date = uiState.selectedDate,
                        tasks = uiState.tasks,
                        journalNotes = uiState.journalNotes,
                        isDayLocked = isDayLocked,
                        onFinalizeDay = {
                            viewModel.markDayCompleted(completedTasksCount, totalTasksCount)
                        },
                        onAddReflection = {
                            showJournalSheet = true
                        }
                    )
                }
            } else {
                // ── TASK EXECUTION MODE ────────────────────────────────
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
                            if (isFutureDate) {
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
                                Spacer(modifier = Modifier.height(8.dp))
                                TextButton(onClick = { showAddTaskSheet = true }) {
                                    Text("+ Add a task for this day")
                                }
                            }
                        }
                    }
                } else {
                    itemsIndexed(uiState.tasks, key = { _, it -> it.checkinId }) { index, task ->
                        com.example.plannerapp.ui.components.StaggeredEntrance(index = index) {
                            Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                                TaskItemWithSubtasks(
                                    task = task,
                                    isFuture = isFutureDate,
                                    isLocked = false,
                                    onCheckedChange = { isChecked -> 
                                        handleCheckinAction {
                                            viewModel.onTaskChecked(task.checkinId, isChecked)
                                        }
                                    },
                                    onSubtaskCheckedChange = { subtaskIndex, isChecked -> 
                                        handleCheckinAction {
                                            viewModel.onSubtaskChecked(task, subtaskIndex, isChecked)
                                        }
                                    },
                                    onEdit = { taskToEdit = task },
                                    onDelete = { taskToDelete = task }
                                )
                            }
                        }
                    }
                }

                // Daily Journal & Reflections Section (Opt-in during execution)
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        if (showReflectionSection || uiState.journalNotes.isNotBlank()) {
                            DailyJournalCard(
                                date = uiState.selectedDate,
                                initialNotes = uiState.journalNotes,
                                onNotesChanged = { newNotes -> viewModel.saveJournalNotes(newNotes) }
                            )
                        } else {
                            TextButton(
                                onClick = { showReflectionSection = true },
                                colors = ButtonDefaults.textButtonColors(
                                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f)
                                )
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.EditNote,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Add reflection or daily notes",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    }
                }

                // Calm action for Complete Day Early
                if (totalTasksCount > 0) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            TextButton(
                                onClick = { showEarlyFinishConfirmDialog = true },
                                colors = ButtonDefaults.textButtonColors(
                                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                )
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.CheckCircle,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Complete day early",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showEarlyFinishConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showEarlyFinishConfirmDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.Outlined.CheckCircle,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(28.dp)
                )
            },
            title = {
                Text(
                    text = "Complete Day Early?",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleLarge
                )
            },
            text = {
                Column {
                    Text(
                        text = "You have completed $completedTasksCount of $totalTasksCount tasks today.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Completing early records your day's achievements. You won't be able to edit today's tasks afterwards.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.markDayCompleted(completedTasksCount, totalTasksCount)
                        showEarlyFinishConfirmDialog = false
                    },
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Complete Day")
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

                // Option 2: Add Subtask
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = if (isDayLocked) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                            else MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.35f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(enabled = !isDayLocked) {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            showAddOptionsSheet = false
                            val activeTask = uiState.tasks.firstOrNull { !it.isCompleted } ?: uiState.tasks.firstOrNull()
                            if (activeTask != null) {
                                taskToEdit = activeTask
                            } else {
                                showAddTaskSheet = true
                            }
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
                                .background(MaterialTheme.colorScheme.tertiary.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Add,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.tertiary,
                                modifier = Modifier.size(26.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Add Subtask",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = if (isDayLocked) "Day is locked" else "Break down an existing goal into smaller steps",
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

            TextField(
                value = notesText,
                onValueChange = {
                    notesText = it
                    isSaved = false
                },
                placeholder = {
                    Text(
                        text = "How was your day? Jot down what went well or what you learned...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 100.dp),
                shape = RoundedCornerShape(8.dp),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    disabledContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
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
fun PlanOverviewHeader(
    planTitle: String,
    dateRange: String,
    currentDay: Int,
    totalDays: Int,
    progressPercent: Int,
    isDayComplete: Boolean,
    selectedDate: LocalDate,
    onCalendarClick: () -> Unit
) {
    val animatedProgress by animateFloatAsState(
        targetValue = (progressPercent / 100f).coerceIn(0f, 1f),
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "planProgress"
    )
    val animatedPercent by animateIntAsState(
        targetValue = progressPercent,
        animationSpec = tween(durationMillis = 350),
        label = "planPercent"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
        border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            // ── 1. Plan Title & Date Range ─────────────────────────
            Text(
                text = planTitle,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            if (dateRange.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = dateRange,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ── 2. Subtle Celebratory Day Complete State ───────────
            AnimatedVisibility(
                visible = isDayComplete,
                enter = fadeIn(animationSpec = tween(250)) + expandVertically(animationSpec = spring(stiffness = Spring.StiffnessMediumLow)),
                exit = fadeOut(animationSpec = tween(200)) + shrinkVertically(animationSpec = spring(stiffness = Spring.StiffnessMediumLow))
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFF10B981).copy(alpha = 0.10f),
                    border = BorderStroke(0.5.dp, Color(0xFF10B981).copy(alpha = 0.30f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 14.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF10B981)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Check,
                                contentDescription = "Day Complete",
                                tint = Color.White,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = if (currentDay in 1..totalDays) "Day $currentDay completed" else "Day completed",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF10B981)
                            )
                            Text(
                                text = "All tasks finished • ${selectedDate.format(DateTimeFormatter.ofPattern("MMM d, yyyy"))}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // ── 3. Progress Section ────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (currentDay in 1..totalDays) "Day $currentDay of $totalDays" else "Selected Day",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

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
                            text = "${(totalDays - currentDay.coerceAtLeast(1)).coerceAtLeast(0)}d left",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            ) {
                if (animatedProgress > 0f) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(fraction = animatedProgress.coerceIn(0f, 1f))
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(4.dp))
                            .background(MaterialTheme.colorScheme.primary)
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "$animatedPercent% completed overall",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun CompletedDayJournalView(
    date: LocalDate,
    tasks: List<DailyTaskView>,
    journalNotes: String,
    isDayLocked: Boolean,
    onFinalizeDay: () -> Unit,
    onAddReflection: () -> Unit,
    modifier: Modifier = Modifier
) {
    val dateFormatter = remember { DateTimeFormatter.ofPattern("MMMM d, yyyy") }
    val formattedDate = remember(date) { date.format(dateFormatter) }
    val gson = remember { Gson() }
    val stringListType = remember { object : TypeToken<List<String>>() {}.type }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // ── 1. Hero Celebration & State Banner ──────────────────────
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            color = Color(0xFF10B981).copy(alpha = 0.08f),
            border = BorderStroke(1.dp, Color(0xFF10B981).copy(alpha = 0.25f))
        ) {
            Column(
                modifier = Modifier.padding(20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFF10B981).copy(alpha = 0.16f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Filled.CheckCircle,
                                contentDescription = null,
                                tint = Color(0xFF10B981),
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "DAY COMPLETE",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF10B981),
                                letterSpacing = 1.sp
                            )
                        }
                    }

                    if (isDayLocked) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Lock,
                                    contentDescription = "Locked in History",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(12.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "History Locked",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = formattedDate,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(4.dp))

                val totalSubtasksCount = remember(tasks) {
                    tasks.sumOf { task ->
                        try {
                            val list: List<String> = gson.fromJson(task.subtasks, stringListType) ?: emptyList()
                            list.size
                        } catch (e: Exception) { 0 }
                    }
                }

                Text(
                    text = "You completed everything planned • ${tasks.size} tasks" +
                            if (totalSubtasksCount > 0) " • $totalSubtasksCount subtasks" else "",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (!isDayLocked) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = onFinalizeDay,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF10B981),
                            contentColor = Color.White
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Check,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Finalize & Record Day in Streak",
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // ── 2. Today's Progress (Clean Reading Layout) ──────────────
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Checklist,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "TODAY'S PROGRESS",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        letterSpacing = 0.5.sp
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                tasks.forEachIndexed { index, task ->
                    val cleanTaskTitle = remember(task.taskDescription) {
                        val clean = SmartLinkParser.extractCleanText(task.taskDescription)
                        if (clean.isNotBlank()) clean else task.taskDescription
                    }

                    val subtasks: List<String> = remember(task.subtasks) {
                        try {
                            gson.fromJson(task.subtasks, stringListType) ?: emptyList()
                        } catch (e: Exception) { emptyList() }
                    }

                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = cleanTaskTitle,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.weight(1f)
                            )
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = Color(0xFF10B981).copy(alpha = 0.12f)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Check,
                                        contentDescription = null,
                                        tint = Color(0xFF10B981),
                                        modifier = Modifier.size(11.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Done",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF10B981)
                                    )
                                }
                            }
                        }

                        if (subtasks.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(start = 12.dp)
                            ) {
                                subtasks.forEach { subtask ->
                                    val cleanSub = remember(subtask) {
                                        val c = SmartLinkParser.extractCleanText(subtask)
                                        if (c.isNotBlank()) c else subtask
                                    }
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(vertical = 3.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.Check,
                                            contentDescription = null,
                                            tint = Color(0xFF10B981),
                                            modifier = Modifier.size(13.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = cleanSub,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }

                        if (index < tasks.lastIndex) {
                            Spacer(modifier = Modifier.height(14.dp))
                            HorizontalDivider(
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f)
                            )
                            Spacer(modifier = Modifier.height(14.dp))
                        }
                    }
                }
            }
        }

        // ── 3. My Journal (Reading Optimized) ───────────────────────
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Outlined.EditNote,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "MY JOURNAL",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            letterSpacing = 0.5.sp
                        )
                    }

                    if (journalNotes.isNotBlank()) {
                        TextButton(
                            onClick = onAddReflection,
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Edit,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(text = "Edit", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                if (journalNotes.isNotBlank()) {
                    Text(
                        text = journalNotes,
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontSize = 17.sp,
                            lineHeight = 26.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.EditNote,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.size(36.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Nothing written yet.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Capture your thoughts, reflections, or breakthroughs for today.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        OutlinedButton(
                            onClick = onAddReflection,
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Add,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(text = "Add reflection")
                        }
                    }
                }
            }
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

    val targetContainerColor = when {
        isSelected -> MaterialTheme.colorScheme.primary
        day.isToday -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
        else -> MaterialTheme.colorScheme.surface
    }
    val containerColor by animateColorAsState(targetValue = targetContainerColor, label = "pillContainer")

    val targetContentColor = when {
        isSelected -> MaterialTheme.colorScheme.onPrimary
        day.isToday -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.onSurface
    }
    val contentColor by animateColorAsState(targetValue = targetContentColor, label = "pillContent")

    Surface(
        modifier = Modifier
            .width(62.dp)
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
        border = if (!isSelected && !isToday) BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)) else null,
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
                color = contentColor.copy(alpha = if (isSelected) 0.85f else 0.65f)
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = day.dayOfMonth,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = contentColor
            )

            Spacer(modifier = Modifier.height(6.dp))

            // Completion / Task Status Dot or Badge
            if (isLocked) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = if (isSelected) MaterialTheme.colorScheme.onPrimary else Color(0xFF10B981),
                    modifier = Modifier.size(18.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Filled.Lock,
                            contentDescription = "Locked",
                            tint = if (isSelected) MaterialTheme.colorScheme.primary else Color.White,
                            modifier = Modifier.size(10.dp)
                        )
                    }
                }
            } else if (day.hasTasks) {
                if (isCompleted) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = if (isSelected) MaterialTheme.colorScheme.onPrimary else Color(0xFF10B981),
                        modifier = Modifier.size(18.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Filled.Check,
                                contentDescription = "Completed",
                                tint = if (isSelected) MaterialTheme.colorScheme.primary else Color.White,
                                modifier = Modifier.size(11.dp)
                            )
                        }
                    }
                } else {
                    // Partial / Pending Tasks dot
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(if (isSelected) MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f) else MaterialTheme.colorScheme.primary)
                    )
                }
            } else {
                Spacer(modifier = Modifier.size(18.dp))
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
    val chevronRotation by animateFloatAsState(
        targetValue = if (isSubtasksExpanded) 180f else 0f,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "chevronRotate"
    )

    val targetContainerColor = if (task.isCompleted) {
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
    } else {
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
    }

    val animatedContainerColor by animateColorAsState(
        targetValue = targetContainerColor,
        label = "taskCardBg"
    )
    val targetAlpha = if (task.isCompleted) 0.55f else 1.0f
    val animatedAlpha by animateFloatAsState(
        targetValue = targetAlpha,
        label = "taskCardAlpha"
    )

    val interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.96f else 1f,
        animationSpec = if (isPressed) PhysicsSpec.PressDown else PhysicsSpec.PressRelease,
        label = "taskScale"
    )

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioNoBouncy,
                    stiffness = Spring.StiffnessMediumLow
                )
            )
            .graphicsLayer { 
                alpha = animatedAlpha
                scaleX = scale
                scaleY = scale 
            }
            .clip(RoundedCornerShape(16.dp)),
        color = animatedContainerColor,
        border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(
                        interactionSource = interactionSource,
                        indication = null,
                        enabled = !isLocked
                    ) { 
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
                    enabled = !isLocked,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    val detectedTaskUrl = remember(task.taskDescription) {
                        SmartLinkParser.findFirstUrl(task.taskDescription)
                    }
                    val cleanTaskTitle = remember(task.taskDescription) {
                        val clean = SmartLinkParser.extractCleanText(task.taskDescription)
                        if (clean.isNotBlank()) clean else (detectedTaskUrl?.let { SmartLinkParser.extractDomain(it) } ?: task.taskDescription)
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = cleanTaskTitle,
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

                    if (detectedTaskUrl != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        SmartLinkCard(
                            url = detectedTaskUrl,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    if (subtasks.isNotEmpty()) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.padding(top = 4.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (completedSubtaskCount == subtasks.size && subtasks.isNotEmpty()) Color(0xFF10B981).copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        isSubtasksExpanded = !isSubtasksExpanded
                                    }
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.5.dp)
                                ) {
                                    Text(
                                        text = "$completedSubtaskCount of ${subtasks.size} done",
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                                        fontWeight = FontWeight.Medium,
                                        color = if (completedSubtaskCount == subtasks.size && subtasks.isNotEmpty()) Color(0xFF10B981) else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Icon(
                                        imageVector = Icons.Filled.KeyboardArrowDown,
                                        contentDescription = if (isSubtasksExpanded) "Collapse subtasks" else "Expand subtasks",
                                        modifier = Modifier
                                            .size(14.dp)
                                            .rotate(chevronRotation),
                                        tint = if (completedSubtaskCount == subtasks.size && subtasks.isNotEmpty()) Color(0xFF10B981) else MaterialTheme.colorScheme.onSurfaceVariant
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
                                text = { Text("Add Subtask") },
                                leadingIcon = { Icon(Icons.Filled.Add, contentDescription = null) },
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
                    enter = fadeIn(animationSpec = PhysicsSpec.SheetSettle) + expandVertically(
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioNoBouncy,
                            stiffness = Spring.StiffnessMediumLow
                        )
                    ),
                    exit = fadeOut(animationSpec = PhysicsSpec.SheetSettle) + shrinkVertically(
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioNoBouncy,
                            stiffness = Spring.StiffnessMediumLow
                        )
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 26.dp, end = 16.dp, top = 2.dp, bottom = 12.dp)
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
                                // Smooth curved tree branch directly anchored beneath parent checkbox
                                SubtaskBranchConnector(
                                    isLastChild = isLast,
                                    isCompleted = isSubChecked,
                                    modifier = Modifier
                                        .width(20.dp)
                                        .fillMaxHeight()
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
                                    modifier = Modifier.size(18.dp)
                                )

                                Spacer(modifier = Modifier.width(8.dp))

                                val detectedSubUrl = remember(subtaskTitle) {
                                    SmartLinkParser.findFirstUrl(subtaskTitle)
                                }
                                val cleanSubText = remember(subtaskTitle) {
                                    val clean = SmartLinkParser.extractCleanText(subtaskTitle)
                                    if (clean.isNotBlank()) clean else (detectedSubUrl?.let { SmartLinkParser.extractDomain(it) } ?: subtaskTitle)
                                }

                                Column(
                                    modifier = Modifier
                                        .weight(1f)
                                        .padding(vertical = 6.dp)
                                ) {
                                    Text(
                                        text = cleanSubText,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = if (isSubChecked) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f) else MaterialTheme.colorScheme.onSurface,
                                        textDecoration = if (isSubChecked) TextDecoration.LineThrough else TextDecoration.None
                                    )
                                    if (detectedSubUrl != null) {
                                        Spacer(modifier = Modifier.height(6.dp))
                                        SmartLinkCard(
                                            url = detectedSubUrl,
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                    }
                                }
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

    var focusSubtaskIndexToRequest by remember { mutableStateOf<Int?>(null) }
    val subtaskFocusRequesters = remember { mutableStateListOf<FocusRequester>() }

    // Synchronize focus requesters with subtask list
    while (subtaskFocusRequesters.size < subtasks.size) {
        subtaskFocusRequesters.add(FocusRequester())
    }

    LaunchedEffect(focusSubtaskIndexToRequest) {
        focusSubtaskIndexToRequest?.let { idx ->
            if (idx in subtaskFocusRequesters.indices) {
                try {
                    subtaskFocusRequesters[idx].requestFocus()
                } catch (e: Exception) {
                    // Safety check if not composed yet
                }
            }
            focusSubtaskIndexToRequest = null
        }
    }

    fun handleSaveAndDismiss() {
        if (text.isNotBlank()) {
            onSave(text.trim(), durationDays, subtasks.filter { it.isNotBlank() })
        }
        onDismiss()
    }

    ModalBottomSheet(
        onDismissRequest = { handleSaveAndDismiss() },
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .imePadding()
        ) {
            // ── Pinned Action Header ──────────────────────────────────────
            // The Save CTA is permanently docked at the top, immune to keyboard occlusion
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onDismiss) {
                    Text("Cancel", style = MaterialTheme.typography.bodyLarge)
                }
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Button(
                    onClick = { handleSaveAndDismiss() },
                    enabled = text.isNotBlank(),
                    shape = RoundedCornerShape(AppDimens.CornerCompact),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp)
                ) {
                    Text("Save", fontWeight = FontWeight.Bold)
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f))

            // ── Scrollable Form Body ──────────────────────────────────────
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = false)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 16.dp)
            ) {
                // Task Description (Soft filled surface, no harsh borders)
                TextField(
                    value = text,
                    onValueChange = { text = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { 
                        Text(
                            "Task description or paste link...",
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        ) 
                    },
                    singleLine = false,
                    maxLines = 3,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    keyboardActions = KeyboardActions(
                        onNext = {
                            if (subtasks.isEmpty()) {
                                subtasks = listOf("")
                                focusSubtaskIndexToRequest = 0
                            } else {
                                focusSubtaskIndexToRequest = 0
                            }
                        }
                    ),
                    shape = RoundedCornerShape(AppDimens.CornerCompact),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent
                    )
                )

                // Live Smart Link Resource Preview
                val detectedFormUrl = remember(text) {
                    SmartLinkParser.findFirstUrl(text)
                }
                if (detectedFormUrl != null) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Resource Link Preview",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    SmartLinkCard(
                        url = detectedFormUrl,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Duration Selection Row
                DurationPickerRow(
                    durationDays = durationDays,
                    baseDate = baseDate,
                    onClick = { showDurationDialog = true }
                )

                Spacer(modifier = Modifier.height(20.dp))
                Text(
                    text = "Subtasks (Optional)",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(10.dp))

                Column(modifier = Modifier.fillMaxWidth()) {
                    subtasks.forEachIndexed { index, subtask ->
                        var isFocused by remember { mutableStateOf(false) }
                        val isLast = index == subtasks.lastIndex

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(IntrinsicSize.Min)
                                .padding(vertical = 2.dp)
                        ) {
                            SubtaskBranchConnector(
                                isLastChild = isLast,
                                isFocused = isFocused,
                                modifier = Modifier
                                    .width(22.dp)
                                    .fillMaxHeight()
                            )

                            Spacer(modifier = Modifier.width(6.dp))

                            TextField(
                                value = subtask,
                                onValueChange = { newSubtask -> 
                                    val newSubtasks = subtasks.toMutableList()
                                    newSubtasks[index] = newSubtask
                                    subtasks = newSubtasks
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .focusRequester(subtaskFocusRequesters[index])
                                    .onFocusChanged { isFocused = it.isFocused },
                                placeholder = { 
                                    Text(
                                        "Subtask ${index + 1}",
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f)
                                    ) 
                                },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                                keyboardActions = KeyboardActions(
                                    onNext = {
                                        if (index == subtasks.lastIndex) {
                                            subtasks = subtasks + ""
                                            focusSubtaskIndexToRequest = index + 1
                                        } else {
                                            focusSubtaskIndexToRequest = index + 1
                                        }
                                    }
                                ),
                                shape = RoundedCornerShape(8.dp),
                                colors = TextFieldDefaults.colors(
                                    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                                    focusedIndicatorColor = Color.Transparent,
                                    unfocusedIndicatorColor = Color.Transparent
                                )
                            )

                            if (subtasks.size > 1) {
                                IconButton(
                                    onClick = {
                                        val newSubtasks = subtasks.toMutableList()
                                        newSubtasks.removeAt(index)
                                        if (index < subtaskFocusRequesters.size) {
                                            subtaskFocusRequesters.removeAt(index)
                                        }
                                        subtasks = newSubtasks
                                    },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Close,
                                        contentDescription = "Remove subtask",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }

                        if (!isLast) {
                            HorizontalDivider(
                                modifier = Modifier.padding(start = 28.dp),
                                thickness = 0.5.dp,
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 24.dp)
                    ) {
                        TextButton(onClick = { 
                            subtasks = subtasks + "" 
                            focusSubtaskIndexToRequest = subtasks.size
                        }) {
                            Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Add another subtask", style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
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
