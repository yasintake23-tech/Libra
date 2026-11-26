package com.libra.app.feature.notifications

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.libra.app.core.di.ServiceLocator
import com.libra.app.core.result.AppResult
import com.libra.app.domain.model.AppNotification
import com.libra.app.notification.LibraNotificationHelper
import com.libra.app.notification.LibraNotificationSyncManager
import com.libra.app.notification.NotificationPreferenceCategory
import com.libra.app.notification.NotificationPreferencesBottomSheet
import com.libra.app.notification.NotificationPreferencesStore
import com.libra.app.notification.classifyNotificationCategory
import com.libra.app.notification.resolveNotificationIconAndColor
import com.libra.app.ui.animation.appleBouncy
import com.libra.app.ui.components.UserAvatar
import com.libra.app.ui.components.formatCommentRelativeTime
import kotlinx.coroutines.launch

private enum class NotificationFilterTab(val label: String) {
    ALL("Tümü"),
    UNREAD("Okunmamış"),
    MESSAGES("Mesajlar"),
    SOCIAL("Etkileşimler"),
    ANNOUNCEMENTS("Duyurular")
}

@Composable
fun NotificationsScreen(
    onBack: () -> Unit,
    onOpenProfile: (String) -> Unit = {},
    onOpenNotification: (AppNotification) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val repo = ServiceLocator.notificationRepository
    val uid = ServiceLocator.authRepository.currentUser.value?.uid.orEmpty()
    val scope = rememberCoroutineScope()
    val prefStore = remember(context) { NotificationPreferencesStore(context) }

    var notifications by remember { mutableStateOf<List<AppNotification>>(emptyList()) }
    var error by remember { mutableStateOf<String?>(null) }
    var selectedFilter by remember { mutableStateOf(NotificationFilterTab.ALL) }
    var showPrefsSheet by remember { mutableStateOf(false) }
    var prefsState by remember { mutableStateOf(prefStore.readPreferences()) }
    var hasSystemPerm by remember { mutableStateOf(LibraNotificationHelper.hasNotificationPermission(context)) }

    val permLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        prefStore.markPermissionPrompted()
        hasSystemPerm = granted || LibraNotificationHelper.hasNotificationPermission(context)
        if (hasSystemPerm) {
            LibraNotificationSyncManager.syncRegistrationState(context, uid, allowStartForegroundService = true)
        }
        if (!granted && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            LibraNotificationHelper.openAppNotificationSettings(context)
        }
    }

    LaunchedEffect(uid) {
        if (uid.isNotBlank()) {
            repo.observeNotifications(uid, limit = 100).collect { result ->
                when (result) {
                    is AppResult.Success -> {
                        notifications = result.data
                        error = null
                    }
                    is AppResult.Error -> error = result.error.message
                }
            }
        }
    }

    val visibleNotifications = remember(notifications, selectedFilter) {
        notifications.filter { item ->
            when (selectedFilter) {
                NotificationFilterTab.ALL -> true
                NotificationFilterTab.UNREAD -> !item.read
                NotificationFilterTab.MESSAGES ->
                    classifyNotificationCategory(item.type) == NotificationPreferenceCategory.MESSAGES
                NotificationFilterTab.SOCIAL ->
                    classifyNotificationCategory(item.type) == NotificationPreferenceCategory.SOCIAL
                NotificationFilterTab.ANNOUNCEMENTS ->
                    classifyNotificationCategory(item.type) == NotificationPreferenceCategory.ANNOUNCEMENTS
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(horizontal = 16.dp, vertical = 10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier.appleBouncy(pressedScale = 0.86f, onClick = onBack)
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, "Geri")
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "Bildirimler",
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold)
                )
                val unreadCount = notifications.count { !it.read }
                if (unreadCount > 0) {
                    Text(
                        "$unreadCount okunmamış bildirim",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
            if (notifications.any { !it.read }) {
                TextButton(
                    onClick = {
                        scope.launch {
                            when (repo.markAllRead(uid)) {
                                is AppResult.Success -> error = null
                                is AppResult.Error -> error = "Bildirimler okunamadı."
                            }
                        }
                    }
                ) {
                    Icon(Icons.Default.DoneAll, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Tümünü oku")
                }
            }
            IconButton(
                onClick = {
                    hasSystemPerm = LibraNotificationHelper.hasNotificationPermission(context)
                    prefsState = prefStore.readPreferences()
                    showPrefsSheet = true
                }
            ) {
                Icon(Icons.Default.Tune, contentDescription = "Bildirim tercihleri")
            }
        }

        if (!hasSystemPerm) {
            Spacer(Modifier.height(6.dp))
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.NotificationsActive,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Android bildirimleri kapalı",
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = "Mesaj ve beğenileri anında görmek için izin ver.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                permLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            } else {
                                LibraNotificationHelper.openAppNotificationSettings(context)
                            }
                        },
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("İzin Ver")
                    }
                }
            }
        }

        Spacer(Modifier.height(8.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            NotificationFilterTab.entries.forEach { tab ->
                FilterChip(
                    selected = selectedFilter == tab,
                    onClick = { selectedFilter = tab },
                    label = { Text(tab.label) }
                )
            }
        }

        error?.let {
            Spacer(Modifier.height(6.dp))
            Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
        }

        Spacer(Modifier.height(6.dp))

        if (visibleNotifications.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Surface(
                        modifier = Modifier.size(64.dp),
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Default.Notifications,
                                null,
                                modifier = Modifier.size(30.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    Text(
                        if (selectedFilter == NotificationFilterTab.ALL) {
                            "Henüz bildirimin yok."
                        } else {
                            "Bu kategoride bildirim bulunamadı."
                        },
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Mesaj, beğeni, yorum, takip ve duyuru bildirimlerin burada görünecek.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                items(visibleNotifications, key = { it.id }) { notification ->
                    NotificationRow(notification, onOpenProfile) {
                        if (!notification.read) {
                            scope.launch { repo.markRead(notification.id) }
                        }
                        onOpenNotification(notification)
                    }
                }
            }
        }
    }

    if (showPrefsSheet) {
        NotificationPreferencesBottomSheet(
            preferences = prefsState,
            systemPermissionGranted = hasSystemPerm,
            onPreferencesChanged = { updated ->
                prefsState = updated
                prefStore.savePreferences(updated)
                LibraNotificationSyncManager.syncRegistrationState(context, uid, allowStartForegroundService = true)
            },
            onRequestOrOpenSystemPermission = {
                if (!hasSystemPerm && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    permLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                } else {
                    LibraNotificationHelper.openAppNotificationSettings(context)
                }
            },
            onDismiss = { showPrefsSheet = false }
        )
    }
}

@Composable
private fun NotificationRow(
    notification: AppNotification,
    onOpenProfile: (String) -> Unit = {},
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(16.dp)
    val (badgeIcon, badgeColor) = resolveNotificationIconAndColor(notification.type)
    val containerColor = if (notification.read) {
        MaterialTheme.colorScheme.surface
    } else {
        MaterialTheme.colorScheme.surfaceVariant
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .appleBouncy(pressedScale = 0.97f, onClick = onClick),
        shape = shape,
        color = containerColor,
        border = BorderStroke(
            width = 1.dp,
            color = if (notification.read) {
                MaterialTheme.colorScheme.outlineVariant
            } else {
                MaterialTheme.colorScheme.primary
            }
        )
    ) {
        Row(
            Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(modifier = Modifier.size(48.dp)) {
                UserAvatar(
                    photoUrl = notification.actorPhotoUrl,
                    initials = notification.actorName.take(1).uppercase().ifBlank { "L" },
                    size = 46.dp,
                    onClick = { if (notification.actorId.isNotBlank()) onOpenProfile(notification.actorId) }
                )
                Surface(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .size(19.dp),
                    shape = CircleShape,
                    color = badgeColor,
                    border = BorderStroke(1.5.dp, containerColor)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = badgeIcon,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(10.dp)
                        )
                    }
                }
            }

            Column(
                Modifier
                    .weight(1f)
                    .padding(horizontal = 12.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = notification.title.ifBlank { "Libra Bildirimi" },
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        ),
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    if (notification.createdAt > 0L) {
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = formatCommentRelativeTime(notification.createdAt),
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Text(
                    text = notification.body,
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontSize = 13.sp,
                        lineHeight = 18.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
            }

            if (!notification.read) {
                Surface(
                    modifier = Modifier.size(9.dp),
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primary
                ) {}
            }
        }
    }
}
