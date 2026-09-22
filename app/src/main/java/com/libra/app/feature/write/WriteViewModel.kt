package com.libra.app.feature.write

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.libra.app.core.di.ServiceLocator
import com.libra.app.core.result.AppResult
import com.libra.app.core.state.UiState
import com.libra.app.domain.model.Book
import com.libra.app.domain.model.BookCategory
import com.libra.app.domain.model.BookStatus
import com.libra.app.domain.model.Chapter
import com.libra.app.domain.repository.AuthRepository
import com.libra.app.domain.repository.BookRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class WriteState(val myBooks: List<Book> = emptyList(), val isCreatingBook: Boolean = false, val lastErrorMessage: String? = null)



class WriteViewModel(
    private val authRepository: AuthRepository = ServiceLocator.authRepository,
    private val bookRepository: BookRepository = ServiceLocator.bookRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow<UiState<WriteState>>(UiState.Loading)
    private val _editorChapters = MutableStateFlow<List<Chapter>>(emptyList())
    val editorChapters: StateFlow<List<Chapter>> = _editorChapters.asStateFlow()
    private val _editorSaving = MutableStateFlow(false)
    val editorSaving: StateFlow<Boolean> = _editorSaving.asStateFlow()
    private val _editorError = MutableStateFlow<String?>(null)
    val editorError: StateFlow<String?> = _editorError.asStateFlow()
    val uiState: StateFlow<UiState<WriteState>> = _uiState.asStateFlow()

    init { loadMyBooks() }

    fun loadMyBooks() {
        viewModelScope.launch {
            val userId = authRepository.currentUser.value?.uid
            if (userId == null) { _uiState.value = UiState.Error(com.libra.app.core.result.AppError.Auth("Oturum bulunamadı.")); return@launch }
            _uiState.value = UiState.Loading
            bookRepository.getUserWrittenBooks(userId).collect { result ->
                _uiState.value = when (result) {
                    is AppResult.Success -> UiState.Success(WriteState(myBooks = result.data))
                    is AppResult.Error -> UiState.Error(result.error)
                }
            }
        }
    }

    fun loadChapters(bookId: String) {
        viewModelScope.launch {
            bookRepository.getBookChapters(bookId).collect { result ->
                when (result) {
                    is AppResult.Success -> _editorChapters.value = result.data
                    is AppResult.Error -> _editorError.value = result.error.message
                }
            }
        }
    }

    fun saveBook(book: Book) {
        viewModelScope.launch {
            _editorSaving.value = true
            _editorError.value = null
            when (val result = bookRepository.updateBook(book)) {
                is AppResult.Success -> loadMyBooks()
                is AppResult.Error -> _editorError.value = result.error.message
            }
            _editorSaving.value = false
        }
    }

    fun saveChapter(chapter: Chapter) {
        viewModelScope.launch {
            _editorSaving.value = true
            _editorError.value = null
            when (val result = bookRepository.saveChapter(chapter)) {
                is AppResult.Success -> {
                    _editorChapters.value = _editorChapters.value
                        .filterNot { it.id == result.data.id } + result.data
                    _editorChapters.value = _editorChapters.value.sortedBy { it.chapterNumber }
                }
                is AppResult.Error -> _editorError.value = result.error.message
            }
            _editorSaving.value = false
        }
    }

    fun publishBook(book: Book) {
        viewModelScope.launch {
            _editorSaving.value = true
            _editorError.value = null
            val chapters = _editorChapters.value
            if (chapters.isEmpty() || chapters.any { it.content.trim().isBlank() }) {
                _editorError.value = "Yayınlamak için en az bir bölüm ve bölüm içeriği gerekli."
                _editorSaving.value = false
                return@launch
            }
            val now = System.currentTimeMillis()
            val publishedChapters = mutableListOf<Chapter>()
            for (chapter in chapters) {
                when (val saved = bookRepository.saveChapter(chapter.copy(isPublished = true, updatedAt = now))) {
                    is AppResult.Success -> publishedChapters += saved.data
                    is AppResult.Error -> {
                        _editorError.value = "Bölüm ${chapter.chapterNumber} yayınlanamadı: ${saved.error.message}"
                        _editorSaving.value = false
                        return@launch
                    }
                }
            }
            when (
                val result = bookRepository.updateBook(
                    book.copy(
                        status = BookStatus.PUBLISHED,
                        chapterCount = publishedChapters.size,
                        updatedAt = now
                    )
                )
            ) {
                is AppResult.Success -> {
                    _editorChapters.value = publishedChapters.sortedBy { it.chapterNumber }
                    loadMyBooks()
                }
                is AppResult.Error -> _editorError.value = result.error.message
            }
            _editorSaving.value = false
        }
    }

    fun clearEditorError() {
        _editorError.value = null
    }

    fun createNewBook(title: String, description: String, category: BookCategory) {
        val user = authRepository.currentUser.value
        if (user == null) { _uiState.value = UiState.Error(com.libra.app.core.result.AppError.Auth("Oturum bulunamadı.")); return }
        if (title.trim().isBlank()) { _uiState.value = UiState.Error(com.libra.app.core.result.AppError.Validation("Kitap başlığı boş olamaz.")); return }
        viewModelScope.launch {
            _uiState.value = UiState.Loading
            val book = Book(ownerId = user.uid, authorName = user.displayName, title = title.trim(), description = description.trim(), category = category, status = BookStatus.DRAFT)
            when (val result = bookRepository.createBook(book)) {
                is AppResult.Error -> _uiState.value = UiState.Error(result.error)
                is AppResult.Success -> {
                    val initialChapter = Chapter(bookId = result.data.id, chapterNumber = 1, title = "1. Bölüm")
                    when (val chapterResult = bookRepository.saveChapter(initialChapter)) {
                        is AppResult.Error -> _uiState.value = UiState.Error(chapterResult.error)
                        is AppResult.Success -> loadMyBooks()
                    }
                }
            }
        }
    }
}
