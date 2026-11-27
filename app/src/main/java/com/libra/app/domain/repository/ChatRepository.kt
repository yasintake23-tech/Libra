package com.libra.app.domain.repository

import com.libra.app.core.result.AppResult
import com.libra.app.domain.model.DirectConversation
import com.libra.app.domain.model.DirectMessage
import com.libra.app.domain.model.GlobalChatMessage
import com.libra.app.domain.model.GroupPermissions
import com.libra.app.domain.model.ServerMessage
import com.libra.app.domain.model.ReactionUser
import com.libra.app.domain.model.SharedContent
import kotlinx.coroutines.flow.Flow

/**
 * Realtime messaging contract.
 *
 * Community server metadata/membership intentionally lives in CommunityRepository.
 * This repository is the single application boundary for live chat messages.
 */
interface ChatRepository {
    fun observeGlobalMessages(limit: Long = 100): Flow<AppResult<List<GlobalChatMessage>>>
    suspend fun sendGlobalMessage(text: String, replyTo: GlobalChatMessage? = null, sharedContent: SharedContent? = null): AppResult<Unit>
    suspend fun sendGlobalMediaMessage(mediaUrl: String, mediaType: String, text: String = "", replyTo: GlobalChatMessage? = null, sharedContent: SharedContent? = null): AppResult<Unit>
    suspend fun editGlobalMessage(messageId: String, text: String): AppResult<Unit>
    suspend fun deleteGlobalMessage(messageId: String): AppResult<Unit>
    suspend fun deleteGlobalMessages(messageIds: List<String>): AppResult<Unit> {
        messageIds.distinct().forEach { id ->
            val res = deleteGlobalMessage(id)
            if (res is AppResult.Error) return res
        }
        return AppResult.Success(Unit)
    }
    suspend fun toggleGlobalMessageReaction(messageId: String, emoji: String): AppResult<Unit>

    fun observeDirectConversations(uid: String): Flow<AppResult<List<DirectConversation>>>
    fun observeDirectMessages(conversationId: String, limit: Long = 100): Flow<AppResult<List<DirectMessage>>>
    suspend fun sendDirectMessage(recipientId: String, text: String, replyTo: DirectMessage? = null, sharedContent: SharedContent? = null): AppResult<Unit>
    suspend fun sendDirectMediaMessage(recipientId: String, mediaUrl: String, mediaType: String, text: String = "", replyTo: DirectMessage? = null, sharedContent: SharedContent? = null): AppResult<Unit>
    suspend fun editDirectMessage(conversationId: String, messageId: String, text: String): AppResult<Unit>
    suspend fun deleteDirectMessage(conversationId: String, messageId: String): AppResult<Unit>
    suspend fun deleteDirectMessages(conversationId: String, messageIds: List<String>): AppResult<Unit> {
        messageIds.distinct().forEach { id ->
            val res = deleteDirectMessage(conversationId, id)
            if (res is AppResult.Error) return res
        }
        return AppResult.Success(Unit)
    }
    suspend fun clearDirectConversation(conversationId: String, removeFromInbox: Boolean = false): AppResult<Unit> =
        if (removeFromInbox) hideDirectConversation(conversationId) else AppResult.Success(Unit)
    suspend fun toggleDirectMessageReaction(conversationId: String, messageId: String, emoji: String): AppResult<Unit>
    suspend fun markDirectConversationRead(conversationId: String): AppResult<Unit>
    suspend fun hideDirectConversation(conversationId: String): AppResult<Unit> = AppResult.Success(Unit)
    suspend fun createGroupConversation(name: String, participantIds: List<String>): AppResult<DirectConversation>
    suspend fun createGroupConversationRich(
        name: String,
        participantIds: List<String>,
        description: String = "",
        photoUrl: String = "",
        bannerUrl: String = "",
        permissions: GroupPermissions = GroupPermissions()
    ): AppResult<DirectConversation> = createGroupConversation(name, participantIds)
    suspend fun sendGroupMessage(conversationId: String, text: String, replyTo: DirectMessage? = null, sharedContent: SharedContent? = null): AppResult<Unit>
    suspend fun sendGroupMediaMessage(conversationId: String, mediaUrl: String, mediaType: String, text: String = "", replyTo: DirectMessage? = null, sharedContent: SharedContent? = null): AppResult<Unit>
    suspend fun updateGroupInfo(conversationId: String, name: String, description: String): AppResult<Unit> = AppResult.Success(Unit)
    suspend fun updateGroupMedia(conversationId: String, photoUrl: String, bannerUrl: String): AppResult<Unit> = AppResult.Success(Unit)
    suspend fun updateGroupPermissions(conversationId: String, permissions: GroupPermissions): AppResult<Unit> = AppResult.Success(Unit)
    suspend fun addGroupMembers(conversationId: String, userIds: List<String>): AppResult<Unit> = AppResult.Success(Unit)
    suspend fun removeGroupMember(conversationId: String, targetUid: String): AppResult<Unit> = AppResult.Success(Unit)
    suspend fun setGroupMemberAdmin(conversationId: String, targetUid: String, isAdmin: Boolean): AppResult<Unit> = AppResult.Success(Unit)

    fun observeServerMessages(serverId: String, limit: Long = 100, channelId: String = ""): Flow<AppResult<List<ServerMessage>>>
    fun observeServerChannelLatestMessage(serverId: String, channelId: String): Flow<ServerMessage?> = kotlinx.coroutines.flow.flowOf(null)
    fun observeServerChannelReadTimestamps(uid: String, serverId: String): Flow<Map<String, Long>> = kotlinx.coroutines.flow.flowOf(emptyMap())
    suspend fun markServerChannelRead(serverId: String, channelId: String, readAtMs: Long = System.currentTimeMillis()): AppResult<Unit> = AppResult.Success(Unit)
    suspend fun sendServerMessage(serverId: String, text: String, replyTo: ServerMessage? = null, sharedContent: SharedContent? = null, channelId: String = ""): AppResult<Unit>
    suspend fun sendServerMediaMessage(serverId: String, mediaUrl: String, mediaType: String, text: String = "", replyTo: ServerMessage? = null, sharedContent: SharedContent? = null, channelId: String = ""): AppResult<Unit>
    suspend fun editServerMessage(serverId: String, messageId: String, text: String, channelId: String = ""): AppResult<Unit>
    suspend fun deleteServerMessage(serverId: String, messageId: String, channelId: String = ""): AppResult<Unit>
    suspend fun deleteServerMessages(serverId: String, messageIds: List<String>, channelId: String = ""): AppResult<Unit> {
        messageIds.distinct().forEach { id ->
            val res = deleteServerMessage(serverId, id, channelId)
            if (res is AppResult.Error) return res
        }
        return AppResult.Success(Unit)
    }
    suspend fun toggleServerMessageReaction(serverId: String, messageId: String, emoji: String, channelId: String = ""): AppResult<Unit>
    suspend fun getServerMessageReactionUsers(serverId: String, messageId: String, emoji: String, channelId: String = ""): AppResult<List<ReactionUser>>
}
