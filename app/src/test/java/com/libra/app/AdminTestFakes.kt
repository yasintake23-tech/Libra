package com.libra.app

import com.libra.app.core.result.AppError
import com.libra.app.core.result.AppResult
import com.libra.app.domain.model.CommunityServer
import com.libra.app.domain.model.DirectConversation
import com.libra.app.domain.model.DirectMessage
import com.libra.app.domain.model.GlobalChatMessage
import com.libra.app.domain.model.ReactionUser
import com.libra.app.domain.model.ServerBan
import com.libra.app.domain.model.ServerCategory
import com.libra.app.domain.model.ServerChannel
import com.libra.app.domain.model.ServerChannelPermissionOverride
import com.libra.app.domain.model.ServerMember
import com.libra.app.domain.model.ServerMessage
import com.libra.app.domain.model.ServerRoleDefinition
import com.libra.app.domain.model.SharedContent
import com.libra.app.domain.model.Story
import com.libra.app.domain.repository.ChatRepository
import com.libra.app.domain.repository.CommunityRepository
import com.libra.app.domain.repository.StoryRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf

internal class FakeAdminStoryRepo(initialStories: List<Story> = emptyList()) : StoryRepository {
    private val storiesFlow = MutableStateFlow<AppResult<List<Story>>>(AppResult.Success(initialStories))
    var lastDeletedStoryActor: String? = null

    private fun currentStories(): List<Story> = (storiesFlow.value as AppResult.Success).data

    override suspend fun createStory(
        authorId: String,
        authorName: String,
        authorPhotoUrl: String,
        text: String,
        mediaUrl: String,
        mediaType: String
    ): AppResult<Unit> = AppResult.Success(Unit)

    override suspend fun getActiveStories(authorIds: Set<String>): AppResult<List<Story>> = storiesFlow.value
    override suspend fun isLiked(storyId: String, userId: String): AppResult<Boolean> = AppResult.Success(false)
    override suspend fun toggleLike(storyId: String, userId: String): AppResult<Boolean> = AppResult.Success(true)
    override fun observeAllStoriesForAdmin(limit: Long): Flow<AppResult<List<Story>>> = storiesFlow
    override suspend fun deleteStory(storyId: String, userId: String): AppResult<Unit> {
        lastDeletedStoryActor = userId
        storiesFlow.value = AppResult.Success(currentStories().filterNot { it.id == storyId })
        return AppResult.Success(Unit)
    }
}

internal class FakeAdminCommunityRepo(
    initialServers: List<CommunityServer> = emptyList(),
    initialMembers: Map<String, List<ServerMember>> = emptyMap(),
    initialCategories: Map<String, List<ServerCategory>> = emptyMap(),
    initialChannels: Map<String, List<ServerChannel>> = emptyMap(),
    initialBans: Map<String, List<ServerBan>> = emptyMap()
) : CommunityRepository {
    private val serversFlow = MutableStateFlow<AppResult<List<CommunityServer>>>(AppResult.Success(initialServers))
    private val membersMap = initialMembers.mapValues { MutableStateFlow<AppResult<List<ServerMember>>>(AppResult.Success(it.value)) }.toMutableMap()
    private val categoriesMap = initialCategories.mapValues { MutableStateFlow<AppResult<List<ServerCategory>>>(AppResult.Success(it.value)) }.toMutableMap()
    private val channelsMap = initialChannels.mapValues { MutableStateFlow<AppResult<List<ServerChannel>>>(AppResult.Success(it.value)) }.toMutableMap()
    private val bansMap = initialBans.mapValues { MutableStateFlow<AppResult<List<ServerBan>>>(AppResult.Success(it.value)) }.toMutableMap()

    val deletedMediaKeys = mutableListOf<String>()

    private fun currentServers(): List<CommunityServer> = (serversFlow.value as AppResult.Success).data

    override fun observeCommunityServers(): Flow<AppResult<List<CommunityServer>>> = serversFlow
    override suspend fun findCommunityServerByInviteKey(inviteKey: String): AppResult<CommunityServer> =
        AppResult.Error(AppError.NotFound("Bulunamadı"))
    override suspend fun ensureCommunityServerInviteKey(serverId: String): AppResult<String> = AppResult.Success("key123456")
    override suspend fun createCommunityServer(name: String, description: String): AppResult<CommunityServer> =
        AppResult.Error(AppError.Database("N/A"))
    override suspend fun updateCommunityServer(serverId: String, name: String, description: String): AppResult<Unit> {
        serversFlow.value = AppResult.Success(
            currentServers().map { if (it.id == serverId) it.copy(name = name, description = description) else it }
        )
        return AppResult.Success(Unit)
    }
    override suspend fun updateCommunityServerMedia(serverId: String, avatarUrl: String, bannerUrl: String): AppResult<Unit> {
        serversFlow.value = AppResult.Success(
            currentServers().map { if (it.id == serverId) it.copy(avatarUrl = avatarUrl, bannerUrl = bannerUrl) else it }
        )
        return AppResult.Success(Unit)
    }
    override suspend fun uploadServerAvatar(serverId: String, fileName: String, bytes: ByteArray, contentType: String, onProgress: (Int) -> Unit): AppResult<String> =
        AppResult.Success("")
    override suspend fun uploadServerBanner(serverId: String, fileName: String, bytes: ByteArray, contentType: String, onProgress: (Int) -> Unit): AppResult<String> =
        AppResult.Success("")
    override suspend fun deleteServerMedia(serverId: String, fileKey: String): AppResult<Unit> {
        deletedMediaKeys.add(fileKey)
        return AppResult.Success(Unit)
    }
    override suspend fun deleteCommunityServer(serverId: String): AppResult<Unit> {
        serversFlow.value = AppResult.Success(currentServers().filterNot { it.id == serverId })
        return AppResult.Success(Unit)
    }
    override suspend fun joinCommunityServer(serverId: String): AppResult<Unit> = AppResult.Success(Unit)
    override suspend fun isServerMember(serverId: String): AppResult<Boolean> = AppResult.Success(true)
    override suspend fun ensureServerStructure(serverId: String): AppResult<Unit> = AppResult.Success(Unit)
    override fun observeServerMembers(serverId: String): Flow<AppResult<List<ServerMember>>> =
        membersMap.getOrPut(serverId) { MutableStateFlow(AppResult.Success(emptyList())) }
    override fun observeServerCategories(serverId: String): Flow<AppResult<List<ServerCategory>>> =
        categoriesMap.getOrPut(serverId) { MutableStateFlow(AppResult.Success(emptyList())) }
    override fun observeServerChannels(serverId: String): Flow<AppResult<List<ServerChannel>>> =
        channelsMap.getOrPut(serverId) { MutableStateFlow(AppResult.Success(emptyList())) }
    override suspend fun getChannelPermissions(serverId: String, channelId: String): AppResult<List<ServerChannelPermissionOverride>> =
        AppResult.Success(emptyList())
    override fun observeChannelPermissions(serverId: String, channelId: String): Flow<AppResult<List<ServerChannelPermissionOverride>>> =
        flowOf(AppResult.Success(emptyList()))
    override suspend fun createServerCategory(serverId: String, name: String): AppResult<ServerCategory> {
        val flow = categoriesMap.getOrPut(serverId) { MutableStateFlow(AppResult.Success(emptyList())) }
        val existing = (flow.value as AppResult.Success).data
        val created = ServerCategory(id = "cat_${existing.size + 1}", name = name.trim(), position = existing.size)
        flow.value = AppResult.Success(existing + created)
        return AppResult.Success(created)
    }
    override suspend fun updateServerCategory(serverId: String, categoryId: String, name: String): AppResult<Unit> = AppResult.Success(Unit)
    override suspend fun deleteServerCategory(serverId: String, categoryId: String): AppResult<Unit> {
        val catFlow = categoriesMap[serverId]
        if (catFlow != null) {
            val current = (catFlow.value as AppResult.Success).data
            catFlow.value = AppResult.Success(current.filterNot { it.id == categoryId })
        }
        return AppResult.Success(Unit)
    }
    override suspend fun createServerChannel(serverId: String, categoryId: String, name: String): AppResult<ServerChannel> {
        val flow = channelsMap.getOrPut(serverId) { MutableStateFlow(AppResult.Success(emptyList())) }
        val existing = (flow.value as AppResult.Success).data
        val created = ServerChannel(id = "ch_${existing.size + 1}", categoryId = categoryId, name = name.trim(), position = existing.size)
        flow.value = AppResult.Success(existing + created)
        return AppResult.Success(created)
    }
    override suspend fun updateServerChannel(serverId: String, channelId: String, name: String): AppResult<Unit> = AppResult.Success(Unit)
    override suspend fun moveServerChannelToCategory(serverId: String, channelId: String, categoryId: String): AppResult<Unit> = AppResult.Success(Unit)
    override suspend fun deleteServerChannel(serverId: String, channelId: String): AppResult<Unit> {
        val flow = channelsMap[serverId]
        if (flow != null) {
            val existing = (flow.value as AppResult.Success).data
            flow.value = AppResult.Success(existing.filterNot { it.id == channelId })
        }
        return AppResult.Success(Unit)
    }
    override suspend fun setChannelPermission(serverId: String, channelId: String, override: ServerChannelPermissionOverride): AppResult<Unit> =
        AppResult.Success(Unit)
    override suspend fun deleteChannelPermission(serverId: String, channelId: String, overrideId: String): AppResult<Unit> =
        AppResult.Success(Unit)
    override fun observeServerRoles(serverId: String): Flow<AppResult<List<ServerRoleDefinition>>> =
        flowOf(AppResult.Success(emptyList()))
    override suspend fun createServerRole(serverId: String, name: String, permissions: List<String>): AppResult<ServerRoleDefinition> =
        AppResult.Success(ServerRoleDefinition("r1", name, permissions))
    override suspend fun updateServerRole(serverId: String, roleId: String, name: String, permissions: List<String>): AppResult<Unit> =
        AppResult.Success(Unit)
    override suspend fun deleteServerRole(serverId: String, roleId: String): AppResult<Unit> = AppResult.Success(Unit)
    override suspend fun moveServerRole(serverId: String, roleId: String, direction: Int): AppResult<Unit> = AppResult.Success(Unit)
    override suspend fun setServerMemberRole(serverId: String, memberId: String, role: String): AppResult<Unit> {
        val flow = membersMap[serverId]
        if (flow != null) {
            val current = (flow.value as AppResult.Success).data
            flow.value = AppResult.Success(current.map { if (it.uid == memberId) it.copy(role = role) else it })
        }
        return AppResult.Success(Unit)
    }
    override suspend fun banServerMember(serverId: String, memberId: String, reason: String): AppResult<Unit> {
        val memberFlow = membersMap[serverId]
        val banFlow = bansMap.getOrPut(serverId) { MutableStateFlow(AppResult.Success(emptyList())) }
        val target = memberFlow?.let { (it.value as AppResult.Success).data.firstOrNull { m -> m.uid == memberId } }
        if (memberFlow != null) {
            memberFlow.value = AppResult.Success((memberFlow.value as AppResult.Success).data.filterNot { it.uid == memberId })
        }
        val existingBans = (banFlow.value as AppResult.Success).data
        banFlow.value = AppResult.Success(existingBans + ServerBan(uid = memberId, displayName = target?.displayName.orEmpty(), reason = reason))
        return AppResult.Success(Unit)
    }
    override fun observeServerBans(serverId: String): Flow<AppResult<List<ServerBan>>> =
        bansMap.getOrPut(serverId) { MutableStateFlow(AppResult.Success(emptyList())) }
    override suspend fun unbanServerMember(serverId: String, memberId: String): AppResult<Unit> {
        val banFlow = bansMap[serverId]
        if (banFlow != null) {
            banFlow.value = AppResult.Success((banFlow.value as AppResult.Success).data.filterNot { it.uid == memberId })
        }
        return AppResult.Success(Unit)
    }
    override suspend fun moveServerCategory(serverId: String, categoryId: String, direction: Int): AppResult<Unit> = AppResult.Success(Unit)
    override suspend fun moveServerChannel(serverId: String, channelId: String, direction: Int): AppResult<Unit> = AppResult.Success(Unit)
    override suspend fun removeServerMember(serverId: String, memberId: String): AppResult<Unit> {
        val memberFlow = membersMap[serverId]
        if (memberFlow != null) {
            memberFlow.value = AppResult.Success((memberFlow.value as AppResult.Success).data.filterNot { it.uid == memberId })
        }
        return AppResult.Success(Unit)
    }
    override suspend fun leaveCommunityServer(serverId: String): AppResult<Unit> = AppResult.Success(Unit)
    override suspend fun isServerBanned(serverId: String): AppResult<Boolean> = AppResult.Success(false)
}

internal class FakeAdminChatRepo(
    initialGlobalMessages: List<GlobalChatMessage> = emptyList(),
    initialServerMessages: List<ServerMessage> = emptyList()
) : ChatRepository {
    private val globalFlow = MutableStateFlow<AppResult<List<GlobalChatMessage>>>(AppResult.Success(initialGlobalMessages))
    private val serverFlow = MutableStateFlow<AppResult<List<ServerMessage>>>(AppResult.Success(initialServerMessages))
    val announcementsSent = mutableListOf<String>()

    private fun currentGlobal(): List<GlobalChatMessage> = (globalFlow.value as AppResult.Success).data
    private fun currentServer(): List<ServerMessage> = (serverFlow.value as AppResult.Success).data

    override fun observeGlobalMessages(limit: Long): Flow<AppResult<List<GlobalChatMessage>>> = globalFlow
    override suspend fun sendGlobalMessage(text: String, replyTo: GlobalChatMessage?, sharedContent: SharedContent?): AppResult<Unit> {
        announcementsSent.add(text)
        val added = GlobalChatMessage(id = "global_new_${announcementsSent.size}", senderId = "admin_uid", senderName = "Yönetim", text = text, createdAt = System.currentTimeMillis())
        globalFlow.value = AppResult.Success(currentGlobal() + added)
        return AppResult.Success(Unit)
    }
    override suspend fun sendGlobalMediaMessage(mediaUrl: String, mediaType: String, text: String, replyTo: GlobalChatMessage?, sharedContent: SharedContent?): AppResult<Unit> =
        AppResult.Success(Unit)
    override suspend fun editGlobalMessage(messageId: String, text: String): AppResult<Unit> = AppResult.Success(Unit)
    override suspend fun deleteGlobalMessage(messageId: String): AppResult<Unit> {
        globalFlow.value = AppResult.Success(currentGlobal().filterNot { it.id == messageId })
        return AppResult.Success(Unit)
    }
    override suspend fun toggleGlobalMessageReaction(messageId: String, emoji: String): AppResult<Unit> = AppResult.Success(Unit)

    override fun observeDirectConversations(uid: String): Flow<AppResult<List<DirectConversation>>> = flowOf(AppResult.Success(emptyList()))
    override fun observeDirectMessages(conversationId: String, limit: Long): Flow<AppResult<List<DirectMessage>>> = flowOf(AppResult.Success(emptyList()))
    override suspend fun sendDirectMessage(recipientId: String, text: String, replyTo: DirectMessage?, sharedContent: SharedContent?): AppResult<Unit> = AppResult.Success(Unit)
    override suspend fun sendDirectMediaMessage(recipientId: String, mediaUrl: String, mediaType: String, text: String, replyTo: DirectMessage?, sharedContent: SharedContent?): AppResult<Unit> = AppResult.Success(Unit)
    override suspend fun editDirectMessage(conversationId: String, messageId: String, text: String): AppResult<Unit> = AppResult.Success(Unit)
    override suspend fun deleteDirectMessage(conversationId: String, messageId: String): AppResult<Unit> = AppResult.Success(Unit)
    override suspend fun toggleDirectMessageReaction(conversationId: String, messageId: String, emoji: String): AppResult<Unit> = AppResult.Success(Unit)
    override suspend fun markDirectConversationRead(conversationId: String): AppResult<Unit> = AppResult.Success(Unit)
    override suspend fun createGroupConversation(name: String, participantIds: List<String>): AppResult<DirectConversation> = AppResult.Error(AppError.Database("N/A"))
    override suspend fun sendGroupMessage(conversationId: String, text: String, replyTo: DirectMessage?, sharedContent: SharedContent?): AppResult<Unit> = AppResult.Success(Unit)
    override suspend fun sendGroupMediaMessage(conversationId: String, mediaUrl: String, mediaType: String, text: String, replyTo: DirectMessage?, sharedContent: SharedContent?): AppResult<Unit> = AppResult.Success(Unit)

    override fun observeServerMessages(serverId: String, limit: Long, channelId: String): Flow<AppResult<List<ServerMessage>>> = serverFlow
    override suspend fun sendServerMessage(serverId: String, text: String, replyTo: ServerMessage?, sharedContent: SharedContent?, channelId: String): AppResult<Unit> = AppResult.Success(Unit)
    override suspend fun sendServerMediaMessage(serverId: String, mediaUrl: String, mediaType: String, text: String, replyTo: ServerMessage?, sharedContent: SharedContent?, channelId: String): AppResult<Unit> = AppResult.Success(Unit)
    override suspend fun editServerMessage(serverId: String, messageId: String, text: String, channelId: String): AppResult<Unit> = AppResult.Success(Unit)
    override suspend fun deleteServerMessage(serverId: String, messageId: String, channelId: String): AppResult<Unit> {
        serverFlow.value = AppResult.Success(currentServer().filterNot { it.id == messageId })
        return AppResult.Success(Unit)
    }
    override suspend fun toggleServerMessageReaction(serverId: String, messageId: String, emoji: String, channelId: String): AppResult<Unit> = AppResult.Success(Unit)
    override suspend fun getServerMessageReactionUsers(serverId: String, messageId: String, emoji: String, channelId: String): AppResult<List<ReactionUser>> = AppResult.Success(emptyList())
}
