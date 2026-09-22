package com.libra.app.feature.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.clickable
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.libra.app.domain.model.UserProfile
import com.libra.app.core.di.ServiceLocator
import com.libra.app.ui.components.UserAvatar

@Composable
fun PublicProfileScreen(
    profile: UserProfile,
    isFollowing: Boolean,
    onBack: () -> Unit,
    onFollow: () -> Unit,
    onMessage: () -> Unit,
    modifier: Modifier = Modifier
) {
    var socialDialog by remember { mutableStateOf<SocialListType?>(null) }
    var followers by remember { mutableStateOf<List<UserProfile>>(emptyList()) }
    var following by remember { mutableStateOf<List<UserProfile>>(emptyList()) }

    LaunchedEffect(profile.uid, isFollowing) {
        ServiceLocator.userRepository.getFollowers(profile.uid).let {
            if (it is com.libra.app.core.result.AppResult.Success) followers = it.data
        }
        ServiceLocator.userRepository.getFollowing(profile.uid).let {
            if (it is com.libra.app.core.result.AppResult.Success) following = it.data
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Geri")
            }
            Text(
                profile.handle,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Profile header is intentionally card-based so the future banner,
            // accent color and badges can slot into this same surface.
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 22.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    UserAvatar(
                        profile.profileImageUrl,
                        profile.initials,
                        size = 112.dp
                    )

                    Spacer(Modifier.height(12.dp))

                    Text(
                        profile.displayName,
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontWeight = FontWeight.Bold
                        )
                    )

                    Text(
                        profile.handle,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    if (profile.bio.isNotBlank()) {
                        Spacer(Modifier.height(10.dp))
                        Text(
                            profile.bio,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }

                    Spacer(Modifier.height(18.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        ProfileStat(profile.booksWrittenCount, "Kitap")
                        ProfileStat(
                            following.size,
                            "Takip",
                            Modifier.clickable { socialDialog = SocialListType.FOLLOWING }
                        )
                        ProfileStat(
                            followers.size,
                            "Takipçi",
                            Modifier.clickable { socialDialog = SocialListType.FOLLOWERS }
                        )
                    }

                    Spacer(Modifier.height(18.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        if (isFollowing) {
                            OutlinedButton(
                                onClick = onFollow,
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.Person, null)
                                Text("  Takipte")
                            }
                        } else {
                            Button(
                                onClick = onFollow,
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.PersonAdd, null)
                                Text("  Takip et")
                            }
                        }

                        OutlinedButton(
                            onClick = onMessage,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.ChatBubbleOutline, null)
                            Text("  Mesaj")
                        }
                    }
                }
            }

            Spacer(Modifier.height(14.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        "Libra'da",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold
                        )
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "Bu profilin kitapları, gönderileri ve sosyal etkinlikleri burada görünecek.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }

    socialDialog?.let { type ->
        SocialListDialog(
            type = type,
            users = if (type == SocialListType.FOLLOWERS) followers else following,
            onDismiss = { socialDialog = null }
        )
    }
}

private enum class SocialListType { FOLLOWERS, FOLLOWING }

@Composable
private fun ProfileStat(value: Int, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            value.toString(),
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
    type: SocialListType,
    users: List<UserProfile>,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(if (type == SocialListType.FOLLOWERS) "Takipçiler" else "Takip ettiklerin")
        },
        text = {
            if (users.isEmpty()) {
                Text(
                    if (type == SocialListType.FOLLOWERS)
                        "Henüz takipçisi yok."
                    else
                        "Henüz kimseyi takip etmiyor."
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
            androidx.compose.material3.TextButton(onClick = onDismiss) {
                Text("Kapat")
            }
        }
    )
}
