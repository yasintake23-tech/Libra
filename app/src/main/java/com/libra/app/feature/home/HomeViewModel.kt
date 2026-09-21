package com.libra.app.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.libra.app.core.di.ServiceLocator
import com.libra.app.core.result.AppResult
import com.libra.app.core.state.UiState
import com.libra.app.domain.model.Book
import com.libra.app.domain.model.ShelfType
import com.libra.app.domain.model.UserProfile
import com.libra.app.domain.model.UserShelfItem
import com.libra.app.domain.repository.AuthRepository
import com.libra.app.domain.repository.BookRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

data class HomeData(
    val currentUser: UserProfile?,
    val featuredBooks: List<Book>,
    val recentBooks: List<Book>,
    val currentlyReading: UserShelfItem?
)

class HomeViewModel(
    private val authRepository: AuthRepository = ServiceLocator.authRepository,
    private val bookRepository: BookRepository = ServiceLocator.bookRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<UiState<HomeData>>(UiState.Loading)
    val uiState: StateFlow<UiState<HomeData>> = _uiState.asStateFlow()

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
                bookRepository.getUserLibrary(userId, ShelfType.READING)
            ) { featured, recent, reading -> Triple(featured, recent, reading) }
                .collect { (featured, recent, reading) ->
                    val error = listOf(featured, recent, reading).filterIsInstance<AppResult.Error>().firstOrNull()
                    if (error != null) {
                        _uiState.value = UiState.Error(error.error)
                        return@collect
                    }

                    val featuredBooks = (featured as AppResult.Success).data
                    val recentBooks = (recent as AppResult.Success).data
                    val readingItems = (reading as AppResult.Success).data
                    _uiState.value = UiState.Success(
                        HomeData(
                            currentUser = authRepository.currentUser.value,
                            featuredBooks = featuredBooks,
                            recentBooks = recentBooks,
                            currentlyReading = readingItems.firstOrNull()
                        )
                    )
                }
        }
    }
}
