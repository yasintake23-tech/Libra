package com.libra.app.feature.discover

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.FormatQuote
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.libra.app.core.state.UiState
import com.libra.app.domain.model.Book
import com.libra.app.domain.model.UserProfile
import com.libra.app.feature.friends.FriendsScreen
import com.libra.app.feature.friends.FriendsState
import com.libra.app.ui.components.UserAvatar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookReelsDiscoverScreen(
    friendsUiState: UiState<FriendsState>,
    onSearchFriends: (String) -> Unit,
    onFollowUser: (UserProfile) -> Unit,
    onUserClick: (UserProfile) -> Unit,
    onRetryFriends: () -> Unit,
    onStartReading: (Book) -> Unit,
    onBookClick: (Book) -> Unit,
    onOpenProfile: (String) -> Unit,
    onShareBook: (Book) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: BookReelsDiscoverViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
) {
    val state by viewModel.state.collectAsState()
    var selectedTopTab by remember { mutableStateOf(0) } // 0: Reels, 1: Friends

    Box(modifier = modifier.fillMaxSize().background(Color.Black)) {
        if (selectedTopTab == 0) {
            BookReelsFeed(
                state = state,
                onLike = viewModel::toggleLike,
                onSave = viewModel::toggleSave,
                onOpenComments = viewModel::openComments,
                onStartReading = { book ->
                    viewModel.startReading(book)
                    onStartReading(book)
                },
                onBookClick = onBookClick,
                onOpenProfile = onOpenProfile,
                onShareBook = onShareBook,
                onRetry = viewModel::loadDiscoverBooks
            )
        } else {
            FriendsScreen(
                uiState = friendsUiState,
                onSearchChanged = onSearchFriends,
                onFollowUser = onFollowUser,
                onUserClick = onUserClick,
                onRetry = onRetryFriends,
                modifier = Modifier.padding(top = 64.dp)
            )
        }

        // Top Floating Segmented Switch
        DiscoverTopSegmentedBar(
            selectedTab = selectedTopTab,
            onTabSelected = { selectedTopTab = it },
            modifier = Modifier
                .align(Alignment.TopCenter)
                .statusBarsPadding()
                .padding(top = 8.dp)
        )

        // Comments Bottom Sheet
        if (state.activeCommentsBookId != null) {
            val bookId = state.activeCommentsBookId!!
            var commentInput by remember { mutableStateOf("") }

            ModalBottomSheet(
                onDismissRequest = { viewModel.closeComments() },
                sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
                containerColor = MaterialTheme.colorScheme.surface
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .navigationBarsPadding()
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Okur Yorumları (${state.comments.size})",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        IconButton(onClick = { viewModel.closeComments() }) {
                            Icon(Icons.Default.Close, contentDescription = "Kapat")
                        }
                    }

                    Spacer(Modifier.height(8.dp))

                    if (state.isLoadingComments) {
                        Box(Modifier.fillMaxWidth().height(160.dp), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(modifier = Modifier.size(28.dp))
                        }
                    } else if (state.comments.isEmpty()) {
                        Box(Modifier.fillMaxWidth().height(140.dp), contentAlignment = Alignment.Center) {
                            Text(
                                text = "Bu kitap için henüz yorum yapılmamış.\nİlk yorumu sen yaz!",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(260.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(state.comments, key = { it.id }) { comment ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.Top
                                ) {
                                    UserAvatar(
                                        photoUrl = comment.authorPhotoUrl,
                                        initials = comment.authorName.take(1).uppercase().ifBlank { "L" },
                                        size = 32.dp
                                    )
                                    Spacer(Modifier.width(10.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = comment.authorName.ifBlank { "Libra Okuru" },
                                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                                        )
                                        Spacer(Modifier.height(2.dp))
                                        Text(
                                            text = comment.text,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = commentInput,
                            onValueChange = { commentInput = it },
                            placeholder = { Text("Yorum ekle...", style = MaterialTheme.typography.bodySmall) },
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(20.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                            )
                        )
                        Spacer(Modifier.width(8.dp))
                        IconButton(
                            onClick = {
                                if (commentInput.trim().isNotBlank()) {
                                    val text = commentInput
                                    commentInput = ""
                                    viewModel.addComment(bookId, text)
                                }
                            },
                            enabled = commentInput.trim().isNotBlank()
                        ) {
                            Icon(
                                Icons.Default.Send,
                                contentDescription = "Gönder",
                                tint = if (commentInput.trim().isNotBlank()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                            )
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                }
            }
        }
    }
}

@Composable
private fun DiscoverTopSegmentedBar(
    selectedTab: Int,
    onTabSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(24.dp),
        color = Color.Black.copy(alpha = 0.65f),
        shadowElevation = 4.dp
    ) {
        Row(
            modifier = Modifier.padding(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Reels tab
            Surface(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .clickable { onTabSelected(0) },
                color = if (selectedTab == 0) MaterialTheme.colorScheme.primary else Color.Transparent,
                shape = RoundedCornerShape(20.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.MenuBook,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = if (selectedTab == 0) MaterialTheme.colorScheme.onPrimary else Color.White.copy(alpha = 0.8f)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = "Kitaplar",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = if (selectedTab == 0) MaterialTheme.colorScheme.onPrimary else Color.White.copy(alpha = 0.8f)
                    )
                }
            }

            Spacer(Modifier.width(4.dp))

            // Friends tab
            Surface(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .clickable { onTabSelected(1) },
                color = if (selectedTab == 1) MaterialTheme.colorScheme.primary else Color.Transparent,
                shape = RoundedCornerShape(20.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.People,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = if (selectedTab == 1) MaterialTheme.colorScheme.onPrimary else Color.White.copy(alpha = 0.8f)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = "Okurlar & Yazarlar",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = if (selectedTab == 1) MaterialTheme.colorScheme.onPrimary else Color.White.copy(alpha = 0.8f)
                    )
                }
            }
        }
    }
}

@Composable
private fun BookReelsFeed(
    state: BookReelsState,
    onLike: (Book) -> Unit,
    onSave: (Book) -> Unit,
    onOpenComments: (String) -> Unit,
    onStartReading: (Book) -> Unit,
    onBookClick: (Book) -> Unit,
    onOpenProfile: (String) -> Unit,
    onShareBook: (Book) -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (state.isLoading && state.books.isEmpty()) {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = Color.White)
        }
        return
    }

    if (state.books.isEmpty()) {
        Box(
            modifier = modifier.fillMaxSize().padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "Keşfedilecek kitap bulunamadı",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = Color.White
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "Yeni yayınlanan kitapları görmek için yenileyin.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.7f)
                )
                Spacer(Modifier.height(16.dp))
                Button(onClick = onRetry) {
                    Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Yenile")
                }
            }
        }
        return
    }

    val pagerState = rememberPagerState(pageCount = { state.books.size })

    VerticalPager(
        state = pagerState,
        modifier = modifier.fillMaxSize()
    ) { page ->
        val book = state.books[page]
        val engagement = state.engagements[book.id] ?: BookEngagementUi(likesCount = book.likesCount)
        val previewSnippet = state.chapterPreviews[book.id]

        BookReelItem(
            book = book,
            engagement = engagement,
            previewSnippet = previewSnippet,
            onLike = { onLike(book) },
            onSave = { onSave(book) },
            onOpenComments = { onOpenComments(book.id) },
            onStartReading = { onStartReading(book) },
            onBookClick = { onBookClick(book) },
            onOpenProfile = { onOpenProfile(book.ownerId) },
            onShareBook = { onShareBook(book) }
        )
    }
}

@Composable
private fun BookReelItem(
    book: Book,
    engagement: BookEngagementUi,
    previewSnippet: String?,
    onLike: () -> Unit,
    onSave: () -> Unit,
    onOpenComments: () -> Unit,
    onStartReading: () -> Unit,
    onBookClick: () -> Unit,
    onOpenProfile: () -> Unit,
    onShareBook: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.fillMaxSize()) {
        // Background Cover with Dark Gradient
        if (book.coverImageUrl.isNotBlank()) {
            AsyncImage(
                model = book.coverImageUrl,
                contentDescription = book.title,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                Color(0xFF2C2A4A),
                                Color(0xFF1E1E2E),
                                Color(0xFF0F0F14)
                            )
                        )
                    )
            )
        }

        // Ambient Dark Overlay
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.45f),
                            Color.Black.copy(alpha = 0.25f),
                            Color.Black.copy(alpha = 0.88f),
                            Color.Black.copy(alpha = 0.98f)
                        )
                    )
                )
        )

        // Bottom Left Content Details
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomStart)
                .navigationBarsPadding()
                .padding(start = 18.dp, end = 72.dp, bottom = 20.dp)
        ) {
            // Category & Tags Row
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.85f)
                ) {
                    Text(
                        text = book.category.displayName.uppercase(),
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 10.sp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }

                book.tags.take(2).forEach { tag ->
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = Color.White.copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = if (tag.startsWith("#")) tag else "#$tag",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                            color = Color.White.copy(alpha = 0.9f),
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                        )
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            // Book Title
            Text(
                text = book.title.ifBlank { "İsimsiz Kitap" },
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                color = Color.White,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.clickable(onClick = onBookClick)
            )

            Spacer(Modifier.height(4.dp))

            // Author Row
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable(onClick = onOpenProfile)
                    .padding(vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                UserAvatar(
                    photoUrl = null,
                    initials = book.authorName.take(1).uppercase().ifBlank { "L" },
                    size = 24.dp
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = book.authorName.ifBlank { "Yazar" },
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = Color.White.copy(alpha = 0.95f)
                )
            }

            // Synopsis Preview
            val synopsis = book.description.ifBlank { book.discoverySummary }
            if (synopsis.isNotBlank()) {
                Spacer(Modifier.height(6.dp))
                Text(
                    text = synopsis,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.8f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Chapter 1 Excerpt Teaser Box
            if (!previewSnippet.isNullOrBlank()) {
                Spacer(Modifier.height(10.dp))
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color.White.copy(alpha = 0.12f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.FormatQuote,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primaryContainer,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                text = "1. Bölümden Kesit",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.primaryContainer
                            )
                        }
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = "“$previewSnippet…”",
                            style = MaterialTheme.typography.bodySmall.copy(fontStyle = FontStyle.Italic),
                            color = Color.White.copy(alpha = 0.9f),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            Spacer(Modifier.height(14.dp))

            // Prominent "Okumaya Başla / Devam Et" Action Button
            Button(
                onClick = onStartReading,
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ),
                contentPadding = PaddingValues(vertical = 12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.AutoMirrored.Filled.MenuBook, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "Okumaya Başla",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            }
        }

        // Right Vertical Reels Action Column
        Column(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .navigationBarsPadding()
                .padding(end = 12.dp, bottom = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Author Profile Avatar Button
            Surface(
                shape = CircleShape,
                color = Color.White.copy(alpha = 0.2f),
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .clickable(onClick = onOpenProfile)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    UserAvatar(
                        photoUrl = null,
                        initials = book.authorName.take(1).uppercase().ifBlank { "L" },
                        size = 40.dp
                    )
                }
            }

            // Like Action
            ReelActionButton(
                icon = if (engagement.isLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                label = if (engagement.likesCount > 0) "${engagement.likesCount}" else "Beğen",
                iconTint = if (engagement.isLiked) Color(0xFFE53935) else Color.White,
                onClick = onLike
            )

            // Comment Action
            ReelActionButton(
                icon = Icons.Default.ChatBubbleOutline,
                label = if (engagement.commentsCount > 0) "${engagement.commentsCount}" else "Yorum",
                iconTint = Color.White,
                onClick = onOpenComments
            )

            // Save Action
            ReelActionButton(
                icon = if (engagement.isSaved) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                label = if (engagement.isSaved) "Kaydedildi" else "Kaydet",
                iconTint = if (engagement.isSaved) Color(0xFFFFB300) else Color.White,
                onClick = onSave
            )

            // Share Action
            ReelActionButton(
                icon = Icons.Default.Share,
                label = "Paylaş",
                iconTint = Color.White,
                onClick = onShareBook
            )

            // Book Details Action
            ReelActionButton(
                icon = Icons.Default.Info,
                label = "Detay",
                iconTint = Color.White,
                onClick = onBookClick
            )
        }
    }
}

@Composable
private fun ReelActionButton(
    icon: ImageVector,
    label: String,
    iconTint: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Surface(
            shape = CircleShape,
            color = Color.Black.copy(alpha = 0.4f),
            modifier = Modifier.size(40.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = iconTint,
                    modifier = Modifier.size(22.dp)
                )
            }
        }
        Spacer(Modifier.height(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 11.sp),
            color = Color.White,
            maxLines = 1
        )
    }
}
