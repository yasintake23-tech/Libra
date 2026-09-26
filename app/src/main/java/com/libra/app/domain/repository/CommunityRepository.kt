package com.libra.app.domain.repository

import com.libra.app.core.result.AppResult
import com.libra.app.domain.model.CommunityServer
import com.libra.app.domain.model.ServerMember
import com.libra.app.domain.model.ServerCategory
import com.libra.app.domain.model.ServerChannel
import com.libra.app.domain.model.ServerChannelPermissionOverride
import kotlinx.coroutines.flow.Flow

interface CommunityRepository {
    fun observeCommunityServers(): Flow<AppResult<List<CommunityServer>>>
    suspend fun createCommunityServer(name: String, description: String): AppResult<CommunityServer>
    suspend fun updateCommunityServer(serverId: String, name: String, description: String): AppResult<Unit>
    suspend fun updateCommunityServerMedia(serverId: String, avatarUrl: String, bannerUrl: String): AppResult<Unit>
    suspend fun uploadServerAvatar(
        serverId: String,
        fileName: String,
        bytes: ByteArray,
        contentType: String,
        onProgress: (Int) -> Unit = {}
    ): AppResult<String>
    suspend fun uploadServerBanner(
        serverId: String,
        fileName: String,
        bytes: ByteArray,
        contentType: String,
        onProgress: (Int) -> Unit = {}
    ): AppResult<String>
    suspend fun deleteServerMedia(serverId: String, fileKey: String): AppResult<Unit>
    suspend fun deleteCommunityServer(serverId: String): AppResult<Unit>
    suspend fun joinCommunityServer(serverId: String): AppResult<Unit>
    suspend fun isServerMember(serverId: String): AppResult<Boolean>
    suspend fun ensureServerStructure(serverId: String): AppResult<Unit>
    fun observeServerMembers(serverId: String): Flow<AppResult<List<ServerMember>>>
    fun observeServerCategories(serverId: String): Flow<AppResult<List<ServerCategory>>>
    fun observeServerChannels(serverId: String): Flow<AppResult<List<ServerChannel>>>
    suspend fun getChannelPermissions(serverId: String, channelId: String): AppResult<List<ServerChannelPermissionOverride>>
    fun observeChannelPermissions(serverId: String, channelId: String): Flow<AppResult<List<ServerChannelPermissionOverride>>>
    suspend fun createServerCategory(serverId: String, name: String): AppResult<ServerCategory>
    suspend fun updateServerCategory(serverId: String, categoryId: String, name: String): AppResult<Unit>
    suspend fun deleteServerCategory(serverId: String, categoryId: String): AppResult<Unit>
    suspend fun createServerChannel(serverId: String, categoryId: String, name: String): AppResult<ServerChannel>
    suspend fun updateServerChannel(serverId: String, channelId: String, name: String): AppResult<Unit>
    suspend fun moveServerChannelToCategory(serverId: String, channelId: String, categoryId: String): AppResult<Unit>
    suspend fun deleteServerChannel(serverId: String, channelId: String): AppResult<Unit>
    suspend fun setChannelPermission(serverId: String, channelId: String, override: ServerChannelPermissionOverride): AppResult<Unit>
    suspend fun deleteChannelPermission(serverId: String, channelId: String, overrideId: String): AppResult<Unit>
    fun observeServerRoles(serverId: String): Flow<AppResult<List<com.libra.app.domain.model.ServerRoleDefinition>>>
    suspend fun createServerRole(serverId: String, name: String, permissions: List<String>): AppResult<com.libra.app.domain.model.ServerRoleDefinition>
    suspend fun updateServerRole(serverId: String, roleId: String, name: String, permissions: List<String>): AppResult<Unit>
    suspend fun deleteServerRole(serverId: String, roleId: String): AppResult<Unit>
    suspend fun setServerMemberRole(serverId: String, memberId: String, role: String): AppResult<Unit>
    suspend fun banServerMember(serverId: String, memberId: String, reason: String): AppResult<Unit>
    suspend fun unbanServerMember(serverId: String, memberId: String): AppResult<Unit>
    suspend fun moveServerCategory(serverId: String, categoryId: String, direction: Int): AppResult<Unit>
    suspend fun moveServerChannel(serverId: String, channelId: String, direction: Int): AppResult<Unit>
    suspend fun removeServerMember(serverId: String, memberId: String): AppResult<Unit>
    suspend fun leaveCommunityServer(serverId: String): AppResult<Unit>
    suspend fun isServerBanned(serverId: String): AppResult<Boolean>
}
