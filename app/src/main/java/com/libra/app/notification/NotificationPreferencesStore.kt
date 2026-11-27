package com.libra.app.notification

import android.content.Context
import android.content.SharedPreferences
import com.libra.app.domain.model.AppNotification
import com.libra.app.domain.model.LibraNotificationTypes
import org.json.JSONArray
import org.json.JSONObject

enum class NotificationPreferenceCategory {
    MESSAGES,
    SOCIAL,
    ANNOUNCEMENTS
}

data class NotificationPreferencesState(
    val masterEnabled: Boolean = true,
    val messagesEnabled: Boolean = true,
    val socialEnabled: Boolean = true,
    val announcementsEnabled: Boolean = true,
    val backgroundServiceEnabled: Boolean = false
) {
    fun allowsType(type: String): Boolean {
        if (!masterEnabled) return false
        return when (classifyNotificationCategory(type)) {
            NotificationPreferenceCategory.MESSAGES -> messagesEnabled
            NotificationPreferenceCategory.SOCIAL -> socialEnabled
            NotificationPreferenceCategory.ANNOUNCEMENTS -> announcementsEnabled
        }
    }
}

fun classifyNotificationCategory(type: String): NotificationPreferenceCategory = when (type.uppercase()) {
    LibraNotificationTypes.MESSAGE,
    LibraNotificationTypes.GROUP_MESSAGE,
    LibraNotificationTypes.SERVER_MENTION -> NotificationPreferenceCategory.MESSAGES

    LibraNotificationTypes.ADMIN_BROADCAST,
    LibraNotificationTypes.ADMIN_FEEDBACK -> NotificationPreferenceCategory.ANNOUNCEMENTS

    else -> NotificationPreferenceCategory.SOCIAL
}

data class NotificationDeliverySelection(
    val toDispatch: List<AppNotification>,
    val idsToMarkNotified: List<String>,
    val nextBaselineMs: Long
)

fun selectNewNotificationsForDelivery(
    notifications: List<AppNotification>,
    hasBaseline: Boolean,
    baselineMs: Long,
    alreadyNotifiedIds: Set<String>,
    preferences: NotificationPreferencesState,
    nowMs: Long = System.currentTimeMillis()
): NotificationDeliverySelection {
    if (notifications.isEmpty()) {
        return NotificationDeliverySelection(
            toDispatch = emptyList(),
            idsToMarkNotified = emptyList(),
            nextBaselineMs = if (hasBaseline) baselineMs else nowMs
        )
    }

    val effectiveBaseline = if (hasBaseline && baselineMs > 0L) {
        baselineMs
    } else {
        nowMs - 15_000L
    }

    val toDispatch = mutableListOf<AppNotification>()
    val idsToMark = mutableListOf<String>()

    notifications.sortedBy { it.createdAt }.forEach { item ->
        val id = item.id
        if (id.isBlank() || id in alreadyNotifiedIds) return@forEach

        val isTooOldForInitialSeed = !hasBaseline && item.createdAt > 0L && item.createdAt < effectiveBaseline
        // Keep a wide 24h window after baseline is established so clock skew between devices
        // or delayed background wakeups never silently drop missed unread notifications.
        val isBeforeBaselineWindow = hasBaseline && item.createdAt > 0L && item.createdAt < (effectiveBaseline - 24L * 60L * 60L * 1000L)

        if (item.read || isTooOldForInitialSeed || isBeforeBaselineWindow) {
            idsToMark.add(id)
        } else {
            idsToMark.add(id)
            if (preferences.allowsType(item.type)) {
                toDispatch.add(item)
            }
        }
    }

    val latestCreated = notifications.maxOfOrNull { it.createdAt } ?: 0L
    val nextBaseline = maxOf(effectiveBaseline, latestCreated)

    return NotificationDeliverySelection(
        toDispatch = toDispatch,
        idsToMarkNotified = idsToMark,
        nextBaselineMs = nextBaseline
    )
}

data class PendingTrayMessage(
    val id: String,
    val senderId: String,
    val senderName: String,
    val body: String,
    val timestampMs: Long
)

fun appendAndMergeTrayMessages(
    existing: List<PendingTrayMessage>,
    incoming: List<PendingTrayMessage>,
    maxMessages: Int = 25
): List<PendingTrayMessage> {
    val merged = LinkedHashMap<String, PendingTrayMessage>()
    fun stableKey(msg: PendingTrayMessage): String =
        msg.id.ifBlank { "${msg.senderId}_${msg.timestampMs}_${msg.body.trim()}" }

    existing.forEach { msg ->
        merged[stableKey(msg)] = msg.copy(body = msg.body.trim())
    }
    incoming.forEach { raw ->
        val msg = raw.copy(body = raw.body.trim())
        val key = stableKey(msg)
        if (merged.containsKey(key)) return@forEach
        val isRtdbFallback = msg.id.startsWith("rtdb_")
        val duplicateRtdbOverlay = if (isRtdbFallback) {
            merged.values.any { prev ->
                !prev.id.startsWith("rtdb_") &&
                    prev.body == msg.body &&
                    kotlin.math.abs(prev.timestampMs - msg.timestampMs) <= 10_000L
            }
        } else {
            false
        }
        if (!duplicateRtdbOverlay) {
            if (!isRtdbFallback) {
                val staleKeys = merged.entries.filter { (_, prev) ->
                    prev.id.startsWith("rtdb_") &&
                        prev.body == msg.body &&
                        kotlin.math.abs(prev.timestampMs - msg.timestampMs) <= 10_000L
                }.map { it.key }
                staleKeys.forEach { merged.remove(it) }
            }
            merged[key] = msg
        }
    }
    return merged.values
        .sortedBy { it.timestampMs }
        .takeLast(maxMessages.coerceAtLeast(1))
}

fun shouldSuppressActiveNotification(
    notification: AppNotification,
    isNotificationsScreenOpen: Boolean,
    activeDirectConversationId: String?,
    isMessagesScreenOpen: Boolean = false
): Boolean {
    if (isNotificationsScreenOpen) return true
    val isChatType = notification.type == LibraNotificationTypes.MESSAGE ||
        notification.type == LibraNotificationTypes.GROUP_MESSAGE
    if (isChatType && isMessagesScreenOpen) return true
    if (activeDirectConversationId.isNullOrBlank()) return false
    return isChatType && notification.referenceId == activeDirectConversationId
}

fun buildRtdbFallbackDmNotifications(
    uid: String,
    conversations: List<com.libra.app.domain.model.DirectConversation>,
    existingFirestoreChatNotifications: List<AppNotification>,
    getLastDmNotifiedAt: (String) -> Long,
    getClearedAt: (String) -> Long,
    preferences: NotificationPreferencesState,
    nowMs: Long = System.currentTimeMillis()
): List<AppNotification> {
    if (uid.isBlank() || conversations.isEmpty()) return emptyList()
    val firestoreLatestByConv = mutableMapOf<String, Long>()
    existingFirestoreChatNotifications.forEach { item ->
        val isChat = item.type == LibraNotificationTypes.MESSAGE ||
            item.type == LibraNotificationTypes.GROUP_MESSAGE
        if (isChat && item.referenceId.isNotBlank()) {
            val prev = firestoreLatestByConv[item.referenceId] ?: 0L
            firestoreLatestByConv[item.referenceId] = maxOf(prev, item.createdAt)
        }
    }

    val minRecentWindowMs = nowMs - 24L * 60L * 60L * 1000L
    return conversations.mapNotNull { conv ->
        if (conv.id.isBlank() || conv.unreadCount <= 0 || conv.updatedAt <= 0L) return@mapNotNull null
        if (conv.updatedAt < minRecentWindowMs) return@mapNotNull null
        val clearedAt = getClearedAt(conv.id)
        if (conv.updatedAt <= clearedAt) return@mapNotNull null
        val lastNotified = getLastDmNotifiedAt(conv.id)
        if (conv.updatedAt <= lastNotified) return@mapNotNull null
        val fsTs = firestoreLatestByConv[conv.id] ?: 0L
        if (fsTs > 0L && kotlin.math.abs(fsTs - conv.updatedAt) <= 10_000L) return@mapNotNull null

        val notifType = if (conv.isGroup) LibraNotificationTypes.GROUP_MESSAGE else LibraNotificationTypes.MESSAGE
        if (!preferences.allowsType(notifType)) return@mapNotNull null

        val senderDisplay = if (conv.isGroup) {
            conv.groupName.ifBlank { "Grup sohbeti" }
        } else {
            conv.otherUserName.ifBlank {
                conv.otherUserUsername.takeIf { it.isNotBlank() }?.let { "@$it" } ?: "Yeni mesaj"
            }
        }
        val preview = conv.lastMessage.ifBlank { "Sana bir mesaj gönderdi." }

        AppNotification(
            id = "rtdb_${conv.id}_${conv.updatedAt}",
            recipientId = uid,
            actorId = if (conv.isGroup) "" else conv.otherUserId,
            actorName = senderDisplay,
            actorUsername = conv.otherUserUsername,
            actorPhotoUrl = if (conv.isGroup) conv.groupPhotoUrl else conv.otherUserPhotoUrl,
            type = notifType,
            title = senderDisplay,
            body = preview,
            referenceId = conv.id,
            createdAt = conv.updatedAt,
            read = false
        )
    }.sortedBy { it.createdAt }
}

class NotificationPreferencesStore(context: Context) {
    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    @Synchronized
    fun readPreferences(): NotificationPreferencesState = NotificationPreferencesState(
        masterEnabled = prefs.getBoolean(KEY_MASTER_ENABLED, true),
        messagesEnabled = prefs.getBoolean(KEY_MESSAGES_ENABLED, true),
        socialEnabled = prefs.getBoolean(KEY_SOCIAL_ENABLED, true),
        announcementsEnabled = prefs.getBoolean(KEY_ANNOUNCEMENTS_ENABLED, true),
        backgroundServiceEnabled = prefs.getBoolean(KEY_BACKGROUND_SERVICE, false)
    )

    @Synchronized
    fun savePreferences(state: NotificationPreferencesState) {
        prefs.edit()
            .putBoolean(KEY_MASTER_ENABLED, state.masterEnabled)
            .putBoolean(KEY_MESSAGES_ENABLED, state.messagesEnabled)
            .putBoolean(KEY_SOCIAL_ENABLED, state.socialEnabled)
            .putBoolean(KEY_ANNOUNCEMENTS_ENABLED, state.announcementsEnabled)
            .putBoolean(KEY_BACKGROUND_SERVICE, state.backgroundServiceEnabled)
            .apply()
    }

    @Synchronized
    fun wasPermissionPrompted(): Boolean = prefs.getBoolean(KEY_PERMISSION_PROMPTED, false)

    @Synchronized
    fun markPermissionPrompted() {
        prefs.edit().putBoolean(KEY_PERMISSION_PROMPTED, true).apply()
    }

    @Synchronized
    fun wasBatteryPrompted(): Boolean = prefs.getBoolean(KEY_BATTERY_PROMPTED, false)

    @Synchronized
    fun markBatteryPrompted() {
        prefs.edit().putBoolean(KEY_BATTERY_PROMPTED, true).apply()
    }

    @Synchronized
    fun getLastDmNotifiedAt(uid: String, conversationId: String): Long =
        if (uid.isBlank() || conversationId.isBlank()) 0L
        else prefs.getLong(dmNotifiedKey(uid, conversationId), 0L)

    @Synchronized
    fun recordDmNotifiedAt(uid: String, conversationId: String, timestampMs: Long) {
        if (uid.isBlank() || conversationId.isBlank() || timestampMs <= 0L) return
        val key = dmNotifiedKey(uid, conversationId)
        val prev = prefs.getLong(key, 0L)
        if (timestampMs > prev) {
            prefs.edit().putLong(key, timestampMs).apply()
        }
    }

    @Synchronized
    fun getBaselineTimestamp(uid: String): Long =
        prefs.getLong(baselineKey(uid), 0L)

    @Synchronized
    fun hasBaseline(uid: String): Boolean =
        prefs.contains(baselineKey(uid))

    @Synchronized
    fun getNotifiedIds(uid: String): Set<String> {
        val raw = prefs.getString(notifiedIdsKey(uid), "").orEmpty()
        if (raw.isBlank()) return emptySet()
        return raw.split(",").filter { it.isNotBlank() }.toSet()
    }

    @Synchronized
    fun recordDeliverySelection(uid: String, selection: NotificationDeliverySelection) {
        if (uid.isBlank()) return
        val existingOrdered = prefs.getString(notifiedIdsKey(uid), "")
            .orEmpty()
            .split(",")
            .filter { it.isNotBlank() }
            .toMutableList()
        selection.idsToMarkNotified.forEach { id ->
            if (id.isNotBlank() && id !in existingOrdered) {
                existingOrdered.add(id)
            }
        }
        val capped = if (existingOrdered.size > MAX_STORED_IDS) {
            existingOrdered.takeLast(MAX_STORED_IDS)
        } else {
            existingOrdered
        }
        val editor = prefs.edit()
            .putLong(baselineKey(uid), selection.nextBaselineMs)
            .putString(notifiedIdsKey(uid), capped.joinToString(","))

        selection.toDispatch.forEach { item ->
            val isChat = item.type == LibraNotificationTypes.MESSAGE ||
                item.type == LibraNotificationTypes.GROUP_MESSAGE
            if (isChat && item.referenceId.isNotBlank() && item.createdAt > 0L) {
                val key = dmNotifiedKey(uid, item.referenceId)
                val prev = prefs.getLong(key, 0L)
                if (item.createdAt > prev) {
                    editor.putLong(key, item.createdAt)
                }
            }
        }
        editor.apply()
    }

    @Synchronized
    fun getChatTrayMessages(uid: String, convKey: String): List<PendingTrayMessage> {
        if (uid.isBlank() || convKey.isBlank()) return emptyList()
        val raw = prefs.getString(trayMessagesKey(uid, convKey), null).orEmpty()
        if (raw.isBlank()) return emptyList()
        return runCatching {
            val array = JSONArray(raw)
            buildList {
                for (i in 0 until array.length()) {
                    val obj = array.optJSONObject(i) ?: continue
                    val body = obj.optString("body").trim()
                    if (body.isBlank()) continue
                    add(
                        PendingTrayMessage(
                            id = obj.optString("id"),
                            senderId = obj.optString("senderId"),
                            senderName = obj.optString("senderName"),
                            body = body,
                            timestampMs = obj.optLong("timestampMs", 0L)
                        )
                    )
                }
            }
        }.getOrDefault(emptyList())
    }

    @Synchronized
    fun appendChatTrayMessages(
        uid: String,
        convKey: String,
        incoming: List<PendingTrayMessage>
    ): List<PendingTrayMessage> {
        if (uid.isBlank() || convKey.isBlank()) return incoming
        val existing = getChatTrayMessages(uid, convKey)
        val merged = appendAndMergeTrayMessages(existing, incoming)
        val array = JSONArray()
        merged.forEach { msg ->
            array.put(
                JSONObject().apply {
                    put("id", msg.id)
                    put("senderId", msg.senderId)
                    put("senderName", msg.senderName)
                    put("body", msg.body)
                    put("timestampMs", msg.timestampMs)
                }
            )
        }
        prefs.edit().putString(trayMessagesKey(uid, convKey), array.toString()).apply()
        return merged
    }

    @Synchronized
    fun clearChatTrayMessages(uid: String, convKey: String) {
        if (uid.isBlank() || convKey.isBlank()) return
        prefs.edit().remove(trayMessagesKey(uid, convKey)).apply()
    }

    private fun baselineKey(uid: String): String = "baseline_ms_$uid"
    private fun notifiedIdsKey(uid: String): String = "notified_ids_$uid"
    private fun dmNotifiedKey(uid: String, convId: String): String = "dm_notified_${uid}_$convId"
    private fun trayMessagesKey(uid: String, convKey: String): String = "tray_msgs_${uid}_$convKey"

    companion object {
        private const val PREFS_NAME = "libra_notification_prefs"
        private const val KEY_MASTER_ENABLED = "master_enabled"
        private const val KEY_MESSAGES_ENABLED = "messages_enabled"
        private const val KEY_SOCIAL_ENABLED = "social_enabled"
        private const val KEY_ANNOUNCEMENTS_ENABLED = "announcements_enabled"
        private const val KEY_BACKGROUND_SERVICE = "background_live_service_opt_in_v2"
        private const val KEY_PERMISSION_PROMPTED = "permission_prompted_v1"
        private const val KEY_BATTERY_PROMPTED = "battery_prompted_v1"
        private const val MAX_STORED_IDS = 500
    }
}
