package com.libra.app.feature.messages

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
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
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
import com.libra.app.domain.model.StorageUploadRequest
import com.libra.app.ui.components.UserAvatar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt

@Composable
fun CommunityServersScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val communityRepository = ServiceLocator.communityRepository
    val scope = rememberCoroutineScope()

    var servers by remember { mutableStateOf<List<CommunityServer>>(emptyList()) }
    var selectedServer by remember { mutableStateOf<CommunityServer?>(null) }
    var selectedChannel by remember { mutableStateOf<ServerChannel?>(null) }
    var categories by remember { mutableStateOf<List<ServerCategory>>(emptyList()) }
    var channels by remember { mutableStateOf<List<ServerChannel>>(emptyList()) }
    var previewServer by remember { mutableStateOf<CommunityServer?>(null) }
    var previewIsMember by remember { mutableStateOf(false) }
    var showCreate by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        communityRepository.observeCommunityServers().collect { result ->
            when (result) {
                is AppResult.Success -> servers = result.data
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
        if (selectedChannel != null) {
            ServerChatScreen(
                server = server,
                channel = selectedChannel!!,
                onBack = { selectedChannel = null },
                modifier = modifier
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
                modifier = modifier
            )
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
                items(servers, key = { it.id }) { server ->
                    Card(Modifier.fillMaxWidth().clickable { previewServer = server }, shape = RoundedCornerShape(18.dp)) {
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
                            Icon(Icons.Default.ChevronRight, "Sunucuyu görüntüle")
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
    var showInfo by remember { mutableStateOf(false) }
    var showCreateCategory by remember { mutableStateOf(false) }
    var showCreateChannel by remember { mutableStateOf<ServerCategory?>(null) }
    var permissionChannel by remember { mutableStateOf<ServerChannel?>(null) }
    var channelMenu by remember { mutableStateOf<ServerChannel?>(null) }
    var categoryMenu by remember { mutableStateOf<ServerCategory?>(null) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(server.id) {
        repo.observeServerMembers(server.id).collect { result ->
            if (result is AppResult.Success) members = result.data
        }
    }

    Row(modifier.fillMaxSize()) {
        Surface(modifier = Modifier.width(180.dp).fillMaxHeight(), tonalElevation = 3.dp) {
            Column(Modifier.fillMaxSize()) {
                Row(Modifier.fillMaxWidth().statusBarsPadding().padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Sunucu listesinden çık") }
                    Spacer(Modifier.weight(1f))
                }

                Row(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).clickable { showInfo = true }
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(Modifier.size(46.dp).clip(RoundedCornerShape(14.dp)).background(MaterialTheme.colorScheme.primaryContainer), contentAlignment = Alignment.Center) {
                        Text(server.name.take(1).uppercase(), fontWeight = FontWeight.Bold)
                    }
                    Column(Modifier.weight(1f).padding(start = 9.dp)) {
                        Text(server.name, maxLines = 1, fontWeight = FontWeight.Bold)
                        Text("Sunucu bilgileri", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Icon(Icons.Default.ChevronRight, null, modifier = Modifier.size(18.dp))
                }

                HorizontalDivider()

                if (isOwner) {
                    TextButton(onClick = { showCreateCategory = true }, modifier = Modifier.fillMaxWidth()) {
                        Text("+ Kategori ekle")
                    }
                }

                if (categories.isEmpty() || channels.isEmpty()) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp))
                    }
                } else {
                    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 18.dp)) {
                        categories.forEach { category ->
                            item(key = "category-" + category.id) {
                                Row(Modifier.fillMaxWidth().padding(start = 10.dp, top = 14.dp, bottom = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Text(category.name, modifier = Modifier.weight(1f), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                            channels.filter { it.categoryId == category.id }.forEach { channel ->
                                item(key = "channel-" + channel.id) {
                                    Row(
                                        Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp))
                                            .pointerInput(channel.id + "-menu") {
                                                detectTapGestures(
                                                    onTap = { onChannelClick(channel) },
                                                    onLongPress = { if (isOwner) channelMenu = channel }
                                                )
                                            }
                                            .padding(horizontal = 10.dp, vertical = 9.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("#", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Spacer(Modifier.width(6.dp))
                                        Text(channel.name, maxLines = 1, style = MaterialTheme.typography.labelMedium)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        Box(Modifier.fillMaxSize().weight(1f), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(24.dp)) {
                Text(server.name, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(6.dp))
                Text("Bir kanal seçerek topluluğa gir.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }

    if (showInfo) {
        AlertDialog(
            onDismissRequest = { showInfo = false },
            title = { Text(server.name) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(server.description.ifBlank { "Bu sunucunun henüz bir açıklaması yok." })
                    Text("\${members.size} üye • \${channels.size} kanal • \${categories.size} kategori", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    if (isOwner) Text("Sunucu sahibi olarak kanal, kategori ve izinleri yönetebilirsin.", style = MaterialTheme.typography.bodySmall)
                }
            },
            confirmButton = { TextButton(onClick = { showInfo = false }) { Text(if (isOwner) "Yönetim seçenekleri aşağıda" else "Tamam") } },
            dismissButton = { TextButton(onClick = { showInfo = false }) { Text("Kapat") } }
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
            text = { Text("Kanal yönetimi") },
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

    categoryMenu?.let { category ->
        AlertDialog(
            onDismissRequest = { categoryMenu = null },
            title = { Text(category.name) },
            text = { Text("Bu kategorideki kanallar da silinecek.") },
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

    error?.let {
        AlertDialog(onDismissRequest = { error = null }, title = { Text("İşlem başarısız") }, text = { Text(it) }, confirmButton = { TextButton(onClick = { error = null }) { Text("Tamam") } })
    }
}

@Composable
private fun NameDialog(
    title: String,
    label: String,
    confirm: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var value by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { OutlinedTextField(value, { if (it.length <= 40) value = it }, label = { Text(label) }, singleLine = true) },
        confirmButton = { TextButton(enabled = value.trim().isNotEmpty(), onClick = { onConfirm(value.trim()) }) { Text(confirm) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Vazgeç") } }
    )
}

@Composable
private fun ChannelPermissionDialog(
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
    var messages by remember(server.id) { mutableStateOf<List<ServerMessage>>(emptyList()) }
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
    var actionError by remember(server.id) { mutableStateOf<String?>(null) }
    var serverName by remember(server.id) { mutableStateOf(server.name) }
    var serverDescription by remember(server.id) { mutableStateOf(server.description) }
    var members by remember { mutableStateOf<List<ServerMember>>(emptyList()) }
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
        Row(
            Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Geri") }
            Column(Modifier.weight(1f)) {
                Text("# " + channel.name, fontWeight = FontWeight.Bold)
                Text(serverName + " • " + members.size + " üye", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
            items(messages, key = { it.id }) { message ->
                var dragX by remember(message.id) { mutableFloatStateOf(0f) }
                Row(
                    Modifier.fillMaxWidth()
                        .pointerInput(message.id + "-swipe") {
                            detectHorizontalDragGestures(
                                onHorizontalDrag = { _, amount -> dragX = (dragX + amount).coerceIn(0f, 96f) },
                                onDragEnd = { if (dragX >= 64f) replyTarget = message; dragX = 0f }
                            )
                        }
                        .offset { IntOffset(dragX.roundToInt(), 0) },
                    horizontalArrangement = if (message.senderId == currentUid) Arrangement.End else Arrangement.Start,
                    verticalAlignment = Alignment.Bottom
                ) {
                    if (message.senderId != currentUid) {
                        UserAvatar(message.senderPhotoUrl, message.senderName.take(1).uppercase(), size = 30.dp)
                        Spacer(Modifier.width(6.dp))
                    }
                    Surface(
                        color = if (message.senderId == currentUid) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.pointerInput(message.id + "-tap") {
                            detectTapGestures(
                                onDoubleTap = { scope.launch { chatRepository.toggleServerMessageReaction(server.id, message.id, "❤️", channelId = channel.id) } },
                                onLongPress = { actionMessage = message }
                            )
                        }
                    ) {
                        Column(Modifier.widthIn(max = 320.dp).padding(6.dp)) {
                            if (message.senderId != currentUid) Text(message.senderName, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, modifier = Modifier.padding(8.dp, 4.dp))
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

                            if (message.mediaUrl.isNotBlank()) AsyncImage(ServiceLocator.storageRepository.getPublicCdnUrl(message.mediaUrl), "Gönderilen fotoğraf", Modifier.width(220.dp).heightIn(max = 280.dp).clip(RoundedCornerShape(12.dp)), contentScale = ContentScale.Crop)
                            if (message.text.isNotBlank()) Text(message.text, Modifier.padding(8.dp, 6.dp))
                            if (message.editedAt != null) Text("düzenlendi", Modifier.padding(horizontal = 8.dp), style = MaterialTheme.typography.labelSmall)
                            if (message.reactions.isNotEmpty()) Text(message.reactions.values.distinct().joinToString(" "), Modifier.padding(horizontal = 8.dp, vertical = 2.dp))
                        }
                    }
                }
            }
        }

        replyTarget?.let { RichReplyBanner(it.senderName, it.text.ifBlank { "📷 Fotoğraf" }, { replyTarget = null }) }
        editingMessage?.let { RichReplyBanner("Mesaj düzenleniyor", it.text, { editingMessage = null; draft = "" }) }

        Row(
            Modifier.fillMaxWidth().navigationBarsPadding().padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(enabled = !uploading && editingMessage == null, onClick = { picker.launch("image/*") }) {
                Icon(Icons.Default.AddPhotoAlternate, "Fotoğraf")
            }
            OutlinedTextField(
                value = draft,
                onValueChange = { if (it.length <= 1000) draft = it },
                modifier = Modifier.weight(1f),
                placeholder = { Text("Topluluğa mesaj yaz…") },
                maxLines = 4
            )
            IconButton(
                enabled = (draft.isNotBlank() || pendingUrl.isNotBlank()) && !uploading,
                onClick = {
                    val text = draft.trim()
                    val edit = editingMessage
                    if (edit != null) {
                        scope.launch { chatRepository.editServerMessage(server.id, edit.id, text, channelId = channel.id) }
                        editingMessage = null
                    } else if (pendingUrl.isNotBlank()) {
                        scope.launch { chatRepository.sendServerMediaMessage(server.id, pendingUrl, pendingType, text, replyTarget, channelId = channel.id) }
                        pendingUri = null
                        pendingUrl = ""
                        replyTarget = null
                    } else {
                        scope.launch { chatRepository.sendServerMessage(server.id, text, replyTarget, channelId = channel.id) }
                        replyTarget = null
                    }
                    draft = ""
                }
            ) { Icon(Icons.Default.Send, "Gönder") }
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