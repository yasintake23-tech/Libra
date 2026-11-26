package com.libra.app.notification

import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import com.google.firebase.FirebaseApp
import com.libra.app.domain.model.AppNotification
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

object LibraNotificationRuntimeState {
    @Volatile
    var isAppInForeground: Boolean = false

    @Volatile
    var isNotificationsScreenOpen: Boolean = false

    @Volatile
    var activeDirectConversationId: String? = null

    private val _unreadCount = MutableStateFlow(0)
    val unreadCount = _unreadCount.asStateFlow()

    private val _inAppBannerEvents = MutableSharedFlow<AppNotification>(extraBufferCapacity = 16)
    val inAppBannerEvents = _inAppBannerEvents.asSharedFlow()

    internal fun updateUnreadCount(count: Int) {
        _unreadCount.value = count.coerceAtLeast(0)
    }

    internal fun emitInAppBanner(notification: AppNotification) {
        _inAppBannerEvents.tryEmit(notification)
    }
}

object NotificationSyncCoordinator {
    private val syncMutex = Mutex()

    suspend fun processIncomingNotifications(
        context: Context,
        uid: String,
        notifications: List<AppNotification>,
        updateUnreadBadgeCount: Boolean = false
    ) {
        if (uid.isBlank()) return
        val appContext = context.applicationContext
        syncMutex.withLock {
            if (updateUnreadBadgeCount) {
                LibraNotificationRuntimeState.updateUnreadCount(notifications.count { !it.read })
            }

            val store = NotificationPreferencesStore(appContext)
            val prefs = store.readPreferences()
            val selection = selectNewNotificationsForDelivery(
                notifications = notifications,
                hasBaseline = store.hasBaseline(uid),
                baselineMs = store.getBaselineTimestamp(uid),
                alreadyNotifiedIds = store.getNotifiedIds(uid),
                preferences = prefs
            )

            if (selection.idsToMarkNotified.isNotEmpty() || !store.hasBaseline(uid)) {
                store.recordDeliverySelection(uid, selection)
            }

            val isForeground = LibraNotificationRuntimeState.isAppInForeground
            val notifScreenOpen = isForeground && LibraNotificationRuntimeState.isNotificationsScreenOpen
            val activeConvId = if (isForeground) LibraNotificationRuntimeState.activeDirectConversationId else null

            selection.toDispatch.forEach { freshNotification ->
                val suppress = shouldSuppressActiveNotification(
                    notification = freshNotification,
                    isNotificationsScreenOpen = notifScreenOpen,
                    activeDirectConversationId = activeConvId
                )
                if (!suppress) {
                    LibraNotificationHelper.dispatchSystemNotification(appContext, freshNotification)
                    if (isForeground) {
                        LibraNotificationRuntimeState.emitInAppBanner(freshNotification)
                    }
                }
            }
        }
    }
}

object LibraNotificationSyncManager {
    fun ensureFirebaseInitialized(context: Context) {
        runCatching {
            val appCtx = context.applicationContext
            if (FirebaseApp.getApps(appCtx).isEmpty()) {
                FirebaseApp.initializeApp(appCtx)
            }
        }
    }

    fun syncRegistrationState(
        context: Context,
        uid: String,
        allowStartForegroundService: Boolean
    ) {
        val appContext = context.applicationContext
        ensureFirebaseInitialized(appContext)
        val prefs = NotificationPreferencesStore(appContext).readPreferences()
        val hasPerm = LibraNotificationHelper.hasNotificationPermission(appContext)

        if (uid.isBlank() || !prefs.masterEnabled || !hasPerm) {
            stopAll(appContext)
            return
        }

        LibraNotificationJobService.schedulePeriodicSync(appContext)
        LibraNotificationReceiver.scheduleBackupAlarm(appContext)

        if (prefs.backgroundServiceEnabled) {
            if (allowStartForegroundService) {
                startForegroundSyncService(appContext)
            }
        } else {
            stopForegroundSyncService(appContext)
        }
    }

    fun startForegroundSyncService(context: Context) {
        val appContext = context.applicationContext
        runCatching {
            val intent = Intent(appContext, LibraNotificationSyncService::class.java)
            ContextCompat.startForegroundService(appContext, intent)
        }
    }

    fun stopForegroundSyncService(context: Context) {
        val appContext = context.applicationContext
        runCatching {
            appContext.stopService(Intent(appContext, LibraNotificationSyncService::class.java))
        }
    }

    fun stopAll(context: Context) {
        val appContext = context.applicationContext
        stopForegroundSyncService(appContext)
        LibraNotificationJobService.cancelAllSync(appContext)
        LibraNotificationReceiver.cancelBackupAlarm(appContext)
    }
}
