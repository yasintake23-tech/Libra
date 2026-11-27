package com.libra.app.feature.messages

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.libra.app.core.di.ServiceLocator
import com.libra.app.core.result.AppResult
import com.libra.app.domain.model.DirectMessage
import com.libra.app.domain.model.UserProfile
import com.libra.app.ui.components.FullScreenImageViewerDialog
import com.libra.app.ui.components.UserAvatar

/**
 * Clean, professional 1-to-1 Chat About screen ("Sohbet Hakkında").
 * Shows live user profile info, shared media grid from real chat messages, shared content,
 * direct profile jump, and chat reset action.
 */
@Composable
fun DirectChatAboutSheet(
    user: UserProfile,
    messages: List<DirectMessage>,
    onDismiss: () -> Unit,
    onOpenProfile: () -> Unit,
    onClearConversation: ((Boolean) -> Unit) -> Unit
) {
    val palette = rememberLibraCommsColors()
    var liveUser by remember(user.uid) { mutableStateOf(user) }
    var confirmClear by remember { mutableStateOf(false) }
    var fullscreenImageKey by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(user.uid) {
        if (user.uid.isNotBlank()) {
            val res = ServiceLocator.userRepository.getUserProfileFresh(user.uid)
            if (res is AppResult.Success && res.data != null) {
                liveUser = res.data
            }
        }
    }

    val sharedMedia = remember(messages) {
        messages.filter { it.mediaUrl.isNotBlank() }.reversed()
    }
    val sharedItems = remember(messages) {
        messages.mapNotNull { msg -> msg.sharedContent?.let { msg to it } }.reversed()
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = palette.canvas
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .navigationBarsPadding()
            ) {
                // Header
                Surface(modifier = Modifier.fillMaxWidth(), color = palette.surface) {
                    Column {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .statusBarsPadding()
                                .padding(horizontal = 8.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(onClick = onDismiss) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, "Geri", tint = palette.inkPrimary)
                            }
                            Text(
                                text = "Sohbet Hakkında",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = palette.inkPrimary,
                                modifier = Modifier.weight(1f)
                            )
                        }
                        HorizontalDivider(color = palette.hairline, thickness = 0.8.dp)
                    }
                }

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 18.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Profile Summary Block
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        UserAvatar(
                            photoUrl = liveUser.profileImageUrl,
                            initials = liveUser.initials,
                            size = 84.dp,
                            onClick = {
                                onDismiss()
                                onOpenProfile()
                            }
                        )
                        Text(
                            text = liveUser.displayName.ifBlank { "Libra Üyesi" },
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = palette.inkPrimary
                        )
                        if (liveUser.username.isNotBlank()) {
                            Text(
                                text = "@${liveUser.username}",
                                style = MaterialTheme.typography.bodySmall,
                                color = palette.inkSecondary
                            )
                        }
                        if (liveUser.bio.isNotBlank()) {
                            Text(
                                text = liveUser.bio,
                                style = MaterialTheme.typography.bodySmall,
                                color = palette.inkSecondary,
                                maxLines = 3,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.padding(horizontal = 16.dp)
                            )
                        }
                    }

                    // Metrics Strip
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ChatAboutStatTile(messages.size.toString(), "Mesaj", Modifier.weight(1f))
                        ChatAboutStatTile(sharedMedia.size.toString(), "Fotoğraf", Modifier.weight(1f))
                        ChatAboutStatTile(sharedItems.size.toString(), "Paylaşım", Modifier.weight(1f))
                    }

                    // Actions Group
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        color = palette.surface,
                        border = BorderStroke(0.85.dp, palette.hairline)
                    ) {
                        Column {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        onDismiss()
                                        onOpenProfile()
                                    }
                                    .padding(horizontal = 14.dp, vertical = 13.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Person, null, tint = palette.brand, modifier = Modifier.size(20.dp))
                                Spacer(Modifier.width(12.dp))
                                Text(
                                    text = "Profili Görüntüle",
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                    color = palette.inkPrimary,
                                    modifier = Modifier.weight(1f)
                                )
                                Icon(Icons.Default.ChevronRight, null, tint = palette.inkSecondary, modifier = Modifier.size(18.dp))
                            }
                            HorizontalDivider(color = palette.hairline, thickness = 0.7.dp, modifier = Modifier.padding(start = 46.dp))
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { confirmClear = true }
                                    .padding(horizontal = 14.dp, vertical = 13.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.DeleteOutline, null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(20.dp))
                                Spacer(Modifier.width(12.dp))
                                Text(
                                    text = "Sohbet Geçmişini Sıfırla",
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                    color = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }

                    // Shared Photos Gallery
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(Icons.Default.PhotoLibrary, null, tint = palette.inkSecondary, modifier = Modifier.size(17.dp))
                            Text(
                                text = "Paylaşılan Fotoğraflar (${sharedMedia.size})",
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                color = palette.inkPrimary
                            )
                        }
                        if (sharedMedia.isEmpty()) {
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(14.dp),
                                color = palette.surface,
                                border = BorderStroke(0.85.dp, palette.hairline)
                            ) {
                                Text(
                                    text = "Bu sohbette henüz fotoğraf paylaşılmadı.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = palette.inkSecondary,
                                    modifier = Modifier.padding(14.dp)
                                )
                            }
                        } else {
                            val rows = remember(sharedMedia) { sharedMedia.take(18).chunked(3) }
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                rows.forEach { rowItems ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        rowItems.forEach { msg ->
                                            LibraSignedImage(
                                                mediaKeyOrUrl = msg.mediaUrl,
                                                contentDescription = "Paylaşılan fotoğraf",
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .aspectRatio(1f)
                                                    .clip(RoundedCornerShape(10.dp))
                                                    .background(palette.recessed)
                                                    .clickable { fullscreenImageKey = msg.mediaUrl }
                                            )
                                        }
                                        repeat(3 - rowItems.size) {
                                            Spacer(Modifier.weight(1f))
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Shared Content (Books / Posts / Stories)
                    if (sharedItems.isNotEmpty()) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(Icons.Default.Share, null, tint = palette.inkSecondary, modifier = Modifier.size(17.dp))
                                Text(
                                    text = "Paylaşılan İçerikler (${sharedItems.size})",
                                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                    color = palette.inkPrimary
                                )
                            }
                            sharedItems.take(10).forEach { (_, shared) ->
                                Surface(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp),
                                    color = palette.surface,
                                    border = BorderStroke(0.85.dp, palette.hairline)
                                ) {
                                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                        Text(
                                            text = when (shared.type) {
                                                "book" -> "Kitap"
                                                "post" -> "Gönderi"
                                                "story" -> "Hikâye"
                                                else -> "Paylaşım"
                                            },
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                            color = palette.brandText
                                        )
                                        if (shared.title.isNotBlank()) {
                                            Text(
                                                text = shared.title,
                                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                                color = palette.inkPrimary,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                        if (shared.text.isNotBlank()) {
                                            Text(
                                                text = shared.text,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = palette.inkSecondary,
                                                maxLines = 2,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    fullscreenImageKey?.let { key ->
        var resolved by remember(key) { mutableStateOf<String?>(null) }
        LaunchedEffect(key) {
            resolved = runCatching { ServiceLocator.storageRepository.getSignedMediaUrl(key) }.getOrNull()
                ?: runCatching { ServiceLocator.storageRepository.getPublicCdnUrl(key) }.getOrNull()
        }
        resolved?.let { url ->
            FullScreenImageViewerDialog(mediaUrl = url, onDismiss = { fullscreenImageKey = null })
        }
    }

    if (confirmClear) {
        AlertDialog(
            onDismissRequest = { confirmClear = false },
            containerColor = palette.surface,
            title = { Text("Sohbeti Sıfırla", fontWeight = FontWeight.Bold) },
            text = { Text("Bu sohbetteki tüm mesajların ve medyaların kalıcı olarak silinecek.") },
            confirmButton = {
                Button(
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    onClick = {
                        confirmClear = false
                        onClearConversation {
                            onDismiss()
                        }
                    }
                ) { Text("Sıfırla") }
            },
            dismissButton = {
                TextButton(onClick = { confirmClear = false }) { Text("Vazgeç") }
            }
        )
    }
}

@Composable
private fun ChatAboutStatTile(value: String, label: String, modifier: Modifier = Modifier) {
    val palette = rememberLibraCommsColors()
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = palette.surface,
        border = BorderStroke(0.85.dp, palette.hairline)
    ) {
        Column(
            modifier = Modifier.padding(vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, fontSize = 16.sp),
                color = palette.inkPrimary
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = palette.inkSecondary
            )
        }
    }
}
