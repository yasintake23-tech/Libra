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
    const val EXTRA_NOTIFICATION_CONV_KEY = "extra_libra_notif_conv_key"

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

    fun canScheduleExactAlarms(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return true
        val am = context.getSystemService(Context.ALARM_SERVICE) as? android.app.AlarmManager ?: return false
        return runCatching { am.canScheduleExactAlarms() }.getOrDefault(false)
    }

    @SuppressLint("BatteryLife")
    fun openBatteryOptimizationSettings(context: Context) {
        val packageUri = Uri.fromParts("package", context.packageName, null)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !isIgnoringBatteryOptimizations(context)) {
            val directIntent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                data = packageUri
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            if (runCatching { context.startActivity(directIntent) }.isSuccess) return
        }

        val listIntent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
        } else {
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply { data = packageUri }
        }
        listIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        runCatching { context.startActivity(listIntent) }.onFailure {
            val fallback = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = packageUri
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            runCatching { context.startActivity(fallback) }
        }
    }

    fun openExactAlarmSettings(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return
        val packageUri = Uri.fromParts("package", context.packageName, null)
        val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
            data = packageUri
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        runCatching { context.startActivity(intent) }.onFailure {
            openAppNotificationSettings(context)
        }
    }

    fun sendTestNotification(context: Context, uid: String) {
        val now = System.currentTimeMillis()
        dispatchSystemNotification(
            context = context,
            notification = AppNotification(
                id = "test_$now",
                recipientId = uid,
                actorId = "",
                actorName = "Libra Bildirim Testi",
                actorUsername = "libra",
                type = LibraNotificationTypes.MESSAGE,
                title = "Libra Bildirim Testi",
                body = "Kapalıyken bildirim, Yanıtla ve Okundu İşaretle çalışıyor.",
                referenceId = "",
                createdAt = now,
                read = false
            )
        )
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

    fun conversationNotificationKey(notification: AppNotification): String {
        val isChat = notification.type == LibraNotificationTypes.MESSAGE ||
            notification.type == LibraNotificationTypes.GROUP_MESSAGE
        return if (isChat) {
            notification.referenceId.ifBlank {
                if (notification.actorId.isNotBlank()) "actor_${notification.actorId}" else notification.id
            }.ifBlank { "${notification.type}_${notification.createdAt}" }
        } else {
            notification.id.ifBlank { "${notification.type}_${notification.createdAt}" }
        }
    }

    fun conversationRequestCode(convKey: String): Int {
        val hash = ("conv_$convKey").hashCode()
        return if (hash == 0) 1001 else hash
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
        intent.removeExtra(EXTRA_NOTIFICATION_CONV_KEY)
    }

    @SuppressLint("MissingPermission")
    fun dispatchSystemNotification(context: Context, notification: AppNotification) {
        val isChatNotification = notification.type == LibraNotificationTypes.MESSAGE ||
            notification.type == LibraNotificationTypes.GROUP_MESSAGE
        if (isChatNotification) {
            val convKey = conversationNotificationKey(notification)
            val senderDisplayName = notification.actorName.ifBlank {
                notification.actorUsername.takeIf { it.isNotBlank() }?.let { "@$it" }
                    ?: notification.title.ifBlank { "Libra" }
            }
            val singleMsg = PendingTrayMessage(
                id = notification.id,
                senderId = notification.actorId,
                senderName = senderDisplayName,
                body = notification.body.ifBlank { "Sana bir mesaj gönderdi." },
                timestampMs = notification.createdAt.takeIf { it > 0L } ?: System.currentTimeMillis()
            )
            dispatchGroupedChatNotification(
                context = context,
                convKey = convKey,
                messages = listOf(singleMsg),
                latestNotification = notification
            )
            return
        }

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
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))

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
    fun dispatchGroupedChatNotification(
        context: Context,
        convKey: String,
        messages: List<PendingTrayMessage>,
        latestNotification: AppNotification
    ) {
        if (!hasNotificationPermission(context)) return
        ensureChannels(context)

        val safeMessages = messages.ifEmpty {
            listOf(
                PendingTrayMessage(
                    id = latestNotification.id,
                    senderId = latestNotification.actorId,
                    senderName = latestNotification.actorName.ifBlank {
                        latestNotification.title.ifBlank { "Libra" }
                    },
                    body = latestNotification.body.ifBlank { "Yeni bir mesajın var." },
                    timestampMs = latestNotification.createdAt.takeIf { it > 0L } ?: System.currentTimeMillis()
                )
            )
        }
        val lastMsg = safeMessages.last()
        val requestCode = conversationRequestCode(convKey)
        val title = latestNotification.title.ifBlank { lastMsg.senderName.ifBlank { "Libra" } }
        val latestBody = lastMsg.body.ifBlank { "Yeni bir mesajın var." }
        val msgCount = safeMessages.size
        val summaryPreview = if (msgCount > 1) "$msgCount okunmamış mesaj: $latestBody" else latestBody
        val senderDisplayName = lastMsg.senderName.ifBlank {
            latestNotification.actorName.ifBlank {
                latestNotification.actorUsername.takeIf { it.isNotBlank() }?.let { "@$it" } ?: title
            }
        }

        val launchIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_NOTIFICATION_ID, latestNotification.id)
            putExtra(EXTRA_NOTIFICATION_TYPE, latestNotification.type)
            putExtra(EXTRA_NOTIFICATION_REF_ID, latestNotification.referenceId)
            putExtra(EXTRA_NOTIFICATION_ACTOR_ID, latestNotification.actorId)
            putExtra(EXTRA_NOTIFICATION_TITLE, title)
            putExtra(EXTRA_NOTIFICATION_BODY, latestBody)
            putExtra(EXTRA_NOTIFICATION_CONV_KEY, convKey)
        }
        val contentPendingIntent = PendingIntent.getActivity(
            context,
            requestCode,
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val dismissIntent = Intent(context, LibraNotificationActionReceiver::class.java).apply {
            action = LibraNotificationActionReceiver.ACTION_DISMISSED
            putExtra(EXTRA_NOTIFICATION_TYPE, latestNotification.type)
            putExtra(EXTRA_NOTIFICATION_REF_ID, latestNotification.referenceId)
            putExtra(EXTRA_NOTIFICATION_CONV_KEY, convKey)
            putExtra(EXTRA_NOTIFICATION_REQUEST_CODE, requestCode)
        }
        val dismissPendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode * 31 + 3,
            dismissIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val mePerson = Person.Builder()
            .setName("Sen")
            .setKey("libra_self")
            .build()
        val messagingStyle = NotificationCompat.MessagingStyle(mePerson)
        val isGroupChat = latestNotification.type == LibraNotificationTypes.GROUP_MESSAGE
        if (isGroupChat) {
            messagingStyle.setConversationTitle(title)
            messagingStyle.setGroupConversation(true)
        }
        safeMessages.forEach { msg ->
            val personName = msg.senderName.ifBlank { senderDisplayName }
            val senderPerson = Person.Builder()
                .setName(personName)
                .setKey(msg.senderId.ifBlank { "libra_sender_${personName.hashCode()}" })
                .build()
            val msgTs = msg.timestampMs.takeIf { it > 0L } ?: System.currentTimeMillis()
            messagingStyle.addMessage(msg.body, msgTs, senderPerson)
        }

        val builder = NotificationCompat.Builder(context, CHANNEL_MESSAGES)
            .setSmallIcon(R.drawable.ic_libra_notification)
            .setContentTitle(title)
            .setContentText(summaryPreview)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .setAutoCancel(true)
            .setNumber(msgCount)
            .setWhen(lastMsg.timestampMs.takeIf { it > 0L } ?: System.currentTimeMillis())
            .setContentIntent(contentPendingIntent)
            .setDeleteIntent(dismissPendingIntent)
            .setStyle(messagingStyle)
        if (msgCount > 1) {
            builder.setSubText("$msgCount okunmamış mesaj")
        }

        // WhatsApp-style inline reply action
        val remoteInput = RemoteInput.Builder(KEY_REPLY_TEXT)
            .setLabel("Yanıt yaz...")
            .build()
        val replyIntent = Intent(context, LibraNotificationActionReceiver::class.java).apply {
            action = LibraNotificationActionReceiver.ACTION_INLINE_REPLY
            putExtra(EXTRA_NOTIFICATION_ID, latestNotification.id)
            putExtra(EXTRA_NOTIFICATION_TYPE, latestNotification.type)
            putExtra(EXTRA_NOTIFICATION_REF_ID, latestNotification.referenceId)
            putExtra(EXTRA_NOTIFICATION_ACTOR_ID, latestNotification.actorId)
            putExtra(EXTRA_NOTIFICATION_TITLE, title)
            putExtra(EXTRA_NOTIFICATION_BODY, latestBody)
            putExtra(EXTRA_NOTIFICATION_ACTOR_NAME, senderDisplayName)
            putExtra(EXTRA_NOTIFICATION_CONV_KEY, convKey)
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

        // "Okundu İşaretle" button across chat notifications
        val markReadIntent = Intent(context, LibraNotificationActionReceiver::class.java).apply {
            action = LibraNotificationActionReceiver.ACTION_MARK_READ
            putExtra(EXTRA_NOTIFICATION_ID, latestNotification.id)
            putExtra(EXTRA_NOTIFICATION_TYPE, latestNotification.type)
            putExtra(EXTRA_NOTIFICATION_REF_ID, latestNotification.referenceId)
            putExtra(EXTRA_NOTIFICATION_ACTOR_ID, latestNotification.actorId)
            putExtra(EXTRA_NOTIFICATION_CONV_KEY, convKey)
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

    fun cancelConversationNotification(context: Context, uid: String, conversationId: String) {
        if (conversationId.isBlank()) return
        if (uid.isNotBlank()) {
            NotificationPreferencesStore(context).clearChatTrayMessages(uid, conversationId)
        }
        cancelNotification(context, conversationRequestCode(conversationId))
    }

    fun cancelNotification(context: Context, requestCode: Int) {
        if (requestCode == 0) return
        runCatching {
            NotificationManagerCompat.from(context).cancel(requestCode)
        }
    }
}
