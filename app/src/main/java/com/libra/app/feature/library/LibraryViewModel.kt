package com.libra.app.feature.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.libra.app.core.di.ServiceLocator
import com.libra.app.core.result.AppError
import com.libra.app.core.result.AppResult
import com.libra.app.core.state.UiState
import com.libra.app.domain.model.Book
import com.libra.app.domain.model.ShelfType
import com.libra.app.domain.model.UserShelfItem
import com.libra.app.domain.repository.AuthRepository
import com.libra.app.domain.repository.LibraryRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

data class ShelfState(
    val items: List<UserShelfItem> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

data class LibraryState(
    val selectedShelf: ShelfType = ShelfType.READING,
    val searchQuery: String = "",
    val shelves: Map<ShelfType, ShelfState> = mapOf(
        ShelfType.MY_WRITINGS to ShelfState(isLoading = true),
        ShelfType.READING to ShelfState(isLoading = true),
        ShelfType.SAVED to ShelfState(isLoading = true)
    )
) {
    val items: List<UserShelfItem>
        get() = shelves[selectedShelf]?.items.orEmpty()

    val currentShelfState: ShelfState
        get() = shelves[selectedShelf] ?: ShelfState()

    val isCurrentLoading: Boolean
        get() = currentShelfState.isLoading

    val currentError: String?
        get() = currentShelfState.error
}

class LibraryViewModel(
    private val authRepository: AuthRepository = ServiceLocator.authRepository,
    private val libraryRepository: LibraryRepository = ServiceLocator.libraryRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<UiState<LibraryState>>(
        UiState.Success(LibraryState())
    )
    val uiState: StateFlow<UiState<LibraryState>> = _uiState.asStateFlow()

    private var readingJob: Job? = null
    private var savedJob: Job? = null
    private var writtenJob: Job? = null
    private var authWatchJob: Job? = null
    private var activeUserId: String? = null

    init {
        observeAuthState()
    }

    private fun observeAuthState() {
        authWatchJob?.cancel()
        authWatchJob = viewModelScope.launch {
            authRepository.currentUser.collect { user ->
                val uid = user?.uid
                if (uid.isNullOrBlank()) {
                    activeUserId = null
                    cancelAllShelfJobs()
                    if (!authRepository.isAuthenticated.value) {
                        _uiState.value = UiState.Error(AppError.Auth("Kütüphaneni görüntülemek için lütfen oturum açın."))
                    } else {
                        // User is authenticated but profile is hydrating: maintain a friendly loading state
                        _uiState.value = UiState.Loading
                    }
                } else if (uid != activeUserId) {
                    activeUserId = uid
                    startObservingShelves(uid)
                }
            }
        }
    }

    private fun startObservingShelves(userId: String) {
        cancelAllShelfJobs()

        val currentState = (_uiState.value as? UiState.Success)?.data ?: LibraryState()
        _uiState.value = UiState.Success(
            currentState.copy(
                shelves = mapOf(
                    ShelfType.MY_WRITINGS to ShelfState(isLoading = true),
                    ShelfType.READING to ShelfState(isLoading = true),
                    ShelfType.SAVED to ShelfState(isLoading = true)
                )
            )
        )

        observeReadingShelf(userId)
        observeSavedShelf(userId)
        observeWrittenShelf(userId)
    }

    private fun observeReadingShelf(userId: String) {
        readingJob?.cancel()
        readingJob = viewModelScope.launch {
            libraryRepository.getUserLibrary(userId, ShelfType.READING)
                .catch { e ->
                    updateShelf(ShelfType.READING) { it.copy(isLoading = false, error = e.localizedMessage ?: "Okudukların yüklenemedi.") }
                }
                .collect { result ->
                    when (result) {
                        is AppResult.Success -> {
                            updateShelf(ShelfType.READING) {
                                it.copy(items = result.data, isLoading = false, error = null)
                            }
                        }
                        is AppResult.Error -> {
                            updateShelf(ShelfType.READING) {
                                it.copy(isLoading = false, error = result.error.message)
                            }
                        }
                    }
                }
        }
    }

    private fun observeSavedShelf(userId: String) {
        savedJob?.cancel()
        savedJob = viewModelScope.launch {
            libraryRepository.getUserLibrary(userId, ShelfType.SAVED)
                .catch { e ->
                    updateShelf(ShelfType.SAVED) { it.copy(isLoading = false, error = e.localizedMessage ?: "Kaydettiklerin yüklenemedi.") }
                }
                .collect { result ->
                    when (result) {
                        is AppResult.Success -> {
                            updateShelf(ShelfType.SAVED) {
                                it.copy(items = result.data, isLoading = false, error = null)
                            }
                        }
                        is AppResult.Error -> {
                            updateShelf(ShelfType.SAVED) {
                                it.copy(isLoading = false, error = result.error.message)
                            }
                        }
                    }
                }
        }
    }

    private fun observeWrittenShelf(userId: String) {
        writtenJob?.cancel()
        writtenJob = viewModelScope.launch {
            libraryRepository.getUserWrittenBooks(userId)
                .catch { e ->
                    updateShelf(ShelfType.MY_WRITINGS) { it.copy(isLoading = false, error = e.localizedMessage ?: "Yazdıkların yüklenemedi.") }
                }
                .collect { result ->
                    when (result) {
                        is AppResult.Success -> {
                            val items = result.data.map { book ->
                                UserShelfItem(
                                    id = "${userId}_MY_WRITINGS_${book.id}",
                                    userId = userId,
                                    book = book,
                                    shelfType = ShelfType.MY_WRITINGS,
                                    progressPercent = 0,
                                    addedAt = book.createdAt,
                                    updatedAt = book.updatedAt
                                )
                            }
                            updateShelf(ShelfType.MY_WRITINGS) {
                                it.copy(items = items, isLoading = false, error = null)
                            }
                        }
                        is AppResult.Error -> {
                            updateShelf(ShelfType.MY_WRITINGS) {
                                it.copy(isLoading = false, error = result.error.message)
                            }
                        }
                    }
                }
        }
    }

    private fun updateShelf(shelfType: ShelfType, transform: (ShelfState) -> ShelfState) {
        val current = (_uiState.value as? UiState.Success)?.data ?: LibraryState()
        val currentShelves = current.shelves.toMutableMap()
        val existing = currentShelves[shelfType] ?: ShelfState()
        currentShelves[shelfType] = transform(existing)
        _uiState.value = UiState.Success(current.copy(shelves = currentShelves))
    }

    fun selectShelf(shelfType: ShelfType) {
        val current = (_uiState.value as? UiState.Success)?.data ?: return
        if (current.selectedShelf != shelfType) {
            _uiState.value = UiState.Success(current.copy(selectedShelf = shelfType))
        }
    }

    fun updateSearchQuery(query: String) {
        val current = (_uiState.value as? UiState.Success)?.data ?: return
        _uiState.value = UiState.Success(current.copy(searchQuery = query))
    }

    fun retryCurrentShelf() {
        val current = (_uiState.value as? UiState.Success)?.data ?: return
        retryShelf(current.selectedShelf)
    }

    fun retryShelf(shelfType: ShelfType) {
        val userId = activeUserId ?: authRepository.currentUser.value?.uid ?: return
        updateShelf(shelfType) { it.copy(isLoading = true, error = null) }
        when (shelfType) {
            ShelfType.READING -> observeReadingShelf(userId)
            ShelfType.SAVED -> observeSavedShelf(userId)
            ShelfType.MY_WRITINGS -> observeWrittenShelf(userId)
        }
    }

    fun removeFromShelf(book: Book, shelfType: ShelfType) {
        val userId = activeUserId ?: authRepository.currentUser.value?.uid ?: return
        val current = (_uiState.value as? UiState.Success)?.data
        if (current != null) {
            val shelf = current.shelves[shelfType]
            if (shelf != null) {
                val updatedItems = shelf.items.filter { it.book.id != book.id }
                updateShelf(shelfType) { it.copy(items = updatedItems) }
            }
        }

        viewModelScope.launch {
            val result = libraryRepository.removeBookFromShelf(userId, book.id, shelfType)
            if (result is AppResult.Error) {
                retryShelf(shelfType)
            }
        }
    }

    fun addToShelf(book: Book, shelfType: ShelfType) {
        val userId = activeUserId ?: authRepository.currentUser.value?.uid ?: return
        viewModelScope.launch {
            libraryRepository.addBookToShelf(userId, book.id, shelfType)
        }
    }

    // Backward-compatible API
    fun loadShelf(shelfType: ShelfType) = selectShelf(shelfType)
    fun loadShelfDefault() = selectShelf(ShelfType.READING)

    private fun cancelAllShelfJobs() {
        readingJob?.cancel()
        savedJob?.cancel()
        writtenJob?.cancel()
    }

    override fun onCleared() {
        super.onCleared()
        cancelAllShelfJobs()
        authWatchJob?.cancel()
    }
}
