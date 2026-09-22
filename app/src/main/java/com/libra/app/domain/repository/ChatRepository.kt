package com.libra.app.domain.repository

import com.libra.app.core.result.AppResult
import com.libra.app.domain.model.DirectConversation
import com.libra.app.domain.model.DirectMessage
import com.libra.app.domain.model.GlobalChatMessage
import kotlinx.coroutines.flow.Flow

interface ChatRepository {
    fun observeGlobalMessages(limit: Long = 100): Flow<AppResult<List<GlobalChatMessage>>>
    suspend fun sendGlobalMessage(text: String): AppResult<Unit>
    fun observeDirectConversations(uid: String): Flow<AppResult<List<DirectConversation>>>
    fun observeDirectMessages(conversationId: String, limit: Long = 100): Flow<AppResult<List<DirectMessage>>>
    suspend fun sendDirectMessage(recipientId: String, text: String): AppResult<Unit>
}
