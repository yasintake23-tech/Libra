package com.libra.app.feature.book

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.libra.app.core.di.ServiceLocator
import com.libra.app.core.result.AppResult
import com.libra.app.domain.model.Book
import com.libra.app.domain.model.BookComment
import com.libra.app.domain.model.Chapter
import com.libra.app.domain.model.ReadingProgress
import com.libra.app.domain.model.ShelfType
import com.libra.app.domain.model.UserProfile
import com.libra.app.domain.repository.AuthRepository
import com.libra.app.domain.repository.BookRepository
import com.libra.app.domain.repository.UserRepository
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
    val isLiked: Boolean = false,
    val isSaved: Boolean = false,
    val likesCount: Int = 0,
    val comments: List<BookComment> = emptyList(),
    val commentsCount: Int = 0,
    val isLoadingComments: Boolean = false,
    val isSendingComment: Boolean = false,
    val commentError: String? = null,
    val loading: Boolean = false,
    val error: String? = null
)

class BookDetailViewModel(
    private val bookRepository: BookRepository = ServiceLocator.bookRepository,
    private val userRepository: UserRepository = ServiceLocator.userRepository,
    private val authRepository: AuthRepository = ServiceLocator.authRepository
) : ViewModel() {
    private val _state = MutableStateFlow(BookDetailState())
    val state: StateFlow<BookDetailState> = _state.asStateFlow()
    private var chaptersJob: Job? = null
    private var progressJob: Job? = null
    private var commentsJob: Job? = null
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
        commentsJob?.cancel()
        _state.value = BookDetailState(
            book = book,
            likesCount = book.likesCount,
            commentsCount = book.commentsCount,
            isLoadingComments = true,
            loading = true
        )

        viewModelScope.launch {
            if (book.ownerId.isNotBlank()) {
                when (val result = userRepository.getUserProfileFresh(book.ownerId)) {
                    is AppResult.Success -> _state.value = _state.value.copy(author = result.data)
                    is AppResult.Error -> Unit
                }
            }
        }

        viewModelScope.launch {
            if (userId.isNotBlank()) {
                when (val engagement = bookRepository.getBookEngagement(book.id, userId)) {
                    is AppResult.Success -> _state.value = _state.value.copy(
                        isLiked = engagement.data.liked,
                        isSaved = engagement.data.saved
                    )
                    is AppResult.Error -> Unit
                }
            }
        }

        chaptersJob = viewModelScope.launch {
            bookRepository.getBookChapters(book.id).collect { result ->
                when (result) {
                    is AppResult.Success -> {
                        val allChapters = result.data.sortedBy { it.chapterNumber }
                        val nonArchivedChapters = allChapters.filter { !it.isArchived }
                        val isAuthor = userId.isNotBlank() && book.ownerId == userId
                        val publishedChapters = nonArchivedChapters.filter { it.isPublished }

                        val visibleChapters = when {
                            isAuthor -> allChapters
                            publishedChapters.isNotEmpty() -> publishedChapters
                            book.status == com.libra.app.domain.model.BookStatus.PUBLISHED -> nonArchivedChapters
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
            bookRepository.getReadingProgress(userId, book.id).collect { result ->
                if (result is AppResult.Success) _state.value = _state.value.copy(progress = result.data)
            }
        }

        commentsJob = viewModelScope.launch {
            bookRepository.getBookComments(book.id).collect { result ->
                when (result) {
                    is AppResult.Success -> {
                        _state.value = _state.value.copy(
                            comments = result.data,
                            commentsCount = result.data.size,
                            isLoadingComments = false,
                            commentError = null
                        )
                    }
                    is AppResult.Error -> {
                        _state.value = _state.value.copy(
                            isLoadingComments = false,
                            commentError = result.error.message
                        )
                    }
                }
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
            when (val result = bookRepository.toggleBookLike(currentBook.id, userId)) {
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
            when (val result = bookRepository.toggleBookSave(currentBook.id, userId)) {
                is AppResult.Success -> {
                    _state.value = _state.value.copy(isSaved = result.data)
                    if (result.data) {
                        bookRepository.addBookToShelf(userId, currentBook.id, ShelfType.SAVED)
                    } else {
                        bookRepository.removeBookFromShelf(userId, currentBook.id, ShelfType.SAVED)
                    }
                }
                is AppResult.Error -> _state.value = _state.value.copy(isSaved = currentlySaved)
            }
        }
    }

    fun addComment(text: String, parentCommentId: String = "") {
        val currentBook = _state.value.book ?: return
        val user = authRepository.currentUser.value ?: return
        val cleanText = text.trim()
        if (cleanText.isBlank()) return
        if (!user.moderation.canComment) {
            _state.value = _state.value.copy(
                commentError = "Yorum yapma yetkiniz geçici olarak kısıtlandı."
            )
            return
        }

        val pending = BookComment(
            id = "",
            bookId = currentBook.id,
            userId = user.uid,
            authorName = user.displayName.ifBlank { user.username.ifBlank { "Libra Kullanıcısı" } },
            authorUsername = user.username,
            authorPhotoUrl = user.profileImageUrl,
            text = cleanText,
            parentCommentId = parentCommentId.trim(),
            createdAt = System.currentTimeMillis()
        )

        _state.value = _state.value.copy(isSendingComment = true, commentError = null)

        viewModelScope.launch {
            when (val result = bookRepository.addBookComment(pending)) {
                is AppResult.Success -> {
                    val saved = result.data
                    val merged = if (_state.value.comments.any { it.id == saved.id }) {
                        _state.value.comments
                    } else {
                        listOf(saved) + _state.value.comments
                    }
                    _state.value = _state.value.copy(
                        isSendingComment = false,
                        comments = merged,
                        commentsCount = merged.size
                    )
                }
                is AppResult.Error -> {
                    _state.value = _state.value.copy(
                        isSendingComment = false,
                        commentError = result.error.message
                    )
                }
            }
        }
    }

    fun deleteComment(commentId: String) {
        val currentBook = _state.value.book ?: return
        if (commentId.isBlank()) return
        viewModelScope.launch {
            when (val result = bookRepository.deleteBookComment(currentBook.id, commentId)) {
                is AppResult.Success -> {
                    val remaining = _state.value.comments.filterNot { it.id == commentId }
                    _state.value = _state.value.copy(
                        comments = remaining,
                        commentsCount = remaining.size
                    )
                }
                is AppResult.Error -> {
                    _state.value = _state.value.copy(commentError = result.error.message)
                }
            }
        }
    }

    fun clearCommentError() {
        _state.value = _state.value.copy(commentError = null)
    }

    fun clear() {
        chaptersJob?.cancel()
        progressJob?.cancel()
        commentsJob?.cancel()
        loadedBookId = ""
        _state.value = BookDetailState()
    }
}
