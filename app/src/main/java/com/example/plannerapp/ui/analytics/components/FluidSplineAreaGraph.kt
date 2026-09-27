package com.example.plannerapp.ui.analytics.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ShowChart
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.LocalDate

/**
 * Component A: Continuous Spline Wave Area Chart
 *
 * Visuals: Drawn with smooth cubic Bézier splines (`Path.cubicTo`) and a soft vertical gradient.
 * Progressive Sweep: Revealing from left-to-right using `clipRect(right = width * progress.value)`
 * driven by an Animatable with FastOutSlowInEasing over 1300ms.
 */
@Composable
fun FluidSplineAreaGraph(
    uiState: AnalyticsGraphUiState,
    modifier: Modifier = Modifier
) {
    val isDark = isSystemInDarkTheme()
    // Styling: Parchment / warm off-white (#FBF8F5) on light, surfaceVariant tint on dark
    val containerBg = if (isDark) {
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
    } else {
        Color(0xFFFBF8F5)
    }
    val strokeColor = Color(0xFFE28A73) // Warm Coral / Peach
    val fillGradient = Brush.verticalGradient(
        colors = listOf(
            Color(0xFFE28A73).copy(alpha = 0.40f), // 40% alpha at the top
            Color(0xFFE28A73).copy(alpha = 0.02f)  // 2% alpha at the bottom
        )
    )

    // Progressive sweep animation: left-to-right reveal
    val sweepProgress = remember { Animatable(0f) }
    LaunchedEffect(uiState.monthCurveData) {
        sweepProgress.snapTo(0f)
        sweepProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(
                durationMillis = 1300,
                easing = FastOutSlowInEasing
            )
        )
    }

    val totalCompleted = remember(uiState.monthCurveData) {
        uiState.monthCurveData.sum().toInt()
    }
    val maxDaily = remember(uiState.monthCurveData) {
        uiState.monthCurveData.maxOrNull()?.toInt() ?: 0
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = containerBg),
        border = BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outlineVariant.copy(alpha = if (isDark) 0.20f else 0.45f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.ShowChart,
                            contentDescription = null,
                            tint = strokeColor,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "Monthly Momentum",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Continuous habit flow wave",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Metric Capsule
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = strokeColor.copy(alpha = 0.12f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "$totalCompleted",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = strokeColor
                        )
                        Text(
                            text = "completed",
                            style = MaterialTheme.typography.labelSmall,
                            color = strokeColor.copy(alpha = 0.85f)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Chart Canvas
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val width = size.width
                    val height = size.height
                    val paddingStart = 4.dp.toPx()
                    val paddingEnd = 4.dp.toPx()
                    val paddingTop = 16.dp.toPx()
                    val paddingBottom = 20.dp.toPx()

                    val usableWidth = width - paddingStart - paddingEnd
                    val usableHeight = height - paddingTop - paddingBottom

                    // Draw soft baseline and guide lines
                    val guideColor = if (isDark) Color.White.copy(alpha = 0.06f) else Color.Black.copy(alpha = 0.05f)
                    val levels = listOf(0.25f, 0.5f, 0.75f, 1.0f)
                    levels.forEach { level ->
                        val y = paddingTop + usableHeight * (1f - level)
                        drawLine(
                            color = guideColor,
                            start = Offset(paddingStart, y),
                            end = Offset(width - paddingEnd, y),
                            strokeWidth = 1.dp.toPx()
                        )
                    }

                    val rawData = uiState.monthCurveData
                    val count = rawData.size

                    if (count >= 2) {
                        val maxVal = (rawData.maxOrNull() ?: 0f).let { if (it > 0f) it * 1.15f else 4f }

                        // Generate points across usable area
                        val points = rawData.mapIndexed { index, value ->
                            val x = paddingStart + (index.toFloat() / (count - 1)) * usableWidth
                            val ratio = (value / maxVal).coerceIn(0f, 1f)
                            val y = paddingTop + (1f - ratio) * usableHeight
                            Offset(x, y)
                        }

                        // Build smooth cubic Bézier spline
                        val strokePath = Path().apply {
                            moveTo(points[0].x, points[0].y)
                            for (i in 0 until points.size - 1) {
                                val p0 = points[i]
                                val p1 = points[i + 1]
                                val midX = (p0.x + p1.x) / 2f
                                cubicTo(midX, p0.y, midX, p1.y, p1.x, p1.y)
                            }
                        }

                        // Build area fill path closing down to bottom
                        val fillPath = Path().apply {
                            addPath(strokePath)
                            lineTo(points.last().x, height - paddingBottom)
                            lineTo(points.first().x, height - paddingBottom)
                            close()
                        }

                        // Progressive sweep animation via clipRect
                        clipRect(
                            left = 0f,
                            top = 0f,
                            right = width * sweepProgress.value,
                            bottom = height
                        ) {
                            // 1. Fill gradient beneath the curve
                            drawPath(
                                path = fillPath,
                                brush = fillGradient
                            )

                            // 2. Continuous spline stroke
                            drawPath(
                                path = strokePath,
                                color = strokeColor,
                                style = Stroke(
                                    width = 2.5.dp.toPx(),
                                    cap = StrokeCap.Round,
                                    join = StrokeJoin.Round
                                )
                            )

                            // 3. Peak highlight points
                            val peakIndex = rawData.indexOfFirst { it == rawData.maxOrNull() }
                            if (peakIndex in points.indices && (rawData.maxOrNull() ?: 0f) > 0f) {
                                val peakPoint = points[peakIndex]
                                drawCircle(
                                    color = strokeColor.copy(alpha = 0.25f),
                                    radius = 7.dp.toPx(),
                                    center = peakPoint
                                )
                                drawCircle(
                                    color = strokeColor,
                                    radius = 4.dp.toPx(),
                                    center = peakPoint
                                )
                                drawCircle(
                                    color = Color.White,
                                    radius = 2.dp.toPx(),
                                    center = peakPoint
                                )
                            }
                        }
                    }
                }
            }

            // Timeline labels at base
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Day 1",
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
                Text(
                    text = "Day 15",
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
                val daysInMonth = remember { LocalDate.now().lengthOfMonth() }
                Text(
                    text = "Day $daysInMonth",
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }
        }
    }
}
