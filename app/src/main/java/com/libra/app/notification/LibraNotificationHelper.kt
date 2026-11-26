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
import androidx.core.app.Person
import androidx.core.app.RemoteInput
import androidx.core.content.ContextCompat
import com.libra.app.MainActivity
import com.libra.app.R
import com.libra.app.domain.model.AppNotification
import com.libra.app.domain.model.LibraNotificationTypes

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
    const val EXTRA_NOTIFICATION_REQUEST_CODE = "extra_libra_notif_req_code"
    const val EXTRA_NOTIFICATION_ACTOR_NAME = "extra_libra_notif_actor_name"

    const val KEY_REPLY_TEXT = "key_libra_reply_text"

    fun ensureChannels(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return
        val includeBackgroundChannel = runCatching {
            NotificationPreferencesStore(context).readPreferences().backgroundServiceEnabled
        }.getOrDefault(false)

        val channels = mutableListOf(
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
            }
        )

        if (includeBackgroundChannel) {
            channels += NotificationChannel(
                CHANNEL_BACKGROUND_SERVICE,
                "Arka Plan Bildirim Bağlantısı",
                NotificationManager.IMPORTANCE_MIN
            ).apply {
                description = "İsteğe bağlı kesintisiz canlı bağlantı durumu."
                setShowBadge(false)
                setSound(null, null)
                enableVibration(false)
            }
        } else {
            runCatching { manager.deleteNotificationChannel(CHANNEL_BACKGROUND_SERVICE) }
        }

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

    fun notificationRequestCode(notificationId: String, type: String, createdAt: Long): Int =
        notificationId.ifBlank { "${type}_$createdAt" }.hashCode()

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
            .setContentText("Canlı bildirim bağlantısı aktif")
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

        val requestCode = notificationRequestCode(notification.id, notification.type, notification.createdAt)
        val title = notification.title.ifBlank { "Libra" }
        val body = notification.body.ifBlank { "Yeni bir bildirimin var." }
        val senderDisplayName = notification.actorName.ifBlank {
            notification.actorUsername.takeIf { it.isNotBlank() }?.let { "@$it" } ?: title
        }

        val launchIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_NOTIFICATION_ID, notification.id)
            putExtra(EXTRA_NOTIFICATION_TYPE, notification.type)
            putExtra(EXTRA_NOTIFICATION_REF_ID, notification.referenceId)
            putExtra(EXTRA_NOTIFICATION_ACTOR_ID, notification.actorId)
            putExtra(EXTRA_NOTIFICATION_TITLE, title)
            putExtra(EXTRA_NOTIFICATION_BODY, body)
        }
        val contentPendingIntent = PendingIntent.getActivity(
            context,
            requestCode,
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_libra_notification)
            .setContentTitle(title)
            .setContentText(body)
            .setPriority(priority)
            .setCategory(compatCategory)
            .setAutoCancel(true)
            .setContentIntent(contentPendingIntent)

        val isChatNotification = notification.type == LibraNotificationTypes.MESSAGE ||
            notification.type == LibraNotificationTypes.GROUP_MESSAGE

        if (isChatNotification) {
            val mePerson = Person.Builder()
                .setName("Sen")
                .setKey("libra_self")
                .build()
            val senderPerson = Person.Builder()
                .setName(senderDisplayName)
                .setKey(notification.actorId.ifBlank { "libra_sender" })
                .build()
            val timestamp = notification.createdAt.takeIf { it > 0L } ?: System.currentTimeMillis()
            val messagingStyle = NotificationCompat.MessagingStyle(mePerson)
            if (notification.type == LibraNotificationTypes.GROUP_MESSAGE) {
                messagingStyle.setConversationTitle(title)
                messagingStyle.setGroupConversation(true)
            }
            messagingStyle.addMessage(body, timestamp, senderPerson)
            builder.setStyle(messagingStyle)

            // WhatsApp-style inline reply action
            val remoteInput = RemoteInput.Builder(KEY_REPLY_TEXT)
                .setLabel("Yanıt yaz...")
                .build()
            val replyIntent = Intent(context, LibraNotificationActionReceiver::class.java).apply {
                action = LibraNotificationActionReceiver.ACTION_INLINE_REPLY
                putExtra(EXTRA_NOTIFICATION_ID, notification.id)
                putExtra(EXTRA_NOTIFICATION_TYPE, notification.type)
                putExtra(EXTRA_NOTIFICATION_REF_ID, notification.referenceId)
                putExtra(EXTRA_NOTIFICATION_ACTOR_ID, notification.actorId)
                putExtra(EXTRA_NOTIFICATION_TITLE, title)
                putExtra(EXTRA_NOTIFICATION_BODY, body)
                putExtra(EXTRA_NOTIFICATION_ACTOR_NAME, senderDisplayName)
                putExtra(EXTRA_NOTIFICATION_REQUEST_CODE, requestCode)
            }
            val replyFlags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
            } else {
                PendingIntent.FLAG_UPDATE_CURRENT
            }
            val replyPendingIntent = PendingIntent.getBroadcast(
                context,
                requestCode * 31 + 1,
                replyIntent,
                replyFlags
            )
            val replyAction = NotificationCompat.Action.Builder(
                android.R.drawable.ic_menu_send,
                "Yanıtla",
                replyPendingIntent
            )
                .addRemoteInput(remoteInput)
                .setAllowGeneratedReplies(true)
                .build()
            builder.addAction(replyAction)
        } else {
            builder.setStyle(NotificationCompat.BigTextStyle().bigText(body))
        }

        // "Okundu İşaretle" button across notifications (marks Firestore read + DM Görüldü)
        val markReadIntent = Intent(context, LibraNotificationActionReceiver::class.java).apply {
            action = LibraNotificationActionReceiver.ACTION_MARK_READ
            putExtra(EXTRA_NOTIFICATION_ID, notification.id)
            putExtra(EXTRA_NOTIFICATION_TYPE, notification.type)
            putExtra(EXTRA_NOTIFICATION_REF_ID, notification.referenceId)
            putExtra(EXTRA_NOTIFICATION_ACTOR_ID, notification.actorId)
            putExtra(EXTRA_NOTIFICATION_REQUEST_CODE, requestCode)
        }
        val markReadPendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode * 31 + 2,
            markReadIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        builder.addAction(
            android.R.drawable.ic_menu_view,
            "Okundu İşaretle",
            markReadPendingIntent
        )

        runCatching {
            NotificationManagerCompat.from(context).notify(requestCode, builder.build())
        }
    }

    @SuppressLint("MissingPermission")
    fun showInlineReplyResultNotification(
        context: Context,
        requestCode: Int,
        conversationTitle: String,
        senderName: String,
        incomingText: String,
        replyText: String,
        isGroup: Boolean,
        sentSuccessfully: Boolean
    ) {
        if (!hasNotificationPermission(context)) return
        ensureChannels(context)

        val mePerson = Person.Builder().setName("Sen").setKey("libra_self").build()
        val senderPerson = Person.Builder().setName(senderName.ifBlank { conversationTitle }).build()
        val now = System.currentTimeMillis()
        val style = NotificationCompat.MessagingStyle(mePerson)
        if (isGroup) {
            style.setConversationTitle(conversationTitle)
            style.setGroupConversation(true)
        }
        if (incomingText.isNotBlank()) {
            style.addMessage(incomingText, now - 1000L, senderPerson)
        }
        val confirmLine = if (sentSuccessfully) replyText else "Gönderilemedi: $replyText"
        style.addMessage(confirmLine, now, mePerson)

        val notification = NotificationCompat.Builder(context, CHANNEL_MESSAGES)
            .setSmallIcon(R.drawable.ic_libra_notification)
            .setStyle(style)
            .setOnlyAlertOnce(true)
            .setTimeoutAfter(if (sentSuccessfully) 2500L else 6000L)
            .setAutoCancel(true)
            .build()

        runCatching {
            NotificationManagerCompat.from(context).notify(requestCode, notification)
        }
    }

    fun cancelNotification(context: Context, requestCode: Int) {
        if (requestCode == 0) return
        runCatching {
            NotificationManagerCompat.from(context).cancel(requestCode)
        }
    }
}
