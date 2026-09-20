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
import com.example.plannerapp.ui.components.InteractiveCommentRail
import com.example.plannerapp.ui.components.TaskThreadBranch
import com.example.plannerapp.smartlink.SmartLinkParser
import com.example.plannerapp.smartlink.ui.SmartLinkCard
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
    val isFollowingCreator by viewModel.isFollowingCreator.collectAsState()
    val haptic = LocalHapticFeedback.current
    var commentInputText by remember { mutableStateOf("") }
    var showOptionsMenu by remember { mutableStateOf(false) }
    var showReportDialog by remember { mutableStateOf(false) }
    var reportTarget by remember { mutableStateOf<Pair<String, String>?>(null) } // Pair(targetId, targetType)
    var selectedReportReason by remember { mutableStateOf("Inappropriate or harmful content") }

    val currentUserId = uiState.currentUser?.cloudUserId ?: uiState.currentUser?.userId?.toString() ?: "local_user"

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

    if (showReportDialog && reportTarget != null) {
        AlertDialog(
            onDismissRequest = {
                showReportDialog = false
                reportTarget = null
            },
            title = {
                Text(
                    text = "Report Content",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Why are you reporting this ${reportTarget!!.second.lowercase()}?",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    listOf(
                        "Inappropriate or harmful content",
                        "Spam or misleading information",
                        "Harassment or bullying",
                        "Intellectual property violation"
                    ).forEach { reason ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { selectedReportReason = reason }
                                .padding(vertical = 4.dp)
                        ) {
                            RadioButton(
                                selected = selectedReportReason == reason,
                                onClick = { selectedReportReason = reason }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = reason, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val target = reportTarget
                        if (target != null) {
                            viewModel.reportContent(target.first, target.second, selectedReportReason)
                        }
                        showReportDialog = false
                        reportTarget = null
                    }
                ) {
                    Text("Submit Report")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showReportDialog = false
                    reportTarget = null
                }) {
                    Text("Cancel")
                }
            }
        )
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
                                    viewModel.joinPlan(startDate = LocalDate.now())
                                }
                            },
                            label = {
                                Text(
                                    text = if (isJoined) "In My Plans" else "Use This Plan",
                                    fontWeight = FontWeight.Bold
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = if (isJoined) Icons.Filled.Check else Icons.Filled.Download,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            },
                            colors = AssistChipDefaults.assistChipColors(
                                containerColor = if (isJoined) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.primaryContainer,
                                labelColor = if (isJoined) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onPrimaryContainer
                            ),
                            modifier = Modifier.padding(end = 4.dp)
                        )

                        Box {
                            IconButton(onClick = { showOptionsMenu = true }) {
                                Icon(Icons.Filled.MoreVert, contentDescription = "Options")
                            }
                            DropdownMenu(
                                expanded = showOptionsMenu,
                                onDismissRequest = { showOptionsMenu = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Report Plan") },
                                    leadingIcon = { Icon(Icons.Outlined.Flag, contentDescription = null) },
                                    onClick = {
                                        showOptionsMenu = false
                                        reportTarget = Pair(post.postId, "PLAN")
                                        showReportDialog = true
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Block @${post.author.username}") },
                                    leadingIcon = { Icon(Icons.Outlined.Block, contentDescription = null) },
                                    onClick = {
                                        showOptionsMenu = false
                                        viewModel.blockAuthor()
                                        onBack()
                                    }
                                )
                            }
                        }
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
                    template = uiState.planTemplate,
                    isAlreadyJoined = uiState.localPlanAlreadyJoinedId != null,
                    isJoining = uiState.isJoining,
                    isFollowing = isFollowingCreator,
                    onToggleFollow = { viewModel.toggleFollowCreator() },
                    onUpvote = { viewModel.onVote(VoteType.UP) },
                    onDownvote = { viewModel.onVote(VoteType.DOWN) },
                    onJoin = { viewModel.joinPlan(startDate = LocalDate.now()) },
                    onSaveToggle = { viewModel.onToggleSave() },
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
                        currentUserId = currentUserId,
                        onReplyClick = { viewModel.onSetReplyTo(it) },
                        onToggleLike = { viewModel.onToggleCommentLike(it) },
                        onDeleteComment = { viewModel.deleteComment(it) },
                        onReportComment = { commentId ->
                            reportTarget = Pair(commentId, "COMMENT")
                            showReportDialog = true
                        }
                    )
                }
            }

            item { Spacer(modifier = Modifier.height(16.dp)) }
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
    template: PlanTemplateDto? = null,
    isAlreadyJoined: Boolean = false,
    isJoining: Boolean = false,
    isFollowing: Boolean = false,
    onToggleFollow: () -> Unit = {},
    onUpvote: () -> Unit,
    onDownvote: () -> Unit,
    onJoin: () -> Unit,
    onSaveToggle: () -> Unit = {},
    onOpenLocalPlan: () -> Unit = {},
    onCreatorClick: (() -> Unit)? = null
) {
    val context = androidx.compose.ui.platform.LocalContext.current

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Author info row + Follow button
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = if (onCreatorClick != null) {
                        Modifier.weight(1f).clickable(onClick = onCreatorClick)
                    } else Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
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
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
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

                Button(
                    onClick = onToggleFollow,
                    modifier = Modifier.height(30.dp),
                    shape = RoundedCornerShape(15.dp),
                    colors = if (isFollowing) {
                        ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        )
                    },
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp)
                ) {
                    Text(
                        text = if (isFollowing) "Following" else "Follow",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold
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

            // Smart Link in description if present
            val detectedPostUrl = remember(post.description) {
                SmartLinkParser.findFirstUrl(post.description)
            }
            if (detectedPostUrl != null) {
                Spacer(modifier = Modifier.height(10.dp))
                SmartLinkCard(
                    url = detectedPostUrl,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Overview Metrics Grid
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
                    .padding(vertical = 10.dp, horizontal = 12.dp),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "${post.durationDays}d",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Duration",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "${template?.tasks?.size ?: 0}",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Tasks",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = formatCompactCount(post.joinCount),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Active Users",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "${post.upvoteCount}",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Upvotes",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

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

            // Action row: Upvote, Downvote, Save, Share, Use This Plan
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // Vote stepper
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                            .padding(horizontal = 4.dp, vertical = 2.dp)
                    ) {
                        IconButton(onClick = onUpvote, modifier = Modifier.size(30.dp)) {
                            Icon(
                                imageVector = Icons.Filled.KeyboardArrowUp,
                                contentDescription = "Upvote",
                                tint = if (post.userVote == VoteType.UP) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Text(
                            text = post.score.toString(),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (post.userVote != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                        )
                        IconButton(onClick = onDownvote, modifier = Modifier.size(30.dp)) {
                            Icon(
                                imageVector = Icons.Filled.KeyboardArrowDown,
                                contentDescription = "Downvote",
                                tint = if (post.userVote == VoteType.DOWN) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // Save Bookmark
                    IconButton(
                        onClick = onSaveToggle,
                        modifier = Modifier.size(34.dp)
                    ) {
                        Icon(
                            imageVector = if (post.isSaved) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
                            contentDescription = "Save Plan",
                            tint = if (post.isSaved) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Share
                    IconButton(
                        onClick = {
                            val sendIntent = android.content.Intent().apply {
                                action = android.content.Intent.ACTION_SEND
                                putExtra(android.content.Intent.EXTRA_TEXT, "Check out this plan: ${post.title}\n${post.description}")
                                type = "text/plain"
                            }
                            context.startActivity(android.content.Intent.createChooser(sendIntent, "Share Plan"))
                        },
                        modifier = Modifier.size(34.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Share,
                            contentDescription = "Share Plan",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
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
                        Text("In My Plans", fontWeight = FontWeight.Bold)
                    }
                } else {
                    Button(
                        onClick = onJoin,
                        shape = RoundedCornerShape(10.dp),
                        enabled = !isJoining
                    ) {
                        if (isJoining) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Cloning...", fontWeight = FontWeight.Bold)
                        } else {
                            Icon(
                                imageVector = Icons.Filled.Download,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Use This Plan",
                                fontWeight = FontWeight.Bold
                            )
                        }
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
    currentUserId: String = "",
    onReplyClick: (PostComment) -> Unit,
    onToggleLike: (String) -> Unit,
    onDeleteComment: (String) -> Unit = {},
    onReportComment: (String) -> Unit = {}
) {
    val haptic = LocalHapticFeedback.current
    var isExpanded by remember { mutableStateOf(false) }
    var isSubtreeCollapsed by remember { mutableStateOf(false) }

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

                val detectedCommentUrl = remember(comment.content) {
                    SmartLinkParser.findFirstUrl(comment.content)
                }
                val cleanCommentText = remember(comment.content) {
                    val clean = SmartLinkParser.extractCleanText(comment.content)
                    if (clean.isNotBlank()) clean else (detectedCommentUrl?.let { SmartLinkParser.extractDomain(it) } ?: comment.content)
                }

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
                            append(cleanCommentText)
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                } else {
                    Text(
                        text = cleanCommentText,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                if (detectedCommentUrl != null) {
                    Spacer(modifier = Modifier.height(6.dp))
                    SmartLinkCard(
                        url = detectedCommentUrl,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Actions: Reply Button, Delete, Report
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
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

                    if (comment.author.userId == currentUserId) {
                        Text(
                            text = "Delete",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .clickable {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    onDeleteComment(comment.commentId)
                                }
                                .padding(vertical = 2.dp, horizontal = 4.dp)
                        )
                    } else {
                        Text(
                            text = "Report",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .clickable {
                                    onReportComment(comment.commentId)
                                }
                                .padding(vertical = 2.dp, horizontal = 4.dp)
                        )
                    }
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

        // ── Nested Replies (Interactive thread line + Instagram-style collapse) ──
        if (comment.replies.isNotEmpty()) {
            val totalReplies = comment.replies.size
            val showCollapseToggle = totalReplies > 1
            val shouldNestRail = depth < 2

            if (isSubtreeCollapsed) {
                // Collapsed teaser (Tap rail or teaser to re-expand)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .padding(start = if (depth == 0) 36.dp else 28.dp, top = 4.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .clickable {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            isSubtreeCollapsed = false
                        }
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.KeyboardArrowDown,
                        contentDescription = "Expand thread",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Show $totalReplies ${if (totalReplies == 1) "reply" else "replies"}",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            } else if (shouldNestRail) {
                // Nested indentation with interactive 32dp continuous guide rail
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = if (depth == 0) 0.dp else 0.dp)
                        .height(IntrinsicSize.Min)
                ) {
                    InteractiveCommentRail(
                        isCollapsed = isSubtreeCollapsed,
                        onToggleCollapse = { isSubtreeCollapsed = !isSubtreeCollapsed },
                        touchWidth = if (depth == 0) 34.dp else 26.dp
                    )

                    Spacer(modifier = Modifier.width(8.dp))

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
                                currentUserId = currentUserId,
                                onReplyClick = onReplyClick,
                                onToggleLike = onToggleLike,
                                onDeleteComment = onDeleteComment,
                                onReportComment = onReportComment
                            )
                        }

                        // Instagram-Style "View X more replies" / "Hide replies"
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
            } else {
                // Indent clamped at depth >= 2 (max 3 levels of indentation: 0, 1, 2)
                // Subsequent replies remain aligned on the same rail without squishing text
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 12.dp)
                ) {
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
