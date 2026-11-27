package com.libra.app.feature.messages

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.Layers
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.rounded.ChatBubble
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material.icons.rounded.Layers
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.libra.app.R
import com.libra.app.core.di.ServiceLocator
import com.libra.app.domain.model.UserProfile
import com.libra.app.ui.animation.appleBouncy
import com.libra.app.ui.components.UserAvatar
import com.libra.app.ui.theme.LocalLibraDarkTheme
import kotlinx.coroutines.delay
import kotlin.math.abs

/**
 * Disciplined Libra-signature color palette for Messaging, Global Chat, Groups, and Servers.
 * Avoids generic violet gradients in favor of crisp editorial Cobalt-Slate & Mineral Teal.
 */
@Immutable
data class LibraCommsColors(
    val canvas: Color,
    val surface: Color,
    val recessed: Color,
    val hairline: Color,
    val inkPrimary: Color,
    val inkSecondary: Color,
    val brand: Color,
    val brandSoft: Color,
    val brandText: Color,
    val community: Color,
    val communitySoft: Color,
    val communityText: Color,
    val server: Color,
    val serverSoft: Color,
    val serverText: Color,
    val unreadBadge: Color,
    val alertDot: Color,
    val sentBubble: Color,
    val sentBubbleQuote: Color,
    val receivedBubble: Color
)

@Composable
fun rememberLibraCommsColors(): LibraCommsColors {
    val darkTheme = LocalLibraDarkTheme.current
    return remember(darkTheme) {
        if (darkTheme) {
            LibraCommsColors(
                canvas = Color(0xFF0E1015),
                surface = Color(0xFF161A22),
                recessed = Color(0xFF1E232E),
                hairline = Color(0xFF262D3B),
                inkPrimary = Color(0xFFF3F5F9),
                inkSecondary = Color(0xFF94A3B8),
                brand = Color(0xFF4F7EF7),
                brandSoft = Color(0xFF19243F),
                brandText = Color(0xFF9BB8FF),
                community = Color(0xFF2DD4BF),
                communitySoft = Color(0xFF112C2B),
                communityText = Color(0xFF7CEEE0),
                server = Color(0xFF7C8CF8),
                serverSoft = Color(0xFF1D223B),
                serverText = Color(0xFFB4BEFF),
                unreadBadge = Color(0xFF4F7EF7),
                alertDot = Color(0xFFFB7185),
                sentBubble = Color(0xFF2F5FD0),
                sentBubbleQuote = Color(0xFF2349A3),
                receivedBubble = Color(0xFF191D26)
            )
        } else {
            LibraCommsColors(
                canvas = Color(0xFFF6F7FA),
                surface = Color(0xFFFFFFFF),
                recessed = Color(0xFFECEFF5),
                hairline = Color(0xFFE3E7F0),
                inkPrimary = Color(0xFF111827),
                inkSecondary = Color(0xFF64748B),
                brand = Color(0xFF2554C7),
                brandSoft = Color(0xFFEAF0FE),
                brandText = Color(0xFF1E44A6),
                community = Color(0xFF0E8A7D),
                communitySoft = Color(0xFFE6F6F4),
                communityText = Color(0xFF0B6E64),
                server = Color(0xFF273764),
                serverSoft = Color(0xFFEAEEF8),
                serverText = Color(0xFF1E2C52),
                unreadBadge = Color(0xFF2554C7),
                alertDot = Color(0xFFE11D48),
                sentBubble = Color(0xFF2554C7),
                sentBubbleQuote = Color(0xFF1C42A0),
                receivedBubble = Color(0xFFFFFFFF)
            )
        }
    }
}

private val EditorialPatternPalettes = listOf(
    Triple(Color(0xFF162447), Color(0xFF1F4068), Color(0xFF93C5FD)),
    Triple(Color(0xFF123034), Color(0xFF0F5C5C), Color(0xFF7CEEE0)),
    Triple(Color(0xFF1E293B), Color(0xFF334155), Color(0xFFCBD5E1)),
    Triple(Color(0xFF2B2219), Color(0xFF6B4423), Color(0xFFFDE68A)),
    Triple(Color(0xFF1B2438), Color(0xFF2F4B8A), Color(0xFFBFDBFE)),
    Triple(Color(0xFF2C1D24), Color(0xFF6F2B45), Color(0xFFFECDD3))
)

private val SenderAccentLight = listOf(
    Color(0xFF1E44A6),
    Color(0xFF0E7490),
    Color(0xFF0F766E),
    Color(0xFFB45309),
    Color(0xFFBE123C),
    Color(0xFF3730A3)
)

private val SenderAccentDark = listOf(
    Color(0xFF93C5FD),
    Color(0xFF67E8F9),
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
 * Reference-faithful persistent top chrome for the 3-mode Messaging Hub:
 * 1. Top row: Libra logo + wordmark on left; circular Search, Notification bell, and Profile avatar on right.
 * 2. Balanced 3-segment realm switcher immediately below: Mesajlar | Genel Sohbet | Sunucular.
 * 3. Smooth collapsible inline search field when the Search icon is toggled.
 */
@Composable
fun LibraMessagingTopChrome(
    activeMode: DmHubMode,
    currentUser: UserProfile?,
    unreadNotificationCount: Int,
    searchActive: Boolean,
    searchQuery: String,
    searchPlaceholder: String,
    onToggleSearch: () -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onNotificationsClick: () -> Unit,
    onProfileClick: () -> Unit,
    onSelectMode: (DmHubMode, Offset) -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = rememberLibraCommsColors()
    var messagesOrigin by remember { mutableStateOf(Offset.Zero) }
    var globalOrigin by remember { mutableStateOf(Offset.Zero) }
    var serversOrigin by remember { mutableStateOf(Offset.Zero) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(palette.canvas)
            .padding(horizontal = 16.dp)
            .padding(top = 10.dp, bottom = 10.dp)
    ) {
        // Row 1: Libra Brand Header + Search / Bell / Profile
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(9.dp),
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_libra_notification),
                    contentDescription = "Libra",
                    tint = palette.brand,
                    modifier = Modifier.size(28.dp)
                )
                Text(
                    text = "Libra",
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontSize = 23.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = (-0.5).sp
                    ),
                    color = palette.inkPrimary
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TopChromeCircleButton(
                    icon = if (searchActive) Icons.Rounded.Close else Icons.Rounded.Search,
                    contentDescription = "Ara",
                    active = searchActive,
                    showDot = false,
                    onClick = onToggleSearch
                )
                TopChromeCircleButton(
                    icon = Icons.Outlined.Notifications,
                    contentDescription = "Bildirimler",
                    active = false,
                    showDot = unreadNotificationCount > 0,
                    onClick = onNotificationsClick
                )
                UserAvatar(
                    photoUrl = currentUser?.profileImageUrl,
                    initials = currentUser?.initials ?: "L",
                    size = 38.dp,
                    onClick = onProfileClick
                )
            }
        }

        Spacer(Modifier.height(12.dp))

        // Row 2: Balanced 3-Segment Switcher — Mesajlar | Genel Sohbet | Sunucular
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            color = palette.recessed,
            border = BorderStroke(1.dp, palette.hairline)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RealmSegmentButton(
                    label = "Mesajlar",
                    icon = if (activeMode == DmHubMode.MESSAGES) Icons.Rounded.ChatBubble else Icons.Outlined.ChatBubbleOutline,
                    selected = activeMode == DmHubMode.MESSAGES,
                    activeColor = palette.brand,
                    spinOnSelect = false,
                    onPositioned = { messagesOrigin = it },
                    onClick = { onSelectMode(DmHubMode.MESSAGES, messagesOrigin) },
                    modifier = Modifier.weight(0.95f)
                )
                RealmSegmentButton(
                    label = "Genel Sohbet",
                    icon = if (activeMode == DmHubMode.GLOBAL_CHAT) Icons.Rounded.Groups else Icons.Outlined.Groups,
                    selected = activeMode == DmHubMode.GLOBAL_CHAT,
                    activeColor = palette.community,
                    spinOnSelect = false,
                    onPositioned = { globalOrigin = it },
                    onClick = { onSelectMode(DmHubMode.GLOBAL_CHAT, globalOrigin) },
                    modifier = Modifier.weight(1.18f)
                )
                RealmSegmentButton(
                    label = "Sunucular",
                    icon = if (activeMode == DmHubMode.SERVERS) Icons.Rounded.Layers else Icons.Outlined.Layers,
                    selected = activeMode == DmHubMode.SERVERS,
                    activeColor = palette.server,
                    spinOnSelect = true,
                    onPositioned = { serversOrigin = it },
                    onClick = { onSelectMode(DmHubMode.SERVERS, serversOrigin) },
                    modifier = Modifier.weight(0.97f)
                )
            }
        }

        // Collapsible Inline Search Field (appears smoothly only when Search is tapped)
        AnimatedVisibility(
            visible = searchActive,
            enter = expandVertically(animationSpec = tween(200)) + fadeIn(tween(180)),
            exit = shrinkVertically(animationSpec = tween(180)) + fadeOut(tween(140))
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp),
                shape = RoundedCornerShape(14.dp),
                color = palette.surface,
                border = BorderStroke(1.dp, if (searchQuery.isNotBlank()) palette.brand else palette.hairline)
            ) {
                TextField(
                    value = searchQuery,
                    onValueChange = onSearchQueryChange,
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyMedium.copy(color = palette.inkPrimary),
                    placeholder = {
                        Text(
                            text = searchPlaceholder,
                            style = MaterialTheme.typography.bodyMedium,
                            color = palette.inkSecondary
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Rounded.Search,
                            contentDescription = null,
                            tint = if (searchQuery.isNotBlank()) palette.brand else palette.inkSecondary,
                            modifier = Modifier.size(19.dp)
                        )
                    },
                    trailingIcon = if (searchQuery.isNotBlank()) {
                        {
                            IconButton(onClick = { onSearchQueryChange("") }) {
                                Icon(
                                    Icons.Rounded.Close,
                                    contentDescription = "Temizle",
                                    tint = palette.inkSecondary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    } else null,
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        cursorColor = palette.brand
                    )
                )
            }
        }
    }
}

@Composable
private fun TopChromeCircleButton(
    icon: ImageVector,
    contentDescription: String,
    active: Boolean,
    showDot: Boolean,
    onClick: () -> Unit
) {
    val palette = rememberLibraCommsColors()
    Box(contentAlignment = Alignment.TopEnd) {
        Surface(
            modifier = Modifier
                .size(38.dp)
                .clip(CircleShape)
                .appleBouncy(pressedScale = 0.88f, onClick = onClick),
            shape = CircleShape,
            color = if (active) palette.brandSoft else palette.surface,
            border = BorderStroke(1.dp, if (active) palette.brand.copy(alpha = 0.45f) else palette.hairline)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = icon,
                    contentDescription = contentDescription,
                    tint = if (active) palette.brand else palette.inkPrimary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
        if (showDot) {
            Box(
                modifier = Modifier
                    .padding(top = 2.dp, end = 2.dp)
                    .size(9.dp)
                    .clip(CircleShape)
                    .background(palette.brand)
            )
        }
    }
}

@Composable
private fun RealmSegmentButton(
    label: String,
    icon: ImageVector,
    selected: Boolean,
    activeColor: Color,
    spinOnSelect: Boolean,
    onPositioned: (Offset) -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = rememberLibraCommsColors()
    val bgColor by animateColorAsState(
        targetValue = if (selected) activeColor else Color.Transparent,
        animationSpec = tween(220),
        label = "realm_seg_bg"
    )
    val contentColor by animateColorAsState(
        targetValue = if (selected) Color.White else palette.inkPrimary,
        animationSpec = tween(220),
        label = "realm_seg_fg"
    )
    val iconRotation = remember { Animatable(0f) }
    val iconScale = remember { Animatable(1f) }

    LaunchedEffect(selected) {
        if (selected) {
            if (spinOnSelect) {
                iconRotation.snapTo(0f)
                iconRotation.animateTo(360f, tween(420, easing = FastOutSlowInEasing))
            } else {
                iconScale.snapTo(0.84f)
                iconScale.animateTo(1f, tween(260, easing = FastOutSlowInEasing))
            }
        }
    }

    Surface(
        modifier = modifier
            .onGloballyPositioned { coords -> onPositioned(coords.boundsInRoot().center) }
            .clip(RoundedCornerShape(20.dp))
            .appleBouncy(pressedScale = 0.95f, onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        color = bgColor
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier
                    .size(17.dp)
                    .graphicsLayer {
                        rotationZ = iconRotation.value
                        scaleX = iconScale.value
                        scaleY = iconScale.value
                    }
            )
            Spacer(Modifier.width(5.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge.copy(
                    fontSize = 12.5.sp,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.SemiBold,
                    letterSpacing = (-0.2).sp
                ),
                color = contentColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
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
 * Clean minimal architectural banner used behind Group & Server headers
 * whenever no custom cover image is uploaded.
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
                val anchor = Offset(size.width * 0.86f, size.height * 0.24f)
                for (ring in 1..3) {
                    drawCircle(
                        color = lineTint.copy(alpha = (0.16f - ring * 0.03f).coerceAtLeast(0.05f)),
                        radius = size.minDimension * (0.28f * ring),
                        center = anchor,
                        style = Stroke(width = 1.dp.toPx())
                    )
                }
            }
        }
    )
}

/**
 * Restrained squircle avatar used for Servers & Group headers.
 */
@Composable
fun LibraSquircleAvatar(
    photoUrl: String?,
    title: String,
    size: Dp = 48.dp,
    cornerRadius: Dp = 14.dp,
    borderColor: Color? = null,
    borderWidth: Dp = 1.dp,
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
                        fontWeight = FontWeight.Bold,
                        fontSize = (size.value * 0.34f).sp,
                        letterSpacing = 0.4.sp
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
    accentColor: Color? = null
) {
    val palette = rememberLibraCommsColors()
    val accent = accentColor ?: palette.brand
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = if (highlighted) accent else palette.brandSoft,
        border = if (!highlighted) BorderStroke(0.8.dp, palette.hairline) else null
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 10.sp
            ),
            color = if (highlighted) Color.White else palette.brandText,
            modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
        )
    }
}
