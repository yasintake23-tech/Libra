package com.libra.app.ui.animation

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.VectorConverter
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import kotlin.math.abs

/**
 * Apple-style elastic rubber-band pull and stretch modifier.
 * When the user drags/tugs the element, it follows with dampened elasticity
 * and stretches along the drag axis. On release, it snaps back with bouncy spring physics.
 */
fun Modifier.elasticStretch(
    maxStretchDp: Dp = 18.dp,
    enabled: Boolean = true,
    onStretchLimitReached: (() -> Unit)? = null
): Modifier = composed {
    if (!enabled) return@composed this

    val density = LocalDensity.current
    val maxStretchPx = with(density) { maxStretchDp.toPx() }
    val animatedOffset = remember { Animatable(Offset.Zero, Offset.VectorConverter) }
    val scope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current

    this
        .graphicsLayer {
            val ox = animatedOffset.value.x
            val oy = animatedOffset.value.y
            translationX = ox
            translationY = oy

            // Subtle dynamic elongation along the pull vector
            val stretchXFactor = (abs(ox) / (maxStretchPx.coerceAtLeast(1f))) * 0.08f
            val stretchYFactor = (abs(oy) / (maxStretchPx.coerceAtLeast(1f))) * 0.08f
            scaleX = 1f + stretchXFactor
            scaleY = 1f + stretchYFactor
        }
        .pointerInput(enabled, maxStretchPx) {
            detectDragGestures(
                onDragStart = {
                    runCatching {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    }
                },
                onDrag = { change, dragAmount ->
                    change.consume()
                    val current = animatedOffset.value + dragAmount
                    // Damped rubber-band formula: x / (1 + |x| / max)
                    val dampedX = current.x / (1f + (abs(current.x) / maxStretchPx))
                    val dampedY = current.y / (1f + (abs(current.y) / maxStretchPx))

                    scope.launch {
                        animatedOffset.snapTo(Offset(dampedX, dampedY))
                    }

                    if (abs(dampedX) >= maxStretchPx * 0.85f || abs(dampedY) >= maxStretchPx * 0.85f) {
                        onStretchLimitReached?.invoke()
                    }
                },
                onDragEnd = {
                    scope.launch {
                        animatedOffset.animateTo(
                            Offset.Zero,
                            animationSpec = AppleSprings.OffsetBouncy
                        )
                    }
                },
                onDragCancel = {
                    scope.launch {
                        animatedOffset.animateTo(
                            Offset.Zero,
                            animationSpec = AppleSprings.OffsetBouncy
                        )
                    }
                }
            )
        }
}
