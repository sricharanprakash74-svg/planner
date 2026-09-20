package com.example.plannerapp.ui.onboarding

import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.plannerapp.notifications.NotificationHelper
import com.example.plannerapp.theme.AppAccent
import com.example.plannerapp.theme.AppDimens
import com.example.plannerapp.theme.AppPrimary
import com.example.plannerapp.theme.AppPrimaryButton
import com.example.plannerapp.theme.AppPrimaryButtonPressed
import com.example.plannerapp.theme.AppSurfaceTintSelectedChip

@Composable
fun OnboardingNotificationsScreen(
    onPermissionResult: (Boolean) -> Unit
) {
    val context = LocalContext.current

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            NotificationHelper.createChannel(context)
        }
        onPermissionResult(isGranted)
    }

    val handleAllow = {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
        } else {
            NotificationHelper.createChannel(context)
            onPermissionResult(true)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = AppDimens.ScreenHorizontalPadding),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.weight(0.6f))

        // ── Bell Icon Container: 80dp square, background rgba(#F5D400, 0.15), cornerRadius 20dp, icon #D4720E size 36dp ──
        Box(
            modifier = Modifier
                .size(AppDimens.IconBoxBell)
                .clip(RoundedCornerShape(AppDimens.CornerPill))
                .background(AppSurfaceTintSelectedChip),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Outlined.NotificationsActive,
                contentDescription = null,
                tint = AppPrimary,
                modifier = Modifier.size(AppDimens.IconSizeDisplay)
            )
        }

        Spacer(modifier = Modifier.height(AppDimens.SectionVerticalGap))

        // ── Screen Title: 26sp bold, letterSpacing -0.3sp, #1A1D23 ──
        Text(
            text = "Stay on track",
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(AppDimens.Space8))

        // ── Body Text: 15sp #6B7280, center-aligned, max 2 lines ──
        Text(
            text = "Get reminders before your plans start and helpful updates when something important happens.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = AppDimens.Space12),
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )

        Spacer(modifier = Modifier.height(AppDimens.SectionVerticalGap))

        // ── Benefits Card: Clean white surface, 16dp corner radius, 2dp elevation ──
        Surface(
            shape = RoundedCornerShape(AppDimens.CornerCard),
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = AppDimens.ElevationCard,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(AppDimens.CardInternalPadding),
                verticalArrangement = Arrangement.spacedBy(AppDimens.Space14)
            ) {
                BenefitRow(
                    title = "Plan reminders",
                    description = "Timely alerts before daily habits and scheduled tasks."
                )
                BenefitRow(
                    title = "Important social updates",
                    description = "When someone uses, upvotes, or comments on your plans."
                )
                BenefitRow(
                    title = "Streak protection reminders",
                    description = "Timely nudges to protect your streak freeze."
                )
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        // ── Primary CTA: #B85E08 (accessible surface), darkens on press to #9E4F04, white text ──
        val buttonSource = remember { MutableInteractionSource() }
        val isButtonPressed by buttonSource.collectIsPressedAsState()
        val buttonBg = if (isButtonPressed) AppPrimaryButtonPressed else AppPrimaryButton

        Button(
            onClick = handleAllow,
            interactionSource = buttonSource,
            modifier = Modifier
                .fillMaxWidth()
                .height(AppDimens.ButtonHeight),
            shape = RoundedCornerShape(AppDimens.CornerButton),
            colors = ButtonDefaults.buttonColors(
                containerColor = buttonBg,
                contentColor = Color.White
            )
        ) {
            Text("Allow notifications", style = MaterialTheme.typography.labelLarge)
        }

        Spacer(modifier = Modifier.height(AppDimens.Space12))

        // ── Ghost Button: 54dp, no bg, no border, label #6B7280 ──
        TextButton(
            onClick = { onPermissionResult(false) },
            modifier = Modifier
                .fillMaxWidth()
                .height(AppDimens.ButtonHeight)
                .padding(bottom = AppDimens.Space16),
            shape = RoundedCornerShape(AppDimens.CornerButton)
        ) {
            Text(
                text = "Maybe later",
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Medium),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun BenefitRow(
    title: String,
    description: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(AppDimens.Space12)
    ) {
        Box(
            modifier = Modifier
                .size(24.dp)
                .clip(CircleShape)
                .background(AppAccent.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = null,
                tint = AppAccent,
                modifier = Modifier.size(AppDimens.IconSizeSm)
            )
        }
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
