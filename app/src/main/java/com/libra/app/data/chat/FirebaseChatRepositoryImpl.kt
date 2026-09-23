package com.libra.app.data.chat

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.libra.app.core.result.AppError
import com.libra.app.core.result.AppResult
import com.libra.app.domain.model.DirectConversation
import com.libra.app.domain.model.AppNotification
import com.libra.app.domain.model.DirectMessage
import com.libra.app.domain.model.CommunityServer
import com.libra.app.domain.model.ServerMessage
import com.libra.app.domain.model.ServerMember
import com.libra.app.domain.model.GlobalChatMessage
import com.libra.app.domain.repository.ChatRepository
import com.libra.app.domain.repository.UserRepository
import com.libra.app.core.di.ServiceLocator
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

    private val conversationsRef
        get() = firestore.collection("directConversations")

    private val serversRef
        get() = firestore.collection("communityServers")

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

    override suspend fun sendGlobalMessage(text: String, replyTo: GlobalChatMessage?): AppResult<Unit> {
        val user = auth.currentUser ?: return AppResult.Error(AppError.Auth("Sohbet için giriş yapmalısın."))
        val clean = text.trim()
        if (clean.isBlank()) return AppResult.Error(AppError.Validation("Mesaj boş olamaz."))
        if (clean.length > 1000) return AppResult.Error(AppError.Validation("Mesaj en fazla 1000 karakter olabilir."))
        return try {
            val profile = (userRepository.getUserProfileFresh(user.uid) as? AppResult.Success)?.data
                ?: return AppResult.Error(AppError.Auth("Profil bulunamadı."))
            messagesRef.add(
                GlobalChatMessage(
                    senderId = user.uid,
                    senderName = profile.displayName,
                    senderUsername = profile.username,
                    senderPhotoUrl = profile.profileImageUrl,
                    text = clean,
                    createdAt = System.currentTimeMillis(),
                    replyToMessageId = replyTo?.id.orEmpty(),
                    replyToText = replyTo?.text?.ifBlank { if (replyTo.mediaUrl.isNotBlank()) "📷 Fotoğraf" else "" }.orEmpty(),
                    replyToSenderId = replyTo?.senderId.orEmpty(),
                    replyToSenderName = replyTo?.senderName.orEmpty()
                )
            ).await()
            AppResult.Success(Unit)
        } catch (e: Exception) {
            AppResult.Error(AppError.Database("Mesaj gönderilemedi.", e))
        }
    }

    override suspend fun sendGlobalMediaMessage(
        mediaUrl: String,
        mediaType: String,
        text: String,
        replyTo: GlobalChatMessage?
    ): AppResult<Unit> {
        val user = auth.currentUser ?: return AppResult.Error(AppError.Auth("Medya göndermek için giriş yapmalısın."))
        val clean = text.trim()
        if (mediaUrl.isBlank()) return AppResult.Error(AppError.Validation("Geçersiz medya."))
        if (clean.length > 1000) return AppResult.Error(AppError.Validation("Mesaj en fazla 1000 karakter olabilir."))
        return try {
            val profile = (userRepository.getUserProfileFresh(user.uid) as? AppResult.Success)?.data
                ?: return AppResult.Error(AppError.Auth("Profil bulunamadı."))
            messagesRef.add(
                GlobalChatMessage(
                    senderId = user.uid,
                    senderName = profile.displayName,
                    senderUsername = profile.username,
                    senderPhotoUrl = profile.profileImageUrl,
                    text = clean,
                    mediaUrl = mediaUrl,
                    mediaType = mediaType,
                    createdAt = System.currentTimeMillis(),
                    replyToMessageId = replyTo?.id.orEmpty(),
                    replyToText = replyTo?.text?.ifBlank { if (replyTo.mediaUrl.isNotBlank()) "📷 Fotoğraf" else "" }.orEmpty(),
                    replyToSenderId = replyTo?.senderId.orEmpty(),
                    replyToSenderName = replyTo?.senderName.orEmpty()
                )
            ).await()
            AppResult.Success(Unit)
        } catch (e: Exception) {
            AppResult.Error(AppError.Database("Medya mesajı gönderilemedi.", e))
        }
    }

    override suspend fun editGlobalMessage(messageId: String, text: String): AppResult<Unit> =
        editCommunityLikeMessage(messagesRef.document(messageId), text, "Genel mesaj")

    override suspend fun deleteGlobalMessage(messageId: String): AppResult<Unit> =
        deleteCommunityLikeMessage(messagesRef.document(messageId))

    override suspend fun toggleGlobalMessageReaction(messageId: String, emoji: String): AppResult<Unit> =
        toggleCommunityLikeReaction(messagesRef.document(messageId), emoji)

    override fun observeDirectConversations(uid: String): Flow<AppResult<List<DirectConversation>>> = callbackFlow {
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
                    updatedAt = doc.getLong("updatedAt") ?: 0L,
                    unreadCount = (doc.getLong("unreadCount_" + uid) ?: 0L).toInt()
                )
            }.sortedByDescending { it.updatedAt }
            trySend(AppResult.Success(conversations))
        }
        awaitClose { registration.remove() }
    }

    override fun observeDirectMessages(conversationId: String, limit: Long): Flow<AppResult<List<DirectMessage>>> = callbackFlow {
        val registration = conversationsRef.document(conversationId).collection("messages")
            .orderBy("createdAt", Query.Direction.ASCENDING).limitToLast(limit)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(AppResult.Error(AppError.Database("Sohbet açılamadı.", error)))
                    return@addSnapshotListener
                }
                val messages = snapshot?.documents.orEmpty().mapNotNull { doc ->
                    runCatching { doc.toObject(DirectMessage::class.java)?.copy(id = doc.id) }.getOrNull()
                }
                trySend(AppResult.Success(messages))
            }
        awaitClose { registration.remove() }
    }

    override suspend fun sendDirectMessage(recipientId: String, text: String, replyTo: DirectMessage?): AppResult<Unit> {
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
                "otherUserPhotoUrl_" + recipientId to senderProfile.profileImageUrl,
                "unreadCount_" + recipientId to com.google.firebase.firestore.FieldValue.increment(1)
            )
            val conversation = conversationsRef.document(conversationId)
            firestore.runTransaction { transaction ->
                val current = transaction.get(conversation).getLong("unreadCount_" + recipientId) ?: 0L
                transaction.set(conversation, base + mapOf("unreadCount_" + recipientId to current + 1L), com.google.firebase.firestore.SetOptions.merge())
                null
            }.await()
            conversation.collection("messages").add(
                com.libra.app.domain.model.DirectMessage(
                    senderId = sender.uid,
                    senderPhotoUrl = senderProfile.profileImageUrl,
                    recipientId = recipientId,
                    text = clean,
                    createdAt = now,
                    replyToMessageId = replyTo?.id.orEmpty(),
                    replyToText = replyTo?.text?.ifBlank { if (replyTo.mediaUrl.isNotBlank()) "📷 Fotoğraf" else "" }.orEmpty(),
                    replyToSenderId = replyTo?.senderId.orEmpty(),
                    replyToSenderName = replyTo?.replyToSenderName?.takeIf { it.isNotBlank() }
                        ?: if (replyTo?.senderId == sender.uid) senderProfile.displayName else recipientProfile.displayName
                )
            ).await()
            ServiceLocator.notificationRepository.create(
                AppNotification(
                    recipientId = recipientId,
                    actorId = sender.uid,
                    actorName = senderProfile.displayName,
                    actorUsername = senderProfile.username,
                    actorPhotoUrl = senderProfile.profileImageUrl,
                    type = "MESSAGE",
                    title = "Yeni mesaj",
                    body = senderProfile.displayName + " sana bir mesaj gönderdi.",
                    referenceId = conversationId,
                    createdAt = now
                )
            )
            AppResult.Success(Unit)
        } catch (e: Exception) {
            AppResult.Error(AppError.Database("Mesaj gönderilemedi.", e))
        }
    }


    override suspend fun sendDirectMediaMessage(recipientId: String, mediaUrl: String, mediaType: String, text: String, replyTo: DirectMessage?): AppResult<Unit> {
        val sender = auth.currentUser ?: return AppResult.Error(AppError.Auth("Medya göndermek için giriş yapmalısın."))
        val cleanText = text.trim()
        if (recipientId.isBlank() || recipientId == sender.uid || mediaUrl.isBlank()) {
            return AppResult.Error(AppError.Validation("Geçersiz medya mesajı."))
        }
        if (cleanText.length > 1000) {
            return AppResult.Error(AppError.Validation("Mesaj en fazla 1000 karakter olabilir."))
        }
        return try {
            val senderProfile = (userRepository.getUserProfileFresh(sender.uid) as? AppResult.Success)?.data
                ?: return AppResult.Error(AppError.Auth("Gönderen profili bulunamadı."))
            val recipientProfile = (userRepository.getUserProfileFresh(recipientId) as? AppResult.Success)?.data
                ?: return AppResult.Error(AppError.Validation("Bu kullanıcı artık mevcut değil."))
            val ids = listOf(sender.uid, recipientId).sorted()
            val conversationId = ids.joinToString("_")
            val now = System.currentTimeMillis()
            val conversation = conversationsRef.document(conversationId)
            val base = mapOf(
                "participants" to ids,
                "updatedAt" to now,
                "lastMessage" to if (cleanText.isBlank()) {
                    if (mediaType.startsWith("image/")) "📷 Fotoğraf" else "📎 Dosya"
                } else cleanText,
                "otherUserName_" + sender.uid to recipientProfile.displayName,
                "otherUserUsername_" + sender.uid to recipientProfile.username,
                "otherUserPhotoUrl_" + sender.uid to recipientProfile.profileImageUrl,
                "otherUserName_" + recipientId to senderProfile.displayName,
                "otherUserUsername_" + recipientId to senderProfile.username,
                "otherUserPhotoUrl_" + recipientId to senderProfile.profileImageUrl
            )
            firestore.runTransaction { transaction ->
                val current = transaction.get(conversation).getLong("unreadCount_" + recipientId) ?: 0L
                transaction.set(conversation, base + mapOf("unreadCount_" + recipientId to current + 1L), com.google.firebase.firestore.SetOptions.merge())
                null
            }.await()
            conversation.collection("messages").add(
                DirectMessage(
                    senderId = sender.uid,
                    senderPhotoUrl = senderProfile.profileImageUrl,
                    recipientId = recipientId,
                    text = cleanText,
                    mediaUrl = mediaUrl,
                    mediaType = mediaType,
                    createdAt = now,
                    replyToMessageId = replyTo?.id.orEmpty(),
                    replyToText = replyTo?.text?.ifBlank { "📷 Fotoğraf" }.orEmpty(),
                    replyToSenderId = replyTo?.senderId.orEmpty(),
                    replyToSenderName = replyTo?.replyToSenderName?.takeIf { it.isNotBlank() }
                        ?: if (replyTo?.senderId == sender.uid) senderProfile.displayName else recipientProfile.displayName
                )
            ).await()
            ServiceLocator.notificationRepository.create(
                AppNotification(
                    recipientId = recipientId,
                    actorId = sender.uid,
                    actorName = senderProfile.displayName,
                    actorUsername = senderProfile.username,
                    actorPhotoUrl = senderProfile.profileImageUrl,
                    type = "MESSAGE",
                    title = "Yeni medya mesajı",
                    body = senderProfile.displayName + if (cleanText.isBlank()) " sana bir medya gönderdi." else " sana bir fotoğraf ve mesaj gönderdi.",
                    referenceId = conversationId,
                    createdAt = now
                )
            )
            AppResult.Success(Unit)
        } catch (e: Exception) {
            AppResult.Error(AppError.Database("Medya mesajı gönderilemedi.", e))
        }
    }

    override suspend fun editDirectMessage(conversationId: String, messageId: String, text: String): AppResult<Unit> {
        val uid = auth.currentUser?.uid ?: return AppResult.Error(AppError.Auth("Giriş yapmalısın."))
        val clean = text.trim()
        if (conversationId.isBlank() || messageId.isBlank()) return AppResult.Error(AppError.Validation("Geçersiz mesaj."))
        if (clean.isBlank()) return AppResult.Error(AppError.Validation("Mesaj boş olamaz."))
        if (clean.length > 1000) return AppResult.Error(AppError.Validation("Mesaj en fazla 1000 karakter olabilir."))
        return try {
            val ref = conversationsRef.document(conversationId).collection("messages").document(messageId)
            val snapshot = ref.get().await()
            if (!snapshot.exists()) return AppResult.Error(AppError.Validation("Mesaj bulunamadı."))
            if (snapshot.getString("senderId") != uid) return AppResult.Error(AppError.Auth("Sadece kendi mesajını düzenleyebilirsin."))
            if (snapshot.getString("mediaUrl").orEmpty().isNotBlank()) return AppResult.Error(AppError.Validation("Medya mesajları düzenlenemez."))
            ref.update("text", clean, "editedAt", System.currentTimeMillis()).await()
            conversationsRef.document(conversationId).update("lastMessage", clean, "updatedAt", System.currentTimeMillis()).await()
            AppResult.Success(Unit)
        } catch (e: Exception) {
            AppResult.Error(AppError.Database("Mesaj düzenlenemedi.", e))
        }
    }

    override suspend fun deleteDirectMessage(conversationId: String, messageId: String): AppResult<Unit> {
        val uid = auth.currentUser?.uid ?: return AppResult.Error(AppError.Auth("Giriş yapmalısın."))
        return try {
            val ref = conversationsRef.document(conversationId).collection("messages").document(messageId)
            val snapshot = ref.get().await()
            if (!snapshot.exists()) return AppResult.Error(AppError.Validation("Mesaj bulunamadı."))
            if (snapshot.getString("senderId") != uid) return AppResult.Error(AppError.Auth("Sadece kendi mesajını silebilirsin."))
            ref.delete().await()
            AppResult.Success(Unit)
        } catch (e: Exception) {
            AppResult.Error(AppError.Database("Mesaj silinemedi.", e))
        }
    }

    override suspend fun toggleDirectMessageReaction(conversationId: String, messageId: String, emoji: String): AppResult<Unit> {
        val uid = auth.currentUser?.uid ?: return AppResult.Error(AppError.Auth("Giriş yapmalısın."))
        val allowed = setOf("❤️", "😂", "😮", "😢", "😡", "👍")
        if (emoji !in allowed) return AppResult.Error(AppError.Validation("Geçersiz tepki."))
        return try {
            val ref = conversationsRef.document(conversationId).collection("messages").document(messageId)
            val snapshot = ref.get().await()
            if (!snapshot.exists()) return AppResult.Error(AppError.Validation("Mesaj bulunamadı."))
            val reactions = (snapshot.get("reactions") as? Map<*, *>)?.mapNotNull { (k, v) ->
                if (k is String && v is String) k to v else null
            }?.toMap().orEmpty()
            if (reactions[uid] == emoji) {
                ref.update("reactions.$uid", com.google.firebase.firestore.FieldValue.delete()).await()
            } else {
                ref.update("reactions.$uid", emoji).await()
            }
            AppResult.Success(Unit)
        } catch (e: Exception) {
            AppResult.Error(AppError.Database("Tepki bırakılamadı.", e))
        }
    }

    override suspend fun markDirectConversationRead(conversationId: String): AppResult<Unit> {
        val uid = auth.currentUser?.uid ?: return AppResult.Error(AppError.Auth("Giriş yapmalısın."))
        if (conversationId.isBlank()) return AppResult.Error(AppError.Validation("Geçersiz sohbet."))
        return try {
            conversationsRef.document(conversationId).update(
                mapOf(
                    "unreadCount_" + uid to 0L,
                    "lastReadAt_" + uid to System.currentTimeMillis()
                )
            ).await()
            AppResult.Success(Unit)
        } catch (e: Exception) {
            AppResult.Error(AppError.Database("Sohbet okundu olarak işaretlenemedi.", e))
        }
    }

    override fun observeCommunityServers(): Flow<AppResult<List<CommunityServer>>> = callbackFlow {
        val registration = serversRef
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(AppResult.Error(AppError.Database("Sunucular yüklenemedi.", error)))
                    return@addSnapshotListener
                }
                val servers = snapshot?.documents.orEmpty().mapNotNull { doc ->
                    runCatching {
                        doc.toObject(CommunityServer::class.java)?.copy(id = doc.id)
                    }.getOrNull()
                }
                trySend(AppResult.Success(servers))
            }
        awaitClose { registration.remove() }
    }

    override suspend fun createCommunityServer(name: String, description: String): AppResult<CommunityServer> {
        val user = auth.currentUser ?: return AppResult.Error(AppError.Auth("Sunucu oluşturmak için giriş yapmalısın."))
        val cleanName = name.trim()
        val cleanDescription = description.trim()
        if (cleanName.length < 2) return AppResult.Error(AppError.Validation("Sunucu adı en az 2 karakter olmalı."))
        if (cleanName.length > 40) return AppResult.Error(AppError.Validation("Sunucu adı en fazla 40 karakter olabilir."))
        if (cleanDescription.length > 160) return AppResult.Error(AppError.Validation("Açıklama en fazla 160 karakter olabilir."))
        return try {
            val profile = (userRepository.getUserProfileFresh(user.uid) as? AppResult.Success)?.data
                ?: return AppResult.Error(AppError.Auth("Profil bulunamadı."))
            val ref = serversRef.document()
            val server = CommunityServer(
                id = ref.id,
                name = cleanName,
                description = cleanDescription,
                ownerId = user.uid,
                createdAt = System.currentTimeMillis()
            )
            val batch = firestore.batch()
            batch.set(ref, server)
            batch.set(ref.collection("members").document(user.uid), mapOf(
                "uid" to user.uid,
                "displayName" to profile.displayName,
                "username" to profile.username,
                "photoUrl" to profile.profileImageUrl,
                "role" to "OWNER",
                "joinedAt" to System.currentTimeMillis()
            ))
            batch.commit().await()
            AppResult.Success(server)
        } catch (e: Exception) {
            AppResult.Error(AppError.Database("Sunucu oluşturulamadı.", e))
        }
    }

    override suspend fun updateCommunityServer(
        serverId: String,
        name: String,
        description: String
    ): AppResult<Unit> {
        val user = auth.currentUser ?: return AppResult.Error(AppError.Auth("Giriş yapmalısın."))
        val cleanName = name.trim()
        val cleanDescription = description.trim()
        if (serverId.isBlank()) return AppResult.Error(AppError.Validation("Geçersiz sunucu."))
        if (cleanName.length < 2) return AppResult.Error(AppError.Validation("Sunucu adı en az 2 karakter olmalı."))
        if (cleanName.length > 40) return AppResult.Error(AppError.Validation("Sunucu adı en fazla 40 karakter olabilir."))
        if (cleanDescription.length > 160) return AppResult.Error(AppError.Validation("Açıklama en fazla 160 karakter olabilir."))

        return try {
            val ref = serversRef.document(serverId)
            val server = ref.get().await().toObject(CommunityServer::class.java)
                ?: return AppResult.Error(AppError.Validation("Sunucu bulunamadı."))
            if (server.ownerId != user.uid) {
                return AppResult.Error(AppError.Auth("Sadece sunucu sahibi düzenleyebilir."))
            }
            ref.update(
                mapOf(
                    "name" to cleanName,
                    "description" to cleanDescription
                )
            ).await()
            AppResult.Success(Unit)
        } catch (e: Exception) {
            AppResult.Error(AppError.Database("Sunucu güncellenemedi.", e))
        }
    }

    override suspend fun joinCommunityServer(serverId: String): AppResult<Unit> {
        val user = auth.currentUser ?: return AppResult.Error(AppError.Auth("Sunucuya katılmak için giriş yapmalısın."))
        if (serverId.isBlank()) return AppResult.Error(AppError.Validation("Geçersiz sunucu."))
        return try {
            val serverRef = serversRef.document(serverId)
            if (!serverRef.get().await().exists()) {
                return AppResult.Error(AppError.Validation("Sunucu bulunamadı."))
            }

            val memberRef = serverRef.collection("members").document(user.uid)
            val memberSnapshot = memberRef.get().await()
            val serverOwnerId = serverRef.get().await().getString("ownerId").orEmpty()

            if (!memberSnapshot.exists()) {
                memberRef.set(
                    mapOf(
                        "uid" to user.uid,
                        "displayName" to (user.displayName ?: ""),
                        "role" to if (serverOwnerId == user.uid) "OWNER" else "MEMBER",
                        "joinedAt" to System.currentTimeMillis()
                    )
                ).await()
            } else if (serverOwnerId == user.uid && memberSnapshot.getString("role") != "OWNER") {
                // Eski sürüm sahibi yanlışlıkla MEMBER olduysa güvenli şekilde düzelt.
                memberRef.delete().await()
                memberRef.set(
                    mapOf(
                        "uid" to user.uid,
                        "displayName" to (user.displayName ?: ""),
                        "role" to "OWNER",
                        "joinedAt" to (memberSnapshot.getLong("joinedAt") ?: System.currentTimeMillis())
                    )
                ).await()
            }
            AppResult.Success(Unit)
        } catch (e: Exception) {
            AppResult.Error(AppError.Database("Sunucuya katılınamadı.", e))
        }
    }

    override fun observeServerMembers(serverId: String): Flow<AppResult<List<ServerMember>>> = callbackFlow {
        val registration = serversRef.document(serverId).collection("members")
            .orderBy("joinedAt", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(AppResult.Error(AppError.Database("Üyeler yüklenemedi.", error)))
                    return@addSnapshotListener
                }
                val members = snapshot?.documents.orEmpty().mapNotNull { doc ->
                    runCatching { doc.toObject(ServerMember::class.java)?.copy(uid = doc.id) }.getOrNull()
                }
                trySend(AppResult.Success(members))
            }
        awaitClose { registration.remove() }
    }

    override suspend fun setServerMemberRole(serverId: String, memberId: String, role: String): AppResult<Unit> {
        val user = auth.currentUser ?: return AppResult.Error(AppError.Auth("Giriş yapmalısın."))
        if (role !in setOf("ADMIN", "MEMBER")) return AppResult.Error(AppError.Validation("Geçersiz rol."))
        return try {
            val serverRef = serversRef.document(serverId)
            val server = serverRef.get().await().toObject(CommunityServer::class.java)
                ?: return AppResult.Error(AppError.Validation("Sunucu bulunamadı."))
            if (server.ownerId != user.uid) return AppResult.Error(AppError.Auth("Sadece sunucu sahibi rol değiştirebilir."))
            if (memberId == server.ownerId) return AppResult.Error(AppError.Validation("Sahibin rolü değiştirilemez."))
            serverRef.collection("members").document(memberId).update("role", role).await()
            AppResult.Success(Unit)
        } catch (e: Exception) {
            AppResult.Error(AppError.Database("Üye rolü değiştirilemedi.", e))
        }
    }

    override suspend fun removeServerMember(serverId: String, memberId: String): AppResult<Unit> {
        val user = auth.currentUser ?: return AppResult.Error(AppError.Auth("Giriş yapmalısın."))
        return try {
            val serverRef = serversRef.document(serverId)
            val server = serverRef.get().await().toObject(CommunityServer::class.java)
                ?: return AppResult.Error(AppError.Validation("Sunucu bulunamadı."))
            if (server.ownerId != user.uid) return AppResult.Error(AppError.Auth("Sadece sunucu sahibi üye çıkarabilir."))
            if (memberId == server.ownerId) return AppResult.Error(AppError.Validation("Sunucu sahibi çıkarılamaz."))
            serverRef.collection("members").document(memberId).delete().await()
            AppResult.Success(Unit)
        } catch (e: Exception) {
            AppResult.Error(AppError.Database("Üye çıkarılamadı.", e))
        }
    }

    override suspend fun leaveCommunityServer(serverId: String): AppResult<Unit> {
        val user = auth.currentUser ?: return AppResult.Error(AppError.Auth("Giriş yapmalısın."))
        return try {
            serversRef.document(serverId).collection("members").document(user.uid).delete().await()
            AppResult.Success(Unit)
        } catch (e: Exception) {
            AppResult.Error(AppError.Database("Sunucudan ayrılamadın.", e))
        }
    }

    override fun observeServerMessages(serverId: String, limit: Long): Flow<AppResult<List<ServerMessage>>> = callbackFlow {
        val registration = serversRef.document(serverId).collection("messages")
            .orderBy("createdAt", Query.Direction.ASCENDING)
            .limitToLast(limit)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(AppResult.Error(AppError.Database("Sunucu sohbeti yüklenemedi.", error)))
                    return@addSnapshotListener
                }
                val messages = snapshot?.documents.orEmpty().mapNotNull { doc ->
                    runCatching { doc.toObject(ServerMessage::class.java)?.copy(id = doc.id) }.getOrNull()
                }
                trySend(AppResult.Success(messages))
            }
        awaitClose { registration.remove() }
    }

    override suspend fun sendServerMessage(serverId: String, text: String, replyTo: ServerMessage?): AppResult<Unit> {
        val user = auth.currentUser ?: return AppResult.Error(AppError.Auth("Mesaj için giriş yapmalısın."))
        val clean = text.trim()
        if (serverId.isBlank()) return AppResult.Error(AppError.Validation("Geçersiz sunucu."))
        if (clean.isBlank()) return AppResult.Error(AppError.Validation("Mesaj boş olamaz."))
        if (clean.length > 1000) return AppResult.Error(AppError.Validation("Mesaj en fazla 1000 karakter olabilir."))
        return try {
            val profile = (userRepository.getUserProfileFresh(user.uid) as? AppResult.Success)?.data
                ?: return AppResult.Error(AppError.Auth("Profil bulunamadı."))
            serversRef.document(serverId).collection("messages").add(
                ServerMessage(
                    senderId = user.uid,
                    senderName = profile.displayName,
                    senderUsername = profile.username,
                    senderPhotoUrl = profile.profileImageUrl,
                    text = clean,
                    createdAt = System.currentTimeMillis(),
                    replyToMessageId = replyTo?.id.orEmpty(),
                    replyToText = replyTo?.text?.ifBlank { if (replyTo.mediaUrl.isNotBlank()) "📷 Fotoğraf" else "" }.orEmpty(),
                    replyToSenderId = replyTo?.senderId.orEmpty(),
                    replyToSenderName = replyTo?.senderName.orEmpty()
                )
            ).await()
            AppResult.Success(Unit)
        } catch (e: Exception) {
            AppResult.Error(AppError.Database("Mesaj gönderilemedi.", e))
        }
    }

    override suspend fun sendServerMediaMessage(
        serverId: String,
        mediaUrl: String,
        mediaType: String,
        text: String,
        replyTo: ServerMessage?
    ): AppResult<Unit> {
        val user = auth.currentUser ?: return AppResult.Error(AppError.Auth("Medya göndermek için giriş yapmalısın."))
        val clean = text.trim()
        if (serverId.isBlank() || mediaUrl.isBlank()) return AppResult.Error(AppError.Validation("Geçersiz medya mesajı."))
        if (clean.length > 1000) return AppResult.Error(AppError.Validation("Mesaj en fazla 1000 karakter olabilir."))
        return try {
            val profile = (userRepository.getUserProfileFresh(user.uid) as? AppResult.Success)?.data
                ?: return AppResult.Error(AppError.Auth("Profil bulunamadı."))
            serversRef.document(serverId).collection("messages").add(
                ServerMessage(
                    senderId = user.uid,
                    senderName = profile.displayName,
                    senderUsername = profile.username,
                    senderPhotoUrl = profile.profileImageUrl,
                    text = clean,
                    mediaUrl = mediaUrl,
                    mediaType = mediaType,
                    createdAt = System.currentTimeMillis(),
                    replyToMessageId = replyTo?.id.orEmpty(),
                    replyToText = replyTo?.text?.ifBlank { "📷 Fotoğraf" }.orEmpty(),
                    replyToSenderId = replyTo?.senderId.orEmpty(),
                    replyToSenderName = replyTo?.senderName.orEmpty()
                )
            ).await()
            AppResult.Success(Unit)
        } catch (e: Exception) {
            AppResult.Error(AppError.Database("Medya mesajı gönderilemedi.", e))
        }
    }

    override suspend fun editServerMessage(serverId: String, messageId: String, text: String): AppResult<Unit> =
        editCommunityLikeMessage(serversRef.document(serverId).collection("messages").document(messageId), text, "Sunucu mesajı")

    override suspend fun deleteServerMessage(serverId: String, messageId: String): AppResult<Unit> =
        deleteCommunityLikeMessage(serversRef.document(serverId).collection("messages").document(messageId))

    override suspend fun toggleServerMessageReaction(serverId: String, messageId: String, emoji: String): AppResult<Unit> =
        toggleCommunityLikeReaction(serversRef.document(serverId).collection("messages").document(messageId), emoji)

    private suspend fun editCommunityLikeMessage(
        ref: com.google.firebase.firestore.DocumentReference,
        text: String,
        label: String
    ): AppResult<Unit> {
        val uid = auth.currentUser?.uid ?: return AppResult.Error(AppError.Auth("Giriş yapmalısın."))
        val clean = text.trim()
        if (clean.isBlank() || clean.length > 1000) return AppResult.Error(AppError.Validation("Mesaj 1-1000 karakter arasında olmalı."))
        return try {
            val snapshot = ref.get().await()
            if (!snapshot.exists()) return AppResult.Error(AppError.Validation("Mesaj bulunamadı."))
            if (snapshot.getString("senderId") != uid) return AppResult.Error(AppError.Auth("Sadece kendi mesajını düzenleyebilirsin."))
            if (snapshot.getString("mediaUrl").orEmpty().isNotBlank()) return AppResult.Error(AppError.Validation("Medya mesajları düzenlenemez."))
            ref.update("text", clean, "editedAt", System.currentTimeMillis()).await()
            AppResult.Success(Unit)
        } catch (e: Exception) {
            AppResult.Error(AppError.Database("$label düzenlenemedi.", e))
        }
    }

    private suspend fun deleteCommunityLikeMessage(
        ref: com.google.firebase.firestore.DocumentReference
    ): AppResult<Unit> {
        val uid = auth.currentUser?.uid ?: return AppResult.Error(AppError.Auth("Giriş yapmalısın."))
        return try {
            val snapshot = ref.get().await()
            if (!snapshot.exists()) return AppResult.Error(AppError.Validation("Mesaj bulunamadı."))
            if (snapshot.getString("senderId") != uid) return AppResult.Error(AppError.Auth("Sadece kendi mesajını silebilirsin."))
            ref.delete().await()
            AppResult.Success(Unit)
        } catch (e: Exception) {
            AppResult.Error(AppError.Database("Mesaj silinemedi.", e))
        }
    }

    private suspend fun toggleCommunityLikeReaction(
        ref: com.google.firebase.firestore.DocumentReference,
        emoji: String
    ): AppResult<Unit> {
        val uid = auth.currentUser?.uid ?: return AppResult.Error(AppError.Auth("Giriş yapmalısın."))
        val allowed = setOf("❤️", "😂", "😮", "😢", "😡", "👍")
        if (emoji !in allowed) return AppResult.Error(AppError.Validation("Geçersiz tepki."))
        return try {
            val snapshot = ref.get().await()
            if (!snapshot.exists()) return AppResult.Error(AppError.Validation("Mesaj bulunamadı."))
            val reactions = (snapshot.get("reactions") as? Map<*, *>)?.mapNotNull { (k, v) ->
                if (k is String && v is String) k to v else null
            }?.toMap().orEmpty()
            if (reactions[uid] == emoji) {
                ref.update("reactions.$uid", com.google.firebase.firestore.FieldValue.delete()).await()
            } else {
                ref.update("reactions.$uid", emoji).await()
            }
            AppResult.Success(Unit)
        } catch (e: Exception) {
            AppResult.Error(AppError.Database("Tepki bırakılamadı.", e))
        }
    }


}
