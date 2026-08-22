package com.example.plannerapp

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
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
import com.example.plannerapp.ui.components.BottomNavBar
import com.example.plannerapp.ui.components.BottomNavTab
import com.example.plannerapp.ui.create.CreatePlanScreen
import com.example.plannerapp.ui.create.CreatePlanViewModel
import com.example.plannerapp.ui.create.CreatePlanViewModelFactory
import com.example.plannerapp.ui.explore.ExploreScreen
import com.example.plannerapp.ui.home.HomeScreen
import com.example.plannerapp.ui.home.HomeViewModel
import com.example.plannerapp.ui.home.HomeViewModelFactory
import com.example.plannerapp.ui.profile.ProfileScreen
import com.example.plannerapp.ui.profile.ProfileViewModel
import com.example.plannerapp.ui.profile.ProfileViewModelFactory
import com.example.plannerapp.ui.profile.AnalyticsScreen
import com.example.plannerapp.ui.settings.*
import com.example.plannerapp.ui.detail.PlanDetailScreen
import com.example.plannerapp.ui.detail.PlanDetailViewModel
import com.example.plannerapp.ui.detail.PlanDetailViewModelFactory

@Composable
fun MainNavigation() {
    val backStack = rememberNavBackStack(Home)
    var currentTab by remember { mutableStateOf(BottomNavTab.HOME) }

    val context = LocalContext.current
    val database = PlannerDatabase.getDatabase(context)
    val repository = PlannerRepository(database.plannerDao())
    val userDao = database.userDao()
    
    // ViewModels scoped properly
    val homeViewModel: HomeViewModel = viewModel(factory = HomeViewModelFactory(repository, userDao))
    val profileViewModel: ProfileViewModel = viewModel(factory = ProfileViewModelFactory(repository, userDao))
    val createPlanViewModel: CreatePlanViewModel = viewModel(factory = CreatePlanViewModelFactory(repository, userDao))
    val settingsViewModel: SettingsViewModel = viewModel(factory = SettingsViewModelFactory(userDao))

    val triggerSync = {
        WorkManager.getInstance(context).enqueue(OneTimeWorkRequest.from(SyncWorker::class.java))
    }

    Scaffold(
        modifier = Modifier.safeDrawingPadding(),
        bottomBar = {
            // Only show bottom nav if we are not on full-screen modals/subpages
            val currentKey = backStack.lastOrNull()
            if (currentKey == Home || currentKey == Explore || currentKey == Profile) {
                BottomNavBar(
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
                                backStack.add(Explore)
                            }
                            BottomNavTab.PROFILE -> {
                                backStack.clear()
                                backStack.add(Profile)
                            }
                        }
                    }
                )
            }
        }
    ) { innerPadding ->
        NavDisplay(
            backStack = backStack,
            onBack = { backStack.removeLastOrNull() },
            modifier = Modifier.padding(innerPadding),
            entryProvider = entryProvider {
                entry<Home> {
                    HomeScreen(
                        viewModel = homeViewModel,
                        onPlanClick = { planId -> backStack.add(PlanDetail(planId)) }
                    )
                }
                entry<Explore> {
                    ExploreScreen()
                }
                entry<Profile> {
                    ProfileScreen(
                        viewModel = profileViewModel,
                        onSettingsClick = { backStack.add(Settings) },
                        onAnalyticsClick = { backStack.add(Analytics) }
                    )
                }
                entry<Analytics> {
                    AnalyticsScreen(
                        viewModel = profileViewModel,
                        onBack = { backStack.removeLastOrNull() }
                    )
                }
                entry<Settings> {
                    SettingsScreen(
                        viewModel = settingsViewModel,
                        onBack = { backStack.removeLastOrNull() },
                        onEditProfileClick = { backStack.add(SettingsEditProfile) },
                        onNotificationsClick = { backStack.add(SettingsNotifications) },
                        onTimezoneClick = { backStack.add(SettingsTimezone) },
                        onPublishPlanClick = { backStack.add(SettingsPublishPlan) },
                        onBackupClick = { backStack.add(SettingsBackup) },
                        onExportClick = { backStack.add(SettingsExportData) },
                        onDeleteAccountClick = { backStack.add(SettingsDeleteAccount) }
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
                    CreatePlanScreen(
                        viewModel = createPlanViewModel,
                        onClose = { backStack.removeLastOrNull() },
                        onPlanCreated = { triggerSync() }
                    )
                }
                entry<PlanDetail> { key ->
                    val planDetailViewModel: PlanDetailViewModel = viewModel(
                        key = "plan_detail_${key.planId}", 
                        factory = PlanDetailViewModelFactory(key.planId, repository)
                    )
                    PlanDetailScreen(viewModel = planDetailViewModel)
                }
            }
        )
    }
}
