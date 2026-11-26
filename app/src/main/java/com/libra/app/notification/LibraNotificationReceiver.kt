package com.libra.app.notification

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.SystemClock
import com.google.firebase.auth.FirebaseAuth
import com.libra.app.core.di.ServiceLocator
import com.libra.app.core.result.AppResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Handles BOOT_COMPLETED, MY_PACKAGE_REPLACED, and Doze-friendly backup
 * alarm checks so unread notifications still reach the tray even when the
 * user has completely closed the app or restarted their device.
 */
class LibraNotificationReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action.orEmpty()
        if (
            action != Intent.ACTION_BOOT_COMPLETED &&
            action != Intent.ACTION_MY_PACKAGE_REPLACED &&
            action != ACTION_ALARM_SYNC
        ) {
            return
        }

        val appContext = context.applicationContext
        val pendingResult = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                LibraNotificationSyncManager.ensureFirebaseInitialized(appContext)
                val uid = runCatching { FirebaseAuth.getInstance().currentUser?.uid }.getOrNull().orEmpty()
                val prefs = NotificationPreferencesStore(appContext).readPreferences()
                val hasPerm = LibraNotificationHelper.hasNotificationPermission(appContext)

                if (uid.isBlank() || !prefs.masterEnabled || !hasPerm) {
                    LibraNotificationJobService.cancelAllSync(appContext)
                    cancelBackupAlarm(appContext)
                    return@launch
                }

                LibraNotificationJobService.schedulePeriodicSync(appContext)
                scheduleBackupAlarm(appContext)

                when (val res = ServiceLocator.notificationRepository.fetchUnreadNotifications(uid, limit = 100)) {
                    is AppResult.Success -> {
                        NotificationSyncCoordinator.processIncomingNotifications(
                            context = appContext,
                            uid = uid,
                            notifications = res.data,
                            updateUnreadBadgeCount = false
                        )
                    }
                    is AppResult.Error -> Unit
                }
            } finally {
                pendingResult.finish()
            }
        }
    }

    companion object {
        const val ACTION_ALARM_SYNC = "com.libra.app.ACTION_NOTIFICATION_SYNC"
        private const val REQUEST_CODE_BACKUP_ALARM = 42073
        private const val BACKUP_INTERVAL_MS = 2L * 60L * 1000L

        fun scheduleBackupAlarm(context: Context) {
            val appContext = context.applicationContext
            val alarmManager = appContext.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
            val pending = createAlarmPendingIntent(appContext)
            val triggerAt = SystemClock.elapsedRealtime() + BACKUP_INTERVAL_MS

            runCatching {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    alarmManager.setAndAllowWhileIdle(
                        AlarmManager.ELAPSED_REALTIME_WAKEUP,
                        triggerAt,
                        pending
                    )
                } else {
                    alarmManager.set(
                        AlarmManager.ELAPSED_REALTIME_WAKEUP,
                        triggerAt,
                        pending
                    )
                }
            }
        }

        fun cancelBackupAlarm(context: Context) {
            val appContext = context.applicationContext
            val alarmManager = appContext.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
            val pending = createAlarmPendingIntent(appContext)
            runCatching { alarmManager.cancel(pending) }
        }

        private fun createAlarmPendingIntent(context: Context): PendingIntent {
            val intent = Intent(context, LibraNotificationReceiver::class.java).apply {
                action = ACTION_ALARM_SYNC
            }
            return PendingIntent.getBroadcast(
                context,
                REQUEST_CODE_BACKUP_ALARM,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        }
    }
}
