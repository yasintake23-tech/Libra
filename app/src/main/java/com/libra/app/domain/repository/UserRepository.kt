package com.libra.app.domain.repository

import com.libra.app.core.result.AppResult
import com.libra.app.domain.model.UserProfile
import kotlinx.coroutines.flow.Flow

interface UserRepository {
    fun getUserProfile(uid: String): Flow<AppResult<UserProfile?>>
    suspend fun getUserProfileFresh(uid: String): AppResult<UserProfile?>
    suspend fun createOrUpdateProfile(profile: UserProfile): AppResult<UserProfile>
    suspend fun completeProfile(profile: UserProfile): AppResult<UserProfile>
    suspend fun isUsernameAvailable(username: String, currentUid: String? = null): AppResult<Boolean>
    suspend fun updateBio(uid: String, bio: String): AppResult<Unit>
    suspend fun updateProfilePhoto(uid: String, photoUrl: String): AppResult<Unit>
    suspend fun updateProfileDetails(
        profile: UserProfile,
        photoUrl: String? = null
    ): AppResult<UserProfile>
    fun searchUsers(query: String): Flow<AppResult<List<UserProfile>>>
    suspend fun getFollowingIds(uid: String): AppResult<Set<String>>
    suspend fun isFollowing(followerId: String, followingId: String): AppResult<Boolean>
    suspend fun followUser(followerId: String, followingId: String): AppResult<Unit>
    suspend fun unfollowUser(followerId: String, followingId: String): AppResult<Unit>
    suspend fun getFollowers(uid: String): AppResult<List<UserProfile>>
    suspend fun getFollowing(uid: String): AppResult<List<UserProfile>>
}