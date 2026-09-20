package com.example.plannerapp.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import com.example.plannerapp.theme.PhysicsSpec
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val STAGGER_DELAY_MS  = 18L   // per-item stagger interval
private const val MAX_STAGGER_INDEX = 5     // items beyond index 5 appear instantly
private const val ENTRANCE_TRAVEL   = 24f   // start Y offset in dp units

/**
 * Wraps [content] in a staggered float-up + fade-in entrance animation.
 *
 * Each item floats upward 24dp while fading from 0f to 1f using [PhysicsSpec.EntranceRise].
 * Items with [index] > [MAX_STAGGER_INDEX] bypass the delay so long lists feel instant.
 *
 * Usage inside a LazyColumn:
 * ```kotlin
 * itemsIndexed(posts) { index, post ->
 *     StaggeredEntrance(index = index) {
 *         SocialFeedPostCard(post = post, ...)
 *     }
 * }
 * ```
 */
@Composable
fun StaggeredEntrance(
    index: Int,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val alpha: Animatable<Float, AnimationVector1D> = remember { Animatable(0f) }
    val translationY: Animatable<Float, AnimationVector1D> = remember { Animatable(ENTRANCE_TRAVEL) }

    LaunchedEffect(Unit) {
        val delayMs = if (index <= MAX_STAGGER_INDEX) index * STAGGER_DELAY_MS else 0L
        if (delayMs > 0) delay(delayMs)

        // Launch alpha and translationY in parallel within the LaunchedEffect coroutine scope
        coroutineScope {
            val alphaJob = launch {
                alpha.animateTo(1f, animationSpec = PhysicsSpec.EntranceRise)
            }
            val yJob = launch {
                translationY.animateTo(0f, animationSpec = PhysicsSpec.EntranceRise)
            }
            alphaJob.join()
            yJob.join()
        }
    }

    Box(
        modifier = modifier.graphicsLayer {
            this.alpha        = alpha.value
            this.translationY = translationY.value.dp.toPx()
        }
    ) {
        content()
    }
}
