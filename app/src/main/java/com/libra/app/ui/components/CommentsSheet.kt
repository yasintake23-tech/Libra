package com.libra.app.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Reply
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.libra.app.domain.model.UserProfile
import com.libra.app.ui.animation.appleBouncy
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

data class LibraCommentItem(
    val id: String,
    val authorId: String,
    val authorName: String,
    val authorUsername: String,
    val authorPhotoUrl: String,
    val text: String,
    val parentCommentId: String = "",
    val createdAt: Long = 0L,
    val isContentAuthor: Boolean = false,
    val canDelete: Boolean = false
)

enum class CommentSortOrder(val label: String) {
    NEWEST("En yeni"),
    OLDEST("En eski")
}

data class CommentThread(
    val root: LibraCommentItem,
    val replies: List<LibraCommentItem>,
    val replyTargetLabels: Map<String, String> = emptyMap()
)

val LIBRA_COMMENT_QUICK_EMOJIS = listOf("❤️", "👏", "🔥", "📚", "✨", "😍", "😮", "💡")

fun resolveCommentDisplayHandle(comment: LibraCommentItem): String =
    comment.authorUsername.trim()
        .takeIf { it.isNotBlank() }
        ?: comment.authorName.trim().ifBlank { "kullanici" }

fun appendEmojiToCommentDraft(current: String, emoji: String, maxChars: Int): String =
    if (current.length + emoji.length <= maxChars) current + emoji else current

fun buildCommentThreads(
    comments: List<LibraCommentItem>,
    sortOrder: CommentSortOrder = CommentSortOrder.NEWEST
): List<CommentThread> {
    val clean = comments.filter { it.id.isNotBlank() }.distinctBy { it.id }
    if (clean.isEmpty()) return emptyList()

    val byId = clean.associateBy { it.id }
    val rootIdCache = mutableMapOf<String, String>()

    fun findRootId(item: LibraCommentItem): String {
        rootIdCache[item.id]?.let { return it }
        var current = item
        val visited = mutableSetOf(current.id)
        while (current.parentCommentId.isNotBlank()) {
            val parent = byId[current.parentCommentId] ?: break
            if (!visited.add(parent.id)) break
            current = parent
        }
        rootIdCache[item.id] = current.id
        return current.id
    }

    val roots = mutableListOf<LibraCommentItem>()
    val repliesByRoot = mutableMapOf<String, MutableList<LibraCommentItem>>()

    clean.forEach { comment ->
        val rootId = findRootId(comment)
        if (rootId == comment.id) {
            roots.add(comment)
        } else {
            repliesByRoot.getOrPut(rootId) { mutableListOf() }.add(comment)
        }
    }

    val unsortedThreads = roots.map { root ->
        val threadReplies = repliesByRoot[root.id].orEmpty()
            .sortedWith(compareBy<LibraCommentItem> { it.createdAt }.thenBy { it.id })
        val targetLabels = buildMap {
            threadReplies.forEach { reply ->
                val parent = byId[reply.parentCommentId]
                if (parent != null) {
                    put(reply.id, resolveCommentDisplayHandle(parent))
                }
            }
        }
        CommentThread(
            root = root,
            replies = threadReplies,
            replyTargetLabels = targetLabels
        )
    }

    return when (sortOrder) {
        CommentSortOrder.NEWEST -> unsortedThreads.sortedWith(
            compareByDescending<CommentThread> { it.root.createdAt }.thenByDescending { it.root.id }
        )
        CommentSortOrder.OLDEST -> unsortedThreads.sortedWith(
            compareBy<CommentThread> { it.root.createdAt }.thenBy { it.root.id }
        )
    }
}

fun formatCommentRelativeTime(
    timestampMs: Long,
    nowMs: Long = System.currentTimeMillis()
): String {
    if (timestampMs <= 0L) return ""
    val diffMs = (nowMs - timestampMs).coerceAtLeast(0L)
    val minuteMs = 60_000L
    val hourMs = 60 * minuteMs
    val dayMs = 24 * hourMs
    val weekMs = 7 * dayMs
    val monthMs = 30 * dayMs

    return when {
        diffMs < minuteMs -> "Şimdi"
        diffMs < hourMs -> "${diffMs / minuteMs} dk"
        diffMs < dayMs -> "${diffMs / hourMs} sa"
        diffMs < weekMs -> "${diffMs / dayMs} gün"
        diffMs < monthMs -> "${diffMs / weekMs} hf"
        else -> {
            val trLocale = Locale("tr")
            val nowCal = Calendar.getInstance().apply { timeInMillis = nowMs }
            val targetCal = Calendar.getInstance().apply { timeInMillis = timestampMs }
            val pattern = if (nowCal.get(Calendar.YEAR) == targetCal.get(Calendar.YEAR)) {
                "d MMM"
            } else {
                "d MMM yyyy"
            }
            SimpleDateFormat(pattern, trLocale).format(Date(timestampMs))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraCommentsBottomSheet(
    comments: List<LibraCommentItem>,
    currentUser: UserProfile?,
    text: String,
    onTextChanged: (String) -> Unit,
    onSend: (parentCommentId: String) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    contextTitle: String = "",
    isLoading: Boolean = false,
    isSending: Boolean = false,
    canComment: Boolean = true,
    repliesEnabled: Boolean = true,
    maxChars: Int = 500,
    error: String? = null,
    onClearError: () -> Unit = {},
    onDeleteComment: (LibraCommentItem) -> Unit = {},
    onOpenProfile: (String) -> Unit = {}
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val focusRequester = remember { FocusRequester() }

    var sortOrder by remember { mutableStateOf(CommentSortOrder.NEWEST) }
    var replyTarget by remember { mutableStateOf<LibraCommentItem?>(null) }
    var pendingDelete by remember { mutableStateOf<LibraCommentItem?>(null) }
    var expandedThreadIds by remember { mutableStateOf<Set<String>>(emptySet()) }
    var collapsedThreadIds by remember { mutableStateOf<Set<String>>(emptySet()) }

    val threads = remember(comments, sortOrder) {
        buildCommentThreads(comments, sortOrder)
    }

    val byId = remember(comments) {
        comments.associateBy { it.id }
    }

    fun findRootThreadId(comment: LibraCommentItem): String {
        var current = comment
        val visited = mutableSetOf(current.id)
        while (current.parentCommentId.isNotBlank()) {
            val parent = byId[current.parentCommentId] ?: break
            if (!visited.add(parent.id)) break
            current = parent
        }
        return current.id
    }

    LaunchedEffect(replyTarget?.id) {
        if (replyTarget != null) {
            runCatching { focusRequester.requestFocus() }
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.background,
        dragHandle = null,
        modifier = modifier.fillMaxHeight(0.92f),
        contentWindowInsets = { WindowInsets(0, 0, 0, 0) }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .imePadding()
                .navigationBarsPadding()
        ) {
            // Drag Handle & Instagram-Style Header
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(top = 10.dp, bottom = 10.dp, start = 16.dp, end = 10.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .width(40.dp)
                        .height(4.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.outline)
                )

                Spacer(Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Compact Sort Pill (left balance)
                    Surface(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .appleBouncy(pressedScale = 0.92f, onClick = {
                                sortOrder = if (sortOrder == CommentSortOrder.NEWEST) {
                                    CommentSortOrder.OLDEST
                                } else {
                                    CommentSortOrder.NEWEST
                                }
                            }),
                        shape = RoundedCornerShape(20.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.SwapVert,
                                contentDescription = "Sıralama",
                                modifier = Modifier.size(14.dp),
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = sortOrder.label,
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontSize = 12.sp,
                                    lineHeight = 15.sp,
                                    fontWeight = FontWeight.SemiBold
                                ),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "Yorumlar",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontSize = 17.sp,
                                    lineHeight = 22.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = (-0.2).sp
                                ),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                            ) {
                                Text(
                                    text = comments.size.toString(),
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontSize = 12.sp,
                                        lineHeight = 14.sp,
                                        fontWeight = FontWeight.Bold
                                    ),
                                    color = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                )
                            }
                        }

                        if (contextTitle.isNotBlank()) {
                            Spacer(Modifier.height(2.dp))
                            Text(
                                text = contextTitle,
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontSize = 12.sp,
                                    lineHeight = 16.sp
                                ),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.appleBouncy(pressedScale = 0.88f, onClick = onDismiss)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Kapat",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            HorizontalDivider(thickness = 1.dp, color = MaterialTheme.colorScheme.outline)

            // Comments Body
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                when {
                    isLoading && comments.isEmpty() -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(30.dp),
                                    strokeWidth = 2.5.dp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(Modifier.height(10.dp))
                                Text(
                                    text = "Yorumlar yükleniyor…",
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontSize = 14.sp,
                                        lineHeight = 20.sp
                                    ),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    threads.isEmpty() -> {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Surface(
                                shape = RoundedCornerShape(18.dp),
                                color = MaterialTheme.colorScheme.surface,
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                            ) {
                                Column(
                                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 28.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Surface(
                                        modifier = Modifier.size(54.dp),
                                        shape = CircleShape,
                                        color = MaterialTheme.colorScheme.surfaceVariant,
                                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                imageVector = Icons.Default.ChatBubbleOutline,
                                                contentDescription = null,
                                                modifier = Modifier.size(24.dp),
                                                tint = MaterialTheme.colorScheme.onSurface
                                            )
                                        }
                                    }
                                    Spacer(Modifier.height(12.dp))
                                    Text(
                                        text = "Henüz yorum yok",
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontSize = 16.sp,
                                            lineHeight = 22.sp,
                                            fontWeight = FontWeight.Bold
                                        ),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Spacer(Modifier.height(4.dp))
                                    Text(
                                        text = "İlk yorumu bırakarak sohbeti başlat.",
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontSize = 13.5.sp,
                                            lineHeight = 19.sp
                                        ),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }
                    }

                    else -> {
                        LazyColumn(
                            state = listState,
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 14.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(threads, key = { it.root.id }) { thread ->
                                val isExpanded = thread.root.id in expandedThreadIds ||
                                    (thread.root.id !in collapsedThreadIds && thread.replies.size <= 2)

                                CommentThreadCard(
                                    thread = thread,
                                    isRepliesExpanded = isExpanded,
                                    repliesEnabled = repliesEnabled && canComment,
                                    onToggleReplies = {
                                        if (isExpanded) {
                                            expandedThreadIds = expandedThreadIds - thread.root.id
                                            collapsedThreadIds = collapsedThreadIds + thread.root.id
                                        } else {
                                            expandedThreadIds = expandedThreadIds + thread.root.id
                                            collapsedThreadIds = collapsedThreadIds - thread.root.id
                                        }
                                    },
                                    onReply = { target ->
                                        replyTarget = target
                                    },
                                    onDelete = { target ->
                                        pendingDelete = target
                                    },
                                    onOpenProfile = { uid ->
                                        if (uid.isNotBlank()) {
                                            onDismiss()
                                            onOpenProfile(uid)
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            }

            // Bottom Composer Dock
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 0.dp
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    HorizontalDivider(thickness = 1.dp, color = MaterialTheme.colorScheme.outline)

                    if (!error.isNullOrBlank()) {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 8.dp),
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.error)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = error,
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontSize = 12.5.sp,
                                        lineHeight = 18.sp,
                                        fontWeight = FontWeight.Medium
                                    ),
                                    color = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.weight(1f)
                                )
                                IconButton(
                                    onClick = onClearError,
                                    modifier = Modifier.size(26.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Hatayı kapat",
                                        tint = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }

                    AnimatedVisibility(
                        visible = replyTarget != null,
                        enter = expandVertically() + fadeIn(),
                        exit = shrinkVertically() + fadeOut()
                    ) {
                        replyTarget?.let { target ->
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                color = MaterialTheme.colorScheme.surfaceVariant
                            ) {
                                Column {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 14.dp, vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.Reply,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(Modifier.width(8.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = "@${resolveCommentDisplayHandle(target)} kullanıcısına yanıt veriliyor",
                                                style = MaterialTheme.typography.labelMedium.copy(
                                                    fontSize = 12.5.sp,
                                                    lineHeight = 16.sp,
                                                    fontWeight = FontWeight.Bold
                                                ),
                                                color = MaterialTheme.colorScheme.primary,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                text = target.text,
                                                style = MaterialTheme.typography.bodySmall.copy(
                                                    fontSize = 12.sp,
                                                    lineHeight = 16.sp
                                                ),
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                        IconButton(
                                            onClick = { replyTarget = null },
                                            modifier = Modifier.size(28.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Close,
                                                contentDescription = "Yanıtı iptal et",
                                                modifier = Modifier.size(16.dp),
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                    HorizontalDivider(thickness = 1.dp, color = MaterialTheme.colorScheme.outline)
                                }
                            }
                        }
                    }

                    if (!canComment) {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            shape = RoundedCornerShape(14.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                        ) {
                            Text(
                                text = "Yorum yapma yetkin geçici olarak kısıtlandı.",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontSize = 13.5.sp,
                                    lineHeight = 18.sp,
                                    fontWeight = FontWeight.Medium
                                ),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)
                            )
                        }
                    } else {
                        // Instagram-style Quick Emoji Strip
                        LazyRow(
                            modifier = Modifier.fillMaxWidth(),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(LIBRA_COMMENT_QUICK_EMOJIS, key = { it }) { emoji ->
                                Surface(
                                    modifier = Modifier
                                        .clip(CircleShape)
                                        .appleBouncy(pressedScale = 0.86f, onClick = {
                                            onTextChanged(appendEmojiToCommentDraft(text, emoji, maxChars))
                                        }),
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.surfaceVariant,
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                                ) {
                                    Box(
                                        modifier = Modifier.padding(horizontal = 11.dp, vertical = 5.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = emoji,
                                            fontSize = 16.sp
                                        )
                                    }
                                }
                            }
                        }

                        // Composer Row
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 14.dp, end = 14.dp, bottom = 12.dp, top = 2.dp),
                            verticalAlignment = Alignment.Bottom
                        ) {
                            UserAvatar(
                                photoUrl = currentUser?.profileImageUrl,
                                initials = currentUser?.initials ?: "L",
                                size = 36.dp,
                                modifier = Modifier.padding(bottom = 4.dp)
                            )

                            Spacer(Modifier.width(10.dp))

                            Surface(
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(24.dp),
                                color = MaterialTheme.colorScheme.background,
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(start = 14.dp, end = 6.dp, top = 6.dp, bottom = 6.dp),
                                    verticalAlignment = Alignment.Bottom
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .padding(vertical = 5.dp),
                                        contentAlignment = Alignment.CenterStart
                                    ) {
                                        if (text.isEmpty()) {
                                            val placeholder = replyTarget?.let {
                                                "@${resolveCommentDisplayHandle(it)} yanıtla…"
                                            } ?: "Yorum ekle…"
                                            Text(
                                                text = placeholder,
                                                style = MaterialTheme.typography.bodyMedium.copy(
                                                    fontSize = 14.5.sp,
                                                    lineHeight = 20.sp
                                                ),
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }

                                        BasicTextField(
                                            value = text,
                                            onValueChange = { incoming ->
                                                onTextChanged(
                                                    if (incoming.length > maxChars) incoming.take(maxChars) else incoming
                                                )
                                            },
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .focusRequester(focusRequester),
                                            textStyle = MaterialTheme.typography.bodyMedium.copy(
                                                fontSize = 14.5.sp,
                                                lineHeight = 20.sp,
                                                color = MaterialTheme.colorScheme.onSurface
                                            ),
                                            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                                            keyboardOptions = KeyboardOptions(
                                                capitalization = KeyboardCapitalization.Sentences
                                            ),
                                            maxLines = 4
                                        )
                                    }

                                    if (text.length >= (maxChars * 0.8f).toInt()) {
                                        Text(
                                            text = "${text.length}/$maxChars",
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontSize = 10.5.sp,
                                                fontWeight = FontWeight.SemiBold
                                            ),
                                            color = if (text.length >= maxChars) {
                                                MaterialTheme.colorScheme.error
                                            } else {
                                                MaterialTheme.colorScheme.onSurfaceVariant
                                            },
                                            modifier = Modifier.padding(end = 6.dp, bottom = 7.dp)
                                        )
                                    }

                                    val canSend = text.trim().isNotEmpty() && !isSending
                                    Surface(
                                        modifier = Modifier
                                            .size(34.dp)
                                            .clip(CircleShape)
                                            .appleBouncy(
                                                enabled = canSend,
                                                pressedScale = 0.88f,
                                                onClick = {
                                                    val parentId = replyTarget?.id.orEmpty()
                                                    replyTarget?.let { target ->
                                                        val rootId = findRootThreadId(target)
                                                        expandedThreadIds = expandedThreadIds + rootId
                                                        collapsedThreadIds = collapsedThreadIds - rootId
                                                    }
                                                    replyTarget = null
                                                    onSend(parentId)
                                                    scope.launch {
                                                        if (sortOrder == CommentSortOrder.NEWEST && threads.isNotEmpty()) {
                                                            listState.animateScrollToItem(0)
                                                        }
                                                    }
                                                }
                                            ),
                                        shape = CircleShape,
                                        color = if (canSend) {
                                            MaterialTheme.colorScheme.primary
                                        } else {
                                            MaterialTheme.colorScheme.surfaceVariant
                                        }
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            if (isSending) {
                                                CircularProgressIndicator(
                                                    modifier = Modifier.size(16.dp),
                                                    strokeWidth = 2.dp,
                                                    color = MaterialTheme.colorScheme.onPrimary
                                                )
                                            } else {
                                                Icon(
                                                    imageVector = Icons.AutoMirrored.Filled.Send,
                                                    contentDescription = "Gönder",
                                                    modifier = Modifier.size(16.dp),
                                                    tint = if (canSend) {
                                                        MaterialTheme.colorScheme.onPrimary
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

    pendingDelete?.let { comment ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            containerColor = MaterialTheme.colorScheme.surface,
            title = {
                Text(
                    text = "Yorumu Sil",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            },
            text = {
                Text(
                    text = "Bu yorumu kalıcı olarak silmek istediğinden emin misin?",
                    style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 20.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val target = comment
                        pendingDelete = null
                        if (replyTarget?.id == target.id) {
                            replyTarget = null
                        }
                        onDeleteComment(target)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Sil")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { pendingDelete = null }) {
                    Text("Vazgeç")
                }
            }
        )
    }
}

@Composable
private fun CommentThreadCard(
    thread: CommentThread,
    isRepliesExpanded: Boolean,
    repliesEnabled: Boolean,
    onToggleReplies: () -> Unit,
    onReply: (LibraCommentItem) -> Unit,
    onDelete: (LibraCommentItem) -> Unit,
    onOpenProfile: (String) -> Unit
) {
    val root = thread.root
    val cardShape = RoundedCornerShape(18.dp)

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(cardShape),
        shape = cardShape,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            CommentEntryBlock(
                comment = root,
                isReplyCard = false,
                replyTargetLabel = null,
                replyCountInThread = thread.replies.size,
                isRepliesExpanded = isRepliesExpanded,
                repliesEnabled = repliesEnabled,
                onToggleReplies = onToggleReplies,
                onReply = { onReply(root) },
                onDelete = { onDelete(root) },
                onOpenProfile = { onOpenProfile(root.authorId) }
            )

            AnimatedVisibility(
                visible = thread.replies.isNotEmpty() && isRepliesExpanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    HorizontalDivider(
                        modifier = Modifier.padding(top = 10.dp, bottom = 2.dp),
                        thickness = 1.dp,
                        color = MaterialTheme.colorScheme.outlineVariant
                    )

                    thread.replies.forEach { reply ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(IntrinsicSize.Min)
                        ) {
                            Box(
                                modifier = Modifier
                                    .padding(start = 8.dp, end = 10.dp, top = 4.dp, bottom = 4.dp)
                                    .width(2.5.dp)
                                    .fillMaxHeight()
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.outline)
                            )

                            val replyShape = RoundedCornerShape(14.dp)
                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(replyShape),
                                shape = replyShape,
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                            ) {
                                Box(modifier = Modifier.padding(12.dp)) {
                                    CommentEntryBlock(
                                        comment = reply,
                                        isReplyCard = true,
                                        replyTargetLabel = thread.replyTargetLabels[reply.id],
                                        replyCountInThread = 0,
                                        isRepliesExpanded = false,
                                        repliesEnabled = repliesEnabled,
                                        onToggleReplies = {},
                                        onReply = { onReply(reply) },
                                        onDelete = { onDelete(reply) },
                                        onOpenProfile = { onOpenProfile(reply.authorId) }
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

@Composable
private fun CommentEntryBlock(
    comment: LibraCommentItem,
    isReplyCard: Boolean,
    replyTargetLabel: String?,
    replyCountInThread: Int,
    isRepliesExpanded: Boolean,
    repliesEnabled: Boolean,
    onToggleReplies: () -> Unit,
    onReply: () -> Unit,
    onDelete: () -> Unit,
    onOpenProfile: () -> Unit
) {
    val displayName = comment.authorName.trim()
        .ifBlank { comment.authorUsername.trim().ifBlank { "Libra Kullanıcısı" } }
    val username = comment.authorUsername.trim()
    val relativeTime = remember(comment.createdAt) {
        formatCommentRelativeTime(comment.createdAt)
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        UserAvatar(
            photoUrl = comment.authorPhotoUrl,
            initials = displayName.take(1).uppercase().ifBlank { "L" },
            size = if (isReplyCard) 30.dp else 38.dp,
            onClick = onOpenProfile
        )

        Spacer(Modifier.width(10.dp))

        Column(modifier = Modifier.weight(1f)) {
            // Header Row: Author Name + Yazar badge + Relative Time
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = displayName,
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontSize = if (isReplyCard) 13.sp else 14.sp,
                        lineHeight = if (isReplyCard) 17.sp else 18.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .clickable(onClick = onOpenProfile)
                )

                if (comment.isContentAuthor) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Text(
                            text = "Yazar",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = if (isReplyCard) 10.sp else 10.5.sp,
                                lineHeight = 13.sp,
                                fontWeight = FontWeight.Bold
                            ),
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                if (relativeTime.isNotBlank()) {
                    Text(
                        text = "• $relativeTime",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = if (isReplyCard) 11.5.sp else 12.sp,
                            lineHeight = 15.sp,
                            fontWeight = FontWeight.Medium
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1
                    )
                }
            }

            if (username.isNotBlank()) {
                Spacer(Modifier.height(1.dp))
                Text(
                    text = "@$username",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = if (isReplyCard) 11.5.sp else 12.sp,
                        lineHeight = 15.sp,
                        fontWeight = FontWeight.Medium
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.clickable(onClick = onOpenProfile)
                )
            }

            Spacer(Modifier.height(6.dp))

            // Comment Body (high readability & generous lineHeight)
            val bodyStyle = MaterialTheme.typography.bodyLarge.copy(
                fontSize = if (isReplyCard) 14.sp else 14.5.sp,
                lineHeight = if (isReplyCard) 20.sp else 21.sp,
                letterSpacing = 0.1.sp,
                fontWeight = FontWeight.Normal
            )
            if (isReplyCard && !replyTargetLabel.isNullOrBlank()) {
                val primaryColor = MaterialTheme.colorScheme.primary
                val annotatedBody = remember(comment.text, replyTargetLabel, primaryColor) {
                    buildAnnotatedString {
                        withStyle(
                            SpanStyle(
                                color = primaryColor,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 13.5.sp
                            )
                        ) {
                            append("@$replyTargetLabel ")
                        }
                        append(comment.text)
                    }
                }
                Text(
                    text = annotatedBody,
                    style = bodyStyle,
                    color = MaterialTheme.colorScheme.onSurface
                )
            } else {
                Text(
                    text = comment.text,
                    style = bodyStyle,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            if (repliesEnabled || replyCountInThread > 0 || comment.canDelete) {
                Spacer(Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (repliesEnabled) {
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .appleBouncy(pressedScale = 0.92f, onClick = onReply)
                                .padding(horizontal = 4.dp, vertical = 3.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Reply,
                                contentDescription = "Yanıtla",
                                modifier = Modifier.size(14.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "Yanıtla",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontSize = 12.5.sp,
                                    lineHeight = 16.sp,
                                    fontWeight = FontWeight.SemiBold
                                ),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    if (!isReplyCard && replyCountInThread > 0) {
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .appleBouncy(pressedScale = 0.92f, onClick = onToggleReplies)
                                .padding(horizontal = 4.dp, vertical = 3.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            Text(
                                text = if (isRepliesExpanded) {
                                    "Yanıtları gizle ($replyCountInThread)"
                                } else {
                                    "$replyCountInThread yanıtı gör"
                                },
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontSize = 12.5.sp,
                                    lineHeight = 16.sp,
                                    fontWeight = FontWeight.SemiBold
                                ),
                                color = MaterialTheme.colorScheme.primary
                            )
                            Icon(
                                imageVector = if (isRepliesExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    Spacer(Modifier.weight(1f))

                    if (comment.canDelete) {
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .appleBouncy(pressedScale = 0.92f, onClick = onDelete)
                                .padding(horizontal = 4.dp, vertical = 3.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.DeleteOutline,
                                contentDescription = "Yorumu Sil",
                                modifier = Modifier.size(14.dp),
                                tint = MaterialTheme.colorScheme.error
                            )
                            Text(
                                text = "Sil",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontSize = 12.sp,
                                    lineHeight = 15.sp,
                                    fontWeight = FontWeight.SemiBold
                                ),
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun BookCommentsPreviewCard(
    comments: List<LibraCommentItem>,
    totalCount: Int,
    canComment: Boolean,
    onOpenCommentsSheet: () -> Unit,
    onOpenProfile: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val previewRoots = remember(comments) {
        buildCommentThreads(comments, CommentSortOrder.NEWEST).take(2)
    }
    val cardShape = RoundedCornerShape(18.dp)

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(cardShape)
            .appleBouncy(pressedScale = 0.98f, onClick = onOpenCommentsSheet),
        shape = cardShape,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Kitap Yorumları",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontSize = 16.sp,
                            lineHeight = 22.sp,
                            fontWeight = FontWeight.Bold
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                    ) {
                        Text(
                            text = totalCount.toString(),
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontSize = 12.sp,
                                lineHeight = 14.sp,
                                fontWeight = FontWeight.Bold
                            ),
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = if (previewRoots.isEmpty() && canComment) "Yorum Yaz" else "Tümünü Gör",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.SemiBold
                        ),
                        color = MaterialTheme.colorScheme.primary
                    )
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(Modifier.height(10.dp))

            if (previewRoots.isEmpty()) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                ) {
                    Text(
                        text = if (canComment) {
                            "Bu kitap hakkında ilk düşünceni paylaş."
                        } else {
                            "Bu kitap için henüz yorum yapılmamış."
                        },
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontSize = 13.5.sp,
                            lineHeight = 19.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)
                    )
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    previewRoots.forEach { thread ->
                        val item = thread.root
                        val authorDisplay = item.authorName.trim()
                            .ifBlank { item.authorUsername.trim().ifBlank { "Libra Kullanıcısı" } }
                        val timeLabel = formatCommentRelativeTime(item.createdAt)
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                UserAvatar(
                                    photoUrl = item.authorPhotoUrl,
                                    initials = authorDisplay.take(1).uppercase().ifBlank { "L" },
                                    size = 32.dp,
                                    onClick = { onOpenProfile(item.authorId) }
                                )
                                Spacer(Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Text(
                                            text = authorDisplay,
                                            style = MaterialTheme.typography.labelLarge.copy(
                                                fontSize = 13.5.sp,
                                                lineHeight = 17.sp,
                                                fontWeight = FontWeight.Bold
                                            ),
                                            color = MaterialTheme.colorScheme.onSurface,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.weight(1f, fill = false)
                                        )
                                        if (item.isContentAuthor) {
                                            Surface(
                                                shape = RoundedCornerShape(6.dp),
                                                color = MaterialTheme.colorScheme.primaryContainer
                                            ) {
                                                Text(
                                                    text = "Yazar",
                                                    style = MaterialTheme.typography.labelSmall.copy(
                                                        fontSize = 10.sp,
                                                        fontWeight = FontWeight.Bold
                                                    ),
                                                    color = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                                )
                                            }
                                        }
                                        if (timeLabel.isNotBlank()) {
                                            Text(
                                                text = "• $timeLabel",
                                                style = MaterialTheme.typography.labelSmall.copy(
                                                    fontSize = 11.5.sp
                                                ),
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                maxLines = 1
                                            )
                                        }
                                    }
                                    Spacer(Modifier.height(4.dp))
                                    Text(
                                        text = item.text,
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontSize = 13.5.sp,
                                            lineHeight = 19.sp
                                        ),
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 3,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    if (thread.replies.isNotEmpty()) {
                                        Spacer(Modifier.height(4.dp))
                                        Text(
                                            text = "${thread.replies.size} yanıt",
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontSize = 11.5.sp,
                                                fontWeight = FontWeight.SemiBold
                                            ),
                                            color = MaterialTheme.colorScheme.primary
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
