package com.libra.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.libra.app.ui.animation.appleBouncy

enum class AppButtonVariant { PRIMARY, SECONDARY, OUTLINED, TEXT }

/**
 * Apple iOS 18 & VisionOS inspired 3D Tactile Spring Button.
 * Features continuous squircle geometry, elevated subtle light refraction border,
 * and responsive spring compression & directional rubber-band pull physics.
 */
@Composable
fun AppButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    variant: AppButtonVariant = AppButtonVariant.PRIMARY,
    icon: ImageVector? = null,
    isLoading: Boolean = false,
    enabled: Boolean = true,
    testTag: String = "app_button"
) {
    val shape = RoundedCornerShape(18.dp)
    val buttonModifier = modifier
        .height(50.dp)
        .testTag(testTag)
        .appleBouncy(
            enabled = enabled && !isLoading,
            pressedScale = 0.94f,
            onClick = onClick
        )

    val primaryBorder = BorderStroke(
        0.8.dp,
        Brush.verticalGradient(
            colors = listOf(
                Color.White.copy(alpha = 0.32f),
                Color.White.copy(alpha = 0.08f)
            )
        )
    )

    val secondaryBorder = BorderStroke(
        0.8.dp,
        Brush.verticalGradient(
            colors = listOf(
                Color.White.copy(alpha = 0.22f),
                Color.White.copy(alpha = 0.05f)
            )
        )
    )

    when (variant) {
        AppButtonVariant.PRIMARY -> Button(
            onClick = onClick,
            enabled = enabled && !isLoading,
            shape = shape,
            border = primaryBorder,
            elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp, pressedElevation = 0.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ),
            modifier = buttonModifier
        ) {
            ButtonContent(text, icon, isLoading, MaterialTheme.colorScheme.onPrimary)
        }
        AppButtonVariant.SECONDARY -> Button(
            onClick = onClick,
            enabled = enabled && !isLoading,
            shape = shape,
            border = secondaryBorder,
            elevation = ButtonDefaults.buttonElevation(defaultElevation = 1.dp, pressedElevation = 0.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.secondary,
                contentColor = MaterialTheme.colorScheme.onSecondary
            ),
            modifier = buttonModifier
        ) {
            ButtonContent(text, icon, isLoading, MaterialTheme.colorScheme.onSecondary)
        }
        AppButtonVariant.OUTLINED -> OutlinedButton(
            onClick = onClick,
            enabled = enabled && !isLoading,
            shape = shape,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)),
            modifier = buttonModifier
        ) {
            ButtonContent(text, icon, isLoading, MaterialTheme.colorScheme.primary)
        }
        AppButtonVariant.TEXT -> TextButton(
            onClick = onClick,
            enabled = enabled && !isLoading,
            shape = shape,
            modifier = buttonModifier
        ) {
            ButtonContent(text, icon, isLoading, MaterialTheme.colorScheme.primary)
        }
    }
}

@Composable
private fun ButtonContent(text: String, icon: ImageVector?, isLoading: Boolean, contentColor: Color) {
    if (isLoading) {
        CircularProgressIndicator(modifier = Modifier.size(20.dp), color = contentColor, strokeWidth = 2.dp)
    } else {
        Row(verticalAlignment = Alignment.CenterVertically) {
            icon?.let {
                Icon(imageVector = it, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text(text, style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold))
        }
    }
}

/**
 * Universal Apple Glass & Tactile Spring Icon Button with squircle or circular frame.
 */
@Composable
fun AppleIconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: Shape = CircleShape,
    containerColor: Color = Color.Transparent,
    contentColor: Color = MaterialTheme.colorScheme.onSurface,
    borderColor: Color? = null,
    borderWidth: Dp = 0.6.dp,
    size: Dp = 42.dp,
    pressedScale: Float = 0.86f,
    onLongClick: (() -> Unit)? = null,
    testTag: String? = null,
    content: @Composable () -> Unit
) {
    val tagModifier = if (testTag != null) Modifier.testTag(testTag) else Modifier
    val borderModifier = if (borderColor != null) Modifier.border(BorderStroke(borderWidth, borderColor), shape) else Modifier

    Box(
        modifier = modifier
            .then(tagModifier)
            .size(size)
            .clip(shape)
            .background(containerColor, shape)
            .then(borderModifier)
            .appleBouncy(
                enabled = enabled,
                pressedScale = pressedScale,
                onClick = onClick,
                onLongClick = onLongClick
            ),
        contentAlignment = Alignment.Center
    ) {
        CompositionLocalProvider(LocalContentColor provides contentColor) {
            content()
        }
    }
}

/**
 * ImageVector overload for AppleIconButton.
 */
@Composable
fun AppleIconButton(
    icon: ImageVector,
    onClick: () -> Unit,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: Shape = CircleShape,
    containerColor: Color = Color.Transparent,
    tint: Color = MaterialTheme.colorScheme.onSurface,
    borderColor: Color? = null,
    size: Dp = 42.dp,
    iconSize: Dp = 22.dp,
    pressedScale: Float = 0.86f,
    onLongClick: (() -> Unit)? = null,
    testTag: String? = null
) {
    AppleIconButton(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        shape = shape,
        containerColor = containerColor,
        contentColor = tint,
        borderColor = borderColor,
        size = size,
        pressedScale = pressedScale,
        onLongClick = onLongClick,
        testTag = testTag
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            modifier = Modifier.size(iconSize),
            tint = tint
        )
    }
}

/**
 * Reusable Apple iOS 18 Back Button with spring bounce and haptic response.
 */
@Composable
fun AppleBackButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tint: Color = MaterialTheme.colorScheme.onSurface,
    testTag: String = "back_button"
) {
    AppleIconButton(
        icon = Icons.AutoMirrored.Filled.ArrowBack,
        onClick = onClick,
        contentDescription = "Geri",
        modifier = modifier,
        tint = tint,
        testTag = testTag
    )
}
