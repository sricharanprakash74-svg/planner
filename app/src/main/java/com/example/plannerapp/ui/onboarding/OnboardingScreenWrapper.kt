package com.example.plannerapp.ui.onboarding

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.plannerapp.data.UserDao
import com.example.plannerapp.theme.AppDimens
import com.example.plannerapp.theme.AppDisabledBgLight
import com.example.plannerapp.theme.AppPrimary

@Composable
fun OnboardingScreenWrapper(
    onFinish: () -> Unit,
    onUpgradeClick: () -> Unit,
    userDao: UserDao
) {
    val context = LocalContext.current
    val viewModel: OnboardingViewModel = viewModel(
        factory = OnboardingViewModelFactory(userDao, context.applicationContext)
    )

    val currentStep by viewModel.currentStep.collectAsState()
    val displayName by viewModel.displayName.collectAsState()
    val username by viewModel.username.collectAsState()
    val usernameValidationState by viewModel.usernameValidationState.collectAsState()
    val usernameErrorMessage by viewModel.usernameErrorMessage.collectAsState()
    val selectedInterests by viewModel.selectedInterests.collectAsState()
    val avatarUri by viewModel.avatarUri.collectAsState()

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = AppDimens.ScreenHorizontalPadding, vertical = AppDimens.Space12),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Stepper: 2 segments, 4dp height, CircleShape, 6dp gap
                Row(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    for (i in 1..2) {
                        val isCompleteOrCurrent = i <= currentStep
                        val color = if (isCompleteOrCurrent) AppPrimary else AppDisabledBgLight
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(AppDimens.ProgressBarHeight)
                                .clip(CircleShape)
                                .background(color)
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            AnimatedContent(
                targetState = currentStep,
                transitionSpec = {
                    if (targetState > initialState) {
                        (slideInHorizontally { width -> width } + fadeIn())
                            .togetherWith(slideOutHorizontally { width -> -width } + fadeOut())
                    } else {
                        (slideInHorizontally { width -> -width } + fadeIn())
                            .togetherWith(slideOutHorizontally { width -> width } + fadeOut())
                    }
                },
                label = "onboarding_step_anim"
            ) { step ->
                when (step) {
                    1 -> OnboardingProfileScreen(
                        displayName = displayName,
                        onDisplayNameChange = { viewModel.setDisplayName(it) },
                        username = username,
                        onUsernameChange = { viewModel.setUsername(it) },
                        usernameValidationState = usernameValidationState,
                        usernameErrorMessage = usernameErrorMessage,
                        avatarUri = avatarUri,
                        onAvatarSelected = { viewModel.setAvatarUri(it) },
                        onContinue = {
                            viewModel.saveProfile(context) {
                                viewModel.setStep(2)
                            }
                        }
                    )
                    2 -> OnboardingPreferencesScreen(
                        selectedInterests = selectedInterests,
                        onToggleInterest = { viewModel.toggleInterest(it) },
                        onContinue = {
                            viewModel.saveInterests {
                                viewModel.finishOnboarding {
                                    onFinish()
                                }
                            }
                        }
                    )
                }
            }
        }
    }
}
