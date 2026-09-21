package com.libra.app.feature.friends

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.libra.app.core.state.UiState
import com.libra.app.domain.model.UserProfile
import com.libra.app.ui.components.AppButton
import com.libra.app.ui.components.AppButtonVariant
import com.libra.app.ui.components.ErrorView
import com.libra.app.ui.components.LoadingView
import com.libra.app.ui.components.SectionHeader
import com.libra.app.ui.components.UserAvatar

@Composable
fun FriendsScreen(uiState: UiState<FriendsState>, onSearchChanged: (String) -> Unit, onFollowUser: (UserProfile) -> Unit, onRetry: () -> Unit, modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxSize().testTag("friends_screen")) {
        Column(modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 8.dp)) {
            Text("Okur Topluluğu", style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.onBackground)
            Text("Diğer okurlarla bağlantı kur, kitap önerilerini incele", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        val currentState = (uiState as? UiState.Success)?.data
        OutlinedTextField(value = currentState?.searchQuery ?: "", onValueChange = onSearchChanged, modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp).testTag("friends_search_input"), placeholder = { Text("Okur veya yazar ara...") }, leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) }, trailingIcon = { if (!currentState?.searchQuery.isNullOrEmpty()) IconButton(onClick = { onSearchChanged("") }) { Icon(Icons.Default.Close, contentDescription = "Temizle") } }, shape = RoundedCornerShape(12.dp), singleLine = true)
        when (uiState) {
            is UiState.Loading -> LoadingView(message = "Topluluk yükleniyor...")
            is UiState.Error -> ErrorView(error = uiState.error, onRetry = onRetry)
            is UiState.Empty, is UiState.Success -> {
                val suggested = currentState?.suggestedUsers ?: emptyList()
                val filtered = suggested.filter { currentState?.searchQuery.isNullOrBlank() || it.displayName.contains(currentState!!.searchQuery, true) || it.username.contains(currentState.searchQuery, true) }
                LazyColumn(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    item { SectionHeader(title = "Önerilen Okurlar & Yazarlar", subtitle = "Arama yaparak okur ve yazarları bulun") }
                    items(filtered) { user -> SuggestedUserCard(user, onFollowClick = { onFollowUser(user) }) }
                    item { Spacer(modifier = Modifier.height(16.dp)); CommunityActivityFeedCard(); Spacer(modifier = Modifier.height(24.dp)) }
                }
            }
        }
    }
}

@Composable
private fun SuggestedUserCard(user: UserProfile, onFollowClick: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth().testTag("user_card_${user.uid}"), shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)) {
        Row(modifier = Modifier.padding(14.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            UserAvatar(photoUrl = user.profileImageUrl, initials = user.initials, size = 46.dp)
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(user.displayName, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                Text(user.handle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (user.bio.isNotBlank()) Text(user.bio, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
            }
            Spacer(modifier = Modifier.width(8.dp))
            AppButton(text = "Takip Et", onClick = onFollowClick, variant = AppButtonVariant.OUTLINED, icon = Icons.Default.PersonAdd, modifier = Modifier.height(38.dp))
        }
    }
}

@Composable
private fun CommunityActivityFeedCard() {
    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Topluluk akışı", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.onSurface)
            Spacer(modifier = Modifier.height(6.dp))
            Text("Arkadaşlık, takip ve aktivite akışı sosyal modüllerle eklenecek.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
