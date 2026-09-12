package com.example.plannerapp.ui.profile

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.platform.LocalContext
import com.example.plannerapp.data.DailyCheckinEntity
import com.example.plannerapp.data.PlanEntity
import com.example.plannerapp.data.UserEntity
import com.example.plannerapp.theme.AppDimens

/**
 * Backward-compatible ViewModel entry point delegating to ProfileScreenWrapper.
 */
@Composable
fun ProfileScreen(
    viewModel: ProfileViewModel,
    onSettingsClick: () -> Unit,
    onAnalyticsClick: () -> Unit,
    onPlanClick: (Long) -> Unit = {},
    onEditProfileClick: () -> Unit = {},
    onCreatorMonetizationClick: () -> Unit = {},
    onBecomeCreatorClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    ProfileScreenWrapper(
        viewModel = viewModel,
        onSettingsClick = onSettingsClick,
        onAnalyticsClick = onAnalyticsClick,
        onPlanClick = onPlanClick,
        onEditProfileClick = onEditProfileClick,
        onCreatorMonetizationClick = onCreatorMonetizationClick,
        onBecomeCreatorClick = onBecomeCreatorClick,
        modifier = modifier
    )
}

/**
 * Pure stateless presentation composable for the Profile Screen.
 * All spacing is locked to the 4pt/8pt grid via AppDimens.
 */
@Composable
fun ProfileScreen(
    user: UserEntity?,
    avatarBitmap: ImageBitmap?,
    userPlans: List<PlanEntity>,
    weeklyCheckins: List<DailyCheckinEntity>,
    streak: Int,
    consistencyPercentage: Int,
    creditBalance: Int,
    freezesCount: Int,
    onPickPhoto: () -> Unit,
    onSettingsClick: () -> Unit,
    onAnalyticsClick: () -> Unit,
    onPlanClick: (Long) -> Unit,
    onEditProfileClick: () -> Unit,
    onOpenCreditHub: () -> Unit,
    onCreatorMonetizationClick: () -> Unit = {},
    onBecomeCreatorClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var selectedTab by remember { mutableIntStateOf(2) } // Default to Plans tab
    var followingExpanded by remember { mutableStateOf(true) } // Following plans open by default
    var savedPublicExpanded by remember { mutableStateOf(false) }

    val handle = remember(user?.displayName) {
        val raw = user?.displayName ?: "user"
        raw.trim().lowercase().replace("\\s+".toRegex(), "_")
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = AppDimens.Space16),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(AppDimens.Space12))

        // 1. Top Bar: Handle Dropdown & Settings Action
        ProfileTopBar(
            handle = handle,
            creditBalance = creditBalance,
            onOpenCreditHub = onOpenCreditHub,
            onSettingsClick = onSettingsClick
        )

        Spacer(modifier = Modifier.height(AppDimens.Space16))

        // 2. Profile Header: Avatar, Stats, Identity, Bio, Actions, and Credits Pill
        ProfileHeaderSection(
            user = user,
            avatarBitmap = avatarBitmap,
            plansCount = userPlans.size,
            streak = streak,
            consistencyPercentage = consistencyPercentage,
            creditBalance = creditBalance,
            freezesCount = freezesCount,
            onPickPhoto = onPickPhoto,
            onEditProfileClick = onEditProfileClick,
            onOpenCreditHub = onOpenCreditHub,
            onCreatorMonetizationClick = onCreatorMonetizationClick,
            onBecomeCreatorClick = onBecomeCreatorClick,
            context = context
        )

        Spacer(modifier = Modifier.height(AppDimens.Space16))

        // 3. Analytics Card (in place of account suggestions carousel)
        ProfileAnalyticsCard(
            weeklyCheckins = weeklyCheckins,
            streak = streak,
            onAnalyticsClick = onAnalyticsClick
        )

        Spacer(modifier = Modifier.height(AppDimens.Space16))

        // 4. Reddit-Style Tabs: Posts | Comments | Plans
        ProfileTabsBar(
            selectedTab = selectedTab,
            onTabSelected = { selectedTab = it }
        )

        Spacer(modifier = Modifier.height(AppDimens.Space16))

        // 5. Tab Content
        when (selectedTab) {
            0 -> ProfilePostsEmptyState()
            1 -> ProfileCommentsEmptyState()
            2 -> PlansDownSlidersSection(
                userPlans = userPlans,
                consistencyPercentage = consistencyPercentage,
                followingExpanded = followingExpanded,
                onToggleFollowing = { followingExpanded = !followingExpanded },
                savedPublicExpanded = savedPublicExpanded,
                onToggleSavedPublic = { savedPublicExpanded = !savedPublicExpanded },
                onPlanClick = onPlanClick
            )
        }

        Spacer(modifier = Modifier.height(AppDimens.Space24))
    }
}
