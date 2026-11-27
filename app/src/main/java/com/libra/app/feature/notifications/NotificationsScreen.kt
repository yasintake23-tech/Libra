package com.libra.app.feature.notifications

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
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
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircleOutline
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Tune
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.libra.app.core.di.ServiceLocator
import com.libra.app.core.result.AppResult
import com.libra.app.domain.model.AppNotification
import com.libra.app.feature.messages.rememberLibraCommsColors
import com.libra.app.notification.LibraNotificationRuntimeState
import com.libra.app.notification.LibraNotificationSyncManager
import com.libra.app.notification.NotificationPreferenceCategory
import com.libra.app.notification.NotificationPreferencesBottomSheet
import com.libra.app.notification.NotificationPreferencesStore
import com.libra.app.notification.classifyNotificationCategory
import com.libra.app.notification.resolveNotificationIconAndColor
import com.libra.app.ui.animation.AppleSprings
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationsScreen(
    onBack: () -> Unit,
    onOpenProfile: (String) -> Unit = {},
    onOpenNotification: (AppNotification) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val palette = rememberLibraCommsColors()
    val context = LocalContext.current
    val repo = ServiceLocator.notificationRepository
    val uid = ServiceLocator.authRepository.currentUser.value?.uid.orEmpty()
    val scope = rememberCoroutineScope()
    val prefStore = remember(context) { NotificationPreferencesStore(context) }

    var allRawNotifications by remember { mutableStateOf<List<AppNotification>>(emptyList()) }
    var dismissedIds by remember(uid) { mutableStateOf(prefStore.getDismissedNotificationIds(uid)) }
    var error by remember { mutableStateOf<String?>(null) }
    var selectedFilter by remember { mutableStateOf(NotificationFilterTab.ALL) }
    var showPrefsSheet by remember { mutableStateOf(false) }
    var prefsState by remember { mutableStateOf(prefStore.readPreferences()) }

    var isSelectionMode by remember { mutableStateOf(false) }
    var selectedIds by remember { mutableStateOf<Set<String>>(emptySet()) }
    var actionTarget by remember { mutableStateOf<AppNotification?>(null) }
    var showBatchDeleteConfirm by remember { mutableStateOf(false) }

    BackHandler(enabled = isSelectionMode) {
        isSelectionMode = false
        selectedIds = emptySet()
    }

    LaunchedEffect(uid) {
        if (uid.isNotBlank()) {
            repo.observeNotifications(uid, limit = 120).collect { result ->
                when (result) {
                    is AppResult.Success -> {
                        allRawNotifications = result.data
                        error = null
                    }
                    is AppResult.Error -> error = result.error.message
                }
            }
        }
    }

    val notifications = remember(allRawNotifications, dismissedIds) {
        if (dismissedIds.isEmpty()) allRawNotifications
        else allRawNotifications.filterNot { it.id in dismissedIds }
    }

    val unreadCount = remember(notifications) { notifications.count { !it.read } }

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

    val visibleIds = remember(visibleNotifications) {
        visibleNotifications.mapNotNull { it.id.takeIf(String::isNotBlank) }
    }
    val allVisibleSelected = visibleIds.isNotEmpty() && visibleIds.all { it in selectedIds }

    LaunchedEffect(visibleIds) {
        if (selectedIds.isNotEmpty()) {
            val validSet = visibleIds.toSet()
            val pruned = selectedIds.intersect(validSet)
            if (pruned != selectedIds) {
                selectedIds = pruned
                if (pruned.isEmpty()) isSelectionMode = false
            }
        }
    }

    fun toggleSelection(id: String) {
        if (id.isBlank()) return
        selectedIds = if (id in selectedIds) selectedIds - id else selectedIds + id
    }

    fun performDeleteIds(ids: List<String>) {
        if (ids.isEmpty()) return
        val idSet = ids.toSet()
        prefStore.addDismissedNotificationIds(uid, idSet)
        dismissedIds = dismissedIds + idSet
        allRawNotifications = allRawNotifications.filterNot { it.id in idSet }
        val remainingUnread = notifications.count { !it.read && it.id !in idSet }
        LibraNotificationRuntimeState.updateUnreadCount(remainingUnread)
        selectedIds = selectedIds - idSet
        if (selectedIds.isEmpty()) isSelectionMode = false
        scope.launch {
            repo.deleteNotifications(ids)
        }
        val msg = if (ids.size == 1) "Bildirim silindi." else "${ids.size} bildirim silindi."
        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
    }

    fun performMarkReadIds(ids: List<String>) {
        if (ids.isEmpty()) return
        val idSet = ids.toSet()
        allRawNotifications = allRawNotifications.map { item ->
            if (item.id in idSet) item.copy(read = true) else item
        }
        val remainingUnread = notifications.count { !it.read && it.id !in idSet }
        LibraNotificationRuntimeState.updateUnreadCount(remainingUnread)
        scope.launch {
            repo.markNotificationsRead(ids)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(palette.canvas)
    ) {
        // Clean Editorial Header (transitions smoothly between normal & batch selection mode)
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = if (isSelectionMode) palette.brandSoft else palette.surface
        ) {
            Column {
                AnimatedContent(
                    targetState = isSelectionMode,
                    transitionSpec = { fadeIn() togetherWith fadeOut() },
                    label = "notifications_header_mode"
                ) { selectionActive ->
                    if (selectionActive) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .statusBarsPadding()
                                .padding(horizontal = 8.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = {
                                    isSelectionMode = false
                                    selectedIds = emptySet()
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Seçimi Kapat",
                                    tint = palette.inkPrimary
                                )
                            }
                            Text(
                                text = "${selectedIds.size} seçildi",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = palette.inkPrimary,
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(start = 4.dp)
                            )
                            TextButton(
                                enabled = visibleIds.isNotEmpty(),
                                onClick = {
                                    selectedIds = if (allVisibleSelected) emptySet() else visibleIds.toSet()
                                }
                            ) {
                                Text(
                                    text = if (allVisibleSelected) "Temizle" else "Tümünü Seç",
                                    fontWeight = FontWeight.SemiBold,
                                    color = palette.brandText
                                )
                            }
                            val hasSelectedUnread = visibleNotifications.any { it.id in selectedIds && !it.read }
                            IconButton(
                                enabled = selectedIds.isNotEmpty() && hasSelectedUnread,
                                onClick = {
                                    val unreadSelected = visibleNotifications
                                        .filter { it.id in selectedIds && !it.read }
                                        .map { it.id }
                                    performMarkReadIds(unreadSelected)
                                    isSelectionMode = false
                                    selectedIds = emptySet()
                                    Toast.makeText(context, "Seçilenler okundu işaretlendi.", Toast.LENGTH_SHORT).show()
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.DoneAll,
                                    contentDescription = "Seçilenleri okundu yap",
                                    tint = if (selectedIds.isNotEmpty() && hasSelectedUnread) palette.brandText else palette.inkSecondary.copy(alpha = 0.4f)
                                )
                            }
                            IconButton(
                                enabled = selectedIds.isNotEmpty(),
                                onClick = { showBatchDeleteConfirm = true }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.DeleteOutline,
                                    contentDescription = "Seçilenleri Sil",
                                    tint = if (selectedIds.isNotEmpty()) MaterialTheme.colorScheme.error else palette.inkSecondary.copy(alpha = 0.4f)
                                )
                            }
                        }
                    } else {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .statusBarsPadding()
                                .padding(horizontal = 8.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(onClick = onBack) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Geri",
                                    tint = palette.inkPrimary
                                )
                            }
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(start = 2.dp)
                            ) {
                                Text(
                                    text = "Bildirimler",
                                    style = MaterialTheme.typography.titleLarge.copy(
                                        fontSize = 21.sp,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = (-0.3).sp
                                    ),
                                    color = palette.inkPrimary
                                )
                                Text(
                                    text = if (unreadCount > 0) "$unreadCount okunmamış bildirim" else "Tüm bildirimler güncel",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (unreadCount > 0) palette.brandText else palette.inkSecondary
                                )
                            }
                            if (visibleNotifications.isNotEmpty()) {
                                IconButton(
                                    onClick = {
                                        isSelectionMode = true
                                        selectedIds = emptySet()
                                    }
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Checklist,
                                        contentDescription = "Toplu Seç",
                                        tint = palette.inkSecondary
                                    )
                                }
                            }
                            if (unreadCount > 0) {
                                IconButton(
                                    onClick = {
                                        val ids = notifications.filter { !it.read }.map { it.id }
                                        allRawNotifications = allRawNotifications.map { it.copy(read = true) }
                                        LibraNotificationRuntimeState.updateUnreadCount(0)
                                        scope.launch {
                                            when (repo.markAllRead(uid)) {
                                                is AppResult.Success -> {
                                                    error = null
                                                    Toast.makeText(context, "Tüm bildirimler okundu.", Toast.LENGTH_SHORT).show()
                                                }
                                                is AppResult.Error -> {
                                                    if (ids.isNotEmpty()) repo.markNotificationsRead(ids)
                                                }
                                            }
                                        }
                                    }
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.DoneAll,
                                        contentDescription = "Tümünü okundu yap",
                                        tint = palette.brand
                                    )
                                }
                            }
                            IconButton(
                                onClick = {
                                    prefsState = prefStore.readPreferences()
                                    showPrefsSheet = true
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Tune,
                                    contentDescription = "Bildirim özelleştirmeleri",
                                    tint = palette.inkPrimary
                                )
                            }
                        }
                    }
                }
                HorizontalDivider(thickness = 0.85.dp, color = palette.hairline)
            }
        }

        // Filter Pills (clean, uncropped circular pills matching Messaging Hub)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            NotificationFilterTab.entries.forEach { tab ->
                val countBadge = if (tab == NotificationFilterTab.UNREAD) unreadCount else 0
                NotificationFilterPill(
                    label = tab.label,
                    badgeCount = countBadge,
                    selected = selectedFilter == tab,
                    onClick = { selectedFilter = tab }
                )
            }
        }

        error?.let { err ->
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 8.dp),
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.errorContainer
            ) {
                Text(
                    text = err,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                )
            }
        }

        if (visibleNotifications.isEmpty()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    color = palette.surface,
                    border = BorderStroke(0.85.dp, palette.hairline)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp, vertical = 30.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Surface(
                            modifier = Modifier.size(52.dp),
                            shape = CircleShape,
                            color = palette.brandSoft
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Notifications,
                                    contentDescription = null,
                                    modifier = Modifier.size(24.dp),
                                    tint = palette.brand
                                )
                            }
                        }
                        Spacer(Modifier.height(12.dp))
                        Text(
                            text = if (selectedFilter == NotificationFilterTab.ALL) {
                                "Henüz bildirimin yok"
                            } else {
                                "Bu kategoride bildirim yok"
                            },
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = palette.inkPrimary
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = "Mesaj, beğeni, yorum, takip ve duyuru bildirimlerin burada düzenli biçimde görünür.",
                            style = MaterialTheme.typography.bodySmall,
                            color = palette.inkSecondary
                        )
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 28.dp)
            ) {
                items(visibleNotifications, key = { it.id }) { notification ->
                    val isSelected = notification.id in selectedIds
                    NotificationRow(
                        notification = notification,
                        isSelectionMode = isSelectionMode,
                        isSelected = isSelected,
                        onOpenProfile = onOpenProfile,
                        onToggleSelection = { toggleSelection(notification.id) },
                        onLongClick = {
                            if (isSelectionMode) {
                                toggleSelection(notification.id)
                            } else {
                                actionTarget = notification
                            }
                        },
                        onClick = {
                            if (isSelectionMode) {
                                toggleSelection(notification.id)
                            } else {
                                if (!notification.read) {
                                    performMarkReadIds(listOf(notification.id))
                                }
                                onOpenNotification(notification)
                            }
                        }
                    )
                }
            }
        }
    }

    // Long-press contextual action sheet
    actionTarget?.let { target ->
        NotificationActionMenuSheet(
            notification = target,
            onDismiss = { actionTarget = null },
            onSelectMultiple = {
                actionTarget = null
                isSelectionMode = true
                if (target.id.isNotBlank()) {
                    selectedIds = setOf(target.id)
                }
            },
            onMarkRead = if (!target.read) {
                {
                    performMarkReadIds(listOf(target.id))
                    actionTarget = null
                    Toast.makeText(context, "Okundu olarak işaretlendi.", Toast.LENGTH_SHORT).show()
                }
            } else null,
            onOpenTarget = {
                actionTarget = null
                if (!target.read) performMarkReadIds(listOf(target.id))
                onOpenNotification(target)
            },
            onDelete = {
                actionTarget = null
                performDeleteIds(listOf(target.id))
            }
        )
    }

    if (showBatchDeleteConfirm) {
        val count = selectedIds.size
        AlertDialog(
            onDismissRequest = { showBatchDeleteConfirm = false },
            containerColor = palette.surface,
            title = { Text("Seçilen Bildirimleri Sil ($count)", fontWeight = FontWeight.Bold) },
            text = { Text("Seçili $count bildirim listeden kalıcı olarak silinecek.") },
            confirmButton = {
                Button(
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    onClick = {
                        val toDelete = selectedIds.toList()
                        showBatchDeleteConfirm = false
                        performDeleteIds(toDelete)
                    }
                ) {
                    Text("Sil", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showBatchDeleteConfirm = false }) {
                    Text("Vazgeç")
                }
            }
        )
    }

    if (showPrefsSheet) {
        NotificationPreferencesBottomSheet(
            preferences = prefsState,
            onPreferencesChanged = { updated ->
                prefsState = updated
                prefStore.savePreferences(updated)
                LibraNotificationSyncManager.syncRegistrationState(context, uid, allowStartForegroundService = true)
            },
            onDismiss = { showPrefsSheet = false }
        )
    }
}

@Composable
private fun NotificationFilterPill(
    label: String,
    badgeCount: Int,
    selected: Boolean,
    onClick: () -> Unit
) {
    val palette = rememberLibraCommsColors()
    Surface(
        modifier = Modifier
            .clip(CircleShape)
            .appleBouncy(pressedScale = 0.95f, onClick = onClick),
        shape = CircleShape,
        color = if (selected) palette.brand else palette.surface,
        border = BorderStroke(
            width = 0.9.dp,
            color = if (selected) palette.brand else palette.hairline
        )
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge.copy(
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                    fontSize = 13.sp
                ),
                color = if (selected) Color.White else palette.inkPrimary,
                maxLines = 1
            )
            if (badgeCount > 0) {
                Surface(
                    shape = CircleShape,
                    color = if (selected) Color.White.copy(alpha = 0.24f) else palette.brand
                ) {
                    Box(
                        modifier = Modifier
                            .defaultMinSize(minWidth = 18.dp, minHeight = 18.dp)
                            .padding(horizontal = 5.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (badgeCount > 99) "99+" else badgeCount.toString(),
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp
                            ),
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun NotificationRow(
    notification: AppNotification,
    isSelectionMode: Boolean,
    isSelected: Boolean,
    onOpenProfile: (String) -> Unit,
    onToggleSelection: () -> Unit,
    onLongClick: () -> Unit,
    onClick: () -> Unit
) {
    val palette = rememberLibraCommsColors()
    val shape = RoundedCornerShape(16.dp)
    val (badgeIcon, badgeColor) = resolveNotificationIconAndColor(notification.type)

    val containerColor by animateColorAsState(
        targetValue = when {
            isSelected -> palette.brandSoft
            !notification.read -> palette.surface
            else -> palette.surface
        },
        label = "notif_row_bg"
    )
    val borderColor = when {
        isSelected -> palette.brand
        !notification.read -> palette.brand.copy(alpha = 0.42f)
        else -> palette.hairline
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .appleBouncy(
                pressedScale = 0.98f,
                onClick = onClick,
                onLongClick = onLongClick
            ),
        shape = shape,
        color = containerColor,
        border = BorderStroke(
            width = if (isSelected || !notification.read) 1.dp else 0.85.dp,
            color = borderColor
        )
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 11.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AnimatedVisibility(
                visible = isSelectionMode,
                enter = fadeIn() + scaleIn(initialScale = 0.7f, animationSpec = AppleSprings.snappy()),
                exit = fadeOut() + scaleOut(targetScale = 0.7f)
            ) {
                Surface(
                    modifier = Modifier
                        .padding(end = 10.dp)
                        .size(22.dp)
                        .clip(CircleShape)
                        .clickable { onToggleSelection() },
                    shape = CircleShape,
                    color = if (isSelected) palette.brand else palette.surface,
                    border = BorderStroke(
                        1.5.dp,
                        if (isSelected) palette.brand else palette.inkSecondary.copy(alpha = 0.45f)
                    )
                ) {
                    if (isSelected) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Seçili",
                                tint = Color.White,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }
            }

            Box(modifier = Modifier.size(46.dp)) {
                UserAvatar(
                    photoUrl = notification.actorPhotoUrl,
                    initials = notification.actorName.take(1).uppercase().ifBlank { "L" },
                    size = 44.dp,
                    onClick = if (!isSelectionMode && notification.actorId.isNotBlank()) {
                        { onOpenProfile(notification.actorId) }
                    } else null
                )
                Surface(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .size(18.dp),
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
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 12.dp, end = 6.dp),
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
                            fontSize = 14.5.sp,
                            fontWeight = FontWeight.Bold
                        ),
                        color = palette.inkPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    if (notification.createdAt > 0L) {
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = formatCommentRelativeTime(notification.createdAt),
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 11.5.sp,
                                fontWeight = if (!notification.read) FontWeight.SemiBold else FontWeight.Normal
                            ),
                            color = if (!notification.read) palette.brandText else palette.inkSecondary
                        )
                    }
                }
                Text(
                    text = notification.body,
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontSize = 13.sp,
                        lineHeight = 18.sp
                    ),
                    color = if (!notification.read) palette.inkPrimary.copy(alpha = 0.85f) else palette.inkSecondary,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
            }

            if (!notification.read && !isSelectionMode) {
                Surface(
                    modifier = Modifier.size(8.dp),
                    shape = CircleShape,
                    color = palette.brand
                ) {}
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NotificationActionMenuSheet(
    notification: AppNotification,
    onDismiss: () -> Unit,
    onSelectMultiple: () -> Unit,
    onMarkRead: (() -> Unit)?,
    onOpenTarget: () -> Unit,
    onDelete: () -> Unit
) {
    val palette = rememberLibraCommsColors()
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = palette.canvas
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 18.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Preview header
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                color = palette.surface,
                border = BorderStroke(0.85.dp, palette.hairline)
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    Text(
                        text = notification.title.ifBlank { "Libra Bildirimi" },
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = palette.inkPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (notification.body.isNotBlank()) {
                        Text(
                            text = notification.body,
                            style = MaterialTheme.typography.bodySmall,
                            color = palette.inkSecondary,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            NotificationSheetActionItem(
                icon = Icons.Default.CheckCircleOutline,
                title = "Seç",
                subtitle = "Çoklu bildirim seçimi ve toplu silme başlat",
                tint = palette.brandText,
                iconBg = palette.brandSoft,
                onClick = onSelectMultiple
            )

            if (onMarkRead != null) {
                NotificationSheetActionItem(
                    icon = Icons.Default.DoneAll,
                    title = "Okundu Yap",
                    subtitle = "Bu bildirimi okunmuş olarak işaretle",
                    tint = palette.community,
                    iconBg = palette.communitySoft,
                    onClick = onMarkRead
                )
            } else {
                NotificationSheetActionItem(
                    icon = Icons.AutoMirrored.Filled.OpenInNew,
                    title = "Bildirime Git",
                    subtitle = "İlgili içeriği aç",
                    tint = palette.inkPrimary,
                    iconBg = palette.recessed,
                    onClick = onOpenTarget
                )
            }

            NotificationSheetActionItem(
                icon = Icons.Default.DeleteOutline,
                title = "Sil",
                subtitle = "Bu bildirimi listeden kaldır",
                tint = MaterialTheme.colorScheme.error,
                iconBg = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f),
                onClick = onDelete
            )

            Spacer(Modifier.height(6.dp))
        }
    }
}

@Composable
private fun NotificationSheetActionItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    tint: Color,
    iconBg: Color,
    onClick: () -> Unit
) {
    val palette = rememberLibraCommsColors()
    val shape = RoundedCornerShape(14.dp)
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .clickable(onClick = onClick),
        shape = shape,
        color = palette.surface,
        border = BorderStroke(0.85.dp, palette.hairline)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 11.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = CircleShape,
                color = iconBg,
                modifier = Modifier.size(38.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = tint,
                        modifier = Modifier.size(19.dp)
                    )
                }
            }
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 12.dp)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = tint
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                    color = palette.inkSecondary
                )
            }
        }
    }
}
