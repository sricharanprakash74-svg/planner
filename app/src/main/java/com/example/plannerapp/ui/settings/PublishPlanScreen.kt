package com.example.plannerapp.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Public
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PublishPlanScreen(
    viewModel: PublishPlanViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val plans by viewModel.plans.collectAsState()
    val publishStatus by viewModel.publishStatus.collectAsState()

    var selectedPlanIndex by remember { mutableIntStateOf(0) }
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("Productivity") }
    var dropdownExpanded by remember { mutableStateOf(false) }
    var isPaidPlan by remember { mutableStateOf(false) }
    var selectedCreditPrice by remember { mutableIntStateOf(100) }

    val categories = listOf("Productivity", "Fitness & Health", "Learning & Study", "Mindfulness", "Habits & Routines", "Personal Development")

    // Pre-fill title when plan selection changes
    LaunchedEffect(selectedPlanIndex, plans) {
        if (plans.isNotEmpty() && selectedPlanIndex < plans.size) {
            title = plans[selectedPlanIndex].heading
            description = plans[selectedPlanIndex].description
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Publish a Plan", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        modifier = modifier
    ) { paddingValues ->
        when (publishStatus) {
            is PublishStatus.Success -> {
                Box(
                    modifier = Modifier.fillMaxSize().padding(paddingValues).padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Outlined.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(64.dp))
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Plan Published to Community!", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Other members can now discover, upvote, and clone your routine.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.height(24.dp))
                        Button(onClick = onBack) { Text("Done") }
                    }
                }
            }
            else -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp)
                ) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Outlined.Public, contentDescription = null, tint = MaterialTheme.colorScheme.onSecondaryContainer)
                            Spacer(modifier = Modifier.width(12.dp))
                            Text("Share your structured routines with the community and earn Creator recognition.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSecondaryContainer)
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Plan selector dropdown
                    if (plans.isEmpty()) {
                        Text("You have no plans to publish. Create a plan first.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    } else {
                        Text("Select a Plan", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
                        Spacer(modifier = Modifier.height(8.dp))
                        ExposedDropdownMenuBox(expanded = dropdownExpanded, onExpandedChange = { dropdownExpanded = it }) {
                            OutlinedTextField(
                                value = if (plans.isNotEmpty()) plans[selectedPlanIndex].heading else "",
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Plan") },
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

                        Spacer(modifier = Modifier.height(16.dp))

                        OutlinedTextField(
                            value = title,
                            onValueChange = { title = it },
                            label = { Text("Community Title") },
                            placeholder = { Text("e.g., 30-Day Morning Workout") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        OutlinedTextField(
                            value = description,
                            onValueChange = { description = it },
                            label = { Text("Description") },
                            placeholder = { Text("Describe what this plan helps accomplish...") },
                            modifier = Modifier.fillMaxWidth(),
                            minLines = 3,
                            shape = RoundedCornerShape(12.dp)
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Text("Category", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
                        Spacer(modifier = Modifier.height(8.dp))

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            categories.take(3).forEach { cat ->
                                FilterChip(selected = selectedCategory == cat, onClick = { selectedCategory = cat }, label = { Text(cat) })
                            }
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            categories.drop(3).forEach { cat ->
                                FilterChip(selected = selectedCategory == cat, onClick = { selectedCategory = cat }, label = { Text(cat) })
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
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

                        if (publishStatus is PublishStatus.Error) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "Error: ${(publishStatus as PublishStatus.Error).message}",
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }

                        Spacer(modifier = Modifier.height(32.dp))

                        Button(
                            onClick = {
                                if (plans.isNotEmpty()) {
                                    viewModel.publish(
                                        planId = plans[selectedPlanIndex].planId,
                                        title = title,
                                        description = description,
                                        category = selectedCategory,
                                        tags = listOf(selectedCategory.lowercase().replace(" & ", "_").replace(" ", "_")),
                                        isPaid = isPaidPlan,
                                        creditCost = if (isPaidPlan) selectedCreditPrice else 0
                                    )
                                }
                            },
                            enabled = title.isNotBlank() && description.isNotBlank() && publishStatus !is PublishStatus.Loading,
                            modifier = Modifier.fillMaxWidth().height(50.dp),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(if (publishStatus is PublishStatus.Loading) "Publishing..." else "Publish to Explore Feed")
                        }
                    }
                }
            }
        }
    }
}
