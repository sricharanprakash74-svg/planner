package com.example.plannerapp.ui.home

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.plannerapp.data.PlannerRepository
import com.example.plannerapp.data.UserDao
import com.example.plannerapp.data.social.SocialRepository

/**
 * Feature wrapper for the Home screen, orchestrating ViewModel state collection
 * and delegating to the modularized Home presentation components.
 */
@Composable
fun HomeScreenWrapper(
    viewModel: HomeViewModel,
    onPlanClick: (Long) -> Unit,
    onPlanCreatedAndOpen: (Long) -> Unit = {},
    onAnalyticsClick: () -> Unit = {},
    onExploreClick: (String) -> Unit = {},
    onSettingsClick: () -> Unit = {},
    plannerRepository: PlannerRepository? = null,
    socialRepository: SocialRepository? = null,
    userDao: UserDao? = null,
    creditViewModel: com.example.plannerapp.credits.CreditViewModel? = null,
    feedViewModel: com.example.plannerapp.ui.social.CommunityFeedViewModel? = null,
    onNavigateToDiscussion: (String) -> Unit = {},
    onCreatorMonetizationClick: () -> Unit = {},
    onBecomeCreatorClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    HomeScreen(
        viewModel = viewModel,
        onPlanClick = onPlanClick,
        onPlanCreatedAndOpen = onPlanCreatedAndOpen,
        onAnalyticsClick = onAnalyticsClick,
        onExploreClick = onExploreClick,
        onSettingsClick = onSettingsClick,
        plannerRepository = plannerRepository,
        socialRepository = socialRepository,
        userDao = userDao,
        creditViewModel = creditViewModel,
        feedViewModel = feedViewModel,
        onNavigateToDiscussion = onNavigateToDiscussion,
        onCreatorMonetizationClick = onCreatorMonetizationClick,
        onBecomeCreatorClick = onBecomeCreatorClick,
        modifier = modifier
    )

}
