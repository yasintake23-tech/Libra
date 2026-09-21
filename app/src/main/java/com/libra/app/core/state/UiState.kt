package com.libra.app.core.state

import com.libra.app.core.result.AppError

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<out T>(val data: T) : UiState<T>
    data class Error(val error: AppError) : UiState<Nothing>
    data object Empty : UiState<Nothing>
}
