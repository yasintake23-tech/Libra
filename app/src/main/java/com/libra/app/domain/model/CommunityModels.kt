package com.libra.app.domain.model

data class CommunityServer(
    val id: String = "",
    val name: String = "",
    val description: String = "",
    val ownerId: String = "",
    val createdAt: Long = 0L
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

data class ServerMessage(
    val id: String = "",
    val senderId: String = "",
    val senderName: String = "",
    val senderUsername: String = "",
    val senderPhotoUrl: String = "",
    val text: String = "",
    val createdAt: Long = 0L
)
