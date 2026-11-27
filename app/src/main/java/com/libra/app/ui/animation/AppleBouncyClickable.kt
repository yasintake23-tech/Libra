package com.libra.app.ui.animation

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.VectorConverter
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs

/**
 * Scroll-safe Apple-style 3D Bouncy interaction modifier.
 *
 * Plays a crisp press scale on touch-down, immediately yields & springs back if the user
 * drags past [touchSlop] or a parent scrollable consumes the gesture (never blocking scroll
 * and never misfiring clicks during scroll), and cleanly triggers [onClick] / [onLongClick]
 * on intentional stationary taps or holds.
 */
fun Modifier.appleBouncy(
    enabled: Boolean = true,
    pressedScale: Float = 0.92f,
    maxStretchDp: Dp = 18.dp,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
    onDoubleTap: (() -> Unit)? = null
): Modifier = composed {
    if (!enabled) return@composed this

    val density = LocalDensity.current
    val maxStretchPx = with(density) { maxStretchDp.toPx() }
    val scope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current

    val scale = remember { Animatable(1f) }
    val offset = remember { Animatable(Offset.Zero, Offset.VectorConverter) }

    this
        .graphicsLayer {
            val ox = offset.value.x
            val oy = offset.value.y
            translationX = ox
            translationY = oy

            val stretchXFactor = (abs(ox) / (maxStretchPx.coerceAtLeast(1f))) * 0.12f
            val stretchYFactor = (abs(oy) / (maxStretchPx.coerceAtLeast(1f))) * 0.12f
            scaleX = scale.value + stretchXFactor
            scaleY = scale.value + stretchYFactor
        }
        .pointerInput(enabled, pressedScale, maxStretchPx, onClick, onLongClick, onDoubleTap) {
            awaitEachGesture {
                val down = awaitFirstDown(requireUnconsumed = false)
                val pointerId = down.id
                val slopPx = viewConfiguration.touchSlop
                val longPressTimeoutMs = viewConfiguration.longPressTimeoutMillis

                var totalDrag = Offset.Zero
                var dragCancelled = false
                var longPressTriggered = false
                var pointerActive = true

                scope.launch {
                    scale.animateTo(pressedScale, animationSpec = AppleSprings.FloatSnappy)
                }

                val longPressJob = if (onLongClick != null) {
                    scope.launch {
                        delay(longPressTimeoutMs)
                        if (pointerActive && !dragCancelled) {
                            longPressTriggered = true
                            scope.launch {
                                offset.animateTo(Offset.Zero, animationSpec = AppleSprings.OffsetBouncy)
                            }
                            scope.launch {
                                scale.animateTo(1f, animationSpec = AppleSprings.FloatBouncy)
                            }
                            runCatching {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            }
                            onLongClick()
                        }
                    }
                } else null

                try {
                    while (true) {
                        val event = awaitPointerEvent(PointerEventPass.Main)
                        val change = event.changes.firstOrNull { it.id == pointerId } ?: break

                        if (!change.pressed) {
                            pointerActive = false
                            longPressJob?.cancel()
                            if (!dragCancelled && !longPressTriggered && !change.isConsumed) {
                                change.consume()
                                runCatching {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                }
                                onClick()
                            }
                            break
                        }

                        // Intentionally NEVER consume move events here so parent LazyColumn,
                        // LazyRow, ModalBottomSheet, and scroll containers always scroll smoothly.
                        val dragDelta = change.positionChange()
                        if (dragDelta != Offset.Zero) {
                            totalDrag += dragDelta
                            val dragDistance = totalDrag.getDistance()
                            if (!dragCancelled && (change.isConsumed || dragDistance > slopPx)) {
                                dragCancelled = true
                                longPressJob?.cancel()
                                scope.launch {
                                    offset.animateTo(Offset.Zero, animationSpec = AppleSprings.OffsetBouncy)
                                }
                                scope.launch {
                                    scale.animateTo(1f, animationSpec = AppleSprings.FloatBouncy)
                                }
                            } else if (!dragCancelled && !longPressTriggered) {
                                val dampedX = (totalDrag.x * 0.45f) / (1f + (abs(totalDrag.x) / maxStretchPx))
                                val dampedY = (totalDrag.y * 0.45f) / (1f + (abs(totalDrag.y) / maxStretchPx))
                                scope.launch {
                                    offset.snapTo(Offset(dampedX, dampedY))
                                }
                            }
                        } else if (!dragCancelled && change.isConsumed) {
                            dragCancelled = true
                            longPressJob?.cancel()
                            scope.launch {
                                offset.animateTo(Offset.Zero, animationSpec = AppleSprings.OffsetBouncy)
                            }
                            scope.launch {
                                scale.animateTo(1f, animationSpec = AppleSprings.FloatBouncy)
                            }
                        }
                    }
                } catch (e: CancellationException) {
                    throw e
                } finally {
                    pointerActive = false
                    longPressJob?.cancel()
                    scope.launch {
                        offset.animateTo(Offset.Zero, animationSpec = AppleSprings.OffsetBouncy)
                    }
                    scope.launch {
                        scale.animateTo(1f, animationSpec = AppleSprings.FloatBouncy)
                    }
                }
            }
        }
}

/**
 * Dedicated 3D Apple Glass Button modifier with elevated light border and elastic physics.
 */
fun Modifier.apple3DButton(
    enabled: Boolean = true,
    shape: Shape = RoundedCornerShape(16.dp),
    pressedScale: Float = 0.90f,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null
): Modifier = composed {
    this
        .appleGlassSurface(cornerRadius = 16.dp, elevation = 4.dp)
        .appleBouncy(
            enabled = enabled,
            pressedScale = pressedScale,
            onClick = onClick,
            onLongClick = onLongClick
        )
}
