package com.libra.app.ui.animation

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Sand Granule Cascade & Converging Mist Entrance Modifier ("Kum Taneleri Gibi Süzülüp Birleşen Efekt").
 *
 * Simulates fine particles of sand or dynamic liquid elements raining down from above:
 * 1. Vertical Drop: Falls smoothly from [initialOffsetY] (-24.dp).
 * 2. Lateral Sand Drift: Items alternate drifting in from left and right angles, converging
 *    seamlessly into the unified layout.
 * 3. Subtle Tilt Settling: Micro-rotates slightly along the entry vector and aligns firmly to 0°.
 * 4. Spring Lock: Settles with organic bounce ([AppleSprings.FloatBouncy]), creating a tactile,
 *    luxurious presentation without any heavy canvas particle overhead.
 */
fun Modifier.staggeredSandEntrance(
    index: Int = 0,
    visible: Boolean = true,
    initialOffsetY: Dp = (-24).dp,
    staggerDelayMs: Long = AppleSprings.VaporStaggerDelayMs
): Modifier = composed {
    val density = LocalDensity.current
    val initialOffsetPx = with(density) { initialOffsetY.toPx() }
    val driftDirection = if (index % 2 == 0) -1f else 1f
    val initialDriftPx = with(density) { (8.dp * driftDirection).toPx() }

    val alpha = remember { Animatable(if (visible) 0f else 1f) }
    val offsetY = remember { Animatable(if (visible) initialOffsetPx else 0f) }
    val driftX = remember { Animatable(if (visible) initialDriftPx else 0f) }
    val rotation = remember { Animatable(if (visible) driftDirection * 1.5f else 0f) }
    val scale = remember { Animatable(if (visible) 0.90f else 1f) }

    LaunchedEffect(visible, index) {
        if (!visible) return@LaunchedEffect

        val cappedIndex = index.coerceIn(0, 12)
        val delayTime = cappedIndex * staggerDelayMs
        if (delayTime > 0) {
            delay(delayTime)
        }

        launch {
            alpha.animateTo(
                targetValue = 1f,
                animationSpec = tween(
                    durationMillis = 300,
                    easing = AppleSprings.AppleFluidEasing
                )
            )
        }
        launch {
            offsetY.animateTo(
                targetValue = 0f,
                animationSpec = AppleSprings.FloatGentle
            )
        }
        launch {
            driftX.animateTo(
                targetValue = 0f,
                animationSpec = AppleSprings.FloatSnappy
            )
        }
        launch {
            rotation.animateTo(
                targetValue = 0f,
                animationSpec = AppleSprings.FloatGentle
            )
        }
        launch {
            scale.animateTo(
                targetValue = 1f,
                animationSpec = AppleSprings.FloatBouncy
            )
        }
    }

    this.graphicsLayer {
        this.alpha = alpha.value
        this.translationY = offsetY.value
        this.translationX = driftX.value
        this.rotationZ = rotation.value
        this.scaleX = scale.value
        this.scaleY = scale.value
    }
}

/**
 * Backward compatibility alias for [staggeredSandEntrance].
 */
fun Modifier.staggeredVaporEntrance(
    index: Int = 0,
    visible: Boolean = true,
    initialOffsetY: Dp = (-24).dp,
    staggerDelayMs: Long = AppleSprings.VaporStaggerDelayMs
): Modifier = staggeredSandEntrance(
    index = index,
    visible = visible,
    initialOffsetY = initialOffsetY,
    staggerDelayMs = staggerDelayMs
)
