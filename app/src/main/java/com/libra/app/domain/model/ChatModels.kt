package com.libra.app.domain.model

data class GlobalChatMessage(
    val id: String = "",
    val senderId: String = "",
    val senderName: String = "",
    val senderUsername: String = "",
    val senderPhotoUrl: String = "",
    val text: String = "",
    val createdAt: Long = 0L
)
