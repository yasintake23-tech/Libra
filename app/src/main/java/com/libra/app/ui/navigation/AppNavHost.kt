package com.libra.app.ui.navigation

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import com.libra.app.ui.animation.AppleSprings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.libra.app.core.state.UiState
import com.libra.app.core.di.ServiceLocator
import com.libra.app.domain.model.Book
import com.libra.app.domain.model.ShelfType
import com.libra.app.domain.model.SharedContent
import com.libra.app.domain.model.AdminPermissionSet
import com.libra.app.feature.share.sharedContentUrl
import com.libra.app.feature.admin.AdminPanelScreen
import com.libra.app.feature.auth.AuthViewModel
import com.libra.app.feature.auth.GoogleAuthHelper
import com.libra.app.feature.auth.LoginScreen
import com.libra.app.feature.friends.FriendsScreen
import com.libra.app.feature.friends.FriendsState
import com.libra.app.feature.friends.FriendsViewModel
import com.libra.app.feature.home.HomeScreen
import com.libra.app.feature.home.HomeViewModel
import com.libra.app.feature.home.CreateContentMode
import com.libra.app.feature.home.CreateContentScreen
import com.libra.app.feature.library.LibraryScreen
import com.libra.app.feature.library.LibraryViewModel
import com.libra.app.feature.messages.DirectMessagesScreen
import com.libra.app.feature.messages.GlobalChatScreen
import com.libra.app.feature.messages.CommunityServersScreen
import com.libra.app.feature.notifications.NotificationsScreen
import com.libra.app.feature.profile.ProfileScreen
import com.libra.app.feature.profile.PublicProfileScreen
import com.libra.app.feature.profile.ProfileSetupScreen
import com.libra.app.feature.profile.ProfileEditScreen
import com.libra.app.feature.profile.ProfileViewModel
import com.libra.app.feature.settings.SettingsScreen
import com.libra.app.feature.book.manage.BookManagementRootScreen
import com.libra.app.feature.write.BookEditorScreen
import com.libra.app.feature.write.WriteScreen
import com.libra.app.feature.write.WriteViewModel
import com.libra.app.feature.write.create.CreateBookScreen
import com.libra.app.feature.reader.BookReaderScreen
import com.libra.app.feature.book.BookDetailScreen
import com.libra.app.ui.components.LoadingView
import com.libra.app.feature.update.AppUpdatePrompt
import com.libra.app.feature.update.AppUpdateViewModel
import kotlinx.coroutines.launch

@Composable
fun AppNavHost(
    authViewModel: AuthViewModel = viewModel(),
    modifier: Modifier = Modifier,
    darkTheme: Boolean = true,
    onDarkThemeChanged: (Boolean) -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val authState by authViewModel.authState.collectAsState()
    val authenticated by authViewModel.isAuthenticated.collectAsState()
    val currentUser by authViewModel.currentUser.collectAsState()

    var selectedTab by remember { mutableStateOf(BottomNavTab.HOME) }
    var showSettings by remember { mutableStateOf(false) }
    var showAdminPanel by remember { mutableStateOf(false) }
    var showProfileEdit by remember { mutableStateOf(false) }
    var showGlobalChat by remember { mutableStateOf(false) }
    var showCommunityServers by remember { mutableStateOf(false) }
    var showNotifications by remember { mutableStateOf(false) }
    var unreadNotificationCount by remember { mutableStateOf(0) }
    var notificationPostTargetId by remember { mutableStateOf<String?>(null) }
    var selectedBook by remember { mutableStateOf<Book?>(null) }
    var readerBook by remember { mutableStateOf<Book?>(null) }
    var readerChapterId by remember { mutableStateOf<String?>(null) }
    var selectedWritingBook by remember { mutableStateOf<Book?>(null) }
    var showCreateBook by remember { mutableStateOf(false) }
    var selectedPublicProfile by remember { mutableStateOf<com.libra.app.domain.model.UserProfile?>(null) }
    var selectedDirectUser by remember { mutableStateOf<com.libra.app.domain.model.UserProfile?>(null) }
    fun openPublicProfile(uid: String) {
        if (uid.isBlank()) return
        scope.launch {
            when (val result = ServiceLocator.userRepository.getUserProfileFresh(uid)) {
                is com.libra.app.core.result.AppResult.Success -> {
                    if (result.data != null) {
                        selectedPublicProfile = result.data
                    } else {
                        Toast.makeText(context, "Bu kullanıcı artık mevcut değil.", Toast.LENGTH_SHORT).show()
                    }
                }
                is com.libra.app.core.result.AppResult.Error -> {
                    Toast.makeText(context, "Kullanıcı profili yüklenemedi.", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
    var createContentMode by remember { mutableStateOf<CreateContentMode?>(null) }
    val friendsVm: FriendsViewModel = viewModel()
    val friendsState by friendsVm.uiState.collectAsState()
    val writeVm: WriteViewModel = viewModel()
    val homeVm: HomeViewModel = viewModel()
    val libraryVm: LibraryViewModel = viewModel()
    val updateVm: AppUpdateViewModel = viewModel()
    val updateState by updateVm.uiState.collectAsState()

    // Keep the update prompt above all authentication/profile early returns.
    // Release metadata is public, so this works for signed-out and incomplete profiles too.
    updateState.release?.let { release ->
        AppUpdatePrompt(
            release = release,
            progress = updateState.progress,
            downloading = updateState.downloading,
            error = updateState.error,
            forceUpdate = release.forceUpdate,
            onDownload = { updateVm.download(context) },
            onDismiss = updateVm::dismissUpdate,
            onClearError = updateVm::clearError,
            downloadedFile = updateState.downloadedFile
        )
    }

    // Update checks must run independently of authentication/profile setup.
    // This lets a fresh install see a mandatory/new release before login or registration.
    LaunchedEffect(currentUser?.uid) {
        updateVm.checkForUpdate()
    }

    LaunchedEffect(currentUser?.uid) {
        val uid = currentUser?.uid.orEmpty()
        if (uid.isBlank()) {
            unreadNotificationCount = 0
            return@LaunchedEffect
        }

        ServiceLocator.notificationRepository.observeNotifications(uid).collect { result ->
            unreadNotificationCount = when (result) {
                is com.libra.app.core.result.AppResult.Success -> result.data.count { !it.read }
                is com.libra.app.core.result.AppResult.Error -> unreadNotificationCount
            }
        }
    }

    LaunchedEffect(currentUser?.uid, selectedTab) {
        if (selectedTab == BottomNavTab.DISCOVER && currentUser?.uid?.isNotBlank() == true) {
            friendsVm.loadSocialData()
        }
    }

    if (!authenticated) {
        when (authState) {
            is UiState.Loading -> LoadingView(message = "Libra hazırlanıyor…")
            else -> LoginScreen(
                authState = authState,
                onGoogleSignInClick = {
                    scope.launch {
                        GoogleAuthHelper.launchGoogleSignIn(
                            context = context,
                            onSuccess = { token, name, email, photo ->
                                authViewModel.signInWithGoogle(token, name, email, photo)
                            },
                            onError = {
                                Toast.makeText(context, it, Toast.LENGTH_LONG).show()
                            }
                        )
                    }
                },
                onEmailSignIn = authViewModel::signInWithEmail,
                onEmailRegister = authViewModel::createAccount
            )
        }
        return
    }

    val profile = currentUser
    if (profile == null) {
        LoadingView(message = "Profil hazırlanıyor…")
        return
    }

    // The app uses state-based navigation, so Android's system back button
    // must unwind those states explicitly instead of finishing the Activity.
    BackHandler {
        when {
            showCreateBook -> showCreateBook = false
            readerBook != null -> readerBook = null
            selectedBook != null -> selectedBook = null
            createContentMode != null -> createContentMode = null
            selectedWritingBook != null -> selectedWritingBook = null
            selectedPublicProfile != null -> selectedPublicProfile = null
            showNotifications -> showNotifications = false
            showCommunityServers -> showCommunityServers = false
            showGlobalChat -> showGlobalChat = false
            showAdminPanel -> showAdminPanel = false
            showProfileEdit -> showProfileEdit = false
            showSettings -> showSettings = false
            selectedDirectUser != null -> selectedDirectUser = null
            selectedTab != BottomNavTab.HOME -> selectedTab = BottomNavTab.HOME
            else -> {
                // No in-app screen is open. Let Android finish the Activity.
            }
        }
    }

    var hasAdminAccess by remember(profile.uid) { mutableStateOf(profile.uid == LIBRA_ADMIN_UID) }
    var adminPermissions by remember(profile.uid) { mutableStateOf(AdminPermissionSet()) }
    LaunchedEffect(profile.uid) {
        if (profile.uid == LIBRA_ADMIN_UID) {
            hasAdminAccess = true
            adminPermissions = AdminPermissionSet(manageMembers = true, manageBans = true, editProfiles = true, manageBooks = true, managePosts = true, manageStories = true, manageServers = true, manageGlobalChat = true, manageAdminRoles = true, manageCosmetics = true, sendFeedback = true, manageUpdates = true)
        } else {
            val role = (ServiceLocator.adminRepository.getAdminRole(profile.uid) as? com.libra.app.core.result.AppResult.Success)?.data
            hasAdminAccess = role != null
            adminPermissions = role?.permissions ?: AdminPermissionSet()
        }
    }

    if (!profile.profileCompleted) {
        ProfileSetupScreen(profile = profile, onCompleted = authViewModel::checkSession, modifier = modifier.fillMaxSize())
        return
    }

    var lastSelectedBook by remember { mutableStateOf<Book?>(null) }
    if (selectedBook != null) lastSelectedBook = selectedBook

    var lastReaderBook by remember { mutableStateOf<Book?>(null) }
    var lastReaderChapterId by remember { mutableStateOf<String?>(null) }
    if (readerBook != null) {
        lastReaderBook = readerBook
        lastReaderChapterId = readerChapterId
    }

    var lastSelectedWritingBook by remember { mutableStateOf<Book?>(null) }
    if (selectedWritingBook != null) lastSelectedWritingBook = selectedWritingBook

    var lastCreateContentMode by remember { mutableStateOf<CreateContentMode?>(null) }
    if (createContentMode != null) lastCreateContentMode = createContentMode

    var lastSelectedPublicProfile by remember { mutableStateOf<com.libra.app.domain.model.UserProfile?>(null) }
    if (selectedPublicProfile != null) lastSelectedPublicProfile = selectedPublicProfile

    val isAnyOverlayActive = showCreateBook || createContentMode != null || readerBook != null ||
        selectedBook != null || selectedWritingBook != null || selectedPublicProfile != null ||
        showNotifications || showCommunityServers || showGlobalChat || showAdminPanel ||
        showProfileEdit || showSettings

    Box(modifier = modifier.fillMaxSize()) {
        Scaffold(
            contentWindowInsets = WindowInsets.safeDrawing.only(WindowInsetsSides.Vertical),
            bottomBar = {
                if (!isAnyOverlayActive) {
                    LibraBottomBar(selectedTab = selectedTab, onTabSelected = { selectedTab = it })
                }
            },
            containerColor = MaterialTheme.colorScheme.background,
            modifier = Modifier.fillMaxSize()
        ) { paddingValues ->
            Box(Modifier.fillMaxSize().padding(paddingValues)) {
                AnimatedContent(
                    targetState = selectedTab,
                    transitionSpec = {
                        val forward = targetState.ordinal > initialState.ordinal
                        val slideOffset = { size: Int -> (size * 0.16f).toInt() }
                        if (forward) {
                            (slideInHorizontally(animationSpec = AppleSprings.snappy(), initialOffsetX = slideOffset) +
                                fadeIn(animationSpec = AppleSprings.snappy()))
                                .togetherWith(
                                    slideOutHorizontally(animationSpec = AppleSprings.snappy(), targetOffsetX = { -slideOffset(it) }) +
                                        fadeOut(animationSpec = AppleSprings.snappy())
                                )
                        } else {
                            (slideInHorizontally(animationSpec = AppleSprings.snappy(), initialOffsetX = { -slideOffset(it) }) +
                                fadeIn(animationSpec = AppleSprings.snappy()))
                                .togetherWith(
                                    slideOutHorizontally(animationSpec = AppleSprings.snappy(), targetOffsetX = slideOffset) +
                                        fadeOut(animationSpec = AppleSprings.snappy())
                                )
                        }
                    },
                    label = "tab_navigation_animation"
                ) { tab ->
                    when (tab) {
                        BottomNavTab.HOME -> {
                            val state by homeVm.uiState.collectAsState()
                            HomeScreen(
                                state,
                                { selectedBook = it },
                                { selectedTab = BottomNavTab.WRITE },
                                { selectedTab = BottomNavTab.LIBRARY },
                                { selectedTab = BottomNavTab.PROFILE },
                                { selectedTab = BottomNavTab.DISCOVER },
                                { showCommunityServers = true },
                                { showNotifications = true },
                                unreadNotificationCount,
                                homeVm::loadHomeData,
                                onCreatePost = homeVm::createPost,
                                onOpenCreatePost = { if (profile.moderation.canPost) createContentMode = CreateContentMode.POST },
                                onOpenCreateStory = { if (profile.moderation.canStory) createContentMode = CreateContentMode.STORY },
                                canComment = profile.moderation.canComment,
                                onToggleLike = homeVm::toggleLike, onToggleSave = homeVm::toggleSave,
                                onDeletePost = homeVm::deletePost, onOpenComments = homeVm::openComments,
                                comments = homeVm.comments.collectAsState().value, onAddComment = homeVm::addComment,
                                onDeleteComment = homeVm::deleteComment, isCommenting = homeVm.isCommenting.collectAsState().value,
                                commentError = homeVm.commentError.collectAsState().value, onClearCommentError = homeVm::clearCommentError,
                                onCloseComments = homeVm::closeComments, isPosting = homeVm.isPosting.collectAsState().value,
                                postError = homeVm.postError.collectAsState().value, onClearPostError = homeVm::clearPostError,
                                onOpenProfile = ::openPublicProfile,
                                onCreateBookClick = { showCreateBook = true },
                                onStartReading = { book ->
                                    selectedBook = book
                                    readerBook = book
                                },
                                onShareBook = { book ->
                                    val shareIntent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                                        type = "text/plain"
                                        putExtra(android.content.Intent.EXTRA_SUBJECT, book.title)
                                        putExtra(android.content.Intent.EXTRA_TEXT, "${book.title} - ${book.authorName} Libra'da!\nhttps://libra.app/book/${book.id}")
                                    }
                                    context.startActivity(android.content.Intent.createChooser(shareIntent, "Kitabı Paylaş"))
                                },
                                initialCommentPostId = notificationPostTargetId,
                                onInitialCommentPostConsumed = { notificationPostTargetId = null },
                                onStoryReply = { story ->
                                    scope.launch {
                                        when (val result = ServiceLocator.userRepository.getUserProfileFresh(story.authorId)) {
                                            is com.libra.app.core.result.AppResult.Success -> result.data?.let {
                                                selectedDirectUser = it
                                                selectedTab = BottomNavTab.DM
                                                val shared = SharedContent(
                                                    type = "story", id = story.id,
                                                    title = if (story.text.isBlank()) "Hikâye" else story.text.take(80),
                                                    text = story.text, authorId = story.authorId, authorName = story.authorName,
                                                    mediaUrl = story.mediaUrl, url = sharedContentUrl("story", story.id)
                                                )
                                                ServiceLocator.chatRepository.sendDirectMessage(
                                                    recipientId = story.authorId, text = "Hikâyenden bahsetti.", sharedContent = shared
                                                )
                                            }
                                            is com.libra.app.core.result.AppResult.Error ->
                                                Toast.makeText(context, "Hikâye sahibi bulunamadı.", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                },
                                onStoryLike = homeVm::toggleStoryLike, onStoriesRefresh = homeVm::refreshStories
                            )
                        }
                        BottomNavTab.DISCOVER -> FriendsScreen(
                            uiState = friendsState,
                            onSearchChanged = friendsVm::updateSearchQuery,
                            onFollowUser = { user -> friendsVm.toggleFollowUser(user) },
                            onUserClick = { user -> openPublicProfile(user.uid) },
                            onRetry = friendsVm::loadSocialData
                        )
                        BottomNavTab.WRITE -> {
                            val state by writeVm.uiState.collectAsState()
                            WriteScreen(
                                uiState = state,
                                onCreateBookClick = { showCreateBook = true },
                                onBookClick = { selectedWritingBook = it },
                                onRetry = writeVm::loadMyBooks
                            )
                        }
                        BottomNavTab.DM -> DirectMessagesScreen(
                            onFindFriends = { selectedTab = BottomNavTab.DISCOVER },
                            onGlobalChatClick = { showGlobalChat = true },
                            initialUser = selectedDirectUser, onInitialUserConsumed = { selectedDirectUser = null },
                            onOpenProfile = ::openPublicProfile, canMessage = profile.moderation.canMessage,
                            onServersClick = { showCommunityServers = true }
                        )
                        BottomNavTab.LIBRARY -> {
                            val state by libraryVm.uiState.collectAsState()
                            LibraryScreen(
                                uiState = state,
                                onTabSelected = libraryVm::selectShelf,
                                onSearchChanged = libraryVm::updateSearchQuery,
                                onBookClick = { book, shelf ->
                                    if (shelf == ShelfType.MY_WRITINGS) {
                                        selectedWritingBook = book
                                    } else {
                                        selectedBook = book
                                    }
                                },
                                onNavigateToWrite = { selectedTab = BottomNavTab.WRITE },
                                onCreateBookClick = { showCreateBook = true },
                                onRetry = libraryVm::retryCurrentShelf,
                                onStartReading = { book ->
                                    selectedBook = book
                                    readerBook = book
                                },
                                onRemoveFromShelf = libraryVm::removeFromShelf,
                                onAddToSaved = { book -> libraryVm.addToShelf(book, ShelfType.SAVED) },
                                onNavigateToDiscover = { selectedTab = BottomNavTab.DISCOVER }
                            )
                        }
                        BottomNavTab.PROFILE -> {
                            val vm: ProfileViewModel = viewModel()
                            val state by vm.uiState.collectAsState()
                            ProfileScreen(
                                uiState = state,
                                onSignOutClick = { authViewModel.signOut(); selectedTab = BottomNavTab.HOME },
                                onRetry = vm::loadProfile,
                                onSettingsClick = { showSettings = true },
                                onAdminClick = if (hasAdminAccess) { { showAdminPanel = true } } else null,
                                onMyBooksClick = { selectedTab = BottomNavTab.LIBRARY },
                                onOpenProfile = ::openPublicProfile
                            )
                        }
                    }
                }
            }
        }

        // --- Animated Modal Presentation Overlays (Apple Sheet / Full-Screen Spring Physics) ---
        AnimatedVisibility(
            visible = showCreateBook,
            enter = slideInVertically(animationSpec = AppleSprings.gentle()) { it } + fadeIn(),
            exit = slideOutVertically(animationSpec = AppleSprings.snappy()) { it } + fadeOut(),
            modifier = Modifier.fillMaxSize()
        ) {
            Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                CreateBookScreen(
                    onBack = { showCreateBook = false },
                    onBookCreated = { newBook ->
                        showCreateBook = false
                        selectedWritingBook = newBook
                        writeVm.loadMyBooks()
                        homeVm.loadHomeData()
                        libraryVm.retryCurrentShelf()
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }
        }

        AnimatedVisibility(
            visible = createContentMode != null,
            enter = slideInVertically(animationSpec = AppleSprings.gentle()) { it } + fadeIn(),
            exit = slideOutVertically(animationSpec = AppleSprings.snappy()) { it } + fadeOut(),
            modifier = Modifier.fillMaxSize()
        ) {
            Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                lastCreateContentMode?.let { mode ->
                    val vm: HomeViewModel = viewModel()
                    CreateContentScreen(
                        mode = mode,
                        user = profile,
                        isPosting = vm.isPosting.collectAsState().value,
                        error = vm.postError.collectAsState().value,
                        onPublishPost = { title, text, tags, mediaUrl, mediaType ->
                            vm.createRichPost(title, text, tags, mediaUrl, mediaType) { success ->
                                if (success && mode == CreateContentMode.POST) {
                                    createContentMode = null
                                    selectedTab = BottomNavTab.HOME
                                }
                            }
                        },
                        onPublishStory = { text, mediaUrl, mediaType ->
                            vm.createStory(text, mediaUrl, mediaType) { success ->
                                if (success) {
                                    createContentMode = null
                                    selectedTab = BottomNavTab.HOME
                                }
                            }
                        },
                        onBack = { createContentMode = null },
                        onClearError = vm::clearPostError,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }

        AnimatedVisibility(
            visible = selectedBook != null && readerBook == null,
            enter = slideInVertically(animationSpec = AppleSprings.gentle()) { it } + fadeIn(),
            exit = slideOutVertically(animationSpec = AppleSprings.snappy()) { it } + fadeOut(),
            modifier = Modifier.fillMaxSize()
        ) {
            Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                lastSelectedBook?.let { book ->
                    BookDetailScreen(
                        book = book,
                        userId = profile.uid,
                        onBack = { selectedBook = null },
                        onRead = { selected, chapter ->
                            readerBook = selected
                            readerChapterId = chapter?.id
                        },
                        onOpenChapter = { selected, chapter ->
                            readerBook = selected
                            readerChapterId = chapter.id
                        },
                        onOpenAuthor = ::openPublicProfile,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }

        AnimatedVisibility(
            visible = readerBook != null,
            enter = slideInVertically(animationSpec = AppleSprings.gentle()) { it } + fadeIn(),
            exit = slideOutVertically(animationSpec = AppleSprings.snappy()) { it } + fadeOut(),
            modifier = Modifier.fillMaxSize()
        ) {
            Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                lastReaderBook?.let { book ->
                    BookReaderScreen(
                        book = book,
                        userId = profile.uid,
                        initialChapterId = lastReaderChapterId,
                        onBack = {
                            readerBook = null
                            readerChapterId = null
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }

        AnimatedVisibility(
            visible = selectedWritingBook != null,
            enter = slideInVertically(animationSpec = AppleSprings.gentle()) { it } + fadeIn(),
            exit = slideOutVertically(animationSpec = AppleSprings.snappy()) { it } + fadeOut(),
            modifier = Modifier.fillMaxSize()
        ) {
            Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                lastSelectedWritingBook?.let { book ->
                    BookManagementRootScreen(
                        book = book,
                        onBack = {
                            selectedWritingBook = null
                            writeVm.loadMyBooks()
                        },
                        onPreviewBook = { target ->
                            selectedBook = target
                        },
                        onDeleteBook = { target ->
                            writeVm.deleteBook(target.id)
                            selectedWritingBook = null
                        },
                        onOpenProfile = ::openPublicProfile,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }

        AnimatedVisibility(
            visible = selectedPublicProfile != null,
            enter = slideInVertically(animationSpec = AppleSprings.gentle()) { it } + fadeIn(),
            exit = slideOutVertically(animationSpec = AppleSprings.snappy()) { it } + fadeOut(),
            modifier = Modifier.fillMaxSize()
        ) {
            Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                lastSelectedPublicProfile?.let { publicProfile ->
                    PublicProfileScreen(
                        profile = publicProfile,
                        isFollowing = friendsState.dataOrNull?.followingIds?.contains(publicProfile.uid) == true,
                        onBack = { selectedPublicProfile = null },
                        onFollow = { friendsVm.toggleFollowUser(publicProfile) },
                        onMessage = {
                            selectedPublicProfile = null
                            selectedDirectUser = publicProfile
                            selectedTab = BottomNavTab.DM
                        },
                        onOpenProfile = ::openPublicProfile,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }

        AnimatedVisibility(
            visible = showNotifications,
            enter = slideInVertically(animationSpec = AppleSprings.gentle()) { it } + fadeIn(),
            exit = slideOutVertically(animationSpec = AppleSprings.snappy()) { it } + fadeOut(),
            modifier = Modifier.fillMaxSize()
        ) {
            Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                NotificationsScreen(
                    onBack = { showNotifications = false },
                    onOpenProfile = ::openPublicProfile,
                    onOpenNotification = { notification ->
                        if (
                            notification.referenceId.isNotBlank() &&
                            (notification.type == "COMMENT" || notification.type == "COMMENT_REPLY")
                        ) {
                            notificationPostTargetId = notification.referenceId
                            showNotifications = false
                            selectedTab = BottomNavTab.HOME
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }
        }

        AnimatedVisibility(
            visible = showCommunityServers,
            enter = slideInVertically(animationSpec = AppleSprings.gentle()) { it } + fadeIn(),
            exit = slideOutVertically(animationSpec = AppleSprings.snappy()) { it } + fadeOut(),
            modifier = Modifier.fillMaxSize()
        ) {
            Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                CommunityServersScreen(
                    onBack = { showCommunityServers = false },
                    onOpenProfile = ::openPublicProfile,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }

        AnimatedVisibility(
            visible = showGlobalChat,
            enter = slideInVertically(animationSpec = AppleSprings.gentle()) { it } + fadeIn(),
            exit = slideOutVertically(animationSpec = AppleSprings.snappy()) { it } + fadeOut(),
            modifier = Modifier.fillMaxSize()
        ) {
            Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                GlobalChatScreen(
                    onBack = { showGlobalChat = false },
                    onOpenProfile = ::openPublicProfile,
                    canMessage = profile.moderation.canMessage && profile.moderation.canMessageInServer,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }

        AnimatedVisibility(
            visible = showAdminPanel && hasAdminAccess,
            enter = slideInVertically(animationSpec = AppleSprings.gentle()) { it } + fadeIn(),
            exit = slideOutVertically(animationSpec = AppleSprings.snappy()) { it } + fadeOut(),
            modifier = Modifier.fillMaxSize()
        ) {
            Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                AdminPanelScreen(
                    onBack = { showAdminPanel = false },
                    permissions = adminPermissions,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }

        AnimatedVisibility(
            visible = showProfileEdit,
            enter = slideInVertically(animationSpec = AppleSprings.gentle()) { it } + fadeIn(),
            exit = slideOutVertically(animationSpec = AppleSprings.snappy()) { it } + fadeOut(),
            modifier = Modifier.fillMaxSize()
        ) {
            Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                ProfileEditScreen(
                    profile = profile,
                    onSaved = { showProfileEdit = false; authViewModel.checkSession() },
                    onBack = { showProfileEdit = false },
                    modifier = Modifier.fillMaxSize()
                )
            }
        }

        AnimatedVisibility(
            visible = showSettings && !showProfileEdit,
            enter = slideInVertically(animationSpec = AppleSprings.gentle()) { it } + fadeIn(),
            exit = slideOutVertically(animationSpec = AppleSprings.snappy()) { it } + fadeOut(),
            modifier = Modifier.fillMaxSize()
        ) {
            Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                SettingsScreen(
                    profile = profile,
                    darkTheme = darkTheme,
                    onDarkThemeChanged = onDarkThemeChanged,
                    onEditProfile = { showProfileEdit = true },
                    onSignOut = { showSettings = false; authViewModel.signOut(); selectedTab = BottomNavTab.HOME },
                    onBack = { showSettings = false },
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}

private const val LIBRA_ADMIN_UID = "ZBkz2js9plg07zrny2PzLW80X3i2"

private val UiState<FriendsState>.dataOrNull: FriendsState?
    get() = (this as? UiState.Success)?.data
