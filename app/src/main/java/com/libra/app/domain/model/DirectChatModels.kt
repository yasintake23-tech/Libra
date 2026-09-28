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
    val unreadCount: Int = 0,
    val isGroup: Boolean = false,
    val groupName: String = "",
    val groupPhotoUrl: String = "",
    val participantIds: List<String> = emptyList()
)

data class DirectMessage(
    val id: String = "",
    val senderId: String = "",
    val senderPhotoUrl: String = "",
    val senderName: String = "",
    val senderUsername: String = "",
    val recipientId: String = "",
    val conversationId: String = "",
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
    val sharedContent: SharedContent? = null
)
