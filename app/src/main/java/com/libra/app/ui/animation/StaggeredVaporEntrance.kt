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
 * Ethereal Staggered Cascade & Vapor Mist Entrance Modifier ("Toz Buharı Gibi Süzülme").
 * Elements cascade down from above in smooth succession with soft alpha and spring settling,
 * preventing sudden harsh pop-ins and creating an organic, luxurious iOS experience.
 */
fun Modifier.staggeredVaporEntrance(
    index: Int = 0,
    visible: Boolean = true,
    initialOffsetY: Dp = (-16).dp,
    staggerDelayMs: Long = AppleSprings.VaporStaggerDelayMs
): Modifier = composed {
    val density = LocalDensity.current
    val initialOffsetPx = with(density) { initialOffsetY.toPx() }

    val alpha = remember { Animatable(if (visible) 0f else 1f) }
    val offsetY = remember { Animatable(if (visible) initialOffsetPx else 0f) }
    val scale = remember { Animatable(if (visible) 0.94f else 1f) }

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
                    durationMillis = 320,
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
            scale.animateTo(
                targetValue = 1f,
                animationSpec = AppleSprings.FloatBouncy
            )
        }
    }

    this.graphicsLayer {
        this.alpha = alpha.value
        this.translationY = offsetY.value
        this.scaleX = scale.value
        this.scaleY = scale.value
    }
}
