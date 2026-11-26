package com.libra.app.notification

import android.annotation.SuppressLint
import android.app.job.JobInfo
import android.app.job.JobParameters
import android.app.job.JobScheduler
import android.app.job.JobService
import android.content.ComponentName
import android.content.Context
import com.google.firebase.auth.FirebaseAuth
import com.libra.app.core.di.ServiceLocator
import com.libra.app.core.result.AppResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

@SuppressLint("SpecifyJobSchedulerIdRange")
class LibraNotificationJobService : JobService() {
    private var serviceScope: CoroutineScope? = null
    private var syncJob: Job? = null

    override fun onStartJob(params: JobParameters?): Boolean {
        LibraNotificationSyncManager.ensureFirebaseInitialized(applicationContext)

        val uid = runCatching { FirebaseAuth.getInstance().currentUser?.uid }.getOrNull().orEmpty()
        if (uid.isBlank() || !LibraNotificationHelper.hasNotificationPermission(this)) {
            return false
        }

        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        serviceScope = scope
        syncJob = scope.launch {
            try {
                val prefs = NotificationPreferencesStore(applicationContext).readPreferences()
                if (!prefs.masterEnabled) return@launch

                when (val res = ServiceLocator.notificationRepository.fetchUnreadNotifications(uid, limit = 100)) {
                    is AppResult.Success -> {
                        NotificationSyncCoordinator.processIncomingNotifications(
                            context = applicationContext,
                            uid = uid,
                            notifications = res.data,
                            updateUnreadBadgeCount = false
                        )
                    }
                    is AppResult.Error -> Unit
                }

                LibraNotificationReceiver.scheduleBackupAlarm(applicationContext)
            } finally {
                jobFinished(params, false)
            }
        }
        return true
    }

    override fun onStopJob(params: JobParameters?): Boolean {
        syncJob?.cancel()
        serviceScope?.cancel()
        serviceScope = null
        return true
    }

    companion object {
        private const val JOB_ID_PERIODIC = 42071
        private const val JOB_ID_IMMEDIATE = 42072
        private const val PERIODIC_INTERVAL_MS = 15L * 60L * 1000L

        fun schedulePeriodicSync(context: Context) {
            runCatching {
                val scheduler = context.getSystemService(Context.JOB_SCHEDULER_SERVICE) as? JobScheduler ?: return
                if (scheduler.getPendingJob(JOB_ID_PERIODIC) != null) return
                val component = ComponentName(context, LibraNotificationJobService::class.java)
                val info = JobInfo.Builder(JOB_ID_PERIODIC, component)
                    .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
                    .setPersisted(true)
                    .setPeriodic(PERIODIC_INTERVAL_MS)
                    .build()
                scheduler.schedule(info)
            }
        }

        fun scheduleImmediateCheck(context: Context) {
            runCatching {
                val scheduler = context.getSystemService(Context.JOB_SCHEDULER_SERVICE) as? JobScheduler ?: return
                val component = ComponentName(context, LibraNotificationJobService::class.java)
                val info = JobInfo.Builder(JOB_ID_IMMEDIATE, component)
                    .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
                    .setMinimumLatency(3_000L)
                    .setOverrideDeadline(20_000L)
                    .build()
                scheduler.schedule(info)
            }
        }

        fun cancelAllSync(context: Context) {
            runCatching {
                val scheduler = context.getSystemService(Context.JOB_SCHEDULER_SERVICE) as? JobScheduler ?: return
                scheduler.cancel(JOB_ID_PERIODIC)
                scheduler.cancel(JOB_ID_IMMEDIATE)
            }
        }
    }
}
