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

            // Keep a single Firebase listener alive after the first result.
            // The previous implementation created one listener for first() and
            // then another for collect(), which made the library unnecessarily
            // fragile when RTDB was slow or the first listener was cancelled.
            val flow = if (shelfType == ShelfType.MY_WRITINGS) {
                bookRepository.getUserWrittenBooks(userId).let { source ->
                    kotlinx.coroutines.flow.flow {
                        source.collect { emit(it) }
                    }
                }
            } else {
                bookRepository.getUserLibrary(userId, shelfType)
            }

            var receivedFirstResult = false
            val firstResult = withTimeoutOrNull(8_000L) {
                flow.collect { result ->
                    receivedFirstResult = true
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

            if (!receivedFirstResult && firstResult == null && shelfJob?.isActive == true) {
                _uiState.value = UiState.Error(
                    AppError.Database(
                        "Kütüphane yüklenemedi. Firebase bağlantısı yanıt vermedi."
                    )
                )
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
