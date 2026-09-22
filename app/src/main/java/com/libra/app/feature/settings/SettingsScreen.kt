package com.libra.app.feature.settings

import android.content.Context
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Brightness6
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.NotificationsNone
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.libra.app.domain.model.UserProfile

@Composable
fun SettingsScreen(
    profile: UserProfile,
    darkTheme: Boolean,
    onDarkThemeChanged: (Boolean) -> Unit,
    onSignOut: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val prefs = remember {
        context.getSharedPreferences("libra_settings", Context.MODE_PRIVATE)
    }

    var notificationsEnabled by remember {
        mutableStateOf(prefs.getBoolean("notifications_enabled", true))
    }
    var onlineStatusEnabled by remember {
        mutableStateOf(prefs.getBoolean("online_status_enabled", true))
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Text(
                "Ayarlar",
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontWeight = FontWeight.Bold
                )
            )
            Text(
                "Libra deneyimini kendine göre düzenle.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        item {
            SettingsSection(
                title = "Hesap",
                icon = Icons.Default.Person
            ) {
                SettingsRow(
                    title = profile.displayName.ifBlank { "Profil" },
                    subtitle = profile.handle + " • " + profile.email,
                    onClick = {}
                )
                HorizontalDivider()
                SettingsRow(
                    title = "Profil bilgileri",
                    subtitle = "Takma isim, kullanıcı adı ve biyografi",
                    onClick = {}
                )
            }
        }

        item {
            SettingsSection(
                title = "Görünüm",
                icon = Icons.Default.Brightness6
            ) {
                SettingsSwitchRow(
                    title = "Koyu tema",
                    subtitle = if (darkTheme) "Şu anda açık" else "Şu anda kapalı",
                    checked = darkTheme,
                    onCheckedChange = onDarkThemeChanged
                )
            }
        }

        item {
            SettingsSection(
                title = "Bildirimler",
                icon = Icons.Default.NotificationsNone
            ) {
                SettingsSwitchRow(
                    title = "Bildirimler",
                    subtitle = "DM, arkadaşlık ve diğer yeni hareketler",
                    checked = notificationsEnabled,
                    onCheckedChange = {
                        notificationsEnabled = it
                        prefs.edit().putBoolean("notifications_enabled", it).apply()
                    }
                )
            }
        }

        item {
            SettingsSection(
                title = "Gizlilik",
                icon = Icons.Default.Lock
            ) {
                SettingsSwitchRow(
                    title = "Çevrimiçi görün",
                    subtitle = "Arkadaşların çevrimiçi durumunu görebilsin",
                    checked = onlineStatusEnabled,
                    onCheckedChange = {
                        onlineStatusEnabled = it
                        prefs.edit().putBoolean("online_status_enabled", it).apply()
                    }
                )
                HorizontalDivider()
                SettingsRow(
                    title = "Gizlilik ayarları",
                    subtitle = "Kim sana ulaşabilir ve arkadaşlık isteği gönderebilir",
                    onClick = {}
                )
            }
        }

        item {
            SettingsSection(
                title = "Diğer",
                icon = Icons.Default.Settings
            ) {
                SettingsRow(
                    title = "Bağlantı ve depolama",
                    subtitle = "Kitap, profil fotoğrafları ve medya altyapısı",
                    onClick = {}
                )
                HorizontalDivider()
                SettingsRow(
                    title = "Libra",
                    subtitle = "Sürüm 1.0.0",
                    onClick = {}
                )
            }
        }

        item {
            TextButton(
                onClick = onSignOut,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Çıkış yap")
            }
        }

        item {
            Spacer(Modifier.height(10.dp))
        }
    }
}

@Composable
private fun SettingsSection(
    title: String,
    icon: ImageVector,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(icon, contentDescription = null)
                Text(
                    title,
                    modifier = Modifier.padding(start = 10.dp),
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.Bold
                    )
                )
            }
            content()
        }
    }
}

@Composable
private fun SettingsRow(
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Spacer(Modifier.height(2.dp))
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Icon(
            Icons.Default.ChevronRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun SettingsSwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Spacer(Modifier.height(2.dp))
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}
