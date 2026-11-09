package com.libra.app.feature.book.manage

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.libra.app.core.di.ServiceLocator
import com.libra.app.core.result.AppError
import com.libra.app.core.result.AppResult
import com.libra.app.domain.model.Book
import com.libra.app.domain.model.BookCategory
import com.libra.app.domain.model.BookComment
import com.libra.app.domain.model.BookStatus
import com.libra.app.domain.model.Chapter
import com.libra.app.domain.model.StorageUploadRequest
import com.libra.app.domain.repository.AuthRepository
import com.libra.app.domain.repository.BookRepository
import com.libra.app.domain.repository.StorageRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.UUID

enum class ManagementSubPage {
    HUB,
    METADATA,
    CHAPTERS,
    STUDIO,
    PUBLISHING,
    PRIVACY,
    ANALYTICS,
    COMMENTS,
    DRAFTS
}

data class BookManagementState(
    val book: Book? = null,
    val chapters: List<Chapter> = emptyList(),
    val comments: List<BookComment> = emptyList(),
    val activeSubPage: ManagementSubPage = ManagementSubPage.HUB,
    val selectedChapterId: String? = null,
    val activeStudioChapter: Chapter? = null,
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val isAutoSaving: Boolean = false,
    val isPublishing: Boolean = false,
    val isUploadingCover: Boolean = false,
    val coverUploadProgress: Int = 0,
    val coverUploadError: String? = null,
    val lastSavedTimestamp: Long = 0L,
    val errorMessage: String? = null,
    val successMessage: String? = null,
    val hasUnsavedChanges: Boolean = false
) {
    val activeChapters: List<Chapter> get() = chapters.filter { !it.isArchived }
    val archivedChapters: List<Chapter> get() = chapters.filter { it.isArchived }
}

class BookManagementViewModel(
    private val bookRepository: BookRepository = ServiceLocator.bookRepository,
    private val authRepository: AuthRepository = ServiceLocator.authRepository,
    private val storageRepository: StorageRepository = ServiceLocator.storageRepository
) : ViewModel() {

    private val _state = MutableStateFlow(BookManagementState())
    val state: StateFlow<BookManagementState> = _state.asStateFlow()

    private var chaptersJob: Job? = null
    private var bookJob: Job? = null
    private var commentsJob: Job? = null
    private var autosaveJob: Job? = null
    private var lastCoverBytes: ByteArray? = null
    private var lastCoverFileName: String? = null
    private var lastCoverContentType: String? = null

    fun initialize(book: Book) {
        if (_state.value.book?.id == book.id && _state.value.chapters.isNotEmpty()) return
        _state.value = BookManagementState(book = book, isLoading = true)

        observeBook(book.id)
        observeChapters(book.id)
        observeComments(book.id)
    }

    private fun observeBook(bookId: String) {
        bookJob?.cancel()
        bookJob = viewModelScope.launch {
            bookRepository.getBookById(bookId).collect { result ->
                if (result is AppResult.Success && result.data != null) {
                    _state.value = _state.value.copy(book = result.data)
                }
            }
        }
    }

    private fun observeChapters(bookId: String) {
        chaptersJob?.cancel()
        chaptersJob = viewModelScope.launch {
            bookRepository.getBookChapters(bookId).collect { result ->
                when (result) {
                    is AppResult.Success -> {
                        val chapters = result.data.sortedBy { it.chapterNumber }
                        val activeStudio = if (_state.value.activeStudioChapter != null) {
                            chapters.find { it.id == _state.value.activeStudioChapter?.id }
                                ?: _state.value.activeStudioChapter
                        } else {
                            chapters.firstOrNull()
                        }
                        _state.value = _state.value.copy(
                            chapters = chapters,
                            activeStudioChapter = activeStudio,
                            isLoading = false
                        )
                    }
                    is AppResult.Error -> {
                        _state.value = _state.value.copy(
                            isLoading = false,
                            errorMessage = result.error.message
                        )
                    }
                }
            }
        }
    }

    private fun observeComments(bookId: String) {
        commentsJob?.cancel()
        commentsJob = viewModelScope.launch {
            bookRepository.getBookComments(bookId).collect { result ->
                if (result is AppResult.Success) {
                    _state.value = _state.value.copy(comments = result.data)
                }
            }
        }
    }

    fun navigateToSubPage(subPage: ManagementSubPage, chapterId: String? = null) {
        val targetChapter = if (chapterId != null) {
            _state.value.chapters.find { it.id == chapterId }
        } else {
            _state.value.activeStudioChapter ?: _state.value.chapters.firstOrNull()
        }
        _state.value = _state.value.copy(
            activeSubPage = subPage,
            selectedChapterId = chapterId ?: targetChapter?.id,
            activeStudioChapter = targetChapter,
            errorMessage = null,
            successMessage = null
        )
    }

    fun updateBookMetadata(
        title: String,
        description: String,
        discoverySummary: String,
        category: BookCategory,
        genre: String,
        subGenre: String,
        tags: List<String>,
        language: String,
        targetAudience: String,
        isSeries: Boolean,
        seriesName: String,
        seriesOrder: Int
    ) {
        val current = _state.value.book ?: return
        if (title.trim().isBlank()) {
            _state.value = _state.value.copy(errorMessage = "Kitap başlığı boş olamaz.")
            return
        }

        val updated = current.copy(
            title = title.trim(),
            description = description.trim(),
            discoverySummary = discoverySummary.trim(),
            category = category,
            genre = genre.trim(),
            subGenre = subGenre.trim(),
            tags = tags,
            language = language,
            targetAudience = targetAudience,
            isSeries = isSeries,
            seriesName = seriesName.trim(),
            seriesOrder = seriesOrder,
            updatedAt = System.currentTimeMillis()
        )

        viewModelScope.launch {
            _state.value = _state.value.copy(isSaving = true, errorMessage = null)
            when (val result = bookRepository.updateBook(updated)) {
                is AppResult.Success -> {
                    _state.value = _state.value.copy(
                        book = result.data,
                        isSaving = false,
                        successMessage = "Kitap bilgileri başarıyla güncellendi."
                    )
                }
                is AppResult.Error -> {
                    _state.value = _state.value.copy(
                        isSaving = false,
                        errorMessage = result.error.message
                    )
                }
            }
        }
    }

    fun uploadBookCover(bytes: ByteArray, fileName: String, contentType: String) {
        val current = _state.value.book ?: return
        val user = authRepository.currentUser.value ?: return

        lastCoverBytes = bytes
        lastCoverFileName = fileName
        lastCoverContentType = contentType

        viewModelScope.launch {
            _state.value = _state.value.copy(
                isSaving = true,
                isUploadingCover = true,
                coverUploadProgress = 0,
                coverUploadError = null,
                errorMessage = null
            )
            try {
                val uploadReq = StorageUploadRequest(
                    fileName = fileName.ifBlank { "cover-${System.currentTimeMillis()}.jpg" },
                    bytes = bytes,
                    contentType = contentType.ifBlank { "image/jpeg" },
                    targetDirectory = "users/${user.uid}/covers"
                )
                when (val upload = storageRepository.uploadMedia(
                    uploadReq,
                    onProgress = { progress ->
                        _state.value = _state.value.copy(coverUploadProgress = progress)
                    }
                ).first()) {
                    is AppResult.Success -> {
                        val cdnUrl = storageRepository.getPublicCdnUrl(upload.data)
                        val updatedBook = current.copy(
                            coverImageUrl = cdnUrl,
                            updatedAt = System.currentTimeMillis()
                        )
                        when (val updateResult = bookRepository.updateBook(updatedBook)) {
                            is AppResult.Success -> {
                                _state.value = _state.value.copy(
                                    book = updateResult.data,
                                    isSaving = false,
                                    isUploadingCover = false,
                                    coverUploadProgress = 100,
                                    coverUploadError = null,
                                    successMessage = "Kapak görseli başarıyla güncellendi."
                                )
                            }
                            is AppResult.Error -> {
                                _state.value = _state.value.copy(
                                    isSaving = false,
                                    isUploadingCover = false,
                                    coverUploadError = updateResult.error.message,
                                    errorMessage = updateResult.error.message
                                )
                            }
                        }
                    }
                    is AppResult.Error -> {
                        _state.value = _state.value.copy(
                            isSaving = false,
                            isUploadingCover = false,
                            coverUploadError = upload.error.message,
                            errorMessage = "Kapak yüklenemedi: ${upload.error.message}"
                        )
                    }
                }
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    isSaving = false,
                    isUploadingCover = false,
                    coverUploadError = e.localizedMessage,
                    errorMessage = e.localizedMessage ?: "Kapak yüklenirken hata oluştu."
                )
            }
        }
    }

    fun retryCoverUpload() {
        val bytes = lastCoverBytes ?: return
        val name = lastCoverFileName.orEmpty()
        val mime = lastCoverContentType.orEmpty()
        uploadBookCover(bytes, name, mime)
    }

    fun clearCoverError() {
        _state.value = _state.value.copy(coverUploadError = null)
    }

    fun createNewChapter(title: String = "") {
        val currentBook = _state.value.book ?: return
        val currentChapters = _state.value.chapters
        val nextNumber = (currentChapters.maxOfOrNull { it.chapterNumber } ?: 0) + 1
        val newChapter = Chapter(
            bookId = currentBook.id,
            chapterNumber = nextNumber,
            title = title.ifBlank { "$nextNumber. Bölüm" },
            content = "",
            isPublished = currentBook.status == BookStatus.PUBLISHED,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )

        viewModelScope.launch {
            _state.value = _state.value.copy(isSaving = true, errorMessage = null)
            when (val result = bookRepository.saveChapter(newChapter)) {
                is AppResult.Success -> {
                    val updatedChapters = (_state.value.chapters.filterNot { it.id == result.data.id } + result.data).sortedBy { it.chapterNumber }
                    _state.value = _state.value.copy(
                        isSaving = false,
                        chapters = updatedChapters,
                        activeStudioChapter = result.data,
                        selectedChapterId = result.data.id,
                        activeSubPage = ManagementSubPage.STUDIO,
                        successMessage = "Yeni bölüm oluşturuldu."
                    )
                }
                is AppResult.Error -> {
                    _state.value = _state.value.copy(
                        isSaving = false,
                        errorMessage = result.error.message
                    )
                }
            }
        }
    }

    fun selectChapterForStudio(chapter: Chapter) {
        _state.value = _state.value.copy(
            activeStudioChapter = chapter,
            selectedChapterId = chapter.id,
            activeSubPage = ManagementSubPage.STUDIO
        )
    }

    fun updateStudioChapterContent(title: String, content: String) {
        val active = _state.value.activeStudioChapter ?: return
        val updated = active.copy(
            title = title,
            content = content,
            wordCount = content.trim().split(Regex("\\s+")).count { it.isNotBlank() }
        )
        _state.value = _state.value.copy(
            activeStudioChapter = updated,
            hasUnsavedChanges = true
        )

        // Debounced autosave
        autosaveJob?.cancel()
        autosaveJob = viewModelScope.launch {
            delay(1500)
            saveChapterNow(updated, isAutosave = true)
        }
    }

    fun saveChapterNow(chapter: Chapter? = null, isAutosave: Boolean = false) {
        val target = chapter ?: _state.value.activeStudioChapter ?: return
        if (target.bookId.isBlank() || target.title.isBlank()) return

        viewModelScope.launch {
            if (isAutosave) {
                _state.value = _state.value.copy(isAutoSaving = true)
            } else {
                _state.value = _state.value.copy(isSaving = true, errorMessage = null)
            }

            when (val result = bookRepository.saveChapter(target)) {
                is AppResult.Success -> {
                    val updatedChapters = _state.value.chapters.map { if (it.id == result.data.id) result.data else it }
                    _state.value = _state.value.copy(
                        activeStudioChapter = result.data,
                        chapters = updatedChapters,
                        isSaving = false,
                        isAutoSaving = false,
                        hasUnsavedChanges = false,
                        lastSavedTimestamp = System.currentTimeMillis(),
                        successMessage = if (!isAutosave) "Bölüm kaydedildi." else null
                    )
                }
                is AppResult.Error -> {
                    _state.value = _state.value.copy(
                        isSaving = false,
                        isAutoSaving = false,
                        errorMessage = result.error.message
                    )
                }
            }
        }
    }

    fun toggleChapterPublishStatus(chapter: Chapter) {
        val updated = chapter.copy(
            isPublished = !chapter.isPublished,
            publishedAt = if (!chapter.isPublished) System.currentTimeMillis() else chapter.publishedAt,
            updatedAt = System.currentTimeMillis()
        )
        viewModelScope.launch {
            when (val result = bookRepository.saveChapter(updated)) {
                is AppResult.Success -> {
                    val updatedChapters = _state.value.chapters.map { if (it.id == result.data.id) result.data else it }
                    _state.value = _state.value.copy(
                        chapters = updatedChapters,
                        successMessage = if (result.data.isPublished) "Bölüm yayınlandı." else "Bölüm taslağa alındı."
                    )
                }
                is AppResult.Error -> {
                    _state.value = _state.value.copy(errorMessage = result.error.message)
                }
            }
        }
    }

    fun duplicateChapter(chapter: Chapter) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isSaving = true, errorMessage = null)
            when (val result = bookRepository.duplicateChapter(chapter.bookId, chapter.id)) {
                is AppResult.Success -> {
                    val updatedChapters = (_state.value.chapters.filterNot { it.id == result.data.id } + result.data).sortedBy { it.chapterNumber }
                    _state.value = _state.value.copy(
                        isSaving = false,
                        chapters = updatedChapters,
                        successMessage = "Bölüm kopyalandı."
                    )
                }
                is AppResult.Error -> {
                    _state.value = _state.value.copy(
                        isSaving = false,
                        errorMessage = result.error.message
                    )
                }
            }
        }
    }

    fun deleteChapter(chapter: Chapter) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isSaving = true, errorMessage = null)
            when (val result = bookRepository.deleteChapter(chapter.bookId, chapter.id)) {
                is AppResult.Success -> {
                    val remainingChapters = _state.value.chapters.filterNot { it.id == chapter.id }
                    _state.value = _state.value.copy(
                        isSaving = false,
                        chapters = remainingChapters,
                        activeStudioChapter = remainingChapters.firstOrNull(),
                        successMessage = "Bölüm silindi."
                    )
                }
                is AppResult.Error -> {
                    _state.value = _state.value.copy(
                        isSaving = false,
                        errorMessage = result.error.message
                    )
                }
            }
        }
    }

    fun moveChapterUp(chapter: Chapter) {
        val list = _state.value.chapters.toMutableList()
        val index = list.indexOfFirst { it.id == chapter.id }
        if (index > 0) {
            val item = list.removeAt(index)
            list.add(index - 1, item)
            reorderChaptersList(list)
        }
    }

    fun moveChapterDown(chapter: Chapter) {
        val list = _state.value.chapters.toMutableList()
        val index = list.indexOfFirst { it.id == chapter.id }
        if (index >= 0 && index < list.size - 1) {
            val item = list.removeAt(index)
            list.add(index + 1, item)
            reorderChaptersList(list)
        }
    }

    private fun reorderChaptersList(reordered: List<Chapter>) {
        val bookId = _state.value.book?.id ?: return
        viewModelScope.launch {
            _state.value = _state.value.copy(chapters = reordered)
            bookRepository.reorderChapters(bookId, reordered)
        }
    }

    fun archiveChapter(chapter: Chapter) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isSaving = true, errorMessage = null)
            when (val result = bookRepository.archiveChapter(chapter.bookId, chapter.id)) {
                is AppResult.Success -> {
                    val updatedChapters = _state.value.chapters.map {
                        if (it.id == chapter.id) it.copy(isArchived = true, isPublished = false) else it
                    }
                    _state.value = _state.value.copy(
                        isSaving = false,
                        chapters = updatedChapters,
                        activeStudioChapter = updatedChapters.firstOrNull { !it.isArchived },
                        successMessage = "Bölüm arşivlendi."
                    )
                }
                is AppResult.Error -> {
                    _state.value = _state.value.copy(
                        isSaving = false,
                        errorMessage = result.error.message
                    )
                }
            }
        }
    }

    fun unarchiveChapter(chapter: Chapter) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isSaving = true, errorMessage = null)
            when (val result = bookRepository.unarchiveChapter(chapter.bookId, chapter.id)) {
                is AppResult.Success -> {
                    val updatedChapters = _state.value.chapters.map {
                        if (it.id == chapter.id) it.copy(isArchived = false) else it
                    }
                    _state.value = _state.value.copy(
                        isSaving = false,
                        chapters = updatedChapters,
                        successMessage = "Bölüm arşivden geri yüklendi."
                    )
                }
                is AppResult.Error -> {
                    _state.value = _state.value.copy(
                        isSaving = false,
                        errorMessage = result.error.message
                    )
                }
            }
        }
    }

    fun validatePrePublish(): List<String> {
        val missing = mutableListOf<String>()
        val book = _state.value.book
        if (book == null) {
            missing.add("Kitap verisi bulunamadı.")
            return missing
        }
        if (book.title.trim().isBlank()) {
            missing.add("Kitap başlığı boş olamaz.")
        }
        if (book.description.trim().isBlank()) {
            missing.add("Kitap açıklaması girilmelidir.")
        }
        val active = _state.value.activeChapters
        if (active.isEmpty()) {
            missing.add("En az 1 adet aktif bölüm eklenmelidir.")
        }
        return missing
    }

    fun saveAllBook() {
        saveChapterNow(isAutosave = false)
        val book = _state.value.book ?: return
        viewModelScope.launch {
            bookRepository.updateBook(book)
            _state.value = _state.value.copy(successMessage = "Tüm kitap başarıyla kaydedildi.")
        }
    }

    fun publishBook(force: Boolean = false) {
        val current = _state.value.book ?: return
        if (!force) {
            val missing = validatePrePublish()
            if (missing.isNotEmpty()) {
                _state.value = _state.value.copy(
                    errorMessage = "Yayınlama öncesi eksikler var:\n" + missing.joinToString("\n• ", prefix = "• ")
                )
                return
            }
        }
        viewModelScope.launch {
            _state.value = _state.value.copy(isPublishing = true, errorMessage = null)
            when (val result = bookRepository.publishBook(current.id)) {
                is AppResult.Success -> {
                    val publishedChapters = _state.value.chapters.map {
                        if (!it.isArchived) it.copy(isPublished = true) else it
                    }
                    _state.value = _state.value.copy(
                        book = result.data,
                        chapters = publishedChapters,
                        isPublishing = false,
                        successMessage = "Tebrikler! Kitabın başarıyla yayınlandı!"
                    )
                }
                is AppResult.Error -> {
                    _state.value = _state.value.copy(
                        isPublishing = false,
                        errorMessage = result.error.message
                    )
                }
            }
        }
    }

    fun unpublishBook() {
        val current = _state.value.book ?: return
        viewModelScope.launch {
            _state.value = _state.value.copy(isPublishing = true, errorMessage = null)
            when (val result = bookRepository.unpublishBook(current.id)) {
                is AppResult.Success -> {
                    _state.value = _state.value.copy(
                        book = result.data,
                        isPublishing = false,
                        successMessage = "Kitap taslak durumuna alındı."
                    )
                }
                is AppResult.Error -> {
                    _state.value = _state.value.copy(
                        isPublishing = false,
                        errorMessage = result.error.message
                    )
                }
            }
        }
    }

    fun updateVisibilityAndPrivacy(
        visibility: String,
        contentWarnings: List<String>
    ) {
        val current = _state.value.book ?: return
        val updated = current.copy(
            visibility = visibility,
            contentWarnings = contentWarnings,
            updatedAt = System.currentTimeMillis()
        )
        viewModelScope.launch {
            _state.value = _state.value.copy(isSaving = true)
            when (val result = bookRepository.updateBook(updated)) {
                is AppResult.Success -> {
                    _state.value = _state.value.copy(
                        book = result.data,
                        isSaving = false,
                        successMessage = "Gizlilik ayarları kaydedildi."
                    )
                }
                is AppResult.Error -> {
                    _state.value = _state.value.copy(
                        isSaving = false,
                        errorMessage = result.error.message
                    )
                }
            }
        }
    }

    fun uploadStudioChapterImage(bytes: ByteArray, fileName: String, contentType: String, onInserted: (String) -> Unit) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isSaving = true, errorMessage = null)
            when (val upload = storageRepository.uploadMedia(
                StorageUploadRequest(
                    fileName = fileName,
                    bytes = bytes,
                    contentType = contentType
                )
            ).first()) {
                is AppResult.Success -> {
                    val url = storageRepository.getPublicCdnUrl(upload.data)
                    onInserted("\n[[IMAGE:$url|Görsel]]\n")
                    _state.value = _state.value.copy(isSaving = false)
                }
                is AppResult.Error -> {
                    _state.value = _state.value.copy(isSaving = false, errorMessage = upload.error.message)
                }
            }
        }
    }

    fun deleteBookComment(commentId: String) {
        val bookId = _state.value.book?.id ?: return
        viewModelScope.launch {
            bookRepository.deleteBookComment(bookId, commentId)
        }
    }

    fun clearMessages() {
        _state.value = _state.value.copy(errorMessage = null, successMessage = null)
    }

    fun clear() {
        bookJob?.cancel()
        chaptersJob?.cancel()
        commentsJob?.cancel()
        autosaveJob?.cancel()
        _state.value = BookManagementState()
    }
}
