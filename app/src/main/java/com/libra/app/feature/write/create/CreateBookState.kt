package com.libra.app.feature.write.create

import android.net.Uri
import com.libra.app.domain.model.BookCategory

enum class CreateBookStage(val stepNumber: Int, val title: String, val subtitle: String) {
    BASIC_INFO(1, "Temel Bilgiler", "Kapak, başlık, kategori ve özet"),
    CHAPTERS(2, "Bölümler", "Bölüm oluşturma, düzenleme ve sıralama"),
    DETAILS(3, "Kitap Detayları", "Etiketler, dil, kitle ve seri"),
    PUBLISH(4, "Yayınlama", "Önizleme, görünürlük ve onay")
}

data class CreateBookUiState(
    val currentStep: Int = 1,
    // Stage 1: Basic Info
    val title: String = "",
    val description: String = "",
    val discoverySummary: String = "",
    val category: BookCategory = BookCategory.FICTION,
    val coverUri: Uri? = null,
    val coverBytes: ByteArray? = null,
    val coverFileName: String = "cover.jpg",
    val coverContentType: String = "image/jpeg",
    val uploadedCoverUrl: String = "",
    val isUploadingCover: Boolean = false,
    val coverUploadProgress: Int = 0,
    val showAiCoverPromptDialog: Boolean = false,
    // Stage 2: Chapters
    val chapters: List<DraftChapter> = listOf(
        DraftChapter(id = "ch_1", chapterNumber = 1, title = "1. Bölüm", content = "")
    ),
    val editingChapterId: String? = null,
    // Stage 3: Details
    val tags: List<String> = emptyList(),
    val language: String = "Türkçe",
    val genre: String = "Roman",
    val subGenre: String = "",
    val targetAudience: String = "Herkes İçin", // "Herkes İçin", "Genç Yetişkin (13+)", "Yetişkin (18+)"
    val contentWarnings: Set<String> = emptySet(),
    val isSeries: Boolean = false,
    val seriesName: String = "",
    val seriesOrder: Int = 1,
    // Stage 4: Publish
    val visibility: String = "PUBLIC", // "PUBLIC", "UNLISTED", "PRIVATE"
    // Loading & Feedback
    val isSavingDraft: Boolean = false,
    val isPublishing: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null,
    val showDiscardConfirmDialog: Boolean = false,
    val hasUnsavedLocalDraft: Boolean = false
) {
    val totalWordCount: Int
        get() = chapters.sumOf { it.wordCount }

    // Validation checks
    val isTitleValid: Boolean
        get() = title.trim().length in 2..120

    val isCategoryValid: Boolean
        get() = true

    val hasCover: Boolean
        get() = coverUri != null || uploadedCoverUrl.isNotBlank()

    val hasChapters: Boolean
        get() = chapters.isNotEmpty()

    val isFirstChapterFilled: Boolean
        get() = chapters.firstOrNull()?.content?.trim()?.isNotBlank() == true

    val canPublish: Boolean
        get() = isTitleValid && hasChapters && isFirstChapterFilled && !isPublishing && !isSavingDraft

    val canProceedFromStep1: Boolean
        get() = isTitleValid

    val canProceedFromStep2: Boolean
        get() = chapters.isNotEmpty() && chapters.all { it.title.trim().isNotBlank() }

    val hasAnyContent: Boolean
        get() = title.isNotBlank() ||
            description.isNotBlank() ||
            discoverySummary.isNotBlank() ||
            coverUri != null ||
            uploadedCoverUrl.isNotBlank() ||
            chapters.any { it.content.isNotBlank() || it.title != "1. Bölüm" } ||
            tags.isNotEmpty() ||
            subGenre.isNotBlank() ||
            seriesName.isNotBlank()
}
