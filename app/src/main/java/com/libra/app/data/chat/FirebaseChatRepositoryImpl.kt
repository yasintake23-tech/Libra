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
}
