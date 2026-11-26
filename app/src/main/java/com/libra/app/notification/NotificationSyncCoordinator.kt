package com.libra.app.notification

import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import com.google.firebase.FirebaseApp
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.messaging.FirebaseMessaging
import com.libra.app.domain.model.AppNotification
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.tasks.await

private const val LIBRA_NOTIF_RTDB_URL =
    "https://libra-3bfb9-default-rtdb.europe-west1.firebasedatabase.app"

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

    suspend fun runBackgroundSyncOnce(context: Context): Boolean {
        val appContext = context.applicationContext
        LibraNotificationSyncManager.ensureFirebaseInitialized(appContext)
        val authUser = runCatching { com.google.firebase.auth.FirebaseAuth.getInstance().currentUser }.getOrNull()
        val uid = authUser?.uid.orEmpty()
        val prefs = NotificationPreferencesStore(appContext).readPreferences()
        val hasPerm = LibraNotificationHelper.hasNotificationPermission(appContext)
        if (uid.isBlank() || !prefs.masterEnabled || !hasPerm) return false

        // Warm up auth token and RTDB connection on cold process wakeup
        runCatching { authUser?.getIdToken(false)?.await() }
        val db = runCatching {
            FirebaseDatabase.getInstance(LIBRA_NOTIF_RTDB_URL).also { it.goOnline() }
        }.getOrNull()

        val firestoreUnread = when (
            val res = com.libra.app.core.di.ServiceLocator.notificationRepository
                .fetchUnreadNotifications(uid, limit = 100)
        ) {
            is com.libra.app.core.result.AppResult.Success -> {
                processIncomingNotifications(
                    context = appContext,
                    uid = uid,
                    notifications = res.data,
                    updateUnreadBadgeCount = false
                )
                res.data
            }
            is com.libra.app.core.result.AppResult.Error -> emptyList()
        }

        // RTDB DM/Group fallback in case Firestore notification lagged or network returned partial
        if (db != null) {
            runCatching {
                val convSnap = db.getReference("directConversations/$uid")
                    .orderByChild("updatedAt")
                    .limitToLast(25)
                    .get()
                    .await()
                val clearedSnap = db.getReference("notifications/$uid/chatClearedAt")
                    .get()
                    .await()
                val clearedMap = clearedSnap.children.associate { child ->
                    child.key.orEmpty() to (child.getValue(Long::class.java) ?: 0L)
                }
                val conversations = convSnap.children.mapNotNull { child ->
                    val convId = child.key.orEmpty()
                    val otherUid = child.child("otherUserId").getValue(String::class.java).orEmpty()
                    val isGroup = child.child("isGroup").getValue(Boolean::class.java) ?: false
                    if (convId.isBlank() || (!isGroup && otherUid.isBlank())) return@mapNotNull null
                    com.libra.app.domain.model.DirectConversation(
                        id = convId,
                        otherUserId = otherUid,
                        otherUserName = child.child("otherUserName").getValue(String::class.java).orEmpty(),
                        otherUserUsername = child.child("otherUserUsername").getValue(String::class.java).orEmpty(),
                        otherUserPhotoUrl = child.child("otherUserPhotoUrl").getValue(String::class.java).orEmpty(),
                        lastMessage = child.child("lastMessage").getValue(String::class.java).orEmpty(),
                        updatedAt = child.child("updatedAt").getValue(Long::class.java) ?: 0L,
                        unreadCount = (child.child("unreadCount").getValue(Long::class.java) ?: 0L).toInt(),
                        isGroup = isGroup,
                        groupName = child.child("groupName").getValue(String::class.java).orEmpty(),
                        groupPhotoUrl = child.child("groupPhotoUrl").getValue(String::class.java).orEmpty()
                    )
                }
                val store = NotificationPreferencesStore(appContext)
                val fallback = buildRtdbFallbackDmNotifications(
                    uid = uid,
                    conversations = conversations,
                    existingFirestoreChatNotifications = firestoreUnread,
                    getLastDmNotifiedAt = { convId -> store.getLastDmNotifiedAt(uid, convId) },
                    getClearedAt = { convId -> clearedMap[convId] ?: 0L },
                    preferences = store.readPreferences()
                )
                if (fallback.isNotEmpty()) {
                    processIncomingNotifications(
                        context = appContext,
                        uid = uid,
                        notifications = fallback,
                        updateUnreadBadgeCount = false
                    )
                }
            }
        }
        return true
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
        LibraNotificationHelper.ensureChannels(appContext)
        val prefs = NotificationPreferencesStore(appContext).readPreferences()
        val hasPerm = LibraNotificationHelper.hasNotificationPermission(appContext)

        if (uid.isBlank() || !prefs.masterEnabled || !hasPerm) {
            stopAll(appContext)
            return
        }

        registerFcmTokenIfAvailable(uid)
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

    fun registerFcmTokenIfAvailable(uid: String) {
        if (uid.isBlank()) return
        runCatching {
            FirebaseMessaging.getInstance().token.addOnSuccessListener { token ->
                if (!token.isNullOrBlank()) {
                    CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
                        saveFcmToken(uid, token)
                    }
                }
            }
        }
    }

    suspend fun saveFcmToken(uid: String, token: String) {
        if (uid.isBlank() || token.isBlank()) return
        runCatching {
            FirebaseDatabase.getInstance(LIBRA_NOTIF_RTDB_URL)
                .getReference("notifications/$uid/fcmToken")
                .setValue(token)
                .await()
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
