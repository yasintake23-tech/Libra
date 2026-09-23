package com.libra.app.data.post

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
        val query = postsRef.orderBy("createdAt", Query.Direction.DESCENDING).limit(limit)
        val registration: ListenerRegistration = query.addSnapshotListener { snapshot, error ->
            if (error != null) {
                trySend(AppResult.Error(AppError.Database("Ana akış yüklenemedi.", error)))
                return@addSnapshotListener
            }

            val documents = snapshot?.documents.orEmpty()
            launch {
                try {
                    val followingIds = (userRepository.getFollowingIds(currentUserId) as? AppResult.Success)?.data.orEmpty() + currentUserId
                    val posts = documents.filter { it.getString("authorId") in followingIds }.mapNotNull { document ->
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
                            tags = (data["tags"] as? List<*>)?.mapNotNull { it as? String }.orEmpty(),
                            likesCount = (data["likesCount"] as? Number)?.toInt() ?: 0,
                            likedByCurrentUser = runCatching {
                                document.reference.collection("likes").document(currentUserId).get().await().exists()
                            }.getOrDefault(false),
                            savedByCurrentUser = runCatching {
                                firestore.collection("savedPosts").document(currentUserId).collection("posts").document(document.id).get().await().exists()
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

    override suspend fun toggleLike(postId: String): AppResult<Boolean> {
        val user = FirebaseAuth.getInstance().currentUser
            ?: return AppResult.Error(AppError.Auth("Beğenmek için giriş yapmalısın."))

        return try {
            val ref = postsRef.document(postId)
            val snapshot = ref.get().await()
            if (!snapshot.exists()) {
                return AppResult.Error(AppError.Validation("Gönderi bulunamadı."))
            }

            val likes = snapshot.get("likedBy") as? List<*> ?: emptyList<Any>()
            val liked = user.uid in likes
            val updatedLikes = if (liked) {
                likes.filterIsInstance<String>().filter { it != user.uid }
            } else {
                likes.filterIsInstance<String>() + user.uid
            }

            ref.update("likedBy", updatedLikes).await()

            if (!liked) {
                val recipientId = snapshot.getString("authorId").orEmpty()
                if (recipientId.isNotBlank() && recipientId != user.uid) {
                    val actor = (userRepository.getUserProfileFresh(user.uid) as? AppResult.Success)?.data
                    if (actor != null) {
                        runCatching {
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

            AppResult.Success(!liked)
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
