package com.libra.app.feature.messages

import androidx.activity.compose.BackHandler

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.IntOffset
import coil.compose.AsyncImage
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.libra.app.core.di.ServiceLocator
import com.libra.app.core.result.AppResult
import com.libra.app.domain.model.CommunityServer
import com.libra.app.domain.model.ServerMessage
import com.libra.app.domain.model.ServerMember
import com.libra.app.domain.model.ServerCategory
import com.libra.app.domain.model.ServerChannel
import com.libra.app.domain.model.ServerChannelPermissionOverride
import com.libra.app.domain.model.ServerRoleDefinition
import com.libra.app.domain.model.StorageUploadRequest
import com.libra.app.ui.components.UserAvatar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.compose.ui.platform.LocalContext
import kotlin.math.roundToInt

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun CommunityServersScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val communityRepository = ServiceLocator.communityRepository
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val pinPrefs = remember { context.getSharedPreferences("libra_server_pins", android.content.Context.MODE_PRIVATE) }

    var servers by remember { mutableStateOf<List<CommunityServer>>(emptyList()) }
    var pinnedServerIds by remember { mutableStateOf(pinPrefs.getStringSet("ids", emptySet()).orEmpty().toSet()) }
    var selectedServer by remember { mutableStateOf<CommunityServer?>(null) }
    var selectedChannel by remember { mutableStateOf<ServerChannel?>(null) }
    var categories by remember { mutableStateOf<List<ServerCategory>>(emptyList()) }
    var channels by remember { mutableStateOf<List<ServerChannel>>(emptyList()) }
    var previewServer by remember { mutableStateOf<CommunityServer?>(null) }
    var previewIsMember by remember { mutableStateOf(false) }
    var showCreate by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var enteringServerId by remember { mutableStateOf<String?>(null) }
    var channelPermissions by remember { mutableStateOf<Map<String, List<ServerChannelPermissionOverride>>>(emptyMap()) }

    BackHandler(enabled = selectedServer != null || selectedChannel != null) {
        if (selectedChannel != null) {
            selectedChannel = null
        } else {
            selectedServer = null
            categories = emptyList()
            channels = emptyList()
        }
    }

    LaunchedEffect(Unit) {
        communityRepository.observeCommunityServers().collect { result ->
            when (result) {
                is AppResult.Success -> servers = result.data.distinctBy { it.id }
                is AppResult.Error -> error = result.error.message
            }
        }
    }

    LaunchedEffect(selectedServer?.id) {
        val serverId = selectedServer?.id ?: return@LaunchedEffect
        communityRepository.ensureServerStructure(serverId)
        launch {
            communityRepository.observeServerCategories(serverId).collect { result ->
                if (result is AppResult.Success) categories = result.data
            }
        }
        launch {
            communityRepository.observeServerChannels(serverId).collect { result ->
                if (result is AppResult.Success) channels = result.data
            }
        }
    }

    LaunchedEffect(selectedServer?.id, channels) {
        if (selectedServer != null && selectedChannel == null && channels.isNotEmpty()) {
            selectedChannel = channels.first()
        }
        if (selectedChannel != null && channels.none { it.id == selectedChannel?.id }) {
            selectedChannel = channels.firstOrNull()
        }
    }

    if (selectedServer != null) {
        val server = selectedServer!!

        Row(modifier.fillMaxSize()) {
            ServerRail(
                servers = servers
                    .distinctBy { it.id }
                    .sortedWith(
                        compareByDescending<CommunityServer> { pinnedServerIds.contains(it.id) }
                            .thenByDescending { it.createdAt }
                    ),
                selectedServerId = server.id,
                onBackToCommunity = {
                    selectedServer = null
                    selectedChannel = null
                    categories = emptyList()
                    channels = emptyList()
                },
                onCreateServer = { showCreate = true },
                onServerClick = { target ->
                    if (target.id != server.id) {
                        scope.launch {
                        when (val member = communityRepository.isServerMember(target.id)) {
                            is AppResult.Success -> {
                                if (member.data) {
                                    selectedServer = target
                                    selectedChannel = null
                                } else {
                                    previewIsMember = false
                                    previewServer = target
                                }
                            }
                            is AppResult.Error -> error = member.error.message
                        }
                        }
                    }
                }
            )

            Box(Modifier.weight(1f).fillMaxHeight()) {
                AnimatedContent(
                    targetState = selectedChannel,
                    transitionSpec = {
                        fadeIn() togetherWith fadeOut()
                    },
                    label = "serverChannelTransition"
                ) { channel ->
                    if (channel != null) {
                        ServerChatScreen(
                            server = server,
                            channel = channel,
                            onBack = { selectedChannel = null },
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        ServerWorkspace(
                            server = server,
                            categories = categories,
                            channels = channels,
                            onBack = {
                                selectedServer = null
                                selectedChannel = null
                                categories = emptyList()
                                channels = emptyList()
                            },
                            onChannelClick = { selectedChannel = it },
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }
        }
        return
    }

    Column(modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Geri") }
            Column(Modifier.weight(1f)) {
                Text("Sunucular", style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold))
                Text("Topluluklara göz at, katıl ve kendi kanallarında sohbet et.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            IconButton(onClick = { showCreate = true }) { Icon(Icons.Default.Add, "Sunucu oluştur") }
        }
        Spacer(Modifier.height(12.dp))
        error?.let { Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(bottom = 8.dp)) }

        if (servers.isEmpty()) {
            Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp)) {
                Column(Modifier.fillMaxWidth().padding(28.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.Groups, null, modifier = Modifier.size(52.dp))
                    Spacer(Modifier.height(12.dp))
                    Text("Henüz sunucu yok.", fontWeight = FontWeight.Bold)
                    Text("İlk topluluğu sen oluştur.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(10.dp))
                    Button(onClick = { showCreate = true }) { Text("Sunucu oluştur") }
                }
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(10.dp), contentPadding = PaddingValues(bottom = 24.dp)) {
                items(servers.distinctBy { it.id }.sortedWith(compareByDescending<CommunityServer> { pinnedServerIds.contains(it.id) }.thenByDescending { it.createdAt }), key = { it.id }) { server ->
                    Card(Modifier.fillMaxWidth().clickable {
                        scope.launch {
                            when (val member = communityRepository.isServerMember(server.id)) {
                                is AppResult.Success -> if (member.data) {
                                    selectedServer = server
                                    selectedChannel = null
                                    communityRepository.ensureServerStructure(server.id)
                                } else {
                                    previewIsMember = false
                                    previewServer = server
                                }
                                is AppResult.Error -> error = member.error.message
                            }
                        }
                    }, shape = RoundedCornerShape(18.dp)) {
                        Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.size(54.dp).clip(RoundedCornerShape(16.dp)).background(MaterialTheme.colorScheme.primaryContainer), contentAlignment = Alignment.Center) {
                                Text(server.name.take(1).uppercase(), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleLarge)
                            }
                            Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                                Text(server.name, fontWeight = FontWeight.Bold)
                                if (server.description.isNotBlank()) Text(server.description, maxLines = 2, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Spacer(Modifier.height(5.dp))
                                Text("Topluluk sunucusu • Katılmak için dokun", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                TextButton(onClick = {
                                    val next = pinnedServerIds.toMutableSet()
                                    if (!next.add(server.id)) next.remove(server.id)
                                    pinnedServerIds = next.toSet()
                                    pinPrefs.edit().putStringSet("ids", next).apply()
                                }) { Text(if (pinnedServerIds.contains(server.id)) "Sabit" else "Sabitle") }
                                Icon(Icons.Default.ChevronRight, "Sunucuyu aç")
                            }
                        }
                    }
                }
            }
        }
    }

    previewServer?.let { server ->
        ServerPreviewDialog(
            server = server,
            isMember = previewIsMember,
            onDismiss = { previewServer = null },
            onEnter = {
                if (enteringServerId != server.id) {
                    enteringServerId = server.id
                    scope.launch {
                    val result = if (previewIsMember) {
                        communityRepository.ensureServerStructure(server.id)
                    } else {
                        when (val joined = communityRepository.joinCommunityServer(server.id)) {
                            is AppResult.Success -> communityRepository.ensureServerStructure(server.id)
                            is AppResult.Error -> joined
                        }
                    }
                    when (result) {
                        is AppResult.Success -> { previewServer = null; selectedServer = server; selectedChannel = null; error = null }
                        is AppResult.Error -> error = result.error.message
                    }
                    enteringServerId = null
                    }
                }
            }
        )
        LaunchedEffect(server.id) {
            previewIsMember = when (val result = communityRepository.isServerMember(server.id)) {
                is AppResult.Success -> result.data
                is AppResult.Error -> false
            }
        }
    }

    if (showCreate) {
        CreateServerDialog(
            onDismiss = { showCreate = false },
            onCreate = { name, description ->
                scope.launch {
                    when (val result = communityRepository.createCommunityServer(name, description)) {
                        is AppResult.Success -> { showCreate = false; error = null; selectedServer = result.data; selectedChannel = null }
                        is AppResult.Error -> error = result.error.message
                    }
                }
            }
        )
    }
}

@Composable
private fun ServerRail(
    servers: List<CommunityServer>,
    selectedServerId: String,
    onBackToCommunity: () -> Unit,
    onCreateServer: () -> Unit,
    onServerClick: (CommunityServer) -> Unit
) {
    Surface(
        modifier = Modifier
            .width(72.dp)
            .fillMaxHeight(),
        color = MaterialTheme.colorScheme.surfaceVariant,
        tonalElevation = 4.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .navigationBarsPadding()
                .padding(vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            RailActionButton(
                icon = Icons.Default.ArrowBack,
                contentDescription = "Topluluklara dön",
                onClick = onBackToCommunity
            )

            Spacer(Modifier.height(8.dp))
            HorizontalDivider(
                modifier = Modifier
                    .width(34.dp)
                    .padding(vertical = 2.dp)
            )
            Spacer(Modifier.height(8.dp))

            LazyColumn(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(vertical = 4.dp)
            ) {
                items(servers, key = { it.id }) { server ->
                    ServerRailItem(
                        server = server,
                        selected = server.id == selectedServerId,
                        onClick = { onServerClick(server) }
                    )
                }
            }

            Spacer(Modifier.height(8.dp))
            RailActionButton(
                icon = Icons.Default.Add,
                contentDescription = "Sunucu oluştur veya ekle",
                onClick = onCreateServer,
                accent = true
            )
        }
    }
}

@Composable
private fun RailActionButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    accent: Boolean = false
) {
    Box(
        modifier = Modifier
            .size(48.dp)
            .clip(CircleShape)
            .background(
                if (accent) MaterialTheme.colorScheme.primaryContainer
                else MaterialTheme.colorScheme.background
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            icon,
            contentDescription = contentDescription,
            tint = if (accent) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.onSurface
            }
        )
    }
}

@Composable
private fun ServerRailItem(
    server: CommunityServer,
    selected: Boolean,
    onClick: () -> Unit
) {
    val itemSize by animateDpAsState(
        targetValue = if (selected) 50.dp else 44.dp,
        label = "serverRailItemSize"
    )
    val corner by animateDpAsState(
        targetValue = if (selected) 16.dp else 14.dp,
        label = "serverRailItemCorner"
    )

    Box(
        modifier = Modifier
            .width(64.dp)
            .height(58.dp),
        contentAlignment = Alignment.Center
    ) {
        if (selected) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .width(4.dp)
                    .height(40.dp)
                    .clip(RoundedCornerShape(topEnd = 4.dp, bottomEnd = 4.dp))
                    .background(MaterialTheme.colorScheme.primary)
            )
        }

        Box(
            modifier = Modifier
                .size(itemSize)
                .clip(RoundedCornerShape(corner))
                .background(
                    if (selected) MaterialTheme.colorScheme.primaryContainer
                    else MaterialTheme.colorScheme.background
                )
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center
        ) {
            if (server.avatarUrl.isNotBlank()) {
                var avatarUrl by remember(server.id, server.avatarUrl) { mutableStateOf<String?>(null) }

                LaunchedEffect(server.id, server.avatarUrl) {
                    avatarUrl = ServiceLocator.storageRepository.getPublicCdnUrl(server.avatarUrl)
                }

                AsyncImage(
                    model = avatarUrl,
                    contentDescription = server.name,
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(if (selected) 16.dp else 14.dp)),
                    contentScale = ContentScale.Crop
                )
            } else {
                Text(
                    server.name.take(1).uppercase().ifBlank { "?" },
                    fontWeight = FontWeight.Bold,
                    color = if (selected) {
                        MaterialTheme.colorScheme.onPrimaryContainer
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    }
                )
            }
        }
    }
}

@Composable
private fun InfoPill(value: String, label: String) {
    Surface(shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.surfaceVariant) {
        Column(Modifier.padding(horizontal = 10.dp, vertical = 7.dp)) {
            Text(value, fontWeight = FontWeight.Bold)
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun ServerPreviewDialog(
    server: CommunityServer,
    isMember: Boolean,
    onDismiss: () -> Unit,
    onEnter: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(52.dp).clip(RoundedCornerShape(16.dp)).background(MaterialTheme.colorScheme.primaryContainer), contentAlignment = Alignment.Center) {
                    Text(server.name.take(1).uppercase(), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleLarge)
                }
                Spacer(Modifier.width(12.dp))
                Text(server.name, fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(server.description.ifBlank { "Libra topluluğuna katıl ve kanallarda sohbet etmeye başla." }, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Surface(color = MaterialTheme.colorScheme.surfaceVariant, shape = RoundedCornerShape(14.dp)) {
                    Column(Modifier.fillMaxWidth().padding(12.dp)) {
                        Text("Sunucu yapısı", fontWeight = FontWeight.SemiBold)
                        Text("Kategoriler • Metin kanalları • Üyeler • Gerçek zamanlı sohbet", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        },
        confirmButton = { Button(onClick = onEnter) { Text(if (isMember) "Sunucuya gir" else "Katıl") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Vazgeç") } }
    )
}

@Composable
private fun ServerWorkspace(
    server: CommunityServer,
    categories: List<ServerCategory>,
    channels: List<ServerChannel>,
    onBack: () -> Unit,
    onChannelClick: (ServerChannel) -> Unit,
    modifier: Modifier = Modifier
) {
    val repo = ServiceLocator.communityRepository
    val scope = rememberCoroutineScope()
    val currentUid = ServiceLocator.authRepository.currentUser.value?.uid.orEmpty()
    val isOwner = currentUid == server.ownerId
    var members by remember { mutableStateOf<List<ServerMember>>(emptyList()) }
    var serverRoles by remember { mutableStateOf<List<ServerRoleDefinition>>(emptyList()) }
    var channelPermissions by remember { mutableStateOf<Map<String, List<ServerChannelPermissionOverride>>>(emptyMap()) }
    var showInfo by remember { mutableStateOf(false) }
    var showServerSettings by remember { mutableStateOf(false) }
    var editServerName by remember(server.id) { mutableStateOf(server.name) }
    var editServerDescription by remember(server.id) { mutableStateOf(server.description) }
    var showCreateCategory by remember { mutableStateOf(false) }
    var showCreateChannel by remember { mutableStateOf<ServerCategory?>(null) }
    var showOwnerManagement by remember { mutableStateOf(false) }
    var permissionChannel by remember { mutableStateOf<ServerChannel?>(null) }
    var channelMenu by remember { mutableStateOf<ServerChannel?>(null) }
    var categoryMenu by remember { mutableStateOf<ServerCategory?>(null) }
    var editChannel by remember { mutableStateOf<ServerChannel?>(null) }
    var editCategory by remember { mutableStateOf<ServerCategory?>(null) }
    var moveChannel by remember { mutableStateOf<ServerChannel?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var channelSearch by remember(server.id) { mutableStateOf("") }
    var collapsedCategories by remember(server.id) { mutableStateOf<Set<String>>(emptySet()) }

    LaunchedEffect(server.id) {
        repo.observeServerMembers(server.id).collect { result ->
            if (result is AppResult.Success) members = result.data
        }
    }

    LaunchedEffect(server.id) {
        repo.observeServerRoles(server.id).collect { result ->
            if (result is AppResult.Success) serverRoles = result.data
        }
    }

    LaunchedEffect(channels, members, isOwner) {
        if (isOwner) {
            channelPermissions = emptyMap()
        } else {
            val map = mutableMapOf<String, List<ServerChannelPermissionOverride>>()
            channels.forEach { channel ->
                when (val result = repo.getChannelPermissions(server.id, channel.id)) {
                    is AppResult.Success -> map[channel.id] = result.data
                    is AppResult.Error -> Unit
                }
            }
            channelPermissions = map
        }
    }

    val currentMember = members.firstOrNull { it.uid == currentUid }
    val currentRolePermissions = serverRoles.firstOrNull { it.id == currentMember?.role }?.permissions.orEmpty()
    val visibleChannels = channels.filter { channel ->
        (isOwner || canViewServerChannel(channel, channelPermissions[channel.id].orEmpty(), currentMember, currentRolePermissions)) &&
            channel.name.contains(channelSearch.trim(), ignoreCase = true)
    }

    Row(modifier.fillMaxSize()) {
        Surface(
            modifier = Modifier.width(220.dp).fillMaxHeight(),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 3.dp
        ) {
            Column(Modifier.fillMaxSize()) {
                Row(Modifier.fillMaxWidth().statusBarsPadding().padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Sunucu listesinden çık") }
                    Text(
                        "KANALLAR",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f)
                    )
                    if (isOwner) {
                        IconButton(onClick = { showOwnerManagement = true }) {
                            Icon(Icons.Default.Settings, "Sunucu ayarları")
                        }
                    }
                }

                Row(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).clickable { showInfo = true }
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        Modifier
                            .size(46.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        if (server.avatarUrl.isNotBlank()) {
                            AsyncImage(
                                ServiceLocator.storageRepository.getPublicCdnUrl(server.avatarUrl),
                                contentDescription = server.name,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Text(server.name.take(1).uppercase(), fontWeight = FontWeight.Bold)
                        }
                    }
                    Column(Modifier.weight(1f).padding(start = 9.dp)) {
                        Text(server.name, maxLines = 1, fontWeight = FontWeight.Bold)
                        Text("Kanal ve sunucu bilgileri", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Icon(Icons.Default.ChevronRight, null, modifier = Modifier.size(18.dp))
                }

                HorizontalDivider()

                OutlinedTextField(
                    value = channelSearch,
                    onValueChange = { channelSearch = it },
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 6.dp),
                    singleLine = true,
                    leadingIcon = { Icon(Icons.Default.Search, "Kanal ara") },
                    placeholder = { Text("Kanal ara") },
                    shape = RoundedCornerShape(12.dp)
                )

                if (isOwner) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp, vertical = 2.dp),
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)
                    ) {
                        TextButton(onClick = { showCreateCategory = true }, modifier = Modifier.fillMaxWidth()) {
                            Text("+ Kategori ekle", fontWeight = FontWeight.SemiBold)
                        }
                    }
                }

                if (categories.isEmpty() || channels.isEmpty()) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp))
                    }
                } else {
                    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 18.dp)) {
                        categories.forEach { category ->
                            val collapsed = collapsedCategories.contains(category.id)
                            item(key = "category-" + category.id) {
                                Row(
                                    Modifier
                                        .fillMaxWidth()
                                        .padding(start = 10.dp, end = 6.dp, top = 14.dp, bottom = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    IconButton(onClick = {
                                        collapsedCategories = if (collapsed) collapsedCategories - category.id else collapsedCategories + category.id
                                    }, modifier = Modifier.size(30.dp)) {
                                        Icon(if (collapsed) Icons.Default.ExpandMore else Icons.Default.ExpandLess, "Kategoriyi aç/kapat", modifier = Modifier.size(17.dp))
                                    }
                                    Text(
                                        category.name.uppercase(),
                                        modifier = Modifier.weight(1f),
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    if (isOwner) {
                                        IconButton(onClick = { showCreateChannel = category }, modifier = Modifier.size(30.dp)) {
                                            Icon(Icons.Default.Add, "Kanal ekle", modifier = Modifier.size(17.dp))
                                        }
                                        IconButton(onClick = { categoryMenu = category }, modifier = Modifier.size(30.dp)) {
                                            Icon(Icons.Default.MoreVert, "Kategori seçenekleri", modifier = Modifier.size(17.dp))
                                        }
                                    }
                                }
                            }
                            if (!collapsed) visibleChannels.filter { it.categoryId == category.id }.forEach { channel ->
                                item(key = "channel-" + channel.id) {
                                    Row(
                                        Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(10.dp))
                                            .pointerInput(channel.id + "-menu") {
                                                detectTapGestures(
                                                    onTap = { onChannelClick(channel) },
                                                    onLongPress = { if (isOwner) channelMenu = channel }
                                                )
                                            }
                                            .background(
                                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.42f)
                                            )
                                            .padding(horizontal = 12.dp, vertical = 10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            "#",
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Spacer(Modifier.width(8.dp))
                                        Text(
                                            channel.name,
                                            maxLines = 1,
                                            style = MaterialTheme.typography.bodyMedium,
                                            modifier = Modifier.weight(1f)
                                        )
                                        if (isOwner) {
                                            Surface(
                                                shape = CircleShape,
                                                color = MaterialTheme.colorScheme.background.copy(alpha = 0.55f)
                                            ) {
                                                Icon(
                                                    Icons.Default.MoreVert,
                                                    contentDescription = "Kanal seçenekleri. Basılı tutarak da yönetebilirsin.",
                                                    modifier = Modifier.padding(5.dp).size(16.dp),
                                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        Box(
            Modifier
                .fillMaxSize()
                .weight(1f)
                .background(MaterialTheme.colorScheme.background)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(Modifier.height(32.dp))
                Box(
                    Modifier
                        .size(96.dp)
                        .clip(RoundedCornerShape(28.dp))
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    if (server.avatarUrl.isNotBlank()) {
                        AsyncImage(
                            ServiceLocator.storageRepository.getPublicCdnUrl(server.avatarUrl),
                            contentDescription = server.name,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Text(
                            server.name.take(1).uppercase().ifBlank { "L" },
                            style = MaterialTheme.typography.displaySmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }

                Spacer(Modifier.height(18.dp))
                Text(
                    server.name,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    server.description.ifBlank {
                        "Libra topluluğuna hoş geldin. Bir kanal seçerek sohbete başlayabilirsin."
                    },
                    modifier = Modifier.widthIn(max = 520.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(Modifier.height(22.dp))

                Row(
                    modifier = Modifier.widthIn(max = 520.dp).fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Surface(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
                    ) {
                        Column(Modifier.padding(14.dp)) {
                            Text("Üye", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(members.size.toString(), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        }
                    }
                    Surface(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
                    ) {
                        Column(Modifier.padding(14.dp)) {
                            Text("Kanal", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(visibleChannels.size.toString(), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Spacer(Modifier.height(22.dp))

                Surface(
                    modifier = Modifier.widthIn(max = 520.dp).fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                ) {
                    Column(Modifier.padding(18.dp)) {
                        Text(
                            "Topluluğa giriş",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            if (visibleChannels.isEmpty()) {
                                "Görüntülenebilen bir kanal bulunmuyor."
                            } else {
                                "Soldan bir kanal seç. Metin kanallarında gerçek zamanlı sohbet başlayacak."
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }

    if (showInfo) {
        AlertDialog(
            onDismissRequest = { showInfo = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier.size(44.dp).clip(RoundedCornerShape(14.dp)).background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        if (server.avatarUrl.isNotBlank()) {
                            AsyncImage(
                                ServiceLocator.storageRepository.getPublicCdnUrl(server.avatarUrl),
                                contentDescription = server.name,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Text(server.name.take(1).uppercase(), fontWeight = FontWeight.Bold)
                        }
                    }
                    Spacer(Modifier.width(10.dp))
                    Column {
                        Text(server.name, fontWeight = FontWeight.Bold)
                        Text("Sunucu bilgileri", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(server.description.ifBlank { "Bu sunucunun henüz bir açıklaması yok." })
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        InfoPill(members.size.toString(), "üye")
                        InfoPill(channels.size.toString(), "kanal")
                        InfoPill(categories.size.toString(), "kategori")
                    }
                    if (isOwner) {
                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Text(
                                "Sunucu sahibi olarak kanal, kategori ve izinleri yönetebilirsin.",
                                modifier = Modifier.padding(10.dp),
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            },
            confirmButton = {
                if (isOwner) {
                    TextButton(onClick = { showInfo = false; showOwnerManagement = true }) { Text("Sunucu ayarları") }
                } else {
                    TextButton(onClick = { showInfo = false }) { Text("Tamam") }
                }
            },
            dismissButton = { TextButton(onClick = { showInfo = false }) { Text("Kapat") }
        )
    }

    if (showServerSettings && isOwner) {
        AlertDialog(
            onDismissRequest = { showServerSettings = false },
            title = { Text("Sunucu ayarları") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = editServerName,
                        onValueChange = { if (it.length <= 40) editServerName = it },
                        label = { Text("Sunucu adı") },
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = editServerDescription,
                        onValueChange = { if (it.length <= 160) editServerDescription = it },
                        label = { Text("Açıklama") },
                        minLines = 3,
                        maxLines = 5
                    )
                    Text(
                        "Sunucu sahibi olarak kanal, kategori ve kanal izinlerini de bu ekrandan yönetebilirsin.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                TextButton(
                    enabled = editServerName.trim().length >= 2,
                    onClick = {
                        scope.launch {
                            when (val result = repo.updateCommunityServer(server.id, editServerName, editServerDescription)) {
                                is AppResult.Success -> showServerSettings = false
                                is AppResult.Error -> error = result.error.message
                            }
                        }
                    }
                ) { Text("Kaydet") }
            },
            dismissButton = { TextButton(onClick = { showServerSettings = false }) { Text("İptal") } }
        )
    }

    if (showCreateCategory) {
        NameDialog("Kategori oluştur", "Kategori adı", "Oluştur", { showCreateCategory = false }) { name ->
            scope.launch {
                when (val result = repo.createServerCategory(server.id, name)) {
                    is AppResult.Success -> showCreateCategory = false
                    is AppResult.Error -> error = result.error.message
                }
            }
        }
    }

    showCreateChannel?.let { category ->
        NameDialog("Kanal oluştur", "# kanal adı", "Oluştur", { showCreateChannel = null }) { name ->
            scope.launch {
                when (val result = repo.createServerChannel(server.id, category.id, name)) {
                    is AppResult.Success -> showCreateChannel = null
                    is AppResult.Error -> error = result.error.message
                }
            }
        }
    }

    channelMenu?.let { channel ->
        AlertDialog(
            onDismissRequest = { channelMenu = null },
            title = { Text("# \${channel.name}") },
            text = { Column { Text("Kanal yönetimi"); TextButton(onClick = { channelMenu = null; editChannel = channel }) { Text("Kanal adını düzenle") }; TextButton(onClick = { channelMenu = null; moveChannel = channel }) { Text("Başka kategoriye taşı") } } },
            confirmButton = { TextButton(onClick = { channelMenu = null; permissionChannel = channel }) { Text("İzinler") } },
            dismissButton = {
                TextButton(onClick = {
                    scope.launch {
                        when (val result = repo.deleteServerChannel(server.id, channel.id)) {
                            is AppResult.Success -> channelMenu = null
                            is AppResult.Error -> error = result.error.message
                        }
                    }
                }) { Text("Kanalı sil") }
            }
        )
    }

    editChannel?.let { channel ->
        NameDialog("Kanalı düzenle", "# kanal adı", "Kaydet", { editChannel = null }, channel.name) { name ->
            scope.launch {
                when (val result = repo.updateServerChannel(server.id, channel.id, name)) {
                    is AppResult.Success -> editChannel = null
                    is AppResult.Error -> error = result.error.message
                }
            }
        }
    }

    editCategory?.let { category ->
        NameDialog("Kategoriyi düzenle", "Kategori adı", "Kaydet", { editCategory = null }, category.name) { name ->
            scope.launch {
                when (val result = repo.updateServerCategory(server.id, category.id, name)) {
                    is AppResult.Success -> editCategory = null
                    is AppResult.Error -> error = result.error.message
                }
            }
        }
    }

    moveChannel?.let { channel ->
        AlertDialog(
            onDismissRequest = { moveChannel = null },
            title = { Text("Kanalı taşı") },
            text = {
                Column {
                    categories.filter { it.id != channel.categoryId }.forEach { category ->
                        TextButton(onClick = {
                            scope.launch {
                                when (val result = repo.moveServerChannelToCategory(server.id, channel.id, category.id)) {
                                    is AppResult.Success -> moveChannel = null
                                    is AppResult.Error -> error = result.error.message
                                }
                            }
                        }) { Text(category.name) }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { moveChannel = null }) { Text("Kapat") } }
        )
    }
    categoryMenu?.let { category ->
        AlertDialog(
            onDismissRequest = { categoryMenu = null },
            title = { Text(category.name) },
            text = { Column { Text("Bu kategorideki kanallar da silinecek."); TextButton(onClick = { categoryMenu = null; editCategory = category }) { Text("Kategori adını düzenle") } } },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch {
                        when (val result = repo.deleteServerCategory(server.id, category.id)) {
                            is AppResult.Success -> categoryMenu = null
                            is AppResult.Error -> error = result.error.message
                        }
                    }
                }) { Text("Kategoriyi sil") }
            },
            dismissButton = { TextButton(onClick = { categoryMenu = null }) { Text("Vazgeç") } }
        )
    }

    permissionChannel?.let { channel ->
        ChannelPermissionDialog(server.id, channel, members) { permissionChannel = null }
    }

    if (showOwnerManagement && isOwner) {
        ServerOwnerSettingsDialog(
            server = server,
            members = members,
            categories = categories,
            channels = channels,
            onDismiss = { showOwnerManagement = false },
            onDeleted = { showOwnerManagement = false; onBack() },
            onError = { error = it }
        )
    }

    error?.let {
        AlertDialog(onDismissRequest = { error = null }, title = { Text("İşlem başarısız") }, text = { Text(it) }, confirmButton = { TextButton(onClick = { error = null }) { Text("Tamam") } })
    }
}

@Composable
private fun MessageDateSeparator(createdAt: Long) {
    val locale = java.util.Locale.getDefault()
    val calendar = java.util.Calendar.getInstance().apply { timeInMillis = createdAt }
    val now = java.util.Calendar.getInstance()

    val text = remember(createdAt) {
        val sameDay = calendar.get(java.util.Calendar.ERA) == now.get(java.util.Calendar.ERA) &&
            calendar.get(java.util.Calendar.YEAR) == now.get(java.util.Calendar.YEAR) &&
            calendar.get(java.util.Calendar.DAY_OF_YEAR) == now.get(java.util.Calendar.DAY_OF_YEAR)

        val yesterday = java.util.Calendar.getInstance().apply {
            timeInMillis = now.timeInMillis
            add(java.util.Calendar.DAY_OF_YEAR, -1)
        }

        val isYesterday = calendar.get(java.util.Calendar.ERA) == yesterday.get(java.util.Calendar.ERA) &&
            calendar.get(java.util.Calendar.YEAR) == yesterday.get(java.util.Calendar.YEAR) &&
            calendar.get(java.util.Calendar.DAY_OF_YEAR) == yesterday.get(java.util.Calendar.DAY_OF_YEAR)

        when {
            sameDay -> "Bugün"
            isYesterday -> "Dün"
            else -> java.text.SimpleDateFormat("dd MMMM yyyy", locale).format(java.util.Date(createdAt))
        }
    }

    Row(
        Modifier.fillMaxWidth().padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        HorizontalDivider(Modifier.weight(1f))
        Surface(
            color = MaterialTheme.colorScheme.surfaceVariant,
            shape = RoundedCornerShape(999.dp)
        ) {
            Text(
                text,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        HorizontalDivider(Modifier.weight(1f))
    }
}

private fun canViewServerChannel(
    channel: ServerChannel,
    permissions: List<ServerChannelPermissionOverride>,
    member: ServerMember?,
    rolePermissions: List<String>
): Boolean {
    var allowed = when (member?.role) {
        "ADMIN", "OWNER", "MEMBER" -> channel.allowEveryoneView
        else -> "VIEW_CHANNEL" in rolePermissions && channel.allowEveryoneView
    }
    permissions.firstOrNull { it.subjectType == "ROLE" && it.subjectId == "EVERYONE" }?.canView?.let { allowed = it }
    val role = member?.role.orEmpty()
    permissions.firstOrNull { it.subjectType == "ROLE" && it.subjectId == role }?.canView?.let { allowed = it }
    permissions.firstOrNull { it.subjectType == "USER" && it.subjectId == member?.uid }?.canView?.let { allowed = it }
    return allowed
}

private fun canSendServerChannel(
    channel: ServerChannel,
    permissions: List<ServerChannelPermissionOverride>,
    member: ServerMember?,
    rolePermissions: List<String>
): Boolean {
    var allowed = when (member?.role) {
        "ADMIN", "OWNER", "MEMBER" -> channel.allowEveryoneSend
        else -> "SEND_MESSAGES" in rolePermissions && channel.allowEveryoneSend
    }
    permissions.firstOrNull { it.subjectType == "ROLE" && it.subjectId == "EVERYONE" }?.canSend?.let { allowed = it }
    val role = member?.role.orEmpty()
    permissions.firstOrNull { it.subjectType == "ROLE" && it.subjectId == role }?.canSend?.let { allowed = it }
    permissions.firstOrNull { it.subjectType == "USER" && it.subjectId == member?.uid }?.canSend?.let { allowed = it }
    return allowed
}

@Composable
private fun NameDialog(
    title: String,
    label: String,
    confirm: String,
    onDismiss: () -> Unit,
    initialValue: String = "",
    onConfirm: (String) -> Unit
) {
    var value by remember { mutableStateOf(initialValue) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { OutlinedTextField(value, { if (it.length <= 40) value = it }, label = { Text(label) }, singleLine = true) },
        confirmButton = { TextButton(enabled = value.trim().isNotEmpty(), onClick = { onConfirm(value.trim()) }) { Text(confirm) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Vazgeç") } }
    )
}

@Composable
fun ChannelPermissionDialog(
    serverId: String,
    channel: ServerChannel,
    members: List<ServerMember>,
    onDismiss: () -> Unit
) {
    val repo = ServiceLocator.communityRepository
    val scope = rememberCoroutineScope()
    var permissions by remember(channel.id) { mutableStateOf<List<ServerChannelPermissionOverride>>(emptyList()) }
    var selected by remember { mutableStateOf<ServerChannelPermissionOverride?>(null) }

    LaunchedEffect(channel.id) {
        repo.observeChannelPermissions(serverId, channel.id).collect { result ->
            if (result is AppResult.Success) permissions = result.data
        }
    }

    val subjects = remember(members) {
        listOf(
            ServerChannelPermissionOverride(subjectType = "ROLE", subjectId = "EVERYONE", subjectName = "@everyone"),
            ServerChannelPermissionOverride(subjectType = "ROLE", subjectId = "ADMIN", subjectName = "@admin"),
            ServerChannelPermissionOverride(subjectType = "ROLE", subjectId = "MEMBER", subjectName = "@member")
        ) + members.map {
            ServerChannelPermissionOverride(subjectType = "USER", subjectId = it.uid, subjectName = "@" + it.username.ifBlank { it.displayName })
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("# \${channel.name} • İzinler") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Rol veya kişi seç. Sonra kanalı görme ve mesaj yazma izinlerini ayarla.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                LazyColumn(Modifier.heightIn(max = 240.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    items(subjects, key = { it.subjectType + ":" + it.subjectId }) { subject ->
                        val active = selected?.subjectType == subject.subjectType && selected?.subjectId == subject.subjectId
                        Surface(color = if (active) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant, shape = RoundedCornerShape(10.dp), modifier = Modifier.fillMaxWidth().clickable {
                            selected = permissions.firstOrNull { it.subjectType == subject.subjectType && it.subjectId == subject.subjectId } ?: subject
                        }) { Text(subject.subjectName, modifier = Modifier.padding(10.dp)) }
                    }
                }
                selected?.let { target ->
                    var canView by remember(target.id, target.subjectId) { mutableStateOf(target.canView) }
                    var canSend by remember(target.id, target.subjectId) { mutableStateOf(target.canSend) }
                    Text("Seçili: \${target.subjectName}", fontWeight = FontWeight.Bold)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = canView == true, onCheckedChange = { canView = if (it) true else false })
                        Text("Kanalı görebilir")
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = canSend == true, onCheckedChange = { canSend = if (it) true else false })
                        Text("Mesaj yazabilir")
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = {
                            scope.launch { repo.setChannelPermission(serverId, channel.id, target.copy(canView = canView, canSend = canSend)) }
                        }) { Text("Kaydet") }
                        OutlinedButton(onClick = {
                            if (target.id.isNotBlank()) scope.launch { repo.deleteChannelPermission(serverId, channel.id, target.id) }
                        }) { Text("Varsayılana dön") }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Kapat") } }
    )
}

@Composable
private fun CreateServerDialog(
    onDismiss: () -> Unit,
    onCreate: (String, String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Yeni sunucu") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(name, { if (it.length <= 40) name = it }, label = { Text("Sunucu adı") }, singleLine = true)
                OutlinedTextField(description, { if (it.length <= 160) description = it }, label = { Text("Açıklama") }, minLines = 2, maxLines = 3)
            }
        },
        confirmButton = {
            TextButton(enabled = name.trim().length >= 2, onClick = { onCreate(name, description) }) { Text("Oluştur") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("İptal") } }
    )
}

@Composable
private fun ServerChatScreen(
    server: CommunityServer,
    channel: ServerChannel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val chatRepository = ServiceLocator.chatRepository
    val communityRepository = ServiceLocator.communityRepository
    val scope = rememberCoroutineScope()
    var messages by remember(server.id, channel.id) { mutableStateOf<List<ServerMessage>>(emptyList()) }
    var error by remember(server.id) { mutableStateOf<String?>(null) }
    var draft by remember { mutableStateOf("") }
    var replyTarget by remember { mutableStateOf<ServerMessage?>(null) }
    var actionMessage by remember { mutableStateOf<ServerMessage?>(null) }
    var editingMessage by remember { mutableStateOf<ServerMessage?>(null) }
    var pendingUri by remember { mutableStateOf<android.net.Uri?>(null) }
    var pendingUrl by remember { mutableStateOf("") }
    var pendingType by remember { mutableStateOf("image/jpeg") }
    var uploading by remember { mutableStateOf(false) }
    var progress by remember { mutableIntStateOf(0) }
    var mediaError by remember { mutableStateOf<String?>(null) }
    var showMembers by remember { mutableStateOf(false) }
    var showManage by remember { mutableStateOf(false) }
    var showServerInfo by remember { mutableStateOf(false) }
    var actionError by remember(server.id) { mutableStateOf<String?>(null) }
    var serverName by remember(server.id) { mutableStateOf(server.name) }
    var serverDescription by remember(server.id) { mutableStateOf(server.description) }
    var members by remember { mutableStateOf<List<ServerMember>>(emptyList()) }
    var serverRoles by remember { mutableStateOf<List<ServerRoleDefinition>>(emptyList()) }
    var canSendInChannel by remember(server.id, channel.id) { mutableStateOf(true) }
    val listState = rememberLazyListState()
    val currentUid = ServiceLocator.authRepository.currentUser.value?.uid.orEmpty()
    val context = androidx.compose.ui.platform.LocalContext.current
    val picker = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.GetContent()
    ) { uri ->
        uri ?: return@rememberLauncherForActivityResult
        pendingUri = uri
        pendingUrl = ""
        pendingType = context.contentResolver.getType(uri).orEmpty().ifBlank { "image/jpeg" }
        uploading = true
        progress = 0
        mediaError = null
        scope.launch {
            try {
                val bytes = withContext(Dispatchers.IO) {
                    context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                } ?: throw IllegalStateException("Fotoğraf okunamadı.")
                if (bytes.size > 8 * 1024 * 1024) throw IllegalStateException("Fotoğraf 8 MB'dan küçük olmalı.")
                val ext = pendingType.substringAfter('/').ifBlank { "jpg" }.take(8)
                val request = StorageUploadRequest(
                    fileName = "server-" + System.currentTimeMillis() + "." + ext,
                    bytes = bytes,
                    contentType = pendingType,
                    targetDirectory = "users/" + currentUid
                )
                when (val result = ServiceLocator.storageRepository.uploadMedia(request) { progress = it }.first()) {
                    is AppResult.Success -> { pendingUrl = result.data; progress = 100 }
                    is AppResult.Error -> mediaError = result.error.message
                }
            } catch (e: Exception) {
                mediaError = e.localizedMessage ?: "Fotoğraf yüklenemedi."
            } finally { uploading = false }
        }
    }

    LaunchedEffect(server.id) {
        communityRepository.observeServerMembers(server.id).collect { result ->
            if (result is AppResult.Success) members = result.data
        }
    }

    LaunchedEffect(server.id) {
        communityRepository.observeServerRoles(server.id).collect { result ->
            if (result is AppResult.Success) serverRoles = result.data
        }
    }

    LaunchedEffect(channel.id, members, serverRoles) {
        val currentMember = members.firstOrNull { it.uid == currentUid }
        when (val result = communityRepository.getChannelPermissions(server.id, channel.id)) {
            is AppResult.Success -> {
                canSendInChannel = currentMember?.let {
                    canSendServerChannel(
                        channel,
                        result.data,
                        it,
                        serverRoles.firstOrNull { role -> role.id == it.role }?.permissions.orEmpty()
                    )
                } ?: true
            }
            is AppResult.Error -> canSendInChannel = true
        }
    }

    LaunchedEffect(server.id, channel.id) {
        messages = emptyList()
        chatRepository.observeServerMessages(server.id, channelId = channel.id).collect { result ->
            when (result) {
                is AppResult.Success -> messages = result.data
                is AppResult.Error -> error = result.error.message
            }
        }
    }

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) listState.animateScrollToItem(messages.lastIndex)
    }

    Column(modifier.fillMaxSize()) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 2.dp
        ) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 6.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, "Kanallara dön")
                }
                Column(
                    Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { showServerInfo = true }
                        .padding(horizontal = 8.dp, vertical = 5.dp)
                ) {
                    Text(
                        "# " + channel.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1
                    )
                    Text(
                        serverName + " • " + members.size + " üye",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1
                    )
                }
                if (server.ownerId == currentUid) {
                    IconButton(onClick = { showManage = true }) {
                        Icon(Icons.Default.Settings, "Sunucu yönetimi")
                    }
                }
                IconButton(onClick = { showMembers = true }) {
                    Icon(Icons.Default.People, "Üyeler")
                }
            }
        }

        error?.let { Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(horizontal = 12.dp)) }
        actionError?.let { Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(horizontal = 12.dp)) }
        mediaError?.let { Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(horizontal = 12.dp)) }

        if (pendingUri != null) {
            Row(Modifier.fillMaxWidth().padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                AsyncImage(pendingUri, "Gönderilecek fotoğraf", Modifier.size(64.dp).clip(RoundedCornerShape(10.dp)), contentScale = ContentScale.Crop)
                Column(Modifier.weight(1f).padding(horizontal = 10.dp)) {
                    Text(if (uploading) "Fotoğraf yükleniyor %" + progress else if (pendingUrl.isNotBlank()) "Fotoğraf hazır ✓" else "Fotoğraf hazırlanıyor…")
                    if (uploading) LinearProgressIndicator({ progress / 100f }, Modifier.fillMaxWidth())
                }
                IconButton(onClick = { pendingUri = null; pendingUrl = ""; mediaError = null }) { Icon(Icons.Default.Close, "Kaldır") }
            }
        }

        LazyColumn(
            state = listState,
            modifier = Modifier.weight(1f).fillMaxWidth(),
            contentPadding = PaddingValues(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            itemsIndexed(messages, key = { _, message -> message.id }) { index, message ->
                val previous = messages.getOrNull(index - 1)
                val groupedWithPrevious = previous != null &&
                    previous.senderId == message.senderId &&
                    message.createdAt - previous.createdAt in 0..300_000L
                if (!groupedWithPrevious) {
                    MessageDateSeparator(message.createdAt)
                }
                var dragX by remember(message.id) { mutableFloatStateOf(0f) }
                Box(
                    Modifier.fillMaxWidth()
                        .pointerInput(message.id + "-swipe") {
                            detectHorizontalDragGestures(
                                onHorizontalDrag = { _, amount ->
                                    dragX = (dragX + amount).coerceIn(0f, 96f)
                                },
                                onDragEnd = {
                                    if (dragX >= 64f) replyTarget = message
                                    dragX = 0f
                                },
                                onDragCancel = { dragX = 0f }
                            )
                        }
                ) {
                    if (dragX > 0f) {
                        Surface(
                            modifier = Modifier
                                .align(Alignment.CenterStart)
                                .padding(start = 8.dp),
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primaryContainer.copy(
                                alpha = (dragX / 96f).coerceIn(0.18f, 1f)
                            )
                        ) {
                            Icon(
                                Icons.Default.ChevronRight,
                                contentDescription = "Yanıtla",
                                modifier = Modifier.padding(7.dp).size(18.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    Row(
                        Modifier.fillMaxWidth()
                            .offset { IntOffset(dragX.roundToInt(), 0) },
                        horizontalArrangement = if (message.senderId == currentUid) Arrangement.End else Arrangement.Start,
                        verticalAlignment = Alignment.Bottom
                    ) {
                    if (message.senderId != currentUid) {
                        if (groupedWithPrevious) {
                            Spacer(Modifier.width(36.dp))
                        } else {
                            UserAvatar(message.senderPhotoUrl, message.senderName.take(1).uppercase(), size = 30.dp)
                        }
                        Spacer(Modifier.width(6.dp))
                    }
                    Surface(
                        color = when {
                            message.mentionedUserIds.contains(currentUid) -> androidx.compose.ui.graphics.Color(0xFFFFE8D5)
                            message.senderId == currentUid -> MaterialTheme.colorScheme.primaryContainer
                            else -> MaterialTheme.colorScheme.surfaceVariant
                        },
                        shape = RoundedCornerShape(
                            topStart = if (groupedWithPrevious) 8.dp else 16.dp,
                            topEnd = if (groupedWithPrevious) 8.dp else 16.dp,
                            bottomStart = 16.dp,
                            bottomEnd = 16.dp
                        ),
                        modifier = Modifier.pointerInput(message.id + "-tap") {
                            detectTapGestures(
                                onDoubleTap = { scope.launch { chatRepository.toggleServerMessageReaction(server.id, message.id, "❤️", channelId = channel.id) } },
                                onLongPress = { actionMessage = message }
                            )
                        }
                    ) {
                        Column(Modifier.widthIn(max = 320.dp).padding(6.dp)) {
                            if (message.senderId != currentUid && !groupedWithPrevious) {
                                Row(
                                    Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        message.senderName.ifBlank { "Libra kullanıcısı" },
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault())
                                            .format(java.util.Date(message.createdAt)),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            if (message.replyToMessageId.isNotBlank()) {
                                Surface(color = androidx.compose.ui.graphics.Color(0xFFFFE8D5), shape = RoundedCornerShape(10.dp), modifier = Modifier.fillMaxWidth()) {
                                    Column(Modifier.padding(8.dp)) {
                                        Text(message.replyToSenderName.ifBlank { "Yanıtlanan mesaj" }, color = androidx.compose.ui.graphics.Color(0xFFB45309), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelSmall)
                                        Text(message.replyToText.ifBlank { "📷 Fotoğraf" }, maxLines = 2, style = MaterialTheme.typography.bodySmall)
                                    }
                                }
                                Spacer(Modifier.height(5.dp))
                            }
                            message.sharedContent?.let { shared ->
                                Surface(
                                    modifier = Modifier.fillMaxWidth().padding(4.dp),
                                    shape = RoundedCornerShape(10.dp),
                                    color = MaterialTheme.colorScheme.background.copy(alpha = 0.45f)
                                ) {
                                    Column(Modifier.padding(8.dp)) {
                                        Text(
                                            when (shared.type) {
                                                "story" -> "Hikâyeden bahsetti"
                                                "post" -> "Bir gönderi paylaştı"
                                                "book" -> "Bir kitap paylaştı"
                                                else -> "Bir içerik paylaştı"
                                            },
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(shared.title.ifBlank { "Libra içeriği" }, fontWeight = FontWeight.SemiBold)
                                        if (shared.text.isNotBlank()) Text(shared.text, maxLines = 2, style = MaterialTheme.typography.bodySmall)
                                    }
                                }
                            }

                            if (message.mediaUrl.isNotBlank()) {
                                AsyncImage(
                                    ServiceLocator.storageRepository.getPublicCdnUrl(message.mediaUrl),
                                    "Gönderilen fotoğraf",
                                    Modifier
                                        .widthIn(max = 260.dp)
                                        .heightIn(max = 280.dp)
                                        .clip(RoundedCornerShape(12.dp)),
                                    contentScale = ContentScale.Crop
                                )
                            }
                            if (message.text.isNotBlank()) {
                                Text(message.text, Modifier.padding(horizontal = 8.dp, vertical = 6.dp))
                            }
                            if (message.editedAt != null) {
                                Text(
                                    "düzenlendi",
                                    Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            if (message.reactions.isNotEmpty()) {
                                Row(
                                    Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 4.dp, vertical = 2.dp),
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    message.reactions.values
                                        .groupingBy { it }
                                        .eachCount()
                                        .forEach { (emoji, count) ->
                                            Surface(
                                                color = MaterialTheme.colorScheme.background.copy(alpha = 0.72f),
                                                shape = RoundedCornerShape(999.dp),
                                                tonalElevation = 1.dp
                                            ) {
                                                Text(
                                                    "$emoji $count",
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                                    style = MaterialTheme.typography.labelSmall
                                                )
                                            }
                                        }
                                }
                            }
                        }
                    }
                }
            }
        }

        replyTarget?.let { RichReplyBanner(it.senderName, it.text.ifBlank { "📷 Fotoğraf" }, { replyTarget = null }) }
        editingMessage?.let { RichReplyBanner("Mesaj düzenleniyor", it.text, { editingMessage = null; draft = "" }) }

        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            shape = RoundedCornerShape(18.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.72f),
            tonalElevation = 1.dp
        ) {
            Row(
                Modifier.padding(horizontal = 4.dp, vertical = 4.dp),
                verticalAlignment = Alignment.Bottom
            ) {
                IconButton(
                    enabled = canSendInChannel && !uploading && editingMessage == null,
                    onClick = { picker.launch("image/*") }
                ) {
                    Icon(Icons.Default.AddPhotoAlternate, "Fotoğraf ekle")
                }
                OutlinedTextField(
                    value = draft,
                    onValueChange = { if (it.length <= 1000) draft = it },
                    modifier = Modifier.weight(1f),
                    placeholder = {
                        Text(
                            if (canSendInChannel) "Mesaj yaz…" else "Bu kanalda mesaj yazma iznin yok."
                        )
                    },
                    enabled = canSendInChannel && !uploading,
                    maxLines = 5,
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = androidx.compose.ui.graphics.Color.Transparent,
                        unfocusedBorderColor = androidx.compose.ui.graphics.Color.Transparent,
                        disabledBorderColor = androidx.compose.ui.graphics.Color.Transparent,
                        focusedContainerColor = androidx.compose.ui.graphics.Color.Transparent,
                        unfocusedContainerColor = androidx.compose.ui.graphics.Color.Transparent,
                        disabledContainerColor = androidx.compose.ui.graphics.Color.Transparent
                    )
                )
                IconButton(
                    enabled = canSendInChannel &&
                        (draft.isNotBlank() || pendingUrl.isNotBlank()) &&
                        !uploading,
                    onClick = {
                        val text = draft.trim()
                        val edit = editingMessage
                        if (edit != null) {
                            scope.launch {
                                when (val result = chatRepository.editServerMessage(server.id, edit.id, text, channelId = channel.id)) {
                                    is AppResult.Error -> actionError = result.error.message
                                    is AppResult.Success -> {
                                        actionError = null
                                        editingMessage = null
                                        draft = ""
                                    }
                                }
                            }
                        } else if (pendingUrl.isNotBlank()) {
                            scope.launch {
                                when (val result = chatRepository.sendServerMediaMessage(server.id, pendingUrl, pendingType, text, replyTarget, channelId = channel.id)) {
                                    is AppResult.Error -> actionError = result.error.message
                                    is AppResult.Success -> {
                                        actionError = null
                                        pendingUri = null
                                        pendingUrl = ""
                                        replyTarget = null
                                        draft = ""
                                    }
                                }
                            }
                        } else {
                            scope.launch {
                                when (val result = chatRepository.sendServerMessage(server.id, text, replyTarget, channelId = channel.id)) {
                                    is AppResult.Error -> actionError = result.error.message
                                    is AppResult.Success -> {
                                        actionError = null
                                        replyTarget = null
                                        draft = ""
                                    }
                                }
                            }
                        }
                    }
                ) {
                    Icon(
                        if (editingMessage != null) Icons.Default.Edit else Icons.Default.Send,
                        if (editingMessage != null) "Düzenlemeyi kaydet" else "Gönder"
                    )
                }
            }
        }
    }
    actionMessage?.let { message ->
        RichMessageActionSheet(
            canEdit = message.senderId == currentUid && message.mediaUrl.isBlank(),
            canDelete = message.senderId == currentUid,
            onDismiss = { actionMessage = null },
            onReply = { replyTarget = message; actionMessage = null },
            onReaction = { emoji -> scope.launch { chatRepository.toggleServerMessageReaction(server.id, message.id, emoji, channelId = channel.id) }; actionMessage = null },
            onEdit = { editingMessage = message; draft = message.text; actionMessage = null },
            onDelete = { scope.launch { chatRepository.deleteServerMessage(server.id, message.id, channelId = channel.id) }; actionMessage = null }
        )
    }

    if (showServerInfo) {
        AlertDialog(
            onDismissRequest = { showServerInfo = false },
            title = { Text(serverName) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(serverDescription.ifBlank { "Bu sunucunun henüz bir açıklaması yok." })
                    Text("${members.size} üye • Sunucu sahibi: ${if (server.ownerId == currentUid) "sensin" else "başka bir kullanıcı"}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            },
            confirmButton = {
                if (server.ownerId == currentUid) {
                    TextButton(onClick = { showServerInfo = false; showManage = true }) { Text("Sunucu ayarları") }
                } else {
                    TextButton(onClick = { showServerInfo = false }) { Text("Tamam") }
                }
            },
            dismissButton = { TextButton(onClick = { showServerInfo = false }) { Text("Kapat") } }
        )
    }

    if (showManage && server.ownerId == currentUid) {
        AlertDialog(
            onDismissRequest = { showManage = false },
            title = { Text("Sunucu yönetimi") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = serverName,
                        onValueChange = { if (it.length <= 40) serverName = it },
                        label = { Text("Sunucu adı") },
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = serverDescription,
                        onValueChange = { if (it.length <= 160) serverDescription = it },
                        label = { Text("Açıklama") },
                        minLines = 2,
                        maxLines = 4
                    )
                    Text(
                        "Üyeler ekranından Admin rolü verebilir, üyeleri çıkarabilir veya rollerini değiştirebilirsin.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    actionError?.let {
                        Text(it, color = MaterialTheme.colorScheme.error)
                    }
                }
            },
            confirmButton = {
                TextButton(
                    enabled = serverName.trim().length >= 2,
                    onClick = {
                        scope.launch {
                            when (val result = communityRepository.updateCommunityServer(server.id, serverName, serverDescription)) {
                                is AppResult.Success -> {
                                    actionError = null
                                    showManage = false
                                }
                                is AppResult.Error -> actionError = result.error.message
                            }
                        }
                    }
                ) { Text("Kaydet") }
            },
            dismissButton = {
                TextButton(onClick = { showManage = false }) { Text("İptal") }
            }
        )
    }

    if (showMembers) {
        AlertDialog(
            onDismissRequest = { showMembers = false },
            title = { Text("Üyeler (${members.size})") },
            text = {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(members, key = { it.uid }) { member ->
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            UserAvatar(member.photoUrl, member.displayName.take(1).uppercase(), size = 38.dp)
                            Spacer(Modifier.width(8.dp))
                            Column(Modifier.weight(1f)) {
                                Text(member.displayName.ifBlank { member.uid }, fontWeight = FontWeight.Bold)
                                Text(member.role, style = MaterialTheme.typography.labelSmall)
                            }
                            if (server.ownerId == currentUid && member.uid != currentUid) {
                                TextButton(onClick = {
                                    scope.launch {
                                        communityRepository.setServerMemberRole(server.id, member.uid, if (member.role == "ADMIN") "MEMBER" else "ADMIN")
                                    }
                                }) { Text(if (member.role == "ADMIN") "Üyeye indir" else "Admin yap") }
                                TextButton(onClick = {
                                    scope.launch { communityRepository.removeServerMember(server.id, member.uid) }
                                }) { Text("Çıkar") }
                            }
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showMembers = false }) { Text("Kapat") } }
        )
    }
}