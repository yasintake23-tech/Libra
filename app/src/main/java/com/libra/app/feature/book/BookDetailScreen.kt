package com.libra.app.feature.book

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.libra.app.core.di.ServiceLocator
import com.libra.app.domain.model.Book
import com.libra.app.domain.model.BookComment
import com.libra.app.domain.model.Chapter
import com.libra.app.domain.model.ReadingProgress
import com.libra.app.domain.model.UserProfile
import com.libra.app.ui.components.BookCommentsPreviewCard
import com.libra.app.ui.components.BookCover
import com.libra.app.ui.components.LibraCommentItem
import com.libra.app.ui.components.LibraCommentsBottomSheet
import com.libra.app.ui.components.UserAvatar
import com.libra.app.ui.animation.appleBouncy

@Composable
fun BookDetailScreen(
    book: Book,
    userId: String,
    onBack: () -> Unit,
    onRead: (Book, Chapter?) -> Unit,
    onOpenChapter: (Book, Chapter) -> Unit,
    onOpenAuthor: ((String) -> Unit)? = null,
    onShare: ((Book) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val vm: BookDetailViewModel = viewModel(key = "book-detail-${book.id}")
    val state by vm.state.collectAsState()
    val currentUser by ServiceLocator.authRepository.currentUser.collectAsState()

    LaunchedEffect(book.id, userId) {
        vm.load(book, userId)
    }

    BackHandler { onBack() }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        when {
            state.loading && state.chapters.isEmpty() -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            state.error != null && state.chapters.isEmpty() -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(state.error.orEmpty(), color = MaterialTheme.colorScheme.error)
                    Spacer(Modifier.height(12.dp))
                    OutlinedButton(onClick = { vm.clear(); vm.load(book, userId) }) {
                        Text("Tekrar dene")
                    }
                }
            }
            else -> {
                BookDetailContent(
                    book = state.book ?: book,
                    chapters = state.chapters,
                    progress = state.progress,
                    author = state.author,
                    currentUser = currentUser,
                    userId = userId,
                    isLiked = state.isLiked,
                    isSaved = state.isSaved,
                    likesCount = state.likesCount,
                    comments = state.comments,
                    commentsCount = state.commentsCount,
                    isLoadingComments = state.isLoadingComments,
                    isSendingComment = state.isSendingComment,
                    commentError = state.commentError,
                    onToggleLike = { vm.toggleLike(userId) },
                    onToggleSave = { vm.toggleSave(userId) },
                    onAddComment = vm::addComment,
                    onDeleteComment = vm::deleteComment,
                    onClearCommentError = vm::clearCommentError,
                    onBack = onBack,
                    onRead = onRead,
                    onOpenChapter = onOpenChapter,
                    onOpenAuthor = onOpenAuthor,
                    onShare = onShare
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun BookDetailContent(
    book: Book,
    chapters: List<Chapter>,
    progress: ReadingProgress?,
    author: UserProfile?,
    currentUser: UserProfile?,
    userId: String,
    isLiked: Boolean,
    isSaved: Boolean,
    likesCount: Int,
    comments: List<BookComment>,
    commentsCount: Int,
    isLoadingComments: Boolean,
    isSendingComment: Boolean,
    commentError: String?,
    onToggleLike: () -> Unit,
    onToggleSave: () -> Unit,
    onAddComment: (String, String) -> Unit,
    onDeleteComment: (String) -> Unit,
    onClearCommentError: () -> Unit,
    onBack: () -> Unit,
    onRead: (Book, Chapter?) -> Unit,
    onOpenChapter: (Book, Chapter) -> Unit,
    onOpenAuthor: ((String) -> Unit)?,
    onShare: ((Book) -> Unit)?
) {
    var isDescriptionExpanded by remember { mutableStateOf(false) }
    var showCommentsSheet by remember(book.id) { mutableStateOf(false) }
    var commentDraft by remember(book.id) { mutableStateOf("") }

    val mappedComments = remember(comments, book.ownerId, userId) {
        comments.map { comment ->
            LibraCommentItem(
                id = comment.id,
                authorId = comment.userId,
                authorName = comment.authorName,
                authorUsername = comment.authorUsername,
                authorPhotoUrl = comment.authorPhotoUrl,
                text = comment.text,
                parentCommentId = comment.parentCommentId,
                createdAt = comment.createdAt,
                isContentAuthor = comment.userId == book.ownerId,
                canDelete = userId.isNotBlank() && (comment.userId == userId || book.ownerId == userId)
            )
        }
    }

    val progressPercent = progress?.progressPercent?.coerceIn(0, 100) ?: 0
    val continueChapter = progress?.chapterId?.let { id -> chapters.firstOrNull { it.id == id } }
        ?: progress?.chapterNumber?.let { n -> chapters.firstOrNull { it.chapterNumber == n } }
    val primaryChapter = continueChapter ?: chapters.firstOrNull()

    val totalWords = chapters.sumOf { it.wordCount }
    val estimatedReadingMinutes = (totalWords / 200).coerceAtLeast(1)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.appleBouncy(pressedScale = 0.86f, onClick = onBack)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Geri")
                    }
                },
                actions = {
                    IconButton(
                        onClick = { showCommentsSheet = true },
                        modifier = Modifier.appleBouncy(pressedScale = 0.86f, onClick = { showCommentsSheet = true })
                    ) {
                        Icon(
                            Icons.Default.ChatBubbleOutline,
                            contentDescription = "Kitap Yorumları",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    IconButton(
                        onClick = onToggleLike,
                        modifier = Modifier.appleBouncy(pressedScale = 0.86f, onClick = onToggleLike)
                    ) {
                        Icon(
                            if (isLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = "Beğen",
                            tint = if (isLiked) Color(0xFFE53935) else MaterialTheme.colorScheme.onSurface
                        )
                    }
                    IconButton(
                        onClick = onToggleSave,
                        modifier = Modifier.appleBouncy(pressedScale = 0.86f, onClick = onToggleSave)
                    ) {
                        Icon(
                            if (isSaved) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                            contentDescription = "Kaydet",
                            tint = if (isSaved) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                        )
                    }
                    onShare?.let { shareFn ->
                        IconButton(
                            onClick = { shareFn(book) },
                            modifier = Modifier.appleBouncy(pressedScale = 0.86f, onClick = { shareFn(book) })
                        ) {
                            Icon(Icons.Default.Share, contentDescription = "Paylaş")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        },
        bottomBar = {
            // Sticky persistent Read/Resume bar
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding(),
                tonalElevation = 6.dp,
                color = MaterialTheme.colorScheme.surface
            ) {
                Column(modifier = Modifier.padding(horizontal = 18.dp, vertical = 12.dp)) {
                    if (progress != null && progressPercent > 0 && chapters.isNotEmpty()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Kaldığın yer: Bölüm ${continueChapter?.chapterNumber ?: progress.chapterNumber}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "%$progressPercent",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Spacer(Modifier.height(6.dp))
                        LinearProgressIndicator(
                            progress = { progressPercent / 100f },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(4.dp)
                                .clip(RoundedCornerShape(2.dp)),
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(Modifier.height(10.dp))
                    }

                    Button(
                        onClick = { primaryChapter?.let { onRead(book, it) } },
                        enabled = primaryChapter != null,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .appleBouncy(
                                enabled = primaryChapter != null,
                                pressedScale = 0.94f,
                                onClick = { primaryChapter?.let { onRead(book, it) } }
                            ),
                        shape = RoundedCornerShape(18.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        )
                    ) {
                        Icon(Icons.Default.MenuBook, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = when {
                                chapters.isEmpty() -> "Henüz yayınlanmış bölüm yok"
                                progress != null && progressPercent > 0 -> "Okumaya Devam Et"
                                else -> "Kitabı Oku (1. Bölüm)"
                            },
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
            // Hero Backdrop & Cover Card
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                                    MaterialTheme.colorScheme.background
                                )
                            )
                        )
                        .padding(horizontal = 20.dp, vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        BookCover(
                            book = book,
                            modifier = Modifier
                                .width(180.dp)
                                .aspectRatio(0.68f),
                            cornerRadius = 16
                        )

                        Spacer(Modifier.height(16.dp))

                        // Category & Series badge
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.primaryContainer
                            ) {
                                Text(
                                    text = book.category.displayName,
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }

                            if (book.isSeries && book.seriesName.isNotBlank()) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant
                                ) {
                                    Text(
                                        text = "${book.seriesName} #${book.seriesOrder}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }

                        Spacer(Modifier.height(10.dp))

                        Text(
                            text = book.title,
                            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                            textAlign = TextAlign.Center
                        )

                        Spacer(Modifier.height(10.dp))

                        // Author Profile Row
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(24.dp))
                                .appleBouncy(
                                    enabled = onOpenAuthor != null,
                                    pressedScale = 0.94f,
                                    onClick = { onOpenAuthor?.invoke(book.ownerId) }
                                )
                                .padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val resolvedAuthorName = author?.displayName?.ifBlank { book.authorName } ?: book.authorName
                            UserAvatar(
                                photoUrl = author?.profileImageUrl,
                                initials = resolvedAuthorName.take(1).uppercase().ifBlank { "L" },
                                size = 36.dp
                            )

                            Spacer(Modifier.width(10.dp))

                            Column {
                                Text(
                                    text = resolvedAuthorName,
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                                )
                                author?.username?.takeIf { it.isNotBlank() }?.let {
                                    Text("@$it", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }
                }
            }

            // Stats metrics bar
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    DetailStatChip(
                        label = "Bölüm",
                        value = "${chapters.size}",
                        icon = Icons.Default.MenuBook,
                        modifier = Modifier.weight(1f)
                    )
                    DetailStatChip(
                        label = "Okunma",
                        value = "${book.readCount}",
                        icon = Icons.Default.Visibility,
                        modifier = Modifier.weight(1f)
                    )
                    DetailStatChip(
                        label = "Beğeni",
                        value = "$likesCount",
                        icon = Icons.Default.Favorite,
                        tint = Color(0xFFE53935),
                        modifier = Modifier.weight(1f)
                    )
                    DetailStatChip(
                        label = "Süre",
                        value = "~$estimatedReadingMinutes dk",
                        icon = Icons.Default.CheckCircle,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Discovery Hook / Summary
            if (book.discoverySummary.isNotBlank()) {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 6.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        ),
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.6f)),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                    ) {
                        Text(
                            text = "“${book.discoverySummary}”",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.Medium,
                                fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                            ),
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }
            }

            // Description / Synopsis
            if (book.description.isNotBlank()) {
                item {
                    Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)) {
                        Text(
                            text = "Özet",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = book.description,
                            style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 24.sp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = if (isDescriptionExpanded) Int.MAX_VALUE else 4,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (book.description.length > 220) {
                            Text(
                                text = if (isDescriptionExpanded) "Daha az göster" else "Devamını oku...",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier
                                    .clickable { isDescriptionExpanded = !isDescriptionExpanded }
                                    .padding(top = 4.dp)
                            )
                        }
                    }
                }
            }

            // Tags
            if (book.tags.isNotEmpty()) {
                item {
                    FlowRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        book.tags.forEach { tag ->
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant
                            ) {
                                Text(
                                    text = "#$tag",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Content Warnings
            if (book.contentWarnings.isNotEmpty()) {
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Warning,
                            contentDescription = null,
                            tint = Color(0xFFF57C00),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = "İçerik Uyarısı: ${book.contentWarnings.joinToString(", ")}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Instagram-inspired Book Comments Preview Section
            item {
                BookCommentsPreviewCard(
                    comments = mappedComments,
                    totalCount = commentsCount,
                    canComment = currentUser?.moderation?.canComment != false,
                    onOpenCommentsSheet = { showCommentsSheet = true },
                    onOpenProfile = { targetUid ->
                        if (targetUid.isNotBlank()) {
                            onOpenAuthor?.invoke(targetUid)
                        }
                    },
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp)
                )
            }

            // Chapters List Header
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Bölümler (${chapters.size})",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                    )
                    Text(
                        text = "$totalWords kelime",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Chapters List
            if (chapters.isEmpty()) {
                item {
                    Text(
                        text = "Bu kitapta henüz yayınlanmış bölüm bulunmuyor.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)
                    )
                }
            } else {
                items(chapters, key = { it.id }) { chapter ->
                    val isCurrentReading = continueChapter?.id == chapter.id
                    val chapReadingMin = (chapter.wordCount / 200).coerceAtLeast(1)

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 4.dp)
                            .appleBouncy(
                                pressedScale = 0.97f,
                                onClick = { onOpenChapter(book, chapter) }
                            ),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        ),
                        border = if (isCurrentReading) {
                            androidx.compose.foundation.BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary)
                        } else {
                            androidx.compose.foundation.BorderStroke(0.6.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.45f))
                        },
                        elevation = CardDefaults.cardElevation(defaultElevation = if (isCurrentReading) 2.dp else 0.5.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = if (isCurrentReading) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier.size(36.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = "${chapter.chapterNumber}",
                                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                        color = if (isCurrentReading) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            Spacer(Modifier.width(14.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = chapter.title.ifBlank { "${chapter.chapterNumber}. Bölüm" },
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = if (isCurrentReading) FontWeight.Bold else FontWeight.SemiBold)
                                )
                                Spacer(Modifier.height(2.dp))
                                Text(
                                    text = "${chapter.wordCount} kelime • ~$chapReadingMin dk",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            if (isCurrentReading) {
                                Icon(
                                    Icons.Default.Bookmark,
                                    contentDescription = "Kaldığın Bölüm",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showCommentsSheet) {
        LibraCommentsBottomSheet(
            comments = mappedComments,
            currentUser = currentUser,
            text = commentDraft,
            onTextChanged = { commentDraft = it },
            onSend = { parentId ->
                val trimmed = commentDraft.trim()
                if (trimmed.isNotEmpty()) {
                    commentDraft = ""
                    onAddComment(trimmed, parentId)
                }
            },
            onDismiss = { showCommentsSheet = false },
            contextTitle = book.title,
            isLoading = isLoadingComments,
            isSending = isSendingComment,
            canComment = currentUser?.moderation?.canComment != false,
            repliesEnabled = true,
            maxChars = 1000,
            error = commentError,
            onClearError = onClearCommentError,
            onDeleteComment = { item -> onDeleteComment(item.id) },
            onOpenProfile = { targetUid ->
                if (targetUid.isNotBlank()) {
                    showCommentsSheet = false
                    onOpenAuthor?.invoke(targetUid)
                }
            }
        )
    }
}

@Composable
private fun DetailStatChip(
    label: String,
    value: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    tint: Color = MaterialTheme.colorScheme.primary
) {
    Surface(
        modifier = modifier.appleBouncy(pressedScale = 0.95f, onClick = {}),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp
    ) {
        Column(
            modifier = Modifier.padding(vertical = 10.dp, horizontal = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(16.dp))
            Spacer(Modifier.height(4.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                maxLines = 1
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
