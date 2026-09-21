package com.libra.app.feature.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.libra.app.core.di.ServiceLocator
import com.libra.app.core.result.AppResult
import com.libra.app.core.state.UiState
import com.libra.app.domain.model.ShelfType
import com.libra.app.domain.model.UserShelfItem
import com.libra.app.domain.repository.AuthRepository
import com.libra.app.domain.repository.BookRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

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

    init { loadShelf(ShelfType.READING) }

    fun loadShelf(shelfType: ShelfType) {
        viewModelScope.launch {
            val userId = authRepository.currentUser.value?.uid
            if (userId == null) {
                _uiState.value = UiState.Error(com.libra.app.core.result.AppError.Auth("Oturum bulunamadı."))
                return@launch
            }
            _uiState.value = UiState.Loading
            bookRepository.getUserLibrary(userId, shelfType).collect { result ->
                _uiState.value = when (result) {
                    is AppResult.Success -> UiState.Success(LibraryState(shelfType, result.data, currentSearch))
                    is AppResult.Error -> UiState.Error(result.error)
                }
            }
        }
    }

    fun updateSearchQuery(query: String) {
        currentSearch = query
        val currentState = (_uiState.value as? UiState.Success)?.data ?: return
        _uiState.value = UiState.Success(currentState.copy(searchQuery = query))
    }
}
