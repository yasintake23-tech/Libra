package com.libra.app.feature.messages

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.libra.app.core.di.ServiceLocator
import com.libra.app.core.result.AppResult
import com.libra.app.domain.model.*
import kotlinx.coroutines.launch

private enum class ServerSettingsPage(val title: String, val description: String) {
    HOME("Sunucu ayarları", "Sunucunu ve topluluk yapısını yönet."),
    ROLES("Roller", "Roller ve izinler"),
    MEMBERS("Üyeler", "Üyeleri ve rollerini yönet"),
    BANS("Yasaklar", "Sunucudan uzaklaştırılan üyeler"),
    CATEGORIES("Kategoriler", "Kanal gruplarını düzenle"),
    CHANNELS("Kanallar", "Kanalları ve kanal izinlerini yönet"),
    MEMBER_DETAIL("Üye yönetimi", "Üye ayrıntıları ve işlemler"),
    ROLE_EDITOR("Rol düzenle", "Rol adı ve izinleri")
}

private fun Modifier.longPressReorder(
    itemKey: String,
    onMove: (Int) -> Unit
): Modifier = pointerInput(itemKey) {
    var accumulatedY = 0f
    var triggered = false
    detectDragGesturesAfterLongPress(
        onDrag = { change, dragAmount ->
            change.consume()
            if (!triggered) {
                accumulatedY += dragAmount.y
                if (kotlin.math.abs(accumulatedY) >= 36f) {
                    triggered = true
                    onMove(if (accumulatedY < 0f) -1 else 1)
                }
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ServerOwnerSettingsDialog(
    server: CommunityServer,
    members: List<ServerMember>,
    categories: List<ServerCategory>,
    channels: List<ServerChannel>,
    onDismiss: () -> Unit,
    onDeleted: () -> Unit,
    onError: (String) -> Unit
) {
    val repo = ServiceLocator.communityRepository
    val scope = rememberCoroutineScope()

    var page by remember { mutableStateOf(ServerSettingsPage.HOME) }
    var roles by remember { mutableStateOf<List<ServerRoleDefinition>>(emptyList()) }
    var bans by remember { mutableStateOf<List<ServerBan>>(emptyList()) }
    var memberSearch by remember { mutableStateOf("") }
    var selectedMember by remember { mutableStateOf<ServerMember?>(null) }
    var editingRole by remember { mutableStateOf<ServerRoleDefinition?>(null) }
    var roleName by remember { mutableStateOf("") }
    var rolePermissions by remember { mutableStateOf(setOf("VIEW_CHANNEL", "SEND_MESSAGES")) }
    var actionError by remember { mutableStateOf<String?>(null) }
    var confirmDelete by remember { mutableStateOf(false) }

    LaunchedEffect(server.id) {
        repo.observeServerRoles(server.id).collect { result ->
            if (result is AppResult.Success) roles = result.data
        }
    }

    LaunchedEffect(server.id) {
        repo.observeServerBans(server.id).collect { result ->
            if (result is AppResult.Success) bans = result.data
        }
    }

    fun goTo(next: ServerSettingsPage) {
        actionError = null
        page = next
    }

    fun back() {
        actionError = null
        when (page) {
            ServerSettingsPage.HOME -> onDismiss()
            ServerSettingsPage.MEMBER_DETAIL -> page = ServerSettingsPage.MEMBERS
            ServerSettingsPage.ROLE_EDITOR -> page = ServerSettingsPage.ROLES
            else -> page = ServerSettingsPage.HOME
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false,
            dismissOnBackPress = false
        )
    ) {
        BackHandler { back() }

        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(Modifier.fillMaxSize()) {
                TopAppBar(
                    title = {
                        Column {
                            Text(page.title, fontWeight = FontWeight.Bold)
                            Text(
                                page.description,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = { back() }) {
                            Icon(
                                Icons.Default.ArrowBack,
                                if (page == ServerSettingsPage.HOME) "Kapat" else "Geri"
                            )
                        }
                    },
                    actions = {
                        if (page != ServerSettingsPage.HOME) {
                            TextButton(onClick = { goTo(ServerSettingsPage.HOME) }) {
                                Text("Ana sayfa")
                            }
                        }
                    }
                )

                HorizontalDivider()

                actionError?.let {
                    Text(
                        it,
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                        color = MaterialTheme.colorScheme.error
                    )
                }

                when (page) {
                    ServerSettingsPage.HOME -> ServerSettingsHome(
                        server = server,
                        rolesCount = roles.size,
                        membersCount = members.size,
                        bansCount = bans.size,
                        categoriesCount = categories.size,
                        channelsCount = channels.size,
                        onRoles = { goTo(ServerSettingsPage.ROLES) },
                        onMembers = { goTo(ServerSettingsPage.MEMBERS) },
                        onBans = { goTo(ServerSettingsPage.BANS) },
                        onCategories = { goTo(ServerSettingsPage.CATEGORIES) },
                        onChannels = { goTo(ServerSettingsPage.CHANNELS) },
                        onDelete = { confirmDelete = true }
                    )

                    ServerSettingsPage.ROLES -> ServerRolesPage(
                        roles = roles,
                        onCreate = {
                            editingRole = null
                            roleName = ""
                            rolePermissions = setOf("VIEW_CHANNEL", "SEND_MESSAGES")
                            goTo(ServerSettingsPage.ROLE_EDITOR)
                        },
                        onEdit = {
                            editingRole = it
                            roleName = it.name
                            rolePermissions = it.permissions.toSet()
                            goTo(ServerSettingsPage.ROLE_EDITOR)
                        },
                        onDelete = { role ->
                            scope.launch {
                                when (val result = repo.deleteServerRole(server.id, role.id)) {
                                    is AppResult.Error -> actionError = result.error.message
                                    is AppResult.Success -> actionError = null
                                }
                            }
                        },
                        onMove = { role, direction ->
                            scope.launch {
                                when (val result = repo.moveServerRole(server.id, role.id, direction)) {
                                    is AppResult.Error -> actionError = result.error.message
                                    is AppResult.Success -> actionError = null
                                }
                            }
                        }
                    )

                    ServerSettingsPage.MEMBERS -> {
                        val filteredMembers = members.filter {
                            it.uid != server.ownerId &&
                                (memberSearch.isBlank() ||
                                    it.displayName.contains(memberSearch, true) ||
                                    it.username.contains(memberSearch, true))
                        }
                        ServerMembersPage(
                            members = filteredMembers,
                            search = memberSearch,
                            onSearchChange = { memberSearch = it },
                            onManage = {
                                selectedMember = it
                                goTo(ServerSettingsPage.MEMBER_DETAIL)
                            }
                        )
                    }

                    ServerSettingsPage.MEMBER_DETAIL -> {
                        val member = selectedMember
                        if (member == null) {
                            goTo(ServerSettingsPage.MEMBERS)
                        } else {
                            ServerMemberDetailPage(
                                member = member,
                                roles = roles,
                                onRoleChange = { role ->
                                    scope.launch {
                                        when (val result = repo.setServerMemberRole(server.id, member.uid, role)) {
                                            is AppResult.Error -> actionError = result.error.message
                                            is AppResult.Success -> {
                                                actionError = null
                                                selectedMember = members.firstOrNull { it.uid == member.uid }
                                                    ?: member.copy(role = role)
                                            }
                                        }
                                    }
                                },
                                onKick = {
                                    scope.launch {
                                        when (val result = repo.removeServerMember(server.id, member.uid)) {
                                            is AppResult.Error -> actionError = result.error.message
                                            is AppResult.Success -> {
                                                selectedMember = null
                                                page = ServerSettingsPage.MEMBERS
                                            }
                                        }
                                    }
                                },
                                onBan = {
                                    scope.launch {
                                        when (val result = repo.banServerMember(server.id, member.uid, "")) {
                                            is AppResult.Error -> actionError = result.error.message
                                            is AppResult.Success -> {
                                                selectedMember = null
                                                page = ServerSettingsPage.BANS
                                            }
                                        }
                                    }
                                }
                            )
                        }
                    }

                    ServerSettingsPage.BANS -> ServerBansPage(
                        bans = bans,
                        onUnban = { ban ->
                            scope.launch {
                                when (val result = repo.unbanServerMember(server.id, ban.uid)) {
                                    is AppResult.Error -> actionError = result.error.message
                                    is AppResult.Success -> actionError = null
                                }
                            }
                        }
                    )

                    ServerSettingsPage.CATEGORIES -> ServerCategoriesPage(
                        categories = categories,
                        onDelete = { category ->
                            scope.launch {
                                when (val result = repo.deleteServerCategory(server.id, category.id)) {
                                    is AppResult.Error -> actionError = result.error.message
                                    is AppResult.Success -> actionError = null
                                }
                            }
                        },
                        onMove = { category, direction ->
                            scope.launch {
                                when (val result = repo.moveServerCategory(server.id, category.id, direction)) {
                                    is AppResult.Error -> actionError = result.error.message
                                    is AppResult.Success -> actionError = null
                                }
                            }
                        }
                    )

                    ServerSettingsPage.CHANNELS -> ServerChannelsPage(
                        channels = channels,
                        members = members,
                        serverId = server.id,
                        onDelete = { channel ->
                            scope.launch {
                                when (val result = repo.deleteServerChannel(server.id, channel.id)) {
                                    is AppResult.Error -> actionError = result.error.message
                                    is AppResult.Success -> actionError = null
                                }
                            }
                        },
                        onMove = { channel, direction ->
                            scope.launch {
                                when (val result = repo.moveServerChannel(server.id, channel.id, direction)) {
                                    is AppResult.Error -> actionError = result.error.message
                                    is AppResult.Success -> actionError = null
                                }
                            }
                        }
                    )

                    ServerSettingsPage.ROLE_EDITOR -> ServerRoleEditorPage(
                        role = editingRole,
                        name = roleName,
                        permissions = rolePermissions,
                        onNameChange = { roleName = it },
                        onPermissionChange = { permission, checked ->
                            rolePermissions = rolePermissions.toMutableSet().apply {
                                if (checked) add(permission) else remove(permission)
                            }
                        },
                        onSave = {
                            scope.launch {
                                val name = roleName.trim()
                                if (name.isBlank()) {
                                    actionError = "Rol adı boş olamaz."
                                    return@launch
                                }
                                val result = if (editingRole == null) {
                                    repo.createServerRole(server.id, name, rolePermissions.toList())
                                } else {
                                    repo.updateServerRole(server.id, editingRole!!.id, name, rolePermissions.toList())
                                }
                                when (result) {
                                    is AppResult.Error -> actionError = result.error.message
                                    is AppResult.Success -> {
                                        actionError = null
                                        page = ServerSettingsPage.ROLES
                                    }
                                }
                            }
                        }
                    )
                }
            }
        }
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Sunucuyu sil") },
            text = { Text("Sunucu, üyeler, roller, kategoriler, kanallar ve ban kayıtları kalıcı olarak silinecek.") },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch {
                        when (val result = repo.deleteCommunityServer(server.id)) {
                            is AppResult.Error -> actionError = result.error.message
                            is AppResult.Success -> {
                                confirmDelete = false
                                onDeleted()
                            }
                        }
                    }
                }) {
                    Text("Kalıcı olarak sil")
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = false }) { Text("İptal") }
            }
        )
    }
}

@Composable
private fun ServerSettingsHome(
    server: CommunityServer,
    rolesCount: Int,
    membersCount: Int,
    bansCount: Int,
    categoriesCount: Int,
    channelsCount: Int,
    onRoles: () -> Unit,
    onMembers: () -> Unit,
    onBans: () -> Unit,
    onCategories: () -> Unit,
    onChannels: () -> Unit,
    onDelete: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Surface(
                Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                color = MaterialTheme.colorScheme.primaryContainer
            ) {
                Column(Modifier.padding(18.dp)) {
                    Text(server.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text(
                        server.description.ifBlank { "Bu sunucunun açıklaması yok." },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        }
        item { SettingsNavigationCard("Roller", "Roller ve izinler", rolesCount, onRoles) }
        item { SettingsNavigationCard("Üyeler", "Üye rolleri ve moderasyon", membersCount, onMembers) }
        item { SettingsNavigationCard("Yasaklar", "Ban kayıtlarını yönet", bansCount, onBans) }
        item { SettingsNavigationCard("Kategoriler", "Kanal gruplarını düzenle", categoriesCount, onCategories) }
        item { SettingsNavigationCard("Kanallar", "Kanal sırası, silme ve izinler", channelsCount, onChannels) }
        item {
            Spacer(Modifier.height(10.dp))
            Button(
                onClick = onDelete,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            ) {
                Icon(Icons.Default.Delete, null)
                Spacer(Modifier.width(8.dp))
                Text("Sunucuyu tamamen sil")
            }
        }
    }
}

@Composable
private fun SettingsNavigationCard(
    title: String,
    description: String,
    count: Int,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        onClick = onClick
    ) {
        Row(
            Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.SemiBold)
                Text(description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text(
                count.toString(),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(horizontal = 10.dp)
            )
            Icon(Icons.Default.ChevronRight, null)
        }
    }
}

@Composable
private fun ServerRolesPage(
    roles: List<ServerRoleDefinition>,
    onCreate: () -> Unit,
    onEdit: (ServerRoleDefinition) -> Unit,
    onDelete: (ServerRoleDefinition) -> Unit,
    onMove: (ServerRoleDefinition, Int) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            FilledTonalButton(onClick = onCreate) {
                Icon(Icons.Default.Add, null)
                Spacer(Modifier.width(6.dp))
                Text("Yeni rol")
            }
        }
        items(roles, key = { it.id }) { role ->
            Surface(
                Modifier
                    .fillMaxWidth()
                    .longPressReorder(role.id) { onMove(role, it) },
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surfaceVariant
            ) {
                Row(Modifier.padding(12.dp), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(role.name, fontWeight = FontWeight.SemiBold)
                        Text(
                            role.permissions.joinToString(", ").ifBlank { "İzin yok" },
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text("Basılı tutup yukarı/aşağı taşı", style = MaterialTheme.typography.labelSmall)
                    }
                    IconButton(onClick = { onEdit(role) }) { Icon(Icons.Default.Edit, "Düzenle") }
                    IconButton(onClick = { onDelete(role) }) { Icon(Icons.Default.Delete, "Sil") }
                }
            }
        }
    }
}

@Composable
private fun ServerMembersPage(
    members: List<ServerMember>,
    search: String,
    onSearchChange: (String) -> Unit,
    onManage: (ServerMember) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            OutlinedTextField(
                value = search,
                onValueChange = onSearchChange,
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                leadingIcon = { Icon(Icons.Default.Search, "Üye ara") },
                placeholder = { Text("Üye ara") }
            )
        }
        items(members, key = { it.uid }) { member ->
            Surface(
                onClick = { onManage(member) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surfaceVariant
            ) {
                Row(Modifier.padding(14.dp), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            member.displayName.ifBlank { member.username.ifBlank { member.uid } },
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            "@" + member.username.ifBlank { "kullanıcı" } + " • " + member.role,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Icon(Icons.Default.ChevronRight, "Üyeyi yönet")
                }
            }
        }
    }
}

@Composable
private fun ServerMemberDetailPage(
    member: ServerMember,
    roles: List<ServerRoleDefinition>,
    onRoleChange: (String) -> Unit,
    onKick: () -> Unit,
    onBan: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Surface(
                Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                color = MaterialTheme.colorScheme.primaryContainer
            ) {
                Column(Modifier.padding(18.dp)) {
                    Text(
                        member.displayName.ifBlank { member.username.ifBlank { member.uid } },
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    if (member.username.isNotBlank()) {
                        Text("@" + member.username, color = MaterialTheme.colorScheme.onPrimaryContainer)
                    }
                    Text("Mevcut rol: " + member.role, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
        item {
            Text("Rol seç", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        }
        item {
            RoleChoiceCard("MEMBER", member.role == "MEMBER") { onRoleChange("MEMBER") }
            Spacer(Modifier.height(6.dp))
            RoleChoiceCard("ADMIN", member.role == "ADMIN") { onRoleChange("ADMIN") }
        }
        items(roles, key = { it.id }) { role ->
            RoleChoiceCard(role.name, member.role == role.id) { onRoleChange(role.id) }
        }
        item {
            Spacer(Modifier.height(8.dp))
            OutlinedButton(onClick = onKick, modifier = Modifier.fillMaxWidth()) {
                Text("Sunucudan çıkar")
            }
        }
        item {
            Button(
                onClick = onBan,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            ) {
                Text("Banla")
            }
        }
    }
}

@Composable
private fun RoleChoiceCard(name: String, selected: Boolean, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
    ) {
        Row(Modifier.padding(14.dp), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
            Text(name, Modifier.weight(1f), fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal)
            if (selected) {
                Text("Seçili", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
            }
        }
    }
}

@Composable
private fun ServerBansPage(
    bans: List<ServerBan>,
    onUnban: (ServerBan) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (bans.isEmpty()) {
            item { Text("Aktif sunucu yasağı yok.", color = MaterialTheme.colorScheme.onSurfaceVariant) }
        }
        items(bans, key = { it.uid }) { ban ->
            Surface(
                Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surfaceVariant
            ) {
                Row(Modifier.padding(14.dp), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            ban.displayName.ifBlank { ban.username.ifBlank { ban.uid } },
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            ban.reason.ifBlank { "Sebep belirtilmedi" },
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    TextButton(onClick = { onUnban(ban) }) { Text("Yasağı kaldır") }
                }
            }
        }
    }
}

@Composable
private fun ServerCategoriesPage(
    categories: List<ServerCategory>,
    onDelete: (ServerCategory) -> Unit,
    onMove: (ServerCategory, Int) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            Text("Kategoriler", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text(
                "Sıralamak için kategoriye basılı tutup yukarı veya aşağı sürükle.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        items(categories, key = { it.id }) { category ->
            Surface(
                Modifier.fillMaxWidth().longPressReorder(category.id) { onMove(category, it) },
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surfaceVariant
            ) {
                Row(Modifier.padding(14.dp), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                    Text(category.name, Modifier.weight(1f), fontWeight = FontWeight.SemiBold)
                    IconButton(onClick = { onDelete(category) }) {
                        Icon(Icons.Default.Delete, "Sil")
                    }
                }
            }
        }
    }
}

@Composable
private fun ServerChannelsPage(
    channels: List<ServerChannel>,
    members: List<ServerMember>,
    serverId: String,
    onDelete: (ServerChannel) -> Unit,
    onMove: (ServerChannel, Int) -> Unit
) {
    var permissionChannel by remember { mutableStateOf<ServerChannel?>(null) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            Text("Kanallar", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text(
                "Kanalı taşımak için basılı tut. İzinler ayrı düzenleyicide açılır.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        items(channels, key = { it.id }) { channel ->
            Surface(
                Modifier.fillMaxWidth().longPressReorder(channel.id) { onMove(channel, it) },
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surfaceVariant
            ) {
                Row(Modifier.padding(horizontal = 10.dp, vertical = 8.dp), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("# " + channel.name, fontWeight = FontWeight.SemiBold)
                        Text(
                            channel.categoryName,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    TextButton(onClick = { permissionChannel = channel }) { Text("İzinler") }
                    IconButton(onClick = { onDelete(channel) }) {
                        Icon(Icons.Default.Delete, "Sil")
                    }
                }
            }
        }
    }

    permissionChannel?.let { channel ->
        ChannelPermissionDialog(serverId, channel, members) {
            permissionChannel = null
        }
    }
}

@Composable
private fun ServerRoleEditorPage(
    role: ServerRoleDefinition?,
    name: String,
    permissions: Set<String>,
    onNameChange: (String) -> Unit,
    onPermissionChange: (String, Boolean) -> Unit,
    onSave: () -> Unit
) {
    val available = listOf(
        "VIEW_CHANNEL" to "Kanalı görebilir",
        "SEND_MESSAGES" to "Mesaj gönderebilir",
        "MANAGE_CHANNELS" to "Kanalları yönetebilir",
        "MANAGE_MEMBERS" to "Üyeleri yönetebilir",
        "BAN_MEMBERS" to "Üyeleri banlayabilir"
    )

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            OutlinedTextField(
                value = name,
                onValueChange = { if (it.length <= 30) onNameChange(it) },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Rol adı") },
                singleLine = true
            )
        }
        item {
            Text("İzinler", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        }
        items(available) { (permission, label) ->
            Surface(
                Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant
            ) {
                Row(Modifier.padding(horizontal = 10.dp, vertical = 4.dp), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                    Checkbox(
                        checked = permission in permissions,
                        onCheckedChange = { onPermissionChange(permission, it) }
                    )
                    Text(label, Modifier.weight(1f))
                }
            }
        }
        item {
            Spacer(Modifier.height(8.dp))
            Button(
                onClick = onSave,
                modifier = Modifier.fillMaxWidth(),
                enabled = name.trim().isNotEmpty()
            ) {
                Text(if (role == null) "Rolü oluştur" else "Değişiklikleri kaydet")
            }
        }
    }
}
