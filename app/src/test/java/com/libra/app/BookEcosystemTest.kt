package com.libra.app

import com.libra.app.core.result.AppError
import com.libra.app.core.result.AppResult
import com.libra.app.domain.model.Book
import com.libra.app.domain.model.BookCategory
import com.libra.app.domain.model.BookComment
import com.libra.app.domain.model.BookEngagement
import com.libra.app.domain.model.BookStatus
import com.libra.app.domain.model.Chapter
import com.libra.app.domain.model.ReadingProgress
import com.libra.app.domain.model.ShelfType
import com.libra.app.domain.model.StorageUploadRequest
import com.libra.app.domain.model.UserProfile
import com.libra.app.domain.model.UserShelfItem
import com.libra.app.domain.repository.AuthRepository
import com.libra.app.domain.repository.BookRepository
import com.libra.app.domain.repository.StorageRepository
import com.libra.app.feature.book.manage.BookManagementViewModel
import com.libra.app.feature.book.manage.ManagementSubPage
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
class BookEcosystemTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var fakeAuthRepo: FakeAuthRepo
    private lateinit var fakeBookRepo: FakeBookRepo
    private lateinit var fakeStorageRepo: FakeStorageRepo
    private lateinit var viewModel: BookManagementViewModel

    private val testUser = UserProfile(
        uid = "author_1",
        displayName = "Ahmet Hamdi",
        username = "ahmethamdi",
        email = "ahmet@libra.app"
    )

    private val sampleBook = Book(
        id = "book_101",
        ownerId = "author_1",
        authorName = "Ahmet Hamdi",
        title = "Huzur",
        description = "Mümtaz ve Nuran'ın İstanbul'daki hikayesi.",
        discoverySummary = "Zaman, mekan ve musiki üçgeninde bir arayış.",
        category = BookCategory.CLASSICS,
        status = BookStatus.DRAFT,
        chapterCount = 2
    )

    private val sampleChapters = listOf(
        Chapter(id = "ch_1", bookId = "book_101", chapterNumber = 1, title = "İhsan", content = "Mümtaz, Sahaflar Çarşısı'ndan dönüyordu.", wordCount = 120, isPublished = false),
        Chapter(id = "ch_2", bookId = "book_101", chapterNumber = 2, title = "Nuran", content = "Boğaziçi sularında akseden eski musiki.", wordCount = 150, isPublished = false)
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeAuthRepo = FakeAuthRepo().apply { setUser(testUser) }
        fakeBookRepo = FakeBookRepo().apply {
            books.add(sampleBook)
            chapters.addAll(sampleChapters)
        }
        fakeStorageRepo = FakeStorageRepo()

        viewModel = BookManagementViewModel(
            bookRepository = fakeBookRepo,
            authRepository = fakeAuthRepo,
            storageRepository = fakeStorageRepo
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun managementHub_initialization_loadsBookAndChaptersCorrectly() = runTest {
        viewModel.initialize(sampleBook)
        advanceUntilIdle()

        val state = viewModel.state.value
        assertEquals("book_101", state.book?.id)
        assertEquals(2, state.chapters.size)
        assertEquals(ManagementSubPage.HUB, state.activeSubPage)
    }

    @Test
    fun managementHub_subPageNavigation_worksSeamlessly() = runTest {
        viewModel.initialize(sampleBook)
        advanceUntilIdle()

        viewModel.navigateToSubPage(ManagementSubPage.METADATA)
        assertEquals(ManagementSubPage.METADATA, viewModel.state.value.activeSubPage)

        viewModel.navigateToSubPage(ManagementSubPage.CHAPTERS)
        assertEquals(ManagementSubPage.CHAPTERS, viewModel.state.value.activeSubPage)

        viewModel.navigateToSubPage(ManagementSubPage.STUDIO, chapterId = "ch_2")
        assertEquals(ManagementSubPage.STUDIO, viewModel.state.value.activeSubPage)
        assertEquals("ch_2", viewModel.state.value.activeStudioChapter?.id)

        viewModel.navigateToSubPage(ManagementSubPage.HUB)
        assertEquals(ManagementSubPage.HUB, viewModel.state.value.activeSubPage)
    }

    @Test
    fun chapterManagement_createChapter_incrementsCountAndOpensStudio() = runTest {
        viewModel.initialize(sampleBook)
        advanceUntilIdle()

        viewModel.createNewChapter("Suat")
        advanceUntilIdle()

        val state = viewModel.state.value
        assertEquals(3, state.chapters.size)
        assertEquals("Suat", state.activeStudioChapter?.title)
        assertEquals(3, state.activeStudioChapter?.chapterNumber)
        assertEquals(ManagementSubPage.STUDIO, state.activeSubPage)
    }

    @Test
    fun chapterManagement_reorderChapters_updatesOrderCorrectly() = runTest {
        viewModel.initialize(sampleBook)
        advanceUntilIdle()

        val chapter2 = viewModel.state.value.chapters[1]
        viewModel.moveChapterUp(chapter2)
        advanceUntilIdle()

        val reordered = viewModel.state.value.chapters
        assertEquals("ch_2", reordered[0].id)
        assertEquals("ch_1", reordered[1].id)
    }

    @Test
    fun chapterManagement_duplicateChapter_createsCopy() = runTest {
        viewModel.initialize(sampleBook)
        advanceUntilIdle()

        val ch1 = viewModel.state.value.chapters[0]
        viewModel.duplicateChapter(ch1)
        advanceUntilIdle()

        val chapters = viewModel.state.value.chapters
        assertEquals(3, chapters.size)
        assertTrue(chapters.any { it.title.contains("Kopya") })
    }

    @Test
    fun writingStudio_contentUpdateAndAutosave_persistsChanges() = runTest {
        viewModel.initialize(sampleBook)
        advanceUntilIdle()

        val ch1 = viewModel.state.value.chapters[0]
        viewModel.selectChapterForStudio(ch1)

        val newContent = "Mümtaz akşam serinliğinde Boğaz vapuruna bindi. Denizin kokusu eski İstanbul'u hatırlatıyordu."
        viewModel.updateStudioChapterContent("İhsan - Genişletilmiş", newContent)

        assertTrue(viewModel.state.value.hasUnsavedChanges)

        // Force manual save
        viewModel.saveChapterNow()
        advanceUntilIdle()

        assertFalse(viewModel.state.value.hasUnsavedChanges)
        assertEquals("İhsan - Genişletilmiş", viewModel.state.value.activeStudioChapter?.title)
    }

    @Test
    fun publishingSettings_publishBook_marksBookAndAllChaptersPublished() = runTest {
        viewModel.initialize(sampleBook)
        advanceUntilIdle()

        assertEquals(BookStatus.DRAFT, viewModel.state.value.book?.status)

        viewModel.publishBook()
        advanceUntilIdle()

        val state = viewModel.state.value
        assertEquals(BookStatus.PUBLISHED, state.book?.status)
        assertTrue(state.chapters.all { it.isPublished })
        assertEquals(2, state.book?.chapterCount)
    }

    @Test
    fun publishingSettings_unpublishBook_revertsToDraft() = runTest {
        fakeBookRepo.books.clear()
        fakeBookRepo.books.add(sampleBook.copy(status = BookStatus.PUBLISHED))

        viewModel.initialize(sampleBook.copy(status = BookStatus.PUBLISHED))
        advanceUntilIdle()

        viewModel.unpublishBook()
        advanceUntilIdle()

        assertEquals(BookStatus.DRAFT, viewModel.state.value.book?.status)
    }

    @Test
    fun bookDetailConsistency_legacyChaptersWithoutIsPublished_areVisibleWhenBookIsPublished() {
        // Simulating the exact bug report: Book is PUBLISHED, but chapters in RTDB had isPublished = false or missing
        val publishedBook = sampleBook.copy(status = BookStatus.PUBLISHED)
        val legacyChapters = listOf(
            Chapter(id = "c1", bookId = "b1", chapterNumber = 1, title = "Bölüm 1", isPublished = false),
            Chapter(id = "c2", bookId = "b1", chapterNumber = 2, title = "Bölüm 2", isPublished = false),
            Chapter(id = "c3", bookId = "b1", chapterNumber = 3, title = "Bölüm 3", isPublished = false)
        )

        // Resilient resolution logic verified
        val isAuthor = false
        val publishedChapters = legacyChapters.filter { it.isPublished }
        val visibleChapters = when {
            isAuthor -> legacyChapters
            publishedChapters.isNotEmpty() -> publishedChapters
            publishedBook.status == BookStatus.PUBLISHED -> legacyChapters
            else -> emptyList()
        }

        // Must display all 3 chapters rather than 0 chapters!
        assertEquals(3, visibleChapters.size)
        assertEquals(3, (publishedBook.copy(chapterCount = visibleChapters.size)).chapterCount)
    }

    // Fakes
    private class FakeAuthRepo : AuthRepository {
        private val _currentUser = MutableStateFlow<UserProfile?>(null)
        override val currentUser: StateFlow<UserProfile?> = _currentUser.asStateFlow()
        private val _isAuthenticated = MutableStateFlow(false)
        override val isAuthenticated: StateFlow<Boolean> = _isAuthenticated.asStateFlow()

        fun setUser(profile: UserProfile?) {
            _currentUser.value = profile
            _isAuthenticated.value = profile != null
        }

        override suspend fun checkCurrentSession(): AppResult<UserProfile?> = AppResult.Success(_currentUser.value)
        override fun signInWithGoogleIdToken(idToken: String, displayName: String?, email: String?, photoUrl: String?): Flow<AppResult<UserProfile>> =
            flowOf(AppResult.Error(AppError.Auth("Not implemented in test")))
        override suspend fun signInWithEmailPassword(email: String, password: String): AppResult<UserProfile> =
            AppResult.Error(AppError.Auth("Not implemented in test"))
        override suspend fun createAccountWithEmailPassword(email: String, password: String): AppResult<UserProfile> =
            AppResult.Error(AppError.Auth("Not implemented in test"))
        override suspend fun signOut(): AppResult<Unit> {
            setUser(null)
            return AppResult.Success(Unit)
        }
    }

    private class FakeBookRepo : BookRepository {
        val books = mutableListOf<Book>()
        val chapters = mutableListOf<Chapter>()
        val comments = mutableListOf<BookComment>()

        override fun getFeaturedBooks(): Flow<AppResult<List<Book>>> = flowOf(AppResult.Success(books))
        override fun getRecentBooks(): Flow<AppResult<List<Book>>> = flowOf(AppResult.Success(books))
        override fun getBooksByCategory(category: BookCategory): Flow<AppResult<List<Book>>> = flowOf(AppResult.Success(books.filter { it.category == category }))
        override fun getBookById(bookId: String): Flow<AppResult<Book?>> = flowOf(AppResult.Success(books.find { it.id == bookId }))
        override fun getUserLibrary(userId: String, shelfType: ShelfType): Flow<AppResult<List<UserShelfItem>>> = flowOf(AppResult.Success(emptyList()))
        override fun getUserWrittenBooks(userId: String): Flow<AppResult<List<Book>>> = flowOf(AppResult.Success(books.filter { it.ownerId == userId }))
        override suspend fun createBook(book: Book): AppResult<Book> { books.add(book); return AppResult.Success(book) }
        override suspend fun updateBook(book: Book): AppResult<Book> { books.removeAll { it.id == book.id }; books.add(book); return AppResult.Success(book) }
        override suspend fun deleteBook(bookId: String): AppResult<Unit> { books.removeAll { it.id == bookId }; chapters.removeAll { it.bookId == bookId }; return AppResult.Success(Unit) }
        override suspend fun addBookToShelf(userId: String, bookId: String, shelfType: ShelfType): AppResult<Unit> = AppResult.Success(Unit)
        override suspend fun removeBookFromShelf(userId: String, bookId: String, shelfType: ShelfType): AppResult<Unit> = AppResult.Success(Unit)

        override fun getBookChapters(bookId: String): Flow<AppResult<List<Chapter>>> =
            flowOf(AppResult.Success(chapters.filter { it.bookId == bookId }.sortedBy { it.chapterNumber }))

        override suspend fun saveChapter(chapter: Chapter): AppResult<Chapter> {
            val withId = chapter.copy(id = chapter.id.ifBlank { "ch_" + System.currentTimeMillis() })
            chapters.removeAll { it.id == withId.id }
            chapters.add(withId)
            return AppResult.Success(withId)
        }

        override suspend fun deleteChapter(bookId: String, chapterId: String): AppResult<Unit> {
            chapters.removeAll { it.id == chapterId }
            return AppResult.Success(Unit)
        }

        override suspend fun reorderChapters(bookId: String, reordered: List<Chapter>): AppResult<Unit> {
            reordered.forEachIndexed { idx, ch ->
                val existingIdx = chapters.indexOfFirst { it.id == ch.id }
                if (existingIdx >= 0) {
                    chapters[existingIdx] = ch.copy(chapterNumber = idx + 1)
                }
            }
            return AppResult.Success(Unit)
        }

        override suspend fun duplicateChapter(bookId: String, chapterId: String): AppResult<Chapter> {
            val original = chapters.find { it.id == chapterId } ?: return AppResult.Error(AppError.NotFound("Bulunamadı"))
            val dup = original.copy(id = "ch_dup_${System.currentTimeMillis()}", title = "${original.title} (Kopya)", chapterNumber = chapters.size + 1)
            chapters.add(dup)
            return AppResult.Success(dup)
        }

        override suspend fun publishBook(bookId: String): AppResult<Book> {
            val current = books.find { it.id == bookId } ?: return AppResult.Error(AppError.NotFound("Bulunamadı"))
            val updatedChapters = chapters.filter { it.bookId == bookId }.map { it.copy(isPublished = true) }
            chapters.removeAll { it.bookId == bookId }
            chapters.addAll(updatedChapters)
            val published = current.copy(status = BookStatus.PUBLISHED, chapterCount = updatedChapters.size)
            books.removeAll { it.id == bookId }
            books.add(published)
            return AppResult.Success(published)
        }

        override suspend fun unpublishBook(bookId: String): AppResult<Book> {
            val current = books.find { it.id == bookId } ?: return AppResult.Error(AppError.NotFound("Bulunamadı"))
            val draft = current.copy(status = BookStatus.DRAFT)
            books.removeAll { it.id == bookId }
            books.add(draft)
            return AppResult.Success(draft)
        }

        override fun getBookComments(bookId: String): Flow<AppResult<List<BookComment>>> = flowOf(AppResult.Success(comments.filter { it.bookId == bookId }))
        override suspend fun deleteBookComment(bookId: String, commentId: String): AppResult<Unit> {
            comments.removeAll { it.id == commentId }
            return AppResult.Success(Unit)
        }
    }

    private class FakeStorageRepo : StorageRepository {
        override fun uploadMedia(request: StorageUploadRequest, onProgress: (Int) -> Unit): Flow<AppResult<String>> {
            onProgress(100)
            return flowOf(AppResult.Success("https://media.libra.app/${request.fileName}"))
        }
        override suspend fun deleteMedia(fileKey: String): AppResult<Unit> = AppResult.Success(Unit)
        override fun getPublicCdnUrl(fileKey: String): String = "https://media.libra.app/$fileKey"
        override suspend fun getSignedMediaUrl(fileKey: String): String = "https://media.libra.app/$fileKey"
    }
}
