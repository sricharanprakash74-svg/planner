package com.example.plannerapp.ui.settings

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
fun AccessibilitySettingsScreen(
    viewModel: SettingsViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val saved = remember { viewModel.loadAccessibilityPrefs(context) }
    val textSizeLabels = listOf("Small", "Default", "Large", "Extra Large")

    var highContrast by remember { mutableStateOf(saved["high_contrast"] as Boolean) }
    var reducedMotion by remember { mutableStateOf(saved["reduced_motion"] as Boolean) }
    var largerTouchTargets by remember { mutableStateOf(saved["larger_targets"] as Boolean) }
    var textSizeIndex by remember { mutableIntStateOf(saved["text_size"] as Int) }

    fun persist() { viewModel.saveAccessibilityPrefs(context, highContrast, reducedMotion, largerTouchTargets, textSizeIndex) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Accessibility", fontWeight = FontWeight.Bold) },
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
            AccessibilitySectionHeader("Text")
            Text("Text size", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium, modifier = Modifier.padding(vertical = 8.dp))
            Slider(
                value = textSizeIndex.toFloat(),
                onValueChange = { textSizeIndex = it.toInt() },
                onValueChangeFinished = { persist() },
                valueRange = 0f..3f,
                steps = 2,
                modifier = Modifier.fillMaxWidth()
            )
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                textSizeLabels.forEach { label -> Text(text = label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            }
            Spacer(modifier = Modifier.height(20.dp))
            AccessibilitySectionHeader("Display")
            AccessibilityToggleRow("High Contrast", "Increase contrast for text and icons", highContrast) { highContrast = it; persist() }
            AccessibilityToggleRow("Reduce Motion", "Minimize animations and transitions", reducedMotion) { reducedMotion = it; persist() }
            AccessibilityToggleRow("Larger Touch Targets", "Increase tappable area for buttons and list items", largerTouchTargets) { largerTouchTargets = it; persist() }
            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Spacer(modifier = Modifier.height(12.dp))
            Text("For system-level accessibility, visit your device Settings > Accessibility.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun AccessibilitySectionHeader(title: String) {
    Text(text = title, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(vertical = 8.dp))
}

@Composable
private fun AccessibilityToggleRow(title: String, subtitle: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
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
