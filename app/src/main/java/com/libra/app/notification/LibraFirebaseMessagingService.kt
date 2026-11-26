package com.libra.app.notification

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.libra.app.core.di.ServiceLocator
import com.libra.app.core.result.AppResult
import com.libra.app.domain.model.AppNotification
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

/**
 * Firebase Cloud Messaging receiver: wakes the app cleanly only when an
 * FCM push arrives, dispatches local tray notifications with inline reply
 * and read actions, and lets Android reclaim the process right afterwards.
 */
class LibraFirebaseMessagingService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        LibraNotificationSyncManager.ensureFirebaseInitialized(applicationContext)
        val uid = runCatching { FirebaseAuth.getInstance().currentUser?.uid }.getOrNull().orEmpty()
        if (uid.isNotBlank() && token.isNotBlank()) {
            CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
                LibraNotificationSyncManager.saveFcmToken(uid, token)
            }
        }
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        LibraNotificationSyncManager.ensureFirebaseInitialized(applicationContext)
        val uid = runCatching { FirebaseAuth.getInstance().currentUser?.uid }.getOrNull().orEmpty()
        if (uid.isBlank() || !LibraNotificationHelper.hasNotificationPermission(applicationContext)) return

        val prefs = NotificationPreferencesStore(applicationContext).readPreferences()
        if (!prefs.masterEnabled) return

        runBlocking(Dispatchers.IO) {
            val res = ServiceLocator.notificationRepository.fetchUnreadNotifications(uid, limit = 50)
            if (res is AppResult.Success && res.data.isNotEmpty()) {
                NotificationSyncCoordinator.processIncomingNotifications(
                    context = applicationContext,
                    uid = uid,
                    notifications = res.data,
                    updateUnreadBadgeCount = false
                )
            } else {
                val data = message.data
                val title = data["title"] ?: message.notification?.title.orEmpty()
                val body = data["body"] ?: message.notification?.body.orEmpty()
                val type = data["type"].orEmpty()
                if (title.isNotBlank() || body.isNotBlank()) {
                    val pushNotif = AppNotification(
                        id = data["notificationId"] ?: (message.messageId ?: "fcm_${System.currentTimeMillis()}"),
                        recipientId = uid,
                        actorId = data["actorId"].orEmpty(),
                        actorName = data["actorName"].orEmpty(),
                        actorUsername = data["actorUsername"].orEmpty(),
                        actorPhotoUrl = data["actorPhotoUrl"].orEmpty(),
                        type = type,
                        title = title,
                        body = body,
                        referenceId = data["referenceId"].orEmpty(),
                        createdAt = System.currentTimeMillis(),
                        read = false
                    )
                    NotificationSyncCoordinator.processIncomingNotifications(
                        context = applicationContext,
                        uid = uid,
                        notifications = listOf(pushNotif),
                        updateUnreadBadgeCount = false
                    )
                }
            }
        }
    }
}
