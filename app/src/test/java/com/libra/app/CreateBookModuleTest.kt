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
import com.libra.app.domain.repository.AuthRepository
import com.libra.app.domain.repository.BookRepository
import com.libra.app.domain.repository.StorageRepository
import com.libra.app.feature.write.create.CreateBookDraft
import com.libra.app.feature.write.create.CreateBookViewModel
import com.libra.app.feature.write.create.DraftChapter
import com.libra.app.feature.write.create.InMemoryBookDraftStorage
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
class CreateBookModuleTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var fakeAuthRepo: FakeAuthRepository
    private lateinit var fakeBookRepo: FakeBookRepository
    private lateinit var fakeStorageRepo: FakeStorageRepository
    private lateinit var draftStorage: InMemoryBookDraftStorage
    private lateinit var viewModel: CreateBookViewModel

    private val testUser = UserProfile(
        uid = "author_123",
        displayName = "Elif Şafak",
        username = "elifsafak",
        email = "elif@libra.app"
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeAuthRepo = FakeAuthRepository().apply { setUser(testUser) }
        fakeBookRepo = FakeBookRepository()
        fakeStorageRepo = FakeStorageRepository()
        draftStorage = InMemoryBookDraftStorage()

        viewModel = CreateBookViewModel(
            authRepository = fakeAuthRepo,
            bookRepository = fakeBookRepo,
            storageRepository = fakeStorageRepo,
            draftStorage = draftStorage
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun createBookDraft_persistenceAndRestoration_worksCorrectly() = runTest {
        val draft = CreateBookDraft(
            title = "Sisler Şehri",
            description = "Eski bir sahafta bulunan gizemli elyazmasının peşinde...",
            discoverySummary = "Karanlık çöktüğünde aynalara asla bakma.",
            categoryName = BookCategory.MYSTERY_THRILLER.name,
            chapters = listOf(
                DraftChapter(id = "c1", chapterNumber = 1, title = "Giriş: Yağmurlu Gece", content = "Yağmur bardaktan boşanırcasına yağıyordu."),
                DraftChapter(id = "c2", chapterNumber = 2, title = "2. Bölüm: Sahaflar Çarşısı", content = "Kapı gıcırdayarak açıldı.")
            ),
            tags = listOf("gizem", "gerilim", "istanbul"),
            language = "Türkçe",
            genre = "Roman",
            subGenre = "Psikolojik Gerilim",
            targetAudience = "Genç Yetişkin (13+)",
            contentWarnings = listOf("Korku & Gerilim"),
            isSeries = true,
            seriesName = "Karanlık Üçleme",
            seriesOrder = 1,
            visibility = "PUBLIC",
            currentStep = 3
        )

        draftStorage.saveDraft(draft)
        assertTrue(draftStorage.hasDraft())

        viewModel.attachDraftStorage(draftStorage)
        assertTrue(viewModel.uiState.value.hasUnsavedLocalDraft)

        viewModel.restoreDraft()
        val state = viewModel.uiState.value

        assertEquals("Sisler Şehri", state.title)
        assertEquals("Eski bir sahafta bulunan gizemli elyazmasının peşinde...", state.description)
        assertEquals("Karanlık çöktüğünde aynalara asla bakma.", state.discoverySummary)
        assertEquals(BookCategory.MYSTERY_THRILLER, state.category)
        assertEquals(2, state.chapters.size)
        assertEquals("Giriş: Yağmurlu Gece", state.chapters[0].title)
        assertEquals(3, state.tags.size)
        assertTrue(state.tags.contains("istanbul"))
        assertEquals("Roman", state.genre)
        assertEquals("Psikolojik Gerilim", state.subGenre)
        assertEquals("Genç Yetişkin (13+)", state.targetAudience)
        assertTrue(state.contentWarnings.contains("Korku & Gerilim"))
        assertTrue(state.isSeries)
        assertEquals("Karanlık Üçleme", state.seriesName)
        assertEquals(3, state.currentStep)
        assertFalse(state.hasUnsavedLocalDraft)
    }

    @Test
    fun createBookViewModel_chapterManagement_addsReordersAndDeletesCorrectly() = runTest {
        // Starts with 1 default chapter
        assertEquals(1, viewModel.uiState.value.chapters.size)
        assertEquals("1. Bölüm", viewModel.uiState.value.chapters[0].title)

        // Add 2 more chapters
        viewModel.addChapter("Kayıp Tapınak", "Tapınağın kapısı mühürlüydü.")
        viewModel.addChapter("Son Karşılaşma", "Kılıçlar çekildi.")

        assertEquals(3, viewModel.uiState.value.chapters.size)
        assertEquals("Kayıp Tapınak", viewModel.uiState.value.chapters[1].title)
        assertEquals("Son Karşılaşma", viewModel.uiState.value.chapters[2].title)

        // Reorder: Move last chapter up
        val lastChapterId = viewModel.uiState.value.chapters[2].id
        viewModel.moveChapterUp(lastChapterId)

        assertEquals("Son Karşılaşma", viewModel.uiState.value.chapters[1].title)
        assertEquals(2, viewModel.uiState.value.chapters[1].chapterNumber)
        assertEquals("Kayıp Tapınak", viewModel.uiState.value.chapters[2].title)
        assertEquals(3, viewModel.uiState.value.chapters[2].chapterNumber)

        // Delete chapter
        viewModel.deleteChapter(lastChapterId)
        assertEquals(2, viewModel.uiState.value.chapters.size)

        // Attempt deleting until 1 chapter remaining
        val remainingChapterId = viewModel.uiState.value.chapters[1].id
        viewModel.deleteChapter(remainingChapterId)
        assertEquals(1, viewModel.uiState.value.chapters.size)

        // Should not allow deleting the last chapter
        val onlyChapterId = viewModel.uiState.value.chapters[0].id
        viewModel.deleteChapter(onlyChapterId)
        assertEquals(1, viewModel.uiState.value.chapters.size)
        assertNotNull(viewModel.uiState.value.errorMessage)
    }

    @Test
    fun createBookViewModel_validationRules_enforcedProperly() = runTest {
        val stateInitial = viewModel.uiState.value
        // Initially empty title
        assertFalse(stateInitial.isTitleValid)
        assertFalse(stateInitial.canProceedFromStep1)
        assertFalse(stateInitial.canPublish)

        // Title with 1 char is invalid
        viewModel.updateTitle("A")
        assertFalse(viewModel.uiState.value.isTitleValid)

        // Title with 2+ chars is valid
        viewModel.updateTitle("Karanlık")
        assertTrue(viewModel.uiState.value.isTitleValid)
        assertTrue(viewModel.uiState.value.canProceedFromStep1)

        // Can't publish if first chapter has no content
        assertFalse(viewModel.uiState.value.canPublish)

        // Update first chapter with content
        val firstChapterId = viewModel.uiState.value.chapters[0].id
        viewModel.updateChapter(firstChapterId, "Bölüm 1", "Bu ilk bölümün sürükleyici metnidir.")
        assertTrue(viewModel.uiState.value.isFirstChapterFilled)
        assertTrue(viewModel.uiState.value.canPublish)
    }

    @Test
    fun createBookViewModel_saveAsDraft_createsDraftBookAndChapters() = runTest {
        viewModel.updateTitle("Taslak Kitabım")
        viewModel.updateDescription("Açıklama metni")
        viewModel.updateCategory(BookCategory.SCIFI_FANTASY)
        viewModel.addTag("bilimkurgu")

        val firstChapterId = viewModel.uiState.value.chapters[0].id
        viewModel.updateChapter(firstChapterId, "Bölüm 1", "Taslak içerik")

        var savedBookResult: Book? = null
        viewModel.saveAsDraft { book ->
            savedBookResult = book
        }
        advanceUntilIdle()

        assertNotNull(savedBookResult)
        assertEquals("Taslak Kitabım", savedBookResult?.title)
        assertEquals(BookStatus.DRAFT, savedBookResult?.status)
        assertEquals("author_123", savedBookResult?.ownerId)
        assertEquals(1, fakeBookRepo.createdBooks.size)
        assertEquals(1, fakeBookRepo.savedChapters.size)
        assertFalse(draftStorage.hasDraft())
    }

    @Test
    fun createBookViewModel_publishBook_validatesAndPublishesSuccessfully() = runTest {
        viewModel.updateTitle("Yıldızlararası Yolculuk")
        viewModel.updateDescription("Derin uzayda geçen destansı bir macera.")
        viewModel.updateDiscoverySummary("Işık hızını aştıklarında geriye dönüş yoktu.")
        viewModel.updateCategory(BookCategory.SCIFI_FANTASY)
        viewModel.addTag("uzay")
        viewModel.addTag("astronomi")

        val firstChapterId = viewModel.uiState.value.chapters[0].id
        viewModel.updateChapter(firstChapterId, "1. Bölüm: Fırlatma", "Geri sayım başladı: 3, 2, 1...")

        var publishedBook: Book? = null
        viewModel.publishBook { book ->
            publishedBook = book
        }
        advanceUntilIdle()

        assertNotNull(publishedBook)
        assertEquals("Yıldızlararası Yolculuk", publishedBook?.title)
        assertEquals(BookStatus.PUBLISHED, publishedBook?.status)
        assertTrue(publishedBook?.publishedAt ?: 0L > 0L)
        assertEquals(1, fakeBookRepo.createdBooks.size)
        assertTrue(fakeBookRepo.savedChapters.first().isPublished)
        assertFalse(draftStorage.hasDraft())
    }

    @Test
    fun createBookUiState_stepNavigation_clampsToValidRange() {
        assertEquals(1, viewModel.uiState.value.currentStep)

        viewModel.nextStep()
        assertEquals(2, viewModel.uiState.value.currentStep)

        viewModel.nextStep()
        assertEquals(3, viewModel.uiState.value.currentStep)

        viewModel.nextStep()
        assertEquals(4, viewModel.uiState.value.currentStep)

        // Clamped at 4
        viewModel.nextStep()
        assertEquals(4, viewModel.uiState.value.currentStep)

        viewModel.previousStep()
        assertEquals(3, viewModel.uiState.value.currentStep)

        viewModel.setStep(1)
        assertEquals(1, viewModel.uiState.value.currentStep)

        // Clamped at 1
        viewModel.previousStep()
        assertEquals(1, viewModel.uiState.value.currentStep)
    }

    // --- Test Doubles ---

    private class FakeAuthRepository : AuthRepository {
        private val _currentUser = MutableStateFlow<UserProfile?>(null)
        override val currentUser: StateFlow<UserProfile?> = _currentUser.asStateFlow()
        private val _isAuthenticated = MutableStateFlow(false)
        override val isAuthenticated: StateFlow<Boolean> = _isAuthenticated.asStateFlow()

        fun setUser(profile: UserProfile?) {
            _currentUser.value = profile
            _isAuthenticated.value = profile != null
        }

        override suspend fun checkCurrentSession(): AppResult<UserProfile?> =
            AppResult.Success(_currentUser.value)

        override fun signInWithGoogleIdToken(
            idToken: String,
            displayName: String?,
            email: String?,
            photoUrl: String?
        ): Flow<AppResult<UserProfile>> =
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

    private class FakeBookRepository : BookRepository {
        val createdBooks = mutableListOf<Book>()
        val savedChapters = mutableListOf<Chapter>()

        override fun getFeaturedBooks(): Flow<AppResult<List<Book>>> = flowOf(AppResult.Success(createdBooks))
        override fun getRecentBooks(): Flow<AppResult<List<Book>>> = flowOf(AppResult.Success(createdBooks))
        override fun getBooksByCategory(category: BookCategory): Flow<AppResult<List<Book>>> = flowOf(AppResult.Success(createdBooks.filter { it.category == category }))
        override fun getBookById(bookId: String): Flow<AppResult<Book?>> = flowOf(AppResult.Success(createdBooks.find { it.id == bookId }))
        override fun getBookChapters(bookId: String): Flow<AppResult<List<Chapter>>> = flowOf(AppResult.Success(savedChapters.filter { it.bookId == bookId }))
        override fun getChapterById(bookId: String, chapterId: String): Flow<AppResult<Chapter?>> = flowOf(AppResult.Success(savedChapters.find { it.id == chapterId }))
        override fun getUserWrittenBooks(userId: String): Flow<AppResult<List<Book>>> = flowOf(AppResult.Success(createdBooks.filter { it.ownerId == userId }))
        override fun getUserLibrary(userId: String, shelfType: ShelfType): Flow<AppResult<List<Book>>> = flowOf(AppResult.Success(emptyList()))

        override suspend fun createBook(book: Book): AppResult<Book> {
            val bookWithId = book.copy(id = book.id.ifBlank { "book_" + System.currentTimeMillis() })
            createdBooks.add(bookWithId)
            return AppResult.Success(bookWithId)
        }

        override suspend fun updateBook(book: Book): AppResult<Book> {
            createdBooks.removeAll { it.id == book.id }
            createdBooks.add(book)
            return AppResult.Success(book)
        }

        override suspend fun deleteBook(bookId: String): AppResult<Unit> {
            createdBooks.removeAll { it.id == bookId }
            return AppResult.Success(Unit)
        }

        override suspend fun saveChapter(chapter: Chapter): AppResult<Chapter> {
            val chapterWithId = chapter.copy(id = chapter.id.ifBlank { "chapter_" + System.currentTimeMillis() })
            savedChapters.removeAll { it.id == chapterWithId.id }
            savedChapters.add(chapterWithId)
            return AppResult.Success(chapterWithId)
        }

        override suspend fun deleteChapter(bookId: String, chapterId: String): AppResult<Unit> {
            savedChapters.removeAll { it.id == chapterId }
            return AppResult.Success(Unit)
        }

        override suspend fun addBookToShelf(userId: String, bookId: String, shelfType: ShelfType): AppResult<Unit> = AppResult.Success(Unit)
        override suspend fun removeBookFromShelf(userId: String, bookId: String, shelfType: ShelfType): AppResult<Unit> = AppResult.Success(Unit)
        override fun observeEngagement(bookId: String, currentUserId: String): Flow<AppResult<BookEngagement>> = flowOf(AppResult.Success(BookEngagement()))
        override suspend fun toggleLike(bookId: String, userId: String): AppResult<Boolean> = AppResult.Success(true)
        override suspend fun toggleSave(bookId: String, userId: String): AppResult<Boolean> = AppResult.Success(true)
        override fun observeComments(bookId: String): Flow<AppResult<List<BookComment>>> = flowOf(AppResult.Success(emptyList()))
        override suspend fun addComment(bookId: String, userId: String, userName: String, userAvatarUrl: String, text: String): AppResult<BookComment> = AppResult.Success(BookComment())
        override suspend fun deleteComment(bookId: String, commentId: String): AppResult<Unit> = AppResult.Success(Unit)
        override fun observeReadingProgress(userId: String, bookId: String): Flow<AppResult<ReadingProgress?>> = flowOf(AppResult.Success(null))
        override suspend fun saveReadingProgress(progress: ReadingProgress): AppResult<Unit> = AppResult.Success(Unit)
    }

    private class FakeStorageRepository : StorageRepository {
        override fun uploadMedia(request: StorageUploadRequest, onProgress: (Int) -> Unit): Flow<AppResult<String>> {
            onProgress(100)
            return flowOf(AppResult.Success("https://media.libra.app/${request.fileName}"))
        }

        override suspend fun deleteMedia(fileKey: String): AppResult<Unit> = AppResult.Success(Unit)
        override fun getPublicCdnUrl(fileKey: String): String = "https://media.libra.app/$fileKey"
        override suspend fun getSignedMediaUrl(fileKey: String): String = "https://media.libra.app/$fileKey"
    }
}
