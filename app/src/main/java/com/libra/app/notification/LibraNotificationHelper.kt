package com.libra.app.notification

import android.Manifest
import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.libra.app.MainActivity
import com.libra.app.R
import com.libra.app.domain.model.AppNotification

data class NotificationLaunchPayload(
    val notificationId: String,
    val type: String,
    val referenceId: String,
    val actorId: String,
    val title: String,
    val body: String
)

object LibraNotificationHelper {
    const val CHANNEL_MESSAGES = "libra_messages"
    const val CHANNEL_SOCIAL = "libra_social"
    const val CHANNEL_ANNOUNCEMENTS = "libra_announcements"
    const val CHANNEL_BACKGROUND_SERVICE = "libra_background_service"

    const val FOREGROUND_SERVICE_NOTIFICATION_ID = 42070

    const val EXTRA_NOTIFICATION_ID = "extra_libra_notif_id"
    const val EXTRA_NOTIFICATION_TYPE = "extra_libra_notif_type"
    const val EXTRA_NOTIFICATION_REF_ID = "extra_libra_notif_ref_id"
    const val EXTRA_NOTIFICATION_ACTOR_ID = "extra_libra_notif_actor_id"
    const val EXTRA_NOTIFICATION_TITLE = "extra_libra_notif_title"
    const val EXTRA_NOTIFICATION_BODY = "extra_libra_notif_body"

    fun ensureChannels(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return

        val channels = listOf(
            NotificationChannel(
                CHANNEL_MESSAGES,
                "Mesajlar ve Sohbetler",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Doğrudan mesajlar, grup sohbetleri ve sunucu bahsedilmeleri."
                enableVibration(true)
            },
            NotificationChannel(
                CHANNEL_SOCIAL,
                "Etkileşimler",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Gönderi, kitap ve hikâye beğenileri, yorumlar ve takipçiler."
            },
            NotificationChannel(
                CHANNEL_ANNOUNCEMENTS,
                "Duyurular ve Yönetim",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Libra yönetim duyuruları ve önemli bilgilendirmeler."
                enableVibration(true)
            },
            NotificationChannel(
                CHANNEL_BACKGROUND_SERVICE,
                "Arka Plan Bildirim Bağlantısı",
                NotificationManager.IMPORTANCE_MIN
            ).apply {
                description = "Uygulama kapalıyken anlık bildirim alabilmek için sessiz bağlantı durumu."
                setShowBadge(false)
                setSound(null, null)
                enableVibration(false)
            }
        )
        manager.createNotificationChannels(channels)
    }

    fun hasNotificationPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            NotificationManagerCompat.from(context).areNotificationsEnabled()
        }
    }

    fun isIgnoringBatteryOptimizations(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return true
        val pm = context.getSystemService(Context.POWER_SERVICE) as? PowerManager ?: return true
        return runCatching { pm.isIgnoringBatteryOptimizations(context.packageName) }.getOrDefault(false)
    }

    fun openBatteryOptimizationSettings(context: Context) {
        val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
        } else {
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.fromParts("package", context.packageName, null)
            }
        }
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        runCatching { context.startActivity(intent) }.onFailure {
            val fallback = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.fromParts("package", context.packageName, null)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            runCatching { context.startActivity(fallback) }
        }
    }

    fun openAppNotificationSettings(context: Context) {
        val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
            }
        } else {
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.fromParts("package", context.packageName, null)
            }
        }
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        runCatching { context.startActivity(intent) }
    }

    fun buildForegroundSyncNotification(context: Context): Notification {
        ensureChannels(context)
        val openIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            FOREGROUND_SERVICE_NOTIFICATION_ID,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(context, CHANNEL_BACKGROUND_SERVICE)
            .setSmallIcon(R.drawable.ic_libra_notification)
            .setContentTitle("Libra")
            .setContentText("Anlık bildirim senkronizasyonu aktif")
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setOngoing(true)
            .setSilent(true)
            .setShowWhen(false)
            .setContentIntent(pendingIntent)
            .build()
    }

    fun extractNotificationPayload(intent: Intent?): NotificationLaunchPayload? {
        intent ?: return null
        val type = intent.getStringExtra(EXTRA_NOTIFICATION_TYPE).orEmpty()
        val id = intent.getStringExtra(EXTRA_NOTIFICATION_ID).orEmpty()
        if (type.isBlank() && id.isBlank()) return null
        return NotificationLaunchPayload(
            notificationId = id,
            type = type,
            referenceId = intent.getStringExtra(EXTRA_NOTIFICATION_REF_ID).orEmpty(),
            actorId = intent.getStringExtra(EXTRA_NOTIFICATION_ACTOR_ID).orEmpty(),
            title = intent.getStringExtra(EXTRA_NOTIFICATION_TITLE).orEmpty(),
            body = intent.getStringExtra(EXTRA_NOTIFICATION_BODY).orEmpty()
        )
    }

    fun clearNotificationExtras(intent: Intent?) {
        intent ?: return
        intent.removeExtra(EXTRA_NOTIFICATION_ID)
        intent.removeExtra(EXTRA_NOTIFICATION_TYPE)
        intent.removeExtra(EXTRA_NOTIFICATION_REF_ID)
        intent.removeExtra(EXTRA_NOTIFICATION_ACTOR_ID)
        intent.removeExtra(EXTRA_NOTIFICATION_TITLE)
        intent.removeExtra(EXTRA_NOTIFICATION_BODY)
    }

    @SuppressLint("MissingPermission")
    fun dispatchSystemNotification(context: Context, notification: AppNotification) {
        if (!hasNotificationPermission(context)) return
        ensureChannels(context)

        val category = classifyNotificationCategory(notification.type)
        val channelId = when (category) {
            NotificationPreferenceCategory.MESSAGES -> CHANNEL_MESSAGES
            NotificationPreferenceCategory.SOCIAL -> CHANNEL_SOCIAL
            NotificationPreferenceCategory.ANNOUNCEMENTS -> CHANNEL_ANNOUNCEMENTS
        }
        val priority = when (category) {
            NotificationPreferenceCategory.MESSAGES,
            NotificationPreferenceCategory.ANNOUNCEMENTS -> NotificationCompat.PRIORITY_HIGH
            NotificationPreferenceCategory.SOCIAL -> NotificationCompat.PRIORITY_DEFAULT
        }
        val compatCategory = when (category) {
            NotificationPreferenceCategory.MESSAGES -> NotificationCompat.CATEGORY_MESSAGE
            NotificationPreferenceCategory.SOCIAL -> NotificationCompat.CATEGORY_SOCIAL
            NotificationPreferenceCategory.ANNOUNCEMENTS -> NotificationCompat.CATEGORY_RECOMMENDATION
        }

        val launchIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_NOTIFICATION_ID, notification.id)
            putExtra(EXTRA_NOTIFICATION_TYPE, notification.type)
            putExtra(EXTRA_NOTIFICATION_REF_ID, notification.referenceId)
            putExtra(EXTRA_NOTIFICATION_ACTOR_ID, notification.actorId)
            putExtra(EXTRA_NOTIFICATION_TITLE, notification.title)
            putExtra(EXTRA_NOTIFICATION_BODY, notification.body)
        }

        val requestCode = notification.id.ifBlank { "${notification.type}_${notification.createdAt}" }.hashCode()
        val pendingIntent = PendingIntent.getActivity(
            context,
            requestCode,
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val title = notification.title.ifBlank { "Libra" }
        val body = notification.body.ifBlank { "Yeni bir bildirimin var." }

        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_libra_notification)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(priority)
            .setCategory(compatCategory)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)

        runCatching {
            NotificationManagerCompat.from(context).notify(requestCode, builder.build())
        }
    }
}
