package com.libra.app.feature.messages

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

val LibraReactionEmojis = listOf("❤️", "😂", "😮", "😢", "😡", "👍")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RichMessageActionSheet(
    canEdit: Boolean,
    canDelete: Boolean,
    onDismiss: () -> Unit,
    onReply: () -> Unit,
    onReaction: (String) -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text("Mesaj işlemleri", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            TextButton(onClick = onReply, modifier = Modifier.fillMaxWidth()) { Text("↩ Yanıtla") }
            HorizontalDivider()
            Text("Tepki", style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(top = 4.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                LibraReactionEmojis.forEach { emoji ->
                    TextButton(onClick = { onReaction(emoji) }) { Text(emoji) }
                }
            }
            if (canEdit) TextButton(onClick = onEdit, modifier = Modifier.fillMaxWidth()) { Text("Düzenle") }
            if (canDelete) TextButton(onClick = onDelete, modifier = Modifier.fillMaxWidth()) { Text("Sil", color = MaterialTheme.colorScheme.error) }
            TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) { Text("Kapat") }
        }
    }
}

@Composable
fun RichReplyBanner(
    senderName: String,
    text: String,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    androidx.compose.material3.Surface(
        modifier = modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 2.dp),
        color = androidx.compose.ui.graphics.Color(0xFFFFF3E8),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp)
    ) {
        Row(Modifier.fillMaxWidth().padding(9.dp)) {
            Column(Modifier.weight(1f)) {
                Text("Yanıtlanıyor: $senderName", color = androidx.compose.ui.graphics.Color(0xFFB45309), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)
                Text(text.ifBlank { "📷 Fotoğraf" }, maxLines = 1, style = MaterialTheme.typography.bodySmall)
            }
            TextButton(onClick = onCancel) { Text("İptal") }
        }
    }
}
