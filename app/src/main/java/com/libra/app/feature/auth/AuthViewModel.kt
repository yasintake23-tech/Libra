package com.libra.app.feature.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.libra.app.core.di.ServiceLocator
import com.libra.app.core.result.AppResult
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
                is AppResult.Success -> {
                    _authState.value = result.data?.let { UiState.Success(it) } ?: UiState.Empty
                }
                is AppResult.Error -> {
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

            authRepository
                .signInWithGoogleIdToken(idToken, displayName, email, photoUrl)
                .collect { result ->
                    _authState.value = when (result) {
                        is AppResult.Success -> UiState.Success(result.data)
                        is AppResult.Error -> UiState.Error(result.error)
                    }
                }
        }
    }

    fun signInWithEmail(email: String, password: String) {
        viewModelScope.launch {
            _authState.value = UiState.Loading

            _authState.value = when (
                val result = authRepository.signInWithEmailPassword(email, password)
            ) {
                is AppResult.Success -> UiState.Success(result.data)
                is AppResult.Error -> UiState.Error(result.error)
            }
        }
    }

    fun createAccount(email: String, password: String) {
        viewModelScope.launch {
            _authState.value = UiState.Loading

            _authState.value = when (
                val result = authRepository.createAccountWithEmailPassword(email, password)
            ) {
                is AppResult.Success -> UiState.Success(result.data)
                is AppResult.Error -> UiState.Error(result.error)
            }
        }
    }

    fun signOut() {
        viewModelScope.launch {
            _authState.value = when (val result = authRepository.signOut()) {
                is AppResult.Success -> UiState.Empty
                is AppResult.Error -> UiState.Error(result.error)
            }
        }
    }
}