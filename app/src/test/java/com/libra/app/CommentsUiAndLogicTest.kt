package com.libra.app

import com.libra.app.core.result.AppError
import com.libra.app.core.result.AppResult
import com.libra.app.domain.model.Book
import com.libra.app.domain.model.BookCategory
import com.libra.app.domain.model.BookComment
import com.libra.app.domain.model.BookEngagement
import com.libra.app.domain.model.BookStatus
import com.libra.app.domain.model.Chapter
import com.libra.app.domain.model.Post
import com.libra.app.domain.model.PostComment
import com.libra.app.domain.model.ReadingProgress
import com.libra.app.domain.model.ShelfType
import com.libra.app.domain.model.StorageUploadRequest
import com.libra.app.domain.model.Story
import com.libra.app.domain.model.UserModeration
import com.libra.app.domain.model.UserProfile
import com.libra.app.domain.model.UserShelfItem
import com.libra.app.domain.repository.AuthRepository
import com.libra.app.domain.repository.BookRepository
import com.libra.app.domain.repository.PostRepository
import com.libra.app.domain.repository.StorageRepository
import com.libra.app.domain.repository.StoryRepository
import com.libra.app.domain.repository.UserRepository
import com.libra.app.feature.book.BookDetailViewModel
import com.libra.app.feature.discover.BookReelsDiscoverViewModel
import com.libra.app.feature.home.HomeViewModel
import com.libra.app.ui.components.CommentSortOrder
import com.libra.app.ui.components.LIBRA_COMMENT_QUICK_EMOJIS
import com.libra.app.ui.components.LibraCommentItem
import com.libra.app.ui.components.appendEmojiToCommentDraft
import com.libra.app.ui.components.buildCommentThreads
import com.libra.app.ui.components.formatCommentRelativeTime
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
class CommentsUiAndLogicTest {

    private val testDispatcher = StandardTestDispatcher()

    private val authorUser = UserProfile(
        uid = "uid_author",
        displayName = "Orhan Pamuk",
        username = "orhanpamuk",
        email = "orhan@libra.app"
    )

    private val readerUser = UserProfile(
        uid = "uid_reader",
        displayName = "Elif Kaya",
        username = "elifk",
        email = "elif@libra.app"
    )

    private val mutedUser = readerUser.copy(
        uid = "uid_muted",
        moderation = UserModeration(commentingDisabled = true)
    )

    private val sampleBook = Book(
        id = "book_masumiyet",
        ownerId = authorUser.uid,
        authorName = authorUser.displayName,
        title = "Masumiyet Müzesi",
        category = BookCategory.FICTION,
        status = BookStatus.PUBLISHED,
        chapterCount = 3,
        likesCount = 19,
        commentsCount = 4
    )

    private val samplePost = Post(
        id = "post_1",
        authorId = authorUser.uid,
        authorName = authorUser.displayName,
        authorUsername = authorUser.username,
        title = "Yazım Süreci",
        text = "Bugün yeni bölümü bitirdim.",
        commentsCount = 2,
        createdAt = 1_000L
    )

    private lateinit var fakeAuthRepo: FakeAuthRepo
    private lateinit var fakeBookRepo: FakeBookRepo
    private lateinit var fakePostRepo: FakePostRepo
    private lateinit var fakeUserRepo: FakeUserRepo
    private lateinit var fakeStoryRepo: FakeStoryRepo
    private lateinit var fakeStorageRepo: FakeStorageRepo

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeAuthRepo = FakeAuthRepo(readerUser)
        fakeBookRepo = FakeBookRepo(listOf(sampleBook))
        fakePostRepo = FakePostRepo(listOf(samplePost))
        fakeUserRepo = FakeUserRepo(listOf(authorUser, readerUser, mutedUser))
        fakeStoryRepo = FakeStoryRepo()
        fakeStorageRepo = FakeStorageRepo()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun buildCommentThreads_groupsMultiLevelRepliesChronologically_andSortsRoots() {
        val c1 = LibraCommentItem(
            id = "c1",
            authorId = readerUser.uid,
            authorName = "Elif Kaya",
            authorUsername = "elifk",
            authorPhotoUrl = "",
            text = "İlk kök yorum",
            createdAt = 1_000L
        )
        val c2 = LibraCommentItem(
            id = "c2",
            authorId = authorUser.uid,
            authorName = "Orhan Pamuk",
            authorUsername = "orhanpamuk",
            authorPhotoUrl = "",
            text = "İkinci kök yorum",
            createdAt = 3_000L,
            isContentAuthor = true
        )
        val replyToC1 = LibraCommentItem(
            id = "r1",
            authorId = authorUser.uid,
            authorName = "Orhan Pamuk",
            authorUsername = "orhanpamuk",
            authorPhotoUrl = "",
            text = "Teşekkürler!",
            parentCommentId = "c1",
            createdAt = 2_000L,
            isContentAuthor = true
        )
        val replyToReply = LibraCommentItem(
            id = "r2",
            authorId = readerUser.uid,
            authorName = "Elif Kaya",
            authorUsername = "elifk",
            authorPhotoUrl = "",
            text = "Rica ederim",
            parentCommentId = "r1",
            createdAt = 2_500L
        )

        val newestFirst = buildCommentThreads(
            listOf(c1, replyToReply, c2, replyToC1),
            CommentSortOrder.NEWEST
        )
        assertEquals(2, newestFirst.size)
        assertEquals("c2", newestFirst[0].root.id)
        assertEquals("c1", newestFirst[1].root.id)
        assertEquals(listOf("r1", "r2"), newestFirst[1].replies.map { it.id })
        assertEquals("elifk", newestFirst[1].replyTargetLabels["r1"])
        assertEquals("orhanpamuk", newestFirst[1].replyTargetLabels["r2"])

        val oldestFirst = buildCommentThreads(
            listOf(c1, replyToReply, c2, replyToC1),
            CommentSortOrder.OLDEST
        )
        assertEquals(listOf("c1", "c2"), oldestFirst.map { it.root.id })
    }

    @Test
    fun buildCommentThreads_promotesOrphanedReplies_andGuardsCycles() {
        val orphan = LibraCommentItem(
            id = "orphan_1",
            authorId = "u1",
            authorName = "Ayşe",
            authorUsername = "ayse",
            authorPhotoUrl = "",
            text = "Silinen yoruma yanıt",
            parentCommentId = "deleted_parent",
            createdAt = 100L
        )
        val cycleA = LibraCommentItem(
            id = "cyc_a",
            authorId = "u1",
            authorName = "A",
            authorUsername = "a",
            authorPhotoUrl = "",
            text = "A",
            parentCommentId = "cyc_b",
            createdAt = 200L
        )
        val cycleB = LibraCommentItem(
            id = "cyc_b",
            authorId = "u2",
            authorName = "B",
            authorUsername = "b",
            authorPhotoUrl = "",
            text = "B",
            parentCommentId = "cyc_a",
            createdAt = 300L
        )

        val threads = buildCommentThreads(listOf(orphan, cycleA, cycleB), CommentSortOrder.OLDEST)
        assertTrue(threads.any { it.root.id == "orphan_1" })
        // Total comments across roots + replies stays bounded without infinite recursion
        val totalRendered = threads.sumOf { 1 + it.replies.size }
        assertTrue(totalRendered <= 3)
    }

    @Test
    fun formatCommentRelativeTime_returnsTurkishLabels() {
        val now = 10_000_000_000L
        assertEquals("", formatCommentRelativeTime(0L, now))
        assertEquals("Şimdi", formatCommentRelativeTime(now - 25_000L, now))
        assertEquals("14 dk", formatCommentRelativeTime(now - 14 * 60_000L, now))
        assertEquals("5 sa", formatCommentRelativeTime(now - 5 * 3_600_000L, now))
        assertEquals("3 gün", formatCommentRelativeTime(now - 3 * 86_400_000L, now))
        assertEquals("2 hf", formatCommentRelativeTime(now - 14 * 86_400_000L, now))
    }

    @Test
    fun appendEmojiToCommentDraft_respectsMaxCharsLimit() {
        assertTrue(LIBRA_COMMENT_QUICK_EMOJIS.isNotEmpty())
        val updated = appendEmojiToCommentDraft("Harika kitap ", "📚", maxChars = 500)
        assertEquals("Harika kitap 📚", updated)

        val almostFull = "a".repeat(499)
        val rejected = appendEmojiToCommentDraft(almostFull, "📚", maxChars = 500)
        assertEquals(almostFull, rejected)
    }

    @Test
    fun bookReelsDiscoverViewModel_initializesCommentsCount_addsReply_deletes_andChecksModeration() = runTest {
        fakeBookRepo.seedComments(
            sampleBook.id,
            listOf(
                BookComment(
                    id = "bc_1",
                    bookId = sampleBook.id,
                    userId = authorUser.uid,
                    authorName = authorUser.displayName,
                    authorUsername = authorUser.username,
                    text = "Okurlara merhaba!",
                    createdAt = 1000L
                )
            )
        )

        val reelsVm = BookReelsDiscoverViewModel(fakeBookRepo, fakeAuthRepo)
        advanceUntilIdle()

        assertEquals(sampleBook.commentsCount, reelsVm.state.value.engagements[sampleBook.id]?.commentsCount)

        reelsVm.openComments(sampleBook.id)
        advanceUntilIdle()
        assertFalse(reelsVm.state.value.isLoadingComments)
        assertEquals(1, reelsVm.state.value.comments.size)
        assertEquals(1, reelsVm.state.value.engagements[sampleBook.id]?.commentsCount)

        // Add threaded reply
        reelsVm.addComment(sampleBook.id, "Çok beğendim!", parentCommentId = "bc_1")
        advanceUntilIdle()
        assertEquals(2, reelsVm.state.value.comments.size)
        assertTrue(reelsVm.state.value.comments.any { it.parentCommentId == "bc_1" && it.text == "Çok beğendim!" })
        assertFalse(reelsVm.state.value.comments.any { it.authorName == "Libra Okuru" })

        // Delete reply
        val replyId = reelsVm.state.value.comments.first { it.parentCommentId == "bc_1" }.id
        reelsVm.deleteComment(sampleBook.id, replyId)
        advanceUntilIdle()
        assertEquals(1, reelsVm.state.value.comments.size)
        assertEquals(1, reelsVm.state.value.engagements[sampleBook.id]?.commentsCount)

        // Muted user blocked with clear Turkish error
        fakeAuthRepo.setUser(mutedUser)
        reelsVm.addComment(sampleBook.id, "Bu gönderilmemeli")
        advanceUntilIdle()
        assertNotNull(reelsVm.state.value.commentError)
        assertTrue(reelsVm.state.value.commentError!!.contains("kısıtlandı"))
        assertEquals(1, reelsVm.state.value.comments.size)

        reelsVm.closeComments()
        assertNull(reelsVm.state.value.activeCommentsBookId)
    }

    @Test
    fun bookDetailViewModel_observesAddsRepliesAndDeletesBookComments() = runTest {
        val detailVm = BookDetailViewModel(fakeBookRepo, fakeUserRepo, fakeAuthRepo)
        detailVm.load(sampleBook, readerUser.uid)
        advanceUntilIdle()

        assertEquals(0, detailVm.state.value.comments.size)
        assertFalse(detailVm.state.value.isLoadingComments)

        detailVm.addComment("Bölümler akıcı ilerliyor.")
        advanceUntilIdle()
        assertEquals(1, detailVm.state.value.comments.size)
        assertEquals(1, detailVm.state.value.commentsCount)
        val rootId = detailVm.state.value.comments.first().id

        detailVm.addComment("Katılıyorum!", parentCommentId = rootId)
        advanceUntilIdle()
        assertEquals(2, detailVm.state.value.comments.size)
        assertTrue(detailVm.state.value.comments.any { it.parentCommentId == rootId })

        detailVm.deleteComment(rootId)
        advanceUntilIdle()
        assertFalse(detailVm.state.value.comments.any { it.id == rootId })
        assertEquals(1, detailVm.state.value.commentsCount)
    }

    @Test
    fun homeViewModel_tracksCommentLoadingAndPassesParentCommentId() = runTest {
        val homeVm = HomeViewModel(
            authRepository = fakeAuthRepo,
            bookRepository = fakeBookRepo,
            postRepository = fakePostRepo,
            storyRepository = fakeStoryRepo,
            storageRepository = fakeStorageRepo
        )
        advanceUntilIdle()

        homeVm.openComments(samplePost)
        advanceUntilIdle()
        assertFalse(homeVm.isLoadingComments.value)

        homeVm.addComment(samplePost, "Harika paylaşım", parentCommentId = "parent_99")
        advanceUntilIdle()
        assertEquals("parent_99", fakePostRepo.lastAddedParentId)
        assertEquals(1, homeVm.comments.value.size)

        homeVm.closeComments()
        assertTrue(homeVm.comments.value.isEmpty())
    }

    // --- Test Fakes ---

    private class FakeAuthRepo(initial: UserProfile?) : AuthRepository {
        private val _currentUser = MutableStateFlow(initial)
        override val currentUser: StateFlow<UserProfile?> = _currentUser.asStateFlow()
        private val _isAuthenticated = MutableStateFlow(initial != null)
        override val isAuthenticated: StateFlow<Boolean> = _isAuthenticated.asStateFlow()

        fun setUser(user: UserProfile?) {
            _currentUser.value = user
            _isAuthenticated.value = user != null
        }

        override suspend fun checkCurrentSession(): AppResult<UserProfile?> = AppResult.Success(_currentUser.value)
        override fun signInWithGoogleIdToken(idToken: String, displayName: String?, email: String?, photoUrl: String?): Flow<AppResult<UserProfile>> =
            flowOf(AppResult.Error(AppError.Auth("Unused")))
        override suspend fun signInWithEmailPassword(email: String, password: String): AppResult<UserProfile> =
            AppResult.Error(AppError.Auth("Unused"))
        override suspend fun createAccountWithEmailPassword(email: String, password: String): AppResult<UserProfile> =
            AppResult.Error(AppError.Auth("Unused"))
        override suspend fun signOut(): AppResult<Unit> = AppResult.Success(Unit)
    }

    private class FakeUserRepo(private val profiles: List<UserProfile>) : UserRepository {
        override fun getUserProfile(uid: String): Flow<AppResult<UserProfile?>> =
            flowOf(AppResult.Success(profiles.firstOrNull { it.uid == uid }))
        override suspend fun getUserProfileFresh(uid: String): AppResult<UserProfile?> =
            AppResult.Success(profiles.firstOrNull { it.uid == uid })
        override suspend fun createOrUpdateProfile(profile: UserProfile): AppResult<UserProfile> = AppResult.Success(profile)
        override suspend fun completeProfile(profile: UserProfile): AppResult<UserProfile> = AppResult.Success(profile)
        override suspend fun isUsernameAvailable(username: String, currentUid: String?): AppResult<Boolean> = AppResult.Success(true)
        override suspend fun updateBio(uid: String, bio: String): AppResult<Unit> = AppResult.Success(Unit)
        override suspend fun updateProfilePhoto(uid: String, photoUrl: String): AppResult<Unit> = AppResult.Success(Unit)
        override suspend fun updateProfileDetails(profile: UserProfile, photoUrl: String?): AppResult<UserProfile> = AppResult.Success(profile)
        override fun searchUsers(query: String): Flow<AppResult<List<UserProfile>>> = flowOf(AppResult.Success(emptyList()))
        override suspend fun getFollowingIds(uid: String): AppResult<Set<String>> = AppResult.Success(emptySet())
        override suspend fun isFollowing(followerId: String, followingId: String): AppResult<Boolean> = AppResult.Success(false)
        override suspend fun followUser(followerId: String, followingId: String): AppResult<Unit> = AppResult.Success(Unit)
        override suspend fun unfollowUser(followerId: String, followingId: String): AppResult<Unit> = AppResult.Success(Unit)
        override suspend fun getFollowers(uid: String): AppResult<List<UserProfile>> = AppResult.Success(emptyList())
        override suspend fun getFollowing(uid: String): AppResult<List<UserProfile>> = AppResult.Success(emptyList())
    }

    private class FakeBookRepo(private val initialBooks: List<Book>) : BookRepository {
        private val commentsFlows = mutableMapOf<String, MutableStateFlow<AppResult<List<BookComment>>>>()

        private fun commentFlowFor(bookId: String): MutableStateFlow<AppResult<List<BookComment>>> =
            commentsFlows.getOrPut(bookId) { MutableStateFlow(AppResult.Success(emptyList())) }

        fun seedComments(bookId: String, items: List<BookComment>) {
            commentFlowFor(bookId).value = AppResult.Success(items)
        }

        override fun getFeaturedBooks(): Flow<AppResult<List<Book>>> = flowOf(AppResult.Success(initialBooks))
        override fun getRecentBooks(): Flow<AppResult<List<Book>>> = flowOf(AppResult.Success(initialBooks))
        override fun getBooksByCategory(category: BookCategory): Flow<AppResult<List<Book>>> = flowOf(AppResult.Success(initialBooks))
        override fun getBookById(bookId: String): Flow<AppResult<Book?>> = flowOf(AppResult.Success(initialBooks.firstOrNull { it.id == bookId }))
        override fun getUserLibrary(userId: String, shelfType: ShelfType): Flow<AppResult<List<UserShelfItem>>> = flowOf(AppResult.Success(emptyList()))
        override fun getUserWrittenBooks(userId: String): Flow<AppResult<List<Book>>> = flowOf(AppResult.Success(emptyList()))
        override suspend fun createBook(book: Book): AppResult<Book> = AppResult.Success(book)
        override suspend fun updateBook(book: Book): AppResult<Book> = AppResult.Success(book)
        override suspend fun deleteBook(bookId: String): AppResult<Unit> = AppResult.Success(Unit)
        override suspend fun addBookToShelf(userId: String, bookId: String, shelfType: ShelfType): AppResult<Unit> = AppResult.Success(Unit)
        override suspend fun removeBookFromShelf(userId: String, bookId: String, shelfType: ShelfType): AppResult<Unit> = AppResult.Success(Unit)
        override fun getBookChapters(bookId: String): Flow<AppResult<List<Chapter>>> = flowOf(AppResult.Success(emptyList()))
        override suspend fun saveChapter(chapter: Chapter): AppResult<Chapter> = AppResult.Success(chapter)
        override suspend fun deleteChapter(bookId: String, chapterId: String): AppResult<Unit> = AppResult.Success(Unit)
        override suspend fun getBookEngagement(bookId: String, userId: String): AppResult<BookEngagement> =
            AppResult.Success(BookEngagement(bookId = bookId))
        override suspend fun toggleBookLike(bookId: String, userId: String): AppResult<Boolean> = AppResult.Success(true)
        override suspend fun toggleBookSave(bookId: String, userId: String): AppResult<Boolean> = AppResult.Success(true)
        override fun getBookComments(bookId: String): Flow<AppResult<List<BookComment>>> = commentFlowFor(bookId)
        override suspend fun addBookComment(comment: BookComment): AppResult<BookComment> {
            val saved = comment.copy(id = comment.id.ifBlank { "bc_${System.nanoTime()}" })
            val flow = commentFlowFor(comment.bookId)
            val current = (flow.value as? AppResult.Success)?.data.orEmpty()
            flow.value = AppResult.Success(listOf(saved) + current)
            return AppResult.Success(saved)
        }
        override suspend fun deleteBookComment(bookId: String, commentId: String): AppResult<Unit> {
            val flow = commentFlowFor(bookId)
            val current = (flow.value as? AppResult.Success)?.data.orEmpty()
            flow.value = AppResult.Success(current.filterNot { it.id == commentId })
            return AppResult.Success(Unit)
        }
        override suspend fun saveReadingProgress(progress: ReadingProgress): AppResult<ReadingProgress> = AppResult.Success(progress)
        override fun getReadingProgress(userId: String, bookId: String): Flow<AppResult<ReadingProgress?>> = flowOf(AppResult.Success(null))
    }

    private class FakePostRepo(private val posts: List<Post>) : PostRepository {
        private val commentsFlow = MutableStateFlow<AppResult<List<PostComment>>>(AppResult.Success(emptyList()))
        var lastAddedParentId: String = ""

        override fun observeFeed(currentUserId: String, limit: Long): Flow<AppResult<List<Post>>> = flowOf(AppResult.Success(posts))
        override suspend fun createPost(authorId: String, text: String, title: String, mediaUrl: String, mediaType: String, tags: List<String>): AppResult<Post> =
            AppResult.Success(posts.first())
        override suspend fun toggleLike(postId: String, userId: String): AppResult<Boolean> = AppResult.Success(true)
        override suspend fun getPostLikeUsers(postId: String): AppResult<List<UserProfile>> = AppResult.Success(emptyList())
        override suspend fun deletePost(postId: String, userId: String): AppResult<Unit> = AppResult.Success(Unit)
        override fun observeComments(postId: String, limit: Long): Flow<AppResult<List<PostComment>>> = commentsFlow
        override suspend fun addComment(postId: String, authorId: String, text: String, parentCommentId: String): AppResult<PostComment> {
            lastAddedParentId = parentCommentId
            val comment = PostComment(
                id = "pc_${System.nanoTime()}",
                postId = postId,
                authorId = authorId,
                authorName = "Elif Kaya",
                authorUsername = "elifk",
                text = text,
                parentCommentId = parentCommentId,
                createdAt = System.currentTimeMillis()
            )
            val existing = (commentsFlow.value as? AppResult.Success)?.data.orEmpty()
            commentsFlow.value = AppResult.Success(existing + comment)
            return AppResult.Success(comment)
        }
        override suspend fun deleteComment(postId: String, commentId: String, userId: String): AppResult<Unit> = AppResult.Success(Unit)
        override suspend fun toggleSave(postId: String, userId: String): AppResult<Boolean> = AppResult.Success(true)
        override fun observeSavedPosts(userId: String, limit: Long): Flow<AppResult<List<Post>>> = flowOf(AppResult.Success(emptyList()))
    }

    private class FakeStoryRepo : StoryRepository {
        override suspend fun createStory(authorId: String, authorName: String, authorPhotoUrl: String, text: String, mediaUrl: String, mediaType: String): AppResult<Unit> =
            AppResult.Success(Unit)
        override suspend fun getActiveStories(authorIds: Set<String>): AppResult<List<Story>> = AppResult.Success(emptyList())
        override suspend fun isLiked(storyId: String, userId: String): AppResult<Boolean> = AppResult.Success(false)
        override suspend fun toggleLike(storyId: String, userId: String): AppResult<Boolean> = AppResult.Success(true)
    }

    private class FakeStorageRepo : StorageRepository {
        override fun uploadMedia(request: StorageUploadRequest, onProgress: (Int) -> Unit): Flow<AppResult<String>> =
            flowOf(AppResult.Success(""))
        override suspend fun deleteMedia(fileKey: String): AppResult<Unit> = AppResult.Success(Unit)
        override fun getPublicCdnUrl(fileKey: String): String = ""
        override suspend fun getSignedMediaUrl(fileKey: String): String = ""
    }
}
