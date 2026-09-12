package com.example.plannerapp.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.plannerapp.theme.AppDimens

/**
 * Builds a shimmer brush that sweeps from left to right.
 *
 * Gradient contrast is intentionally subtle (alpha 0.04 → 0.12) so the shimmer
 * is readable without being aggressive or distracting in dark mode.
 */
@Composable
private fun shimmerBrush(widthPx: Float = 1000f): Brush {
    val shimmerColors = listOf(
        Color.White.copy(alpha = 0.04f),
        Color.White.copy(alpha = 0.12f),
        Color.White.copy(alpha = 0.04f),
    )
    val transition = rememberInfiniteTransition(label = "shimmer")
    val translateAnim by transition.animateFloat(
        initialValue = 0f,
        targetValue = widthPx * 2,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 1200,
                easing = FastOutSlowInEasing
            ),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmerTranslate"
    )
    return Brush.linearGradient(
        colors = shimmerColors,
        start = Offset(translateAnim - widthPx, 0f),
        end = Offset(translateAnim, 0f)
    )
}

/**
 * Generic shimmer placeholder box.
 *
 * @param modifier Caller-provided sizing/layout modifier.
 * @param cornerRadius Corner radius for the rounded rectangle.
 */
@Composable
fun ShimmerBox(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = AppDimens.CornerMicro
) {
    val baseColor = MaterialTheme.colorScheme.surfaceVariant
    val brush = shimmerBrush()
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(cornerRadius))
            .background(baseColor)
            .background(brush)
    )
}

/**
 * Skeleton placeholder that mimics the visual layout of [PlanFolderCard].
 *
 * Layout: folder icon line | title lines | date line | progress bar | footer row.
 */
@Composable
fun PlanCardSkeleton(modifier: Modifier = Modifier) {
    val baseColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
    val brush = shimmerBrush()
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(AppDimens.CornerCard))
            .background(baseColor)
            .background(brush)
    ) {
        Column(modifier = Modifier.padding(AppDimens.Space16)) {
            // Top row: icon box + menu dot
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                ShimmerBox(
                    modifier = Modifier
                        .size(AppDimens.IconSizeLg)
                        .clip(RoundedCornerShape(AppDimens.CornerMicro))
                )
                ShimmerBox(
                    modifier = Modifier
                        .size(width = 16.dp, height = AppDimens.IconSizeLg)
                        .clip(RoundedCornerShape(AppDimens.CornerMicro))
                )
            }

            Spacer(modifier = Modifier.height(AppDimens.Space12))

            // Title line 1
            ShimmerBox(
                modifier = Modifier
                    .fillMaxWidth(0.8f)
                    .height(14.dp)
            )
            Spacer(modifier = Modifier.height(AppDimens.Space4))
            // Title line 2 (partial)
            ShimmerBox(
                modifier = Modifier
                    .fillMaxWidth(0.55f)
                    .height(14.dp)
            )

            Spacer(modifier = Modifier.height(AppDimens.Space8))

            // Date range line
            ShimmerBox(
                modifier = Modifier
                    .fillMaxWidth(0.5f)
                    .height(11.dp)
            )

            Spacer(modifier = Modifier.height(AppDimens.Space8))

            // Progress bar
            ShimmerBox(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp),
                cornerRadius = 2.dp
            )

            Spacer(modifier = Modifier.height(AppDimens.Space8))

            // Footer row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                ShimmerBox(
                    modifier = Modifier
                        .width(64.dp)
                        .height(11.dp)
                )
                ShimmerBox(
                    modifier = Modifier
                        .width(40.dp)
                        .height(11.dp)
                )
            }
        }
    }
}

/**
 * Skeleton placeholder for the Analytics screen.
 *
 * Layout: chart card stub | two stat card stubs.
 */
@Composable
fun AnalyticsSkeleton(modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxWidth()) {
        // Chart card
        ShimmerBox(
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp),
            cornerRadius = AppDimens.CornerCard
        )

        Spacer(modifier = Modifier.height(AppDimens.Space16))

        // Stat cards row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(AppDimens.Space16)
        ) {
            ShimmerBox(
                modifier = Modifier
                    .weight(1f)
                    .height(88.dp),
                cornerRadius = AppDimens.CornerCard
            )
            ShimmerBox(
                modifier = Modifier
                    .weight(1f)
                    .height(88.dp),
                cornerRadius = AppDimens.CornerCard
            )
        }

        Spacer(modifier = Modifier.height(AppDimens.Space16))

        // Bar chart card
        ShimmerBox(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp),
            cornerRadius = AppDimens.CornerCard
        )

        Spacer(modifier = Modifier.height(AppDimens.Space16))

        // Heatmap card
        ShimmerBox(
            modifier = Modifier
                .fillMaxWidth()
                .height(140.dp),
            cornerRadius = AppDimens.CornerCard
        )
    }
}
