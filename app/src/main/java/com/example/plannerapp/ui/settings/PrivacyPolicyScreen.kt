package com.example.plannerapp.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrivacyPolicyScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Privacy Policy", fontWeight = FontWeight.Bold) },
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
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Text(
                text = "Effective Date: September 1, 2026",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(16.dp))

            PrivacySection(
                title = "1. Information We Collect",
                body = "PlannerApp collects the information you provide when creating an account, such as your display name and email address. We also collect usage data, including plans and tasks you create, completion history, and streak data. No sensitive personal data is required to use the app."
            )
            PrivacySection(
                title = "2. How We Use Your Information",
                body = "Your data is used to provide the core functionality of the app: storing your plans locally on your device, syncing them to the cloud when you enable backup, and showing your activity analytics. We do not sell your personal data to third parties."
            )
            PrivacySection(
                title = "3. Data Storage",
                body = "All plans and tasks are stored locally on your device using an encrypted database. If you enable cloud backup, your data is encrypted in transit and at rest on our servers. You can export or delete your data at any time from the Settings screen."
            )
            PrivacySection(
                title = "4. Community Content",
                body = "If you enable Creator Mode and publish plans to the Explore feed, those plans become publicly visible to all app users. Published plans include your display name. You can unpublish a plan at any time."
            )
            PrivacySection(
                title = "5. Your Rights",
                body = "You have the right to access, correct, or delete your personal data at any time. To delete all your data, go to Settings > Profile Center > Delete Account. For data access requests, contact our support team."
            )
            PrivacySection(
                title = "6. Contact",
                body = "If you have questions about this policy, please contact us at privacy@plannerapp.example.com."
            )
        }
    }
}

@Composable
private fun PrivacySection(title: String, body: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.Bold
    )
    Spacer(modifier = Modifier.height(6.dp))
    Text(
        text = body,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
    Spacer(modifier = Modifier.height(20.dp))
}
