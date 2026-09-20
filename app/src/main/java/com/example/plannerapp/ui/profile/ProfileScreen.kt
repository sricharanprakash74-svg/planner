package com.example.plannerapp.ui.profile

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Article
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.ui.graphics.graphicsLayer
import com.example.plannerapp.theme.PhysicsSpec
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.plannerapp.data.DailyCheckinEntity
import com.example.plannerapp.data.PlanEntity
import com.example.plannerapp.data.UserEntity
import com.example.plannerapp.data.social.CommunityPost
import com.example.plannerapp.data.social.VisibilityMicroChip
import com.example.plannerapp.data.social.VisibilityTier
import com.example.plannerapp.data.social.VoteType
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
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    user: UserEntity?,
    avatarBitmap: ImageBitmap?,
    userPlans: List<PlanEntity>,
    userPosts: List<CommunityPost> = emptyList(),
    publicPostsCount: Int = 0,
    privatePostsCount: Int = 0,
    vaultPostsCount: Int = 0,
    weeklyCheckins: List<DailyCheckinEntity>,
    streak: Int,
    consistencyPercentage: Int,
    creditBalance: Int,
    freezesCount: Int,
    // planId -> completion fraction [0.0, 1.0] from Room.
    // Sourced from the same local Room DB offline and online — no branching.
    planCompletionMap: Map<Long, Float> = emptyMap(),
    // The set of plan IDs that are "Following Plans" (joined from community).
    joinedCommunityPlanIds: Set<Long> = emptySet(),
    followingUsers: List<com.example.plannerapp.data.social.CloudUser> = emptyList(),
    followersUsers: List<com.example.plannerapp.data.social.CloudUser> = emptyList(),
    onToggleFollowUser: (String, Boolean) -> Unit = { _, _ -> },
    onCreatorClick: (String) -> Unit = {},
    // Whether system animations are enabled (false = reduced-motion mode).
    isAnimationEnabled: Boolean = true,
    onPickPhoto: () -> Unit,
    onSettingsClick: () -> Unit,
    onAnalyticsClick: () -> Unit,
    onPlanClick: (Long) -> Unit,
    onEditProfileClick: () -> Unit,
    onOpenCreditHub: () -> Unit,
    onCreatorMonetizationClick: () -> Unit = {},
    onBecomeCreatorClick: () -> Unit = {},
    onCreatePost: (title: String, content: String, category: String, visibility: VisibilityTier) -> Unit = { _, _, _, _ -> },
    onVotePost: (String, VoteType) -> Unit = { _, _ -> },
    onSavePost: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var selectedTab by remember { mutableIntStateOf(0) } // Default to Posts tab
    var followingExpanded by remember { mutableStateOf(true) }
    var savedPublicExpanded by remember { mutableStateOf(false) }
    var showAnalyticsSheet by remember { mutableStateOf(false) }
    var showConnectsSheet by remember { mutableStateOf(false) }
    var showCreatePostSheet by remember { mutableStateOf(false) }
    var showCreatorWaitlistSheet by remember { mutableStateOf(false) }

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

        // 2. Profile Header: Avatar, Stats (Connects, Streak, Posts), Identity, Actions (including Analytics)
        ProfileHeaderSection(
            user = user,
            avatarBitmap = avatarBitmap,
            plansCount = userPlans.size,
            connectsCount = followingUsers.size + followersUsers.size,
            streak = streak,
            consistencyPercentage = consistencyPercentage,
            creditBalance = creditBalance,
            freezesCount = freezesCount,
            postsCount = userPosts.size,
            publicPostsCount = publicPostsCount,
            privatePostsCount = privatePostsCount,
            vaultPostsCount = vaultPostsCount,
            isOwner = true,
            onPickPhoto = onPickPhoto,
            onEditProfileClick = onEditProfileClick,
            onAnalyticsClick = { showAnalyticsSheet = true },
            onConnectsClick = { showConnectsSheet = true },
            onOpenCreditHub = onOpenCreditHub,
            onCreatorMonetizationClick = { showCreatorWaitlistSheet = true },
            onBecomeCreatorClick = { showCreatorWaitlistSheet = true },
            context = context
        )

        Spacer(modifier = Modifier.height(AppDimens.Space16))

        // 3. Reddit-Style Tabs: Posts | Comments | Plans (Analytics de-parented to on-demand sheet)
        ProfileTabsBar(
            selectedTab = selectedTab,
            onTabSelected = { selectedTab = it }
        )

        Spacer(modifier = Modifier.height(AppDimens.Space16))

        // 4. Tab Content
        when (selectedTab) {
            0 -> {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Contextual Post Creation Card Hook with Spring Press
                    val createPostInteraction = remember { MutableInteractionSource() }
                    val isCreatePostPressed by createPostInteraction.collectIsPressedAsState()
                    val createPostScale by animateFloatAsState(
                        targetValue = if (isCreatePostPressed) 0.97f else 1f,
                        animationSpec = if (isCreatePostPressed) PhysicsSpec.PressDown else PhysicsSpec.PressRelease,
                        label = "create_post_scale"
                    )

                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .graphicsLayer {
                                scaleX = createPostScale
                                scaleY = createPostScale
                            }
                            .clickable(
                                interactionSource = createPostInteraction,
                                indication = null
                            ) { showCreatePostSheet = true }
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primaryContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.EditNote,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Share an insight or progress...",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "Post routines, tips, or breakthroughs",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                )
                            }
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                            ) {
                                Text(
                                    text = "Post",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }

                    if (userPosts.isEmpty()) {
                        BuildInPublicEmptyState(
                            onCreatePostClick = { showCreatePostSheet = true }
                        )
                    } else {
                        userPosts.forEachIndexed { index, post ->
                            com.example.plannerapp.ui.components.StaggeredEntrance(index = index) {
                                ProfilePostCard(
                                    post = post,
                                    onVote = { voteType -> onVotePost(post.postId, voteType) },
                                    onToggleSave = { onSavePost(post.postId) }
                                )
                            }
                        }
                    }
                }
            }
            1 -> ProfileCommentsEmptyState()
            2 -> PlansDownSlidersSection(
                userPlans = userPlans,
                consistencyPercentage = consistencyPercentage,
                planCompletionMap = planCompletionMap,
                joinedCommunityPlanIds = joinedCommunityPlanIds,
                isAnimationEnabled = isAnimationEnabled,
                followingExpanded = followingExpanded,
                onToggleFollowing = { followingExpanded = !followingExpanded },
                savedPublicExpanded = savedPublicExpanded,
                onToggleSavedPublic = { savedPublicExpanded = !savedPublicExpanded },
                onPlanClick = onPlanClick
            )
        }

        Spacer(modifier = Modifier.height(AppDimens.Space24))
    }

    // Connects BottomSheet (from Connects Header Stat)
    if (showConnectsSheet) {
        ModalBottomSheet(
            onDismissRequest = { showConnectsSheet = false },
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 12.dp)
            ) {
                ProfileConnectsSection(
                    followingUsers = followingUsers,
                    followersUsers = followersUsers,
                    onToggleFollowUser = onToggleFollowUser,
                    onCreatorClick = { creatorId ->
                        showConnectsSheet = false
                        onCreatorClick(creatorId)
                    }
                )
                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }

    // On-Demand Personal Analytics BottomSheet
    if (showAnalyticsSheet) {
        ModalBottomSheet(
            onDismissRequest = { showAnalyticsSheet = false },
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 12.dp)
            ) {
                ProfileAnalyticsCard(
                    weeklyCheckins = weeklyCheckins,
                    streak = streak,
                    onAnalyticsClick = {
                        showAnalyticsSheet = false
                        onAnalyticsClick()
                    }
                )
                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }

    // Post Creation BottomSheet
    if (showCreatePostSheet) {
        CreatePostSheet(
            onDismissRequest = { showCreatePostSheet = false },
            onPublish = { title, content, category, visibility ->
                onCreatePost(title, content, category, visibility)
                showCreatePostSheet = false
            }
        )
    }

    // Creator Early Access Waitlist BottomSheet
    if (showCreatorWaitlistSheet) {
        com.example.plannerapp.ui.creator.CreatorWaitlistSheet(
            onDismissRequest = { showCreatorWaitlistSheet = false }
        )
    }
}

@Composable
fun BuildInPublicEmptyState(
    onCreatePostClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 32.dp, horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
            modifier = Modifier.size(64.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.Article,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(32.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Build in Public",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "Share lessons learned, daily consistency breakthroughs, and routine advice with fellow creators.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(0.85f)
        )
        Spacer(modifier = Modifier.height(18.dp))
        val postBtnInteraction = remember { MutableInteractionSource() }
        val isPostBtnPressed by postBtnInteraction.collectIsPressedAsState()
        val postBtnScale by animateFloatAsState(
            targetValue = if (isPostBtnPressed) 0.94f else 1f,
            animationSpec = if (isPostBtnPressed) PhysicsSpec.PressDown else PhysicsSpec.PressRelease,
            label = "empty_post_btn_scale"
        )
        Button(
            onClick = onCreatePostClick,
            interactionSource = postBtnInteraction,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.graphicsLayer {
                scaleX = postBtnScale
                scaleY = postBtnScale
            }
        ) {
            Icon(
                imageVector = Icons.Outlined.Add,
                contentDescription = null,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "Create Your First Post",
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
fun ProfilePostCard(
    post: CommunityPost,
    onVote: (VoteType) -> Unit,
    onToggleSave: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header: Category, Visibility Micro-Chip, Date
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                    ) {
                        Text(
                            text = post.category,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                    VisibilityMicroChip(tier = VisibilityTier.fromId(post.visibility))
                }
                Text(
                    text = formatPostTime(post.createdAt),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Title
            Text(
                text = post.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            if (post.description.isNotBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = post.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 4,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Action bar: Upvotes, Comments count, Bookmark
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Upvote
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { onVote(VoteType.UP) }
                            .padding(4.dp)
                    ) {
                        Icon(
                            imageVector = if (post.userVote == VoteType.UP) Icons.Filled.ThumbUp else Icons.Outlined.ThumbUp,
                            contentDescription = "Upvote",
                            tint = if (post.userVote == VoteType.UP) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "${post.upvoteCount}",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Medium,
                            color = if (post.userVote == VoteType.UP) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Comments count
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.ChatBubbleOutline,
                            contentDescription = "Comments",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "${post.commentCount}",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Bookmark / Save
                IconButton(
                    onClick = onToggleSave,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = if (post.isSaved) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
                        contentDescription = "Save post",
                        tint = if (post.isSaved) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

private fun formatPostTime(timestamp: Long): String {
    val diff = System.currentTimeMillis() - timestamp
    val seconds = diff / 1000
    val minutes = seconds / 60
    val hours = minutes / 60
    val days = hours / 24
    return when {
        days > 0 -> "${days}d ago"
        hours > 0 -> "${hours}h ago"
        minutes > 0 -> "${minutes}m ago"
        else -> "Just now"
    }
}

@Composable
fun ProfileConnectsSection(
    followingUsers: List<com.example.plannerapp.data.social.CloudUser>,
    followersUsers: List<com.example.plannerapp.data.social.CloudUser>,
    onToggleFollowUser: (String, Boolean) -> Unit,
    onCreatorClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var connectsSubTab by remember { mutableIntStateOf(0) } // 0 = Following, 1 = Followers

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Minimalist Segment Control
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(10.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
            border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(3.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Tab 0: Following
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .height(34.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { connectsSubTab = 0 },
                    shape = RoundedCornerShape(8.dp),
                    color = if (connectsSubTab == 0) MaterialTheme.colorScheme.surface else androidx.compose.ui.graphics.Color.Transparent,
                    shadowElevation = if (connectsSubTab == 0) 1.dp else 0.dp,
                    border = if (connectsSubTab == 0) BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f)) else null
                ) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                        Text(
                            text = "Following (${followingUsers.size})",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = if (connectsSubTab == 0) FontWeight.Bold else FontWeight.Medium,
                            color = if (connectsSubTab == 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Tab 1: Followers
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .height(34.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { connectsSubTab = 1 },
                    shape = RoundedCornerShape(8.dp),
                    color = if (connectsSubTab == 1) MaterialTheme.colorScheme.surface else androidx.compose.ui.graphics.Color.Transparent,
                    shadowElevation = if (connectsSubTab == 1) 1.dp else 0.dp,
                    border = if (connectsSubTab == 1) BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f)) else null
                ) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                        Text(
                            text = "Followers (${followersUsers.size})",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = if (connectsSubTab == 1) FontWeight.Bold else FontWeight.Medium,
                            color = if (connectsSubTab == 1) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        val followingIds = remember(followingUsers) { followingUsers.map { it.userId }.toSet() }

        if (connectsSubTab == 0) {
            if (followingUsers.isEmpty()) {
                ProfileConnectsEmptyState(isFollowingTab = true)
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth().heightIn(max = 450.dp)
                ) {
                    itemsIndexed(followingUsers, key = { _, user -> user.userId }) { index, user ->
                        com.example.plannerapp.ui.components.StaggeredEntrance(index = index) {
                            ConnectUserCard(
                                user = user,
                                isFollowing = true,
                                onToggleFollow = { onToggleFollowUser(user.userId, true) },
                                onClick = { onCreatorClick(user.userId) }
                            )
                        }
                    }
                }
            }
        } else {
            if (followersUsers.isEmpty()) {
                ProfileConnectsEmptyState(isFollowingTab = false)
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth().heightIn(max = 450.dp)
                ) {
                    itemsIndexed(followersUsers, key = { _, user -> user.userId }) { index, user ->
                        val isFollowing = followingIds.contains(user.userId)
                        com.example.plannerapp.ui.components.StaggeredEntrance(index = index) {
                            ConnectUserCard(
                                user = user,
                                isFollowing = isFollowing,
                                onToggleFollow = { onToggleFollowUser(user.userId, isFollowing) },
                                onClick = { onCreatorClick(user.userId) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ConnectUserCard(
    user: com.example.plannerapp.data.social.CloudUser,
    isFollowing: Boolean,
    onToggleFollow: () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
        border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f)),
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = user.displayName.firstOrNull()?.uppercase() ?: "U",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = user.displayName,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (user.isCreator) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.Filled.Check,
                            contentDescription = "Creator",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
                Text(
                    text = "@${user.username.ifBlank { "user" }}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (user.bio.isNotBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = user.bio,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            if (isFollowing) {
                OutlinedButton(
                    onClick = onToggleFollow,
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                    modifier = Modifier.height(32.dp)
                ) {
                    Text(
                        text = "Following",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            } else {
                Button(
                    onClick = onToggleFollow,
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                    modifier = Modifier.height(32.dp)
                ) {
                    Text(
                        text = "Follow Back",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
