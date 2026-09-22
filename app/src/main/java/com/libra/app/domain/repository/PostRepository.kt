package com.libra.app.domain.repository

import com.libra.app.core.result.AppResult
import com.libra.app.domain.model.Post
import kotlinx.coroutines.flow.Flow

interface PostRepository {
    fun observeFeed(currentUserId: String, limit: Long = 50): Flow<AppResult<List<Post>>>
    suspend fun createPost(authorId: String, text: String): AppResult<Post>
    suspend fun toggleLike(postId: String, userId: String): AppResult<Boolean>
    suspend fun deletePost(postId: String, userId: String): AppResult<Unit>
}
