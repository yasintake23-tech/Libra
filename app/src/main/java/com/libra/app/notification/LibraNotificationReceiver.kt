package com.libra.app.notification

import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.PowerManager
import android.os.SystemClock
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Handles BOOT_COMPLETED, MY_PACKAGE_REPLACED, and Doze-friendly wakeup
 * alarm checks so unread notifications still reach the tray even when the
 * user has completely closed the app or restarted their device.
 */
class LibraNotificationReceiver : BroadcastReceiver() {

    @SuppressLint("WakelockTimeout")
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
        val pm = appContext.getSystemService(Context.POWER_SERVICE) as? PowerManager
        val wakeLock = runCatching {
            pm?.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "libra:notif_wake_sync")?.apply {
                setReferenceCounted(false)
                acquire(15_000L)
            }
        }.getOrNull()

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

                runCatching {
                    NotificationSyncCoordinator.runBackgroundSyncOnce(appContext)
                }
            } finally {
                runCatching {
                    if (wakeLock?.isHeld == true) wakeLock.release()
                }
                runCatching { pendingResult.finish() }
            }
        }
    }

    companion object {
        const val ACTION_ALARM_SYNC = "com.libra.app.ACTION_NOTIFICATION_SYNC"
        private const val REQUEST_CODE_BACKUP_ALARM = 42073
        private const val FAST_WAKE_INTERVAL_MS = 20_000L
        private const val FALLBACK_WAKE_INTERVAL_MS = 45_000L

        fun scheduleBackupAlarm(context: Context) {
            val appContext = context.applicationContext
            val alarmManager = appContext.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
            val pending = createAlarmPendingIntent(appContext)
            val useFastWake = LibraNotificationHelper.canScheduleExactAlarms(appContext) ||
                LibraNotificationHelper.isIgnoringBatteryOptimizations(appContext)
            val intervalMs = if (useFastWake) FAST_WAKE_INTERVAL_MS else FALLBACK_WAKE_INTERVAL_MS
            val triggerAt = SystemClock.elapsedRealtime() + intervalMs

            runCatching {
                val scheduledExact = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                    LibraNotificationHelper.canScheduleExactAlarms(appContext)
                ) {
                    runCatching {
                        alarmManager.setExactAndAllowWhileIdle(
                            AlarmManager.ELAPSED_REALTIME_WAKEUP,
                            triggerAt,
                            pending
                        )
                    }.isSuccess
                } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M &&
                    Build.VERSION.SDK_INT < Build.VERSION_CODES.S
                ) {
                    runCatching {
                        alarmManager.setExactAndAllowWhileIdle(
                            AlarmManager.ELAPSED_REALTIME_WAKEUP,
                            triggerAt,
                            pending
                        )
                    }.isSuccess
                } else {
                    false
                }

                if (!scheduledExact) {
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
