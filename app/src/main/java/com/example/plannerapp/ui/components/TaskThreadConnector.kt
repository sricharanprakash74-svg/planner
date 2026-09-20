package com.example.plannerapp.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Premium, hardware-accelerated curved node connector linking parent tasks to child subtasks.
 *
 * Employs a quadratic Bezier curve to sweep a smooth 90-degree quarter-circle from the vertical
 * trunk directly into the subtask terminal, eliminating harsh right angles.
 */
@Composable
fun SubtaskBranchConnector(
    modifier: Modifier = Modifier,
    isLastChild: Boolean = false,
    isCompleted: Boolean = false,
    isFocused: Boolean = false,
    strokeWidth: Dp = 1.5.dp,
    curveRadius: Dp = 10.dp,
    trunkX: Dp? = null,
    customColor: Color? = null
) {
    val targetColor = when {
        customColor != null -> customColor
        isFocused -> MaterialTheme.colorScheme.primary
        isCompleted -> MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.15f)
        else -> MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
    }

    val animatedStrokeColor by animateColorAsState(
        targetValue = targetColor,
        animationSpec = tween(durationMillis = 200),
        label = "branchStrokeColor"
    )

    Canvas(modifier = modifier) {
        val strokeWidthPx = strokeWidth.toPx()
        val radiusPx = curveRadius.toPx().coerceAtMost(size.height / 2f)
        val trunkPos = trunkX?.toPx() ?: (strokeWidthPx / 2f)
        val curveCenterY = size.height / 2f

        val path = Path().apply {
            // 1. Vertical trunk originating from top
            moveTo(x = trunkPos, y = 0f)

            // 2. Draw straight down to the curve origin
            lineTo(x = trunkPos, y = (curveCenterY - radiusPx).coerceAtLeast(0f))

            // 3. Sweep a smooth quarter-circle arc into the subtask
            quadraticTo(
                x1 = trunkPos,
                y1 = curveCenterY,
                x2 = (trunkPos + radiusPx).coerceAtMost(size.width),
                y2 = curveCenterY
            )

            // 4. Horizontal branch into the child node
            lineTo(x = size.width, y = curveCenterY)
        }

        drawPath(
            path = path,
            color = animatedStrokeColor,
            style = Stroke(
                width = strokeWidthPx,
                cap = StrokeCap.Round
            )
        )

        // If not the last child, the vertical trunk continues downward past this node
        if (!isLastChild) {
            drawLine(
                color = animatedStrokeColor,
                start = Offset(x = trunkPos, y = curveCenterY),
                end = Offset(x = trunkPos, y = size.height),
                strokeWidth = strokeWidthPx,
                cap = StrokeCap.Round
            )
        }
    }
}

/**
 * Backward-compatible wrapper delegating to the smooth curved [SubtaskBranchConnector].
 */
@Composable
fun TaskThreadBranch(
    isLast: Boolean,
    isCompleted: Boolean,
    modifier: Modifier = Modifier,
    isFocused: Boolean = false,
    width: Dp = 20.dp,
    strokeWidth: Dp = 1.5.dp,
    cornerRadius: Dp = 10.dp,
    trunkX: Dp? = null
) {
    SubtaskBranchConnector(
        modifier = modifier.width(width),
        isLastChild = isLast,
        isCompleted = isCompleted,
        isFocused = isFocused,
        strokeWidth = strokeWidth,
        curveRadius = cornerRadius,
        trunkX = trunkX
    )
}

/**
 * Interactive vertical rail for Reddit-style comment threads.
 * Provides a 32dp touch hit-box that collapses/expands the entire thread on tap.
 */
@Composable
fun InteractiveCommentRail(
    isCollapsed: Boolean,
    onToggleCollapse: () -> Unit,
    modifier: Modifier = Modifier,
    touchWidth: Dp = 32.dp,
    strokeColor: Color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)
) {
    val haptic = LocalHapticFeedback.current
    val primaryColor = MaterialTheme.colorScheme.primary
    val lineColor = if (isCollapsed) primaryColor else strokeColor

    Box(
        modifier = modifier
            .width(touchWidth)
            .fillMaxHeight()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onToggleCollapse()
                }
            ),
        contentAlignment = Alignment.Center
    ) {
        Canvas(
            modifier = Modifier
                .width(1.5.dp)
                .fillMaxHeight()
        ) {
            drawLine(
                color = lineColor,
                start = Offset(size.width / 2f, 0f),
                end = Offset(size.width / 2f, size.height),
                strokeWidth = size.width,
                cap = StrokeCap.Round
            )
        }
    }
}

/**
 * Continuous vertical thread guide line.
 */
@Composable
fun TaskThreadVerticalLine(
    modifier: Modifier = Modifier,
    width: Dp = 1.5.dp,
    color: Color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
) {
    Canvas(
        modifier = modifier
            .width(width)
            .fillMaxHeight()
    ) {
        drawLine(
            color = color,
            start = Offset(size.width / 2f, 0f),
            end = Offset(size.width / 2f, size.height),
            strokeWidth = size.width,
            cap = StrokeCap.Round
        )
    }
}
