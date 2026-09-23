package com.libra.app.data.chat

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.libra.app.core.result.AppError
import com.libra.app.core.result.AppResult
import com.libra.app.domain.model.DirectConversation
import com.libra.app.domain.model.AppNotification
import com.libra.app.domain.model.DirectMessage
import com.libra.app.domain.model.GlobalChatMessage
import com.libra.app.domain.model.ServerMessage
import com.libra.app.domain.repository.ChatRepository
import com.libra.app.domain.repository.NotificationRepository
import com.libra.app.domain.repository.StorageRepository
import com.libra.app.domain.repository.UserRepository
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

/**
 * All live messaging is stored in Firebase Realtime Database.
 * Community metadata is provided by a separate CommunityRepository.
 */
class RealtimeChatRepositoryImpl(
    private val userRepository: UserRepository,
    private val storageRepository: StorageRepository,
    private val notificationRepository: NotificationRepository
) : ChatRepository {

    private val auth = FirebaseAuth.getInstance()
    private val database: FirebaseDatabase? by lazy {
        runCatching {
            FirebaseDatabase.getInstance("https://libra-3bfb9-default-rtdb.firebaseio.com")
        }.getOrNull()
    }

    private fun error(message: String, cause: Exception? = null): AppResult.Error =
        AppResult.Error(AppError.Database(message, cause))

    private fun requireUid(): String? = auth.currentUser?.uid

    private fun ref(path: String): DatabaseReference? = database?.getReference(path)

    private fun conversationId(a: String, b: String): String =
        listOf(a, b).sorted().joinToString("_")

    private fun directMessageFrom(snapshot: DataSnapshot): DirectMessage? =
        runCatching {
            DirectMessage(
                id = snapshot.key.orEmpty(),
                senderId = snapshot.child("senderId").getValue(String::class.java).orEmpty(),
                senderPhotoUrl = snapshot.child("senderPhotoUrl").getValue(String::class.java).orEmpty(),
                recipientId = snapshot.child("recipientId").getValue(String::class.java).orEmpty(),
                text = snapshot.child("text").getValue(String::class.java).orEmpty(),
                mediaUrl = snapshot.child("mediaUrl").getValue(String::class.java).orEmpty(),
                mediaType = snapshot.child("mediaType").getValue(String::class.java).orEmpty(),
                createdAt = snapshot.child("createdAt").getValue(Long::class.java) ?: 0L,
                editedAt = snapshot.child("editedAt").getValue(Long::class.java),
                replyToMessageId = snapshot.child("replyToMessageId").getValue(String::class.java).orEmpty(),
                replyToText = snapshot.child("replyToText").getValue(String::class.java).orEmpty(),
                replyToSenderId = snapshot.child("replyToSenderId").getValue(String::class.java).orEmpty(),
                replyToSenderName = snapshot.child("replyToSenderName").getValue(String::class.java).orEmpty(),
                reactions = snapshot.child("reactions").children.associateNotNull { key.orEmpty() to getValue(String::class.java) }
            )
        }.getOrNull()

    private fun globalMessageFrom(snapshot: DataSnapshot): GlobalChatMessage? =
        runCatching {
            GlobalChatMessage(
                id = snapshot.key.orEmpty(),
                senderId = snapshot.child("senderId").getValue(String::class.java).orEmpty(),
                senderName = snapshot.child("senderName").getValue(String::class.java).orEmpty(),
                senderUsername = snapshot.child("senderUsername").getValue(String::class.java).orEmpty(),
                senderPhotoUrl = snapshot.child("senderPhotoUrl").getValue(String::class.java).orEmpty(),
                text = snapshot.child("text").getValue(String::class.java).orEmpty(),
                mediaUrl = snapshot.child("mediaUrl").getValue(String::class.java).orEmpty(),
                mediaType = snapshot.child("mediaType").getValue(String::class.java).orEmpty(),
                createdAt = snapshot.child("createdAt").getValue(Long::class.java) ?: 0L,
                editedAt = snapshot.child("editedAt").getValue(Long::class.java),
                replyToMessageId = snapshot.child("replyToMessageId").getValue(String::class.java).orEmpty(),
                replyToText = snapshot.child("replyToText").getValue(String::class.java).orEmpty(),
                replyToSenderId = snapshot.child("replyToSenderId").getValue(String::class.java).orEmpty(),
                replyToSenderName = snapshot.child("replyToSenderName").getValue(String::class.java).orEmpty(),
                reactions = snapshot.child("reactions").children.associateNotNull { key.orEmpty() to getValue(String::class.java) }
            )
        }.getOrNull()

    private fun serverMessageFrom(snapshot: DataSnapshot): ServerMessage? =
        runCatching {
            ServerMessage(
                id = snapshot.key.orEmpty(),
                senderId = snapshot.child("senderId").getValue(String::class.java).orEmpty(),
                senderName = snapshot.child("senderName").getValue(String::class.java).orEmpty(),
                senderUsername = snapshot.child("senderUsername").getValue(String::class.java).orEmpty(),
                senderPhotoUrl = snapshot.child("senderPhotoUrl").getValue(String::class.java).orEmpty(),
                text = snapshot.child("text").getValue(String::class.java).orEmpty(),
                mediaUrl = snapshot.child("mediaUrl").getValue(String::class.java).orEmpty(),
                mediaType = snapshot.child("mediaType").getValue(String::class.java).orEmpty(),
                createdAt = snapshot.child("createdAt").getValue(Long::class.java) ?: 0L,
                editedAt = snapshot.child("editedAt").getValue(Long::class.java),
                replyToMessageId = snapshot.child("replyToMessageId").getValue(String::class.java).orEmpty(),
                replyToText = snapshot.child("replyToText").getValue(String::class.java).orEmpty(),
                replyToSenderId = snapshot.child("replyToSenderId").getValue(String::class.java).orEmpty(),
                replyToSenderName = snapshot.child("replyToSenderName").getValue(String::class.java).orEmpty(),
                reactions = snapshot.child("reactions").children.associateNotNull { key.orEmpty() to getValue(String::class.java) }
            )
        }.getOrNull()

    private fun summary(
        otherUid: String,
        otherName: String,
        otherUsername: String,
        otherPhotoUrl: String,
        lastMessage: String,
        updatedAt: Long,
        unreadCount: Any
    ) = mapOf(
        "otherUserId" to otherUid,
        "otherUserName" to otherName,
        "otherUserUsername" to otherUsername,
        "otherUserPhotoUrl" to otherPhotoUrl,
        "lastMessage" to lastMessage,
        "updatedAt" to updatedAt,
        "unreadCount" to unreadCount
    )

    override fun observeGlobalMessages(limit: Long): Flow<AppResult<List<GlobalChatMessage>>> = callbackFlow {
        val query = ref("globalMessages")?.orderByChild("createdAt")?.limitToLast(limit.toInt())
            ?: run { trySend(error("Realtime Database yapılandırması bulunamadı.")); close(); return@callbackFlow }
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                trySend(AppResult.Success(snapshot.children.mapNotNull(::globalMessageFrom).sortedBy { it.createdAt }))
            }
            override fun onCancelled(error: DatabaseError) {
                trySend(this@RealtimeChatRepositoryImpl.error(error.message, error.toException()))
            }
        }
        query.addValueEventListener(listener)
        awaitClose { query.removeEventListener(listener) }
    }

    override suspend fun sendGlobalMessage(text: String, replyTo: GlobalChatMessage?): AppResult<Unit> =
        sendGlobalInternal(text, "", "", replyTo)

    override suspend fun sendGlobalMediaMessage(mediaUrl: String, mediaType: String, text: String, replyTo: GlobalChatMessage?): AppResult<Unit> =
        sendGlobalInternal(text, mediaUrl, mediaType, replyTo)

    private suspend fun sendGlobalInternal(text: String, mediaUrl: String, mediaType: String, replyTo: GlobalChatMessage?): AppResult<Unit> {
        val user = auth.currentUser ?: return AppResult.Error(AppError.Auth("Sohbet için giriş yapmalısın."))
        val clean = text.trim()
        if (clean.isBlank() && mediaUrl.isBlank()) return AppResult.Error(AppError.Validation("Mesaj boş olamaz."))
        if (clean.length > 1000) return AppResult.Error(AppError.Validation("Mesaj en fazla 1000 karakter olabilir."))
        val profile = (userRepository.getUserProfileFresh(user.uid) as? AppResult.Success)?.data
            ?: return AppResult.Error(AppError.Auth("Profil bulunamadı."))
        val messageRef = ref("globalMessages")?.push() ?: return error("Realtime Database yapılandırması bulunamadı.")
        val data = mapOf(
            "senderId" to user.uid,
            "senderName" to profile.displayName,
            "senderUsername" to profile.username,
            "senderPhotoUrl" to profile.profileImageUrl,
            "text" to clean,
            "mediaUrl" to mediaUrl,
            "mediaType" to mediaType,
            "createdAt" to System.currentTimeMillis(),
            "replyToMessageId" to replyTo?.id.orEmpty(),
            "replyToText" to replyTo?.text?.ifBlank { if (replyTo.mediaUrl.isNotBlank()) "📷 Fotoğraf" else "" }.orEmpty(),
            "replyToSenderId" to replyTo?.senderId.orEmpty(),
            "replyToSenderName" to replyTo?.senderName.orEmpty(),
            "reactions" to emptyMap<String, String>()
        )
        return try {
            messageRef.setValue(data).await()
            AppResult.Success(Unit)
        } catch (e: Exception) {
            cleanupUploadedMedia(mediaUrl)
            error("Mesaj gönderilemedi.", e)
        }
    }

    override suspend fun editGlobalMessage(messageId: String, text: String): AppResult<Unit> =
        editMessage(ref("globalMessages/$messageId"), text)

    override suspend fun deleteGlobalMessage(messageId: String): AppResult<Unit> =
        deleteMessage(ref("globalMessages/$messageId"))

    override suspend fun toggleGlobalMessageReaction(messageId: String, emoji: String): AppResult<Unit> =
        toggleReaction(ref("globalMessages/$messageId"), emoji)

    override fun observeDirectConversations(uid: String): Flow<AppResult<List<DirectConversation>>> = callbackFlow {
        val query = ref("directConversations/$uid")?.orderByChild("updatedAt")?.limitToLast(100)
            ?: run { trySend(error("Realtime Database yapılandırması bulunamadı.")); close(); return@callbackFlow }
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val items = snapshot.children.map { child ->
                    DirectConversation(
                        id = child.key.orEmpty(),
                        participants = listOf(uid, child.child("otherUserId").getValue(String::class.java).orEmpty()).filter { it.isNotBlank() },
                        otherUserId = child.child("otherUserId").getValue(String::class.java).orEmpty(),
                        otherUserName = child.child("otherUserName").getValue(String::class.java).orEmpty(),
                        otherUserUsername = child.child("otherUserUsername").getValue(String::class.java).orEmpty(),
                        otherUserPhotoUrl = child.child("otherUserPhotoUrl").getValue(String::class.java).orEmpty(),
                        lastMessage = child.child("lastMessage").getValue(String::class.java).orEmpty(),
                        updatedAt = child.child("updatedAt").getValue(Long::class.java) ?: 0L,
                        unreadCount = (child.child("unreadCount").getValue(Long::class.java) ?: 0L).toInt()
                    )
                }.sortedByDescending { it.updatedAt }
                trySend(AppResult.Success(items))
            }
            override fun onCancelled(error: DatabaseError) {
                trySend(this@RealtimeChatRepositoryImpl.error(error.message, error.toException()))
            }
        }
        query.addValueEventListener(listener)
        awaitClose { query.removeEventListener(listener) }
    }

    override fun observeDirectMessages(conversationId: String, limit: Long): Flow<AppResult<List<DirectMessage>>> = callbackFlow {
        val query = ref("directMessages/$conversationId")?.orderByChild("createdAt")?.limitToLast(limit.toInt())
            ?: run { trySend(error("Realtime Database yapılandırması bulunamadı.")); close(); return@callbackFlow }
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                trySend(AppResult.Success(snapshot.children.mapNotNull(::directMessageFrom).sortedBy { it.createdAt }))
            }
            override fun onCancelled(error: DatabaseError) {
                trySend(this@RealtimeChatRepositoryImpl.error(error.message, error.toException()))
            }
        }
        query.addValueEventListener(listener)
        awaitClose { query.removeEventListener(listener) }
    }

    override suspend fun sendDirectMessage(recipientId: String, text: String, replyTo: DirectMessage?): AppResult<Unit> =
        sendDirectInternal(recipientId, text, "", "", replyTo)

    override suspend fun sendDirectMediaMessage(recipientId: String, mediaUrl: String, mediaType: String, text: String, replyTo: DirectMessage?): AppResult<Unit> =
        sendDirectInternal(recipientId, text, mediaUrl, mediaType, replyTo)

    private suspend fun sendDirectInternal(recipientId: String, text: String, mediaUrl: String, mediaType: String, replyTo: DirectMessage?): AppResult<Unit> {
        val sender = auth.currentUser ?: return AppResult.Error(AppError.Auth("Mesaj göndermek için giriş yapmalısın."))
        if (recipientId.isBlank() || recipientId == sender.uid) return AppResult.Error(AppError.Validation("Geçerli bir alıcı seçilmedi."))
        val clean = text.trim()
        if (clean.isBlank() && mediaUrl.isBlank()) return AppResult.Error(AppError.Validation("Mesaj boş olamaz."))
        if (clean.length > 2000) return AppResult.Error(AppError.Validation("Mesaj en fazla 2000 karakter olabilir."))

        val senderProfile = (userRepository.getUserProfileFresh(sender.uid) as? AppResult.Success)?.data
            ?: return AppResult.Error(AppError.Database("Gönderen profili bulunamadı."))
        val recipientProfile = (userRepository.getUserProfileFresh(recipientId) as? AppResult.Success)?.data
            ?: return AppResult.Error(AppError.NotFound("Alıcı bulunamadı."))

        val conversation = conversationId(sender.uid, recipientId)
        val messageRef = ref("directMessages/$conversation")?.push() ?: return error("Realtime Database yapılandırması bulunamadı.")
        val now = System.currentTimeMillis()
        val lastText = clean.ifBlank { if (mediaUrl.isNotBlank()) "📷 Fotoğraf" else "" }
        val message = mapOf(
            "senderId" to sender.uid,
            "senderPhotoUrl" to senderProfile.profileImageUrl,
            "recipientId" to recipientId,
            "text" to clean,
            "mediaUrl" to mediaUrl,
            "mediaType" to mediaType,
            "createdAt" to now,
            "editedAt" to null,
            "replyToMessageId" to replyTo?.id.orEmpty(),
            "replyToText" to replyTo?.text?.ifBlank { if (replyTo.mediaUrl.isNotBlank()) "📷 Fotoğraf" else "" }.orEmpty(),
            "replyToSenderId" to replyTo?.senderId.orEmpty(),
            "replyToSenderName" to when {
                replyTo == null -> ""
                replyTo.senderId == sender.uid -> senderProfile.displayName
                replyTo.senderId == recipientId -> recipientProfile.displayName
                else -> replyTo.replyToSenderName.orEmpty()
            },
            "reactions" to emptyMap<String, String>()
        )

        return try {
            val root = database?.reference
                ?: return error("Realtime Database yapılandırması bulunamadı.")

            // The actual message must not be rolled back just because a recipient
            // conversation summary rule is stale or temporarily unavailable.
            // Write the message first. Conversation summaries are secondary metadata
            // and must never make a valid message disappear from the chat.
            messageRef.setValue(message).await()

            runCatching {
                root.updateChildren(
                    mapOf(
                        "directConversations/${sender.uid}/$conversation" to summary(
                            recipientId,
                            recipientProfile.displayName,
                            recipientProfile.username,
                            recipientProfile.profileImageUrl,
                            lastText,
                            now,
                            0
                        )
                    )
                ).await()
            }

            runCatching {
                root.updateChildren(
                    mapOf(
                        "directConversations/$recipientId/$conversation" to summary(
                            sender.uid,
                            senderProfile.displayName,
                            senderProfile.username,
                            senderProfile.profileImageUrl,
                            lastText,
                            now,
                            com.google.firebase.database.ServerValue.increment(1)
                        )
                    )
                ).await()
            }
            runCatching {
                notificationRepository.create(
                    AppNotification(
                        recipientId = recipientId,
                        actorId = sender.uid,
                        actorName = senderProfile.displayName,
                        actorUsername = senderProfile.username,
                        actorPhotoUrl = senderProfile.profileImageUrl,
                        type = "MESSAGE",
                        title = "Yeni mesaj",
                        body = senderProfile.displayName + " sana bir mesaj gönderdi.",
                        referenceId = conversation,
                        createdAt = now
                    )
                )
            }
            AppResult.Success(Unit)
        } catch (e: Exception) {
            cleanupUploadedMedia(mediaUrl)
            error("Mesaj gönderilemedi.", e)
        }
    }

    override suspend fun editDirectMessage(conversationId: String, messageId: String, text: String): AppResult<Unit> {
        val result = editMessage(ref("directMessages/$conversationId/$messageId"), text)
        if (result is AppResult.Success) refreshDirectConversationSummary(conversationId)
        return result
    }

    override suspend fun deleteDirectMessage(conversationId: String, messageId: String): AppResult<Unit> {
        val result = deleteMessage(ref("directMessages/$conversationId/$messageId"))
        if (result is AppResult.Success) refreshDirectConversationSummary(conversationId)
        return result
    }

    override suspend fun toggleDirectMessageReaction(conversationId: String, messageId: String, emoji: String): AppResult<Unit> =
        toggleReaction(ref("directMessages/$conversationId/$messageId"), emoji)

    override suspend fun markDirectConversationRead(conversationId: String): AppResult<Unit> {
        val uid = requireUid() ?: return AppResult.Error(AppError.Auth("Oturum bulunamadı."))
        return try {
            ref("directConversations/$uid/$conversationId/unreadCount")?.setValue(0)?.await()
            AppResult.Success(Unit)
        } catch (e: Exception) { error("Sohbet okundu olarak işaretlenemedi.", e) }
    }

    override fun observeServerMessages(serverId: String, limit: Long): Flow<AppResult<List<ServerMessage>>> = callbackFlow {
        val query = ref("serverMessages/$serverId")?.orderByChild("createdAt")?.limitToLast(limit.toInt())
            ?: run { trySend(error("Realtime Database yapılandırması bulunamadı.")); close(); return@callbackFlow }
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                trySend(AppResult.Success(snapshot.children.mapNotNull(::serverMessageFrom).sortedBy { it.createdAt }))
            }
            override fun onCancelled(error: DatabaseError) {
                trySend(this@RealtimeChatRepositoryImpl.error(error.message, error.toException()))
            }
        }
        query.addValueEventListener(listener)
        awaitClose { query.removeEventListener(listener) }
    }

    override suspend fun sendServerMessage(serverId: String, text: String, replyTo: ServerMessage?): AppResult<Unit> =
        sendServerInternal(serverId, text, "", "", replyTo)

    override suspend fun sendServerMediaMessage(serverId: String, mediaUrl: String, mediaType: String, text: String, replyTo: ServerMessage?): AppResult<Unit> =
        sendServerInternal(serverId, text, mediaUrl, mediaType, replyTo)

    private suspend fun sendServerInternal(serverId: String, text: String, mediaUrl: String, mediaType: String, replyTo: ServerMessage?): AppResult<Unit> {
        val user = auth.currentUser ?: return AppResult.Error(AppError.Auth("Mesaj göndermek için giriş yapmalısın."))
        val clean = text.trim()
        if (serverId.isBlank()) return AppResult.Error(AppError.Validation("Sunucu bulunamadı."))
        if (clean.isBlank() && mediaUrl.isBlank()) return AppResult.Error(AppError.Validation("Mesaj boş olamaz."))
        if (clean.length > 2000) return AppResult.Error(AppError.Validation("Mesaj çok uzun."))
        val profile = (userRepository.getUserProfileFresh(user.uid) as? AppResult.Success)?.data
            ?: return AppResult.Error(AppError.Database("Profil bulunamadı."))
        val messageRef = ref("serverMessages/$serverId")?.push() ?: return error("Realtime Database yapılandırması bulunamadı.")
        val data = mapOf(
            "senderId" to user.uid,
            "senderName" to profile.displayName,
            "senderUsername" to profile.username,
            "senderPhotoUrl" to profile.profileImageUrl,
            "text" to clean,
            "mediaUrl" to mediaUrl,
            "mediaType" to mediaType,
            "createdAt" to System.currentTimeMillis(),
            "replyToMessageId" to replyTo?.id.orEmpty(),
            "replyToText" to replyTo?.text?.ifBlank { if (replyTo.mediaUrl.isNotBlank()) "📷 Fotoğraf" else "" }.orEmpty(),
            "replyToSenderId" to replyTo?.senderId.orEmpty(),
            "replyToSenderName" to replyTo?.senderName.orEmpty(),
            "reactions" to emptyMap<String, String>()
        )
        return try {
            messageRef.setValue(data).await()
            AppResult.Success(Unit)
        } catch (e: Exception) {
            cleanupUploadedMedia(mediaUrl)
            error("Sunucu mesajı gönderilemedi.", e)
        }
    }

    override suspend fun editServerMessage(serverId: String, messageId: String, text: String): AppResult<Unit> =
        editMessage(ref("serverMessages/$serverId/$messageId"), text)

    override suspend fun deleteServerMessage(serverId: String, messageId: String): AppResult<Unit> =
        deleteMessage(ref("serverMessages/$serverId/$messageId"))

    override suspend fun toggleServerMessageReaction(serverId: String, messageId: String, emoji: String): AppResult<Unit> =
        toggleReaction(ref("serverMessages/$serverId/$messageId"), emoji)

    private suspend fun editMessage(messageRef: DatabaseReference?, text: String): AppResult<Unit> {
        val uid = requireUid() ?: return AppResult.Error(AppError.Auth("Oturum bulunamadı."))
        val clean = text.trim()
        if (clean.isBlank()) return AppResult.Error(AppError.Validation("Mesaj boş olamaz."))
        if (clean.length > 2000) return AppResult.Error(AppError.Validation("Mesaj çok uzun."))
        val messageRefSafe = messageRef ?: return error("Realtime Database yapılandırması bulunamadı.")
        return try {
            val snapshot = messageRefSafe.get().await()
            if (!snapshot.exists()) return AppResult.Error(AppError.NotFound("Mesaj bulunamadı."))
            if (snapshot.child("senderId").getValue(String::class.java) != uid) {
                return AppResult.Error(AppError.Auth("Bu mesajı düzenleme yetkin yok."))
            }
            if (snapshot.child("mediaUrl").getValue(String::class.java).orEmpty().isNotBlank()) {
                return AppResult.Error(AppError.Validation("Medya mesajları düzenlenemez."))
            }
            messageRefSafe.updateChildren(
                mapOf(
                    "text" to clean,
                    "editedAt" to System.currentTimeMillis()
                )
            ).await()
            AppResult.Success(Unit)
        } catch (e: Exception) { error("Mesaj düzenlenemedi.", e) }
    }

    private suspend fun deleteMessage(messageRef: DatabaseReference?): AppResult<Unit> {
        val uid = requireUid() ?: return AppResult.Error(AppError.Auth("Oturum bulunamadı."))
        val messageRefSafe = messageRef ?: return error("Realtime Database yapılandırması bulunamadı.")
        return try {
            val snapshot = messageRefSafe.get().await()
            if (!snapshot.exists()) return AppResult.Success(Unit)
            if (snapshot.child("senderId").getValue(String::class.java) != uid) {
                return AppResult.Error(AppError.Auth("Bu mesajı silme yetkin yok."))
            }

            val mediaKey = snapshot.child("mediaUrl").getValue(String::class.java).orEmpty()
            messageRefSafe.removeValue().await()

            if (mediaKey.isNotBlank()) {
                runCatching { storageRepository.deleteMedia(mediaKey) }
            }

            AppResult.Success(Unit)
        } catch (e: Exception) { error("Mesaj silinemedi.", e) }
    }

    private suspend fun refreshDirectConversationSummary(conversationId: String): AppResult<Unit> {
        val uid = requireUid() ?: return AppResult.Error(AppError.Auth("Oturum bulunamadı."))
        val mySummaryRef = ref("directConversations/$uid/$conversationId")
            ?: return error("Realtime Database yapılandırması bulunamadı.")

        return try {
            val mySummary = mySummaryRef.get().await()
            if (!mySummary.exists()) return AppResult.Success(Unit)

            val otherUid = mySummary.child("otherUserId").getValue(String::class.java).orEmpty()
            if (otherUid.isBlank()) return AppResult.Success(Unit)

            val messagesRef = ref("directMessages/$conversationId")
                ?: return error("Realtime Database yapılandırması bulunamadı.")
            val latest = messagesRef
                .orderByChild("createdAt")
                .limitToLast(1)
                .get()
                .await()
                .children
                .mapNotNull(::directMessageFrom)
                .maxByOrNull { it.createdAt }

            val otherSummaryRef = ref("directConversations/$otherUid/$conversationId")
                ?: return error("Realtime Database yapılandırması bulunamadı.")
            val updates = mutableMapOf<String, Any?>()

            if (latest == null) {
                updates["directConversations/$uid/$conversationId/lastMessage"] = ""
                updates["directConversations/$uid/$conversationId/updatedAt"] = 0L
                if (otherSummaryRef.get().await().exists()) {
                    updates["directConversations/$otherUid/$conversationId/lastMessage"] = ""
                    updates["directConversations/$otherUid/$conversationId/updatedAt"] = 0L
                }
            } else {
                val lastText = latest.text.ifBlank {
                    if (latest.mediaUrl.isNotBlank()) "📷 Fotoğraf" else ""
                }
                updates["directConversations/$uid/$conversationId/lastMessage"] = lastText
                updates["directConversations/$uid/$conversationId/updatedAt"] = latest.createdAt
                if (otherSummaryRef.get().await().exists()) {
                    updates["directConversations/$otherUid/$conversationId/lastMessage"] = lastText
                    updates["directConversations/$otherUid/$conversationId/updatedAt"] = latest.createdAt
                }
            }

            if (updates.isNotEmpty()) {
                (database?.reference ?: return error("Realtime Database yapılandırması bulunamadı."))
                    .updateChildren(updates)
                    .await()
            }

            AppResult.Success(Unit)
        } catch (e: Exception) {
            error("Sohbet özeti güncellenemedi.", e)
        }
    }

    private suspend fun cleanupUploadedMedia(mediaKey: String) {
        if (mediaKey.isBlank()) return
        runCatching { storageRepository.deleteMedia(mediaKey) }
    }

    private suspend fun toggleReaction(messageRef: DatabaseReference?, emoji: String): AppResult<Unit> {
        val uid = requireUid() ?: return AppResult.Error(AppError.Auth("Oturum bulunamadı."))
        val allowed = setOf("❤️", "😂", "😮", "😢", "😡", "👍")
        if (emoji !in allowed) return AppResult.Error(AppError.Validation("Geçersiz emoji."))
        val messageRefSafe = messageRef ?: return error("Realtime Database yapılandırması bulunamadı.")
        return try {
            val reactionRef = messageRefSafe.child("reactions").child(uid)
            val current = reactionRef.get().await().getValue(String::class.java)
            if (current == emoji) reactionRef.removeValue().await() else reactionRef.setValue(emoji).await()
            AppResult.Success(Unit)
        } catch (e: Exception) { error("Tepki güncellenemedi.", e) }
    }

    private inline fun <K, V> Iterable<DataSnapshot>.associateNotNull(
        transform: DataSnapshot.() -> Pair<K, V?>
    ): Map<K, V> = buildMap {
        for (item in this@associateNotNull) {
            val (key, value) = item.transform()
            if (value != null) put(key, value)
        }
    }
}
