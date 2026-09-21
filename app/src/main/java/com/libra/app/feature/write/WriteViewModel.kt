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
