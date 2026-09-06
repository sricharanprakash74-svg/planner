package com.example.plannerapp.ui.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.Assignment
import androidx.compose.material.icons.automirrored.outlined.TrendingUp
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.outlined.People
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.plannerapp.data.social.CloudUser
import com.example.plannerapp.data.social.VoteType
import com.example.plannerapp.ui.feed.CommunityPlanCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreatorProfileScreen(
    viewModel: CreatorProfileViewModel,
    onBack: () -> Unit,
    onPostClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val haptic = LocalHapticFeedback.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = uiState.creator?.displayName ?: "Creator Profile",
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        modifier = modifier
    ) { paddingValues ->
        if (uiState.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        val creator = uiState.creator

        if (creator == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "Creator not found",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedButton(onClick = onBack) {
                        Text("Go Back")
                    }
                }
            }
            return@Scaffold
        }

        val totalJoins = uiState.posts.sumOf { it.joinCount }.coerceAtLeast(creator.totalMembersJoined)

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(vertical = 12.dp)
        ) {
            // ── Creator Header Card ─────────────────────────────
            item {
                CreatorHeaderSection(
                    creator = creator,
                    isFollowing = uiState.isFollowing,
                    onToggleFollow = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        viewModel.toggleFollow()
                    }
                )
            }

            // ── Metrics Stat Bar ────────────────────────────────
            item {
                CreatorStatsRow(
                    publishedPlansCount = uiState.posts.size,
                    totalJoins = totalJoins,
                    followersCount = creator.followerCount
                )
            }

            // ── Published Plans Header ──────────────────────────
            item {
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Published Plans (${uiState.posts.size})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            // ── Plans List or Empty Placeholder ─────────────────
            if (uiState.posts.isEmpty()) {
                item {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Outlined.Assignment,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(36.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "No published plans yet",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                items(uiState.posts, key = { it.postId }) { post ->
                    CommunityPlanCard(
                        creatorName = post.author.displayName,
                        creatorAvatarUrl = post.author.avatarUrl,
                        planTitle = post.title,
                        planDescription = post.description,
                        durationText = "${post.durationDays} days",
                        upvoteCount = post.score,
                        isSaved = post.isSaved,
                        onUpvote = { viewModel.onVote(post.postId, VoteType.UP) },
                        onDownvote = { viewModel.onVote(post.postId, VoteType.DOWN) },
                        onSaveToggle = { viewModel.toggleSave(post.postId) },
                        onCardClick = { onPostClick(post.postId) }
                    )
                }
            }
        }
    }
}

@Composable
private fun CreatorHeaderSection(
    creator: CloudUser,
    isFollowing: Boolean,
    onToggleFollow: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Avatar circle
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = creator.displayName.take(1).uppercase(),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = creator.displayName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        if (creator.isCreator) {
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                imageVector = Icons.Filled.Verified,
                                contentDescription = "Verified Creator",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                    Text(
                        text = "@${creator.username}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Follow / Following Action Button
                if (isFollowing) {
                    OutlinedButton(
                        onClick = onToggleFollow,
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Check,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Following", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                    }
                } else {
                    Button(
                        onClick = onToggleFollow,
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.PersonAdd,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Follow", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                    }
                }
            }

            if (creator.bio.isNotBlank()) {
                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    text = creator.bio,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun CreatorStatsRow(
    publishedPlansCount: Int,
    totalJoins: Int,
    followersCount: Int
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            StatMetricItem(
                icon = Icons.AutoMirrored.Outlined.Assignment,
                value = publishedPlansCount.toString(),
                label = "Plans"
            )
            VerticalDivider(modifier = Modifier.height(30.dp), color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f))
            StatMetricItem(
                icon = Icons.AutoMirrored.Outlined.TrendingUp,
                value = formatMetricNumber(totalJoins),
                label = "Total Joins"
            )
            VerticalDivider(modifier = Modifier.height(30.dp), color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f))
            StatMetricItem(
                icon = Icons.Outlined.People,
                value = formatMetricNumber(followersCount),
                label = "Followers"
            )
        }
    }
}

@Composable
private fun StatMetricItem(
    icon: ImageVector,
    value: String,
    label: String
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

private fun formatMetricNumber(num: Int): String {
    return when {
        num >= 1_000_000 -> String.format("%.1fM", num / 1_000_000.0)
        num >= 1_000 -> String.format("%.1fk", num / 1_000.0)
        else -> num.toString()
    }
}
