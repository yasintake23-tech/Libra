package com.libra.app.ui.navigation

import android.widget.Toast
import androidx.compose.foundation.layout.Box
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
import com.libra.app.R
import com.libra.app.core.state.UiState
import com.libra.app.domain.model.Book
import com.libra.app.domain.model.ShelfType
import com.libra.app.feature.auth.AuthViewModel
import com.libra.app.feature.auth.GoogleAuthHelper
import com.libra.app.feature.auth.LoginScreen
import com.libra.app.feature.friends.FriendsScreen
import com.libra.app.feature.friends.FriendsViewModel
import com.libra.app.feature.home.HomeScreen
import com.libra.app.feature.home.HomeViewModel
import com.libra.app.feature.library.LibraryScreen
import com.libra.app.feature.library.LibraryViewModel
import com.libra.app.feature.profile.ProfileScreen
import com.libra.app.feature.profile.ProfileViewModel
import com.libra.app.feature.write.WriteScreen
import com.libra.app.feature.write.WriteViewModel
import com.libra.app.ui.components.AppButton
import com.libra.app.ui.components.AppButtonVariant
import com.libra.app.ui.components.LoadingView
import kotlinx.coroutines.launch

@Composable
fun AppNavHost(
    authViewModel: AuthViewModel = viewModel(),
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val authState by authViewModel.authState.collectAsState()
    val isAuthenticated by authViewModel.isAuthenticated.collectAsState()

    var selectedTab by remember { mutableStateOf(BottomNavTab.HOME) }
    var selectedBookForDetail by remember { mutableStateOf<Book?>(null) }

    if (!isAuthenticated) {
        when (authState) {
            is UiState.Loading -> LoadingView(message = "Oturum kontrol ediliyor...")
            else -> LoginScreen(
                authState = authState,
                onGoogleSignInClick = {
                    scope.launch {
                        GoogleAuthHelper.launchGoogleSignIn(
                            context = context,
                            serverClientId = context.getString(R.string.google_web_client_id),
                            onSuccess = { idToken, name, email, photo ->
                                authViewModel.signInWithGoogle(idToken, name, email, photo)
                            },
                            onError = { errorMsg ->
                                Toast.makeText(context, errorMsg, Toast.LENGTH_LONG).show()
                            }
                        )
                    }
                }
            )
        }
        return
    }

    Scaffold(
        bottomBar = {
            AppBottomNavBar(
                selectedTab = selectedTab,
                onTabSelected = { selectedTab = it }
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
        modifier = modifier.fillMaxSize()
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            when (selectedTab) {
                BottomNavTab.HOME -> {
                    val vm: HomeViewModel = viewModel()
                    val state by vm.uiState.collectAsState()
                    HomeScreen(
                        uiState = state,
                        onBookClick = { selectedBookForDetail = it },
                        onNavigateToWrite = { selectedTab = BottomNavTab.WRITE },
                        onNavigateToLibrary = { selectedTab = BottomNavTab.LIBRARY },
                        onNavigateToProfile = { selectedTab = BottomNavTab.PROFILE },
                        onRetry = vm::loadHomeData
                    )
                }
                BottomNavTab.LIBRARY -> {
                    val vm: LibraryViewModel = viewModel()
                    val state by vm.uiState.collectAsState()
                    LibraryScreen(
                        uiState = state,
                        onTabSelected = vm::loadShelf,
                        onSearchChanged = vm::updateSearchQuery,
                        onBookClick = { selectedBookForDetail = it },
                        onNavigateToWrite = { selectedTab = BottomNavTab.WRITE },
                        onRetry = { vm.loadShelf(ShelfType.READING) }
                    )
                }
                BottomNavTab.WRITE -> {
                    val vm: WriteViewModel = viewModel()
                    val state by vm.uiState.collectAsState()
                    WriteScreen(
                        uiState = state,
                        onCreateBook = { title, desc, category ->
                            vm.createNewBook(title, desc, category)
                        },
                        onBookClick = { selectedBookForDetail = it },
                        onRetry = vm::loadMyBooks
                    )
                }
                BottomNavTab.FRIENDS -> {
                    val vm: FriendsViewModel = viewModel()
                    val state by vm.uiState.collectAsState()
                    FriendsScreen(
                        uiState = state,
                        onSearchChanged = vm::updateSearchQuery,
                        onFollowUser = { user ->
                            Toast.makeText(context, "${user.displayName} için takip altyapısı sonraki aşamada eklenecek.", Toast.LENGTH_SHORT).show()
                        },
                        onRetry = vm::loadSocialData
                    )
                }
                BottomNavTab.PROFILE -> {
                    val vm: ProfileViewModel = viewModel()
                    val state by vm.uiState.collectAsState()
                    ProfileScreen(
                        uiState = state,
                        onSignOutClick = {
                            vm.signOut {
                                selectedTab = BottomNavTab.HOME
                                Toast.makeText(context, "Oturum kapatıldı", Toast.LENGTH_SHORT).show()
                            }
                        },
                        onRetry = vm::loadProfile
                    )
                }
            }
        }
    }

    selectedBookForDetail?.let { book ->
        AlertDialog(
            onDismissRequest = { selectedBookForDetail = null },
            title = { Text(book.title, style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)) },
            text = {
                Column {
                    Text("Yazar: ${book.authorName}", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(6.dp))
                    Text("Kategori: ${book.category.displayName}", style = MaterialTheme.typography.bodySmall)
                    Spacer(Modifier.height(10.dp))
                    Text(book.description.ifBlank { "Bu kitap için henüz açıklama eklenmedi." })
                }
            },
            confirmButton = {
                AppButton(
                    text = "Kapat",
                    onClick = { selectedBookForDetail = null },
                    variant = AppButtonVariant.PRIMARY,
                    modifier = Modifier.height(42.dp)
                )
            },
            dismissButton = {
                TextButton(onClick = { selectedBookForDetail = null }) { Text("İptal") }
            }
        )
    }
}
