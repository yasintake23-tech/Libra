package com.libra.app.feature.messages

import androidx.compose.foundation.background
import coil.compose.AsyncImage
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.IntOffset
import kotlin.math.roundToInt
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.libra.app.core.di.ServiceLocator
import com.libra.app.core.result.AppResult
import com.libra.app.domain.model.DirectConversation
import com.libra.app.domain.model.DirectMessage
import com.libra.app.domain.model.StorageUploadRequest
import com.libra.app.domain.model.UserProfile
import com.libra.app.ui.components.UserAvatar
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

class DirectMessagesViewModel : ViewModel() {
    private val repository = ServiceLocator.chatRepository
    private val auth = ServiceLocator.authRepository
    private val _conversations = MutableStateFlow<List<DirectConversation>>(emptyList())
    val conversations = _conversations.asStateFlow()
    private val _messages = MutableStateFlow<List<DirectMessage>>(emptyList())
    val messages = _messages.asStateFlow()
    private val _error = MutableStateFlow<String?>(null)
    val error = _error.asStateFlow()
    private var conversationJob: kotlinx.coroutines.Job? = null

    init {
        val uid = auth.currentUser.value?.uid.orEmpty()
        if (uid.isNotBlank()) viewModelScope.launch {
            repository.observeDirectConversations(uid).collect { result ->
                when (result) {
                    is AppResult.Success -> _conversations.value = result.data
                    is AppResult.Error -> _error.value = result.error.message
                }
            }
        }
    }

    fun openConversation(user: UserProfile) {
        val ids = listOf(auth.currentUser.value?.uid.orEmpty(), user.uid).sorted()
        if (ids.any { it.isBlank() }) return
        observeConversation(ids.joinToString("_"))
    }

    fun openConversation(conversation: DirectConversation) {
        observeConversation(conversation.id)
        markRead(conversation.id)
    }

    fun markRead(conversationId: String) {
        viewModelScope.launch { repository.markDirectConversationRead(conversationId) }
    }

    private fun observeConversation(id: String) {
        conversationJob?.cancel()
        conversationJob = viewModelScope.launch {
            repository.observeDirectMessages(id).collect { result ->
                when (result) {
                    is AppResult.Success -> { _messages.value = result.data; _error.value = null }
                    is AppResult.Error -> _error.value = result.error.message
                }
            }
        }
    }

    fun send(recipientId: String, text: String, replyTo: DirectMessage? = null) {
        viewModelScope.launch {
            when (val result = repository.sendDirectMessage(recipientId, text, replyTo)) {
                is AppResult.Success -> _error.value = null
                is AppResult.Error -> _error.value = result.error.message
            }
        }
    }

    fun sendMedia(recipientId: String, mediaUrl: String, mediaType: String, text: String = "", replyTo: DirectMessage? = null) {
        viewModelScope.launch {
            when (val result = repository.sendDirectMediaMessage(recipientId, mediaUrl, mediaType, text, replyTo)) {
                is AppResult.Success -> _error.value = null
                is AppResult.Error -> _error.value = result.error.message
            }
        }
    }

    fun edit(conversationId: String, messageId: String, text: String) {
        viewModelScope.launch {
            when (val result = repository.editDirectMessage(conversationId, messageId, text)) {
                is AppResult.Success -> _error.value = null
                is AppResult.Error -> _error.value = result.error.message
            }
        }
    }

    fun delete(conversationId: String, messageId: String) {
        viewModelScope.launch {
            when (val result = repository.deleteDirectMessage(conversationId, messageId)) {
                is AppResult.Success -> _error.value = null
                is AppResult.Error -> _error.value = result.error.message
            }
        }
    }

    fun react(conversationId: String, messageId: String, emoji: String) {
        viewModelScope.launch {
            when (val result = repository.toggleDirectMessageReaction(conversationId, messageId, emoji)) {
                is AppResult.Success -> _error.value = null
                is AppResult.Error -> _error.value = result.error.message
            }
        }
    }
}

private fun authUserId(): String = ServiceLocator.authRepository.currentUser.value?.uid.orEmpty()

private enum class MessageSection { MESSAGES, COMMUNITIES }

@Composable
fun DirectMessagesScreen(
    onFindFriends: () -> Unit,
    onGlobalChatClick: () -> Unit,
    onServersClick: () -> Unit,
    initialUser: UserProfile? = null,
    onInitialUserConsumed: () -> Unit = {},
    modifier: Modifier = Modifier,
    viewModel: DirectMessagesViewModel = viewModel()
) {
    var section by remember { mutableStateOf(MessageSection.MESSAGES) }
    var selectedUser by remember { mutableStateOf<UserProfile?>(null) }
    val conversations by viewModel.conversations.collectAsState()
    val messages by viewModel.messages.collectAsState()
    val error by viewModel.error.collectAsState()

    LaunchedEffect(initialUser?.uid) {
        initialUser?.let {
            selectedUser = it
            viewModel.openConversation(it)
            viewModel.markRead(listOf(authUserId(), it.uid).sorted().joinToString("_"))
            onInitialUserConsumed()
        }
    }

    selectedUser?.let { user ->
        DirectConversationScreen(
            user = user,
            messages = messages,
            error = error,
            onBack = { selectedUser = null },
            onSend = { text, reply -> viewModel.send(user.uid, text, reply) },
            onSendMedia = { url, type, text, reply -> viewModel.sendMedia(user.uid, url, type, text, reply) },
            onEdit = { id, text -> viewModel.edit(listOf(authUserId(), user.uid).sorted().joinToString("_"), id, text) },
            onDelete = { id -> viewModel.delete(listOf(authUserId(), user.uid).sorted().joinToString("_"), id) },
            onReaction = { id, emoji -> viewModel.react(listOf(authUserId(), user.uid).sorted().joinToString("_"), id, emoji) },
            modifier = modifier
        )
        return
    }

    Column(modifier = modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 12.dp)) {
        Text("DM", style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold))
        Text("Arkadaşlarınla konuş, topluluklara katıl.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(14.dp))

        Row(
            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(MaterialTheme.colorScheme.surface).padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            SectionButton("Mesajlar", section == MessageSection.MESSAGES, { section = MessageSection.MESSAGES }, Modifier.weight(1f))
            SectionButton("Topluluklar", section == MessageSection.COMMUNITIES, { section = MessageSection.COMMUNITIES }, Modifier.weight(1f))
        }
        Spacer(Modifier.height(12.dp))

        when (section) {
            MessageSection.MESSAGES -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    item { Text("Sohbetlerin", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)) }
                    if (conversations.isEmpty()) {
                        item {
                            Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
                                Column(Modifier.fillMaxWidth().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(Icons.Default.ChatBubbleOutline, null, modifier = Modifier.size(42.dp))
                                    Spacer(Modifier.height(10.dp))
                                    Text("Henüz bir sohbetin yok.", fontWeight = FontWeight.Bold)
                                    Text("Birinin profiline girip Mesaj'a dokun.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    TextButton(onClick = onFindFriends) {
                                        Icon(Icons.Default.PersonAdd, null, modifier = Modifier.size(18.dp))
                                        Text("  Arkadaş bul")
                                    }
                                }
                            }
                        }
                    } else {
                        items(conversations, key = { it.id }) { conversation ->
                            Card(
                                modifier = Modifier.fillMaxWidth().clickable {
                                    selectedUser = UserProfile(
                                        uid = conversation.otherUserId,
                                        displayName = conversation.otherUserName,
                                        username = conversation.otherUserUsername,
                                        profileImageUrl = conversation.otherUserPhotoUrl
                                    )
                                    viewModel.openConversation(conversation)
                                },
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                    UserAvatar(conversation.otherUserPhotoUrl, conversation.otherUserName.take(1).uppercase(), size = 48.dp)
                                    Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                                        Text(conversation.otherUserName, fontWeight = FontWeight.Bold)
                                        Text("@"+conversation.otherUserUsername, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        if (conversation.lastMessage.isNotBlank()) Text(conversation.lastMessage, maxLines = 1, style = MaterialTheme.typography.bodySmall)
                                    }
                                    if (conversation.unreadCount > 0) {
                                        Surface(
                                            shape = CircleShape,
                                            color = MaterialTheme.colorScheme.primary
                                        ) {
                                            Text(
                                                conversation.unreadCount.coerceAtMost(99).toString(),
                                                color = MaterialTheme.colorScheme.onPrimary,
                                                style = MaterialTheme.typography.labelSmall,
                                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 4.dp)
                                            )
                                        }
                                        Spacer(Modifier.width(6.dp))
                                    }
                                    Icon(Icons.Default.ChevronRight, null)
                                }
                            }
                        }
                    }
                }
            }
            MessageSection.COMMUNITIES -> CommunityList(onGlobalChatClick, onServersClick)
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun DirectConversationScreen(
    user: UserProfile,
    messages: List<DirectMessage>,
    error: String?,
    onBack: () -> Unit,
    onSend: (String, DirectMessage?) -> Unit,
    onSendMedia: (String, String, String, DirectMessage?) -> Unit,
    onEdit: (String, String) -> Unit,
    onDelete: (String) -> Unit,
    onReaction: (String, String) -> Unit,
    modifier: Modifier = Modifier
) {
    var draft by remember { mutableStateOf("") }
    var replyTarget by remember { mutableStateOf<DirectMessage?>(null) }
    var editingMessage by remember { mutableStateOf<DirectMessage?>(null) }
    var actionMessage by remember { mutableStateOf<DirectMessage?>(null) }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val context = androidx.compose.ui.platform.LocalContext.current
    var mediaUploading by remember { mutableStateOf(false) }
    var mediaProgress by remember { mutableIntStateOf(0) }
    var mediaError by remember { mutableStateOf<String?>(null) }
    var pendingMediaUri by remember { mutableStateOf<Uri?>(null) }
    var pendingMediaUrl by remember { mutableStateOf("") }
    var pendingMediaType by remember { mutableStateOf("image/jpeg") }
    var mediaUploadJob by remember { mutableStateOf<kotlinx.coroutines.Job?>(null) }
    val conversationId = remember(user.uid) { listOf(authUserId(), user.uid).sorted().joinToString("_") }
    val currentUid = authUserId()

    val mediaLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri ?: return@rememberLauncherForActivityResult
        mediaUploadJob?.cancel()
        pendingMediaUri = uri
        pendingMediaUrl = ""
        mediaProgress = 0
        mediaError = null
        mediaUploading = true
        pendingMediaType = context.contentResolver.getType(uri).orEmpty().ifBlank { "image/jpeg" }
        mediaUploadJob = scope.launch {
            try {
                val bytes = withContext(kotlinx.coroutines.Dispatchers.IO) {
                    context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                } ?: throw IllegalStateException("Fotoğraf okunamadı.")
                if (bytes.isEmpty()) throw IllegalStateException("Fotoğraf boş.")
                if (bytes.size > 8 * 1024 * 1024) throw IllegalStateException("Fotoğraf 8 MB'dan küçük olmalı.")
                val extension = pendingMediaType.substringAfter('/').ifBlank { "jpg" }.take(8)
                val upload = StorageUploadRequest(
                    fileName = "dm-" + System.currentTimeMillis() + "." + extension,
                    bytes = bytes,
                    contentType = pendingMediaType,
                    targetDirectory = "users/" + currentUid
                )
                when (val result = ServiceLocator.storageRepository.uploadMedia(upload) { mediaProgress = it }.first()) {
                    is AppResult.Success -> {
                        pendingMediaUrl = result.data
                        mediaProgress = 100
                    }
                    is AppResult.Error -> mediaError = result.error.message
                }
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Exception) {
                mediaError = e.localizedMessage ?: "Fotoğraf yüklenemedi."
            } finally {
                mediaUploading = false
            }
        }
    }

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) listState.animateScrollToItem(messages.lastIndex)
    }

    Column(modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Geri") }
            UserAvatar(user.profileImageUrl, user.initials, size = 40.dp)
            Column(Modifier.padding(start = 10.dp)) {
                Text(user.displayName, fontWeight = FontWeight.Bold)
                Text(user.handle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        if (error != null) Text(error, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(12.dp))
        mediaError?.let { Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)) }

        if (pendingMediaUri != null) {
            Surface(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 4.dp),
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surfaceVariant
            ) {
                Row(Modifier.fillMaxWidth().padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    AsyncImage(
                        model = pendingMediaUri,
                        contentDescription = "Gönderilecek fotoğraf",
                        modifier = Modifier.size(64.dp).clip(RoundedCornerShape(10.dp)),
                        contentScale = androidx.compose.ui.layout.ContentScale.Crop
                    )
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            when {
                                mediaError != null -> "Yükleme başarısız"
                                mediaUploading -> "Fotoğraf yükleniyor %$mediaProgress"
                                pendingMediaUrl.isNotBlank() -> "Fotoğraf hazır ✓"
                                else -> "Fotoğraf hazırlanıyor…"
                            },
                            fontWeight = FontWeight.SemiBold,
                            style = MaterialTheme.typography.labelMedium
                        )
                        if (mediaUploading) {
                            LinearProgressIndicator(
                                progress = { mediaProgress / 100f },
                                modifier = Modifier.fillMaxWidth().padding(top = 6.dp)
                            )
                        }
                    }
                    IconButton(onClick = {
                        mediaUploadJob?.cancel()
                        pendingMediaUri = null
                        pendingMediaUrl = ""
                        mediaError = null
                        mediaUploading = false
                        mediaProgress = 0
                    }) {
                        Icon(Icons.Default.Close, "Fotoğrafı kaldır")
                    }
                }
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
                val mine = message.senderId == currentUid
                val replyPreview = message.replyToMessageId.isNotBlank()

                Row(
                    Modifier
                        .fillMaxWidth()
                        .pointerInput(message.id) {
                            detectHorizontalDragGestures(
                                onHorizontalDrag = { _, amount ->
                                    dragX = (dragX + amount).coerceIn(0f, 96f)
                                },
                                onDragEnd = {
                                    if (dragX >= 64f) replyTarget = message
                                    dragX = 0f
                                }
                            )
                        },
                    horizontalArrangement = if (mine) Arrangement.End else Arrangement.Start,
                    verticalAlignment = Alignment.Bottom
                ) {
                    if (!mine) {
                        UserAvatar(
                            message.senderPhotoUrl.ifBlank { user.profileImageUrl },
                            message.senderId.take(1).uppercase(),
                            size = 30.dp
                        )
                        Spacer(Modifier.width(6.dp))
                    }
                    Column(
                        modifier = Modifier
                            .offset { IntOffset(dragX.roundToInt(), 0) }
                            .widthIn(max = 320.dp)
                            .pointerInput(message.id + "-tap") {
                                detectTapGestures(
                                    onDoubleTap = { onReaction(message.id, "❤️") },
                                    onLongPress = { actionMessage = message }
                                )
                            }
                    ) {
                        Surface(
                            color = if (mine) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Column(Modifier.padding(6.dp)) {
                                if (replyPreview) {
                                    Surface(
                                        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(11.dp)),
                                        color = Color(0xFFFFE8D5)
                                    ) {
                                        Column(Modifier.padding(horizontal = 10.dp, vertical = 7.dp)) {
                                            Text(
                                                message.replyToSenderName.ifBlank { "Yanıtlanan mesaj" },
                                                color = Color(0xFFB45309),
                                                style = MaterialTheme.typography.labelMedium,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Text(
                                                message.replyToText.ifBlank { "📷 Fotoğraf" },
                                                maxLines = 2,
                                                color = Color(0xFF7C2D12),
                                                style = MaterialTheme.typography.bodySmall
                                            )
                                        }
                                    }
                                    Spacer(Modifier.height(5.dp))
                                }

                                if (message.mediaUrl.isNotBlank()) {
                                    var mediaFailed by remember(message.mediaUrl) { mutableStateOf(false) }
                                    if (mediaFailed) {
                                        Surface(
                                            modifier = Modifier.width(220.dp).height(120.dp),
                                            shape = RoundedCornerShape(12.dp),
                                            color = MaterialTheme.colorScheme.surfaceVariant
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Text("Fotoğraf yüklenemedi", color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            }
                                        }
                                    } else {
                                        AsyncImage(
                                            model = message.mediaUrl,
                                            contentDescription = "Gönderilen fotoğraf",
                                            modifier = Modifier.width(220.dp).heightIn(max = 280.dp).clip(RoundedCornerShape(12.dp)),
                                            contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                                            onError = { mediaFailed = true }
                                        )
                                    }
                                }

                                if (message.text.isNotBlank()) {
                                    Text(message.text, modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp))
                                }

                                if (message.editedAt != null) {
                                    Text(
                                        "düzenlendi",
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 1.dp),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }

                        if (message.reactions.isNotEmpty()) {
                            Row(
                                modifier = Modifier.padding(start = if (mine) 0.dp else 8.dp, top = 2.dp),
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                message.reactions.values.distinct().forEach { emoji ->
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = MaterialTheme.colorScheme.surfaceVariant
                                    ) {
                                        Text(emoji, modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        replyTarget?.let { target ->
            Surface(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 2.dp),
                color = Color(0xFFFFF3E8),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(Modifier.fillMaxWidth().padding(9.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Yanıtlanıyor: " + target.replyToSenderName.ifBlank {
                            if (target.senderId == currentUid) "Sen" else user.displayName
                        }, color = Color(0xFFB45309), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)
                        Text(target.text.ifBlank { "📷 Fotoğraf" }, maxLines = 1, style = MaterialTheme.typography.bodySmall)
                    }
                    TextButton(onClick = { replyTarget = null }) { Text("İptal") }
                }
            }
        }

        editingMessage?.let {
            Surface(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 2.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(Modifier.fillMaxWidth().padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Mesaj düzenleniyor", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)
                        Text(it.text, maxLines = 1, style = MaterialTheme.typography.bodySmall)
                    }
                    TextButton(onClick = { editingMessage = null; draft = "" }) { Text("İptal") }
                }
            }
        }

        Row(
            Modifier.fillMaxWidth().navigationBarsPadding().padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(enabled = !mediaUploading && editingMessage == null, onClick = { mediaLauncher.launch("image/*") }) {
                Icon(Icons.Default.AddPhotoAlternate, "Fotoğraf")
            }
            OutlinedTextField(
                value = draft,
                onValueChange = { if (it.length <= 1000) draft = it },
                modifier = Modifier.weight(1f),
                placeholder = { Text(if (editingMessage != null) "Mesajı düzenle…" else "Mesaj yaz…") },
                maxLines = 4
            )
            IconButton(
                enabled = (draft.isNotBlank() || pendingMediaUrl.isNotBlank()) && !mediaUploading,
                onClick = {
                    val text = draft.trim()
                    val editing = editingMessage
                    if (editing != null) {
                        onEdit(editing.id, text)
                        editingMessage = null
                    } else if (pendingMediaUrl.isNotBlank()) {
                        onSendMedia(pendingMediaUrl, pendingMediaType, text, replyTarget)
                        pendingMediaUri = null
                        pendingMediaUrl = ""
                        mediaError = null
                        mediaProgress = 0
                        replyTarget = null
                    } else {
                        onSend(text, replyTarget)
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
            onReply = {
                replyTarget = message
                actionMessage = null
            },
            onReaction = { emoji ->
                onReaction(message.id, emoji)
                actionMessage = null
            },
            onEdit = {
                editingMessage = message
                draft = message.text
                replyTarget = null
                actionMessage = null
            },
            onDelete = {
                onDelete(message.id)
                actionMessage = null
            }
        )
    }
}

@Composable
private fun SectionButton(text: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.clip(RoundedCornerShape(10.dp))
            .background(if (selected) MaterialTheme.colorScheme.onBackground else MaterialTheme.colorScheme.surface)
            .clickable(onClick = onClick).padding(vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text, color = if (selected) MaterialTheme.colorScheme.background else MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun CommunityList(onGlobalChatClick: () -> Unit, onServersClick: () -> Unit) {
    LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item { Text("Topluluklar", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)) }
        item { CommunityCard(Icons.Default.Forum, "Genel Chat", "Tüm Libra üyelerinin ortak sohbeti.", onGlobalChatClick) }
        item { CommunityCard(Icons.Default.Groups, "Sunucular", "Kitap türlerine göre topluluklar.", onServersClick) }
    }
}

@Composable
private fun CommunityCard(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, subtitle: String, onClick: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth().clickable(onClick = onClick), shape = RoundedCornerShape(16.dp)) {
        Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(46.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceVariant), contentAlignment = Alignment.Center) { Icon(icon, null) }
            Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                Text(title, fontWeight = FontWeight.Bold)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(Icons.Default.ChevronRight, null)
        }
    }
}
