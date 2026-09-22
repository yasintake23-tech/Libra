package com.libra.app.feature.friends

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.libra.app.core.state.UiState
import com.libra.app.domain.model.UserProfile
import com.libra.app.ui.components.UserAvatar

@Composable
fun FriendsScreen(
    uiState: UiState<FriendsState>,
    onSearchChanged: (String) -> Unit,
    onFollowUser: (UserProfile) -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state = (uiState as? UiState.Success)?.data ?: FriendsState()
    Column(modifier = modifier.fillMaxSize().padding(top = 10.dp)) {
        Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)) {
            Text("Keşfet", style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold))
            Text("Kitapları, yazarları ve okurları bul.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        OutlinedTextField(
            value = state.searchQuery,
            onValueChange = onSearchChanged,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            placeholder = { Text("Kitap, yazar veya kullanıcı ara…") },
            leadingIcon = { Icon(Icons.Default.Search, null) },
            trailingIcon = { if (state.searchQuery.isNotEmpty()) IconButton({ onSearchChanged("") }) { Icon(Icons.Default.Close, "Temizle") } },
            shape = RoundedCornerShape(14.dp),
            singleLine = true
        )

        when (uiState) {
            is UiState.Loading -> Text("Aranıyor…", modifier = Modifier.padding(20.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
            is UiState.Error -> Column(modifier = Modifier.fillMaxWidth().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(uiState.error.message, color = MaterialTheme.colorScheme.error)
                TextButton(onClick = onRetry) { Text("Tekrar dene") }
            }
            is UiState.Empty, is UiState.Success -> {
                val users = state.suggestedUsers
                if (users.isEmpty()) {
                    Column(modifier = Modifier.fillMaxWidth().padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.Search, null, modifier = Modifier.size(40.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.size(10.dp))
                        Text(if (state.searchQuery.isBlank()) "Bir arama yap." else "Sonuç bulunamadı.", fontWeight = FontWeight.SemiBold)
                        Text("Kullanıcı adı veya isimle arayabilirsin.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                } else {
                    LazyColumn(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(users, key = { it.uid }) { user ->
                            UserResult(
                                user = user,
                                isFollowing = state.followingIds.contains(user.uid),
                                isActionLoading = state.actionUserIds.contains(user.uid),
                                onFollow = onFollowUser
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun UserResult(
    user: UserProfile,
    isFollowing: Boolean,
    isActionLoading: Boolean,
    onFollow: (UserProfile) -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Row(modifier = Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            UserAvatar(user.profileImageUrl, user.initials, size = 48.dp)
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(user.displayName, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
                Text(user.handle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            TextButton(
                onClick = { onFollow(user) },
                enabled = !isActionLoading
            ) {
                Icon(
                    if (isFollowing) Icons.Default.Person else Icons.Default.PersonAdd,
                    null,
                    modifier = Modifier.size(16.dp)
                )
                Text(if (isActionLoading) "  ..." else if (isFollowing) "  Takipte" else "  Takip")
            }
        }
    }
}
