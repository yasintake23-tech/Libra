package com.libra.app.domain.model

data class Post(
    val id: String = "",
    val authorId: String = "",
    val authorName: String = "",
    val authorUsername: String = "",
    val authorPhotoUrl: String = "",
    val title: String = "",
    val text: String = "",
    val mediaUrl: String = "",
    val mediaType: String = "",
    val tags: List<String> = emptyList(),
    val likesCount: Int = 0,
    val likedByCurrentUser: Boolean = false,
    val savedByCurrentUser: Boolean = false,
    val createdAt: Long = 0L
)
