package com.libra.app.domain.repository

import com.libra.app.core.result.AppResult
import com.libra.app.domain.model.CommunityServer
import com.libra.app.domain.model.ServerMember
import com.libra.app.domain.model.ServerCategory
import com.libra.app.domain.model.ServerChannel
import kotlinx.coroutines.flow.Flow

interface CommunityRepository {
    fun observeCommunityServers(): Flow<AppResult<List<CommunityServer>>>
    suspend fun createCommunityServer(name: String, description: String): AppResult<CommunityServer>
    suspend fun updateCommunityServer(serverId: String, name: String, description: String): AppResult<Unit>
    suspend fun joinCommunityServer(serverId: String): AppResult<Unit>
    suspend fun isServerMember(serverId: String): AppResult<Boolean>
    suspend fun ensureServerStructure(serverId: String): AppResult<Unit>
    fun observeServerMembers(serverId: String): Flow<AppResult<List<ServerMember>>>
    fun observeServerCategories(serverId: String): Flow<AppResult<List<ServerCategory>>>
    fun observeServerChannels(serverId: String): Flow<AppResult<List<ServerChannel>>>
    suspend fun setServerMemberRole(serverId: String, memberId: String, role: String): AppResult<Unit>
    suspend fun removeServerMember(serverId: String, memberId: String): AppResult<Unit>
    suspend fun leaveCommunityServer(serverId: String): AppResult<Unit>
}
