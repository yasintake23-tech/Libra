package com.libra.app.feature.settings

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Brightness6
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.NotificationsNone
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.filled.Warning
import java.util.Locale
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.libra.app.BuildConfig
import com.libra.app.domain.model.UserProfile
import com.libra.app.feature.reader.ReaderPreferences
import com.libra.app.feature.reader.ReaderPreferencesStore
import com.libra.app.feature.reader.ReaderTheme
import com.libra.app.notification.LibraNotificationHelper
import com.libra.app.notification.NotificationPreferencesBottomSheet
import com.libra.app.notification.NotificationPreferencesStore
import com.libra.app.ui.animation.appleBouncy
import com.libra.app.ui.components.UserAvatar
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    profile: UserProfile,
    darkTheme: Boolean,
    onDarkThemeChanged: (Boolean) -> Unit,
    onSignOut: () -> Unit,
    onEditProfile: () -> Unit,
    onBack: () -> Unit,
    hasAdminAccess: Boolean = false,
    onOpenAdminPanel: () -> Unit = {},
    onOpenWriteStudio: () -> Unit = {},
    onOpenLibrary: () -> Unit = {},
    onOpenServers: () -> Unit = {},
    onOpenNotifications: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val readerStore = remember { ReaderPreferencesStore(context) }
    val notificationStore = remember(context) { NotificationPreferencesStore(context) }

    var searchQuery by remember { mutableStateOf("") }
    var readerPrefs by remember { mutableStateOf(readerStore.read()) }
    var notificationPrefs by remember { mutableStateOf(notificationStore.readPreferences()) }
    var hasSystemNotifPerm by remember { mutableStateOf(LibraNotificationHelper.hasNotificationPermission(context)) }

    var showReaderPrefsSheet by remember { mutableStateOf(false) }
    var showNotificationPrefsSheet by remember { mutableStateOf(false) }
    var showAccountStatusDialog by remember { mutableStateOf(false) }
    var showSignOutConfirm by remember { mutableStateOf(false) }

    val notifPermLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        notificationStore.markPermissionPrompted()
        hasSystemNotifPerm = granted || LibraNotificationHelper.hasNotificationPermission(context)
        if (!granted && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            LibraNotificationHelper.openAppNotificationSettings(context)
        }
    }

    val q = searchQuery.trim().lowercase()
    fun matches(vararg keywords: String): Boolean {
        if (q.isEmpty()) return true
        return keywords.any { it.lowercase().contains(q) }
    }

    val showAccountGroup = matches(
        "hesap", "profil", "düzenle", "yazar", "stüdyo", "kitap", "kaydedilen", "kütüphane", "yönetim", "admin"
    )
    val showPreferencesGroup = matches(
        "görünüm", "tema", "koyu", "açık", "bildirim", "okuma", "okuyucu", "yazı", "sepya", "tercih"
    )
    val showCommunityGroup = matches(
        "topluluk", "sunucu", "hesap durumu", "moderasyon", "izin", "güvenlik"
    )
    val showSessionGroup = matches("çıkış", "oturum", "güvenli", "sürüm", "libra")

    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            // Instagram Settings and Activity Top Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier.appleBouncy(pressedScale = 0.86f, onClick = onBack)
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Geri")
                }
                Text(
                    text = "Ayarlar ve hareketler",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Search Bar (Instagram style)
                item(key = "settings-search") {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("settings_search_input"),
                        placeholder = { Text("Ayarlarda ara…") },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(Icons.Default.Close, contentDescription = "Temizle")
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surface,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surface
                        )
                    )
                }

                // 1. Hesabım & Stüdyo
                if (showAccountGroup) {
                    item(key = "group-account") {
                        InstagramSettingsGroup(title = "Hesabım ve Stüdyo") {
                            var first = true
                            if (matches("hesap", "profil", "düzenle", profile.displayName, profile.handle)) {
                                first = false
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .appleBouncy(pressedScale = 0.97f, onClick = onEditProfile)
                                        .padding(horizontal = 14.dp, vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    UserAvatar(
                                        photoUrl = profile.profileImageUrl,
                                        initials = profile.initials,
                                        size = 44.dp
                                    )
                                    Spacer(Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "Profili Düzenle",
                                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = "${profile.displayName.ifBlank { "Profil" }} • ${profile.handle}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    Icon(
                                        Icons.Default.ChevronRight,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            if (matches("yazar", "stüdyo", "kitaplarım", "yaz")) {
                                if (!first) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                                first = false
                                InstagramSettingsRow(
                                    icon = Icons.Default.Edit,
                                    title = "Yazar Stüdyosu",
                                    subtitle = "Kitaplarını yönet, yeni bölüm ekle",
                                    onClick = onOpenWriteStudio
                                )
                            }

                            if (matches("kaydedilen", "kütüphane", "gönderi", "kitap")) {
                                if (!first) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                                first = false
                                InstagramSettingsRow(
                                    icon = Icons.Default.BookmarkBorder,
                                    title = "Kaydedilen Gönderiler ve Kitaplar",
                                    subtitle = "Kütüphane rafların ve okuma listen",
                                    onClick = onOpenLibrary
                                )
                            }

                            if (hasAdminAccess && matches("yönetim", "admin", "panel")) {
                                if (!first) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                                InstagramSettingsRow(
                                    icon = Icons.Default.AdminPanelSettings,
                                    title = "Yönetim Paneli",
                                    subtitle = "Üye, içerik, rol ve güncelleme yönetimi",
                                    iconTint = MaterialTheme.colorScheme.primary,
                                    onClick = onOpenAdminPanel,
                                    modifier = Modifier.testTag("settings_admin_row")
                                )
                            }
                        }
                    }
                }

                // 2. Uygulama & Okuma Tercihleri
                if (showPreferencesGroup) {
                    item(key = "group-preferences") {
                        InstagramSettingsGroup(title = "Uygulama ve Okuma Tercihleri") {
                            var first = true
                            if (matches("görünüm", "tema", "koyu", "açık")) {
                                first = false
                                InstagramSettingsSwitchRow(
                                    icon = Icons.Default.Brightness6,
                                    title = "Koyu tema",
                                    subtitle = if (darkTheme) "Karanlık görünüm etkin" else "Açık görünüm etkin",
                                    checked = darkTheme,
                                    onCheckedChange = onDarkThemeChanged
                                )
                            }

                            if (matches("bildirim", "hareket")) {
                                if (!first) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                                first = false
                                InstagramSettingsRow(
                                    icon = Icons.Default.NotificationsNone,
                                    title = "Bildirim Merkezi",
                                    subtitle = "Son hareketlerini ve yanıtları görüntüle",
                                    onClick = onOpenNotifications
                                )
                            }

                            if (matches("bildirim", "izin", "tercih", "mesaj", "beğeni")) {
                                if (!first) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                                first = false
                                val notifSubtitle = when {
                                    !hasSystemNotifPerm -> "Android bildirim izni kapalı • Açmak için dokun"
                                    !notificationPrefs.masterEnabled -> "Uygulama bildirimleri duraklatıldı"
                                    else -> "Mesaj, beğeni, yorum ve duyuru tercihleri"
                                }
                                InstagramSettingsRow(
                                    icon = Icons.Default.NotificationsActive,
                                    title = "Bildirim İzinleri ve Tercihler",
                                    subtitle = notifSubtitle,
                                    onClick = {
                                        hasSystemNotifPerm = LibraNotificationHelper.hasNotificationPermission(context)
                                        notificationPrefs = notificationStore.readPreferences()
                                        showNotificationPrefsSheet = true
                                    }
                                )
                            }

                            if (matches("okuyucu", "okuma", "yazı", "sepya", "tema", "tercih")) {
                                if (!first) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                                val readerThemeLabel = when (readerPrefs.theme) {
                                    ReaderTheme.LIGHT -> "Açık"
                                    ReaderTheme.DARK -> "Koyu"
                                    ReaderTheme.SEPIA -> "Sepya"
                                }
                                InstagramSettingsRow(
                                    icon = Icons.Default.MenuBook,
                                    title = "Okuyucu Tercihleri",
                                    subtitle = "$readerThemeLabel tema • ${readerPrefs.fontSizeSp.roundToInt()}sp yazı",
                                    onClick = { showReaderPrefsSheet = true },
                                    modifier = Modifier.testTag("settings_reader_prefs_row")
                                )
                            }
                        }
                    }
                }

                // 3. Topluluk & Güvenlik
                if (showCommunityGroup) {
                    item(key = "group-community") {
                        InstagramSettingsGroup(title = "Topluluk ve Güvenlik") {
                            var first = true
                            if (matches("sunucu", "topluluk", "sohbet")) {
                                first = false
                                InstagramSettingsRow(
                                    icon = Icons.Default.Groups,
                                    title = "Sunucularım ve Topluluklar",
                                    subtitle = "Katıldığın ve yönettiğin kitap toplulukları",
                                    onClick = onOpenServers
                                )
                            }

                            if (matches("hesap durumu", "moderasyon", "izin", "durum", "güvenlik")) {
                                if (!first) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                                InstagramSettingsRow(
                                    icon = Icons.Default.VerifiedUser,
                                    title = "Hesap Durumu",
                                    subtitle = if (profile.moderation.isBanned) {
                                        "Hesapta kısıtlama var"
                                    } else {
                                        "Tüm topluluk özellikleri açık"
                                    },
                                    onClick = { showAccountStatusDialog = true }
                                )
                            }
                        }
                    }
                }

                // 4. Oturum
                if (showSessionGroup) {
                    item(key = "group-session") {
                        InstagramSettingsGroup(title = "Oturum") {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .appleBouncy(pressedScale = 0.97f, onClick = { showSignOutConfirm = true })
                                    .padding(horizontal = 14.dp, vertical = 14.dp)
                                    .testTag("settings_sign_out_row"),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(22.dp)
                                )
                                Spacer(Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Güvenli Çıkış Yap",
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.error
                                    )
                                    Text(
                                        text = profile.email.ifBlank { profile.handle },
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }

                    item(key = "settings-footer") {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 6.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "Libra • Sürüm ${BuildConfig.VERSION_NAME}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                if (!showAccountGroup && !showPreferencesGroup && !showCommunityGroup && !showSessionGroup) {
                    item(key = "settings-empty-search") {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.surface,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Search,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(32.dp)
                                )
                                Spacer(Modifier.height(8.dp))
                                Text(
                                    text = "Sonuç bulunamadı",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                                )
                                Text(
                                    text = "\"$searchQuery\" ile eşleşen bir ayar yok.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Real Reader Preferences Sheet backed by ReaderPreferencesStore
    if (showReaderPrefsSheet) {
        ModalBottomSheet(
            onDismissRequest = { showReaderPrefsSheet = false },
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = "Okuyucu Tercihleri",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                )

                Text(
                    text = "Sayfa Teması",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ReaderTheme.entries.forEach { themeOption ->
                        val selected = readerPrefs.theme == themeOption
                        val label = when (themeOption) {
                            ReaderTheme.LIGHT -> "Açık"
                            ReaderTheme.DARK -> "Koyu"
                            ReaderTheme.SEPIA -> "Sepya"
                        }
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .appleBouncy(pressedScale = 0.94f, onClick = {
                                    val updated = readerPrefs.copy(theme = themeOption)
                                    readerPrefs = updated
                                    readerStore.save(updated)
                                }),
                            shape = RoundedCornerShape(12.dp),
                            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                            border = BorderStroke(
                                1.dp,
                                if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
                            )
                        ) {
                            Box(
                                modifier = Modifier.padding(vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                                    color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }

                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Yazı Boyutu", style = MaterialTheme.typography.labelLarge)
                        Text(
                            "${readerPrefs.fontSizeSp.roundToInt()} sp",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Slider(
                        value = readerPrefs.fontSizeSp,
                        onValueChange = { value ->
                            val updated = readerPrefs.copy(fontSizeSp = value)
                            readerPrefs = updated
                            readerStore.save(updated)
                        },
                        valueRange = 14f..30f
                    )
                }

                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Satır Aralığı", style = MaterialTheme.typography.labelLarge)
                        Text(
                            String.format(Locale.getDefault(), "%.2fx", readerPrefs.lineSpacing),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Slider(
                        value = readerPrefs.lineSpacing,
                        onValueChange = { value ->
                            val updated = readerPrefs.copy(lineSpacing = value)
                            readerPrefs = updated
                            readerStore.save(updated)
                        },
                        valueRange = 1.15f..2.1f
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Okuma ilerlemesini göster", style = MaterialTheme.typography.bodyLarge)
                        Text(
                            "Bölüm yüzdesini okuyucuda göster",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = readerPrefs.showProgress,
                        onCheckedChange = { checked ->
                            val updated = readerPrefs.copy(showProgress = checked)
                            readerPrefs = updated
                            readerStore.save(updated)
                        }
                    )
                }

                Button(
                    onClick = { showReaderPrefsSheet = false },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text("Tamam")
                }
                Spacer(Modifier.height(6.dp))
            }
        }
    }

    if (showNotificationPrefsSheet) {
        NotificationPreferencesBottomSheet(
            preferences = notificationPrefs,
            systemPermissionGranted = hasSystemNotifPerm,
            onPreferencesChanged = { updated ->
                notificationPrefs = updated
                notificationStore.savePreferences(updated)
            },
            onRequestOrOpenSystemPermission = {
                if (!hasSystemNotifPerm && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    notifPermLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                } else {
                    LibraNotificationHelper.openAppNotificationSettings(context)
                }
            },
            onDismiss = { showNotificationPrefsSheet = false }
        )
    }

    if (showAccountStatusDialog) {
        val m = profile.moderation
        AlertDialog(
            onDismissRequest = { showAccountStatusDialog = false },
            containerColor = MaterialTheme.colorScheme.surface,
            title = { Text("Hesap Durumu", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    AccountCapabilityRow("Hesap Erişimi", !m.isBanned)
                    AccountCapabilityRow("Gönderi Paylaşma", m.canPost)
                    AccountCapabilityRow("Yorum Yapma", m.canComment)
                    AccountCapabilityRow("Hikâye Paylaşma", m.canStory)
                    AccountCapabilityRow("Doğrudan Mesajlaşma", m.canMessage)
                    AccountCapabilityRow("Kitap Yayınlama", m.canPublishBook)
                    AccountCapabilityRow("Topluluk Oluşturma", m.canCreateServer)
                    if (m.isBanned && m.banReason.isNotBlank()) {
                        Text(
                            text = "Not: ${m.banReason}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showAccountStatusDialog = false }) { Text("Kapat") }
            }
        )
    }

    if (showSignOutConfirm) {
        AlertDialog(
            onDismissRequest = { showSignOutConfirm = false },
            containerColor = MaterialTheme.colorScheme.surface,
            title = { Text("Çıkış Yap", fontWeight = FontWeight.Bold) },
            text = {
                Text("Libra hesabından (${profile.handle}) çıkış yapmak istediğine emin misin?")
            },
            confirmButton = {
                Button(
                    onClick = {
                        showSignOutConfirm = false
                        onSignOut()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    )
                ) {
                    Text("Çıkış Yap")
                }
            },
            dismissButton = {
                TextButton(onClick = { showSignOutConfirm = false }) { Text("Vazgeç") }
            }
        )
    }
}

@Composable
private fun AccountCapabilityRow(label: String, allowed: Boolean) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, style = MaterialTheme.typography.bodyMedium)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = if (allowed) Icons.Default.CheckCircle else Icons.Default.Warning,
                contentDescription = null,
                tint = if (allowed) Color(0xFF16A34A) else MaterialTheme.colorScheme.error,
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = if (allowed) "Açık" else "Kısıtlı",
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                color = if (allowed) Color(0xFF16A34A) else MaterialTheme.colorScheme.error
            )
        }
    }
}

@Composable
private fun InstagramSettingsGroup(
    title: String,
    content: @Composable () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 6.dp)
        )
        val shape = RoundedCornerShape(16.dp)
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clip(shape),
            shape = shape,
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                content()
            }
        }
    }
}

@Composable
private fun InstagramSettingsRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    iconTint: Color = MaterialTheme.colorScheme.onSurface
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .appleBouncy(pressedScale = 0.97f, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = iconTint,
            modifier = Modifier.size(22.dp)
        )
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                color = MaterialTheme.colorScheme.onSurface
            )
            if (subtitle.isNotBlank()) {
                Spacer(Modifier.height(1.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun InstagramSettingsSwitchRow(
    icon: ImageVector,
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
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.size(22.dp)
        )
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.height(1.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(Modifier.width(8.dp))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}
