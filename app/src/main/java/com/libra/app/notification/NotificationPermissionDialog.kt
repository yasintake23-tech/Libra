package com.libra.app.notification

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.libra.app.feature.messages.rememberLibraCommsColors

@Composable
fun NotificationPermissionRationaleDialog(
    onAllowClick: () -> Unit,
    onDismiss: () -> Unit
) {
    val palette = rememberLibraCommsColors()
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = palette.surface,
        icon = {
            Surface(
                modifier = Modifier.size(50.dp),
                shape = CircleShape,
                color = palette.brandSoft
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.NotificationsActive,
                        contentDescription = null,
                        tint = palette.brand,
                        modifier = Modifier.size(25.dp)
                    )
                }
            }
        },
        title = {
            Text(
                text = "Bildirimleri Aç",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = palette.inkPrimary,
                textAlign = TextAlign.Center
            )
        },
        text = {
            Text(
                text = "Yeni mesajlar, gönderi ve kitap beğenileri, yorum yanıtları ile yönetim duyurularını anında görebilmek için bildirim izni verebilirsin.",
                style = MaterialTheme.typography.bodyMedium,
                color = palette.inkSecondary,
                textAlign = TextAlign.Center
            )
        },
        confirmButton = {
            Button(
                onClick = onAllowClick,
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(containerColor = palette.brand)
            ) {
                Text("İzin Ver", color = Color.White, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Şimdi Değil")
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationPreferencesBottomSheet(
    preferences: NotificationPreferencesState,
    systemPermissionGranted: Boolean = true,
    onPreferencesChanged: (NotificationPreferencesState) -> Unit,
    onRequestOrOpenSystemPermission: () -> Unit = {},
    onDismiss: () -> Unit
) {
    val palette = rememberLibraCommsColors()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = palette.canvas
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .padding(horizontal = 18.dp, vertical = 6.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Bildirim Özelleştirmeleri",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp
                        ),
                        color = palette.inkPrimary
                    )
                    Text(
                        text = "Hangi bildirimleri nasıl almak istediğini seç",
                        style = MaterialTheme.typography.bodySmall,
                        color = palette.inkSecondary
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Kapat",
                        tint = palette.inkSecondary
                    )
                }
            }

            // Section 1: General & Quiet control
            PreferenceSectionCard(title = "GENEL KONTROL") {
                NotificationToggleItem(
                    title = "Tüm Bildirimler",
                    subtitle = "Uygulama içi ve telefon bildirimlerini aç veya kapat",
                    checked = preferences.masterEnabled,
                    onCheckedChange = {
                        onPreferencesChanged(preferences.copy(masterEnabled = it))
                    }
                )
                HorizontalDivider(
                    thickness = 0.75.dp,
                    color = palette.hairline,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
                NotificationToggleItem(
                    title = "Sessiz Mod",
                    subtitle = "Bildirimler ses ve titreşim olmadan sessiz düşer",
                    checked = preferences.silentModeEnabled,
                    enabled = preferences.masterEnabled,
                    onCheckedChange = {
                        onPreferencesChanged(preferences.copy(silentModeEnabled = it))
                    }
                )
            }

            // Section 2: Categories
            PreferenceSectionCard(title = "BİLDİRİM KATEGORİLERİ") {
                NotificationToggleItem(
                    title = "Mesajlar ve Sohbetler",
                    subtitle = "DM, grup sohbetleri ve sunucu bahsedilmeleri",
                    checked = preferences.messagesEnabled,
                    enabled = preferences.masterEnabled,
                    onCheckedChange = {
                        onPreferencesChanged(preferences.copy(messagesEnabled = it))
                    }
                )
                HorizontalDivider(
                    thickness = 0.75.dp,
                    color = palette.hairline,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
                NotificationToggleItem(
                    title = "Beğeni, Yorum ve Takipler",
                    subtitle = "Gönderi, kitap ve hikâye etkileşimleri",
                    checked = preferences.socialEnabled,
                    enabled = preferences.masterEnabled,
                    onCheckedChange = {
                        onPreferencesChanged(preferences.copy(socialEnabled = it))
                    }
                )
                HorizontalDivider(
                    thickness = 0.75.dp,
                    color = palette.hairline,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
                NotificationToggleItem(
                    title = "Duyurular ve Yönetim",
                    subtitle = "Libra genel ve özel duyuruları",
                    checked = preferences.announcementsEnabled,
                    enabled = preferences.masterEnabled,
                    onCheckedChange = {
                        onPreferencesChanged(preferences.copy(announcementsEnabled = it))
                    }
                )
            }

            // Section 3: Display & Privacy customizations
            PreferenceSectionCard(title = "GÖRÜNÜM VE GİZLİLİK") {
                NotificationToggleItem(
                    title = "İçerik Önizlemesini Göster",
                    subtitle = "Kapalıyken kilit ekranı ve bantta mesaj içeriği gizlenir",
                    checked = preferences.showPreviewEnabled,
                    enabled = preferences.masterEnabled,
                    onCheckedChange = {
                        onPreferencesChanged(preferences.copy(showPreviewEnabled = it))
                    }
                )
                HorizontalDivider(
                    thickness = 0.75.dp,
                    color = palette.hairline,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
                NotificationToggleItem(
                    title = "Uygulama İçi Bildirim Bandı",
                    subtitle = "Uygulama içindeyken üstten açılan anlık bildirim bandı",
                    checked = preferences.inAppBannerEnabled,
                    enabled = preferences.masterEnabled,
                    onCheckedChange = {
                        onPreferencesChanged(preferences.copy(inAppBannerEnabled = it))
                    }
                )
                HorizontalDivider(
                    thickness = 0.75.dp,
                    color = palette.hairline,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
                NotificationToggleItem(
                    title = "Sohbet Bildirimlerini Grupla",
                    subtitle = "Aynı kişiden gelen mesajları tek bildirimde toplar",
                    checked = preferences.groupChatNotifications,
                    enabled = preferences.masterEnabled,
                    onCheckedChange = {
                        onPreferencesChanged(preferences.copy(groupChatNotifications = it))
                    }
                )
            }

            Button(
                onClick = onDismiss,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(containerColor = palette.brand)
            ) {
                Text("Tamam", color = Color.White, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(6.dp))
        }
    }
}

@Composable
private fun PreferenceSectionCard(
    title: String,
    content: @Composable () -> Unit
) {
    val palette = rememberLibraCommsColors()
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.7.sp
            ),
            color = palette.inkSecondary,
            modifier = Modifier.padding(start = 4.dp)
        )
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            color = palette.surface,
            border = BorderStroke(0.85.dp, palette.hairline)
        ) {
            Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)) {
                content()
            }
        }
    }
}

@Composable
private fun NotificationToggleItem(
    title: String,
    subtitle: String,
    checked: Boolean,
    enabled: Boolean = true,
    onCheckedChange: (Boolean) -> Unit
) {
    val palette = rememberLibraCommsColors()
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                color = if (enabled) palette.inkPrimary else palette.inkSecondary.copy(alpha = 0.55f)
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp, lineHeight = 16.sp),
                color = if (enabled) palette.inkSecondary else palette.inkSecondary.copy(alpha = 0.45f)
            )
        }
        Switch(
            checked = checked,
            enabled = enabled,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = palette.brand,
                uncheckedThumbColor = palette.inkSecondary,
                uncheckedTrackColor = palette.recessed,
                uncheckedBorderColor = palette.hairline
            )
        )
    }
}
