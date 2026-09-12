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
fun AppearanceSettingsScreen(
    viewModel: SettingsViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val saved = remember { viewModel.loadAppearancePrefs(context) }

    var darkMode by remember { mutableStateOf(saved["dark_mode"] as Boolean) }
    var useDynamicColor by remember { mutableStateOf(saved["dynamic_color"] as Boolean) }
    var compactLayout by remember { mutableStateOf(saved["compact"] as Boolean) }

    fun persist() { viewModel.saveAppearancePrefs(context, darkMode, useDynamicColor, compactLayout) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Appearance", fontWeight = FontWeight.Bold) },
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
            AppearanceSectionHeader("Theme")
            AppearanceToggleRow("Dark Mode", "Switch to a darker color scheme", darkMode) { darkMode = it; persist() }
            AppearanceToggleRow("Dynamic Color", "Adapt colors from your wallpaper (Android 12+)", useDynamicColor) { useDynamicColor = it; persist() }
            Spacer(modifier = Modifier.height(16.dp))
            AppearanceSectionHeader("Layout")
            AppearanceToggleRow("Compact Layout", "Show more items per screen with reduced padding", compactLayout) { compactLayout = it; persist() }
            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "Additional theme options will be available in a future update.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun AppearanceSectionHeader(title: String) {
    Text(text = title, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(vertical = 8.dp))
}

@Composable
private fun AppearanceToggleRow(title: String, subtitle: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
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
