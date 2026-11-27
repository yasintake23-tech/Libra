package com.libra.app.feature.messages

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Reply
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircleOutline
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material.icons.rounded.Forum
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.graphics.graphicsLayer
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
import com.libra.app.domain.model.GlobalChatMessage
import com.libra.app.domain.model.StorageUploadRequest
import com.libra.app.ui.animation.appleBouncy
import com.libra.app.ui.components.FullScreenImageViewerDialog
import com.libra.app.ui.components.UserAvatar
import com.libra.app.ui.theme.LocalLibraDarkTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

private const val GLOBAL_LIBRA_FOUNDER_UID = "ZBkz2js9plg07zrny2PzLW80X3i2"

class GlobalChatViewModel : ViewModel() {
    private val repository = ServiceLocator.chatRepository
    private val _messages = MutableStateFlow<List<GlobalChatMessage>>(emptyList())
    val messages = _messages.asStateFlow()
    private val _isLoading = MutableStateFlow(true)
    val isLoading = _isLoading.asStateFlow()
    private val _error = MutableStateFlow<String?>(null)
    val error = _error.asStateFlow()
    private var observeJob: Job? = null

    init {
        refresh()
    }

    fun refresh() {
        observeJob?.cancel()
        _isLoading.value = _messages.value.isEmpty()
        _error.value = null
        observeJob = viewModelScope.launch {
            repository.observeGlobalMessages().collect { result ->
                _isLoading.value = false
                when (result) {
                    is AppResult.Success -> {
                        _messages.value = result.data
                        _error.value = null
                    }
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

    fun deleteBatch(ids: List<String>, onComplete: (Boolean) -> Unit = {}) = viewModelScope.launch {
        when (val result = repository.deleteGlobalMessages(ids)) {
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

    fun react(id: String, emoji: String) = viewModelScope.launch {
        when (val result = repository.toggleGlobalMessageReaction(id, emoji)) {
            is AppResult.Success -> _error.value = null
            is AppResult.Error -> _error.value = result.error.message
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun GlobalChatScreen(
    onBack: () -> Unit = {},
    viewModel: GlobalChatViewModel = viewModel(),
    onOpenProfile: (String) -> Unit = {},
    canMessage: Boolean = true,
    isAdminModerator: Boolean = false,
    embeddedInHub: Boolean = false,
    externalSearchQuery: String = "",
    listState: LazyListState = rememberLazyListState(),
    modifier: Modifier = Modifier
) {
    val palette = rememberLibraCommsColors()
    val darkTheme = LocalLibraDarkTheme.current
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    val currentUid = ServiceLocator.authRepository.currentUser.value?.uid.orEmpty()

    val messages by viewModel.messages.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
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
    var fullscreenImageKey by remember { mutableStateOf<String?>(null) }

    var isSelectionMode by remember { mutableStateOf(false) }
    var selectedMessageIds by remember { mutableStateOf<Set<String>>(emptySet()) }
    var showBatchDeleteConfirm by remember { mutableStateOf(false) }
    var highlightedMessageId by remember { mutableStateOf<String?>(null) }
    var highlightNonce by remember { mutableIntStateOf(0) }

    // Coordinated room entrance animation (respects reduced motion)
    val reducedMotion = remember(context) { areSystemAnimationsReduced(context) }
    val entranceProgress = remember { Animatable(if (reducedMotion) 1f else 0f) }
    LaunchedEffect(Unit) {
        if (!reducedMotion) {
            entranceProgress.animateTo(1f, tween(340, easing = FastOutSlowInEasing))
        }
    }

    BackHandler(enabled = isSelectionMode) {
        isSelectionMode = false
        selectedMessageIds = emptySet()
    }

    fun toggleSelection(messageId: String) {
        val next = if (messageId in selectedMessageIds) selectedMessageIds - messageId else selectedMessageIds + messageId
        selectedMessageIds = next
        if (next.isEmpty()) isSelectionMode = false
    }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        uri ?: return@rememberLauncherForActivityResult
        pendingUri = uri
        pendingUrl = ""
        mediaError = null
        uploading = true
        progress = 0
        scope.launch {
            try {
                val prepared = prepareChatImageForUpload(context, uri, prefix = "global")
                pendingType = prepared.contentType
                val request = StorageUploadRequest(
                    fileName = prepared.fileName,
                    bytes = prepared.bytes,
                    contentType = prepared.contentType,
                    targetDirectory = "users/$currentUid"
                )
                when (
                    val result = ServiceLocator.storageRepository.uploadMedia(request) { pct ->
                        scope.launch(Dispatchers.Main) { progress = pct }
                    }.first()
                ) {
                    is AppResult.Success -> {
                        pendingUrl = result.data
                        progress = 100
                    }
                    is AppResult.Error -> mediaError = result.error.message
                }
            } catch (e: Exception) {
                mediaError = e.localizedMessage ?: "Fotoğraf yüklenemedi."
            } finally {
                uploading = false
            }
        }
    }

    val filteredMessages = remember(messages, externalSearchQuery) {
        val q = externalSearchQuery.trim()
        if (q.isBlank()) messages
        else messages.filter {
            it.text.contains(q, ignoreCase = true) ||
                it.senderName.contains(q, ignoreCase = true) ||
                it.senderUsername.contains(q, ignoreCase = true)
        }
    }

    val activeParticipantCount = remember(messages) {
        messages.mapNotNull { it.senderId.takeIf { uid -> uid.isNotBlank() } }.distinct().size
    }

    val imeBottomPx = WindowInsets.ime.getBottom(LocalDensity.current)
    val lastMessageId = filteredMessages.lastOrNull()?.id.orEmpty()
    val showScrollToBottom by remember { derivedStateOf { listState.canScrollForward } }

    LaunchedEffect(lastMessageId, imeBottomPx) {
        if (filteredMessages.isNotEmpty() && !isSelectionMode && externalSearchQuery.isBlank()) {
            val lastVisible = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: -1
            if (imeBottomPx > 0 || lastVisible < 0 || lastVisible >= filteredMessages.lastIndex - 3) {
                listState.animateScrollToItem(filteredMessages.lastIndex)
            }
        }
    }

    LaunchedEffect(highlightedMessageId, highlightNonce) {
        if (highlightedMessageId != null) {
            delay(1700L)
            highlightedMessageId = null
        }
    }

    val deletableSelectedIds = remember(messages, selectedMessageIds, currentUid, isAdminModerator) {
        messages.filter { it.id in selectedMessageIds && (it.senderId == currentUid || isAdminModerator) }.map { it.id }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(palette.canvas)
    ) {
        // Community Room Header Bar (clean, distinct, non-bloated)
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .graphicsLayer {
                    val v = entranceProgress.value
                    alpha = v
                    translationY = (1f - v) * (-14f)
                },
            color = if (isSelectionMode) palette.communitySoft else palette.surface
        ) {
            Column {
                if (isSelectionMode) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .then(if (!embeddedInHub) Modifier.statusBarsPadding() else Modifier)
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { isSelectionMode = false; selectedMessageIds = emptySet() }) {
                            Icon(Icons.Default.Close, "Seçimi Kapat", tint = palette.inkPrimary)
                        }
                        Text(
                            text = "${selectedMessageIds.size} mesaj seçildi",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = palette.inkPrimary,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(
                            enabled = selectedMessageIds.isNotEmpty(),
                            onClick = {
                                val copied = messages.filter { it.id in selectedMessageIds }
                                    .sortedBy { it.createdAt }
                                    .mapNotNull { m ->
                                        m.text.trim().takeIf { it.isNotBlank() }
                                            ?.let { "${m.senderName.ifBlank { "Üye" }}: $it" }
                                    }
                                    .joinToString("\n")
                                if (copied.isNotBlank()) {
                                    clipboard.setText(AnnotatedString(copied))
                                    Toast.makeText(context, "Seçilen mesajlar kopyalandı.", Toast.LENGTH_SHORT).show()
                                }
                                isSelectionMode = false
                                selectedMessageIds = emptySet()
                            }
                        ) { Icon(Icons.Default.ContentCopy, "Kopyala", tint = palette.inkPrimary) }
                        IconButton(
                            enabled = deletableSelectedIds.isNotEmpty(),
                            onClick = { showBatchDeleteConfirm = true }
                        ) {
                            Icon(
                                Icons.Default.DeleteOutline,
                                "Sil",
                                tint = if (deletableSelectedIds.isNotEmpty()) MaterialTheme.colorScheme.error else palette.inkSecondary
                            )
                        }
                    }
                } else {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .then(if (!embeddedInHub) Modifier.statusBarsPadding() else Modifier)
                            .padding(horizontal = if (embeddedInHub) 16.dp else 8.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (!embeddedInHub) {
                            IconButton(onClick = onBack) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, "Geri", tint = palette.inkPrimary)
                            }
                        } else {
                            Surface(
                                shape = CircleShape,
                                color = palette.communitySoft,
                                modifier = Modifier.size(34.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Rounded.Forum,
                                        contentDescription = null,
                                        tint = palette.community,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                            Spacer(Modifier.width(10.dp))
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = "Genel Sohbet",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp
                                    ),
                                    color = palette.inkPrimary
                                )
                                if (isAdminModerator) {
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = palette.brandSoft
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(3.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Shield,
                                                contentDescription = null,
                                                tint = palette.brandText,
                                                modifier = Modifier.size(11.dp)
                                            )
                                            Text(
                                                text = "Mod",
                                                style = MaterialTheme.typography.labelSmall.copy(
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold
                                                ),
                                                color = palette.brandText
                                            )
                                        }
                                    }
                                }
                            }
                            Text(
                                text = when {
                                    externalSearchQuery.isNotBlank() -> "${filteredMessages.size} sonuç bulundu"
                                    activeParticipantCount > 0 -> "$activeParticipantCount aktif katılımcı • Tüm Libra topluluğu"
                                    else -> "Tüm Libra üyeleriyle ortak topluluk alanı"
                                },
                                style = MaterialTheme.typography.labelSmall,
                                color = palette.inkSecondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        if (messages.isNotEmpty()) {
                            IconButton(
                                onClick = { isSelectionMode = true },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircleOutline,
                                    contentDescription = if (isAdminModerator) "Moderasyon / Seç" else "Mesajları Seç",
                                    tint = palette.inkSecondary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
                HorizontalDivider(color = palette.hairline, thickness = 0.8.dp)
            }
        }

        // Error / Connection Recovery Strip
        error?.let { err ->
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 6.dp),
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.errorContainer
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.WifiOff, null, tint = MaterialTheme.colorScheme.onErrorContainer, modifier = Modifier.size(16.dp))
                    Text(
                        text = err,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.weight(1f)
                    )
                    TextButton(onClick = { viewModel.refresh() }) {
                        Text("Yenile", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onErrorContainer)
                    }
                }
            }
        }

        mediaError?.let { err ->
            Text(
                text = err,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 4.dp)
            )
        }

        // Messages Stream
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .graphicsLayer {
                    alpha = entranceProgress.value
                    translationY = (1f - entranceProgress.value) * 18f
                }
        ) {
            if (isLoading && messages.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = palette.community, strokeWidth = 2.4.dp)
                }
            } else if (filteredMessages.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 28.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = if (externalSearchQuery.isNotBlank()) "Aramana uyan mesaj yok" else "Genel Sohbet sessiz",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = palette.inkPrimary
                        )
                        Text(
                            text = if (externalSearchQuery.isNotBlank()) {
                                "Farklı bir kelime ya da kullanıcı adı dene."
                            } else {
                                "Libra topluluğuna ilk selamı sen verebilirsin."
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = palette.inkSecondary
                        )
                    }
                }
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    itemsIndexed(filteredMessages, key = { _, it -> it.id }) { index, message ->
                        val prev = filteredMessages.getOrNull(index - 1)
                        val next = filteredMessages.getOrNull(index + 1)
                        val mine = message.senderId == currentUid
                        val showDate = prev == null ||
                            (message.createdAt > 0L && prev.createdAt > 0L &&
                                formatChatTimeHeader(prev.createdAt) != formatChatTimeHeader(message.createdAt))
                        val groupedWithPrev = !showDate && prev != null &&
                            prev.senderId == message.senderId &&
                            (message.createdAt - prev.createdAt) in 0..300_000L
                        val groupedWithNext = next != null &&
                            next.senderId == message.senderId &&
                            (next.createdAt - message.createdAt) in 0..300_000L

                        var dragX by remember(message.id) { mutableFloatStateOf(0f) }
                        val isSelected = message.id in selectedMessageIds
                        val isHighlighted = highlightedMessageId == message.id
                        val rowBg by animateColorAsState(
                            targetValue = when {
                                isHighlighted -> palette.communitySoft
                                isSelected -> palette.brandSoft
                                else -> Color.Transparent
                            },
                            label = "global_msg_highlight"
                        )

                        Column(modifier = Modifier.animateItem()) {
                            if (showDate && message.createdAt > 0L) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Surface(
                                        shape = CircleShape,
                                        color = palette.surface,
                                        border = BorderStroke(0.8.dp, palette.hairline)
                                    ) {
                                        Text(
                                            text = formatChatTimeHeader(message.createdAt),
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                            color = palette.inkSecondary,
                                            modifier = Modifier.padding(horizontal = 11.dp, vertical = 4.dp)
                                        )
                                    }
                                }
                            } else if (!groupedWithPrev && index > 0) {
                                Spacer(Modifier.height(6.dp))
                            }

                            val bubbleShape = remember(mine, groupedWithPrev, groupedWithNext) {
                                val outer = 16.dp
                                val inner = 5.dp
                                if (mine) {
                                    RoundedCornerShape(
                                        topStart = outer,
                                        bottomStart = outer,
                                        topEnd = if (groupedWithPrev) inner else outer,
                                        bottomEnd = if (groupedWithNext) inner else outer
                                    )
                                } else {
                                    RoundedCornerShape(
                                        topStart = if (groupedWithPrev) inner else outer,
                                        bottomStart = if (groupedWithNext) inner else outer,
                                        topEnd = outer,
                                        bottomEnd = outer
                                    )
                                }
                            }

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(rowBg)
                                    .let { mod ->
                                        if (isSelectionMode) {
                                            mod.clickable { toggleSelection(message.id) }
                                        } else {
                                            mod.pointerInput(message.id + "-swipe") {
                                                detectHorizontalDragGestures(
                                                    onHorizontalDrag = { _, amount ->
                                                        dragX = (dragX + amount).coerceIn(0f, 88f)
                                                    },
                                                    onDragEnd = {
                                                        if (dragX >= 56f) {
                                                            runCatching { haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove) }
                                                            replyTarget = message
                                                        }
                                                        dragX = 0f
                                                    },
                                                    onDragCancel = { dragX = 0f }
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
                                            .size(28.dp),
                                        shape = CircleShape,
                                        color = palette.communitySoft
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                Icons.AutoMirrored.Filled.Reply,
                                                "Yanıtla",
                                                tint = palette.community,
                                                modifier = Modifier.size(15.dp)
                                            )
                                        }
                                    }
                                }

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .offset { IntOffset(dragX.roundToInt(), 0) },
                                    horizontalArrangement = if (mine) Arrangement.End else Arrangement.Start,
                                    verticalAlignment = Alignment.Bottom
                                ) {
                                    if (isSelectionMode) {
                                        Surface(
                                            modifier = Modifier
                                                .padding(end = 6.dp, bottom = 6.dp)
                                                .size(20.dp),
                                            shape = CircleShape,
                                            color = if (isSelected) palette.brand else palette.surface,
                                            border = BorderStroke(1.5.dp, if (isSelected) palette.brand else palette.hairline)
                                        ) {
                                            if (isSelected) {
                                                Box(contentAlignment = Alignment.Center) {
                                                    Icon(Icons.Default.Check, null, tint = Color.White, modifier = Modifier.size(13.dp))
                                                }
                                            }
                                        }
                                    }

                                    if (!mine) {
                                        if (!groupedWithNext) {
                                            UserAvatar(
                                                photoUrl = message.senderPhotoUrl,
                                                initials = message.senderName.take(1).uppercase().ifBlank { "U" },
                                                size = 28.dp,
                                                onClick = {
                                                    if (isSelectionMode) toggleSelection(message.id)
                                                    else onOpenProfile(message.senderId)
                                                }
                                            )
                                        } else {
                                            Spacer(Modifier.width(28.dp))
                                        }
                                        Spacer(Modifier.width(7.dp))
                                    }

                                    Column(
                                        modifier = Modifier.widthIn(max = 296.dp),
                                        horizontalAlignment = if (mine) Alignment.End else Alignment.Start
                                    ) {
                                        if (!mine && !groupedWithPrev) {
                                            Row(
                                                modifier = Modifier
                                                    .padding(start = 4.dp, bottom = 2.dp)
                                                    .clickable { onOpenProfile(message.senderId) },
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(5.dp)
                                            ) {
                                                Text(
                                                    text = message.senderName.ifBlank {
                                                        message.senderUsername.takeIf { it.isNotBlank() }?.let { "@$it" } ?: "Üye"
                                                    },
                                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                                    color = deterministicSenderAccent(message.senderId, darkTheme)
                                                )
                                                if (message.senderId == GLOBAL_LIBRA_FOUNDER_UID) {
                                                    LibraRoleChip(label = "Libra Ekibi", highlighted = true, accentColor = palette.brand)
                                                }
                                            }
                                        }

                                        val bubbleBg = if (mine) palette.sentBubble else palette.receivedBubble
                                        val textColor = if (mine) Color.White else palette.inkPrimary
                                        val timeColor = if (mine) Color.White.copy(alpha = 0.78f) else palette.inkSecondary

                                        Surface(
                                            color = bubbleBg,
                                            shape = bubbleShape,
                                            border = when {
                                                isSelected -> BorderStroke(1.8.dp, if (mine) Color.White else palette.brand)
                                                mine -> null
                                                else -> BorderStroke(0.85.dp, palette.hairline)
                                            },
                                            modifier = Modifier.pointerInput(message.id + "-tap", isSelectionMode) {
                                                detectTapGestures(
                                                    onTap = { if (isSelectionMode) toggleSelection(message.id) },
                                                    onDoubleTap = {
                                                        if (isSelectionMode) toggleSelection(message.id)
                                                        else {
                                                            runCatching { haptic.performHapticFeedback(HapticFeedbackType.LongPress) }
                                                            viewModel.react(message.id, "❤️")
                                                        }
                                                    },
                                                    onLongPress = {
                                                        if (isSelectionMode) toggleSelection(message.id)
                                                        else {
                                                            runCatching { haptic.performHapticFeedback(HapticFeedbackType.LongPress) }
                                                            actionMessage = message
                                                        }
                                                    }
                                                )
                                            }
                                        ) {
                                            Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp)) {
                                                if (message.replyToMessageId.isNotBlank()) {
                                                    Surface(
                                                        color = if (mine) palette.sentBubbleQuote else palette.recessed,
                                                        shape = RoundedCornerShape(10.dp),
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .padding(bottom = 5.dp)
                                                            .clip(RoundedCornerShape(10.dp))
                                                            .clickable {
                                                                if (isSelectionMode) {
                                                                    toggleSelection(message.id)
                                                                } else {
                                                                    val idx = findReplyTargetIndex(filteredMessages, message.replyToMessageId) { it.id }
                                                                    if (idx >= 0) {
                                                                        scope.launch {
                                                                            listState.animateScrollToItem(idx)
                                                                            highlightedMessageId = message.replyToMessageId
                                                                            highlightNonce++
                                                                        }
                                                                    } else {
                                                                        Toast.makeText(context, "Alıntılanan mesaj bulunamadı.", Toast.LENGTH_SHORT).show()
                                                                    }
                                                                }
                                                            }
                                                    ) {
                                                        Row(
                                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                                            verticalAlignment = Alignment.CenterVertically
                                                        ) {
                                                            Box(
                                                                modifier = Modifier
                                                                    .width(2.5.dp)
                                                                    .height(24.dp)
                                                                    .clip(CircleShape)
                                                                    .background(if (mine) Color.White else palette.community)
                                                            )
                                                            Spacer(Modifier.width(7.dp))
                                                            Column {
                                                                Text(
                                                                    text = message.replyToSenderName.ifBlank { "Yanıtlanan mesaj" },
                                                                    color = if (mine) Color.White else palette.communityText,
                                                                    fontWeight = FontWeight.Bold,
                                                                    style = MaterialTheme.typography.labelSmall,
                                                                    maxLines = 1,
                                                                    overflow = TextOverflow.Ellipsis
                                                                )
                                                                Text(
                                                                    text = message.replyToText.ifBlank { "📷 Fotoğraf" },
                                                                    maxLines = 1,
                                                                    overflow = TextOverflow.Ellipsis,
                                                                    style = MaterialTheme.typography.bodySmall,
                                                                    color = timeColor
                                                                )
                                                            }
                                                        }
                                                    }
                                                }

                                                message.sharedContent?.let { shared ->
                                                    Surface(
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .padding(bottom = 4.dp),
                                                        shape = RoundedCornerShape(10.dp),
                                                        color = if (mine) palette.sentBubbleQuote else palette.recessed
                                                    ) {
                                                        Column(Modifier.padding(8.dp)) {
                                                            Text(
                                                                text = when (shared.type) {
                                                                    "story" -> "Hikâyeden bahsetti"
                                                                    "post" -> "Bir gönderi paylaştı"
                                                                    "book" -> "Bir kitap paylaştı"
                                                                    else -> "Paylaşılan içerik"
                                                                },
                                                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                                                color = if (mine) Color.White.copy(alpha = 0.85f) else palette.brandText
                                                            )
                                                            Text(shared.title.ifBlank { "Libra içeriği" }, fontWeight = FontWeight.SemiBold, color = textColor)
                                                            if (shared.text.isNotBlank()) {
                                                                Text(shared.text, maxLines = 2, style = MaterialTheme.typography.bodySmall, color = timeColor)
                                                            }
                                                        }
                                                    }
                                                }

                                                if (message.mediaUrl.isNotBlank()) {
                                                    LibraSignedImage(
                                                        mediaKeyOrUrl = message.mediaUrl,
                                                        contentDescription = "Gönderilen fotoğraf",
                                                        modifier = Modifier
                                                            .widthIn(min = 160.dp, max = 236.dp)
                                                            .heightIn(min = 110.dp, max = 260.dp)
                                                            .clip(RoundedCornerShape(12.dp))
                                                            .clickable {
                                                                if (isSelectionMode) toggleSelection(message.id)
                                                                else fullscreenImageKey = message.mediaUrl
                                                            }
                                                    )
                                                }

                                                if (message.text.isNotBlank()) {
                                                    Text(
                                                        text = message.text,
                                                        color = textColor,
                                                        style = MaterialTheme.typography.bodyMedium.copy(
                                                            fontSize = 14.5.sp,
                                                            lineHeight = 20.sp
                                                        ),
                                                        modifier = Modifier.padding(top = if (message.mediaUrl.isNotBlank()) 5.dp else 0.dp)
                                                    )
                                                }

                                                Row(
                                                    modifier = Modifier
                                                        .align(Alignment.End)
                                                        .padding(top = 2.dp),
                                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    if (message.editedAt != null) {
                                                        Text("düzenlendi •", style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp), color = timeColor)
                                                    }
                                                    if (message.createdAt > 0L) {
                                                        Text(formatMessageHour(message.createdAt), style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp), color = timeColor)
                                                    }
                                                }
                                            }
                                        }

                                        if (message.reactions.isNotEmpty()) {
                                            val counts = remember(message.reactions) {
                                                message.reactions.values.groupingBy { it }.eachCount()
                                            }
                                            Surface(
                                                modifier = Modifier
                                                    .offset(y = (-5).dp)
                                                    .padding(horizontal = 6.dp),
                                                shape = CircleShape,
                                                color = palette.surface,
                                                border = BorderStroke(0.85.dp, palette.hairline)
                                            ) {
                                                Row(
                                                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp),
                                                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    counts.forEach { (emoji, count) ->
                                                        Row(
                                                            modifier = Modifier
                                                                .clip(CircleShape)
                                                                .clickable { viewModel.react(message.id, emoji) }
                                                                .padding(horizontal = 2.dp),
                                                            verticalAlignment = Alignment.CenterVertically,
                                                            horizontalArrangement = Arrangement.spacedBy(2.dp)
                                                        ) {
                                                            Text(emoji, fontSize = 12.sp)
                                                            if (count > 1) {
                                                                Text(
                                                                    text = count.toString(),
                                                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.5.sp, fontWeight = FontWeight.Bold),
                                                                    color = palette.inkSecondary
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
                }
            }

            androidx.compose.animation.AnimatedVisibility(
                visible = showScrollToBottom && filteredMessages.isNotEmpty(),
                enter = fadeIn() + scaleIn(initialScale = 0.85f),
                exit = fadeOut() + scaleOut(targetScale = 0.85f),
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 14.dp, bottom = 10.dp)
            ) {
                Surface(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .appleBouncy(
                            pressedScale = 0.86f,
                            onClick = {
                                if (filteredMessages.isNotEmpty()) {
                                    scope.launch { listState.animateScrollToItem(filteredMessages.lastIndex) }
                                }
                            }
                        ),
                    shape = CircleShape,
                    color = palette.surface,
                    border = BorderStroke(1.dp, palette.hairline),
                    shadowElevation = 2.dp
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.KeyboardArrowDown, "En alta git", tint = palette.inkPrimary)
                    }
                }
            }
        }

        // Pending Photo Attachment Strip
        if (pendingUri != null) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 4.dp),
                shape = RoundedCornerShape(14.dp),
                color = palette.surface,
                border = BorderStroke(1.dp, palette.hairline)
            ) {
                Row(
                    modifier = Modifier.padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AsyncImage(
                        model = pendingUri,
                        contentDescription = "Gönderilecek fotoğraf",
                        modifier = Modifier.size(52.dp).clip(RoundedCornerShape(10.dp)),
                        contentScale = ContentScale.Crop
                    )
                    Column(Modifier.weight(1f).padding(horizontal = 10.dp)) {
                        Text(
                            text = if (uploading) "Fotoğraf yükleniyor %$progress" else if (pendingUrl.isNotBlank()) "Fotoğraf hazır ✓" else "Hazırlanıyor…",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = palette.inkPrimary
                        )
                        if (uploading) {
                            LinearProgressIndicator(
                                progress = { (progress / 100f).coerceIn(0f, 1f) },
                                modifier = Modifier.fillMaxWidth().padding(top = 4.dp).height(3.dp).clip(CircleShape),
                                color = palette.community
                            )
                        }
                    }
                    IconButton(onClick = { pendingUri = null; pendingUrl = ""; mediaError = null }) {
                        Icon(Icons.Default.Close, "Kaldır", tint = palette.inkSecondary)
                    }
                }
            }
        }

        replyTarget?.let {
            RichReplyBanner("Yanıtlanıyor: ${it.senderName}", it.text.ifBlank { "📷 Fotoğraf" }, { replyTarget = null })
        }
        editingMessage?.let {
            RichReplyBanner("Mesaj düzenleniyor", it.text, { editingMessage = null; draft = "" })
        }

        // Clean Pill Composer Dock
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .then(
                    if (embeddedInHub) Modifier
                    else Modifier.windowInsetsPadding(WindowInsets.navigationBars.union(WindowInsets.ime))
                )
                .padding(horizontal = 12.dp, vertical = 8.dp)
                .graphicsLayer {
                    val v = entranceProgress.value
                    alpha = v
                    translationY = (1f - v) * 24f
                },
            shape = RoundedCornerShape(24.dp),
            color = palette.surface,
            border = BorderStroke(1.dp, palette.hairline)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 6.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val canPick = canMessage && !uploading && editingMessage == null
                Surface(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .appleBouncy(
                            enabled = canPick,
                            pressedScale = 0.86f,
                            onClick = { picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }
                        ),
                    shape = CircleShape,
                    color = if (canPick) palette.communitySoft else palette.recessed
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.AddPhotoAlternate,
                            contentDescription = "Fotoğraf ekle",
                            tint = if (canPick) palette.community else palette.inkSecondary,
                            modifier = Modifier.size(19.dp)
                        )
                    }
                }

                TextField(
                    value = draft,
                    onValueChange = { if (canMessage && it.length <= 1000) draft = it },
                    modifier = Modifier.weight(1f),
                    enabled = canMessage,
                    textStyle = MaterialTheme.typography.bodyMedium.copy(color = palette.inkPrimary),
                    placeholder = {
                        Text(
                            text = when {
                                !canMessage -> "Mesaj gönderme engellendi"
                                editingMessage != null -> "Mesajı düzenle…"
                                else -> "Genel Sohbete yaz…"
                            },
                            color = palette.inkSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    maxLines = 4,
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        disabledContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        disabledIndicatorColor = Color.Transparent,
                        cursorColor = palette.community
                    )
                )

                val canSend = canMessage && (draft.isNotBlank() || pendingUrl.isNotBlank()) && !uploading
                Surface(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .appleBouncy(
                            enabled = canSend,
                            pressedScale = 0.85f,
                            onClick = {
                                val text = draft.trim()
                                val edit = editingMessage
                                when {
                                    edit != null -> {
                                        if (text.isNotBlank()) {
                                            viewModel.edit(edit.id, text)
                                            editingMessage = null
                                            draft = ""
                                        }
                                    }
                                    pendingUrl.isNotBlank() -> {
                                        viewModel.sendMedia(pendingUrl, pendingType, text, replyTarget)
                                        pendingUri = null
                                        pendingUrl = ""
                                        replyTarget = null
                                        draft = ""
                                    }
                                    text.isNotBlank() -> {
                                        viewModel.send(text, replyTarget)
                                        replyTarget = null
                                        draft = ""
                                    }
                                }
                            }
                        ),
                    shape = CircleShape,
                    color = if (canSend) palette.community else palette.recessed
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = if (editingMessage != null) Icons.Default.Check else Icons.AutoMirrored.Filled.Send,
                            contentDescription = "Gönder",
                            tint = if (canSend) Color.White else palette.inkSecondary,
                            modifier = Modifier.size(18.dp)
                        )
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

    actionMessage?.let { message ->
        val canDelete = message.senderId == currentUid || isAdminModerator
        RichMessageActionSheet(
            canEdit = message.senderId == currentUid && message.mediaUrl.isBlank(),
            canDelete = canDelete,
            previewText = message.text,
            previewMediaUrl = message.mediaUrl,
            isOwnMessage = message.senderId == currentUid,
            selectedReaction = message.reactions[currentUid],
            onDismiss = { actionMessage = null },
            onReply = { replyTarget = message; actionMessage = null },
            onSelectMultiple = {
                actionMessage = null
                isSelectionMode = true
                selectedMessageIds = setOf(message.id)
            },
            onCopy = if (message.text.isNotBlank()) {
                {
                    clipboard.setText(AnnotatedString(message.text))
                    Toast.makeText(context, "Mesaj kopyalandı.", Toast.LENGTH_SHORT).show()
                    actionMessage = null
                }
            } else null,
            onReaction = { emoji -> viewModel.react(message.id, emoji); actionMessage = null },
            onEdit = { editingMessage = message; draft = message.text; actionMessage = null },
            onDelete = { viewModel.delete(message.id); actionMessage = null }
        )
    }

    if (showBatchDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showBatchDeleteConfirm = false },
            containerColor = palette.surface,
            title = { Text("Mesajları Sil (${deletableSelectedIds.size})", fontWeight = FontWeight.Bold) },
            text = {
                val skipped = selectedMessageIds.size - deletableSelectedIds.size
                val note = if (skipped > 0) " Yetkin olmayan $skipped mesaj atlanacak." else ""
                Text("Seçilen ${deletableSelectedIds.size} mesaj kalıcı olarak silinecek.$note")
            },
            confirmButton = {
                Button(
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    onClick = {
                        val ids = deletableSelectedIds
                        showBatchDeleteConfirm = false
                        isSelectionMode = false
                        selectedMessageIds = emptySet()
                        viewModel.deleteBatch(ids) { ok ->
                            if (ok) Toast.makeText(context, "${ids.size} mesaj silindi.", Toast.LENGTH_SHORT).show()
                        }
                    }
                ) { Text("Sil") }
            },
            dismissButton = {
                TextButton(onClick = { showBatchDeleteConfirm = false }) { Text("Vazgeç") }
            }
        )
    }
}
