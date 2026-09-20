package com.example.plannerapp.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.FolderSpecial
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.sin

/**
 * Premium fluid-level progress card for Following Plans.
 *
 * Sourced directly from local Room checkin data (daily_checkins table).
 * Works identically offline and online with zero network dependencies.
 *
 * At 0% completion, a visible resting fluid reservoir sits at the bottom of
 * the card with gentle organic wave motion. As progress increases, the liquid
 * rises smoothly with low-bouncy spring easing (natural overshoot and settle).
 *
 * Tapping the card triggers an organic liquid wave disturbance and ripple.
 */
@Composable
fun FluidProgressCard(
    planName: String,
    dateRange: String,
    completionFraction: Float,
    currentDay: Int,
    totalDays: Int,
    isCompleted: Boolean,
    isAnimationEnabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val coroutineScope = rememberCoroutineScope()
    val clampedFraction = completionFraction.coerceIn(0f, 1f)
    val actualPercent = (clampedFraction * 100).toInt()

    // Smoothly animated percentage counter
    val animatedPercent by animateIntAsState(
        targetValue = actualPercent,
        animationSpec = tween(durationMillis = 450, easing = FastOutSlowInEasing),
        label = "animatedPercent"
    )

    val accessibilityLabel = "$planName, $actualPercent% complete, Day $currentDay of $totalDays"

    // Visual liquid target: at 0% there is a guaranteed visible resting baseline (~10% height)
    // so the fluid body is always visible and rippling gently at the bottom of the card.
    val targetVisualLevel = when {
        isCompleted || clampedFraction >= 1f -> 1.0f
        else -> 0.10f + clampedFraction * 0.88f
    }

    // Spring-animated fluid elevation: provides organic slight overshoot and settle
    val liquidElevation = remember { Animatable(targetVisualLevel) }
    LaunchedEffect(targetVisualLevel, isAnimationEnabled) {
        if (!isAnimationEnabled) {
            liquidElevation.snapTo(targetVisualLevel)
        } else {
            liquidElevation.animateTo(
                targetValue = targetVisualLevel,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioLowBouncy,
                    stiffness = Spring.StiffnessMediumLow
                )
            )
        }
    }

    // Continuous organic wave motion
    val infiniteTransition = rememberInfiniteTransition(label = "waveTransition")
    val waveSpeedMillis1 = if (isCompleted) 4000 else 2600
    val waveSpeedMillis2 = if (isCompleted) 5500 else 3800

    val wavePhase1 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2f * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = if (isAnimationEnabled) waveSpeedMillis1 else Int.MAX_VALUE,
                easing = LinearEasing
            )
        ),
        label = "wavePhase1"
    )

    val wavePhase2 by infiniteTransition.animateFloat(
        initialValue = PI.toFloat() * 0.6f,
        targetValue = (2.6f * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = if (isAnimationEnabled) waveSpeedMillis2 else Int.MAX_VALUE,
                easing = LinearEasing
            )
        ),
        label = "wavePhase2"
    )

    // Tap disturbance & ripple
    val waveDisturbance = remember { Animatable(0f) }
    val rippleRadius = remember { Animatable(0f) }
    val rippleAlpha = remember { Animatable(0f) }
    var rippleCenter by remember { mutableStateOf(Offset.Zero) }

    // Color palette
    val primaryColor = MaterialTheme.colorScheme.primary
    val onPrimary = MaterialTheme.colorScheme.onPrimary
    val onSurface = MaterialTheme.colorScheme.onSurface
    val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant
    val surfaceVariant = MaterialTheme.colorScheme.surfaceVariant

    val currentFluidLevel = liquidElevation.value
    val cardShape = RoundedCornerShape(16.dp)

    Surface(
        shape = cardShape,
        color = surfaceVariant.copy(alpha = 0.35f),
        border = BorderStroke(
            width = if (isCompleted) 1.5.dp else 1.dp,
            color = if (isCompleted)
                primaryColor.copy(alpha = 0.65f)
            else
                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)
        ),
        modifier = modifier
            .fillMaxWidth()
            .height(130.dp)
            .semantics { contentDescription = accessibilityLabel }
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(cardShape)
                .pointerInput(Unit) {
                    detectTapGestures(
                        onPress = { tapOffset ->
                            rippleCenter = tapOffset
                            coroutineScope.launch {
                                rippleRadius.snapTo(0f)
                                rippleAlpha.snapTo(0.35f)
                                launch {
                                    rippleRadius.animateTo(
                                        targetValue = 180f,
                                        animationSpec = spring(
                                            dampingRatio = Spring.DampingRatioNoBouncy,
                                            stiffness = Spring.StiffnessMediumLow
                                        )
                                    )
                                }
                                launch {
                                    rippleAlpha.animateTo(
                                        targetValue = 0f,
                                        animationSpec = tween(320)
                                    )
                                }
                                launch {
                                    waveDisturbance.snapTo(1f)
                                    waveDisturbance.animateTo(
                                        targetValue = 0f,
                                        animationSpec = spring(
                                            dampingRatio = Spring.DampingRatioMediumBouncy,
                                            stiffness = Spring.StiffnessLow
                                        )
                                    )
                                }
                            }
                            val released = tryAwaitRelease()
                            if (released) {
                                coroutineScope.launch {
                                    // Short delay so user experiences tactile liquid ripple
                                    delay(160)
                                    onClick()
                                }
                            }
                        }
                    )
                }
        ) {
            // Fluid Canvas Background Simulation
            Canvas(modifier = Modifier.fillMaxSize()) {
                val canvasW = size.width
                val canvasH = size.height
                val levelY = canvasH * (1f - currentFluidLevel)

                val baseAmplitude = if (isCompleted) 2.dp.toPx() else 4.5.dp.toPx()
                val totalAmplitude = baseAmplitude + (waveDisturbance.value * 5.dp.toPx())

                // Layer 1: Back Wave (lighter tone, depth offset)
                drawFluidWave(
                    canvasW = canvasW,
                    canvasH = canvasH,
                    levelY = levelY - 3.dp.toPx(),
                    wavePhase = wavePhase2,
                    amplitude = totalAmplitude * 0.75f,
                    color = primaryColor.copy(alpha = 0.20f)
                )

                // Layer 2: Main Liquid Body (rich vertical gradient)
                val liquidGradient = Brush.verticalGradient(
                    colors = listOf(
                        primaryColor.copy(alpha = 0.42f),
                        primaryColor.copy(alpha = 0.76f)
                    ),
                    startY = (levelY - totalAmplitude).coerceAtLeast(0f),
                    endY = canvasH
                )

                drawFluidWave(
                    canvasW = canvasW,
                    canvasH = canvasH,
                    levelY = levelY,
                    wavePhase = wavePhase1,
                    amplitude = totalAmplitude,
                    color = primaryColor.copy(alpha = 0.60f),
                    gradient = liquidGradient
                )

                // Layer 3: Surface Glint / Crest Highlight Line
                drawWaveCrestHighlight(
                    canvasW = canvasW,
                    canvasH = canvasH,
                    levelY = levelY,
                    wavePhase = wavePhase1,
                    amplitude = totalAmplitude,
                    highlightColor = Color.White.copy(alpha = 0.35f)
                )

                // Layer 4: Tap Ripple
                if (rippleAlpha.value > 0f && rippleRadius.value > 0f) {
                    drawCircle(
                        color = Color.White.copy(alpha = rippleAlpha.value),
                        radius = rippleRadius.value,
                        center = rippleCenter
                    )
                }
            }

            // Foreground Text and Controls (Protected contrast zone)
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Row 1: Header (Icon + Title + Status)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (isCompleted) Icons.Outlined.CheckCircle else Icons.Outlined.FolderSpecial,
                        contentDescription = null,
                        tint = if (currentFluidLevel > 0.85f) onPrimary else primaryColor,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = planName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (currentFluidLevel > 0.85f) onPrimary else onSurface,
                        maxLines = 1,
                        modifier = Modifier.weight(1f)
                    )

                    if (isCompleted) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = primaryColor.copy(alpha = 0.20f)
                        ) {
                            Text(
                                text = "Completed",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (currentFluidLevel > 0.85f) onPrimary else primaryColor,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }
                }

                // Row 2: Subtitle Date Range
                Text(
                    text = dateRange,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (currentFluidLevel > 0.70f) onPrimary.copy(alpha = 0.85f) else onSurfaceVariant,
                    maxLines = 1
                )

                Spacer(modifier = Modifier.weight(1f))

                // Row 3: Bottom Day Count + Percentage Pill
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Day count pill
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (currentFluidLevel > 0.45f)
                            MaterialTheme.colorScheme.surface.copy(alpha = 0.85f)
                        else
                            MaterialTheme.colorScheme.surface.copy(alpha = 0.70f),
                        border = BorderStroke(
                            0.5.dp,
                            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.30f)
                        )
                    ) {
                        Text(
                            text = "Day $currentDay / $totalDays",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Medium,
                            color = onSurface,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }

                    // Percentage pill
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (currentFluidLevel > 0.45f)
                            MaterialTheme.colorScheme.surface.copy(alpha = 0.90f)
                        else
                            primaryColor.copy(alpha = 0.12f),
                        border = BorderStroke(
                            0.5.dp,
                            if (currentFluidLevel > 0.45f)
                                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
                            else
                                primaryColor.copy(alpha = 0.25f)
                        )
                    ) {
                        Text(
                            text = "$animatedPercent%",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = primaryColor,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Canvas Drawing Helpers
// ---------------------------------------------------------------------------

private fun DrawScope.drawFluidWave(
    canvasW: Float,
    canvasH: Float,
    levelY: Float,
    wavePhase: Float,
    amplitude: Float,
    color: Color,
    gradient: Brush? = null,
) {
    val clampedLevelY = levelY.coerceIn(0f, canvasH)
    val path = Path()
    path.moveTo(0f, canvasH)
    path.lineTo(0f, clampedLevelY + amplitude * sin(wavePhase))

    val steps = 64
    for (i in 1..steps) {
        val x = canvasW * i.toFloat() / steps
        val theta = (x / canvasW) * 2f * PI.toFloat() + wavePhase
        val y = (clampedLevelY + amplitude * sin(theta)).coerceAtMost(canvasH)
        path.lineTo(x, y)
    }

    path.lineTo(canvasW, canvasH)
    path.close()

    if (gradient != null) {
        drawPath(path = path, brush = gradient)
    } else {
        drawPath(path = path, color = color)
    }
}

private fun DrawScope.drawWaveCrestHighlight(
    canvasW: Float,
    canvasH: Float,
    levelY: Float,
    wavePhase: Float,
    amplitude: Float,
    highlightColor: Color,
) {
    val clampedLevelY = levelY.coerceIn(0f, canvasH)
    val path = Path()
    path.moveTo(0f, clampedLevelY + amplitude * sin(wavePhase))

    val steps = 64
    for (i in 1..steps) {
        val x = canvasW * i.toFloat() / steps
        val theta = (x / canvasW) * 2f * PI.toFloat() + wavePhase
        val y = (clampedLevelY + amplitude * sin(theta)).coerceAtMost(canvasH)
        path.lineTo(x, y)
    }

    drawPath(
        path = path,
        color = highlightColor,
        style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.5.dp.toPx())
    )
}