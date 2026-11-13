package com.libra.app.feature.home

import androidx.activity.compose.BackHandler
import com.libra.app.core.di.ServiceLocator
import com.libra.app.feature.discover.BookDiscoverySection

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import com.libra.app.ui.animation.AppleSprings
import com.libra.app.ui.animation.appleBouncy
import com.libra.app.ui.animation.appleGlassSurface
import com.libra.app.ui.animation.appleSquircle
import com.libra.app.ui.animation.elasticStretch
import com.libra.app.ui.animation.staggeredVaporEntrance
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowForward
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
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
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
import com.libra.app.domain.model.Story
import com.libra.app.domain.model.BookCategory
import com.libra.app.domain.model.UserProfile
import com.libra.app.domain.model.UserShelfItem
import com.libra.app.domain.model.SharedContent
import com.libra.app.feature.share.ShareSheet
import com.libra.app.feature.share.sharedContentUrl
import com.libra.app.ui.components.BookCover
import com.libra.app.ui.components.FullScreenImageViewerDialog
import com.libra.app.ui.components.HorizontalBookCard
import com.libra.app.ui.components.LoadingView
import com.libra.app.ui.components.SectionHeader
import com.libra.app.ui.components.UserAvatar
import com.libra.app.ui.components.VerticalBookCard

@OptIn(ExperimentalMaterial3Api::class)
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
    unreadNotificationCount: Int = 0,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
    onCreatePost: (String) -> Unit = {},
    onOpenCreatePost: () -> Unit = {},
    onOpenCreateStory: () -> Unit = {},
    canPost: Boolean = true,
    canStory: Boolean = true,
    canComment: Boolean = true,
    onToggleLike: (Post) -> Unit = {},
    onToggleSave: (Post) -> Unit = {},
    onDeletePost: (Post) -> Unit = {},
    onOpenComments: (Post) -> Unit = {},
    comments: List<PostComment> = emptyList(),
    onAddComment: (Post, String, String) -> Unit = { _, _, _ -> },
    onDeleteComment: (Post, PostComment) -> Unit = { _, _ -> },
    isCommenting: Boolean = false,
    commentError: String? = null,
    onClearCommentError: () -> Unit = {},
    onCloseComments: () -> Unit = {},
    isPosting: Boolean = false,
    postError: String? = null,
    onClearPostError: () -> Unit = {},
    onStoryReply: (Story) -> Unit = {},
    onStoryLike: (Story) -> Unit = {},
    onStoriesRefresh: () -> Unit = {},
    onOpenProfile: (String) -> Unit = {},
    onCreateBookClick: () -> Unit = {},
    onStartReading: (Book) -> Unit = {},
    onShareBook: (Book) -> Unit = {},
    initialSection: HomeSection = HomeSection.POSTS,
    initialCommentPostId: String? = null,
    onInitialCommentPostConsumed: () -> Unit = {}
) {
    when (uiState) {
        is UiState.Loading -> LoadingView(message = "Libra hazırlanıyor…")
        is UiState.Error -> HomeError(uiState.error.message, onRetry)
        is UiState.Empty -> LoadingView(message = "Hazırlanıyor…")
        is UiState.Success -> {
            val data = uiState.data
            var selectedPost by remember { mutableStateOf<Post?>(null) }
            var selectedLikePost by remember { mutableStateOf<Post?>(null) }
            var commentText by remember { mutableStateOf("") }
            var replyTarget by remember { mutableStateOf<PostComment?>(null) }
            var showCreateMenu by remember { mutableStateOf(false) }
            var selectedSection by remember(initialSection) { mutableStateOf(initialSection) }
            var selectedStoryIndex by remember { mutableStateOf<Int?>(null) }
            var shareContent by remember { mutableStateOf<SharedContent?>(null) }
            var likeUsersPost by remember { mutableStateOf<Post?>(null) }

            LaunchedEffect(initialCommentPostId, data.posts) {
                val targetId = initialCommentPostId ?: return@LaunchedEffect
                val targetPost = data.posts.firstOrNull { it.id == targetId }
                if (targetPost != null) {
                    selectedPost = targetPost
                    commentText = ""
                    replyTarget = null
                    onOpenComments(targetPost)
                    onInitialCommentPostConsumed()
                }
            }

            Box(modifier = Modifier.fillMaxSize()) {
                when (selectedSection) {
                    HomeSection.POSTS -> {
                        LazyColumn(
                            modifier = modifier.fillMaxSize(),
                            contentPadding = PaddingValues(bottom = 18.dp)
                        ) {
                            item {
                                HomeHeader(
                                    data.currentUser,
                                    onNavigateToProfile,
                                    onNotifications,
                                    unreadNotificationCount
                                )
                            }

                            item {
                                StoryStrip(
                                    stories = data.stories,
                                    currentUserId = data.currentUser?.uid.orEmpty(),
                                    onStoryClick = { story -> selectedStoryIndex = data.stories.indexOfFirst { it.id == story.id }.takeIf { it >= 0 } },
                                    onOpenProfile = onOpenProfile,
                                    onCreateStory = onOpenCreateStory,
                                    modifier = Modifier.padding(top = 2.dp)
                                )
                            }

                            item {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 4.dp),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    HomeSectionButton(
                                        title = "Gönderiler",
                                        selected = true,
                                        modifier = Modifier.weight(1f),
                                        onClick = { selectedSection = HomeSection.POSTS }
                                    )
                                    HomeSectionButton(
                                        title = "Kitaplar",
                                        selected = false,
                                        modifier = Modifier.weight(1f),
                                        onClick = { selectedSection = HomeSection.BOOKS }
                                    )
                                }
                            }

                            if (data.posts.isEmpty()) {
                                item { EmptySection("Henüz gösterilecek gönderi yok.") }
                            } else {
                                itemsIndexed(data.posts, key = { _, it -> it.id }) { index, post ->
                                    PostCard(
                                        post = post,
                                        currentUserId = data.currentUser?.uid.orEmpty(),
                                        onLike = { onToggleLike(post) },
                                        onShowLikes = { likeUsersPost = post },
                                        onComment = {
                                            if (canComment) {
                                                selectedPost = post
                                                commentText = ""
                                                replyTarget = null
                                                onOpenComments(post)
                                            }
                                        },
                                        onSave = { onToggleSave(post) },
                                        onShare = {
                                            shareContent = SharedContent(
                                                type = "post",
                                                id = post.id,
                                                title = post.title.ifBlank { "Gönderi" },
                                                text = post.text,
                                                authorId = post.authorId,
                                                authorName = post.authorName,
                                                mediaUrl = post.mediaUrl,
                                                url = sharedContentUrl("post", post.id)
                                            )
                                        },
                                        onOpenProfile = { onOpenProfile(post.authorId) },
                                        onDetailClick = {
                                            if (canComment) {
                                                selectedPost = post
                                                commentText = ""
                                                replyTarget = null
                                                onOpenComments(post)
                                            }
                                        },
                                        onDelete = { onDeletePost(post) },
                                        canComment = canComment,
                                        index = index
                                    )
                                    Spacer(Modifier.height(10.dp))
                                }
                            }
                        }
                    }

                    HomeSection.BOOKS -> {
                        BackHandler(enabled = true) {
                            selectedSection = HomeSection.POSTS
                        }

                        BookDiscoverySection(
                            onStartReading = onStartReading,
                            onBookClick = onBookClick,
                            onOpenProfile = onOpenProfile,
                            onShareBook = onShareBook,
                            onCreateBookClick = onCreateBookClick,
                            topBar = {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .statusBarsPadding()
                                        .padding(horizontal = 16.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(24.dp),
                                        color = Color.Black.copy(alpha = 0.55f),
                                        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.18f))
                                    ) {
                                        Row(modifier = Modifier.padding(3.dp)) {
                                            Surface(
                                                shape = RoundedCornerShape(20.dp),
                                                color = Color.Transparent,
                                                modifier = Modifier.clickable { selectedSection = HomeSection.POSTS }
                                            ) {
                                                Text(
                                                    text = "Gönderiler",
                                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                                                    color = Color.White.copy(alpha = 0.8f),
                                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                                                )
                                            }
                                            Surface(
                                                shape = RoundedCornerShape(20.dp),
                                                color = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.clickable { }
                                            ) {
                                                Text(
                                                    text = "Kitaplar",
                                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                                    color = MaterialTheme.colorScheme.onPrimary,
                                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                                                )
                                            }
                                        }
                                    }

                                    Surface(
                                        shape = RoundedCornerShape(20.dp),
                                        color = Color.Black.copy(alpha = 0.55f),
                                        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.18f)),
                                        modifier = Modifier.clickable { onCreateBookClick() }
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                        ) {
                                            Icon(
                                                Icons.Default.Add,
                                                contentDescription = null,
                                                tint = Color.White,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(Modifier.width(4.dp))
                                            Text(
                                                text = "Kitap Yaz",
                                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                                color = Color.White
                                            )
                                        }
                                    }
                                }
                            },
                            modifier = modifier.fillMaxSize()
                        )
                    }
                }

                AnimatedVisibility(
                    visible = showCreateMenu && selectedSection == HomeSection.POSTS,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = 76.dp, end = 18.dp),
                    enter = fadeIn(
                        animationSpec = tween(140)
                    ) + scaleIn(
                        initialScale = 0.88f,
                        animationSpec = tween(180, easing = FastOutSlowInEasing)
                    ),
                    exit = fadeOut(
                        animationSpec = tween(100)
                    ) + scaleOut(
                        targetScale = 0.92f,
                        animationSpec = tween(120, easing = FastOutSlowInEasing)
                    )
                ) {
                    CreateChoiceMenu(
                        canStory = canStory,
                        canPost = canPost,
                        onStory = {
                            showCreateMenu = false
                            onOpenCreateStory()
                        },
                        onPost = {
                            showCreateMenu = false
                            onOpenCreatePost()
                        },
                        onBook = {
                            showCreateMenu = false
                            onCreateBookClick()
                        }
                    )
                }

                if (selectedSection == HomeSection.POSTS) {
                    val createButtonRotation by animateFloatAsState(
                        targetValue = if (showCreateMenu) 45f else 0f,
                        animationSpec = tween(durationMillis = 200, easing = FastOutSlowInEasing),
                        label = "createButtonRotation"
                    )

                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(top = 14.dp, end = 20.dp)
                            .size(44.dp)
                            .elasticStretch(maxStretchDp = 14.dp)
                            .appleBouncy(
                                pressedScale = 0.88f,
                                onClick = { showCreateMenu = !showCreateMenu }
                            )
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.onBackground),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = if (showCreateMenu) "Kapat" else "Oluştur",
                            tint = MaterialTheme.colorScheme.background,
                            modifier = Modifier
                                .size(22.dp)
                                .graphicsLayer {
                                    rotationZ = createButtonRotation
                                }
                        )
                    }
                }

                selectedStoryIndex?.let { index ->
                    StoryViewer(
                        stories = data.stories,
                        initialIndex = index,
                        currentUserId = data.currentUser?.uid.orEmpty(),
                        onDismiss = {
                            selectedStoryIndex = null
                            onStoriesRefresh()
                        },
                        onReply = { story ->
                            onStoryReply(story)
                            selectedStoryIndex = null
                        },
                        onLike = onStoryLike,
                        onOpenProfile = onOpenProfile,
                        onShare = { story ->
                            shareContent = SharedContent(
                                "story",
                                story.id,
                                if (story.text.isBlank()) "Hikâye" else story.text.take(80),
                                story.text,
                                story.authorId,
                                story.authorName,
                                story.mediaUrl,
                                sharedContentUrl("story", story.id)
                            )
                        }
                    )
                }

                likeUsersPost?.let { post ->
                    PostLikersBottomSheet(
                        post = post,
                        onOpenProfile = onOpenProfile,
                        onDismiss = { likeUsersPost = null }
                    )
                }

                shareContent?.let { content ->
                    ShareSheet(content = content, onDismiss = { shareContent = null })
                }

                selectedPost?.let { post ->
                    PostCommentsDialog(
                        post = post,
                        currentUserId = data.currentUser?.uid.orEmpty(),
                        comments = comments,
                        text = commentText,
                        onTextChanged = { if (it.length <= 500) commentText = it },
                        replyTarget = replyTarget,
                        onReply = { replyTarget = it },
                        onSend = {
                            onAddComment(post, commentText.trim(), replyTarget?.id.orEmpty())
                            commentText = ""
                            replyTarget = null
                        },
                        onDeleteComment = { onDeleteComment(post, it) },
                        isSending = isCommenting,
                        error = commentError,
                        onClearError = onClearCommentError,
                        onOpenProfile = onOpenProfile,
                        onDismiss = {
                            selectedPost = null
                            commentText = ""
                            replyTarget = null
                            onCloseComments()
                        }
                    )
                }
            }
        }
    }
}

enum class HomeSection { POSTS, BOOKS }

@Composable
private fun HomeSectionButton(
    title: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val scale by animateFloatAsState(
        targetValue = if (selected) 1.02f else 1.0f,
        animationSpec = AppleSprings.FloatSnappy,
        label = "section_btn_scale"
    )
    val shape = RoundedCornerShape(14.dp)

    Box(
        modifier = modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(shape)
            .background(
                if (selected) MaterialTheme.colorScheme.onBackground
                else MaterialTheme.colorScheme.surface
            )
            .then(
                if (!selected) {
                    Modifier.border(
                        BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.6f)),
                        shape = shape
                    )
                } else {
                    Modifier
                }
            )
            .appleBouncy(pressedScale = 0.95f, onClick = onClick)
            .padding(vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
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
    canStory: Boolean = true,
    canPost: Boolean = true,
    onStory: () -> Unit,
    onPost: () -> Unit,
    onBook: () -> Unit
) {
    Box(
        modifier = Modifier
            .width(190.dp)
            .appleGlassSurface(cornerRadius = 18.dp, elevation = 12.dp)
    ) {
        Column(Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .staggeredVaporEntrance(index = 0)
                    .appleBouncy(enabled = canStory, pressedScale = 0.95f, onClick = onStory)
                    .alpha(if (canStory) 1f else 0.45f)
                    .padding(horizontal = 16.dp, vertical = 13.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.AutoStories, null, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(12.dp))
                Text("Hikâye", fontWeight = FontWeight.SemiBold)
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .staggeredVaporEntrance(index = 1)
                    .appleBouncy(enabled = canPost, pressedScale = 0.95f, onClick = onPost)
                    .alpha(if (canPost) 1f else 0.45f)
                    .padding(horizontal = 16.dp, vertical = 13.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Edit, null, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(12.dp))
                Text("Gönderi", fontWeight = FontWeight.SemiBold)
            }

            androidx.compose.material3.HorizontalDivider(
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .staggeredVaporEntrance(index = 2)
                    .appleBouncy(pressedScale = 0.95f, onClick = onBook)
                    .padding(horizontal = 16.dp, vertical = 13.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.MenuBook,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(12.dp))
                Text(
                    "Kitap Yaz",
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun PostCard(
    post: Post,
    currentUserId: String,
    onLike: () -> Unit,
    onShowLikes: () -> Unit = {},
    onComment: () -> Unit = {},
    onSave: () -> Unit = {},
    onShare: () -> Unit = {},
    onOpenProfile: () -> Unit = {},
    onDetailClick: () -> Unit = onComment,
    onDelete: () -> Unit = {},
    canComment: Boolean = true,
    index: Int = 0
) {
    var showOptionsMenu by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .staggeredVaporEntrance(index = index)
            .appleSquircle(16.dp)
            .padding(horizontal = 12.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(Modifier.padding(14.dp)) {
            // Header Row: Avatar, Names, Options Menu
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                UserAvatar(
                    photoUrl = post.authorPhotoUrl,
                    initials = post.authorName.take(1).uppercase().ifBlank { "L" },
                    size = 42.dp,
                    onClick = onOpenProfile
                )
                Spacer(Modifier.width(10.dp))
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .appleBouncy(pressedScale = 0.98f, onClick = onOpenProfile)
                ) {
                    Text(
                        text = post.authorName.ifBlank { "Libra kullanıcısı" },
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "@${post.authorUsername.ifBlank { "kullanici" }}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Options dropdown menu (Üç nokta menüsü -> Gönderi seçenekleri)
                Box {
                    IconButton(onClick = { showOptionsMenu = true }) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "Gönderi seçenekleri"
                        )
                    }
                    DropdownMenu(
                        expanded = showOptionsMenu,
                        onDismissRequest = { showOptionsMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Paylaş") },
                            onClick = {
                                showOptionsMenu = false
                                onShare()
                            },
                            leadingIcon = {
                                Icon(Icons.Default.Share, contentDescription = null)
                            }
                        )
                        DropdownMenuItem(
                            text = {
                                Text(if (post.savedByCurrentUser) "Kaydedilenlerden Çıkar" else "Kaydet")
                            },
                            onClick = {
                                showOptionsMenu = false
                                onSave()
                            },
                            leadingIcon = {
                                Icon(
                                    if (post.savedByCurrentUser) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                                    contentDescription = null
                                )
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Profili Gör") },
                            onClick = {
                                showOptionsMenu = false
                                onOpenProfile()
                            },
                            leadingIcon = {
                                Icon(Icons.Default.Person, contentDescription = null)
                            }
                        )
                        if (post.authorId == currentUserId) {
                            DropdownMenuItem(
                                text = {
                                    Text("Gönderiyi Sil", color = MaterialTheme.colorScheme.error)
                                },
                                onClick = {
                                    showOptionsMenu = false
                                    onDelete()
                                },
                                leadingIcon = {
                                    Icon(
                                        Icons.Default.DeleteOutline,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                }
                            )
                        }
                    }
                }
            }

            // Post Title & Body (clickable to open details)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onDetailClick)
            ) {
                if (post.title.isNotBlank()) {
                    Spacer(Modifier.height(10.dp))
                    Text(
                        post.title,
                        style = MaterialTheme.typography.titleMedium.copy(lineHeight = 22.sp),
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    post.text,
                    style = MaterialTheme.typography.bodyLarge.copy(lineHeight = 24.sp)
                )
            }

            // Post Media Image (Gönderi görseli -> Tam Ekran Görüntüleyici)
            if (post.mediaUrl.isNotBlank() && (post.mediaType == "image" || post.mediaType.startsWith("image/"))) {
                Spacer(Modifier.height(12.dp))
                var mediaFailed by remember(post.mediaUrl) { mutableStateOf(false) }
                var showFullScreenImage by remember(post.mediaUrl) { mutableStateOf(false) }
                if (mediaFailed) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp),
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
                        val currentUrl = mediaUrl
                        AsyncImage(
                            model = currentUrl,
                            contentDescription = "Gönderi fotoğrafı",
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(220.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .clickable { showFullScreenImage = true },
                            contentScale = ContentScale.Crop,
                            onError = { mediaFailed = true }
                        )

                        if (showFullScreenImage && currentUrl != null) {
                            FullScreenImageViewerDialog(
                                mediaUrl = currentUrl,
                                onDismiss = { showFullScreenImage = false }
                            )
                        }
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

            Spacer(Modifier.height(8.dp))

            // Action Row: Like, Like Count, Comment, Comment Count, Share, Save
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                val haptic = LocalHapticFeedback.current

                val likeScale by animateFloatAsState(
                    targetValue = if (post.likedByCurrentUser) 1.25f else 1.0f,
                    animationSpec = AppleSprings.FloatBouncy,
                    label = "heart_bounce"
                )

                // Like Button (Tek Dokunma: Beğen / Beğeniyi Kaldır, Basılı Tutma: Beğenen Kişileri Gör)
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .appleBouncy(
                            pressedScale = 0.85f,
                            onClick = onLike,
                            onLongClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                onShowLikes()
                            }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (post.likedByCurrentUser) {
                            Icons.Default.Favorite
                        } else {
                            Icons.Default.FavoriteBorder
                        },
                        contentDescription = if (post.likedByCurrentUser) "Beğeniyi kaldır" else "Beğen (Basılı tutarak beğenenleri gör)",
                        tint = if (post.likedByCurrentUser) {
                            MaterialTheme.colorScheme.error
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        modifier = Modifier.graphicsLayer {
                            scaleX = likeScale
                            scaleY = likeScale
                        }
                    )
                }

                // Like Count (Tek dokunma veya basılı tutma: Beğenenleri Gör)
                Text(
                    text = post.likesCount.toString(),
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .appleBouncy(
                            pressedScale = 0.90f,
                            onClick = onShowLikes,
                            onLongClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                onShowLikes()
                            }
                        )
                        .padding(horizontal = 6.dp, vertical = 8.dp),
                    style = MaterialTheme.typography.labelMedium
                )

                Spacer(Modifier.width(8.dp))

                // Comment Button
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .appleBouncy(
                            enabled = canComment,
                            pressedScale = 0.85f,
                            onClick = onComment
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.ChatBubbleOutline,
                        contentDescription = "Yorumlar",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Comment Count (Single click opens Comments!)
                Text(
                    text = post.commentsCount.toString(),
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .appleBouncy(
                            enabled = canComment,
                            pressedScale = 0.90f,
                            onClick = onComment
                        )
                        .padding(horizontal = 6.dp, vertical = 8.dp),
                    style = MaterialTheme.typography.labelMedium
                )

                Spacer(Modifier.weight(1f))

                // Share Button
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .appleBouncy(
                            pressedScale = 0.85f,
                            onClick = onShare
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = "Paylaş",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                val saveScale by animateFloatAsState(
                    targetValue = if (post.savedByCurrentUser) 1.20f else 1.0f,
                    animationSpec = AppleSprings.FloatBouncy,
                    label = "save_bounce"
                )

                // Save Button
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .appleBouncy(
                            pressedScale = 0.85f,
                            onClick = onSave
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (post.savedByCurrentUser) {
                            Icons.Default.Bookmark
                        } else {
                            Icons.Default.BookmarkBorder
                        },
                        contentDescription = if (post.savedByCurrentUser) "Kaydedildi" else "Kaydet",
                        tint = if (post.savedByCurrentUser) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        modifier = Modifier.graphicsLayer {
                            scaleX = saveScale
                            scaleY = saveScale
                        }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PostLikersBottomSheet(
    post: Post,
    onOpenProfile: (String) -> Unit = {},
    onDismiss: () -> Unit
) {
    var users by remember(post.id) { mutableStateOf<List<UserProfile>>(emptyList()) }
    var loading by remember(post.id) { mutableStateOf(true) }
    var error by remember(post.id) { mutableStateOf<String?>(null) }
    var searchQuery by remember(post.id) { mutableStateOf("") }
    var reloadTrigger by remember(post.id) { mutableStateOf(0) }

    LaunchedEffect(post.id, reloadTrigger) {
        loading = true
        error = null
        when (val result = ServiceLocator.postRepository.getPostLikeUsers(post.id)) {
            is com.libra.app.core.result.AppResult.Success -> users = result.data
            is com.libra.app.core.result.AppResult.Error -> error = result.error.message
        }
        loading = false
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = {
            Surface(
                modifier = Modifier.padding(vertical = 10.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                shape = CircleShape
            ) {
                Box(modifier = Modifier.size(width = 36.dp, height = 4.dp))
            }
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 6.dp)
                .navigationBarsPadding()
        ) {
            // Header: Heart icon, title, count badge, close button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Favorite,
                        contentDescription = null,
                        tint = Color(0xFFE53935),
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "Beğenenler",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.width(10.dp))
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        val count = if (users.isNotEmpty()) users.size else post.likesCount
                        Text(
                            text = "$count kişi",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }

                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Kapat")
                }
            }

            Spacer(Modifier.height(10.dp))

            // Search Filter (shown if more than 3 likers)
            if (users.size > 3) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Beğenenlerde ara…", style = MaterialTheme.typography.bodySmall) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(18.dp)) },
                    trailingIcon = {
                        if (searchQuery.isNotBlank()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Close, contentDescription = "Temizle", modifier = Modifier.size(16.dp))
                            }
                        }
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp)
                )
                Spacer(Modifier.height(12.dp))
            }

            // Body
            when {
                loading -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(32.dp),
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(Modifier.height(12.dp))
                            Text(
                                text = "Beğenenler yükleniyor…",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                error != null -> {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = error ?: "Beğenenler yüklenemedi.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error
                        )
                        Spacer(Modifier.height(12.dp))
                        Button(onClick = { reloadTrigger++ }) {
                            Text("Tekrar Dene")
                        }
                    }
                }
                users.isEmpty() -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(160.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Default.FavoriteBorder,
                                contentDescription = null,
                                modifier = Modifier.size(36.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                            )
                            Spacer(Modifier.height(8.dp))
                            Text(
                                text = "Bu gönderiyi henüz kimse beğenmedi.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                else -> {
                    val filteredUsers = if (searchQuery.isBlank()) users else {
                        users.filter {
                            it.displayName.contains(searchQuery, ignoreCase = true) ||
                            it.username.contains(searchQuery, ignoreCase = true)
                        }
                    }

                    if (filteredUsers.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Aramana uygun kullanıcı bulunamadı.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 420.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(filteredUsers, key = { it.uid }) { user ->
                                Surface(
                                    shape = RoundedCornerShape(14.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            onDismiss()
                                            onOpenProfile(user.uid)
                                        }
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 12.dp, vertical = 10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        UserAvatar(
                                            photoUrl = user.profileImageUrl,
                                            initials = user.initials,
                                            size = 42.dp,
                                            onClick = {
                                                onDismiss()
                                                onOpenProfile(user.uid)
                                            }
                                        )
                                        Spacer(Modifier.width(12.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = user.displayName.ifBlank { "Libra kullanıcısı" },
                                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            if (user.username.isNotBlank()) {
                                                Text(
                                                    text = "@${user.username}",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                        }
                                        Icon(
                                            imageVector = Icons.Default.ChevronRight,
                                            contentDescription = "Profili Gör",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
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
    replyTarget: PostComment?,
    onReply: (PostComment?) -> Unit,
    onSend: () -> Unit,
    onDeleteComment: (PostComment) -> Unit,
    isSending: Boolean,
    error: String?,
    onClearError: () -> Unit,
    onOpenProfile: (String) -> Unit = {},
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
            modifier = Modifier.fillMaxWidth().fillMaxHeight().imePadding().navigationBarsPadding()
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 8.dp)
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
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (comments.isEmpty()) {
                    item {
                        Box(Modifier.fillMaxWidth().padding(vertical = 50.dp), contentAlignment = Alignment.Center) {
                            Text("Henüz yorum yok. İlk yorumu sen bırak.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                } else {
                    items(comments, key = { it.id }) { comment ->
                        val isReply = comment.parentCommentId.isNotBlank()
                        Row(
                            modifier = Modifier.padding(start = if (isReply) 38.dp else 0.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            UserAvatar(
                                photoUrl = comment.authorPhotoUrl,
                                initials = comment.authorName.take(1).uppercase().ifBlank { "L" },
                                size = if (isReply) 32.dp else 36.dp,
                                onClick = {
                                    onDismiss()
                                    onOpenProfile(comment.authorId)
                                }
                            )
                            Spacer(Modifier.width(10.dp))
                            Column(Modifier.weight(1f)) {
                                Surface(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(14.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
                                ) {
                                    Column(Modifier.padding(horizontal = 12.dp, vertical = 9.dp)) {
                                        Text(
                                            text = comment.authorName.ifBlank { "Libra kullanıcısı" },
                                            fontWeight = FontWeight.SemiBold,
                                            modifier = Modifier.clickable {
                                                onDismiss()
                                                onOpenProfile(comment.authorId)
                                            }
                                        )
                                        Text(comment.text, style = MaterialTheme.typography.bodyMedium)
                                    }
                                }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    if (comment.authorId != currentUserId) {
                                        TextButton(onClick = { onReply(comment) }, contentPadding = PaddingValues(horizontal = 4.dp)) {
                                            Text("Yanıtla")
                                        }
                                    }
                                    if (comment.authorId == currentUserId) {
                                        TextButton(onClick = { onDeleteComment(comment) }, contentPadding = PaddingValues(horizontal = 4.dp)) {
                                            Text("Sil")
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            if (replyTarget != null) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Row(Modifier.padding(horizontal = 14.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("Yanıtlanıyor", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                            Text(
                                "@${replyTarget.authorUsername.ifBlank { replyTarget.authorName }}: ${replyTarget.text}",
                                maxLines = 1,
                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                        TextButton(onClick = { onReply(null) }) { Text("Kapat") }
                    }
                }
            }
            if (error != null) {
                TextButton(onClick = onClearError, modifier = Modifier.padding(horizontal = 14.dp)) {
                    Text(error, color = MaterialTheme.colorScheme.error)
                }
            }
            Surface(modifier = Modifier.fillMaxWidth(), tonalElevation = 3.dp) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.Bottom
                ) {
                    OutlinedTextField(
                        value = text,
                        onValueChange = onTextChanged,
                        placeholder = { Text(if (replyTarget != null) "Yanıtını yaz…" else "Yorum yaz…") },
                        modifier = Modifier.weight(1f),
                        minLines = 1,
                        maxLines = 4,
                        shape = RoundedCornerShape(22.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    TextButton(enabled = text.trim().isNotEmpty() && !isSending, onClick = onSend) {
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
private fun HomeHeader(user: UserProfile?, onProfile: () -> Unit, onNotifications: () -> Unit, unreadNotificationCount: Int = 0) {
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
            Box {
                androidx.compose.material3.IconButton(onClick = onNotifications) {
                    Icon(
                        Icons.Default.NotificationsNone,
                        "Bildirimler",
                        tint = if (unreadNotificationCount > 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                    )
                }
                if (unreadNotificationCount > 0) {
                    Surface(
                        modifier = Modifier.align(Alignment.TopEnd).padding(top = 2.dp, end = 1.dp),
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    ) {
                        Text(
                            if (unreadNotificationCount > 99) "99+" else unreadNotificationCount.toString(),
                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
            UserAvatar(photoUrl = user?.profileImageUrl, initials = user?.initials ?: "L", size = 40.dp, onClick = onProfile)
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
