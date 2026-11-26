package com.libra.app

import android.net.Uri
import com.libra.app.core.result.AppError
import com.libra.app.core.result.AppResult
import com.libra.app.domain.model.AdminAudienceCategory
import com.libra.app.domain.model.AdminNotificationCampaignLog
import com.libra.app.domain.model.AdminNotificationDraft
import com.libra.app.domain.model.AdminRole
import com.libra.app.domain.model.AppNotification
import com.libra.app.domain.model.AppReleaseInfo
import com.libra.app.domain.model.AppUpdate
import com.libra.app.domain.model.Book
import com.libra.app.domain.model.BookCategory
import com.libra.app.domain.model.Chapter
import com.libra.app.domain.model.CosmeticRole
import com.libra.app.domain.model.DirectConversation
import com.libra.app.domain.model.LibraNotificationTypes
import com.libra.app.domain.model.Post
import com.libra.app.domain.model.PostComment
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
import com.libra.app.feature.admin.AdminViewModel
import com.libra.app.feature.admin.LIBRA_PRIMARY_ADMIN_UID
import com.libra.app.feature.admin.filterAndSortAdminNotificationDrafts
import com.libra.app.feature.admin.resolveAdminAudienceLabel
import com.libra.app.feature.admin.resolveAdminNotificationRecipients
import com.libra.app.notification.LibraNotificationHelper
import com.libra.app.notification.NotificationPreferenceCategory
import com.libra.app.notification.NotificationPreferencesState
import com.libra.app.notification.buildRtdbFallbackDmNotifications
import com.libra.app.notification.classifyNotificationCategory
import com.libra.app.notification.selectNewNotificationsForDelivery
import com.libra.app.notification.shouldSuppressActiveNotification
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
class NotificationSystemTest {

    private val testDispatcher = StandardTestDispatcher()
    private val nowMs = 1_700_000_000_000L

    private val authorMember = UserProfile(
        uid = "uid_author",
        displayName = "Sabahattin Ali",
        username = "sabahattin",
        booksWrittenCount = 2,
        cosmeticRoleIds = listOf("role_gold"),
        createdAt = nowMs - (30L * 24 * 60 * 60 * 1000L)
    )
    private val creatorMember = UserProfile(
        uid = "uid_creator",
        displayName = "Halide Edib",
        username = "halide",
        cosmeticRoleIds = listOf("role_star"),
        createdAt = nowMs - (25L * 24 * 60 * 60 * 1000L)
    )
    private val newMember = UserProfile(
        uid = "uid_new",
        displayName = "Yeni Okur",
        username = "yeniokur",
        createdAt = nowMs - (3L * 24 * 60 * 60 * 1000L)
    )
    private val adminMember = UserProfile(
        uid = "uid_mod",
        displayName = "Moderator",
        username = "mod",
        createdAt = nowMs - (60L * 24 * 60 * 60 * 1000L)
    )
    private val primaryAdmin = UserProfile(
        uid = LIBRA_PRIMARY_ADMIN_UID,
        displayName = "Yasin",
        username = "yasin",
        createdAt = nowMs - (90L * 24 * 60 * 60 * 1000L)
    )
    private val bannedMember = UserProfile(
        uid = "uid_banned",
        displayName = "Engelli Üye",
        username = "banned",
        moderation = UserModeration(banned = true),
        createdAt = nowMs - (5L * 24 * 60 * 60 * 1000L)
    )

    private val scifiBook = Book(
        id = "book_1",
        ownerId = "uid_author",
        title = "Yıldızlar Altında",
        category = BookCategory.SCIFI_FANTASY
    )
    private val creatorPost = Post(id = "post_1", authorId = "uid_creator", text = "Merhaba")
    private val creatorStory = Story(id = "story_1", authorId = "uid_new", text = "Hikaye")

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun classifyNotificationCategory_mapsAllSupportedTypesCorrectly() {
        assertEquals(NotificationPreferenceCategory.MESSAGES, classifyNotificationCategory(LibraNotificationTypes.MESSAGE))
        assertEquals(NotificationPreferenceCategory.MESSAGES, classifyNotificationCategory(LibraNotificationTypes.GROUP_MESSAGE))
        assertEquals(NotificationPreferenceCategory.MESSAGES, classifyNotificationCategory(LibraNotificationTypes.SERVER_MENTION))
        assertEquals(NotificationPreferenceCategory.ANNOUNCEMENTS, classifyNotificationCategory(LibraNotificationTypes.ADMIN_BROADCAST))
        assertEquals(NotificationPreferenceCategory.ANNOUNCEMENTS, classifyNotificationCategory(LibraNotificationTypes.ADMIN_FEEDBACK))
        assertEquals(NotificationPreferenceCategory.SOCIAL, classifyNotificationCategory(LibraNotificationTypes.LIKE))
        assertEquals(NotificationPreferenceCategory.SOCIAL, classifyNotificationCategory(LibraNotificationTypes.COMMENT_REPLY))
        assertEquals(NotificationPreferenceCategory.SOCIAL, classifyNotificationCategory(LibraNotificationTypes.BOOK_LIKE))
        assertEquals(NotificationPreferenceCategory.SOCIAL, classifyNotificationCategory(LibraNotificationTypes.BOOK_COMMENT_REPLY))
        assertEquals(NotificationPreferenceCategory.SOCIAL, classifyNotificationCategory(LibraNotificationTypes.STORY_LIKE))
        assertEquals(NotificationPreferenceCategory.SOCIAL, classifyNotificationCategory(LibraNotificationTypes.FOLLOW))
    }

    @Test
    fun selectNewNotificationsForDelivery_deduplicatesAndRespectsBaselineAndCategoryPreferences() {
        val oldUnread = AppNotification(
            id = "n_old",
            type = LibraNotificationTypes.LIKE,
            createdAt = nowMs - 120_000L,
            read = false
        )
        val freshMessage = AppNotification(
            id = "n_msg",
            type = LibraNotificationTypes.MESSAGE,
            createdAt = nowMs - 5_000L,
            read = false
        )
        val alreadyRead = AppNotification(
            id = "n_read",
            type = LibraNotificationTypes.BOOK_LIKE,
            createdAt = nowMs - 2_000L,
            read = true
        )

        // First baseline: skips old backlog, marks all IDs, delivers fresh unread message
        val initialSelection = selectNewNotificationsForDelivery(
            notifications = listOf(oldUnread, freshMessage, alreadyRead),
            hasBaseline = false,
            baselineMs = 0L,
            alreadyNotifiedIds = emptySet(),
            preferences = NotificationPreferencesState(),
            nowMs = nowMs
        )
        assertEquals(listOf("n_msg"), initialSelection.toDispatch.map { it.id })
        assertEquals(setOf("n_old", "n_msg", "n_read"), initialSelection.idsToMarkNotified.toSet())

        // Second snapshot with same notifications + 1 social like & 1 admin broadcast while social disabled
        val freshLike = AppNotification(
            id = "n_like",
            type = LibraNotificationTypes.LIKE,
            createdAt = nowMs + 1_000L,
            read = false
        )
        val freshBroadcast = AppNotification(
            id = "n_admin",
            type = LibraNotificationTypes.ADMIN_BROADCAST,
            createdAt = nowMs + 2_000L,
            read = false
        )

        val secondSelection = selectNewNotificationsForDelivery(
            notifications = listOf(oldUnread, freshMessage, alreadyRead, freshLike, freshBroadcast),
            hasBaseline = true,
            baselineMs = initialSelection.nextBaselineMs,
            alreadyNotifiedIds = initialSelection.idsToMarkNotified.toSet(),
            preferences = NotificationPreferencesState(
                masterEnabled = true,
                messagesEnabled = true,
                socialEnabled = false,
                announcementsEnabled = true
            ),
            nowMs = nowMs + 3_000L
        )
        assertEquals(listOf("n_admin"), secondSelection.toDispatch.map { it.id })
        assertTrue("n_like" in secondSelection.idsToMarkNotified)
        assertFalse("n_msg" in secondSelection.idsToMarkNotified)
    }

    @Test
    fun selectNewNotificationsForDelivery_deliversMissedUnreadAfterBaselineDespiteClockSkew() {
        val delayedUnreadFromOtherDevice = AppNotification(
            id = "n_delayed_dm",
            type = LibraNotificationTypes.MESSAGE,
            createdAt = nowMs - (10 * 60_000L), // 10 minutes earlier due to offline/clock skew
            read = false
        )
        val selection = selectNewNotificationsForDelivery(
            notifications = listOf(delayedUnreadFromOtherDevice),
            hasBaseline = true,
            baselineMs = nowMs,
            alreadyNotifiedIds = setOf("n_prev"),
            preferences = NotificationPreferencesState(backgroundServiceEnabled = true),
            nowMs = nowMs + 5_000L
        )
        assertEquals(listOf("n_delayed_dm"), selection.toDispatch.map { it.id })
        assertTrue("n_delayed_dm" in selection.idsToMarkNotified)
    }

    @Test
    fun shouldSuppressActiveNotification_suppressesActiveConversationAndNotificationsScreen() {
        val dm = AppNotification(
            id = "n1",
            type = LibraNotificationTypes.MESSAGE,
            referenceId = "conv_123"
        )
        val groupMsg = AppNotification(
            id = "n2",
            type = LibraNotificationTypes.GROUP_MESSAGE,
            referenceId = "conv_123"
        )
        val postLike = AppNotification(
            id = "n3",
            type = LibraNotificationTypes.LIKE,
            referenceId = "post_1"
        )

        assertTrue(shouldSuppressActiveNotification(postLike, isNotificationsScreenOpen = true, activeDirectConversationId = null))
        assertTrue(shouldSuppressActiveNotification(dm, isNotificationsScreenOpen = false, activeDirectConversationId = "conv_123"))
        assertTrue(shouldSuppressActiveNotification(groupMsg, isNotificationsScreenOpen = false, activeDirectConversationId = "conv_123"))
        assertFalse(shouldSuppressActiveNotification(dm, isNotificationsScreenOpen = false, activeDirectConversationId = "conv_other"))
        assertFalse(shouldSuppressActiveNotification(postLike, isNotificationsScreenOpen = false, activeDirectConversationId = "conv_123"))
    }

    @Test
    fun notificationPreferencesDefaultIsEphemeralAndRequestCodeIsDeterministic() {
        val defaults = NotificationPreferencesState()
        assertTrue(defaults.masterEnabled)
        assertFalse(defaults.backgroundServiceEnabled)

        val codeA = LibraNotificationHelper.notificationRequestCode("notif_abc", LibraNotificationTypes.MESSAGE, 1234L)
        val codeB = LibraNotificationHelper.notificationRequestCode("notif_abc", LibraNotificationTypes.MESSAGE, 5678L)
        val fallbackA = LibraNotificationHelper.notificationRequestCode("", LibraNotificationTypes.MESSAGE, 999L)
        val fallbackB = LibraNotificationHelper.notificationRequestCode("", LibraNotificationTypes.MESSAGE, 999L)
        assertEquals(codeA, codeB)
        assertEquals(fallbackA, fallbackB)
    }

    @Test
    fun resolveAdminNotificationRecipients_filtersAllEightCategoriesAccurately() {
        val allMembers = listOf(authorMember, creatorMember, newMember, adminMember, primaryAdmin, bannedMember)
        val books = listOf(scifiBook)
        val posts = listOf(creatorPost)
        val stories = listOf(creatorStory)
        val adminUids = setOf("uid_mod")

        val all = resolveAdminNotificationRecipients(
            members = allMembers,
            books = books,
            posts = posts,
            stories = stories,
            adminRoleUserIds = adminUids,
            categoryType = AdminAudienceCategory.ALL,
            nowMillis = nowMs
        )
        assertEquals(5, all.size)
        assertFalse(all.any { it.uid == "uid_banned" })

        val authors = resolveAdminNotificationRecipients(
            members = allMembers,
            books = books,
            posts = posts,
            stories = stories,
            adminRoleUserIds = adminUids,
            categoryType = AdminAudienceCategory.AUTHORS,
            nowMillis = nowMs
        )
        assertEquals(listOf("uid_author"), authors.map { it.uid })

        val creators = resolveAdminNotificationRecipients(
            members = allMembers,
            books = books,
            posts = posts,
            stories = stories,
            adminRoleUserIds = adminUids,
            categoryType = AdminAudienceCategory.CREATORS,
            nowMillis = nowMs
        )
        assertEquals(setOf("uid_creator", "uid_new"), creators.map { it.uid }.toSet())

        val recent = resolveAdminNotificationRecipients(
            members = allMembers,
            books = books,
            posts = posts,
            stories = stories,
            adminRoleUserIds = adminUids,
            categoryType = AdminAudienceCategory.NEW_MEMBERS,
            nowMillis = nowMs
        )
        assertEquals(listOf("uid_new"), recent.map { it.uid })

        val roleWearers = resolveAdminNotificationRecipients(
            members = allMembers,
            books = books,
            posts = posts,
            stories = stories,
            adminRoleUserIds = adminUids,
            categoryType = AdminAudienceCategory.COSMETIC_ROLE,
            categorySubId = "role_star",
            nowMillis = nowMs
        )
        assertEquals(listOf("uid_creator"), roleWearers.map { it.uid })

        val scifiAuthors = resolveAdminNotificationRecipients(
            members = allMembers,
            books = books,
            posts = posts,
            stories = stories,
            adminRoleUserIds = adminUids,
            categoryType = AdminAudienceCategory.BOOK_CATEGORY_AUTHORS,
            categorySubId = BookCategory.SCIFI_FANTASY.name,
            nowMillis = nowMs
        )
        assertEquals(listOf("uid_author"), scifiAuthors.map { it.uid })

        val admins = resolveAdminNotificationRecipients(
            members = allMembers,
            books = books,
            posts = posts,
            stories = stories,
            adminRoleUserIds = adminUids,
            categoryType = AdminAudienceCategory.ADMINS,
            nowMillis = nowMs
        )
        assertEquals(setOf("uid_mod", LIBRA_PRIMARY_ADMIN_UID), admins.map { it.uid }.toSet())

        val custom = resolveAdminNotificationRecipients(
            members = allMembers,
            books = books,
            posts = posts,
            stories = stories,
            adminRoleUserIds = adminUids,
            categoryType = AdminAudienceCategory.CUSTOM_USERS,
            customUserIds = listOf("uid_new", "uid_banned", "uid_author"),
            nowMillis = nowMs
        )
        assertEquals(setOf("uid_new", "uid_author"), custom.map { it.uid }.toSet())
    }

    @Test
    fun filterAndSortAdminNotificationDrafts_andLabelsWorkCleanly() {
        val drafts = listOf(
            AdminNotificationDraft(id = "d1", title = "Haftalık Okuma", body = "Katılın", tagLabel = "Etkinlik", updatedAt = 100L),
            AdminNotificationDraft(id = "d2", title = "Sistem Bakımı", body = "Gece 02:00", tagLabel = "Sistem", updatedAt = 300L),
            AdminNotificationDraft(id = "d3", title = "Yeni Bölüm", body = "Yayında", tagLabel = "Duyuru", lastSentAt = 400L)
        )
        assertEquals(listOf("d3", "d2", "d1"), filterAndSortAdminNotificationDrafts(drafts).map { it.id })
        assertEquals(listOf("d2"), filterAndSortAdminNotificationDrafts(drafts, query = "bakım").map { it.id })
        assertEquals(listOf("d1"), filterAndSortAdminNotificationDrafts(drafts, tagFilter = "Etkinlik").map { it.id })

        val roles = listOf(CosmeticRole(id = "role_star", name = "Yıldız Yazar"))
        assertEquals(
            "Rozet: Yıldız Yazar",
            resolveAdminAudienceLabel(AdminAudienceCategory.COSMETIC_ROLE, "role_star", cosmeticRoles = roles)
        )
        assertEquals(
            "Bilim Kurgu & Fantastik Yazarları",
            resolveAdminAudienceLabel(AdminAudienceCategory.BOOK_CATEGORY_AUTHORS, BookCategory.SCIFI_FANTASY.name)
        )
    }

    @Test
    fun adminViewModel_savesDraftSendsCategorizedAndEveryoneBroadcastsAndUpdatesCounts() = runTest(testDispatcher) {
        val fakeAdminRepo = FakeAdminRepo(
            initialMembers = listOf(authorMember, creatorMember, newMember),
            initialAdminRoleIds = setOf("uid_author")
        )
        val fakeNotifRepo = FakeNotificationRepo()
        val vm = AdminViewModel(
            repo = fakeAdminRepo,
            bookRepo = FakeBookRepo(listOf(scifiBook)),
            postRepo = FakePostRepo(),
            storyRepo = FakeAdminStoryRepo(),
            communityRepo = FakeAdminCommunityRepo(),
            chatRepo = FakeAdminChatRepo(),
            updateRepo = FakeUpdateRepo(),
            releaseStorageRepo = FakeReleaseStorageRepo(),
            notificationRepo = fakeNotifRepo
        )
        advanceUntilIdle()

        var savedDraft: AdminNotificationDraft? = null
        vm.saveNotificationDraft(
            AdminNotificationDraft(
                title = "Yazar Duyurusu",
                body = "Yeni bölüm yarışması başladı!",
                categoryType = AdminAudienceCategory.AUTHORS.key,
                tagLabel = "Etkinlik",
                createdBy = LIBRA_PRIMARY_ADMIN_UID
            )
        ) { savedDraft = it }
        advanceUntilIdle()

        assertNotNull(savedDraft)
        assertEquals(1, vm.notificationDrafts.value.size)

        // 1) Categorized send from draft -> only authorMember matches AUTHORS
        var firstSuccess = false
        var firstCount = 0
        vm.sendAdminDraftNotification(
            draft = savedDraft!!,
            senderUid = LIBRA_PRIMARY_ADMIN_UID,
            senderName = "Libra Yönetim",
            sendToEveryone = false
        ) { ok, count ->
            firstSuccess = ok
            firstCount = count
        }
        advanceUntilIdle()

        assertTrue(firstSuccess)
        assertEquals(1, firstCount)
        assertEquals(listOf("uid_author"), fakeNotifRepo.sent.map { it.recipientId })
        assertEquals(LibraNotificationTypes.ADMIN_BROADCAST, fakeNotifRepo.sent.first().type)
        assertEquals(1, vm.notificationDrafts.value.first().sendCount)
        assertEquals(1, vm.notificationLogs.value.size)

        // 2) Re-use same draft and toggle "Herkese gönder" -> reaches all 3 eligible members
        fakeNotifRepo.sent.clear()
        val latestDraft = vm.notificationDrafts.value.first()
        var secondCount = 0
        vm.sendAdminDraftNotification(
            draft = latestDraft,
            senderUid = LIBRA_PRIMARY_ADMIN_UID,
            senderName = "Libra Yönetim",
            sendToEveryone = true
        ) { _, count ->
            secondCount = count
        }
        advanceUntilIdle()

        assertEquals(3, secondCount)
        assertEquals(setOf("uid_author", "uid_creator", "uid_new"), fakeNotifRepo.sent.map { it.recipientId }.toSet())
        assertEquals(2, vm.notificationDrafts.value.first().sendCount)
        assertEquals(2, vm.notificationLogs.value.size)

        // 3) Delete draft
        var deleted = false
        vm.deleteNotificationDraft(latestDraft.id) { deleted = it }
        advanceUntilIdle()
        assertTrue(deleted)
        assertTrue(vm.notificationDrafts.value.isEmpty())
    }

    @Test
    fun buildRtdbFallbackDmNotifications_deduplicatesFirestoreAndRespectsClearedChats() {
        val now = 1_795_700_000_000L
        val prefs = NotificationPreferencesState(
            masterEnabled = true,
            messagesEnabled = true,
            socialEnabled = true,
            announcementsEnabled = true,
            backgroundServiceEnabled = false
        )
        val unreadDm = DirectConversation(
            id = "me_peer1",
            otherUserId = "peer1",
            otherUserName = "Deniz",
            otherUserUsername = "deniz",
            lastMessage = "Naber?",
            updatedAt = now - 5_000L,
            unreadCount = 1
        )
        val alreadyCoveredByFirestore = DirectConversation(
            id = "me_peer2",
            otherUserId = "peer2",
            otherUserName = "Mert",
            lastMessage = "Bunu gördüm",
            updatedAt = now - 3_000L,
            unreadCount = 2
        )
        val clearedConv = DirectConversation(
            id = "me_peer3",
            otherUserId = "peer3",
            otherUserName = "Eski",
            lastMessage = "Eski mesaj",
            updatedAt = now - 20_000L,
            unreadCount = 1
        )
        val unreadGroup = DirectConversation(
            id = "grp_1",
            isGroup = true,
            groupName = "Kitap Kulübü",
            lastMessage = "Yeni bölüm çıktı",
            updatedAt = now - 2_000L,
            unreadCount = 3
        )
        val existingFs = listOf(
            AppNotification(
                id = "fs_1",
                type = LibraNotificationTypes.MESSAGE,
                referenceId = "me_peer2",
                createdAt = now - 2_500L,
                read = false
            )
        )

        val fallback = buildRtdbFallbackDmNotifications(
            uid = "me",
            conversations = listOf(unreadDm, alreadyCoveredByFirestore, clearedConv, unreadGroup),
            existingFirestoreChatNotifications = existingFs,
            getLastDmNotifiedAt = { 0L },
            getClearedAt = { convId -> if (convId == "me_peer3") now - 10_000L else 0L },
            preferences = prefs,
            nowMs = now
        )

        assertEquals(listOf("me_peer1", "grp_1"), fallback.map { it.referenceId })
        assertEquals(LibraNotificationTypes.MESSAGE, fallback[0].type)
        assertEquals(LibraNotificationTypes.GROUP_MESSAGE, fallback[1].type)
    }

    // --- Fakes ---

    private class FakeAdminRepo(
        initialMembers: List<UserProfile>,
        initialAdminRoleIds: Set<String> = emptySet()
    ) : AdminRepository {
        private val membersFlow = MutableStateFlow<AppResult<List<UserProfile>>>(AppResult.Success(initialMembers))
        private val draftsFlow = MutableStateFlow<AppResult<List<AdminNotificationDraft>>>(AppResult.Success(emptyList()))
        private val logsFlow = MutableStateFlow<AppResult<List<AdminNotificationCampaignLog>>>(AppResult.Success(emptyList()))
        private val adminIdsFlow = MutableStateFlow<AppResult<Set<String>>>(AppResult.Success(initialAdminRoleIds))
        private var nextDraftId = 1
        private var nextLogId = 1

        override fun observeMembers(): Flow<AppResult<List<UserProfile>>> = membersFlow
        override suspend fun updateMemberProfile(profile: UserProfile): AppResult<Unit> = AppResult.Success(Unit)
        override suspend fun updateModeration(uid: String, moderation: UserModeration): AppResult<Unit> = AppResult.Success(Unit)
        override suspend fun getAdminRole(uid: String): AppResult<AdminRole?> = AppResult.Success(null)
        override suspend fun setAdminRole(uid: String, role: AdminRole?): AppResult<Unit> = AppResult.Success(Unit)
        override fun observeCosmeticRoles(): Flow<AppResult<List<CosmeticRole>>> = flowOf(AppResult.Success(emptyList()))
        override suspend fun saveCosmeticRole(role: CosmeticRole): AppResult<Unit> = AppResult.Success(Unit)
        override suspend fun deleteCosmeticRole(roleId: String): AppResult<Unit> = AppResult.Success(Unit)
        override suspend fun assignCosmeticRole(uid: String, roleId: String, assigned: Boolean): AppResult<Unit> = AppResult.Success(Unit)

        override fun observeAdminRoleUserIds(): Flow<AppResult<Set<String>>> = adminIdsFlow
        override fun observeAdminNotificationDrafts(): Flow<AppResult<List<AdminNotificationDraft>>> = draftsFlow

        override suspend fun saveAdminNotificationDraft(draft: AdminNotificationDraft): AppResult<AdminNotificationDraft> {
            val saved = if (draft.id.isBlank()) draft.copy(id = "draft_${nextDraftId++}") else draft
            val current = (draftsFlow.value as AppResult.Success).data.filterNot { it.id == saved.id }
            draftsFlow.value = AppResult.Success(listOf(saved) + current)
            return AppResult.Success(saved)
        }

        override suspend fun deleteAdminNotificationDraft(draftId: String): AppResult<Unit> {
            val current = (draftsFlow.value as AppResult.Success).data.filterNot { it.id == draftId }
            draftsFlow.value = AppResult.Success(current)
            return AppResult.Success(Unit)
        }

        override fun observeAdminNotificationLogs(limit: Long): Flow<AppResult<List<AdminNotificationCampaignLog>>> = logsFlow

        override suspend fun logAdminNotificationCampaign(log: AdminNotificationCampaignLog): AppResult<Unit> {
            val saved = if (log.id.isBlank()) log.copy(id = "camp_${nextLogId++}") else log
            val current = (logsFlow.value as AppResult.Success).data
            logsFlow.value = AppResult.Success(listOf(saved) + current)
            return AppResult.Success(Unit)
        }
    }

    private class FakeBookRepo(initialBooks: List<Book>) : BookRepository {
        private val booksFlow = MutableStateFlow<AppResult<List<Book>>>(AppResult.Success(initialBooks))
        override fun observeAllBooksForAdmin(): Flow<AppResult<List<Book>>> = booksFlow
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
