package com.libra.app.feature.share

import android.content.Intent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.libra.app.core.di.ServiceLocator
import com.libra.app.core.result.AppResult
import com.libra.app.domain.model.SharedContent
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

private data class ShareTarget(val key: String, val title: String, val subtitle: String)
private const val LIBRA_WEB_BASE_URL = "https://libra-3bfb9.web.app"

fun sharedContentUrl(type: String, id: String) = "$LIBRA_WEB_BASE_URL/share/$type/$id"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShareSheet(content: SharedContent, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val selected = remember { mutableStateListOf<String>() }
    var targets by remember { mutableStateOf<List<ShareTarget>>(emptyList()) }
    var status by remember { mutableStateOf("") }
    var sending by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        val uid = ServiceLocator.authRepository.currentUser.value?.uid.orEmpty()
        ServiceLocator.chatRepository.observeDirectConversations(uid).collectLatest { r ->
            if (r is AppResult.Success) targets = targets.filter { !it.key.startsWith("dm:") } + r.data.map { ShareTarget("dm:${it.otherUserId}", it.otherUserName, "@${it.otherUserUsername}") }
        }
    }
    LaunchedEffect(Unit) {
        ServiceLocator.communityRepository.observeCommunityServers().collectLatest { r ->
            if (r is AppResult.Success) targets = targets.filter { !it.key.startsWith("server:") } + r.data.map { ShareTarget("server:${it.id}", it.name, "Sunucu") }
        }
    }
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.fillMaxWidth().navigationBarsPadding().padding(18.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("Paylaş", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.weight(1f))
                IconButton(onClick = onDismiss) { Icon(Icons.Default.Close, "Kapat") }
            }
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(14.dp)) {
                    Text(content.title.ifBlank { "Libra içeriği" }, fontWeight = FontWeight.Bold)
                    if (content.text.isNotBlank()) Text(content.text, maxLines = 2)
                    Text(content.url, style = MaterialTheme.typography.labelSmall, maxLines = 1)
                }
            }
            Spacer(Modifier.height(10.dp))
            ShareTargetRow(ShareTarget("global", "Genel Chat", "Herkese gönder"), "global" in selected) { toggle(selected, "global") }
            Text("Kişiler ve sunucular", style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(vertical = 8.dp))
            LazyColumn(Modifier.heightIn(max = 280.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                items(targets, key = { it.key }) { t -> ShareTargetRow(t, t.key in selected) { toggle(selected, t.key) } }
            }
            Spacer(Modifier.height(8.dp))
            Button(enabled = selected.isNotEmpty() && !sending, onClick = {
                sending = true
                scope.launch {
                    var sent = 0
                    selected.toList().forEach { key ->
                        val result = when {
                            key == "global" -> ServiceLocator.chatRepository.sendGlobalMessage("Libra'da bir içerik paylaştı.", sharedContent = content)
                            key.startsWith("dm:") -> ServiceLocator.chatRepository.sendDirectMessage(key.removePrefix("dm:"), "Libra'da bir içerik paylaştı.", sharedContent = content)
                            else -> ServiceLocator.chatRepository.sendServerMessage(key.removePrefix("server:"), "Libra'da bir içerik paylaştı.", sharedContent = content)
                        }
                        if (result is AppResult.Success) sent++
                    }
                    status = "$sent hedefe gönderildi."
                    selected.clear()
                    sending = false
                }
            }, modifier = Modifier.fillMaxWidth()) { Icon(Icons.Default.Send, null); Spacer(Modifier.width(6.dp)); Text("Seçilenlere gönder") }
            Button(onClick = {
                val intent = Intent(Intent.ACTION_SEND).apply { type = "text/plain"; putExtra(Intent.EXTRA_TEXT, content.title + "\n" + content.url) }
                context.startActivity(Intent.createChooser(intent, "Libra içeriğini paylaş"))
            }, modifier = Modifier.fillMaxWidth()) { Icon(Icons.Default.Share, null); Spacer(Modifier.width(6.dp)); Text("Dışarı paylaş") }
            if (status.isNotBlank()) Text(status, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(8.dp))
        }
    }
}

@Composable
private fun ShareTargetRow(target: ShareTarget, selected: Boolean, onClick: () -> Unit) {
    Surface(Modifier.fillMaxWidth().clickable(onClick = onClick), shape = androidx.compose.foundation.shape.RoundedCornerShape(14.dp), color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(if (target.key == "global") Icons.Default.Public else Icons.Default.Send, null)
            Column(Modifier.weight(1f).padding(start = 12.dp)) { Text(target.title, fontWeight = FontWeight.SemiBold); Text(target.subtitle, style = MaterialTheme.typography.labelSmall) }
            if (selected) Icon(Icons.Default.CheckCircle, null)
        }
    }
}
private fun toggle(selected: MutableList<String>, key: String) { if (key in selected) selected.remove(key) else selected.add(key) }