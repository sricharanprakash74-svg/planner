package com.example.plannerapp

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkManager
import com.example.plannerapp.data.PlannerDatabase
import com.example.plannerapp.data.PlannerRepository
import com.example.plannerapp.sync.SyncWorker
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
import com.example.plannerapp.data.social.InMemorySocialRepository
import com.example.plannerapp.data.social.SocialRepository
import com.example.plannerapp.ui.social.CommunityDiscussionScreen
import com.example.plannerapp.ui.social.CommunityDiscussionViewModel
import com.example.plannerapp.ui.social.CommunityDiscussionViewModelFactory
import com.example.plannerapp.ui.social.CommunityFeedViewModel
import com.example.plannerapp.ui.social.CommunityFeedViewModelFactory
import com.example.plannerapp.ui.auth.SignInScreenWrapper
import com.example.plannerapp.ui.auth.AuthViewModel
import com.example.plannerapp.ui.auth.AuthViewModelFactory

@Composable
fun MainNavigation() {
    val context = LocalContext.current
    val database = PlannerDatabase.getDatabase(context)
    val repository = PlannerRepository(database.plannerDao())
    val userDao = database.userDao()
    val socialRepository: SocialRepository = remember { InMemorySocialRepository() }

    // Instant session restoration: check if user already signed in (guest or cloud)
    var isAuthChecked by remember { mutableStateOf(false) }
    var initialHasUser by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        val activeUser = userDao.getActiveUserOnce()
        initialHasUser = activeUser != null
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

    val backStack = rememberNavBackStack(if (initialHasUser) Home else SignIn)
    var currentTab by remember { mutableStateOf(BottomNavTab.HOME) }
    
    // ViewModels scoped properly
    val homeViewModel: HomeViewModel = viewModel(factory = HomeViewModelFactory(repository, userDao, context.applicationContext))
    val profileViewModel: ProfileViewModel = viewModel(factory = ProfileViewModelFactory(repository, userDao))
    val createPlanViewModel: CreatePlanViewModel = viewModel(factory = CreatePlanViewModelFactory(repository, userDao, context.applicationContext))
    val settingsViewModel: SettingsViewModel = viewModel(factory = SettingsViewModelFactory(userDao))
    val authViewModel: AuthViewModel = viewModel(factory = AuthViewModelFactory(userDao))

    val triggerSync = {
        WorkManager.getInstance(context).enqueue(OneTimeWorkRequest.from(SyncWorker::class.java))
    }

    val currentKey = backStack.lastOrNull() ?: if (initialHasUser) Home else SignIn

    // Synchronize bottom bar active tab indicator with the current destination on the back stack
    LaunchedEffect(currentKey) {
        when (currentKey) {
            Home -> currentTab = BottomNavTab.HOME
            Profile -> currentTab = BottomNavTab.PROFILE
            else -> { /* Subpages keep their parent tab active */ }
        }
    }

    // Handle system back gesture & hardware back button
    BackHandler(enabled = backStack.size > 1 || (currentKey != Home && currentKey != SignIn)) {
        if (backStack.size > 1) {
            backStack.removeLastOrNull()
        } else if (currentKey != Home && currentKey != SignIn) {
            backStack.clear()
            backStack.add(Home)
            currentTab = BottomNavTab.HOME
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

    AppScaffoldWrapper(
        currentTab = currentTab,
        onTabSelected = { tab ->
            currentTab = tab
            when (tab) {
                BottomNavTab.HOME -> {
                    backStack.clear()
                    backStack.add(Home)
                }
                BottomNavTab.PROFILE -> {
                    backStack.clear()
                    backStack.add(Home)
                    backStack.add(Profile)
                }
            }
        },
        showBottomBar = currentKey == Home || currentKey == Profile
    ) { innerPadding ->
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
            modifier = Modifier.padding(innerPadding),
            entryProvider = entryProvider {
                entry<Home> {
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
                        onNavigateToDiscussion = { postId -> backStack.add(CommunityDiscussion(postId)) }
                    )
                }
                entry<Explore> { key ->
                    val feedViewModel: CommunityFeedViewModel = viewModel(
                        key = "explore_feed_${key.initialQuery}",
                        factory = CommunityFeedViewModelFactory(socialRepository, key.initialQuery)
                    )
                    ExploreScreen(
                        viewModel = feedViewModel,
                        onPostClick = { postId -> backStack.add(CommunityDiscussion(postId)) },
                        onCreatorClick = { creatorId -> backStack.add(CreatorProfile(creatorId)) }
                    )
                }
                entry<CommunityDiscussion> { key ->
                    val discussionViewModel: CommunityDiscussionViewModel = viewModel(
                        key = "discussion_${key.postId}",
                        factory = CommunityDiscussionViewModelFactory(key.postId, socialRepository, repository, userDao)
                    )
                    CommunityDiscussionScreen(
                        viewModel = discussionViewModel,
                        onBack = { backStack.removeLastOrNull() },
                        onPlanJoinedAndOpen = { newPlanId ->
                            backStack.add(PlanDetail(newPlanId))
                        },
                        onCreatorClick = { creatorId -> backStack.add(CreatorProfile(creatorId)) }
                    )
                }
                entry<Profile> {
                    ProfileScreenWrapper(
                        viewModel = profileViewModel,
                        onSettingsClick = { backStack.add(Settings) },
                        onAnalyticsClick = { backStack.add(Analytics) },
                        onPlanClick = { planId -> backStack.add(PlanDetail(planId)) },
                        onEditProfileClick = { backStack.add(SettingsEditProfile) }
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
                        },
                        onContinueAsGuest = {
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
                        onNotificationsClick = { backStack.add(SettingsNotifications) },
                        onTimezoneClick = { backStack.add(SettingsTimezone) },
                        onPublishPlanClick = { backStack.add(SettingsPublishPlan) },
                        onBackupClick = { backStack.add(SettingsBackup) },
                        onExportClick = { backStack.add(SettingsExportData) },
                        onDeleteAccountClick = { backStack.add(SettingsDeleteAccount) },
                        onViewCreatorProfileClick = { creatorId -> backStack.add(CreatorProfile(creatorId)) },
                        onSignInClick = { backStack.add(SignIn) }
                    )
                }
                entry<SettingsEditProfile> {
                    EditProfileScreen(
                        viewModel = settingsViewModel,
                        onBack = { backStack.removeLastOrNull() }
                    )
                }
                entry<SettingsNotifications> {
                    NotificationSettingsScreen(
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
                        onBack = { backStack.removeLastOrNull() }
                    )
                }
                entry<SettingsBackup> {
                    BackupSettingsScreen(
                        onBack = { backStack.removeLastOrNull() }
                    )
                }
                entry<SettingsExportData> {
                    ExportDataScreen(
                        onBack = { backStack.removeLastOrNull() }
                    )
                }
                entry<SettingsDeleteAccount> {
                    DeleteAccountScreen(
                        viewModel = settingsViewModel,
                        onBack = { backStack.removeLastOrNull() },
                        onAccountDeleted = {
                            backStack.clear()
                            backStack.add(Home)
                        }
                    )
                }
                entry<CreatePlan> {
                    CreatePlanScreenWrapper(
                        viewModel = createPlanViewModel,
                        onClose = { backStack.removeLastOrNull() },
                        onPlanCreated = { triggerSync() }
                    )
                }
                entry<PlanDetail> { key ->
                    val planDetailViewModel: PlanDetailViewModel = viewModel(
                        key = "plan_detail_${key.planId}",
                        factory = PlanDetailViewModelFactory(key.planId, repository, context.applicationContext)
                    )
                    PlanDetailScreen(
                        viewModel = planDetailViewModel,
                        autoOpenAddTask = key.autoOpenAddTask,
                        onBack = { backStack.removeLastOrNull() },
                        onOpenCommunityDiscussion = { postId -> backStack.add(CommunityDiscussion(postId)) }
                    )
                }
                entry<CreatorProfile> { key ->
                    val creatorViewModel: CreatorProfileViewModel = viewModel(
                        key = "creator_profile_${key.userId}",
                        factory = CreatorProfileViewModelFactory(key.userId, socialRepository)
                    )
                    CreatorProfileScreen(
                        viewModel = creatorViewModel,
                        onBack = { backStack.removeLastOrNull() },
                        onPostClick = { postId -> backStack.add(CommunityDiscussion(postId)) }
                    )
                }
            }
        )
    }
}
