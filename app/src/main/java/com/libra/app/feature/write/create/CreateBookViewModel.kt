package com.libra.app.feature.write.create

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.libra.app.core.di.ServiceLocator
import com.libra.app.core.result.AppError
import com.libra.app.core.result.AppResult
import com.libra.app.domain.model.Book
import com.libra.app.domain.model.BookCategory
import com.libra.app.domain.model.BookStatus
import com.libra.app.domain.model.Chapter
import com.libra.app.domain.model.StorageUploadRequest
import com.libra.app.domain.repository.AuthRepository
import com.libra.app.domain.repository.BookRepository
import com.libra.app.domain.repository.StorageRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.UUID

class CreateBookViewModel(
    private val authRepository: AuthRepository = ServiceLocator.authRepository,
    private val bookRepository: BookRepository = ServiceLocator.bookRepository,
    private val storageRepository: StorageRepository = ServiceLocator.storageRepository,
    var draftStorage: BookDraftStorage? = null
) : ViewModel() {

    private val _uiState = MutableStateFlow(CreateBookUiState())
    val uiState: StateFlow<CreateBookUiState> = _uiState.asStateFlow()

    fun attachDraftStorage(storage: BookDraftStorage) {
        this.draftStorage = storage
        checkExistingDraft()
    }

    private fun checkExistingDraft() {
        val draft = draftStorage?.loadDraft()
        if (draft != null && draft.isNotEmpty()) {
            _uiState.value = _uiState.value.copy(hasUnsavedLocalDraft = true)
        }
    }

    fun restoreDraft() {
        val draft = draftStorage?.loadDraft() ?: return
        val category = runCatching { BookCategory.valueOf(draft.categoryName) }.getOrDefault(BookCategory.FICTION)
        val restoredChapters = if (draft.chapters.isEmpty()) {
            listOf(DraftChapter(id = "ch_1", chapterNumber = 1, title = "1. Bölüm", content = ""))
        } else {
            draft.chapters
        }

        _uiState.value = _uiState.value.copy(
            title = draft.title,
            description = draft.description,
            discoverySummary = draft.discoverySummary,
            category = category,
            coverUri = draft.coverUriString?.let { runCatching { Uri.parse(it) }.getOrNull() },
            uploadedCoverUrl = draft.uploadedCoverUrl,
            chapters = restoredChapters,
            tags = draft.tags,
            language = draft.language.ifBlank { "Türkçe" },
            genre = draft.genre.ifBlank { "Roman" },
            subGenre = draft.subGenre,
            targetAudience = draft.targetAudience.ifBlank { "Herkes İçin" },
            contentWarnings = draft.contentWarnings.toSet(),
            isSeries = draft.isSeries,
            seriesName = draft.seriesName,
            seriesOrder = draft.seriesOrder,
            visibility = draft.visibility.ifBlank { "PUBLIC" },
            currentStep = draft.currentStep.coerceIn(1, 4),
            hasUnsavedLocalDraft = false
        )
    }

    fun discardLocalDraft() {
        draftStorage?.clearDraft()
        _uiState.value = CreateBookUiState()
    }

    private fun persistDraftDebounced() {
        val state = _uiState.value
        val draft = CreateBookDraft(
            title = state.title,
            description = state.description,
            discoverySummary = state.discoverySummary,
            categoryName = state.category.name,
            coverUriString = state.coverUri?.toString(),
            uploadedCoverUrl = state.uploadedCoverUrl,
            chapters = state.chapters,
            tags = state.tags,
            language = state.language,
            genre = state.genre,
            subGenre = state.subGenre,
            targetAudience = state.targetAudience,
            contentWarnings = state.contentWarnings.toList(),
            isSeries = state.isSeries,
            seriesName = state.seriesName,
            seriesOrder = state.seriesOrder,
            visibility = state.visibility,
            currentStep = state.currentStep
        )
        if (draft.isNotEmpty()) {
            draftStorage?.saveDraft(draft)
        }
    }

    fun setStep(step: Int) {
        if (step in 1..4) {
            _uiState.value = _uiState.value.copy(currentStep = step)
            persistDraftDebounced()
        }
    }

    fun nextStep() {
        val current = _uiState.value.currentStep
        if (current < 4) {
            _uiState.value = _uiState.value.copy(currentStep = current + 1)
            persistDraftDebounced()
        }
    }

    fun previousStep() {
        val current = _uiState.value.currentStep
        if (current > 1) {
            _uiState.value = _uiState.value.copy(currentStep = current - 1)
            persistDraftDebounced()
        }
    }

    fun updateTitle(newTitle: String) {
        if (newTitle.length <= 120) {
            _uiState.value = _uiState.value.copy(title = newTitle, errorMessage = null)
            persistDraftDebounced()
        }
    }

    fun updateDescription(newDescription: String) {
        if (newDescription.length <= 2000) {
            _uiState.value = _uiState.value.copy(description = newDescription)
            persistDraftDebounced()
        }
    }

    fun updateDiscoverySummary(newSummary: String) {
        if (newSummary.length <= 500) {
            _uiState.value = _uiState.value.copy(discoverySummary = newSummary)
            persistDraftDebounced()
        }
    }

    fun updateCategory(newCategory: BookCategory) {
        _uiState.value = _uiState.value.copy(category = newCategory)
        persistDraftDebounced()
    }

    fun setCoverImage(uri: Uri?, bytes: ByteArray?, fileName: String, contentType: String) {
        if (bytes != null && bytes.size > 8 * 1024 * 1024) {
            _uiState.value = _uiState.value.copy(errorMessage = "Kapak görseli 8 MB'dan küçük olmalıdır.")
            return
        }
        _uiState.value = _uiState.value.copy(
            coverUri = uri,
            coverBytes = bytes,
            coverFileName = fileName,
            coverContentType = contentType,
            uploadedCoverUrl = "",
            errorMessage = null
        )
        persistDraftDebounced()
    }

    fun removeCoverImage() {
        _uiState.value = _uiState.value.copy(
            coverUri = null,
            coverBytes = null,
            uploadedCoverUrl = ""
        )
        persistDraftDebounced()
    }

    fun setAiCoverPromptDialog(visible: Boolean) {
        _uiState.value = _uiState.value.copy(showAiCoverPromptDialog = visible)
    }

    fun addChapter(title: String = "", content: String = "") {
        val currentList = _uiState.value.chapters
        val nextNumber = currentList.size + 1
        val newChapter = DraftChapter(
            id = "ch_" + UUID.randomUUID().toString().take(8),
            chapterNumber = nextNumber,
            title = title.ifBlank { "$nextNumber. Bölüm" },
            content = content
        )
        _uiState.value = _uiState.value.copy(
            chapters = currentList + newChapter,
            editingChapterId = newChapter.id
        )
        persistDraftDebounced()
    }

    fun updateChapter(id: String, title: String, content: String) {
        _uiState.value = _uiState.value.copy(
            chapters = _uiState.value.chapters.map { chapter ->
                if (chapter.id == id) {
                    chapter.copy(title = title, content = content)
                } else chapter
            }
        )
        persistDraftDebounced()
    }

    fun deleteChapter(id: String) {
        val current = _uiState.value.chapters
        if (current.size <= 1) {
            _uiState.value = _uiState.value.copy(errorMessage = "Kitapta en az bir bölüm bulunmalıdır.")
            return
        }
        val filtered = current.filterNot { it.id == id }
        val reordered = filtered.mapIndexed { index, ch -> ch.copy(chapterNumber = index + 1) }
        _uiState.value = _uiState.value.copy(
            chapters = reordered,
            editingChapterId = if (_uiState.value.editingChapterId == id) null else _uiState.value.editingChapterId
        )
        persistDraftDebounced()
    }

    fun moveChapterUp(id: String) {
        val list = _uiState.value.chapters.toMutableList()
        val index = list.indexOfFirst { it.id == id }
        if (index > 0) {
            val item = list.removeAt(index)
            list.add(index - 1, item)
            val reordered = list.mapIndexed { i, ch -> ch.copy(chapterNumber = i + 1) }
            _uiState.value = _uiState.value.copy(chapters = reordered)
            persistDraftDebounced()
        }
    }

    fun moveChapterDown(id: String) {
        val list = _uiState.value.chapters.toMutableList()
        val index = list.indexOfFirst { it.id == id }
        if (index >= 0 && index < list.size - 1) {
            val item = list.removeAt(index)
            list.add(index + 1, item)
            val reordered = list.mapIndexed { i, ch -> ch.copy(chapterNumber = i + 1) }
            _uiState.value = _uiState.value.copy(chapters = reordered)
            persistDraftDebounced()
        }
    }

    fun setEditingChapterId(id: String?) {
        _uiState.value = _uiState.value.copy(editingChapterId = id)
    }

    fun addTag(rawTag: String) {
        val clean = rawTag.trim().removePrefix("#").trim()
        if (clean.isBlank()) return
        val current = _uiState.value.tags
        if (current.size >= 15) {
            _uiState.value = _uiState.value.copy(errorMessage = "En fazla 15 etiket ekleyebilirsiniz.")
            return
        }
        if (!current.contains(clean)) {
            _uiState.value = _uiState.value.copy(tags = current + clean, errorMessage = null)
            persistDraftDebounced()
        }
    }

    fun removeTag(tag: String) {
        _uiState.value = _uiState.value.copy(tags = _uiState.value.tags.filterNot { it == tag })
        persistDraftDebounced()
    }

    fun updateLanguage(newLang: String) {
        _uiState.value = _uiState.value.copy(language = newLang)
        persistDraftDebounced()
    }

    fun updateGenre(genre: String, subGenre: String) {
        _uiState.value = _uiState.value.copy(genre = genre, subGenre = subGenre)
        persistDraftDebounced()
    }

    fun updateTargetAudience(audience: String) {
        _uiState.value = _uiState.value.copy(targetAudience = audience)
        persistDraftDebounced()
    }

    fun toggleContentWarning(warning: String) {
        val current = _uiState.value.contentWarnings.toMutableSet()
        if (current.contains(warning)) {
            current.remove(warning)
        } else {
            current.add(warning)
        }
        _uiState.value = _uiState.value.copy(contentWarnings = current)
        persistDraftDebounced()
    }

    fun updateSeries(isSeries: Boolean, name: String, order: Int) {
        _uiState.value = _uiState.value.copy(isSeries = isSeries, seriesName = name, seriesOrder = order)
        persistDraftDebounced()
    }

    fun updateVisibility(visibility: String) {
        _uiState.value = _uiState.value.copy(visibility = visibility)
        persistDraftDebounced()
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }

    fun setDiscardConfirmDialog(show: Boolean) {
        _uiState.value = _uiState.value.copy(showDiscardConfirmDialog = show)
    }

    private suspend fun uploadCoverIfNeeded(userId: String): String {
        val state = _uiState.value
        if (state.uploadedCoverUrl.isNotBlank()) return state.uploadedCoverUrl
        val bytes = state.coverBytes ?: return ""
        _uiState.value = _uiState.value.copy(isUploadingCover = true, coverUploadProgress = 10)

        val uploadRequest = StorageUploadRequest(
            fileName = state.coverFileName.ifBlank { "book-cover-" + System.currentTimeMillis() + ".jpg" },
            bytes = bytes,
            contentType = state.coverContentType.ifBlank { "image/jpeg" },
            targetDirectory = "users/$userId/covers"
        )
        val result = storageRepository.uploadMedia(
            uploadRequest,
            onProgress = { progress ->
                _uiState.value = _uiState.value.copy(coverUploadProgress = progress)
            }
        ).first()

        _uiState.value = _uiState.value.copy(isUploadingCover = false)
        return when (result) {
            is AppResult.Success -> {
                _uiState.value = _uiState.value.copy(uploadedCoverUrl = result.data)
                result.data
            }
            is AppResult.Error -> throw IllegalStateException("Kapak yüklenemedi: " + result.error.message)
        }
    }

    fun saveAsDraft(onSuccess: (Book) -> Unit) {
        val user = authRepository.currentUser.value
        if (user == null) {
            _uiState.value = _uiState.value.copy(errorMessage = "Oturum bulunamadı.")
            return
        }
        val state = _uiState.value
        val cleanTitle = state.title.trim().ifBlank { "Taslak Kitap" }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSavingDraft = true, errorMessage = null)
            try {
                val coverUrl = runCatching { uploadCoverIfNeeded(user.uid) }.getOrDefault(state.uploadedCoverUrl)
                val now = System.currentTimeMillis()
                val book = Book(
                    ownerId = user.uid,
                    authorName = user.displayName.ifBlank { user.username },
                    title = cleanTitle,
                    description = state.description.trim(),
                    discoverySummary = state.discoverySummary.trim(),
                    coverImageUrl = coverUrl,
                    category = state.category,
                    status = BookStatus.DRAFT,
                    chapterCount = state.chapters.size,
                    tags = state.tags,
                    language = state.language,
                    genre = state.genre,
                    subGenre = state.subGenre,
                    targetAudience = state.targetAudience,
                    contentWarnings = state.contentWarnings.toList(),
                    isSeries = state.isSeries,
                    seriesName = state.seriesName.trim(),
                    seriesOrder = state.seriesOrder,
                    visibility = state.visibility,
                    createdAt = now,
                    updatedAt = now
                )

                when (val result = bookRepository.createBook(book)) {
                    is AppResult.Success -> {
                        val savedBook = result.data
                        // Save chapters
                        for (chapter in state.chapters) {
                            bookRepository.saveChapter(
                                Chapter(
                                    bookId = savedBook.id,
                                    chapterNumber = chapter.chapterNumber,
                                    title = chapter.title.ifBlank { "${chapter.chapterNumber}. Bölüm" },
                                    content = chapter.content,
                                    wordCount = chapter.wordCount,
                                    isPublished = false,
                                    createdAt = now,
                                    updatedAt = now
                                )
                            )
                        }
                        draftStorage?.clearDraft()
                        _uiState.value = _uiState.value.copy(
                            isSavingDraft = false,
                            successMessage = "Kitap taslak olarak kaydedildi!"
                        )
                        onSuccess(savedBook)
                    }
                    is AppResult.Error -> {
                        _uiState.value = _uiState.value.copy(
                            isSavingDraft = false,
                            errorMessage = "Taslak kaydedilemedi: ${result.error.message}"
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isSavingDraft = false,
                    errorMessage = e.localizedMessage ?: "Taslak kaydedilirken beklenmeyen hata oluştu."
                )
            }
        }
    }

    fun publishBook(onSuccess: (Book) -> Unit) {
        val user = authRepository.currentUser.value
        if (user == null) {
            _uiState.value = _uiState.value.copy(errorMessage = "Oturum bulunamadı.")
            return
        }
        val state = _uiState.value
        if (!state.isTitleValid) {
            _uiState.value = _uiState.value.copy(errorMessage = "Kitap adı en az 2 karakter olmalıdır.")
            return
        }
        if (state.chapters.isEmpty()) {
            _uiState.value = _uiState.value.copy(errorMessage = "Yayınlamak için en az bir bölüm eklemelisiniz.")
            return
        }
        if (!state.isFirstChapterFilled) {
            _uiState.value = _uiState.value.copy(errorMessage = "Yayınlamadan önce ilk bölümün içeriğini yazmalısınız.")
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isPublishing = true, errorMessage = null)
            try {
                val coverUrl = runCatching { uploadCoverIfNeeded(user.uid) }.getOrDefault(state.uploadedCoverUrl)
                val now = System.currentTimeMillis()
                val book = Book(
                    ownerId = user.uid,
                    authorName = user.displayName.ifBlank { user.username },
                    title = state.title.trim(),
                    description = state.description.trim(),
                    discoverySummary = state.discoverySummary.trim(),
                    coverImageUrl = coverUrl,
                    category = state.category,
                    status = BookStatus.PUBLISHED,
                    chapterCount = state.chapters.size,
                    tags = state.tags,
                    language = state.language,
                    genre = state.genre,
                    subGenre = state.subGenre,
                    targetAudience = state.targetAudience,
                    contentWarnings = state.contentWarnings.toList(),
                    isSeries = state.isSeries,
                    seriesName = state.seriesName.trim(),
                    seriesOrder = state.seriesOrder,
                    visibility = state.visibility,
                    createdAt = now,
                    updatedAt = now,
                    publishedAt = now
                )

                when (val result = bookRepository.createBook(book)) {
                    is AppResult.Success -> {
                        val savedBook = result.data
                        for (chapter in state.chapters) {
                            bookRepository.saveChapter(
                                Chapter(
                                    bookId = savedBook.id,
                                    chapterNumber = chapter.chapterNumber,
                                    title = chapter.title.ifBlank { "${chapter.chapterNumber}. Bölüm" },
                                    content = chapter.content,
                                    wordCount = chapter.wordCount,
                                    isPublished = true,
                                    publishedAt = now,
                                    createdAt = now,
                                    updatedAt = now
                                )
                            )
                        }
                        val publishedBook = when (val updateRes = bookRepository.updateBook(
                            savedBook.copy(
                                status = BookStatus.PUBLISHED,
                                publishedAt = now,
                                updatedAt = now,
                                chapterCount = state.chapters.size
                            )
                        )) {
                            is AppResult.Success -> updateRes.data
                            is AppResult.Error -> savedBook.copy(status = BookStatus.PUBLISHED, publishedAt = now)
                        }
                        draftStorage?.clearDraft()
                        _uiState.value = _uiState.value.copy(
                            isPublishing = false,
                            successMessage = "Tebrikler! Kitabın başarıyla yayınlandı!"
                        )
                        onSuccess(publishedBook)
                    }
                    is AppResult.Error -> {
                        _uiState.value = _uiState.value.copy(
                            isPublishing = false,
                            errorMessage = "Kitap yayınlanamadı: ${result.error.message}"
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isPublishing = false,
                    errorMessage = e.localizedMessage ?: "Yayınlanırken beklenmeyen bir hata oluştu."
                )
            }
        }
    }
}
