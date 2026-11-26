package com.libra.app.feature.messages

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Reply
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircleOutline
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.filled.GroupAdd
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.libra.app.core.di.ServiceLocator
import com.libra.app.core.result.AppResult
import com.libra.app.domain.model.DirectConversation
import com.libra.app.domain.model.DirectMessage
import com.libra.app.domain.model.StorageUploadRequest
import com.libra.app.domain.model.UserProfile
import com.libra.app.ui.animation.AppleSprings
import com.libra.app.ui.animation.appleBouncy
import com.libra.app.ui.components.FullScreenImageViewerDialog
import com.libra.app.ui.components.UserAvatar
import com.libra.app.ui.theme.LibraLavender
import com.libra.app.ui.theme.LibraLavenderBorder
import com.libra.app.ui.theme.LibraLavenderDarkBorder
import com.libra.app.ui.theme.LibraLavenderDarkSurface
import com.libra.app.ui.theme.LibraLavenderDarkText
import com.libra.app.ui.theme.LibraLavenderSurface
import com.libra.app.ui.theme.LibraLavenderText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID
import kotlin.math.roundToInt

class DirectMessagesViewModel : ViewModel() {
    private val repository = ServiceLocator.chatRepository
    private val auth = ServiceLocator.authRepository
    private val _conversations = MutableStateFlow<List<DirectConversation>>(emptyList())
    val conversations = _conversations.asStateFlow()
    private val _messages = MutableStateFlow<List<DirectMessage>>(emptyList())
    val messages = _messages.asStateFlow()
    private val _error = MutableStateFlow<String?>(null)
    val error = _error.asStateFlow()
    private var conversationJob: Job? = null
    private var observedConversationId: String? = null
    private val pendingMessages = linkedMapOf<String, DirectMessage>()

    init {
        viewModelScope.launch {
            val uid = auth.currentUser.filterNotNull().first().uid
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

    fun createGroup(
        name: String,
        participantIds: List<String>,
        onComplete: (DirectConversation?) -> Unit = {}
    ) {
        viewModelScope.launch {
            when (val result = repository.createGroupConversation(name, participantIds)) {
                is AppResult.Success -> {
                    _error.value = null
                    onComplete(result.data)
                }
                is AppResult.Error -> {
                    _error.value = result.error.message
                    onComplete(null)
                }
            }
        }
    }

    fun sendGroup(
        conversationId: String,
        text: String,
        replyTo: DirectMessage? = null,
        onComplete: (Boolean) -> Unit = {}
    ) {
        viewModelScope.launch {
            when (val result = repository.sendGroupMessage(conversationId, text, replyTo)) {
                is AppResult.Success -> {
                    _error.value = null
                    onComplete(true)
                }
                is AppResult.Error -> {
                    _error.value = result.error.message
                    onComplete(false)
                }
            }
        }
    }

    fun sendGroupMedia(
        conversationId: String,
        mediaUrl: String,
        mediaType: String,
        text: String = "",
        replyTo: DirectMessage? = null,
        onComplete: (Boolean) -> Unit = {}
    ) {
        viewModelScope.launch {
            when (
                val result = repository.sendGroupMediaMessage(
                    conversationId = conversationId,
                    mediaUrl = mediaUrl,
                    mediaType = mediaType,
                    text = text,
                    replyTo = replyTo
                )
            ) {
                is AppResult.Success -> {
                    _error.value = null
                    onComplete(true)
                }
                is AppResult.Error -> {
                    _error.value = result.error.message
                    onComplete(false)
                }
            }
        }
    }

    fun markRead(conversationId: String) {
        _conversations.value = _conversations.value.map {
            if (it.id == conversationId) it.copy(unreadCount = 0) else it
        }
        viewModelScope.launch { repository.markDirectConversationRead(conversationId) }
    }

    fun clearConversation(
        conversationId: String,
        removeFromInbox: Boolean,
        onComplete: (Boolean) -> Unit = {}
    ) {
        if (observedConversationId == conversationId) {
            pendingMessages.clear()
            _messages.value = emptyList()
        }
        viewModelScope.launch {
            when (val result = repository.clearDirectConversation(conversationId, removeFromInbox)) {
                is AppResult.Success -> {
                    if (removeFromInbox) {
                        _conversations.value = _conversations.value.filterNot { it.id == conversationId }
                    } else {
                        _conversations.value = _conversations.value.map {
                            if (it.id == conversationId) it.copy(lastMessage = "", unreadCount = 0) else it
                        }
                    }
                    _error.value = null
                    onComplete(true)
                }
                is AppResult.Error -> {
                    _error.value = result.error.message
                    onComplete(false)
                }
            }
        }
    }

    fun hideConversation(
        conversationId: String,
        onComplete: (Boolean) -> Unit = {}
    ) = clearConversation(conversationId, removeFromInbox = true, onComplete = onComplete)

    private fun observeConversation(id: String) {
        conversationJob?.cancel()
        observedConversationId = id
        pendingMessages.clear()
        _messages.value = emptyList()
        _error.value = null
        conversationJob = viewModelScope.launch {
            repository.observeDirectMessages(id).collect { result ->
                when (result) {
                    is AppResult.Success -> mergeMessages(result.data)
                    is AppResult.Error -> _error.value = result.error.message
                }
            }
        }
    }

    private fun mergeMessages(serverMessages: List<DirectMessage>) {
        val matchedPendingIds = pendingMessages.filter { (_, pending) ->
            serverMessages.any { server ->
                server.senderId == pending.senderId &&
                    server.recipientId == pending.recipientId &&
                    server.text == pending.text &&
                    server.mediaUrl == pending.mediaUrl &&
                    kotlin.math.abs(server.createdAt - pending.createdAt) <= 15_000L
            }
        }.keys
        matchedPendingIds.forEach(pendingMessages::remove)
        _messages.value = (serverMessages + pendingMessages.values)
            .distinctBy { it.id }
            .sortedBy { it.createdAt }
    }

    private fun addOptimisticMessage(
        recipientId: String,
        text: String,
        mediaUrl: String,
        mediaType: String,
        replyTo: DirectMessage?
    ): String? {
        val sender = auth.currentUser.value ?: return null
        val now = System.currentTimeMillis()
        val localId = "local-" + now + "-" + UUID.randomUUID()
        pendingMessages[localId] = DirectMessage(
            id = localId,
            senderId = sender.uid,
            senderPhotoUrl = sender.profileImageUrl,
            senderName = sender.displayName,
            senderUsername = sender.username,
            recipientId = recipientId,
            text = text.trim(),
            mediaUrl = mediaUrl,
            mediaType = mediaType,
            createdAt = now,
            replyToMessageId = replyTo?.id.orEmpty(),
            replyToText = replyTo?.text?.ifBlank {
                if (replyTo.mediaUrl.isNotBlank()) "📷 Fotoğraf" else ""
            }.orEmpty(),
            replyToSenderId = replyTo?.senderId.orEmpty(),
            replyToSenderName = replyTo?.senderName?.ifBlank {
                if (replyTo.senderId == sender.uid) sender.displayName else replyTo.replyToSenderName
            }.orEmpty()
        )
        mergeMessages(_messages.value)
        return localId
    }

    fun send(
        recipientId: String,
        text: String,
        replyTo: DirectMessage? = null,
        onComplete: (Boolean) -> Unit = {}
    ) {
        val localId = addOptimisticMessage(recipientId, text, "", "", replyTo)
        if (localId == null) {
            _error.value = "Oturum bulunamadı."
            onComplete(false)
            return
        }
        viewModelScope.launch {
            when (val result = repository.sendDirectMessage(recipientId, text, replyTo)) {
                is AppResult.Success -> {
                    _error.value = null
                    onComplete(true)
                }
                is AppResult.Error -> {
                    pendingMessages.remove(localId)
                    mergeMessages(_messages.value)
                    _error.value = result.error.message
                    onComplete(false)
                }
            }
        }
    }

    fun sendMedia(
        recipientId: String,
        mediaUrl: String,
        mediaType: String,
        text: String = "",
        replyTo: DirectMessage? = null,
        onComplete: (Boolean) -> Unit = {}
    ) {
        val localId = addOptimisticMessage(recipientId, text, mediaUrl, mediaType, replyTo)
        if (localId == null) {
            _error.value = "Oturum bulunamadı."
            onComplete(false)
            return
        }
        viewModelScope.launch {
            when (
                val result = repository.sendDirectMediaMessage(
                    recipientId = recipientId,
                    mediaUrl = mediaUrl,
                    mediaType = mediaType,
                    text = text,
                    replyTo = replyTo
                )
            ) {
                is AppResult.Success -> {
                    _error.value = null
                    onComplete(true)
                }
                is AppResult.Error -> {
                    pendingMessages.remove(localId)
                    mergeMessages(_messages.value)
                    _error.value = result.error.message
                    onComplete(false)
                }
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
        pendingMessages.remove(messageId)
        _messages.value = _messages.value.filterNot { it.id == messageId }
        viewModelScope.launch {
            when (val result = repository.deleteDirectMessage(conversationId, messageId)) {
                is AppResult.Success -> _error.value = null
                is AppResult.Error -> _error.value = result.error.message
            }
        }
    }

    fun deleteMessages(
        conversationId: String,
        messageIds: List<String>,
        onComplete: (Boolean) -> Unit = {}
    ) {
        val targetSet = messageIds.toSet()
        if (targetSet.isEmpty()) {
            onComplete(true)
            return
        }
        targetSet.forEach(pendingMessages::remove)
        _messages.value = _messages.value.filterNot { it.id in targetSet }
        viewModelScope.launch {
            when (val result = repository.deleteDirectMessages(conversationId, messageIds)) {
                is AppResult.Success -> {
                    _error.value = null
                    onComplete(true)
                }
                is AppResult.Error -> {
                    _error.value = result.error.message
                    onComplete(false)
                }
            }
        }
    }

    fun react(conversationId: String, messageId: String, emoji: String) {
        val uid = auth.currentUser.value?.uid.orEmpty()
        if (uid.isNotBlank()) {
            _messages.value = _messages.value.map { msg ->
                if (msg.id == messageId) {
                    val updated = msg.reactions.toMutableMap()
                    if (updated[uid] == emoji) updated.remove(uid) else updated[uid] = emoji
                    msg.copy(reactions = updated)
                } else msg
            }
        }
        viewModelScope.launch {
            when (val result = repository.toggleDirectMessageReaction(conversationId, messageId, emoji)) {
                is AppResult.Success -> _error.value = null
                is AppResult.Error -> _error.value = result.error.message
            }
        }
    }

    fun forwardMessage(
        message: DirectMessage,
        targetConversation: DirectConversation?,
        sendToGlobalChat: Boolean,
        fallbackAuthorName: String,
        onComplete: (Boolean) -> Unit
    ) {
        val me = auth.currentUser.value?.uid.orEmpty()
        val isOwnPhoto = message.senderId == me && message.mediaUrl.isNotBlank()
        val shared = if (!isOwnPhoto && (message.mediaUrl.isNotBlank() || message.sharedContent != null)) {
            buildForwardSharedContent(message, fallbackAuthorName)
        } else {
            null
        }
        val forwardText = when {
            message.text.isNotBlank() -> message.text.trim()
            shared != null -> "Bir mesaj paylaştı."
            else -> ""
        }

        viewModelScope.launch {
            val result = when {
                sendToGlobalChat -> {
                    if (isOwnPhoto) {
                        repository.sendGlobalMediaMessage(
                            mediaUrl = message.mediaUrl,
                            mediaType = message.mediaType.ifBlank { "image/jpeg" },
                            text = message.text.trim()
                        )
                    } else {
                        repository.sendGlobalMessage(
                            text = forwardText,
                            sharedContent = shared
                        )
                    }
                }
                targetConversation != null && targetConversation.isGroup -> {
                    if (isOwnPhoto) {
                        repository.sendGroupMediaMessage(
                            conversationId = targetConversation.id,
                            mediaUrl = message.mediaUrl,
                            mediaType = message.mediaType.ifBlank { "image/jpeg" },
                            text = message.text.trim()
                        )
                    } else {
                        repository.sendGroupMessage(
                            conversationId = targetConversation.id,
                            text = forwardText,
                            sharedContent = shared
                        )
                    }
                }
                targetConversation != null && targetConversation.otherUserId.isNotBlank() -> {
                    if (isOwnPhoto) {
                        repository.sendDirectMediaMessage(
                            recipientId = targetConversation.otherUserId,
                            mediaUrl = message.mediaUrl,
                            mediaType = message.mediaType.ifBlank { "image/jpeg" },
                            text = message.text.trim()
                        )
                    } else {
                        repository.sendDirectMessage(
                            recipientId = targetConversation.otherUserId,
                            text = forwardText,
                            sharedContent = shared
                        )
                    }
                }
                else -> {
                    onComplete(false)
                    return@launch
                }
            }

            if (result is AppResult.Success) {
                _error.value = null
                onComplete(true)
            } else if (result is AppResult.Error) {
                _error.value = result.error.message
                onComplete(false)
            }
        }
    }
}

private fun authUserId(): String = ServiceLocator.authRepository.currentUser.value?.uid.orEmpty()

private enum class MessageSection { MESSAGES, COMMUNITIES }

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun DirectMessagesScreen(
    onFindFriends: () -> Unit,
    onGlobalChatClick: () -> Unit,
    onServersClick: () -> Unit,
    initialUser: UserProfile? = null,
    onInitialUserConsumed: () -> Unit = {},
    initialConversationId: String? = null,
    onInitialConversationConsumed: () -> Unit = {},
    onActiveConversationChanged: (String?) -> Unit = {},
    onOpenProfile: (String) -> Unit = {},
    modifier: Modifier = Modifier,
    canMessage: Boolean = true,
    viewModel: DirectMessagesViewModel = viewModel()
) {
    val darkTheme = isSystemInDarkTheme()
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val clipboard = LocalClipboardManager.current

    var section by remember { mutableStateOf(MessageSection.MESSAGES) }
    var conversationSearch by remember { mutableStateOf("") }
    var selectedUser by remember { mutableStateOf<UserProfile?>(null) }
    var selectedConversation by remember { mutableStateOf<DirectConversation?>(null) }
    var showNewChat by remember { mutableStateOf(false) }
    var longPressedConversation by remember { mutableStateOf<DirectConversation?>(null) }
    var confirmDeleteConversation by remember { mutableStateOf<DirectConversation?>(null) }

    val conversations by viewModel.conversations.collectAsState()
    val messages by viewModel.messages.collectAsState()
    val error by viewModel.error.collectAsState()

    val currentActiveConversationId = remember(selectedConversation?.id, selectedUser?.uid) {
        selectedConversation?.id ?: selectedUser?.let { peer ->
            val me = authUserId()
            if (me.isNotBlank() && peer.uid.isNotBlank()) {
                listOf(me, peer.uid).sorted().joinToString("_")
            } else null
        }
    }

    LaunchedEffect(currentActiveConversationId) {
        onActiveConversationChanged(currentActiveConversationId)
    }
    DisposableEffect(Unit) {
        onDispose { onActiveConversationChanged(null) }
    }

    LaunchedEffect(initialUser?.uid) {
        initialUser?.let {
            selectedUser = it
            selectedConversation = null
            viewModel.openConversation(it)
            viewModel.markRead(listOf(authUserId(), it.uid).sorted().joinToString("_"))
            onInitialUserConsumed()
        }
    }

    LaunchedEffect(initialConversationId, conversations) {
        val targetConvId = initialConversationId?.takeIf { it.isNotBlank() } ?: return@LaunchedEffect
        val matched = conversations.firstOrNull { it.id == targetConvId }
        if (matched != null) {
            if (matched.isGroup) {
                selectedUser = null
                selectedConversation = matched
                viewModel.openConversation(matched)
            } else {
                val peerUid = matched.otherUserId
                if (peerUid.isNotBlank()) {
                    selectedConversation = null
                    selectedUser = UserProfile(
                        uid = peerUid,
                        displayName = matched.otherUserName,
                        username = matched.otherUserUsername,
                        profileImageUrl = matched.otherUserPhotoUrl
                    )
                    viewModel.openConversation(selectedUser!!)
                    viewModel.markRead(matched.id)
                }
            }
            onInitialConversationConsumed()
        }
    }

    selectedConversation?.let { conversation ->
        val liveConv = conversations.firstOrNull { it.id == conversation.id } ?: conversation
        DirectConversationScreen(
            user = UserProfile(
                uid = "",
                displayName = liveConv.groupName.ifBlank { "Grup sohbeti" },
                profileImageUrl = liveConv.groupPhotoUrl
            ),
            conversationIdOverride = liveConv.id,
            isGroup = true,
            groupMemberCount = liveConv.participantIds.size,
            peerLastReadAt = liveConv.peerLastReadAt,
            conversations = conversations,
            messages = messages,
            error = error,
            onBack = { selectedConversation = null },
            onSend = { text, reply, onComplete -> viewModel.sendGroup(liveConv.id, text, reply, onComplete) },
            onSendMedia = { url, type, text, reply, onComplete -> viewModel.sendGroupMedia(liveConv.id, url, type, text, reply, onComplete) },
            onEdit = { id, text -> viewModel.edit(liveConv.id, id, text) },
            onDelete = { id -> viewModel.delete(liveConv.id, id) },
            onDeleteBatch = { ids, done -> viewModel.deleteMessages(liveConv.id, ids, done) },
            onClearConversation = { done -> viewModel.clearConversation(liveConv.id, removeFromInbox = false, onComplete = done) },
            onReaction = { id, emoji -> viewModel.react(liveConv.id, id, emoji) },
            onMarkRead = { viewModel.markRead(liveConv.id) },
            onForwardMessage = { msg, targetConv, toGlobal, done ->
                viewModel.forwardMessage(
                    message = msg,
                    targetConversation = targetConv,
                    sendToGlobalChat = toGlobal,
                    fallbackAuthorName = msg.senderName.ifBlank { liveConv.groupName },
                    onComplete = done
                )
            },
            canMessage = canMessage,
            modifier = modifier
        )
        return
    }

    selectedUser?.let { user ->
        val dmConvId = listOf(authUserId(), user.uid).sorted().joinToString("_")
        val liveDmConv = conversations.firstOrNull { it.id == dmConvId }
        DirectConversationScreen(
            user = user,
            peerLastReadAt = liveDmConv?.peerLastReadAt ?: 0L,
            conversations = conversations,
            messages = messages,
            error = error,
            onBack = { selectedUser = null },
            onSend = { text, reply, onComplete -> viewModel.send(user.uid, text, reply, onComplete) },
            onSendMedia = { url, type, text, reply, onComplete -> viewModel.sendMedia(user.uid, url, type, text, reply, onComplete) },
            onEdit = { id, text -> viewModel.edit(dmConvId, id, text) },
            onDelete = { id -> viewModel.delete(dmConvId, id) },
            onDeleteBatch = { ids, done -> viewModel.deleteMessages(dmConvId, ids, done) },
            onClearConversation = { done -> viewModel.clearConversation(dmConvId, removeFromInbox = false, onComplete = done) },
            onReaction = { id, emoji -> viewModel.react(dmConvId, id, emoji) },
            onMarkRead = { viewModel.markRead(dmConvId) },
            onForwardMessage = { msg, targetConv, toGlobal, done ->
                viewModel.forwardMessage(
                    message = msg,
                    targetConversation = targetConv,
                    sendToGlobalChat = toGlobal,
                    fallbackAuthorName = if (msg.senderId == user.uid) user.displayName else msg.senderName,
                    onComplete = done
                )
            },
            onOpenProfile = { onOpenProfile(user.uid) },
            canMessage = canMessage,
            conversationIdOverride = dmConvId,
            modifier = modifier
        )
        return
    }

    val totalUnread = remember(conversations) { conversations.sumOf { it.unreadCount } }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        // Header Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Mesajlar",
                        style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold)
                    )
                    if (totalUnread > 0) {
                        Surface(
                            shape = CircleShape,
                            color = if (darkTheme) LibraLavenderDarkSurface else LibraLavenderSurface,
                            border = BorderStroke(1.dp, if (darkTheme) LibraLavenderDarkBorder else LibraLavenderBorder)
                        ) {
                            Text(
                                text = "$totalUnread yeni",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = if (darkTheme) LibraLavenderDarkText else LibraLavenderText,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }
                }
                Text(
                    text = "Birebir veya grup sohbeti başlat.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Surface(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .appleBouncy(pressedScale = 0.86f, onClick = { showNewChat = true }),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Yeni sohbet",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        if (section == MessageSection.MESSAGES) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                TextField(
                    value = conversationSearch,
                    onValueChange = { conversationSearch = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    placeholder = {
                        Text(
                            text = "Mesajlarda ara…",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    trailingIcon = if (conversationSearch.isNotBlank()) {
                        {
                            IconButton(onClick = { conversationSearch = "" }) {
                                Icon(Icons.Default.Close, contentDescription = "Temizle")
                            }
                        }
                    } else null,
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent
                    )
                )
            }
            Spacer(Modifier.height(10.dp))
        }

        // Section switcher
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                SectionButton(
                    text = "Mesajlar",
                    selected = section == MessageSection.MESSAGES,
                    onClick = { section = MessageSection.MESSAGES },
                    modifier = Modifier.weight(1f)
                )
                SectionButton(
                    text = "Topluluklar",
                    selected = section == MessageSection.COMMUNITIES,
                    onClick = { section = MessageSection.COMMUNITIES },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        Spacer(Modifier.height(12.dp))

        when (section) {
            MessageSection.MESSAGES -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Sohbetlerin",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                modifier = Modifier.weight(1f)
                            )
                            if (conversations.isNotEmpty()) {
                                Text(
                                    text = "Basılı tutunca menü açılır",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    if (conversations.isEmpty()) {
                        item {
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(20.dp),
                                color = MaterialTheme.colorScheme.surface,
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                            ) {
                                Column(
                                    Modifier
                                        .fillMaxWidth()
                                        .padding(24.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Surface(
                                        shape = CircleShape,
                                        color = if (darkTheme) LibraLavenderDarkSurface else LibraLavenderSurface,
                                        modifier = Modifier.size(56.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                imageVector = Icons.Default.ChatBubbleOutline,
                                                contentDescription = null,
                                                tint = if (darkTheme) LibraLavenderDarkText else LibraLavenderText,
                                                modifier = Modifier.size(28.dp)
                                            )
                                        }
                                    }
                                    Spacer(Modifier.height(12.dp))
                                    Text("Henüz bir sohbetin yok.", fontWeight = FontWeight.Bold)
                                    Spacer(Modifier.height(4.dp))
                                    Text(
                                        text = "Birinin profiline girip Mesaj'a dokunabilir veya + ile yeni sohbet açabilirsin.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(Modifier.height(10.dp))
                                    OutlinedButton(
                                        onClick = onFindFriends,
                                        shape = RoundedCornerShape(14.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.PersonAdd,
                                            null,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(Modifier.width(6.dp))
                                        Text("Arkadaş bul")
                                    }
                                }
                            }
                        }
                    } else {
                        val visibleConversations = conversations.filter { conversation ->
                            conversationSearch.isBlank() ||
                                conversation.otherUserName.contains(conversationSearch, ignoreCase = true) ||
                                conversation.otherUserUsername.contains(conversationSearch, ignoreCase = true) ||
                                conversation.groupName.contains(conversationSearch, ignoreCase = true) ||
                                conversation.lastMessage.contains(conversationSearch, ignoreCase = true)
                        }

                        if (visibleConversations.isEmpty()) {
                            item {
                                Text(
                                    text = "Aramana uygun sohbet yok.",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(vertical = 28.dp)
                                )
                            }
                        } else {
                            items(visibleConversations, key = { it.id }) { conversation ->
                                val hasUnread = conversation.unreadCount > 0
                                val shape = RoundedCornerShape(20.dp)
                                val displayName = if (conversation.isGroup) {
                                    conversation.groupName.ifBlank { "Grup sohbeti" }
                                } else {
                                    conversation.otherUserName.ifBlank { "Libra Üyesi" }
                                }
                                val avatarUrl = if (conversation.isGroup) {
                                    conversation.groupPhotoUrl
                                } else {
                                    conversation.otherUserPhotoUrl
                                }
                                val openConv = {
                                    if (conversation.isGroup) {
                                        selectedConversation = conversation
                                        selectedUser = null
                                    } else {
                                        selectedUser = UserProfile(
                                            uid = conversation.otherUserId,
                                            displayName = conversation.otherUserName,
                                            username = conversation.otherUserUsername,
                                            profileImageUrl = conversation.otherUserPhotoUrl
                                        )
                                        selectedConversation = null
                                    }
                                    viewModel.openConversation(conversation)
                                }

                                Surface(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(shape)
                                        .combinedClickable(
                                            onClick = openConv,
                                            onLongClick = {
                                                runCatching {
                                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                }
                                                longPressedConversation = conversation
                                            }
                                        ),
                                    shape = shape,
                                    color = MaterialTheme.colorScheme.surface,
                                    border = BorderStroke(
                                        width = if (hasUnread) 1.5.dp else 1.dp,
                                        color = if (hasUnread) {
                                            if (darkTheme) LibraLavenderDarkBorder else LibraLavender
                                        } else {
                                            MaterialTheme.colorScheme.outline
                                        }
                                    )
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 14.dp, vertical = 12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(contentAlignment = Alignment.BottomEnd) {
                                            Surface(
                                                shape = CircleShape,
                                                border = if (hasUnread) BorderStroke(2.dp, LibraLavender) else null,
                                                color = MaterialTheme.colorScheme.surface
                                            ) {
                                                UserAvatar(
                                                    photoUrl = avatarUrl,
                                                    initials = displayName.take(1).uppercase(),
                                                    size = 50.dp,
                                                    modifier = Modifier.padding(if (hasUnread) 2.dp else 0.dp),
                                                    onClick = {
                                                        if (conversation.isGroup) openConv()
                                                        else onOpenProfile(conversation.otherUserId)
                                                    }
                                                )
                                            }
                                        }

                                        Column(
                                            modifier = Modifier
                                                .weight(1f)
                                                .padding(horizontal = 12.dp)
                                        ) {
                                            Text(
                                                text = displayName,
                                                style = MaterialTheme.typography.titleSmall.copy(
                                                    fontWeight = if (hasUnread) FontWeight.ExtraBold else FontWeight.Bold
                                                ),
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                text = if (conversation.isGroup) {
                                                    "${conversation.participantIds.size} kişi • Grup"
                                                } else if (conversation.otherUserUsername.isNotBlank()) {
                                                    "@${conversation.otherUserUsername}"
                                                } else {
                                                    "Direkt mesaj"
                                                },
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            if (conversation.lastMessage.isNotBlank()) {
                                                Spacer(Modifier.height(2.dp))
                                                Text(
                                                    text = conversation.lastMessage,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis,
                                                    style = MaterialTheme.typography.bodySmall.copy(
                                                        fontWeight = if (hasUnread) FontWeight.SemiBold else FontWeight.Normal
                                                    ),
                                                    color = if (hasUnread) {
                                                        MaterialTheme.colorScheme.onSurface
                                                    } else {
                                                        MaterialTheme.colorScheme.onSurfaceVariant
                                                    }
                                                )
                                            }
                                        }

                                        Column(
                                            horizontalAlignment = Alignment.End,
                                            verticalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            if (conversation.updatedAt > 0L) {
                                                Text(
                                                    text = formatConversationRelativeTime(conversation.updatedAt),
                                                    style = MaterialTheme.typography.labelSmall.copy(
                                                        fontWeight = if (hasUnread) FontWeight.Bold else FontWeight.Normal
                                                    ),
                                                    color = if (hasUnread) {
                                                        if (darkTheme) LibraLavenderDarkText else LibraLavenderText
                                                    } else {
                                                        MaterialTheme.colorScheme.onSurfaceVariant
                                                    }
                                                )
                                            }
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                                            ) {
                                                if (hasUnread) {
                                                    Surface(
                                                        shape = CircleShape,
                                                        color = LibraLavender
                                                    ) {
                                                        Text(
                                                            text = conversation.unreadCount.coerceAtMost(99).toString(),
                                                            color = Color.White,
                                                            style = MaterialTheme.typography.labelSmall.copy(
                                                                fontWeight = FontWeight.Bold
                                                            ),
                                                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                                                        )
                                                    }
                                                }
                                                Icon(
                                                    imageVector = Icons.Default.ChevronRight,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(18.dp),
                                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
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

            MessageSection.COMMUNITIES -> {
                CommunityList(onGlobalChatClick, onServersClick)
            }
        }
    }

    longPressedConversation?.let { targetConv ->
        RichConversationActionOverlay(
            conversation = targetConv,
            onDismiss = { longPressedConversation = null },
            onOpenConversation = {
                longPressedConversation = null
                if (targetConv.isGroup) {
                    selectedConversation = targetConv
                    selectedUser = null
                } else {
                    selectedUser = UserProfile(
                        uid = targetConv.otherUserId,
                        displayName = targetConv.otherUserName,
                        username = targetConv.otherUserUsername,
                        profileImageUrl = targetConv.otherUserPhotoUrl
                    )
                    selectedConversation = null
                }
                viewModel.openConversation(targetConv)
            },
            onOpenProfile = if (!targetConv.isGroup && targetConv.otherUserId.isNotBlank()) {
                {
                    longPressedConversation = null
                    onOpenProfile(targetConv.otherUserId)
                }
            } else null,
            onMarkRead = if (targetConv.unreadCount > 0) {
                {
                    viewModel.markRead(targetConv.id)
                    longPressedConversation = null
                    Toast.makeText(context, "Okundu olarak işaretlendi.", Toast.LENGTH_SHORT).show()
                }
            } else null,
            onShare = {
                val shareText = if (targetConv.isGroup) {
                    "Libra Grup Sohbeti: ${targetConv.groupName.ifBlank { "Grup" }}"
                } else {
                    val handle = targetConv.otherUserUsername.takeIf { it.isNotBlank() }?.let { " (@$it)" }.orEmpty()
                    "Libra kullanıcısı: ${targetConv.otherUserName}$handle"
                }
                clipboard.setText(AnnotatedString(shareText))
                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, shareText)
                }
                longPressedConversation = null
                context.startActivity(Intent.createChooser(intent, "Sohbeti Paylaş"))
            },
            onDeleteConversation = {
                longPressedConversation = null
                confirmDeleteConversation = targetConv
            }
        )
    }

    confirmDeleteConversation?.let { targetConv ->
        val label = if (targetConv.isGroup) targetConv.groupName.ifBlank { "bu grubu" } else targetConv.otherUserName.ifBlank { "bu sohbeti" }
        AlertDialog(
            onDismissRequest = { confirmDeleteConversation = null },
            containerColor = MaterialTheme.colorScheme.surface,
            title = { Text("Sohbeti Kalıcı Sil", fontWeight = FontWeight.Bold) },
            text = {
                Text("\"$label\" sohbetindeki tüm mesajların ve medyaların kalıcı olarak silinecek, geçmiş tamamen sıfırlanacak. Sohbeti tekrar açtığında sıfırdan başlar.")
            },
            confirmButton = {
                Button(
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    onClick = {
                        viewModel.clearConversation(targetConv.id, removeFromInbox = true) { ok ->
                            if (ok) Toast.makeText(context, "Sohbet tamamen silindi.", Toast.LENGTH_SHORT).show()
                        }
                        confirmDeleteConversation = null
                    }
                ) { Text("Kalıcı Sil") }
            },
            dismissButton = {
                TextButton(onClick = { confirmDeleteConversation = null }) { Text("Vazgeç") }
            }
        )
    }

    if (showNewChat) {
        NewChatSheet(
            onDismiss = { showNewChat = false },
            onDirectUser = { user ->
                showNewChat = false
                selectedConversation = null
                selectedUser = user
                viewModel.openConversation(user)
            },
            onCreateGroup = { name, ids ->
                viewModel.createGroup(name, ids) { conversation ->
                    if (conversation != null) {
                        showNewChat = false
                        selectedUser = null
                        selectedConversation = conversation
                        viewModel.openConversation(conversation)
                    }
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NewChatSheet(
    onDismiss: () -> Unit,
    onDirectUser: (UserProfile) -> Unit,
    onCreateGroup: (String, List<String>) -> Unit
) {
    var mode by remember { mutableStateOf(0) }
    var query by remember { mutableStateOf("") }
    var groupName by remember { mutableStateOf("") }
    var users by remember { mutableStateOf<List<UserProfile>>(emptyList()) }
    var selected by remember { mutableStateOf<List<UserProfile>>(emptyList()) }

    LaunchedEffect(query) {
        if (query.trim().length < 2) {
            users = emptyList()
        } else {
            ServiceLocator.userRepository.searchUsers(query.trim()).collect { result ->
                if (result is AppResult.Success) users = result.data.take(20)
            }
        }
    }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp)
                .navigationBarsPadding()
        ) {
            Text("Yeni sohbet", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold))
            Spacer(Modifier.height(12.dp))

            if (mode == 0) {
                NewChatOption(
                    icon = Icons.Default.ChatBubbleOutline,
                    title = "Birine mesaj gönder",
                    subtitle = "Kullanıcı adıyla kişiyi bul ve sohbet başlat.",
                    onClick = { mode = 1 }
                )
                Spacer(Modifier.height(8.dp))
                NewChatOption(
                    icon = Icons.Default.GroupAdd,
                    title = "Grup oluştur",
                    subtitle = "Birden fazla kişiyi seçip yeni grup sohbeti başlat.",
                    onClick = { mode = 2 }
                )
            } else {
                TextButton(onClick = { mode = 0 }) { Text("Geri") }
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    placeholder = { Text("Kullanıcı ara...") },
                    leadingIcon = { Icon(Icons.Default.Search, null) }
                )
                Spacer(Modifier.height(8.dp))

                if (mode == 2) {
                    OutlinedTextField(
                        value = groupName,
                        onValueChange = { if (it.length <= 60) groupName = it },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        label = { Text("Grup adı") }
                    )
                    Spacer(Modifier.height(8.dp))
                    if (selected.isNotEmpty()) {
                        Text(
                            text = "${selected.size} kişi seçildi",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(Modifier.height(4.dp))
                    }
                }

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 360.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(users, key = { it.uid }) { user ->
                        val isSelected = selected.any { it.uid == user.uid }
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .clickable {
                                    if (mode == 1) {
                                        onDirectUser(user)
                                    } else {
                                        selected = if (isSelected) {
                                            selected.filterNot { it.uid == user.uid }
                                        } else {
                                            (selected + user).take(49)
                                        }
                                    }
                                },
                            shape = RoundedCornerShape(14.dp),
                            color = MaterialTheme.colorScheme.surface,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                        ) {
                            Row(
                                Modifier
                                    .fillMaxWidth()
                                    .padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                UserAvatar(user.profileImageUrl, user.initials, size = 42.dp)
                                Column(
                                    Modifier
                                        .weight(1f)
                                        .padding(horizontal = 10.dp)
                                ) {
                                    Text(user.displayName, fontWeight = FontWeight.SemiBold)
                                    if (user.username.isNotBlank()) {
                                        Text(
                                            text = "@${user.username}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                                if (mode == 2 && isSelected) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        tint = LibraLavender
                                    )
                                }
                            }
                        }
                    }
                }

                if (mode == 2) {
                    Spacer(Modifier.height(10.dp))
                    Button(
                        onClick = { onCreateGroup(groupName.trim(), selected.map { it.uid }) },
                        enabled = groupName.trim().isNotBlank() && selected.isNotEmpty(),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Grubu oluştur")
                    }
                }

                if (query.trim().length < 2) {
                    Text(
                        text = "Aramak için en az 2 karakter yaz.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 14.dp)
                    )
                } else if (users.isEmpty()) {
                    Text(
                        text = "Kullanıcı bulunamadı.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 14.dp)
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
        }
    }
}

@Composable
private fun NewChatOption(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .appleBouncy(pressedScale = 0.97f, onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, null, modifier = Modifier.size(28.dp))
            Column(
                Modifier
                    .weight(1f)
                    .padding(start = 14.dp)
            ) {
                Text(title, fontWeight = FontWeight.Bold)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(Icons.Default.ChevronRight, null)
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun DirectConversationScreen(
    user: UserProfile,
    conversationIdOverride: String? = null,
    isGroup: Boolean = false,
    groupMemberCount: Int = 0,
    peerLastReadAt: Long = 0L,
    conversations: List<DirectConversation> = emptyList(),
    messages: List<DirectMessage>,
    error: String?,
    onBack: () -> Unit,
    onSend: (String, DirectMessage?, (Boolean) -> Unit) -> Unit,
    onSendMedia: (String, String, String, DirectMessage?, (Boolean) -> Unit) -> Unit,
    onEdit: (String, String) -> Unit,
    onDelete: (String) -> Unit,
    onDeleteBatch: (List<String>, (Boolean) -> Unit) -> Unit = { ids, done -> ids.forEach(onDelete); done(true) },
    onClearConversation: ((Boolean) -> Unit) -> Unit = { it(false) },
    onReaction: (String, String) -> Unit,
    onMarkRead: () -> Unit = {},
    onForwardMessage: (DirectMessage, DirectConversation?, Boolean, (Boolean) -> Unit) -> Unit,
    onOpenProfile: () -> Unit = {},
    canMessage: Boolean = true,
    modifier: Modifier = Modifier
) {
    val darkTheme = isSystemInDarkTheme()
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val clipboard = LocalClipboardManager.current
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    var draft by remember { mutableStateOf("") }
    var replyTarget by remember { mutableStateOf<DirectMessage?>(null) }
    var editingMessage by remember { mutableStateOf<DirectMessage?>(null) }
    var actionMessage by remember { mutableStateOf<DirectMessage?>(null) }
    var shareTargetMessage by remember { mutableStateOf<DirectMessage?>(null) }

    var isSelectionMode by remember { mutableStateOf(false) }
    var selectedMessageIds by remember { mutableStateOf<Set<String>>(emptySet()) }
    var showBatchDeleteConfirm by remember { mutableStateOf(false) }
    var showClearChatConfirm by remember { mutableStateOf(false) }
    var showConversationMenu by remember { mutableStateOf(false) }
    var highlightedMessageId by remember { mutableStateOf<String?>(null) }
    var highlightNonce by remember { mutableIntStateOf(0) }

    BackHandler(enabled = isSelectionMode) {
        isSelectionMode = false
        selectedMessageIds = emptySet()
    }
    BackHandler(enabled = !isSelectionMode, onBack = onBack)

    var mediaUploading by remember { mutableStateOf(false) }
    var mediaProgress by remember { mutableIntStateOf(0) }
    var mediaError by remember { mutableStateOf<String?>(null) }
    var pendingMediaUri by remember { mutableStateOf<Uri?>(null) }
    var pendingMediaUrl by remember { mutableStateOf("") }
    var pendingMediaType by remember { mutableStateOf("image/jpeg") }
    var pendingMediaSizeBytes by remember { mutableIntStateOf(0) }
    var pendingMediaResolution by remember { mutableStateOf("") }
    var mediaUploadJob by remember { mutableStateOf<Job?>(null) }
    var sending by remember { mutableStateOf(false) }
    var sendWhenUploadReady by remember { mutableStateOf(false) }

    val conversationId = conversationIdOverride ?: remember(user.uid) { listOf(authUserId(), user.uid).sorted().joinToString("_") }
    val currentUid = authUserId()

    fun toggleMessageSelection(messageId: String) {
        if (messageId.isBlank() || messageId.startsWith("local-")) return
        runCatching { haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove) }
        val updated = if (messageId in selectedMessageIds) {
            selectedMessageIds - messageId
        } else {
            selectedMessageIds + messageId
        }
        selectedMessageIds = updated
        if (updated.isEmpty()) isSelectionMode = false
    }

    fun startImageUpload(uri: Uri) {
        mediaUploadJob?.cancel()
        pendingMediaUri = uri
        pendingMediaUrl = ""
        mediaProgress = 0
        mediaError = null
        mediaUploading = true
        pendingMediaSizeBytes = 0
        pendingMediaResolution = ""
        mediaUploadJob = scope.launch {
            try {
                val prepared = prepareChatImageForUpload(context, uri, prefix = "dm")
                pendingMediaType = prepared.contentType
                pendingMediaSizeBytes = prepared.bytes.size
                pendingMediaResolution = if (prepared.width > 0 && prepared.height > 0) {
                    "${prepared.width}×${prepared.height}"
                } else ""

                val upload = StorageUploadRequest(
                    fileName = prepared.fileName,
                    bytes = prepared.bytes,
                    contentType = prepared.contentType,
                    targetDirectory = "users/$currentUid"
                )
                val result = ServiceLocator.storageRepository
                    .uploadMedia(upload) { pct ->
                        scope.launch(Dispatchers.Main) { mediaProgress = pct }
                    }
                    .first()

                when (result) {
                    is AppResult.Success -> {
                        pendingMediaUrl = result.data
                        mediaProgress = 100
                        mediaError = null
                    }
                    is AppResult.Error -> {
                        mediaError = result.error.message
                        sendWhenUploadReady = false
                    }
                }
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Exception) {
                mediaError = e.localizedMessage ?: "Fotoğraf yüklenemedi."
                sendWhenUploadReady = false
            } finally {
                mediaUploading = false
            }
        }
    }

    val mediaLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        uri?.let(::startImageUpload)
    }

    val legacyMediaLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let(::startImageUpload)
    }

    fun launchPhotoPicker() {
        runCatching {
            mediaLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        }.onFailure {
            legacyMediaLauncher.launch("image/*")
        }
    }

    // Auto-send immediately once queued upload finishes
    LaunchedEffect(sendWhenUploadReady, pendingMediaUrl, mediaUploading, mediaError) {
        if (sendWhenUploadReady && !mediaUploading && pendingMediaUrl.isNotBlank() && mediaError == null && !sending) {
            sendWhenUploadReady = false
            val text = draft.trim()
            val reply = replyTarget
            sending = true
            onSendMedia(pendingMediaUrl, pendingMediaType, text, reply) { success ->
                sending = false
                if (success) {
                    pendingMediaUri = null
                    pendingMediaUrl = ""
                    pendingMediaSizeBytes = 0
                    pendingMediaResolution = ""
                    mediaError = null
                    mediaProgress = 0
                    replyTarget = null
                    draft = ""
                }
            }
        }
    }

    val imeBottomPx = WindowInsets.ime.getBottom(LocalDensity.current)
    val lastMessageId = messages.lastOrNull()?.id.orEmpty()
    val selectableMessageIds = remember(messages) {
        messages.filter { !it.id.startsWith("local-") }.map { it.id }.toSet()
    }
    val showScrollToBottom by remember {
        derivedStateOf { listState.canScrollForward }
    }

    LaunchedEffect(lastMessageId, imeBottomPx) {
        if (messages.isNotEmpty() && !isSelectionMode) {
            listState.animateScrollToItem(messages.lastIndex)
        }
    }

    LaunchedEffect(messages) {
        if (selectedMessageIds.isNotEmpty()) {
            val pruned = selectedMessageIds.intersect(selectableMessageIds)
            if (pruned != selectedMessageIds) {
                selectedMessageIds = pruned
                if (pruned.isEmpty()) isSelectionMode = false
            }
        }
    }

    LaunchedEffect(highlightedMessageId, highlightNonce) {
        if (highlightedMessageId != null) {
            delay(1700L)
            highlightedMessageId = null
        }
    }

    LaunchedEffect(conversationId, lastMessageId) {
        val lastMessage = messages.lastOrNull()
        if (lastMessage != null && lastMessage.senderId != currentUid) {
            onMarkRead()
        }
    }

    val lastSentMessageId = remember(messages, currentUid) {
        messages.lastOrNull { it.senderId == currentUid }?.id.orEmpty()
    }

    val sentBubbleColor = if (darkTheme) Color(0xFF6D28D9) else LibraLavender
    val sentBubbleTextColor = Color.White
    val sentTimestampColor = Color(0xFFEDE9FE)
    val receivedBubbleColor = MaterialTheme.colorScheme.surface
    val receivedBubbleBorder = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Aesthetic Top Header (transforms into contextual batch-action bar in selection mode)
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = if (isSelectionMode) {
                if (darkTheme) LibraLavenderDarkSurface else LibraLavenderSurface
            } else {
                MaterialTheme.colorScheme.surface
            }
        ) {
            Column {
                if (isSelectionMode) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .statusBarsPadding()
                            .padding(horizontal = 8.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = {
                                isSelectionMode = false
                                selectedMessageIds = emptySet()
                            }
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "Seçimi Kapat")
                        }
                        Text(
                            text = "${selectedMessageIds.size} seçildi",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            modifier = Modifier
                                .weight(1f)
                                .padding(start = 6.dp)
                        )
                        val allSelected = selectableMessageIds.isNotEmpty() &&
                            selectedMessageIds.size == selectableMessageIds.size
                        TextButton(
                            enabled = selectableMessageIds.isNotEmpty(),
                            onClick = {
                                selectedMessageIds = if (allSelected) emptySet() else selectableMessageIds
                            }
                        ) {
                            Text(
                                text = if (allSelected) "Temizle" else "Tümü",
                                fontWeight = FontWeight.SemiBold,
                                color = if (darkTheme) LibraLavenderDarkText else LibraLavenderText
                            )
                        }
                        IconButton(
                            enabled = selectedMessageIds.isNotEmpty(),
                            onClick = {
                                val chosen = messages.filter { it.id in selectedMessageIds }
                                val formatted = formatSelectedMessagesForCopy(
                                    messages = chosen,
                                    currentUid = currentUid,
                                    peerFallbackName = user.displayName
                                )
                                if (formatted.isNotBlank()) {
                                    clipboard.setText(AnnotatedString(formatted))
                                    Toast.makeText(context, "${chosen.size} mesaj kopyalandı.", Toast.LENGTH_SHORT).show()
                                }
                                isSelectionMode = false
                                selectedMessageIds = emptySet()
                            }
                        ) {
                            Icon(Icons.Default.ContentCopy, contentDescription = "Kopyala")
                        }
                        IconButton(
                            enabled = selectedMessageIds.isNotEmpty(),
                            onClick = { showBatchDeleteConfirm = true }
                        ) {
                            Icon(
                                imageVector = Icons.Default.DeleteOutline,
                                contentDescription = "Seçilenleri Sil",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                } else {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .statusBarsPadding()
                            .padding(horizontal = 8.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = onBack,
                            modifier = Modifier.appleBouncy(pressedScale = 0.86f, onClick = onBack)
                        ) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Geri")
                        }
                        UserAvatar(
                            photoUrl = user.profileImageUrl,
                            initials = user.initials,
                            size = 42.dp,
                            onClick = { if (!isGroup) onOpenProfile() }
                        )
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .padding(start = 10.dp)
                                .let { mod -> if (isGroup) mod else mod.clickable(onClick = onOpenProfile) }
                        ) {
                            Text(
                                text = user.displayName.ifBlank { "Libra Üyesi" },
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = if (isGroup) {
                                    if (groupMemberCount > 0) "$groupMemberCount katılımcı • Grup sohbeti" else "Grup sohbeti"
                                } else {
                                    user.handle
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        if (!isGroup) {
                            IconButton(
                                onClick = onOpenProfile,
                                modifier = Modifier.appleBouncy(pressedScale = 0.88f, onClick = onOpenProfile)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Person,
                                    contentDescription = "Profili Gör",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Box {
                            IconButton(onClick = { showConversationMenu = true }) {
                                Icon(
                                    imageVector = Icons.Default.MoreVert,
                                    contentDescription = "Sohbet seçenekleri",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            DropdownMenu(
                                expanded = showConversationMenu,
                                onDismissRequest = { showConversationMenu = false },
                                containerColor = MaterialTheme.colorScheme.surface
                            ) {
                                if (selectableMessageIds.isNotEmpty()) {
                                    DropdownMenuItem(
                                        text = { Text("Mesajları Seç") },
                                        leadingIcon = { Icon(Icons.Default.CheckCircleOutline, contentDescription = null) },
                                        onClick = {
                                            showConversationMenu = false
                                            isSelectionMode = true
                                        }
                                    )
                                }
                                DropdownMenuItem(
                                    text = { Text("Sohbeti Sıfırla", color = MaterialTheme.colorScheme.error) },
                                    leadingIcon = {
                                        Icon(
                                            Icons.Default.DeleteOutline,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.error
                                        )
                                    },
                                    onClick = {
                                        showConversationMenu = false
                                        showClearChatConfirm = true
                                    }
                                )
                            }
                        }
                    }
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outline)
            }
        }

        if (error != null) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.errorContainer
            ) {
                Text(
                    text = error,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                )
            }
        }

        // Messages List + Empty Slate + Floating Scroll-to-Bottom Pill
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            if (messages.isEmpty()) {
                Surface(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(horizontal = 28.dp),
                    shape = RoundedCornerShape(22.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 22.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "Sohbet sıfırdan başlıyor ✨",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = "Burada henüz mesaj yok. İlk mesajı yazarak konuşmayı başlatabilirsin.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                itemsIndexed(messages, key = { _, msg -> msg.id }) { index, message ->
                val groupPos = remember(messages, index, currentUid, isGroup) {
                    computeDirectMessageGrouping(
                        messages = messages,
                        index = index,
                        currentUid = currentUid,
                        isGroup = isGroup
                    )
                }
                val mine = message.senderId == currentUid
                val replyPreview = message.replyToMessageId.isNotBlank()
                val myReaction = message.reactions[currentUid]
                val isSelected = message.id in selectedMessageIds
                val isHighlighted = highlightedMessageId == message.id

                val highlightContainerColor by animateColorAsState(
                    targetValue = when {
                        isHighlighted -> if (darkTheme) Color(0xFF4C1D95) else Color(0xFFEDE9FE)
                        isSelected -> if (darkTheme) Color(0xFF2E1065) else Color(0xFFF3E8FF)
                        else -> Color.Transparent
                    },
                    label = "message_highlight_bg"
                )

                var dragX by remember(message.id) { mutableFloatStateOf(0f) }
                var swipeHapticFired by remember(message.id) { mutableStateOf(false) }

                if (groupPos.showTimestampDivider && message.createdAt > 0L) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.surface,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                        ) {
                            Text(
                                text = formatChatTimeHeader(message.createdAt),
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
                            )
                        }
                    }
                } else if (groupPos.isFirstInGroup && index > 0) {
                    Spacer(Modifier.height(8.dp))
                }

                val bubbleShape = remember(mine, groupPos) {
                    val outer = 20.dp
                    val inner = 6.dp
                    if (mine) {
                        RoundedCornerShape(
                            topStart = outer,
                            bottomStart = outer,
                            topEnd = if (groupPos.isFirstInGroup) outer else inner,
                            bottomEnd = if (groupPos.isLastInGroup) outer else inner
                        )
                    } else {
                        RoundedCornerShape(
                            topStart = if (groupPos.isFirstInGroup) outer else inner,
                            bottomStart = if (groupPos.isLastInGroup) outer else inner,
                            topEnd = outer,
                            bottomEnd = outer
                        )
                    }
                }

                val openActionMenu = {
                    if (isSelectionMode) {
                        toggleMessageSelection(message.id)
                    } else {
                        runCatching { haptic.performHapticFeedback(HapticFeedbackType.LongPress) }
                        actionMessage = message
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .background(highlightContainerColor)
                        .let { mod ->
                            if (isSelectionMode) {
                                mod.clickable { toggleMessageSelection(message.id) }
                            } else {
                                mod.pointerInput(message.id) {
                                    detectHorizontalDragGestures(
                                        onHorizontalDrag = { _, amount ->
                                            dragX = (dragX + amount).coerceIn(0f, 96f)
                                            if (dragX >= 58f && !swipeHapticFired) {
                                                swipeHapticFired = true
                                                runCatching { haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove) }
                                            }
                                        },
                                        onDragCancel = {
                                            dragX = 0f
                                            swipeHapticFired = false
                                        },
                                        onDragEnd = {
                                            if (dragX >= 58f) replyTarget = message
                                            dragX = 0f
                                            swipeHapticFired = false
                                        }
                                    )
                                }
                            }
                        }
                ) {
                    if (dragX > 10f) {
                        Surface(
                            modifier = Modifier
                                .align(Alignment.CenterStart)
                                .padding(start = 6.dp)
                                .size(32.dp),
                            shape = CircleShape,
                            color = if (darkTheme) LibraLavenderDarkSurface else LibraLavenderSurface,
                            border = BorderStroke(1.dp, if (darkTheme) LibraLavenderDarkBorder else LibraLavenderBorder)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.Reply,
                                    contentDescription = "Yanıtla",
                                    tint = if (darkTheme) LibraLavenderDarkText else LibraLavenderText,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 1.dp)
                            .offset { IntOffset(dragX.roundToInt(), 0) },
                        horizontalArrangement = if (mine) Arrangement.End else Arrangement.Start,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        if (isSelectionMode) {
                            Surface(
                                modifier = Modifier
                                    .padding(start = 4.dp, end = 8.dp, bottom = 6.dp)
                                    .size(22.dp)
                                    .clip(CircleShape)
                                    .clickable { toggleMessageSelection(message.id) },
                                shape = CircleShape,
                                color = if (isSelected) LibraLavender else MaterialTheme.colorScheme.surface,
                                border = BorderStroke(
                                    1.5.dp,
                                    if (isSelected) LibraLavender else MaterialTheme.colorScheme.outline
                                )
                            ) {
                                if (isSelected) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = "Seçili",
                                            tint = Color.White,
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                }
                            }
                        }

                        if (!mine) {
                            if (groupPos.showAvatar) {
                                UserAvatar(
                                    photoUrl = message.senderPhotoUrl.ifBlank { user.profileImageUrl },
                                    initials = (message.senderName.ifBlank { user.displayName }).take(1).uppercase(),
                                    size = 30.dp,
                                    onClick = {
                                        if (isSelectionMode) toggleMessageSelection(message.id)
                                        else if (!isGroup) onOpenProfile()
                                    }
                                )
                            } else {
                                Spacer(Modifier.width(30.dp))
                            }
                            Spacer(Modifier.width(8.dp))
                        }

                        Column(
                            modifier = Modifier.widthIn(max = 310.dp),
                            horizontalAlignment = if (mine) Alignment.End else Alignment.Start
                        ) {
                            if (groupPos.showSenderName) {
                                val senderLabel = message.senderName.ifBlank {
                                    message.senderUsername.takeIf { it.isNotBlank() }?.let { "@$it" } ?: "Üye"
                                }
                                Text(
                                    text = senderLabel,
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(start = 8.dp, bottom = 3.dp)
                                )
                            }

                            Surface(
                                modifier = Modifier
                                    .clip(bubbleShape)
                                    .combinedClickable(
                                        onClick = {
                                            if (isSelectionMode) toggleMessageSelection(message.id)
                                        },
                                        onDoubleClick = {
                                            if (isSelectionMode) {
                                                toggleMessageSelection(message.id)
                                            } else {
                                                runCatching { haptic.performHapticFeedback(HapticFeedbackType.LongPress) }
                                                onReaction(message.id, "❤️")
                                            }
                                        },
                                        onLongClick = openActionMenu
                                    ),
                                color = if (mine) sentBubbleColor else receivedBubbleColor,
                                shape = bubbleShape,
                                border = when {
                                    isSelected -> BorderStroke(2.dp, if (mine) Color.White else LibraLavender)
                                    mine -> null
                                    else -> receivedBubbleBorder
                                }
                            ) {
                                val primaryTextColor = if (mine) sentBubbleTextColor else MaterialTheme.colorScheme.onSurface
                                val secondaryTextColor = if (mine) sentTimestampColor else MaterialTheme.colorScheme.onSurfaceVariant

                                Column(modifier = Modifier.padding(horizontal = 8.dp, vertical = 7.dp)) {
                                    if (replyPreview) {
                                        val quoteBg = if (mine) {
                                            Color(0xFF5B21B6)
                                        } else if (darkTheme) {
                                            MaterialTheme.colorScheme.surfaceVariant
                                        } else {
                                            Color(0xFFF5F3FF)
                                        }
                                        val quoteTitleColor = if (mine) Color.White else if (darkTheme) LibraLavenderDarkText else LibraLavenderText
                                        val quoteBodyColor = if (mine) Color(0xFFEDE9FE) else MaterialTheme.colorScheme.onSurfaceVariant
                                        val replySenderTitle = message.replyToSenderName.ifBlank {
                                            if (message.replyToSenderId == currentUid) "Sen" else user.displayName.ifBlank { "Yanıtlanan mesaj" }
                                        }

                                        Surface(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(bottom = 6.dp)
                                                .clip(RoundedCornerShape(12.dp))
                                                .clickable {
                                                    if (isSelectionMode) {
                                                        toggleMessageSelection(message.id)
                                                    } else {
                                                        val targetIdx = findReplyTargetIndex(
                                                            messages = messages,
                                                            replyToMessageId = message.replyToMessageId
                                                        ) { it.id }
                                                        if (targetIdx >= 0) {
                                                            runCatching { haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove) }
                                                            scope.launch {
                                                                listState.animateScrollToItem(targetIdx)
                                                                highlightedMessageId = message.replyToMessageId
                                                                highlightNonce++
                                                            }
                                                        } else {
                                                            Toast.makeText(
                                                                context,
                                                                "Alıntılanan mesaj silinmiş veya sohbet geçmişinde yok.",
                                                                Toast.LENGTH_SHORT
                                                            ).show()
                                                        }
                                                    }
                                                },
                                            shape = RoundedCornerShape(12.dp),
                                            color = quoteBg
                                        ) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(horizontal = 9.dp, vertical = 7.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .width(3.dp)
                                                        .height(28.dp)
                                                        .clip(RoundedCornerShape(3.dp))
                                                        .background(if (mine) Color.White else LibraLavender)
                                                )
                                                Spacer(Modifier.width(8.dp))
                                                Column {
                                                    Text(
                                                        text = replySenderTitle,
                                                        color = quoteTitleColor,
                                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                    Text(
                                                        text = message.replyToText.ifBlank { "📷 Fotoğraf" },
                                                        maxLines = 2,
                                                        overflow = TextOverflow.Ellipsis,
                                                        color = quoteBodyColor,
                                                        style = MaterialTheme.typography.bodySmall
                                                    )
                                                }
                                            }
                                        }
                                    }

                                    message.sharedContent?.let { shared ->
                                        var sharedUrl by remember(shared.id, shared.mediaUrl) { mutableStateOf<String?>(null) }
                                        var showSharedImageFullscreen by remember(shared.id) { mutableStateOf(false) }

                                        LaunchedEffect(shared.id, shared.mediaUrl) {
                                            if (shared.mediaUrl.isNotBlank()) {
                                                sharedUrl = runCatching {
                                                    ServiceLocator.storageRepository.getSignedMediaUrl(shared.mediaUrl)
                                                }.getOrNull()
                                            }
                                        }

                                        val sharedCardBg = if (mine) Color(0xFF5B21B6) else MaterialTheme.colorScheme.surfaceVariant
                                        Surface(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(bottom = 4.dp),
                                            shape = RoundedCornerShape(14.dp),
                                            color = sharedCardBg,
                                            border = if (mine) null else BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                                        ) {
                                            Column(
                                                modifier = Modifier.padding(9.dp),
                                                verticalArrangement = Arrangement.spacedBy(5.dp)
                                            ) {
                                                Text(
                                                    text = when (shared.type) {
                                                        "story" -> "Hikâyeden bahsetti"
                                                        "post" -> "Bir gönderi paylaştı"
                                                        "book" -> "Bir kitap paylaştı"
                                                        "dm_photo" -> "İletilen fotoğraf"
                                                        "message" -> "İletilen mesaj"
                                                        else -> "Paylaşılan içerik"
                                                    },
                                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                                    color = if (mine) Color(0xFFDDD6FE) else LibraLavender
                                                )
                                                sharedUrl?.takeIf { it.isNotBlank() }?.let { resolvedSharedImg ->
                                                    AsyncImage(
                                                        model = resolvedSharedImg,
                                                        contentDescription = "Paylaşılan medya",
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .height(140.dp)
                                                            .clip(RoundedCornerShape(10.dp))
                                                            .combinedClickable(
                                                                onClick = {
                                                                    if (isSelectionMode) toggleMessageSelection(message.id)
                                                                    else showSharedImageFullscreen = true
                                                                },
                                                                onDoubleClick = {
                                                                    if (isSelectionMode) toggleMessageSelection(message.id)
                                                                    else onReaction(message.id, "❤️")
                                                                },
                                                                onLongClick = openActionMenu
                                                            ),
                                                        contentScale = ContentScale.Crop
                                                    )
                                                    if (showSharedImageFullscreen) {
                                                        FullScreenImageViewerDialog(
                                                            mediaUrl = resolvedSharedImg,
                                                            onDismiss = { showSharedImageFullscreen = false }
                                                        )
                                                    }
                                                }
                                                if (shared.title.isNotBlank()) {
                                                    Text(
                                                        text = shared.title,
                                                        color = primaryTextColor,
                                                        fontWeight = FontWeight.SemiBold,
                                                        style = MaterialTheme.typography.bodyMedium
                                                    )
                                                }
                                                if (shared.text.isNotBlank()) {
                                                    Text(
                                                        text = shared.text,
                                                        color = secondaryTextColor,
                                                        maxLines = 3,
                                                        overflow = TextOverflow.Ellipsis,
                                                        style = MaterialTheme.typography.bodySmall
                                                    )
                                                }
                                            }
                                        }
                                    }

                                    if (message.mediaUrl.isNotBlank()) {
                                        var resolvedMediaUrl by remember(message.mediaUrl) { mutableStateOf<String?>(null) }
                                        var signingDone by remember(message.mediaUrl) { mutableStateOf(false) }
                                        var mediaFailed by remember(message.mediaUrl) { mutableStateOf(false) }
                                        var reloadKey by remember(message.mediaUrl) { mutableIntStateOf(0) }
                                        var showDmImageFullscreen by remember(message.mediaUrl) { mutableStateOf(false) }

                                        LaunchedEffect(message.mediaUrl, reloadKey) {
                                            signingDone = false
                                            mediaFailed = false
                                            while (true) {
                                                val fetched = runCatching {
                                                    ServiceLocator.storageRepository.getSignedMediaUrl(message.mediaUrl)
                                                }.getOrNull()
                                                resolvedMediaUrl = fetched
                                                signingDone = true
                                                if (fetched.isNullOrBlank()) {
                                                    mediaFailed = true
                                                    break
                                                }
                                                delay(9 * 60 * 1000L)
                                            }
                                        }

                                        val imgShape = RoundedCornerShape(15.dp)
                                        when {
                                            !signingDone -> {
                                                Surface(
                                                    modifier = Modifier
                                                        .width(230.dp)
                                                        .height(165.dp),
                                                    shape = imgShape,
                                                    color = if (mine) Color(0xFF5B21B6) else MaterialTheme.colorScheme.surfaceVariant
                                                ) {
                                                    Box(contentAlignment = Alignment.Center) {
                                                        Column(
                                                            horizontalAlignment = Alignment.CenterHorizontally,
                                                            verticalArrangement = Arrangement.spacedBy(8.dp)
                                                        ) {
                                                            CircularProgressIndicator(
                                                                modifier = Modifier.size(24.dp),
                                                                strokeWidth = 2.5.dp,
                                                                color = primaryTextColor
                                                            )
                                                            Text(
                                                                text = "Fotoğraf yükleniyor…",
                                                                style = MaterialTheme.typography.labelSmall,
                                                                color = secondaryTextColor
                                                            )
                                                        }
                                                    }
                                                }
                                            }

                                            mediaFailed || resolvedMediaUrl.isNullOrBlank() -> {
                                                Surface(
                                                    modifier = Modifier
                                                        .width(230.dp)
                                                        .height(140.dp)
                                                        .clip(imgShape)
                                                        .clickable { reloadKey++ },
                                                    shape = imgShape,
                                                    color = if (mine) Color(0xFF5B21B6) else MaterialTheme.colorScheme.surfaceVariant
                                                ) {
                                                    Box(contentAlignment = Alignment.Center) {
                                                        Column(
                                                            horizontalAlignment = Alignment.CenterHorizontally,
                                                            verticalArrangement = Arrangement.spacedBy(6.dp)
                                                        ) {
                                                            Icon(
                                                                imageVector = Icons.Default.Refresh,
                                                                contentDescription = "Tekrar yükle",
                                                                tint = primaryTextColor
                                                            )
                                                            Text(
                                                                text = "Fotoğraf yüklenemedi • Dokun",
                                                                style = MaterialTheme.typography.labelSmall,
                                                                color = secondaryTextColor
                                                            )
                                                        }
                                                    }
                                                }
                                            }

                                            else -> {
                                                val dmImgUrl = resolvedMediaUrl!!
                                                AsyncImage(
                                                    model = dmImgUrl,
                                                    contentDescription = "Gönderilen fotoğraf",
                                                    modifier = Modifier
                                                        .widthIn(min = 170.dp, max = 250.dp)
                                                        .heightIn(min = 120.dp, max = 290.dp)
                                                        .clip(imgShape)
                                                        .combinedClickable(
                                                            onClick = {
                                                                if (isSelectionMode) toggleMessageSelection(message.id)
                                                                else showDmImageFullscreen = true
                                                            },
                                                            onDoubleClick = {
                                                                if (isSelectionMode) {
                                                                    toggleMessageSelection(message.id)
                                                                } else {
                                                                    runCatching { haptic.performHapticFeedback(HapticFeedbackType.LongPress) }
                                                                    onReaction(message.id, "❤️")
                                                                }
                                                            },
                                                            onLongClick = openActionMenu
                                                        ),
                                                    contentScale = ContentScale.Crop,
                                                    onError = { mediaFailed = true }
                                                )

                                                if (showDmImageFullscreen) {
                                                    FullScreenImageViewerDialog(
                                                        mediaUrl = dmImgUrl,
                                                        onDismiss = { showDmImageFullscreen = false }
                                                    )
                                                }
                                            }
                                        }
                                    }

                                    if (message.text.isNotBlank()) {
                                        Text(
                                            text = message.text,
                                            color = primaryTextColor,
                                            style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 20.sp),
                                            modifier = Modifier.padding(
                                                start = 4.dp,
                                                end = 4.dp,
                                                top = if (message.mediaUrl.isNotBlank()) 6.dp else 1.dp,
                                                bottom = 1.dp
                                            )
                                        )
                                    }

                                    // Compact time + edited + seen receipt indicator row
                                    val messageSeen = isMessageSeenByPeer(message, peerLastReadAt, currentUid)
                                    Row(
                                        modifier = Modifier
                                            .align(Alignment.End)
                                            .padding(top = 2.dp, start = 4.dp, end = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        if (message.editedAt != null) {
                                            Text(
                                                text = "düzenlendi •",
                                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                                color = secondaryTextColor
                                            )
                                        }
                                        if (message.createdAt > 0L) {
                                            Text(
                                                text = formatMessageHour(message.createdAt),
                                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                                color = secondaryTextColor
                                            )
                                        }
                                        if (message.id.startsWith("local-")) {
                                            CircularProgressIndicator(
                                                modifier = Modifier.size(9.dp),
                                                strokeWidth = 1.5.dp,
                                                color = secondaryTextColor
                                            )
                                        } else if (mine) {
                                            Icon(
                                                imageVector = if (messageSeen) Icons.Default.DoneAll else Icons.Default.Check,
                                                contentDescription = if (messageSeen) "Görüldü" else "Gönderildi",
                                                modifier = Modifier.size(13.dp),
                                                tint = if (messageSeen) Color(0xFF93C5FD) else secondaryTextColor
                                            )
                                        }
                                    }
                                }
                            }

                            if (mine && message.id == lastSentMessageId &&
                                isMessageSeenByPeer(message, peerLastReadAt, currentUid)
                            ) {
                                Row(
                                    modifier = Modifier.padding(top = 3.dp, end = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.DoneAll,
                                        contentDescription = null,
                                        modifier = Modifier.size(13.dp),
                                        tint = if (darkTheme) LibraLavenderDarkText else LibraLavenderText
                                    )
                                    Text(
                                        text = "Görüldü",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.SemiBold
                                        ),
                                        color = if (darkTheme) LibraLavenderDarkText else LibraLavenderText
                                    )
                                }
                            }

                            // Overlapping Reaction Pill Badge
                            if (message.reactions.isNotEmpty()) {
                                val counts = remember(message.reactions) {
                                    message.reactions.values.groupingBy { it }.eachCount()
                                }
                                Surface(
                                    modifier = Modifier
                                        .offset(y = (-6).dp)
                                        .padding(horizontal = 8.dp),
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.surface,
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                                    shadowElevation = 2.dp
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                        horizontalArrangement = Arrangement.spacedBy(5.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        counts.forEach { (emoji, count) ->
                                            Row(
                                                modifier = Modifier
                                                    .clip(CircleShape)
                                                    .clickable { onReaction(message.id, emoji) }
                                                    .padding(horizontal = 2.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(2.dp)
                                            ) {
                                                Text(emoji, fontSize = 13.sp)
                                                if (count > 1) {
                                                    Text(
                                                        text = count.toString(),
                                                        style = MaterialTheme.typography.labelSmall.copy(
                                                            fontWeight = FontWeight.Bold,
                                                            fontSize = 11.sp
                                                        ),
                                                        color = if (emoji == myReaction) {
                                                            if (darkTheme) LibraLavenderDarkText else LibraLavenderText
                                                        } else {
                                                            MaterialTheme.colorScheme.onSurfaceVariant
                                                        }
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
            }

            AnimatedVisibility(
                visible = showScrollToBottom && messages.isNotEmpty(),
                enter = fadeIn() + scaleIn(initialScale = 0.85f),
                exit = fadeOut() + scaleOut(targetScale = 0.85f),
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 14.dp, bottom = 12.dp)
            ) {
                Surface(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .appleBouncy(
                            pressedScale = 0.86f,
                            onClick = {
                                if (messages.isNotEmpty()) {
                                    scope.launch { listState.animateScrollToItem(messages.lastIndex) }
                                }
                            }
                        ),
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                    shadowElevation = 4.dp
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowDown,
                            contentDescription = "En alta git",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }

        // Photo Upload Attachment Dock
        AnimatedVisibility(
            visible = pendingMediaUri != null,
            enter = fadeIn() + scaleIn(initialScale = 0.94f, animationSpec = AppleSprings.snappy()),
            exit = fadeOut() + scaleOut(targetScale = 0.94f)
        ) {
            pendingMediaUri?.let { localUri ->
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 4.dp),
                    shape = RoundedCornerShape(18.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                    shadowElevation = 4.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            AsyncImage(
                                model = localUri,
                                contentDescription = "Gönderilecek fotoğraf",
                                modifier = Modifier
                                    .size(66.dp)
                                    .clip(RoundedCornerShape(13.dp)),
                                contentScale = ContentScale.Crop
                            )
                            if (mediaUploading) {
                                Surface(
                                    shape = CircleShape,
                                    color = Color.Black.copy(alpha = 0.65f),
                                    modifier = Modifier.size(34.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        CircularProgressIndicator(
                                            progress = { (mediaProgress / 100f).coerceIn(0.05f, 1f) },
                                            modifier = Modifier.size(22.dp),
                                            strokeWidth = 2.5.dp,
                                            color = Color.White
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = when {
                                    mediaError != null -> "Yükleme başarısız"
                                    mediaUploading -> if (sendWhenUploadReady) {
                                        "Yükleniyor (%$mediaProgress) — bitince gönderilecek"
                                    } else {
                                        "Fotoğraf yükleniyor %$mediaProgress"
                                    }
                                    pendingMediaUrl.isNotBlank() -> "Fotoğraf gönderime hazır ✓"
                                    else -> "Fotoğraf optimize ediliyor…"
                                },
                                fontWeight = FontWeight.SemiBold,
                                style = MaterialTheme.typography.labelLarge,
                                color = if (mediaError != null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                            )

                            val metaParts = listOfNotNull(
                                pendingMediaResolution.takeIf { it.isNotBlank() },
                                pendingMediaSizeBytes.takeIf { it > 0 }?.let { formatReadableByteSize(it) }
                            )
                            if (mediaError != null) {
                                Text(
                                    text = mediaError!!,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.error,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            } else if (metaParts.isNotEmpty()) {
                                Text(
                                    text = metaParts.joinToString(" • "),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            if (mediaUploading) {
                                LinearProgressIndicator(
                                    progress = { (mediaProgress / 100f).coerceIn(0f, 1f) },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 6.dp)
                                        .height(4.dp)
                                        .clip(CircleShape),
                                    color = LibraLavender
                                )
                            }
                        }

                        if (mediaError != null) {
                            IconButton(onClick = { startImageUpload(localUri) }) {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = "Tekrar Dene",
                                    tint = LibraLavender
                                )
                            }
                        }

                        IconButton(onClick = {
                            mediaUploadJob?.cancel()
                            sendWhenUploadReady = false
                            pendingMediaUri = null
                            pendingMediaUrl = ""
                            pendingMediaSizeBytes = 0
                            pendingMediaResolution = ""
                            mediaError = null
                            mediaUploading = false
                            mediaProgress = 0
                        }) {
                            Icon(Icons.Default.Close, contentDescription = "Fotoğrafı kaldır")
                        }
                    }
                }
            }
        }

        replyTarget?.let { target ->
            val replyName = target.senderName.ifBlank {
                if (target.senderId == currentUid) "Sen" else user.displayName
            }
            RichReplyBanner(
                senderName = "Yanıtlanıyor: $replyName",
                text = target.text.ifBlank { "📷 Fotoğraf" },
                onCancel = { replyTarget = null }
            )
        }

        editingMessage?.let { editing ->
            RichReplyBanner(
                senderName = "Mesaj düzenleniyor",
                text = editing.text,
                onCancel = {
                    editingMessage = null
                    draft = ""
                }
            )
        }

        // Instagram-Style Pill Composer Dock
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.navigationBars.union(WindowInsets.ime))
                .padding(horizontal = 12.dp, vertical = 10.dp),
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
            shadowElevation = 2.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 6.dp, vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val canAttach = canMessage && !sending && editingMessage == null
                val galleryCircleColor = if (darkTheme) LibraLavenderDarkSurface else LibraLavenderSurface
                val galleryIconTint = if (darkTheme) LibraLavenderDarkText else LibraLavenderText

                Surface(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .appleBouncy(enabled = canAttach, pressedScale = 0.86f, onClick = { launchPhotoPicker() }),
                    shape = CircleShape,
                    color = if (canAttach) galleryCircleColor else MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.AddPhotoAlternate,
                            contentDescription = "Fotoğraf ekle",
                            tint = if (canAttach) galleryIconTint else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(21.dp)
                        )
                    }
                }

                TextField(
                    value = draft,
                    onValueChange = { if (canMessage && it.length <= 2000) draft = it },
                    modifier = Modifier.weight(1f),
                    enabled = canMessage,
                    placeholder = {
                        Text(
                            text = when {
                                !canMessage -> "Mesaj gönderme engellendi"
                                editingMessage != null -> "Mesajı düzenle…"
                                pendingMediaUri != null -> "Fotoğrafa açıklama ekle…"
                                else -> "Mesaj yaz…"
                            },
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    maxLines = 4,
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        disabledContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        disabledIndicatorColor = Color.Transparent
                    )
                )

                val onSendClick: () -> Unit = {
                    val text = draft.trim()
                    val editing = editingMessage
                    when {
                        editing != null -> {
                            if (text.isNotBlank()) {
                                onEdit(editing.id, text)
                                editingMessage = null
                                draft = ""
                            }
                        }
                        mediaUploading && pendingMediaUri != null -> {
                            sendWhenUploadReady = true
                        }
                        pendingMediaUrl.isNotBlank() -> {
                            sending = true
                            onSendMedia(
                                pendingMediaUrl,
                                pendingMediaType,
                                text,
                                replyTarget
                            ) { success ->
                                sending = false
                                if (success) {
                                    pendingMediaUri = null
                                    pendingMediaUrl = ""
                                    pendingMediaSizeBytes = 0
                                    pendingMediaResolution = ""
                                    mediaError = null
                                    mediaProgress = 0
                                    replyTarget = null
                                    draft = ""
                                }
                            }
                        }
                        text.isNotBlank() -> {
                            sending = true
                            onSend(text, replyTarget) { success ->
                                sending = false
                                if (success) {
                                    replyTarget = null
                                    draft = ""
                                }
                            }
                        }
                    }
                }

                val canSend = canMessage && !sending && (
                    draft.isNotBlank() ||
                        pendingMediaUrl.isNotBlank() ||
                        (mediaUploading && pendingMediaUri != null)
                    )

                Surface(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .appleBouncy(enabled = canSend, pressedScale = 0.84f, onClick = onSendClick),
                    shape = CircleShape,
                    color = if (canSend) LibraLavender else MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        when {
                            sending || sendWhenUploadReady -> {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    strokeWidth = 2.dp,
                                    color = Color.White
                                )
                            }
                            editingMessage != null -> {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Kaydet",
                                    tint = if (canSend) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            else -> {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.Send,
                                    contentDescription = "Gönder",
                                    tint = if (canSend) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(19.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    actionMessage?.let { message ->
        val mine = message.senderId == currentUid
        val isPendingLocal = message.id.startsWith("local-")
        val shareableText = resolveShareableMessageText(message)
        RichMessageActionSheet(
            canEdit = mine && !isPendingLocal && message.mediaUrl.isBlank() && message.sharedContent == null,
            canDelete = !isPendingLocal,
            previewText = message.text,
            previewMediaUrl = message.mediaUrl.ifBlank { message.sharedContent?.mediaUrl.orEmpty() },
            previewSharedTitle = message.sharedContent?.title.orEmpty(),
            previewTimestamp = buildString {
                if (message.createdAt > 0L) append(formatMessageHour(message.createdAt))
                if (message.editedAt != null) {
                    if (isNotEmpty()) append(" • ")
                    append("düzenlendi")
                }
            },
            isOwnMessage = mine,
            selectedReaction = message.reactions[currentUid],
            onDismiss = { actionMessage = null },
            onReply = {
                replyTarget = message
                actionMessage = null
            },
            onSelectMultiple = if (!isPendingLocal) {
                {
                    actionMessage = null
                    isSelectionMode = true
                    selectedMessageIds = setOf(message.id)
                }
            } else null,
            onReaction = { emoji ->
                onReaction(message.id, emoji)
                actionMessage = null
            },
            onCopy = if (shareableText.isNotBlank()) {
                {
                    clipboard.setText(AnnotatedString(shareableText))
                    Toast.makeText(context, "Mesaj kopyalandı.", Toast.LENGTH_SHORT).show()
                    actionMessage = null
                }
            } else null,
            onShare = {
                shareTargetMessage = message
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
                Toast.makeText(context, "Mesaj kalıcı olarak silindi.", Toast.LENGTH_SHORT).show()
                actionMessage = null
            }
        )
    }

    if (showBatchDeleteConfirm) {
        val chosen = messages.filter { it.id in selectedMessageIds }
        val ownCount = chosen.count { it.senderId == currentUid }
        val peerCount = (chosen.size - ownCount).coerceAtLeast(0)
        val confirmBody = when {
            peerCount == 0 -> "Seçilen $ownCount mesajın ve varsa medyaları kalıcı olarak herkesten silinecek."
            ownCount == 0 -> "Seçilen $peerCount gelen mesaj sohbetinden kalıcı olarak temizlenecek."
            else -> "Seçilen mesajlardan senin gönderdiğin $ownCount mesaj herkesten kalıcı silinecek, gelen $peerCount mesaj ise sohbetinden temizlenecek."
        }
        AlertDialog(
            onDismissRequest = { showBatchDeleteConfirm = false },
            containerColor = MaterialTheme.colorScheme.surface,
            title = { Text("Seçilen Mesajları Sil (${chosen.size})", fontWeight = FontWeight.Bold) },
            text = { Text(confirmBody) },
            confirmButton = {
                Button(
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    onClick = {
                        val ids = selectedMessageIds.toList()
                        showBatchDeleteConfirm = false
                        isSelectionMode = false
                        selectedMessageIds = emptySet()
                        onDeleteBatch(ids) { ok ->
                            if (ok) Toast.makeText(context, "${ids.size} mesaj silindi.", Toast.LENGTH_SHORT).show()
                        }
                    }
                ) { Text("Kalıcı Sil") }
            },
            dismissButton = {
                TextButton(onClick = { showBatchDeleteConfirm = false }) { Text("Vazgeç") }
            }
        )
    }

    if (showClearChatConfirm) {
        AlertDialog(
            onDismissRequest = { showClearChatConfirm = false },
            containerColor = MaterialTheme.colorScheme.surface,
            title = { Text("Sohbeti Sıfırla", fontWeight = FontWeight.Bold) },
            text = {
                Text("Bu sohbette gönderdiğin tüm mesajlar ve fotoğraflar kalıcı olarak silinecek, tüm sohbet geçmişin sıfırlanacak.")
            },
            confirmButton = {
                Button(
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    onClick = {
                        showClearChatConfirm = false
                        onClearConversation { ok ->
                            if (ok) {
                                Toast.makeText(context, "Sohbet tamamen sıfırlandı.", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                ) { Text("Sıfırla") }
            },
            dismissButton = {
                TextButton(onClick = { showClearChatConfirm = false }) { Text("Vazgeç") }
            }
        )
    }

    shareTargetMessage?.let { message ->
        ForwardOrShareMessageSheet(
            message = message,
            conversations = conversations.filterNot { it.id == conversationId },
            onDismiss = { shareTargetMessage = null },
            onForwardToConversation = { targetConv, done ->
                onForwardMessage(message, targetConv, false, done)
            },
            onForwardToGlobalChat = { done ->
                onForwardMessage(message, null, true, done)
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ForwardOrShareMessageSheet(
    message: DirectMessage,
    conversations: List<DirectConversation>,
    onDismiss: () -> Unit,
    onForwardToConversation: (DirectConversation, (Boolean) -> Unit) -> Unit,
    onForwardToGlobalChat: ((Boolean) -> Unit) -> Unit
) {
    val darkTheme = isSystemInDarkTheme()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var query by remember { mutableStateOf("") }
    var sendingTargetKey by remember { mutableStateOf<String?>(null) }
    val sentKeys = remember { mutableStateOf(setOf<String>()) }
    var sharingExternally by remember { mutableStateOf(false) }

    val filteredConversations = remember(conversations, query) {
        val clean = query.trim()
        if (clean.isBlank()) conversations
        else conversations.filter {
            it.otherUserName.contains(clean, ignoreCase = true) ||
                it.otherUserUsername.contains(clean, ignoreCase = true) ||
                it.groupName.contains(clean, ignoreCase = true)
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 18.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Mesajı Paylaş / İlet",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Kapat")
                }
            }

            // Preview card
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = if (message.mediaUrl.isNotBlank()) "📷 Fotoğraflı mesaj" else "Mesaj önizlemesi",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = if (darkTheme) LibraLavenderDarkText else LibraLavenderText
                    )
                    Spacer(Modifier.height(3.dp))
                    Text(
                        text = resolveShareableMessageText(message).ifBlank { "Fotoğraf" },
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // External share button
            OutlinedButton(
                enabled = !sharingExternally,
                onClick = {
                    sharingExternally = true
                    scope.launch {
                        shareDirectMessageExternally(context, message)
                        sharingExternally = false
                        onDismiss()
                    }
                },
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                if (sharingExternally) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                } else {
                    Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                }
                Spacer(Modifier.width(8.dp))
                Text("Diğer Uygulamalarda Paylaş")
            }

            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                placeholder = { Text("Sohbet veya kişi ara…") },
                leadingIcon = { Icon(Icons.Default.Search, null) },
                shape = RoundedCornerShape(14.dp)
            )

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 280.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item(key = "global_forward") {
                    val isSent = "global" in sentKeys.value
                    val isBusy = sendingTargetKey == "global"
                    ForwardTargetRow(
                        title = "Genel Chat",
                        subtitle = "Tüm Libra üyeleriyle paylaş",
                        photoUrl = "",
                        isGlobal = true,
                        isSent = isSent,
                        isBusy = isBusy,
                        onSend = {
                            sendingTargetKey = "global"
                            onForwardToGlobalChat { ok ->
                                sendingTargetKey = null
                                if (ok) {
                                    sentKeys.value = sentKeys.value + "global"
                                    Toast.makeText(context, "Genel Chate iletildi.", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    )
                }

                items(filteredConversations, key = { it.id }) { conv ->
                    val title = if (conv.isGroup) conv.groupName.ifBlank { "Grup" } else conv.otherUserName.ifBlank { "Libra Üyesi" }
                    val subtitle = if (conv.isGroup) "${conv.participantIds.size} kişi" else "@${conv.otherUserUsername}"
                    val photo = if (conv.isGroup) conv.groupPhotoUrl else conv.otherUserPhotoUrl
                    val isSent = conv.id in sentKeys.value
                    val isBusy = sendingTargetKey == conv.id

                    ForwardTargetRow(
                        title = title,
                        subtitle = subtitle,
                        photoUrl = photo,
                        isGlobal = false,
                        isSent = isSent,
                        isBusy = isBusy,
                        onSend = {
                            sendingTargetKey = conv.id
                            onForwardToConversation(conv) { ok ->
                                sendingTargetKey = null
                                if (ok) {
                                    sentKeys.value = sentKeys.value + conv.id
                                    Toast.makeText(context, "$title sohbetine iletildi.", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    )
                }
            }

            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun ForwardTargetRow(
    title: String,
    subtitle: String,
    photoUrl: String,
    isGlobal: Boolean,
    isSent: Boolean,
    isBusy: Boolean,
    onSend: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isGlobal) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.size(40.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Public, contentDescription = null, modifier = Modifier.size(20.dp))
                    }
                }
            } else {
                UserAvatar(photoUrl = photoUrl, initials = title.take(1).uppercase(), size = 40.dp)
            }
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 10.dp)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (subtitle.isNotBlank()) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            Button(
                enabled = !isBusy && !isSent,
                onClick = onSend,
                shape = CircleShape,
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isSent) MaterialTheme.colorScheme.surfaceVariant else LibraLavender
                )
            ) {
                if (isBusy) {
                    CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp, color = Color.White)
                } else {
                    Text(
                        text = if (isSent) "İletildi ✓" else "Gönder",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = if (isSent) MaterialTheme.colorScheme.onSurfaceVariant else Color.White
                    )
                }
            }
        }
    }
}

@Composable
private fun SectionButton(text: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(13.dp))
            .background(if (selected) MaterialTheme.colorScheme.onBackground else MaterialTheme.colorScheme.surface)
            .appleBouncy(pressedScale = 0.93f, onClick = onClick)
            .padding(vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            color = if (selected) MaterialTheme.colorScheme.background else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun CommunityList(onGlobalChatClick: () -> Unit, onServersClick: () -> Unit) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Column {
                Text(
                    "Topluluklar",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "Sohbet et, toplulukları keşfet ve kendi alanlarını bul.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        item {
            CommunityFeatureCard(
                icon = Icons.Default.Forum,
                eyebrow = "ORTAK SOHBET",
                title = "Genel Chat",
                subtitle = "Tüm Libra üyeleriyle aynı sohbette buluş.",
                containerColor = MaterialTheme.colorScheme.surface,
                iconContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                onClick = onGlobalChatClick
            )
        }

        item {
            CommunityFeatureCard(
                icon = Icons.Default.Groups,
                eyebrow = "TOPLULUKLARI KEŞFET",
                title = "Sunucular",
                subtitle = "Kitap, yazarlık ve ilgi alanlarına göre topluluklara katıl.",
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                iconContainerColor = MaterialTheme.colorScheme.surface,
                onClick = onServersClick,
                large = true
            )
        }
    }
}

@Composable
private fun CommunityFeatureCard(
    icon: ImageVector,
    eyebrow: String,
    title: String,
    subtitle: String,
    containerColor: Color,
    iconContainerColor: Color,
    onClick: () -> Unit,
    large: Boolean = false
) {
    val shape = RoundedCornerShape(if (large) 24.dp else 20.dp)
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .appleBouncy(pressedScale = 0.97f, onClick = onClick),
        shape = shape,
        color = containerColor,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = if (large) 22.dp else 18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(if (large) 60.dp else 54.dp)
                    .clip(CircleShape)
                    .background(iconContainerColor),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    modifier = Modifier.size(if (large) 28.dp else 25.dp),
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }

            Spacer(Modifier.width(14.dp))

            Column(Modifier.weight(1f)) {
                Text(
                    eyebrow,
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.height(3.dp))
                Text(
                    title,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    subtitle,
                    maxLines = if (large) 3 else 2,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(Modifier.width(8.dp))
            Icon(
                Icons.Default.ChevronRight,
                contentDescription = "Aç",
                tint = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
