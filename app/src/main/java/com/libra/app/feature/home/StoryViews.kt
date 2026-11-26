package com.libra.app.feature.home

import android.app.Activity
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Reply
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.libra.app.core.di.ServiceLocator
import com.libra.app.core.result.AppResult
import com.libra.app.domain.model.Story
import com.libra.app.ui.animation.appleBouncy
import com.libra.app.ui.components.UserAvatar
import kotlin.math.abs
import kotlin.math.min
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

internal data class StoryVisualPalette(
    val topSliceLuminance: Float = 0.15f,
    val bottomSliceLuminance: Float = 0.15f,
    val imageWidthPx: Int = 0,
    val imageHeightPx: Int = 0,
    val ambientTopColorArgb: Int = 0xFF151622.toInt(),
    val ambientBottomColorArgb: Int = 0xFF0D0E15.toInt()
)

private fun argbRed(argb: Int): Int = (argb ushr 16) and 0xFF
private fun argbGreen(argb: Int): Int = (argb ushr 8) and 0xFF
private fun argbBlue(argb: Int): Int = argb and 0xFF
private fun packRgb(r: Int, g: Int, b: Int): Int =
    (0xFF shl 24) or ((r and 0xFF) shl 16) or ((g and 0xFF) shl 8) or (b and 0xFF)

internal fun computeRelativeLuminance(argb: Int): Float {
    val r = argbRed(argb) / 255f
    val g = argbGreen(argb) / 255f
    val b = argbBlue(argb) / 255f
    return (0.2126f * r + 0.7152f * g + 0.0722f * b).coerceIn(0f, 1f)
}

internal fun darkenToAmbientArgb(argb: Int, scale: Float = 0.24f): Int {
    val r = (argbRed(argb) * scale).toInt().coerceIn(10, 56)
    val g = (argbGreen(argb) * scale).toInt().coerceIn(10, 56)
    val b = (argbBlue(argb) * scale).toInt().coerceIn(14, 68)
    return packRgb(r, g, b)
}

internal fun computeStoryHeaderIsLight(
    topSliceLuminance: Float,
    cardWidthPx: Int,
    cardHeightPx: Int,
    imageWidthPx: Int,
    imageHeightPx: Int,
    headerHeightPx: Float,
    lightThreshold: Float = 0.56f
): Boolean {
    if (topSliceLuminance < lightThreshold) return false
    if (imageWidthPx <= 0 || imageHeightPx <= 0) return false
    if (cardWidthPx <= 0 || cardHeightPx <= 0) {
        return imageHeightPx.toFloat() / imageWidthPx.toFloat() >= 1.35f
    }
    val scale = min(
        cardWidthPx.toFloat() / imageWidthPx.toFloat(),
        cardHeightPx.toFloat() / imageHeightPx.toFloat()
    )
    val drawnHeight = imageHeightPx * scale
    val imageTopY = ((cardHeightPx - drawnHeight) / 2f).coerceAtLeast(0f)
    return imageTopY <= headerHeightPx * 0.65f
}

internal fun analyzeStoryBitmapPalette(
    bitmap: Bitmap,
    sourceWidthPx: Int = bitmap.width,
    sourceHeightPx: Int = bitmap.height
): StoryVisualPalette {
    val w = bitmap.width
    val h = bitmap.height
    if (w <= 0 || h <= 0) return StoryVisualPalette()

    val topEnd = (h * 0.22f).toInt().coerceIn(1, h)
    val bottomStart = (h * 0.72f).toInt().coerceIn(0, h - 1)

    var topLumSum = 0f
    var topCount = 0
    var topR = 0L
    var topG = 0L
    var topB = 0L

    for (y in 0 until topEnd) {
        for (x in 0 until w) {
            val px = bitmap.getPixel(x, y)
            topLumSum += computeRelativeLuminance(px)
            topR += argbRed(px)
            topG += argbGreen(px)
            topB += argbBlue(px)
            topCount++
        }
    }

    var bottomLumSum = 0f
    var bottomCount = 0
    var bottomR = 0L
    var bottomG = 0L
    var bottomB = 0L

    for (y in bottomStart until h) {
        for (x in 0 until w) {
            val px = bitmap.getPixel(x, y)
            bottomLumSum += computeRelativeLuminance(px)
            bottomR += argbRed(px)
            bottomG += argbGreen(px)
            bottomB += argbBlue(px)
            bottomCount++
        }
    }

    val topAvgRgb = if (topCount > 0) {
        packRgb((topR / topCount).toInt(), (topG / topCount).toInt(), (topB / topCount).toInt())
    } else {
        0xFF151622.toInt()
    }
    val bottomAvgRgb = if (bottomCount > 0) {
        packRgb((bottomR / bottomCount).toInt(), (bottomG / bottomCount).toInt(), (bottomB / bottomCount).toInt())
    } else {
        0xFF0D0E15.toInt()
    }

    return StoryVisualPalette(
        topSliceLuminance = if (topCount > 0) topLumSum / topCount else 0.15f,
        bottomSliceLuminance = if (bottomCount > 0) bottomLumSum / bottomCount else 0.15f,
        imageWidthPx = sourceWidthPx,
        imageHeightPx = sourceHeightPx,
        ambientTopColorArgb = darkenToAmbientArgb(topAvgRgb),
        ambientBottomColorArgb = darkenToAmbientArgb(bottomAvgRgb)
    )
}

private fun sampleDrawablePalette(drawable: Drawable): StoryVisualPalette = runCatching {
    val srcW = drawable.intrinsicWidth.takeIf { it > 0 } ?: 24
    val srcH = drawable.intrinsicHeight.takeIf { it > 0 } ?: 36
    val sample = Bitmap.createBitmap(24, 36, Bitmap.Config.ARGB_8888)
    try {
        val canvas = Canvas(sample)
        if (drawable is BitmapDrawable) {
            val bmp = drawable.bitmap
            val safeBmp = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && bmp.config == Bitmap.Config.HARDWARE) {
                bmp.copy(Bitmap.Config.ARGB_8888, false)
            } else {
                bmp
            }
            if (safeBmp != null) {
                canvas.drawBitmap(
                    safeBmp,
                    null,
                    android.graphics.Rect(0, 0, sample.width, sample.height),
                    null
                )
                if (safeBmp !== bmp) safeBmp.recycle()
            }
        } else {
            val prevBounds = android.graphics.Rect(drawable.bounds)
            drawable.setBounds(0, 0, sample.width, sample.height)
            drawable.draw(canvas)
            drawable.bounds = prevBounds
        }
        analyzeStoryBitmapPalette(sample, sourceWidthPx = srcW, sourceHeightPx = srcH)
    } finally {
        sample.recycle()
    }
}.getOrDefault(StoryVisualPalette())

internal fun selectStoryTextBackdropColors(storyId: String): Pair<Long, Long> {
    val palettes = listOf(
        0xFF1F244D to 0xFF101326,
        0xFF3A1C46 to 0xFF180D24,
        0xFF163832 to 0xFF0C1D1B,
        0xFF3E221D to 0xFF1B100F,
        0xFF1E293B to 0xFF0F172A
    )
    val idx = abs(storyId.hashCode()) % palettes.size
    return palettes[idx]
}

@Composable
fun StoryStrip(
    stories: List<Story>,
    currentUserId: String,
    onStoryClick: (Story) -> Unit,
    onOpenProfile: (String) -> Unit = {},
    onCreateStory: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val latestByAuthor = stories
        .groupBy { it.authorId }
        .mapValues { (_, values) -> values.maxByOrNull { it.createdAt }!! }
        .values
        .sortedWith(compareByDescending<Story> { it.authorId == currentUserId }.thenByDescending { it.createdAt })

    val activeStoryRing = Brush.linearGradient(
        colors = listOf(
            Color(0xFF4F46E5),
            Color(0xFFEC4899),
            Color(0xFFF59E0B)
        )
    )

    LazyRow(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 4.dp, bottom = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        contentPadding = PaddingValues(horizontal = 18.dp)
    ) {
        item(key = "add-story") {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .width(68.dp)
                    .appleBouncy(pressedScale = 0.92f, onClick = onCreateStory)
            ) {
                Surface(
                    modifier = Modifier.size(62.dp),
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    border = BorderStroke(2.dp, MaterialTheme.colorScheme.outline)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Hikâye ekle",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                }
                Spacer(Modifier.height(5.dp))
                Text(
                    "Hikâye ekle",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onBackground,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        items(latestByAuthor.toList(), key = { it.authorId }) { story ->
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .width(68.dp)
                    .appleBouncy(pressedScale = 0.92f, onClick = { onStoryClick(story) })
            ) {
                Box(
                    modifier = Modifier
                        .size(62.dp)
                        .border(2.5.dp, activeStoryRing, CircleShape)
                        .padding(3.dp)
                        .clip(CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    UserAvatar(
                        photoUrl = story.authorPhotoUrl,
                        initials = story.authorName.take(1).uppercase().ifBlank { "L" },
                        size = 53.dp
                    )
                }
                Spacer(Modifier.height(5.dp))
                Text(
                    text = if (story.authorId == currentUserId) "Sen" else story.authorName.ifBlank { "Libra" },
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                    color = MaterialTheme.colorScheme.onBackground,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
fun StoryViewer(
    stories: List<Story>,
    initialIndex: Int,
    currentUserId: String,
    onDismiss: () -> Unit,
    onReply: (Story) -> Unit,
    onLike: (Story) -> Unit,
    onOpenProfile: (String) -> Unit = {},
    onShare: (Story) -> Unit = {}
) {
    if (stories.isEmpty()) return

    val viewerStories = remember { stories.toList() }
    if (viewerStories.isEmpty()) return

    var index by remember {
        mutableIntStateOf(initialIndex.coerceIn(0, viewerStories.lastIndex))
    }
    val story = viewerStories[index]

    BackHandler(onBack = onDismiss)

    val view = LocalView.current
    DisposableEffect(view) {
        val window = (view.context as? Activity)?.window
        val controller = window?.let { WindowCompat.getInsetsController(it, view) }
        val prevLightStatus = controller?.isAppearanceLightStatusBars
        val prevLightNav = controller?.isAppearanceLightNavigationBars
        if (controller != null) {
            controller.isAppearanceLightStatusBars = false
            controller.isAppearanceLightNavigationBars = false
        }
        onDispose {
            if (controller != null) {
                if (prevLightStatus != null) controller.isAppearanceLightStatusBars = prevLightStatus
                if (prevLightNav != null) controller.isAppearanceLightNavigationBars = prevLightNav
            }
        }
    }

    val context = LocalContext.current
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()

    var mediaUrl by remember(story.id, story.mediaUrl) { mutableStateOf("") }
    var isMediaReady by remember(story.id, story.mediaUrl) { mutableStateOf(story.mediaUrl.isBlank()) }
    var liked by remember(story.id, currentUserId) { mutableStateOf(false) }
    var paused by remember(story.id) { mutableStateOf(false) }
    var palette by remember(story.id, story.mediaUrl) { mutableStateOf(StoryVisualPalette()) }
    var cardWidthPx by remember { mutableIntStateOf(0) }
    var cardHeightPx by remember { mutableIntStateOf(0) }
    val progress = remember(story.id) { Animatable(0f) }

    LaunchedEffect(story.id, currentUserId) {
        mediaUrl = if (story.mediaUrl.isBlank()) "" else runCatching {
            ServiceLocator.storageRepository.getSignedMediaUrl(story.mediaUrl)
        }.getOrElse { story.mediaUrl }

        if (mediaUrl.isBlank()) {
            isMediaReady = true
        }

        when (val result = ServiceLocator.storyRepository.isLiked(story.id, currentUserId)) {
            is AppResult.Success -> liked = result.data
            is AppResult.Error -> Unit
        }
    }

    LaunchedEffect(story.id, paused, isMediaReady) {
        if (paused || !isMediaReady) return@LaunchedEffect
        val remaining = ((1f - progress.value) * 6000f).coerceAtLeast(1f).toInt()
        progress.animateTo(
            targetValue = 1f,
            animationSpec = tween(
                durationMillis = remaining,
                easing = LinearEasing
            )
        )
        if (!paused && isMediaReady) {
            if (index < viewerStories.lastIndex) index++ else onDismiss()
        }
    }

    val headerOverlapPx = with(density) { 88.dp.toPx() }
    val hasPhoto = mediaUrl.isNotBlank()
    val headerIsLight = hasPhoto && computeStoryHeaderIsLight(
        topSliceLuminance = palette.topSliceLuminance,
        cardWidthPx = cardWidthPx,
        cardHeightPx = cardHeightPx,
        imageWidthPx = palette.imageWidthPx,
        imageHeightPx = palette.imageHeightPx,
        headerHeightPx = headerOverlapPx
    )

    val textBackdropPair = remember(story.id) { selectStoryTextBackdropColors(story.id) }
    val targetAmbientTop = if (hasPhoto) Color(palette.ambientTopColorArgb) else Color(textBackdropPair.first)
    val targetAmbientBottom = if (hasPhoto) Color(palette.ambientBottomColorArgb) else Color(textBackdropPair.second)

    val ambientTopColor by animateColorAsState(
        targetValue = targetAmbientTop,
        animationSpec = tween(durationMillis = 320),
        label = "storyAmbientTop"
    )
    val ambientBottomColor by animateColorAsState(
        targetValue = targetAmbientBottom,
        animationSpec = tween(durationMillis = 320),
        label = "storyAmbientBottom"
    )
    val headerFgColor by animateColorAsState(
        targetValue = if (headerIsLight) Color(0xFF121218) else Color.White,
        animationSpec = tween(durationMillis = 220),
        label = "storyHeaderFg"
    )
    val headerSubFgColor by animateColorAsState(
        targetValue = if (headerIsLight) Color(0xFF3E404A) else Color.White.copy(alpha = 0.80f),
        animationSpec = tween(durationMillis = 220),
        label = "storyHeaderSubFg"
    )
    val trackActiveColor by animateColorAsState(
        targetValue = if (headerIsLight) Color(0xFF121218) else Color.White,
        animationSpec = tween(durationMillis = 220),
        label = "storyTrackActive"
    )
    val trackInactiveColor by animateColorAsState(
        targetValue = if (headerIsLight) Color(0xFF121218).copy(alpha = 0.24f) else Color.White.copy(alpha = 0.28f),
        animationSpec = tween(durationMillis = 220),
        label = "storyTrackInactive"
    )
    val topScrimStart by animateColorAsState(
        targetValue = if (headerIsLight) Color.White.copy(alpha = 0.82f) else Color.Black.copy(alpha = 0.62f),
        animationSpec = tween(durationMillis = 220),
        label = "storyScrimStart"
    )
    val topScrimMid by animateColorAsState(
        targetValue = if (headerIsLight) Color.White.copy(alpha = 0.36f) else Color.Black.copy(alpha = 0.24f),
        animationSpec = tween(durationMillis = 220),
        label = "storyScrimMid"
    )
    val headerChipBg by animateColorAsState(
        targetValue = if (headerIsLight) Color(0xFFF3F4F6).copy(alpha = 0.90f) else Color(0xFF16171F).copy(alpha = 0.68f),
        animationSpec = tween(durationMillis = 220),
        label = "storyHeaderChipBg"
    )
    val headerChipBorder by animateColorAsState(
        targetValue = if (headerIsLight) Color(0xFF121218).copy(alpha = 0.14f) else Color.White.copy(alpha = 0.18f),
        animationSpec = tween(durationMillis = 220),
        label = "storyHeaderChipBorder"
    )

    val textShadow = remember(headerIsLight) {
        Shadow(
            color = if (headerIsLight) Color.White.copy(alpha = 0.72f) else Color.Black.copy(alpha = 0.55f),
            offset = Offset(0f, 1f),
            blurRadius = 6f
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        ambientTopColor,
                        Color(0xFF0A0A0F),
                        ambientBottomColor
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            // Raised Story Stage Card (sits comfortably higher above bottom action dock)
            val cardShape = RoundedCornerShape(22.dp)
            Surface(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(start = 8.dp, end = 8.dp, top = 4.dp, bottom = 4.dp)
                    .onGloballyPositioned { coords ->
                        cardWidthPx = coords.size.width
                        cardHeightPx = coords.size.height
                    },
                shape = cardShape,
                color = Color(0xFF121218),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.10f))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    ambientTopColor,
                                    Color(0xFF121218),
                                    ambientBottomColor
                                )
                            )
                        )
                        .pointerInput(story.id, viewerStories.size) {
                            detectTapGestures(
                                onLongPress = { paused = true },
                                onPress = {
                                    try {
                                        tryAwaitRelease()
                                    } finally {
                                        paused = false
                                    }
                                },
                                onTap = { offset ->
                                    if (offset.x < size.width * 0.28f) {
                                        if (index > 0) {
                                            index--
                                        } else {
                                            scope.launch { progress.snapTo(0f) }
                                        }
                                    } else {
                                        if (index < viewerStories.lastIndex) {
                                            index++
                                        } else {
                                            onDismiss()
                                        }
                                    }
                                }
                            )
                        }
                ) {
                    if (hasPhoto) {
                        val imageRequest = remember(mediaUrl, context) {
                            ImageRequest.Builder(context)
                                .data(mediaUrl)
                                .allowHardware(false)
                                .crossfade(200)
                                .build()
                        }

                        AsyncImage(
                            model = imageRequest,
                            contentDescription = "Hikâye",
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(bottom = if (story.text.isNotBlank()) 44.dp else 0.dp),
                            contentScale = ContentScale.Fit,
                            onLoading = { isMediaReady = false },
                            onError = { isMediaReady = true },
                            onSuccess = { success ->
                                isMediaReady = true
                                val drawable = success.result.drawable
                                scope.launch {
                                    val sampled = withContext(Dispatchers.Default) {
                                        sampleDrawablePalette(drawable)
                                    }
                                    palette = sampled
                                }
                            }
                        )

                        if (!isMediaReady) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(30.dp),
                                    strokeWidth = 2.5.dp,
                                    color = Color.White
                                )
                            }
                        }
                    } else if (story.text.isNotBlank()) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.linearGradient(
                                        colors = listOf(
                                            Color(textBackdropPair.first),
                                            Color(textBackdropPair.second)
                                        )
                                    )
                                )
                                .padding(horizontal = 28.dp, vertical = 96.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = story.text,
                                color = Color.White,
                                textAlign = TextAlign.Center,
                                style = MaterialTheme.typography.headlineSmall.copy(
                                    fontSize = 23.sp,
                                    lineHeight = 32.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    shadow = Shadow(
                                        color = Color.Black.copy(alpha = 0.35f),
                                        offset = Offset(0f, 2f),
                                        blurRadius = 8f
                                    )
                                )
                            )
                        }
                    }

                    // Top adaptive gradient protection + Segmented progress + Author header
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.TopCenter)
                            .background(
                                Brush.verticalGradient(
                                    0.0f to topScrimStart,
                                    0.6f to topScrimMid,
                                    1.0f to Color.Transparent
                                )
                            )
                            .padding(start = 12.dp, end = 12.dp, top = 12.dp, bottom = 22.dp)
                    ) {
                        if (viewerStories.size <= 12) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                viewerStories.forEachIndexed { segIndex, _ ->
                                    val fillRatio = when {
                                        segIndex < index -> 1f
                                        segIndex == index -> progress.value.coerceIn(0f, 1f)
                                        else -> 0f
                                    }
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(3.dp)
                                            .clip(CircleShape)
                                            .background(trackInactiveColor)
                                    ) {
                                        if (fillRatio > 0f) {
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxHeight()
                                                    .fillMaxWidth(fillRatio)
                                                    .clip(CircleShape)
                                                    .background(trackActiveColor)
                                            )
                                        }
                                    }
                                }
                            }
                        } else {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(3.dp)
                                        .clip(CircleShape)
                                        .background(trackInactiveColor)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxHeight()
                                            .fillMaxWidth(progress.value.coerceIn(0f, 1f))
                                            .clip(CircleShape)
                                            .background(trackActiveColor)
                                    )
                                }
                                Surface(
                                    shape = CircleShape,
                                    color = headerChipBg,
                                    border = BorderStroke(1.dp, headerChipBorder)
                                ) {
                                    Text(
                                        text = "${index + 1}/${viewerStories.size}",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontSize = 10.5.sp,
                                            fontWeight = FontWeight.SemiBold
                                        ),
                                        color = headerFgColor,
                                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }

                        Spacer(Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .border(
                                        width = 1.5.dp,
                                        color = headerChipBorder,
                                        shape = CircleShape
                                    )
                                    .padding(2.dp)
                            ) {
                                UserAvatar(
                                    photoUrl = story.authorPhotoUrl,
                                    initials = story.authorName.take(1).uppercase().ifBlank { "L" },
                                    size = 36.dp,
                                    onClick = { onOpenProfile(story.authorId) }
                                )
                            }

                            Spacer(Modifier.width(10.dp))

                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { onOpenProfile(story.authorId) }
                            ) {
                                Text(
                                    text = story.authorName.ifBlank { "Libra kullanıcısı" },
                                    color = headerFgColor,
                                    style = MaterialTheme.typography.titleSmall.copy(
                                        fontSize = 14.5.sp,
                                        lineHeight = 18.sp,
                                        fontWeight = FontWeight.Bold,
                                        shadow = textShadow
                                    ),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = if (paused) "Duraklatıldı" else "24 saatlik hikâye",
                                        color = headerSubFgColor,
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontSize = 11.5.sp,
                                            lineHeight = 14.sp,
                                            fontWeight = FontWeight.Medium,
                                            shadow = textShadow
                                        ),
                                        maxLines = 1
                                    )
                                }
                            }

                            Surface(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .appleBouncy(pressedScale = 0.86f, onClick = onDismiss),
                                shape = CircleShape,
                                color = headerChipBg,
                                border = BorderStroke(1.dp, headerChipBorder)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Kapat",
                                        tint = headerFgColor,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }

                    // High-contrast floating caption pill over image
                    if (story.text.isNotBlank() && hasPhoto) {
                        val isCaptionLight = palette.bottomSliceLuminance >= 0.56f
                        Surface(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(horizontal = 14.dp, vertical = 14.dp),
                            shape = RoundedCornerShape(16.dp),
                            color = if (isCaptionLight) {
                                Color(0xEC14151C)
                            } else {
                                Color(0xCC12131A)
                            },
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.16f))
                        ) {
                            Text(
                                text = story.text,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                color = Color.White,
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontSize = 14.5.sp,
                                    lineHeight = 20.sp,
                                    fontWeight = FontWeight.Medium
                                ),
                                textAlign = TextAlign.Center,
                                maxLines = 4,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }

            // Dedicated Bottom Action Dock below media stage (always visible & legible)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 12.dp, end = 12.dp, top = 6.dp, bottom = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .height(46.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .appleBouncy(pressedScale = 0.96f, onClick = { onReply(story) }),
                    shape = RoundedCornerShape(24.dp),
                    color = Color(0xFF1A1B24),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.16f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Reply,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "Mesaj ata",
                            color = Color.White.copy(alpha = 0.90f),
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium
                            ),
                            maxLines = 1
                        )
                    }
                }

                Surface(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .appleBouncy(
                            pressedScale = 0.86f,
                            onClick = {
                                liked = !liked
                                onLike(story)
                            }
                        ),
                    shape = CircleShape,
                    color = if (liked) Color(0xFF3A1823) else Color(0xFF1A1B24),
                    border = BorderStroke(
                        width = 1.dp,
                        color = if (liked) Color(0xFFFF4D6D) else Color.White.copy(alpha = 0.16f)
                    )
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = if (liked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = "Beğen",
                            tint = if (liked) Color(0xFFFF4D6D) else Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Surface(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .appleBouncy(pressedScale = 0.86f, onClick = { onShare(story) }),
                    shape = CircleShape,
                    color = Color(0xFF1A1B24),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.16f))
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "Paylaş",
                            tint = Color.White,
                            modifier = Modifier.size(19.dp)
                        )
                    }
                }
            }
        }
    }
}
