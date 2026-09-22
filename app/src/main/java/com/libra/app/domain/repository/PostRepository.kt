package com.libra.app.domain.repository

import com.libra.app.core.result.AppResult
import com.libra.app.domain.model.Post
import com.libra.app.domain.model.PostComment
import kotlinx.coroutines.flow.Flow

interface PostRepository {
    fun observeFeed(currentUserId: String, limit: Long = 50): Flow<AppResult<List<Post>>>
    suspend fun createPost(
        authorId: String,
        text: String,
        title: String = "",
        mediaUrl: String = "",
        mediaType: String = "",
        tags: List<String> = emptyList()
    ): AppResult<Post>
    suspend fun toggleLike(postId: String, userId: String): AppResult<Boolean>
    suspend fun deletePost(postId: String, userId: String): AppResult<Unit>
    fun observeComments(postId: String, limit: Long = 100): Flow<AppResult<List<PostComment>>>
    suspend fun addComment(postId: String, authorId: String, text: String): AppResult<PostComment>
    suspend fun deleteComment(postId: String, commentId: String, userId: String): AppResult<Unit>
    suspend fun toggleSave(postId: String, userId: String): AppResult<Boolean>
    fun observeSavedPosts(userId: String, limit: Long = 100): Flow<AppResult<List<Post>>>
}
