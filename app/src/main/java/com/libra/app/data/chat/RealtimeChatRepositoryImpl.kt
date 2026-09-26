package com.libra.app.data.chat

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ServerValue
import com.google.firebase.firestore.FirebaseFirestore
import com.libra.app.core.result.AppError
import com.libra.app.core.result.AppResult
import com.libra.app.domain.model.AppNotification
import com.libra.app.domain.model.DirectConversation
import com.libra.app.domain.model.DirectMessage
import com.libra.app.domain.model.GlobalChatMessage
import com.libra.app.domain.model.ServerMessage
import com.libra.app.domain.model.SharedContent
import com.libra.app.domain.model.UserProfile
import com.libra.app.domain.repository.ChatRepository
import com.libra.app.domain.repository.NotificationRepository
import com.libra.app.domain.repository.StorageRepository
import com.libra.app.domain.repository.UserRepository
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.delay

private const val LIBRA_RTDB_URL =
    "https://libra-3bfb9-default-rtdb.europe-west1.firebasedatabase.app"

/**
 * Single realtime chat implementation.
 *
 * All chat features are pinned to the regional RTDB instance used by this
 * Firebase project. This avoids SDK/default-instance resolution differences
 * across devices and keeps global chat, DMs, and server chat on one database.
 *
 * Message writes are independent from secondary metadata (conversation
 * summaries and notifications). A summary/notification failure must never
 * make a successfully written message disappear.
 */
class RealtimeChatRepositoryImpl(
    private val userRepository: UserRepository,
    private val storageRepository: StorageRepository,
    private val notificationRepository: NotificationRepository
) : ChatRepository {

    private val auth = FirebaseAuth.getInstance()

    private fun database(): FirebaseDatabase? =
        runCatching { FirebaseDatabase.getInstance(LIBRA_RTDB_URL) }.getOrNull()

    private fun ref(path: String): DatabaseReference? =
        database()?.getReference(path)

    private fun currentUid(): String? =
        auth.currentUser?.uid?.takeIf { it.isNotBlank() }

    private fun error(message: String, cause: Exception? = null): AppResult.Error =
        AppResult.Error(AppError.Database(message, cause))

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
                reactions = reactions(snapshot.child("reactions")),
                sharedContent = sharedContent(snapshot.child("sharedContent"))
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
                reactions = reactions(snapshot.child("reactions")),
                mentionedUserIds = snapshot.child("mentionedUserIds").children.mapNotNull { it.getValue(String::class.java) },
                mentionsEveryone = snapshot.child("mentionsEveryone").getValue(Boolean::class.java) ?: false,
                mentionsHere = snapshot.child("mentionsHere").getValue(Boolean::class.java) ?: false,
                sharedContent = sharedContent(snapshot.child("sharedContent"))
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
                reactions = reactions(snapshot.child("reactions"))
            )
        }.getOrNull()


    private fun sharedContent(snapshot: DataSnapshot): SharedContent? {
        if (!snapshot.exists()) return null
        val type = snapshot.child("type").getValue(String::class.java).orEmpty()
        val id = snapshot.child("id").getValue(String::class.java).orEmpty()
        if (type.isBlank() || id.isBlank()) return null
        return SharedContent(
            type = type,
            id = id,
            title = snapshot.child("title").getValue(String::class.java).orEmpty(),
            text = snapshot.child("text").getValue(String::class.java).orEmpty(),
            authorId = snapshot.child("authorId").getValue(String::class.java).orEmpty(),
            authorName = snapshot.child("authorName").getValue(String::class.java).orEmpty(),
            mediaUrl = snapshot.child("mediaUrl").getValue(String::class.java).orEmpty(),
            url = snapshot.child("url").getValue(String::class.java).orEmpty()
        )
    }

    private fun sharedContentData(content: SharedContent?): Map<String, Any>? =
        content?.let {
            mapOf(
                "type" to it.type,
                "id" to it.id,
                "title" to it.title,
                "text" to it.text,
                "authorId" to it.authorId,
                "authorName" to it.authorName,
                "mediaUrl" to it.mediaUrl,
                "url" to it.url
            )
        }

    private fun reactions(snapshot: DataSnapshot): Map<String, String> =
        buildMap {
            snapshot.children.forEach { child ->
                val uid = child.key.orEmpty()
                val emoji = child.getValue(String::class.java)
                if (uid.isNotBlank() && !emoji.isNullOrBlank()) put(uid, emoji)
            }
        }

    private fun summaryData(
        otherUserId: String,
        otherUserName: String,
        otherUserUsername: String,
        otherUserPhotoUrl: String,
        lastMessage: String,
        updatedAt: Long,
        unreadCount: Any
    ): Map<String, Any> = mapOf(
        "otherUserId" to otherUserId,
        "otherUserName" to otherUserName,
        "otherUserUsername" to otherUserUsername,
        "otherUserPhotoUrl" to otherUserPhotoUrl,
        "lastMessage" to lastMessage,
        "updatedAt" to updatedAt,
        "unreadCount" to unreadCount
    )

    private suspend fun profileOrFallback(uid: String, authName: String = "", authPhoto: String = ""): UserProfile {
        val profile = runCatching {
            userRepository.getUserProfileFresh(uid) as? AppResult.Success
        }.getOrNull()?.data

        return profile ?: UserProfile(
            uid = uid,
            displayName = authName.ifBlank { "Kullanıcı" },
            profileImageUrl = authPhoto
        )
    }

    private fun validateMediaOwner(uid: String, mediaUrl: String, mediaType: String): String? {
        if (mediaUrl.isBlank()) return null
        if (!mediaUrl.startsWith("users/" + uid + "/")) {
            return "Medya yolu geçersiz."
        }
        if (mediaType.isBlank()) return "Medya türü belirlenemedi."
        if (mediaType != "image" && !mediaType.startsWith("image/")) {
            return "Şimdilik yalnızca fotoğraf gönderilebilir."
        }
        return null
    }

    override fun observeGlobalMessages(limit: Long): Flow<AppResult<List<GlobalChatMessage>>> = callbackFlow {
        val safeLimit = limit.coerceIn(1L, 200L).toInt()
        val query = ref("globalMessages")
            ?.orderByChild("createdAt")
            ?.limitToLast(safeLimit)

        if (query == null) {
            trySend(error("Realtime Database yapılandırması bulunamadı."))
            close()
            return@callbackFlow
        }

        val listener = object : com.google.firebase.database.ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                trySend(
                    AppResult.Success(
                        snapshot.children
                            .mapNotNull(::globalMessageFrom)
                            .sortedBy { it.createdAt }
                    )
                )
            }

            override fun onCancelled(error: DatabaseError) {
                trySend(this@RealtimeChatRepositoryImpl.error(error.message, error.toException()))
            }
        }

        query.addValueEventListener(listener)
        awaitClose { query.removeEventListener(listener) }
    }

    override suspend fun sendGlobalMessage(
        text: String,
        replyTo: GlobalChatMessage?,
        sharedContent: SharedContent?
    ): AppResult<Unit> =
        sendGlobalInternal(text, "", "", replyTo, sharedContent)

    override suspend fun sendGlobalMediaMessage(
        mediaUrl: String,
        mediaType: String,
        text: String,
        replyTo: GlobalChatMessage?,
        sharedContent: SharedContent?
    ): AppResult<Unit> =
        sendGlobalInternal(text, mediaUrl, mediaType, replyTo, sharedContent)

    private suspend fun sendGlobalInternal(
        text: String,
        mediaUrl: String,
        mediaType: String,
        replyTo: GlobalChatMessage?,
        sharedContent: SharedContent?
    ): AppResult<Unit> {
        val user = auth.currentUser
            ?: return AppResult.Error(AppError.Auth("Sohbet için giriş yapmalısın."))

        val clean = text.trim()
        if (clean.isBlank() && mediaUrl.isBlank()) {
            return AppResult.Error(AppError.Validation("Mesaj boş olamaz."))
        }
        if (clean.length > 1000) {
            return AppResult.Error(AppError.Validation("Mesaj en fazla 1000 karakter olabilir."))
        }

        validateMediaOwner(user.uid, mediaUrl, mediaType)?.let {
            return AppResult.Error(AppError.Validation(it))
        }

        val profile = profileOrFallback(
            uid = user.uid,
            authName = user.displayName.orEmpty(),
            authPhoto = user.photoUrl?.toString().orEmpty()
        )

        val messageRef = ref("globalMessages")?.push()
            ?: return error("Realtime Database yapılandırması bulunamadı.")

        val now = System.currentTimeMillis()
        val data = mapOf(
            "senderId" to user.uid,
            "senderName" to profile.displayName,
            "senderUsername" to profile.username,
            "senderPhotoUrl" to profile.profileImageUrl,
            "text" to clean,
            "mediaUrl" to mediaUrl,
            "mediaType" to mediaType,
            "createdAt" to now,
            "replyToMessageId" to (replyTo?.id ?: ""),
            "replyToText" to replyText(replyTo?.text, replyTo?.mediaUrl),
            "replyToSenderId" to (replyTo?.senderId ?: ""),
            "replyToSenderName" to (replyTo?.senderName ?: ""),
            "sharedContent" to (sharedContentData(sharedContent) ?: emptyMap<String, Any>()),
            "mentionedUserIds" to mentionInfo.userIds,
            "mentionsEveryone" to mentionInfo.everyone,
            "mentionsHere" to mentionInfo.here
        )

        return try {
            messageRef.setValue(data).await()
            runCatching {
                val recipients = mentionInfo.userIds.filter { it != user.uid }.distinct()
                val mentionLabel = when {
                    mentionInfo.everyone -> "@everyone"
                    mentionInfo.here -> "@here"
                    else -> "bahsedilme"
                }
                recipients.forEach { recipientId ->
                    notificationRepository.create(
                        AppNotification(
                            recipientId = recipientId,
                            actorId = user.uid,
                            actorName = profile.displayName,
                            actorUsername = profile.username,
                            actorPhotoUrl = profile.profileImageUrl,
                            type = "SERVER_MENTION",
                            title = "Sunucuda bahsedildin",
                            body = if (mentionLabel.startsWith("@")) {
                                profile.displayName + " " + mentionLabel + " ile senden bahsetti: " + clean.take(120)
                            } else {
                                profile.displayName + " senden bahsetti: " + clean.take(120)
                            },
                            referenceId = serverId + ":" + channelId + ":" + messageRef.key.orEmpty(),
                            createdAt = System.currentTimeMillis()
                        )
                    )
                }
            }
            AppResult.Success(Unit)
        } catch (e: Exception) {
            cleanupMedia(mediaUrl)
            error("Mesaj gönderilemedi.", e)
        }
    }

    override suspend fun editGlobalMessage(messageId: String, text: String): AppResult<Unit> =
        editMessage(ref("globalMessages/$messageId"), text, 1000)

    override suspend fun deleteGlobalMessage(messageId: String): AppResult<Unit> =
        deleteMessage(ref("globalMessages/$messageId"))

    override suspend fun toggleGlobalMessageReaction(
        messageId: String,
        emoji: String
    ): AppResult<Unit> =
        toggleReaction(ref("globalMessages/$messageId"), emoji)

    override fun observeDirectConversations(uid: String): Flow<AppResult<List<DirectConversation>>> =
        callbackFlow {
            if (uid.isBlank()) {
                trySend(AppResult.Success(emptyList()))
                close()
                return@callbackFlow
            }

            val query = ref("directConversations/$uid")
                ?.orderByChild("updatedAt")
                ?.limitToLast(100)

            if (query == null) {
                trySend(error("Realtime Database yapılandırması bulunamadı."))
                close()
                return@callbackFlow
            }

            val listener = object : com.google.firebase.database.ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val items = snapshot.children.mapNotNull { child ->
                        val otherUid = child.child("otherUserId")
                            .getValue(String::class.java)
                            .orEmpty()

                        if (child.key.isNullOrBlank() || otherUid.isBlank()) {
                            return@mapNotNull null
                        }

                        DirectConversation(
                            id = child.key.orEmpty(),
                            participants = listOf(uid, otherUid).distinct(),
                            otherUserId = otherUid,
                            otherUserName = child.child("otherUserName")
                                .getValue(String::class.java).orEmpty(),
                            otherUserUsername = child.child("otherUserUsername")
                                .getValue(String::class.java).orEmpty(),
                            otherUserPhotoUrl = child.child("otherUserPhotoUrl")
                                .getValue(String::class.java).orEmpty(),
                            lastMessage = child.child("lastMessage")
                                .getValue(String::class.java).orEmpty(),
                            updatedAt = child.child("updatedAt")
                                .getValue(Long::class.java) ?: 0L,
                            unreadCount = (
                                child.child("unreadCount")
                                    .getValue(Long::class.java) ?: 0L
                                ).toInt().coerceAtLeast(0)
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

    override fun observeDirectMessages(
        conversationId: String,
        limit: Long
    ): Flow<AppResult<List<DirectMessage>>> = callbackFlow {
        val uid = currentUid()
        if (uid == null) {
            trySend(AppResult.Error(AppError.Auth("Oturum bulunamadı.")))
            close()
            return@callbackFlow
        }

        val participants = conversationId.split("_")
        if (participants.size != 2 || uid !in participants) {
            trySend(AppResult.Error(AppError.Auth("Bu sohbeti görüntüleme yetkin yok.")))
            close()
            return@callbackFlow
        }

        val safeLimit = limit.coerceIn(1L, 200L).toInt()
        val query = ref("directMessages/$conversationId")
            ?.orderByChild("createdAt")
            ?.limitToLast(safeLimit)

        if (query == null) {
            trySend(error("Realtime Database yapılandırması bulunamadı."))
            close()
            return@callbackFlow
        }

        val listener = object : com.google.firebase.database.ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val messages = snapshot.children
                    .mapNotNull(::directMessageFrom)
                    .filter { it.senderId == uid || it.recipientId == uid }
                    .sortedBy { it.createdAt }

                trySend(AppResult.Success(messages))
            }

            override fun onCancelled(error: DatabaseError) {
                trySend(this@RealtimeChatRepositoryImpl.error(error.message, error.toException()))
            }
        }

        query.addValueEventListener(listener)
        awaitClose { query.removeEventListener(listener) }
    }

    override suspend fun sendDirectMessage(
        recipientId: String,
        text: String,
        replyTo: DirectMessage?,
        sharedContent: SharedContent?
    ): AppResult<Unit> =
        sendDirectInternal(recipientId, text, "", "", replyTo, sharedContent)

    override suspend fun sendDirectMediaMessage(
        recipientId: String,
        mediaUrl: String,
        mediaType: String,
        text: String,
        replyTo: DirectMessage?,
        sharedContent: SharedContent?
    ): AppResult<Unit> =
        sendDirectInternal(recipientId, text, mediaUrl, mediaType, replyTo, sharedContent)

    private suspend fun sendDirectInternal(
        recipientId: String,
        text: String,
        mediaUrl: String,
        mediaType: String,
        replyTo: DirectMessage?,
        sharedContent: SharedContent?
    ): AppResult<Unit> {
        val sender = auth.currentUser
            ?: return AppResult.Error(AppError.Auth("Mesaj göndermek için giriş yapmalısın."))

        if (recipientId.isBlank() || recipientId == sender.uid) {
            return AppResult.Error(AppError.Validation("Geçerli bir alıcı seçilmedi."))
        }

        val clean = text.trim()
        if (clean.isBlank() && mediaUrl.isBlank()) {
            return AppResult.Error(AppError.Validation("Mesaj boş olamaz."))
        }
        if (clean.length > 2000) {
            return AppResult.Error(AppError.Validation("Mesaj en fazla 2000 karakter olabilir."))
        }

        validateMediaOwner(sender.uid, mediaUrl, mediaType)?.let {
            return AppResult.Error(AppError.Validation(it))
        }

        // The message itself depends only on Firebase Auth + RTDB.
        // Firestore profiles are best-effort metadata, never a prerequisite.
        val senderProfile = profileOrFallback(
            uid = sender.uid,
            authName = sender.displayName.orEmpty(),
            authPhoto = sender.photoUrl?.toString().orEmpty()
        )

        val conversation = conversationId(sender.uid, recipientId)
        val messageRef = ref("directMessages/$conversation")?.push()
            ?: return error("Realtime Database yapılandırması bulunamadı.")

        val now = System.currentTimeMillis()
        val message = mapOf(
            "senderId" to sender.uid,
            "senderPhotoUrl" to senderProfile.profileImageUrl,
            "recipientId" to recipientId,
            "text" to clean,
            "mediaUrl" to mediaUrl,
            "mediaType" to mediaType,
            "createdAt" to now,
            "replyToMessageId" to (replyTo?.id ?: ""),
            "replyToText" to replyText(replyTo?.text, replyTo?.mediaUrl),
            "replyToSenderId" to (replyTo?.senderId ?: ""),
            "replyToSenderName" to (replyTo?.replyToSenderName
                ?.ifBlank {
                    if (replyTo.senderId == sender.uid) senderProfile.displayName else ""
                }
                ?: ""),
            "sharedContent" to (sharedContentData(sharedContent) ?: emptyMap<String, Any>())
        )

        return try {
            // This is the only operation that determines send success.
            messageRef.setValue(message).await()

            // Secondary indexes/notifications are deliberately best-effort.
            writeDirectConversationSummaries(
                senderUid = sender.uid,
                recipientUid = recipientId,
                conversationId = conversation,
                senderProfile = senderProfile,
                timestamp = now,
                lastMessage = lastMessage(clean, mediaUrl, sharedContent)
            )

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
            cleanupMedia(mediaUrl)
            error("Mesaj gönderilemedi.", e)
        }
    }

    private suspend fun writeDirectConversationSummaries(
        senderUid: String,
        recipientUid: String,
        conversationId: String,
        senderProfile: UserProfile,
        timestamp: Long,
        lastMessage: String
    ) {
        val root = database()?.reference ?: return
        val recipientProfile = profileOrFallback(recipientUid)

        val updates = mapOf(
            "directConversations/$senderUid/$conversationId" to summaryData(
                otherUserId = recipientUid,
                otherUserName = recipientProfile.displayName,
                otherUserUsername = recipientProfile.username,
                otherUserPhotoUrl = recipientProfile.profileImageUrl,
                lastMessage = lastMessage,
                updatedAt = timestamp,
                unreadCount = 0
            ),
            "directConversations/$recipientUid/$conversationId" to summaryData(
                otherUserId = senderUid,
                otherUserName = senderProfile.displayName,
                otherUserUsername = senderProfile.username,
                otherUserPhotoUrl = senderProfile.profileImageUrl,
                lastMessage = lastMessage,
                updatedAt = timestamp,
                unreadCount = ServerValue.increment(1)
            )
        )

        repeat(3) { attempt ->
            try {
                root.updateChildren(updates).await()
                return
            } catch (_: Exception) {
                if (attempt < 2) delay(400L * (attempt + 1))
            }
        }

        // The message itself is already persisted. Summary failure is retried
        // here but must never turn a successful message send into a failed send.
    }

    override suspend fun editDirectMessage(
        conversationId: String,
        messageId: String,
        text: String
    ): AppResult<Unit> {
        val result = editMessage(
            ref("directMessages/$conversationId/$messageId"),
            text,
            2000
        )
        if (result is AppResult.Success) {
            runCatching { refreshDirectConversationSummary(conversationId) }
        }
        return result
    }

    override suspend fun deleteDirectMessage(
        conversationId: String,
        messageId: String
    ): AppResult<Unit> {
        val result = deleteMessage(
            ref("directMessages/$conversationId/$messageId")
        )
        if (result is AppResult.Success) {
            runCatching { refreshDirectConversationSummary(conversationId) }
        }
        return result
    }

    override suspend fun toggleDirectMessageReaction(
        conversationId: String,
        messageId: String,
        emoji: String
    ): AppResult<Unit> =
        toggleReaction(ref("directMessages/$conversationId/$messageId"), emoji)

    override suspend fun markDirectConversationRead(conversationId: String): AppResult<Unit> {
        val uid = currentUid()
            ?: return AppResult.Error(AppError.Auth("Oturum bulunamadı."))

        val summaryRef = ref("directConversations/$uid/$conversationId")
            ?: return error("Realtime Database yapılandırması bulunamadı.")

        return try {
            val snapshot = summaryRef.get().await()
            if (!snapshot.exists()) {
                return AppResult.Success(Unit)
            }

            summaryRef.child("unreadCount").setValue(0).await()
            AppResult.Success(Unit)
        } catch (e: Exception) {
            // Read receipts are metadata. A permission/rule mismatch here must not
            // make opening or sending a conversation look broken.
            AppResult.Success(Unit)
        }
    }

    override fun observeServerMessages(
        serverId: String,
        limit: Long,
        channelId: String
    ): Flow<AppResult<List<ServerMessage>>> = callbackFlow {
        if (serverId.isBlank()) {
            trySend(AppResult.Success(emptyList()))
            close()
            return@callbackFlow
        }

        val safeLimit = limit.coerceIn(1L, 200L).toInt()
        val query = ref(serverMessagesPath(serverId, channelId))
            ?.orderByChild("createdAt")
            ?.limitToLast(safeLimit)

        if (query == null) {
            trySend(error("Realtime Database yapılandırması bulunamadı."))
            close()
            return@callbackFlow
        }

        val listener = object : com.google.firebase.database.ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                trySend(
                    AppResult.Success(
                        snapshot.children
                            .mapNotNull(::serverMessageFrom)
                            .sortedBy { it.createdAt }
                    )
                )
            }

            override fun onCancelled(error: DatabaseError) {
                trySend(this@RealtimeChatRepositoryImpl.error(error.message, error.toException()))
            }
        }

        query.addValueEventListener(listener)
        awaitClose { query.removeEventListener(listener) }
    }

    override suspend fun sendServerMessage(
        serverId: String,
        text: String,
        replyTo: ServerMessage?,
        sharedContent: SharedContent?,
        channelId: String
    ): AppResult<Unit> =
        sendServerInternal(serverId, text, "", "", replyTo, sharedContent, channelId)

    override suspend fun sendServerMediaMessage(
        serverId: String,
        mediaUrl: String,
        mediaType: String,
        text: String,
        replyTo: ServerMessage?,
        sharedContent: SharedContent?,
        channelId: String
    ): AppResult<Unit> =
        sendServerInternal(serverId, text, mediaUrl, mediaType, replyTo, sharedContent, channelId)

    private suspend fun sendServerInternal(
        serverId: String,
        text: String,
        mediaUrl: String,
        mediaType: String,
        replyTo: ServerMessage?,
        sharedContent: SharedContent?,
        channelId: String
    ): AppResult<Unit> {
        val user = auth.currentUser
            ?: return AppResult.Error(AppError.Auth("Mesaj göndermek için giriş yapmalısın."))

        if (serverId.isBlank()) {
            return AppResult.Error(AppError.Validation("Sunucu bulunamadı."))
        }

        if (channelId.isNotBlank() && !canUseServerChannel(serverId, channelId, user.uid, send = true)) {
            return AppResult.Error(AppError.Auth("Bu kanalda mesaj gönderme iznin yok."))
        }

        val clean = text.trim()
        if (clean.isBlank() && mediaUrl.isBlank()) {
            return AppResult.Error(AppError.Validation("Mesaj boş olamaz."))
        }
        if (clean.length > 2000) {
            return AppResult.Error(AppError.Validation("Mesaj en fazla 2000 karakter olabilir."))
        }

        validateMediaOwner(user.uid, mediaUrl, mediaType)?.let {
            return AppResult.Error(AppError.Validation(it))
        }

        val profile = profileOrFallback(
            uid = user.uid,
            authName = user.displayName.orEmpty(),
            authPhoto = user.photoUrl?.toString().orEmpty()
        )

        val mentionInfo = resolveServerMentions(serverId, clean, user.uid)

        val messageRef = ref(serverMessagesPath(serverId, channelId))?.push()
            ?: return error("Realtime Database yapılandırması bulunamadı.")

        val data = mapOf(
            "senderId" to user.uid,
            "senderName" to profile.displayName,
            "senderUsername" to profile.username,
            "senderPhotoUrl" to profile.profileImageUrl,
            "text" to clean,
            "mediaUrl" to mediaUrl,
            "mediaType" to mediaType,
            "createdAt" to System.currentTimeMillis(),
            "replyToMessageId" to (replyTo?.id ?: ""),
            "replyToText" to replyText(replyTo?.text, replyTo?.mediaUrl),
            "replyToSenderId" to (replyTo?.senderId ?: ""),
            "replyToSenderName" to (replyTo?.senderName ?: ""),
            "sharedContent" to (sharedContentData(sharedContent) ?: emptyMap<String, Any>())
        )

        return try {
            messageRef.setValue(data).await()
            AppResult.Success(Unit)
        } catch (e: Exception) {
            cleanupMedia(mediaUrl)
            error("Sunucu mesajı gönderilemedi.", e)
        }
    }

    override suspend fun editServerMessage(
        serverId: String,
        messageId: String,
        text: String,
        channelId: String
    ): AppResult<Unit> =
        editMessage(ref(serverMessagesPath(serverId, channelId) + "/$messageId"), text, 2000)

    override suspend fun deleteServerMessage(
        serverId: String,
        messageId: String,
        channelId: String
    ): AppResult<Unit> =
        deleteMessage(ref(serverMessagesPath(serverId, channelId) + "/$messageId"))

    override suspend fun toggleServerMessageReaction(
        serverId: String,
        messageId: String,
        emoji: String,
        channelId: String
    ): AppResult<Unit> =
        toggleReaction(ref(serverMessagesPath(serverId, channelId) + "/$messageId"), emoji)

    private suspend fun editMessage(
        messageRef: DatabaseReference?,
        text: String,
        maxLength: Int
    ): AppResult<Unit> {
        val uid = currentUid()
            ?: return AppResult.Error(AppError.Auth("Oturum bulunamadı."))

        val target = messageRef
            ?: return error("Realtime Database yapılandırması bulunamadı.")

        val clean = text.trim()
        if (clean.isBlank()) {
            return AppResult.Error(AppError.Validation("Mesaj boş olamaz."))
        }
        if (clean.length > maxLength) {
            return AppResult.Error(
                AppError.Validation("Mesaj en fazla $maxLength karakter olabilir.")
            )
        }

        return try {
            val snapshot = target.get().await()
            if (!snapshot.exists()) {
                return AppResult.Error(AppError.NotFound("Mesaj bulunamadı."))
            }

            if (snapshot.child("senderId").getValue(String::class.java) != uid) {
                return AppResult.Error(AppError.Auth("Bu mesajı düzenleme yetkin yok."))
            }

            if (snapshot.child("mediaUrl").getValue(String::class.java).orEmpty().isNotBlank()) {
                return AppResult.Error(AppError.Validation("Medya mesajları düzenlenemez."))
            }

            target.updateChildren(
                mapOf(
                    "text" to clean,
                    "editedAt" to System.currentTimeMillis()
                )
            ).await()

            AppResult.Success(Unit)
        } catch (e: Exception) {
            error("Mesaj düzenlenemedi.", e)
        }
    }

    private suspend fun deleteMessage(messageRef: DatabaseReference?): AppResult<Unit> {
        val uid = currentUid()
            ?: return AppResult.Error(AppError.Auth("Oturum bulunamadı."))

        val target = messageRef
            ?: return error("Realtime Database yapılandırması bulunamadı.")

        return try {
            val snapshot = target.get().await()
            if (!snapshot.exists()) {
                return AppResult.Success(Unit)
            }

            if (snapshot.child("senderId").getValue(String::class.java) != uid) {
                return AppResult.Error(AppError.Auth("Bu mesajı silme yetkin yok."))
            }

            val mediaKey = snapshot.child("mediaUrl")
                .getValue(String::class.java)
                .orEmpty()

            target.removeValue().await()
            cleanupMedia(mediaKey)

            AppResult.Success(Unit)
        } catch (e: Exception) {
            error("Mesaj silinemedi.", e)
        }
    }

    private suspend fun refreshDirectConversationSummary(conversationId: String) {
        val uid = currentUid() ?: return
        val participants = conversationId.split("_")
        if (participants.size != 2 || uid !in participants) return

        val otherUid = participants.first { it != uid }
        val latest = ref("directMessages/$conversationId")
            ?.orderByChild("createdAt")
            ?.limitToLast(1)
            ?.get()
            ?.await()
            ?.children
            ?.mapNotNull(::directMessageFrom)
            ?.maxByOrNull { it.createdAt }

        val lastMessage = latest?.let {
            lastMessage(it.text, it.mediaUrl)
        }.orEmpty()
        val updatedAt = latest?.createdAt ?: 0L

        val otherProfile = profileOrFallback(otherUid)
        val root = database()?.reference ?: return

        runCatching {
            root.updateChildren(
                mapOf(
                    "directConversations/$uid/$conversationId/lastMessage" to lastMessage,
                    "directConversations/$uid/$conversationId/updatedAt" to updatedAt,
                    "directConversations/$uid/$conversationId/otherUserId" to otherUid,
                    "directConversations/$uid/$conversationId/otherUserName" to otherProfile.displayName,
                    "directConversations/$uid/$conversationId/otherUserUsername" to otherProfile.username,
                    "directConversations/$uid/$conversationId/otherUserPhotoUrl" to otherProfile.profileImageUrl
                )
            ).await()
        }
    }

    private suspend fun canUseServerChannel(
        serverId: String,
        channelId: String,
        uid: String,
        send: Boolean
    ): Boolean {
        return runCatching {
            val firestore = FirebaseFirestore.getInstance()
            val serverRef = firestore.collection("communityServers").document(serverId)
            val server = serverRef.get().await()
            if (!server.exists()) return@runCatching false
            if (server.getString("ownerId") == uid) return@runCatching true

            val member = serverRef.collection("members").document(uid).get().await()
            if (!member.exists()) return@runCatching false
            val role = member.getString("role").orEmpty()

            val channel = serverRef.collection("channels").document(channelId).get().await()
            if (!channel.exists()) return@runCatching false

            var allowed = if (send) {
                channel.getBoolean("allowEveryoneSend") ?: true
            } else {
                channel.getBoolean("allowEveryoneView") ?: true
            }

            val overrides = serverRef.collection("channels").document(channelId)
                .collection("permissions").get().await().documents

            fun apply(subjectType: String, subjectId: String) {
                overrides.firstOrNull {
                    it.getString("subjectType") == subjectType && it.getString("subjectId") == subjectId
                }?.let { doc ->
                    val value = if (send) doc.getBoolean("canSend") else doc.getBoolean("canView")
                    if (value != null) allowed = value
                }
            }

            apply("ROLE", "EVERYONE")
            apply("ROLE", role)
            apply("USER", uid)
            allowed
        }.getOrDefault(false)
    }

    private data class ServerMentionInfo(
        val userIds: List<String>,
        val everyone: Boolean,
        val here: Boolean
    )

    private suspend fun resolveServerMentions(
        serverId: String,
        text: String,
        senderId: String
    ): ServerMentionInfo {
        if (serverId.isBlank() || text.isBlank()) return ServerMentionInfo(emptyList(), false, false)

        return runCatching {
            val memberDocs = FirebaseFirestore.getInstance()
                .collection("communityServers")
                .document(serverId)
                .collection("members")
                .get()
                .await()
                .documents

            val everyone = Regex("(?i)(^|\\s)@everyone\\b").containsMatchIn(text)
            val here = Regex("(?i)(^|\\s)@here\\b").containsMatchIn(text)
            val ids = linkedSetOf<String>()
            if (everyone || here) {
                memberDocs.mapTo(ids) { it.id }
            }

            val usernameMentions = Regex("@([A-Za-z0-9._]{3,30})")
                .findAll(text)
                .map { it.groupValues[1].lowercase() }
                .toSet()

            memberDocs.forEach { doc ->
                val username = doc.getString("username").orEmpty().lowercase()
                if (username.isNotBlank() && username in usernameMentions) ids += doc.id
            }

            ServerMentionInfo(ids.filter { it != senderId }, everyone, here)
        }.getOrElse {
            ServerMentionInfo(emptyList(), false, false)
        }
    }

    private fun serverMessagesPath(serverId: String, channelId: String): String =
        if (channelId.isBlank()) {
            "serverMessages/$serverId"
        } else {
            "serverMessages/$serverId/channels/$channelId"
        }

    private suspend fun cleanupMedia(mediaKey: String) {
        if (mediaKey.isBlank()) return
        runCatching { storageRepository.deleteMedia(mediaKey) }
    }

    private suspend fun toggleReaction(
        messageRef: DatabaseReference?,
        emoji: String
    ): AppResult<Unit> {
        val uid = currentUid()
            ?: return AppResult.Error(AppError.Auth("Oturum bulunamadı."))

        val allowed = setOf("❤️", "😂", "😮", "😢", "😡", "👍")
        if (emoji !in allowed) {
            return AppResult.Error(AppError.Validation("Geçersiz emoji."))
        }

        val target = messageRef
            ?: return error("Realtime Database yapılandırması bulunamadı.")

        return try {
            val message = target.get().await()
            if (!message.exists()) {
                return AppResult.Error(AppError.NotFound("Mesaj bulunamadı."))
            }

            val reactionRef = target.child("reactions").child(uid)
            val current = reactionRef.get().await().getValue(String::class.java)

            if (current == emoji) {
                reactionRef.removeValue().await()
            } else {
                reactionRef.setValue(emoji).await()
            }

            AppResult.Success(Unit)
        } catch (e: Exception) {
            error("Tepki güncellenemedi.", e)
        }
    }

    private fun lastMessage(text: String, mediaUrl: String, sharedContent: SharedContent? = null): String =
        text.ifBlank {
            sharedContent?.title?.ifBlank {
                if (mediaUrl.isNotBlank()) "📷 Fotoğraf" else "Paylaşılan içerik"
            } ?: if (mediaUrl.isNotBlank()) "📷 Fotoğraf" else "Paylaşılan içerik"
        }

    private fun replyText(text: String?, mediaUrl: String?): String =
        text.orEmpty().ifBlank {
            if (!mediaUrl.isNullOrBlank()) "📷 Fotoğraf" else ""
        }
}
