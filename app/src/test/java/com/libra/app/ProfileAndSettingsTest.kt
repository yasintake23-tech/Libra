package com.libra.app

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.libra.app.core.result.AppResult
import com.libra.app.core.state.UiState
import com.libra.app.domain.model.AdminPermissionSet
import com.libra.app.domain.model.AdminRole
import com.libra.app.domain.model.Book
import com.libra.app.domain.model.BookCategory
import com.libra.app.domain.model.BookStatus
import com.libra.app.domain.model.Chapter
import com.libra.app.domain.model.CosmeticRole
import com.libra.app.domain.model.Post
import com.libra.app.domain.model.PostComment
import com.libra.app.domain.model.ShelfType
import com.libra.app.domain.model.Story
import com.libra.app.domain.model.UserModeration
import com.libra.app.domain.model.UserProfile
import com.libra.app.domain.model.UserShelfItem
import com.libra.app.domain.repository.AdminRepository
import com.libra.app.domain.repository.AuthRepository
import com.libra.app.domain.repository.BookRepository
import com.libra.app.domain.repository.PostRepository
import com.libra.app.domain.repository.StoryRepository
import com.libra.app.domain.repository.UserRepository
import com.libra.app.feature.profile.ProfileContentTab
import com.libra.app.feature.profile.ProfileViewModel
import com.libra.app.feature.reader.ReaderPreferences
import com.libra.app.feature.reader.ReaderPreferencesStore
import com.libra.app.feature.reader.ReaderTheme
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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ProfileAndSettingsTest {

    private val testDispatcher = StandardTestDispatcher()

    private val testUser = UserProfile(
        uid = "uid_sabahattin",
        displayName = "Sabahattin Ali",
        username = "sabahattin",
        email = "sabahattin@libra.app",
        bio = "Kürk Mantolu Madonna yazarı.",
        booksWrittenCount = 2,
        booksReadCount = 14,
        followersCount = 120,
        followingCount = 45,
        profileCompleted = true,
        cosmeticRoleIds = listOf("role_classic")
    )

    private val followerUser = UserProfile(
        uid = "uid_okur_1",
        displayName = "Maria Puder",
        username = "mariapuder"
    )

    private val samplePost = Post(
        id = "post_1",
        authorId = "uid_sabahattin",
        authorName = "Sabahattin Ali",
        authorUsername = "sabahattin",
        title = "Yeni Bölüm Notları",
        text = "Yazmak, sessiz bir odada dünyayı yeniden kurmaktır.",
        likesCount = 10,
        commentsCount = 3,
        likedByCurrentUser = false,
        savedByCurrentUser = false,
        createdAt = 1_700_000_000_000L
    )

    private val sampleBook = Book(
        id = "book_madonna",
        ownerId = "uid_sabahattin",
        authorName = "Sabahattin Ali",
        title = "Kürk Mantolu Madonna",
        category = BookCategory.CLASSICS,
        status = BookStatus.PUBLISHED,
        chapterCount = 12,
        readCount = 540
    )

    private val sampleSavedBook = UserShelfItem(
        id = "shelf_1",
        userId = "uid_sabahattin",
        book = Book(
            id = "book_tutunamayanlar",
            ownerId = "uid_oguz",
            authorName = "Oğuz Atay",
            title = "Tutunamayanlar",
            category = BookCategory.CLASSICS,
            status = BookStatus.PUBLISHED
        ),
        shelfType = ShelfType.SAVED,
        progressPercent = 20
    )

    private val sampleStory = Story(
        id = "story_1",
        authorId = "uid_sabahattin",
        authorName = "Sabahattin Ali",
        text = "Bugün yeni bölüm yayında!",
        createdAt = 1_700_000_000_000L,
        expiresAt = 1_700_086_400_000L
    )

    private lateinit var fakeAuth: FakeAuthRepository
    private lateinit var fakeUserRepo: FakeUserRepository
    private lateinit var fakePostRepo: FakePostRepository
    private lateinit var fakeBookRepo: FakeBookRepository
    private lateinit var fakeStoryRepo: FakeStoryRepository
    private lateinit var fakeAdminRepo: FakeAdminRepository

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeAuth = FakeAuthRepository(testUser)
        fakeUserRepo = FakeUserRepository(
            profile = testUser,
            followers = listOf(followerUser),
            following = listOf(followerUser)
        )
        fakePostRepo = FakePostRepository(
            userPosts = listOf(samplePost),
            savedPosts = listOf(samplePost.copy(id = "post_saved", savedByCurrentUser = true))
        )
        fakeBookRepo = FakeBookRepository(
            writtenBooks = listOf(sampleBook),
            savedBooks = listOf(sampleSavedBook)
        )
        fakeStoryRepo = FakeStoryRepository(activeStories = listOf(sampleStory))
        fakeAdminRepo = FakeAdminRepository(
            adminRole = AdminRole(id = "admin", name = "Kurucu", permissions = AdminPermissionSet(manageMembers = true)),
            cosmeticRoles = listOf(CosmeticRole(id = "role_classic", name = "Usta Kalem"))
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createVm(): ProfileViewModel = ProfileViewModel(
        authRepository = fakeAuth,
        userRepository = fakeUserRepo,
        postRepository = fakePostRepo,
        bookRepository = fakeBookRepo,
        storyRepository = fakeStoryRepo,
        adminRepository = fakeAdminRepo
    )

    @Test
    fun profileViewModel_loadsInstagramProfileDataAndMetadata() = runTest(testDispatcher) {
        val vm = createVm()
        advanceUntilIdle()

        val state = vm.uiState.value
        assertTrue(state is UiState.Success)
        assertEquals("sabahattin", (state as UiState.Success).data.username)

        val content = vm.contentState.value
        assertEquals(1, content.posts.size)
        assertEquals("post_1", content.posts.first().id)
        assertEquals(1, content.writtenBooks.size)
        assertEquals("book_madonna", content.writtenBooks.first().id)
        assertEquals(1, content.savedPosts.size)
        assertEquals(1, content.savedBooks.size)
        assertEquals(1, content.followers.size)
        assertEquals(1, content.following.size)
        assertEquals(1, content.activeStories.size)
        assertEquals("Kurucu", content.adminRole?.name)
        assertEquals(1, content.cosmeticRoles.size)
    }

    @Test
    fun profileViewModel_signedOutUserProducesEmptyState() = runTest(testDispatcher) {
        fakeAuth.setUser(null)
        val vm = createVm()
        advanceUntilIdle()

        assertTrue(vm.uiState.value is UiState.Empty)
        assertTrue(vm.contentState.value.posts.isEmpty())
        assertTrue(vm.contentState.value.writtenBooks.isEmpty())
    }

    @Test
    fun profileViewModel_refreshProfileUpdatesAndStopsRefreshing() = runTest(testDispatcher) {
        val vm = createVm()
        advanceUntilIdle()

        val updatedBio = "Güncellenmiş biyografi."
        fakeUserRepo.profile = testUser.copy(bio = updatedBio)

        vm.refreshProfile()
        advanceUntilIdle()

        val state = vm.uiState.value as UiState.Success
        assertEquals(updatedBio, state.data.bio)
        assertFalse(vm.contentState.value.isRefreshing)
    }

    @Test
    fun profileViewModel_toggleLikeSaveAndDeletePostUpdateContentState() = runTest(testDispatcher) {
        val vm = createVm()
        advanceUntilIdle()

        val initialPost = vm.contentState.value.posts.first()
        vm.togglePostLike(initialPost)
        advanceUntilIdle()

        val likedPost = vm.contentState.value.posts.first()
        assertTrue(likedPost.likedByCurrentUser)
        assertEquals(11, likedPost.likesCount)
        assertEquals("post_1", fakePostRepo.lastLikedPostId)

        vm.togglePostSave(likedPost)
        advanceUntilIdle()
        assertTrue(vm.contentState.value.posts.first().savedByCurrentUser)
        assertEquals("post_1", fakePostRepo.lastSavedPostId)

        vm.deletePost(likedPost)
        advanceUntilIdle()
        assertTrue(vm.contentState.value.posts.isEmpty())
        assertEquals("post_1", fakePostRepo.lastDeletedPostId)
    }

    @Test
    fun profileViewModel_signOutInvokesCallback() = runTest(testDispatcher) {
        val vm = createVm()
        advanceUntilIdle()

        var signedOut = false
        vm.signOut { signedOut = true }
        advanceUntilIdle()

        assertTrue(signedOut)
        assertTrue(fakeAuth.signOutCalled)
    }

    @Test
    fun profileContentTabs_matchInstagramStructure() {
        val ownTabs = ProfileContentTab.entries
        assertEquals(listOf(ProfileContentTab.POSTS, ProfileContentTab.BOOKS, ProfileContentTab.SAVED), ownTabs)
    }

    @Test
    fun settingsPreferences_persistReaderAndPrivacyChoices() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val prefs = context.getSharedPreferences("libra_settings", Context.MODE_PRIVATE)
        prefs.edit()
            .putBoolean("dark_theme", false)
            .putBoolean("notifications_enabled", false)
            .putBoolean("online_status_enabled", false)
            .putString("dm_privacy_scope", "FOLLOWING")
            .commit()

        assertFalse(prefs.getBoolean("dark_theme", true))
        assertFalse(prefs.getBoolean("notifications_enabled", true))
        assertFalse(prefs.getBoolean("online_status_enabled", true))
        assertEquals("FOLLOWING", prefs.getString("dm_privacy_scope", "EVERYONE"))

        val readerStore = ReaderPreferencesStore(context)
        readerStore.save(
            ReaderPreferences(
                theme = ReaderTheme.SEPIA,
                fontSizeSp = 22f,
                lineSpacing = 1.8f,
                showProgress = false
            )
        )
        val restored = readerStore.read()
        assertEquals(ReaderTheme.SEPIA, restored.theme)
        assertEquals(22f, restored.fontSizeSp)
        assertEquals(1.8f, restored.lineSpacing)
        assertFalse(restored.showProgress)
    }

    // --- Fakes ---

    private class FakeAuthRepository(initialUser: UserProfile?) : AuthRepository {
        private val _currentUser = MutableStateFlow(initialUser)
        override val currentUser: StateFlow<UserProfile?> = _currentUser.asStateFlow()
        private val _isAuthenticated = MutableStateFlow(initialUser != null)
        override val isAuthenticated: StateFlow<Boolean> = _isAuthenticated.asStateFlow()
        var signOutCalled = false

        fun setUser(user: UserProfile?) {
            _currentUser.value = user
            _isAuthenticated.value = user != null
        }

        override suspend fun checkCurrentSession(): AppResult<UserProfile?> = AppResult.Success(_currentUser.value)
        override fun signInWithGoogleIdToken(idToken: String, displayName: String?, email: String?, photoUrl: String?): Flow<AppResult<UserProfile>> =
            flowOf(AppResult.Success(_currentUser.value!!))
        override suspend fun signInWithEmailPassword(email: String, password: String): AppResult<UserProfile> =
            AppResult.Success(_currentUser.value!!)
        override suspend fun createAccountWithEmailPassword(email: String, password: String): AppResult<UserProfile> =
            AppResult.Success(_currentUser.value!!)
        override suspend fun signOut(): AppResult<Unit> {
            signOutCalled = true
            setUser(null)
            return AppResult.Success(Unit)
        }
    }

    private class FakeUserRepository(
        var profile: UserProfile,
        private val followers: List<UserProfile>,
        private val following: List<UserProfile>
    ) : UserRepository {
        override fun getUserProfile(uid: String): Flow<AppResult<UserProfile?>> = flowOf(AppResult.Success(profile))
        override suspend fun getUserProfileFresh(uid: String): AppResult<UserProfile?> = AppResult.Success(profile)
        override suspend fun createOrUpdateProfile(profile: UserProfile): AppResult<UserProfile> = AppResult.Success(profile)
        override suspend fun completeProfile(profile: UserProfile): AppResult<UserProfile> = AppResult.Success(profile)
        override suspend fun isUsernameAvailable(username: String, currentUid: String?): AppResult<Boolean> = AppResult.Success(true)
        override suspend fun updateBio(uid: String, bio: String): AppResult<Unit> = AppResult.Success(Unit)
        override suspend fun updateProfilePhoto(uid: String, photoUrl: String): AppResult<Unit> = AppResult.Success(Unit)
        override suspend fun updateProfileDetails(profile: UserProfile, photoUrl: String?): AppResult<UserProfile> = AppResult.Success(profile)
        override fun searchUsers(query: String): Flow<AppResult<List<UserProfile>>> = flowOf(AppResult.Success(emptyList()))
        override suspend fun getFollowingIds(uid: String): AppResult<Set<String>> = AppResult.Success(following.map { it.uid }.toSet())
        override suspend fun isFollowing(followerId: String, followingId: String): AppResult<Boolean> = AppResult.Success(false)
        override suspend fun followUser(followerId: String, followingId: String): AppResult<Unit> = AppResult.Success(Unit)
        override suspend fun unfollowUser(followerId: String, followingId: String): AppResult<Unit> = AppResult.Success(Unit)
        override suspend fun getFollowers(uid: String): AppResult<List<UserProfile>> = AppResult.Success(followers)
        override suspend fun getFollowing(uid: String): AppResult<List<UserProfile>> = AppResult.Success(following)
    }

    private class FakePostRepository(
        private val userPosts: List<Post>,
        private val savedPosts: List<Post>
    ) : PostRepository {
        var lastLikedPostId: String? = null
        var lastSavedPostId: String? = null
        var lastDeletedPostId: String? = null

        override fun observeFeed(currentUserId: String, limit: Long): Flow<AppResult<List<Post>>> = flowOf(AppResult.Success(userPosts))
        override fun observeUserPosts(userId: String, currentUserId: String, limit: Long): Flow<AppResult<List<Post>>> =
            flowOf(AppResult.Success(userPosts))
        override suspend fun createPost(authorId: String, text: String, title: String, mediaUrl: String, mediaType: String, tags: List<String>): AppResult<Post> =
            AppResult.Success(userPosts.first())
        override suspend fun toggleLike(postId: String, userId: String): AppResult<Boolean> {
            lastLikedPostId = postId
            return AppResult.Success(true)
        }
        override suspend fun getPostLikeUsers(postId: String): AppResult<List<UserProfile>> = AppResult.Success(emptyList())
        override suspend fun deletePost(postId: String, userId: String): AppResult<Unit> {
            lastDeletedPostId = postId
            return AppResult.Success(Unit)
        }
        override fun observeComments(postId: String, limit: Long): Flow<AppResult<List<PostComment>>> = flowOf(AppResult.Success(emptyList()))
        override suspend fun addComment(postId: String, authorId: String, text: String, parentCommentId: String): AppResult<PostComment> =
            AppResult.Success(PostComment())
        override suspend fun deleteComment(postId: String, commentId: String, userId: String): AppResult<Unit> = AppResult.Success(Unit)
        override suspend fun toggleSave(postId: String, userId: String): AppResult<Boolean> {
            lastSavedPostId = postId
            return AppResult.Success(true)
        }
        override fun observeSavedPosts(userId: String, limit: Long): Flow<AppResult<List<Post>>> = flowOf(AppResult.Success(savedPosts))
    }

    private class FakeBookRepository(
        private val writtenBooks: List<Book>,
        private val savedBooks: List<UserShelfItem>
    ) : BookRepository {
        override fun getFeaturedBooks(): Flow<AppResult<List<Book>>> = flowOf(AppResult.Success(writtenBooks))
        override fun getRecentBooks(): Flow<AppResult<List<Book>>> = flowOf(AppResult.Success(writtenBooks))
        override fun getBooksByCategory(category: BookCategory): Flow<AppResult<List<Book>>> = flowOf(AppResult.Success(writtenBooks))
        override fun getBookById(bookId: String): Flow<AppResult<Book?>> = flowOf(AppResult.Success(writtenBooks.firstOrNull()))
        override fun getUserLibrary(userId: String, shelfType: ShelfType): Flow<AppResult<List<UserShelfItem>>> =
            flowOf(AppResult.Success(if (shelfType == ShelfType.SAVED) savedBooks else emptyList()))
        override fun getUserWrittenBooks(userId: String): Flow<AppResult<List<Book>>> = flowOf(AppResult.Success(writtenBooks))
        override suspend fun createBook(book: Book): AppResult<Book> = AppResult.Success(book)
        override suspend fun updateBook(book: Book): AppResult<Book> = AppResult.Success(book)
        override suspend fun deleteBook(bookId: String): AppResult<Unit> = AppResult.Success(Unit)
        override suspend fun addBookToShelf(userId: String, bookId: String, shelfType: ShelfType): AppResult<Unit> = AppResult.Success(Unit)
        override suspend fun removeBookFromShelf(userId: String, bookId: String, shelfType: ShelfType): AppResult<Unit> = AppResult.Success(Unit)
        override fun getBookChapters(bookId: String): Flow<AppResult<List<Chapter>>> = flowOf(AppResult.Success(emptyList()))
        override suspend fun saveChapter(chapter: Chapter): AppResult<Chapter> = AppResult.Success(chapter)
        override suspend fun deleteChapter(bookId: String, chapterId: String): AppResult<Unit> = AppResult.Success(Unit)
    }

    private class FakeStoryRepository(private val activeStories: List<Story>) : StoryRepository {
        override suspend fun createStory(authorId: String, authorName: String, authorPhotoUrl: String, text: String, mediaUrl: String, mediaType: String): AppResult<Unit> =
            AppResult.Success(Unit)
        override suspend fun getActiveStories(authorIds: Set<String>): AppResult<List<Story>> = AppResult.Success(activeStories)
        override suspend fun isLiked(storyId: String, userId: String): AppResult<Boolean> = AppResult.Success(false)
        override suspend fun toggleLike(storyId: String, userId: String): AppResult<Boolean> = AppResult.Success(true)
    }

    private class FakeAdminRepository(
        private val adminRole: AdminRole?,
        private val cosmeticRoles: List<CosmeticRole>
    ) : AdminRepository {
        override fun observeMembers(): Flow<AppResult<List<UserProfile>>> = flowOf(AppResult.Success(emptyList()))
        override suspend fun updateMemberProfile(profile: UserProfile): AppResult<Unit> = AppResult.Success(Unit)
        override suspend fun updateModeration(uid: String, moderation: UserModeration): AppResult<Unit> = AppResult.Success(Unit)
        override suspend fun getAdminRole(uid: String): AppResult<AdminRole?> = AppResult.Success(adminRole)
        override suspend fun setAdminRole(uid: String, role: AdminRole?): AppResult<Unit> = AppResult.Success(Unit)
        override fun observeCosmeticRoles(): Flow<AppResult<List<CosmeticRole>>> = flowOf(AppResult.Success(cosmeticRoles))
        override suspend fun saveCosmeticRole(role: CosmeticRole): AppResult<Unit> = AppResult.Success(Unit)
        override suspend fun deleteCosmeticRole(roleId: String): AppResult<Unit> = AppResult.Success(Unit)
        override suspend fun assignCosmeticRole(uid: String, roleId: String, assigned: Boolean): AppResult<Unit> = AppResult.Success(Unit)
    }
}
