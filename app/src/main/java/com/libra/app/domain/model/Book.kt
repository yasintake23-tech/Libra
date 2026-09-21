package com.libra.app.domain.model

enum class BookStatus { DRAFT, PUBLISHED, ARCHIVED }

enum class BookCategory(val displayName: String) {
    FICTION("Kurgu"), NON_FICTION("Kurgu Dışı"), SCIFI_FANTASY("Bilim Kurgu & Fantastik"), MYSTERY_THRILLER("Gizem & Gerilim"), ROMANCE("Romantik"), POETRY("Şiir"), PERSONAL_DEVELOPMENT("Kişisel Gelişim"), PHILOSOPHY("Felsefe & Düşünce"), CLASSICS("Klasikler")
}

data class Book(val id: String = "", val ownerId: String = "", val authorName: String = "", val title: String = "", val description: String = "", val coverImageUrl: String = "", val category: BookCategory = BookCategory.FICTION, val status: BookStatus = BookStatus.DRAFT, val rating: Float = 0f, val ratingCount: Int = 0, val readCount: Int = 0, val likesCount: Int = 0, val chapterCount: Int = 0, val createdAt: Long = 0L, val updatedAt: Long = 0L)
