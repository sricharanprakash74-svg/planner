package com.example.plannerapp.theme

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring

/**
 * Centralized physics-based spring animation specifications.
 *
 * Design philosophy:
 *   - No tween curves: every motion is governed by mass-spring physics.
 *   - Asymmetric press/release: pressing feels stiff (instant feedback),
 *     releasing feels organic (soft settle).
 *   - All values are Float to match animateFloatAsState / graphicsLayer.
 */
object PhysicsSpec {

    /**
     * Press-down compression.
     * Stiff, zero-bounce — feels like pressing a real mechanical key.
     * Use for the initial scale-down on touch.
     */
    val PressDown = spring<Float>(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness    = Spring.StiffnessMedium
    )

    /**
     * Press-release decompression.
     * Relaxed, low-bounce — soft micro-settle before navigation fires.
     * Use for scale returning to 1.0f after touch-up.
     */
    val PressRelease = spring<Float>(
        dampingRatio = Spring.DampingRatioLowBouncy,
        stiffness    = Spring.StiffnessMediumLow
    )

    /**
     * Sheet / modal dismiss and settle.
     * Critically damped (no bounce), relaxed deceleration.
     * Matches iOS sheet velocity-inheritance feel.
     */
    val SheetSettle = spring<Float>(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness    = Spring.StiffnessMediumLow
    )

    /**
     * Entrance float-up animation for list items.
     * Light medium bounce — items feel assembled in 3D space, not painted flat.
     */
    val EntranceRise = spring<Float>(
        dampingRatio = Spring.DampingRatioMediumBouncy,
        stiffness    = Spring.StiffnessMediumLow
    )

    /**
     * Generic predictive-back scale-down.
     * No bounce — screen should feel like a solid card receding.
     */
    val BackScale = spring<Float>(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness    = Spring.StiffnessMedium
    )
}
