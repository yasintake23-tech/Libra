package com.libra.app.domain.repository

import com.libra.app.core.result.AppResult
import com.libra.app.domain.model.AdminNotificationCampaignLog
import com.libra.app.domain.model.AdminNotificationDraft
import com.libra.app.domain.model.AdminRole
import com.libra.app.domain.model.CosmeticRole
import com.libra.app.domain.model.UserModeration
import com.libra.app.domain.model.UserProfile
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

interface AdminRepository {
    fun observeMembers(): Flow<AppResult<List<UserProfile>>>
    suspend fun updateMemberProfile(profile: UserProfile): AppResult<Unit>
    suspend fun updateModeration(uid: String, moderation: UserModeration): AppResult<Unit>
    suspend fun getAdminRole(uid: String): AppResult<AdminRole?>
    suspend fun setAdminRole(uid: String, role: AdminRole?): AppResult<Unit>
    fun observeCosmeticRoles(): Flow<AppResult<List<CosmeticRole>>>
    suspend fun saveCosmeticRole(role: CosmeticRole): AppResult<Unit>
    suspend fun deleteCosmeticRole(roleId: String): AppResult<Unit>
    suspend fun assignCosmeticRole(uid: String, roleId: String, assigned: Boolean): AppResult<Unit>

    fun observeAdminRoleUserIds(): Flow<AppResult<Set<String>>> = flowOf(AppResult.Success(emptySet()))
    fun observeAdminNotificationDrafts(): Flow<AppResult<List<AdminNotificationDraft>>> =
        flowOf(AppResult.Success(emptyList()))
    suspend fun saveAdminNotificationDraft(draft: AdminNotificationDraft): AppResult<AdminNotificationDraft> =
        AppResult.Success(draft)
    suspend fun deleteAdminNotificationDraft(draftId: String): AppResult<Unit> = AppResult.Success(Unit)
    fun observeAdminNotificationLogs(limit: Long = 50): Flow<AppResult<List<AdminNotificationCampaignLog>>> =
        flowOf(AppResult.Success(emptyList()))
    suspend fun logAdminNotificationCampaign(log: AdminNotificationCampaignLog): AppResult<Unit> =
        AppResult.Success(Unit)
}