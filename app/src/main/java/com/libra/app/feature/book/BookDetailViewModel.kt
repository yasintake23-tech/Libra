package com.libra.app.feature.book

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.libra.app.core.di.ServiceLocator
import com.libra.app.core.result.AppResult
import com.libra.app.domain.model.Book
import com.libra.app.domain.model.Chapter
import com.libra.app.domain.model.ReadingProgress
import com.libra.app.domain.model.UserProfile
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class BookDetailState(
    val book: Book? = null,
    val chapters: List<Chapter> = emptyList(),
    val progress: ReadingProgress? = null,
    val author: UserProfile? = null,
    val loading: Boolean = false,
    val error: String? = null
)

class BookDetailViewModel : ViewModel() {
    private val _state = MutableStateFlow(BookDetailState())
    val state: StateFlow<BookDetailState> = _state.asStateFlow()
    private var chaptersJob: Job? = null
    private var progressJob: Job? = null
    private var loadedBookId = ""

    fun load(book: Book, userId: String) {
        if (book.id.isBlank()) {
            _state.value = BookDetailState(error = "Kitap kimliği bulunamadı.")
            return
        }
        if (loadedBookId == book.id && _state.value.book?.id == book.id) return
        loadedBookId = book.id
        chaptersJob?.cancel()
        progressJob?.cancel()
        _state.value = BookDetailState(book = book, loading = true)

        viewModelScope.launch {
            if (book.ownerId.isNotBlank()) {
                when (val result = ServiceLocator.userRepository.getUserProfileFresh(book.ownerId)) {
                    is AppResult.Success -> _state.value = _state.value.copy(author = result.data)
                    is AppResult.Error -> Unit
                }
            }
        }

        chaptersJob = viewModelScope.launch {
            ServiceLocator.bookRepository.getBookChapters(book.id).collect { result ->
                when (result) {
                    is AppResult.Success -> _state.value = _state.value.copy(
                        chapters = result.data.filter { it.isPublished }.sortedBy { it.chapterNumber },
                        loading = false,
                        error = null
                    )
                    is AppResult.Error -> _state.value = _state.value.copy(
                        loading = false,
                        error = result.error.message
                    )
                }
            }
        }

        progressJob = viewModelScope.launch {
            ServiceLocator.bookRepository.getReadingProgress(userId, book.id).collect { result ->
                if (result is AppResult.Success) _state.value = _state.value.copy(progress = result.data)
            }
        }
    }

    fun clear() {
        chaptersJob?.cancel()
        progressJob?.cancel()
        loadedBookId = ""
        _state.value = BookDetailState()
    }
}
