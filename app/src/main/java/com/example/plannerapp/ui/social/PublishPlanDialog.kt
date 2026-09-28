package com.example.plannerapp.ui.social

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.plannerapp.auth.SupabaseConfig
import com.example.plannerapp.data.JoinedCommunityEntity
import com.example.plannerapp.data.PlanEntity
import com.example.plannerapp.data.PlannerRepository
import com.example.plannerapp.data.TaskTemplateEntity
import com.example.plannerapp.data.UserDao
import com.example.plannerapp.data.UserEntity
import com.example.plannerapp.data.social.CloudUser
import com.example.plannerapp.data.social.SocialRepository
import com.example.plannerapp.data.template.PlanExporter
import com.google.gson.Gson
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.temporal.ChronoUnit

/**
 * Guided, multi-step ModalBottomSheet for publishing a local plan to the public Explore community.
 *
 * Steps:
 *  1. Quality & Curriculum Check (Verifies cloud account, connectivity, and inspects tasks)
 *  2. Community Metadata & Tags (Title, instructions, category, tags)
 *  3. Audience, Monetization & Live Feed Preview (Preview card in explore feed, publish confirmation)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PublishPlanDialog(
    plan: PlanEntity,
    plannerRepository: PlannerRepository,
    socialRepository: SocialRepository,
    userDao: UserDao,
    onDismiss: () -> Unit,
    onPublished: (String) -> Unit,
    onNavigateToSignIn: (() -> Unit)? = null
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val creditRepository = remember(context) {
        com.example.plannerapp.credits.CreditRepository(
            com.example.plannerapp.data.PlannerDatabase.getDatabase(context).creditDao()
        )
    }

    var currentStep by remember { mutableIntStateOf(1) }
    var title by remember { mutableStateOf(plan.heading) }
    var description by remember { mutableStateOf(plan.description) }
    var selectedCategory by remember { mutableStateOf("Productivity") }
    var customTagInput by remember { mutableStateOf("") }
    var selectedTags by remember { mutableStateOf(listOf("routine", "habits")) }
    var visibility by remember { mutableStateOf("PUBLIC") }
    var isPaidPlan by remember { mutableStateOf(false) }
    var selectedCreditPrice by remember { mutableIntStateOf(100) }

    var isPublishing by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var publishedPostId by remember { mutableStateOf<String?>(null) }

    val activeUserFlow by userDao.getActiveUser().collectAsState(initial = null)
    val activeUser = activeUserFlow
    val isCreator = activeUser?.isCreator == true

    // Retrieve task templates to validate plan has at least 1 task
    val templatesState = produceState<List<TaskTemplateEntity>?>(initialValue = null, plan.planId) {
        value = plannerRepository.getTemplatesForPlan(plan.planId)
    }
    val templates = templatesState.value ?: emptyList()
    val hasTasks = templates.isNotEmpty()

    val cloudUid = remember(activeUser) {
        SupabaseConfig.auth.currentUserOrNull()?.id ?: activeUser?.cloudUserId
    }
    val isAuthenticated = !cloudUid.isNullOrBlank()

    val categories = remember {
        listOf(
            "Productivity",
            "Fitness & Health",
            "Learning & Study",
            "Mindfulness",
            "Habits & Routines",
            "Personal Development"
        )
    }

    val suggestedTags = remember {
        listOf("morning", "fitness", "deepwork", "study", "mindset", "focus", "discipline")
    }

    val planDurationDays = remember(plan.startDate, plan.endDate) {
        try {
            val start = LocalDate.parse(plan.startDate)
            val end = LocalDate.parse(plan.endDate)
            (ChronoUnit.DAYS.between(start, end) + 1).coerceAtLeast(1).toInt()
        } catch (_: Exception) {
            7
        }
    }

    ModalBottomSheet(
        onDismissRequest = { if (!isPublishing) onDismiss() },
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(bottom = 16.dp)
        ) {
            // ── Top Header with Step Indicator ──────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Outlined.Public,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Publish to Community",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        if (publishedPostId == null) {
                            Text(
                                text = when (currentStep) {
                                    1 -> "Step 1 of 3: Plan Review"
                                    2 -> "Step 2 of 3: Details & Category"
                                    else -> "Step 3 of 3: Feed Preview"
                                },
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                IconButton(onClick = onDismiss, enabled = !isPublishing) {
                    Icon(Icons.Filled.Close, contentDescription = "Close")
                }
            }

            if (publishedPostId == null) {
                LinearProgressIndicator(
                    progress = { currentStep / 3f },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 4.dp),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
            }

            HorizontalDivider(
                thickness = 0.5.dp,
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
            )

            // ── Main Content Area ───────────────────────────────────────────────
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = false)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 16.dp)
            ) {
                if (publishedPostId != null) {
                    // Success View
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.size(72.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Outlined.CheckCircle,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(40.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Plan Published Successfully",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Your routine is now live in the Community Explore feed. Other members can discover, fork, and track their progress alongside you.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 12.dp)
                        )
                        Spacer(modifier = Modifier.height(28.dp))

                        Button(
                            onClick = {
                                val postId = publishedPostId
                                if (postId != null) {
                                    onPublished(postId)
                                }
                            },
                            modifier = Modifier.fillMaxWidth().height(48.dp),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Outlined.Forum, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Open Community Discussion", fontWeight = FontWeight.SemiBold)
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        OutlinedButton(
                            onClick = onDismiss,
                            modifier = Modifier.fillMaxWidth().height(48.dp),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Done", fontWeight = FontWeight.SemiBold)
                        }
                    }
                } else {
                    AnimatedContent(
                        targetState = currentStep,
                        transitionSpec = { fadeIn() togetherWith fadeOut() },
                        label = "publish_step_content"
                    ) { step ->
                        when (step) {
                            1 -> {
                                // ── Step 1: Pre-flight Verification & Curriculum Review ──
                                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                                    // Quality Checklist Card
                                    Card(
                                        shape = RoundedCornerShape(14.dp),
                                        colors = CardDefaults.cardColors(
                                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                                        ),
                                        border = androidx.compose.foundation.BorderStroke(
                                            1.dp,
                                            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
                                        ),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                            Text(
                                                text = "Pre-flight Verification",
                                                style = MaterialTheme.typography.labelLarge,
                                                fontWeight = FontWeight.Bold
                                            )

                                            // Check 1: Account
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(
                                                    imageVector = if (isAuthenticated) Icons.Outlined.CheckCircle else Icons.Outlined.ErrorOutline,
                                                    contentDescription = null,
                                                    tint = if (isAuthenticated) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                                Spacer(modifier = Modifier.width(10.dp))
                                                Text(
                                                    text = if (isAuthenticated) "Signed in as @${activeUser?.username ?: activeUser?.displayName}" else "Sign in required to publish online",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = if (isAuthenticated) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.error
                                                )
                                            }

                                            // Check 2: Task Count
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(
                                                    imageVector = if (hasTasks) Icons.Outlined.CheckCircle else Icons.Outlined.ErrorOutline,
                                                    contentDescription = null,
                                                    tint = if (hasTasks) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                                Spacer(modifier = Modifier.width(10.dp))
                                                Text(
                                                    text = if (hasTasks) "${templates.size} recurring task${if (templates.size > 1) "s" else ""} ready to share" else "Plan has 0 tasks (add at least 1 task)",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = if (hasTasks) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.error
                                                )
                                            }

                                            // Check 3: Public sharing policy
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(
                                                    imageVector = Icons.Outlined.Info,
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                                Spacer(modifier = Modifier.width(10.dp))
                                                Text(
                                                    text = "Your private check-ins and notes will not be shared",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }
                                    }

                                    if (!isAuthenticated && onNavigateToSignIn != null) {
                                        Button(
                                            onClick = onNavigateToSignIn,
                                            modifier = Modifier.fillMaxWidth(),
                                            shape = RoundedCornerShape(10.dp)
                                        ) {
                                            Text("Sign In to Continue")
                                        }
                                    }

                                    // Curriculum Summary
                                    Text(
                                        text = "Tasks in this Plan",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold
                                    )

                                    if (templatesState.value == null) {
                                        Box(modifier = Modifier.fillMaxWidth().height(80.dp), contentAlignment = Alignment.Center) {
                                            CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                                        }
                                    } else if (templates.isEmpty()) {
                                        Card(
                                            shape = RoundedCornerShape(12.dp),
                                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Column(modifier = Modifier.padding(14.dp)) {
                                                Text(
                                                    text = "Cannot publish an empty routine",
                                                    style = MaterialTheme.typography.labelLarge,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.error
                                                )
                                                Text(
                                                    text = "Please add at least one task template to this plan before sharing it with the community.",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onErrorContainer
                                                )
                                            }
                                        }
                                    } else {
                                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                            templates.forEachIndexed { index, template ->
                                                Surface(
                                                    shape = RoundedCornerShape(10.dp),
                                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                                                    border = androidx.compose.foundation.BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)),
                                                    modifier = Modifier.fillMaxWidth()
                                                ) {
                                                    Row(
                                                        modifier = Modifier.padding(12.dp),
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        Box(
                                                            modifier = Modifier
                                                                .size(24.dp)
                                                                .clip(CircleShape)
                                                                .background(MaterialTheme.colorScheme.primaryContainer),
                                                            contentAlignment = Alignment.Center
                                                        ) {
                                                            Text(
                                                                text = "${index + 1}",
                                                                style = MaterialTheme.typography.labelSmall,
                                                                fontWeight = FontWeight.Bold,
                                                                color = MaterialTheme.colorScheme.onPrimaryContainer
                                                            )
                                                        }
                                                        Spacer(modifier = Modifier.width(12.dp))
                                                        Column(modifier = Modifier.weight(1f)) {
                                                            Text(
                                                                text = template.taskDescription,
                                                                style = MaterialTheme.typography.bodyMedium,
                                                                fontWeight = FontWeight.Medium
                                                            )
                                                            Text(
                                                                text = formatDayChips(template.selectedDays),
                                                                style = MaterialTheme.typography.labelSmall,
                                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            2 -> {
                                // ── Step 2: Metadata, Category & Tags ──
                                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                                    OutlinedTextField(
                                        value = title,
                                        onValueChange = { title = it },
                                        label = { Text("Community Plan Title") },
                                        placeholder = { Text("e.g. 30-Day Morning Focus Routine") },
                                        singleLine = true,
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(12.dp)
                                    )

                                    OutlinedTextField(
                                        value = description,
                                        onValueChange = { description = it },
                                        label = { Text("Description & Guidelines") },
                                        placeholder = { Text("Explain how to follow this plan and the expected outcomes...") },
                                        minLines = 3,
                                        maxLines = 5,
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(12.dp)
                                    )

                                    // Category Selection
                                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Text(
                                            text = "Category",
                                            style = MaterialTheme.typography.labelLarge,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .horizontalScroll(rememberScrollState()),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            categories.forEach { category ->
                                                FilterChip(
                                                    selected = selectedCategory == category,
                                                    onClick = { selectedCategory = category },
                                                    label = { Text(category, style = MaterialTheme.typography.labelMedium) },
                                                    shape = RoundedCornerShape(8.dp)
                                                )
                                            }
                                        }
                                    }

                                    // Tags Section
                                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Text(
                                            text = "Tags",
                                            style = MaterialTheme.typography.labelLarge,
                                            fontWeight = FontWeight.SemiBold
                                        )

                                        // Active Tags
                                        if (selectedTags.isNotEmpty()) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .horizontalScroll(rememberScrollState()),
                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                selectedTags.forEach { tag ->
                                                    InputChip(
                                                        selected = true,
                                                        onClick = {
                                                            selectedTags = selectedTags - tag
                                                        },
                                                        label = { Text("#$tag") },
                                                        trailingIcon = {
                                                            Icon(
                                                                Icons.Filled.Close,
                                                                contentDescription = "Remove tag",
                                                                modifier = Modifier.size(14.dp)
                                                            )
                                                        }
                                                    )
                                                }
                                            }
                                        }

                                        // Tag Input Row
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            OutlinedTextField(
                                                value = customTagInput,
                                                onValueChange = { customTagInput = it.lowercase().replace(" ", "_") },
                                                placeholder = { Text("Add custom tag") },
                                                singleLine = true,
                                                modifier = Modifier.weight(1f),
                                                shape = RoundedCornerShape(10.dp)
                                            )
                                            Button(
                                                onClick = {
                                                    val cleanTag = customTagInput.trim().removePrefix("#")
                                                    if (cleanTag.isNotBlank() && cleanTag !in selectedTags) {
                                                        selectedTags = selectedTags + cleanTag
                                                        customTagInput = ""
                                                    }
                                                },
                                                enabled = customTagInput.isNotBlank(),
                                                shape = RoundedCornerShape(10.dp)
                                            ) {
                                                Icon(Icons.Filled.Add, contentDescription = "Add Tag")
                                            }
                                        }

                                        // Suggested Tags
                                        Text(
                                            text = "Suggested:",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .horizontalScroll(rememberScrollState()),
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            suggestedTags.filter { it !in selectedTags }.forEach { tag ->
                                                FilterChip(
                                                    selected = false,
                                                    onClick = { selectedTags = selectedTags + tag },
                                                    label = { Text("#$tag", style = MaterialTheme.typography.labelSmall) }
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            3 -> {
                                // ── Step 3: Audience, Monetization & Live Feed Preview ──
                                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                                    // Visibility Options
                                    Card(
                                        shape = RoundedCornerShape(14.dp),
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                            Text(
                                                text = "Audience Visibility",
                                                style = MaterialTheme.typography.labelLarge,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                                            ) {
                                                FilterChip(
                                                    selected = visibility == "PUBLIC",
                                                    onClick = { visibility = "PUBLIC" },
                                                    label = { Text("Public (Recommended)") },
                                                    leadingIcon = {
                                                        if (visibility == "PUBLIC") Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                                                    },
                                                    modifier = Modifier.weight(1f)
                                                )
                                                FilterChip(
                                                    selected = visibility == "UNLISTED",
                                                    onClick = { visibility = "UNLISTED" },
                                                    label = { Text("Unlisted (Link Only)") },
                                                    leadingIcon = {
                                                        if (visibility == "UNLISTED") Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                                                    },
                                                    modifier = Modifier.weight(1f)
                                                )
                                            }
                                        }
                                    }

                                    // Creator Monetization
                                    if (isCreator) {
                                        Card(
                                            shape = RoundedCornerShape(14.dp),
                                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Column(modifier = Modifier.padding(14.dp)) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    modifier = Modifier.fillMaxWidth()
                                                ) {
                                                    Column(modifier = Modifier.weight(1f)) {
                                                        Text("Monetize Plan", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                                        Text(
                                                            text = if (isPaidPlan) "Followers spend credits to unlock" else "Free for all community users",
                                                            style = MaterialTheme.typography.bodySmall,
                                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                                        )
                                                    }
                                                    Switch(checked = isPaidPlan, onCheckedChange = { isPaidPlan = it })
                                                }

                                                if (isPaidPlan) {
                                                    Spacer(modifier = Modifier.height(12.dp))
                                                    Text("Credit Unlock Price", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold)
                                                    Spacer(modifier = Modifier.height(6.dp))
                                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                        listOf(50, 100, 250, 500).forEach { price ->
                                                            FilterChip(
                                                                selected = selectedCreditPrice == price,
                                                                onClick = { selectedCreditPrice = price },
                                                                label = { Text("$price pts", style = MaterialTheme.typography.labelSmall) }
                                                            )
                                                        }
                                                    }
                                                    val creatorEarns = Math.max(1, (selectedCreditPrice * 0.70).toInt())
                                                    val usdEst = String.format(java.util.Locale.US, "%.2f", creatorEarns * 0.007)
                                                    Spacer(modifier = Modifier.height(6.dp))
                                                    Text(
                                                        text = "You earn 70%: $creatorEarns credits (~$$usdEst USD) per unlock",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = MaterialTheme.colorScheme.primary,
                                                        fontWeight = FontWeight.Medium
                                                    )
                                                }
                                            }
                                        }
                                    }

                                    // Live Feed Card Preview
                                    Text(
                                        text = "Explore Feed Card Preview",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold
                                    )

                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = MaterialTheme.colorScheme.surface,
                                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                                        tonalElevation = 2.dp,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(modifier = Modifier.padding(16.dp)) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(32.dp)
                                                        .clip(CircleShape)
                                                        .background(MaterialTheme.colorScheme.primaryContainer),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Text(
                                                        text = (activeUser?.displayName ?: "P").take(1).uppercase(),
                                                        style = MaterialTheme.typography.labelMedium,
                                                        fontWeight = FontWeight.Bold,
                                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                                    )
                                                }
                                                Spacer(modifier = Modifier.width(10.dp))
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(
                                                        text = activeUser?.displayName ?: "Planner Creator",
                                                        style = MaterialTheme.typography.bodyMedium,
                                                        fontWeight = FontWeight.Bold,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                    Text(
                                                        text = "@${activeUser?.username ?: "creator"}",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }
                                                Surface(
                                                    shape = RoundedCornerShape(6.dp),
                                                    color = MaterialTheme.colorScheme.secondaryContainer
                                                ) {
                                                    Text(
                                                        text = "$planDurationDays Days",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        fontWeight = FontWeight.SemiBold,
                                                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                                    )
                                                }
                                            }

                                            Spacer(modifier = Modifier.height(12.dp))

                                            Text(
                                                text = title.ifBlank { "Untitled Routine" },
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Bold,
                                                maxLines = 2,
                                                overflow = TextOverflow.Ellipsis
                                            )

                                            Spacer(modifier = Modifier.height(4.dp))

                                            Text(
                                                text = description.ifBlank { "No description provided." },
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                maxLines = 2,
                                                overflow = TextOverflow.Ellipsis
                                            )

                                            Spacer(modifier = Modifier.height(12.dp))

                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Surface(
                                                    shape = RoundedCornerShape(6.dp),
                                                    color = MaterialTheme.colorScheme.surfaceVariant
                                                ) {
                                                    Text(
                                                        text = selectedCategory,
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                                    )
                                                }

                                                Text(
                                                    text = "${templates.size} Tasks Included",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    fontWeight = FontWeight.Medium,
                                                    color = MaterialTheme.colorScheme.primary
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                if (errorMessage != null) {
                    Spacer(modifier = Modifier.height(14.dp))
                    Card(
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Outlined.ErrorOutline, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = errorMessage ?: "Unknown error occurred",
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            }

            // ── Bottom Action Toolbar ───────────────────────────────────────────
            if (publishedPostId == null) {
                HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (currentStep > 1) {
                        OutlinedButton(
                            onClick = {
                                errorMessage = null
                                currentStep--
                            },
                            enabled = !isPublishing,
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Back")
                        }
                    } else {
                        TextButton(onClick = onDismiss, enabled = !isPublishing) {
                            Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }

                    if (currentStep < 3) {
                        Button(
                            onClick = {
                                if (currentStep == 1) {
                                    if (!hasTasks) {
                                        errorMessage = "Cannot publish a plan with 0 tasks."
                                        return@Button
                                    }
                                    if (!isAuthenticated) {
                                        errorMessage = "You must be signed in with your account to publish."
                                        return@Button
                                    }
                                } else if (currentStep == 2) {
                                    if (title.isBlank()) {
                                        errorMessage = "Title cannot be blank."
                                        return@Button
                                    }
                                }
                                errorMessage = null
                                currentStep++
                            },
                            enabled = when (currentStep) {
                                1 -> hasTasks && isAuthenticated
                                2 -> title.isNotBlank()
                                else -> true
                            },
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("Continue")
                            Spacer(modifier = Modifier.width(6.dp))
                            Icon(Icons.AutoMirrored.Outlined.ArrowForward, contentDescription = null, modifier = Modifier.size(16.dp))
                        }
                    } else {
                        Button(
                            onClick = {
                                if (title.isBlank()) {
                                    errorMessage = "Plan title cannot be blank"
                                    return@Button
                                }
                                if (!hasTasks) {
                                    errorMessage = "Plan must have at least 1 task"
                                    return@Button
                                }
                                if (cloudUid.isNullOrBlank()) {
                                    errorMessage = "Please sign in to publish plans to the online community"
                                    return@Button
                                }

                                isPublishing = true
                                errorMessage = null

                                scope.launch {
                                    try {
                                        val authorEntity = activeUser ?: userDao.getActiveUserOnce()
                                            ?: throw IllegalStateException("No active user session")

                                        val planExporter = PlanExporter()
                                        val exportResult = plannerRepository.exportPlanTemplate(
                                            planId = plan.planId,
                                            author = authorEntity,
                                            tags = selectedTags,
                                            category = selectedCategory
                                        )

                                        if (exportResult.isSuccess) {
                                            val templateDto = exportResult.getOrThrow()
                                            val json = planExporter.toJson(templateDto)

                                            val cloudAuthor = CloudUser(
                                                userId = cloudUid,
                                                username = authorEntity.username ?: authorEntity.displayName.lowercase().replace(" ", "_"),
                                                displayName = authorEntity.displayName,
                                                avatarUrl = authorEntity.avatarUrl,
                                                isCreator = authorEntity.isCreator
                                            )

                                            val postResult = socialRepository.createPost(
                                                author = cloudAuthor,
                                                title = title,
                                                description = description,
                                                planTemplateJson = json,
                                                durationDays = templateDto.targetDurationDays,
                                                tags = templateDto.tags,
                                                category = selectedCategory,
                                                isPaid = isPaidPlan && isCreator,
                                                creditCost = if (isPaidPlan && isCreator) selectedCreditPrice else 0,
                                                visibility = visibility
                                            )

                                            if (postResult.isSuccess) {
                                                val post = postResult.getOrThrow()

                                                // Update local Room database so plan reflects its public status
                                                plannerRepository.setPlanPublicStatus(plan.planId, true)

                                                // Link local plan to published community post
                                                plannerRepository.insertJoinedCommunity(
                                                    JoinedCommunityEntity(
                                                        localPlanId = plan.planId,
                                                        postId = post.postId,
                                                        communityTitle = title,
                                                        creatorName = authorEntity.displayName
                                                    )
                                                )

                                                // Award viral plan share credits
                                                creditRepository.awardPlanShare(authorEntity.userId, post.postId)

                                                publishedPostId = post.postId
                                            } else {
                                                errorMessage = postResult.exceptionOrNull()?.message ?: "Failed to publish post to community"
                                            }
                                        } else {
                                            errorMessage = exportResult.exceptionOrNull()?.message ?: "Failed to export plan template"
                                        }
                                    } catch (e: Exception) {
                                        errorMessage = e.message ?: "An unexpected error occurred during publish"
                                    } finally {
                                        isPublishing = false
                                    }
                                }
                            },
                            enabled = !isPublishing && title.isNotBlank() && hasTasks && isAuthenticated,
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            if (isPublishing) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Publishing...")
                            } else {
                                Icon(Icons.Outlined.Public, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Publish Plan", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun formatDayChips(selectedDays: String): String {
    if (selectedDays.isBlank()) return "No days set"
    val days = selectedDays.split(",").mapNotNull { it.trim().toIntOrNull() }.sorted()
    if (days.size == 7) return "Repeats every day"
    val dayNames = mapOf(1 to "Mon", 2 to "Tue", 3 to "Wed", 4 to "Thu", 5 to "Fri", 6 to "Sat", 7 to "Sun")
    return "Repeats: " + days.mapNotNull { dayNames[it] }.joinToString(", ")
}
