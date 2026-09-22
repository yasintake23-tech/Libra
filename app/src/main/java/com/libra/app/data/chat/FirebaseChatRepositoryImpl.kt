package com.libra.app.data.chat

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.libra.app.core.result.AppError
import com.libra.app.core.result.AppResult
import com.libra.app.domain.model.GlobalChatMessage
import com.libra.app.domain.repository.ChatRepository
import com.libra.app.domain.repository.UserRepository
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class FirebaseChatRepositoryImpl(
    private val userRepository: UserRepository
) : ChatRepository {
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private val messagesRef
        get() = firestore.collection("globalChatMessages")

    override fun observeGlobalMessages(limit: Long): Flow<AppResult<List<GlobalChatMessage>>> = callbackFlow {
        val registration = messagesRef
            .orderBy("createdAt", Query.Direction.ASCENDING)
            .limitToLast(limit)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(AppResult.Error(AppError.Database("Genel sohbet yüklenemedi.", error)))
                    return@addSnapshotListener
                }

                val messages = snapshot?.documents.orEmpty().mapNotNull { document ->
                    runCatching {
                        document.toObject(GlobalChatMessage::class.java)?.copy(id = document.id)
                    }.getOrNull()
                }
                trySend(AppResult.Success(messages))
            }

        awaitClose { registration.remove() }
    }

    override suspend fun sendGlobalMessage(text: String): AppResult<Unit> {
        val user = auth.currentUser ?: return AppResult.Error(AppError.Auth("Sohbet için giriş yapmalısın."))
        val clean = text.trim()
        if (clean.isBlank()) return AppResult.Error(AppError.Validation("Mesaj boş olamaz."))
        if (clean.length > 1000) return AppResult.Error(AppError.Validation("Mesaj en fazla 1000 karakter olabilir."))

        return try {
            val profileResult = userRepository.getUserProfileFresh(user.uid)
            val profile = (profileResult as? AppResult.Success)?.data
                ?: return AppResult.Error(AppError.Auth("Profil bulunamadı."))

            messagesRef.add(
                GlobalChatMessage(
                    senderId = user.uid,
                    senderName = profile.displayName,
                    senderUsername = profile.username,
                    senderPhotoUrl = profile.profileImageUrl,
                    text = clean,
                    createdAt = System.currentTimeMillis()
                )
            ).await()

            AppResult.Success(Unit)
        } catch (e: Exception) {
            AppResult.Error(AppError.Database("Mesaj gönderilemedi.", e))
        }
    }
    override fun observeDirectConversations(uid: String): Flow<AppResult<List<com.libra.app.domain.model.DirectConversation>>> = callbackFlow {
        val registration = conversationsRef.whereArrayContains("participants", uid).addSnapshotListener { snapshot, error ->
            if (error != null) {
                trySend(AppResult.Error(AppError.Database("Mesajlar yüklenemedi.", error)))
                return@addSnapshotListener
            }
            val conversations = snapshot?.documents.orEmpty().mapNotNull { doc ->
                val participants = (doc.get("participants") as? List<*>)?.filterIsInstance<String>().orEmpty()
                val otherId = participants.firstOrNull { it != uid }.orEmpty()
                if (otherId.isBlank()) null else DirectConversation(
                    id = doc.id, participants = participants, otherUserId = otherId,
                    otherUserName = doc.getString("otherUserName_" + uid).orEmpty(),
                    otherUserUsername = doc.getString("otherUserUsername_" + uid).orEmpty(),
                    otherUserPhotoUrl = doc.getString("otherUserPhotoUrl_" + uid).orEmpty(),
                    lastMessage = doc.getString("lastMessage").orEmpty(),
                    updatedAt = doc.getLong("updatedAt") ?: 0L
                )
            }.sortedByDescending { it.updatedAt }
            trySend(AppResult.Success(conversations))
        }
        awaitClose { registration.remove() }
    }

    override fun observeDirectMessages(conversationId: String, limit: Long): Flow<AppResult<List<com.libra.app.domain.model.DirectMessage>>> = callbackFlow {
        val registration = conversationsRef.document(conversationId).collection("messages")
            .orderBy("createdAt", Query.Direction.ASCENDING).limitToLast(limit)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(AppResult.Error(AppError.Database("Sohbet açılamadı.", error)))
                    return@addSnapshotListener
                }
                val messages = snapshot?.documents.orEmpty().mapNotNull { doc ->
                    runCatching { doc.toObject(com.libra.app.domain.model.DirectMessage::class.java)?.copy(id = doc.id) }.getOrNull()
                }
                trySend(AppResult.Success(messages))
            }
        awaitClose { registration.remove() }
    }

    override suspend fun sendDirectMessage(recipientId: String, text: String): AppResult<Unit> {
        val sender = auth.currentUser ?: return AppResult.Error(AppError.Auth("Mesaj için giriş yapmalısın."))
        val clean = text.trim()
        if (recipientId.isBlank() || recipientId == sender.uid) return AppResult.Error(AppError.Validation("Geçersiz kullanıcı."))
        if (clean.isBlank()) return AppResult.Error(AppError.Validation("Mesaj boş olamaz."))
        if (clean.length > 1000) return AppResult.Error(AppError.Validation("Mesaj en fazla 1000 karakter olabilir."))
        return try {
            val senderProfile = (userRepository.getUserProfileFresh(sender.uid) as? AppResult.Success)?.data
                ?: return AppResult.Error(AppError.Auth("Gönderen profili bulunamadı."))
            val recipientProfile = (userRepository.getUserProfileFresh(recipientId) as? AppResult.Success)?.data
                ?: return AppResult.Error(AppError.Validation("Bu kullanıcı artık mevcut değil."))
            val ids = listOf(sender.uid, recipientId).sorted()
            val conversationId = ids.joinToString("_")
            val now = System.currentTimeMillis()
            val base = mapOf(
                "participants" to ids,
                "updatedAt" to now,
                "lastMessage" to clean,
                "otherUserName_" + sender.uid to recipientProfile.displayName,
                "otherUserUsername_" + sender.uid to recipientProfile.username,
                "otherUserPhotoUrl_" + sender.uid to recipientProfile.profileImageUrl,
                "otherUserName_" + recipientId to senderProfile.displayName,
                "otherUserUsername_" + recipientId to senderProfile.username,
                "otherUserPhotoUrl_" + recipientId to senderProfile.profileImageUrl
            )
            val conversation = conversationsRef.document(conversationId)
            conversation.set(base, com.google.firebase.firestore.SetOptions.merge()).await()
            conversation.collection("messages").add(
                com.libra.app.domain.model.DirectMessage(
                    senderId = sender.uid, recipientId = recipientId, text = clean, createdAt = now
                )
            ).await()
            AppResult.Success(Unit)
        } catch (e: Exception) {
            AppResult.Error(AppError.Database("Mesaj gönderilemedi.", e))
        }
    }

}
