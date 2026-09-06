package com.example.plannerapp.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeleteAccountScreen(
    viewModel: SettingsViewModel,
    onBack: () -> Unit,
    onAccountDeleted: () -> Unit,
    modifier: Modifier = Modifier
) {
    var confirmed by remember { mutableStateOf(false) }
    var selectedReason by remember { mutableIntStateOf(0) }
    var showConfirmDialog by remember { mutableStateOf(false) }

    val reasons = listOf(
        "I'm not using the app anymore",
        "Found a better alternative",
        "Privacy or security concerns",
        "I want to start over with a fresh account",
        "Other reason"
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Delete Account", fontWeight = FontWeight.Bold) },
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
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Warning,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onErrorContainer
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Warning: This action is permanent",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Deleting your account will erase all your local plans, habit streaks, custom badges, and cloud backups. This cannot be undone.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Why are you leaving?",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))

            reasons.forEachIndexed { index, reason ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { selectedReason = index }
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = selectedReason == index,
                        onClick = { selectedReason = index }
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = reason, style = MaterialTheme.typography.bodyMedium)
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { confirmed = !confirmed }
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = confirmed,
                    onCheckedChange = { confirmed = it }
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "I understand that my account and all data will be permanently deleted.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = { showConfirmDialog = true },
                enabled = confirmed,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Delete My Account", color = MaterialTheme.colorScheme.onError)
            }
        }
    }

    if (showConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showConfirmDialog = false },
            title = { Text("Confirm Deletion") },
            text = { Text("Are you completely sure? This will wipe your account and all associated plans immediately.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showConfirmDialog = false
                        viewModel.deleteAccount {
                            onAccountDeleted()
                        }
                    }
                ) {
                    Text("Yes, Delete", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
