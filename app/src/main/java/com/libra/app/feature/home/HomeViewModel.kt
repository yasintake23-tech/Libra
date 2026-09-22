package com.libra.app.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.libra.app.core.di.ServiceLocator
import com.libra.app.core.result.AppResult
import com.libra.app.core.state.UiState
import com.libra.app.domain.model.Book
import com.libra.app.domain.model.Post
import com.libra.app.domain.model.PostComment
import com.libra.app.domain.model.ShelfType
import com.libra.app.domain.model.UserProfile
import com.libra.app.domain.model.UserShelfItem
import com.libra.app.domain.repository.AuthRepository
import com.libra.app.domain.repository.BookRepository
import com.libra.app.domain.repository.PostRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

data class HomeData(
    val currentUser: UserProfile?,
    val featuredBooks: List<Book>,
    val recentBooks: List<Book>,
    val currentlyReading: UserShelfItem?,
    val posts: List<Post> = emptyList()
)

class HomeViewModel(
    private val authRepository: AuthRepository = ServiceLocator.authRepository,
    private val bookRepository: BookRepository = ServiceLocator.bookRepository,
    private val postRepository: PostRepository = ServiceLocator.postRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<UiState<HomeData>>(UiState.Loading)
    val uiState: StateFlow<UiState<HomeData>> = _uiState.asStateFlow()

    private val _isPosting = MutableStateFlow(false)
    val isPosting: StateFlow<Boolean> = _isPosting.asStateFlow()

    private val _postError = MutableStateFlow<String?>(null)
    val postError: StateFlow<String?> = _postError.asStateFlow()

    private val _comments = MutableStateFlow<List<PostComment>>(emptyList())
    val comments: StateFlow<List<PostComment>> = _comments.asStateFlow()
    private val _commentError = MutableStateFlow<String?>(null)
    val commentError: StateFlow<String?> = _commentError.asStateFlow()
    private val _isCommenting = MutableStateFlow(false)
    val isCommenting: StateFlow<Boolean> = _isCommenting.asStateFlow()

    private var homeJob: Job? = null
    private var postsJob: Job? = null
    private var commentsJob: Job? = null

    init { loadHomeData() }

    fun loadHomeData() {
        homeJob?.cancel()
        postsJob?.cancel()

        val userId = authRepository.currentUser.value?.uid
        if (userId == null) {
            _uiState.value = UiState.Error(com.libra.app.core.result.AppError.Auth("Oturum bulunamadı."))
            return
        }

        homeJob = viewModelScope.launch {
            combine(
                bookRepository.getFeaturedBooks(),
                bookRepository.getRecentBooks(),
                bookRepository.getUserLibrary(userId, ShelfType.READING)
            ) { featured, recent, reading -> Triple(featured, recent, reading) }
                .collectLatest { values ->
                    val error = listOf(values.first, values.second, values.third)
                        .filterIsInstance<AppResult.Error>()
                        .firstOrNull()
                    if (error != null) {
                        _uiState.value = UiState.Error(error.error)
                        return@collectLatest
                    }

                    val existingPosts = (_uiState.value as? UiState.Success)?.data?.posts.orEmpty()
                    _uiState.value = UiState.Success(
                        HomeData(
                            currentUser = authRepository.currentUser.value,
                            featuredBooks = (values.first as AppResult.Success).data,
                            recentBooks = (values.second as AppResult.Success).data,
                            currentlyReading = (values.third as AppResult.Success).data.firstOrNull(),
                            posts = existingPosts
                        )
                    )
                }
        }

        postsJob = viewModelScope.launch {
            postRepository.observeFeed(userId).collect { result ->
                if (result is AppResult.Success) {
                    val current = (_uiState.value as? UiState.Success)?.data ?: return@collect
                    _uiState.value = UiState.Success(current.copy(posts = result.data))
                }
            }
        }
    }

    fun createPost(text: String) {
        val userId = authRepository.currentUser.value?.uid ?: return
        viewModelScope.launch {
            _isPosting.value = true
            _postError.value = null
            when (val result = postRepository.createPost(userId, text)) {
                is AppResult.Success -> Unit
                is AppResult.Error -> _postError.value = result.error.message
            }
            _isPosting.value = false
        }
    }

    fun toggleLike(post: Post) {
        val userId = authRepository.currentUser.value?.uid ?: return
        viewModelScope.launch {
            when (val result = postRepository.toggleLike(post.id, userId)) {
                is AppResult.Error -> _postError.value = result.error.message
                is AppResult.Success -> Unit
            }
        }
    }

    fun deletePost(post: Post) {
        val userId = authRepository.currentUser.value?.uid ?: return
        viewModelScope.launch {
            when (val result = postRepository.deletePost(post.id, userId)) {
                is AppResult.Error -> _postError.value = result.error.message
                is AppResult.Success -> Unit
            }
        }
    }

    fun openComments(post: Post) {
        commentsJob?.cancel()
        _comments.value = emptyList()
        _commentError.value = null
        commentsJob = viewModelScope.launch {
            postRepository.observeComments(post.id).collect { result ->
                when (result) {
                    is AppResult.Success -> _comments.value = result.data
                    is AppResult.Error -> _commentError.value = result.error.message
                }
            }
        }
    }

    fun closeComments() {
        commentsJob?.cancel()
        commentsJob = null
        _comments.value = emptyList()
        _commentError.value = null
    }

    fun addComment(post: Post, text: String) {
        val userId = authRepository.currentUser.value?.uid ?: return
        viewModelScope.launch {
            _isCommenting.value = true
            _commentError.value = null
            when (val result = postRepository.addComment(post.id, userId, text)) {
                is AppResult.Error -> _commentError.value = result.error.message
                is AppResult.Success -> Unit
            }
            _isCommenting.value = false
        }
    }

    fun deleteComment(post: Post, comment: PostComment) {
        val userId = authRepository.currentUser.value?.uid ?: return
        viewModelScope.launch {
            when (val result = postRepository.deleteComment(post.id, comment.id, userId)) {
                is AppResult.Error -> _commentError.value = result.error.message
                is AppResult.Success -> Unit
            }
        }
    }

    fun clearCommentError() { _commentError.value = null }

    fun clearPostError() { _postError.value = null }

    private data class Quadruple<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)
}
