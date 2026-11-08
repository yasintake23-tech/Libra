package com.libra.app

import com.libra.app.core.result.AppError
import com.libra.app.core.result.AppResult
import com.libra.app.core.state.UiState
import com.libra.app.domain.model.Book
import com.libra.app.domain.model.BookCategory
import com.libra.app.domain.model.BookStatus
import com.libra.app.domain.model.ShelfType
import com.libra.app.domain.model.UserProfile
import com.libra.app.domain.model.UserShelfItem
import com.libra.app.domain.repository.AuthRepository
import com.libra.app.domain.repository.LibraryRepository
import com.libra.app.feature.library.LibraryState
import com.libra.app.feature.library.LibraryViewModel
import com.libra.app.feature.library.ShelfState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class LibraryModuleTest {

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun userShelfItem_defaultsAndProperties_preserved() {
        val book = Book(
            id = "book_1",
            title = "Kürk Mantolu Madonna",
            authorName = "Sabahattin Ali",
            status = BookStatus.PUBLISHED
        )

        val item = UserShelfItem(
            id = "u1_READING_book_1",
            userId = "u1",
            book = book,
            shelfType = ShelfType.READING,
            progressPercent = 65,
            addedAt = 1000L,
            updatedAt = 2000L
        )

        assertEquals("u1_READING_book_1", item.id)
        assertEquals("u1", item.userId)
        assertEquals("book_1", item.book.id)
        assertEquals(ShelfType.READING, item.shelfType)
        assertEquals(65, item.progressPercent)
        assertEquals(1000L, item.addedAt)
        assertEquals(2000L, item.updatedAt)
    }

    @Test
    fun libraryState_itemsAndSearchFiltering_workCorrectly() {
        val book1 = Book(id = "b1", title = "Nutuk", authorName = "Mustafa Kemal Atatürk")
        val book2 = Book(id = "b2", title = "Saatleri Ayarlama Enstitüsü", authorName = "Ahmet Hamdi Tanpınar")
        val item1 = UserShelfItem(id = "1", book = book1, shelfType = ShelfType.READING)
        val item2 = UserShelfItem(id = "2", book = book2, shelfType = ShelfType.READING)

        val state = LibraryState(
            selectedShelf = ShelfType.READING,
            shelves = mapOf(
                ShelfType.READING to ShelfState(items = listOf(item1, item2), isLoading = false),
                ShelfType.SAVED to ShelfState(items = emptyList(), isLoading = false),
                ShelfType.MY_WRITINGS to ShelfState(items = emptyList(), isLoading = false)
            )
        )

        assertEquals(2, state.items.size)
        assertEquals("Nutuk", state.items[0].book.title)

        // Filter by title
        val titleFiltered = state.items.filter { it.book.title.contains("Nutuk", ignoreCase = true) }
        assertEquals(1, titleFiltered.size)
        assertEquals("b1", titleFiltered[0].book.id)

        // Filter by author
        val authorFiltered = state.items.filter { it.book.authorName.contains("Tanpınar", ignoreCase = true) }
        assertEquals(1, authorFiltered.size)
        assertEquals("b2", authorFiltered[0].book.id)

        // Filter with empty query matches all
        val allFiltered = state.items.filter { "" in it.book.title }
        assertEquals(2, allFiltered.size)
    }

    @Test
    fun fakeLibraryRepository_addingToSaved_doesNotRemoveReading() = runTest {
        val fakeRepo = FakeLibraryRepository()
        val userId = "user_abc"
        val bookId = "book_xyz"

        // 1. Add book to Reading
        val addReadingResult = fakeRepo.addBookToShelf(userId, bookId, ShelfType.READING)
        assertTrue(addReadingResult is AppResult.Success)

        // 2. Add same book to Saved
        val addSavedResult = fakeRepo.addBookToShelf(userId, bookId, ShelfType.SAVED)
        assertTrue(addSavedResult is AppResult.Success)

        // Verify book is present on BOTH shelves simultaneously
        val readingItems = fakeRepo.getUserLibraryDirect(userId, ShelfType.READING)
        val savedItems = fakeRepo.getUserLibraryDirect(userId, ShelfType.SAVED)

        assertEquals(1, readingItems.size)
        assertEquals(bookId, readingItems[0].book.id)
        assertEquals(1, savedItems.size)
        assertEquals(bookId, savedItems[0].book.id)

        // 3. Remove book from Saved
        val removeSavedResult = fakeRepo.removeBookFromShelf(userId, bookId, ShelfType.SAVED)
        assertTrue(removeSavedResult is AppResult.Success)

        // Verify it was removed from Saved but STILL exists in Reading
        val savedAfterRemove = fakeRepo.getUserLibraryDirect(userId, ShelfType.SAVED)
        val readingAfterRemove = fakeRepo.getUserLibraryDirect(userId, ShelfType.READING)

        assertEquals(0, savedAfterRemove.size)
        assertEquals(1, readingAfterRemove.size)
        assertEquals(bookId, readingAfterRemove[0].book.id)
    }

    @Test
    fun libraryViewModel_authAndShelfObservation_updatesStateIndependently() = runTest {
        val fakeAuthRepo = FakeAuthRepository()
        val fakeLibraryRepo = FakeLibraryRepository()

        val bookReading = Book(id = "r1", title = "Yaban", authorName = "Yakup Kadri Karaosmanoğlu", status = BookStatus.PUBLISHED)
        val bookSaved = Book(id = "s1", title = "Çalıkuşu", authorName = "Reşat Nuri Güntekin", status = BookStatus.PUBLISHED)
        val bookWritten = Book(id = "w1", title = "Kendi Hikayem", authorName = "Ben", status = BookStatus.PUBLISHED)

        fakeLibraryRepo.seedBook(bookReading)
        fakeLibraryRepo.seedBook(bookSaved)
        fakeLibraryRepo.seedBook(bookWritten)

        val user = UserProfile(uid = "test_user_1", username = "tester")
        fakeAuthRepo.setUser(user)

        fakeLibraryRepo.addBookToShelf(user.uid, bookReading.id, ShelfType.READING)
        fakeLibraryRepo.addBookToShelf(user.uid, bookSaved.id, ShelfType.SAVED)
        fakeLibraryRepo.addWrittenBook(user.uid, bookWritten)

        val viewModel = LibraryViewModel(
            authRepository = fakeAuthRepo,
            libraryRepository = fakeLibraryRepo
        )

        advanceUntilIdle()

        val uiState = viewModel.uiState.value
        assertTrue("Expected UiState.Success but got $uiState", uiState is UiState.Success)

        val data = (uiState as UiState.Success).data
        assertEquals(1, data.shelves[ShelfType.READING]?.items?.size)
        assertEquals("Yaban", data.shelves[ShelfType.READING]?.items?.first()?.book?.title)
        assertFalse(data.shelves[ShelfType.READING]?.isLoading ?: true)

        assertEquals(1, data.shelves[ShelfType.SAVED]?.items?.size)
        assertEquals("Çalıkuşu", data.shelves[ShelfType.SAVED]?.items?.first()?.book?.title)
        assertFalse(data.shelves[ShelfType.SAVED]?.isLoading ?: true)

        assertEquals(1, data.shelves[ShelfType.MY_WRITINGS]?.items?.size)
        assertEquals("Kendi Hikayem", data.shelves[ShelfType.MY_WRITINGS]?.items?.first()?.book?.title)
        assertFalse(data.shelves[ShelfType.MY_WRITINGS]?.isLoading ?: true)

        // Test smooth tab switching without loading freeze
        viewModel.selectShelf(ShelfType.SAVED)
        val savedTabState = (viewModel.uiState.value as UiState.Success).data
        assertEquals(ShelfType.SAVED, savedTabState.selectedShelf)
        assertEquals("Çalıkuşu", savedTabState.items.first().book.title)
        assertFalse(savedTabState.isCurrentLoading)

        // Test optimistic removal
        viewModel.removeFromShelf(bookSaved, ShelfType.SAVED)
        advanceUntilIdle()

        val stateAfterRemoval = (viewModel.uiState.value as UiState.Success).data
        assertEquals(0, stateAfterRemoval.shelves[ShelfType.SAVED]?.items?.size)
        // Reading shelf is still 1
        assertEquals(1, stateAfterRemoval.shelves[ShelfType.READING]?.items?.size)
    }

    @Test
    fun libraryViewModel_searchQuery_updatesStateCorrectly() = runTest {
        val fakeAuthRepo = FakeAuthRepository()
        val fakeLibraryRepo = FakeLibraryRepository()
        fakeAuthRepo.setUser(UserProfile(uid = "u1"))

        val viewModel = LibraryViewModel(fakeAuthRepo, fakeLibraryRepo)
        advanceUntilIdle()

        viewModel.updateSearchQuery("Dostoyevski")
        val state = (viewModel.uiState.value as UiState.Success).data
        assertEquals("Dostoyevski", state.searchQuery)
    }

    private class FakeAuthRepository : AuthRepository {
        private val _currentUser = MutableStateFlow<UserProfile?>(null)
        override val currentUser: StateFlow<UserProfile?> = _currentUser.asStateFlow()
        private val _isAuthenticated = MutableStateFlow(false)
        override val isAuthenticated: StateFlow<Boolean> = _isAuthenticated.asStateFlow()

        fun setUser(profile: UserProfile?) {
            _currentUser.value = profile
            _isAuthenticated.value = profile != null
        }

        override suspend fun signInWithEmail(email: String, pass: String): AppResult<UserProfile> =
            AppResult.Error(AppError.Auth("Not implemented"))
        override suspend fun registerWithEmail(email: String, pass: String, user: String, name: String): AppResult<UserProfile> =
            AppResult.Error(AppError.Auth("Not implemented"))
        override suspend fun signInWithGoogle(idToken: String): AppResult<UserProfile> =
            AppResult.Error(AppError.Auth("Not implemented"))
        override suspend fun signOut(): AppResult<Unit> {
            setUser(null)
            return AppResult.Success(Unit)
        }
        override suspend fun sendPasswordResetEmail(email: String): AppResult<Unit> =
            AppResult.Success(Unit)
        override suspend fun deleteAccount(): AppResult<Unit> =
            AppResult.Success(Unit)
    }

    private class FakeLibraryRepository : LibraryRepository {
        private val books = mutableMapOf<String, Book>()
        private val shelves = mutableMapOf<String, MutableMap<ShelfType, MutableMap<String, UserShelfItem>>>()
        private val writtenBooks = mutableMapOf<String, MutableList<Book>>()

        fun seedBook(book: Book) {
            books[book.id] = book
        }

        fun addWrittenBook(userId: String, book: Book) {
            writtenBooks.getOrPut(userId) { mutableListOf() }.add(book)
        }

        fun getUserLibraryDirect(userId: String, shelfType: ShelfType): List<UserShelfItem> {
            return shelves[userId]?.get(shelfType)?.values?.toList() ?: emptyList()
        }

        override fun getUserLibrary(userId: String, shelfType: ShelfType): Flow<AppResult<List<UserShelfItem>>> {
            val list = shelves[userId]?.get(shelfType)?.values?.toList() ?: emptyList()
            return flowOf(AppResult.Success(list))
        }

        override fun getUserWrittenBooks(userId: String): Flow<AppResult<List<Book>>> {
            val list = writtenBooks[userId]?.toList() ?: emptyList()
            return flowOf(AppResult.Success(list))
        }

        override suspend fun addBookToShelf(userId: String, bookId: String, shelfType: ShelfType): AppResult<Unit> {
            val userShelves = shelves.getOrPut(userId) { mutableMapOf() }
            val shelf = userShelves.getOrPut(shelfType) { mutableMapOf() }
            val book = books[bookId] ?: Book(id = bookId, title = "Book $bookId", status = BookStatus.PUBLISHED)
            shelf[bookId] = UserShelfItem(
                id = "${userId}_${shelfType.name}_$bookId",
                userId = userId,
                book = book,
                shelfType = shelfType,
                progressPercent = if (shelfType == ShelfType.READING) 10 else 0,
                addedAt = System.currentTimeMillis()
            )
            return AppResult.Success(Unit)
        }

        override suspend fun removeBookFromShelf(userId: String, bookId: String, shelfType: ShelfType): AppResult<Unit> {
            shelves[userId]?.get(shelfType)?.remove(bookId)
            return AppResult.Success(Unit)
        }
    }
}
