package com.libra.app.ui.animation

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import kotlinx.coroutines.launch

/**
 * Universal Apple-style bouncy interaction modifier.
 * On finger press: smoothly compresses to [pressedScale] (default 0.94x).
 * On release: springs back with elastic overshoot ([AppleSprings.FloatBouncy]) and triggers haptics.
 */
fun Modifier.appleBouncy(
    enabled: Boolean = true,
    pressedScale: Float = 0.94f,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
    onDoubleTap: (() -> Unit)? = null
): Modifier = composed {
    if (!enabled) return@composed this

    val scale = remember { Animatable(1f) }
    val scope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current

    this
        .graphicsLayer {
            scaleX = scale.value
            scaleY = scale.value
        }
        .pointerInput(enabled, pressedScale, onClick, onLongClick, onDoubleTap) {
            detectTapGestures(
                onPress = {
                    scope.launch {
                        scale.animateTo(
                            pressedScale,
                            animationSpec = AppleSprings.FloatSnappy
                        )
                    }
                    val released = try {
                        tryAwaitRelease()
                    } catch (e: Exception) {
                        false
                    }
                    scope.launch {
                        scale.animateTo(
                            1f,
                            animationSpec = AppleSprings.FloatBouncy
                        )
                    }
                },
                onTap = {
                    runCatching {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    }
                    onClick()
                },
                onLongPress = if (onLongClick != null) {
                    {
                        runCatching {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        }
                        onLongClick()
                    }
                } else null,
                onDoubleTap = if (onDoubleTap != null) {
                    {
                        runCatching {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        }
                        onDoubleTap()
                    }
                } else null
            )
        }
}
