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

