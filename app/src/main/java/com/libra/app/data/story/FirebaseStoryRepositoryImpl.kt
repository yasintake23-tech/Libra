package com.libra.app.data.story

import com.google.firebase.firestore.FirebaseFirestore
import com.libra.app.core.result.AppError
import com.libra.app.core.result.AppResult
import com.libra.app.domain.repository.StoryRepository
import kotlinx.coroutines.tasks.await

class FirebaseStoryRepositoryImpl : StoryRepository {

    private val firestore = FirebaseFirestore.getInstance()
    private val storiesRef get() = firestore.collection("stories")

    override suspend fun createStory(
        authorId: String,
        authorName: String,
        authorPhotoUrl: String,
        text: String,
        mediaUrl: String,
        mediaType: String
    ): AppResult<Unit> {
        val cleanText = text.trim()
        if (authorId.isBlank()) {
            return AppResult.Error(AppError.Auth("Oturum bulunamadı."))
        }
        if (cleanText.isBlank() && mediaUrl.isBlank()) {
            return AppResult.Error(AppError.Validation("Hikâye boş olamaz."))
        }
        if (cleanText.length > 500) {
            return AppResult.Error(AppError.Validation("Hikâye metni en fazla 500 karakter olabilir."))
        }
        if (mediaUrl.isNotBlank() && mediaType != "image") {
            return AppResult.Error(AppError.Validation("Şimdilik yalnızca fotoğraf hikâyeleri destekleniyor."))
        }

        return try {
            val now = System.currentTimeMillis()
            storiesRef.add(
                mapOf(
                    "authorId" to authorId,
                    "authorName" to authorName.trim(),
                    "authorPhotoUrl" to authorPhotoUrl.trim(),
                    "text" to cleanText,
                    "mediaUrl" to mediaUrl.trim(),
                    "mediaType" to mediaType.trim(),
                    "createdAt" to now,
                    "expiresAt" to now + 24L * 60L * 60L * 1000L
                )
            ).await()
            AppResult.Success(Unit)
        } catch (e: Exception) {
            AppResult.Error(AppError.Database("Hikâye paylaşılırken hata oluştu.", e))
        }
    }
}
