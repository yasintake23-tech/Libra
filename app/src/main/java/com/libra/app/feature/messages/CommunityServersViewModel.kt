package com.libra.app.feature.messages

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.libra.app.core.di.ServiceLocator
import com.libra.app.core.result.AppResult
import com.libra.app.domain.model.CommunityServer
import com.libra.app.domain.model.ServerCategory
import com.libra.app.domain.model.ServerChannel
import com.libra.app.domain.model.ServerChannelPermissionOverride
import com.libra.app.domain.model.ServerMember
import com.libra.app.domain.model.ServerMessage
import com.libra.app.domain.model.ServerRoleDefinition
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlin.math.max

data class PendingServerImageUpload(
    val fileName: String,
    val bytes: ByteArray,
    val contentType: String
)

class CommunityServersViewModel : ViewModel() {
    private val communityRepository = ServiceLocator.communityRepository
    private val chatRepository = ServiceLocator.chatRepository
    private val authRepository = ServiceLocator.authRepository

    private var workspacePrefs: DmWorkspacePreferences? = null

    private val _allServers = MutableStateFlow<List<CommunityServer>>(emptyList())
    val allServers = _allServers.asStateFlow()

    private val _joinedServers = MutableStateFlow<List<CommunityServer>>(emptyList())
    val joinedServers = _joinedServers.asStateFlow()

    private val _pinnedServerIds = MutableStateFlow<Set<String>>(emptySet())
    val pinnedServerIds = _pinnedServerIds.asStateFlow()

    private val _isLoadingServers = MutableStateFlow(true)
    val isLoadingServers = _isLoadingServers.asStateFlow()

    private val _serversError = MutableStateFlow<String?>(null)
    val serversError = _serversError.asStateFlow()

    private val _selectedServerId = MutableStateFlow<String?>(null)
    val selectedServerId = _selectedServerId.asStateFlow()

    private val _selectedChannelId = MutableStateFlow<String?>(null)
    val selectedChannelId = _selectedChannelId.asStateFlow()

    private val _isLoadingWorkspace = MutableStateFlow(false)
    val isLoadingWorkspace = _isLoadingWorkspace.asStateFlow()

    private val _categories = MutableStateFlow<List<ServerCategory>>(emptyList())
    val categories = _categories.asStateFlow()

    private val _channels = MutableStateFlow<List<ServerChannel>>(emptyList())
    val channels = _channels.asStateFlow()

    private val _members = MutableStateFlow<List<ServerMember>>(emptyList())
    val members = _members.asStateFlow()

    private val _roles = MutableStateFlow<List<ServerRoleDefinition>>(emptyList())
    val roles = _roles.asStateFlow()

    private val _channelPermissions =
        MutableStateFlow<Map<String, List<ServerChannelPermissionOverride>>>(emptyMap())
    val channelPermissions = _channelPermissions.asStateFlow()

    private val _activeInviteKey = MutableStateFlow("")
    val activeInviteKey = _activeInviteKey.asStateFlow()

    private val _latestMessageByChannel = MutableStateFlow<Map<String, ServerMessage?>>(emptyMap())
    val latestMessageByChannel = _latestMessageByChannel.asStateFlow()

    private val _readAtByChannel = MutableStateFlow<Map<String, Long>>(emptyMap())
    val readAtByChannel = _readAtByChannel.asStateFlow()

    private var serversJob: Job? = null
    private var membershipJob: Job? = null
    private var workspaceJob: Job? = null
    private var permissionsJob: Job? = null
    private var channelLatestJobs: MutableMap<String, Job> = linkedMapOf()
    private var observedLatestServerId: String? = null

    init {
        refreshServers()
    }

    fun bindPreferences(context: Context) {
        if (workspacePrefs == null) {
            val prefs = DmWorkspacePreferences(context)
            workspacePrefs = prefs
            _pinnedServerIds.value = prefs.readPinnedServerIds()
            // Merge local read timestamps for any already-selected server
            val uid = authRepository.currentUser.value?.uid.orEmpty()
            val sid = _selectedServerId.value.orEmpty()
            if (uid.isNotBlank() && sid.isNotBlank()) {
                mergeReadMap(prefs.readChannelReadMap(uid, sid))
            }
        }
    }

    fun refreshServers() {
        serversJob?.cancel()
        membershipJob?.cancel()
        _isLoadingServers.value = _joinedServers.value.isEmpty()
        _serversError.value = null

        serversJob = viewModelScope.launch {
            authRepository.currentUser.filterNotNull().first()
            communityRepository.observeCommunityServers().collect { result ->
                when (result) {
                    is AppResult.Success -> {
                        val unique = result.data.distinctBy { it.id }
                        _allServers.value = unique
                        _serversError.value = null
                        resolveJoinedServers(unique)
                    }
                    is AppResult.Error -> {
                        _isLoadingServers.value = false
                        _serversError.value = result.error.message
                    }
                }
            }
        }
    }

    private fun resolveJoinedServers(servers: List<CommunityServer>) {
        membershipJob?.cancel()
        if (servers.isEmpty()) {
            _joinedServers.value = emptyList()
            _isLoadingServers.value = false
            return
        }
        membershipJob = viewModelScope.launch {
            val currentUid = authRepository.currentUser.value?.uid.orEmpty()
            val joined = coroutineScope {
                servers.map { server ->
                    async {
                        if (currentUid.isNotBlank() && server.ownerId == currentUid) {
                            server
                        } else {
                            when (val res = communityRepository.isServerMember(server.id)) {
                                is AppResult.Success -> if (res.data) server else null
                                is AppResult.Error -> null
                            }
                        }
                    }
                }.awaitAll().filterNotNull()
            }
            _joinedServers.value = joined
            _isLoadingServers.value = false
        }
    }

    fun togglePinServer(serverId: String) {
        if (serverId.isBlank()) return
        val next = if (serverId in _pinnedServerIds.value) {
            _pinnedServerIds.value - serverId
        } else {
            _pinnedServerIds.value + serverId
        }
        _pinnedServerIds.value = next
        workspacePrefs?.savePinnedServerIds(next)
    }

    fun selectServer(serverId: String?) {
        if (serverId == _selectedServerId.value) {
            if (serverId == null) _selectedChannelId.value = null
            return
        }
        _selectedServerId.value = serverId
        _selectedChannelId.value = null
        clearWorkspaceListeners()

        if (serverId.isNullOrBlank()) {
            _categories.value = emptyList()
            _channels.value = emptyList()
            _members.value = emptyList()
            _roles.value = emptyList()
            _channelPermissions.value = emptyMap()
            _latestMessageByChannel.value = emptyMap()
            _readAtByChannel.value = emptyMap()
            _activeInviteKey.value = ""
            _isLoadingWorkspace.value = false
            return
        }

        _isLoadingWorkspace.value = true
        _categories.value = emptyList()
        _channels.value = emptyList()
        _members.value = emptyList()
        _roles.value = emptyList()
        _channelPermissions.value = emptyMap()
        _latestMessageByChannel.value = emptyMap()

        val uid = authRepository.currentUser.value?.uid.orEmpty()
        val localReads = workspacePrefs?.readChannelReadMap(uid, serverId).orEmpty()
        _readAtByChannel.value = localReads

        val knownServer = _allServers.value.firstOrNull { it.id == serverId }
            ?: _joinedServers.value.firstOrNull { it.id == serverId }
        _activeInviteKey.value = knownServer?.inviteKey.orEmpty()

        workspaceJob = viewModelScope.launch {
            launch {
                communityRepository.ensureServerStructure(serverId)
            }
            launch {
                val initialInvite = knownServer?.inviteKey.orEmpty()
                if (initialInvite.isBlank()) {
                    when (val res = communityRepository.ensureCommunityServerInviteKey(serverId)) {
                        is AppResult.Success -> _activeInviteKey.value = res.data
                        is AppResult.Error -> Unit
                    }
                }
            }
            launch {
                communityRepository.observeServerCategories(serverId).collect { res ->
                    if (res is AppResult.Success) {
                        _categories.value = res.data
                        if (res.data.isNotEmpty()) _isLoadingWorkspace.value = false
                    }
                }
            }
            launch {
                communityRepository.observeServerChannels(serverId).collect { res ->
                    if (res is AppResult.Success) {
                        _channels.value = res.data
                        _isLoadingWorkspace.value = false
                        if (_selectedChannelId.value != null && res.data.none { it.id == _selectedChannelId.value }) {
                            _selectedChannelId.value = null
                        }
                        refreshChannelPermissions(serverId, res.data)
                        syncChannelLatestMessageObservers(serverId, res.data)
                    }
                }
            }
            launch {
                communityRepository.observeServerMembers(serverId).collect { res ->
                    if (res is AppResult.Success) _members.value = res.data
                }
            }
            launch {
                communityRepository.observeServerRoles(serverId).collect { res ->
                    if (res is AppResult.Success) _roles.value = res.data
                }
            }
            if (uid.isNotBlank()) {
                launch {
                    chatRepository.observeServerChannelReadTimestamps(uid, serverId).collect { map ->
                        mergeReadMap(map)
                    }
                }
            }
        }
    }

    fun selectChannel(channelId: String?) {
        _selectedChannelId.value = channelId
        val serverId = _selectedServerId.value
        if (!serverId.isNullOrBlank() && !channelId.isNullOrBlank()) {
            val latestTs = _latestMessageByChannel.value[channelId]?.createdAt ?: 0L
            markChannelRead(serverId, channelId, max(System.currentTimeMillis(), latestTs))
        }
    }

    fun markChannelRead(serverId: String, channelId: String, timestampMs: Long = System.currentTimeMillis()) {
        if (serverId.isBlank() || channelId.isBlank()) return
        val effectiveTs = timestampMs.coerceAtLeast(System.currentTimeMillis())
        val uid = authRepository.currentUser.value?.uid.orEmpty()
        workspacePrefs?.saveChannelReadAt(uid, serverId, channelId, effectiveTs)
        if (_selectedServerId.value == serverId) {
            val prev = _readAtByChannel.value[channelId] ?: 0L
            if (effectiveTs > prev) {
                _readAtByChannel.value = _readAtByChannel.value + (channelId to effectiveTs)
            }
        }
        viewModelScope.launch {
            chatRepository.markServerChannelRead(serverId, channelId, effectiveTs)
        }
    }

    private fun mergeReadMap(incoming: Map<String, Long>) {
        if (incoming.isEmpty()) return
        val merged = _readAtByChannel.value.toMutableMap()
        incoming.forEach { (channelId, ts) ->
            val current = merged[channelId] ?: 0L
            if (ts > current) merged[channelId] = ts
        }
        _readAtByChannel.value = merged
    }

    private fun refreshChannelPermissions(serverId: String, channels: List<ServerChannel>) {
        permissionsJob?.cancel()
        val uid = authRepository.currentUser.value?.uid.orEmpty()
        val ownerId = _allServers.value.firstOrNull { it.id == serverId }?.ownerId
            ?: _joinedServers.value.firstOrNull { it.id == serverId }?.ownerId
        if (uid.isNotBlank() && uid == ownerId) {
            _channelPermissions.value = emptyMap()
            return
        }
        permissionsJob = viewModelScope.launch {
            val entries = coroutineScope {
                channels.map { channel ->
                    async {
                        val res = communityRepository.getChannelPermissions(serverId, channel.id)
                        channel.id to ((res as? AppResult.Success)?.data.orEmpty())
                    }
                }.awaitAll()
            }
            if (_selectedServerId.value == serverId) {
                _channelPermissions.value = entries.toMap()
            }
        }
    }

    private fun syncChannelLatestMessageObservers(serverId: String, channels: List<ServerChannel>) {
        if (observedLatestServerId != serverId) {
            channelLatestJobs.values.forEach { it.cancel() }
            channelLatestJobs.clear()
            observedLatestServerId = serverId
        }
        val activeIds = channels.map { it.id }.toSet()
        val staleIds = channelLatestJobs.keys.filterNot { it in activeIds }
        staleIds.forEach { id ->
            channelLatestJobs.remove(id)?.cancel()
        }
        if (staleIds.isNotEmpty()) {
            _latestMessageByChannel.value = _latestMessageByChannel.value.filterKeys { it in activeIds }
        }
        channels.forEach { channel ->
            if (channel.id !in channelLatestJobs) {
                channelLatestJobs[channel.id] = viewModelScope.launch {
                    chatRepository.observeServerChannelLatestMessage(serverId, channel.id).collect { latest ->
                        if (_selectedServerId.value == serverId) {
                            _latestMessageByChannel.value = _latestMessageByChannel.value + (channel.id to latest)
                            if (_selectedChannelId.value == channel.id && latest != null) {
                                markChannelRead(serverId, channel.id, max(System.currentTimeMillis(), latest.createdAt))
                            }
                        }
                    }
                }
            }
        }
    }

    private fun clearWorkspaceListeners() {
        workspaceJob?.cancel()
        workspaceJob = null
        permissionsJob?.cancel()
        permissionsJob = null
        channelLatestJobs.values.forEach { it.cancel() }
        channelLatestJobs.clear()
        observedLatestServerId = null
    }

    fun createServer(
        name: String,
        description: String,
        avatarUpload: PendingServerImageUpload? = null,
        bannerUpload: PendingServerImageUpload? = null,
        onDone: (CommunityServer?, String?) -> Unit
    ) {
        viewModelScope.launch {
            when (val created = communityRepository.createCommunityServer(name, description)) {
                is AppResult.Error -> onDone(null, created.error.message)
                is AppResult.Success -> {
                    var server = created.data
                    var avatarKey = server.avatarUrl
                    var bannerKey = server.bannerUrl
                    var mediaChanged = false

                    if (avatarUpload != null) {
                        val up = communityRepository.uploadServerAvatar(
                            serverId = server.id,
                            fileName = avatarUpload.fileName,
                            bytes = avatarUpload.bytes,
                            contentType = avatarUpload.contentType
                        )
                        if (up is AppResult.Success) {
                            avatarKey = up.data
                            mediaChanged = true
                        }
                    }
                    if (bannerUpload != null) {
                        val up = communityRepository.uploadServerBanner(
                            serverId = server.id,
                            fileName = bannerUpload.fileName,
                            bytes = bannerUpload.bytes,
                            contentType = bannerUpload.contentType
                        )
                        if (up is AppResult.Success) {
                            bannerKey = up.data
                            mediaChanged = true
                        }
                    }
                    if (mediaChanged) {
                        val saved = communityRepository.updateCommunityServerMedia(server.id, avatarKey, bannerKey)
                        if (saved is AppResult.Success) {
                            server = server.copy(avatarUrl = avatarKey, bannerUrl = bannerKey)
                        }
                    }
                    if (_joinedServers.value.none { it.id == server.id }) {
                        _joinedServers.value = listOf(server) + _joinedServers.value
                    }
                    if (_allServers.value.none { it.id == server.id }) {
                        _allServers.value = listOf(server) + _allServers.value
                    }
                    selectServer(server.id)
                    onDone(server, null)
                }
            }
        }
    }

    fun lookupByInviteKey(
        rawKey: String,
        onDone: (CommunityServer?, Boolean, String?) -> Unit
    ) {
        viewModelScope.launch {
            when (val res = communityRepository.findCommunityServerByInviteKey(rawKey)) {
                is AppResult.Error -> onDone(null, false, res.error.message)
                is AppResult.Success -> {
                    val isMember = when (val memberRes = communityRepository.isServerMember(res.data.id)) {
                        is AppResult.Success -> memberRes.data
                        is AppResult.Error -> false
                    }
                    onDone(res.data, isMember, null)
                }
            }
        }
    }

    fun joinOrEnterServer(
        server: CommunityServer,
        isAlreadyMember: Boolean,
        onDone: (Boolean, String?) -> Unit
    ) {
        viewModelScope.launch {
            val result = if (isAlreadyMember) {
                communityRepository.ensureServerStructure(server.id)
            } else {
                when (val joined = communityRepository.joinCommunityServer(server.id)) {
                    is AppResult.Success -> communityRepository.ensureServerStructure(server.id)
                    is AppResult.Error -> joined
                }
            }
            when (result) {
                is AppResult.Success -> {
                    if (_joinedServers.value.none { it.id == server.id }) {
                        _joinedServers.value = listOf(server) + _joinedServers.value
                    }
                    if (_allServers.value.none { it.id == server.id }) {
                        _allServers.value = listOf(server) + _allServers.value
                    }
                    selectServer(server.id)
                    onDone(true, null)
                }
                is AppResult.Error -> onDone(false, result.error.message)
            }
        }
    }

    fun leaveServer(
        serverId: String,
        onDone: (Boolean, String?) -> Unit
    ) {
        viewModelScope.launch {
            when (val res = communityRepository.leaveCommunityServer(serverId)) {
                is AppResult.Success -> {
                    _joinedServers.value = _joinedServers.value.filterNot { it.id == serverId }
                    if (_selectedServerId.value == serverId) {
                        selectServer(null)
                    }
                    onDone(true, null)
                }
                is AppResult.Error -> onDone(false, res.error.message)
            }
        }
    }

    fun onServerDeleted(serverId: String) {
        _joinedServers.value = _joinedServers.value.filterNot { it.id == serverId }
        _allServers.value = _allServers.value.filterNot { it.id == serverId }
        if (_selectedServerId.value == serverId) {
            selectServer(null)
        }
    }

    override fun onCleared() {
        clearWorkspaceListeners()
        serversJob?.cancel()
        membershipJob?.cancel()
        super.onCleared()
    }
}
