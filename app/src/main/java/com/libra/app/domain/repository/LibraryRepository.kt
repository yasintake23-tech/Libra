package com.libra.app.domain.repository

import com.libra.app.core.result.AppResult
import com.libra.app.domain.model.Book
import com.libra.app.domain.model.ShelfType
import com.libra.app.domain.model.UserShelfItem
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface dedicated to library shelves, reading lists, bookmarks, and user writings.
 */
interface LibraryRepository {
    fun getUserLibrary(userId: String, shelfType: ShelfType): Flow<AppResult<List<UserShelfItem>>>
    fun getUserWrittenBooks(userId: String): Flow<AppResult<List<Book>>>
    suspend fun addBookToShelf(userId: String, bookId: String, shelfType: ShelfType): AppResult<Unit>
    suspend fun removeBookFromShelf(userId: String, bookId: String, shelfType: ShelfType): AppResult<Unit>
}
