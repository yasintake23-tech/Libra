package com.libra.app.domain.repository

import com.libra.app.core.result.AppResult
import com.libra.app.domain.model.CommunityServer
import com.libra.app.domain.model.ServerMember
import kotlinx.coroutines.flow.Flow

interface CommunityRepository {
    fun observeCommunityServers(): Flow<AppResult<List<CommunityServer>>>
    suspend fun createCommunityServer(name: String, description: String): AppResult<CommunityServer>
    suspend fun updateCommunityServer(serverId: String, name: String, description: String): AppResult<Unit>
    suspend fun joinCommunityServer(serverId: String): AppResult<Unit>
    fun observeServerMembers(serverId: String): Flow<AppResult<List<ServerMember>>>
    suspend fun setServerMemberRole(serverId: String, memberId: String, role: String): AppResult<Unit>
    suspend fun removeServerMember(serverId: String, memberId: String): AppResult<Unit>
    suspend fun leaveCommunityServer(serverId: String): AppResult<Unit>
}
