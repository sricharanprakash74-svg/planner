package com.example.plannerapp.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlanPrivacySettingsScreen(
    viewModel: SettingsViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val saved = remember { viewModel.loadPlanPrivacyPrefs(context) }
    val visibilityOptions = listOf(
        Pair("Public", "Anyone in the community can view your published plans"),
        Pair("Private", "Only you can see your plans"),
        Pair("Friends Only", "Only mutual followers can view your plans")
    )

    var selectedVisibility by remember { mutableIntStateOf(saved["visibility"] as Int) }
    var allowComments by remember { mutableStateOf(saved["allow_comments"] as Boolean) }
    var allowForking by remember { mutableStateOf(saved["allow_forking"] as Boolean) }

    fun persist() { viewModel.savePlanPrivacyPrefs(context, selectedVisibility, allowComments, allowForking) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Plan Privacy", fontWeight = FontWeight.Bold) },
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
            Text("Default Visibility", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(4.dp))
            Text("Choose who can see your published plans by default.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(12.dp))
            visibilityOptions.forEachIndexed { index, (label, desc) ->
                Row(
                    modifier = Modifier.fillMaxWidth().clickable { selectedVisibility = index; persist() }.padding(vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(selected = selectedVisibility == index, onClick = { selectedVisibility = index; persist() })
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(text = label, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                        Text(text = desc, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Spacer(modifier = Modifier.height(16.dp))
            Text("Interactions", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            PrivacyToggleRow("Allow Comments", "Let community members comment on your published plans", allowComments) { allowComments = it; persist() }
            PrivacyToggleRow("Allow Plan Forking", "Let others clone and adapt your plans for personal use", allowForking) { allowForking = it; persist() }
        }
    }
}

@Composable
private fun PrivacyToggleRow(title: String, subtitle: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
            Text(text = subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(modifier = Modifier.width(16.dp))
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
