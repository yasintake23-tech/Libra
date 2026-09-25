package com.libra.app.feature.profile

import androidx.compose.foundation.clickable
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.libra.app.core.state.UiState
import com.libra.app.domain.model.UserProfile
import com.libra.app.domain.model.Post
import com.libra.app.ui.components.UserAvatar
import com.libra.app.core.di.ServiceLocator
import kotlinx.coroutines.launch

@Composable
fun ProfileScreen(
    uiState: UiState<UserProfile>,
    onSignOutClick: () -> Unit,
    onRetry: () -> Unit,
    onSettingsClick: () -> Unit = {},
    onAdminClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    when (uiState) {
        is UiState.Loading -> Text(
            "Profil yükleniyor…",
            modifier = Modifier.padding(20.dp)
        )
        is UiState.Error -> Column(
            modifier = Modifier.fillMaxWidth().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(uiState.error.message, color = MaterialTheme.colorScheme.error)
            TextButton(onClick = onRetry) { Text("Tekrar dene") }
        }
        is UiState.Empty -> Text(
            "Profil bulunamadı",
            modifier = Modifier.padding(20.dp)
        )
        is UiState.Success -> ProfileContent(
            uiState.data,
            onSignOutClick,
            onSettingsClick,
            onAdminClick,
            modifier
        )
    }
}

@Composable
private fun ProfileContent(
    profile: UserProfile,
    onSignOut: () -> Unit,
    onSettingsClick: () -> Unit,
    onAdminClick: (() -> Unit)?,
    modifier: Modifier
) {
    var socialDialog by remember { mutableStateOf<OwnSocialListType?>(null) }
    var showSavedPosts by remember { mutableStateOf(false) }
    var followers by remember { mutableStateOf<List<UserProfile>>(emptyList()) }
    var following by remember { mutableStateOf<List<UserProfile>>(emptyList()) }

    LaunchedEffect(profile.uid) {
        launch {
            ServiceLocator.userRepository.getFollowers(profile.uid).let {
                if (it is com.libra.app.core.result.AppResult.Success) followers = it.data
            }
        }
        launch {
            ServiceLocator.userRepository.getFollowing(profile.uid).let {
                if (it is com.libra.app.core.result.AppResult.Success) following = it.data
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Profil",
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontWeight = FontWeight.Bold
                )
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (onAdminClick != null) {
                    androidx.compose.material3.Button(
                        onClick = onAdminClick,
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(
                            horizontal = 14.dp,
                            vertical = 8.dp
                        )
                    ) {
                        Icon(
                            Icons.Default.Settings,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            "Yönetim",
                            modifier = Modifier.padding(start = 6.dp),
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
                androidx.compose.material3.IconButton(onClick = onSettingsClick) {
                    Icon(
                        Icons.Default.Settings,
                        contentDescription = "Ayarlar",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Spacer(Modifier.height(24.dp))

        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            UserAvatar(
                profile.profileImageUrl,
                profile.initials,
                size = 84.dp
            )

            Spacer(Modifier.height(10.dp))

            Text(
                profile.displayName,
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold
                )
            )

            Text(
                profile.handle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (profile.bio.isNotBlank()) {
                Spacer(Modifier.height(10.dp))
                Text(
                    profile.bio,
                    style = MaterialTheme.typography.bodyMedium
                )
            } else {
                Spacer(Modifier.height(8.dp))
                Text(
                    "Henüz bir hakkında yazısı yok.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(Modifier.height(24.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            Stat(profile.booksWrittenCount.toString(), "Kitap")
            Stat(following.size.toString(), "Takip", Modifier.clickable { socialDialog = OwnSocialListType.FOLLOWING })
            Stat(followers.size.toString(), "Takipçi", Modifier.clickable { socialDialog = OwnSocialListType.FOLLOWERS })
        }

        Spacer(Modifier.height(24.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Column(modifier = Modifier.padding(4.dp)) {
                ProfileAction(Icons.Default.Book, "Kitaplarım")
                ProfileAction(
                    Icons.Default.BookmarkBorder,
                    "Kaydedilen gönderiler",
                    onClick = { showSavedPosts = true }
                )
                ProfileAction(
                    Icons.Default.People,
                    "Takip ettiklerim",
                    onClick = { socialDialog = OwnSocialListType.FOLLOWING }
                )
                ProfileAction(Icons.Default.Settings, "Ayarlar", onSettingsClick)
            }
        }

        Spacer(Modifier.height(8.dp))

        if (socialDialog != null) {
            SocialListDialog(
                type = socialDialog!!,
                users = if (socialDialog == OwnSocialListType.FOLLOWERS) followers else following,
                onDismiss = { socialDialog = null }
            )
        }

        if (showSavedPosts) {
            SavedPostsDialog(
                userId = profile.uid,
                onDismiss = { showSavedPosts = false }
            )
        }

        TextButton(
            onClick = onSignOut,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.ExitToApp, contentDescription = null)
            Text("  Çıkış yap")
        }
    }
}

@Composable
private fun ProfileAction(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit = {}
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null)
        Text("  " + label)
    }
}

private enum class OwnSocialListType { FOLLOWERS, FOLLOWING }


@Composable
private fun SavedPostsDialog(
    userId: String,
    onDismiss: () -> Unit
) {
    var posts by remember(userId) { mutableStateOf<List<Post>>(emptyList()) }
    var error by remember(userId) { mutableStateOf<String?>(null) }

    LaunchedEffect(userId) {
        ServiceLocator.postRepository.observeSavedPosts(userId).collect { result ->
            when (result) {
                is com.libra.app.core.result.AppResult.Success -> posts = result.data
                is com.libra.app.core.result.AppResult.Error -> error = result.error.message
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Kaydedilen gönderiler") },
        text = {
            when {
                error != null -> Text(error!!, color = MaterialTheme.colorScheme.error)
                posts.isEmpty() -> Text("Henüz kaydettiğin bir gönderi yok.")
                else -> LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(posts, key = { it.id }) { post ->
                        Card(Modifier.fillMaxWidth()) {
                            Column(Modifier.padding(12.dp)) {
                                Text(
                                    post.authorName.ifBlank { post.authorUsername },
                                    fontWeight = FontWeight.Bold
                                )
                                if (post.authorUsername.isNotBlank()) {
                                    Text(
                                        "@${post.authorUsername}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Spacer(Modifier.height(6.dp))
                                Text(post.text)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Kapat") }
        }
    )
}

@Composable
private fun Stat(
    value: String,
    label: String,
    modifier: Modifier = Modifier
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier.padding(horizontal = 12.dp)
    ) {
        Text(
            value,
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
        )
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun SocialListDialog(
    type: OwnSocialListType,
    users: List<UserProfile>,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(if (type == OwnSocialListType.FOLLOWERS) "Takipçiler" else "Takip ettiklerin")
        },
        text = {
            if (users.isEmpty()) {
                Text(
                    if (type == OwnSocialListType.FOLLOWERS)
                        "Henüz takipçin yok."
                    else
                        "Henüz kimseyi takip etmiyorsun."
                )
            } else {
                LazyColumn {
                    items(users, key = { it.uid }) { user ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 7.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            UserAvatar(user.profileImageUrl, user.initials, size = 42.dp)
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(start = 10.dp)
                            ) {
                                Text(user.displayName, fontWeight = FontWeight.SemiBold)
                                Text(
                                    user.handle,
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
            TextButton(onClick = onDismiss) { Text("Kapat") }
        }
    )
}
