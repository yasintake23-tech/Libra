package com.libra.app.domain.model

data class BookEngagement(
    val bookId: String = "",
    val liked: Boolean = false,
    val saved: Boolean = false
)

data class BookComment(
    val id: String = "",
    val bookId: String = "",
    val userId: String = "",
    val authorName: String = "",
    val authorUsername: String = "",
    val authorPhotoUrl: String = "",
    val text: String = "",
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L
)

data class ReadingProgress(
    val userId: String = "",
    val bookId: String = "",
    val chapterId: String = "",
    val chapterNumber: Int = 0,
    val progressPercent: Int = 0,
    val position: Int = 0,
    val updatedAt: Long = 0L
)
