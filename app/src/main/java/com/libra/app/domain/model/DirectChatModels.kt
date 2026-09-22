package com.libra.app.domain.model

data class DirectConversation(
    val id: String = "",
    val participants: List<String> = emptyList(),
    val otherUserId: String = "",
    val otherUserName: String = "",
    val otherUserUsername: String = "",
    val otherUserPhotoUrl: String = "",
    val lastMessage: String = "",
    val updatedAt: Long = 0L,
    val unreadCount: Int = 0
)

data class DirectMessage(
    val id: String = "",
    val senderId: String = "",
    val recipientId: String = "",
    val text: String = "",
    val mediaUrl: String = "",
    val mediaType: String = "",
    val createdAt: Long = 0L
)
