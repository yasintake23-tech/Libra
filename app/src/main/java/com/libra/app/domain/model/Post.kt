package com.libra.app.domain.model

data class Post(
    val id: String = "",
    val authorId: String = "",
    val authorName: String = "",
    val authorUsername: String = "",
    val authorPhotoUrl: String = "",
    val text: String = "",
    val likesCount: Int = 0,
    val likedByCurrentUser: Boolean = false,
    val createdAt: Long = 0L
)
