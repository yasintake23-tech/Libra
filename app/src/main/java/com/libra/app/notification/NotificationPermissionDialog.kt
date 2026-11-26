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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.firebase.auth.FirebaseAuth

@Composable
fun NotificationPermissionRationaleDialog(
    onAllowClick: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        icon = {
            Surface(
                modifier = Modifier.size(52.dp),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.NotificationsActive,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(26.dp)
                    )
                }
            }
        },
        title = {
            Text(
                text = "Bildirimleri Aç",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                textAlign = TextAlign.Center
            )
        },
        text = {
            Text(
                text = "Yeni mesajlar, gönderi ve kitap beğenileri, yorum yanıtları ile yönetim duyurularını anında görebilmek için bildirim izni verebilirsin.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        },
        confirmButton = {
            Button(
                onClick = onAllowClick,
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("İzin Ver")
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
    systemPermissionGranted: Boolean,
    onPreferencesChanged: (NotificationPreferencesState) -> Unit,
    onRequestOrOpenSystemPermission: () -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var ignoringBattery by remember {
        mutableStateOf(LibraNotificationHelper.isIgnoringBatteryOptimizations(context))
    }
    var exactAlarmGranted by remember {
        mutableStateOf(LibraNotificationHelper.canScheduleExactAlarms(context))
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                ignoringBattery = LibraNotificationHelper.isIgnoringBatteryOptimizations(context)
                exactAlarmGranted = LibraNotificationHelper.canScheduleExactAlarms(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Bildirim Tercihleri",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
            )

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Android Bildirim İzni",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = if (systemPermissionGranted) {
                                "Sistem bildirimleri açık"
                            } else {
                                "Kapalı — yeni mesaj ve beğeniler için izin ver"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(Modifier.width(10.dp))
                    OutlinedButton(
                        onClick = onRequestOrOpenSystemPermission,
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(if (systemPermissionGranted) "Ayarlar" else "İzin Ver")
                    }
                }
            }

            if (!ignoringBattery) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Pil Kısıtlaması (Kapalıyken Bildirim)",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                            )
                            Text(
                                text = "Uygulama kapalıyken uyandırma kontrolü için 'Kısıtlanmamış' yap",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(Modifier.width(10.dp))
                        OutlinedButton(
                            onClick = { LibraNotificationHelper.openBatteryOptimizationSettings(context) },
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("Aç")
                        }
                    }
                }
            }

            if (!exactAlarmGranted) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Tam Zamanlı Uyanma İzni",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                            )
                            Text(
                                text = "Kapalıyken gecikmesiz uyanma döngüsü için izin ver",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(Modifier.width(10.dp))
                        OutlinedButton(
                            onClick = { LibraNotificationHelper.openExactAlarmSettings(context) },
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("İzin Ver")
                        }
                    }
                }
            }

            NotificationToggleItem(
                title = "Tüm Bildirimler",
                subtitle = "Uygulama içi ve sistem bildirimlerini yönet",
                checked = preferences.masterEnabled,
                onCheckedChange = {
                    onPreferencesChanged(preferences.copy(masterEnabled = it))
                }
            )
            NotificationToggleItem(
                title = "Anlık Canlı Bağlantı (İsteğe Bağlı)",
                subtitle = "Kapalıyken uygulama yalnızca bildirim kontrolünde kısa süre uyanıp kapanır; kesintisiz anlık iletim için açabilirsin",
                checked = preferences.backgroundServiceEnabled,
                enabled = preferences.masterEnabled,
                onCheckedChange = {
                    onPreferencesChanged(preferences.copy(backgroundServiceEnabled = it))
                }
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            NotificationToggleItem(
                title = "Mesajlar ve Sohbetler",
                subtitle = "DM, grup mesajları ve sunucu etiketleri",
                checked = preferences.messagesEnabled,
                enabled = preferences.masterEnabled,
                onCheckedChange = {
                    onPreferencesChanged(preferences.copy(messagesEnabled = it))
                }
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
            NotificationToggleItem(
                title = "Duyurular ve Yönetim",
                subtitle = "Libra genel ve kategorize yönetim bildirimleri",
                checked = preferences.announcementsEnabled,
                enabled = preferences.masterEnabled,
                onCheckedChange = {
                    onPreferencesChanged(preferences.copy(announcementsEnabled = it))
                }
            )

            OutlinedButton(
                onClick = {
                    if (!LibraNotificationHelper.hasNotificationPermission(context)) {
                        onRequestOrOpenSystemPermission()
                    } else {
                        val uid = FirebaseAuth.getInstance().currentUser?.uid.orEmpty()
                        LibraNotificationHelper.sendTestNotification(context, uid)
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text("Test Bildirimi Gönder (Yanıtla + Okundu İşaretle)")
            }

            Button(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text("Tamam")
            }
            Spacer(Modifier.height(8.dp))
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
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium)
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(Modifier.width(8.dp))
        Switch(
            checked = checked,
            enabled = enabled,
            onCheckedChange = onCheckedChange
        )
    }
}
