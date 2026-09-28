package com.libra.app.domain.repository

import com.libra.app.core.result.AppResult
import com.libra.app.core.result.AppError
import com.libra.app.domain.model.Book
import com.libra.app.domain.model.BookCategory
import com.libra.app.domain.model.BookComment
import com.libra.app.domain.model.BookEngagement
import com.libra.app.domain.model.ReadingProgress
import com.libra.app.domain.model.Chapter
import com.libra.app.domain.model.ShelfType
import com.libra.app.domain.model.UserShelfItem
import kotlinx.coroutines.flow.Flow

/**
 * Contract for Book discovery, user library shelves, drafts, and chapter operations.
 */
interface BookRepository {
    fun getFeaturedBooks(): Flow<AppResult<List<Book>>>
    fun getRecentBooks(): Flow<AppResult<List<Book>>>
    fun getBooksByCategory(category: BookCategory): Flow<AppResult<List<Book>>>
    fun getBookById(bookId: String): Flow<AppResult<Book?>>
    fun getUserLibrary(userId: String, shelfType: ShelfType): Flow<AppResult<List<UserShelfItem>>>
    fun getUserWrittenBooks(userId: String): Flow<AppResult<List<Book>>>
    
    suspend fun createBook(book: Book): AppResult<Book>
    suspend fun updateBook(book: Book): AppResult<Book>
    suspend fun deleteBook(bookId: String): AppResult<Unit>
    suspend fun addBookToShelf(userId: String, bookId: String, shelfType: ShelfType): AppResult<Unit>

    // Chapters
    fun getBookChapters(bookId: String): Flow<AppResult<List<Chapter>>>
    suspend fun saveChapter(chapter: Chapter): AppResult<Chapter>

    // Book social and reader state. Implemented by the data layer in the next book-system phase.
    suspend fun getBookEngagement(bookId: String, userId: String): AppResult<BookEngagement> =
        AppResult.Error(AppError.Database("Kitap etkileşim sistemi henüz bağlanmadı."))
    suspend fun toggleBookLike(bookId: String, userId: String): AppResult<Boolean> =
        AppResult.Error(AppError.Database("Kitap beğeni sistemi henüz bağlanmadı."))
    suspend fun toggleBookSave(bookId: String, userId: String): AppResult<Boolean> =
        AppResult.Error(AppError.Database("Kitap kaydetme sistemi henüz bağlanmadı."))
    fun getBookComments(bookId: String): Flow<AppResult<List<BookComment>>> =
        kotlinx.coroutines.flow.flowOf(AppResult.Success(emptyList()))
    suspend fun addBookComment(comment: BookComment): AppResult<BookComment> =
        AppResult.Error(AppError.Database("Kitap yorum sistemi henüz bağlanmadı."))
    suspend fun deleteBookComment(bookId: String, commentId: String): AppResult<Unit> =
        AppResult.Error(AppError.Database("Kitap yorum sistemi henüz bağlanmadı."))
    suspend fun saveReadingProgress(progress: ReadingProgress): AppResult<ReadingProgress> =
        AppResult.Error(AppError.Database("Okuma ilerleme sistemi henüz bağlanmadı."))
    fun getReadingProgress(userId: String, bookId: String): Flow<AppResult<ReadingProgress?>> =
        kotlinx.coroutines.flow.flowOf(AppResult.Success(null))
}
