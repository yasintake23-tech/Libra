package com.libra.app.feature.profile

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.libra.app.core.di.ServiceLocator
import com.libra.app.domain.model.AdminRole
import com.libra.app.domain.model.Book
import com.libra.app.domain.model.BookStatus
import com.libra.app.domain.model.CosmeticRole
import com.libra.app.domain.model.Post
import com.libra.app.domain.model.UserProfile
import com.libra.app.ui.animation.AppleSprings
import com.libra.app.ui.animation.appleBouncy
import com.libra.app.ui.components.BookCover
import com.libra.app.ui.components.CosmeticRoleBadge
import com.libra.app.ui.components.FullScreenImageViewerDialog
import com.libra.app.ui.components.LibraCommentItem
import com.libra.app.ui.components.LibraCommentsBottomSheet
import com.libra.app.ui.components.UserAvatar
import kotlinx.coroutines.launch

internal enum class ProfileSocialListType { FOLLOWERS, FOLLOWING }

@Composable
internal fun InstagramProfileHeader(
    profile: UserProfile,
    postsCount: Int,
    booksCount: Int,
    followersCount: Int,
    followingCount: Int,
    hasActiveStory: Boolean,
    showAddStoryBadge: Boolean,
    onAvatarClick: () -> Unit,
    onAddStoryClick: () -> Unit = {},
    onPostsStatClick: () -> Unit,
    onBooksStatClick: () -> Unit,
    onFollowersClick: () -> Unit,
    onFollowingClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        ProfileStoryAvatar(
            photoUrl = profile.profileImageUrl,
            initials = profile.initials,
            hasActiveStory = hasActiveStory,
            showAddStoryBadge = showAddStoryBadge,
            onAvatarClick = onAvatarClick,
            onAddStoryClick = onAddStoryClick
        )

        Spacer(Modifier.width(14.dp))

        Row(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            InstagramStatItem(
                value = postsCount,
                label = "Gönderi",
                onClick = onPostsStatClick,
                modifier = Modifier.weight(1f)
            )
            InstagramStatItem(
                value = booksCount,
                label = "Kitap",
                onClick = onBooksStatClick,
                modifier = Modifier.weight(1f)
            )
            InstagramStatItem(
                value = followersCount,
                label = "Takipçi",
                onClick = onFollowersClick,
                modifier = Modifier.weight(1f)
            )
            InstagramStatItem(
                value = followingCount,
                label = "Takip",
                onClick = onFollowingClick,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun ProfileStoryAvatar(
    photoUrl: String,
    initials: String,
    hasActiveStory: Boolean,
    showAddStoryBadge: Boolean,
    onAvatarClick: () -> Unit,
    onAddStoryClick: () -> Unit
) {
    val storyGradient = remember {
        Brush.linearGradient(
            colors = listOf(
                Color(0xFF8B5CF6),
                Color(0xFFEC4899),
                Color(0xFFF59E0B)
            )
        )
    }

    Box(
        modifier = Modifier.size(90.dp),
        contentAlignment = Alignment.Center
    ) {
        val ringModifier = if (hasActiveStory) {
            Modifier
                .size(88.dp)
                .clip(CircleShape)
                .border(2.8.dp, storyGradient, CircleShape)
                .padding(4.dp)
        } else {
            Modifier
                .size(86.dp)
                .clip(CircleShape)
                .border(1.2.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape)
                .padding(3.dp)
        }

        Box(
            modifier = ringModifier
                .clip(CircleShape)
                .appleBouncy(pressedScale = 0.92f, onClick = onAvatarClick),
            contentAlignment = Alignment.Center
        ) {
            UserAvatar(
                photoUrl = photoUrl,
                initials = initials,
                size = 78.dp
            )
        }

        if (showAddStoryBadge) {
            Surface(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .size(26.dp)
                    .clip(CircleShape)
                    .appleBouncy(pressedScale = 0.85f, onClick = onAddStoryClick),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primary,
                border = BorderStroke(2.dp, MaterialTheme.colorScheme.background)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Hikâye ekle",
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun InstagramStatItem(
    value: Int,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .appleBouncy(pressedScale = 0.92f, onClick = onClick)
            .padding(vertical = 6.dp, horizontal = 2.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value.toString(),
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 17.sp
            ),
            maxLines = 1
        )
        Spacer(Modifier.height(1.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.5.sp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun InstagramProfileBioSection(
    profile: UserProfile,
    booksCount: Int,
    adminRole: AdminRole?,
    cosmeticRoles: List<CosmeticRole>,
    isOwnProfile: Boolean,
    onCosmeticClick: (CosmeticRole) -> Unit,
    onEmptyBioClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = profile.displayName.ifBlank { "Libra Kullanıcısı" },
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            adminRole?.let { role ->
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Text(
                        text = role.name,
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp),
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        }

        val assignedCosmetics = cosmeticRoles.filter { it.id in profile.cosmeticRoleIds }
        if (assignedCosmetics.isNotEmpty()) {
            Spacer(Modifier.height(6.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                assignedCosmetics.forEach { role ->
                    CosmeticRoleBadge(role) { onCosmeticClick(role) }
                }
            }
        }

        if (profile.bio.isNotBlank()) {
            Spacer(Modifier.height(8.dp))
            Text(
                text = profile.bio,
                style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 20.sp),
                color = MaterialTheme.colorScheme.onBackground
            )
        } else if (isOwnProfile) {
            Spacer(Modifier.height(6.dp))
            Text(
                text = "Biyografi ekle…",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .appleBouncy(pressedScale = 0.96f, onClick = onEmptyBioClick)
                    .padding(vertical = 2.dp)
            )
        }
    }
}

@Composable
internal fun InstagramProfileSecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    leadingIcon: ImageVector? = null
) {
    val shape = RoundedCornerShape(12.dp)
    Surface(
        modifier = modifier
            .height(38.dp)
            .clip(shape)
            .appleBouncy(pressedScale = 0.94f, onClick = onClick),
        shape = shape,
        color = MaterialTheme.colorScheme.surfaceVariant,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (leadingIcon != null) {
                Icon(
                    imageVector = leadingIcon,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.width(6.dp))
            }
            Text(
                text = text,
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
internal fun InstagramProfilePrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    leadingIcon: ImageVector? = null
) {
    val shape = RoundedCornerShape(12.dp)
    Surface(
        modifier = modifier
            .height(38.dp)
            .clip(shape)
            .appleBouncy(pressedScale = 0.94f, onClick = onClick),
        shape = shape,
        color = MaterialTheme.colorScheme.primary
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (leadingIcon != null) {
                Icon(
                    imageVector = leadingIcon,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.onPrimary
                )
                Spacer(Modifier.width(6.dp))
            }
            Text(
                text = text,
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
internal fun InstagramProfileTabBar(
    selectedTab: ProfileContentTab,
    includeSavedTab: Boolean,
    onTabSelected: (ProfileContentTab) -> Unit,
    modifier: Modifier = Modifier
) {
    val tabs = remember(includeSavedTab) {
        if (includeSavedTab) {
            listOf(ProfileContentTab.POSTS, ProfileContentTab.BOOKS, ProfileContentTab.SAVED)
        } else {
            listOf(ProfileContentTab.POSTS, ProfileContentTab.BOOKS)
        }
    }

    Column(modifier = modifier.fillMaxWidth()) {
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        Row(modifier = Modifier.fillMaxWidth()) {
            tabs.forEach { tab ->
                val selected = selectedTab == tab
                val tint by animateColorAsState(
                    targetValue = if (selected) {
                        MaterialTheme.colorScheme.onBackground
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    animationSpec = AppleSprings.snappy(),
                    label = "profile_tab_tint"
                )
                val icon = when (tab) {
                    ProfileContentTab.POSTS -> Icons.Default.GridOn
                    ProfileContentTab.BOOKS -> Icons.Default.MenuBook
                    ProfileContentTab.SAVED -> Icons.Default.BookmarkBorder
                }
                val label = when (tab) {
                    ProfileContentTab.POSTS -> "Gönderiler"
                    ProfileContentTab.BOOKS -> "Kitaplar"
                    ProfileContentTab.SAVED -> "Kaydedilenler"
                }

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .appleBouncy(pressedScale = 0.94f, onClick = { onTabSelected(tab) })
                        .padding(top = 10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = label,
                            tint = tint,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                            ),
                            color = tint,
                            maxLines = 1
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(2.dp)
                            .background(
                                if (selected) MaterialTheme.colorScheme.onBackground
                                else Color.Transparent
                            )
                    )
                }
            }
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
    }
}

internal fun LazyListScope.instagramPostsGridItems(
    posts: List<Post>,
    keyPrefix: String,
    onPostClick: (Post) -> Unit
) {
    val rows = posts.chunked(3)
    items(rows, key = { row -> "$keyPrefix-${row.joinToString("_") { it.id }}" }) { row ->
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 2.dp, vertical = 1.dp),
            horizontalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            row.forEach { post ->
                InstagramPostGridTile(
                    post = post,
                    onClick = { onPostClick(post) },
                    modifier = Modifier.weight(1f)
                )
            }
            repeat(3 - row.size) {
                Spacer(modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun InstagramPostGridTile(
    post: Post,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(6.dp)
    val hasImage = post.mediaUrl.isNotBlank() &&
        (post.mediaType == "image" || post.mediaType.startsWith("image/") || post.mediaType.isBlank())

    var signedUrl by remember(post.mediaUrl) { mutableStateOf<String?>(null) }
    var imageFailed by remember(post.mediaUrl) { mutableStateOf(false) }

    LaunchedEffect(post.mediaUrl, hasImage) {
        if (hasImage && post.mediaUrl.isNotBlank()) {
            signedUrl = runCatching {
                ServiceLocator.storageRepository.getSignedMediaUrl(post.mediaUrl)
            }.getOrNull()
        } else {
            signedUrl = null
        }
    }

    Surface(
        modifier = modifier
            .aspectRatio(1f)
            .clip(shape)
            .appleBouncy(pressedScale = 0.94f, onClick = onClick),
        shape = shape,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(0.8.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            if (hasImage && !signedUrl.isNullOrBlank() && !imageFailed) {
                AsyncImage(
                    model = signedUrl,
                    contentDescription = post.title.ifBlank { "Gönderi görseli" },
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                    onError = { imageFailed = true }
                )
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(8.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        if (post.title.isNotBlank()) {
                            Text(
                                text = post.title,
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(Modifier.height(3.dp))
                        }
                        Text(
                            text = post.text,
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontSize = 11.sp,
                                lineHeight = 14.sp
                            ),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = if (post.title.isNotBlank()) 3 else 5,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Icon(
                                imageVector = if (post.likedByCurrentUser) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                contentDescription = null,
                                modifier = Modifier.size(11.dp),
                                tint = if (post.likedByCurrentUser) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = post.likesCount.toString(),
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ChatBubbleOutline,
                                contentDescription = null,
                                modifier = Modifier.size(11.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = post.commentsCount.toString(),
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            if (hasImage && !signedUrl.isNullOrBlank() && !imageFailed && post.likesCount > 0) {
                Surface(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(5.dp),
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surface
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Favorite,
                            contentDescription = null,
                            modifier = Modifier.size(10.dp),
                            tint = Color(0xFFE53935)
                        )
                        Text(
                            text = post.likesCount.toString(),
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold
                            ),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    }
}

internal fun LazyListScope.instagramBooksGridItems(
    books: List<Book>,
    keyPrefix: String,
    showStatusBadge: Boolean,
    onBookClick: (Book) -> Unit
) {
    val rows = books.chunked(2)
    items(rows, key = { row -> "$keyPrefix-${row.joinToString("_") { it.id }}" }) { row ->
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            row.forEach { book ->
                ProfileBookGridCard(
                    book = book,
                    showStatusBadge = showStatusBadge,
                    onClick = { onBookClick(book) },
                    modifier = Modifier.weight(1f)
                )
            }
            if (row.size == 1) {
                Spacer(modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun ProfileBookGridCard(
    book: Book,
    showStatusBadge: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(16.dp)
    Surface(
        modifier = modifier
            .clip(shape)
            .appleBouncy(pressedScale = 0.96f, onClick = onClick),
        shape = shape,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            BookCover(
                book = book,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(0.70f),
                cornerRadius = 10
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = book.title.ifBlank { "İsimsiz Kitap" },
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = book.category.displayName,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (showStatusBadge) {
                    val statusLabel = when (book.status) {
                        BookStatus.PUBLISHED -> "Yayında"
                        BookStatus.DRAFT -> "Taslak"
                        BookStatus.ARCHIVED -> "Arşiv"
                    }
                    val statusBg = if (book.status == BookStatus.PUBLISHED) {
                        MaterialTheme.colorScheme.primaryContainer
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant
                    }
                    val statusText = if (book.status == BookStatus.PUBLISHED) {
                        MaterialTheme.colorScheme.onPrimaryContainer
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = statusBg
                    ) {
                        Text(
                            text = statusLabel,
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                            color = statusText,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                } else {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Visibility,
                            contentDescription = null,
                            modifier = Modifier.size(12.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = book.readCount.toString(),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Text(
                    text = "${book.chapterCount.coerceAtLeast(1)} Bölüm",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
internal fun ProfileEmptyTabCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    actionLabel: String? = null,
    onActionClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(18.dp)
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 18.dp),
        shape = shape,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Surface(
                modifier = Modifier.size(56.dp),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surfaceVariant,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        modifier = Modifier.size(26.dp),
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            if (actionLabel != null && onActionClick != null) {
                Spacer(Modifier.height(14.dp))
                Button(
                    onClick = onActionClick,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.appleBouncy(pressedScale = 0.94f, onClick = onActionClick)
                ) {
                    Text(actionLabel, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
internal fun ProfilePostDetailDialog(
    post: Post,
    isOwnPost: Boolean,
    onDismiss: () -> Unit,
    onToggleLike: () -> Unit,
    onToggleSave: () -> Unit,
    onShare: () -> Unit,
    onOpenAuthor: (String) -> Unit,
    onDelete: (() -> Unit)? = null
) {
    var signedMediaUrl by remember(post.mediaUrl) { mutableStateOf<String?>(null) }
    var showFullImage by remember { mutableStateOf(false) }
    var showCommentsSheet by remember(post.id) { mutableStateOf(false) }
    var commentDraft by remember(post.id) { mutableStateOf("") }
    var comments by remember(post.id) { mutableStateOf<List<com.libra.app.domain.model.PostComment>>(emptyList()) }
    var hasLoadedComments by remember(post.id) { mutableStateOf(false) }
    var isLoadingComments by remember(post.id) { mutableStateOf(false) }
    var isSendingComment by remember(post.id) { mutableStateOf(false) }
    var commentError by remember(post.id) { mutableStateOf<String?>(null) }
    var currentUser by remember { mutableStateOf(ServiceLocator.authRepository.currentUser.value) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        ServiceLocator.authRepository.currentUser.collect { user ->
            currentUser = user
        }
    }

    LaunchedEffect(post.mediaUrl) {
        signedMediaUrl = if (post.mediaUrl.isNotBlank()) {
            runCatching {
                ServiceLocator.storageRepository.getSignedMediaUrl(post.mediaUrl)
            }.getOrNull()
        } else {
            null
        }
    }

    LaunchedEffect(showCommentsSheet, post.id) {
        if (showCommentsSheet) {
            isLoadingComments = !hasLoadedComments
            ServiceLocator.postRepository.observeComments(post.id).collect { res ->
                isLoadingComments = false
                when (res) {
                    is com.libra.app.core.result.AppResult.Success -> {
                        hasLoadedComments = true
                        comments = res.data
                        commentError = null
                    }
                    is com.libra.app.core.result.AppResult.Error -> {
                        commentError = res.error.message
                    }
                }
            }
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        val shape = RoundedCornerShape(20.dp)
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .heightIn(max = 640.dp),
            shape = shape,
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    UserAvatar(
                        photoUrl = post.authorPhotoUrl,
                        initials = post.authorName.take(1).uppercase().ifBlank { "L" },
                        size = 40.dp,
                        onClick = {
                            onDismiss()
                            onOpenAuthor(post.authorId)
                        }
                    )
                    Spacer(Modifier.width(10.dp))
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .clickable {
                                onDismiss()
                                onOpenAuthor(post.authorId)
                            }
                    ) {
                        Text(
                            text = post.authorName.ifBlank { "Libra Kullanıcısı" },
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        if (post.authorUsername.isNotBlank()) {
                            Text(
                                text = "@${post.authorUsername}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Kapat")
                    }
                }

                if (!signedMediaUrl.isNullOrBlank()) {
                    Spacer(Modifier.height(12.dp))
                    AsyncImage(
                        model = signedMediaUrl,
                        contentDescription = "Gönderi fotoğrafı",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(240.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .clickable { showFullImage = true }
                    )
                }

                if (post.title.isNotBlank()) {
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = post.title,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Spacer(Modifier.height(8.dp))
                Text(
                    text = post.text,
                    style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 22.sp),
                    color = MaterialTheme.colorScheme.onSurface
                )

                if (post.tags.isNotEmpty()) {
                    Spacer(Modifier.height(10.dp))
                    Text(
                        text = post.tags.joinToString("  "),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Spacer(Modifier.height(14.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Spacer(Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .appleBouncy(pressedScale = 0.90f, onClick = onToggleLike)
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Icon(
                            imageVector = if (post.likedByCurrentUser) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = "Beğen",
                            tint = if (post.likedByCurrentUser) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = post.likesCount.toString(),
                            style = MaterialTheme.typography.labelLarge
                        )
                    }

                    Spacer(Modifier.width(4.dp))

                    val displayCommentsCount = if (hasLoadedComments) comments.size else post.commentsCount
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .appleBouncy(pressedScale = 0.90f, onClick = { showCommentsSheet = true })
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ChatBubbleOutline,
                            contentDescription = "Yorumlar",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = displayCommentsCount.toString(),
                            style = MaterialTheme.typography.labelLarge
                        )
                    }

                    Spacer(Modifier.width(4.dp))

                    IconButton(
                        onClick = onToggleSave,
                        modifier = Modifier.appleBouncy(pressedScale = 0.88f, onClick = onToggleSave)
                    ) {
                        Icon(
                            imageVector = if (post.savedByCurrentUser) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                            contentDescription = "Kaydet",
                            tint = if (post.savedByCurrentUser) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                        )
                    }

                    IconButton(
                        onClick = onShare,
                        modifier = Modifier.appleBouncy(pressedScale = 0.88f, onClick = onShare)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "Paylaş"
                        )
                    }

                    Spacer(Modifier.weight(1f))

                    if (isOwnPost && onDelete != null) {
                        IconButton(
                            onClick = onDelete,
                            modifier = Modifier.appleBouncy(pressedScale = 0.88f, onClick = onDelete)
                        ) {
                            Icon(
                                imageVector = Icons.Default.DeleteOutline,
                                contentDescription = "Sil",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }
        }
    }

    if (showFullImage && !signedMediaUrl.isNullOrBlank()) {
        FullScreenImageViewerDialog(
            mediaUrl = signedMediaUrl!!,
            onDismiss = { showFullImage = false }
        )
    }

    if (showCommentsSheet) {
        val currentUid = currentUser?.uid.orEmpty()
        val mappedComments = remember(comments, post.authorId, currentUid) {
            comments.map { comment ->
                LibraCommentItem(
                    id = comment.id,
                    authorId = comment.authorId,
                    authorName = comment.authorName,
                    authorUsername = comment.authorUsername,
                    authorPhotoUrl = comment.authorPhotoUrl,
                    text = comment.text,
                    parentCommentId = comment.parentCommentId,
                    createdAt = comment.createdAt,
                    isContentAuthor = comment.authorId == post.authorId,
                    canDelete = currentUid.isNotBlank() && comment.authorId == currentUid
                )
            }
        }

        LibraCommentsBottomSheet(
            comments = mappedComments,
            currentUser = currentUser,
            text = commentDraft,
            onTextChanged = { if (it.length <= 500) commentDraft = it },
            onSend = { parentId ->
                val trimmed = commentDraft.trim()
                if (trimmed.isNotEmpty() && currentUid.isNotBlank()) {
                    commentDraft = ""
                    isSendingComment = true
                    commentError = null
                    scope.launch {
                        when (val res = ServiceLocator.postRepository.addComment(post.id, currentUid, trimmed, parentId)) {
                            is com.libra.app.core.result.AppResult.Error -> commentError = res.error.message
                            is com.libra.app.core.result.AppResult.Success -> Unit
                        }
                        isSendingComment = false
                    }
                }
            },
            onDismiss = { showCommentsSheet = false },
            contextTitle = post.title.ifBlank { post.text.take(60) },
            isLoading = isLoadingComments,
            isSending = isSendingComment,
            canComment = currentUser?.moderation?.canComment != false,
            repliesEnabled = true,
            maxChars = 500,
            error = commentError,
            onClearError = { commentError = null },
            onDeleteComment = { item ->
                if (currentUid.isNotBlank()) {
                    scope.launch {
                        when (val res = ServiceLocator.postRepository.deleteComment(post.id, item.id, currentUid)) {
                            is com.libra.app.core.result.AppResult.Error -> commentError = res.error.message
                            is com.libra.app.core.result.AppResult.Success -> Unit
                        }
                    }
                }
            },
            onOpenProfile = { uid ->
                showCommentsSheet = false
                onDismiss()
                onOpenAuthor(uid)
            }
        )
    }
}

@Composable
internal fun ProfileSocialListDialog(
    title: String,
    emptyMessage: String,
    users: List<UserProfile>,
    onDismiss: () -> Unit,
    onOpenProfile: (String) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        title = {
            Text(title, fontWeight = FontWeight.Bold)
        },
        text = {
            if (users.isEmpty()) {
                Text(emptyMessage, color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                LazyColumn(
                    modifier = Modifier.heightIn(max = 360.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(users, key = { it.uid }) { user ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .appleBouncy(pressedScale = 0.97f, onClick = {
                                    onDismiss()
                                    onOpenProfile(user.uid)
                                })
                                .padding(horizontal = 6.dp, vertical = 7.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            UserAvatar(
                                photoUrl = user.profileImageUrl,
                                initials = user.initials,
                                size = 42.dp
                            )
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(start = 10.dp)
                            ) {
                                Text(
                                    text = user.displayName.ifBlank { "Libra Kullanıcısı" },
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = user.handle,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Kapat")
            }
        }
    )
}
