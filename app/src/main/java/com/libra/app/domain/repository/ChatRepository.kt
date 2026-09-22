package com.libra.app.domain.repository

import com.libra.app.core.result.AppResult
import com.libra.app.domain.model.DirectConversation
import com.libra.app.domain.model.DirectMessage
import com.libra.app.domain.model.GlobalChatMessage
import com.libra.app.domain.model.ServerMember
import kotlinx.coroutines.flow.Flow

interface ChatRepository {
    fun observeGlobalMessages(limit: Long = 100): Flow<AppResult<List<GlobalChatMessage>>>
    suspend fun sendGlobalMessage(text: String): AppResult<Unit>
    fun observeDirectConversations(uid: String): Flow<AppResult<List<DirectConversation>>>
    fun observeDirectMessages(conversationId: String, limit: Long = 100): Flow<AppResult<List<DirectMessage>>>
    suspend fun sendDirectMessage(recipientId: String, text: String): AppResult<Unit>
    suspend fun sendDirectMediaMessage(recipientId: String, mediaUrl: String, mediaType: String): AppResult<Unit>
    suspend fun markDirectConversationRead(conversationId: String): AppResult<Unit>
    fun observeCommunityServers(): Flow<AppResult<List<com.libra.app.domain.model.CommunityServer>>>
    suspend fun createCommunityServer(name: String, description: String): AppResult<com.libra.app.domain.model.CommunityServer>
    suspend fun joinCommunityServer(serverId: String): AppResult<Unit>
    fun observeServerMembers(serverId: String): Flow<AppResult<List<ServerMember>>>
    suspend fun setServerMemberRole(serverId: String, memberId: String, role: String): AppResult<Unit>
    suspend fun removeServerMember(serverId: String, memberId: String): AppResult<Unit>
    suspend fun leaveCommunityServer(serverId: String): AppResult<Unit>
    fun observeServerMessages(serverId: String, limit: Long = 100): Flow<AppResult<List<com.libra.app.domain.model.ServerMessage>>>
    suspend fun sendServerMessage(serverId: String, text: String): AppResult<Unit>
}
