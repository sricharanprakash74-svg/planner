package com.example.plannerapp.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Renders a Reddit-style thread line connector that links a parent task to a subtask item.
 *
 * @param isLast Whether this subtask is the last item in the thread tree.
 * @param isCompleted Whether this subtask is completed (used for dynamic accent tinting).
 * @param width The horizontal slot width for the thread line and branch.
 * @param trunkX The horizontal offset where the vertical trunk line runs.
 * @param strokeWidth The thickness of the thread line.
 * @param cornerRadius The radius of the curve branching right into the subtask.
 * @param inactiveColor Color of the thread when subtask is pending.
 * @param activeColor Color of the thread branch when subtask is completed.
 */
@Composable
fun TaskThreadBranch(
    isLast: Boolean,
    isCompleted: Boolean,
    modifier: Modifier = Modifier,
    width: Dp = 28.dp,
    trunkX: Dp = 10.dp,
    strokeWidth: Dp = 2.dp,
    cornerRadius: Dp = 8.dp,
    inactiveColor: Color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.65f),
    activeColor: Color = Color(0xFF10B981).copy(alpha = 0.75f)
) {
    val animatedBranchColor by animateColorAsState(
        targetValue = if (isCompleted) activeColor else inactiveColor,
        animationSpec = tween(durationMillis = 250),
        label = "subtaskBranchColor"
    )

    Canvas(
        modifier = modifier
            .width(width)
            .fillMaxHeight()
    ) {
        val strokePx = strokeWidth.toPx()
        val radiusPx = cornerRadius.toPx().coerceAtMost(size.height / 2f)
        val stroke = Stroke(
            width = strokePx,
            cap = StrokeCap.Round,
            join = StrokeJoin.Round
        )

        val trunkXPx = trunkX.toPx()
        val branchCenterY = size.height / 2f
        val branchEndX = size.width

        // 1. Vertical trunk line from top (y = 0) down to branch curve
        drawLine(
            color = inactiveColor,
            start = Offset(trunkXPx, 0f),
            end = Offset(trunkXPx, (branchCenterY - radiusPx).coerceAtLeast(0f)),
            strokeWidth = strokePx,
            cap = StrokeCap.Round
        )

        // 2. Curved branch to the right (toward subtask checkbox / indicator)
        val branchPath = Path().apply {
            moveTo(trunkXPx, (branchCenterY - radiusPx).coerceAtLeast(0f))
            quadraticTo(trunkXPx, branchCenterY, trunkXPx + radiusPx, branchCenterY)
            lineTo(branchEndX, branchCenterY)
        }
        drawPath(
            path = branchPath,
            color = animatedBranchColor,
            style = stroke
        )

        // 3. Continuation vertical line going down to subsequent items (if not the last item)
        if (!isLast) {
            drawLine(
                color = inactiveColor,
                start = Offset(trunkXPx, branchCenterY),
                end = Offset(trunkXPx, size.height),
                strokeWidth = strokePx,
                cap = StrokeCap.Round
            )
        }
    }
}

/**
 * A continuous vertical thread guide line, matching Reddit's nested comment thread gutter.
 */
@Composable
fun TaskThreadVerticalLine(
    modifier: Modifier = Modifier,
    width: Dp = 2.dp,
    color: Color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f)
) {
    Canvas(
        modifier = modifier
            .width(width)
            .fillMaxHeight()
    ) {
        val strokePx = size.width
        drawLine(
            color = color,
            start = Offset(size.width / 2f, 0f),
            end = Offset(size.width / 2f, size.height),
            strokeWidth = strokePx,
            cap = StrokeCap.Round
        )
    }
}
