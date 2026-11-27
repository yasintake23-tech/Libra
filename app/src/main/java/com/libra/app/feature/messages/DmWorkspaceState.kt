package com.libra.app.feature.messages

import android.content.Context
import androidx.compose.ui.geometry.Offset
import com.libra.app.domain.model.ServerMessage

enum class DmHubMode {
    MESSAGES,
    SERVERS
}

fun parseDmHubMode(raw: String?): DmHubMode = when (raw?.uppercase()) {
    "SERVERS" -> DmHubMode.SERVERS
    else -> DmHubMode.MESSAGES
}

/**
 * Pure unread check for a server channel:
 * A channel is unread if it has a real message sent by someone else whose timestamp
 * is strictly newer than the user's last-read watermark for that channel.
 */
fun isServerChannelUnread(
    latestMessage: ServerMessage?,
    lastReadAtMs: Long,
    currentUid: String
): Boolean {
    val msg = latestMessage ?: return false
    if (msg.createdAt <= 0L) return false
    if (currentUid.isNotBlank() && msg.senderId == currentUid) return false
    return msg.createdAt > lastReadAtMs
}

fun countUnreadServerChannels(
    latestByChannel: Map<String, ServerMessage?>,
    readAtByChannel: Map<String, Long>,
    currentUid: String
): Int = latestByChannel.entries.count { (channelId, latest) ->
    isServerChannelUnread(
        latestMessage = latest,
        lastReadAtMs = readAtByChannel[channelId] ?: 0L,
        currentUid = currentUid
    )
}

data class PortalTransitionRequest(
    val targetMode: DmHubMode,
    val originInRoot: Offset,
    val nonce: Int
)

class DmWorkspacePreferences(context: Context) {
    private val prefs = context.applicationContext
        .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    @Synchronized
    fun readHubMode(): DmHubMode =
        parseDmHubMode(prefs.getString(KEY_HUB_MODE, DmHubMode.MESSAGES.name))

    @Synchronized
    fun saveHubMode(mode: DmHubMode) {
        prefs.edit().putString(KEY_HUB_MODE, mode.name).apply()
    }

    @Synchronized
    fun readPinnedServerIds(): Set<String> =
        prefs.getStringSet(KEY_PINNED_SERVERS, emptySet()).orEmpty().toSet()

    @Synchronized
    fun savePinnedServerIds(ids: Set<String>) {
        prefs.edit().putStringSet(KEY_PINNED_SERVERS, ids).apply()
    }

    @Synchronized
    fun readChannelReadMap(uid: String, serverId: String): Map<String, Long> {
        if (uid.isBlank() || serverId.isBlank()) return emptyMap()
        val prefix = channelReadPrefix(uid, serverId)
        val result = mutableMapOf<String, Long>()
        prefs.all.forEach { (key, value) ->
            if (key.startsWith(prefix) && value is Long && value > 0L) {
                val channelId = key.removePrefix(prefix)
                if (channelId.isNotBlank()) result[channelId] = value
            }
        }
        return result
    }

    @Synchronized
    fun saveChannelReadAt(uid: String, serverId: String, channelId: String, timestampMs: Long) {
        if (uid.isBlank() || serverId.isBlank() || channelId.isBlank() || timestampMs <= 0L) return
        val key = channelReadPrefix(uid, serverId) + channelId
        val prev = prefs.getLong(key, 0L)
        if (timestampMs > prev) {
            prefs.edit().putLong(key, timestampMs).apply()
        }
    }

    private fun channelReadPrefix(uid: String, serverId: String): String =
        "ch_read_${uid}_${serverId}_"

    companion object {
        private const val PREFS_NAME = "libra_dm_workspace"
        private const val KEY_HUB_MODE = "dm_hub_mode_v1"
        private const val KEY_PINNED_SERVERS = "pinned_server_ids_v1"
    }
}
