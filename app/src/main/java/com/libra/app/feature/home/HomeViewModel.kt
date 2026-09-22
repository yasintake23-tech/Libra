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
import com.libra.app.domain.model.StorageUploadRequest
import com.libra.app.domain.repository.AuthRepository
import com.libra.app.domain.repository.BookRepository
import com.libra.app.domain.repository.PostRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

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
    private val postRepository: PostRepository = ServiceLocator.postRepository,
    private val storageRepository: com.libra.app.domain.repository.StorageRepository = ServiceLocator.storageRepository
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

        // Do not let a slow/unavailable library listener block the whole Home screen.
        _uiState.value = UiState.Success(
            HomeData(
                currentUser = authRepository.currentUser.value,
                featuredBooks = emptyList(),
                recentBooks = emptyList(),
                currentlyReading = null,
                posts = emptyList()
            )
        )

        homeJob = viewModelScope.launch {
            launch {
                bookRepository.getFeaturedBooks().collect { result ->
                    if (result is AppResult.Success) {
                        updateHome { it.copy(featuredBooks = result.data) }
                    }
                }
            }

            launch {
                bookRepository.getRecentBooks().collect { result ->
                    if (result is AppResult.Success) {
                        updateHome { it.copy(recentBooks = result.data) }
                    }
                }
            }

            launch {
                val reading = withTimeoutOrNull(8_000L) {
                    bookRepository.getUserLibrary(userId, ShelfType.READING)
                        .first { it is AppResult.Success || it is AppResult.Error }
                }
                if (reading is AppResult.Success) {
                    updateHome { it.copy(currentlyReading = reading.data.firstOrNull()) }
                }
            }
        }

        postsJob = viewModelScope.launch {
            postRepository.observeFeed(userId).collect { result ->
                if (result is AppResult.Success) {
                    updateHome { it.copy(posts = result.data) }
                }
            }
        }
    }

    private fun updateHome(transform: (HomeData) -> HomeData) {
        val current = (_uiState.value as? UiState.Success)?.data ?: return
        _uiState.value = UiState.Success(transform(current))
    }

    fun createPost(text: String) {
        createRichPost(
            title = "",
            text = text,
            tags = emptyList(),
            imageBytes = null,
            imageFileName = "",
            imageContentType = ""
        )
    }

    fun createRichPost(
        title: String,
        text: String,
        tags: List<String>,
        imageBytes: ByteArray?,
        imageFileName: String,
        imageContentType: String
    ) {
        val userId = authRepository.currentUser.value?.uid ?: return
        viewModelScope.launch {
            _isPosting.value = true
            _postError.value = null
            try {
                var mediaUrl = ""
                var mediaType = ""
                if (imageBytes != null && imageBytes.isNotEmpty()) {
                    val upload = storageRepository.uploadMedia(
                        StorageUploadRequest(
                            fileName = imageFileName.ifBlank { "post.jpg" },
                            bytes = imageBytes,
                            contentType = imageContentType.ifBlank { "image/jpeg" },
                            targetDirectory = "users/$userId"
                        )
                    ).first()
                    when (upload) {
                        is AppResult.Success -> {
                            mediaUrl = upload.data
                            mediaType = "image"
                        }
                        is AppResult.Error -> {
                            _postError.value = upload.error.message
                            return@launch
                        }
                    }
                }

                when (
                    val result = postRepository.createPost(
                        authorId = userId,
                        text = text,
                        title = title,
                        mediaUrl = mediaUrl,
                        mediaType = mediaType,
                        tags = tags
                    )
                ) {
                    is AppResult.Success -> Unit
                    is AppResult.Error -> _postError.value = result.error.message
                }
            } finally {
                _isPosting.value = false
            }
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

    fun toggleSave(post: Post) {
        val userId = authRepository.currentUser.value?.uid ?: return
        viewModelScope.launch {
            when (val result = postRepository.toggleSave(post.id, userId)) {
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
}
