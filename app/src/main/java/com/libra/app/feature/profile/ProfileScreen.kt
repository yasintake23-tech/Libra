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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
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
import com.libra.app.ui.components.AppButton
import com.libra.app.ui.components.AppButtonVariant
import com.libra.app.ui.components.ErrorView
import com.libra.app.ui.components.LoadingView
import com.libra.app.ui.components.SectionHeader
import com.libra.app.ui.components.StatChip
import com.libra.app.ui.components.UserAvatar

@Composable
fun ProfileScreen(uiState: UiState<UserProfile>, onSignOutClick: () -> Unit, onRetry: () -> Unit, modifier: Modifier = Modifier) {
    var showSignOutConfirm by remember { mutableStateOf(false) }
    when (uiState) {
        is UiState.Loading -> LoadingView(message = "Profil bilgileri yükleniyor...")
        is UiState.Error -> ErrorView(error = uiState.error, onRetry = onRetry)
        is UiState.Empty -> LoadingView()
        is UiState.Success -> {
            val profile = uiState.data
            Column(modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                Surface(modifier = Modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.surface, tonalElevation = 2.dp) {
                    Column(modifier = Modifier.fillMaxWidth().padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        UserAvatar(profile.profileImageUrl, profile.initials, size = 80.dp)
                        Spacer(Modifier.height(12.dp))
                        Text(profile.displayName, style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold))
                        Text(profile.handle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        if (profile.bio.isNotBlank()) { Spacer(Modifier.height(8.dp)); Text(profile.bio, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                        Spacer(Modifier.height(18.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                            StatChip("${profile.booksWrittenCount}", "Yazdığım", icon = Icons.Default.Edit)
                            StatChip("${profile.booksReadCount}", "Okunan", icon = Icons.Default.MenuBook)
                            StatChip("${profile.followersCount}", "Takipçi", icon = Icons.Default.People)
                            StatChip("${profile.followingCount}", "Takip", icon = Icons.Default.People)
                        }
                    }
                }
                Spacer(Modifier.height(16.dp))
                SectionHeader(title = "Altyapı")
                Column(modifier = Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    StatusCard("Firebase Auth & Realtime Database", "Kimlik doğrulama ve kullanıcı profilleri gerçek Firebase katmanında.", Icons.Default.Security)
                    StatusCard("Cloudflare R2", "Medya depolama için güvenli presigned-upload abstraction hazır.", Icons.Default.CloudUpload)
                }
                Spacer(Modifier.height(24.dp))
                SectionHeader(title = "Hesap")
                AppButton(text = "Oturumu Kapat", icon = Icons.Default.ExitToApp, onClick = { showSignOutConfirm = true }, variant = AppButtonVariant.OUTLINED, modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp), testTag = "logout_button")
                Spacer(Modifier.height(32.dp))
            }
        }
    }
    if (showSignOutConfirm) AlertDialog(
        onDismissRequest = { showSignOutConfirm = false },
        title = { Text("Çıkış Yap") },
        text = { Text("Hesabınızdan çıkış yapmak istediğinize emin misiniz?") },
        confirmButton = { TextButton(onClick = { showSignOutConfirm = false; onSignOutClick() }) { Text("Çıkış Yap", color = MaterialTheme.colorScheme.error) } },
        dismissButton = { TextButton(onClick = { showSignOutConfirm = false }) { Text("İptal") } }
    )
}

@Composable
private fun StatusCard(title: String, description: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Row(modifier = Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(shape = RoundedCornerShape(8.dp), color = MaterialTheme.colorScheme.primaryContainer, modifier = Modifier.size(40.dp)) {
                Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(9.dp))
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) { Text(title, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)); Text(description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        }
    }
}
