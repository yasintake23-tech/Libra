package com.libra.app.ui.animation

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Apple iOS 18 & VisionOS Glass & Squircle styling modifier.
 * Applies continuous squircle rounding, subtle edge light refraction border,
 * and high-contrast ambient glass surface.
 */
fun Modifier.appleGlassSurface(
    cornerRadius: Dp = 16.dp,
    borderWidth: Dp = 0.6.dp,
    elevation: Dp = 2.dp,
    containerColor: Color? = null
): Modifier = composed {
    val shape = RoundedCornerShape(cornerRadius)
    val background = containerColor ?: MaterialTheme.colorScheme.surface.copy(alpha = 0.88f)

    val borderBrush = Brush.verticalGradient(
        colors = listOf(
            Color.White.copy(alpha = 0.28f),
            Color.White.copy(alpha = 0.08f)
        )
    )

    this
        .shadow(elevation, shape, clip = false)
        .clip(shape)
        .background(background)
        .border(
            border = BorderStroke(borderWidth, borderBrush),
            shape = shape
        )
}

/**
 * Clean squircle clipping helper.
 */
fun Modifier.appleSquircle(cornerRadius: Dp = 14.dp): Modifier =
    this.clip(RoundedCornerShape(cornerRadius))
