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
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Locale
import com.example.plannerapp.credits.CreditHubSheet
import com.example.plannerapp.credits.CreditViewModel
import com.example.plannerapp.ui.components.TaskThreadBranch
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
    creditViewModel: CreditViewModel? = null,
    onCreatorClick: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val haptic = LocalHapticFeedback.current
    var commentInputText by remember { mutableStateOf("") }
    var showJoinConfirmDialog by remember { mutableStateOf(false) }

    val creditBalance = if (creditViewModel != null) {
        creditViewModel.balanceFlow.collectAsState().value
    } else 0
    var showCreditSheet by remember { mutableStateOf(false) }
    var purchaseErrorMessage by remember { mutableStateOf<String?>(null) }

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
                        val isPaid = post.isPaid && post.creditCost > 0
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
                                    text = if (isJoined) "Joined" else if (isPaid) "Unlock (${post.creditCost} C)" else "Join (${post.joinCount})",
                                    fontWeight = FontWeight.Bold
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = if (isJoined) Icons.Filled.Check else if (isPaid) Icons.Outlined.Paid else Icons.Filled.Add,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            },
                            colors = AssistChipDefaults.assistChipColors(
                                containerColor = if (isJoined) MaterialTheme.colorScheme.secondaryContainer else if (isPaid) Color(0xFF1E88E5).copy(alpha = 0.15f) else MaterialTheme.colorScheme.primaryContainer,
                                labelColor = if (isJoined) MaterialTheme.colorScheme.onSecondaryContainer else if (isPaid) Color(0xFF1E88E5) else MaterialTheme.colorScheme.onPrimaryContainer
                            ),
                            modifier = Modifier.padding(end = 8.dp)
                        )
                    }
                }
            )
        },
        bottomBar = {
            // Instagram / Reddit styled Comment input bar
            Surface(
                tonalElevation = 2.dp,
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                        .navigationBarsPadding()
                        .imePadding()
                ) {
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
                        // Current user's avatar
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = uiState.currentUser?.displayName?.take(1)?.uppercase() ?: "U",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }

                        Spacer(modifier = Modifier.width(10.dp))

                        OutlinedTextField(
                            value = commentInputText,
                            onValueChange = { commentInputText = it },
                            placeholder = {
                                Text(
                                    text = if (uiState.replyToComment != null)
                                        "Reply to @${uiState.replyToComment?.author?.username}..."
                                    else
                                        "Add a comment...",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f)
                                )
                            },
                            modifier = Modifier.weight(1f),
                            maxLines = 3,
                            shape = RoundedCornerShape(24.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f),
                                focusedBorderColor = MaterialTheme.colorScheme.primary
                            )
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
                            colors = IconButtonDefaults.filledIconButtonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Send,
                                contentDescription = "Send Comment",
                                tint = if (commentInputText.isNotBlank()) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
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
                    ThreadedCommentItem(
                        comment = comment,
                        depth = 0,
                        onReplyClick = { viewModel.onSetReplyTo(it) },
                        onToggleLike = { viewModel.onToggleCommentLike(it) }
                    )
                }
            }

            item { Spacer(modifier = Modifier.height(16.dp)) }
        }
    }

    if (showJoinConfirmDialog) {
        val post = uiState.post
        val isPaid = post?.isPaid == true && (post.creditCost > 0)
        val cost = post?.creditCost ?: 0
        val hasEnoughCredits = creditBalance >= cost

        if (isPaid) {
            AlertDialog(
                onDismissRequest = { showJoinConfirmDialog = false },
                icon = { Icon(Icons.Outlined.MonetizationOn, contentDescription = null, tint = Color(0xFF1E88E5)) },
                title = { Text("Unlock Creator Plan", fontWeight = FontWeight.Bold) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "\"${post.title}\" was published by a verified creator for $cost credits."
                        )
                        Text(
                            text = "70% of proceeds go directly to support the creator. Unlocking clones this plan into your private offline planner.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Your Credit Balance:", style = MaterialTheme.typography.bodyMedium)
                            Text(
                                text = "$creditBalance Credits",
                                fontWeight = FontWeight.Bold,
                                color = if (hasEnoughCredits) Color(0xFF2E7D32) else MaterialTheme.colorScheme.error
                            )
                        }
                        if (!hasEnoughCredits) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "You need ${cost - creditBalance} more credits to unlock this plan.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                },
                confirmButton = {
                    if (hasEnoughCredits) {
                        Button(
                            onClick = {
                                showJoinConfirmDialog = false
                                if (creditViewModel != null) {
                                    val creatorIdNum = post.author.userId.toLongOrNull() ?: 0L
                                    creditViewModel.unlockCreatorPlan(
                                        planTitle = post.title,
                                        cost = cost,
                                        creatorId = creatorIdNum
                                    ) { success, msg ->
                                        if (success) {
                                            viewModel.joinPlan(startDate = LocalDate.now())
                                        } else {
                                            purchaseErrorMessage = msg
                                        }
                                    }
                                } else {
                                    viewModel.joinPlan(startDate = LocalDate.now())
                                }
                            }
                        ) {
                            Text("Unlock for $cost Credits")
                        }
                    } else {
                        Button(
                            onClick = {
                                showJoinConfirmDialog = false
                                showCreditSheet = true
                            }
                        ) {
                            Text("Get Credits")
                        }
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showJoinConfirmDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        } else {
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

    if (showCreditSheet && creditViewModel != null) {
        CreditHubSheet(
            viewModel = creditViewModel,
            onDismissRequest = { showCreditSheet = false }
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

            if (post.isPaid && post.creditCost > 0) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFF1E88E5).copy(alpha = 0.12f),
                    modifier = Modifier.padding(bottom = 8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Paid,
                            contentDescription = null,
                            tint = Color(0xFF1E88E5),
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Creator Plan • ${post.creditCost} Credits",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1E88E5)
                        )
                    }
                }
            }

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
                    val isPaid = post.isPaid && post.creditCost > 0
                    Button(
                        onClick = onJoin,
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(
                            imageVector = if (isPaid) Icons.Outlined.Paid else Icons.Filled.Add,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isPaid) "Unlock (${post.creditCost} Credits)" else "Join Plan (${post.durationDays}d)",
                            fontWeight = FontWeight.Bold
                        )
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
                Spacer(modifier = Modifier.height(4.dp))
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 2.dp)
                ) {
                    task.subtasks.forEachIndexed { index, subtask ->
                        val isLast = index == task.subtasks.lastIndex
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(IntrinsicSize.Min)
                                .padding(vertical = 1.dp)
                        ) {
                            TaskThreadBranch(
                                isLast = isLast,
                                isCompleted = false,
                                width = 18.dp,
                                trunkX = 7.dp,
                                cornerRadius = 6.dp,
                                modifier = Modifier.fillMaxHeight()
                            )
                            Spacer(modifier = Modifier.width(4.dp))
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
private fun ThreadedCommentItem(
    comment: PostComment,
    depth: Int = 0,
    parentAuthorUsername: String? = null,
    onReplyClick: (PostComment) -> Unit,
    onToggleLike: (String) -> Unit
) {
    val haptic = LocalHapticFeedback.current
    var isExpanded by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        // ── Main Comment Row ──────────────────────────────────────────
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top
        ) {
            // Avatar (34.dp for top-level, 26.dp for nested replies)
            val avatarSize = if (depth == 0) 34.dp else 26.dp
            Box(
                modifier = Modifier
                    .size(avatarSize)
                    .clip(CircleShape)
                    .background(
                        if (depth == 0) MaterialTheme.colorScheme.primaryContainer
                        else MaterialTheme.colorScheme.secondaryContainer
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = comment.author.displayName.take(1).uppercase(),
                    style = if (depth == 0) MaterialTheme.typography.labelMedium else MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = if (depth == 0) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSecondaryContainer
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            // Text & Actions Column
            Column(modifier = Modifier.weight(1f)) {
                // Author row: Username + Verified Creator Badge + Relative Time
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = comment.author.username.ifBlank { comment.author.displayName },
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    if (comment.author.isCreator) {
                        Icon(
                            imageVector = Icons.Filled.Verified,
                            contentDescription = "Creator",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(14.dp)
                        )
                    }

                    Text(
                        text = formatRelativeTime(comment.createdAt),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(2.dp))

                // Comment Content (with highlighted @parent mention if replying)
                if (depth > 0 && parentAuthorUsername != null) {
                    Text(
                        text = buildAnnotatedString {
                            withStyle(
                                SpanStyle(
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.SemiBold
                                )
                            ) {
                                append("@$parentAuthorUsername ")
                            }
                            append(comment.content)
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                } else {
                    Text(
                        text = comment.content,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Actions: Reply Button
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(top = 2.dp)
                ) {
                    Text(
                        text = "Reply",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .clickable {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onReplyClick(comment)
                            }
                            .padding(vertical = 2.dp, horizontal = 4.dp)
                    )
                }
            }

            // Like / Heart Icon & Counter (Right side, matching Image 1)
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(start = 6.dp)
            ) {
                IconButton(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onToggleLike(comment.commentId)
                    },
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = if (comment.isLiked) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                        contentDescription = "Like",
                        tint = if (comment.isLiked) Color(0xFFE53935) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        modifier = Modifier.size(16.dp)
                    )
                }

                if (comment.upvoteCount > 0) {
                    Text(
                        text = formatCompactCount(comment.upvoteCount),
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // ── Nested Replies (Reddit-style tree line + Instagram-style collapse) ──
        if (comment.replies.isNotEmpty()) {
            val totalReplies = comment.replies.size
            val showCollapseToggle = totalReplies > 1

            // Replies container with continuous vertical line on left (Reddit style, matching Image 2)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = if (depth == 0) 17.dp else 13.dp)
                    .height(IntrinsicSize.Min)
            ) {
                // Reddit-Style vertical thread guide line
                Box(
                    modifier = Modifier
                        .width(1.5.dp)
                        .fillMaxHeight()
                        .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f))
                )

                Spacer(modifier = Modifier.width(14.dp))

                // Replies column
                Column(modifier = Modifier.weight(1f)) {
                    val displayedReplies = if (!showCollapseToggle || isExpanded) {
                        comment.replies
                    } else {
                        listOf(comment.replies.first())
                    }

                    displayedReplies.forEach { reply ->
                        Spacer(modifier = Modifier.height(8.dp))
                        ThreadedCommentItem(
                            comment = reply,
                            depth = depth + 1,
                            parentAuthorUsername = comment.author.username.ifBlank { comment.author.displayName },
                            onReplyClick = onReplyClick,
                            onToggleLike = onToggleLike
                        )
                    }

                    // Instagram-Style "View X more replies" / "Hide replies" (circled in Image 1)
                    if (showCollapseToggle) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .clickable {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    isExpanded = !isExpanded
                                }
                                .padding(vertical = 6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .width(28.dp)
                                    .height(1.dp)
                                    .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (!isExpanded) "View ${totalReplies - 1} more replies" else "Hide replies",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun formatRelativeTime(epochMillis: Long): String {
    val now = System.currentTimeMillis()
    val diffSec = ((now - epochMillis) / 1000).coerceAtLeast(0)
    return when {
        diffSec < 60 -> "just now"
        diffSec < 3600 -> "${diffSec / 60}m"
        diffSec < 86400 -> "${diffSec / 3600}h"
        diffSec < 604800 -> "${diffSec / 86400}d"
        diffSec < 31536000 -> "${diffSec / 604800}w"
        else -> "${diffSec / 31536000}y"
    }
}

private fun formatCompactCount(count: Int): String {
    return when {
        count >= 1000 -> String.format(Locale.US, "%.1fK", count / 1000.0)
        else -> count.toString()
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
