package com.libra.app.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.libra.app.core.di.ServiceLocator
import com.libra.app.core.result.AppResult
import com.libra.app.core.state.UiState
import com.libra.app.domain.model.Book
import com.libra.app.domain.model.Post
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

    init { loadHomeData() }

    fun loadHomeData() {
        viewModelScope.launch {
            val userId = authRepository.currentUser.value?.uid
            if (userId == null) {
                _uiState.value = UiState.Error(com.libra.app.core.result.AppError.Auth("Oturum bulunamadı."))
                return@launch
            }

            combine(
                bookRepository.getFeaturedBooks(),
                bookRepository.getRecentBooks(),
                bookRepository.getUserLibrary(userId, ShelfType.READING),
                postRepository.observeFeed(userId)
            ) { featured, recent, reading, posts -> Quadruple(featured, recent, reading, posts) }
                .collect { values ->
                    val error = listOf(values.first, values.second, values.third, values.fourth)
                        .filterIsInstance<AppResult.Error>()
                        .firstOrNull()
                    if (error != null) {
                        _uiState.value = UiState.Error(error.error)
                        return@collect
                    }

                    val featuredBooks = (values.first as AppResult.Success).data
                    val recentBooks = (values.second as AppResult.Success).data
                    val readingItems = (values.third as AppResult.Success).data
                    val posts = (values.fourth as AppResult.Success).data

                    _uiState.value = UiState.Success(
                        HomeData(
                            currentUser = authRepository.currentUser.value,
                            featuredBooks = featuredBooks,
                            recentBooks = recentBooks,
                            currentlyReading = readingItems.firstOrNull(),
                            posts = posts
                        )
                    )
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

    fun clearPostError() { _postError.value = null }

    private data class Quadruple<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)
}
