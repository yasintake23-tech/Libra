package com.libra.app.ui.navigation

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.sin

private val GlassWhite = Color(0xFFF9FCFF)
private val GlassMist = Color(0xFFDCEBFF)
private val GlassBlue = Color(0xFF6DADFF)
private val GlassDeepBlue = Color(0xFF3284EF)
private val GlassHighlight = Color(0xFFB9D7FF)

private fun glassBrush(selected: Boolean): Brush = Brush.linearGradient(
    colors = if (selected) {
        listOf(GlassWhite, GlassMist, GlassBlue, GlassDeepBlue)
    } else {
        listOf(
            GlassWhite.copy(alpha = 0.76f),
            GlassMist.copy(alpha = 0.70f),
            GlassBlue.copy(alpha = 0.64f)
        )
    },
    start = Offset(2f, 2f),
    end = Offset(22f, 22f)
)

@Composable
fun LibraAnimatedNavIcon(
    tab: BottomNavTab,
    selected: Boolean,
    tapToken: Int,
    backgroundColor: Color,
    modifier: Modifier = Modifier
) {
    var progress by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(tapToken) {
        if (tapToken == 0) return@LaunchedEffect
        val animation = Animatable(0f)
        animation.animateTo(
            1f,
            animationSpec = tween(760, easing = FastOutSlowInEasing)
        ) {
            progress = value
        }
    }

    Canvas(modifier = modifier.size(25.dp)) {
        when (tab) {
            BottomNavTab.HOME -> drawHome(progress, selected, backgroundColor)
            BottomNavTab.DISCOVER -> drawCompass(progress, selected)
            BottomNavTab.DM -> drawChat(progress, selected)
            BottomNavTab.LIBRARY -> drawBook(progress, selected)
            BottomNavTab.PROFILE -> drawProfile(progress, selected)
            BottomNavTab.WRITE -> drawPen(progress, selected)
        }
    }
}

private fun DrawScope.drawHome(progress: Float, selected: Boolean, backgroundColor: Color) {
    val brush = glassBrush(selected)
    val w = size.width
    val h = size.height

    drawRoundRect(
        brush = brush,
        topLeft = Offset(w * 0.23f, h * 0.47f),
        size = Size(w * 0.54f, h * 0.37f),
        cornerRadius = CornerRadius(w * 0.09f, w * 0.09f)
    )

    val roof = Path().apply {
        moveTo(w * 0.12f, h * 0.50f)
        lineTo(w * 0.50f, h * 0.17f)
        lineTo(w * 0.88f, h * 0.50f)
    }
    drawPath(
        path = roof,
        brush = Brush.linearGradient(
            colors = listOf(GlassWhite, GlassBlue, GlassDeepBlue),
            start = Offset(w * 0.18f, h * 0.48f),
            end = Offset(w * 0.83f, h * 0.32f)
        ),
        style = Stroke(width = w * 0.16f, cap = StrokeCap.Round, join = StrokeJoin.Round)
    )

    val chimneyX = w * 0.68f
    drawRoundRect(
        brush = brush,
        topLeft = Offset(chimneyX, h * 0.22f),
        size = Size(w * 0.17f, h * 0.25f),
        cornerRadius = CornerRadius(w * 0.035f, w * 0.035f)
    )

    repeat(3) { index ->
        val delay = index * 0.13f
        val local = ((progress - delay) / (1f - delay)).coerceIn(0f, 1f)
        val rise = sin(local * PI / 2f).toFloat()
        val alpha = if (progress <= delay) 0f else (0.66f - index * 0.16f) * (1f - local * 0.2f)
        drawCircle(
            color = GlassMist.copy(alpha = alpha.coerceIn(0f, 0.72f)),
            radius = w * (0.052f + index * 0.01f),
            center = Offset(
                chimneyX + w * (0.03f + index * 0.012f),
                h * (0.17f - index * 0.08f) - h * 0.07f * rise
            )
        )
    }

    drawRoundRect(
        color = backgroundColor,
        topLeft = Offset(w * 0.44f, h * 0.64f),
        size = Size(w * 0.12f, h * 0.20f),
        cornerRadius = CornerRadius(w * 0.05f, w * 0.05f)
    )
}

private fun DrawScope.drawChat(progress: Float, selected: Boolean) {
    val brush = glassBrush(selected)
    val w = size.width
    val h = size.height

    val bubble = Path().apply {
        moveTo(w * 0.18f, h * 0.18f)
        quadraticTo(w * 0.10f, h * 0.18f, w * 0.10f, h * 0.31f)
        lineTo(w * 0.10f, h * 0.60f)
        quadraticTo(w * 0.10f, h * 0.77f, w * 0.27f, h * 0.77f)
        lineTo(w * 0.24f, h * 0.91f)
        lineTo(w * 0.43f, h * 0.77f)
        lineTo(w * 0.76f, h * 0.77f)
        quadraticTo(w * 0.90f, h * 0.77f, w * 0.90f, h * 0.61f)
        lineTo(w * 0.90f, h * 0.31f)
        quadraticTo(w * 0.90f, h * 0.18f, w * 0.76f, h * 0.18f)
        close()
    }
    drawPath(bubble, brush)
    drawPath(
        bubble,
        color = GlassHighlight.copy(alpha = 0.82f),
        style = Stroke(width = w * 0.034f, join = StrokeJoin.Round)
    )

    repeat(3) { index ->
        val local = ((progress * 1.32f) - index * 0.16f).coerceIn(0f, 1f)
        val bounce = sin(local * PI).toFloat()
        drawCircle(
            color = GlassDeepBlue.copy(alpha = if (selected) 0.94f else 0.76f),
            radius = w * 0.072f * (1f + bounce * 0.08f),
            center = Offset(
                w * (0.34f + index * 0.16f),
                h * 0.48f - h * 0.075f * bounce
            )
        )
    }
}

private fun DrawScope.drawCompass(progress: Float, selected: Boolean) {
    val w = size.width
    val h = size.height
    val center = Offset(w / 2f, h / 2f)
    val fade = 1f - 0.88f * sin(progress * PI).toFloat()
    val needleScale = 0.94f + 0.06f * fade

    drawCircle(
        color = GlassMist.copy(alpha = if (selected) 0.92f else 0.64f),
        radius = w * 0.41f,
        style = Stroke(width = w * 0.13f)
    )
    drawCircle(
        color = GlassHighlight.copy(alpha = 0.76f),
        radius = w * 0.405f,
        style = Stroke(width = w * 0.025f)
    )

    withTransform({ scale(needleScale, needleScale, center) }) {
        val needle = Path().apply {
            moveTo(w * 0.62f, h * 0.25f)
            lineTo(w * 0.46f, h * 0.61f)
            lineTo(w * 0.26f, h * 0.75f)
            lineTo(w * 0.42f, h * 0.39f)
            close()
        }
        drawPath(
            needle,
            color = GlassBlue.copy(alpha = fade),
            style = Stroke(width = w * 0.055f, join = StrokeJoin.Round)
        )
        drawCircle(
            color = GlassDeepBlue.copy(alpha = fade),
            radius = w * 0.06f,
            center = center
        )
    }
}

private fun DrawScope.drawBook(progress: Float, selected: Boolean) {
    val brush = glassBrush(selected)
    val w = size.width
    val h = size.height
    val center = Offset(w / 2f, h * 0.53f)
    val close = sin(progress * PI).toFloat() * 7.5f

    drawRoundRect(
        color = GlassBlue.copy(alpha = 0.36f),
        topLeft = Offset(w * 0.08f, h * 0.19f),
        size = Size(w * 0.84f, h * 0.61f),
        cornerRadius = CornerRadius(w * 0.07f, w * 0.07f)
    )

    withTransform({ rotate(-close, center) }) {
        val leftPage = Path().apply {
            moveTo(w * 0.18f, h * 0.18f)
            quadraticTo(w * 0.31f, h * 0.15f, w * 0.50f, h * 0.28f)
            lineTo(w * 0.50f, h * 0.84f)
            quadraticTo(w * 0.31f, h * 0.67f, w * 0.18f, h * 0.73f)
            close()
        }
        drawPath(leftPage, brush)
    }

    withTransform({ rotate(close, center) }) {
        val rightPage = Path().apply {
            moveTo(w * 0.82f, h * 0.18f)
            quadraticTo(w * 0.69f, h * 0.15f, w * 0.50f, h * 0.28f)
            lineTo(w * 0.50f, h * 0.84f)
            quadraticTo(w * 0.69f, h * 0.67f, w * 0.82f, h * 0.73f)
            close()
        }
        drawPath(rightPage, brush)
    }

    drawLine(
        color = GlassWhite.copy(alpha = 0.94f),
        start = Offset(w * 0.50f, h * 0.22f),
        end = Offset(w * 0.50f, h * 0.84f),
        strokeWidth = w * 0.045f,
        cap = StrokeCap.Round
    )
}

private fun DrawScope.drawProfile(progress: Float, selected: Boolean) {
    val brush = glassBrush(selected)
    val w = size.width
    val h = size.height
    val centerX = w / 2f
    val bounce = sin(progress * PI).toFloat()
    val headY = h * 0.30f - h * 0.105f * bounce
    val headScale = 1f + 0.045f * bounce

    withTransform({ scale(headScale, headScale, Offset(centerX, headY)) }) {
        drawCircle(
            brush = brush,
            radius = w * 0.18f,
            center = Offset(centerX, headY)
        )
    }

    val shoulders = Path().apply {
        moveTo(w * 0.20f, h * 0.82f)
        quadraticTo(w * 0.22f, h * 0.58f, w * 0.50f, h * 0.58f)
        quadraticTo(w * 0.78f, h * 0.58f, w * 0.80f, h * 0.82f)
        quadraticTo(w * 0.80f, h * 0.88f, w * 0.70f, h * 0.88f)
        lineTo(w * 0.30f, h * 0.88f)
        quadraticTo(w * 0.20f, h * 0.88f, w * 0.20f, h * 0.82f)
        close()
    }
    drawPath(shoulders, brush)
    drawPath(
        shoulders,
        color = GlassHighlight.copy(alpha = 0.70f),
        style = Stroke(width = w * 0.025f, join = StrokeJoin.Round)
    )
}

private fun DrawScope.drawPen(progress: Float, selected: Boolean) {
    val brush = glassBrush(selected)
    val w = size.width
    val h = size.height
    val tilt = sin(progress * PI).toFloat() * 4f

    withTransform({ rotate(tilt, Offset(w / 2f, h / 2f)) }) {
        val nib = Path().apply {
            moveTo(w * 0.25f, h * 0.78f)
            lineTo(w * 0.36f, h * 0.45f)
            lineTo(w * 0.73f, h * 0.18f)
            lineTo(w * 0.63f, h * 0.55f)
            lineTo(w * 0.25f, h * 0.78f)
            close()
        }
        drawPath(nib, brush)
        drawCircle(
            color = GlassDeepBlue.copy(alpha = 0.84f),
            radius = w * 0.065f,
            center = Offset(w * 0.59f, h * 0.40f)
        )
    }
}
