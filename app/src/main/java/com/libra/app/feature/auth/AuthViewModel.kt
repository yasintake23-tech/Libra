package com.libra.app.feature.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.libra.app.core.di.ServiceLocator
import com.libra.app.core.state.UiState
import com.libra.app.domain.model.UserProfile
import com.libra.app.domain.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AuthViewModel(
    private val authRepository: AuthRepository = ServiceLocator.authRepository
) : ViewModel() {

    private val _authState = MutableStateFlow<UiState<UserProfile?>>(UiState.Loading)
    val authState: StateFlow<UiState<UserProfile?>> = _authState.asStateFlow()

    val currentUser: StateFlow<UserProfile?> = authRepository.currentUser
    val isAuthenticated: StateFlow<Boolean> = authRepository.isAuthenticated

    init {
        checkSession()
    }

    fun checkSession() {
        viewModelScope.launch {
            _authState.value = UiState.Loading
            when (val result = authRepository.checkCurrentSession()) {
                is com.libra.app.core.result.AppResult.Success -> {
                    _authState.value = result.data?.let { UiState.Success(it) } ?: UiState.Empty
                }
                is com.libra.app.core.result.AppResult.Error -> {
                    _authState.value = UiState.Error(result.error)
                }
            }
        }
    }

    fun signInWithGoogle(
        idToken: String,
        displayName: String?,
        email: String?,
        photoUrl: String?
    ) {
        viewModelScope.launch {
            _authState.value = UiState.Loading
            authRepository.signInWithGoogleIdToken(idToken, displayName, email, photoUrl).collect { result ->
                _authState.value = when (result) {
                    is com.libra.app.core.result.AppResult.Success -> UiState.Success(result.data)
                    is com.libra.app.core.result.AppResult.Error -> UiState.Error(result.error)
                }
            }
        }
    }

    fun signOut() {
        viewModelScope.launch {
            val result = authRepository.signOut()
            _authState.value = when (result) {
                is com.libra.app.core.result.AppResult.Success -> UiState.Empty
                is com.libra.app.core.result.AppResult.Error -> UiState.Error(result.error)
            }
        }
    }
}
