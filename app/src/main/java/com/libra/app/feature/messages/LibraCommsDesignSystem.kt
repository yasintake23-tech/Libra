package com.libra.app.feature.messages

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.libra.app.core.di.ServiceLocator
import com.libra.app.ui.animation.appleBouncy
import com.libra.app.ui.theme.LibraLavender
import com.libra.app.ui.theme.LibraLavenderBorder
import com.libra.app.ui.theme.LibraLavenderDarkBorder
import com.libra.app.ui.theme.LibraLavenderDarkSurface
import com.libra.app.ui.theme.LibraLavenderDarkText
import com.libra.app.ui.theme.LibraLavenderSurface
import com.libra.app.ui.theme.LibraLavenderText
import com.libra.app.ui.theme.LocalLibraDarkTheme
import kotlinx.coroutines.delay
import kotlin.math.abs

private val EditorialPatternPalettes = listOf(
    Triple(Color(0xFF1E1B3A), Color(0xFF312E81), Color(0xFFA5B4FC)),
    Triple(Color(0xFF1C2634), Color(0xFF0F4C5C), Color(0xFF7DD3FC)),
    Triple(Color(0xFF2B1C34), Color(0xFF5B21B6), Color(0xFFD8B4FE)),
    Triple(Color(0xFF2A2118), Color(0xFF78350F), Color(0xFFFCD34D)),
    Triple(Color(0xFF172A26), Color(0xFF115E59), Color(0xFF5EEAD4)),
    Triple(Color(0xFF2E1B24), Color(0xFF881337), Color(0xFFFDA4AF))
)

private val SenderAccentLight = listOf(
    Color(0xFF6D28D9),
    Color(0xFF0369A1),
    Color(0xFF0F766E),
    Color(0xFFB45309),
    Color(0xFFBE123C),
    Color(0xFF4338CA)
)

private val SenderAccentDark = listOf(
    Color(0xFFC4B5FD),
    Color(0xFF7DD3FC),
    Color(0xFF5EEAD4),
    Color(0xFFFCD34D),
    Color(0xFFFDA4AF),
    Color(0xFFA5B4FC)
)

fun deterministicSenderAccent(senderId: String, darkTheme: Boolean): Color {
    val palette = if (darkTheme) SenderAccentDark else SenderAccentLight
    val idx = abs(senderId.hashCode()) % palette.size
    return palette[idx]
}

/**
 * Signed R2 / HTTP image composable with safe automatic URL signing + refresh and bespoke fallback.
 */
@Composable
fun LibraSignedImage(
    mediaKeyOrUrl: String?,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop,
    fallback: @Composable () -> Unit = {}
) {
    val raw = mediaKeyOrUrl.orEmpty().trim()
    if (raw.isBlank()) {
        Box(modifier = modifier) { fallback() }
        return
    }

    var resolvedUrl by remember(raw) { mutableStateOf<String?>(null) }
    var failed by remember(raw) { mutableStateOf(false) }

    LaunchedEffect(raw) {
        failed = false
        while (true) {
            val signed = runCatching {
                ServiceLocator.storageRepository.getSignedMediaUrl(raw)
            }.getOrNull()
            resolvedUrl = signed?.takeIf { it.isNotBlank() }
                ?: runCatching { ServiceLocator.storageRepository.getPublicCdnUrl(raw) }.getOrNull()
            if (resolvedUrl.isNullOrBlank()) {
                failed = true
                break
            }
            delay(9 * 60 * 1000L)
        }
    }

    Box(modifier = modifier) {
        if (!failed && !resolvedUrl.isNullOrBlank()) {
            AsyncImage(
                model = resolvedUrl,
                contentDescription = contentDescription,
                contentScale = contentScale,
                modifier = Modifier.fillMaxSize(),
                onError = { failed = true }
            )
        } else {
            fallback()
        }
    }
}

/**
 * Architectural Libra geometric cover banner used behind Group & Server hero headers
 * whenever no custom cover image is set (or while loading).
 */
@Composable
fun LibraArchitecturalBanner(
    seedKey: String,
    bannerMediaKey: String?,
    modifier: Modifier = Modifier
) {
    val idx = abs(seedKey.hashCode()) % EditorialPatternPalettes.size
    val (deep, mid, lineTint) = EditorialPatternPalettes[idx]

    LibraSignedImage(
        mediaKeyOrUrl = bannerMediaKey,
        contentDescription = "Kapak görseli",
        modifier = modifier,
        fallback = {
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Brush.linearGradient(listOf(deep, mid)))
            ) {
                val anchor = Offset(size.width * 0.84f, size.height * 0.25f)
                for (ring in 1..4) {
                    drawCircle(
                        color = lineTint.copy(alpha = (0.20f - ring * 0.03f).coerceAtLeast(0.05f)),
                        radius = size.minDimension * (0.26f * ring),
                        center = anchor,
                        style = Stroke(width = 1.2.dp.toPx())
                    )
                }
                drawLine(
                    color = lineTint.copy(alpha = 0.14f),
                    start = Offset(0f, size.height * 0.78f),
                    end = Offset(size.width, size.height * 0.34f),
                    strokeWidth = 1.dp.toPx()
                )
                drawCircle(
                    color = lineTint.copy(alpha = 0.28f),
                    radius = 4.dp.toPx(),
                    center = anchor
                )
            }
        }
    )
}

/**
 * Distinct squircle avatar used for Groups & Servers so users instantly distinguish
 * multi-person spaces (squircles) from 1-to-1 people (circles).
 */
@Composable
fun LibraSquircleAvatar(
    photoUrl: String?,
    title: String,
    size: Dp = 52.dp,
    cornerRadius: Dp = 16.dp,
    borderColor: Color? = null,
    borderWidth: Dp = 1.5.dp,
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val darkTheme = LocalLibraDarkTheme.current
    val shape = RoundedCornerShape(cornerRadius)
    val idx = abs(title.hashCode()) % EditorialPatternPalettes.size
    val (deep, mid, lineTint) = EditorialPatternPalettes[idx]

    val initials = remember(title) {
        val parts = title.trim().split(" ").filter { it.isNotBlank() }
        when {
            parts.size >= 2 -> "${parts[0].first()}${parts[1].first()}".uppercase()
            parts.size == 1 -> parts[0].take(2).uppercase()
            else -> "L"
        }
    }

    Surface(
        modifier = modifier
            .size(size)
            .clip(shape)
            .then(
                if (onClick != null) Modifier.appleBouncy(pressedScale = 0.92f, onClick = onClick)
                else Modifier
            ),
        shape = shape,
        color = if (darkTheme) deep else mid,
        border = borderColor?.let { BorderStroke(borderWidth, it) }
    ) {
        LibraSignedImage(
            mediaKeyOrUrl = photoUrl,
            contentDescription = title,
            modifier = Modifier.fillMaxSize(),
            fallback = {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Brush.linearGradient(listOf(deep, mid))),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = initials,
                        color = lineTint,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = (size.value * 0.34f).sp,
                        letterSpacing = 0.5.sp
                    )
                }
            }
        )
    }
}

@Composable
fun LibraRoleChip(
    label: String,
    highlighted: Boolean = false,
    accentColor: Color = LibraLavender
) {
    val darkTheme = LocalLibraDarkTheme.current
    Surface(
        shape = CircleShape,
        color = when {
            highlighted -> accentColor
            darkTheme -> LibraLavenderDarkSurface
            else -> LibraLavenderSurface
        },
        border = if (!highlighted) {
            BorderStroke(1.dp, if (darkTheme) LibraLavenderDarkBorder else LibraLavenderBorder)
        } else null
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 10.sp
            ),
            color = when {
                highlighted -> Color.White
                darkTheme -> LibraLavenderDarkText
                else -> LibraLavenderText
            },
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
        )
    }
}
