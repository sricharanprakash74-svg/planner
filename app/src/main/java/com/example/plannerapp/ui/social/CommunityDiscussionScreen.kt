package com.example.plannerapp.ui.social

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.outlined.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.plannerapp.data.social.CommunityPost
import com.example.plannerapp.data.social.PostComment
import com.example.plannerapp.data.social.VoteType
import com.example.plannerapp.data.template.PlanTemplateDto
import com.example.plannerapp.data.template.TaskTemplateDto
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommunityDiscussionScreen(
    viewModel: CommunityDiscussionViewModel,
    onBack: () -> Unit,
    onPlanJoinedAndOpen: (Long) -> Unit,
    onCreatorClick: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val haptic = LocalHapticFeedback.current
    var commentInputText by remember { mutableStateOf("") }
    var showJoinConfirmDialog by remember { mutableStateOf(false) }

    // Navigate to local plan if successfully joined
    LaunchedEffect(uiState.joinedLocalPlanId) {
        uiState.joinedLocalPlanId?.let { newPlanId ->
            viewModel.clearJoinedEvent()
            onPlanJoinedAndOpen(newPlanId)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = uiState.post?.category ?: "Plan Discussion",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    uiState.post?.let { post ->
                        val isJoined = uiState.localPlanAlreadyJoinedId != null
                        AssistChip(
                            onClick = {
                                if (isJoined) {
                                    onPlanJoinedAndOpen(uiState.localPlanAlreadyJoinedId!!)
                                } else {
                                    showJoinConfirmDialog = true
                                }
                            },
                            label = {
                                Text(
                                    text = if (isJoined) "Joined" else "Join (${post.joinCount})",
                                    fontWeight = FontWeight.Bold
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = if (isJoined) Icons.Filled.Check else Icons.Filled.Add,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            },
                            colors = AssistChipDefaults.assistChipColors(
                                containerColor = if (isJoined) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.primaryContainer,
                                labelColor = if (isJoined) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onPrimaryContainer
                            ),
                            modifier = Modifier.padding(end = 8.dp)
                        )
                    }
                }
            )
        },
        bottomBar = {
            // Comment input bar
            Surface(
                tonalElevation = 3.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                    // Replying to banner
                    AnimatedVisibility(visible = uiState.replyToComment != null) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 6.dp)
                        ) {
                            Text(
                                text = "Replying to @${uiState.replyToComment?.author?.username}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.SemiBold
                            )
                            IconButton(
                                onClick = { viewModel.onSetReplyTo(null) },
                                modifier = Modifier.size(20.dp)
                            ) {
                                Icon(Icons.Filled.Close, contentDescription = "Cancel reply", modifier = Modifier.size(14.dp))
                            }
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedTextField(
                            value = commentInputText,
                            onValueChange = { commentInputText = it },
                            placeholder = { Text("Add to the discussion...") },
                            modifier = Modifier.weight(1f),
                            maxLines = 3,
                            shape = RoundedCornerShape(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        IconButton(
                            onClick = {
                                if (commentInputText.isNotBlank()) {
                                    viewModel.postComment(commentInputText.trim())
                                    commentInputText = ""
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                }
                            },
                            enabled = commentInputText.isNotBlank(),
                            colors = IconButtonDefaults.filledIconButtonColors()
                        ) {
                            Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send Comment")
                        }
                    }
                }
            }
        },
        modifier = modifier
    ) { paddingValues ->
        val post = uiState.post

        if (post == null) {
            Box(modifier = Modifier.fillMaxSize().padding(paddingValues), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // ── Post Details Card ──────────────────────────────────────
            item {
                PostDetailHeaderCard(
                    post = post,
                    isAlreadyJoined = uiState.localPlanAlreadyJoinedId != null,
                    onUpvote = { viewModel.onVote(VoteType.UP) },
                    onDownvote = { viewModel.onVote(VoteType.DOWN) },
                    onJoin = { showJoinConfirmDialog = true },
                    onOpenLocalPlan = {
                        uiState.localPlanAlreadyJoinedId?.let { onPlanJoinedAndOpen(it) }
                    },
                    onCreatorClick = { onCreatorClick(post.author.userId) }
                )
            }

            // ── Plan Schedule Breakdown ────────────────────────────────
            uiState.planTemplate?.let { template ->
                item {
                    PlanSchedulePreviewSection(template = template)
                }
            }

            // ── Comments Section Header ────────────────────────────────
            item {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                ) {
                    Text(
                        text = "Discussion & Experiences (${uiState.comments.size})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // ── Nested Comments List ───────────────────────────────────
            if (uiState.comments.isEmpty()) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(24.dp).fillMaxWidth()
                        ) {
                            Text(
                                text = "No comments yet.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Be the first to share your thoughts or ask a question!",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            )
                        }
                    }
                }
            } else {
                items(uiState.comments, key = { it.commentId }) { comment ->
                    NestedCommentItem(
                        comment = comment,
                        depth = 0,
                        onReplyClick = { viewModel.onSetReplyTo(it) }
                    )
                }
            }

            item { Spacer(modifier = Modifier.height(16.dp)) }
        }
    }

    if (showJoinConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showJoinConfirmDialog = false },
            icon = { Icon(Icons.Filled.Download, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
            title = { Text("Join & Fork Plan", fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    text = "This will clone \"${uiState.post?.title}\" into your private local planner starting today. All day-to-day checkboxes and notes remain 100% offline on your device."
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showJoinConfirmDialog = false
                        viewModel.joinPlan(startDate = LocalDate.now())
                    }
                ) {
                    Text("Join Plan Now")
                }
            },
            dismissButton = {
                TextButton(onClick = { showJoinConfirmDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun PostDetailHeaderCard(
    post: CommunityPost,
    isAlreadyJoined: Boolean = false,
    onUpvote: () -> Unit,
    onDownvote: () -> Unit,
    onJoin: () -> Unit,
    onOpenLocalPlan: () -> Unit = {},
    onCreatorClick: (() -> Unit)? = null
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Author info row (clickable if onCreatorClick provided)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = if (onCreatorClick != null) {
                    Modifier.clickable(onClick = onCreatorClick)
                } else Modifier
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = post.author.displayName.take(1).uppercase(),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = post.author.displayName,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold
                        )
                        if (post.author.isCreator) {
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                imageVector = Icons.Filled.Verified,
                                contentDescription = "Creator",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                    Text(
                        text = "@${post.author.username} • ${formatDate(post.createdAt)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = post.title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = post.description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // Tags
            if (post.tags.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    post.tags.forEach { tag ->
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        ) {
                            Text(
                                text = "#$tag",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
            Spacer(modifier = Modifier.height(8.dp))

            // Action row: Upvote, Downvote, Join
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    IconButton(onClick = onUpvote, modifier = Modifier.size(32.dp)) {
                        Icon(
                            imageVector = Icons.Filled.KeyboardArrowUp,
                            contentDescription = "Upvote",
                            tint = if (post.userVote == VoteType.UP) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text(
                        text = post.score.toString(),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = if (post.userVote != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                    )
                    IconButton(onClick = onDownvote, modifier = Modifier.size(32.dp)) {
                        Icon(
                            imageVector = Icons.Filled.KeyboardArrowDown,
                            contentDescription = "Downvote",
                            tint = if (post.userVote == VoteType.DOWN) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                if (isAlreadyJoined) {
                    OutlinedButton(
                        onClick = onOpenLocalPlan,
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Open in My Plans", fontWeight = FontWeight.Bold)
                    }
                } else {
                    Button(
                        onClick = onJoin,
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Join Plan (${post.durationDays}d)", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun PlanSchedulePreviewSection(template: PlanTemplateDto) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Outlined.CalendarMonth,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Plan Schedule (${template.targetDurationDays} Days)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                ) {
                    Text(
                        text = "${template.tasks.size} Routine Tasks",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            template.tasks.forEach { task ->
                TaskPreviewRow(task = task)
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun TaskPreviewRow(task: TaskTemplateDto) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Outlined.CheckCircle,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = task.taskDescription,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            if (task.subtasks.isNotEmpty()) {
                Spacer(modifier = Modifier.height(6.dp))
                Column(modifier = Modifier.padding(start = 26.dp)) {
                    task.subtasks.forEach { subtask ->
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 1.dp)) {
                            Text("• ", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                            Text(
                                text = subtask,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun NestedCommentItem(
    comment: PostComment,
    depth: Int = 0,
    onReplyClick: (PostComment) -> Unit
) {
    val maxDepth = 3
    val clampedDepth = depth.coerceAtMost(maxDepth)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = (clampedDepth * 16).dp)
    ) {
        if (depth > 0) {
            // Reddit-style vertical line for nested threads
            Box(
                modifier = Modifier
                    .width(2.dp)
                    .fillMaxHeight()
                    .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f))
            )
            Spacer(modifier = Modifier.width(10.dp))
        }

        Column(modifier = Modifier.weight(1f)) {
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    // Header
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.secondaryContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = comment.author.displayName.take(1).uppercase(),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = comment.author.displayName,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold
                            )
                            if (comment.author.isCreator) {
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(
                                    Icons.Filled.Verified,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }

                        Text(
                            text = formatDate(comment.createdAt),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = comment.content,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "▲ ${comment.upvoteCount}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        TextButton(
                            onClick = { onReplyClick(comment) },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Icon(Icons.AutoMirrored.Outlined.Reply, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Reply", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }

            // Recursive replies rendering
            if (comment.replies.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                comment.replies.forEach { reply ->
                    NestedCommentItem(
                        comment = reply,
                        depth = depth + 1,
                        onReplyClick = onReplyClick
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }
}

private fun formatDate(epochMillis: Long): String {
    return try {
        val date = Instant.ofEpochMilli(epochMillis).atZone(ZoneId.systemDefault()).toLocalDate()
        date.format(DateTimeFormatter.ofPattern("MMM d"))
    } catch (e: Exception) {
        "recently"
    }
}
