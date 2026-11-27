package com.libra.app.ui.theme

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val LightColorScheme = lightColorScheme(
    primary = LibraAccent,
    onPrimary = Color.White,
    primaryContainer = LibraAccentSoft,
    onPrimaryContainer = LibraInk,
    secondary = LibraInkSoft,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFF4F4F5),
    onSecondaryContainer = LibraInk,
    background = LibraBackground,
    onBackground = LibraInk,
    surface = LibraSurface,
    onSurface = LibraInk,
    surfaceVariant = Color(0xFFF4F4F5),
    onSurfaceVariant = LibraMuted,
    outline = LibraLine,
    error = Color(0xFFBA1A1A),
    onError = Color.White
)

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFFE4E4E7),
    onPrimary = LibraInk,
    primaryContainer = Color(0xFF3F3F46),
    onPrimaryContainer = Color.White,
    secondary = Color(0xFFD4D4D8),
    onSecondary = LibraInk,
    secondaryContainer = Color(0xFF27272A),
    onSecondaryContainer = Color.White,
    background = LibraDarkBackground,
    onBackground = LibraDarkText,
    surface = LibraDarkSurface,
    onSurface = LibraDarkText,
    surfaceVariant = Color(0xFF222222),
    onSurfaceVariant = LibraDarkMuted,
    outline = LibraDarkLine,
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005)
)

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

@Composable
fun LibraTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colors = if (darkTheme) DarkColorScheme else LightColorScheme
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = view.context.findActivity()?.window ?: return@SideEffect
            val controller = WindowCompat.getInsetsController(window, view)
            controller.isAppearanceLightStatusBars = !darkTheme
            controller.isAppearanceLightNavigationBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colors,
        typography = Typography
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = colors.background,
            contentColor = colors.onBackground,
            content = content
        )
    }
}
