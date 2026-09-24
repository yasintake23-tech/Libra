package com.libra.app.domain.model

data class GlobalChatMessage(
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
    val reactions: Map<String, String> = emptyMap()
)
