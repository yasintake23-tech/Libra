package com.libra.app.domain.repository

import com.libra.app.core.result.AppResult
import com.libra.app.domain.model.Book
import com.libra.app.domain.model.BookCategory
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
}
