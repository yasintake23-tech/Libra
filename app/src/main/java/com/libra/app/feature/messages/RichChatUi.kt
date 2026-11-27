package com.libra.app.feature.messages

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Reply
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.CheckCircleOutline
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.libra.app.core.di.ServiceLocator
import com.libra.app.domain.model.DirectConversation
import com.libra.app.ui.animation.AppleSprings
import com.libra.app.ui.animation.appleBouncy
import com.libra.app.ui.components.UserAvatar
import com.libra.app.ui.theme.LibraLavender
import com.libra.app.ui.theme.LibraLavenderBorder
import com.libra.app.ui.theme.LibraLavenderDarkBorder
import com.libra.app.ui.theme.LibraLavenderDarkSurface
import com.libra.app.ui.theme.LibraLavenderDarkText
import com.libra.app.ui.theme.LibraLavenderSurface
import com.libra.app.ui.theme.LibraLavenderText
import com.libra.app.ui.theme.LocalLibraDarkTheme

val LibraReactionEmojis = listOf("❤️", "😂", "😮", "😢", "😡", "👍")

data class SpotlightCircularAction(
    val label: String,
    val icon: ImageVector,
    val circleColor: Color,
    val iconTint: Color,
    val borderColor: Color,
    val onClick: () -> Unit
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun RichMessageActionSheet(
    canEdit: Boolean,
    canDelete: Boolean,
    onDismiss: () -> Unit,
    onReply: () -> Unit,
    onReaction: (String) -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    previewText: String = "",
    previewMediaUrl: String = "",
    previewSharedTitle: String = "",
    previewTimestamp: String = "",
    isOwnMessage: Boolean = false,
    selectedReaction: String? = null,
    onCopy: (() -> Unit)? = null,
    onShare: (() -> Unit)? = null,
    onSelectMultiple: (() -> Unit)? = null
) {
    val darkTheme = LocalLibraDarkTheme.current
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }

    var resolvedPreviewPhoto by remember(previewMediaUrl) { mutableStateOf<String?>(null) }
    LaunchedEffect(previewMediaUrl) {
        if (previewMediaUrl.isNotBlank()) {
            resolvedPreviewPhoto = runCatching {
                ServiceLocator.storageRepository.getSignedMediaUrl(previewMediaUrl)
            }.getOrNull()
        }
    }

    val neutralCircleBg = MaterialTheme.colorScheme.surfaceVariant
    val neutralCircleBorder = MaterialTheme.colorScheme.outline
    val neutralIconTint = MaterialTheme.colorScheme.onSurface

    val palette = rememberLibraCommsColors()
    val replyCircleBg = palette.brandSoft
    val replyCircleBorder = palette.brand.copy(alpha = 0.35f)
    val replyIconTint = palette.brandText

    val shareCircleBg = if (darkTheme) Color(0xFF172554) else Color(0xFFE0F2FE)
    val shareCircleBorder = if (darkTheme) Color(0xFF1E40AF) else Color(0xFFBAE6FD)
    val shareIconTint = if (darkTheme) Color(0xFF93C5FD) else Color(0xFF0369A1)

    val editCircleBg = if (darkTheme) Color(0xFF422006) else Color(0xFFFEF3C7)
    val editCircleBorder = if (darkTheme) Color(0xFF854D0E) else Color(0xFFFDE68A)
    val editIconTint = if (darkTheme) Color(0xFFFCD34D) else Color(0xFFB45309)

    val deleteCircleBg = if (darkTheme) Color(0xFF450A0A) else Color(0xFFFEE2E2)
    val deleteCircleBorder = if (darkTheme) Color(0xFF991B1B) else Color(0xFFFECACA)
    val deleteIconTint = if (darkTheme) Color(0xFFFCA5A5) else Color(0xFFDC2626)

    val actions = buildList {
        add(
            SpotlightCircularAction(
                label = "Yanıtla",
                icon = Icons.AutoMirrored.Filled.Reply,
                circleColor = replyCircleBg,
                iconTint = replyIconTint,
                borderColor = replyCircleBorder,
                onClick = onReply
            )
        )
        if (onSelectMultiple != null) {
            add(
                SpotlightCircularAction(
                    label = "Seç",
                    icon = Icons.Default.CheckCircleOutline,
                    circleColor = neutralCircleBg,
                    iconTint = neutralIconTint,
                    borderColor = neutralCircleBorder,
                    onClick = onSelectMultiple
                )
            )
        }
        if (onShare != null) {
            add(
                SpotlightCircularAction(
                    label = "Paylaş",
                    icon = Icons.Default.Share,
                    circleColor = shareCircleBg,
                    iconTint = shareIconTint,
                    borderColor = shareCircleBorder,
                    onClick = onShare
                )
            )
        }
        if (onCopy != null) {
            add(
                SpotlightCircularAction(
                    label = "Kopyala",
                    icon = Icons.Default.ContentCopy,
                    circleColor = neutralCircleBg,
                    iconTint = neutralIconTint,
                    borderColor = neutralCircleBorder,
                    onClick = onCopy
                )
            )
        }
        if (canEdit) {
            add(
                SpotlightCircularAction(
                    label = "Düzenle",
                    icon = Icons.Default.Edit,
                    circleColor = editCircleBg,
                    iconTint = editIconTint,
                    borderColor = editCircleBorder,
                    onClick = onEdit
                )
            )
        }
        if (canDelete) {
            add(
                SpotlightCircularAction(
                    label = "Sil",
                    icon = Icons.Default.DeleteOutline,
                    circleColor = deleteCircleBg,
                    iconTint = deleteIconTint,
                    borderColor = deleteCircleBorder,
                    onClick = onDelete
                )
            )
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.56f))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onDismiss
                ),
            contentAlignment = Alignment.Center
        ) {
            AnimatedVisibility(
                visible = visible,
                enter = fadeIn(animationSpec = AppleSprings.snappy()) +
                    scaleIn(initialScale = 0.88f, animationSpec = AppleSprings.bouncy()),
                exit = fadeOut() + scaleOut(targetScale = 0.92f)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = {}
                        ),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // 1. Instagram-style floating reaction pill bar
                    Surface(
                        shape = RoundedCornerShape(32.dp),
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                        shadowElevation = 12.dp
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            LibraReactionEmojis.forEach { emoji ->
                                val isPicked = emoji == selectedReaction
                                Surface(
                                    modifier = Modifier
                                        .size(42.dp)
                                        .clip(CircleShape)
                                        .appleBouncy(pressedScale = 0.80f, onClick = { onReaction(emoji) }),
                                    shape = CircleShape,
                                    color = if (isPicked) replyCircleBg else MaterialTheme.colorScheme.surfaceVariant,
                                    border = if (isPicked) BorderStroke(1.5.dp, replyCircleBorder) else null
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(emoji, fontSize = 20.sp)
                                    }
                                }
                            }
                        }
                    }

                    // 2. Spotlight message bubble preview
                    val hasPreview = previewText.isNotBlank() ||
                        previewMediaUrl.isNotBlank() ||
                        previewSharedTitle.isNotBlank()

                    if (hasPreview) {
                        val bubbleShape = RoundedCornerShape(16.dp)
                        val bubbleColor = if (isOwnMessage) palette.sentBubble else palette.receivedBubble
                        val bubbleTextColor = if (isOwnMessage) Color.White else palette.inkPrimary
                        val bubbleBorder = if (isOwnMessage) null else BorderStroke(0.85.dp, palette.hairline)

                        Surface(
                            modifier = Modifier.widthIn(min = 140.dp, max = 296.dp),
                            shape = bubbleShape,
                            color = bubbleColor,
                            border = bubbleBorder,
                            shadowElevation = 6.dp
                        ) {
                            Column(
                                modifier = Modifier.padding(10.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                resolvedPreviewPhoto?.takeIf { it.isNotBlank() }?.let { imgUrl ->
                                    AsyncImage(
                                        model = imgUrl,
                                        contentDescription = "Seçilen fotoğraf",
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .heightIn(max = 170.dp)
                                            .clip(RoundedCornerShape(12.dp)),
                                        contentScale = ContentScale.Crop
                                    )
                                } ?: if (previewMediaUrl.isNotBlank()) {
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = if (isOwnMessage) palette.sentBubbleQuote else palette.recessed
                                    ) {
                                        Text(
                                            text = "Fotoğraf mesajı",
                                            color = bubbleTextColor,
                                            style = MaterialTheme.typography.labelMedium,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)
                                        )
                                    }
                                } else Unit

                                if (previewSharedTitle.isNotBlank()) {
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = if (isOwnMessage) palette.sentBubbleQuote else palette.recessed
                                    ) {
                                        Text(
                                            text = previewSharedTitle,
                                            color = bubbleTextColor,
                                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp)
                                        )
                                    }
                                }

                                if (previewText.isNotBlank()) {
                                    Text(
                                        text = previewText,
                                        color = bubbleTextColor,
                                        style = MaterialTheme.typography.bodyMedium,
                                        maxLines = 5,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.padding(horizontal = 4.dp)
                                    )
                                }

                                if (previewTimestamp.isNotBlank()) {
                                    Text(
                                        text = previewTimestamp,
                                        color = if (isOwnMessage) Color(0xFFE9D5FF) else MaterialTheme.colorScheme.onSurfaceVariant,
                                        style = MaterialTheme.typography.labelSmall,
                                        modifier = Modifier
                                            .align(Alignment.End)
                                            .padding(horizontal = 4.dp)
                                    )
                                }
                            }
                        }
                    }

                    // 3. Circular quick-action buttons dock (Instagram DM style)
                    Surface(
                        shape = RoundedCornerShape(26.dp),
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                        shadowElevation = 14.dp
                    ) {
                        FlowRow(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                            horizontalArrangement = Arrangement.spacedBy(14.dp, Alignment.CenterHorizontally),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            actions.forEach { action ->
                                CircularSpotlightActionButton(action = action)
                            }
                        }
                    }

                    // Subtle dismiss pill
                    Surface(
                        modifier = Modifier
                            .clip(CircleShape)
                            .appleBouncy(pressedScale = 0.88f, onClick = onDismiss),
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                        shadowElevation = 6.dp
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Kapat",
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "Kapat",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Medium),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun RichConversationActionOverlay(
    conversation: DirectConversation,
    onDismiss: () -> Unit,
    onOpenConversation: () -> Unit,
    onOpenProfile: (() -> Unit)?,
    onMarkRead: (() -> Unit)?,
    onShare: () -> Unit,
    onDeleteConversation: () -> Unit
) {
    val darkTheme = LocalLibraDarkTheme.current
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }

    val palette = rememberLibraCommsColors()
    val primaryBg = palette.brandSoft
    val primaryBorder = palette.brand.copy(alpha = 0.35f)
    val primaryTint = palette.brandText

    val shareBg = if (darkTheme) Color(0xFF172554) else Color(0xFFE0F2FE)
    val shareBorder = if (darkTheme) Color(0xFF1E40AF) else Color(0xFFBAE6FD)
    val shareTint = if (darkTheme) Color(0xFF93C5FD) else Color(0xFF0369A1)

    val deleteBg = if (darkTheme) Color(0xFF450A0A) else Color(0xFFFEE2E2)
    val deleteBorder = if (darkTheme) Color(0xFF991B1B) else Color(0xFFFECACA)
    val deleteTint = if (darkTheme) Color(0xFFFCA5A5) else Color(0xFFDC2626)

    val actions = buildList {
        add(
            SpotlightCircularAction(
                label = "Sohbete Gir",
                icon = Icons.Default.ChatBubbleOutline,
                circleColor = primaryBg,
                iconTint = primaryTint,
                borderColor = primaryBorder,
                onClick = onOpenConversation
            )
        )
        if (onOpenProfile != null) {
            add(
                SpotlightCircularAction(
                    label = "Profil",
                    icon = Icons.Default.Person,
                    circleColor = MaterialTheme.colorScheme.surfaceVariant,
                    iconTint = MaterialTheme.colorScheme.onSurface,
                    borderColor = MaterialTheme.colorScheme.outline,
                    onClick = onOpenProfile
                )
            )
        }
        if (onMarkRead != null) {
            add(
                SpotlightCircularAction(
                    label = "Okundu",
                    icon = Icons.Default.DoneAll,
                    circleColor = MaterialTheme.colorScheme.surfaceVariant,
                    iconTint = MaterialTheme.colorScheme.onSurface,
                    borderColor = MaterialTheme.colorScheme.outline,
                    onClick = onMarkRead
                )
            )
        }
        add(
            SpotlightCircularAction(
                label = "Paylaş",
                icon = Icons.Default.Share,
                circleColor = shareBg,
                iconTint = shareTint,
                borderColor = shareBorder,
                onClick = onShare
            )
        )
        add(
            SpotlightCircularAction(
                label = "Sil",
                icon = Icons.Default.DeleteOutline,
                circleColor = deleteBg,
                iconTint = deleteTint,
                borderColor = deleteBorder,
                onClick = onDeleteConversation
            )
        )
    }

    val title = if (conversation.isGroup) {
        conversation.groupName.ifBlank { "Grup sohbeti" }
    } else {
        conversation.otherUserName.ifBlank { "Libra Üyesi" }
    }
    val subtitle = if (conversation.isGroup) {
        "${conversation.participantIds.size} katılımcı"
    } else if (conversation.otherUserUsername.isNotBlank()) {
        "@${conversation.otherUserUsername}"
    } else ""

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.56f))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onDismiss
                ),
            contentAlignment = Alignment.Center
        ) {
            AnimatedVisibility(
                visible = visible,
                enter = fadeIn(animationSpec = AppleSprings.snappy()) +
                    scaleIn(initialScale = 0.88f, animationSpec = AppleSprings.bouncy()),
                exit = fadeOut() + scaleOut(targetScale = 0.92f)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = {}
                        ),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Elevated Conversation Preview Card
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(22.dp),
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                        shadowElevation = 14.dp
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            UserAvatar(
                                photoUrl = if (conversation.isGroup) conversation.groupPhotoUrl else conversation.otherUserPhotoUrl,
                                initials = title.take(1).uppercase(),
                                size = 52.dp
                            )
                            Spacer(Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = title,
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                if (subtitle.isNotBlank()) {
                                    Text(
                                        text = subtitle,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                if (conversation.lastMessage.isNotBlank()) {
                                    Spacer(Modifier.height(3.dp))
                                    Text(
                                        text = conversation.lastMessage,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                            if (conversation.updatedAt > 0L) {
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = formatConversationRelativeTime(conversation.updatedAt),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    // Circular quick actions dock
                    Surface(
                        shape = RoundedCornerShape(26.dp),
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                        shadowElevation = 14.dp
                    ) {
                        FlowRow(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                            horizontalArrangement = Arrangement.spacedBy(14.dp, Alignment.CenterHorizontally),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            actions.forEach { action ->
                                CircularSpotlightActionButton(action = action)
                            }
                        }
                    }

                    Surface(
                        modifier = Modifier
                            .clip(CircleShape)
                            .appleBouncy(pressedScale = 0.88f, onClick = onDismiss),
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                        shadowElevation = 6.dp
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Kapat",
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "Kapat",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Medium),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CircularSpotlightActionButton(
    action: SpotlightCircularAction
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier.width(62.dp)
    ) {
        Surface(
            modifier = Modifier
                .size(50.dp)
                .clip(CircleShape)
                .appleBouncy(pressedScale = 0.84f, onClick = action.onClick),
            shape = CircleShape,
            color = action.circleColor,
            border = BorderStroke(1.dp, action.borderColor),
            shadowElevation = 4.dp
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = action.icon,
                    contentDescription = action.label,
                    tint = action.iconTint,
                    modifier = Modifier.size(22.dp)
                )
            }
        }
        Text(
            text = action.label,
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.SemiBold,
                fontSize = 11.sp
            ),
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun RichReplyBanner(
    senderName: String,
    text: String,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = rememberLibraCommsColors()
    val bannerSurface = palette.surface
    val accentColor = palette.brandText
    val barColor = palette.brand

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp),
        color = bannerSurface,
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(0.85.dp, palette.hairline)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .width(3.5.dp)
                    .height(30.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(barColor)
            )
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = senderName.ifBlank { "Yanıtlanıyor" },
                    color = accentColor,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.labelMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = text.ifBlank { "📷 Fotoğraf" },
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(
                onClick = onCancel,
                modifier = Modifier.size(30.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "İptal",
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
