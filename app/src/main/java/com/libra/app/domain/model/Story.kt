package com.libra.app.domain.model

data class Story(
    val id: String = "",
    val authorId: String = "",
    val authorName: String = "",
    val authorPhotoUrl: String = "",
    val text: String = "",
    val mediaUrl: String = "",
    val mediaType: String = "",
    val createdAt: Long = 0L,
    val expiresAt: Long = 0L,
    val likesCount: Int = 0
)
