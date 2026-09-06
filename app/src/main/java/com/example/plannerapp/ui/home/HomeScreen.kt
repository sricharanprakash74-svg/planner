package com.example.plannerapp.ui.home

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.TrendingUp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.Checklist
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material.icons.outlined.Public
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Settings
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.plannerapp.data.PlanEntity
import com.example.plannerapp.data.PlannerRepository
import com.example.plannerapp.data.UserDao
import com.example.plannerapp.data.social.SocialRepository
import com.example.plannerapp.ui.components.DurationPickerDialog
import com.example.plannerapp.ui.components.DurationPickerRow
import com.example.plannerapp.ui.social.PublishPlanDialog
import com.example.plannerapp.ui.state.Resource
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onPlanClick: (Long) -> Unit,
    onPlanCreatedAndOpen: (Long) -> Unit = {},
    onAnalyticsClick: () -> Unit = {},
    onExploreClick: (String) -> Unit = {},
    onSettingsClick: () -> Unit = {},
    plannerRepository: PlannerRepository? = null,
    socialRepository: SocialRepository? = null,
    userDao: UserDao? = null,
    onNavigateToDiscussion: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val uiStateResource by viewModel.uiState.collectAsState()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val coroutineScope = rememberCoroutineScope()

    var searchQuery by remember { mutableStateOf("") }
    var showCreateDialog by remember { mutableStateOf(false) }
    var planToEdit by remember { mutableStateOf<PlanEntity?>(null) }
    var planToDelete by remember { mutableStateOf<PlanEntity?>(null) }
    var planForInfo by remember { mutableStateOf<PlanEntity?>(null) }
    var planForActions by remember { mutableStateOf<PlanEntity?>(null) }
    var planToPublish by remember { mutableStateOf<PlanEntity?>(null) }

    // Multi-Select State
    var isSelectionMode by remember { mutableStateOf(false) }
    var selectedPlanIds by remember { mutableStateOf(setOf<Long>()) }
    var showBatchDeleteDialog by remember { mutableStateOf(false) }

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

    val filteredPlans = remember(uiState.plans, searchQuery) {
        if (searchQuery.isBlank()) {
            uiState.plans
        } else {
            uiState.plans.filter {
                it.heading.contains(searchQuery, ignoreCase = true) ||
                it.description.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    val pinnedPlans = remember(uiState.plans) {
        uiState.plans.filter { it.isPinned }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                modifier = Modifier.width(310.dp),
                drawerContainerColor = MaterialTheme.colorScheme.surface
            ) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(vertical = 16.dp)
                ) {
                    // Header Brand
                    item {
                        Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.primary),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.Folder,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onPrimary,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = "PlannerApp",
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "${uiState.plans.size} Active Plans",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    // Main Feeds / Discover Section
                    item {
                        Text(
                            text = "Discover & Feeds",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
                        )

                        NavigationDrawerItem(
                            label = { Text("Popular & Community Plans", fontWeight = FontWeight.SemiBold) },
                            icon = { Icon(Icons.Outlined.Explore, contentDescription = null) },
                            selected = false,
                            onClick = {
                                coroutineScope.launch { drawerState.close() }
                                onExploreClick("")
                            },
                            modifier = Modifier.padding(horizontal = 12.dp)
                        )

                        NavigationDrawerItem(
                            label = { Text("Performance & Stock Graph", fontWeight = FontWeight.SemiBold) },
                            icon = { Icon(Icons.AutoMirrored.Outlined.TrendingUp, contentDescription = null) },
                            selected = false,
                            onClick = {
                                coroutineScope.launch { drawerState.close() }
                                onAnalyticsClick()
                            },
                            modifier = Modifier.padding(horizontal = 12.dp)
                        )
                    }

                    // Pinned Plans Section (if any)
                    if (pinnedPlans.isNotEmpty()) {
                        item {
                            Spacer(modifier = Modifier.height(8.dp))
                            HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = "Pinned Plans",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
                            )
                        }

                        items(pinnedPlans, key = { "pinned_${it.planId}" }) { plan ->
                            NavigationDrawerItem(
                                label = {
                                    Text(
                                        text = plan.heading,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                },
                                icon = {
                                    Icon(
                                        imageVector = Icons.Filled.PushPin,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                },
                                selected = false,
                                onClick = {
                                    coroutineScope.launch { drawerState.close() }
                                    onPlanClick(plan.planId)
                                },
                                modifier = Modifier.padding(horizontal = 12.dp)
                            )
                        }
                    }

                    // Settings & Resources Section
                    item {
                        Spacer(modifier = Modifier.height(8.dp))
                        HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "Preferences",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
                        )

                        NavigationDrawerItem(
                            label = { Text("Settings", fontWeight = FontWeight.SemiBold) },
                            icon = { Icon(Icons.Outlined.Settings, contentDescription = null) },
                            selected = false,
                            onClick = {
                                coroutineScope.launch { drawerState.close() }
                                onSettingsClick()
                            },
                            modifier = Modifier.padding(horizontal = 12.dp)
                        )
                    }
                }
            }
        }
    ) {
        Scaffold(
            modifier = modifier,
            topBar = {
                if (isSelectionMode) {
                    TopAppBar(
                        title = { Text("${selectedPlanIds.size} Selected", fontWeight = FontWeight.Bold) },
                        navigationIcon = {
                            IconButton(onClick = {
                                isSelectionMode = false
                                selectedPlanIds = emptySet()
                            }) {
                                Icon(Icons.Filled.Close, contentDescription = "Exit Selection")
                            }
                        },
                        actions = {
                            if (selectedPlanIds.isNotEmpty()) {
                                IconButton(onClick = {
                                    viewModel.batchPinPlans(selectedPlanIds.toList(), true)
                                    isSelectionMode = false
                                    selectedPlanIds = emptySet()
                                }) {
                                    Icon(Icons.Filled.PushPin, contentDescription = "Pin Selected")
                                }
                                IconButton(onClick = { showBatchDeleteDialog = true }) {
                                    Icon(Icons.Filled.Delete, contentDescription = "Delete Selected", tint = MaterialTheme.colorScheme.error)
                                }
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    )
                } else {
                    // Reddit-inspired Top Bar with Hamburger Menu and Pill Search Bar
                    Surface(
                        color = MaterialTheme.colorScheme.surface,
                        tonalElevation = 1.dp
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Hamburger Menu Icon
                            IconButton(
                                onClick = { coroutineScope.launch { drawerState.open() } }
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Menu,
                                    contentDescription = "Main Menu",
                                    tint = MaterialTheme.colorScheme.onSurface
                                )
                            }

                            Spacer(modifier = Modifier.width(6.dp))

                            // Pill-shaped Search Bar
                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(44.dp),
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(horizontal = 14.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Search,
                                        contentDescription = "Search",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))

                                    TextField(
                                        value = searchQuery,
                                        onValueChange = { searchQuery = it },
                                        placeholder = {
                                            Text(
                                                text = "Search plans or community...",
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        },
                                        colors = TextFieldDefaults.colors(
                                            focusedContainerColor = Color.Transparent,
                                            unfocusedContainerColor = Color.Transparent,
                                            disabledContainerColor = Color.Transparent,
                                            focusedIndicatorColor = Color.Transparent,
                                            unfocusedIndicatorColor = Color.Transparent
                                        ),
                                        singleLine = true,
                                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                                            imeAction = androidx.compose.ui.text.input.ImeAction.Search
                                        ),
                                        keyboardActions = androidx.compose.foundation.text.KeyboardActions(
                                            onSearch = {
                                                if (searchQuery.isNotBlank()) {
                                                    onExploreClick(searchQuery)
                                                }
                                            }
                                        ),
                                        modifier = Modifier.weight(1f)
                                    )

                                    if (searchQuery.isNotBlank()) {
                                        // Navigate to Explore with the typed query
                                        IconButton(
                                            onClick = { onExploreClick(searchQuery) },
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Filled.Search,
                                                contentDescription = "Search Community",
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                        IconButton(
                                            onClick = { searchQuery = "" },
                                            modifier = Modifier.size(24.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Filled.Close,
                                                contentDescription = "Clear",
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            },
            floatingActionButton = {
                if (!isSelectionMode) {
                    FloatingActionButton(onClick = { showCreateDialog = true }) {
                        Icon(Icons.Filled.Add, contentDescription = "Create Plan")
                    }
                }
            },
            containerColor = MaterialTheme.colorScheme.background
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                if (filteredPlans.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp), 
                        contentAlignment = Alignment.Center
                    ) {
                        if (searchQuery.isNotBlank()) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector = Icons.Filled.Search,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                    modifier = Modifier.size(48.dp)
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = "No plans matching \"$searchQuery\"",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "Try searching for another keyword or check community plans.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                OutlinedButton(onClick = { searchQuery = "" }) {
                                    Text("Clear Search")
                                }
                            }
                        } else {
                            Card(
                                shape = RoundedCornerShape(20.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier.padding(24.dp)
                                ) {
                                    Surface(
                                        shape = CircleShape,
                                        color = MaterialTheme.colorScheme.primaryContainer,
                                        modifier = Modifier.size(64.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                imageVector = Icons.Outlined.Checklist,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(32.dp)
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Text(
                                        text = "Your Journey Starts Here",
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "Create your first routine, challenge, or project plan to start building unstoppable daily habits.",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        textAlign = TextAlign.Center
                                    )
                                    Spacer(modifier = Modifier.height(20.dp))
                                    Button(
                                        onClick = { showCreateDialog = true },
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.Add,
                                            contentDescription = null,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Create Your First Plan", fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        contentPadding = PaddingValues(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        items(filteredPlans, key = { it.planId }) { plan ->
                            val isSelected = selectedPlanIds.contains(plan.planId)
                            PlanFolderCard(
                                plan = plan, 
                                isSelectionMode = isSelectionMode,
                                isSelected = isSelected,
                                onClick = { 
                                    if (isSelectionMode) {
                                        selectedPlanIds = if (isSelected) selectedPlanIds - plan.planId else selectedPlanIds + plan.planId
                                    } else {
                                        onPlanClick(plan.planId)
                                    }
                                },
                                onLongClick = {
                                    if (isSelectionMode) {
                                        selectedPlanIds = if (isSelected) selectedPlanIds - plan.planId else selectedPlanIds + plan.planId
                                    } else {
                                        planForActions = plan
                                    }
                                },
                                onEdit = { planToEdit = plan },
                                onDelete = { planToDelete = plan },
                                onInfo = { planForInfo = plan },
                                onTogglePin = { viewModel.togglePinPlan(plan.planId, !plan.isPinned) }
                            )
                        }
                    }
                }
            }
        }
    }

    // Plan Actions Bottom Sheet (from Long-Press)
    planForActions?.let { plan ->
        ModalBottomSheet(
            onDismissRequest = { planForActions = null }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
                    .padding(bottom = 24.dp)
            ) {
                Text(
                    text = plan.heading,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(16.dp))

                ListItem(
                    headlineContent = { Text(if (plan.isPinned) "Unpin Plan" else "Pin to Top") },
                    leadingContent = { 
                        Icon(
                            if (plan.isPinned) Icons.Filled.PushPin else Icons.Outlined.PushPin, 
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        ) 
                    },
                    modifier = Modifier.clickable {
                        viewModel.togglePinPlan(plan.planId, !plan.isPinned)
                        planForActions = null
                    }
                )

                ListItem(
                    headlineContent = { Text("Project Details & Info") },
                    leadingContent = { Icon(Icons.Outlined.Info, contentDescription = null) },
                    modifier = Modifier.clickable {
                        val currentPlan = plan
                        planForActions = null
                        planForInfo = currentPlan
                    }
                )

                ListItem(
                    headlineContent = { Text("Publish to Community") },
                    leadingContent = { Icon(Icons.Outlined.Public, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                    modifier = Modifier.clickable {
                        val currentPlan = plan
                        planForActions = null
                        planToPublish = currentPlan
                    }
                )

                ListItem(
                    headlineContent = { Text("Edit Plan") },
                    leadingContent = { Icon(Icons.Outlined.Edit, contentDescription = null) },
                    modifier = Modifier.clickable {
                        val currentPlan = plan
                        planForActions = null
                        planToEdit = currentPlan
                    }
                )

                ListItem(
                    headlineContent = { Text("Select Multiple") },
                    leadingContent = { Icon(Icons.Outlined.Checklist, contentDescription = null) },
                    modifier = Modifier.clickable {
                        isSelectionMode = true
                        selectedPlanIds = setOf(plan.planId)
                        planForActions = null
                    }
                )

                ListItem(
                    headlineContent = { Text("Delete Plan", color = MaterialTheme.colorScheme.error) },
                    leadingContent = { Icon(Icons.Outlined.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                    modifier = Modifier.clickable {
                        val currentPlan = plan
                        planForActions = null
                        planToDelete = currentPlan
                    }
                )
            }
        }
    }

    // Plan Info Dialog (showing description and full details)
    planForInfo?.let { plan ->
        PlanInfoDialog(
            plan = plan,
            onDismiss = { planForInfo = null }
        )
    }

    planToPublish?.let { plan ->
        if (plannerRepository != null && socialRepository != null && userDao != null) {
            PublishPlanDialog(
                plan = plan,
                plannerRepository = plannerRepository,
                socialRepository = socialRepository,
                userDao = userDao,
                onDismiss = { planToPublish = null },
                onPublished = { newPostId ->
                    planToPublish = null
                    onNavigateToDiscussion(newPostId)
                }
            )
        }
    }

    if (showCreateDialog) {
        PlanFormDialog(
            title = "New Plan Folder",
            initialName = "",
            initialDescription = "",
            initialDurationDays = 7,
            initialMakeDefault = true,
            initialReminderEnabled = false,
            initialReminderTime = "08:00",
            confirmText = "Create Plan",
            onDismiss = { showCreateDialog = false },
            onSave = { name, desc, durationDays, defaultTaskDuration, reminderEnabled, reminderTime ->
                viewModel.createQuickPlan(
                    title = name,
                    description = desc,
                    durationDays = durationDays,
                    defaultTaskDurationDays = defaultTaskDuration,
                    reminderEnabled = reminderEnabled,
                    reminderTime = reminderTime,
                    onCreated = { newPlanId ->
                        showCreateDialog = false
                        onPlanCreatedAndOpen(newPlanId)
                    }
                )
                showCreateDialog = false
            }
        )
    }

    planToEdit?.let { plan ->
        val startDate = try { LocalDate.parse(plan.startDate) } catch (e: Exception) { LocalDate.now() }
        val endDate = try { LocalDate.parse(plan.endDate) } catch (e: Exception) { startDate.plusDays(29) }
        val currentDays = (ChronoUnit.DAYS.between(startDate, endDate) + 1).coerceAtLeast(1).toInt()

        PlanFormDialog(
            title = "Edit Plan",
            initialName = plan.heading,
            initialDescription = plan.description,
            initialDurationDays = currentDays,
            initialMakeDefault = plan.defaultTaskDurationDays == currentDays,
            initialReminderEnabled = plan.reminderEnabled,
            initialReminderTime = plan.reminderTime ?: "08:00",
            confirmText = "Save Changes",
            onDismiss = { planToEdit = null },
            onSave = { name, desc, durationDays, defaultTaskDuration, reminderEnabled, reminderTime ->
                val newStart = startDate.format(DateTimeFormatter.ISO_LOCAL_DATE)
                val newEnd = startDate.plusDays((durationDays - 1).coerceAtLeast(0).toLong()).format(DateTimeFormatter.ISO_LOCAL_DATE)
                viewModel.updatePlan(
                    planId = plan.planId,
                    heading = name,
                    description = desc,
                    startDate = newStart,
                    endDate = newEnd,
                    defaultTaskDurationDays = defaultTaskDuration,
                    reminderEnabled = reminderEnabled,
                    reminderTime = reminderTime
                )
                planToEdit = null
            }
        )
    }

    planToDelete?.let { plan ->
        AlertDialog(
            onDismissRequest = { planToDelete = null },
            title = { Text("Delete Plan") },
            text = { Text("Are you sure you want to delete \"${plan.heading}\"? All tasks inside will be permanently deleted.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deletePlan(plan.planId)
                        planToDelete = null
                    }
                ) {
                    Text("Delete", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { planToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showBatchDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showBatchDeleteDialog = false },
            title = { Text("Delete Selected Plans") },
            text = { Text("Are you sure you want to delete ${selectedPlanIds.size} plans? All tasks inside will be permanently deleted.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.batchDeletePlans(selectedPlanIds.toList())
                        selectedPlanIds = emptySet()
                        isSelectionMode = false
                        showBatchDeleteDialog = false
                    }
                ) {
                    Text("Delete All", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showBatchDeleteDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PlanFolderCard(
    plan: PlanEntity,
    isSelectionMode: Boolean,
    isSelected: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onInfo: () -> Unit,
    onTogglePin: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    var menuExpanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .combinedClickable(
                onClick = onClick,
                onLongClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onLongClick()
                }
            ),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
            else MaterialTheme.colorScheme.primaryContainer
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(14.dp)
        ) {
            // Top row: folder icon + pin badge + menu/checkbox
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Outlined.Folder,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(28.dp)
                    )
                    if (plan.isPinned) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.Filled.PushPin,
                            contentDescription = "Pinned",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }

                if (isSelectionMode) {
                    Checkbox(
                        checked = isSelected,
                        onCheckedChange = { onClick() }
                    )
                } else {
                    Box {
                        IconButton(
                            onClick = { menuExpanded = true },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.MoreVert,
                                contentDescription = "Options",
                                tint = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }

                        DropdownMenu(
                            expanded = menuExpanded,
                            onDismissRequest = { menuExpanded = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text(if (plan.isPinned) "Unpin" else "Pin to Top") },
                                leadingIcon = { Icon(Icons.Outlined.PushPin, contentDescription = null) },
                                onClick = {
                                    menuExpanded = false
                                    onTogglePin()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Project Details") },
                                leadingIcon = { Icon(Icons.Outlined.Info, contentDescription = null) },
                                onClick = {
                                    menuExpanded = false
                                    onInfo()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Edit Plan") },
                                leadingIcon = { Icon(Icons.Outlined.Edit, contentDescription = null) },
                                onClick = {
                                    menuExpanded = false
                                    onEdit()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Delete Plan", color = MaterialTheme.colorScheme.error) },
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

            Spacer(modifier = Modifier.height(10.dp))

            // Plan title
            Text(
                text = plan.heading,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(6.dp))

            // Formatted date range
            val cardDateFormatter = DateTimeFormatter.ofPattern("MMM d")
            val startDate = try { LocalDate.parse(plan.startDate) } catch (e: Exception) { LocalDate.now() }
            val endDate = try { LocalDate.parse(plan.endDate) } catch (e: Exception) { startDate }
            val totalDays = (ChronoUnit.DAYS.between(startDate, endDate) + 1).coerceAtLeast(1)
            val daysElapsed = ChronoUnit.DAYS.between(startDate, LocalDate.now()).coerceIn(0L, totalDays)
            val progressFraction = (daysElapsed.toFloat() / totalDays.toFloat()).coerceIn(0f, 1f)

            Text(
                text = "${startDate.format(cardDateFormatter)} – ${endDate.format(cardDateFormatter)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.65f)
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Days-elapsed progress bar
            LinearProgressIndicator(
                progress = { progressFraction },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp)),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.15f)
            )

            Spacer(modifier = Modifier.height(6.dp))

            // Bottom row: day count + reminder badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Day $daysElapsed / $totalDays",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.6f)
                )
                if (plan.reminderEnabled) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Outlined.NotificationsActive,
                            contentDescription = "Daily Reminder",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(2.dp))
                        Text(
                            text = formatReminderTime(plan.reminderTime),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun PlanInfoDialog(
    plan: PlanEntity,
    onDismiss: () -> Unit
) {
    val startDate = try { LocalDate.parse(plan.startDate) } catch (e: Exception) { LocalDate.now() }
    val endDate = try { LocalDate.parse(plan.endDate) } catch (e: Exception) { startDate.plusDays(29) }
    val durationDays = (ChronoUnit.DAYS.between(startDate, endDate) + 1).coerceAtLeast(1)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Outlined.Info,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Project Information", fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Column {
                    Text("Plan Name", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(plan.heading, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }

                if (plan.description.isNotBlank()) {
                    Column {
                        Text("Description", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(plan.description, style = MaterialTheme.typography.bodyMedium)
                    }
                } else {
                    Column {
                        Text("Description", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("No description provided.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f))
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column {
                        Text("Duration", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("$durationDays Days", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                    }
                    Column {
                        Text("Default Task Length", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("${plan.defaultTaskDurationDays} Days", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                    }
                }

                Column {
                    Text("Timeline Span", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("${plan.startDate} to ${plan.endDate}", style = MaterialTheme.typography.bodyMedium)
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (plan.reminderEnabled) Icons.Outlined.NotificationsActive else Icons.Outlined.Notifications,
                        contentDescription = null,
                        tint = if (plan.reminderEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text("Daily Reminder", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            if (plan.reminderEnabled) "Active at ${formatReminderTime(plan.reminderTime)}" else "Disabled",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
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
    confirmText: String,
    onDismiss: () -> Unit,
    onSave: (name: String, description: String, durationDays: Int, defaultTaskDuration: Int, reminderEnabled: Boolean, reminderTime: String?) -> Unit
) {
    var text by remember { mutableStateOf(initialName) }
    var description by remember { mutableStateOf(initialDescription) }
    var durationDays by remember { mutableIntStateOf(initialDurationDays) }
    var makeDefault by remember { mutableStateOf(initialMakeDefault) }
    var reminderEnabled by remember { mutableStateOf(initialReminderEnabled) }
    var reminderTime by remember { mutableStateOf(initialReminderTime) }
    var showDurationDialog by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, fontWeight = FontWeight.Bold) },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    label = { Text("Plan Name") },
                    placeholder = { Text("e.g., Morning Routine") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description (Optional)") },
                    placeholder = { Text("Add notes or context for this plan") },
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
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
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    )
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
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
                                Spacer(modifier = Modifier.width(8.dp))
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
                            Spacer(modifier = Modifier.height(10.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                val presets = listOf("08:00" to "8:00 AM", "12:00" to "12:00 PM", "20:00" to "8:00 PM")
                                presets.forEach { (timeVal, label) ->
                                    FilterChip(
                                        selected = reminderTime == timeVal,
                                        onClick = { reminderTime = timeVal },
                                        label = { Text(label, fontSize = 11.sp) }
                                    )
                                }
                                val isCustom = presets.none { it.first == reminderTime }
                                FilterChip(
                                    selected = isCustom,
                                    onClick = { showTimePicker = true },
                                    label = {
                                        Text(if (isCustom) formatReminderTime(reminderTime) else "Custom", fontSize = 11.sp)
                                    }
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (text.isNotBlank()) {
                        val defaultTaskDur = if (makeDefault) durationDays else 1
                        onSave(text, description, durationDays, defaultTaskDur, reminderEnabled, if (reminderEnabled) reminderTime else null)
                    }
                },
                enabled = text.isNotBlank()
            ) {
                Text(confirmText, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )

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

private fun formatReminderTime(timeStr: String?): String {
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
