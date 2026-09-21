package com.libra.app.feature.profile

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
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
fun ProfileScreen(
    uiState: UiState<UserProfile>,
    onSignOutClick: () -> Unit,
    onRetry: () -> Unit,
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
            modifier
        )
    }
}

@Composable
private fun ProfileContent(
    profile: UserProfile,
    onSignOut: () -> Unit,
    modifier: Modifier
) {
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
            Icon(
                Icons.Default.Settings,
                contentDescription = "Ayarlar",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
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
            Stat(profile.followingCount.toString(), "Takip")
            Stat(profile.followersCount.toString(), "Takipçi")
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
                ProfileAction(Icons.Default.People, "Takip ettiklerim")
                ProfileAction(Icons.Default.Settings, "Ayarlar")
            }
        }

        Spacer(Modifier.height(8.dp))

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
    label: String
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null)
        Text("  " + label)
    }
}

@Composable
private fun Stat(value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            value,
            style = MaterialTheme.typography.titleLarge.copy(
                fontWeight = FontWeight.Bold
            )
        )
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}