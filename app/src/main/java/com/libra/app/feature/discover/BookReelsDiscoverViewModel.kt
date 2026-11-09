package com.libra.app.feature.discover

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.libra.app.core.di.ServiceLocator
import com.libra.app.core.result.AppResult
import com.libra.app.domain.model.Book
import com.libra.app.domain.model.BookComment
import com.libra.app.domain.model.BookStatus
import com.libra.app.domain.model.ShelfType
import com.libra.app.domain.repository.AuthRepository
import com.libra.app.domain.repository.BookRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

data class BookEngagementUi(
    val isLiked: Boolean = false,
    val likesCount: Int = 0,
    val isSaved: Boolean = false,
    val commentsCount: Int = 0
)

data class BookReelsState(
    val books: List<Book> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val engagements: Map<String, BookEngagementUi> = emptyMap(),
    val chapterPreviews: Map<String, String> = emptyMap(),
    val activeCommentsBookId: String? = null,
    val comments: List<BookComment> = emptyList(),
    val isLoadingComments: Boolean = false
)

class BookReelsDiscoverViewModel(
    private val bookRepository: BookRepository = ServiceLocator.bookRepository,
    private val authRepository: AuthRepository = ServiceLocator.authRepository
) : ViewModel() {

    private val _state = MutableStateFlow(BookReelsState())
    val state: StateFlow<BookReelsState> = _state.asStateFlow()

    init {
        loadDiscoverBooks()
    }

    fun loadDiscoverBooks() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, errorMessage = null)
            val featuredResult = bookRepository.getFeaturedBooks().first()
            val recentResult = bookRepository.getRecentBooks().first()

            val featuredList = if (featuredResult is AppResult.Success) featuredResult.data else emptyList()
            val recentList = if (recentResult is AppResult.Success) recentResult.data else emptyList()

            val combined = (featuredList + recentList).distinctBy { it.id }
            val publishedBooks = combined.filter { it.status == BookStatus.PUBLISHED }
            val finalBooks = if (publishedBooks.isNotEmpty()) publishedBooks else combined

            val initialEngagements = mutableMapOf<String, BookEngagementUi>()
            finalBooks.forEach { b ->
                initialEngagements[b.id] = BookEngagementUi(
                    isLiked = false,
                    likesCount = b.likesCount,
                    isSaved = false,
                    commentsCount = 0
                )
            }

            _state.value = _state.value.copy(
                isLoading = false,
                books = finalBooks,
                engagements = initialEngagements
            )

            // Asynchronously load real user engagements and chapter 1 teasers
            val userId = authRepository.currentUser.value?.uid.orEmpty()
            finalBooks.forEach { book ->
                launch {
                    loadChapterPreview(book.id)
                }
                if (userId.isNotBlank()) {
                    launch {
                        loadEngagement(book.id, userId, book.likesCount)
                    }
                }
            }
        }
    }

    private suspend fun loadEngagement(bookId: String, userId: String, fallbackLikes: Int) {
        when (val result = bookRepository.getBookEngagement(bookId, userId)) {
            is AppResult.Success -> {
                val data = result.data
                val updated = _state.value.engagements.toMutableMap()
                val current = updated[bookId] ?: BookEngagementUi(likesCount = fallbackLikes)
                updated[bookId] = current.copy(
                    isLiked = data.liked,
                    likesCount = fallbackLikes,
                    isSaved = data.saved
                )
                _state.value = _state.value.copy(engagements = updated)
            }
            is AppResult.Error -> Unit
        }
    }

    private suspend fun loadChapterPreview(bookId: String) {
        when (val chaptersResult = bookRepository.getBookChapters(bookId).first()) {
            is AppResult.Success -> {
                val activeChapters = chaptersResult.data.filter { !it.isArchived }.sortedBy { it.chapterNumber }
                val firstChapter = activeChapters.firstOrNull()
                if (firstChapter != null && firstChapter.content.isNotBlank()) {
                    val cleanSnippet = firstChapter.content
                        .trim()
                        .replace(Regex("\\s+"), " ")
                        .take(220)
                    val updatedPreviews = _state.value.chapterPreviews.toMutableMap()
                    updatedPreviews[bookId] = cleanSnippet
                    _state.value = _state.value.copy(chapterPreviews = updatedPreviews)
                }
            }
            is AppResult.Error -> Unit
        }
    }

    fun toggleLike(book: Book) {
        val userId = authRepository.currentUser.value?.uid.orEmpty()
        if (userId.isBlank()) return

        val current = _state.value.engagements[book.id] ?: BookEngagementUi(likesCount = book.likesCount)
        val newLiked = !current.isLiked
        val newCount = (current.likesCount + if (newLiked) 1 else -1).coerceAtLeast(0)

        val updatedMap = _state.value.engagements.toMutableMap()
        updatedMap[book.id] = current.copy(isLiked = newLiked, likesCount = newCount)
        _state.value = _state.value.copy(engagements = updatedMap)

        viewModelScope.launch {
            when (val res = bookRepository.toggleBookLike(book.id, userId)) {
                is AppResult.Success -> {
                    val verifiedMap = _state.value.engagements.toMutableMap()
                    val verified = verifiedMap[book.id] ?: current
                    verifiedMap[book.id] = verified.copy(isLiked = res.data)
                    _state.value = _state.value.copy(engagements = verifiedMap)
                }
                is AppResult.Error -> {
                    // Revert on failure
                    val rollback = _state.value.engagements.toMutableMap()
                    rollback[book.id] = current
                    _state.value = _state.value.copy(engagements = rollback)
                }
            }
        }
    }

    fun toggleSave(book: Book) {
        val userId = authRepository.currentUser.value?.uid.orEmpty()
        if (userId.isBlank()) return

        val current = _state.value.engagements[book.id] ?: BookEngagementUi()
        val newSaved = !current.isSaved

        val updatedMap = _state.value.engagements.toMutableMap()
        updatedMap[book.id] = current.copy(isSaved = newSaved)
        _state.value = _state.value.copy(engagements = updatedMap)

        viewModelScope.launch {
            when (val res = bookRepository.toggleBookSave(book.id, userId)) {
                is AppResult.Success -> {
                    val isNowSaved = res.data
                    val verifiedMap = _state.value.engagements.toMutableMap()
                    val verified = verifiedMap[book.id] ?: current
                    verifiedMap[book.id] = verified.copy(isSaved = isNowSaved)
                    _state.value = _state.value.copy(engagements = verifiedMap)

                    if (isNowSaved) {
                        bookRepository.addBookToShelf(userId, book.id, ShelfType.SAVED)
                    } else {
                        bookRepository.removeBookFromShelf(userId, book.id, ShelfType.SAVED)
                    }
                }
                is AppResult.Error -> {
                    val rollback = _state.value.engagements.toMutableMap()
                    rollback[book.id] = current
                    _state.value = _state.value.copy(engagements = rollback)
                }
            }
        }
    }

    fun startReading(book: Book) {
        val userId = authRepository.currentUser.value?.uid.orEmpty()
        if (userId.isNotBlank()) {
            viewModelScope.launch {
                bookRepository.addBookToShelf(userId, book.id, ShelfType.READING)
            }
        }
    }

    fun openComments(bookId: String) {
        _state.value = _state.value.copy(
            activeCommentsBookId = bookId,
            isLoadingComments = true,
            comments = emptyList()
        )
        viewModelScope.launch {
            when (val result = bookRepository.getBookComments(bookId).first()) {
                is AppResult.Success -> {
                    _state.value = _state.value.copy(
                        isLoadingComments = false,
                        comments = result.data
                    )
                }
                is AppResult.Error -> {
                    _state.value = _state.value.copy(isLoadingComments = false)
                }
            }
        }
    }

    fun closeComments() {
        _state.value = _state.value.copy(activeCommentsBookId = null, comments = emptyList())
    }

    fun addComment(bookId: String, text: String) {
        val user = authRepository.currentUser.value ?: return
        if (text.trim().isBlank()) return

        val newComment = BookComment(
            id = "c-${System.currentTimeMillis()}",
            bookId = bookId,
            userId = user.uid,
            authorName = user.displayName.ifBlank { "Libra Okuru" },
            authorUsername = user.username,
            authorPhotoUrl = user.profileImageUrl,
            text = text.trim(),
            createdAt = System.currentTimeMillis()
        )

        // Optimistically append comment
        val updatedComments = listOf(newComment) + _state.value.comments
        val updatedEngagements = _state.value.engagements.toMutableMap()
        val curEng = updatedEngagements[bookId] ?: BookEngagementUi()
        updatedEngagements[bookId] = curEng.copy(commentsCount = curEng.commentsCount + 1)

        _state.value = _state.value.copy(
            comments = updatedComments,
            engagements = updatedEngagements
        )

        viewModelScope.launch {
            bookRepository.addBookComment(newComment)
        }
    }
}
