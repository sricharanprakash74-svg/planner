package com.example.plannerapp.ui.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.ui.graphics.graphicsLayer
import com.example.plannerapp.theme.PhysicsSpec
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
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
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import com.example.plannerapp.ui.components.CommunityPostSkeleton
import com.example.plannerapp.data.PlanEntity
import com.example.plannerapp.data.PlannerRepository
import com.example.plannerapp.data.UserDao
import com.example.plannerapp.data.social.SocialRepository
import com.example.plannerapp.theme.AppDimens
import com.example.plannerapp.ui.components.DurationPickerDialog
import com.example.plannerapp.ui.components.DurationPickerRow
import com.example.plannerapp.ui.components.PlanCardSkeleton
import com.example.plannerapp.data.social.FeedFilter
import com.example.plannerapp.data.social.VoteType
import com.example.plannerapp.ui.explore.CreatorSearchResultCard
import com.example.plannerapp.ui.explore.SocialFeedPostCard
import com.example.plannerapp.ui.social.PublishPlanDialog
import com.example.plannerapp.ui.state.Resource
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

enum class HomeFeedTab(val label: String) {
    MY_PLANS("My Routines"),
    FOR_YOU("For You"),
    FOLLOWING("Following"),
    POPULAR("Popular"),
    RECENT("Recent")
}

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
    onPostClick: (String) -> Unit = {},
    onCreatorClick: (String) -> Unit = {},
    onCreatorMonetizationClick: () -> Unit = {},
    onBecomeCreatorClick: () -> Unit = {},
    onNotificationsClick: () -> Unit = {},
    onConversationsClick: () -> Unit = {},
    onMenuClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val isOnline = com.example.plannerapp.data.LocalIsOnline.current
    val uiStateResource by viewModel.uiState.collectAsStateWithLifecycle()
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val activeUser = creditViewModel?.activeUserFlow?.collectAsStateWithLifecycle()?.value
    val isCreator = activeUser?.isCreator == true

    val creditBalance = if (creditViewModel != null) {
        creditViewModel.balanceFlow.collectAsStateWithLifecycle().value
    } else 0

    val availableFreezes = if (creditViewModel != null) {
        creditViewModel.freezesFlow.collectAsStateWithLifecycle().value
    } else 0

    var showCreditHubSheet by remember { mutableStateOf(false) }
    var showCreatorWaitlistSheet by remember { mutableStateOf(false) }
    var showProDialog by remember { mutableStateOf(false) }
    var isPlannerProExpanded by remember { mutableStateOf(true) }
    var isResourcesExpanded by remember { mutableStateOf(true) }
    var showAllRecentPlans by remember { mutableStateOf(false) }

    var searchQuery by remember { mutableStateOf("") }
    var isSearchActive by remember { mutableStateOf(false) }
    val searchFocusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    LaunchedEffect(isSearchActive) {
        if (isSearchActive) {
            searchFocusRequester.requestFocus()
            keyboardController?.show()
        }
    }

    LaunchedEffect(viewModel) {
        viewModel.uiEvents.collect { event ->
            when (event) {
                is HomeUiEvent.ShowSnackbar -> {
                    coroutineScope.launch {
                        val result = snackbarHostState.showSnackbar(
                            message = event.message,
                            actionLabel = event.actionLabel,
                            duration = SnackbarDuration.Short
                        )
                        if (result == SnackbarResult.ActionPerformed) {
                            event.onAction?.invoke()
                        }
                    }
                }
            }
        }
    }

    var selectedFeedTab by remember { mutableStateOf(HomeFeedTab.MY_PLANS) }
    val activeFeedTab = if (isOnline) selectedFeedTab else HomeFeedTab.MY_PLANS
    val feedUiState by (feedViewModel?.uiState?.collectAsStateWithLifecycle() ?: remember { mutableStateOf(com.example.plannerapp.ui.social.CommunityFeedUiState()) })

    LaunchedEffect(selectedFeedTab, isOnline) {
        if (isOnline && feedViewModel != null) {
            when (selectedFeedTab) {
                HomeFeedTab.FOR_YOU -> feedViewModel.onFilterSelected(FeedFilter.TRENDING)
                HomeFeedTab.FOLLOWING -> feedViewModel.onFilterSelected(FeedFilter.FOLLOWING)
                HomeFeedTab.POPULAR -> feedViewModel.onFilterSelected(FeedFilter.MOST_DOWNLOADED)
                HomeFeedTab.RECENT -> feedViewModel.onFilterSelected(FeedFilter.RECENT)
                HomeFeedTab.MY_PLANS -> {}
            }
        }
    }

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

    val allPlans = (uiStateResource as? Resource.Success)?.data?.plans ?: emptyList()

    val localMatches = remember(allPlans, searchQuery) {
        if (searchQuery.isBlank()) {
            emptyList()
        } else {
            allPlans.filter {
                it.heading.contains(searchQuery, ignoreCase = true) ||
                it.description.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    val filteredPlans = remember(allPlans) { allPlans }

    val pinnedPlans = remember(allPlans) {
        allPlans.filter { it.isPinned }
    }

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
                            // Menu Icon (smoothly animates out when search is active)
                            AnimatedVisibility(
                                visible = !isSearchActive,
                                enter = fadeIn(tween(250)) + expandHorizontally(tween(250, easing = FastOutSlowInEasing)),
                                exit = fadeOut(tween(200)) + shrinkHorizontally(tween(200, easing = FastOutSlowInEasing))
                            ) {
                                IconButton(
                                    onClick = onMenuClick,
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Menu,
                                        contentDescription = "Main Menu",
                                        tint = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }

                                if (!isSearchActive) {
                                    Spacer(modifier = Modifier.width(6.dp))
                                }

                                // Animated Search Bar (Compact when inactive, expands to fill top bar when active)
                                Surface(
                                    shape = RoundedCornerShape(20.dp),
                                    color = if (isSearchActive) MaterialTheme.colorScheme.surface else Color(0xFFF0F0F5),
                                    border = if (isSearchActive) BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)) else null,
                                    shadowElevation = if (isSearchActive) 2.dp else 0.dp,
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(38.dp)
                                        .animateContentSize(animationSpec = tween(durationMillis = 250, easing = FastOutSlowInEasing))
                                        .clickable(
                                            interactionSource = remember { MutableInteractionSource() },
                                            indication = null
                                        ) {
                                            if (!isSearchActive) {
                                                isSearchActive = true
                                            }
                                        }
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(horizontal = 12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Search,
                                            contentDescription = "Search",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                            modifier = Modifier.size(18.dp)
                                        )

                                        Spacer(modifier = Modifier.width(8.dp))

                                        if (isSearchActive) {
                                            BasicTextField(
                                                value = searchQuery,
                                                onValueChange = {
                                                    searchQuery = it
                                                    if (isOnline) {
                                                        feedViewModel?.onSearchQueryChanged(it)
                                                    }
                                                },
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .focusRequester(searchFocusRequester),
                                                singleLine = true,
                                                textStyle = MaterialTheme.typography.bodyMedium.copy(
                                                    color = MaterialTheme.colorScheme.onSurface
                                                ),
                                                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                                                decorationBox = { innerTextField ->
                                                    if (searchQuery.isEmpty()) {
                                                        Text(
                                                            text = "Search plans, people...",
                                                            style = MaterialTheme.typography.bodyMedium,
                                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                                        )
                                                    }
                                                    innerTextField()
                                                }
                                            )

                                            if (searchQuery.isNotBlank()) {
                                                IconButton(
                                                    onClick = {
                                                        searchQuery = ""
                                                        if (isOnline && activeFeedTab != HomeFeedTab.MY_PLANS) {
                                                            feedViewModel?.onSearchQueryChanged("")
                                                        }
                                                    },
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
                                        } else {
                                            Text(
                                                text = "Search plans, people...",
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                                modifier = Modifier.weight(1f)
                                            )
                                        }
                                    }
                                }

                                // Navigation Icons (Bell, Mail - visible when not active)
                                AnimatedVisibility(
                                    visible = !isSearchActive,
                                    enter = fadeIn(tween(250)) + expandHorizontally(tween(250, easing = FastOutSlowInEasing)),
                                    exit = fadeOut(tween(200)) + shrinkHorizontally(tween(200, easing = FastOutSlowInEasing))
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(start = 6.dp)
                                    ) {
                                        if (isOnline) {
                                            val notifInteraction = remember { MutableInteractionSource() }
                                            val isNotifPressed by notifInteraction.collectIsPressedAsState()
                                            val notifScale by animateFloatAsState(
                                                targetValue = if (isNotifPressed) 0.88f else 1f,
                                                animationSpec = if (isNotifPressed) PhysicsSpec.PressDown else PhysicsSpec.PressRelease,
                                                label = "notif_scale"
                                            )
                                            IconButton(
                                                onClick = onNotificationsClick,
                                                interactionSource = notifInteraction,
                                                modifier = Modifier
                                                    .size(36.dp)
                                                    .graphicsLayer {
                                                        scaleX = notifScale
                                                        scaleY = notifScale
                                                    }
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Outlined.Notifications,
                                                    contentDescription = "Notifications",
                                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    modifier = Modifier.size(20.dp)
                                                )
                                            }

                                            val convInteraction = remember { MutableInteractionSource() }
                                            val isConvPressed by convInteraction.collectIsPressedAsState()
                                            val convScale by animateFloatAsState(
                                                targetValue = if (isConvPressed) 0.88f else 1f,
                                                animationSpec = if (isConvPressed) PhysicsSpec.PressDown else PhysicsSpec.PressRelease,
                                                label = "conv_scale"
                                            )
                                            IconButton(
                                                onClick = onConversationsClick,
                                                interactionSource = convInteraction,
                                                modifier = Modifier
                                                    .size(36.dp)
                                                    .graphicsLayer {
                                                        scaleX = convScale
                                                        scaleY = convScale
                                                    }
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Outlined.Email,
                                                    contentDescription = "Direct Messages",
                                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    modifier = Modifier.size(20.dp)
                                                )
                                            }
                                        }
                                    }
                                }

                                // Trailing "Cancel" TextButton (slides in from right when active)
                                AnimatedVisibility(
                                    visible = isSearchActive,
                                    enter = slideInHorizontally(
                                        initialOffsetX = { it },
                                        animationSpec = tween(250, easing = FastOutSlowInEasing)
                                    ) + fadeIn(tween(250)),
                                    exit = slideOutHorizontally(
                                        targetOffsetX = { it },
                                        animationSpec = tween(200, easing = FastOutSlowInEasing)
                                    ) + fadeOut(tween(200))
                                ) {
                                    TextButton(
                                        onClick = {
                                            searchQuery = ""
                                            isSearchActive = false
                                            focusManager.clearFocus()
                                            keyboardController?.hide()
                                            if (isOnline) {
                                                feedViewModel?.onSearchQueryChanged("")
                                            }
                                        },
                                        contentPadding = PaddingValues(horizontal = 8.dp)
                                    ) {
                                        Text(
                                            text = "Cancel",
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
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
            floatingActionButton = {},
            snackbarHost = { SnackbarHost(snackbarHostState) },
            containerColor = MaterialTheme.colorScheme.background
        ) { paddingValues ->
            var isMyPlansRefreshing by remember { mutableStateOf(false) }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                // Architectural Top-to-Bottom Flow: Header -> Sliding Tabs -> Content
                if (isOnline && !isSearchActive && searchQuery.isBlank()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .padding(horizontal = 16.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val tabHaptic = LocalHapticFeedback.current
                        HomeFeedTab.entries.forEach { tab ->
                            val isSelected = activeFeedTab == tab
                            val tabInteraction = remember { MutableInteractionSource() }
                            val isTabPressed by tabInteraction.collectIsPressedAsState()
                            val tabScale by animateFloatAsState(
                                targetValue = if (isTabPressed) 0.94f else 1f,
                                animationSpec = if (isTabPressed) PhysicsSpec.PressDown else PhysicsSpec.PressRelease,
                                label = "tab_scale_${tab.name}"
                            )
                            val tabBgColor by androidx.compose.animation.animateColorAsState(
                                targetValue = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                                animationSpec = tween(180),
                                label = "tab_bg_${tab.name}"
                            )
                            val tabTextColor by androidx.compose.animation.animateColorAsState(
                                targetValue = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                animationSpec = tween(180),
                                label = "tab_text_${tab.name}"
                            )

                            Surface(
                                modifier = Modifier
                                    .height(34.dp)
                                    .graphicsLayer {
                                        scaleX = tabScale
                                        scaleY = tabScale
                                    }
                                    .clip(RoundedCornerShape(17.dp))
                                    .clickable(
                                        interactionSource = tabInteraction,
                                        indication = null
                                    ) {
                                        tabHaptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        selectedFeedTab = tab
                                    },
                                shape = RoundedCornerShape(17.dp),
                                color = tabBgColor,
                                shadowElevation = if (isSelected) 1.dp else 0.dp,
                                border = if (isSelected) null else BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f))
                            ) {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        text = tab.label,
                                        style = MaterialTheme.typography.labelMedium.copy(fontSize = 12.5.sp),
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = tabTextColor,
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                    }
                }

                if (searchQuery.isNotBlank()) {
                    // ── UNIVERSAL SEARCH (My Routines, Community Plans, Creators) ──
                    val searchResults = feedUiState.searchResults
                    val hasLocal = localMatches.isNotEmpty()
                    val hasCommunityPlans = isOnline && searchResults.plans.isNotEmpty()
                    val hasCreators = isOnline && searchResults.creators.isNotEmpty()
                    val isSearching = isOnline && feedUiState.isSearching

                    if (!hasLocal && !hasCommunityPlans && !hasCreators) {
                        if (isSearching) {
                            LazyColumn(
                                contentPadding = PaddingValues(AppDimens.Space16),
                                verticalArrangement = Arrangement.spacedBy(AppDimens.Space12),
                                modifier = Modifier.fillMaxSize()
                            ) {
                                items(3) {
                                    CommunityPostSkeleton()
                                }
                            }
                        } else {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(24.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(
                                        imageVector = Icons.Filled.Search,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                                        modifier = Modifier.size(48.dp)
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Text(
                                        text = "No results matching \"$searchQuery\"",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = "Try searching for a different routine, creator, or keyword.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        textAlign = TextAlign.Center
                                    )
                                    Spacer(modifier = Modifier.height(16.dp))
                                    OutlinedButton(onClick = {
                                        searchQuery = ""
                                        if (isOnline) feedViewModel?.onSearchQueryChanged("")
                                    }) {
                                        Text("Clear Search")
                                    }
                                }
                            }
                        }
                    } else {
                        LazyColumn(
                            contentPadding = PaddingValues(AppDimens.Space16),
                            verticalArrangement = Arrangement.spacedBy(AppDimens.Space12),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            // Section 1: My Routines
                            if (hasLocal) {
                                item(key = "header_local_plans") {
                                    Text(
                                        text = "MY ROUTINES (${localMatches.size})",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary,
                                        letterSpacing = 0.5.sp
                                    )
                                }
                                itemsIndexed(localMatches, key = { _, plan -> "local_plan_${plan.planId}" }) { index, plan ->
                                    com.example.plannerapp.ui.components.StaggeredEntrance(index = index) {
                                        Surface(
                                            shape = RoundedCornerShape(12.dp),
                                            color = MaterialTheme.colorScheme.surface,
                                            border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable { onPlanClick(plan.planId) }
                                        ) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(14.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Surface(
                                                    shape = CircleShape,
                                                    color = MaterialTheme.colorScheme.primaryContainer,
                                                    modifier = Modifier.size(38.dp)
                                                ) {
                                                    Box(contentAlignment = Alignment.Center) {
                                                        Icon(
                                                            imageVector = Icons.Outlined.Folder,
                                                            contentDescription = null,
                                                            tint = MaterialTheme.colorScheme.primary,
                                                            modifier = Modifier.size(20.dp)
                                                        )
                                                    }
                                                }
                                                Spacer(modifier = Modifier.width(12.dp))
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(
                                                        text = plan.heading,
                                                        style = MaterialTheme.typography.titleMedium,
                                                        fontWeight = FontWeight.SemiBold,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                    if (plan.description.isNotBlank()) {
                                                        Text(
                                                            text = plan.description,
                                                            style = MaterialTheme.typography.bodySmall,
                                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                            maxLines = 1,
                                                            overflow = TextOverflow.Ellipsis
                                                        )
                                                    }
                                                }
                                                Icon(
                                                    imageVector = Icons.AutoMirrored.Outlined.ArrowForward,
                                                    contentDescription = "Open",
                                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            // Section 2: Community Plans
                            if (hasCommunityPlans) {
                                item(key = "header_community_plans") {
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = "COMMUNITY PLANS (${searchResults.plans.size})",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary,
                                        letterSpacing = 0.5.sp
                                    )
                                }
                                itemsIndexed(searchResults.plans, key = { _, plan -> "search_plan_${plan.postId}" }) { index, post ->
                                    com.example.plannerapp.ui.components.StaggeredEntrance(index = index) {
                                        SocialFeedPostCard(
                                            post = post,
                                            onUpvote = { feedViewModel?.onVote(post.postId, VoteType.UP) },
                                            onDownvote = { feedViewModel?.onVote(post.postId, VoteType.DOWN) },
                                            onSaveToggle = { feedViewModel?.onToggleSave(post.postId) },
                                            onClick = { onPostClick(post.postId) },
                                            onCreatorClick = { onCreatorClick(post.author.userId) }
                                        )
                                    }
                                }
                            }

                            // Section 3: Creators & People
                            if (hasCreators) {
                                item(key = "header_creators") {
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = "CREATORS & PEOPLE (${searchResults.creators.size})",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary,
                                        letterSpacing = 0.5.sp
                                    )
                                }
                                itemsIndexed(searchResults.creators, key = { _, user -> "search_user_${user.userId}" }) { index, creator ->
                                    com.example.plannerapp.ui.components.StaggeredEntrance(index = index) {
                                        CreatorSearchResultCard(
                                            creator = creator,
                                            isFollowing = feedUiState.followingUserIds.contains(creator.userId),
                                            onToggleFollow = { feedViewModel?.onToggleFollow(creator.userId) },
                                            onClick = { onCreatorClick(creator.userId) }
                                        )
                                    }
                                }
                            }
                        }
                    }
                } else if (activeFeedTab == HomeFeedTab.MY_PLANS || !isOnline) {
                    PullToRefreshBox(
                        isRefreshing = isMyPlansRefreshing,
                        onRefresh = {
                            coroutineScope.launch {
                                isMyPlansRefreshing = true
                                kotlinx.coroutines.delay(500)
                                isMyPlansRefreshing = false
                            }
                        },
                        modifier = Modifier.fillMaxSize()
                    ) {
                        when (val state = uiStateResource) {
                            is Resource.Loading -> {
                                LazyVerticalGrid(
                                    columns = GridCells.Fixed(2),
                                    contentPadding = PaddingValues(AppDimens.Space16),
                                    horizontalArrangement = Arrangement.spacedBy(AppDimens.Space16),
                                    verticalArrangement = Arrangement.spacedBy(AppDimens.Space16),
                                    modifier = Modifier.fillMaxSize()
                                ) {
                                    items(4) { index -> 
                                        com.example.plannerapp.ui.components.StaggeredEntrance(index = index) {
                                            PlanCardSkeleton() 
                                        }
                                    }
                                }
                            }
                            is Resource.Error -> {
                                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    Text("Error: ${state.message}", color = MaterialTheme.colorScheme.error)
                                }
                            }
                            is Resource.Success -> {
                                if (filteredPlans.isEmpty()) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(24.dp), 
                                        contentAlignment = Alignment.Center
                                    ) {
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
                                } else {
                                    LazyVerticalGrid(
                                        columns = GridCells.Fixed(2),
                                        contentPadding = PaddingValues(AppDimens.Space16),
                                        horizontalArrangement = Arrangement.spacedBy(AppDimens.Space16),
                                        verticalArrangement = Arrangement.spacedBy(AppDimens.Space16)
                                    ) {
                                        itemsIndexed(filteredPlans, key = { _, it -> it.planId }) { index, plan ->
                                            val isSelected = selectedPlanIds.contains(plan.planId)
                                            com.example.plannerapp.ui.components.StaggeredEntrance(index = index) {
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

                                        if (isOnline) {
                                            item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(maxLineSpan) }) {
                                                Surface(
                                                    shape = RoundedCornerShape(12.dp),
                                                    color = MaterialTheme.colorScheme.surface,
                                                    tonalElevation = 1.dp,
                                                    border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)),
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .padding(vertical = 8.dp)
                                                        .clickable {
                                                            selectedFeedTab = HomeFeedTab.FOR_YOU
                                                        }
                                                ) {
                                                    Row(
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .padding(14.dp),
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.SpaceBetween
                                                    ) {
                                                        Row(
                                                            verticalAlignment = Alignment.CenterVertically,
                                                            modifier = Modifier.weight(1f)
                                                        ) {
                                                            Surface(
                                                                shape = CircleShape,
                                                                color = MaterialTheme.colorScheme.primaryContainer,
                                                                modifier = Modifier.size(36.dp)
                                                            ) {
                                                                Box(contentAlignment = Alignment.Center) {
                                                                    Icon(
                                                                        imageVector = Icons.Outlined.Explore,
                                                                        contentDescription = null,
                                                                        tint = MaterialTheme.colorScheme.primary,
                                                                        modifier = Modifier.size(20.dp)
                                                                    )
                                                                }
                                                            }
                                                            Spacer(modifier = Modifier.width(12.dp))
                                                            Column {
                                                                Text(
                                                                    text = "Explore Community Routines",
                                                                    style = MaterialTheme.typography.titleSmall,
                                                                    fontWeight = FontWeight.SemiBold
                                                                )
                                                                Text(
                                                                    text = "Discover proven habits and plans from creators",
                                                                    style = MaterialTheme.typography.bodySmall,
                                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                                )
                                                            }
                                                        }
                                                        Icon(
                                                            imageVector = Icons.AutoMirrored.Outlined.ArrowForward,
                                                            contentDescription = "Explore",
                                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                            modifier = Modifier.size(18.dp)
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
                } else {
                    // ── COMMUNITY FEED WITH PULL-TO-REFRESH & SKELETON LOADERS ──
                    PullToRefreshBox(
                        isRefreshing = feedUiState.isRefreshing,
                        onRefresh = {
                            feedViewModel?.refresh()
                        },
                        modifier = Modifier.fillMaxSize()
                    ) {
                        if (feedUiState.isLoading) {
                            LazyColumn(
                                contentPadding = PaddingValues(AppDimens.Space16),
                                verticalArrangement = Arrangement.spacedBy(AppDimens.Space16),
                                modifier = Modifier.fillMaxSize()
                            ) {
                                items(4) {
                                    CommunityPostSkeleton()
                                }
                            }
                        } else if (feedUiState.posts.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(24.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(
                                        imageVector = Icons.Outlined.Explore,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                        modifier = Modifier.size(48.dp)
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = if (activeFeedTab == HomeFeedTab.FOLLOWING)
                                            "You are not following any creators yet. Follow creators to see their public plans here."
                                        else "No community posts yet",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        } else {
                            LazyColumn(
                                contentPadding = PaddingValues(AppDimens.Space16),
                                verticalArrangement = Arrangement.spacedBy(AppDimens.Space16),
                                modifier = Modifier.fillMaxSize()
                            ) {
                                itemsIndexed(feedUiState.posts, key = { _, post -> post.postId }) { index, post ->
                                    com.example.plannerapp.ui.components.StaggeredEntrance(index = index) {
                                        SocialFeedPostCard(
                                            post = post,
                                            onUpvote = { feedViewModel?.onVote(post.postId, VoteType.UP) },
                                            onDownvote = { feedViewModel?.onVote(post.postId, VoteType.DOWN) },
                                            onSaveToggle = { feedViewModel?.onToggleSave(post.postId) },
                                            onClick = { onPostClick(post.postId) },
                                            onCreatorClick = { onCreatorClick(post.author.userId) }
                                        )
                                    }
                                }
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

    if (showCreatorWaitlistSheet) {
        com.example.plannerapp.ui.creator.CreatorWaitlistSheet(
            onDismissRequest = { showCreatorWaitlistSheet = false }
        )
    }

    if (showProDialog) {
        PlannerProDialog(
            onDismissRequest = { showProDialog = false },
            onVisitStoreClick = {
                showProDialog = false
                showCreditHubSheet = true
            }
        )
    }

    if (showCreditHubSheet && creditViewModel != null) {
        CreditHubSheet(
            viewModel = creditViewModel,
            onDismissRequest = { showCreditHubSheet = false }
        )
    }
}

@Composable
fun PlannerProDialog(
    onDismissRequest: () -> Unit,
    onVisitStoreClick: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
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
            Button(onClick = onVisitStoreClick) {
                Text("Visit Store")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) {
                Text("Maybe Later")
            }
        }
    )
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

