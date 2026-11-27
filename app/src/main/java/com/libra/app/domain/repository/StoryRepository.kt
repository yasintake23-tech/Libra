package com.libra.app.domain.repository

import com.libra.app.core.result.AppResult
import com.libra.app.domain.model.Story
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

interface StoryRepository {
    suspend fun createStory(
        authorId: String,
        authorName: String,
        authorPhotoUrl: String,
        text: String,
        mediaUrl: String,
        mediaType: String
    ): AppResult<Unit>

    suspend fun getActiveStories(authorIds: Set<String>): AppResult<List<Story>>

    suspend fun isLiked(storyId: String, userId: String): AppResult<Boolean>

    suspend fun toggleLike(storyId: String, userId: String): AppResult<Boolean>

    fun observeAllStoriesForAdmin(limit: Long = 250): Flow<AppResult<List<Story>>> =
        flowOf(AppResult.Success(emptyList()))

    suspend fun deleteStory(storyId: String, userId: String): AppResult<Unit> =
        AppResult.Success(Unit)
}
