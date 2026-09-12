package com.example.plannerapp.ui.home

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import androidx.compose.material.icons.outlined.*
import com.example.plannerapp.credits.CreditViewModel
import com.example.plannerapp.credits.CreditHubSheet
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
import com.example.plannerapp.theme.AppDimens
import com.example.plannerapp.ui.components.DurationPickerDialog
import com.example.plannerapp.ui.components.DurationPickerRow
import com.example.plannerapp.ui.components.PlanCardSkeleton
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
    creditViewModel: CreditViewModel? = null,
    feedViewModel: com.example.plannerapp.ui.social.CommunityFeedViewModel? = null,
    onNavigateToDiscussion: (String) -> Unit = {},
    onCreatorMonetizationClick: () -> Unit = {},
    onBecomeCreatorClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val isOnline = com.example.plannerapp.data.LocalIsOnline.current
    val uiStateResource by viewModel.uiState.collectAsState()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val coroutineScope = rememberCoroutineScope()

    val activeUser = creditViewModel?.activeUserFlow?.collectAsState()?.value
    val isCreator = activeUser?.isCreator == true

    val creditBalance = if (creditViewModel != null) {
        creditViewModel.balanceFlow.collectAsState().value
    } else 0

    val availableFreezes = if (creditViewModel != null) {
        creditViewModel.freezesFlow.collectAsState().value
    } else 0

    var showCreditHubSheet by remember { mutableStateOf(false) }
    var showProDialog by remember { mutableStateOf(false) }
    var isPlannerProExpanded by remember { mutableStateOf(true) }
    var isResourcesExpanded by remember { mutableStateOf(true) }
    var showAllRecentPlans by remember { mutableStateOf(false) }

    var searchQuery by remember { mutableStateOf("") }
    var isSearchActive by remember { mutableStateOf(false) }
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
            if (!isOnline && feedViewModel != null) return // Will render below if offline anyway
            // Skeleton loader — preserves layout, no blocking spinner
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                contentPadding = PaddingValues(AppDimens.Space16),
                horizontalArrangement = Arrangement.spacedBy(AppDimens.Space16),
                verticalArrangement = Arrangement.spacedBy(AppDimens.Space16),
                modifier = modifier.fillMaxSize()
            ) {
                items(4) { PlanCardSkeleton() }
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
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = "PlannerApp",
                                            style = MaterialTheme.typography.titleLarge,
                                            fontWeight = FontWeight.Bold
                                        )
                                        if (isCreator) {
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Surface(
                                                shape = RoundedCornerShape(8.dp),
                                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                                            ) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Outlined.Verified,
                                                        contentDescription = "Verified Creator",
                                                        tint = MaterialTheme.colorScheme.primary,
                                                        modifier = Modifier.size(12.dp)
                                                    )
                                                    Spacer(modifier = Modifier.width(3.dp))
                                                    Text(
                                                        text = "Creator",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        fontWeight = FontWeight.Bold,
                                                        color = MaterialTheme.colorScheme.primary
                                                    )
                                                }
                                            }
                                        }
                                    }
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

                    // ── Recently Visited (Image 2 style) ────────────────
                    if (uiState.plans.isNotEmpty()) {
                        item {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 20.dp, vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Recently Visited",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                TextButton(
                                    onClick = { showAllRecentPlans = !showAllRecentPlans },
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                                ) {
                                    Text(
                                        text = if (showAllRecentPlans) "Show less" else "See all",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }

                        val recentList = if (showAllRecentPlans) uiState.plans else uiState.plans.take(3)
                        items(recentList, key = { "recent_${it.planId}" }) { plan ->
                            val colorSeed = (plan.heading.hashCode() and 0x7FFFFFFF) % 4
                            val avatarColor = when (colorSeed) {
                                0 -> Color(0xFFE53935)
                                1 -> Color(0xFF1E88E5)
                                2 -> Color(0xFF43A047)
                                else -> Color(0xFFFB8C00)
                            }
                            NavigationDrawerItem(
                                label = {
                                    Text(
                                        text = plan.heading,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        fontWeight = FontWeight.Medium
                                    )
                                },
                                icon = {
                                    Box(
                                        modifier = Modifier
                                            .size(28.dp)
                                            .clip(CircleShape)
                                            .background(avatarColor),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = plan.heading.firstOrNull()?.uppercase() ?: "P",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                    }
                                },
                                selected = false,
                                onClick = {
                                    coroutineScope.launch { drawerState.close() }
                                    onPlanClick(plan.planId)
                                },
                                modifier = Modifier.padding(horizontal = 12.dp)
                            )
                        }

                        item {
                            Spacer(modifier = Modifier.height(4.dp))
                            HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
                            Spacer(modifier = Modifier.height(4.dp))
                        }
                    }

                    // ── Planner Pro Section (Image 1 style) ───────────────
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { isPlannerProExpanded = !isPlannerProExpanded }
                                .padding(horizontal = 20.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Planner Pro",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Icon(
                                imageVector = if (isPlannerProExpanded) Icons.Outlined.KeyboardArrowUp else Icons.Outlined.KeyboardArrowDown,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    if (isPlannerProExpanded) {
                        item {
                            NavigationDrawerItem(
                                label = { Text("Try Planner Pro", fontWeight = FontWeight.Medium) },
                                icon = { Icon(Icons.Outlined.ChatBubbleOutline, contentDescription = null) },
                                selected = false,
                                onClick = {
                                    coroutineScope.launch { drawerState.close() }
                                    showProDialog = true
                                },
                                modifier = Modifier.padding(horizontal = 12.dp)
                            )
                        }
                    }

                    item {
                        Spacer(modifier = Modifier.height(4.dp))
                        HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
                        Spacer(modifier = Modifier.height(4.dp))
                    }

                    // ── Resources Section (Image 1 style) ─────────────────
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { isResourcesExpanded = !isResourcesExpanded }
                                .padding(horizontal = 20.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Resources",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Icon(
                                imageVector = if (isResourcesExpanded) Icons.Outlined.KeyboardArrowUp else Icons.Outlined.KeyboardArrowDown,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    if (isResourcesExpanded) {
                        item {
                            NavigationDrawerItem(
                                label = {
                                    Column {
                                        Text("Planner Premium", fontWeight = FontWeight.Medium)
                                        Text(
                                            "Ads-free & unlimited cloud sync",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                },
                                icon = { Icon(Icons.Outlined.Shield, contentDescription = null) },
                                selected = false,
                                onClick = {
                                    coroutineScope.launch { drawerState.close() }
                                    showProDialog = true
                                },
                                modifier = Modifier.padding(horizontal = 12.dp)
                            )

                            NavigationDrawerItem(
                                label = {
                                    Column {
                                        Text("Earn", fontWeight = FontWeight.Medium)
                                        Text(
                                            "Earn credits on PlannerApp",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                },
                                icon = { Icon(Icons.Outlined.Token, contentDescription = null) },
                                selected = false,
                                onClick = {
                                    coroutineScope.launch { drawerState.close() }
                                    showCreditHubSheet = true
                                },
                                modifier = Modifier.padding(horizontal = 12.dp)
                            )

                            NavigationDrawerItem(
                                label = {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text("Planner Store", fontWeight = FontWeight.Medium)
                                            Text(
                                                "Get credits & streak freezes",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        Box(
                                            modifier = Modifier
                                                .size(8.dp)
                                                .clip(CircleShape)
                                                .background(Color(0xFFFF5722))
                                        )
                                    }
                                },
                                icon = { Icon(Icons.Outlined.Storefront, contentDescription = null) },
                                selected = false,
                                onClick = {
                                    coroutineScope.launch { drawerState.close() }
                                    showCreditHubSheet = true
                                },
                                modifier = Modifier.padding(horizontal = 12.dp)
                            )
                        }
                    }

                    item {
                        Spacer(modifier = Modifier.height(4.dp))
                        HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
                        Spacer(modifier = Modifier.height(4.dp))
                    }

                    // ── Creator Section ──────────────────────────────────
                    item {
                        Text(
                            text = if (isCreator) "Creator Studio" else "Creator Program",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
                        )

                        if (isCreator) {
                            NavigationDrawerItem(
                                label = {
                                    Column {
                                        Text("Monetization & Payouts", fontWeight = FontWeight.SemiBold)
                                        Text(
                                            "70% earnings split & cash-outs",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                },
                                icon = {
                                    Icon(
                                        imageVector = Icons.Outlined.MonetizationOn,
                                        contentDescription = null,
                                        tint = Color(0xFF2E7D32)
                                    )
                                },
                                selected = false,
                                onClick = {
                                    coroutineScope.launch { drawerState.close() }
                                    onCreatorMonetizationClick()
                                },
                                modifier = Modifier.padding(horizontal = 12.dp)
                            )
                        } else {
                            NavigationDrawerItem(
                                label = {
                                    Column {
                                        Text("Become a Creator", fontWeight = FontWeight.SemiBold)
                                        Text(
                                            "Monetize plans & earn credits",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                },
                                icon = {
                                    Icon(
                                        imageVector = Icons.Outlined.Star,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                },
                                selected = false,
                                onClick = {
                                    coroutineScope.launch { drawerState.close() }
                                    onBecomeCreatorClick()
                                },
                                modifier = Modifier.padding(horizontal = 12.dp)
                            )
                        }
                    }

                    item {
                        Spacer(modifier = Modifier.height(4.dp))
                        HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
                        Spacer(modifier = Modifier.height(4.dp))
                    }

                    // ── Discover & Feeds ──────────────────────────────────
                    item {
                        if (isOnline) {
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
                        }

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

                    // Preferences
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
                    // WhatsApp-style Minimalist Top Bar
                    Surface(
                        color = MaterialTheme.colorScheme.surface,
                        tonalElevation = 0.dp
                    ) {
                        Column {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (isSearchActive) {
                                    // WhatsApp-style active search bar
                                    IconButton(
                                        onClick = {
                                            isSearchActive = false
                                            searchQuery = ""
                                        }
                                    ) {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                            contentDescription = "Close Search",
                                            tint = MaterialTheme.colorScheme.onSurface
                                        )
                                    }

                                    TextField(
                                        value = searchQuery,
                                        onValueChange = { searchQuery = it },
                                        placeholder = {
                                            Text(
                                                text = if (isOnline) "Search community plans..." else "Search local plans...",
                                                style = MaterialTheme.typography.bodyLarge,
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
                                        modifier = Modifier.weight(1f)
                                    )

                                    if (searchQuery.isNotBlank()) {
                                        IconButton(onClick = { searchQuery = "" }) {
                                            Icon(
                                                imageVector = Icons.Filled.Close,
                                                contentDescription = "Clear",
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                } else {
                                    // WhatsApp-style header: Menu + Brand Title + Actions
                                    IconButton(
                                        onClick = { coroutineScope.launch { drawerState.open() } }
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.Menu,
                                            contentDescription = "Main Menu",
                                            tint = MaterialTheme.colorScheme.onSurface
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(4.dp))

                                    Text(
                                        text = "Planner",
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )

                                    Spacer(modifier = Modifier.weight(1f))

                                    if (!isOnline) {
                                        Icon(
                                            imageVector = Icons.Outlined.CloudOff,
                                            contentDescription = "Offline Mode",
                                            tint = MaterialTheme.colorScheme.error,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                    }

                                    IconButton(onClick = { isSearchActive = true }) {
                                        Icon(
                                            imageVector = Icons.Filled.Search,
                                            contentDescription = "Search",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }

                                    if (creditViewModel != null) {
                                        Spacer(modifier = Modifier.width(6.dp))

                                        // Plain Minimalist Credit Badge: No diamond icon, no separate freezes pill
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                            border = BorderStroke(
                                                1.dp,
                                                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
                                            ),
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(8.dp))
                                                .clickable { showCreditHubSheet = true }
                                        ) {
                                            Text(
                                                text = "$creditBalance cr",
                                                style = MaterialTheme.typography.labelMedium,
                                                fontWeight = FontWeight.SemiBold,
                                                color = MaterialTheme.colorScheme.onSurface,
                                                modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp)
                                            )
                                        }
                                    }
                                }
                            }

                            HorizontalDivider(
                                thickness = 0.5.dp,
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f)
                            )
                        }
                    }
                }
            },
            floatingActionButton = {
                if (!isSelectionMode) {
                    FloatingActionButton(
                        onClick = { showCreateDialog = true },
                        shape = RoundedCornerShape(16.dp),
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                        elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 2.dp)
                    ) {
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
                if (isOnline && feedViewModel != null) {
                    com.example.plannerapp.ui.explore.ExploreScreen(
                        viewModel = feedViewModel,
                        onPostClick = { postId -> onNavigateToDiscussion(postId) },
                        onCreatorClick = {}
                    )
                } else {
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
                                shape = RoundedCornerShape(AppDimens.CornerCard),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier.padding(AppDimens.Space24)
                                ) {
                                    Surface(
                                        shape = CircleShape,
                                        color = MaterialTheme.colorScheme.primaryContainer,
                                        modifier = Modifier.size(AppDimens.IconBoxLg)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                imageVector = Icons.Outlined.Checklist,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(AppDimens.IconSizeXl)
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(AppDimens.Space16))
                                    Text(
                                        text = "Your Journey Starts Here",
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Spacer(modifier = Modifier.height(AppDimens.Space8))
                                    Text(
                                        text = "Create your first routine, challenge, or project plan to start building unstoppable daily habits.",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        textAlign = TextAlign.Center
                                    )
                                    Spacer(modifier = Modifier.height(AppDimens.Space20))
                                    Button(
                                        onClick = { showCreateDialog = true },
                                        shape = RoundedCornerShape(AppDimens.CornerCompact)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.Add,
                                            contentDescription = null,
                                            modifier = Modifier.size(AppDimens.IconSizeMd)
                                        )
                                        Spacer(modifier = Modifier.width(AppDimens.Space8))
                                        Text("Create Your First Plan", fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        contentPadding = PaddingValues(AppDimens.Space16),
                        horizontalArrangement = Arrangement.spacedBy(AppDimens.Space16),
                        verticalArrangement = Arrangement.spacedBy(AppDimens.Space16)
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

    if (showCreditHubSheet && creditViewModel != null) {
        CreditHubSheet(
            viewModel = creditViewModel,
            onDismissRequest = { showCreditHubSheet = false }
        )
    }

    if (showProDialog) {
        AlertDialog(
            onDismissRequest = { showProDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.Outlined.WorkspacePremium,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(32.dp)
                )
            },
            title = {
                Text(
                    text = "Planner Pro",
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Supercharge your productivity with Pro features:",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    ProBenefitRow(
                        icon = Icons.Outlined.CloudSync,
                        title = "Unlimited Cloud Sync",
                        subtitle = "Sync routines across all your devices seamlessly"
                    )
                    ProBenefitRow(
                        icon = Icons.Outlined.AutoGraph,
                        title = "Advanced Analytics",
                        subtitle = "Deep-dive charts and habit consistency metrics"
                    )
                    ProBenefitRow(
                        icon = Icons.Outlined.Storefront,
                        title = "Creator Marketplace",
                        subtitle = "Publish paid plans and earn credits from forks"
                    )
                    ProBenefitRow(
                        icon = Icons.Outlined.Bolt,
                        title = "Streak Multipliers",
                        subtitle = "Bonus credits earned for consistency milestones"
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showProDialog = false
                        showCreditHubSheet = true
                    }
                ) {
                    Text("Visit Store")
                }
            },
            dismissButton = {
                TextButton(onClick = { showProDialog = false }) {
                    Text("Maybe Later")
                }
            }
        )
    }
    }
}

@Composable
fun ProBenefitRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

