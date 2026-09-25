package com.libra.app.feature.notifications

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.libra.app.core.di.ServiceLocator
import com.libra.app.core.result.AppResult
import com.libra.app.domain.model.AppNotification
import com.libra.app.ui.components.UserAvatar
import kotlinx.coroutines.launch

@Composable
fun NotificationsScreen(onBack: () -> Unit, onOpenProfile: (String) -> Unit = {}, modifier: Modifier = Modifier) {
    val repo = ServiceLocator.notificationRepository
    val uid = ServiceLocator.authRepository.currentUser.value?.uid.orEmpty()
    val scope = rememberCoroutineScope()
    var notifications by remember { mutableStateOf<List<AppNotification>>(emptyList()) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(uid) {
        if (uid.isNotBlank()) repo.observeNotifications(uid).collect { result ->
            when (result) {
                is AppResult.Success -> notifications = result.data
                is AppResult.Error -> error = result.error.message
            }
        }
    }

    Column(modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Geri") }
            Text("Bildirimler", style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold), modifier = Modifier.weight(1f))
            if (notifications.any { !it.read }) {
                TextButton(onClick = {
                    scope.launch {
                        when (repo.markAllRead(uid)) {
                            is AppResult.Success -> error = null
                            is AppResult.Error -> error = "Bildirimler okunamadı."
                        }
                    }
                }) { Text("Tümünü oku") }
            }
        }

        error?.let { Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(8.dp)) }

        if (notifications.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.Notifications, null, modifier = Modifier.size(48.dp))
                    Spacer(Modifier.height(10.dp))
                    Text("Henüz bildirimin yok.", fontWeight = FontWeight.Bold)
                    Text("Takip ve mesaj bildirimlerin burada görünecek.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), contentPadding = PaddingValues(bottom = 20.dp)) {
                items(notifications, key = { it.id }) { notification ->
                    NotificationRow(notification, onOpenProfile) {
                        if (!notification.read) scope.launch { repo.markRead(notification.id) }
                    }
                }
            }
        }
    }
}

@Composable
private fun NotificationRow(notification: AppNotification, onOpenProfile: (String) -> Unit = {}, onClick: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = MaterialTheme.shapes.medium,
        color = if (notification.read) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.primaryContainer
    ) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            UserAvatar(notification.actorPhotoUrl, notification.actorName.take(1).uppercase(), size = 46.dp, onClick = { onOpenProfile(notification.actorId) })
            Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                Text(notification.title, fontWeight = FontWeight.Bold)
                Text(notification.body, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (!notification.read) {
                Box(Modifier.size(9.dp), contentAlignment = Alignment.Center) {
                    Surface(Modifier.size(9.dp), shape = CircleShape, color = MaterialTheme.colorScheme.primary) {}
                }
            }
        }
    }
}
