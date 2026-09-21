package com.libra.app.domain.repository

import com.libra.app.core.result.AppResult
import com.libra.app.domain.model.UserProfile
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

interface AuthRepository {
    val currentUser: StateFlow<UserProfile?>
    val isAuthenticated: StateFlow<Boolean>

    suspend fun checkCurrentSession(): AppResult<UserProfile?>

    fun signInWithGoogleIdToken(
        idToken: String,
        displayName: String?,
        email: String?,
        photoUrl: String?
    ): Flow<AppResult<UserProfile>>

    suspend fun signInWithEmailPassword(email: String, password: String): AppResult<UserProfile>

    suspend fun createAccountWithEmailPassword(email: String, password: String): AppResult<UserProfile>

    suspend fun signOut(): AppResult<Unit>
}