package com.libra.app.feature.messages

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.libra.app.core.di.ServiceLocator
import com.libra.app.core.result.AppResult
import com.libra.app.domain.model.GlobalChatMessage
import com.libra.app.domain.model.StorageUploadRequest
import com.libra.app.ui.components.UserAvatar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt

class GlobalChatViewModel : ViewModel() {
    private val repository = ServiceLocator.chatRepository
    private val _messages = MutableStateFlow<List<GlobalChatMessage>>(emptyList())
    val messages = _messages.asStateFlow()
    private val _error = MutableStateFlow<String?>(null)
    val error = _error.asStateFlow()

    init {
        viewModelScope.launch {
            repository.observeGlobalMessages().collect { result ->
                when (result) {
                    is AppResult.Success -> { _messages.value = result.data; _error.value = null }
                    is AppResult.Error -> _error.value = result.error.message
                }
            }
        }
    }

    fun send(text: String, reply: GlobalChatMessage? = null) = viewModelScope.launch {
        when (val result = repository.sendGlobalMessage(text, reply)) {
            is AppResult.Success -> _error.value = null
            is AppResult.Error -> _error.value = result.error.message
        }
    }

    fun sendMedia(url: String, type: String, text: String, reply: GlobalChatMessage? = null) = viewModelScope.launch {
        when (val result = repository.sendGlobalMediaMessage(url, type, text, reply)) {
            is AppResult.Success -> _error.value = null
            is AppResult.Error -> _error.value = result.error.message
        }
    }

    fun edit(id: String, text: String) = viewModelScope.launch {
        when (val result = repository.editGlobalMessage(id, text)) {
            is AppResult.Success -> _error.value = null
            is AppResult.Error -> _error.value = result.error.message
        }
    }

    fun delete(id: String) = viewModelScope.launch {
        when (val result = repository.deleteGlobalMessage(id)) {
            is AppResult.Success -> _error.value = null
            is AppResult.Error -> _error.value = result.error.message
        }
    }

    fun react(id: String, emoji: String) = viewModelScope.launch {
        when (val result = repository.toggleGlobalMessageReaction(id, emoji)) {
            is AppResult.Success -> _error.value = null
            is AppResult.Error -> _error.value = result.error.message
        }
    }
}

@Composable
fun GlobalChatScreen(
    onBack: () -> Unit,
    viewModel: GlobalChatViewModel = viewModel(),
    modifier: Modifier = Modifier
) {
    val messages by viewModel.messages.collectAsState()
    val error by viewModel.error.collectAsState()
    var draft by remember { mutableStateOf("") }
    var replyTarget by remember { mutableStateOf<GlobalChatMessage?>(null) }
    var actionMessage by remember { mutableStateOf<GlobalChatMessage?>(null) }
    var editingMessage by remember { mutableStateOf<GlobalChatMessage?>(null) }
    var pendingUri by remember { mutableStateOf<Uri?>(null) }
    var pendingUrl by remember { mutableStateOf("") }
    var pendingType by remember { mutableStateOf("image/jpeg") }
    var uploading by remember { mutableStateOf(false) }
    var progress by remember { mutableIntStateOf(0) }
    var mediaError by remember { mutableStateOf<String?>(null) }
    val context = androidx.compose.ui.platform.LocalContext.current
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    val currentUid = ServiceLocator.authRepository.currentUser.value?.uid.orEmpty()

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri ?: return@rememberLauncherForActivityResult
        pendingUri = uri
        pendingUrl = ""
        pendingType = context.contentResolver.getType(uri).orEmpty().ifBlank { "image/jpeg" }
        mediaError = null
        uploading = true
        progress = 0
        scope.launch {
            try {
                val bytes = withContext(Dispatchers.IO) { context.contentResolver.openInputStream(uri)?.use { it.readBytes() } }
                    ?: throw IllegalStateException("Fotoğraf okunamadı.")
                if (bytes.size > 8 * 1024 * 1024) throw IllegalStateException("Fotoğraf 8 MB'dan küçük olmalı.")
                val ext = pendingType.substringAfter('/').ifBlank { "jpg" }.take(8)
                val request = StorageUploadRequest(
                    fileName = "global-" + System.currentTimeMillis() + "." + ext,
                    bytes = bytes,
                    contentType = pendingType,
                    targetDirectory = "users/$currentUid"
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

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) listState.animateScrollToItem(messages.lastIndex)
    }

    Column(modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 8.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Geri") }
            Column(Modifier.weight(1f)) {
                Text("Genel Chat", fontWeight = FontWeight.Bold)
                Text("Tüm Libra üyeleri", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        error?.let { Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(12.dp)) }
        mediaError?.let { Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(horizontal = 12.dp)) }

        if (pendingUri != null) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                AsyncImage(pendingUri, "Gönderilecek fotoğraf", Modifier.size(64.dp).clip(RoundedCornerShape(10.dp)), contentScale = ContentScale.Crop)
                Column(Modifier.weight(1f).padding(horizontal = 10.dp)) {
                    Text(if (uploading) "Fotoğraf yükleniyor %$progress" else if (pendingUrl.isNotBlank()) "Fotoğraf hazır ✓" else "Fotoğraf hazırlanıyor…")
                    if (uploading) LinearProgressIndicator({ progress / 100f }, Modifier.fillMaxWidth())
                }
                IconButton(onClick = { pendingUri = null; pendingUrl = ""; mediaError = null }) { Icon(Icons.Default.Close, "Kaldır") }
            }
        }

        LazyColumn(state = listState, modifier = Modifier.weight(1f).fillMaxWidth(), contentPadding = PaddingValues(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
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
                            detectTapGestures(onDoubleTap = { viewModel.react(message.id, "❤️") }, onLongPress = { actionMessage = message })
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
                            if (message.mediaUrl.isNotBlank()) AsyncImage(message.mediaUrl, "Gönderilen fotoğraf", Modifier.width(220.dp).heightIn(max=280.dp).clip(RoundedCornerShape(12.dp)), contentScale = ContentScale.Crop)
                            if (message.text.isNotBlank()) Text(message.text, Modifier.padding(8.dp, 6.dp))
                            if (message.editedAt != null) Text("düzenlendi", Modifier.padding(horizontal=8.dp), style=MaterialTheme.typography.labelSmall)
                            if (message.reactions.isNotEmpty()) Text(message.reactions.values.distinct().joinToString(" "), Modifier.padding(horizontal=8.dp, vertical=2.dp))
                        }
                    }
                }
            }
        }

        replyTarget?.let {
            RichReplyBanner(it.senderName, it.text.ifBlank { "📷 Fotoğraf" }, { replyTarget = null })
        }
        editingMessage?.let {
            RichReplyBanner("Mesaj düzenleniyor", it.text, { editingMessage = null; draft = "" })
        }

        Row(Modifier.fillMaxWidth().navigationBarsPadding().padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(enabled = !uploading && editingMessage == null, onClick = { picker.launch("image/*") }) { Icon(Icons.Default.AddPhotoAlternate, "Fotoğraf") }
            OutlinedTextField(
                value = draft,
                onValueChange = { if (it.length <= 1000) draft = it },
                modifier = Modifier.weight(1f),
                placeholder = { Text(if (editingMessage != null) "Mesajı düzenle…" else "Mesaj yaz…") },
                maxLines = 4
            )
            IconButton(
                enabled = (draft.isNotBlank() || pendingUrl.isNotBlank()) && !uploading,
                onClick = {
                    val edit = editingMessage
                    if (edit != null) { viewModel.edit(edit.id, draft); editingMessage = null }
                    else if (pendingUrl.isNotBlank()) { viewModel.sendMedia(pendingUrl, pendingType, draft, replyTarget); pendingUri=null; pendingUrl=""; replyTarget=null }
                    else { viewModel.send(draft, replyTarget); replyTarget=null }
                    draft=""
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
            onReaction = { emoji -> viewModel.react(message.id, emoji); actionMessage = null },
            onEdit = { editingMessage = message; draft = message.text; actionMessage = null },
            onDelete = { viewModel.delete(message.id); actionMessage = null }
        )
    }
}
