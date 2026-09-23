package com.libra.app.domain.repository

import com.libra.app.core.result.AppResult

interface StoryRepository {
    suspend fun createStory(
        authorId: String,
        authorName: String,
        authorPhotoUrl: String,
        text: String,
        mediaUrl: String,
        mediaType: String
    ): AppResult<Unit>
}
