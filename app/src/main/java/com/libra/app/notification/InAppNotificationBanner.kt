package com.libra.app.notification

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.libra.app.domain.model.AppNotification
import com.libra.app.domain.model.LibraNotificationTypes
import com.libra.app.ui.animation.AppleSprings
import com.libra.app.ui.animation.appleBouncy
import com.libra.app.ui.components.UserAvatar

@Composable
fun InAppNotificationBanner(
    notification: AppNotification?,
    onOpen: (AppNotification) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = notification != null,
        enter = slideInVertically(animationSpec = AppleSprings.snappy()) { -it } + fadeIn(),
        exit = slideOutVertically(animationSpec = AppleSprings.snappy()) { -it } + fadeOut(),
        modifier = modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 14.dp, vertical = 8.dp)
    ) {
        notification?.let { item ->
            val shape = RoundedCornerShape(18.dp)
            val (badgeIcon, badgeColor) = resolveNotificationIconAndColor(item.type)
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(shape)
                    .appleBouncy(pressedScale = 0.97f, onClick = { onOpen(item) }),
                shape = shape,
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(modifier = Modifier.size(44.dp)) {
                        UserAvatar(
                            photoUrl = item.actorPhotoUrl,
                            initials = item.actorName.take(1).uppercase().ifBlank { "L" },
                            size = 42.dp
                        )
                        Surface(
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .size(18.dp),
                            shape = CircleShape,
                            color = badgeColor,
                            border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.surface)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = badgeIcon,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(10.dp)
                                )
                            }
                        }
                    }

                    Spacer(Modifier.width(10.dp))

                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text(
                            text = item.title.ifBlank { "Yeni Bildirim" },
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontSize = 13.5.sp,
                                lineHeight = 17.sp,
                                fontWeight = FontWeight.Bold
                            ),
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = item.body.ifBlank { "Görmek için dokun" },
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontSize = 12.5.sp,
                                lineHeight = 16.sp
                            ),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Spacer(Modifier.width(6.dp))

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(30.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Kapat",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}

fun resolveNotificationIconAndColor(type: String): Pair<ImageVector, Color> = when (type.uppercase()) {
    LibraNotificationTypes.MESSAGE,
    LibraNotificationTypes.GROUP_MESSAGE,
    LibraNotificationTypes.SERVER_MENTION -> Icons.AutoMirrored.Filled.Chat to Color(0xFF16A34A)

    LibraNotificationTypes.LIKE,
    LibraNotificationTypes.BOOK_LIKE,
    LibraNotificationTypes.STORY_LIKE -> Icons.Default.Favorite to Color(0xFFE11D48)

    LibraNotificationTypes.FOLLOW -> Icons.Default.PersonAdd to Color(0xFF2563EB)

    LibraNotificationTypes.ADMIN_BROADCAST,
    LibraNotificationTypes.ADMIN_FEEDBACK -> Icons.Default.Campaign to Color(0xFF7C3AED)

    else -> Icons.Default.Notifications to Color(0xFF0EA5E9)
}
