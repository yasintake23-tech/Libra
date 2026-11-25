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
import com.libra.app.feature.admin.AdminPostFilter
import com.libra.app.feature.admin.AdminPostSortOption
import com.libra.app.feature.admin.AdminViewModel
import com.libra.app.feature.admin.filterAndSortAdminPosts
import com.libra.app.feature.admin.summarizeAdminPostTags
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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AdminPostManagementTest {

    private val testDispatcher = StandardTestDispatcher()

    private val member1 = UserProfile(
        uid = "uid_user_1",
        displayName = "Halide Edip",
        username = "halide",
        email = "halide@libra.app"
    )
    private val member2 = UserProfile(
        uid = "uid_user_2",
        displayName = "Yakup Kadri",
        username = "yakupkadri",
        email = "yakup@libra.app"
    )

    private val post1 = Post(
        id = "post_1",
        authorId = "uid_user_1",
        authorName = "Halide Edip",
        authorUsername = "halide",
        title = "Ateşten Gömlek Notları",
        text = "Yeni bölüm taslakları üzerinde çalışıyorum.",
        mediaUrl = "https://cdn.example.com/posts/1.jpg",
        mediaType = "image",
        tags = listOf("roman", "tarih"),
        likesCount = 42,
        commentsCount = 5,
        createdAt = 3000L
    )

    private val post2 = Post(
        id = "post_2",
        authorId = "uid_user_2",
        authorName = "Yakup Kadri",
        authorUsername = "yakupkadri",
        title = "",
        text = "Sadece metin olarak paylaşılmış bir okuma düşüncesi.",
        mediaUrl = "",
        mediaType = "",
        tags = listOf("Roman", "deneme"),
        likesCount = 15,
        commentsCount = 12,
        createdAt = 2000L
    )

    private val post3 = Post(
        id = "post_3",
        authorId = "uid_user_1",
        authorName = "Halide Edip",
        authorUsername = "halide",
        title = "Etiketsiz Görsel",
        text = "Kütüphane masamdan kare.",
        mediaUrl = "https://cdn.example.com/posts/3.jpg",
        mediaType = "image",
        tags = emptyList(),
        likesCount = 8,
        commentsCount = 1,
        createdAt = 1000L
    )

    private lateinit var fakeAdminRepo: FakeAdminRepo
    private lateinit var fakeBookRepo: FakeBookRepo
    private lateinit var fakePostRepo: FakePostRepo
    private lateinit var fakeUpdateRepo: FakeUpdateRepo
    private lateinit var fakeReleaseStorageRepo: FakeReleaseStorageRepo
    private lateinit var fakeNotificationRepo: FakeNotificationRepo

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeAdminRepo = FakeAdminRepo(listOf(member1, member2))
        fakeBookRepo = FakeBookRepo()
        fakePostRepo = FakePostRepo(listOf(post1, post2, post3))
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
    fun filterAndSortAdminPosts_handlesMediaTextTagsAuthorQueryAndSorts() {
        val all = listOf(post1, post2, post3)

        assertEquals(listOf("post_1", "post_3"), filterAndSortAdminPosts(all, filter = AdminPostFilter.WITH_MEDIA).map { it.id })
        assertEquals(listOf("post_2"), filterAndSortAdminPosts(all, filter = AdminPostFilter.TEXT_ONLY).map { it.id })
        assertEquals(listOf("post_1", "post_2"), filterAndSortAdminPosts(all, filter = AdminPostFilter.TAGGED).map { it.id })
        assertEquals(listOf("post_1", "post_2"), filterAndSortAdminPosts(all, tagFilter = "#roman").map { it.id })
        assertEquals(listOf("post_1", "post_3"), filterAndSortAdminPosts(all, ownerFilterUid = "uid_user_1").map { it.id })
        assertEquals(listOf("post_2"), filterAndSortAdminPosts(all, query = "yakupkadri").map { it.id })

        assertEquals(listOf("post_1", "post_2", "post_3"), filterAndSortAdminPosts(all, sortOption = AdminPostSortOption.NEWEST).map { it.id })
        assertEquals(listOf("post_3", "post_2", "post_1"), filterAndSortAdminPosts(all, sortOption = AdminPostSortOption.OLDEST).map { it.id })
        assertEquals(listOf("post_1", "post_2", "post_3"), filterAndSortAdminPosts(all, sortOption = AdminPostSortOption.MOST_LIKED).map { it.id })
        assertEquals(listOf("post_2", "post_1", "post_3"), filterAndSortAdminPosts(all, sortOption = AdminPostSortOption.MOST_COMMENTED).map { it.id })
    }

    @Test
    fun summarizeAdminPostTags_aggregatesCaseInsensitively() {
        val summaries = summarizeAdminPostTags(listOf(post1, post2, post3))
        assertEquals(3, summaries.size)

        val roman = summaries.first { it.tag == "roman" }
        assertEquals(2, roman.postCount)
        assertEquals(1, roman.mediaPostCount)
        assertEquals(57, roman.totalLikes)
        assertEquals(17, roman.totalComments)
    }

    @Test
    fun adminViewModel_updateCategorizationBatchTagAndDeletePostAndComment() = runTest(testDispatcher) {
        val vm = createVm()
        advanceUntilIdle()
        assertEquals(3, vm.posts.value.size)

        var updatedOk = false
        vm.updatePostCategorization(
            post = post3,
            title = "Güncellenmiş Masa",
            tags = listOf("#kitap", "okuma", "KİTAP")
        ) { updatedOk = it }
        advanceUntilIdle()
        assertTrue(updatedOk)
        val updatedPost3 = vm.posts.value.first { it.id == "post_3" }
        assertEquals("Güncellenmiş Masa", updatedPost3.title)
        assertEquals(listOf("kitap", "okuma"), updatedPost3.tags)

        var taggedCount = 0
        vm.batchAddTagToPosts(setOf("post_2", "post_3"), "#seçki") { taggedCount = it }
        advanceUntilIdle()
        assertEquals(2, taggedCount)
        assertTrue("seçki" in vm.posts.value.first { it.id == "post_2" }.tags)

        var commentDeleted = false
        vm.deletePostCommentAsAdmin("post_1", "cmt_1", "admin_uid") { commentDeleted = it }
        advanceUntilIdle()
        assertTrue(commentDeleted)
        assertEquals(4, vm.posts.value.first { it.id == "post_1" }.commentsCount)
        assertEquals("admin_uid", fakePostRepo.lastDeleteCommentActor)

        var deleted = false
        vm.deletePostAsAdmin(post2, "admin_uid") { deleted = it }
        advanceUntilIdle()
        assertTrue(deleted)
        assertFalse(vm.posts.value.any { it.id == "post_2" })
        assertEquals("admin_uid", fakePostRepo.lastDeletePostActor)
    }

    @Test
    fun adminViewModel_toggleAuthorPostingAndCommentingPermissions() = runTest(testDispatcher) {
        val vm = createVm()
        advanceUntilIdle()

        var okPost = false
        vm.toggleAuthorPostingPermission("uid_user_1", disabled = true) { okPost = it }
        advanceUntilIdle()
        assertTrue(okPost)
        assertTrue(vm.members.value.first { it.uid == "uid_user_1" }.moderation.postingDisabled)

        var okComment = false
        vm.toggleAuthorCommentingPermission("uid_user_1", disabled = true) { okComment = it }
        advanceUntilIdle()
        assertTrue(okComment)
        val mod = vm.members.value.first { it.uid == "uid_user_1" }.moderation
        assertTrue(mod.postingDisabled)
        assertTrue(mod.commentingDisabled)
    }

    @Test
    fun adminViewModel_sendPostModerationNotificationUsesAdminFeedbackType() = runTest(testDispatcher) {
        val vm = createVm()
        advanceUntilIdle()

        var sentOk = false
        vm.sendPostModerationNotification(
            recipientUid = "uid_user_1",
            actorUid = "admin_uid",
            title = "Etiket Düzenlemesi",
            body = "Gönderinizin etiketleri güncellendi.",
            postId = "post_1"
        ) { sentOk = it }
        advanceUntilIdle()

        assertTrue(sentOk)
        assertEquals(1, fakeNotificationRepo.sent.size)
        val notification = fakeNotificationRepo.sent.first()
        assertEquals("ADMIN_FEEDBACK", notification.type)
        assertEquals("post_1", notification.referenceId)
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

    private class FakeBookRepo : BookRepository {
        override fun observeAllBooksForAdmin(): Flow<AppResult<List<Book>>> = flowOf(AppResult.Success(emptyList()))
        override fun getFeaturedBooks(): Flow<AppResult<List<Book>>> = flowOf(AppResult.Success(emptyList()))
        override fun getRecentBooks(): Flow<AppResult<List<Book>>> = flowOf(AppResult.Success(emptyList()))
        override fun getBooksByCategory(category: BookCategory): Flow<AppResult<List<Book>>> = flowOf(AppResult.Success(emptyList()))
        override fun getBookById(bookId: String): Flow<AppResult<Book?>> = flowOf(AppResult.Success(null))
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
        override suspend fun publishBook(bookId: String): AppResult<Book> = AppResult.Error(AppError.NotFound("Yok"))
        override suspend fun unpublishBook(bookId: String): AppResult<Book> = AppResult.Error(AppError.NotFound("Yok"))
    }

    private class FakePostRepo(initialPosts: List<Post>) : PostRepository {
        private val postsFlow = MutableStateFlow<AppResult<List<Post>>>(AppResult.Success(initialPosts))
        var lastDeletePostActor: String? = null
        var lastDeleteCommentActor: String? = null

        private fun currentPosts(): List<Post> = (postsFlow.value as AppResult.Success).data

        override fun observeFeed(currentUserId: String, limit: Long): Flow<AppResult<List<Post>>> = postsFlow
        override fun observeAllPostsForAdmin(limit: Long): Flow<AppResult<List<Post>>> = postsFlow
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
        override suspend fun deletePost(postId: String, userId: String): AppResult<Unit> {
            lastDeletePostActor = userId
            postsFlow.value = AppResult.Success(currentPosts().filterNot { it.id == postId })
            return AppResult.Success(Unit)
        }
        override fun observeComments(postId: String, limit: Long): Flow<AppResult<List<PostComment>>> =
            flowOf(AppResult.Success(emptyList()))
        override suspend fun addComment(postId: String, authorId: String, text: String, parentCommentId: String): AppResult<PostComment> =
            AppResult.Error(AppError.Database("N/A"))
        override suspend fun deleteComment(postId: String, commentId: String, userId: String): AppResult<Unit> {
            lastDeleteCommentActor = userId
            postsFlow.value = AppResult.Success(
                currentPosts().map { if (it.id == postId) it.copy(commentsCount = (it.commentsCount - 1).coerceAtLeast(0)) else it }
            )
            return AppResult.Success(Unit)
        }
        override suspend fun toggleSave(postId: String, userId: String): AppResult<Boolean> = AppResult.Success(true)
        override fun observeSavedPosts(userId: String, limit: Long): Flow<AppResult<List<Post>>> =
            flowOf(AppResult.Success(emptyList()))
        override suspend fun updatePostCategorization(postId: String, title: String, tags: List<String>): AppResult<Post> {
            val target = currentPosts().firstOrNull { it.id == postId }
                ?: return AppResult.Error(AppError.NotFound("Bulunamadı"))
            val updated = target.copy(
                title = title.trim().take(120),
                tags = tags.map { it.trim().removePrefix("#") }.filter { it.isNotBlank() }.distinctBy { it.lowercase() }.take(10)
            )
            postsFlow.value = AppResult.Success(currentPosts().map { if (it.id == postId) updated else it })
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
}
