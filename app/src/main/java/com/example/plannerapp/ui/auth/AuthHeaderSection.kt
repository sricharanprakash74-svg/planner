package com.example.plannerapp.ui.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import com.example.plannerapp.theme.AppDimens

@Composable
fun AuthHeaderSection(
    isSignUpMode: Boolean,
    onTabSelected: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // App Brand Icon — 56dp on the 4pt grid
        Box(
            modifier = Modifier
                .size(AppDimens.IconBoxXl)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Outlined.Folder,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size(AppDimens.IconSizeXl)
            )
        }

        Spacer(modifier = Modifier.height(AppDimens.Space16))

        Text(
            text = "PlannerApp",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(modifier = Modifier.height(AppDimens.Space8))

        Text(
            text = if (isSignUpMode) {
                "Create an account to backup and sync your plans"
            } else {
                "Sign in to access your plans and community features"
            },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(AppDimens.Space24))

        // Auth Mode Tabs (Sign In / Create Account)
        PrimaryTabRow(
            selectedTabIndex = if (isSignUpMode) 1 else 0,
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
            contentColor = MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(AppDimens.CornerCompact))
        ) {
            Tab(
                selected = !isSignUpMode,
                onClick = { onTabSelected(false) },
                text = { Text("Sign In", fontWeight = FontWeight.SemiBold) }
            )
            Tab(
                selected = isSignUpMode,
                onClick = { onTabSelected(true) },
                text = { Text("Create Account", fontWeight = FontWeight.SemiBold) }
            )
        }
    }
}
