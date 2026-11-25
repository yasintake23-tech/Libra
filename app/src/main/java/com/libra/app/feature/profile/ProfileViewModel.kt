package com.libra.app.feature.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.libra.app.core.di.ServiceLocator
import com.libra.app.core.result.AppResult
import com.libra.app.core.state.UiState
import com.libra.app.domain.model.AdminRole
import com.libra.app.domain.model.Book
import com.libra.app.domain.model.CosmeticRole
import com.libra.app.domain.model.Post
import com.libra.app.domain.model.ShelfType
import com.libra.app.domain.model.Story
import com.libra.app.domain.model.UserProfile
import com.libra.app.domain.model.UserShelfItem
import com.libra.app.domain.repository.AdminRepository
import com.libra.app.domain.repository.AuthRepository
import com.libra.app.domain.repository.BookRepository
import com.libra.app.domain.repository.PostRepository
import com.libra.app.domain.repository.StoryRepository
import com.libra.app.domain.repository.UserRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class ProfileContentTab {
    POSTS,
    BOOKS,
    SAVED
}

data class ProfileContentState(
    val posts: List<Post> = emptyList(),
    val writtenBooks: List<Book> = emptyList(),
    val savedPosts: List<Post> = emptyList(),
    val savedBooks: List<UserShelfItem> = emptyList(),
    val followers: List<UserProfile> = emptyList(),
    val following: List<UserProfile> = emptyList(),
    val activeStories: List<Story> = emptyList(),
    val adminRole: AdminRole? = null,
    val cosmeticRoles: List<CosmeticRole> = emptyList(),
    val isRefreshing: Boolean = false
)

class ProfileViewModel(
    private val authRepository: AuthRepository = ServiceLocator.authRepository,
    private val userRepository: UserRepository = ServiceLocator.userRepository,
    private val postRepository: PostRepository = ServiceLocator.postRepository,
    private val bookRepository: BookRepository = ServiceLocator.bookRepository,
    private val storyRepository: StoryRepository = ServiceLocator.storyRepository,
    private val adminRepository: AdminRepository = ServiceLocator.adminRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<UiState<UserProfile>>(UiState.Loading)
    val uiState: StateFlow<UiState<UserProfile>> = _uiState.asStateFlow()

    private val _contentState = MutableStateFlow(ProfileContentState())
    val contentState: StateFlow<ProfileContentState> = _contentState.asStateFlow()

    private var loadJob: Job? = null

    init { loadProfile() }

    fun loadProfile() {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            val current = authRepository.currentUser.value
            if (current == null) {
                _uiState.value = UiState.Empty
                _contentState.value = ProfileContentState()
                return@launch
            }

            _uiState.value = UiState.Loading

            launch {
                userRepository.getUserProfile(current.uid).collect { result ->
                    _uiState.value = when (result) {
                        is AppResult.Success -> result.data?.let { UiState.Success(it) } ?: UiState.Success(current)
                        is AppResult.Error -> UiState.Error(result.error)
                    }
                }
            }

            launch {
                postRepository.observeUserPosts(current.uid, current.uid).collect { result ->
                    if (result is AppResult.Success) {
                        _contentState.update { it.copy(posts = result.data) }
                    }
                }
            }

            launch {
                bookRepository.getUserWrittenBooks(current.uid).collect { result ->
                    if (result is AppResult.Success) {
                        _contentState.update { it.copy(writtenBooks = result.data) }
                    }
                }
            }

            launch {
                postRepository.observeSavedPosts(current.uid).collect { result ->
                    if (result is AppResult.Success) {
                        _contentState.update { it.copy(savedPosts = result.data) }
                    }
                }
            }

            launch {
                bookRepository.getUserLibrary(current.uid, ShelfType.SAVED).collect { result ->
                    if (result is AppResult.Success) {
                        _contentState.update { it.copy(savedBooks = result.data) }
                    }
                }
            }

            launch {
                refreshTransientProfileMetadata(current.uid)
            }

            launch {
                (adminRepository.observeCosmeticRoles().first() as? AppResult.Success)?.let { result ->
                    _contentState.update { it.copy(cosmeticRoles = result.data) }
                }
            }

            launch {
                val role = (adminRepository.getAdminRole(current.uid) as? AppResult.Success)?.data
                _contentState.update { it.copy(adminRole = role) }
            }
        }
    }

    fun refreshProfile() {
        val current = authRepository.currentUser.value ?: return
        viewModelScope.launch {
            _contentState.update { it.copy(isRefreshing = true) }
            val freshProfile = userRepository.getUserProfileFresh(current.uid)
            if (freshProfile is AppResult.Success && freshProfile.data != null) {
                _uiState.value = UiState.Success(freshProfile.data)
            }
            refreshTransientProfileMetadata(current.uid)
            delay(280)
            _contentState.update { it.copy(isRefreshing = false) }
        }
    }

    private suspend fun refreshTransientProfileMetadata(uid: String) {
        val followersResult = userRepository.getFollowers(uid)
        if (followersResult is AppResult.Success) {
            _contentState.update { it.copy(followers = followersResult.data) }
        }
        val followingResult = userRepository.getFollowing(uid)
        if (followingResult is AppResult.Success) {
            _contentState.update { it.copy(following = followingResult.data) }
        }
        val storiesResult = storyRepository.getActiveStories(setOf(uid))
        if (storiesResult is AppResult.Success) {
            _contentState.update { it.copy(activeStories = storiesResult.data) }
        }
    }

    fun togglePostLike(post: Post) {
        val uid = authRepository.currentUser.value?.uid ?: return
        val newLiked = !post.likedByCurrentUser
        val newCount = (post.likesCount + if (newLiked) 1 else -1).coerceAtLeast(0)
        _contentState.update { state ->
            state.copy(
                posts = state.posts.map {
                    if (it.id == post.id) it.copy(likedByCurrentUser = newLiked, likesCount = newCount) else it
                },
                savedPosts = state.savedPosts.map {
                    if (it.id == post.id) it.copy(likedByCurrentUser = newLiked, likesCount = newCount) else it
                }
            )
        }
        viewModelScope.launch {
            postRepository.toggleLike(post.id, uid)
        }
    }

    fun togglePostSave(post: Post) {
        val uid = authRepository.currentUser.value?.uid ?: return
        val newSaved = !post.savedByCurrentUser
        _contentState.update { state ->
            state.copy(
                posts = state.posts.map {
                    if (it.id == post.id) it.copy(savedByCurrentUser = newSaved) else it
                }
            )
        }
        viewModelScope.launch {
            postRepository.toggleSave(post.id, uid)
        }
    }

    fun deletePost(post: Post) {
        val uid = authRepository.currentUser.value?.uid ?: return
        viewModelScope.launch {
            if (postRepository.deletePost(post.id, uid) is AppResult.Success) {
                _contentState.update { state ->
                    state.copy(
                        posts = state.posts.filterNot { it.id == post.id },
                        savedPosts = state.savedPosts.filterNot { it.id == post.id }
                    )
                }
            }
        }
    }

    fun toggleStoryLike(story: Story) {
        val uid = authRepository.currentUser.value?.uid ?: return
        viewModelScope.launch {
            storyRepository.toggleLike(story.id, uid)
        }
    }

    fun signOut(onSignedOut: () -> Unit) {
        viewModelScope.launch {
            when (authRepository.signOut()) {
                is AppResult.Success -> onSignedOut()
                is AppResult.Error -> Unit
            }
        }
    }
}
