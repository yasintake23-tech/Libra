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
import com.libra.app.domain.model.GroupPermissions
import com.libra.app.domain.model.ServerMessage
import com.libra.app.domain.model.ReactionUser
import com.libra.app.domain.model.SharedContent
import com.libra.app.domain.model.UserProfile
import com.libra.app.domain.model.canAddGroupMembers
import com.libra.app.domain.model.canEditGroupInfo
import com.libra.app.domain.model.canManageGroupSettings
import com.libra.app.domain.model.canSendInGroup
import com.libra.app.domain.model.canSendMediaInGroup
import com.libra.app.domain.model.resolveGroupOwnerId
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
private const val LIBRA_CHAT_ADMIN_UID = "ZBkz2js9plg07zrny2PzLW80X3i2"

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
                senderName = snapshot.child("senderName").getValue(String::class.java).orEmpty(),
                senderUsername = snapshot.child("senderUsername").getValue(String::class.java).orEmpty(),
                recipientId = snapshot.child("recipientId").getValue(String::class.java).orEmpty(),
                conversationId = snapshot.child("conversationId").getValue(String::class.java).orEmpty(),
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
                reactions = reactions(snapshot.child("reactions")),
                mentionedUserIds = snapshot.child("mentionedUserIds").children.mapNotNull { it.getValue(String::class.java) },
                mentionsEveryone = snapshot.child("mentionsEveryone").getValue(Boolean::class.java) ?: false,
                mentionsHere = snapshot.child("mentionsHere").getValue(Boolean::class.java) ?: false
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
        unreadCount: Any,
        peerLastReadAt: Long? = null,
        myLastReadAt: Long? = null
    ): Map<String, Any> = buildMap {
        put("otherUserId", otherUserId)
        put("otherUserName", otherUserName)
        put("otherUserUsername", otherUserUsername)
        put("otherUserPhotoUrl", otherUserPhotoUrl)
        put("lastMessage", lastMessage)
        put("updatedAt", updatedAt)
        put("unreadCount", unreadCount)
        if (peerLastReadAt != null) put("peerLastReadAt", peerLastReadAt)
        if (myLastReadAt != null) put("myLastReadAt", myLastReadAt)
    }

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
            "sharedContent" to (sharedContentData(sharedContent) ?: emptyMap<String, Any>())
        )

        return try {
            messageRef.setValue(data).await()
            AppResult.Success(Unit)
        } catch (e: Exception) {
            cleanupMedia(mediaUrl)
            error("Mesaj gönderilemedi.", e)
        }
    }

    override suspend fun editGlobalMessage(messageId: String, text: String): AppResult<Unit> =
        editMessage(ref("globalMessages/$messageId"), text, 1000)

    override suspend fun deleteGlobalMessage(messageId: String): AppResult<Unit> =
        deleteMessage(ref("globalMessages/$messageId"), requiredAdminPermission = "manageGlobalChat")

    override suspend fun toggleGlobalMessageReaction(
        messageId: String,
        emoji: String
    ): AppResult<Unit> =
        toggleReaction(ref("globalMessages/$messageId"), emoji)

    private fun participantMap(ids: List<String>): Map<String, String> =
        ids.filter { it.isNotBlank() }.distinct().associateWith { it }

    private fun permissionsMap(p: GroupPermissions): Map<String, Any> = mapOf(
        "onlyAdminsCanMessage" to p.onlyAdminsCanMessage,
        "membersCanAddOthers" to p.membersCanAddOthers,
        "membersCanEditInfo" to p.membersCanEditInfo,
        "membersCanSendMedia" to p.membersCanSendMedia
    )

    private fun parseConversationSnapshot(uid: String, child: DataSnapshot): DirectConversation? {
        val convId = child.key.orEmpty()
        if (convId.isBlank()) return null
        if (child.child("removedMember").getValue(Boolean::class.java) == true) return null

        val otherUid = child.child("otherUserId").getValue(String::class.java).orEmpty()
        val isGroup = child.child("isGroup").getValue(Boolean::class.java) ?: false
        val participantIds = child.child("participantIds").children
            .mapNotNull { it.getValue(String::class.java) ?: it.key }
            .filter { it.isNotBlank() }
            .distinct()

        if (!isGroup && otherUid.isBlank()) return null
        if (isGroup && participantIds.isNotEmpty() && uid !in participantIds) return null

        val adminIds = child.child("adminIds").children
            .mapNotNull { it.getValue(String::class.java) ?: it.key }
            .filter { it.isNotBlank() }
            .distinct()
        val permNode = child.child("groupPermissions")
        val groupPermissions = GroupPermissions(
            onlyAdminsCanMessage = permNode.child("onlyAdminsCanMessage").getValue(Boolean::class.java) ?: false,
            membersCanAddOthers = permNode.child("membersCanAddOthers").getValue(Boolean::class.java) ?: true,
            membersCanEditInfo = permNode.child("membersCanEditInfo").getValue(Boolean::class.java) ?: false,
            membersCanSendMedia = permNode.child("membersCanSendMedia").getValue(Boolean::class.java) ?: true
        )

        return DirectConversation(
            id = convId,
            participants = if (isGroup) participantIds else listOf(uid, otherUid).distinct(),
            otherUserId = otherUid,
            otherUserName = child.child("otherUserName").getValue(String::class.java).orEmpty(),
            otherUserUsername = child.child("otherUserUsername").getValue(String::class.java).orEmpty(),
            otherUserPhotoUrl = child.child("otherUserPhotoUrl").getValue(String::class.java).orEmpty(),
            lastMessage = child.child("lastMessage").getValue(String::class.java).orEmpty(),
            updatedAt = child.child("updatedAt").getValue(Long::class.java) ?: 0L,
            unreadCount = (child.child("unreadCount").getValue(Long::class.java) ?: 0L).toInt().coerceAtLeast(0),
            isGroup = isGroup,
            groupName = child.child("groupName").getValue(String::class.java).orEmpty(),
            groupDescription = child.child("groupDescription").getValue(String::class.java).orEmpty(),
            groupPhotoUrl = child.child("groupPhotoUrl").getValue(String::class.java).orEmpty(),
            participantIds = participantIds,
            ownerId = child.child("ownerId").getValue(String::class.java).orEmpty(),
            adminIds = adminIds,
            groupPermissions = groupPermissions,
            createdAt = child.child("createdAt").getValue(Long::class.java) ?: 0L,
            peerLastReadAt = child.child("peerLastReadAt").getValue(Long::class.java) ?: 0L,
            myLastReadAt = child.child("myLastReadAt").getValue(Long::class.java) ?: 0L
        )
    }

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
                        parseConversationSnapshot(uid, child)
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


    override suspend fun createGroupConversation(
        name: String,
        participantIds: List<String>
    ): AppResult<DirectConversation> {
        val owner = auth.currentUser
            ?: return AppResult.Error(AppError.Auth("Grup oluşturmak için giriş yapmalısın."))

        val cleanName = name.trim().take(60)
        val members = (participantIds + owner.uid).filter { it.isNotBlank() }.distinct()
        if (cleanName.isBlank()) return AppResult.Error(AppError.Validation("Grup adı boş olamaz."))
        if (members.size < 2) return AppResult.Error(AppError.Validation("En az bir kişi daha seçmelisin."))
        if (members.size > 50) return AppResult.Error(AppError.Validation("Bir grup en fazla 50 kişi olabilir."))

        val root = database()?.reference ?: return error("Realtime Database yapılandırması bulunamadı.")
        val conversationId = root.child("directMessages").push().key
            ?: return error("Grup oluşturulamadı.")
        val now = System.currentTimeMillis()
        val defaultPerms = GroupPermissions()
        val membersMap = participantMap(members)
        val adminsMap = participantMap(listOf(owner.uid))

        val updates = mutableMapOf<String, Any>()
        members.forEach { memberUid ->
            updates["directConversations/${memberUid}/${conversationId}"] = mapOf(
                "isGroup" to true,
                "groupName" to cleanName,
                "groupDescription" to "",
                "groupPhotoUrl" to "",
                "ownerId" to owner.uid,
                "adminIds" to adminsMap,
                "groupPermissions" to permissionsMap(defaultPerms),
                "createdAt" to now,
                "participantIds" to membersMap,
                "lastMessage" to "",
                "updatedAt" to now,
                "unreadCount" to 0
            )
        }

        return try {
            root.updateChildren(updates).await()
            AppResult.Success(
                DirectConversation(
                    id = conversationId,
                    participants = members,
                    isGroup = true,
                    groupName = cleanName,
                    groupDescription = "",
                    participantIds = members,
                    ownerId = owner.uid,
                    adminIds = listOf(owner.uid),
                    groupPermissions = defaultPerms,
                    createdAt = now
                )
            )
        } catch (e: Exception) {
            error("Grup oluşturulamadı.", e)
        }
    }

    override suspend fun sendGroupMessage(
        conversationId: String,
        text: String,
        replyTo: DirectMessage?,
        sharedContent: SharedContent?
    ): AppResult<Unit> =
        sendGroupInternal(conversationId, text, "", "", replyTo, sharedContent)

    override suspend fun sendGroupMediaMessage(
        conversationId: String,
        mediaUrl: String,
        mediaType: String,
        text: String,
        replyTo: DirectMessage?,
        sharedContent: SharedContent?
    ): AppResult<Unit> =
        sendGroupInternal(conversationId, text, mediaUrl, mediaType, replyTo, sharedContent)

    private suspend fun sendGroupInternal(
        conversationId: String,
        text: String,
        mediaUrl: String,
        mediaType: String,
        replyTo: DirectMessage?,
        sharedContent: SharedContent?
    ): AppResult<Unit> {
        val sender = auth.currentUser
            ?: return AppResult.Error(AppError.Auth("Mesaj göndermek için giriş yapmalısın."))
        if (conversationId.isBlank()) return AppResult.Error(AppError.Validation("Grup bulunamadı."))

        val memberSnapshot = ref("directConversations/${sender.uid}/${conversationId}")?.get()?.await()
        if (memberSnapshot == null || memberSnapshot.child("isGroup").getValue(Boolean::class.java) != true) {
            return AppResult.Error(AppError.Auth("Bu gruba erişim yetkin yok."))
        }
        val groupConv = parseConversationSnapshot(sender.uid, memberSnapshot)
            ?: return AppResult.Error(AppError.Auth("Bu gruba erişim yetkin yok."))
        val members = groupConv.participantIds
        if (sender.uid !in members) return AppResult.Error(AppError.Auth("Bu gruba erişim yetkin yok."))

        if (!canSendInGroup(groupConv, sender.uid)) {
            return AppResult.Error(AppError.Auth("Bu grupta yalnızca grup yöneticileri mesaj gönderebilir."))
        }
        if (mediaUrl.isNotBlank() && !canSendMediaInGroup(groupConv, sender.uid)) {
            return AppResult.Error(AppError.Auth("Bu grupta yalnızca yöneticiler medya paylaşabilir."))
        }

        val clean = text.trim()
        if (clean.isBlank() && mediaUrl.isBlank()) return AppResult.Error(AppError.Validation("Mesaj boş olamaz."))
        if (clean.length > 2000) return AppResult.Error(AppError.Validation("Mesaj en fazla 2000 karakter olabilir."))
        validateMediaOwner(sender.uid, mediaUrl, mediaType)?.let {
            return AppResult.Error(AppError.Validation(it))
        }

        val profile = profileOrFallback(sender.uid, sender.displayName.orEmpty(), sender.photoUrl?.toString().orEmpty())
        val groupName = groupConv.groupName.ifBlank { "Grup sohbeti" }
        val needsMapMigration = !memberSnapshot.child("participantIds").hasChild(sender.uid)
        val membersMap = if (needsMapMigration) participantMap(members) else null

        val root = database()?.reference
            ?: return error("Realtime Database yapılandırması bulunamadı.")
        val messageRef = ref("directMessages/${conversationId}")?.push()
            ?: return error("Realtime Database yapılandırması bulunamadı.")
        val now = System.currentTimeMillis()
        val message = mapOf(
            "senderId" to sender.uid,
            "senderName" to profile.displayName,
            "senderUsername" to profile.username,
            "senderPhotoUrl" to profile.profileImageUrl,
            "recipientId" to "",
            "conversationId" to conversationId,
            "isGroup" to true,
            "text" to clean,
            "mediaUrl" to mediaUrl,
            "mediaType" to mediaType,
            "createdAt" to now,
            "replyToMessageId" to (replyTo?.id ?: ""),
            "replyToText" to replyText(replyTo?.text, replyTo?.mediaUrl),
            "replyToSenderId" to (replyTo?.senderId ?: ""),
            "replyToSenderName" to (replyTo?.senderName
                ?.ifBlank {
                    if (replyTo.senderId == sender.uid) profile.displayName else replyTo.replyToSenderName
                }
                ?: ""),
            "sharedContent" to (sharedContentData(sharedContent) ?: emptyMap<String, Any>())
        )

        return try {
            messageRef.setValue(message).await()
            val last = lastMessage(clean, mediaUrl, sharedContent)
            val updates = mutableMapOf<String, Any>(
                "directConversations/${sender.uid}/${conversationId}/lastMessage" to last,
                "directConversations/${sender.uid}/${conversationId}/updatedAt" to now,
                "directConversations/${sender.uid}/${conversationId}/unreadCount" to 0
            )
            if (membersMap != null) {
                updates["directConversations/${sender.uid}/${conversationId}/participantIds"] = membersMap
            }
            val recipients = members.filter { it != sender.uid }.distinct()
            recipients.forEach { memberUid ->
                updates["directConversations/${memberUid}/${conversationId}/lastMessage"] = last
                updates["directConversations/${memberUid}/${conversationId}/updatedAt"] = now
                updates["directConversations/${memberUid}/${conversationId}/unreadCount"] = ServerValue.increment(1)
                if (membersMap != null) {
                    updates["directConversations/${memberUid}/${conversationId}/participantIds"] = membersMap
                }
            }
            root.updateChildren(updates).await()

            if (recipients.isNotEmpty()) {
                runCatching {
                    val senderDisplay = profile.displayName.ifBlank { profile.username.ifBlank { "Bir kullanıcı" } }
                    val previewText = when {
                        clean.isNotBlank() -> "$senderDisplay: ${clean.take(100)}"
                        sharedContent != null -> "$senderDisplay bir içerik paylaştı."
                        mediaUrl.isNotBlank() -> "$senderDisplay bir medya gönderdi."
                        else -> "$senderDisplay gruba mesaj gönderdi."
                    }
                    val groupNotifications = recipients.map { memberUid ->
                        AppNotification(
                            recipientId = memberUid,
                            actorId = sender.uid,
                            actorName = profile.displayName,
                            actorUsername = profile.username,
                            actorPhotoUrl = profile.profileImageUrl,
                            type = "GROUP_MESSAGE",
                            title = groupName,
                            body = previewText,
                            referenceId = conversationId,
                            createdAt = now
                        )
                    }
                    notificationRepository.createBatch(groupNotifications)
                }
            }
            AppResult.Success(Unit)
        } catch (e: Exception) {
            cleanupMedia(mediaUrl)
            error("Grup mesajı gönderilemedi.", e)
        }
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

        val membership = ref("directConversations/$uid/$conversationId")?.get()?.await()
        val isGroup = membership?.child("isGroup")?.getValue(Boolean::class.java) == true
        val participants = conversationId.split("_")
        if (!isGroup && (participants.size != 2 || uid !in participants)) {
            trySend(AppResult.Error(AppError.Auth("Bu sohbeti görüntüleme yetkin yok.")))
            close()
            return@callbackFlow
        }
        val removedMember = membership?.child("removedMember")?.getValue(Boolean::class.java) == true
        if (isGroup && (removedMember || membership?.child("participantIds")?.children?.none { (it.getValue(String::class.java) ?: it.key) == uid } != false)) {
            trySend(AppResult.Error(AppError.Auth("Bu grubu görüntüleme yetkin yok.")))
            close()
            return@callbackFlow
        }

        val safeLimit = limit.coerceIn(1L, 200L).toInt()
        val messagesRef = ref("directMessages/$conversationId")
        val clearedRef = ref("notifications/$uid/chatClearedAt/$conversationId")
        val deletedIdsRef = ref("notifications/$uid/deletedMessageIds/$conversationId")

        if (messagesRef == null || clearedRef == null || deletedIdsRef == null) {
            trySend(error("Realtime Database yapılandırması bulunamadı."))
            close()
            return@callbackFlow
        }

        val query = messagesRef.orderByChild("createdAt").limitToLast(safeLimit)
        var latestRawMessages: List<DirectMessage> = emptyList()
        var clearedAtMs = 0L
        var deletedMessageIds = emptySet<String>()

        fun emitFiltered() {
            val filtered = com.libra.app.feature.messages.filterVisibleDirectMessages(
                messages = latestRawMessages,
                clearedAtMs = clearedAtMs,
                deletedMessageIds = deletedMessageIds
            )
            trySend(AppResult.Success(filtered))
        }

        val msgListener = object : com.google.firebase.database.ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                latestRawMessages = snapshot.children
                    .mapNotNull(::directMessageFrom)
                    .filter { isGroup || it.senderId == uid || it.recipientId == uid }
                    .sortedBy { it.createdAt }
                emitFiltered()
            }

            override fun onCancelled(error: DatabaseError) {
                trySend(this@RealtimeChatRepositoryImpl.error(error.message, error.toException()))
            }
        }

        val clearedListener = object : com.google.firebase.database.ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                clearedAtMs = snapshot.getValue(Long::class.java) ?: 0L
                emitFiltered()
            }

            override fun onCancelled(error: DatabaseError) = Unit
        }

        val deletedIdsListener = object : com.google.firebase.database.ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                deletedMessageIds = snapshot.children.mapNotNull { it.key }.toSet()
                emitFiltered()
            }

            override fun onCancelled(error: DatabaseError) = Unit
        }

        // Seed watermark/deleted IDs first when available so old messages never flash briefly on reopen
        clearedAtMs = runCatching { clearedRef.get().await().getValue(Long::class.java) ?: 0L }.getOrDefault(0L)
        deletedMessageIds = runCatching {
            deletedIdsRef.get().await().children.mapNotNull { it.key }.toSet()
        }.getOrDefault(emptySet())

        clearedRef.addValueEventListener(clearedListener)
        deletedIdsRef.addValueEventListener(deletedIdsListener)
        query.addValueEventListener(msgListener)
        awaitClose {
            query.removeEventListener(msgListener)
            clearedRef.removeEventListener(clearedListener)
            deletedIdsRef.removeEventListener(deletedIdsListener)
        }
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

        val resolvedReplySenderName = replyTo?.let { target ->
            target.senderName.ifBlank {
                when {
                    target.senderId == sender.uid -> senderProfile.displayName
                    target.senderId.isNotBlank() -> profileOrFallback(target.senderId).displayName
                    else -> target.replyToSenderName
                }
            }
        }.orEmpty()

        val now = System.currentTimeMillis()
        val message = mapOf(
            "senderId" to sender.uid,
            "senderName" to senderProfile.displayName,
            "senderUsername" to senderProfile.username,
            "senderPhotoUrl" to senderProfile.profileImageUrl,
            "recipientId" to recipientId,
            "text" to clean,
            "mediaUrl" to mediaUrl,
            "mediaType" to mediaType,
            "createdAt" to now,
            "replyToMessageId" to (replyTo?.id ?: ""),
            "replyToText" to replyText(replyTo?.text, replyTo?.mediaUrl),
            "replyToSenderId" to (replyTo?.senderId ?: ""),
            "replyToSenderName" to resolvedReplySenderName,
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
                val senderTitle = senderProfile.displayName.ifBlank {
                    senderProfile.username.ifBlank { "Yeni mesaj" }
                }
                val messageBody = when {
                    clean.isNotBlank() -> clean.take(120)
                    sharedContent != null -> "Bir içerik paylaştı."
                    mediaUrl.isNotBlank() -> "Bir medya paylaştı."
                    else -> "Sana bir mesaj gönderdi."
                }
                notificationRepository.create(
                    AppNotification(
                        recipientId = recipientId,
                        actorId = sender.uid,
                        actorName = senderProfile.displayName,
                        actorUsername = senderProfile.username,
                        actorPhotoUrl = senderProfile.profileImageUrl,
                        type = "MESSAGE",
                        title = senderTitle,
                        body = messageBody,
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
                unreadCount = 0,
                peerLastReadAt = 0L,
                myLastReadAt = timestamp
            ),
            "directConversations/$recipientUid/$conversationId" to summaryData(
                otherUserId = senderUid,
                otherUserName = senderProfile.displayName,
                otherUserUsername = senderProfile.username,
                otherUserPhotoUrl = senderProfile.profileImageUrl,
                lastMessage = lastMessage,
                updatedAt = timestamp,
                unreadCount = ServerValue.increment(1),
                peerLastReadAt = timestamp
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
    ): AppResult<Unit> = deleteDirectMessages(conversationId, listOf(messageId))

    override suspend fun deleteDirectMessages(
        conversationId: String,
        messageIds: List<String>
    ): AppResult<Unit> {
        val uid = currentUid()
            ?: return AppResult.Error(AppError.Auth("Oturum bulunamadı."))
        val distinctIds = messageIds.map { it.trim() }.filter { it.isNotBlank() && !it.startsWith("local-") }.distinct()
        if (conversationId.isBlank() || distinctIds.isEmpty()) return AppResult.Success(Unit)

        val root = database()?.reference
            ?: return error("Realtime Database yapılandırması bulunamadı.")

        return try {
            val mediaToCleanup = mutableListOf<String>()
            val updates = linkedMapOf<String, Any?>()

            for (msgId in distinctIds) {
                val msgRef = ref("directMessages/$conversationId/$msgId") ?: continue
                val snapshot = msgRef.get().await()
                if (!snapshot.exists()) continue

                val senderId = snapshot.child("senderId").getValue(String::class.java).orEmpty()
                if (senderId == uid) {
                    updates["directMessages/$conversationId/$msgId"] = null
                    val mediaKey = snapshot.child("mediaUrl").getValue(String::class.java).orEmpty()
                    if (mediaKey.isNotBlank()) mediaToCleanup += mediaKey
                } else {
                    // Peer's messages cannot be removed from peer's node under security rules,
                    // so mark deleted for the current user under owner-writable path.
                    updates["notifications/$uid/deletedMessageIds/$conversationId/$msgId"] = true
                }
            }

            if (updates.isNotEmpty()) {
                root.updateChildren(updates).await()
            }
            mediaToCleanup.forEach { key -> cleanupMedia(key) }
            runCatching { refreshDirectConversationSummary(conversationId) }

            AppResult.Success(Unit)
        } catch (e: Exception) {
            error("Mesajlar silinemedi.", e)
        }
    }

    override suspend fun clearDirectConversation(
        conversationId: String,
        removeFromInbox: Boolean
    ): AppResult<Unit> {
        val uid = currentUid()
            ?: return AppResult.Error(AppError.Auth("Oturum bulunamadı."))
        if (conversationId.isBlank()) {
            return AppResult.Error(AppError.Validation("Sohbet bulunamadı."))
        }

        val root = database()?.reference
            ?: return error("Realtime Database yapılandırması bulunamadı.")

        return try {
            val now = System.currentTimeMillis()
            val allMessagesSnap = ref("directMessages/$conversationId")?.get()?.await()
            val ownMessageUpdates = linkedMapOf<String, Any?>()
            val mediaToCleanup = mutableListOf<String>()

            allMessagesSnap?.children?.forEach { child ->
                val msgId = child.key.orEmpty()
                val senderId = child.child("senderId").getValue(String::class.java).orEmpty()
                if (msgId.isNotBlank() && senderId == uid) {
                    ownMessageUpdates["directMessages/$conversationId/$msgId"] = null
                    val mediaKey = child.child("mediaUrl").getValue(String::class.java).orEmpty()
                    if (mediaKey.isNotBlank()) mediaToCleanup += mediaKey
                }
            }

            // Delete own messages BEFORE removing directConversations summary (required for group rules).
            if (ownMessageUpdates.isNotEmpty()) {
                ownMessageUpdates.entries.chunked(250).forEach { chunk ->
                    val batch = linkedMapOf<String, Any?>()
                    chunk.forEach { batch[it.key] = it.value }
                    root.updateChildren(batch).await()
                }
            }
            mediaToCleanup.forEach { key -> cleanupMedia(key) }

            val summaryRef = ref("directConversations/$uid/$conversationId")
            val summarySnap = summaryRef?.get()?.await()
            val metaUpdates = linkedMapOf<String, Any?>(
                "notifications/$uid/chatClearedAt/$conversationId" to now,
                "notifications/$uid/deletedMessageIds/$conversationId" to null
            )
            if (removeFromInbox) {
                metaUpdates["directConversations/$uid/$conversationId"] = null
            } else if (summarySnap?.exists() == true) {
                metaUpdates["directConversations/$uid/$conversationId/lastMessage"] = ""
                metaUpdates["directConversations/$uid/$conversationId/unreadCount"] = 0
                metaUpdates["directConversations/$uid/$conversationId/myLastReadAt"] = now
            }

            root.updateChildren(metaUpdates).await()
            AppResult.Success(Unit)
        } catch (e: Exception) {
            error("Sohbet temizlenemedi.", e)
        }
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
        if (conversationId.isBlank()) return AppResult.Success(Unit)

        val summaryRef = ref("directConversations/$uid/$conversationId")
            ?: return error("Realtime Database yapılandırması bulunamadı.")

        return try {
            val now = System.currentTimeMillis()
            val snapshot = summaryRef.get().await()
            if (snapshot.exists()) {
                summaryRef.updateChildren(
                    mapOf(
                        "unreadCount" to 0,
                        "myLastReadAt" to now
                    )
                ).await()
            }

            // Notify peer(s) that we have read messages up to 'now' (Görüldü).
            val isGroup = snapshot.child("isGroup").getValue(Boolean::class.java) == true
            if (isGroup) {
                val members = snapshot.child("participantIds").children
                    .mapNotNull { it.getValue(String::class.java) ?: it.key }
                    .filter { it.isNotBlank() && it != uid }
                    .distinct()
                members.forEach { memberUid ->
                    runCatching {
                        ref("directConversations/$memberUid/$conversationId/peerLastReadAt")
                            ?.setValue(now)
                            ?.await()
                    }
                }
            } else {
                val otherUid = snapshot.child("otherUserId").getValue(String::class.java)
                    ?.takeIf { it.isNotBlank() }
                    ?: conversationId.split("_").firstOrNull { it.isNotBlank() && it != uid }
                if (!otherUid.isNullOrBlank() && otherUid != uid) {
                    runCatching {
                        ref("directConversations/$otherUid/$conversationId/peerLastReadAt")
                            ?.setValue(now)
                            ?.await()
                    }
                }
            }

            runCatching { notificationRepository.markConversationNotificationsRead(uid, conversationId) }
            AppResult.Success(Unit)
        } catch (e: Exception) {
            // Read receipts are metadata. A permission/rule mismatch here must not
            // make opening or sending a conversation look broken.
            AppResult.Success(Unit)
        }
    }

    override suspend fun hideDirectConversation(conversationId: String): AppResult<Unit> =
        clearDirectConversation(conversationId, removeFromInbox = true)

    private suspend fun loadActiveGroupConversation(
        conversationId: String,
        uid: String
    ): AppResult<DirectConversation> {
        if (conversationId.isBlank()) return AppResult.Error(AppError.Validation("Grup bulunamadı."))
        val snap = ref("directConversations/$uid/$conversationId")?.get()?.await()
            ?: return error("Realtime Database yapılandırması bulunamadı.")
        val conv = parseConversationSnapshot(uid, snap)
        if (conv == null || !conv.isGroup || uid !in conv.participantIds) {
            return AppResult.Error(AppError.Auth("Bu gruba erişim yetkin yok."))
        }
        return AppResult.Success(conv)
    }

    override suspend fun updateGroupInfo(
        conversationId: String,
        name: String,
        description: String
    ): AppResult<Unit> {
        val uid = currentUid()
            ?: return AppResult.Error(AppError.Auth("Oturum bulunamadı."))
        val conv = when (val res = loadActiveGroupConversation(conversationId, uid)) {
            is AppResult.Success -> res.data
            is AppResult.Error -> return res
        }
        if (!canEditGroupInfo(conv, uid)) {
            return AppResult.Error(AppError.Auth("Grup bilgilerini düzenleme yetkin yok."))
        }
        val cleanName = name.trim().take(60)
        val cleanDesc = description.trim().take(300)
        if (cleanName.isBlank()) {
            return AppResult.Error(AppError.Validation("Grup adı boş olamaz."))
        }
        val root = database()?.reference ?: return error("Realtime Database yapılandırması bulunamadı.")
        val presidentUid = resolveGroupOwnerId(conv)
        val membersMap = participantMap(conv.participantIds)
        val updates = mutableMapOf<String, Any>()
        conv.participantIds.forEach { memberUid ->
            updates["directConversations/$memberUid/$conversationId/groupName"] = cleanName
            updates["directConversations/$memberUid/$conversationId/groupDescription"] = cleanDesc
            updates["directConversations/$memberUid/$conversationId/participantIds"] = membersMap
            if (conv.ownerId.isBlank() && presidentUid.isNotBlank()) {
                updates["directConversations/$memberUid/$conversationId/ownerId"] = presidentUid
            }
        }
        return try {
            root.updateChildren(updates).await()
            AppResult.Success(Unit)
        } catch (e: Exception) {
            error("Grup bilgileri güncellenemedi.", e)
        }
    }

    override suspend fun updateGroupPermissions(
        conversationId: String,
        permissions: GroupPermissions
    ): AppResult<Unit> {
        val uid = currentUid()
            ?: return AppResult.Error(AppError.Auth("Oturum bulunamadı."))
        val conv = when (val res = loadActiveGroupConversation(conversationId, uid)) {
            is AppResult.Success -> res.data
            is AppResult.Error -> return res
        }
        if (!canManageGroupSettings(conv, uid)) {
            return AppResult.Error(AppError.Auth("Grup izinlerini yalnızca grup başkanı veya yöneticiler değiştirebilir."))
        }
        val root = database()?.reference ?: return error("Realtime Database yapılandırması bulunamadı.")
        val presidentUid = resolveGroupOwnerId(conv)
        val membersMap = participantMap(conv.participantIds)
        val permPayload = permissionsMap(permissions)
        val updates = mutableMapOf<String, Any>()
        conv.participantIds.forEach { memberUid ->
            updates["directConversations/$memberUid/$conversationId/groupPermissions"] = permPayload
            updates["directConversations/$memberUid/$conversationId/participantIds"] = membersMap
            if (conv.ownerId.isBlank() && presidentUid.isNotBlank()) {
                updates["directConversations/$memberUid/$conversationId/ownerId"] = presidentUid
            }
        }
        return try {
            root.updateChildren(updates).await()
            AppResult.Success(Unit)
        } catch (e: Exception) {
            error("Grup izinleri güncellenemedi.", e)
        }
    }

    override suspend fun addGroupMembers(
        conversationId: String,
        userIds: List<String>
    ): AppResult<Unit> {
        val uid = currentUid()
            ?: return AppResult.Error(AppError.Auth("Oturum bulunamadı."))
        val conv = when (val res = loadActiveGroupConversation(conversationId, uid)) {
            is AppResult.Success -> res.data
            is AppResult.Error -> return res
        }
        if (!canAddGroupMembers(conv, uid)) {
            return AppResult.Error(AppError.Auth("Gruba kişi ekleme yetkin yok."))
        }
        val toAdd = userIds.map { it.trim() }
            .filter { it.isNotBlank() && it !in conv.participantIds }
            .distinct()
        if (toAdd.isEmpty()) return AppResult.Success(Unit)

        val updatedMembers = (conv.participantIds + toAdd).distinct()
        if (updatedMembers.size > 50) {
            return AppResult.Error(AppError.Validation("Bir grup en fazla 50 kişi olabilir."))
        }
        val root = database()?.reference ?: return error("Realtime Database yapılandırması bulunamadı.")
        val now = System.currentTimeMillis()
        val presidentUid = resolveGroupOwnerId(conv)
        val resolvedAdmins = (conv.adminIds + listOfNotNull(presidentUid.takeIf { it.isNotBlank() })).distinct()
        val membersMap = participantMap(updatedMembers)
        val adminsMap = participantMap(resolvedAdmins)
        val permPayload = permissionsMap(conv.groupPermissions)

        val updates = mutableMapOf<String, Any>()
        conv.participantIds.forEach { memberUid ->
            updates["directConversations/$memberUid/$conversationId/participantIds"] = membersMap
            if (conv.ownerId.isBlank() && presidentUid.isNotBlank()) {
                updates["directConversations/$memberUid/$conversationId/ownerId"] = presidentUid
                updates["directConversations/$memberUid/$conversationId/adminIds"] = adminsMap
            }
        }
        toAdd.forEach { newUid ->
            updates["directConversations/$newUid/$conversationId"] = mapOf(
                "isGroup" to true,
                "groupName" to conv.groupName.ifBlank { "Grup sohbeti" },
                "groupDescription" to conv.groupDescription,
                "groupPhotoUrl" to conv.groupPhotoUrl,
                "ownerId" to presidentUid,
                "adminIds" to adminsMap,
                "groupPermissions" to permPayload,
                "createdAt" to conv.createdAt.takeIf { it > 0L } ?: now,
                "participantIds" to membersMap,
                "lastMessage" to conv.lastMessage,
                "updatedAt" to now,
                "unreadCount" to 0
            )
        }
        return try {
            root.updateChildren(updates).await()
            AppResult.Success(Unit)
        } catch (e: Exception) {
            error("Kişiler gruba eklenemedi.", e)
        }
    }

    override suspend fun removeGroupMember(
        conversationId: String,
        targetUid: String
    ): AppResult<Unit> {
        val uid = currentUid()
            ?: return AppResult.Error(AppError.Auth("Oturum bulunamadı."))
        val cleanTarget = targetUid.trim()
        if (cleanTarget.isBlank()) {
            return AppResult.Error(AppError.Validation("Geçersiz üye."))
        }
        val conv = when (val res = loadActiveGroupConversation(conversationId, uid)) {
            is AppResult.Success -> res.data
            is AppResult.Error -> return res
        }
        if (cleanTarget !in conv.participantIds) {
            return AppResult.Success(Unit)
        }

        val presidentUid = resolveGroupOwnerId(conv)
        val isSelfLeave = cleanTarget == uid
        if (!isSelfLeave) {
            if (!canManageGroupSettings(conv, uid)) {
                return AppResult.Error(AppError.Auth("Üye çıkarma yetkin yok."))
            }
            if (cleanTarget == presidentUid) {
                return AppResult.Error(AppError.Auth("Grup Başkanı gruptan çıkarılamaz."))
            }
            if (cleanTarget in conv.adminIds && uid != presidentUid) {
                return AppResult.Error(AppError.Auth("Bir yöneticiyi yalnızca Grup Başkanı çıkarabilir."))
            }
        }

        val remainingMembers = conv.participantIds.filter { it != cleanTarget }
        val updatedAdmins = conv.adminIds.filter { it in remainingMembers }.toMutableList()
        val nextPresidentUid = when {
            remainingMembers.isEmpty() -> ""
            presidentUid == cleanTarget -> {
                updatedAdmins.firstOrNull() ?: remainingMembers.first()
            }
            else -> presidentUid
        }
        if (nextPresidentUid.isNotBlank() && nextPresidentUid !in updatedAdmins) {
            updatedAdmins.add(0, nextPresidentUid)
        }

        val root = database()?.reference ?: return error("Realtime Database yapılandırması bulunamadı.")
        val now = System.currentTimeMillis()
        val updates = linkedMapOf<String, Any?>()
        val remainingMap = participantMap(remainingMembers)
        val adminsMap = participantMap(updatedAdmins)

        remainingMembers.forEach { memberUid ->
            updates["directConversations/$memberUid/$conversationId/participantIds"] = remainingMap
            updates["directConversations/$memberUid/$conversationId/ownerId"] = nextPresidentUid
            updates["directConversations/$memberUid/$conversationId/adminIds"] = adminsMap
        }

        if (isSelfLeave) {
            updates["directConversations/$uid/$conversationId"] = null
        } else {
            updates["directConversations/$cleanTarget/$conversationId"] = mapOf(
                "isGroup" to true,
                "groupName" to conv.groupName.ifBlank { "Grup sohbeti" },
                "groupDescription" to conv.groupDescription,
                "groupPhotoUrl" to conv.groupPhotoUrl,
                "ownerId" to nextPresidentUid,
                "adminIds" to emptyMap<String, String>(),
                "groupPermissions" to permissionsMap(conv.groupPermissions),
                "participantIds" to mapOf(uid to uid),
                "removedMember" to true,
                "lastMessage" to "",
                "updatedAt" to now,
                "unreadCount" to 0
            )
        }

        return try {
            root.updateChildren(updates).await()
            AppResult.Success(Unit)
        } catch (e: Exception) {
            error(if (isSelfLeave) "Gruptan ayrılınamadı." else "Üye çıkarılamadı.", e)
        }
    }

    override suspend fun setGroupMemberAdmin(
        conversationId: String,
        targetUid: String,
        isAdmin: Boolean
    ): AppResult<Unit> {
        val uid = currentUid()
            ?: return AppResult.Error(AppError.Auth("Oturum bulunamadı."))
        val cleanTarget = targetUid.trim()
        val conv = when (val res = loadActiveGroupConversation(conversationId, uid)) {
            is AppResult.Success -> res.data
            is AppResult.Error -> return res
        }
        val presidentUid = resolveGroupOwnerId(conv)
        if (uid != presidentUid) {
            return AppResult.Error(AppError.Auth("Yönetici atama yetkisi yalnızca Grup Başkanındadır."))
        }
        if (cleanTarget.isBlank() || cleanTarget !in conv.participantIds) {
            return AppResult.Error(AppError.Validation("Kullanıcı bu grubun üyesi değil."))
        }
        if (cleanTarget == presidentUid) {
            return AppResult.Error(AppError.Validation("Grup Başkanının yönetici yetkisi değiştirilemez."))
        }

        val updatedAdmins = conv.adminIds.toMutableSet()
        updatedAdmins.add(presidentUid)
        if (isAdmin) {
            updatedAdmins.add(cleanTarget)
        } else {
            updatedAdmins.remove(cleanTarget)
        }
        val root = database()?.reference ?: return error("Realtime Database yapılandırması bulunamadı.")
        val membersMap = participantMap(conv.participantIds)
        val adminsMap = participantMap(updatedAdmins.toList())
        val updates = mutableMapOf<String, Any>()
        conv.participantIds.forEach { memberUid ->
            updates["directConversations/$memberUid/$conversationId/ownerId"] = presidentUid
            updates["directConversations/$memberUid/$conversationId/adminIds"] = adminsMap
            updates["directConversations/$memberUid/$conversationId/participantIds"] = membersMap
        }
        return try {
            root.updateChildren(updates).await()
            AppResult.Success(Unit)
        } catch (e: Exception) {
            error("Yönetici yetkisi güncellenemedi.", e)
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
        deleteMessage(
            ref(serverMessagesPath(serverId, channelId) + "/$messageId"),
            requiredAdminPermission = "manageServers"
        )

    override suspend fun toggleServerMessageReaction(
        serverId: String,
        messageId: String,
        emoji: String,
        channelId: String
    ): AppResult<Unit> =
        toggleReaction(ref(serverMessagesPath(serverId, channelId) + "/$messageId"), emoji)

    override suspend fun getServerMessageReactionUsers(
        serverId: String,
        messageId: String,
        emoji: String,
        channelId: String
    ): AppResult<List<ReactionUser>> {
        if (serverId.isBlank() || messageId.isBlank() || emoji.isBlank()) {
            return AppResult.Error(AppError.Validation("Geçersiz tepki bilgisi."))
        }

        return try {
            val snapshot = ref(serverMessagesPath(serverId, channelId) + "/$messageId/reactions")
                ?.get()
                ?.await()
                ?: return error("Realtime Database yapılandırması bulunamadı.")

            val users = snapshot.children.mapNotNull { child ->
                val uid = child.key.orEmpty()
                val value = child.getValue(String::class.java).orEmpty()
                if (uid.isBlank() || value != emoji) {
                    null
                } else {
                    uid
                }
            }.map { uid ->
                val profile = profileOrFallback(uid)
                ReactionUser(
                    uid = uid,
                    displayName = profile.displayName,
                    username = profile.username,
                    photoUrl = profile.profileImageUrl,
                    emoji = emoji
                )
            }

            AppResult.Success(users)
        } catch (e: Exception) {
            error("Tepki verenler yüklenemedi.", e)
        }
    }

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

    private suspend fun isChatAdmin(uid: String, permissionKey: String): Boolean {
        if (uid.isBlank() || permissionKey.isBlank()) return false
        if (uid == LIBRA_CHAT_ADMIN_UID) return true
        return runCatching {
            FirebaseFirestore.getInstance()
                .collection("adminRoles")
                .document(uid)
                .get()
                .await()
                .get("permissions.$permissionKey") == true
        }.getOrDefault(false)
    }

    private suspend fun deleteMessage(
        messageRef: DatabaseReference?,
        requiredAdminPermission: String = ""
    ): AppResult<Unit> {
        val uid = currentUid()
            ?: return AppResult.Error(AppError.Auth("Oturum bulunamadı."))

        val target = messageRef
            ?: return error("Realtime Database yapılandırması bulunamadı.")

        return try {
            val snapshot = target.get().await()
            if (!snapshot.exists()) {
                return AppResult.Success(Unit)
            }

            val senderId = snapshot.child("senderId").getValue(String::class.java)
            if (senderId != uid && !isChatAdmin(uid, requiredAdminPermission)) {
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
        val summarySnap = ref("directConversations/$uid/$conversationId")?.get()?.await()
        val isGroup = summarySnap?.child("isGroup")?.getValue(Boolean::class.java) == true
        val participants = conversationId.split("_")
        if (!isGroup && (participants.size != 2 || uid !in participants)) return

        val clearedAtMs = runCatching {
            ref("notifications/$uid/chatClearedAt/$conversationId")?.get()?.await()?.getValue(Long::class.java) ?: 0L
        }.getOrDefault(0L)
        val deletedIds = runCatching {
            ref("notifications/$uid/deletedMessageIds/$conversationId")?.get()?.await()?.children?.mapNotNull { it.key }?.toSet()
        }.getOrNull().orEmpty()

        val recentMessages = ref("directMessages/$conversationId")
            ?.orderByChild("createdAt")
            ?.limitToLast(60)
            ?.get()
            ?.await()
            ?.children
            ?.mapNotNull(::directMessageFrom)
            .orEmpty()

        val visibleMessages = com.libra.app.feature.messages.filterVisibleDirectMessages(
            messages = recentMessages,
            clearedAtMs = clearedAtMs,
            deletedMessageIds = deletedIds
        )
        val latest = visibleMessages.maxByOrNull { it.createdAt }

        val lastMessage = latest?.let {
            lastMessage(it.text, it.mediaUrl, it.sharedContent)
        }.orEmpty()
        val updatedAt = latest?.createdAt ?: (summarySnap?.child("updatedAt")?.getValue(Long::class.java) ?: 0L)

        val root = database()?.reference ?: return
        if (isGroup) {
            if (summarySnap?.exists() == true) {
                runCatching {
                    root.updateChildren(
                        mapOf(
                            "directConversations/$uid/$conversationId/lastMessage" to lastMessage,
                            "directConversations/$uid/$conversationId/updatedAt" to updatedAt
                        )
                    ).await()
                }
            }
        } else {
            val otherUid = participants.first { it != uid }
            val otherProfile = profileOrFallback(otherUid)
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
