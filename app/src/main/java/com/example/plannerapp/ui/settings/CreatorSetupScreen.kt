package com.example.plannerapp.ui.settings

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

private val creatorCategories = listOf(
    "Productivity",
    "Fitness & Health",
    "Learning & Study",
    "Mindfulness & Wellness",
    "Habits & Routines",
    "Financial Planning",
    "Personal Development",
    "Creative Projects"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreatorSetupScreen(
    viewModel: SettingsViewModel,
    onBack: () -> Unit,
    onMonetizationClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val user by viewModel.currentUser.collectAsState()
    val isAlreadyCreator = user?.isCreator == true

    var step by remember { mutableIntStateOf(1) }
    val totalSteps = 3

    var selectedCategories by remember {
        mutableStateOf(viewModel.loadCreatorCategories(context).toSet())
    }
    var tagline by remember {
        mutableStateOf(viewModel.loadCreatorTagline(context))
    }
    var guidelinesAccepted by remember { mutableStateOf(isAlreadyCreator) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (isAlreadyCreator) "Creator Studio" else "Become a Creator",
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (isAlreadyCreator) {
                        IconButton(onClick = onMonetizationClick) {
                            Icon(
                                imageVector = Icons.Outlined.Payments,
                                contentDescription = "Creator Earnings & Monetization",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            )
        },
        modifier = modifier
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            LinearProgressIndicator(
                progress = { step / totalSteps.toFloat() },
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )

            AnimatedContent(
                targetState = step,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "creator_setup_step"
            ) { currentStep ->
                when (currentStep) {
                    1 -> StepCategories(
                        selectedCategories = selectedCategories,
                        onToggleCategory = { cat ->
                            selectedCategories = if (cat in selectedCategories) selectedCategories - cat else selectedCategories + cat
                        },
                        onNext = { step = 2 }
                    )
                    2 -> StepTagline(
                        tagline = tagline,
                        onTaglineChange = { tagline = it },
                        onBack = { step = 1 },
                        onNext = { step = 3 }
                    )
                    else -> StepGuidelines(
                        guidelinesAccepted = guidelinesAccepted,
                        onAcceptChange = { guidelinesAccepted = it },
                        onBack = { step = 2 },
                        isAlreadyCreator = isAlreadyCreator,
                        onFinish = {
                            viewModel.saveCreatorCategories(context, selectedCategories.toList())
                            viewModel.saveCreatorTagline(context, tagline)
                            viewModel.setCreatorStatus(true)
                            onBack()
                        },
                        onDisableCreator = {
                            viewModel.setCreatorStatus(false)
                            onBack()
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun StepCategories(
    selectedCategories: Set<String>,
    onToggleCategory: (String) -> Unit,
    onNext: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp)
    ) {
        Text(
            text = "Step 1 of 3",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "What do you create plans for?",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Select all the topics that describe the routines and plans you want to share with the community.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(24.dp))

        creatorCategories.forEach { category ->
            val isSelected = category in selectedCategories
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                shape = RoundedCornerShape(12.dp),
                color = if (isSelected) MaterialTheme.colorScheme.primaryContainer
                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                onClick = { onToggleCategory(category) }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = category,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                        color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer
                        else MaterialTheme.colorScheme.onSurface
                    )
                    if (isSelected) {
                        Icon(
                            imageVector = Icons.Outlined.CheckCircle,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))
        Button(
            onClick = onNext,
            enabled = selectedCategories.isNotEmpty(),
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = RoundedCornerShape(12.dp)
        ) { Text("Continue") }
    }
}

@Composable
private fun StepTagline(
    tagline: String,
    onTaglineChange: (String) -> Unit,
    onBack: () -> Unit,
    onNext: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp)
    ) {
        Text(
            text = "Step 2 of 3",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Your creator tagline",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "A short line that describes you as a creator. This appears on your public creator profile.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(24.dp))
        OutlinedTextField(
            value = tagline,
            onValueChange = { if (it.length <= 100) onTaglineChange(it) },
            label = { Text("Creator tagline") },
            placeholder = { Text("e.g., Helping you build better daily habits") },
            minLines = 2,
            maxLines = 4,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            supportingText = { Text("${tagline.length}/100") }
        )
        Spacer(modifier = Modifier.height(32.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(
                onClick = onBack,
                modifier = Modifier.weight(1f).height(52.dp),
                shape = RoundedCornerShape(12.dp)
            ) { Text("Back") }
            Button(
                onClick = onNext,
                enabled = tagline.isNotBlank(),
                modifier = Modifier.weight(1f).height(52.dp),
                shape = RoundedCornerShape(12.dp)
            ) { Text("Continue") }
        }
    }
}

@Composable
private fun StepGuidelines(
    guidelinesAccepted: Boolean,
    onAcceptChange: (Boolean) -> Unit,
    onBack: () -> Unit,
    isAlreadyCreator: Boolean,
    onFinish: () -> Unit,
    onDisableCreator: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp)
    ) {
        Text(
            text = "Step 3 of 3",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Community guidelines",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(16.dp))
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                GuidelineRow(Icons.Outlined.CheckCircle, "Publish original, structured routines and plans")
                GuidelineRow(Icons.Outlined.CheckCircle, "Keep content meaningful and actionable for real users")
                GuidelineRow(Icons.Outlined.CheckCircle, "Respect other creators and community members")
                GuidelineRow(Icons.Outlined.CheckCircle, "Do not publish misleading, harmful, or duplicate plans")
            }
        }
        Spacer(modifier = Modifier.height(20.dp))
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
            Checkbox(checked = guidelinesAccepted, onCheckedChange = onAcceptChange)
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "I agree to follow the PlannerApp creator community guidelines.",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 12.dp)
            )
        }
        Spacer(modifier = Modifier.height(32.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(
                onClick = onBack,
                modifier = Modifier.weight(1f).height(52.dp),
                shape = RoundedCornerShape(12.dp)
            ) { Text("Back") }
            Button(
                onClick = onFinish,
                enabled = guidelinesAccepted,
                modifier = Modifier.weight(1f).height(52.dp),
                shape = RoundedCornerShape(12.dp)
            ) { Text(if (isAlreadyCreator) "Save Changes" else "Start Creating") }
        }
        if (isAlreadyCreator) {
            Spacer(modifier = Modifier.height(24.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Spacer(modifier = Modifier.height(16.dp))
            TextButton(onClick = onDisableCreator, modifier = Modifier.fillMaxWidth()) {
                Text("Disable Creator Mode", color = MaterialTheme.colorScheme.error)
            }
        }
    }
}

@Composable
private fun GuidelineRow(icon: ImageVector, text: String) {
    Row(verticalAlignment = Alignment.Top) {
        Icon(imageVector = icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp).padding(top = 2.dp))
        Spacer(modifier = Modifier.width(10.dp))
        Text(text = text, style = MaterialTheme.typography.bodyMedium)
    }
}
