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
import com.libra.app.domain.model.CommunityServer
import com.libra.app.domain.model.CosmeticRole
import com.libra.app.domain.model.GlobalChatMessage
import com.libra.app.domain.model.Post
import com.libra.app.domain.model.PostComment
import com.libra.app.domain.model.ServerCategory
import com.libra.app.domain.model.ServerChannel
import com.libra.app.domain.model.ServerMember
import com.libra.app.domain.model.ServerMessage
import com.libra.app.domain.model.SharedContent
import com.libra.app.domain.model.ShelfType
import com.libra.app.domain.model.Story
import com.libra.app.domain.model.UserModeration
import com.libra.app.domain.model.UserProfile
import com.libra.app.domain.model.UserShelfItem
import com.libra.app.domain.repository.AdminRepository
import com.libra.app.domain.repository.AppReleaseStorageRepository
import com.libra.app.domain.repository.BookRepository
import com.libra.app.domain.repository.NotificationRepository
import com.libra.app.domain.repository.PostRepository
import com.libra.app.domain.repository.UpdateRepository
import com.libra.app.feature.admin.AdminGlobalChatFilter
import com.libra.app.feature.admin.AdminGlobalChatSortOption
import com.libra.app.feature.admin.AdminServerFilter
import com.libra.app.feature.admin.AdminServerSortOption
import com.libra.app.feature.admin.AdminStoryFilter
import com.libra.app.feature.admin.AdminStorySortOption
import com.libra.app.feature.admin.AdminViewModel
import com.libra.app.feature.admin.filterAndSortAdminGlobalMessages
import com.libra.app.feature.admin.filterAndSortAdminServers
import com.libra.app.feature.admin.filterAndSortAdminStories
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
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
class AdminModulesManagementTest {

    private val testDispatcher = StandardTestDispatcher()

    private val member1 = UserProfile(uid = "uid_1", displayName = "Ahmet Hamdi", username = "tanpinar")
    private val member2 = UserProfile(uid = "uid_2", displayName = "Oğuz Atay", username = "oguzatay")

    private val nowMs = 10_000_000L

    private val activeStory = Story(
        id = "st_1",
        authorId = "uid_1",
        authorName = "Ahmet Hamdi",
        text = "Saatleri Ayarlama Enstitüsü",
        mediaUrl = "users/uid_1/story1.jpg",
        mediaType = "image",
        createdAt = 5_000_000L,
        expiresAt = 20_000_000L,
        likesCount = 18
    )
    private val expiredStory = Story(
        id = "st_2",
        authorId = "uid_2",
        authorName = "Oğuz Atay",
        text = "Tutunamayanlar notu",
        mediaUrl = "",
        mediaType = "",
        createdAt = 2_000_000L,
        expiresAt = 8_000_000L,
        likesCount = 44
    )

    private val server1 = CommunityServer(
        id = "srv_1",
        name = "Klasik Okurlar",
        description = "Dünya ve Türk klasikleri kulübü",
        ownerId = "uid_1",
        createdAt = 5000L,
        inviteKey = "AbCdEf123",
        avatarUrl = "users/uid_1/srv-avatar.jpg",
        bannerUrl = "users/uid_1/srv-banner.jpg"
    )
    private val server2 = CommunityServer(
        id = "srv_2",
        name = "Bilimkurgu Atölyesi",
        description = "Fantastik ve bilimkurgu tartışmaları",
        ownerId = "uid_2",
        createdAt = 2000L,
        inviteKey = "",
        avatarUrl = "",
        bannerUrl = ""
    )

    private val global1 = GlobalChatMessage(
        id = "gc_1",
        senderId = "uid_1",
        senderName = "Ahmet Hamdi",
        senderUsername = "tanpinar",
        text = "Herkese iyi okumalar!",
        mediaUrl = "users/uid_1/gc1.jpg",
        mediaType = "image",
        createdAt = 4000L,
        reactions = mapOf("u2" to "❤️", "u3" to "👍")
    )
    private val global2 = GlobalChatMessage(
        id = "gc_2",
        senderId = "uid_2",
        senderName = "Oğuz Atay",
        senderUsername = "oguzatay",
        text = "Yeni kitap paylaştım",
        createdAt = 2000L,
        editedAt = 2500L,
        sharedContent = SharedContent(type = "book", id = "bk_1", title = "Tutunamayanlar")
    )

    private lateinit var fakeAdminRepo: FakeAdminRepo
    private lateinit var fakeBookRepo: FakeBookRepo
    private lateinit var fakePostRepo: FakePostRepo
    private lateinit var fakeStoryRepo: FakeAdminStoryRepo
    private lateinit var fakeCommunityRepo: FakeAdminCommunityRepo
    private lateinit var fakeChatRepo: FakeAdminChatRepo
    private lateinit var fakeUpdateRepo: FakeUpdateRepo
    private lateinit var fakeReleaseStorageRepo: FakeReleaseStorageRepo
    private lateinit var fakeNotificationRepo: FakeNotificationRepo

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeAdminRepo = FakeAdminRepo(listOf(member1, member2))
        fakeBookRepo = FakeBookRepo()
        fakePostRepo = FakePostRepo()
        fakeStoryRepo = FakeAdminStoryRepo(listOf(activeStory, expiredStory))
        fakeCommunityRepo = FakeAdminCommunityRepo(
            initialServers = listOf(server1, server2),
            initialMembers = mapOf(
                "srv_1" to listOf(
                    ServerMember(uid = "uid_1", displayName = "Ahmet Hamdi", role = "OWNER"),
                    ServerMember(uid = "uid_2", displayName = "Oğuz Atay", role = "MEMBER")
                )
            ),
            initialCategories = mapOf("srv_1" to listOf(ServerCategory("cat_1", "GENEL", 0))),
            initialChannels = mapOf("srv_1" to listOf(ServerChannel("ch_1", "cat_1", "GENEL", "sohbet", "TEXT", 0)))
        )
        fakeChatRepo = FakeAdminChatRepo(
            initialGlobalMessages = listOf(global1, global2),
            initialServerMessages = listOf(ServerMessage(id = "sm_1", senderId = "uid_2", senderName = "Oğuz Atay", text = "Selam kanal!"))
        )
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
        storyRepo = fakeStoryRepo,
        communityRepo = fakeCommunityRepo,
        chatRepo = fakeChatRepo,
        updateRepo = fakeUpdateRepo,
        releaseStorageRepo = fakeReleaseStorageRepo,
        notificationRepo = fakeNotificationRepo
    )

    @Test
    fun filterAndSortAdminStories_handlesExpiryMediaOwnerAndSorting() {
        val all = listOf(activeStory, expiredStory)
        assertEquals(listOf("st_1"), filterAndSortAdminStories(all, filter = AdminStoryFilter.ACTIVE, nowMs = nowMs).map { it.id })
        assertEquals(listOf("st_2"), filterAndSortAdminStories(all, filter = AdminStoryFilter.EXPIRED, nowMs = nowMs).map { it.id })
        assertEquals(listOf("st_1"), filterAndSortAdminStories(all, filter = AdminStoryFilter.WITH_MEDIA, nowMs = nowMs).map { it.id })
        assertEquals(listOf("st_2"), filterAndSortAdminStories(all, filter = AdminStoryFilter.TEXT_ONLY, nowMs = nowMs).map { it.id })
        assertEquals(listOf("st_2", "st_1"), filterAndSortAdminStories(all, sortOption = AdminStorySortOption.MOST_LIKED, nowMs = nowMs).map { it.id })
    }

    @Test
    fun adminViewModel_storyModerationDeleteBatchAndPermission() = runTest(testDispatcher) {
        val vm = createVm()
        advanceUntilIdle()
        assertEquals(2, vm.stories.value.size)

        var deleted = false
        vm.deleteStoryAsAdmin(activeStory, "admin_uid") { deleted = it }
        advanceUntilIdle()
        assertTrue(deleted)
        assertEquals("admin_uid", fakeStoryRepo.lastDeletedStoryActor)
        assertEquals(listOf("st_2"), vm.stories.value.map { it.id })

        var restricted = false
        vm.toggleAuthorStoryPermission("uid_1", disabled = true) { restricted = it }
        advanceUntilIdle()
        assertTrue(restricted)
        assertTrue(vm.members.value.first { it.uid == "uid_1" }.moderation.storyDisabled)
    }

    @Test
    fun adminViewModel_serverModerationChannelsMediaMembersBansAndMessages() = runTest(testDispatcher) {
        val all = listOf(server1, server2)
        assertEquals(listOf("srv_1"), filterAndSortAdminServers(all, filter = AdminServerFilter.WITH_MEDIA).map { it.id })
        assertEquals(listOf("srv_1"), filterAndSortAdminServers(all, filter = AdminServerFilter.WITH_INVITE).map { it.id })
        assertEquals(listOf("srv_2", "srv_1"), filterAndSortAdminServers(all, sortOption = AdminServerSortOption.NAME_ASC).map { it.id })

        val vm = createVm()
        advanceUntilIdle()
        assertEquals(2, vm.servers.value.size)

        vm.updateServerMetadataAsAdmin("srv_1", "Klasikler Kulübü", "Yeni açıklama")
        advanceUntilIdle()
        assertEquals("Klasikler Kulübü", vm.servers.value.first { it.id == "srv_1" }.name)

        vm.clearServerMediaAsAdmin(vm.servers.value.first { it.id == "srv_1" }, clearAvatar = true, clearBanner = false)
        advanceUntilIdle()
        assertEquals("", vm.servers.value.first { it.id == "srv_1" }.avatarUrl)
        assertTrue("users/uid_1/srv-avatar.jpg" in fakeCommunityRepo.deletedMediaKeys)

        vm.createServerChannelAsAdmin("srv_1", "cat_1", "duyuru")
        advanceUntilIdle()
        assertEquals(2, (vm.observeServerChannels("srv_1").first() as AppResult.Success).data.size)

        vm.banServerMemberAsAdmin("srv_1", "uid_2", "Spam")
        advanceUntilIdle()
        assertEquals(1, (vm.observeServerMembers("srv_1").first() as AppResult.Success).data.size)
        assertEquals(1, (vm.observeServerBans("srv_1").first() as AppResult.Success).data.size)

        vm.unbanServerMemberAsAdmin("srv_1", "uid_2")
        advanceUntilIdle()
        assertTrue((vm.observeServerBans("srv_1").first() as AppResult.Success).data.isEmpty())

        vm.deleteServerMessageAsAdmin("srv_1", "sm_1", "ch_1")
        advanceUntilIdle()
        assertTrue((vm.observeServerMessages("srv_1", "ch_1").first() as AppResult.Success).data.isEmpty())

        vm.deleteServerAsAdmin(server2)
        advanceUntilIdle()
        assertFalse(vm.servers.value.any { it.id == "srv_2" })
    }

    @Test
    fun adminViewModel_globalChatModerationFilteringBatchDeleteAnnouncementAndBans() = runTest(testDispatcher) {
        val all = listOf(global1, global2)
        assertEquals(listOf("gc_1"), filterAndSortAdminGlobalMessages(all, filter = AdminGlobalChatFilter.WITH_MEDIA).map { it.id })
        assertEquals(listOf("gc_2"), filterAndSortAdminGlobalMessages(all, filter = AdminGlobalChatFilter.WITH_SHARED_CONTENT).map { it.id })
        assertEquals(listOf("gc_2"), filterAndSortAdminGlobalMessages(all, filter = AdminGlobalChatFilter.EDITED).map { it.id })
        assertEquals(listOf("gc_1"), filterAndSortAdminGlobalMessages(all, filter = AdminGlobalChatFilter.WITH_REACTIONS).map { it.id })
        assertEquals(listOf("gc_1", "gc_2"), filterAndSortAdminGlobalMessages(all, sortOption = AdminGlobalChatSortOption.MOST_REACTED).map { it.id })

        val vm = createVm()
        advanceUntilIdle()
        assertEquals(2, vm.globalMessages.value.size)

        vm.sendGlobalAnnouncementAsAdmin("Kurallara uyalım")
        advanceUntilIdle()
        assertEquals(listOf("Kurallara uyalım"), fakeChatRepo.announcementsSent)
        assertEquals(3, vm.globalMessages.value.size)

        var deletedCount = 0
        vm.batchDeleteGlobalMessages(setOf("gc_1", "gc_2")) { deletedCount = it }
        advanceUntilIdle()
        assertEquals(2, deletedCount)
        assertEquals(1, vm.globalMessages.value.size)

        vm.toggleUserGlobalMessagingPermission("uid_2", disabled = true)
        vm.toggleUserServerMessagingPermission("uid_2", disabled = true)
        advanceUntilIdle()
        val mod = vm.members.value.first { it.uid == "uid_2" }.moderation
        assertTrue(mod.messagingDisabled)
        assertTrue(mod.serverMessagingDisabled)
    }

    // --- Minimal local fakes matching existing test suite pattern ---

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

    private class FakePostRepo : PostRepository {
        override fun observeFeed(currentUserId: String, limit: Long): Flow<AppResult<List<Post>>> = flowOf(AppResult.Success(emptyList()))
        override suspend fun createPost(authorId: String, text: String, title: String, mediaUrl: String, mediaType: String, tags: List<String>): AppResult<Post> =
            AppResult.Error(AppError.Database("N/A"))
        override suspend fun toggleLike(postId: String, userId: String): AppResult<Boolean> = AppResult.Success(true)
        override suspend fun getPostLikeUsers(postId: String): AppResult<List<UserProfile>> = AppResult.Success(emptyList())
        override suspend fun deletePost(postId: String, userId: String): AppResult<Unit> = AppResult.Success(Unit)
        override fun observeComments(postId: String, limit: Long): Flow<AppResult<List<PostComment>>> = flowOf(AppResult.Success(emptyList()))
        override suspend fun addComment(postId: String, authorId: String, text: String, parentCommentId: String): AppResult<PostComment> =
            AppResult.Error(AppError.Database("N/A"))
        override suspend fun deleteComment(postId: String, commentId: String, userId: String): AppResult<Unit> = AppResult.Success(Unit)
        override suspend fun toggleSave(postId: String, userId: String): AppResult<Boolean> = AppResult.Success(true)
        override fun observeSavedPosts(userId: String, limit: Long): Flow<AppResult<List<Post>>> = flowOf(AppResult.Success(emptyList()))
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
        override fun observeNotifications(uid: String, limit: Long): Flow<AppResult<List<AppNotification>>> = flowOf(AppResult.Success(emptyList()))
        override suspend fun create(notification: AppNotification): AppResult<Unit> {
            sent.add(notification)
            return AppResult.Success(Unit)
        }
        override suspend fun markRead(notificationId: String): AppResult<Unit> = AppResult.Success(Unit)
        override suspend fun markAllRead(uid: String): AppResult<Unit> = AppResult.Success(Unit)
    }
}
