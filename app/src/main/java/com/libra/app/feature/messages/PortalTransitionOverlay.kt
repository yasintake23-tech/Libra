package com.libra.app.feature.messages

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.libra.app.ui.animation.appleBouncy
import com.libra.app.ui.theme.LibraLavender
import com.libra.app.ui.theme.LibraLavenderDarkText
import com.libra.app.ui.theme.LibraLavenderText
import com.libra.app.ui.theme.LocalLibraDarkTheme
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.sin

private val EngineObsidian = Color(0xFF10101A)
private val EngineIndigo = Color(0xFF4F46E5)
private val EngineAmber = Color(0xFFF59E0B)
private val EngineCyan = Color(0xFF38BDF8)
private val EngineLightCanvas = Color(0xFFF6F4FC)

/**
 * Full-screen kinetic mechanical-ignition overlay transitioning between Mesajlar and Sunucular.
 *
 * Pure GPU Canvas geometry (zero offscreen bitmaps, zero blur passes) tuned for 60/120fps:
 * - 0.00..0.28: Concentric machined gear/turbine ignites & accelerates at the button's exact coordinates.
 * - 0.28..0.64: Kinetic shockwave & rotating orbital rings expand across the full diagonal to repaint the screen.
 * - 0.56: Midpoint realm swap executes seamlessly underneath 100% coverage.
 * - 0.64..1.00: Aperture decelerates & dissolves cleanly, revealing the reconstructed destination screen.
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
    val haptic = LocalHapticFeedback.current
    val progress = remember(activeRequest.nonce) { Animatable(0f) }

    LaunchedEffect(activeRequest.nonce) {
        var swapped = false
        runCatching { haptic.performHapticFeedback(HapticFeedbackType.LongPress) }
        progress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 720, easing = LinearEasing)
        ) {
            if (!swapped && value >= 0.56f) {
                swapped = true
                onMidpointSwap(activeRequest.targetMode)
            }
        }
        if (!swapped) {
            onMidpointSwap(activeRequest.targetMode)
        }
        onCompleted()
    }

    val p = progress.value
    val toServers = activeRequest.targetMode == DmHubMode.SERVERS

    val veilColor = when {
        toServers && darkTheme -> EngineObsidian
        toServers -> Color(0xFF17162A)
        darkTheme -> Color(0xFF14131E)
        else -> EngineLightCanvas
    }
    val primaryRingColor = if (toServers) EngineIndigo else LibraLavender
    val sparkAccentColor = if (toServers) EngineAmber else EngineCyan
    val rimLightColor = if (toServers) Color(0xFFA5B4FC) else Color(0xFFC4B5FD)

    Canvas(modifier = modifier.fillMaxSize()) {
        val origin = if (activeRequest.originInRoot == Offset.Zero) {
            Offset(size.width * 0.72f, size.height * 0.22f)
        } else {
            Offset(
                x = activeRequest.originInRoot.x.coerceIn(0f, size.width),
                y = activeRequest.originInRoot.y.coerceIn(0f, size.height)
            )
        }

        val maxRadius = max(
            max(hypot(origin.x, origin.y), hypot(size.width - origin.x, origin.y)),
            max(hypot(origin.x, size.height - origin.y), hypot(size.width - origin.x, size.height - origin.y))
        ) * 1.16f

        val spinDir = if (toServers) 1f else -1f

        // Phase 1: Mechanical ignition spin-up (0.00 -> 0.30)
        val spinPhase = (p / 0.30f).coerceIn(0f, 1f)
        // Phase 2: Kinetic expansion wave painting screen (0.24 -> 0.62)
        val expandRaw = ((p - 0.24f) / 0.38f).coerceIn(0f, 1f)
        val expandEased = FastOutSlowInEasing.transform(expandRaw)
        // Phase 3: Reconstruction settle & clean aperture dissolve (0.62 -> 1.00)
        val settleRaw = ((p - 0.62f) / 0.38f).coerceIn(0f, 1f)
        val settleEased = FastOutSlowInEasing.transform(settleRaw)

        val masterAlpha = (1f - settleEased).coerceIn(0f, 1f)
        val spinAngle = spinDir * (p * p * 680f + p * 120f)

        // Expanding painted canvas veil
        if (expandRaw > 0f) {
            val currentRadius = (28.dp.toPx() + (maxRadius - 28.dp.toPx()) * expandEased)
            drawCircle(
                color = veilColor.copy(alpha = (0.96f * masterAlpha).coerceIn(0f, 1f)),
                radius = currentRadius,
                center = origin
            )

            // Interior geometric telemetry grid rings "constructing" the new realm
            val ringCount = 3
            for (i in 1..ringCount) {
                val ringRadius = currentRadius * (0.38f + i * 0.21f)
                if (ringRadius > 16f && ringRadius < maxRadius * 1.05f) {
                    val ringAlpha = (0.26f * masterAlpha * (1f - i * 0.14f)).coerceIn(0f, 0.35f)
                    drawMachinedArcRing(
                        center = origin,
                        radius = ringRadius,
                        segments = 6 + i * 2,
                        sweepRatio = 0.56f,
                        rotationDeg = spinAngle * (if (i % 2 == 0) -0.35f else 0.45f),
                        strokeWidth = (2.2f - i * 0.3f).dp.toPx(),
                        color = rimLightColor.copy(alpha = ringAlpha)
                    )
                }
            }

            // Crisp luminous paint-front shockwave ring
            val waveAlpha = ((1f - settleEased * 1.2f) * (0.92f - expandEased * 0.3f)).coerceIn(0f, 1f)
            if (waveAlpha > 0.02f) {
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color.Transparent,
                            primaryRingColor.copy(alpha = waveAlpha * 0.45f),
                            sparkAccentColor.copy(alpha = waveAlpha * 0.85f)
                        ),
                        center = origin,
                        radius = currentRadius
                    ),
                    radius = currentRadius,
                    center = origin,
                    style = Stroke(width = (10.dp.toPx() * (1f - expandEased * 0.55f)).coerceAtLeast(2.dp.toPx()))
                )

                // Orbital energy nodes riding along the wavefront
                val nodeCount = 10
                val nodeOrbitAngle = spinAngle * 0.55f
                for (n in 0 until nodeCount) {
                    val rad = ((nodeOrbitAngle + n * (360f / nodeCount)) * PI / 180.0)
                    val nx = origin.x + (cos(rad) * currentRadius).toFloat()
                    val ny = origin.y + (sin(rad) * currentRadius).toFloat()
                    drawCircle(
                        color = if (n % 2 == 0) sparkAccentColor.copy(alpha = waveAlpha)
                        else rimLightColor.copy(alpha = waveAlpha * 0.85f),
                        radius = (3.5.dp.toPx() * (1f - settleEased * 0.6f)).coerceAtLeast(1.2.dp.toPx()),
                        center = Offset(nx, ny)
                    )
                }
            }
        }

        // Phase 1 & early Phase 2: Mechanical Rotor Mechanism centered directly on the button
        val rotorVisibility = when {
            p < 0.28f -> (p / 0.10f).coerceIn(0f, 1f)
            p < 0.68f -> 1f - ((p - 0.28f) / 0.40f).coerceIn(0f, 1f)
            else -> 0f
        }
        if (rotorVisibility > 0.01f) {
            val baseRotorRadius = (24.dp.toPx() + 34.dp.toPx() * spinPhase + 46.dp.toPx() * expandEased)

            // Charged energy core glow
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        sparkAccentColor.copy(alpha = 0.85f * rotorVisibility),
                        primaryRingColor.copy(alpha = 0.55f * rotorVisibility),
                        Color.Transparent
                    ),
                    center = origin,
                    radius = baseRotorRadius * 1.15f
                ),
                radius = baseRotorRadius * 1.15f,
                center = origin
            )

            // Outer notched machined gear ring
            drawMachinedArcRing(
                center = origin,
                radius = baseRotorRadius,
                segments = 12,
                sweepRatio = 0.52f,
                rotationDeg = spinAngle,
                strokeWidth = 4.dp.toPx(),
                color = rimLightColor.copy(alpha = 0.95f * rotorVisibility)
            )

            // Counter-rotating inner precision ring
            drawMachinedArcRing(
                center = origin,
                radius = baseRotorRadius * 0.68f,
                segments = 6,
                sweepRatio = 0.60f,
                rotationDeg = -spinAngle * 1.35f,
                strokeWidth = 3.dp.toPx(),
                color = sparkAccentColor.copy(alpha = 0.92f * rotorVisibility)
            )

            // 8 accelerating turbine blades radiating from core
            rotate(degrees = spinAngle * 0.85f, pivot = origin) {
                val blades = 8
                for (b in 0 until blades) {
                    val angleRad = b * (2.0 * PI / blades)
                    val innerR = baseRotorRadius * 0.22f
                    val outerR = baseRotorRadius * 0.88f
                    drawLine(
                        color = primaryRingColor.copy(alpha = 0.9f * rotorVisibility),
                        start = Offset(
                            origin.x + (cos(angleRad) * innerR).toFloat(),
                            origin.y + (sin(angleRad) * innerR).toFloat()
                        ),
                        end = Offset(
                            origin.x + (cos(angleRad) * outerR).toFloat(),
                            origin.y + (sin(angleRad) * outerR).toFloat()
                        ),
                        strokeWidth = 2.4.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                }
            }

            // Precision ignition pivot hub
            drawCircle(
                color = Color.White.copy(alpha = 0.95f * rotorVisibility),
                radius = 5.dp.toPx() * (1f + spinPhase * 0.35f),
                center = origin
            )
        }
    }
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

/**
 * Tactile mechanical-ignition button used for "Sunuculara Geçiş Yap" and the reverse "Mesajlar" trigger.
 * Captures its exact center in root coordinates so PortalTransitionOverlay launches directly from it.
 */
@Composable
fun MechanicalPortalTriggerButton(
    title: String,
    subtitle: String? = null,
    isServerDestination: Boolean,
    badgeCount: Int = 0,
    compact: Boolean = false,
    onTrigger: (originInRoot: Offset) -> Unit,
    modifier: Modifier = Modifier
) {
    val darkTheme = LocalLibraDarkTheme.current
    var centerInRoot by remember { mutableStateOf(Offset.Zero) }

    val infiniteTransition = rememberInfiniteTransition(label = "portal_rotor_idle")
    val idleRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 14000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "portal_rotor_deg"
    )

    val shape = RoundedCornerShape(if (compact) 16.dp else 20.dp)
    val containerColor = when {
        isServerDestination && darkTheme -> Color(0xFF1D1B31)
        isServerDestination -> Color(0xFF181729)
        darkTheme -> Color(0xFF231D34)
        else -> Color(0xFFEDE9FE)
    }
    val borderColor = when {
        isServerDestination -> EngineIndigo.copy(alpha = 0.65f)
        darkTheme -> LibraLavender.copy(alpha = 0.55f)
        else -> LibraLavender.copy(alpha = 0.45f)
    }
    val primaryTextColor = when {
        isServerDestination -> Color.White
        darkTheme -> Color.White
        else -> Color(0xFF2E1065)
    }
    val secondaryTextColor = when {
        isServerDestination -> Color(0xFFC7D2FE)
        darkTheme -> LibraLavenderDarkText
        else -> LibraLavenderText
    }
    val rotorOuter = if (isServerDestination) EngineAmber else LibraLavender
    val rotorInner = if (isServerDestination) Color(0xFFA5B4FC) else EngineCyan

    Surface(
        modifier = modifier
            .onGloballyPositioned { coords ->
                centerInRoot = coords.boundsInRoot().center
            }
            .clip(shape)
            .appleBouncy(
                pressedScale = 0.94f,
                onClick = { onTrigger(centerInRoot) }
            ),
        shape = shape,
        color = containerColor,
        border = BorderStroke(1.2.dp, borderColor),
        shadowElevation = if (isServerDestination) 4.dp else 1.dp
    ) {
        Row(
            modifier = Modifier.padding(
                horizontal = if (compact) 12.dp else 14.dp,
                vertical = if (compact) 8.dp else 11.dp
            ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier.size(if (compact) 24.dp else 32.dp),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val c = Offset(size.width / 2f, size.height / 2f)
                    val r = size.minDimension * 0.44f
                    drawMachinedArcRing(
                        center = c,
                        radius = r,
                        segments = 6,
                        sweepRatio = 0.54f,
                        rotationDeg = if (isServerDestination) idleRotation else -idleRotation,
                        strokeWidth = 2.dp.toPx(),
                        color = rotorOuter
                    )
                    drawMachinedArcRing(
                        center = c,
                        radius = r * 0.58f,
                        segments = 3,
                        sweepRatio = 0.58f,
                        rotationDeg = if (isServerDestination) -idleRotation * 1.4f else idleRotation * 1.4f,
                        strokeWidth = 1.6.dp.toPx(),
                        color = rotorInner
                    )
                    drawCircle(
                        color = primaryTextColor,
                        radius = 2.4.dp.toPx(),
                        center = c
                    )
                }
            }

            Spacer(Modifier.width(if (compact) 8.dp else 10.dp))

            Column(
                modifier = Modifier.weight(1f, fill = !compact)
            ) {
                Text(
                    text = title,
                    style = if (compact) {
                        MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.ExtraBold)
                    } else {
                        MaterialTheme.typography.titleSmall.copy(
                            fontSize = 14.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = (-0.2).sp
                        )
                    },
                    color = primaryTextColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (!subtitle.isNullOrBlank() && !compact) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                        color = secondaryTextColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            if (badgeCount > 0) {
                Spacer(Modifier.width(6.dp))
                Surface(
                    shape = CircleShape,
                    color = Color(0xFFF43F5E)
                ) {
                    Text(
                        text = if (badgeCount > 99) "99+" else badgeCount.toString(),
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 10.sp
                        ),
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
        }
    }
}
