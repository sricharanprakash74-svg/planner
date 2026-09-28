package com.example.plannerapp.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.plannerapp.auth.SupabaseConfig
import com.example.plannerapp.data.TaskTemplateEntity
import java.time.LocalDate
import java.time.temporal.ChronoUnit

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PublishPlanScreen(
    viewModel: PublishPlanViewModel,
    onBack: () -> Unit,
    onNavigateToDiscussion: ((String) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val plans by viewModel.plans.collectAsState()
    val publishStatus by viewModel.publishStatus.collectAsState()
    val activeUser by viewModel.activeUser.collectAsState(initial = null)

    var selectedPlanIndex by remember { mutableIntStateOf(0) }
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("Productivity") }
    var dropdownExpanded by remember { mutableStateOf(false) }
    var isPaidPlan by remember { mutableStateOf(false) }
    var selectedCreditPrice by remember { mutableIntStateOf(100) }
    var customTagInput by remember { mutableStateOf("") }
    var selectedTags by remember { mutableStateOf(listOf("routine", "habits")) }

    val categories = remember {
        listOf("Productivity", "Fitness & Health", "Learning & Study", "Mindfulness", "Habits & Routines", "Personal Development")
    }
    val suggestedTags = remember {
        listOf("morning", "fitness", "deepwork", "study", "mindset", "focus", "discipline")
    }

    val selectedPlan = plans.getOrNull(selectedPlanIndex)

    // Pre-fill title & description when plan selection changes
    LaunchedEffect(selectedPlanIndex, plans) {
        if (plans.isNotEmpty() && selectedPlanIndex < plans.size) {
            title = plans[selectedPlanIndex].heading
            description = plans[selectedPlanIndex].description
        }
    }

    // Inspect tasks of the currently selected plan
    var templates by remember { mutableStateOf<List<TaskTemplateEntity>>(emptyList()) }
    var isLoadingTemplates by remember { mutableStateOf(false) }

    LaunchedEffect(selectedPlan?.planId) {
        val planId = selectedPlan?.planId
        if (planId != null) {
            isLoadingTemplates = true
            templates = viewModel.getTemplatesForPlan(planId)
            isLoadingTemplates = false
        } else {
            templates = emptyList()
        }
    }

    val planDurationDays = remember(selectedPlan) {
        if (selectedPlan == null) 7
        else {
            try {
                val start = LocalDate.parse(selectedPlan.startDate)
                val end = LocalDate.parse(selectedPlan.endDate)
                (ChronoUnit.DAYS.between(start, end) + 1).coerceAtLeast(1).toInt()
            } catch (_: Exception) {
                7
            }
        }
    }

    val cloudUid = remember(activeUser) {
        SupabaseConfig.auth.currentUserOrNull()?.id ?: activeUser?.cloudUserId
    }
    val isAuthenticated = !cloudUid.isNullOrBlank()
    val isCreator = activeUser?.isCreator == true
    val hasTasks = templates.isNotEmpty()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Publish to Community", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        modifier = modifier
    ) { paddingValues ->
        when (val status = publishStatus) {
            is PublishStatus.Success -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth()
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
                            text = "Plan Published to Community",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Other members can now discover, fork, and follow your structured routine in the Explore feed.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(28.dp))

                        if (onNavigateToDiscussion != null) {
                            Button(
                                onClick = {
                                    viewModel.resetStatus()
                                    onNavigateToDiscussion(status.postId)
                                },
                                modifier = Modifier.fillMaxWidth().height(48.dp),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Outlined.Forum, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Open Community Discussion", fontWeight = FontWeight.SemiBold)
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                        }

                        OutlinedButton(
                            onClick = {
                                viewModel.resetStatus()
                                onBack()
                            },
                            modifier = Modifier.fillMaxWidth().height(48.dp),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Done")
                        }
                    }
                }
            }
            else -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Header Info Card
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Outlined.Public, contentDescription = null, tint = MaterialTheme.colorScheme.onSecondaryContainer)
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = "Share your structured routines with the community. Other users can discover, fork, and track routines alongside you.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                    }

                    // Account Status Warning
                    if (!isAuthenticated) {
                        Card(
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Outlined.ErrorOutline, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(24.dp))
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = "Account Sign-In Required",
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.error
                                    )
                                    Text(
                                        text = "You must be signed in with your account to publish plans to the online community.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onErrorContainer
                                    )
                                }
                            }
                        }
                    }

                    // Plan selector dropdown
                    if (plans.isEmpty()) {
                        Card(
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Outlined.Layers, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(40.dp))
                                Spacer(modifier = Modifier.height(10.dp))
                                Text(
                                    text = "No Plans Found",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Create a plan folder with tasks first before sharing with the community.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                            }
                        }
                    } else {
                        Text("Select a Plan to Publish", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)

                        ExposedDropdownMenuBox(expanded = dropdownExpanded, onExpandedChange = { dropdownExpanded = it }) {
                            OutlinedTextField(
                                value = if (plans.isNotEmpty()) plans[selectedPlanIndex].heading else "",
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Plan Folder") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = dropdownExpanded) },
                                modifier = Modifier.fillMaxWidth().menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable),
                                shape = RoundedCornerShape(12.dp)
                            )
                            ExposedDropdownMenu(expanded = dropdownExpanded, onDismissRequest = { dropdownExpanded = false }) {
                                plans.forEachIndexed { index, plan ->
                                    DropdownMenuItem(
                                        text = { Text(plan.heading) },
                                        onClick = { selectedPlanIndex = index; dropdownExpanded = false }
                                    )
                                }
                            }
                        }

                        // Curriculum Review Card for the selected plan
                        Card(
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (hasTasks) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f) else MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
                            ),
                            border = androidx.compose.foundation.BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = if (hasTasks) Icons.Outlined.CheckCircle else Icons.Outlined.ErrorOutline,
                                        contentDescription = null,
                                        tint = if (hasTasks) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = if (isLoadingTemplates) "Loading routine tasks..."
                                        else if (hasTasks) "Includes ${templates.size} recurring task${if (templates.size > 1) "s" else ""} • $planDurationDays Days duration"
                                        else "Cannot publish: Plan has 0 tasks configured",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.SemiBold,
                                        color = if (hasTasks) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.error
                                    )
                                }

                                if (hasTasks && templates.isNotEmpty()) {
                                    templates.take(3).forEach { tmpl ->
                                        Text(
                                            text = "• ${tmpl.taskDescription}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    if (templates.size > 3) {
                                        Text(
                                            text = "+ ${templates.size - 3} more tasks",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.primary,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                }
                            }
                        }

                        OutlinedTextField(
                            value = title,
                            onValueChange = { title = it },
                            label = { Text("Community Title") },
                            placeholder = { Text("e.g. 30-Day Morning Workout") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        )

                        OutlinedTextField(
                            value = description,
                            onValueChange = { description = it },
                            label = { Text("Description & Instructions") },
                            placeholder = { Text("Describe what this plan helps accomplish and tips for followers...") },
                            modifier = Modifier.fillMaxWidth(),
                            minLines = 3,
                            shape = RoundedCornerShape(12.dp)
                        )

                        // Category Selection
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Category", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                categories.forEach { cat ->
                                    FilterChip(
                                        selected = selectedCategory == cat,
                                        onClick = { selectedCategory = cat },
                                        label = { Text(cat) },
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                }
                            }
                        }

                        // Tags Selection
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Tags", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)

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
                                            onClick = { selectedTags = selectedTags - tag },
                                            label = { Text("#$tag") },
                                            trailingIcon = {
                                                Icon(Icons.Filled.Close, contentDescription = "Remove tag", modifier = Modifier.size(14.dp))
                                            }
                                        )
                                    }
                                }
                            }

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
                                        val clean = customTagInput.trim().removePrefix("#")
                                        if (clean.isNotBlank() && clean !in selectedTags) {
                                            selectedTags = selectedTags + clean
                                            customTagInput = ""
                                        }
                                    },
                                    enabled = customTagInput.isNotBlank(),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Icon(Icons.Filled.Add, contentDescription = "Add")
                                }
                            }

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

                        // Creator Monetization Card
                        if (isCreator) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = "Monetize Plan",
                                                style = MaterialTheme.typography.titleSmall,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Text(
                                                text = if (isPaidPlan) "Users spend credits to unlock" else "Free for all community users",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        Switch(
                                            checked = isPaidPlan,
                                            onCheckedChange = { isPaidPlan = it }
                                        )
                                    }

                                    if (isPaidPlan) {
                                        Spacer(modifier = Modifier.height(10.dp))
                                        Text(
                                            text = "Unlock Price",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            listOf(50, 100, 250, 500).forEach { price ->
                                                FilterChip(
                                                    selected = selectedCreditPrice == price,
                                                    onClick = { selectedCreditPrice = price },
                                                    label = { Text("$price pts", style = MaterialTheme.typography.labelSmall) }
                                                )
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(6.dp))
                                        val creatorEarns = Math.max(1, (selectedCreditPrice * 0.70).toInt())
                                        val usdVal = String.format(java.util.Locale.US, "%.2f", creatorEarns * 0.007)
                                        Text(
                                            text = "You earn 70%: $creatorEarns credits (~$$usdVal USD) per unlock",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.primary,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                }
                            }
                        }

                        // Explore Feed Live Card Preview
                        Text("Live Explore Card Preview", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)

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
                                            text = activeUser?.displayName ?: "Planner User",
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = "@${activeUser?.username ?: "user"}",
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

                                Spacer(modifier = Modifier.height(10.dp))

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
                                        text = "${templates.size} Tasks",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }

                        if (publishStatus is PublishStatus.Error) {
                            Card(
                                shape = RoundedCornerShape(10.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Outlined.ErrorOutline, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(20.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = (publishStatus as PublishStatus.Error).message,
                                        color = MaterialTheme.colorScheme.onErrorContainer,
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        val canPublish = title.isNotBlank() && hasTasks && isAuthenticated && publishStatus !is PublishStatus.Loading

                        Button(
                            onClick = {
                                val currentPlan = selectedPlan ?: return@Button
                                viewModel.publish(
                                    planId = currentPlan.planId,
                                    title = title.trim(),
                                    description = description.trim(),
                                    category = selectedCategory,
                                    tags = selectedTags,
                                    isPaid = isPaidPlan && isCreator,
                                    creditCost = if (isPaidPlan && isCreator) selectedCreditPrice else 0
                                )
                            },
                            enabled = canPublish,
                            modifier = Modifier.fillMaxWidth().height(50.dp),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            if (publishStatus is PublishStatus.Loading) {
                                CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                                Spacer(modifier = Modifier.width(10.dp))
                                Text("Publishing to Community...")
                            } else {
                                Icon(Icons.Outlined.Public, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Publish to Explore Feed", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}
