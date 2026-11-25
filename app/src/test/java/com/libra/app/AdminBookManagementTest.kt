package com.libra.app

import android.net.Uri
import com.libra.app.core.result.AppError
import com.libra.app.core.result.AppResult
import com.libra.app.domain.model.AdminRole
import com.libra.app.domain.model.AppNotification
import com.libra.app.domain.model.AppReleaseInfo
import com.libra.app.domain.model.AppUpdate
import com.libra.app.domain.model.Book
import com.libra.app.domain.model.BookCategory
import com.libra.app.domain.model.BookStatus
import com.libra.app.domain.model.Chapter
import com.libra.app.domain.model.CosmeticRole
import com.libra.app.domain.model.Post
import com.libra.app.domain.model.PostComment
import com.libra.app.domain.model.ShelfType
import com.libra.app.domain.model.UserModeration
import com.libra.app.domain.model.UserProfile
import com.libra.app.domain.model.UserShelfItem
import com.libra.app.domain.repository.AdminRepository
import com.libra.app.domain.repository.AppReleaseStorageRepository
import com.libra.app.domain.repository.BookRepository
import com.libra.app.domain.repository.NotificationRepository
import com.libra.app.domain.repository.PostRepository
import com.libra.app.domain.repository.UpdateRepository
import com.libra.app.feature.admin.AdminBookAudienceFilter
import com.libra.app.feature.admin.AdminBookSortOption
import com.libra.app.feature.admin.AdminBookStatusFilter
import com.libra.app.feature.admin.AdminBookVisibilityFilter
import com.libra.app.feature.admin.AdminViewModel
import com.libra.app.feature.admin.filterAndSortAdminBooks
import com.libra.app.feature.admin.summarizeAdminBookCategories
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
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
class AdminBookManagementTest {

    private val testDispatcher = StandardTestDispatcher()

    private val author1 = UserProfile(
        uid = "uid_author_1",
        displayName = "Orhan Kemal",
        username = "orhankemal",
        email = "orhan@libra.app"
    )
    private val author2 = UserProfile(
        uid = "uid_author_2",
        displayName = "Peyami Safa",
        username = "peyamisafa",
        email = "peyami@libra.app"
    )

    private val book1 = Book(
        id = "bk_1",
        ownerId = "uid_author_1",
        authorName = "Orhan Kemal",
        title = "Bereketli Topraklar Üzerinde",
        category = BookCategory.CLASSICS,
        status = BookStatus.PUBLISHED,
        visibility = "PUBLIC",
        targetAudience = "ALL",
        genre = "Roman",
        subGenre = "Toplumsal Gerçekçilik",
        tags = listOf("anadolu", "emek"),
        readCount = 850,
        likesCount = 120,
        chapterCount = 14,
        updatedAt = 3000L
    )

    private val book2 = Book(
        id = "bk_2",
        ownerId = "uid_author_2",
        authorName = "Peyami Safa",
        title = "Dokuzuncu Hariciye Koğuşu",
        category = BookCategory.FICTION,
        status = BookStatus.DRAFT,
        visibility = "PRIVATE",
        targetAudience = "YA",
        genre = "Psikolojik Roman",
        tags = listOf("hastane", "gençlik"),
        isSeries = true,
        seriesName = "Klasik Seçki",
        seriesOrder = 2,
        readCount = 120,
        likesCount = 35,
        chapterCount = 4,
        updatedAt = 5000L
    )

    private val book3 = Book(
        id = "bk_3",
        ownerId = "uid_author_1",
        authorName = "Orhan Kemal",
        title = "Arşivdeki Deneme",
        category = BookCategory.SCIFI_FANTASY,
        status = BookStatus.ARCHIVED,
        visibility = "UNLISTED",
        targetAudience = "ADULT",
        genre = "Bilim Kurgu",
        tags = listOf("uzay"),
        readCount = 40,
        likesCount = 5,
        chapterCount = 2,
        updatedAt = 1000L
    )

    private lateinit var fakeAdminRepo: FakeAdminRepo
    private lateinit var fakeBookRepo: FakeAdminBookRepo
    private lateinit var fakePostRepo: FakeAdminPostRepo
    private lateinit var fakeUpdateRepo: FakeUpdateRepo
    private lateinit var fakeReleaseStorageRepo: FakeReleaseStorageRepo
    private lateinit var fakeNotificationRepo: FakeNotificationRepo

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeAdminRepo = FakeAdminRepo(listOf(author1, author2))
        fakeBookRepo = FakeAdminBookRepo(listOf(book1, book2, book3))
        fakePostRepo = FakeAdminPostRepo()
        fakeUpdateRepo = FakeUpdateRepo()
        fakeReleaseStorageRepo = FakeReleaseStorageRepo()
        fakeNotificationRepo = FakeNotificationRepo()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createVm() = AdminViewModel(
        repo = fakeAdminRepo,
        bookRepo = fakeBookRepo,
        postRepo = fakePostRepo,
        updateRepo = fakeUpdateRepo,
        releaseStorageRepo = fakeReleaseStorageRepo,
        notificationRepo = fakeNotificationRepo
    )

    @Test
    fun filterAndSortAdminBooks_filtersByStatusCategoryVisibilityAudienceAndQuery() {
        val all = listOf(book1, book2, book3)

        assertEquals(listOf("bk_1"), filterAndSortAdminBooks(all, statusFilter = AdminBookStatusFilter.PUBLISHED).map { it.id })
        assertEquals(listOf("bk_2"), filterAndSortAdminBooks(all, statusFilter = AdminBookStatusFilter.DRAFT).map { it.id })
        assertEquals(listOf("bk_3"), filterAndSortAdminBooks(all, statusFilter = AdminBookStatusFilter.ARCHIVED).map { it.id })
        assertEquals(listOf("bk_2", "bk_3"), filterAndSortAdminBooks(all, statusFilter = AdminBookStatusFilter.RESTRICTED_VISIBILITY).map { it.id })

        assertEquals(listOf("bk_2"), filterAndSortAdminBooks(all, categoryFilter = BookCategory.FICTION).map { it.id })
        assertEquals(listOf("bk_3"), filterAndSortAdminBooks(all, visibilityFilter = AdminBookVisibilityFilter.UNLISTED).map { it.id })
        assertEquals(listOf("bk_2"), filterAndSortAdminBooks(all, audienceFilter = AdminBookAudienceFilter.YA).map { it.id })
        assertEquals(
            listOf("bk_tr"),
            filterAndSortAdminBooks(
                listOf(book1.copy(id = "bk_tr", targetAudience = "Genç Yetişkin (13+)")),
                audienceFilter = AdminBookAudienceFilter.YA
            ).map { it.id }
        )
        assertEquals(listOf("bk_1", "bk_3"), filterAndSortAdminBooks(all, ownerFilterUid = "uid_author_1").map { it.id })

        // Search by tag, seriesName, and subGenre
        assertEquals(listOf("bk_1"), filterAndSortAdminBooks(all, query = "toplumsal").map { it.id })
        assertEquals(listOf("bk_2"), filterAndSortAdminBooks(all, query = "seçki").map { it.id })
        assertEquals(listOf("bk_3"), filterAndSortAdminBooks(all, query = "uzay").map { it.id })

        // Sorting
        assertEquals(listOf("bk_2", "bk_1", "bk_3"), filterAndSortAdminBooks(all, sortOption = AdminBookSortOption.RECENTLY_UPDATED).map { it.id })
        assertEquals(listOf("bk_1", "bk_2", "bk_3"), filterAndSortAdminBooks(all, sortOption = AdminBookSortOption.MOST_READ).map { it.id })
        assertEquals(listOf("bk_1", "bk_2", "bk_3"), filterAndSortAdminBooks(all, sortOption = AdminBookSortOption.MOST_LIKED).map { it.id })
        assertEquals(listOf("bk_1", "bk_2", "bk_3"), filterAndSortAdminBooks(all, sortOption = AdminBookSortOption.MOST_CHAPTERS).map { it.id })
        assertEquals(listOf("bk_3", "bk_1", "bk_2"), filterAndSortAdminBooks(all, sortOption = AdminBookSortOption.TITLE_ASC).map { it.id })
    }

    @Test
    fun summarizeAdminBookCategories_returnsAllCategoriesWithAccurateMetrics() {
        val summaries = summarizeAdminBookCategories(listOf(book1, book2, book3))
        assertEquals(BookCategory.entries.size, summaries.size)

        val classics = summaries.first { it.category == BookCategory.CLASSICS }
        assertEquals(1, classics.totalCount)
        assertEquals(1, classics.publishedCount)
        assertEquals(850, classics.totalReads)

        val fiction = summaries.first { it.category == BookCategory.FICTION }
        assertEquals(1, fiction.totalCount)
        assertEquals(1, fiction.draftCount)
        assertEquals(120, fiction.totalReads)

        val scifi = summaries.first { it.category == BookCategory.SCIFI_FANTASY }
        assertEquals(1, scifi.totalCount)
        assertEquals(1, scifi.archivedCount)
    }

    @Test
    fun adminViewModel_categorizeBookPreservesOwnerAndNormalizesTags() = runTest(testDispatcher) {
        val vm = createVm()
        advanceUntilIdle()

        var succeeded = false
        vm.updateBookCategorization(
            book = book2,
            category = BookCategory.MYSTERY_THRILLER,
            genre = "Gerilim",
            subGenre = "Psikolojik",
            tags = listOf("#gizem", "gizem", "roman", " "),
            targetAudience = "ADULT",
            language = "en",
            isSeries = true,
            seriesName = "Yeni Seri",
            seriesOrder = 3
        ) { succeeded = it }
        advanceUntilIdle()

        assertTrue(succeeded)
        val updated = vm.books.value.first { it.id == "bk_2" }
        assertEquals("uid_author_2", updated.ownerId)
        assertEquals(BookCategory.MYSTERY_THRILLER, updated.category)
        assertEquals("Gerilim", updated.genre)
        assertEquals("Psikolojik", updated.subGenre)
        assertEquals(listOf("gizem", "roman"), updated.tags)
        assertEquals("ADULT", updated.targetAudience)
        assertEquals("en", updated.language)
        assertTrue(updated.isSeries)
        assertEquals("Yeni Seri", updated.seriesName)
        assertEquals(3, updated.seriesOrder)
    }

    @Test
    fun adminViewModel_statusVisibilityAndDeleteOperationsUpdateBooks() = runTest(testDispatcher) {
        val vm = createVm()
        advanceUntilIdle()

        vm.publishBookAsAdmin("bk_2")
        advanceUntilIdle()
        assertEquals(BookStatus.PUBLISHED, vm.books.value.first { it.id == "bk_2" }.status)

        vm.unpublishBookAsAdmin("bk_2")
        advanceUntilIdle()
        assertEquals(BookStatus.DRAFT, vm.books.value.first { it.id == "bk_2" }.status)

        vm.archiveBookAsAdmin("bk_1")
        advanceUntilIdle()
        assertEquals(BookStatus.ARCHIVED, vm.books.value.first { it.id == "bk_1" }.status)

        vm.updateBookVisibilityAsAdmin(book1, "PRIVATE", listOf("Hassas İçerik"))
        advanceUntilIdle()
        val updatedVis = vm.books.value.first { it.id == "bk_1" }
        assertEquals("PRIVATE", updatedVis.visibility)
        assertEquals(listOf("Hassas İçerik"), updatedVis.contentWarnings)

        vm.deleteBookAsAdmin("bk_3")
        advanceUntilIdle()
        assertFalse(vm.books.value.any { it.id == "bk_3" })
    }

    @Test
    fun adminViewModel_batchCategorizeStatusAndVisibilityWorkOnMultipleBooks() = runTest(testDispatcher) {
        val vm = createVm()
        advanceUntilIdle()

        var catCount = 0
        vm.batchCategorizeBooks(setOf("bk_1", "bk_2"), BookCategory.PHILOSOPHY) { catCount = it }
        advanceUntilIdle()
        assertEquals(2, catCount)
        assertEquals(BookCategory.PHILOSOPHY, vm.books.value.first { it.id == "bk_1" }.category)
        assertEquals(BookCategory.PHILOSOPHY, vm.books.value.first { it.id == "bk_2" }.category)

        var statusCount = 0
        vm.batchSetBookStatus(setOf("bk_2", "bk_3"), BookStatus.PUBLISHED) { statusCount = it }
        advanceUntilIdle()
        assertEquals(2, statusCount)
        assertEquals(BookStatus.PUBLISHED, vm.books.value.first { it.id == "bk_2" }.status)
        assertEquals(BookStatus.PUBLISHED, vm.books.value.first { it.id == "bk_3" }.status)

        var visCount = 0
        vm.batchSetBookVisibility(setOf("bk_1", "bk_3"), "UNLISTED") { visCount = it }
        advanceUntilIdle()
        assertEquals(2, visCount)
        assertEquals("UNLISTED", vm.books.value.first { it.id == "bk_1" }.visibility)
        assertEquals("UNLISTED", vm.books.value.first { it.id == "bk_3" }.visibility)
    }

    @Test
    fun adminViewModel_toggleAuthorBookPublishingAndSendNotice() = runTest(testDispatcher) {
        val vm = createVm()
        advanceUntilIdle()

        vm.toggleAuthorBookPublishing("uid_author_1", disabled = true)
        advanceUntilIdle()
        val restricted = vm.members.value.first { it.uid == "uid_author_1" }
        assertTrue(restricted.moderation.bookPublishingDisabled)

        vm.sendBookModerationNotification(
            recipientUid = "uid_author_1",
            actorUid = "admin_uid",
            title = "Kategori Güncellemesi",
            body = "Kitabınız Klasikler kategorisine taşındı.",
            bookId = "bk_1"
        )
        advanceUntilIdle()
        val sent = fakeNotificationRepo.sent.lastOrNull()
        assertNotNull(sent)
        assertEquals("uid_author_1", sent!!.recipientId)
        assertEquals("ADMIN_BOOK_NOTICE", sent.type)
        assertEquals("bk_1", sent.referenceId)
    }

    // --- Fakes ---

    private class FakeAdminRepo(initialMembers: List<UserProfile>) : AdminRepository {
        private val membersFlow = MutableStateFlow<AppResult<List<UserProfile>>>(AppResult.Success(initialMembers))
        override fun observeMembers(): Flow<AppResult<List<UserProfile>>> = membersFlow
        override suspend fun updateMemberProfile(profile: UserProfile): AppResult<Unit> = AppResult.Success(Unit)
        override suspend fun updateModeration(uid: String, moderation: UserModeration): AppResult<Unit> {
            val current = (membersFlow.value as AppResult.Success).data
            membersFlow.value = AppResult.Success(current.map { if (it.uid == uid) it.copy(moderation = moderation) else it })
            return AppResult.Success(Unit)
        }
        override suspend fun getAdminRole(uid: String): AppResult<AdminRole?> = AppResult.Success(null)
        override suspend fun setAdminRole(uid: String, role: AdminRole?): AppResult<Unit> = AppResult.Success(Unit)
        override fun observeCosmeticRoles(): Flow<AppResult<List<CosmeticRole>>> = flowOf(AppResult.Success(emptyList()))
        override suspend fun saveCosmeticRole(role: CosmeticRole): AppResult<Unit> = AppResult.Success(Unit)
        override suspend fun deleteCosmeticRole(roleId: String): AppResult<Unit> = AppResult.Success(Unit)
        override suspend fun assignCosmeticRole(uid: String, roleId: String, assigned: Boolean): AppResult<Unit> = AppResult.Success(Unit)
    }

    private class FakeAdminBookRepo(initialBooks: List<Book>) : BookRepository {
        private val booksFlow = MutableStateFlow<AppResult<List<Book>>>(AppResult.Success(initialBooks))

        private fun currentBooks(): List<Book> = (booksFlow.value as AppResult.Success).data

        override fun observeAllBooksForAdmin(): Flow<AppResult<List<Book>>> = booksFlow
        override fun getFeaturedBooks(): Flow<AppResult<List<Book>>> = booksFlow
        override fun getRecentBooks(): Flow<AppResult<List<Book>>> = booksFlow
        override fun getBooksByCategory(category: BookCategory): Flow<AppResult<List<Book>>> =
            flowOf(AppResult.Success(currentBooks().filter { it.category == category }))
        override fun getBookById(bookId: String): Flow<AppResult<Book?>> =
            flowOf(AppResult.Success(currentBooks().firstOrNull { it.id == bookId }))
        override fun getUserLibrary(userId: String, shelfType: ShelfType): Flow<AppResult<List<UserShelfItem>>> =
            flowOf(AppResult.Success(emptyList()))
        override fun getUserWrittenBooks(userId: String): Flow<AppResult<List<Book>>> =
            flowOf(AppResult.Success(currentBooks().filter { it.ownerId == userId }))
        override suspend fun createBook(book: Book): AppResult<Book> = AppResult.Success(book)
        override suspend fun updateBook(book: Book): AppResult<Book> {
            booksFlow.value = AppResult.Success(currentBooks().map { if (it.id == book.id) book else it })
            return AppResult.Success(book)
        }
        override suspend fun deleteBook(bookId: String): AppResult<Unit> {
            booksFlow.value = AppResult.Success(currentBooks().filterNot { it.id == bookId })
            return AppResult.Success(Unit)
        }
        override suspend fun addBookToShelf(userId: String, bookId: String, shelfType: ShelfType): AppResult<Unit> = AppResult.Success(Unit)
        override suspend fun removeBookFromShelf(userId: String, bookId: String, shelfType: ShelfType): AppResult<Unit> = AppResult.Success(Unit)
        override fun getBookChapters(bookId: String): Flow<AppResult<List<Chapter>>> = flowOf(AppResult.Success(emptyList()))
        override suspend fun saveChapter(chapter: Chapter): AppResult<Chapter> = AppResult.Success(chapter)
        override suspend fun deleteChapter(bookId: String, chapterId: String): AppResult<Unit> = AppResult.Success(Unit)
        override suspend fun publishBook(bookId: String): AppResult<Book> {
            val target = currentBooks().firstOrNull { it.id == bookId } ?: return AppResult.Error(AppError.NotFound("Yok"))
            val updated = target.copy(status = BookStatus.PUBLISHED)
            updateBook(updated)
            return AppResult.Success(updated)
        }
        override suspend fun unpublishBook(bookId: String): AppResult<Book> {
            val target = currentBooks().firstOrNull { it.id == bookId } ?: return AppResult.Error(AppError.NotFound("Yok"))
            val updated = target.copy(status = BookStatus.DRAFT)
            updateBook(updated)
            return AppResult.Success(updated)
        }
        override suspend fun archiveBook(bookId: String): AppResult<Book> {
            val target = currentBooks().firstOrNull { it.id == bookId } ?: return AppResult.Error(AppError.NotFound("Yok"))
            val updated = target.copy(status = BookStatus.ARCHIVED)
            updateBook(updated)
            return AppResult.Success(updated)
        }
    }

    private class FakeUpdateRepo : UpdateRepository {
        override fun observeActiveRelease(): Flow<AppResult<AppUpdate?>> = flowOf(AppResult.Success(null))
        override suspend fun getActiveRelease(): AppResult<AppUpdate?> = AppResult.Success(null)
        override suspend fun publishRelease(release: AppUpdate): AppResult<Unit> = AppResult.Success(Unit)
    }

    private class FakeReleaseStorageRepo : AppReleaseStorageRepository {
        override suspend fun inspectApk(uri: Uri): AppResult<AppReleaseInfo> = AppResult.Error(AppError.Storage("N/A"))
        override fun uploadApk(uri: Uri, fileName: String, fileSize: Long, onProgress: (Int) -> Unit): Flow<AppResult<String>> =
            flowOf(AppResult.Error(AppError.Storage("N/A")))
        override suspend fun deleteApk(objectKey: String): AppResult<Unit> = AppResult.Success(Unit)
        override suspend fun getSignedApkUrl(objectKey: String): String? = null
        override suspend fun publishPublicRelease(release: AppUpdate): AppResult<Unit> = AppResult.Success(Unit)
        override suspend fun getPublicRelease(): AppResult<AppUpdate?> = AppResult.Success(null)
    }

    private class FakeNotificationRepo : NotificationRepository {
        val sent = mutableListOf<AppNotification>()
        override fun observeNotifications(uid: String, limit: Long): Flow<AppResult<List<AppNotification>>> =
            flowOf(AppResult.Success(emptyList()))
        override suspend fun create(notification: AppNotification): AppResult<Unit> {
            sent.add(notification)
            return AppResult.Success(Unit)
        }
        override suspend fun markRead(notificationId: String): AppResult<Unit> = AppResult.Success(Unit)
        override suspend fun markAllRead(uid: String): AppResult<Unit> = AppResult.Success(Unit)
    }

    private class FakeAdminPostRepo : PostRepository {
        override fun observeFeed(currentUserId: String, limit: Long): Flow<AppResult<List<Post>>> =
            flowOf(AppResult.Success(emptyList()))
        override fun observeAllPostsForAdmin(limit: Long): Flow<AppResult<List<Post>>> =
            flowOf(AppResult.Success(emptyList()))
        override suspend fun createPost(
            authorId: String,
            text: String,
            title: String,
            mediaUrl: String,
            mediaType: String,
            tags: List<String>
        ): AppResult<Post> = AppResult.Error(AppError.Database("N/A"))
        override suspend fun toggleLike(postId: String, userId: String): AppResult<Boolean> = AppResult.Success(true)
        override suspend fun getPostLikeUsers(postId: String): AppResult<List<UserProfile>> = AppResult.Success(emptyList())
        override suspend fun deletePost(postId: String, userId: String): AppResult<Unit> = AppResult.Success(Unit)
        override fun observeComments(postId: String, limit: Long): Flow<AppResult<List<PostComment>>> =
            flowOf(AppResult.Success(emptyList()))
        override suspend fun addComment(postId: String, authorId: String, text: String, parentCommentId: String): AppResult<PostComment> =
            AppResult.Error(AppError.Database("N/A"))
        override suspend fun deleteComment(postId: String, commentId: String, userId: String): AppResult<Unit> = AppResult.Success(Unit)
        override suspend fun toggleSave(postId: String, userId: String): AppResult<Boolean> = AppResult.Success(true)
        override fun observeSavedPosts(userId: String, limit: Long): Flow<AppResult<List<Post>>> =
            flowOf(AppResult.Success(emptyList()))
    }
}
