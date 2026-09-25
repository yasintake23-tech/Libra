package com.libra.app.feature.messages

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.IntOffset
import coil.compose.AsyncImage
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.libra.app.core.di.ServiceLocator
import com.libra.app.core.result.AppResult
import com.libra.app.domain.model.CommunityServer
import com.libra.app.domain.model.ServerMessage
import com.libra.app.domain.model.ServerMember
import com.libra.app.domain.model.StorageUploadRequest
import com.libra.app.ui.components.UserAvatar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt

@Composable
fun CommunityServersScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val communityRepository = ServiceLocator.communityRepository
    val scope = rememberCoroutineScope()
    var servers by remember { mutableStateOf<List<CommunityServer>>(emptyList()) }
    var selectedServer by remember { mutableStateOf<CommunityServer?>(null) }
    var showCreate by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        communityRepository.observeCommunityServers().collect { result ->
            when (result) {
                is AppResult.Success -> servers = result.data
                is AppResult.Error -> error = result.error.message
            }
        }
    }

    selectedServer?.let { server ->
        ServerChatScreen(
            server = server,
            onBack = { selectedServer = null },
            modifier = modifier
        )
        return
    }

    Column(modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Geri") }
            Column(Modifier.weight(1f)) {
                Text("Sunucular", style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold))
                Text("Kitap türlerine göre topluluklara katıl.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            IconButton(onClick = { showCreate = true }) {
                Icon(Icons.Default.Add, "Sunucu oluştur")
            }
        }
        Spacer(Modifier.height(12.dp))

        error?.let {
            Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(bottom = 8.dp))
        }

        if (servers.isEmpty()) {
            Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp)) {
                Column(Modifier.fillMaxWidth().padding(28.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.Groups, null, modifier = Modifier.size(48.dp))
                    Spacer(Modifier.height(10.dp))
                    Text("Henüz sunucu yok.", fontWeight = FontWeight.Bold)
                    Text("İlk topluluğu sen oluştur.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(8.dp))
                    Button(onClick = { showCreate = true }) { Text("Sunucu oluştur") }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(bottom = 20.dp)
            ) {
                items(servers, key = { it.id }) { server ->
                    Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
                        Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                Modifier.size(48.dp).clip(RoundedCornerShape(14.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant),
                                contentAlignment = Alignment.Center
                            ) { Icon(Icons.Default.Groups, null) }
                            Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                                Text(server.name, fontWeight = FontWeight.Bold)
                                if (server.description.isNotBlank()) {
                                    Text(server.description, maxLines = 2, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                            IconButton(onClick = {
                                scope.launch {
                                    when (val result = communityRepository.joinCommunityServer(server.id)) {
                                        is AppResult.Success -> {
                                            error = null
                                            selectedServer = server
                                        }
                                        is AppResult.Error -> error = result.error.message
                                    }
                                }
                            }) {
                                Icon(Icons.Default.ChevronRight, "Katıl")
                            }
                        }
                    }
                }
            }
        }
    }

    if (showCreate) {
        CreateServerDialog(
            onDismiss = { showCreate = false },
            onCreate = { name, description ->
                scope.launch {
                    when (val result = communityRepository.createCommunityServer(name, description)) {
                        is AppResult.Success -> {
                            showCreate = false
                            error = null
                            selectedServer = result.data
                        }
                        is AppResult.Error -> error = result.error.message
                    }
                }
            }
        )
    }

}

@Composable
private fun CreateServerDialog(
    onDismiss: () -> Unit,
    onCreate: (String, String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Yeni sunucu") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(name, { if (it.length <= 40) name = it }, label = { Text("Sunucu adı") }, singleLine = true)
                OutlinedTextField(description, { if (it.length <= 160) description = it }, label = { Text("Açıklama") }, minLines = 2, maxLines = 3)
            }
        },
        confirmButton = {
            TextButton(enabled = name.trim().length >= 2, onClick = { onCreate(name, description) }) { Text("Oluştur") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("İptal") } }
    )
}

@Composable
private fun ServerChatScreen(
    server: CommunityServer,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val chatRepository = ServiceLocator.chatRepository
    val communityRepository = ServiceLocator.communityRepository
    val scope = rememberCoroutineScope()
    var messages by remember(server.id) { mutableStateOf<List<ServerMessage>>(emptyList()) }
    var error by remember(server.id) { mutableStateOf<String?>(null) }
    var draft by remember { mutableStateOf("") }
    var replyTarget by remember { mutableStateOf<ServerMessage?>(null) }
    var actionMessage by remember { mutableStateOf<ServerMessage?>(null) }
    var editingMessage by remember { mutableStateOf<ServerMessage?>(null) }
    var pendingUri by remember { mutableStateOf<android.net.Uri?>(null) }
    var pendingUrl by remember { mutableStateOf("") }
    var pendingType by remember { mutableStateOf("image/jpeg") }
    var uploading by remember { mutableStateOf(false) }
    var progress by remember { mutableIntStateOf(0) }
    var mediaError by remember { mutableStateOf<String?>(null) }
    var showMembers by remember { mutableStateOf(false) }
    var showManage by remember { mutableStateOf(false) }
    var actionError by remember(server.id) { mutableStateOf<String?>(null) }
    var serverName by remember(server.id) { mutableStateOf(server.name) }
    var serverDescription by remember(server.id) { mutableStateOf(server.description) }
    var members by remember { mutableStateOf<List<ServerMember>>(emptyList()) }
    val listState = rememberLazyListState()
    val currentUid = ServiceLocator.authRepository.currentUser.value?.uid.orEmpty()
    val context = androidx.compose.ui.platform.LocalContext.current
    val picker = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.GetContent()
    ) { uri ->
        uri ?: return@rememberLauncherForActivityResult
        pendingUri = uri
        pendingUrl = ""
        pendingType = context.contentResolver.getType(uri).orEmpty().ifBlank { "image/jpeg" }
        uploading = true
        progress = 0
        mediaError = null
        scope.launch {
            try {
                val bytes = withContext(Dispatchers.IO) {
                    context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                } ?: throw IllegalStateException("Fotoğraf okunamadı.")
                if (bytes.size > 8 * 1024 * 1024) throw IllegalStateException("Fotoğraf 8 MB'dan küçük olmalı.")
                val ext = pendingType.substringAfter('/').ifBlank { "jpg" }.take(8)
                val request = StorageUploadRequest(
                    fileName = "server-" + System.currentTimeMillis() + "." + ext,
                    bytes = bytes,
                    contentType = pendingType,
                    targetDirectory = "users/" + currentUid
                )
                when (val result = ServiceLocator.storageRepository.uploadMedia(request) { progress = it }.first()) {
                    is AppResult.Success -> { pendingUrl = result.data; progress = 100 }
                    is AppResult.Error -> mediaError = result.error.message
                }
            } catch (e: Exception) {
                mediaError = e.localizedMessage ?: "Fotoğraf yüklenemedi."
            } finally { uploading = false }
        }
    }

    LaunchedEffect(server.id) {
        communityRepository.observeServerMembers(server.id).collect { result ->
            if (result is AppResult.Success) members = result.data
        }
    }

    LaunchedEffect(server.id) {
        chatRepository.observeServerMessages(server.id).collect { result ->
            when (result) {
                is AppResult.Success -> messages = result.data
                is AppResult.Error -> error = result.error.message
            }
        }
    }

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) listState.animateScrollToItem(messages.lastIndex)
    }

    Column(modifier.fillMaxSize()) {
        Row(
            Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Geri") }
            Column(Modifier.weight(1f)) {
                Text(serverName, fontWeight = FontWeight.Bold)
                Text("${members.size} üye • Topluluk sohbeti", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (server.ownerId == currentUid) {
                IconButton(onClick = { showManage = true }) {
                    Icon(Icons.Default.Settings, "Sunucu yönetimi")
                }
            }
            IconButton(onClick = { showMembers = true }) {
                Icon(Icons.Default.People, "Üyeler")
            }
        }

        error?.let { Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(horizontal = 12.dp)) }
        actionError?.let { Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(horizontal = 12.dp)) }
        mediaError?.let { Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(horizontal = 12.dp)) }

        if (pendingUri != null) {
            Row(Modifier.fillMaxWidth().padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                AsyncImage(pendingUri, "Gönderilecek fotoğraf", Modifier.size(64.dp).clip(RoundedCornerShape(10.dp)), contentScale = ContentScale.Crop)
                Column(Modifier.weight(1f).padding(horizontal = 10.dp)) {
                    Text(if (uploading) "Fotoğraf yükleniyor %" + progress else if (pendingUrl.isNotBlank()) "Fotoğraf hazır ✓" else "Fotoğraf hazırlanıyor…")
                    if (uploading) LinearProgressIndicator({ progress / 100f }, Modifier.fillMaxWidth())
                }
                IconButton(onClick = { pendingUri = null; pendingUrl = ""; mediaError = null }) { Icon(Icons.Default.Close, "Kaldır") }
            }
        }

        LazyColumn(
            state = listState,
            modifier = Modifier.weight(1f).fillMaxWidth(),
            contentPadding = PaddingValues(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(messages, key = { it.id }) { message ->
                var dragX by remember(message.id) { mutableFloatStateOf(0f) }
                Row(
                    Modifier.fillMaxWidth()
                        .pointerInput(message.id + "-swipe") {
                            detectHorizontalDragGestures(
                                onHorizontalDrag = { _, amount -> dragX = (dragX + amount).coerceIn(0f, 96f) },
                                onDragEnd = { if (dragX >= 64f) replyTarget = message; dragX = 0f }
                            )
                        }
                        .offset { IntOffset(dragX.roundToInt(), 0) },
                    horizontalArrangement = if (message.senderId == currentUid) Arrangement.End else Arrangement.Start,
                    verticalAlignment = Alignment.Bottom
                ) {
                    if (message.senderId != currentUid) {
                        UserAvatar(message.senderPhotoUrl, message.senderName.take(1).uppercase(), size = 30.dp)
                        Spacer(Modifier.width(6.dp))
                    }
                    Surface(
                        color = if (message.senderId == currentUid) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.pointerInput(message.id + "-tap") {
                            detectTapGestures(
                                onDoubleTap = { scope.launch { chatRepository.toggleServerMessageReaction(server.id, message.id, "❤️") } },
                                onLongPress = { actionMessage = message }
                            )
                        }
                    ) {
                        Column(Modifier.widthIn(max = 320.dp).padding(6.dp)) {
                            if (message.senderId != currentUid) Text(message.senderName, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, modifier = Modifier.padding(8.dp, 4.dp))
                            if (message.replyToMessageId.isNotBlank()) {
                                Surface(color = androidx.compose.ui.graphics.Color(0xFFFFE8D5), shape = RoundedCornerShape(10.dp), modifier = Modifier.fillMaxWidth()) {
                                    Column(Modifier.padding(8.dp)) {
                                        Text(message.replyToSenderName.ifBlank { "Yanıtlanan mesaj" }, color = androidx.compose.ui.graphics.Color(0xFFB45309), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelSmall)
                                        Text(message.replyToText.ifBlank { "📷 Fotoğraf" }, maxLines = 2, style = MaterialTheme.typography.bodySmall)
                                    }
                                }
                                Spacer(Modifier.height(5.dp))
                            }
                            message.sharedContent?.let { shared ->
                                Surface(
                                    modifier = Modifier.fillMaxWidth().padding(4.dp),
                                    shape = RoundedCornerShape(10.dp),
                                    color = MaterialTheme.colorScheme.background.copy(alpha = 0.45f)
                                ) {
                                    Column(Modifier.padding(8.dp)) {
                                        Text(
                                            when (shared.type) {
                                                "story" -> "Hikâyeden bahsetti"
                                                "post" -> "Bir gönderi paylaştı"
                                                "book" -> "Bir kitap paylaştı"
                                                else -> "Bir içerik paylaştı"
                                            },
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(shared.title.ifBlank { "Libra içeriği" }, fontWeight = FontWeight.SemiBold)
                                        if (shared.text.isNotBlank()) Text(shared.text, maxLines = 2, style = MaterialTheme.typography.bodySmall)
                                    }
                                }
                            }

                            if (message.mediaUrl.isNotBlank()) AsyncImage(ServiceLocator.storageRepository.getPublicCdnUrl(message.mediaUrl), "Gönderilen fotoğraf", Modifier.width(220.dp).heightIn(max = 280.dp).clip(RoundedCornerShape(12.dp)), contentScale = ContentScale.Crop)
                            if (message.text.isNotBlank()) Text(message.text, Modifier.padding(8.dp, 6.dp))
                            if (message.editedAt != null) Text("düzenlendi", Modifier.padding(horizontal = 8.dp), style = MaterialTheme.typography.labelSmall)
                            if (message.reactions.isNotEmpty()) Text(message.reactions.values.distinct().joinToString(" "), Modifier.padding(horizontal = 8.dp, vertical = 2.dp))
                        }
                    }
                }
            }
        }

        replyTarget?.let { RichReplyBanner(it.senderName, it.text.ifBlank { "📷 Fotoğraf" }, { replyTarget = null }) }
        editingMessage?.let { RichReplyBanner("Mesaj düzenleniyor", it.text, { editingMessage = null; draft = "" }) }

        Row(
            Modifier.fillMaxWidth().navigationBarsPadding().padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(enabled = !uploading && editingMessage == null, onClick = { picker.launch("image/*") }) {
                Icon(Icons.Default.AddPhotoAlternate, "Fotoğraf")
            }
            OutlinedTextField(
                value = draft,
                onValueChange = { if (it.length <= 1000) draft = it },
                modifier = Modifier.weight(1f),
                placeholder = { Text("Topluluğa mesaj yaz…") },
                maxLines = 4
            )
            IconButton(
                enabled = (draft.isNotBlank() || pendingUrl.isNotBlank()) && !uploading,
                onClick = {
                    val text = draft.trim()
                    val edit = editingMessage
                    if (edit != null) {
                        scope.launch { chatRepository.editServerMessage(server.id, edit.id, text) }
                        editingMessage = null
                    } else if (pendingUrl.isNotBlank()) {
                        scope.launch { chatRepository.sendServerMediaMessage(server.id, pendingUrl, pendingType, text, replyTarget) }
                        pendingUri = null
                        pendingUrl = ""
                        replyTarget = null
                    } else {
                        scope.launch { chatRepository.sendServerMessage(server.id, text, replyTarget) }
                        replyTarget = null
                    }
                    draft = ""
                }
            ) { Icon(Icons.Default.Send, "Gönder") }
        }
    }
    actionMessage?.let { message ->
        RichMessageActionSheet(
            canEdit = message.senderId == currentUid && message.mediaUrl.isBlank(),
            canDelete = message.senderId == currentUid,
            onDismiss = { actionMessage = null },
            onReply = { replyTarget = message; actionMessage = null },
            onReaction = { emoji -> scope.launch { chatRepository.toggleServerMessageReaction(server.id, message.id, emoji) }; actionMessage = null },
            onEdit = { editingMessage = message; draft = message.text; actionMessage = null },
            onDelete = { scope.launch { chatRepository.deleteServerMessage(server.id, message.id) }; actionMessage = null }
        )
    }

    if (showManage && server.ownerId == currentUid) {
        AlertDialog(
            onDismissRequest = { showManage = false },
            title = { Text("Sunucu yönetimi") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = serverName,
                        onValueChange = { if (it.length <= 40) serverName = it },
                        label = { Text("Sunucu adı") },
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = serverDescription,
                        onValueChange = { if (it.length <= 160) serverDescription = it },
                        label = { Text("Açıklama") },
                        minLines = 2,
                        maxLines = 4
                    )
                    Text(
                        "Üyeler ekranından Admin rolü verebilir, üyeleri çıkarabilir veya rollerini değiştirebilirsin.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    actionError?.let {
                        Text(it, color = MaterialTheme.colorScheme.error)
                    }
                }
            },
            confirmButton = {
                TextButton(
                    enabled = serverName.trim().length >= 2,
                    onClick = {
                        scope.launch {
                            when (val result = communityRepository.updateCommunityServer(server.id, serverName, serverDescription)) {
                                is AppResult.Success -> {
                                    actionError = null
                                    showManage = false
                                }
                                is AppResult.Error -> actionError = result.error.message
                            }
                        }
                    }
                ) { Text("Kaydet") }
            },
            dismissButton = {
                TextButton(onClick = { showManage = false }) { Text("İptal") }
            }
        )
    }

    if (showMembers) {
        AlertDialog(
            onDismissRequest = { showMembers = false },
            title = { Text("Üyeler (${members.size})") },
            text = {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(members, key = { it.uid }) { member ->
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            UserAvatar(member.photoUrl, member.displayName.take(1).uppercase(), size = 38.dp)
                            Spacer(Modifier.width(8.dp))
                            Column(Modifier.weight(1f)) {
                                Text(member.displayName.ifBlank { member.uid }, fontWeight = FontWeight.Bold)
                                Text(member.role, style = MaterialTheme.typography.labelSmall)
                            }
                            if (server.ownerId == currentUid && member.uid != currentUid) {
                                TextButton(onClick = {
                                    scope.launch {
                                        communityRepository.setServerMemberRole(server.id, member.uid, if (member.role == "ADMIN") "MEMBER" else "ADMIN")
                                    }
                                }) { Text(if (member.role == "ADMIN") "Üyeye indir" else "Admin yap") }
                                TextButton(onClick = {
                                    scope.launch { communityRepository.removeServerMember(server.id, member.uid) }
                                }) { Text("Çıkar") }
                            }
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showMembers = false }) { Text("Kapat") } }
        )
    }
}