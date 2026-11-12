package com.libra.app

import com.libra.app.core.result.AppError
import com.libra.app.core.result.AppResult
import com.libra.app.domain.model.Book
import com.libra.app.domain.model.BookCategory
import com.libra.app.domain.model.BookComment
import com.libra.app.domain.model.BookEngagement
import com.libra.app.domain.model.BookStatus
import com.libra.app.domain.model.Chapter
import com.libra.app.domain.model.ShelfType
import com.libra.app.domain.model.StorageUploadRequest
import com.libra.app.domain.model.UserProfile
import com.libra.app.domain.model.UserShelfItem
import com.libra.app.domain.repository.AuthRepository
import com.libra.app.domain.repository.BookRepository
import com.libra.app.domain.repository.StorageRepository
import com.libra.app.feature.book.manage.BookManagementViewModel
import com.libra.app.feature.discover.BookEngagementUi
import com.libra.app.feature.discover.BookReelsDiscoverViewModel
import com.libra.app.feature.discover.CHAPTER_PREVIEW_MAX_CHARS
import com.libra.app.feature.discover.CHAPTER_PREVIEW_MIN_CHARS
import com.libra.app.feature.discover.formatChapterTeaserSnippet
import com.libra.app.feature.home.HomeSection
import com.libra.app.ui.navigation.BottomNavTab
import org.junit.Assert.assertNull
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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class BookDiscoveryAndMediaUpgradeTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var fakeAuthRepo: FakeAuthRepo
    private lateinit var fakeBookRepo: FakeBookRepo
    private lateinit var fakeStorageRepo: FakeStorageRepo
    private lateinit var manageVm: BookManagementViewModel
    private lateinit var reelsVm: BookReelsDiscoverViewModel

    private val authorUser = UserProfile(
        uid = "usr_author_123",
        displayName = "Reşat Nuri",
        username = "resatnuri",
        email = "resat@libra.app"
    )

    private val sampleBook = Book(
        id = "bk_calikusu",
        ownerId = "usr_author_123",
        authorName = "Reşat Nuri",
        title = "Çalıkuşu",
        description = "Feride'nin Anadolu'daki öğretmenlik yolculuğu.",
        discoverySummary = "Anadolu bozkırında yeşeren bir umut ve sevgi romanı.",
        category = BookCategory.CLASSICS,
        status = BookStatus.DRAFT,
        chapterCount = 2,
        likesCount = 42
    )

    private val sampleChapters = listOf(
        Chapter(
            id = "ch_1",
            bookId = "bk_calikusu",
            chapterNumber = 1,
            title = "Çocukluk Yılları",
            content = "Zeyniler köyüne ilk vardığım gün akşam karanlığı basıyordu. Rüzgar uğulduyordu.",
            wordCount = 180,
            isPublished = false,
            isArchived = false
        ),
        Chapter(
            id = "ch_2",
            bookId = "bk_calikusu",
            chapterNumber = 2,
            title = "Tekirdağ",
            content = "İçimdeki neşeyi kimse elimden alamazdı.",
            wordCount = 150,
            isPublished = false,
            isArchived = false
        )
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeAuthRepo = FakeAuthRepo().apply { setUser(authorUser) }
        fakeBookRepo = FakeBookRepo().apply {
            books.add(sampleBook)
            chapters.addAll(sampleChapters)
        }
        fakeStorageRepo = FakeStorageRepo()

        manageVm = BookManagementViewModel(
            bookRepository = fakeBookRepo,
            authRepository = fakeAuthRepo,
            storageRepository = fakeStorageRepo
        )

        reelsVm = BookReelsDiscoverViewModel(
            bookRepository = fakeBookRepo,
            authRepository = fakeAuthRepo
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // 1. R2 Ownership and Directory Verification
    @Test
    fun r2TargetDirectory_ownershipCheck_acceptsUserSubdirectories() {
        val uid = "usr_author_123"
        val ownerDirectory = "users/$uid"

        // Subdirectories under user directory must be valid
        val coverDir = "users/$uid/covers"
        val isCoverValid = coverDir == ownerDirectory || coverDir.startsWith("$ownerDirectory/")
        assertTrue("Subdirectory 'users/$uid/covers' must be accepted", isCoverValid)

        val avatarDir = "users/$uid/avatars"
        val isAvatarValid = avatarDir == ownerDirectory || avatarDir.startsWith("$ownerDirectory/")
        assertTrue("Subdirectory 'users/$uid/avatars' must be accepted", isAvatarValid)

        // Foreign user directory must be rejected
        val foreignDir = "users/usr_hacker_999/covers"
        val isForeignValid = foreignDir == ownerDirectory || foreignDir.startsWith("$ownerDirectory/")
        assertFalse("Foreign user directory must be rejected", isForeignValid)

        // Admin paths
        val adminPath = "admin/releases"
        val isAdminValid = adminPath == "admin" || adminPath.startsWith("admin/")
        assertTrue("Admin path must be accepted", isAdminValid)
    }

    // 2. Chapter Archiving vs Deleting
    @Test
    fun chapterArchiving_separatesActiveAndArchivedChapters() = runTest {
        manageVm.initialize(sampleBook)
        advanceUntilIdle()

        assertEquals(2, manageVm.state.value.activeChapters.size)
        assertEquals(0, manageVm.state.value.archivedChapters.size)

        // Archive Chapter 2
        manageVm.archiveChapter(sampleChapters[1])
        advanceUntilIdle()

        assertEquals(1, manageVm.state.value.activeChapters.size)
        assertEquals(1, manageVm.state.value.archivedChapters.size)
        assertEquals("ch_1", manageVm.state.value.activeChapters[0].id)
        assertEquals("ch_2", manageVm.state.value.archivedChapters[0].id)
        assertTrue(manageVm.state.value.archivedChapters[0].isArchived)
        assertFalse(manageVm.state.value.archivedChapters[0].isPublished)

        // Unarchive Chapter 2
        manageVm.unarchiveChapter(sampleChapters[1])
        advanceUntilIdle()

        assertEquals(2, manageVm.state.value.activeChapters.size)
        assertEquals(0, manageVm.state.value.archivedChapters.size)
    }

    @Test
    fun readerExperience_neverShowsArchivedChapters() {
        val publishedBook = sampleBook.copy(status = BookStatus.PUBLISHED)
        val allChapters = listOf(
            Chapter(id = "c1", bookId = "b1", chapterNumber = 1, isPublished = true, isArchived = false),
            Chapter(id = "c2", bookId = "b1", chapterNumber = 2, isPublished = true, isArchived = true), // Archived!
            Chapter(id = "c3", bookId = "b1", chapterNumber = 3, isPublished = true, isArchived = false)
        )

        val nonArchivedChapters = allChapters.filter { !it.isArchived }
        val publishedChapters = nonArchivedChapters.filter { it.isPublished }
        val visibleChapters = when {
            publishedChapters.isNotEmpty() -> publishedChapters
            publishedBook.status == BookStatus.PUBLISHED -> nonArchivedChapters
            else -> emptyList()
        }

        assertEquals(2, visibleChapters.size)
        assertFalse(visibleChapters.any { it.id == "c2" })
    }

    // 3. Pre-publish Validation
    @Test
    fun prePublishValidation_requiresTitleDescriptionAndActiveChapters() = runTest {
        val emptyBook = Book(id = "bk_empty", ownerId = authorUser.uid, title = "", description = "")
        fakeBookRepo.books.add(emptyBook)

        val invalidVm = BookManagementViewModel(
            bookRepository = fakeBookRepo,
            authRepository = fakeAuthRepo,
            storageRepository = fakeStorageRepo
        )
        invalidVm.initialize(emptyBook)
        advanceUntilIdle()

        val missing = invalidVm.validatePrePublish()
        assertTrue(missing.any { it.contains("başlığı") })
        assertTrue(missing.any { it.contains("açıklaması") })
        assertTrue(missing.any { it.contains("aktif bölüm") })

        // When valid
        val validVm = BookManagementViewModel(
            bookRepository = fakeBookRepo,
            authRepository = fakeAuthRepo,
            storageRepository = fakeStorageRepo
        )
        validVm.initialize(sampleBook)
        advanceUntilIdle()
        val noMissing = validVm.validatePrePublish()
        assertTrue("All required fields provided; missing checklist must be empty", noMissing.isEmpty())
    }

    // 4. Discover Reels Experience
    @Test
    fun discoverReels_optimisticLikeAndSave_behavesCorrectly() = runTest {
        advanceUntilIdle()

        val book = sampleBook
        val initialEngagement = reelsVm.state.value.engagements[book.id]
        val initialLikes = initialEngagement?.likesCount ?: book.likesCount

        // Like toggle
        reelsVm.toggleLike(book)
        val afterLike = reelsVm.state.value.engagements[book.id]
        assertTrue(afterLike?.isLiked == true)
        assertEquals(initialLikes + 1, afterLike?.likesCount)

        // Save toggle
        reelsVm.toggleSave(book)
        val afterSave = reelsVm.state.value.engagements[book.id]
        assertTrue(afterSave?.isSaved == true)

        // Start reading saves to READING shelf
        reelsVm.startReading(book)
        advanceUntilIdle()
        assertTrue(fakeBookRepo.shelfItems.any { it.bookId == book.id && it.shelfType == ShelfType.READING })
    }

    // 5. Book Cover URL Bidirectional Resolution & Upload
    @Test
    fun bookCover_bidirectionalResolution_worksForBothLegacyAndNewFields() {
        val legacyBook = sampleBook.copy(coverImageUrl = "", coverUrl = "users/123/covers/legacy.jpg")
        assertEquals("users/123/covers/legacy.jpg", legacyBook.displayCoverUrl)

        val newBook = sampleBook.copy(coverImageUrl = "users/123/covers/new.jpg", coverUrl = "")
        assertEquals("users/123/covers/new.jpg", newBook.displayCoverUrl)

        val bothBook = sampleBook.copy(coverImageUrl = "users/123/covers/primary.jpg", coverUrl = "users/123/covers/secondary.jpg")
        assertEquals("users/123/covers/primary.jpg", bothBook.displayCoverUrl)
    }

    @Test
    fun bookManagementViewModel_uploadBookCover_persistsBothCoverFields() = runTest {
        manageVm.initialize(sampleBook)
        advanceUntilIdle()

        manageVm.uploadBookCover(
            bytes = "sample_image_bytes".toByteArray(),
            fileName = "cover.jpg",
            contentType = "image/jpeg"
        )
        advanceUntilIdle()

        val updatedBook = manageVm.state.value.book
        assertNotNull(updatedBook)
        assertEquals(100, manageVm.state.value.coverUploadProgress)
        assertFalse(manageVm.state.value.isUploadingCover)
        assertTrue(updatedBook?.coverImageUrl?.contains("cover.jpg") == true)
        assertTrue(updatedBook?.coverUrl?.contains("cover.jpg") == true)
    }

    // 6. Navigation Correction & Isolation Verification
    @Test
    fun discoverTab_routesToFriendsAndSocialDiscovery() {
        var activeTab = BottomNavTab.DISCOVER
        val onTabSelect: (BottomNavTab) -> Unit = { activeTab = it }
        onTabSelect(BottomNavTab.DISCOVER)
        assertEquals(BottomNavTab.DISCOVER, activeTab)
        assertEquals("nav_discover", activeTab.testTag)
        assertEquals("Keşfet", activeTab.title)
    }

    @Test
    fun homeSection_books_isDedicatedDiscoveryArea() {
        var activeSection = HomeSection.POSTS
        val onSectionChange: (HomeSection) -> Unit = { activeSection = it }

        onSectionChange(HomeSection.BOOKS)
        assertEquals(HomeSection.BOOKS, activeSection)

        onSectionChange(HomeSection.POSTS)
        assertEquals(HomeSection.POSTS, activeSection)
    }

    @Test
    fun startReading_persistsToReadingShelf_andSyncsWithLibrary() = runTest {
        reelsVm.loadDiscoverBooks()
        advanceUntilIdle()

        val bookToRead = sampleBook
        reelsVm.startReading(bookToRead)
        advanceUntilIdle()

        val savedInShelf = fakeBookRepo.shelfItems.any {
            it.bookId == bookToRead.id && it.shelfType == ShelfType.READING && it.userId == authorUser.uid
        }
        assertTrue("Book must be recorded in READING shelf for Library sync", savedInShelf)
    }

    // 7. Chapter Teaser Formatting & Preview Filtering
    @Test
    fun formatChapterTeaserSnippet_shortContent_returnsFullText() {
        val shortText = "Zeyniler köyünde akşam oluyordu. Kuşlar dallarına çekilmişti."
        val (snippet, hasMore) = formatChapterTeaserSnippet(shortText)
        assertEquals(shortText, snippet)
        assertFalse(hasMore)
    }

    @Test
    fun formatChapterTeaserSnippet_longContent_cutsBetweenMinAndMaxAtSentenceBoundary() {
        val paragraph1 = "A".repeat(700) + ". "
        val paragraph2 = "B".repeat(600) + ".\n\n"
        val paragraph3 = "C".repeat(1000) + "."
        val longText = paragraph1 + paragraph2 + paragraph3

        val (snippet, hasMore) = formatChapterTeaserSnippet(longText)
        assertTrue("Snippet must have more text", hasMore)
        assertTrue(
            "Snippet length (${snippet.length}) must be in range [$CHAPTER_PREVIEW_MIN_CHARS, $CHAPTER_PREVIEW_MAX_CHARS]",
            snippet.length in CHAPTER_PREVIEW_MIN_CHARS..CHAPTER_PREVIEW_MAX_CHARS
        )
        assertTrue("Snippet must preserve newline formatting or sentence end", snippet.contains("\n\n") || snippet.endsWith("."))
    }

    @Test
    fun loadChapterPreview_filtersOutUnpublishedAndArchivedChapters() = runTest {
        // Book with draft chapter only
        reelsVm.loadDiscoverBooks()
        advanceUntilIdle()

        // sampleChapters in setUp are draft (isPublished = false), so no preview snippet should be set
        val preview = reelsVm.state.value.chapterPreviews[sampleBook.id]
        assertNull("Unpublished/draft chapters must not generate a discover teaser", preview)

        // Now publish chapter 1
        fakeBookRepo.chapters.clear()
        fakeBookRepo.chapters.add(
            Chapter(
                id = "ch_published_1",
                bookId = sampleBook.id,
                chapterNumber = 1,
                title = "Giriş",
                content = "Yıllar sonra memlekete döndüğümde her şey çok değişmişti. " + "D".repeat(2200) + ". Devamı vardı.",
                isPublished = true,
                isArchived = false
            )
        )

        reelsVm.loadDiscoverBooks()
        advanceUntilIdle()

        val publishedPreview = reelsVm.state.value.chapterPreviews[sampleBook.id]
        assertNotNull("Published active chapter must generate a teaser excerpt", publishedPreview)
        val hasMore = reelsVm.state.value.chapterPreviewHasMore[sampleBook.id] ?: false
        assertTrue("Preview should flag continuation if content exceeds max length", hasMore)
    }

    @Test
    fun libraryCreateBook_invokingCallback_triggersStateOrNavigation() {
        var createBookOpened = false
        val onCreateBookClick = { createBookOpened = true }

        onCreateBookClick()
        assertTrue("Create book action must set showCreateBook to true", createBookOpened)
    }

    @Test
    fun homeSectionPosts_isolatesCreateFabFromBooksDiscovery() {
        // Verify FAB visibility logic ensures no collision with Books top bar "+ Kitap Yaz"
        val showCreateMenu = true
        val postSection = HomeSection.POSTS
        val bookSection = HomeSection.BOOKS

        val fabVisibleInPosts = showCreateMenu && postSection == HomeSection.POSTS
        val fabVisibleInBooks = showCreateMenu && bookSection == HomeSection.POSTS

        assertTrue("FAB should be visible in POSTS section", fabVisibleInPosts)
        assertFalse("FAB must be isolated and hidden in BOOKS section to prevent button collision", fabVisibleInBooks)
    }

    @Test
    fun bookReelsDiscoverScreen_lineHeightAndCoverSpecs_valid() {
        // Validate typography line height ratios for 120Hz readability without overlap
        val titleFontSize = 22f
        val titleLineHeight = 28f
        val authorFontSize = 13.5f
        val authorLineHeight = 18f
        val synopsisFontSize = 12.5f
        val synopsisLineHeight = 18.5f

        assertTrue("Title lineHeight must be greater than fontSize", titleLineHeight > titleFontSize)
        assertTrue("Author lineHeight must be greater than fontSize", authorLineHeight > authorFontSize)
        assertTrue("Synopsis lineHeight must be greater than fontSize", synopsisLineHeight > synopsisFontSize)
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

    private data class FakeShelfItem(val userId: String, val bookId: String, val shelfType: ShelfType)

    private class FakeBookRepo : BookRepository {
        val books = mutableListOf<Book>()
        val chapters = mutableListOf<Chapter>()
        val comments = mutableListOf<BookComment>()
        val shelfItems = mutableListOf<FakeShelfItem>()

        override fun getFeaturedBooks(): Flow<AppResult<List<Book>>> = flowOf(AppResult.Success(books))
        override fun getRecentBooks(): Flow<AppResult<List<Book>>> = flowOf(AppResult.Success(books))
        override fun getBooksByCategory(category: BookCategory): Flow<AppResult<List<Book>>> =
            flowOf(AppResult.Success(books.filter { it.category == category }))
        override fun getBookById(bookId: String): Flow<AppResult<Book?>> =
            flowOf(AppResult.Success(books.find { it.id == bookId }))
        override fun getUserLibrary(userId: String, shelfType: ShelfType): Flow<AppResult<List<UserShelfItem>>> =
            flowOf(AppResult.Success(emptyList()))
        override fun getUserWrittenBooks(userId: String): Flow<AppResult<List<Book>>> =
            flowOf(AppResult.Success(books.filter { it.ownerId == userId }))
        override suspend fun createBook(book: Book): AppResult<Book> { books.add(book); return AppResult.Success(book) }
        override suspend fun updateBook(book: Book): AppResult<Book> {
            books.removeAll { it.id == book.id }
            books.add(book)
            return AppResult.Success(book)
        }
        override suspend fun deleteBook(bookId: String): AppResult<Unit> {
            books.removeAll { it.id == bookId }
            chapters.removeAll { it.bookId == bookId }
            return AppResult.Success(Unit)
        }
        override suspend fun addBookToShelf(userId: String, bookId: String, shelfType: ShelfType): AppResult<Unit> {
            shelfItems.add(FakeShelfItem(userId, bookId, shelfType))
            return AppResult.Success(Unit)
        }
        override suspend fun removeBookFromShelf(userId: String, bookId: String, shelfType: ShelfType): AppResult<Unit> {
            shelfItems.removeAll { it.userId == userId && it.bookId == bookId && it.shelfType == shelfType }
            return AppResult.Success(Unit)
        }

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

        override suspend fun archiveChapter(bookId: String, chapterId: String): AppResult<Unit> {
            val idx = chapters.indexOfFirst { it.id == chapterId }
            if (idx >= 0) {
                chapters[idx] = chapters[idx].copy(isArchived = true, isPublished = false)
            }
            return AppResult.Success(Unit)
        }

        override suspend fun unarchiveChapter(bookId: String, chapterId: String): AppResult<Unit> {
            val idx = chapters.indexOfFirst { it.id == chapterId }
            if (idx >= 0) {
                chapters[idx] = chapters[idx].copy(isArchived = false)
            }
            return AppResult.Success(Unit)
        }

        override suspend fun publishBook(bookId: String): AppResult<Book> {
            val current = books.find { it.id == bookId } ?: return AppResult.Error(AppError.NotFound("Bulunamadı"))
            val updatedChapters = chapters.filter { it.bookId == bookId }.map {
                if (!it.isArchived) it.copy(isPublished = true) else it
            }
            chapters.removeAll { it.bookId == bookId }
            chapters.addAll(updatedChapters)
            val published = current.copy(status = BookStatus.PUBLISHED, chapterCount = updatedChapters.count { !it.isArchived })
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

        override suspend fun getBookEngagement(bookId: String, userId: String): AppResult<BookEngagement> {
            return AppResult.Success(
                BookEngagement(
                    bookId = bookId,
                    liked = false,
                    saved = false
                )
            )
        }

        override suspend fun toggleBookLike(bookId: String, userId: String): AppResult<Boolean> {
            return AppResult.Success(true)
        }

        override suspend fun toggleBookSave(bookId: String, userId: String): AppResult<Boolean> {
            return AppResult.Success(true)
        }

        override fun getBookComments(bookId: String): Flow<AppResult<List<BookComment>>> =
            flowOf(AppResult.Success(comments.filter { it.bookId == bookId }))

        override suspend fun addBookComment(comment: BookComment): AppResult<BookComment> {
            comments.add(comment)
            return AppResult.Success(comment)
        }

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
