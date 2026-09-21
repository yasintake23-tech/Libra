package com.libra.app.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = TerracottaLight,
    onPrimary = InkNavyDark,
    primaryContainer = TerracottaDark,
    onPrimaryContainer = Color.White,
    secondary = WarmAmber,
    onSecondary = Color.Black,
    tertiary = SageGreen,
    onTertiary = Color.White,
    background = MidnightDark,
    onBackground = SlateTextDark,
    surface = MidnightCardDark,
    onSurface = SlateTextDark,
    surfaceVariant = MidnightBorderDark,
    onSurfaceVariant = SlateMutedDark,
    outline = Color(0xFF3B485A)
)

private val LightColorScheme = lightColorScheme(
    primary = Terracotta,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFDBD1),
    onPrimaryContainer = Color(0xFF3D0600),
    secondary = InkNavy,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE2E8F0),
    onSecondaryContainer = InkNavyDark,
    tertiary = WarmAmber,
    onTertiary = Color.White,
    background = ParchmentLight,
    onBackground = SlateTextLight,
    surface = ParchmentCardLight,
    onSurface = SlateTextLight,
    surfaceVariant = ParchmentBorderLight,
    onSurfaceVariant = SlateMutedLight,
    outline = Color(0xFFCBD5E1)
)

@Composable
fun LibraTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
