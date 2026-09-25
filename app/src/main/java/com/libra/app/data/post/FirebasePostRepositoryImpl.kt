package com.libra.app.data.post

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.AggregateSource
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.libra.app.core.result.AppError
import com.libra.app.core.result.AppResult
import com.libra.app.domain.model.Post
import com.libra.app.domain.model.PostComment
import com.libra.app.domain.repository.PostRepository
import com.libra.app.domain.repository.NotificationRepository
import com.libra.app.domain.repository.StorageRepository
import com.libra.app.domain.repository.UserRepository
import com.libra.app.domain.model.AppNotification
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class FirebasePostRepositoryImpl(
    private val userRepository: UserRepository,
    private val storageRepository: StorageRepository,
    private val notificationRepository: NotificationRepository
) : PostRepository {

    private val firestore = FirebaseFirestore.getInstance()
    private val postsRef get() = firestore.collection("posts")

    override fun observeFeed(currentUserId: String, limit: Long): Flow<AppResult<List<Post>>> = callbackFlow {
        if (currentUserId.isBlank()) {
            trySend(AppResult.Error(AppError.Auth("Oturum bulunamadı.")))
            close()
            return@callbackFlow
        }

        val safeLimit = limit.coerceIn(1L, 100L)
        val registration = postsRef
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(safeLimit)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(AppResult.Error(AppError.Database("Ana akış yüklenemedi.", error)))
                    return@addSnapshotListener
                }

                launch {
                    try {
                        // The home feed is a public social feed. Do not re-filter it by
                        // follows on the client, otherwise a new user can only see
                        // their own posts. Like/save state is loaded separately so a
                        // single broken secondary query cannot hide the whole feed.
                        val likedIds = runCatching {
                            firestore.collectionGroup("likes")
                                .whereEqualTo("userId", currentUserId)
                                .get()
                                .await()
                                .documents
                                .mapNotNull { it.reference.parent.parent?.id }
                                .toSet()
                        }.getOrDefault(emptySet())

                        val savedIds = runCatching {
                            firestore.collection("savedPosts")
                                .document(currentUserId)
                                .collection("posts")
                                .get()
                                .await()
                                .documents
                                .mapNotNull { it.getString("postId") ?: it.id }
                                .toSet()
                        }.getOrDefault(emptySet())

                        val posts = snapshot?.documents.orEmpty().mapNotNull { document ->
                            val data = document.data ?: return@mapNotNull null
                            Post(
                                id = document.id,
                                authorId = data["authorId"] as? String ?: "",
                                authorName = data["authorName"] as? String ?: "",
                                authorUsername = data["authorUsername"] as? String ?: "",
                                authorPhotoUrl = data["authorPhotoUrl"] as? String ?: "",
                                title = data["title"] as? String ?: "",
                                text = data["text"] as? String ?: "",
                                mediaUrl = data["mediaUrl"] as? String ?: "",
                                mediaType = data["mediaType"] as? String ?: "",
                                tags = (data["tags"] as? List<*>)?.filterIsInstance<String>().orEmpty(),
                                likesCount = (data["likesCount"] as? Number)?.toInt()?.coerceAtLeast(0) ?: 0,
                                commentsCount = (data["commentsCount"] as? Number)?.toInt()?.coerceAtLeast(0) ?: runCatching {
                                    document.reference.collection("comments").count().get(AggregateSource.SERVER).await().count.toInt()
                                }.getOrDefault(0),
                                likedByCurrentUser = document.id in likedIds,
                                savedByCurrentUser = document.id in savedIds,
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

    override suspend fun createPost(
        authorId: String,
        text: String,
        title: String,
        mediaUrl: String,
        mediaType: String,
        tags: List<String>
    ): AppResult<Post> {
        val cleanTitle = title.trim()
        val cleanText = text.trim()
        val cleanTags = tags.map { it.trim() }.filter { it.isNotBlank() }.distinct().take(10)
        if (authorId.isBlank()) return AppResult.Error(AppError.Auth("Oturum bulunamadı."))
        if (cleanText.isBlank()) return AppResult.Error(AppError.Validation("Gönderi metni boş olamaz."))
        if (cleanText.length > 2000) return AppResult.Error(AppError.Validation("Gönderi en fazla 2000 karakter olabilir."))
        if (cleanTitle.length > 120) return AppResult.Error(AppError.Validation("Başlık en fazla 120 karakter olabilir."))
        if (mediaUrl.isNotBlank() && mediaType != "image") return AppResult.Error(AppError.Validation("Şimdilik yalnızca fotoğraf paylaşılabilir."))

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
                title = cleanTitle,
                text = cleanText,
                mediaUrl = mediaUrl.trim(),
                mediaType = mediaType.trim(),
                tags = cleanTags,
                createdAt = System.currentTimeMillis()
            )

            postsRef.document(post.id).set(
                mapOf(
                    "authorId" to post.authorId,
                    "authorName" to post.authorName,
                    "authorUsername" to post.authorUsername,
                    "authorPhotoUrl" to post.authorPhotoUrl,
                    "title" to post.title,
                    "text" to post.text,
                    "mediaUrl" to post.mediaUrl,
                    "mediaType" to post.mediaType,
                    "tags" to post.tags,
                    "likesCount" to 0,
                    "commentsCount" to 0,
                    "createdAt" to post.createdAt
                )
            ).await()

            AppResult.Success(post)
        } catch (e: Exception) {
            if (mediaUrl.isNotBlank()) {
                runCatching { storageRepository.deleteMedia(mediaUrl) }
            }
            AppResult.Error(AppError.Database("Gönderi paylaşılamadı.", e))
        }
    }

    override suspend fun toggleLike(postId: String, userId: String): AppResult<Boolean> {
        val user = FirebaseAuth.getInstance().currentUser
            ?: return AppResult.Error(AppError.Auth("Beğenmek için giriş yapmalısın."))
        if (postId.isBlank() || user.uid != userId) {
            return AppResult.Error(AppError.Auth("Oturum bilgisi geçersiz."))
        }

        return try {
            val postRef = postsRef.document(postId)
            val likeRef = postRef.collection("likes").document(user.uid)

            var nowLiked = false
            firestore.runTransaction { transaction ->
                val postSnapshot = transaction.get(postRef)
                if (!postSnapshot.exists()) {
                    throw IllegalStateException("Gönderi bulunamadı.")
                }

                val likeSnapshot = transaction.get(likeRef)
                val currentCount = (postSnapshot.getLong("likesCount") ?: 0L).coerceAtLeast(0L)

                if (likeSnapshot.exists()) {
                    transaction.delete(likeRef)
                    transaction.update(postRef, "likesCount", (currentCount - 1L).coerceAtLeast(0L))
                    nowLiked = false
                } else {
                    transaction.set(
                        likeRef,
                        mapOf(
                            "userId" to user.uid,
                            "createdAt" to System.currentTimeMillis()
                        )
                    )
                    transaction.update(postRef, "likesCount", currentCount + 1L)
                    nowLiked = true
                }
                null
            }.await()

            if (nowLiked) {
                val recipientId = postsRef.document(postId).get().await().getString("authorId").orEmpty()
                if (recipientId.isNotBlank() && recipientId != user.uid) {
                    runCatching {
                        val actor = (userRepository.getUserProfileFresh(user.uid) as? AppResult.Success)?.data
                        if (actor != null) {
                            notificationRepository.create(
                                AppNotification(
                                    recipientId = recipientId,
                                    actorId = user.uid,
                                    actorName = actor.displayName,
                                    actorUsername = actor.username,
                                    actorPhotoUrl = actor.profileImageUrl,
                                    type = "LIKE",
                                    title = "Yeni beğeni",
                                    body = actor.displayName + " gönderini beğendi.",
                                    referenceId = postId,
                                    createdAt = System.currentTimeMillis()
                                )
                            )
                        }
                    }
                }
            }

            AppResult.Success(nowLiked)
        } catch (e: Exception) {
            AppResult.Error(AppError.Database("Beğeni işlemi başarısız.", e))
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
            runCatching {
                snapshot.getString("mediaUrl").orEmpty().takeIf { it.isNotBlank() }?.let {
                    storageRepository.deleteMedia(it)
                }
            }
            AppResult.Success(Unit)
        } catch (e: Exception) {
            AppResult.Error(AppError.Database("Gönderi silinemedi.", e))
        }
    }
    override fun observeComments(postId: String, limit: Long): Flow<AppResult<List<PostComment>>> = callbackFlow {
        if (postId.isBlank()) {
            trySend(AppResult.Success(emptyList()))
            close()
            return@callbackFlow
        }
        val registration = postsRef.document(postId).collection("comments")
            .orderBy("createdAt", Query.Direction.ASCENDING)
            .limit(limit)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(AppResult.Error(AppError.Database("Yorumlar yüklenemedi.", error)))
                    return@addSnapshotListener
                }
                val comments = snapshot?.documents.orEmpty().mapNotNull { doc ->
                    runCatching {
                        doc.toObject(PostComment::class.java)?.copy(id = doc.id, postId = postId)
                    }.getOrNull()
                }
                trySend(AppResult.Success(comments))
            }
        awaitClose { registration.remove() }
    }

    override suspend fun addComment(postId: String, authorId: String, text: String): AppResult<PostComment> {
        val cleanText = text.trim()
        if (postId.isBlank() || authorId.isBlank()) return AppResult.Error(AppError.Auth("Oturum bulunamadı."))
        if (cleanText.isBlank()) return AppResult.Error(AppError.Validation("Yorum boş olamaz."))
        if (cleanText.length > 500) return AppResult.Error(AppError.Validation("Yorum en fazla 500 karakter olabilir."))
        return try {
            val profile = (userRepository.getUserProfileFresh(authorId) as? AppResult.Success)?.data
                ?: return AppResult.Error(AppError.Database("Profil bilgileri alınamadı."))
            if (!profile.moderation.canComment) {
                return AppResult.Error(AppError.Auth("Yorum yapma yetkiniz geçici olarak kısıtlandı."))
            }
            val ref = postsRef.document(postId).collection("comments").document()
            val comment = PostComment(
                id = ref.id,
                postId = postId,
                authorId = authorId,
                authorName = profile.displayName,
                authorUsername = profile.username,
                authorPhotoUrl = profile.profileImageUrl,
                text = cleanText,
                createdAt = System.currentTimeMillis()
            )
            ref.set(comment).await()
            postsRef.document(postId).update(
                "commentsCount",
                com.google.firebase.firestore.FieldValue.increment(1)
            ).await()
            val postSnapshot = postsRef.document(postId).get().await()
            val recipientId = postSnapshot.getString("authorId").orEmpty()
            if (recipientId.isNotBlank() && recipientId != authorId) {
                runCatching { notificationRepository.create(
                    AppNotification(
                        recipientId = recipientId,
                        actorId = authorId,
                        actorName = profile.displayName,
                        actorUsername = profile.username,
                        actorPhotoUrl = profile.profileImageUrl,
                        type = "COMMENT",
                        title = "Yeni yorum",
                        body = profile.displayName + " gönderine yorum yaptı.",
                        referenceId = postId,
                        createdAt = System.currentTimeMillis()
                    )
                    )
                }
            }
            AppResult.Success(comment)
        } catch (e: Exception) {
            AppResult.Error(AppError.Database("Yorum gönderilemedi.", e))
        }
    }

    override suspend fun deleteComment(postId: String, commentId: String, userId: String): AppResult<Unit> {
        if (postId.isBlank() || commentId.isBlank() || userId.isBlank()) return AppResult.Error(AppError.Auth("Oturum bulunamadı."))
        return try {
            val ref = postsRef.document(postId).collection("comments").document(commentId)
            val snapshot = ref.get().await()
            if (!snapshot.exists()) return AppResult.Success(Unit)
            if (snapshot.getString("authorId") != userId) {
                return AppResult.Error(AppError.Auth("Bu yorumu silme yetkin yok."))
            }
            ref.delete().await()
            postsRef.document(postId).update(
                "commentsCount",
                com.google.firebase.firestore.FieldValue.increment(-1)
            ).await()
            AppResult.Success(Unit)
        } catch (e: Exception) {
            AppResult.Error(AppError.Database("Yorum silinemedi.", e))
        }
    }


    override suspend fun toggleSave(postId: String, userId: String): AppResult<Boolean> {
        if (postId.isBlank() || userId.isBlank()) return AppResult.Error(AppError.Auth("Oturum bulunamadı."))
        return try {
            val ref = firestore.collection("savedPosts").document(userId).collection("posts").document(postId)
            if (ref.get().await().exists()) {
                ref.delete().await()
                AppResult.Success(false)
            } else {
                if (!postsRef.document(postId).get().await().exists()) {
                    return AppResult.Error(AppError.Database("Gönderi bulunamadı."))
                }
                ref.set(mapOf("postId" to postId, "userId" to userId, "savedAt" to System.currentTimeMillis())).await()
                AppResult.Success(true)
            }
        } catch (e: Exception) {
            AppResult.Error(AppError.Database("Kaydetme işlemi tamamlanamadı.", e))
        }
    }

    override fun observeSavedPosts(userId: String, limit: Long): Flow<AppResult<List<Post>>> = callbackFlow {
        if (userId.isBlank()) {
            trySend(AppResult.Success(emptyList()))
            close()
            return@callbackFlow
        }
        val registration = firestore.collection("savedPosts").document(userId).collection("posts")
            .orderBy("savedAt", Query.Direction.DESCENDING).limit(limit)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(AppResult.Error(AppError.Database("Kaydedilenler yüklenemedi.", error)))
                    return@addSnapshotListener
                }
                launch {
                    try {
                        val posts = snapshot?.documents.orEmpty().mapNotNull { saved ->
                            val id = saved.getString("postId") ?: saved.id
                            postsRef.document(id).get().await().takeIf { it.exists() }?.let { doc ->
                                val data = doc.data ?: return@let null
                                Post(
                                    id = doc.id,
                                    authorId = data["authorId"] as? String ?: "",
                                    authorName = data["authorName"] as? String ?: "",
                                    authorUsername = data["authorUsername"] as? String ?: "",
                                    authorPhotoUrl = data["authorPhotoUrl"] as? String ?: "",
                                    title = data["title"] as? String ?: "",
                                    text = data["text"] as? String ?: "",
                                    mediaUrl = data["mediaUrl"] as? String ?: "",
                                    mediaType = data["mediaType"] as? String ?: "",
                                    tags = (data["tags"] as? List<*>)?.mapNotNull { it as? String }.orEmpty(),
                                    likesCount = (data["likesCount"] as? Number)?.toInt() ?: 0,
                                    likedByCurrentUser = doc.reference.collection("likes").document(userId).get().await().exists(),
                                    savedByCurrentUser = true,
                                    createdAt = (data["createdAt"] as? Number)?.toLong() ?: 0L
                                )
                            }
                        }
                        trySend(AppResult.Success(posts))
                    } catch (e: Exception) {
                        trySend(AppResult.Error(AppError.Database("Kaydedilenler çözümlenemedi.", e)))
                    }
                }
            }
        awaitClose { registration.remove() }
    }

}
