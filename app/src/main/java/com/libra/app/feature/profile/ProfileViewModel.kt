package com.libra.app.feature.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.libra.app.core.di.ServiceLocator
import com.libra.app.core.result.AppResult
import com.libra.app.core.state.UiState
import com.libra.app.domain.model.UserProfile
import com.libra.app.domain.repository.AuthRepository
import com.libra.app.domain.repository.UserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class ProfileViewModel(
    private val authRepository: AuthRepository = ServiceLocator.authRepository,
    private val userRepository: UserRepository = ServiceLocator.userRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<UiState<UserProfile>>(UiState.Loading)
    val uiState: StateFlow<UiState<UserProfile>> = _uiState.asStateFlow()

    init { loadProfile() }

    fun loadProfile() {
        viewModelScope.launch {
            val current = authRepository.currentUser.value
            if (current == null) {
                _uiState.value = UiState.Empty
                return@launch
            }

            _uiState.value = UiState.Loading
            userRepository.getUserProfile(current.uid).collect { result ->
                _uiState.value = when (result) {
                    is AppResult.Success -> result.data?.let { UiState.Success(it) } ?: UiState.Success(current)
                    is AppResult.Error -> UiState.Error(result.error)
                }
            }
        }
    }

    fun signOut(onSignedOut: () -> Unit) {
        viewModelScope.launch {
            when (authRepository.signOut()) {
                is AppResult.Success -> onSignedOut()
                is AppResult.Error -> Unit
            }
        }
    }
}
