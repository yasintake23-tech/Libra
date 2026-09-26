package com.libra.app.domain.model

data class CommunityServer(
    val id: String = "",
    val name: String = "",
    val description: String = "",
    val ownerId: String = "",
    val createdAt: Long = 0L,
    // R2 object keys. Older server documents may not contain these fields.
    val avatarUrl: String = "",
    val bannerUrl: String = ""
)

enum class ServerRole { OWNER, ADMIN, MEMBER }

data class ServerMember(
    val uid: String = "",
    val displayName: String = "",
    val username: String = "",
    val photoUrl: String = "",
    val role: String = "MEMBER",
    val joinedAt: Long = 0L
)

data class ServerCategory(
    val id: String = "",
    val name: String = "",
    val position: Int = 0
)

data class ServerChannel(
    val id: String = "",
    val categoryId: String = "",
    val categoryName: String = "",
    val name: String = "",
    val type: String = "TEXT",
    val position: Int = 0,
    val allowEveryoneView: Boolean = true,
    val allowEveryoneSend: Boolean = true
)

data class ServerChannelPermissionOverride(
    val id: String = "",
    val subjectType: String = "ROLE",
    val subjectId: String = "MEMBER",
    val subjectName: String = "",
    val canView: Boolean? = null,
    val canSend: Boolean? = null
)

data class ServerMessage(
    val id: String = "",
    val senderId: String = "",
    val senderName: String = "",
    val senderUsername: String = "",
    val senderPhotoUrl: String = "",
    val text: String = "",
    val mediaUrl: String = "",
    val mediaType: String = "",
    val createdAt: Long = 0L,
    val editedAt: Long? = null,
    val replyToMessageId: String = "",
    val replyToText: String = "",
    val replyToSenderId: String = "",
    val replyToSenderName: String = "",
    val reactions: Map<String, String> = emptyMap(),
    val sharedContent: SharedContent? = null,
    val mentionedUserIds: List<String> = emptyList(),
    val mentionsEveryone: Boolean = false,
    val mentionsHere: Boolean = false
)


data class ServerRoleDefinition(
    val id: String = "",
    val name: String = "",
    val permissions: List<String> = emptyList(),
    val position: Int = 0
)

data class ServerBan(
    val uid: String = "",
    val displayName: String = "",
    val username: String = "",
    val bannedAt: Long = 0L,
    val reason: String = ""
)
