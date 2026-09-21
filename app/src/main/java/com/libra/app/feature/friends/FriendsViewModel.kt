package com.libra.app.feature.friends

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.libra.app.core.di.ServiceLocator
import com.libra.app.core.result.AppResult
import com.libra.app.core.state.UiState
import com.libra.app.domain.model.FriendConnection
import com.libra.app.domain.model.UserProfile
import com.libra.app.domain.repository.AuthRepository
import com.libra.app.domain.repository.UserRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class FriendsState(val friendsList: List<FriendConnection> = emptyList(), val suggestedUsers: List<UserProfile> = emptyList(), val searchQuery: String = "")

class FriendsViewModel(private val authRepository: AuthRepository = ServiceLocator.authRepository, private val userRepository: UserRepository = ServiceLocator.userRepository) : ViewModel() {
    private val _uiState = MutableStateFlow<UiState<FriendsState>>(UiState.Success(FriendsState()))
    val uiState: StateFlow<UiState<FriendsState>> = _uiState.asStateFlow()
    private var searchJob: Job? = null

    fun loadSocialData() {
        val query = (_uiState.value as? UiState.Success)?.data?.searchQuery.orEmpty()
        if (query.isBlank()) _uiState.value = UiState.Success(FriendsState(searchQuery = "")) else updateSearchQuery(query)
    }

    fun updateSearchQuery(query: String) {
        val normalized = query.trim()
        val current = (_uiState.value as? UiState.Success)?.data ?: FriendsState()
        _uiState.value = UiState.Success(current.copy(searchQuery = query))
        searchJob?.cancel()
        if (normalized.isBlank()) { _uiState.value = UiState.Success(current.copy(searchQuery = "", suggestedUsers = emptyList())); return }
        searchJob = viewModelScope.launch {
            userRepository.searchUsers(normalized).collect { result ->
                when (result) {
                    is AppResult.Success -> _uiState.value = UiState.Success(current.copy(searchQuery = query, suggestedUsers = result.data.filter { it.uid != authRepository.currentUser.value?.uid }))
                    is AppResult.Error -> _uiState.value = UiState.Error(result.error)
                }
            }
        }
    }
}
