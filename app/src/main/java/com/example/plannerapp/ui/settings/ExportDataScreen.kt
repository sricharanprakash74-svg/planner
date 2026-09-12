package com.example.plannerapp.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.FileDownload
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExportDataScreen(
    viewModel: ExportDataViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val exportStatus by viewModel.exportStatus.collectAsState()

    val formatOptions = listOf("JSON (Recommended for Backup)", "CSV (Excel Compatible)", "Markdown (Readable Notes)")
    val formatKeys = listOf("JSON", "CSV", "Markdown")
    var selectedFormat by remember { mutableIntStateOf(0) }
    var includeHistory by remember { mutableStateOf(true) }
    var includeBadges by remember { mutableStateOf(true) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Export Data", fontWeight = FontWeight.Bold) },
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
                .padding(16.dp)
        ) {
            Text("Export Format", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))

            formatOptions.forEachIndexed { index, format ->
                Surface(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).clickable { selectedFormat = index },
                    shape = RoundedCornerShape(10.dp),
                    color = if (selectedFormat == index) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                    else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                ) {
                    Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(selected = selectedFormat == index, onClick = { selectedFormat = index })
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = format, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
            Spacer(modifier = Modifier.height(16.dp))

            Text("Data to Include", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))

            Row(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = true, onCheckedChange = {}, enabled = false)
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text("Plan & Task Definitions", style = MaterialTheme.typography.bodyLarge)
                    Text("Included by default", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Row(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = includeHistory, onCheckedChange = { includeHistory = it })
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text("Daily Check-in History", style = MaterialTheme.typography.bodyLarge)
                    Text("Completion timestamps and subtask status", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Row(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = includeBadges, onCheckedChange = { includeBadges = it })
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text("Badges & Streaks", style = MaterialTheme.typography.bodyLarge)
                    Text("All unlocked achievement records", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            when (val status = exportStatus) {
                is ExportStatus.Success -> {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Outlined.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text("Export complete!", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                                Text("Saved to: ${status.filePath}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f))
                            }
                        }
                    }
                }
                is ExportStatus.Error -> {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Export failed: ${status.message}", modifier = Modifier.padding(16.dp), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onErrorContainer)
                    }
                }
                is ExportStatus.Loading -> {
                    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(modifier = Modifier.padding(16.dp))
                    }
                }
                else -> {}
            }

            Spacer(modifier = Modifier.height(32.dp))
            Button(
                onClick = {
                    viewModel.exportData(context, formatKeys[selectedFormat], includeHistory, includeBadges)
                },
                enabled = exportStatus !is ExportStatus.Loading,
                modifier = Modifier.fillMaxWidth().height(50.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Outlined.FileDownload, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(if (exportStatus is ExportStatus.Loading) "Exporting..." else "Generate Export")
            }
        }
    }
}
