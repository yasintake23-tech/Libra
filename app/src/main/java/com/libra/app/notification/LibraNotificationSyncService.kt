package com.libra.app.notification

import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.ServiceCompat
import com.google.firebase.auth.FirebaseAuth
import com.libra.app.core.di.ServiceLocator
import com.libra.app.core.result.AppResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * Keeps a persistent realtime Firestore notification listener alive while
 * the user is in background or closes the Activity so new DMs, likes,
 * comments, and admin broadcasts arrive without delay.
 */
class LibraNotificationSyncService : Service() {
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var listenerJob: Job? = null
    private var observedUid: String? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForegroundCompat()
        LibraNotificationSyncManager.ensureFirebaseInitialized(applicationContext)

        val uid = runCatching { FirebaseAuth.getInstance().currentUser?.uid }.getOrNull().orEmpty()
        val prefs = NotificationPreferencesStore(applicationContext).readPreferences()
        val hasPerm = LibraNotificationHelper.hasNotificationPermission(applicationContext)

        if (uid.isBlank() || !hasPerm || !prefs.masterEnabled || !prefs.backgroundServiceEnabled) {
            stopSelfSafely()
            return START_NOT_STICKY
        }

        if (observedUid != uid || listenerJob?.isActive != true) {
            observedUid = uid
            listenerJob?.cancel()
            listenerJob = serviceScope.launch {
                ServiceLocator.notificationRepository
                    .observeNotifications(uid, limit = 120)
                    .collect { result ->
                        when (result) {
                            is AppResult.Success -> {
                                NotificationSyncCoordinator.processIncomingNotifications(
                                    context = applicationContext,
                                    uid = uid,
                                    notifications = result.data,
                                    updateUnreadBadgeCount = true
                                )
                            }
                            is AppResult.Error -> Unit
                        }
                    }
            }
        }

        return START_STICKY
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        val uid = runCatching { FirebaseAuth.getInstance().currentUser?.uid }.getOrNull().orEmpty()
        val prefs = NotificationPreferencesStore(applicationContext).readPreferences()
        if (uid.isNotBlank() && prefs.masterEnabled && LibraNotificationHelper.hasNotificationPermission(applicationContext)) {
            LibraNotificationJobService.scheduleImmediateCheck(applicationContext)
            LibraNotificationReceiver.scheduleBackupAlarm(applicationContext)
        }
        super.onTaskRemoved(rootIntent)
    }

    override fun onDestroy() {
        listenerJob?.cancel()
        serviceScope.cancel()
        super.onDestroy()
    }

    private fun startForegroundCompat() {
        val notification = LibraNotificationHelper.buildForegroundSyncNotification(applicationContext)
        val fgType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            ServiceInfo.FOREGROUND_SERVICE_TYPE_REMOTE_MESSAGING
        } else {
            0
        }
        runCatching {
            ServiceCompat.startForeground(
                this,
                LibraNotificationHelper.FOREGROUND_SERVICE_NOTIFICATION_ID,
                notification,
                fgType
            )
        }
    }

    private fun stopSelfSafely() {
        listenerJob?.cancel()
        observedUid = null
        runCatching {
            ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        }
        stopSelf()
    }
}
