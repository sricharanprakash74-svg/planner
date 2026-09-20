package com.example.plannerapp.ui.profile

import android.graphics.BitmapFactory
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.plannerapp.credits.CreditHubSheet
import com.example.plannerapp.credits.CreditRepository
import com.example.plannerapp.credits.CreditViewModel
import com.example.plannerapp.credits.CreditViewModelFactory
import com.example.plannerapp.data.PlannerDatabase
import com.example.plannerapp.data.PlannerRepository
import com.example.plannerapp.ui.state.Resource
import java.io.File

@Composable
fun ProfileScreenWrapper(
    viewModel: ProfileViewModel,
    onSettingsClick: () -> Unit,
    onAnalyticsClick: () -> Unit,
    onPlanClick: (Long) -> Unit = {},
    onCreatorClick: (String) -> Unit = {},
    onEditProfileClick: () -> Unit = {},
    onCreatorMonetizationClick: () -> Unit = {},
    onBecomeCreatorClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val uiStateResource by viewModel.uiState.collectAsStateWithLifecycle()
    val avatarVersion by viewModel.avatarVersion.collectAsStateWithLifecycle()

    val uiState = when (val state = uiStateResource) {
        is Resource.Loading -> {
            Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return
        }
        is Resource.Error -> {
            Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = "Error: ${state.message}",
                    color = MaterialTheme.colorScheme.error
                )
            }
            return
        }
        is Resource.Success -> state.data
    }

    val user = uiState.user

    // Gallery launcher for selecting profile photo
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let { viewModel.updateProfilePicture(context, it) }
    }

    // Memory-safe decoded local avatar bitmap
    val avatarBitmap = remember(user?.avatarUrl, avatarVersion) {
        user?.avatarUrl?.let { path ->
            try {
                val file = File(path)
                if (file.exists()) {
                    val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                    BitmapFactory.decodeFile(path, options)
                    var inSampleSize = 1
                    val reqSize = 300
                    if (options.outHeight > reqSize || options.outWidth > reqSize) {
                        val halfH = options.outHeight / 2
                        val halfW = options.outWidth / 2
                        while ((halfH / inSampleSize) >= reqSize && (halfW / inSampleSize) >= reqSize) {
                            inSampleSize *= 2
                        }
                    }
                    val decodeOptions = BitmapFactory.Options().apply { this.inSampleSize = inSampleSize }
                    BitmapFactory.decodeFile(path, decodeOptions)?.asImageBitmap()
                } else null
            } catch (e: Exception) {
                null
            }
        }
    }

    // Credit ViewModel for credit hub
    var showCreditHub by remember { mutableStateOf(false) }
    val creditViewModel: CreditViewModel = viewModel(
        factory = remember {
            val db = PlannerDatabase.getDatabase(context)
            CreditViewModelFactory(CreditRepository(db.creditDao()), db.userDao())
        }
    )
    val creditBalance by creditViewModel.balanceFlow.collectAsState()
    val freezesCount by creditViewModel.freezesFlow.collectAsState()

    val userPosts = uiState.userPosts
    val publicPostsCount  = remember(userPosts) { userPosts.count { it.visibility.equals("public",  ignoreCase = true) } }
    val privatePostsCount = remember(userPosts) { userPosts.count { it.visibility.equals("private", ignoreCase = true) } }
    val vaultPostsCount   = remember(userPosts) { userPosts.count { it.visibility.equals("vault",   ignoreCase = true) } }

    // -- Per-plan completion fractions (offline-first, Room-backed) ---------
    // We create one PlannerRepository instance (backed by the local Room DB)
    // and collect a getPlanCompletionFraction() Flow for every Following Plan.
    //
    // Why this is safe offline AND online:
    //  - Offline: local task completions write to daily_checkins in Room.
    //    The Flow emits immediately. The fluid rises. No network needed.
    //  - Online (post-sync): the sync worker writes the server's state into
    //    the same daily_checkins table. The same Flow emits. The fluid updates.
    //    No special online/offline branching anywhere.
    // Both modes converge through the same Room ? Flow ? collectAsState path.
    val joinedPlanIds = uiState.joinedCommunityPlanIds
    val followingPlans = remember(uiState.userPlans, joinedPlanIds) {
        uiState.userPlans.filter { it.sourcePlanId == null || it.planId in joinedPlanIds }
    }

    val localRepository = remember(context) {
        PlannerRepository(PlannerDatabase.getDatabase(context).plannerDao())
    }

    // One collectAsState per plan — Compose only re-renders the card whose
    // fraction changed. This avoids sweeping recompositions across all cards.
    val planCompletionMap: Map<Long, Float> = followingPlans.associate { plan ->
        val fraction by localRepository
            .getPlanCompletionFraction(plan.planId)
            .collectAsState(initial = 0f)
        plan.planId to fraction
    }

    // Reduced-motion: check Android's ANIMATOR_DURATION_SCALE system setting.
    // A scale of 0.0 means "Remove animations" is active (Developer Options or
    // Accessibility -> Animation settings). This is the correct cross-version way
    // to detect reduced-motion on Android -- LocalAccessibilityManager does not
    // expose isAnimationEnabled in all Compose versions.
    val isAnimationEnabled = remember {
        val scale = android.provider.Settings.Global.getFloat(
            context.contentResolver,
            android.provider.Settings.Global.ANIMATOR_DURATION_SCALE,
            1f
        )
        scale > 0f
    }

    ProfileScreen(
        user = user,
        avatarBitmap = avatarBitmap,
        userPlans = uiState.userPlans,
        userPosts = userPosts,
        publicPostsCount = publicPostsCount,
        privatePostsCount = privatePostsCount,
        vaultPostsCount = vaultPostsCount,
        weeklyCheckins = uiState.weeklyCheckins,
        streak = uiState.streak,
        consistencyPercentage = uiState.consistencyPercentage,
        creditBalance = creditBalance,
        freezesCount = freezesCount,
        planCompletionMap = planCompletionMap,
        joinedCommunityPlanIds = joinedPlanIds,
        followingUsers = uiState.followingUsers,
        followersUsers = uiState.followersUsers,
        onToggleFollowUser = { targetId, isFollowing -> viewModel.toggleFollowUser(targetId, isFollowing) },
        onCreatorClick = onCreatorClick,
        isAnimationEnabled = isAnimationEnabled,
        onPickPhoto = { galleryLauncher.launch("image/*") },
        onSettingsClick = onSettingsClick,
        onAnalyticsClick = onAnalyticsClick,
        onPlanClick = onPlanClick,
        onEditProfileClick = onEditProfileClick,
        onOpenCreditHub = { showCreditHub = true },
        onCreatorMonetizationClick = onCreatorMonetizationClick,
        onBecomeCreatorClick = onBecomeCreatorClick,
        onCreatePost = { title, content, category, visibilityTier ->
            viewModel.createPost(
                title = title,
                content = content,
                visibility = visibilityTier.id,
                category = category
            )
        },
        onVotePost = { postId, voteType -> viewModel.onVote(postId, voteType) },
        onSavePost = { postId -> viewModel.toggleSave(postId) },
        modifier = modifier
    )

    if (showCreditHub) {
        CreditHubSheet(
            viewModel = creditViewModel,
            onDismissRequest = { showCreditHub = false }
        )
    }
}


