package com.libra.app.feature.messages

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import com.libra.app.ui.theme.LocalLibraDarkTheme
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.sin

/**
 * Full-screen choreographed realm transition overlay for the 3-mode Libra Messaging Hub:
 * - Target = SERVERS: Mechanical motor ignition at the Sunucular button -> accelerating gears/blades
 *   breaking free of button bounds -> fluid layered color surface painting the screen -> smooth reveal.
 * - Target = GLOBAL_CHAT: Community convergence pulse rippling from Genel Sohbet -> luminous mineral-teal
 *   wave welcoming the user into the live room.
 * - Target = MESSAGES: Reverse precision deceleration sweep returning smoothly to the inbox.
 *
 * Non-blocking (zero pointer interception), rapid-tap safe (cancels & swaps cleanly on new nonce),
 * and honors Android's system reduced-motion setting.
 */
@Composable
fun PortalTransitionOverlay(
    request: PortalTransitionRequest?,
    onMidpointSwap: (DmHubMode) -> Unit,
    onCompleted: () -> Unit,
    modifier: Modifier = Modifier
) {
    val activeRequest = request ?: return
    val darkTheme = LocalLibraDarkTheme.current
    val palette = rememberLibraCommsColors()
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val reducedMotion = remember(context) { areSystemAnimationsReduced(context) }

    val progress = remember(activeRequest.nonce) { Animatable(0f) }

    LaunchedEffect(activeRequest.nonce) {
        var swapped = false
        val durationMs = when {
            reducedMotion -> 160
            activeRequest.targetMode == DmHubMode.SERVERS -> 800
            activeRequest.targetMode == DmHubMode.GLOBAL_CHAT -> 680
            else -> 640
        }
        val swapPoint = when {
            reducedMotion -> 0.45f
            activeRequest.targetMode == DmHubMode.SERVERS -> 0.54f
            activeRequest.targetMode == DmHubMode.GLOBAL_CHAT -> 0.48f
            else -> 0.50f
        }
        if (!reducedMotion) {
            runCatching { haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove) }
        }
        try {
            progress.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = durationMs, easing = LinearEasing)
            ) {
                if (!swapped && value >= swapPoint) {
                    swapped = true
                    onMidpointSwap(activeRequest.targetMode)
                }
            }
        } finally {
            if (!swapped) {
                onMidpointSwap(activeRequest.targetMode)
            }
        }
        onCompleted()
    }

    val p = progress.value

    if (reducedMotion) {
        val alpha = if (p < 0.5f) (p / 0.5f) * 0.45f else ((1f - p) / 0.5f) * 0.45f
        Canvas(modifier = modifier.fillMaxSize()) {
            drawRect(color = palette.canvas.copy(alpha = alpha.coerceIn(0f, 0.5f)))
        }
        return
    }

    Canvas(modifier = modifier.fillMaxSize()) {
        val defaultOrigin = when (activeRequest.targetMode) {
            DmHubMode.MESSAGES -> Offset(size.width * 0.18f, size.height * 0.12f)
            DmHubMode.GLOBAL_CHAT -> Offset(size.width * 0.50f, size.height * 0.12f)
            DmHubMode.SERVERS -> Offset(size.width * 0.82f, size.height * 0.12f)
        }
        val origin = if (activeRequest.originInRoot == Offset.Zero) {
            defaultOrigin
        } else {
            Offset(
                x = activeRequest.originInRoot.x.coerceIn(0f, size.width),
                y = activeRequest.originInRoot.y.coerceIn(0f, size.height)
            )
        }
        val maxRadius = max(
            max(hypot(origin.x, origin.y), hypot(size.width - origin.x, origin.y)),
            max(hypot(origin.x, size.height - origin.y), hypot(size.width - origin.x, size.height - origin.y))
        ) * 1.15f

        when (activeRequest.targetMode) {
            DmHubMode.SERVERS -> drawServerMotorTransition(
                p = p,
                origin = origin,
                maxRadius = maxRadius,
                darkTheme = darkTheme,
                palette = palette
            )
            DmHubMode.GLOBAL_CHAT -> drawGlobalCommunityTransition(
                p = p,
                origin = origin,
                maxRadius = maxRadius,
                darkTheme = darkTheme,
                palette = palette
            )
            DmHubMode.MESSAGES -> drawMessagesReturnTransition(
                p = p,
                origin = origin,
                maxRadius = maxRadius,
                darkTheme = darkTheme,
                palette = palette
            )
        }
    }
}

private fun DrawScope.drawServerMotorTransition(
    p: Float,
    origin: Offset,
    maxRadius: Float,
    darkTheme: Boolean,
    palette: LibraCommsColors
) {
    // 0.00..0.28: Motor spin-up inside/around Sunucular button
    // 0.22..0.60: Fluid surface color wave expands across screen, repainting UI
    // 0.60..1.00: Aperture decelerates & dissolves cleanly into Server UI
    val spinPhase = (p / 0.28f).coerceIn(0f, 1f)
    val expandRaw = ((p - 0.22f) / 0.38f).coerceIn(0f, 1f)
    val expandEased = FastOutSlowInEasing.transform(expandRaw)
    val settleRaw = ((p - 0.60f) / 0.40f).coerceIn(0f, 1f)
    val settleEased = FastOutSlowInEasing.transform(settleRaw)
    val masterAlpha = (1f - settleEased).coerceIn(0f, 1f)

    val spinAngle = p * p * 740f + p * 110f
    val primaryWash = if (darkTheme) Color(0xFF101424) else Color(0xFF1B2748)
    val midWash = if (darkTheme) Color(0xFF1F2B52) else palette.brand
    val sparkColor = Color(0xFFF59E0B)
    val rimLight = Color(0xFF93C5FD)

    // Expanding fluid layered color surface
    if (expandRaw > 0f) {
        val waveRadius = 22.dp.toPx() + (maxRadius - 22.dp.toPx()) * expandEased
        val trailingRadius = (waveRadius * 0.86f).coerceAtLeast(1f)

        // Outer kinetic color edge
        drawCircle(
            color = midWash.copy(alpha = (0.94f * masterAlpha).coerceIn(0f, 1f)),
            radius = waveRadius,
            center = origin
        )
        // Core painted deep surface following behind the wavefront
        drawCircle(
            color = primaryWash.copy(alpha = (0.97f * masterAlpha).coerceIn(0f, 1f)),
            radius = trailingRadius,
            center = origin
        )

        // Precision concentric mechanical telemetry arcs riding inside the wash
        for (i in 1..3) {
            val r = trailingRadius * (0.34f + i * 0.22f)
            if (r > 18f) {
                drawMachinedArcRing(
                    center = origin,
                    radius = r,
                    segments = 6 + i * 2,
                    sweepRatio = 0.54f,
                    rotationDeg = spinAngle * (if (i % 2 == 0) -0.32f else 0.42f),
                    strokeWidth = (2.1f - i * 0.35f).dp.toPx(),
                    color = rimLight.copy(alpha = (0.28f * masterAlpha * (1f - i * 0.16f)).coerceIn(0f, 0.3f))
                )
            }
        }

        // Crisp luminous paint-front border
        val edgeAlpha = ((1f - settleEased * 1.25f) * 0.9f).coerceIn(0f, 1f)
        if (edgeAlpha > 0.02f) {
            drawCircle(
                color = sparkColor.copy(alpha = edgeAlpha * 0.65f),
                radius = waveRadius,
                center = origin,
                style = Stroke(width = (5.dp.toPx() * (1f - expandEased * 0.55f)).coerceAtLeast(1.5.dp.toPx()))
            )
        }
    }

    // Mechanical Motor Rotor at button origin (detaches, accelerates, expands)
    val rotorAlpha = when {
        p < 0.28f -> (p / 0.08f).coerceIn(0f, 1f)
        p < 0.66f -> 1f - ((p - 0.28f) / 0.38f).coerceIn(0f, 1f)
        else -> 0f
    }
    if (rotorAlpha > 0.01f) {
        val rotorR = 18.dp.toPx() + 28.dp.toPx() * spinPhase + 40.dp.toPx() * expandEased
        // Machined gear teeth ring
        drawMachinedArcRing(
            center = origin,
            radius = rotorR,
            segments = 10,
            sweepRatio = 0.50f,
            rotationDeg = spinAngle,
            strokeWidth = 3.5.dp.toPx(),
            color = rimLight.copy(alpha = 0.95f * rotorAlpha)
        )
        // Counter-rotating inner rotor
        drawMachinedArcRing(
            center = origin,
            radius = rotorR * 0.64f,
            segments = 6,
            sweepRatio = 0.58f,
            rotationDeg = -spinAngle * 1.45f,
            strokeWidth = 2.6.dp.toPx(),
            color = sparkColor.copy(alpha = 0.92f * rotorAlpha)
        )
        // 6 rotating mechanical spokes
        rotate(degrees = spinAngle * 0.9f, pivot = origin) {
            for (b in 0 until 6) {
                val rad = b * (2.0 * PI / 6)
                val rIn = rotorR * 0.20f
                val rOut = rotorR * 0.86f
                drawLine(
                    color = Color.White.copy(alpha = 0.88f * rotorAlpha),
                    start = Offset(origin.x + (cos(rad) * rIn).toFloat(), origin.y + (sin(rad) * rIn).toFloat()),
                    end = Offset(origin.x + (cos(rad) * rOut).toFloat(), origin.y + (sin(rad) * rOut).toFloat()),
                    strokeWidth = 2.dp.toPx(),
                    cap = StrokeCap.Round
                )
            }
        }
        drawCircle(
            color = Color.White.copy(alpha = rotorAlpha),
            radius = 4.dp.toPx(),
            center = origin
        )
    }
}

private fun DrawScope.drawGlobalCommunityTransition(
    p: Float,
    origin: Offset,
    maxRadius: Float,
    darkTheme: Boolean,
    palette: LibraCommsColors
) {
    // Community convergence pulse: expanding mineral-teal & cobalt resonance rings + clean curtain reveal
    val expandRaw = (p / 0.52f).coerceIn(0f, 1f)
    val expandEased = FastOutSlowInEasing.transform(expandRaw)
    val fadeRaw = ((p - 0.50f) / 0.50f).coerceIn(0f, 1f)
    val fadeEased = FastOutSlowInEasing.transform(fadeRaw)
    val masterAlpha = (1f - fadeEased).coerceIn(0f, 1f)

    val washColor = if (darkTheme) Color(0xFF0E2124) else Color(0xFFEAF7F5)
    val ringColor = palette.community
    val secondRing = palette.brand

    val mainRadius = 20.dp.toPx() + (maxRadius - 20.dp.toPx()) * expandEased
    drawCircle(
        color = washColor.copy(alpha = (0.94f * masterAlpha).coerceIn(0f, 1f)),
        radius = mainRadius,
        center = origin
    )

    // 3 staggered concentric community resonance ripples
    for (ring in 0..2) {
        val delayed = ((p - ring * 0.08f) / 0.65f).coerceIn(0f, 1f)
        if (delayed > 0f && delayed < 1f) {
            val r = 18.dp.toPx() + maxRadius * 0.78f * FastOutSlowInEasing.transform(delayed)
            val a = ((1f - delayed) * 0.65f * masterAlpha).coerceIn(0f, 0.7f)
            drawCircle(
                color = (if (ring % 2 == 0) ringColor else secondRing).copy(alpha = a),
                radius = r,
                center = origin,
                style = Stroke(width = (3.dp.toPx() * (1f - delayed * 0.5f)).coerceAtLeast(1.2.dp.toPx()))
            )
        }
    }

    // Subtle rising bottom-to-top chat room baseline line during reveal
    if (p in 0.35f..0.92f) {
        val lineProgress = FastOutSlowInEasing.transform(((p - 0.35f) / 0.57f).coerceIn(0f, 1f))
        val y = size.height * (1f - 0.12f * lineProgress)
        val lineAlpha = (sin(lineProgress * PI).toFloat() * 0.45f).coerceIn(0f, 0.45f)
        drawRoundRect(
            color = ringColor.copy(alpha = lineAlpha),
            topLeft = Offset(size.width * 0.14f, y),
            size = Size(size.width * 0.72f, 3.dp.toPx()),
            cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
        )
    }
}

private fun DrawScope.drawMessagesReturnTransition(
    p: Float,
    origin: Offset,
    maxRadius: Float,
    darkTheme: Boolean,
    palette: LibraCommsColors
) {
    // Precision counter-clockwise deceleration sweep back to Mesajlar
    val expandRaw = (p / 0.52f).coerceIn(0f, 1f)
    val expandEased = FastOutSlowInEasing.transform(expandRaw)
    val settleRaw = ((p - 0.52f) / 0.48f).coerceIn(0f, 1f)
    val settleEased = FastOutSlowInEasing.transform(settleRaw)
    val masterAlpha = (1f - settleEased).coerceIn(0f, 1f)

    val veil = if (darkTheme) Color(0xFF10131C) else Color(0xFFF3F6FC)
    val currentRadius = 20.dp.toPx() + (maxRadius - 20.dp.toPx()) * expandEased

    drawCircle(
        color = veil.copy(alpha = (0.94f * masterAlpha).coerceIn(0f, 1f)),
        radius = currentRadius,
        center = origin
    )

    // Crisp cobalt reverse-spin deceleration arc
    val revAngle = -(1f - (1f - p) * (1f - p)) * 360f
    drawMachinedArcRing(
        center = origin,
        radius = (currentRadius * 0.55f).coerceAtLeast(24.dp.toPx()),
        segments = 8,
        sweepRatio = 0.48f,
        rotationDeg = revAngle,
        strokeWidth = 2.4.dp.toPx(),
        color = palette.brand.copy(alpha = (0.48f * masterAlpha).coerceIn(0f, 0.5f))
    )
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(Color.Transparent, palette.brand.copy(alpha = 0.32f * masterAlpha)),
            center = origin,
            radius = currentRadius.coerceAtLeast(1f)
        ),
        radius = currentRadius,
        center = origin,
        style = Stroke(width = 3.dp.toPx())
    )
}

private fun DrawScope.drawMachinedArcRing(
    center: Offset,
    radius: Float,
    segments: Int,
    sweepRatio: Float,
    rotationDeg: Float,
    strokeWidth: Float,
    color: Color
) {
    if (radius <= 0f || segments <= 0) return
    val step = 360f / segments
    val sweep = step * sweepRatio
    val topLeft = Offset(center.x - radius, center.y - radius)
    val arcSize = Size(radius * 2f, radius * 2f)
    for (i in 0 until segments) {
        drawArc(
            color = color,
            startAngle = rotationDeg + i * step,
            sweepAngle = sweep,
            useCenter = false,
            topLeft = topLeft,
            size = arcSize,
            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
        )
    }
}
