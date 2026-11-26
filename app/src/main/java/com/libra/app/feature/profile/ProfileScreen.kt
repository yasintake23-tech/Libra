package com.libra.app.feature.profile

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddBox
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.libra.app.core.state.UiState
import com.libra.app.domain.model.Book
import com.libra.app.domain.model.CosmeticRole
import com.libra.app.domain.model.Post
import com.libra.app.domain.model.SharedContent
import com.libra.app.domain.model.UserProfile
import com.libra.app.feature.home.StoryViewer
import com.libra.app.feature.share.ShareSheet
import com.libra.app.feature.share.sharedContentUrl
import com.libra.app.ui.animation.appleBouncy
import com.libra.app.ui.components.CosmeticRoleReveal
import com.libra.app.ui.components.LoadingView

private enum class SavedFilterType { POSTS, BOOKS }

@Composable
fun ProfileScreen(
    uiState: UiState<UserProfile>,
    onSignOutClick: () -> Unit = {},
    onRetry: () -> Unit,
    onSettingsClick: () -> Unit = {},
    onEditProfileClick: () -> Unit = {},
    onCreatePostClick: () -> Unit = {},
    onCreateStoryClick: () -> Unit = {},
    onCreateBookClick: () -> Unit = {},
    onOpenBook: (Book) -> Unit = {},
    onManageBook: (Book) -> Unit = {},
    onMyBooksClick: () -> Unit = {},
    onOpenProfile: (String) -> Unit = {},
    contentState: ProfileContentState = ProfileContentState(),
    onRefresh: () -> Unit = onRetry,
    onTogglePostLike: (Post) -> Unit = {},
    onTogglePostSave: (Post) -> Unit = {},
    onDeletePost: (Post) -> Unit = {},
    onToggleStoryLike: (com.libra.app.domain.model.Story) -> Unit = {},
    onHideBottomBarChanged: (Boolean) -> Unit = {},
    onFullScreenChanged: (Boolean) -> Unit = {},
    modifier: Modifier = Modifier
) {
    when (uiState) {
        is UiState.Loading -> LoadingView(message = "Profil yükleniyor…", modifier = modifier)
        is UiState.Error -> Column(
            modifier = modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(uiState.error.message, color = MaterialTheme.colorScheme.error)
            Spacer(Modifier.height(8.dp))
            TextButton(onClick = onRetry) { Text("Tekrar dene") }
        }
        is UiState.Empty -> Column(
            modifier = modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text("Profil bulunamadı", color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(8.dp))
            TextButton(onClick = onRetry) { Text("Yenile") }
        }
        is UiState.Success -> ProfileContent(
            profile = uiState.data,
            contentState = contentState,
            onSettingsClick = onSettingsClick,
            onEditProfileClick = onEditProfileClick,
            onCreatePostClick = onCreatePostClick,
            onCreateStoryClick = onCreateStoryClick,
            onCreateBookClick = onCreateBookClick,
            onOpenBook = onOpenBook,
            onManageBook = onManageBook,
            onMyBooksClick = onMyBooksClick,
            onOpenProfile = onOpenProfile,
            onRefresh = onRefresh,
            onTogglePostLike = onTogglePostLike,
            onTogglePostSave = onTogglePostSave,
            onDeletePost = onDeletePost,
            onToggleStoryLike = onToggleStoryLike,
            onHideBottomBarChanged = onHideBottomBarChanged,
            onFullScreenChanged = onFullScreenChanged,
            modifier = modifier
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ProfileContent(
    profile: UserProfile,
    contentState: ProfileContentState,
    onSettingsClick: () -> Unit,
    onEditProfileClick: () -> Unit,
    onCreatePostClick: () -> Unit,
    onCreateStoryClick: () -> Unit,
    onCreateBookClick: () -> Unit,
    onOpenBook: (Book) -> Unit,
    onManageBook: (Book) -> Unit,
    onMyBooksClick: () -> Unit,
    onOpenProfile: (String) -> Unit,
    onRefresh: () -> Unit,
    onTogglePostLike: (Post) -> Unit,
    onTogglePostSave: (Post) -> Unit,
    onDeletePost: (Post) -> Unit,
    onToggleStoryLike: (com.libra.app.domain.model.Story) -> Unit,
    onHideBottomBarChanged: (Boolean) -> Unit,
    onFullScreenChanged: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableStateOf(ProfileContentTab.POSTS) }
    var savedFilter by remember { mutableStateOf(SavedFilterType.POSTS) }
    var socialDialog by remember { mutableStateOf<ProfileSocialListType?>(null) }
    var selectedPostId by remember { mutableStateOf<String?>(null) }
    var showCreateSheet by remember { mutableStateOf(false) }
    var showStoryViewer by remember { mutableStateOf(false) }
    var shareContent by remember { mutableStateOf<SharedContent?>(null) }
    var selectedCosmetic by remember { mutableStateOf<CosmeticRole?>(null) }

    val selectedPost = remember(selectedPostId, contentState.posts, contentState.savedPosts) {
        selectedPostId?.let { id ->
            contentState.posts.firstOrNull { it.id == id }
                ?: contentState.savedPosts.firstOrNull { it.id == id }
        }
    }

    val hideBottomBar = showStoryViewer || selectedPost != null || showCreateSheet ||
        socialDialog != null || shareContent != null || selectedCosmetic != null
    val isImmersiveFullScreen = showStoryViewer
    androidx.compose.runtime.LaunchedEffect(hideBottomBar) {
        onHideBottomBarChanged(hideBottomBar)
    }
    androidx.compose.runtime.LaunchedEffect(isImmersiveFullScreen) {
        onFullScreenChanged(isImmersiveFullScreen)
    }
    androidx.compose.runtime.DisposableEffect(Unit) {
        onDispose {
            onHideBottomBarChanged(false)
            onFullScreenChanged(false)
        }
    }

    val booksCount = maxOf(profile.booksWrittenCount, contentState.writtenBooks.size)
    val followersCount = maxOf(profile.followersCount, contentState.followers.size)
    val followingCount = maxOf(profile.followingCount, contentState.following.size)

    Box(modifier = modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Instagram-style Top App Bar: @username on left, Create (+) and single Settings Menu on right
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 8.dp, top = 8.dp, bottom = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = profile.handle,
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (contentState.adminRole != null) {
                        Icon(
                            imageVector = Icons.Default.Verified,
                            contentDescription = "Yetkili hesap",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    IconButton(
                        onClick = { showCreateSheet = true },
                        modifier = Modifier
                            .appleBouncy(pressedScale = 0.86f, onClick = { showCreateSheet = true })
                            .testTag("profile_create_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.AddBox,
                            contentDescription = "Oluştur",
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                    }

                    IconButton(
                        onClick = onSettingsClick,
                        modifier = Modifier
                            .appleBouncy(pressedScale = 0.86f, onClick = onSettingsClick)
                            .testTag("profile_settings_menu_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Menu,
                            contentDescription = "Ayarlar ve hareketler",
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                    }
                }
            }

            PullToRefreshBox(
                isRefreshing = contentState.isRefreshing,
                onRefresh = onRefresh,
                modifier = Modifier.fillMaxSize()
            ) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 96.dp)
                ) {
                    item(key = "profile-header") {
                        InstagramProfileHeader(
                            profile = profile,
                            postsCount = contentState.posts.size,
                            booksCount = booksCount,
                            followersCount = followersCount,
                            followingCount = followingCount,
                            hasActiveStory = contentState.activeStories.isNotEmpty(),
                            showAddStoryBadge = profile.moderation.canStory,
                            onAvatarClick = {
                                if (contentState.activeStories.isNotEmpty()) {
                                    showStoryViewer = true
                                } else if (profile.moderation.canStory) {
                                    onCreateStoryClick()
                                }
                            },
                            onAddStoryClick = {
                                if (profile.moderation.canStory) onCreateStoryClick()
                            },
                            onPostsStatClick = { selectedTab = ProfileContentTab.POSTS },
                            onBooksStatClick = { selectedTab = ProfileContentTab.BOOKS },
                            onFollowersClick = { socialDialog = ProfileSocialListType.FOLLOWERS },
                            onFollowingClick = { socialDialog = ProfileSocialListType.FOLLOWING }
                        )
                    }

                    item(key = "profile-bio") {
                        Spacer(Modifier.height(4.dp))
                        InstagramProfileBioSection(
                            profile = profile,
                            booksCount = booksCount,
                            adminRole = contentState.adminRole,
                            cosmeticRoles = contentState.cosmeticRoles,
                            isOwnProfile = true,
                            onCosmeticClick = { selectedCosmetic = it },
                            onEmptyBioClick = onEditProfileClick
                        )
                    }

                    item(key = "profile-actions") {
                        Spacer(Modifier.height(14.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            InstagramProfileSecondaryButton(
                                text = "Profili Düzenle",
                                onClick = onEditProfileClick,
                                leadingIcon = Icons.Default.Edit,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("profile_edit_button")
                            )
                            InstagramProfileSecondaryButton(
                                text = "Profili Paylaş",
                                onClick = {
                                    shareContent = SharedContent(
                                        type = "profile",
                                        id = profile.uid,
                                        title = "${profile.displayName} (${profile.handle})",
                                        text = profile.bio.ifBlank { "Libra'daki profilimi keşfet!" },
                                        authorId = profile.uid,
                                        authorName = profile.displayName,
                                        mediaUrl = profile.profileImageUrl,
                                        url = sharedContentUrl("profile", profile.uid)
                                    )
                                },
                                leadingIcon = Icons.Default.Share,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("profile_share_button")
                            )
                        }
                        Spacer(Modifier.height(14.dp))
                    }

                    item(key = "profile-tabs") {
                        InstagramProfileTabBar(
                            selectedTab = selectedTab,
                            includeSavedTab = true,
                            onTabSelected = { selectedTab = it }
                        )
                        Spacer(Modifier.height(4.dp))
                    }

                    when (selectedTab) {
                        ProfileContentTab.POSTS -> {
                            if (contentState.posts.isEmpty()) {
                                item(key = "empty-posts") {
                                    ProfileEmptyTabCard(
                                        icon = Icons.Default.GridOn,
                                        title = "Henüz gönderin yok",
                                        subtitle = "Düşüncelerini, alıntılarını veya kitap görsellerini toplulukla paylaş.",
                                        actionLabel = if (profile.moderation.canPost) "Gönderi Paylaş" else null,
                                        onActionClick = if (profile.moderation.canPost) onCreatePostClick else null
                                    )
                                }
                            } else {
                                instagramPostsGridItems(
                                    posts = contentState.posts,
                                    keyPrefix = "own-post",
                                    onPostClick = { selectedPostId = it.id }
                                )
                            }
                        }

                        ProfileContentTab.BOOKS -> {
                            if (contentState.writtenBooks.isEmpty()) {
                                item(key = "empty-books") {
                                    ProfileEmptyTabCard(
                                        icon = Icons.Default.MenuBook,
                                        title = "Henüz yazdığın bir kitap yok",
                                        subtitle = "İlk kitabını oluşturup Libra okurlarıyla buluşturabilirsin.",
                                        actionLabel = if (profile.moderation.canPublishBook) "Yeni Kitap Oluştur" else null,
                                        onActionClick = if (profile.moderation.canPublishBook) onCreateBookClick else null
                                    )
                                }
                            } else {
                                instagramBooksGridItems(
                                    books = contentState.writtenBooks,
                                    keyPrefix = "own-book",
                                    showStatusBadge = true,
                                    onBookClick = onManageBook
                                )
                            }
                        }

                        ProfileContentTab.SAVED -> {
                            item(key = "saved-filter") {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 8.dp),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    SavedFilterChip(
                                        label = "Gönderiler (${contentState.savedPosts.size})",
                                        selected = savedFilter == SavedFilterType.POSTS,
                                        onClick = { savedFilter = SavedFilterType.POSTS },
                                        modifier = Modifier.weight(1f)
                                    )
                                    SavedFilterChip(
                                        label = "Kitaplar (${contentState.savedBooks.size})",
                                        selected = savedFilter == SavedFilterType.BOOKS,
                                        onClick = { savedFilter = SavedFilterType.BOOKS },
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }

                            if (savedFilter == SavedFilterType.POSTS) {
                                if (contentState.savedPosts.isEmpty()) {
                                    item(key = "empty-saved-posts") {
                                        ProfileEmptyTabCard(
                                            icon = Icons.Default.BookmarkBorder,
                                            title = "Kaydedilen gönderin yok",
                                            subtitle = "Ana akışta beğendiğin gönderileri yer imine ekleyerek burada saklayabilirsin."
                                        )
                                    }
                                } else {
                                    instagramPostsGridItems(
                                        posts = contentState.savedPosts,
                                        keyPrefix = "saved-post",
                                        onPostClick = { selectedPostId = it.id }
                                    )
                                }
                            } else {
                                if (contentState.savedBooks.isEmpty()) {
                                    item(key = "empty-saved-books") {
                                        ProfileEmptyTabCard(
                                            icon = Icons.Default.AutoStories,
                                            title = "Kaydedilen kitabın yok",
                                            subtitle = "Kütüphanene eklediğin kitaplar burada görünür.",
                                            actionLabel = "Kütüphaneye Git",
                                            onActionClick = onMyBooksClick
                                        )
                                    }
                                } else {
                                    instagramBooksGridItems(
                                        books = contentState.savedBooks.map { it.book },
                                        keyPrefix = "saved-book",
                                        showStatusBadge = false,
                                        onBookClick = onOpenBook
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        if (showStoryViewer && contentState.activeStories.isNotEmpty()) {
            StoryViewer(
                stories = contentState.activeStories,
                initialIndex = 0,
                currentUserId = profile.uid,
                onDismiss = { showStoryViewer = false },
                onReply = { showStoryViewer = false },
                onLike = onToggleStoryLike,
                onOpenProfile = {
                    showStoryViewer = false
                },
                onShare = { story ->
                    showStoryViewer = false
                    shareContent = SharedContent(
                        type = "story",
                        id = story.id,
                        title = story.text.ifBlank { "Hikâye" }.take(80),
                        text = story.text,
                        authorId = story.authorId,
                        authorName = story.authorName,
                        mediaUrl = story.mediaUrl,
                        url = sharedContentUrl("story", story.id)
                    )
                }
            )
        }
    }

    if (showCreateSheet) {
        ModalBottomSheet(
            onDismissRequest = { showCreateSheet = false },
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 18.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Oluştur",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    modifier = Modifier.padding(bottom = 4.dp)
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                CreateSheetRow(
                    icon = Icons.Default.Edit,
                    title = "Gönderi",
                    subtitle = "Görsel veya yazı paylaş",
                    enabled = profile.moderation.canPost,
                    onClick = {
                        showCreateSheet = false
                        onCreatePostClick()
                    }
                )
                CreateSheetRow(
                    icon = Icons.Default.AutoStories,
                    title = "Hikâye",
                    subtitle = "24 saatlik hikâye ekle",
                    enabled = profile.moderation.canStory,
                    onClick = {
                        showCreateSheet = false
                        onCreateStoryClick()
                    }
                )
                CreateSheetRow(
                    icon = Icons.Default.MenuBook,
                    title = "Kitap Yaz",
                    subtitle = "Yeni bir Libra kitabı başlat",
                    enabled = profile.moderation.canPublishBook,
                    onClick = {
                        showCreateSheet = false
                        onCreateBookClick()
                    }
                )
                Spacer(Modifier.height(10.dp))
            }
        }
    }

    selectedPost?.let { post ->
        ProfilePostDetailDialog(
            post = post,
            isOwnPost = post.authorId == profile.uid,
            onDismiss = { selectedPostId = null },
            onToggleLike = { onTogglePostLike(post) },
            onToggleSave = { onTogglePostSave(post) },
            onShare = {
                selectedPostId = null
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
            onOpenAuthor = onOpenProfile,
            onDelete = if (post.authorId == profile.uid) {
                {
                    selectedPostId = null
                    onDeletePost(post)
                }
            } else null
        )
    }

    socialDialog?.let { type ->
        ProfileSocialListDialog(
            title = if (type == ProfileSocialListType.FOLLOWERS) "Takipçiler" else "Takip Edilenler",
            emptyMessage = if (type == ProfileSocialListType.FOLLOWERS) {
                "Henüz takipçin yok."
            } else {
                "Henüz kimseyi takip etmiyorsun."
            },
            users = if (type == ProfileSocialListType.FOLLOWERS) contentState.followers else contentState.following,
            onDismiss = { socialDialog = null },
            onOpenProfile = onOpenProfile
        )
    }

    shareContent?.let { content ->
        ShareSheet(
            content = content,
            onDismiss = { shareContent = null }
        )
    }

    selectedCosmetic?.let { role ->
        CosmeticRoleReveal(
            role = role,
            onDismiss = { selectedCosmetic = null }
        )
    }
}

@Composable
private fun SavedFilterChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(12.dp)
    Surface(
        modifier = modifier
            .clip(shape)
            .appleBouncy(pressedScale = 0.95f, onClick = onClick),
        shape = shape,
        color = if (selected) MaterialTheme.colorScheme.onBackground else MaterialTheme.colorScheme.surface,
        border = BorderStroke(
            1.dp,
            if (selected) MaterialTheme.colorScheme.onBackground else MaterialTheme.colorScheme.outlineVariant
        )
    ) {
        Box(
            modifier = Modifier.padding(vertical = 8.dp, horizontal = 10.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                color = if (selected) MaterialTheme.colorScheme.background else MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun CreateSheetRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    enabled: Boolean,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(14.dp)
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .appleBouncy(enabled = enabled, pressedScale = 0.96f, onClick = onClick),
        shape = shape,
        color = MaterialTheme.colorScheme.surfaceVariant,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (enabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(22.dp)
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = if (enabled) subtitle else "Bu işlem geçici olarak kısıtlandı",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
