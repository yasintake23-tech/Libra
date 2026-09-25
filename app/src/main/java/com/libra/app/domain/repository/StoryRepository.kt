package com.libra.app.domain.repository

import com.libra.app.core.result.AppResult
import com.libra.app.domain.model.Story

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

    suspend fun toggleLike(storyId: String, userId: String): AppResult<Boolean>
}
