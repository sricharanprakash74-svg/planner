package com.example.plannerapp

import androidx.activity.BackEventCompat
import androidx.activity.compose.BackHandler
import androidx.activity.compose.PredictiveBackHandler
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import com.example.plannerapp.theme.PhysicsSpec
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.collectAsState
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import com.example.plannerapp.creator.CreatorRepository
import com.example.plannerapp.creator.CreatorStudioViewModel
import com.example.plannerapp.creator.CreatorStudioViewModelFactory
import com.example.plannerapp.credits.PlannerStoreScreen
import com.example.plannerapp.data.PlannerDatabase
import com.example.plannerapp.data.PlannerRepository
import com.example.plannerapp.ui.creator.CreatorStudioScreen
import com.example.plannerapp.ui.navigation.AppScaffoldWrapper
import com.example.plannerapp.ui.navigation.BottomNavTab
import com.example.plannerapp.ui.create.CreatePlanScreenWrapper
import com.example.plannerapp.ui.create.CreatePlanViewModel
import com.example.plannerapp.ui.create.CreatePlanViewModelFactory
import com.example.plannerapp.ui.explore.ExploreScreen
import com.example.plannerapp.ui.home.HomeScreenWrapper
import com.example.plannerapp.ui.home.HomeViewModel
import com.example.plannerapp.ui.home.HomeViewModelFactory
import com.example.plannerapp.ui.profile.ProfileScreenWrapper
import com.example.plannerapp.ui.profile.ProfileViewModel
import com.example.plannerapp.ui.profile.ProfileViewModelFactory
import com.example.plannerapp.ui.profile.CreatorProfileScreen
import com.example.plannerapp.ui.profile.CreatorProfileViewModel
import com.example.plannerapp.ui.profile.CreatorProfileViewModelFactory
import com.example.plannerapp.ui.analytics.AnalyticsScreenWrapper
import com.example.plannerapp.ui.settings.*
import com.example.plannerapp.ui.detail.PlanDetailScreen
import com.example.plannerapp.ui.detail.PlanDetailViewModel
import com.example.plannerapp.ui.detail.PlanDetailViewModelFactory
import com.example.plannerapp.ui.detail.PublicPlanDetailScreen
import com.example.plannerapp.ui.detail.PublicPlanDetailViewModel
import com.example.plannerapp.ui.detail.PublicPlanDetailViewModelFactory
import com.example.plannerapp.ui.messages.*
import com.example.plannerapp.ui.notifications.*
import com.example.plannerapp.data.social.InMemorySocialRepository
import com.example.plannerapp.data.social.SocialRepository
import com.example.plannerapp.data.social.SupabaseSocialRepository
import com.example.plannerapp.ui.social.CommunityDiscussionScreen
import com.example.plannerapp.ui.social.CommunityDiscussionViewModel
import com.example.plannerapp.ui.social.CommunityDiscussionViewModelFactory
import com.example.plannerapp.ui.social.CommunityFeedViewModel
import com.example.plannerapp.ui.social.CommunityFeedViewModelFactory
import com.example.plannerapp.ui.auth.SignInScreenWrapper
import com.example.plannerapp.ui.auth.AuthViewModel
import com.example.plannerapp.ui.auth.AuthViewModelFactory
import com.example.plannerapp.billing.BillingViewModel
import com.example.plannerapp.billing.BillingViewModelFactory
import com.example.plannerapp.ui.billing.PaywallScreen

@Composable
fun MainNavigation() {
    val context = LocalContext.current
    val database = PlannerDatabase.getDatabase(context)
    val repository = PlannerRepository(database.plannerDao())
    val userDao = database.userDao()
    val socialRepository: SocialRepository = remember { SupabaseSocialRepository() }

    // Instant session restoration: check if user already signed in (guest or cloud)
    var isAuthChecked by remember { mutableStateOf(false) }
    var initialHasUser by remember { mutableStateOf(false) }
    var initialOnboardingComplete by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        val activeUser = userDao.getActiveUserOnce()
        initialHasUser = activeUser != null && !activeUser.cloudUserId.isNullOrBlank()
        initialOnboardingComplete = activeUser?.onboardingComplete == 1
        isAuthChecked = true
    }

    if (!isAuthChecked) {
        // Seamless background while checking local DB (instant, <10ms)
        // Prevents the sign-in screen from appearing when offline or reopening the app
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        )
        return
    }

    val initialDestination = when {
        !initialHasUser -> Onboarding
        !initialOnboardingComplete -> Onboarding
        else -> Home
    }
    val backStack = rememberNavBackStack(initialDestination)
    var currentTab by remember { mutableStateOf(BottomNavTab.HOME) }
    
    // ViewModels scoped properly
    val creditRepository = remember { com.example.plannerapp.credits.CreditRepository(database.creditDao()) }
    val homeViewModel: HomeViewModel = viewModel(factory = HomeViewModelFactory(repository, userDao, context.applicationContext))
    val profileViewModel: ProfileViewModel = viewModel(factory = ProfileViewModelFactory(repository, userDao, socialRepository, creditRepository))
    val createPlanViewModel: CreatePlanViewModel = viewModel(factory = CreatePlanViewModelFactory(repository, userDao, context.applicationContext, socialRepository))
    val settingsViewModel: SettingsViewModel = viewModel(factory = SettingsViewModelFactory(userDao))
    val authViewModel: AuthViewModel = viewModel(factory = AuthViewModelFactory(userDao, context.applicationContext))
    val activityLogViewModel: ActivityLogViewModel = viewModel(factory = ActivityLogViewModelFactory(repository, userDao, database.badgeDao()))
    val exportDataViewModel: ExportDataViewModel = viewModel(factory = ExportDataViewModelFactory(repository, userDao))
    val publishPlanViewModel: PublishPlanViewModel = viewModel(factory = PublishPlanViewModelFactory(repository, userDao, socialRepository, creditRepository))
    val creditViewModel: com.example.plannerapp.credits.CreditViewModel = viewModel(
        factory = remember {
            com.example.plannerapp.credits.CreditViewModelFactory(
                creditRepository,
                userDao
            )
        }
    )
    val creatorRepository = remember {
        CreatorRepository(
            creatorDao = database.creatorDao(),
            planVersionDao = database.planVersionDao(),
            ledgerDao = database.ledgerDao(),
            planEntitlementDao = database.planEntitlementDao(),
            plannerDao = database.plannerDao(),
            userDao = userDao
        )
    }
    val creatorStudioViewModel: CreatorStudioViewModel = viewModel(
        factory = remember {
            CreatorStudioViewModelFactory(
                creatorRepository = creatorRepository,
                userDao = userDao,
                plannerDao = database.plannerDao(),
                context = context.applicationContext
            )
        }
    )

    val currentKey = backStack.lastOrNull() ?: if (initialHasUser && initialOnboardingComplete) Home else Onboarding

    // Synchronize bottom bar active tab indicator with the current destination on the back stack
    LaunchedEffect(currentKey) {
        when (currentKey) {
            Home -> currentTab = BottomNavTab.HOME
            Profile -> currentTab = BottomNavTab.PROFILE
            is Explore -> currentTab = BottomNavTab.EXPLORE
            else -> { /* Subpages keep their parent tab active */ }
        }
    }

    // Handle system back gesture & hardware back button with predictive back physics
    val canGoBack = backStack.size > 1 || (currentKey != Home && currentKey != Onboarding)
    var backProgress by remember { mutableFloatStateOf(0f) }
    var isPredictiveBackActive by remember { mutableStateOf(false) }

    val backScale by animateFloatAsState(
        targetValue = if (isPredictiveBackActive) 1f - (backProgress * 0.08f) else 1f,
        animationSpec = if (isPredictiveBackActive) spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMedium) else PhysicsSpec.SheetSettle,
        label = "predictiveBackScale"
    )
    val backCornerRadius by animateDpAsState(
        targetValue = if (isPredictiveBackActive) (backProgress * 16).dp else 0.dp,
        animationSpec = if (isPredictiveBackActive) spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMedium) else spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMediumLow),
        label = "predictiveBackCorner"
    )

    PredictiveBackHandler(enabled = canGoBack) { progress: Flow<BackEventCompat> ->
        try {
            isPredictiveBackActive = true
            progress.collect { backEvent ->
                backProgress = backEvent.progress
            }
            if (backStack.size > 1) {
                backStack.removeLastOrNull()
            } else if (currentKey != Home && currentKey != Onboarding) {
                backStack.clear()
                backStack.add(Home)
                currentTab = BottomNavTab.HOME
            }
        } catch (e: CancellationException) {
            // Cancelled
        } finally {
            isPredictiveBackActive = false
            backProgress = 0f
        }
    }

    // Modal dialog triggered whenever a shared plan deep link is opened
    val pendingDeepLink = com.example.plannerapp.sharing.DeepLinkState.pendingDeepLink.value
    if (pendingDeepLink != null) {
        com.example.plannerapp.sharing.ImportPlanDialog(
            deepLinkUri = pendingDeepLink,
            onDismissRequest = { com.example.plannerapp.sharing.DeepLinkState.clear() },
            onPlanCloned = { newPlanId ->
                com.example.plannerapp.sharing.DeepLinkState.clear()
                if (newPlanId > 0) {
                    backStack.add(PlanDetail(newPlanId))
                }
            }
        )
    }

    val connectivityObserver = remember { com.example.plannerapp.data.NetworkConnectivityObserver(context) }
    val connectivityStatus by connectivityObserver.observe().collectAsState(initial = com.example.plannerapp.data.ConnectivityStatus.Available)
    val isOnline = connectivityStatus == com.example.plannerapp.data.ConnectivityStatus.Available && com.example.plannerapp.auth.SupabaseConfig.isConfigured
    
    val rootScope = rememberCoroutineScope()
    var showCreatePlanDialog by remember { mutableStateOf(false) }

    val activeUser by userDao.getActiveUser().collectAsStateWithLifecycle(initialValue = null)
    val homeUiState by homeViewModel.uiState.collectAsStateWithLifecycle()
    val allPlans: List<com.example.plannerapp.data.PlanEntity> = (homeUiState as? com.example.plannerapp.ui.state.Resource.Success)?.data?.plans ?: emptyList()
    val pinnedPlans = remember(allPlans) { allPlans.filter { it.isPinned } }
    var showAllRecentPlans by remember { mutableStateOf(false) }

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val coroutineScope = rememberCoroutineScope()
    var showCreditHubSheet by remember { mutableStateOf(false) }
    var showCreatorWaitlistSheet by remember { mutableStateOf(false) }
    var showProDialog by remember { mutableStateOf(false) }

    androidx.compose.runtime.CompositionLocalProvider(
        com.example.plannerapp.data.LocalIsOnline provides isOnline
    ) {
        ModalNavigationDrawer(
            drawerState = drawerState,
            gesturesEnabled = (currentKey == Home || currentKey is Explore) && currentTab == BottomNavTab.HOME,
            drawerContent = {
                com.example.plannerapp.ui.navigation.AppNavDrawerSheet(
                    isCreator = activeUser?.isCreator == true,
                    isOnline = isOnline,
                    plans = allPlans,
                    pinnedPlans = pinnedPlans,
                    showAllRecentPlans = showAllRecentPlans,
                    onToggleShowAllRecent = { showAllRecentPlans = !showAllRecentPlans },
                    onPlanClick = { planId ->
                        coroutineScope.launch { drawerState.close() }
                        backStack.add(PlanDetail(planId))
                    },
                    onAddPlanClick = {
                        coroutineScope.launch { drawerState.close() }
                        showCreatePlanDialog = true
                    },
                    onExploreClick = { query ->
                        coroutineScope.launch { drawerState.close() }
                        backStack.add(Explore(initialQuery = query))
                    },
                    onCreditStoreClick = {
                        coroutineScope.launch { drawerState.close() }
                        showCreditHubSheet = true
                    },
                    onCreatorProgramClick = {
                        coroutineScope.launch { drawerState.close() }
                        showCreatorWaitlistSheet = true
                    },
                    onUpgradeProClick = {
                        coroutineScope.launch { drawerState.close() }
                        showProDialog = true
                    },
                    onSettingsClick = {
                        coroutineScope.launch { drawerState.close() }
                        backStack.add(Settings)
                    },
                    onCloseDrawer = {
                        coroutineScope.launch { drawerState.close() }
                    }
                )
            }
        ) {
            AppScaffoldWrapper(
            modifier = Modifier.fillMaxSize(),
            currentTab = currentTab,
            onTabSelected = { tab ->
                currentTab = tab
                when (tab) {
                    BottomNavTab.HOME -> {
                        backStack.clear()
                        backStack.add(Home)
                    }
                    BottomNavTab.EXPLORE -> {
                        backStack.clear()
                        backStack.add(Home)
                        backStack.add(Explore())
                    }
                    BottomNavTab.PROFILE -> {
                        backStack.clear()
                        backStack.add(Home)
                        backStack.add(Profile)
                    }
                }
            },
            showBottomBar = currentKey == Home || currentKey == Profile || currentKey is Explore,
            onAddClick = { showCreatePlanDialog = true },
            avatarUrl = activeUser?.avatarUrl
        ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .graphicsLayer {
                    scaleX = backScale
                    scaleY = backScale
                    alpha = 1f - (backProgress * 0.15f)
                }
                .clip(RoundedCornerShape(backCornerRadius))
        ) {
            NavDisplay(
                backStack = backStack,
                onBack = { 
                    if (backStack.size > 1) {
                        backStack.removeLastOrNull()
                    } else if (currentKey != Home) {
                        backStack.clear()
                        backStack.add(Home)
                    }
                },
                modifier = Modifier.fillMaxSize(),
            entryProvider = entryProvider {
                entry<Home> {
                    val homeFeedViewModel: CommunityFeedViewModel = viewModel(
                        key = "home_community_feed_vm",
                        factory = CommunityFeedViewModelFactory(socialRepository, userDao)
                    )
                    HomeScreenWrapper(
                        viewModel = homeViewModel,
                        onPlanClick = { planId -> backStack.add(PlanDetail(planId)) },
                        onPlanCreatedAndOpen = { planId -> backStack.add(PlanDetail(planId, autoOpenAddTask = true)) },
                        onAnalyticsClick = { backStack.add(Analytics) },
                        onExploreClick = { query -> backStack.add(Explore(initialQuery = query)) },
                        onSettingsClick = { backStack.add(Settings) },
                        plannerRepository = repository,
                        socialRepository = socialRepository,
                        userDao = userDao,
                        creditViewModel = creditViewModel,
                        feedViewModel = homeFeedViewModel,
                        onNavigateToDiscussion = { postId -> backStack.add(CommunityDiscussion(postId)) },
                        onPostClick = { postId -> backStack.add(PublicPlanDetail(postId)) },
                        onCreatorClick = { creatorId -> backStack.add(CreatorProfile(creatorId)) },
                        onCreatorMonetizationClick = { /* Parked: Handled via CreatorWaitlistSheet */ },
                        onBecomeCreatorClick = { /* Parked: Handled via CreatorWaitlistSheet */ },
                        onNotificationsClick = { backStack.add(Notifications) },
                        onConversationsClick = { backStack.add(Conversations) },
                        onMenuClick = { coroutineScope.launch { drawerState.open() } }
                    )
                }
                entry<Explore> { key ->
                    val feedViewModel: CommunityFeedViewModel = viewModel(
                        key = "explore_feed_${key.initialQuery}",
                        factory = CommunityFeedViewModelFactory(socialRepository, userDao, key.initialQuery)
                    )
                    ExploreScreen(
                        viewModel = feedViewModel,
                        onPostClick = { postId -> backStack.add(PublicPlanDetail(postId)) },
                        onCreatorClick = { creatorId -> backStack.add(CreatorProfile(creatorId)) }
                    )
                }
                entry<Profile> {
                    ProfileScreenWrapper(
                        viewModel = profileViewModel,
                        onSettingsClick = { backStack.add(Settings) },
                        onAnalyticsClick = { backStack.add(Analytics) },
                        onPlanClick = { planId -> backStack.add(PlanDetail(planId)) },
                        onCreatorClick = { creatorId -> backStack.add(CreatorProfile(creatorId)) },
                        onEditProfileClick = { backStack.add(SettingsEditProfile) },
                        onCreatorMonetizationClick = { /* Parked: Handled via CreatorWaitlistSheet */ },
                        onBecomeCreatorClick = { /* Parked: Handled via CreatorWaitlistSheet */ }
                    )
                }
                entry<Analytics> {
                    AnalyticsScreenWrapper(
                        viewModel = profileViewModel,
                        onBack = { backStack.removeLastOrNull() }
                    )
                }
                entry<SignIn> {
                    SignInScreenWrapper(
                        viewModel = authViewModel,
                        onSignInSuccess = {
                            backStack.clear()
                            backStack.add(Home)
                            currentTab = BottomNavTab.HOME
                        }
                    )
                }
                entry<Onboarding> {
                    com.example.plannerapp.ui.onboarding.OnboardingScreenWrapper(
                        onProceedToAuth = {
                            backStack.add(SignIn)
                        },
                        onSignInClick = {
                            backStack.add(SignIn)
                        },
                        userDao = userDao
                    )
                }
                entry<OnboardingPaywall> {
                    val billingViewModel: BillingViewModel = viewModel(
                        key = "revenuecat_billing_vm_onboarding",
                        factory = BillingViewModelFactory(context.applicationContext)
                    )
                    com.example.plannerapp.ui.billing.PaywallScreen(
                        viewModel = billingViewModel,
                        onDismiss = {
                            backStack.clear()
                            backStack.add(Home)
                            currentTab = BottomNavTab.HOME
                        }
                    )
                }
                entry<Settings> {
                    SettingsScreenWrapper(
                        viewModel = settingsViewModel,
                        onBack = { backStack.removeLastOrNull() },
                        onEditProfileClick = { backStack.add(SettingsEditProfile) },
                        onManageAccountClick = { backStack.add(SettingsManageAccount) },
                        onSavedPlansClick = { backStack.add(SettingsSavedPlans) },
                        onActivityLogClick = { backStack.add(SettingsActivityLog) },
                        onNotificationsClick = { backStack.add(SettingsNotifications) },
                        onTimeFocusClick = { backStack.add(SettingsTimeFocus) },
                        onTimezoneClick = { backStack.add(SettingsTimezone) },
                        onPlanPrivacyClick = { backStack.add(SettingsPlanPrivacy) },
                        onPublishPlanClick = { backStack.add(SettingsPublishPlan) },
                        onAppearanceClick = { backStack.add(SettingsAppearance) },
                        onBackupClick = { backStack.add(SettingsBackup) },
                        onExportClick = { backStack.add(SettingsExportData) },
                        onAccessibilityClick = { backStack.add(SettingsAccessibility) },
                        onCreatorSetupClick = { /* Parked: Handled via CreatorWaitlistSheet */ },
                        onCreatorMonetizationClick = { /* Parked: Handled via CreatorWaitlistSheet */ },
                        onCreatorStudioClick = { /* Parked: Handled via CreatorWaitlistSheet */ },
                        onSubscriptionClick = { backStack.add(Paywall) },
                        onHelpClick = { backStack.add(SettingsHelp) },
                        onPrivacyPolicyClick = { backStack.add(SettingsPrivacyPolicy) },
                        onAboutClick = { backStack.add(SettingsAbout) },
                        onSignInClick = { backStack.add(SignIn) },
                        onSignOutClick = {
                            backStack.clear()
                            backStack.add(SignIn)
                            currentTab = BottomNavTab.HOME
                        }
                    )
                }
                entry<SettingsEditProfile> {
                    ProfileCenterScreen(
                        viewModel = settingsViewModel,
                        onBack = { backStack.removeLastOrNull() },
                        onSignInClick = { backStack.add(SignIn) }
                    )
                }
                entry<SettingsNotifications> {
                    NotificationSettingsScreen(
                        viewModel = settingsViewModel,
                        onBack = { backStack.removeLastOrNull() }
                    )
                }
                entry<SettingsTimezone> {
                    TimezoneSettingsScreen(
                        viewModel = settingsViewModel,
                        onBack = { backStack.removeLastOrNull() }
                    )
                }
                entry<SettingsPublishPlan> {
                    PublishPlanScreen(
                        viewModel = publishPlanViewModel,
                        onBack = { backStack.removeLastOrNull() },
                        onNavigateToDiscussion = { postId -> backStack.add(CommunityDiscussion(postId)) }
                    )
                }
                entry<SettingsBackup> {
                    BackupSettingsScreen(
                        onBack = { backStack.removeLastOrNull() }
                    )
                }
                entry<SettingsExportData> {
                    ExportDataScreen(
                        viewModel = exportDataViewModel,
                        onBack = { backStack.removeLastOrNull() }
                    )
                }
                entry<SettingsManageAccount> {
                    val creditBalance by creditViewModel.balanceFlow.collectAsState()
                    ManageAccountScreen(
                        viewModel = settingsViewModel,
                        onBack = { backStack.removeLastOrNull() },
                        onExportClick = { backStack.add(SettingsExportData) },
                        onSignInClick = { backStack.add(SignIn) },
                        creditBalance = creditBalance,
                        onAccountDeleted = {
                            backStack.clear()
                            backStack.add(SignIn)
                            currentTab = BottomNavTab.HOME
                        }
                    )
                }
                entry<SettingsDeleteAccount> {
                    val creditBalance by creditViewModel.balanceFlow.collectAsState()
                    ManageAccountScreen(
                        viewModel = settingsViewModel,
                        onBack = { backStack.removeLastOrNull() },
                        onExportClick = { backStack.add(SettingsExportData) },
                        onSignInClick = { backStack.add(SignIn) },
                        creditBalance = creditBalance,
                        onAccountDeleted = {
                            backStack.clear()
                            backStack.add(SignIn)
                            currentTab = BottomNavTab.HOME
                        }
                    )
                }
                entry<SettingsSavedPlans> {
                    SavedPlansScreen(
                        repository = repository,
                        onBack = { backStack.removeLastOrNull() }
                    )
                }
                entry<SettingsActivityLog> {
                    ActivityLogScreen(
                        viewModel = activityLogViewModel,
                        onBack = { backStack.removeLastOrNull() }
                    )
                }
                entry<SettingsTimeFocus> {
                    TimeFocusSettingsScreen(
                        viewModel = settingsViewModel,
                        onBack = { backStack.removeLastOrNull() }
                    )
                }
                entry<SettingsPlanPrivacy> {
                    PlanPrivacySettingsScreen(
                        viewModel = settingsViewModel,
                        onBack = { backStack.removeLastOrNull() }
                    )
                }
                entry<SettingsAppearance> {
                    AppearanceSettingsScreen(
                        viewModel = settingsViewModel,
                        onBack = { backStack.removeLastOrNull() }
                    )
                }
                entry<SettingsAccessibility> {
                    AccessibilitySettingsScreen(
                        viewModel = settingsViewModel,
                        onBack = { backStack.removeLastOrNull() }
                    )
                }
                entry<SettingsCreatorSetup> {
                    CreatorSetupScreen(
                        viewModel = settingsViewModel,
                        onBack = { backStack.removeLastOrNull() },
                        onMonetizationClick = { backStack.add(SettingsCreatorMonetization) }
                    )
                }
                entry<SettingsCreatorMonetization> {
                    com.example.plannerapp.ui.settings.CreatorMonetizationScreen(
                        creditViewModel = creditViewModel,
                        onBack = { backStack.removeLastOrNull() },
                        onBecomeCreatorClick = { backStack.add(SettingsCreatorSetup) }
                    )
                }
                entry<SettingsHelp> {
                    HelpScreen(
                        onBack = { backStack.removeLastOrNull() }
                    )
                }
                entry<SettingsPrivacyPolicy> {
                    PrivacyPolicyScreen(
                        onBack = { backStack.removeLastOrNull() }
                    )
                }
                entry<SettingsAbout> {
                    AboutScreen(
                        onBack = { backStack.removeLastOrNull() }
                    )
                }
                entry<CreatePlan> {
                    CreatePlanScreenWrapper(
                        viewModel = createPlanViewModel,
                        onClose = { backStack.removeLastOrNull() },
                        onPlanCreated = { }
                    )
                }
                entry<PlanDetail> { key ->
                    val planDetailViewModel: PlanDetailViewModel = viewModel(
                        key = "plan_detail_${key.planId}",
                        factory = PlanDetailViewModelFactory(key.planId, repository, context.applicationContext, socialRepository)
                    )
                    PlanDetailScreen(
                        viewModel = planDetailViewModel,
                        autoOpenAddTask = key.autoOpenAddTask,
                        onBack = { backStack.removeLastOrNull() },
                        onOpenCommunityDiscussion = { postId -> backStack.add(CommunityDiscussion(postId)) },
                        onPlanCloned = { newPlanId -> backStack.add(PlanDetail(newPlanId)) }
                    )
                }
                entry<CreatorProfile> { key ->
                    val creatorViewModel: CreatorProfileViewModel = viewModel(
                        key = "creator_profile_${key.userId}",
                        factory = CreatorProfileViewModelFactory(key.userId, socialRepository, userDao)
                    )
                    CreatorProfileScreen(
                        viewModel = creatorViewModel,
                        onBack = { backStack.removeLastOrNull() },
                        onPostClick = { postId -> backStack.add(PublicPlanDetail(postId)) },
                        onNavigateToChat = { convId, recipientId, recipientName ->
                            backStack.add(Chat(convId, recipientId, recipientName))
                        }
                    )
                }
                entry<CommunityDiscussion> { key ->
                    val discussionViewModel: CommunityDiscussionViewModel = viewModel(
                        key = "discussion_${key.postId}",
                        factory = CommunityDiscussionViewModelFactory(
                            postId = key.postId,
                            socialRepository = socialRepository,
                            plannerRepository = repository,
                            userDao = userDao,
                            creditRepository = creditRepository
                        )
                    )
                    CommunityDiscussionScreen(
                        viewModel = discussionViewModel,
                        creditViewModel = creditViewModel,
                        onBack = { backStack.removeLastOrNull() },
                        onPlanJoinedAndOpen = { planId ->
                            backStack.removeLastOrNull()
                            backStack.add(PlanDetail(planId))
                        },
                        onCreatorClick = { creatorId -> backStack.add(CreatorProfile(creatorId)) }
                    )
                }
                entry<Paywall> {
                    val billingViewModel: BillingViewModel = viewModel(
                        key = "revenuecat_billing_vm",
                        factory = BillingViewModelFactory(context.applicationContext)
                    )
                    PaywallScreen(
                        viewModel = billingViewModel,
                        onDismiss = { backStack.removeLastOrNull() }
                    )
                }
                entry<CreatorStudio> {
                    CreatorStudioScreen(
                        viewModel = creatorStudioViewModel,
                        onBack = { backStack.removeLastOrNull() },
                        onNavigateToSettings = { backStack.add(Settings) }
                    )
                }
                entry<PlannerStore> {
                    PlannerStoreScreen(
                        viewModel = creditViewModel,
                        onBack = { backStack.removeLastOrNull() }
                    )
                }
                entry<PublicPlanDetail> { key ->
                    val detailViewModel: PublicPlanDetailViewModel = viewModel(
                        key = "public_plan_${key.publicPlanId}",
                        factory = PublicPlanDetailViewModelFactory(
                            planId = key.publicPlanId,
                            socialRepository = socialRepository,
                            plannerRepository = repository,
                            userDao = userDao,
                            creditRepository = creditRepository
                        )
                    )
                    PublicPlanDetailScreen(
                        viewModel = detailViewModel,
                        onBack = { backStack.removeLastOrNull() },
                        onCreatorClick = { creatorId -> backStack.add(CreatorProfile(creatorId)) },
                        onOpenLocalPlan = { localPlanId ->
                            backStack.removeLastOrNull()
                            backStack.add(PlanDetail(localPlanId))
                        }
                    )
                }
                entry<Conversations> {
                    val convViewModel: ConversationsViewModel = viewModel(
                        key = "conversations_list_vm",
                        factory = ConversationsViewModelFactory(socialRepository, userDao)
                    )
                    ConversationsScreen(
                        viewModel = convViewModel,
                        onBack = { backStack.removeLastOrNull() },
                        onConversationClick = { convId, recipientId, recipientName ->
                            backStack.add(Chat(convId, recipientId, recipientName))
                        }
                    )
                }
                entry<Chat> { key ->
                    val chatViewModel: ChatViewModel = viewModel(
                        key = "chat_${key.conversationId}",
                        factory = ChatViewModelFactory(key.conversationId, key.recipientUserId, socialRepository, userDao)
                    )
                    ChatScreen(
                        viewModel = chatViewModel,
                        recipientName = key.recipientName,
                        onBack = { backStack.removeLastOrNull() }
                    )
                }
                entry<Notifications> {
                    val notifViewModel: NotificationsViewModel = viewModel(
                        key = "notifications_vm",
                        factory = NotificationsViewModelFactory(socialRepository, userDao)
                    )
                    NotificationsScreen(
                        viewModel = notifViewModel,
                        onBack = { backStack.removeLastOrNull() },
                        onNavigateToPlan = { planId -> backStack.add(PublicPlanDetail(planId)) },
                        onNavigateToCreator = { creatorId -> backStack.add(CreatorProfile(creatorId)) },
                        onNavigateToChat = { convId, recipientId, recipientName ->
                            backStack.add(Chat(convId, recipientId, recipientName))
                        }
                    )
                }
            }
        )
        }
        }
        }

        if (showCreatePlanDialog) {
            com.example.plannerapp.ui.home.PlanFormDialog(
                title = "New Plan Folder",
                initialName = "",
                initialDescription = "",
                initialDurationDays = 7,
                initialMakeDefault = true,
                initialReminderEnabled = false,
                initialReminderTime = "08:00",
                confirmText = "Create Plan",
                onDismiss = { showCreatePlanDialog = false },
                onSave = { name, desc, durationDays, defaultTaskDuration, reminderEnabled, reminderTime ->
                    homeViewModel.createQuickPlan(
                        title = name,
                        description = desc,
                        durationDays = durationDays,
                        defaultTaskDurationDays = defaultTaskDuration,
                        reminderEnabled = reminderEnabled,
                        reminderTime = reminderTime,
                        onCreated = { newPlanId ->
                            showCreatePlanDialog = false
                            backStack.add(PlanDetail(newPlanId, autoOpenAddTask = true))
                        }
                    )
                    showCreatePlanDialog = false
                }
            )
        }

        if (showCreditHubSheet) {
            com.example.plannerapp.credits.CreditHubSheet(
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
            com.example.plannerapp.ui.home.PlannerProDialog(
                onDismissRequest = { showProDialog = false },
                onVisitStoreClick = {
                    showProDialog = false
                    showCreditHubSheet = true
                }
            )
        }
    }
}
