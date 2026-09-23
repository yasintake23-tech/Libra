package com.libra.app.domain.repository

import com.libra.app.core.result.AppResult
import com.libra.app.domain.model.DirectConversation
import com.libra.app.domain.model.DirectMessage
import com.libra.app.domain.model.GlobalChatMessage
import com.libra.app.domain.model.ServerMessage
import kotlinx.coroutines.flow.Flow

/**
 * Realtime messaging contract.
 *
 * Community server metadata/membership intentionally lives in CommunityRepository.
 * This repository is the single application boundary for live chat messages.
 */
interface ChatRepository {
    fun observeGlobalMessages(limit: Long = 100): Flow<AppResult<List<GlobalChatMessage>>>
    suspend fun sendGlobalMessage(text: String, replyTo: GlobalChatMessage? = null): AppResult<Unit>
    suspend fun sendGlobalMediaMessage(mediaUrl: String, mediaType: String, text: String = "", replyTo: GlobalChatMessage? = null): AppResult<Unit>
    suspend fun editGlobalMessage(messageId: String, text: String): AppResult<Unit>
    suspend fun deleteGlobalMessage(messageId: String): AppResult<Unit>
    suspend fun toggleGlobalMessageReaction(messageId: String, emoji: String): AppResult<Unit>

    fun observeDirectConversations(uid: String): Flow<AppResult<List<DirectConversation>>>
    fun observeDirectMessages(conversationId: String, limit: Long = 100): Flow<AppResult<List<DirectMessage>>>
    suspend fun sendDirectMessage(recipientId: String, text: String, replyTo: DirectMessage? = null): AppResult<Unit>
    suspend fun sendDirectMediaMessage(recipientId: String, mediaUrl: String, mediaType: String, text: String = "", replyTo: DirectMessage? = null): AppResult<Unit>
    suspend fun editDirectMessage(conversationId: String, messageId: String, text: String): AppResult<Unit>
    suspend fun deleteDirectMessage(conversationId: String, messageId: String): AppResult<Unit>
    suspend fun toggleDirectMessageReaction(conversationId: String, messageId: String, emoji: String): AppResult<Unit>
    suspend fun markDirectConversationRead(conversationId: String): AppResult<Unit>

    fun observeServerMessages(serverId: String, limit: Long = 100): Flow<AppResult<List<ServerMessage>>>
    suspend fun sendServerMessage(serverId: String, text: String, replyTo: ServerMessage? = null): AppResult<Unit>
    suspend fun sendServerMediaMessage(serverId: String, mediaUrl: String, mediaType: String, text: String = "", replyTo: ServerMessage? = null): AppResult<Unit>
    suspend fun editServerMessage(serverId: String, messageId: String, text: String): AppResult<Unit>
    suspend fun deleteServerMessage(serverId: String, messageId: String): AppResult<Unit>
    suspend fun toggleServerMessageReaction(serverId: String, messageId: String, emoji: String): AppResult<Unit>
}
