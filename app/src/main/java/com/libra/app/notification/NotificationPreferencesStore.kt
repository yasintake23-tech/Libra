package com.libra.app.notification

import android.content.Context
import android.content.SharedPreferences
import com.libra.app.domain.model.AppNotification
import com.libra.app.domain.model.LibraNotificationTypes

enum class NotificationPreferenceCategory {
    MESSAGES,
    SOCIAL,
    ANNOUNCEMENTS
}

data class NotificationPreferencesState(
    val masterEnabled: Boolean = true,
    val messagesEnabled: Boolean = true,
    val socialEnabled: Boolean = true,
    val announcementsEnabled: Boolean = true
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
        val isBeforeBaselineWindow = hasBaseline && item.createdAt > 0L && item.createdAt < (effectiveBaseline - 60_000L)

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

fun shouldSuppressActiveNotification(
    notification: AppNotification,
    isNotificationsScreenOpen: Boolean,
    activeDirectConversationId: String?
): Boolean {
    if (isNotificationsScreenOpen) return true
    if (activeDirectConversationId.isNullOrBlank()) return false
    val isChatType = notification.type == LibraNotificationTypes.MESSAGE ||
        notification.type == LibraNotificationTypes.GROUP_MESSAGE
    return isChatType && notification.referenceId == activeDirectConversationId
}

class NotificationPreferencesStore(context: Context) {
    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun readPreferences(): NotificationPreferencesState = NotificationPreferencesState(
        masterEnabled = prefs.getBoolean(KEY_MASTER_ENABLED, true),
        messagesEnabled = prefs.getBoolean(KEY_MESSAGES_ENABLED, true),
        socialEnabled = prefs.getBoolean(KEY_SOCIAL_ENABLED, true),
        announcementsEnabled = prefs.getBoolean(KEY_ANNOUNCEMENTS_ENABLED, true)
    )

    fun savePreferences(state: NotificationPreferencesState) {
        prefs.edit()
            .putBoolean(KEY_MASTER_ENABLED, state.masterEnabled)
            .putBoolean(KEY_MESSAGES_ENABLED, state.messagesEnabled)
            .putBoolean(KEY_SOCIAL_ENABLED, state.socialEnabled)
            .putBoolean(KEY_ANNOUNCEMENTS_ENABLED, state.announcementsEnabled)
            .apply()
    }

    fun wasPermissionPrompted(): Boolean = prefs.getBoolean(KEY_PERMISSION_PROMPTED, false)

    fun markPermissionPrompted() {
        prefs.edit().putBoolean(KEY_PERMISSION_PROMPTED, true).apply()
    }

    fun getBaselineTimestamp(uid: String): Long =
        prefs.getLong(baselineKey(uid), 0L)

    fun hasBaseline(uid: String): Boolean =
        prefs.contains(baselineKey(uid))

    fun getNotifiedIds(uid: String): Set<String> {
        val raw = prefs.getString(notifiedIdsKey(uid), "").orEmpty()
        if (raw.isBlank()) return emptySet()
        return raw.split(",").filter { it.isNotBlank() }.toSet()
    }

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
        prefs.edit()
            .putLong(baselineKey(uid), selection.nextBaselineMs)
            .putString(notifiedIdsKey(uid), capped.joinToString(","))
            .apply()
    }

    private fun baselineKey(uid: String): String = "baseline_ms_$uid"
    private fun notifiedIdsKey(uid: String): String = "notified_ids_$uid"

    companion object {
        private const val PREFS_NAME = "libra_notification_prefs"
        private const val KEY_MASTER_ENABLED = "master_enabled"
        private const val KEY_MESSAGES_ENABLED = "messages_enabled"
        private const val KEY_SOCIAL_ENABLED = "social_enabled"
        private const val KEY_ANNOUNCEMENTS_ENABLED = "announcements_enabled"
        private const val KEY_PERMISSION_PROMPTED = "permission_prompted_v1"
        private const val MAX_STORED_IDS = 250
    }
}
