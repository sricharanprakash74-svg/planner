package com.example.plannerapp.ui.onboarding

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.plannerapp.theme.AppDimens
import com.example.plannerapp.theme.AppDisabledBgLight
import com.example.plannerapp.theme.AppDisabledText
import com.example.plannerapp.theme.AppPrimary
import com.example.plannerapp.theme.AppPrimaryButton
import com.example.plannerapp.theme.AppPrimaryButtonPressed
import com.example.plannerapp.theme.AppPrimaryDark
import com.example.plannerapp.theme.AppPrimaryTextOnLightTint
import com.example.plannerapp.theme.AppSurfaceTintDark
import com.example.plannerapp.theme.AppSurfaceTintSelectedChip

@Composable
fun OnboardingPreferencesScreen(
    selectedInterests: Set<String>,
    onToggleInterest: (String) -> Unit,
    onContinue: () -> Unit
) {
    val haptic = LocalHapticFeedback.current

    val interests = remember {
        listOf(
            "Fitness" to "🏃",
            "Travel" to "✈️",
            "Learning" to "📚",
            "Finance" to "💰",
            "Nutrition" to "🥗",
            "Mindfulness" to "🧘",
            "Career" to "💼",
            "Creative" to "🎨",
            "Home" to "🏠",
            "Social" to "👥"
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = AppDimens.ScreenHorizontalPadding),
        horizontalAlignment = Alignment.Start
    ) {
        Spacer(modifier = Modifier.height(AppDimens.Space16))

        Text(
            text = "What do you want to plan?",
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "Choose a few interests to personalize your discovery feed.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(AppDimens.SectionVerticalGap))

        // ── Interest Picker Chips (Grid, 48dp height, 12dp radius) ──
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 140.dp),
            horizontalArrangement = Arrangement.spacedBy(AppDimens.Space12),
            verticalArrangement = Arrangement.spacedBy(AppDimens.Space12),
            modifier = Modifier.weight(1f)
        ) {
            items(interests, key = { it.first }) { (interest, emoji) ->
                val isSelected = selectedInterests.contains(interest)
                CleanPreferenceChip(
                    label = interest,
                    emoji = emoji,
                    isSelected = isSelected,
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onToggleInterest(interest)
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(AppDimens.Space16))

        // ── Primary Button: #B85E08 (accessible surface), darkens on press to #9E4F04, white text ──
        val buttonSource = remember { MutableInteractionSource() }
        val isButtonPressed by buttonSource.collectIsPressedAsState()
        val buttonBg = if (isButtonPressed) AppPrimaryButtonPressed else AppPrimaryButton

        Button(
            onClick = onContinue,
            enabled = selectedInterests.isNotEmpty(),
            interactionSource = buttonSource,
            modifier = Modifier
                .fillMaxWidth()
                .height(AppDimens.ButtonHeight),
            shape = RoundedCornerShape(AppDimens.CornerButton),
            colors = ButtonDefaults.buttonColors(
                containerColor = buttonBg,
                contentColor = Color.White,
                disabledContainerColor = AppDisabledBgLight,
                disabledContentColor = AppDisabledText
            )
        ) {
            Text(
                text = "Personalize my feed",
                style = MaterialTheme.typography.labelLarge
            )
        }

        Spacer(modifier = Modifier.height(AppDimens.Space28))
    }
}

/**
 * Clean Minimalist Preference Chip:
 * - Resting state: background #F0F2F5, text #1A1D23
 * - Selected state: background rgba(#F5D400, 0.15) (0.10 in dark mode), border 1.5dp #D4720E, accessible text #A85400
 * - Animate selection toggle with animateColorAsState duration 200ms
 */
@Composable
private fun CleanPreferenceChip(
    label: String,
    emoji: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isDark = isSystemInDarkTheme()

    val primaryBorderColor = AppPrimary // #D4720E
    val restingBgColor = MaterialTheme.colorScheme.surfaceVariant
    val selectedBgColor = if (isDark) AppSurfaceTintDark else AppSurfaceTintSelectedChip
    val selectedTextColor = if (isDark) AppPrimaryDark else AppPrimaryTextOnLightTint

    val backgroundColor by animateColorAsState(
        targetValue = if (isSelected) selectedBgColor else restingBgColor,
        animationSpec = tween(200),
        label = "chip_bg_color"
    )

    val borderColor by animateColorAsState(
        targetValue = if (isSelected) primaryBorderColor else Color.Transparent,
        animationSpec = tween(200),
        label = "chip_border_color"
    )

    val textColor by animateColorAsState(
        targetValue = if (isSelected) selectedTextColor else MaterialTheme.colorScheme.onSurface,
        animationSpec = tween(200),
        label = "chip_text_color"
    )

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(AppDimens.ChipHeight)
            .clip(RoundedCornerShape(AppDimens.CornerCompact))
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            ),
        shape = RoundedCornerShape(AppDimens.CornerCompact),
        color = backgroundColor,
        border = if (isSelected) BorderStroke(AppDimens.BorderFocus, borderColor) else null
    ) {
        Box(
            modifier = Modifier.fillMaxSize().padding(horizontal = AppDimens.Space16),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "$emoji  $label",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                color = textColor
            )
        }
    }
}
