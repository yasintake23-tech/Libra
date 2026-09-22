package com.libra.app.domain.model

data class PostComment(
    val id: String = "",
    val postId: String = "",
    val authorId: String = "",
    val authorName: String = "",
    val authorUsername: String = "",
    val authorPhotoUrl: String = "",
    val text: String = "",
    val createdAt: Long = 0L
)
