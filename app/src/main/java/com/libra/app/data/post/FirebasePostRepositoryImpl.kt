package com.libra.app.data.post

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.libra.app.core.result.AppError
import com.libra.app.core.result.AppResult
import com.libra.app.domain.model.Post
import com.libra.app.domain.repository.PostRepository
import com.libra.app.domain.repository.UserRepository
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class FirebasePostRepositoryImpl(
    private val userRepository: UserRepository
) : PostRepository {

    private val firestore = FirebaseFirestore.getInstance()
    private val postsRef get() = firestore.collection("posts")

    override fun observeFeed(currentUserId: String, limit: Long): Flow<AppResult<List<Post>>> = callbackFlow {
        val query = postsRef.orderBy("createdAt", Query.Direction.DESCENDING).limit(limit)
        val registration: ListenerRegistration = query.addSnapshotListener { snapshot, error ->
            if (error != null) {
                trySend(AppResult.Error(AppError.Database("Ana akış yüklenemedi.", error)))
                return@addSnapshotListener
            }

            val documents = snapshot?.documents.orEmpty()
            kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
                try {
                    val posts = documents.mapNotNull { document ->
                        val data = document.data ?: return@mapNotNull null
                        Post(
                            id = document.id,
                            authorId = data["authorId"] as? String ?: "",
                            authorName = data["authorName"] as? String ?: "",
                            authorUsername = data["authorUsername"] as? String ?: "",
                            authorPhotoUrl = data["authorPhotoUrl"] as? String ?: "",
                            text = data["text"] as? String ?: "",
                            likesCount = (data["likesCount"] as? Number)?.toInt() ?: 0,
                            likedByCurrentUser = runCatching {
                                document.reference.collection("likes").document(currentUserId).get().await().exists()
                            }.getOrDefault(false),
                            createdAt = (data["createdAt"] as? Number)?.toLong() ?: 0L
                        )
                    }
                    trySend(AppResult.Success(posts))
                } catch (e: Exception) {
                    trySend(AppResult.Error(AppError.Database("Gönderiler çözümlenemedi.", e)))
                }
            }
        }
        awaitClose { registration.remove() }
    }

    override suspend fun createPost(authorId: String, text: String): AppResult<Post> {
        val cleanText = text.trim()
        if (authorId.isBlank()) return AppResult.Error(AppError.Auth("Oturum bulunamadı."))
        if (cleanText.isBlank()) return AppResult.Error(AppError.Validation("Gönderi boş olamaz."))
        if (cleanText.length > 1000) return AppResult.Error(AppError.Validation("Gönderi en fazla 1000 karakter olabilir."))

        return try {
            val profileResult = userRepository.getUserProfileFresh(authorId)
            val profile = (profileResult as? AppResult.Success)?.data
                ?: return AppResult.Error(AppError.Database("Profil bilgileri alınamadı."))

            val post = Post(
                id = postsRef.document().id,
                authorId = authorId,
                authorName = profile.displayName,
                authorUsername = profile.username,
                authorPhotoUrl = profile.profileImageUrl,
                text = cleanText,
                createdAt = System.currentTimeMillis()
            )

            postsRef.document(post.id).set(
                mapOf(
                    "authorId" to post.authorId,
                    "authorName" to post.authorName,
                    "authorUsername" to post.authorUsername,
                    "authorPhotoUrl" to post.authorPhotoUrl,
                    "text" to post.text,
                    "likesCount" to 0,
                    "createdAt" to post.createdAt
                )
            ).await()

            AppResult.Success(post)
        } catch (e: Exception) {
            AppResult.Error(AppError.Database("Gönderi paylaşılamadı.", e))
        }
    }

    override suspend fun toggleLike(postId: String, userId: String): AppResult<Boolean> {
        if (postId.isBlank() || userId.isBlank()) return AppResult.Error(AppError.Auth("Oturum bulunamadı."))

        return try {
            val postRef = postsRef.document(postId)
            val likeRef = postRef.collection("likes").document(userId)
            var liked = false

            firestore.runTransaction { transaction ->
                val postSnapshot = transaction.get(postRef)
                if (!postSnapshot.exists()) throw IllegalStateException("Gönderi bulunamadı.")

                val likeSnapshot = transaction.get(likeRef)
                val currentCount = (postSnapshot.getLong("likesCount") ?: 0L).coerceAtLeast(0L)

                if (likeSnapshot.exists()) {
                    transaction.delete(likeRef)
                    transaction.update(postRef, "likesCount", (currentCount - 1L).coerceAtLeast(0L))
                    liked = false
                } else {
                    transaction.set(likeRef, mapOf("userId" to userId, "createdAt" to System.currentTimeMillis()))
                    transaction.update(postRef, "likesCount", currentCount + 1L)
                    liked = true
                }
                null
            }.await()

            AppResult.Success(liked)
        } catch (e: Exception) {
            AppResult.Error(AppError.Database("Beğeni işlemi tamamlanamadı.", e))
        }
    }

    override suspend fun deletePost(postId: String, userId: String): AppResult<Unit> {
        return try {
            val ref = postsRef.document(postId)
            val snapshot = ref.get().await()
            if (!snapshot.exists()) return AppResult.Success(Unit)
            if (snapshot.getString("authorId") != userId) {
                return AppResult.Error(AppError.Auth("Bu gönderiyi silme yetkin yok."))
            }
            ref.delete().await()
            AppResult.Success(Unit)
        } catch (e: Exception) {
            AppResult.Error(AppError.Database("Gönderi silinemedi.", e))
        }
    }
}
