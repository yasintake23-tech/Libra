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

data class FriendsState(
    val friendsList: List<FriendConnection> = emptyList(),
    val suggestedUsers: List<UserProfile> = emptyList(),
    val searchQuery: String = "",
    val followingIds: Set<String> = emptySet(),
    val actionUserIds: Set<String> = emptySet()
)

class FriendsViewModel(private val authRepository: AuthRepository = ServiceLocator.authRepository, private val userRepository: UserRepository = ServiceLocator.userRepository) : ViewModel() {
    private val _uiState = MutableStateFlow<UiState<FriendsState>>(UiState.Success(FriendsState()))
    val uiState: StateFlow<UiState<FriendsState>> = _uiState.asStateFlow()
    private var searchJob: Job? = null

    fun loadSocialData() {
        viewModelScope.launch {
            val currentUid = authRepository.currentUser.value?.uid.orEmpty()
            if (currentUid.isBlank()) {
                _uiState.value = UiState.Error(
                    com.libra.app.core.result.AppError.Auth("Oturum bulunamadı.")
                )
                return@launch
            }

            val current = (_uiState.value as? UiState.Success)?.data ?: FriendsState()
            when (val result = userRepository.getFollowingIds(currentUid)) {
                is AppResult.Success -> {
                    _uiState.value = UiState.Success(current.copy(followingIds = result.data))
                    if (current.searchQuery.isBlank()) {
                        _uiState.value = UiState.Success(current.copy(followingIds = result.data))
                    } else {
                        updateSearchQuery(current.searchQuery)
                    }
                }

                is AppResult.Error -> {
                    // Takip listesi okunamasa bile kullanıcı araması kullanılabilir.
                    _uiState.value = UiState.Success(current)
                    if (current.searchQuery.isNotBlank()) updateSearchQuery(current.searchQuery)
                }
            }
        }
    }

    fun updateSearchQuery(query: String) {
        val normalized = query.trim()
        val current = (_uiState.value as? UiState.Success)?.data ?: FriendsState()
        searchJob?.cancel()

        if (normalized.isBlank()) {
            _uiState.value = UiState.Success(
                current.copy(searchQuery = "", suggestedUsers = emptyList())
            )
            return
        }

        _uiState.value = UiState.Success(current.copy(searchQuery = query))
        searchJob = viewModelScope.launch {
            userRepository.searchUsers(normalized).collect { result ->
                when (result) {
                    is AppResult.Success -> {
                        val uid = authRepository.currentUser.value?.uid
                        val latest = (_uiState.value as? UiState.Success)?.data ?: current
                        _uiState.value = UiState.Success(
                            latest.copy(
                                searchQuery = query,
                                suggestedUsers = result.data.filter { it.uid != uid }
                            )
                        )
                    }

                    is AppResult.Error -> _uiState.value = UiState.Error(result.error)
                }
            }
        }
    }

    fun toggleFollowUser(user: UserProfile) {
        val followerId = authRepository.currentUser.value?.uid.orEmpty()
        if (followerId.isBlank() || user.uid.isBlank() || followerId == user.uid) return

        val current = (_uiState.value as? UiState.Success)?.data ?: FriendsState()
        if (current.actionUserIds.contains(user.uid)) return

        val currentlyFollowing = current.followingIds.contains(user.uid)
        _uiState.value = UiState.Success(
            current.copy(actionUserIds = current.actionUserIds + user.uid)
        )

        viewModelScope.launch {
            val result = if (currentlyFollowing) {
                userRepository.unfollowUser(followerId, user.uid)
            } else {
                userRepository.followUser(followerId, user.uid)
            }

            val latest = (_uiState.value as? UiState.Success)?.data ?: current
            when (result) {
                is AppResult.Success -> {
                    val ids = if (currentlyFollowing) {
                        latest.followingIds - user.uid
                    } else {
                        latest.followingIds + user.uid
                    }
                    _uiState.value = UiState.Success(
                        latest.copy(
                            followingIds = ids,
                            actionUserIds = latest.actionUserIds - user.uid
                        )
                    )
                }

                is AppResult.Error -> {
                    _uiState.value = UiState.Success(
                        latest.copy(actionUserIds = latest.actionUserIds - user.uid)
                    )
                }
            }
        }
    }

}
