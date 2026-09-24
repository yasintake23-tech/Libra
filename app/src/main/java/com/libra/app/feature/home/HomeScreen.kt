package com.libra.app.feature.home

import com.libra.app.core.di.ServiceLocator

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.NotificationsNone
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.PeopleOutline
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.Surface
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.libra.app.core.state.UiState
import com.libra.app.domain.model.Book
import com.libra.app.domain.model.Post
import com.libra.app.domain.model.PostComment
import com.libra.app.domain.model.BookCategory
import com.libra.app.domain.model.UserProfile
import com.libra.app.domain.model.UserShelfItem
import com.libra.app.ui.components.BookCover
import com.libra.app.ui.components.HorizontalBookCard
import com.libra.app.ui.components.LoadingView
import com.libra.app.ui.components.SectionHeader
import com.libra.app.ui.components.UserAvatar
import com.libra.app.ui.components.VerticalBookCard

@Composable
fun HomeScreen(
    uiState: UiState<HomeData>,
    onBookClick: (Book) -> Unit,
    onNavigateToWrite: () -> Unit,
    onNavigateToLibrary: () -> Unit,
    onNavigateToProfile: () -> Unit,
    onNavigateToDiscover: () -> Unit,
    onNavigateToServers: () -> Unit,
    onNotifications: () -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
    onCreatePost: (String) -> Unit = {},
    onOpenCreatePost: () -> Unit = {},
    onOpenCreateStory: () -> Unit = {},
    onToggleLike: (Post) -> Unit = {},
    onToggleSave: (Post) -> Unit = {},
    onDeletePost: (Post) -> Unit = {},
    onOpenComments: (Post) -> Unit = {},
    comments: List<PostComment> = emptyList(),
    onAddComment: (Post, String) -> Unit = { _, _ -> },
    onDeleteComment: (Post, PostComment) -> Unit = { _, _ -> },
    isCommenting: Boolean = false,
    commentError: String? = null,
    onClearCommentError: () -> Unit = {},
    onCloseComments: () -> Unit = {},
    isPosting: Boolean = false,
    postError: String? = null,
    onClearPostError: () -> Unit = {}
) {
    when (uiState) {
        is UiState.Loading -> LoadingView(message = "Libra hazırlanıyor…")
        is UiState.Error -> HomeError(uiState.error.message, onRetry)
        is UiState.Empty -> LoadingView(message = "Hazırlanıyor…")
        is UiState.Success -> {
            val data = uiState.data
            var selectedPost by remember { mutableStateOf<Post?>(null) }
            var commentText by remember { mutableStateOf("") }
            var showCreateMenu by remember { mutableStateOf(false) }
            var selectedSection by remember { mutableStateOf(HomeSection.POSTS) }

            Box(modifier = Modifier.fillMaxSize()) {
                LazyColumn(
                    modifier = modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 18.dp)
                ) {
                    item {
                        HomeHeader(data.currentUser, onNavigateToProfile, onNotifications)
                    }

                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            HomeSectionButton(
                                title = "Gönderiler",
                                selected = selectedSection == HomeSection.POSTS,
                                modifier = Modifier.weight(1f),
                                onClick = { selectedSection = HomeSection.POSTS }
                            )
                            HomeSectionButton(
                                title = "Kitaplar",
                                selected = selectedSection == HomeSection.BOOKS,
                                modifier = Modifier.weight(1f),
                                onClick = { selectedSection = HomeSection.BOOKS }
                            )
                        }
                    }

                    when (selectedSection) {
                        HomeSection.POSTS -> {
                            if (data.posts.isEmpty()) {
                                item { EmptySection("Henüz gösterilecek gönderi yok.") }
                            } else {
                                items(data.posts, key = { it.id }) { post ->
                                    PostCard(
                                        post,
                                        data.currentUser?.uid.orEmpty(),
                                        { onToggleLike(post) },
                                        { onDeletePost(post) },
                                        { onToggleSave(post) },
                                        {
                                            selectedPost = post
                                            commentText = ""
                                            onOpenComments(post)
                                        }
                                    )
                                }
                            }
                        }

                        HomeSection.BOOKS -> {
                            if (data.recentBooks.isEmpty()) {
                                item { EmptySection("Henüz yayınlanmış kitap yok.") }
                            } else {
                                items(data.recentBooks.take(20), key = { it.id }) { book ->
                                    HorizontalBookCard(
                                        book,
                                        { onBookClick(book) },
                                        Modifier.padding(horizontal = 16.dp, vertical = 5.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                AnimatedVisibility(
                    visible = showCreateMenu,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = 76.dp, end = 18.dp),
                    enter = fadeIn() + scaleIn(),
                    exit = fadeOut() + scaleOut()
                ) {
                    CreateChoiceMenu(
                        onStory = {
                            showCreateMenu = false
                            onOpenCreateStory()
                        },
                        onPost = {
                            showCreateMenu = false
                            onOpenCreatePost()
                        }
                    )
                }

                androidx.compose.material3.FloatingActionButton(
                    onClick = { showCreateMenu = !showCreateMenu },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = 12.dp, end = 18.dp),
                    containerColor = MaterialTheme.colorScheme.onBackground,
                    contentColor = MaterialTheme.colorScheme.background
                ) {
                    Icon(
                        Icons.Default.Add,
                        if (showCreateMenu) "Kapat" else "Oluştur"
                    )
                }

                selectedPost?.let { post ->
                    PostCommentsDialog(
                        post = post,
                        currentUserId = data.currentUser?.uid.orEmpty(),
                        comments = comments,
                        text = commentText,
                        onTextChanged = { if (it.length <= 500) commentText = it },
                        onSend = {
                            onAddComment(post, commentText.trim())
                            commentText = ""
                        },
                        onDeleteComment = { onDeleteComment(post, it) },
                        isSending = isCommenting,
                        error = commentError,
                        onClearError = onClearCommentError,
                        onDismiss = {
                            selectedPost = null
                            commentText = ""
                            onCloseComments()
                        }
                    )
                }
            }
        }
    }
}

private enum class HomeSection { POSTS, BOOKS }

@Composable
private fun HomeSectionButton(
    title: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(
                if (selected) MaterialTheme.colorScheme.onBackground
                else MaterialTheme.colorScheme.surface
            )
            .clickable(onClick = onClick)
            .padding(vertical = 13.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            title,
            fontWeight = FontWeight.Bold,
            color = if (selected) {
                MaterialTheme.colorScheme.background
            } else {
                MaterialTheme.colorScheme.onSurface
            }
        )
    }
}

@Composable
private fun EmptySection(text: String) {
    Card(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .padding(28.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun CreateChoiceMenu(
    onStory: () -> Unit,
    onPost: () -> Unit
) {
    Card(
        modifier = Modifier.width(190.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column(Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onStory)
                    .padding(horizontal = 16.dp, vertical = 15.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.AutoStories, null, modifier = Modifier.size(22.dp))
                Spacer(Modifier.width(12.dp))
                Text("Hikâye", fontWeight = FontWeight.SemiBold)
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onPost)
                    .padding(horizontal = 16.dp, vertical = 15.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Edit, null, modifier = Modifier.size(22.dp))
                Spacer(Modifier.width(12.dp))
                Text("Gönderi", fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun PostCard(post: Post, currentUserId: String, onLike: () -> Unit, onDelete: () -> Unit, onSave: () -> Unit, onComment: () -> Unit) {
    Card(
        Modifier.fillMaxWidth().padding(horizontal = 12.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                UserAvatar(post.authorPhotoUrl, post.authorName.take(1).uppercase().ifBlank { "L" }, size = 42.dp)
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(post.authorName.ifBlank { "Libra kullanıcısı" }, fontWeight = FontWeight.SemiBold)
                    Text("@${post.authorUsername.ifBlank { "kullanici" }}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                if (post.authorId == currentUserId) {
                    IconButton(onClick = onDelete) { Icon(Icons.Default.DeleteOutline, "Gönderiyi sil") }
                }
            }
            if (post.title.isNotBlank()) {
                Spacer(Modifier.height(10.dp))
                Text(
                    post.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(Modifier.height(8.dp))
            Text(post.text, style = MaterialTheme.typography.bodyLarge)

            if (post.mediaUrl.isNotBlank() && (post.mediaType == "image" || post.mediaType.startsWith("image/"))) {
                Spacer(Modifier.height(12.dp))
                var mediaFailed by remember(post.mediaUrl) { mutableStateOf(false) }
                if (mediaFailed) {
                    Surface(
                        modifier = Modifier.fillMaxWidth().height(180.dp),
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                "Fotoğraf yüklenemedi",
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else {
                    var mediaUrl by remember(post.mediaUrl) { mutableStateOf<String?>(null) }
                    LaunchedEffect(post.mediaUrl) {
                        mediaUrl = ServiceLocator.storageRepository.getSignedMediaUrl(post.mediaUrl)
                    }

                    if (mediaUrl == null) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(220.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "Fotoğraf hazırlanıyor…",
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        AsyncImage(
                            model = mediaUrl,
                            contentDescription = "Gönderi fotoğrafı",
                            modifier = Modifier.fillMaxWidth().height(220.dp).clip(RoundedCornerShape(14.dp)),
                            contentScale = ContentScale.Crop,
                            onError = { mediaFailed = true }
                        )
                    }
                }
            }

            if (post.tags.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                Text(
                    post.tags.joinToString("  "),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onLike) {
                    Icon(if (post.likedByCurrentUser) Icons.Default.Favorite else Icons.Default.FavoriteBorder, "Beğen")
                }
                Text(post.likesCount.toString(), style = MaterialTheme.typography.labelMedium)
                IconButton(onClick = onComment) { Icon(Icons.Default.ChatBubbleOutline, "Yorumlar") }
                IconButton(onClick = onSave) { Icon(if (post.savedByCurrentUser) Icons.Default.Bookmark else Icons.Default.BookmarkBorder, "Kaydet") }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PostCommentsDialog(
    post: Post,
    currentUserId: String,
    comments: List<PostComment>,
    text: String,
    onTextChanged: (String) -> Unit,
    onSend: () -> Unit,
    onDeleteComment: (PostComment) -> Unit,
    isSending: Boolean,
    error: String?,
    onClearError: () -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        modifier = Modifier.fillMaxHeight(),
        contentWindowInsets = { androidx.compose.foundation.layout.WindowInsets(0, 0, 0, 0) }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight()
                .imePadding()
                .navigationBarsPadding()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp, vertical = 8.dp)
            ) {
                Text("Yorumlar", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text(
                    "${comments.size} yorum",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            androidx.compose.material3.HorizontalDivider()

            LazyColumn(
                modifier = Modifier.fillMaxWidth().weight(1f),
                contentPadding = PaddingValues(horizontal = 18.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                if (comments.isEmpty()) {
                    item {
                        Box(
                            Modifier.fillMaxWidth().padding(vertical = 50.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "Henüz yorum yok. İlk yorumu sen bırak.",
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else {
                    items(comments, key = { it.id }) { comment ->
                        Row(verticalAlignment = Alignment.Top) {
                            UserAvatar(
                                comment.authorPhotoUrl,
                                comment.authorName.take(1).uppercase().ifBlank { "L" },
                                size = 36.dp
                            )
                            Spacer(Modifier.width(10.dp))
                            Column(Modifier.weight(1f)) {
                                Text(
                                    comment.authorName.ifBlank { "Libra kullanıcısı" },
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(comment.text, style = MaterialTheme.typography.bodyMedium)
                                if (comment.authorId == currentUserId) {
                                    TextButton(
                                        onClick = { onDeleteComment(comment) },
                                        contentPadding = PaddingValues(0.dp)
                                    ) { Text("Sil") }
                                }
                            }
                        }
                    }
                }
            }

            if (error != null) {
                TextButton(
                    onClick = onClearError,
                    modifier = Modifier.padding(horizontal = 14.dp)
                ) {
                    Text(error, color = MaterialTheme.colorScheme.error)
                }
            }

            Surface(
                modifier = Modifier.fillMaxWidth(),
                tonalElevation = 3.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.Bottom
                ) {
                    OutlinedTextField(
                        value = text,
                        onValueChange = onTextChanged,
                        placeholder = { Text("Yorum yaz…") },
                        modifier = Modifier.weight(1f),
                        minLines = 1,
                        maxLines = 4,
                        shape = RoundedCornerShape(22.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    TextButton(
                        enabled = text.trim().isNotEmpty() && !isSending,
                        onClick = onSend
                    ) {
                        Text(if (isSending) "…" else "Gönder")
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyFeed(onDiscover: () -> Unit, onWrite: () -> Unit) {
    Card(
        Modifier.fillMaxWidth().padding(horizontal = 12.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(Modifier.fillMaxWidth().padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Akışın daha yeni başlıyor.", fontWeight = FontWeight.SemiBold)
            Text("Keşfet'ten insanları bulabilir veya ilk hikâyeni yazabilirsin.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row {
                TextButton(onClick = onDiscover) { Text("Keşfet") }
                TextButton(onClick = onWrite) { Text("Kitap yaz") }
            }
        }
    }
}

@Composable
private fun HomeHeader(user: UserProfile?, onProfile: () -> Unit, onNotifications: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier.size(34.dp).clip(CircleShape).background(MaterialTheme.colorScheme.onBackground),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.MenuBook, null, tint = MaterialTheme.colorScheme.background, modifier = Modifier.size(19.dp))
            }
            Spacer(Modifier.width(10.dp))
            Column {
                Text("Libra", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold, letterSpacing = (-0.4).sp))
                Text("Her hikâyenin bir yeri var.", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            androidx.compose.material3.IconButton(onClick = onNotifications) {
                Icon(Icons.Default.NotificationsNone, "Bildirimler")
            }
            UserAvatar(user?.profileImageUrl, user?.initials ?: "L", size = 40.dp, modifier = Modifier.clickable(onClick = onProfile))
        }
    }
}

@Composable
private fun SearchBar(onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 2.dp)
            .clip(RoundedCornerShape(14.dp)).background(MaterialTheme.colorScheme.surface)
            .clickable(onClick = onClick).padding(horizontal = 14.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Default.Search, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(10.dp))
        Text("Kitap, yazar veya kullanıcı ara…", color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun HeroCard() {
    Box(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp)
            .height(154.dp).clip(RoundedCornerShape(18.dp))
            .background(Brush.linearGradient(listOf(Color(0xFF20262D), Color(0xFF697382))))
    ) {
        Column(modifier = Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.Center) {
            Text("“Daha iyi bir sen,\nher zaman bir kitap uzaklıktadır.”", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Medium, lineHeight = 28.sp), color = Color.White)
            Spacer(Modifier.height(10.dp))
            Text("LIBRA", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = .7f))
        }
    }
}

@Composable
private fun QuickActions(onDiscover: () -> Unit, onWrite: () -> Unit, onLibrary: () -> Unit, onFriends: () -> Unit, onServers: () -> Unit) {
    LazyRow(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item { ActionTile(Icons.Default.Search, "Keşfet", onDiscover) }
        item { ActionTile(Icons.Default.Edit, "Yaz", onWrite) }
        item { ActionTile(Icons.Default.BookmarkBorder, "Kütüphane", onLibrary) }
        item { ActionTile(Icons.Default.PeopleOutline, "Arkadaşlar", onFriends) }
        item { ActionTile(Icons.Default.Groups, "Sunucular", onServers) }
    }
}

@Composable
private fun ActionTile(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, onClick: () -> Unit) {
    Card(
        modifier = Modifier.width(78.dp).clickable(onClick = onClick),
        shape = RoundedCornerShape(13.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(icon, null, modifier = Modifier.size(22.dp))
            Spacer(Modifier.height(5.dp))
            Text(label, style = MaterialTheme.typography.labelSmall, maxLines = 1)
        }
    }
}

@Composable
private fun CurrentlyReadingCard(item: UserShelfItem, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Card(modifier = modifier.fillMaxWidth().clickable(onClick = onClick), shape = RoundedCornerShape(14.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            BookCover(item.book, Modifier.width(52.dp).aspectRatio(.69f))
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(item.book.title, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold), maxLines = 1)
                Text(item.book.authorName, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(9.dp))
                LinearProgressIndicator(progress = { item.progressPercent / 100f }, modifier = Modifier.fillMaxWidth().height(4.dp).clip(CircleShape))
                Spacer(Modifier.height(4.dp))
                Text("Okuma ilerlemen", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun CategoryRow(selected: BookCategory?, onSelected: (BookCategory) -> Unit) {
    Row(modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        BookCategory.values().take(6).forEach { category ->
            val active = selected == category
            Box(
                modifier = Modifier.clip(RoundedCornerShape(10.dp))
                    .background(if (active) MaterialTheme.colorScheme.onBackground else MaterialTheme.colorScheme.surface)
                    .clickable { onSelected(category) }
                    .padding(horizontal = 15.dp, vertical = 10.dp)
            ) {
                Text(category.displayName, style = MaterialTheme.typography.labelMedium, color = if (active) MaterialTheme.colorScheme.background else MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun EmptyStrip(text: String) {
    Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp).clip(RoundedCornerShape(13.dp)).background(MaterialTheme.colorScheme.surface).padding(18.dp)) {
        Text(text, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun HomeError(message: String, onRetry: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Text("Bir sorun oluştu", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold))
        Spacer(Modifier.height(8.dp))
        Text(message, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        TextButton(onClick = onRetry) { Text("Tekrar dene") }
    }
}
