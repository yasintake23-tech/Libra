package com.libra.app.data.library

import com.libra.app.core.result.AppResult
import com.libra.app.domain.model.Book
import com.libra.app.domain.model.ShelfType
import com.libra.app.domain.model.UserShelfItem
import com.libra.app.domain.repository.BookRepository
import com.libra.app.domain.repository.LibraryRepository
import kotlinx.coroutines.flow.Flow

/**
 * Concrete implementation of [LibraryRepository] delegating to [BookRepository].
 */
class LibraryRepositoryImpl(
    private val bookRepository: BookRepository
) : LibraryRepository {

    override fun getUserLibrary(userId: String, shelfType: ShelfType): Flow<AppResult<List<UserShelfItem>>> =
        bookRepository.getUserLibrary(userId, shelfType)

    override fun getUserWrittenBooks(userId: String): Flow<AppResult<List<Book>>> =
        bookRepository.getUserWrittenBooks(userId)

    override suspend fun addBookToShelf(userId: String, bookId: String, shelfType: ShelfType): AppResult<Unit> =
        bookRepository.addBookToShelf(userId, bookId, shelfType)

    override suspend fun removeBookFromShelf(userId: String, bookId: String, shelfType: ShelfType): AppResult<Unit> =
        bookRepository.removeBookFromShelf(userId, bookId, shelfType)
}
