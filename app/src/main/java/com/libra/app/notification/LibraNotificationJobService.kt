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
        val uid = runCatching { FirebaseAuth.getInstance().currentUser?.uid }.getOrNull().orEmpty()
        if (uid.isBlank() || !LibraNotificationHelper.hasNotificationPermission(this)) {
            return false
        }

        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        serviceScope = scope
        syncJob = scope.launch {
            try {
                val store = NotificationPreferencesStore(applicationContext)
                val prefs = store.readPreferences()
                if (!prefs.masterEnabled) return@launch

                when (val res = ServiceLocator.notificationRepository.fetchUnreadNotifications(uid, limit = 25)) {
                    is AppResult.Success -> {
                        val selection = selectNewNotificationsForDelivery(
                            notifications = res.data,
                            hasBaseline = store.hasBaseline(uid),
                            baselineMs = store.getBaselineTimestamp(uid),
                            alreadyNotifiedIds = store.getNotifiedIds(uid),
                            preferences = prefs
                        )
                        selection.toDispatch.forEach { notification ->
                            LibraNotificationHelper.dispatchSystemNotification(applicationContext, notification)
                        }
                        if (selection.idsToMarkNotified.isNotEmpty() || !store.hasBaseline(uid)) {
                            store.recordDeliverySelection(uid, selection)
                        }
                    }
                    is AppResult.Error -> Unit
                }
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
        private const val JOB_ID = 42071
        private const val PERIODIC_INTERVAL_MS = 15L * 60L * 1000L

        fun schedulePeriodicSync(context: Context) {
            runCatching {
                val scheduler = context.getSystemService(Context.JOB_SCHEDULER_SERVICE) as? JobScheduler ?: return
                if (scheduler.getPendingJob(JOB_ID) != null) return
                val component = ComponentName(context, LibraNotificationJobService::class.java)
                val info = JobInfo.Builder(JOB_ID, component)
                    .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
                    .setPeriodic(PERIODIC_INTERVAL_MS)
                    .build()
                scheduler.schedule(info)
            }
        }

        fun cancelPeriodicSync(context: Context) {
            runCatching {
                val scheduler = context.getSystemService(Context.JOB_SCHEDULER_SERVICE) as? JobScheduler ?: return
                scheduler.cancel(JOB_ID)
            }
        }
    }
}
