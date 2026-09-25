package com.libra.app.data.story

import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.libra.app.core.result.AppError
import com.libra.app.core.result.AppResult
import com.libra.app.domain.model.Story
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
        if (authorId.isBlank()) return AppResult.Error(AppError.Auth("Oturum bulunamadı."))
        if (cleanText.isBlank() && mediaUrl.isBlank()) return AppResult.Error(AppError.Validation("Hikâye boş olamaz."))
        if (cleanText.length > 500) return AppResult.Error(AppError.Validation("Hikâye metni en fazla 500 karakter olabilir."))
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
                    "expiresAt" to now + 24L * 60L * 60L * 1000L,
                    "likesCount" to 0
                )
            ).await()
            AppResult.Success(Unit)
        } catch (e: Exception) {
            AppResult.Error(AppError.Database("Hikâye paylaşılırken hata oluştu.", e))
        }
    }

    override suspend fun getActiveStories(authorIds: Set<String>): AppResult<List<Story>> {
        if (authorIds.isEmpty()) return AppResult.Success(emptyList())
        return try {
            val now = System.currentTimeMillis()

            // Query only by expiry so story loading does not depend on a
            // Firestore composite index for authorId + expiresAt.
            val stories = storiesRef
                .whereGreaterThan("expiresAt", now)
                .limit(200)
                .get()
                .await()
                .documents
                .mapNotNull { doc ->
                    doc.toObject(Story::class.java)?.copy(id = doc.id)
                }
                .filter { it.authorId in authorIds }
                .sortedBy { it.createdAt }

            AppResult.Success(stories)
        } catch (e: Exception) {
            AppResult.Error(AppError.Database("Hikâyeler yüklenemedi.", e))
        }
    }

    override suspend fun isLiked(storyId: String, userId: String): AppResult<Boolean> {
        if (storyId.isBlank() || userId.isBlank()) return AppResult.Success(false)
        return try {
            AppResult.Success(storiesRef.document(storyId).collection("likes").document(userId).get().await().exists())
        } catch (e: Exception) {
            AppResult.Error(AppError.Database("Hikâye beğenisi okunamadı.", e))
        }
    }

    override suspend fun toggleLike(storyId: String, userId: String): AppResult<Boolean> {
        if (storyId.isBlank() || userId.isBlank()) return AppResult.Error(AppError.Validation("Geçersiz hikâye."))
        return try {
            val likeRef = storiesRef.document(storyId).collection("likes").document(userId)
            val storyRef = storiesRef.document(storyId)
            val liked = firestore.runTransaction { tx ->
                val like = tx.get(likeRef)
                if (like.exists()) {
                    tx.delete(likeRef)
                    tx.update(storyRef, "likesCount", FieldValue.increment(-1))
                    false
                } else {
                    tx.set(likeRef, mapOf("userId" to userId, "createdAt" to System.currentTimeMillis()))
                    tx.update(storyRef, "likesCount", FieldValue.increment(1))
                    true
                }
            }.await()
            AppResult.Success(liked)
        } catch (e: Exception) {
            AppResult.Error(AppError.Database("Hikâye beğenisi güncellenemedi.", e))
        }
    }
}
