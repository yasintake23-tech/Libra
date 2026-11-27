package com.libra.app.feature.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.libra.app.core.di.ServiceLocator
import com.libra.app.core.result.AppResult
import com.libra.app.domain.model.AdminRole
import com.libra.app.domain.model.Book
import com.libra.app.domain.model.BookStatus
import com.libra.app.domain.model.CosmeticRole
import com.libra.app.domain.model.Post
import com.libra.app.domain.model.SharedContent
import com.libra.app.domain.model.Story
import com.libra.app.domain.model.UserProfile
import com.libra.app.feature.home.StoryViewer
import com.libra.app.feature.share.ShareSheet
import com.libra.app.feature.share.sharedContentUrl
import com.libra.app.ui.animation.appleBouncy
import com.libra.app.ui.components.CosmeticRoleReveal
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PublicProfileScreen(
    profile: UserProfile,
    isFollowing: Boolean,
    onBack: () -> Unit,
    onFollow: () -> Unit,
    onMessage: () -> Unit,
    onOpenProfile: (String) -> Unit = {},
    onOpenBook: (Book) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    val currentUserId = ServiceLocator.authRepository.currentUser.value?.uid.orEmpty()

    var currentProfile by remember(profile.uid) { mutableStateOf(profile) }
    var selectedTab by remember(profile.uid) { mutableStateOf(ProfileContentTab.POSTS) }
    var socialDialog by remember { mutableStateOf<ProfileSocialListType?>(null) }
    var followers by remember(profile.uid) { mutableStateOf<List<UserProfile>>(emptyList()) }
    var following by remember(profile.uid) { mutableStateOf<List<UserProfile>>(emptyList()) }
    var posts by remember(profile.uid) { mutableStateOf<List<Post>>(emptyList()) }
    var books by remember(profile.uid) { mutableStateOf<List<Book>>(emptyList()) }
    var activeStories by remember(profile.uid) { mutableStateOf<List<Story>>(emptyList()) }
    var adminRole by remember(profile.uid) { mutableStateOf<AdminRole?>(null) }
    var cosmeticRoles by remember { mutableStateOf<List<CosmeticRole>>(emptyList()) }
    var selectedCosmetic by remember { mutableStateOf<CosmeticRole?>(null) }
    var selectedPostId by remember { mutableStateOf<String?>(null) }
    var showStoryViewer by remember { mutableStateOf(false) }
    var shareContent by remember { mutableStateOf<SharedContent?>(null) }
    var isRefreshing by remember { mutableStateOf(false) }

    suspend fun loadTransientMetadata() {
        (ServiceLocator.userRepository.getUserProfileFresh(profile.uid) as? AppResult.Success)?.data?.let {
            currentProfile = it
        }
        (ServiceLocator.userRepository.getFollowers(profile.uid) as? AppResult.Success)?.let {
            followers = it.data
        }
        (ServiceLocator.userRepository.getFollowing(profile.uid) as? AppResult.Success)?.let {
            following = it.data
        }
        (ServiceLocator.storyRepository.getActiveStories(setOf(profile.uid)) as? AppResult.Success)?.let {
            activeStories = it.data
        }
    }

    LaunchedEffect(profile.uid, isFollowing) {
        loadTransientMetadata()
    }

    LaunchedEffect(profile.uid) {
        launch {
            ServiceLocator.postRepository.observeUserPosts(profile.uid, currentUserId).collect { result ->
                if (result is AppResult.Success) posts = result.data
            }
        }
        launch {
            ServiceLocator.bookRepository.getUserWrittenBooks(profile.uid).collect { result ->
                if (result is AppResult.Success) {
                    books = result.data.filter {
                        it.status == BookStatus.PUBLISHED && !it.visibility.equals("PRIVATE", ignoreCase = true)
                    }
                }
            }
        }
        launch {
            adminRole = (ServiceLocator.adminRepository.getAdminRole(profile.uid) as? AppResult.Success)?.data
        }
        launch {
            (ServiceLocator.adminRepository.observeCosmeticRoles().first() as? AppResult.Success)?.let {
                cosmeticRoles = it.data
            }
        }
    }

    val selectedPost = remember(selectedPostId, posts) {
        selectedPostId?.let { id -> posts.firstOrNull { it.id == id } }
    }

    val booksCount = maxOf(currentProfile.booksWrittenCount, books.size)
    val followersCount = maxOf(currentProfile.followersCount, followers.size)
    val followingCount = maxOf(currentProfile.followingCount, following.size)

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Instagram-style Top App Bar: Back + @username + Share (NO settings button)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier.appleBouncy(pressedScale = 0.86f, onClick = onBack)
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Geri")
                }

                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = currentProfile.handle,
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (adminRole != null) {
                        Icon(
                            imageVector = Icons.Default.Verified,
                            contentDescription = "Yetkili hesap",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                IconButton(
                    onClick = {
                        shareContent = SharedContent(
                            type = "profile",
                            id = currentProfile.uid,
                            title = "${currentProfile.displayName} (${currentProfile.handle})",
                            text = currentProfile.bio.ifBlank { "Libra profilini keşfet!" },
                            authorId = currentProfile.uid,
                            authorName = currentProfile.displayName,
                            mediaUrl = currentProfile.profileImageUrl,
                            url = sharedContentUrl("profile", currentProfile.uid)
                        )
                    },
                    modifier = Modifier.appleBouncy(pressedScale = 0.86f, onClick = {
                        shareContent = SharedContent(
                            type = "profile",
                            id = currentProfile.uid,
                            title = "${currentProfile.displayName} (${currentProfile.handle})",
                            text = currentProfile.bio.ifBlank { "Libra profilini keşfet!" },
                            authorId = currentProfile.uid,
                            authorName = currentProfile.displayName,
                            mediaUrl = currentProfile.profileImageUrl,
                            url = sharedContentUrl("profile", currentProfile.uid)
                        )
                    })
                ) {
                    Icon(Icons.Default.Share, contentDescription = "Profili paylaş")
                }
            }

            PullToRefreshBox(
                isRefreshing = isRefreshing,
                onRefresh = {
                    scope.launch {
                        isRefreshing = true
                        loadTransientMetadata()
                        delay(260)
                        isRefreshing = false
                    }
                },
                modifier = Modifier.fillMaxSize()
            ) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 32.dp)
                ) {
                    item(key = "public-header") {
                        InstagramProfileHeader(
                            profile = currentProfile,
                            postsCount = posts.size,
                            booksCount = booksCount,
                            followersCount = followersCount,
                            followingCount = followingCount,
                            hasActiveStory = activeStories.isNotEmpty(),
                            showAddStoryBadge = false,
                            onAvatarClick = {
                                if (activeStories.isNotEmpty()) showStoryViewer = true
                            },
                            onPostsStatClick = { selectedTab = ProfileContentTab.POSTS },
                            onBooksStatClick = { selectedTab = ProfileContentTab.BOOKS },
                            onFollowersClick = { socialDialog = ProfileSocialListType.FOLLOWERS },
                            onFollowingClick = { socialDialog = ProfileSocialListType.FOLLOWING }
                        )
                    }

                    item(key = "public-bio") {
                        Spacer(Modifier.height(4.dp))
                        InstagramProfileBioSection(
                            profile = currentProfile,
                            booksCount = booksCount,
                            adminRole = adminRole,
                            cosmeticRoles = cosmeticRoles,
                            isOwnProfile = false,
                            onCosmeticClick = { selectedCosmetic = it }
                        )
                    }

                    item(key = "public-actions") {
                        Spacer(Modifier.height(14.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            if (isFollowing) {
                                InstagramProfileSecondaryButton(
                                    text = "Takiptesin",
                                    onClick = onFollow,
                                    leadingIcon = Icons.Default.Person,
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("public_profile_follow_button")
                                )
                            } else {
                                InstagramProfilePrimaryButton(
                                    text = "Takip Et",
                                    onClick = onFollow,
                                    leadingIcon = Icons.Default.PersonAdd,
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("public_profile_follow_button")
                                )
                            }

                            InstagramProfileSecondaryButton(
                                text = "Mesaj Gönder",
                                onClick = onMessage,
                                leadingIcon = Icons.Default.ChatBubbleOutline,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("public_profile_message_button")
                            )
                        }
                        Spacer(Modifier.height(14.dp))
                    }

                    item(key = "public-tabs") {
                        InstagramProfileTabBar(
                            selectedTab = selectedTab,
                            includeSavedTab = false,
                            onTabSelected = { selectedTab = it }
                        )
                        Spacer(Modifier.height(4.dp))
                    }

                    when (selectedTab) {
                        ProfileContentTab.POSTS, ProfileContentTab.SAVED -> {
                            if (posts.isEmpty()) {
                                item(key = "public-empty-posts") {
                                    ProfileEmptyTabCard(
                                        icon = Icons.Default.GridOn,
                                        title = "Henüz gönderi yok",
                                        subtitle = "${currentProfile.displayName} henüz bir gönderi paylaşmamış."
                                    )
                                }
                            } else {
                                instagramPostsGridItems(
                                    posts = posts,
                                    keyPrefix = "public-post",
                                    onPostClick = { selectedPostId = it.id }
                                )
                            }
                        }

                        ProfileContentTab.BOOKS -> {
                            if (books.isEmpty()) {
                                item(key = "public-empty-books") {
                                    ProfileEmptyTabCard(
                                        icon = Icons.Default.MenuBook,
                                        title = "Henüz yayınlanmış kitap yok",
                                        subtitle = "${currentProfile.displayName} henüz herkese açık bir kitap yayınlamamış."
                                    )
                                }
                            } else {
                                instagramBooksGridItems(
                                    books = books,
                                    keyPrefix = "public-book",
                                    showStatusBadge = false,
                                    onBookClick = onOpenBook
                                )
                            }
                        }
                    }
                }
            }
        }

        if (showStoryViewer && activeStories.isNotEmpty()) {
            StoryViewer(
                stories = activeStories,
                initialIndex = 0,
                currentUserId = currentUserId,
                onDismiss = { showStoryViewer = false },
                onReply = {
                    showStoryViewer = false
                    onMessage()
                },
                onLike = { story ->
                    if (currentUserId.isNotBlank()) {
                        scope.launch {
                            ServiceLocator.storyRepository.toggleLike(story.id, currentUserId)
                        }
                    }
                },
                onOpenProfile = { showStoryViewer = false },
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

    selectedPost?.let { post ->
        ProfilePostDetailDialog(
            post = post,
            isOwnPost = post.authorId == currentUserId,
            onDismiss = { selectedPostId = null },
            onToggleLike = {
                if (currentUserId.isNotBlank()) {
                    val newLiked = !post.likedByCurrentUser
                    val newCount = (post.likesCount + if (newLiked) 1 else -1).coerceAtLeast(0)
                    posts = posts.map {
                        if (it.id == post.id) it.copy(likedByCurrentUser = newLiked, likesCount = newCount) else it
                    }
                    scope.launch {
                        ServiceLocator.postRepository.toggleLike(post.id, currentUserId)
                    }
                }
            },
            onToggleSave = {
                if (currentUserId.isNotBlank()) {
                    val newSaved = !post.savedByCurrentUser
                    posts = posts.map {
                        if (it.id == post.id) it.copy(savedByCurrentUser = newSaved) else it
                    }
                    scope.launch {
                        ServiceLocator.postRepository.toggleSave(post.id, currentUserId)
                    }
                }
            },
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
            onOpenAuthor = { uid ->
                if (uid != currentProfile.uid) {
                    onOpenProfile(uid)
                }
            }
        )
    }

    selectedCosmetic?.let { role ->
        CosmeticRoleReveal(
            role = role,
            onDismiss = { selectedCosmetic = null }
        )
    }

    socialDialog?.let { type ->
        ProfileSocialListDialog(
            title = if (type == ProfileSocialListType.FOLLOWERS) "Takipçiler" else "Takip Edilenler",
            emptyMessage = if (type == ProfileSocialListType.FOLLOWERS) {
                "Henüz takipçisi yok."
            } else {
                "Henüz kimseyi takip etmiyor."
            },
            users = if (type == ProfileSocialListType.FOLLOWERS) followers else following,
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
}
