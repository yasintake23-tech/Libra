package com.libra.app.ui.navigation

import android.widget.Toast
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
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.libra.app.feature.write.BookEditorScreen
import com.libra.app.feature.write.WriteScreen
import com.libra.app.feature.write.WriteViewModel
import com.libra.app.ui.components.LoadingView
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
    var selectedBook by remember { mutableStateOf<Book?>(null) }
    var selectedWritingBook by remember { mutableStateOf<Book?>(null) }
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
                                authViewModel.signInWithGoogle(
                                    token,
                                    name,
                                    email,
                                    photo
                                )
                            },
                            onError = {
                                Toast.makeText(
                                    context,
                                    it,
                                    Toast.LENGTH_LONG
                                ).show()
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

    var hasAdminAccess by remember(profile.uid) { mutableStateOf(profile.uid == LIBRA_ADMIN_UID) }
    var adminPermissions by remember(profile.uid) { mutableStateOf(AdminPermissionSet()) }
    LaunchedEffect(profile.uid) {
        if (profile.uid == LIBRA_ADMIN_UID) { hasAdminAccess = true; adminPermissions = AdminPermissionSet(true,true,true,true,true,true,true,true,true,true,true) }
        else { val role = (ServiceLocator.adminRepository.getAdminRole(profile.uid) as? com.libra.app.core.result.AppResult.Success)?.data; hasAdminAccess = role != null; adminPermissions = role?.permissions ?: AdminPermissionSet() }
    }

    createContentMode?.let { mode ->
        val vm: HomeViewModel = viewModel()
        CreateContentScreen(
            mode = mode,
            user = profile,
            isPosting = vm.isPosting.collectAsState().value,
            error = vm.postError.collectAsState().value,
            onPublishPost = { title, text, tags, mediaUrl, mediaType ->
                vm.createRichPost(
                    title = title,
                    text = text,
                    tags = tags,
                    mediaUrl = mediaUrl,
                    mediaType = mediaType,
                    onComplete = { success ->
                        if (success && mode == CreateContentMode.POST) {
                            createContentMode = null
                            selectedTab = BottomNavTab.HOME
                        }
                    }
                )
            },
            onPublishStory = { text, mediaUrl, mediaType ->
                vm.createStory(
                    text = text,
                    mediaUrl = mediaUrl,
                    mediaType = mediaType,
                    onComplete = { success ->
                        if (success) {
                            createContentMode = null
                            selectedTab = BottomNavTab.HOME
                        }
                    }
                )
            },
            onBack = { createContentMode = null },
            onClearError = vm::clearPostError,
            modifier = modifier.fillMaxSize()
        )
        return
    }

    selectedWritingBook?.let { book ->
        val chapters by writeVm.editorChapters.collectAsState()
        val isSaving by writeVm.editorSaving.collectAsState()
        val editorError by writeVm.editorError.collectAsState()

        BookEditorScreen(
            book = book,
            chapters = chapters,
            isSaving = isSaving,
            error = editorError,
            onLoadChapters = writeVm::loadChapters,
            onSaveBook = writeVm::saveBook,
            onSaveChapter = writeVm::saveChapter,
            onPublish = writeVm::publishBook,
            onClearError = writeVm::clearEditorError,
            onBack = { selectedWritingBook = null },
            modifier = modifier.fillMaxSize()
        )
        return
    }

    selectedPublicProfile?.let { publicProfile ->
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
            modifier = modifier.fillMaxSize()
        )
        return
    }

    if (showNotifications) {
        NotificationsScreen(
            onBack = { showNotifications = false },
            onOpenProfile = ::openPublicProfile,
            modifier = modifier.fillMaxSize()
        )
        return
    }

    if (showCommunityServers) {
        CommunityServersScreen(
            onBack = { showCommunityServers = false },
            modifier = modifier.fillMaxSize()
        )
        return
    }

    if (showGlobalChat) {
        GlobalChatScreen(
            onBack = { showGlobalChat = false },
            onOpenProfile = ::openPublicProfile,
            canMessage = profile.moderation.canMessage && profile.moderation.canMessageInServer,
            modifier = modifier.fillMaxSize()
        )
        return
    }

    if (showAdminPanel) {
        if (hasAdminAccess) {
            AdminPanelScreen(
                onBack = { showAdminPanel = false },
                permissions = adminPermissions,
                modifier = modifier.fillMaxSize()
            )
        } else {
            showAdminPanel = false
        }
        return
    }

    if (showProfileEdit) {
        ProfileEditScreen(
            profile = profile,
            onSaved = {
                showProfileEdit = false
                authViewModel.checkSession()
            },
            onBack = { showProfileEdit = false },
            modifier = modifier.fillMaxSize()
        )
        return
    }

    if (showSettings) {
        SettingsScreen(
            profile = profile,
            darkTheme = darkTheme,
            onDarkThemeChanged = onDarkThemeChanged,
            onEditProfile = { showProfileEdit = true },
            onSignOut = {
                showSettings = false
                authViewModel.signOut()
                selectedTab = BottomNavTab.HOME
            },
            onBack = { showSettings = false },
            modifier = modifier.fillMaxSize()
        )
        return
    }

    if (!profile.profileCompleted) {
        ProfileSetupScreen(
            profile = profile,
            onCompleted = authViewModel::checkSession,
            modifier = modifier.fillMaxSize()
        )
        return
    }

    Scaffold(
        contentWindowInsets = WindowInsets.safeDrawing.only(WindowInsetsSides.Vertical),
        bottomBar = {
            LibraBottomBar(
                selectedTab = selectedTab,
                onTabSelected = { selectedTab = it }
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
        modifier = modifier.fillMaxSize()
    ) { paddingValues ->
        Box(
            Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (selectedTab) {
                BottomNavTab.HOME -> {
                    val vm: HomeViewModel = viewModel()
                    val state by vm.uiState.collectAsState()

                    HomeScreen(
                        state,
                        { selectedBook = it },
                        { selectedTab = BottomNavTab.WRITE },
                        { selectedTab = BottomNavTab.LIBRARY },
                        { selectedTab = BottomNavTab.PROFILE },
                        { selectedTab = BottomNavTab.DISCOVER },
                        { showCommunityServers = true },
                        { showNotifications = true },
                        vm::loadHomeData,
                        onCreatePost = vm::createPost,
                        onOpenCreatePost = { if (profile.moderation.canPost) createContentMode = CreateContentMode.POST },
                        onOpenCreateStory = { if (profile.moderation.canStory) createContentMode = CreateContentMode.STORY },
                        onToggleLike = vm::toggleLike,
                        onToggleSave = vm::toggleSave,
                        onDeletePost = vm::deletePost,
                        onOpenComments = vm::openComments,
                        comments = vm.comments.collectAsState().value,
                        onAddComment = vm::addComment,
                        onDeleteComment = vm::deleteComment,
                        isCommenting = vm.isCommenting.collectAsState().value,
                        commentError = vm.commentError.collectAsState().value,
                        onClearCommentError = vm::clearCommentError,
                        onCloseComments = vm::closeComments,
                        isPosting = vm.isPosting.collectAsState().value,
                        postError = vm.postError.collectAsState().value,
                        onClearPostError = vm::clearPostError,
                        onOpenProfile = ::openPublicProfile,
                        onStoryReply = { story ->
                            scope.launch {
                                when (val result = ServiceLocator.userRepository.getUserProfileFresh(story.authorId)) {
                                    is com.libra.app.core.result.AppResult.Success -> {
                                        result.data?.let {
                                            selectedDirectUser = it
                                            selectedTab = BottomNavTab.DM
                                            scope.launch {
                                                val shared = SharedContent(
                                                    type = "story",
                                                    id = story.id,
                                                    title = if (story.text.isBlank()) "Hikâye" else story.text.take(80),
                                                    text = story.text,
                                                    authorId = story.authorId,
                                                    authorName = story.authorName,
                                                    mediaUrl = story.mediaUrl,
                                                    url = sharedContentUrl("story", story.id)
                                                )
                                                ServiceLocator.chatRepository.sendDirectMessage(
                                                    recipientId = story.authorId,
                                                    text = "Hikâyenden bahsetti.",
                                                    sharedContent = shared
                                                )
                                            }
                                        }
                                    }
                                    is com.libra.app.core.result.AppResult.Error -> {
                                        Toast.makeText(context, "Hikâye sahibi bulunamadı.", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }
                        },
                        onStoryLike = vm::toggleStoryLike,
                        onStoriesRefresh = vm::refreshStories
                    )
                }

                BottomNavTab.DISCOVER -> {
                    FriendsScreen(
                        friendsState,
                        friendsVm::updateSearchQuery,
                        { user ->
                            scope.launch {
                                when (val result = ServiceLocator.userRepository.getUserProfileFresh(user.uid)) {
                                    is com.libra.app.core.result.AppResult.Success -> {
                                        if (result.data != null) {
                                            selectedPublicProfile = result.data
                                        } else {
                                            Toast.makeText(
                                                context,
                                                "Bu kullanıcı artık mevcut değil.",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        }
                                    }
                                    is com.libra.app.core.result.AppResult.Error -> {
                                        Toast.makeText(
                                            context,
                                            "Kullanıcı profili yenilenemedi.",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                }
                            }
                        },
                        { user ->
                            scope.launch {
                                when (val result = ServiceLocator.userRepository.getUserProfileFresh(user.uid)) {
                                    is com.libra.app.core.result.AppResult.Success -> {
                                        if (result.data != null) selectedPublicProfile = result.data
                                    }
                                    is com.libra.app.core.result.AppResult.Error -> {
                                        Toast.makeText(context, "Kullanıcı profili yenilenemedi.", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }
                        },
                        friendsVm::loadSocialData
                    )
                }

                BottomNavTab.WRITE -> {
                    val state by writeVm.uiState.collectAsState()

                    WriteScreen(
                        state,
                        writeVm::createNewBook,
                        { selectedWritingBook = it },
                        writeVm::loadMyBooks
                    )
                }

                BottomNavTab.DM -> {
                    DirectMessagesScreen(
                        onFindFriends = { selectedTab = BottomNavTab.DISCOVER },
                        onGlobalChatClick = {
                            showGlobalChat = true
                        },
                        initialUser = selectedDirectUser,
                        onInitialUserConsumed = { selectedDirectUser = null },
                        onOpenProfile = ::openPublicProfile,
                        canMessage = profile.moderation.canMessage,
                        onServersClick = {
                            showCommunityServers = true
                        }
                    )
                }

                BottomNavTab.LIBRARY -> {
                    val vm: LibraryViewModel = viewModel()
                    val state by vm.uiState.collectAsState()

                    LibraryScreen(
                        state,
                        vm::loadShelf,
                        vm::updateSearchQuery,
                        { selectedBook = it },
                        { selectedTab = BottomNavTab.WRITE },
                        { vm.loadShelf(ShelfType.READING) }
                    )
                }

                BottomNavTab.PROFILE -> {
                    val vm: ProfileViewModel = viewModel()
                    val state by vm.uiState.collectAsState()

                    ProfileScreen(
                        state,
                        {
                            authViewModel.signOut()
                            selectedTab = BottomNavTab.HOME
                        },
                        vm::loadProfile,
                        onSettingsClick = { showSettings = true },
                        onAdminClick = if (hasAdminAccess) { { showAdminPanel = true } } else null
                    )
                }
            }
        }
    }

    selectedBook?.let { book ->
        AlertDialog(
            onDismissRequest = { selectedBook = null },
            title = {
                Text(
                    book.title,
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold
                    )
                )
            },
            text = {
                Column {
                    Text(
                        "Yazar: " + book.authorName,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        book.category.displayName,
                        style = MaterialTheme.typography.bodySmall
                    )
                    Spacer(Modifier.height(10.dp))
                    Text(
                        book.description.ifBlank {
                            "Bu kitap için henüz açıklama eklenmedi."
                        }
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { selectedBook = null }) {
                    Text("Kapat")
                }
            }
        )
    }
}

private const val LIBRA_ADMIN_UID = "ZBkz2js9plg07zrny2PzLW80X3i2"

private val UiState<FriendsState>.dataOrNull: FriendsState?
    get() = (this as? UiState.Success)?.data
