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
import com.libra.app.feature.auth.AuthViewModel
import com.libra.app.feature.auth.GoogleAuthHelper
import com.libra.app.feature.auth.LoginScreen
import com.libra.app.feature.friends.FriendsScreen
import com.libra.app.feature.friends.FriendsState
import com.libra.app.feature.friends.FriendsViewModel
import com.libra.app.feature.home.HomeScreen
import com.libra.app.feature.home.HomeViewModel
import com.libra.app.feature.library.LibraryScreen
import com.libra.app.feature.library.LibraryViewModel
import com.libra.app.feature.messages.DirectMessagesScreen
import com.libra.app.feature.messages.GlobalChatScreen
import com.libra.app.feature.messages.CommunityServersScreen
import com.libra.app.feature.notifications.NotificationsScreen
import com.libra.app.feature.profile.ProfileScreen
import com.libra.app.feature.profile.PublicProfileScreen
import com.libra.app.feature.profile.ProfileSetupScreen
import com.libra.app.feature.profile.ProfileViewModel
import com.libra.app.feature.settings.SettingsScreen
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
    var showGlobalChat by remember { mutableStateOf(false) }
    var showCommunityServers by remember { mutableStateOf(false) }
    var showNotifications by remember { mutableStateOf(false) }
    var selectedBook by remember { mutableStateOf<Book?>(null) }
    var selectedPublicProfile by remember { mutableStateOf<com.libra.app.domain.model.UserProfile?>(null) }
    var selectedDirectUser by remember { mutableStateOf<com.libra.app.domain.model.UserProfile?>(null) }
    val friendsVm: FriendsViewModel = viewModel()
    val friendsState by friendsVm.uiState.collectAsState()

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
            modifier = modifier.fillMaxSize()
        )
        return
    }

    if (showNotifications) {
        NotificationsScreen(
            onBack = { showNotifications = false },
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
            modifier = modifier.fillMaxSize()
        )
        return
    }

    if (showSettings) {
        SettingsScreen(
            profile = profile,
            darkTheme = darkTheme,
            onDarkThemeChanged = onDarkThemeChanged,
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
                        onToggleLike = vm::toggleLike,
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
                        onClearPostError = vm::clearPostError
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
                    val vm: WriteViewModel = viewModel()
                    val state by vm.uiState.collectAsState()

                    WriteScreen(
                        state,
                        vm::createNewBook,
                        { selectedBook = it },
                        vm::loadMyBooks
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
                        onSettingsClick = { showSettings = true }
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

private val UiState<FriendsState>.dataOrNull: FriendsState?
    get() = (this as? UiState.Success)?.data
