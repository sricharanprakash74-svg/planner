package com.example.plannerapp.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

private val faqItems = listOf(
    Pair(
        "How do I create a plan?",
        "Tap the create button on the Home screen. Give your plan a name, set a start date, and add tasks to it. Tasks can have subtasks, time estimates, and repeat settings."
    ),
    Pair(
        "What is Creator Mode?",
        "Creator Mode lets you publish your plans to the community Explore feed. Other users can discover, upvote, and clone your routines. Go to Settings > Creator Studio to enable it."
    ),
    Pair(
        "How does the streak system work?",
        "A streak counts consecutive days you complete at least one task in any active plan. Missing a day resets the streak. You can view your current streak on the Analytics screen."
    ),
    Pair(
        "Can I use the app offline?",
        "Yes. All your plans and tasks are stored locally on your device. Changes sync to the cloud automatically when you are connected to the internet."
    ),
    Pair(
        "How do I back up my data?",
        "Go to Settings > Backup & Sync. You can trigger a manual backup or enable automatic cloud backups. You can also export your data as JSON, CSV, or Markdown."
    ),
    Pair(
        "How do I delete a plan?",
        "Open the plan, tap the three-dot menu in the top right, and select Delete. This action cannot be undone."
    )
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HelpScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var expandedIndex by remember { mutableIntStateOf(-1) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Help & FAQ", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
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
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Frequently Asked Questions",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(16.dp))

            faqItems.forEachIndexed { index, (question, answer) ->
                val isExpanded = expandedIndex == index
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { expandedIndex = if (isExpanded) -1 else index }
                        .padding(vertical = 12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = question,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.weight(1f)
                        )
                        Icon(
                            imageVector = if (isExpanded) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (isExpanded) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = answer,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            }
        }
    }
}
