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

const val CHAPTER_PREVIEW_MIN_CHARS = 1200
const val CHAPTER_PREVIEW_MAX_CHARS = 1800

/**
 * Formats a chapter teaser snippet:
 * - Targets between [minChars] and [maxChars] characters (default 1,200 - 1,800).
 * - If text <= [maxChars], returns full text with hasMore = false.
 * - Preserves paragraph formatting (\n\n) without flattening lines.
 * - Cuts cleanly at sentence boundaries (. , .\n, !, ?, etc.) within the target window,
 *   or at word boundaries before [maxChars], avoiding awkward truncated words.
 * - Returns a Pair of (formattedSnippet, hasMore).
 */
fun formatChapterTeaserSnippet(
    rawContent: String,
    minChars: Int = CHAPTER_PREVIEW_MIN_CHARS,
    maxChars: Int = CHAPTER_PREVIEW_MAX_CHARS
): Pair<String, Boolean> {
    val trimmed = rawContent.trim()
    if (trimmed.isEmpty()) {
        return Pair("", false)
    }
    if (trimmed.length <= maxChars) {
        return Pair(trimmed, false)
    }

    val candidate = trimmed.substring(0, maxChars)

    // Look for sentence delimiters in range [minChars, maxChars]
    val sentenceDelimiters = listOf(". ", ".\n", "!\n", "! ", "?\n", "? ", "\n\n")
    var bestCut = -1

    for (delim in sentenceDelimiters) {
        val lastIdx = candidate.lastIndexOf(delim)
        if (lastIdx in minChars..maxChars) {
            val cutPoint = lastIdx + 1
            if (cutPoint > bestCut) {
                bestCut = cutPoint
            }
        }
    }

    // If no sentence ender found within [minChars, maxChars], check from minChars / 2 up to maxChars
    if (bestCut == -1) {
        for (delim in sentenceDelimiters) {
            val lastIdx = candidate.lastIndexOf(delim)
            if (lastIdx >= (minChars / 2) && (lastIdx + 1) > bestCut) {
                bestCut = lastIdx + 1
            }
        }
    }

    // If still no sentence boundary found, cut at the last word boundary (space or newline) before maxChars
    if (bestCut == -1) {
        val lastSpace = candidate.lastIndexOf(' ')
        val lastNewline = candidate.lastIndexOf('\n')
        val wordBreak = maxOf(lastSpace, lastNewline)
        bestCut = if (wordBreak >= (minChars / 2)) wordBreak else maxChars
    }

    val cutSnippet = candidate.substring(0, bestCut).trimEnd()
    return Pair(cutSnippet, true)
}

const val BOOK_PAGE_SIZE = 5

data class BookReelsState(
    val books: List<Book> = emptyList(),
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val isLoadingMore: Boolean = false,
    val hasMoreBooks: Boolean = false,
    val errorMessage: String? = null,
    val engagements: Map<String, BookEngagementUi> = emptyMap(),
    val chapterPreviews: Map<String, String> = emptyMap(),
    val chapterPreviewHasMore: Map<String, Boolean> = emptyMap(),
    val firstChapterIds: Map<String, String> = emptyMap(),
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

    private var allBooksPool: List<Book> = emptyList()
    private var rawBooksCache: List<Book> = emptyList()

    init {
        loadDiscoverBooks()
    }

    fun loadDiscoverBooks(isRefresh: Boolean = false) {
        viewModelScope.launch {
            if (isRefresh) {
                _state.value = _state.value.copy(isRefreshing = true, errorMessage = null)
            } else if (_state.value.books.isEmpty()) {
                _state.value = _state.value.copy(isLoading = true, errorMessage = null)
            }

            val featuredResult = bookRepository.getFeaturedBooks().first()
            val recentResult = bookRepository.getRecentBooks().first()

            val featuredList = if (featuredResult is AppResult.Success) featuredResult.data else emptyList()
            val recentList = if (recentResult is AppResult.Success) recentResult.data else emptyList()

            val combined = (featuredList + recentList).distinctBy { it.id }
            val publishedBooks = combined.filter { it.status == BookStatus.PUBLISHED }
            val finalBooks = if (publishedBooks.isNotEmpty()) publishedBooks else combined
            rawBooksCache = finalBooks

            // Shuffle randomly on initial load and on every refresh!
            allBooksPool = finalBooks.shuffled()

            val initialBooks = allBooksPool.take(BOOK_PAGE_SIZE)
            val hasMore = allBooksPool.size > BOOK_PAGE_SIZE

            val initialEngagements = _state.value.engagements.toMutableMap()
            initialBooks.forEach { b ->
                if (!initialEngagements.containsKey(b.id)) {
                    initialEngagements[b.id] = BookEngagementUi(
                        isLiked = false,
                        likesCount = b.likesCount,
                        isSaved = false,
                        commentsCount = 0
                    )
                }
            }

            if (isRefresh) {
                kotlinx.coroutines.delay(300)
            }

            _state.value = _state.value.copy(
                isLoading = false,
                isRefreshing = false,
                books = initialBooks,
                hasMoreBooks = hasMore,
                isLoadingMore = false,
                engagements = initialEngagements
            )

            // Asynchronously load real user engagements and chapter 1 teasers for the visible batch
            loadBatchMetadata(initialBooks)
        }
    }

    fun refreshBooks() {
        loadDiscoverBooks(isRefresh = true)
    }

    fun loadMoreBooks() {
        val currentCount = _state.value.books.size
        if (_state.value.isLoadingMore || currentCount >= allBooksPool.size) return

        viewModelScope.launch {
            _state.value = _state.value.copy(isLoadingMore = true)
            kotlinx.coroutines.delay(200)

            val nextBatch = allBooksPool.drop(currentCount).take(BOOK_PAGE_SIZE)
            val updatedBooks = _state.value.books + nextBatch
            val hasMore = updatedBooks.size < allBooksPool.size

            val updatedEngagements = _state.value.engagements.toMutableMap()
            nextBatch.forEach { b ->
                if (!updatedEngagements.containsKey(b.id)) {
                    updatedEngagements[b.id] = BookEngagementUi(
                        isLiked = false,
                        likesCount = b.likesCount,
                        isSaved = false,
                        commentsCount = 0
                    )
                }
            }

            _state.value = _state.value.copy(
                books = updatedBooks,
                hasMoreBooks = hasMore,
                isLoadingMore = false,
                engagements = updatedEngagements
            )

            loadBatchMetadata(nextBatch)
        }
    }

    private fun loadBatchMetadata(books: List<Book>) {
        val userId = authRepository.currentUser.value?.uid.orEmpty()
        books.forEach { book ->
            viewModelScope.launch {
                loadChapterPreview(book.id)
            }
            if (userId.isNotBlank()) {
                viewModelScope.launch {
                    loadEngagement(book.id, userId, book.likesCount)
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
                // Only published, active (non-archived) chapters with readable text
                val publishedChapters = chaptersResult.data
                    .filter { it.isPublished && !it.isArchived && it.content.isNotBlank() }
                    .sortedBy { it.chapterNumber }
                val firstChapter = publishedChapters.firstOrNull()

                if (firstChapter != null) {
                    val (snippet, hasMore) = formatChapterTeaserSnippet(firstChapter.content)
                    if (snippet.isNotBlank()) {
                        val updatedPreviews = _state.value.chapterPreviews.toMutableMap()
                        val updatedHasMore = _state.value.chapterPreviewHasMore.toMutableMap()
                        val updatedFirstChapterIds = _state.value.firstChapterIds.toMutableMap()

                        updatedPreviews[bookId] = snippet
                        updatedHasMore[bookId] = hasMore
                        updatedFirstChapterIds[bookId] = firstChapter.id

                        _state.value = _state.value.copy(
                            chapterPreviews = updatedPreviews,
                            chapterPreviewHasMore = updatedHasMore,
                            firstChapterIds = updatedFirstChapterIds
                        )
                    }
                } else {
                    // If no published chapter exists, clear teaser so no empty container is displayed
                    if (_state.value.chapterPreviews.containsKey(bookId)) {
                        val updatedPreviews = _state.value.chapterPreviews.toMutableMap()
                        val updatedHasMore = _state.value.chapterPreviewHasMore.toMutableMap()
                        val updatedFirstChapterIds = _state.value.firstChapterIds.toMutableMap()

                        updatedPreviews.remove(bookId)
                        updatedHasMore.remove(bookId)
                        updatedFirstChapterIds.remove(bookId)

                        _state.value = _state.value.copy(
                            chapterPreviews = updatedPreviews,
                            chapterPreviewHasMore = updatedHasMore,
                            firstChapterIds = updatedFirstChapterIds
                        )
                    }
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
