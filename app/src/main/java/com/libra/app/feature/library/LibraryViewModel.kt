package com.libra.app.feature.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.libra.app.core.di.ServiceLocator
import com.libra.app.core.result.AppError
import com.libra.app.core.result.AppResult
import com.libra.app.core.state.UiState
import com.libra.app.domain.model.ShelfType
import com.libra.app.domain.model.UserShelfItem
import com.libra.app.domain.repository.AuthRepository
import com.libra.app.domain.repository.BookRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

data class LibraryState(
    val selectedShelf: ShelfType = ShelfType.READING,
    val items: List<UserShelfItem> = emptyList(),
    val searchQuery: String = ""
)

class LibraryViewModel(
    private val authRepository: AuthRepository = ServiceLocator.authRepository,
    private val bookRepository: BookRepository = ServiceLocator.bookRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<UiState<LibraryState>>(UiState.Loading)
    val uiState: StateFlow<UiState<LibraryState>> = _uiState.asStateFlow()
    private var currentSearch = ""
    private var shelfJob: Job? = null

    init { loadShelf(ShelfType.READING) }

    fun loadShelf(shelfType: ShelfType) {
        shelfJob?.cancel()
        shelfJob = viewModelScope.launch {
            val userId = authRepository.currentUser.value?.uid
            if (userId == null) {
                _uiState.value = UiState.Error(AppError.Auth("Oturum bulunamadı."))
                return@launch
            }

            _uiState.value = UiState.Loading

            val flow = if (shelfType == ShelfType.MY_WRITINGS) {
                bookRepository.getUserWrittenBooks(userId)
            } else {
                bookRepository.getUserLibrary(userId, shelfType)
            }

            // Keep one listener alive. Only the first Firebase emission has an
            // 8-second timeout, so a slow connection cannot leave the screen
            // spinning forever while later updates still remain live.
            val firstResult = kotlinx.coroutines.CompletableDeferred<Unit>()
            val collector = launch {
                flow.collect { result ->
                    firstResult.complete(Unit)
                    when (result) {
                        is AppResult.Success -> {
                            val items = if (shelfType == ShelfType.MY_WRITINGS) {
                                result.data.map { book ->
                                    UserShelfItem(
                                        id = "${userId}_MY_WRITINGS_${book.id}",
                                        userId = userId,
                                        book = book,
                                        shelfType = ShelfType.MY_WRITINGS,
                                        progressPercent = 0,
                                        addedAt = book.updatedAt
                                    )
                                }
                            } else {
                                result.data
                            }
                            _uiState.value = UiState.Success(
                                LibraryState(shelfType, items, currentSearch)
                            )
                        }
                        is AppResult.Error -> {
                            _uiState.value = UiState.Error(result.error)
                        }
                    }
                }
            }

            val received = withTimeoutOrNull(8_000L) {
                firstResult.await()
                true
            } ?: false

            if (!received) {
                collector.cancel()
                _uiState.value = UiState.Error(
                    AppError.Database(
                        "Kütüphane yüklenemedi. Firebase bağlantısı yanıt vermedi."
                    )
                )
            } else {
                collector.join()
            }
        }
    }

    fun loadShelfDefault() = loadShelf(ShelfType.READING)

    fun updateSearchQuery(query: String) {
        currentSearch = query
        val currentState = (_uiState.value as? UiState.Success)?.data ?: return
        _uiState.value = UiState.Success(currentState.copy(searchQuery = query))
    }
}
