package com.libra.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

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
    outline = LibraLine
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
    outline = LibraDarkLine
)

@Composable
fun LibraTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme,
        typography = Typography,
        content = content
    )
}
