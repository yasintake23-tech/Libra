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
import com.libra.app.domain.model.Story
import com.libra.app.domain.repository.AuthRepository
import com.libra.app.domain.repository.BookRepository
import com.libra.app.domain.repository.PostRepository
import com.libra.app.domain.repository.StoryRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

const val POST_PAGE_SIZE = 5

data class HomeData(
    val currentUser: UserProfile?,
    val featuredBooks: List<Book>,
    val recentBooks: List<Book>,
    val currentlyReading: UserShelfItem?,
    val posts: List<Post> = emptyList(),
    val stories: List<Story> = emptyList(),
    val hasMorePosts: Boolean = false,
    val isLoadingMorePosts: Boolean = false,
    val isRefreshing: Boolean = false
)

class HomeViewModel(
    private val authRepository: AuthRepository = ServiceLocator.authRepository,
    private val bookRepository: BookRepository = ServiceLocator.bookRepository,
    private val postRepository: PostRepository = ServiceLocator.postRepository,
    private val storyRepository: StoryRepository = ServiceLocator.storyRepository,
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

    private var allPostsPool: List<Post> = emptyList()
    private var rawPostsCache: List<Post> = emptyList()
    private val _visiblePostsCount = MutableStateFlow(POST_PAGE_SIZE)

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private val _isLoadingMorePosts = MutableStateFlow(false)
    val isLoadingMorePosts: StateFlow<Boolean> = _isLoadingMorePosts.asStateFlow()

    private var homeJob: Job? = null
    private var postsJob: Job? = null
    private var commentsJob: Job? = null

    // Firestore can update the like document and post counter in separate
    // listener snapshots. Keep both the desired state and the optimistic
    // counter until the listener has caught up with the whole operation.
    private data class PendingLike(
        val desiredLiked: Boolean,
        val expectedCount: Int
    )

    private val pendingLikeOverrides = mutableMapOf<String, PendingLike>()

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
                posts = emptyList(),
                hasMorePosts = false,
                isLoadingMorePosts = false,
                isRefreshing = false
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
                    val rawPosts = result.data
                    rawPostsCache = rawPosts

                    if (allPostsPool.isEmpty()) {
                        // Initial load: shuffle randomly!
                        allPostsPool = rawPosts.shuffled()
                    } else {
                        // Background snapshot update: preserve existing order, update contents
                        val rawMap = rawPosts.associateBy { it.id }
                        val currentIds = allPostsPool.map { it.id }.toSet()
                        val updatedPool = allPostsPool.map { existing -> rawMap[existing.id] ?: existing }.toMutableList()
                        val newItems = rawPosts.filter { it.id !in currentIds }
                        if (newItems.isNotEmpty()) {
                            updatedPool.addAll(0, newItems)
                        }
                        allPostsPool = updatedPool
                    }
                    updateVisiblePosts()
                }
            }
        }

        viewModelScope.launch {
            loadStories(userId)
        }
    }

    private fun updateVisiblePosts() {
        val count = _visiblePostsCount.value
        val visible = allPostsPool.take(count).map { post ->
            val pending = pendingLikeOverrides[post.id]
            if (pending == null) {
                post
            } else {
                val caughtUp =
                    post.likedByCurrentUser == pending.desiredLiked &&
                        post.likesCount == pending.expectedCount

                if (caughtUp) {
                    pendingLikeOverrides.remove(post.id)
                    post
                } else {
                    // Firebase may deliver the like state before
                    // the counter (or vice versa). Keep the complete
                    // optimistic result until both match.
                    post.copy(
                        likedByCurrentUser = pending.desiredLiked,
                        likesCount = pending.expectedCount
                    )
                }
            }
        }
        val hasMore = count < allPostsPool.size
        updateHome {
            it.copy(
                posts = visible,
                hasMorePosts = hasMore,
                isLoadingMorePosts = _isLoadingMorePosts.value,
                isRefreshing = _isRefreshing.value
            )
        }
    }

    fun refreshHome() {
        val userId = authRepository.currentUser.value?.uid ?: return
        viewModelScope.launch {
            _isRefreshing.value = true
            updateHome { it.copy(isRefreshing = true) }
            _visiblePostsCount.value = POST_PAGE_SIZE

            // Shuffle randomly on refresh!
            if (rawPostsCache.isNotEmpty()) {
                allPostsPool = rawPostsCache.shuffled()
            }
            updateVisiblePosts()

            loadStories(userId)

            kotlinx.coroutines.delay(350)
            _isRefreshing.value = false
            updateHome { it.copy(isRefreshing = false) }
        }
    }

    fun loadMorePosts() {
        if (_isLoadingMorePosts.value || _visiblePostsCount.value >= allPostsPool.size) return
        viewModelScope.launch {
            _isLoadingMorePosts.value = true
            updateHome { it.copy(isLoadingMorePosts = true) }
            kotlinx.coroutines.delay(200)
            _visiblePostsCount.value += POST_PAGE_SIZE
            updateVisiblePosts()
            _isLoadingMorePosts.value = false
            updateHome { it.copy(isLoadingMorePosts = false) }
        }
    }

    private fun updateHome(transform: (HomeData) -> HomeData) {
        val current = (_uiState.value as? UiState.Success)?.data ?: return
        _uiState.value = UiState.Success(transform(current))
    }

    fun refreshStories() {
        val userId = authRepository.currentUser.value?.uid ?: return
        viewModelScope.launch { loadStories(userId) }
    }

    private suspend fun loadStories(userId: String) {
        val followingIds = when (val result = ServiceLocator.userRepository.getFollowingIds(userId)) {
            is AppResult.Success -> result.data
            is AppResult.Error -> emptySet()
        }
        val authorIds = followingIds + userId
        when (val result = storyRepository.getActiveStories(authorIds)) {
            is AppResult.Success -> updateHome { it.copy(stories = result.data) }
            is AppResult.Error -> Unit
        }
    }

    fun toggleStoryLike(story: Story) {
        val userId = authRepository.currentUser.value?.uid ?: return
        viewModelScope.launch {
            when (val result = storyRepository.toggleLike(story.id, userId)) {
                is AppResult.Success -> refreshStories()
                is AppResult.Error -> _postError.value = result.error.message
            }
        }
    }

    fun createPost(text: String) {
        createRichPost(
            title = "",
            text = text,
            tags = emptyList(),
            mediaUrl = "",
            mediaType = ""
        )
    }

    fun createRichPost(
        title: String,
        text: String,
        tags: List<String>,
        mediaUrl: String,
        mediaType: String,
        onComplete: (Boolean) -> Unit = {}
    ) {
        val userId = authRepository.currentUser.value?.uid ?: return
        viewModelScope.launch {
            _isPosting.value = true
            _postError.value = null
            try {
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
                    is AppResult.Success -> {
                        val newPost = result.data
                        allPostsPool = listOf(newPost) + allPostsPool
                        rawPostsCache = listOf(newPost) + rawPostsCache
                        updateVisiblePosts()
                        onComplete(true)
                    }
                    is AppResult.Error -> {
                        _postError.value = result.error.message
                        onComplete(false)
                    }
                }
            } finally {
                _isPosting.value = false
            }
        }
    }

    fun createStory(
        text: String,
        mediaUrl: String,
        mediaType: String,
        onComplete: (Boolean) -> Unit = {}
    ) {
        val user = authRepository.currentUser.value
        if (user == null) {
            _postError.value = "Oturum bulunamadı."
            onComplete(false)
            return
        }

        viewModelScope.launch {
            _isPosting.value = true
            _postError.value = null
            try {
                when (
                    val result = storyRepository.createStory(
                        authorId = user.uid,
                        authorName = user.displayName ?: "Libra kullanıcısı",
                        authorPhotoUrl = authRepository.currentUser.value?.profileImageUrl.orEmpty(),
                        text = text,
                        mediaUrl = mediaUrl,
                        mediaType = mediaType
                    )
                ) {
                    is AppResult.Success -> {
                        refreshStories()
                        onComplete(true)
                    }
                    is AppResult.Error -> {
                        _postError.value = result.error.message
                        if (mediaUrl.isNotBlank()) {
                            runCatching { storageRepository.deleteMedia(mediaUrl) }
                        }
                        onComplete(false)
                    }
                }
            } catch (e: Exception) {
                _postError.value = "Hikâye paylaşılırken hata oluştu: ${e.localizedMessage ?: "Bilinmeyen hata."}"
                onComplete(false)
            } finally {
                _isPosting.value = false
            }
        }
    }
    fun toggleLike(post: Post) {
        val userId = authRepository.currentUser.value?.uid ?: return
        val current = (_uiState.value as? UiState.Success)?.data?.posts?.firstOrNull { it.id == post.id }
            ?: post
        val desiredLiked = !current.likedByCurrentUser
        val expectedCount =
            (current.likesCount + if (desiredLiked) 1 else -1).coerceAtLeast(0)

        // Keep the complete optimistic result until Firestore confirms both
        // the like state and the counter. This prevents a transient 7 -> 8 -> 7
        // jump when the two Firestore listeners arrive in different snapshots.
        pendingLikeOverrides[post.id] = PendingLike(desiredLiked, expectedCount)
        allPostsPool = allPostsPool.map { item ->
            if (item.id != post.id) item else item.copy(
                likedByCurrentUser = desiredLiked,
                likesCount = expectedCount
            )
        }
        updateVisiblePosts()

        viewModelScope.launch {
            when (val result = postRepository.toggleLike(post.id, userId)) {
                is AppResult.Error -> {
                    // Only roll back if this request is still the latest user intent.
                    if (pendingLikeOverrides[post.id]?.desiredLiked == desiredLiked) {
                        pendingLikeOverrides.remove(post.id)
                        allPostsPool = allPostsPool.map { item ->
                            if (item.id == post.id) item.copy(
                                likedByCurrentUser = current.likedByCurrentUser,
                                likesCount = current.likesCount
                            ) else item
                        }
                        updateVisiblePosts()
                    }
                    _postError.value = result.error.message
                }
                is AppResult.Success -> {
                    // A slower earlier request must never overwrite a newer tap.
                    if (pendingLikeOverrides[post.id]?.desiredLiked == desiredLiked &&
                        result.data != desiredLiked
                    ) {
                        pendingLikeOverrides[post.id] = PendingLike(
                            desiredLiked = result.data,
                            expectedCount = current.likesCount + if (result.data) 1 else -1
                        )
                    }
                }
            }
        }
    }

    fun toggleSave(post: Post) {
        val userId = authRepository.currentUser.value?.uid ?: return

        // Optimistic UI: reflect the bookmark immediately instead of waiting
        // for the Firestore listener / a screen recreation.
        val previousSaved = post.savedByCurrentUser
        allPostsPool = allPostsPool.map { current ->
            if (current.id == post.id) {
                current.copy(savedByCurrentUser = !previousSaved)
            } else {
                current
            }
        }
        updateVisiblePosts()

        viewModelScope.launch {
            when (val result = postRepository.toggleSave(post.id, userId)) {
                is AppResult.Error -> {
                    // Roll back the optimistic state when persistence fails.
                    allPostsPool = allPostsPool.map { current ->
                        if (current.id == post.id) {
                            current.copy(savedByCurrentUser = previousSaved)
                        } else {
                            current
                        }
                    }
                    updateVisiblePosts()
                    _postError.value = result.error.message
                }
                is AppResult.Success -> Unit
            }
        }
    }

    fun deletePost(post: Post) {
        val userId = authRepository.currentUser.value?.uid ?: return
        allPostsPool = allPostsPool.filter { it.id != post.id }
        rawPostsCache = rawPostsCache.filter { it.id != post.id }
        updateVisiblePosts()

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

    fun addComment(post: Post, text: String, parentCommentId: String = "") {
        val userId = authRepository.currentUser.value?.uid ?: return
        viewModelScope.launch {
            _isCommenting.value = true
            _commentError.value = null
            when (val result = postRepository.addComment(post.id, userId, text, parentCommentId)) {
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
