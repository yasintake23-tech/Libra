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

import com.libra.app.domain.model.ShelfType

data class BookDetailState(
    val book: Book? = null,
    val chapters: List<Chapter> = emptyList(),
    val progress: ReadingProgress? = null,
    val author: UserProfile? = null,
    val isLiked: Boolean = false,
    val isSaved: Boolean = false,
    val likesCount: Int = 0,
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
        _state.value = BookDetailState(
            book = book,
            likesCount = book.likesCount,
            loading = true
        )

        viewModelScope.launch {
            if (book.ownerId.isNotBlank()) {
                when (val result = ServiceLocator.userRepository.getUserProfileFresh(book.ownerId)) {
                    is AppResult.Success -> _state.value = _state.value.copy(author = result.data)
                    is AppResult.Error -> Unit
                }
            }
        }

        viewModelScope.launch {
            if (userId.isNotBlank()) {
                when (val engagement = ServiceLocator.bookRepository.getBookEngagement(book.id, userId)) {
                    is AppResult.Success -> _state.value = _state.value.copy(
                        isLiked = engagement.data.liked,
                        isSaved = engagement.data.saved
                    )
                    is AppResult.Error -> Unit
                }
            }
        }

        chaptersJob = viewModelScope.launch {
            ServiceLocator.bookRepository.getBookChapters(book.id).collect { result ->
                when (result) {
                    is AppResult.Success -> {
                        val allChapters = result.data.sortedBy { it.chapterNumber }
                        val isAuthor = userId.isNotBlank() && book.ownerId == userId
                        val publishedChapters = allChapters.filter { it.isPublished }

                        val visibleChapters = when {
                            isAuthor -> allChapters
                            publishedChapters.isNotEmpty() -> publishedChapters
                            book.status == com.libra.app.domain.model.BookStatus.PUBLISHED -> allChapters
                            else -> emptyList()
                        }

                        val currentBook = _state.value.book ?: book
                        val updatedBook = if (visibleChapters.isNotEmpty()) {
                            currentBook.copy(chapterCount = visibleChapters.size)
                        } else {
                            currentBook
                        }

                        _state.value = _state.value.copy(
                            book = updatedBook,
                            chapters = visibleChapters,
                            loading = false,
                            error = null
                        )
                    }
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

    fun toggleLike(userId: String) {
        val currentBook = _state.value.book ?: return
        if (userId.isBlank()) return
        val currentlyLiked = _state.value.isLiked
        val currentCount = _state.value.likesCount
        val newLiked = !currentlyLiked
        val newCount = (currentCount + if (newLiked) 1 else -1).coerceAtLeast(0)
        _state.value = _state.value.copy(isLiked = newLiked, likesCount = newCount)

        viewModelScope.launch {
            when (val result = ServiceLocator.bookRepository.toggleBookLike(currentBook.id, userId)) {
                is AppResult.Success -> _state.value = _state.value.copy(isLiked = result.data)
                is AppResult.Error -> _state.value = _state.value.copy(isLiked = currentlyLiked, likesCount = currentCount)
            }
        }
    }

    fun toggleSave(userId: String) {
        val currentBook = _state.value.book ?: return
        if (userId.isBlank()) return
        val currentlySaved = _state.value.isSaved
        val newSaved = !currentlySaved
        _state.value = _state.value.copy(isSaved = newSaved)

        viewModelScope.launch {
            when (val result = ServiceLocator.bookRepository.toggleBookSave(currentBook.id, userId)) {
                is AppResult.Success -> {
                    _state.value = _state.value.copy(isSaved = result.data)
                    if (result.data) {
                        ServiceLocator.bookRepository.addBookToShelf(userId, currentBook.id, ShelfType.SAVED)
                    } else {
                        ServiceLocator.bookRepository.removeBookFromShelf(userId, currentBook.id, ShelfType.SAVED)
                    }
                }
                is AppResult.Error -> _state.value = _state.value.copy(isSaved = currentlySaved)
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
