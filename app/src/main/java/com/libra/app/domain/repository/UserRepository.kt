package com.libra.app.domain.repository

import com.libra.app.core.result.AppResult
import com.libra.app.domain.model.UserProfile
import kotlinx.coroutines.flow.Flow

interface UserRepository {
    fun getUserProfile(uid: String): Flow<AppResult<UserProfile?>>
    suspend fun createOrUpdateProfile(profile: UserProfile): AppResult<UserProfile>
    suspend fun completeProfile(profile: UserProfile): AppResult<UserProfile>
    suspend fun isUsernameAvailable(username: String, currentUid: String? = null): AppResult<Boolean>
    suspend fun updateBio(uid: String, bio: String): AppResult<Unit>
    suspend fun updateProfilePhoto(uid: String, photoUrl: String): AppResult<Unit>
    fun searchUsers(query: String): Flow<AppResult<List<UserProfile>>>
}