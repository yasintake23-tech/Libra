package com.libra.app.ui.animation

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.VectorConverter
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import kotlin.math.abs

/**
 * Universal Apple-style 3D Bouncy & Elastic Pull-Stretch Interaction Modifier.
 *
 * Implements the full physical tactile feel of Apple iOS 18 & VisionOS:
 * 1. Finger Touch-Down: Compresses inward ([pressedScale], default 0.92x) with snappy spring.
 * 2. Finger Drag / Pull ("Çekiştirme"): The button physically stretches and follows the finger
 *    with a dampened rubber-band formula [x / (1 + |x| / maxStretch)] and dynamic directional elongation!
 * 3. Finger Release: Snaps back to resting position with playful organic spring overshoot
 *    ([AppleSprings.OffsetBouncy] & [AppleSprings.FloatBouncy]), producing tactile haptic feedback.
 * 4. Activation Check: If released within the activation bounds, fires [onClick]. If pulled
 *    beyond the cancellation threshold, cancels the click cleanly.
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

            // Dynamic 3D elongation along the stretch vector
            val stretchXFactor = (abs(ox) / (maxStretchPx.coerceAtLeast(1f))) * 0.16f
            val stretchYFactor = (abs(oy) / (maxStretchPx.coerceAtLeast(1f))) * 0.16f
            scaleX = scale.value + stretchXFactor
            scaleY = scale.value + stretchYFactor
        }
        .pointerInput(enabled, pressedScale, maxStretchPx, onClick, onLongClick, onDoubleTap) {
            awaitEachGesture {
                val down = awaitFirstDown(requireUnconsumed = false)
                val downTime = System.currentTimeMillis()

                scope.launch {
                    scale.animateTo(pressedScale, animationSpec = AppleSprings.FloatSnappy)
                }
                runCatching {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                }

                var totalDrag = Offset.Zero
                var longPressTriggered = false
                val pointerId = down.id

                while (true) {
                    val event = awaitPointerEvent()
                    val change = event.changes.firstOrNull { it.id == pointerId } ?: break

                    if (!change.pressed) {
                        change.consume()
                        break
                    }

                    val dragDelta = change.positionChange()
                    if (dragDelta != Offset.Zero) {
                        totalDrag += dragDelta
                        change.consume()

                        // Apple rubber-band formula
                        val dampedX = totalDrag.x / (1f + (abs(totalDrag.x) / maxStretchPx))
                        val dampedY = totalDrag.y / (1f + (abs(totalDrag.y) / maxStretchPx))

                        scope.launch {
                            offset.snapTo(Offset(dampedX, dampedY))
                        }
                    }

                    // Long press detection while holding relatively still
                    if (onLongClick != null && !longPressTriggered &&
                        (System.currentTimeMillis() - downTime > 450L) &&
                        totalDrag.getDistance() < 14f
                    ) {
                        longPressTriggered = true
                        runCatching {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        }
                        onLongClick()
                    }
                }

                // Snap back with bouncy spring physics
                scope.launch {
                    offset.animateTo(Offset.Zero, animationSpec = AppleSprings.OffsetBouncy)
                }
                scope.launch {
                    scale.animateTo(1f, animationSpec = AppleSprings.FloatBouncy)
                }

                val dragDistance = totalDrag.getDistance()
                val isInsideClickBounds = dragDistance < (maxStretchPx * 1.35f)

                if (isInsideClickBounds && !longPressTriggered) {
                    runCatching {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    }
                    onClick()
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
