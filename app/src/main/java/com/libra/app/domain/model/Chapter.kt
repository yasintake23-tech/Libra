package com.libra.app.domain.model

data class Chapter(
    val id: String = "",
    val bookId: String = "",
    val chapterNumber: Int = 1,
    val title: String = "",
    val content: String = "",
    val wordCount: Int = 0,
    val isPublished: Boolean = false,
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L
)
