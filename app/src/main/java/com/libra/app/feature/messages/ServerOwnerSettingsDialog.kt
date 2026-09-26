package com.libra.app.feature.messages

import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.libra.app.core.di.ServiceLocator
import com.libra.app.core.result.AppResult
import com.libra.app.domain.model.*
import kotlinx.coroutines.launch


@Composable
private fun RoleDragHandle(
    itemKey: String,
    onMove: (Int) -> Unit
) {
    var triggered by remember(itemKey) { mutableStateOf(false) }

    Text(
        text = "Basılı tut ve sürükle",
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier
            .pointerInput(itemKey) {
                detectDragGesturesAfterLongPress(
                    onDrag = { change, dragAmount ->
                        change.consume()
                        if (!triggered && kotlin.math.abs(dragAmount.y) >= 18f) {
                            triggered = true
                            onMove(if (dragAmount.y < 0) -1 else 1)
                        }
                    },
                    onDragEnd = { triggered = false },
                    onDragCancel = { triggered = false }
                )
            }
    )
}

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
    var roles by remember { mutableStateOf<List<ServerRoleDefinition>>(emptyList()) }
    var newRoleName by remember { mutableStateOf("") }
    var rolePermissions by remember { mutableStateOf(setOf("VIEW_CHANNEL", "SEND_MESSAGES")) }
    var roleMember by remember { mutableStateOf<ServerMember?>(null) }
    var selectedMember by remember { mutableStateOf<ServerMember?>(null) }
    var banMember by remember { mutableStateOf<ServerMember?>(null) }
    var permissionChannel by remember { mutableStateOf<ServerChannel?>(null) }
    var confirmDelete by remember { mutableStateOf(false) }
    var showRoleCreate by remember { mutableStateOf(false) }
    var editRole by remember { mutableStateOf<ServerRoleDefinition?>(null) }
    var memberSearch by remember { mutableStateOf("") }
    var bans by remember { mutableStateOf<List<ServerBan>>(emptyList()) }

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

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Sunucu ayarları") },
        text = {
            LazyColumn(
                modifier = Modifier.heightIn(max = 560.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                item { Text("ROLLER", style = MaterialTheme.typography.labelLarge) }
                item {
                    Button(onClick = { showRoleCreate = true }) { Text("Rol oluştur") }
                }
                items(roles, key = { "role-" + it.id }) { role ->
                    Row(Modifier.fillMaxWidth()) {
                        Column(Modifier.weight(1f)) {
                            Text(role.name)
                            Text(role.permissions.joinToString(", ").ifBlank { "İzin yok" }, style = MaterialTheme.typography.labelSmall)
                        }
                        RoleDragHandle(
                            itemKey = role.id,
                            onMove = { direction ->
                                scope.launch {
                                    when (val r = repo.moveServerRole(server.id, role.id, direction)) {
                                        is AppResult.Error -> onError(r.error.message)
                                        is AppResult.Success -> Unit
                                    }
                                }
                            }
                        )
                        TextButton(onClick = { editRole = role }) { Text("Düzenle") }
                        TextButton(onClick = {
                            scope.launch {
                                when (val r = repo.deleteServerRole(server.id, role.id)) {
                                    is AppResult.Error -> onError(r.error.message)
                                    is AppResult.Success -> Unit
                                }
                            }
                        }) { Text("Sil") }
                    }
                }

                item { Spacer(Modifier.height(8.dp)); Text("ÜYELER", style = MaterialTheme.typography.labelLarge) }
                item { OutlinedTextField(memberSearch, { memberSearch = it }, Modifier.fillMaxWidth(), singleLine = true, leadingIcon = { Icon(Icons.Default.Search, "Üye ara") }, placeholder = { Text("Üye ara") }) }
                items(members.filter { it.uid != server.ownerId && (memberSearch.isBlank() || it.displayName.contains(memberSearch, true) || it.username.contains(memberSearch, true)) }, key = { "member-" + it.uid }) { member ->
                    Row(Modifier.fillMaxWidth()) {
                        Column(Modifier.weight(1f)) {
                            Text(member.displayName.ifBlank { member.username.ifBlank { member.uid } })
                            Text("Rol: " + member.role, style = MaterialTheme.typography.labelSmall)
                        }
                        TextButton(onClick = { selectedMember = member }) { Text("Detay") }
                        TextButton(onClick = { roleMember = member }) { Text("Rol") }
                        TextButton(onClick = {
                            scope.launch {
                                when (val r = repo.removeServerMember(server.id, member.uid)) {
                                    is AppResult.Error -> onError(r.error.message)
                                    is AppResult.Success -> Unit
                                }
                            }
                        }) { Text("At") }
                        TextButton(onClick = { banMember = member }) { Text("Ban") }
                    }
                }

                item { Spacer(Modifier.height(8.dp)); Text("YASAKLAR", style = MaterialTheme.typography.labelLarge) }
                if (bans.isEmpty()) {
                    item { Text("Aktif sunucu yasağı yok.", style = MaterialTheme.typography.bodySmall) }
                } else {
                    items(bans, key = { "ban-" + it.uid }) { ban ->
                        Row(Modifier.fillMaxWidth()) {
                            Column(Modifier.weight(1f)) {
                                Text(ban.displayName.ifBlank { ban.username.ifBlank { ban.uid } })
                                Text(ban.reason.ifBlank { "Sebep belirtilmedi" }, style = MaterialTheme.typography.labelSmall)
                            }
                            TextButton(onClick = {
                                scope.launch {
                                    when (val r = repo.unbanServerMember(server.id, ban.uid)) {
                                        is AppResult.Error -> onError(r.error.message)
                                        is AppResult.Success -> Unit
                                    }
                                }
                            }) { Text("Yasağı kaldır") }
                        }
                    }
                }

                item { Spacer(Modifier.height(8.dp)); Text("KATEGORİLER", style = MaterialTheme.typography.labelLarge) }
                items(categories, key = { "cat-" + it.id }) { category ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .pointerInput(category.id) {
                                detectDragGesturesAfterLongPress(
                                    onDrag = { change, dragAmount ->
                                        change.consume()
                                        if (kotlin.math.abs(dragAmount.y) >= 18f) {
                                            scope.launch {
                                                when (val r = repo.moveServerCategory(server.id, category.id, if (dragAmount.y < 0) -1 else 1)) {
                                                    is AppResult.Error -> onError(r.error.message)
                                                    is AppResult.Success -> Unit
                                                }
                                            }
                                        }
                                    }
                                )
                            }
                    ) {
                        Text(category.name, Modifier.weight(1f))
                        Text("Basılı tut ve sürükle", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        TextButton(onClick = {
                            scope.launch {
                                when (val r = repo.deleteServerCategory(server.id, category.id)) {
                                    is AppResult.Error -> onError(r.error.message)
                                    is AppResult.Success -> Unit
                                }
                            }
                        }) { Text("Sil") }
                    }
                }

                item { Spacer(Modifier.height(8.dp)); Text("KANALLAR", style = MaterialTheme.typography.labelLarge) }
                items(channels, key = { "channel-" + it.id }) { channel ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .pointerInput(channel.id) {
                                detectDragGesturesAfterLongPress(
                                    onDrag = { change, dragAmount ->
                                        change.consume()
                                        if (kotlin.math.abs(dragAmount.y) >= 18f) {
                                            scope.launch {
                                                when (val r = repo.moveServerChannel(server.id, channel.id, if (dragAmount.y < 0) -1 else 1)) {
                                                    is AppResult.Error -> onError(r.error.message)
                                                    is AppResult.Success -> Unit
                                                }
                                            }
                                        }
                                    }
                                )
                            }
                    ) {
                        Text("# " + channel.name, Modifier.weight(1f))
                        Text("Basılı tut ve sürükle", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        TextButton(onClick = { permissionChannel = channel }) { Text("İzin") }
                        TextButton(onClick = {
                            scope.launch {
                                when (val r = repo.deleteServerChannel(server.id, channel.id)) {
                                    is AppResult.Error -> onError(r.error.message)
                                    is AppResult.Success -> Unit
                                }
                            }
                        }) { Text("Sil") }
                    }
                }

                item {
                    Spacer(Modifier.height(12.dp))
                    Button(
                        onClick = { confirmDelete = true },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    ) { Text("Sunucuyu tamamen sil") }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Kapat") } }
    )

    if (showRoleCreate) {
        AlertDialog(
            onDismissRequest = { showRoleCreate = false },
            title = { Text("Rol oluştur") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    OutlinedTextField(newRoleName, { if (it.length <= 30) newRoleName = it }, label = { Text("Rol adı") }, singleLine = true)
                    listOf(
                        "VIEW_CHANNEL" to "Kanalı görebilir",
                        "SEND_MESSAGES" to "Mesaj gönderebilir",
                        "MANAGE_CHANNELS" to "Kanalları yönetebilir",
                        "MANAGE_MEMBERS" to "Üyeleri yönetebilir",
                        "BAN_MEMBERS" to "Üyeleri banlayabilir"
                    ).forEach { pair ->
                        Row {
                            Checkbox(pair.first in rolePermissions, { checked ->
                                rolePermissions = rolePermissions.toMutableSet().apply {
                                    if (checked) add(pair.first) else remove(pair.first)
                                }
                            })
                            Text(pair.second)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(enabled = newRoleName.trim().isNotEmpty(), onClick = {
                    scope.launch {
                        when (val r = repo.createServerRole(server.id, newRoleName, rolePermissions.toList())) {
                            is AppResult.Error -> onError(r.error.message)
                            is AppResult.Success -> {
                                newRoleName = ""
                                rolePermissions = setOf("VIEW_CHANNEL", "SEND_MESSAGES")
                                showRoleCreate = false
                            }
                        }
                    }
                }) { Text("Oluştur") }
            },
            dismissButton = { TextButton(onClick = { showRoleCreate = false }) { Text("İptal") } }
        )
    }

    editRole?.let { role ->
        var roleName by remember(role.id) { mutableStateOf(role.name) }
        var permissions by remember(role.id) { mutableStateOf(role.permissions.toSet()) }
        AlertDialog(
            onDismissRequest = { editRole = null },
            title = { Text("Rolü düzenle") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    OutlinedTextField(value = roleName, onValueChange = { if (it.length <= 30) roleName = it }, label = { Text("Rol adı") }, singleLine = true)
                    listOf("VIEW_CHANNEL" to "Kanalı görebilir", "SEND_MESSAGES" to "Mesaj gönderebilir", "MANAGE_CHANNELS" to "Kanalları yönetebilir", "MANAGE_MEMBERS" to "Üyeleri yönetebilir", "BAN_MEMBERS" to "Üyeleri banlayabilir").forEach { (permission, label) ->
                        Row(Modifier.fillMaxWidth()) {
                            Checkbox(checked = permission in permissions, onCheckedChange = { checked -> permissions = permissions.toMutableSet().apply { if (checked) add(permission) else remove(permission) } })
                            Text(label)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(enabled = roleName.trim().isNotEmpty(), onClick = {
                    scope.launch {
                        when (val r = repo.updateServerRole(server.id, role.id, roleName.trim(), permissions.toList())) {
                            is AppResult.Error -> onError(r.error.message)
                            is AppResult.Success -> editRole = null
                        }
                    }
                }) { Text("Kaydet") }
            },
            dismissButton = { TextButton(onClick = { editRole = null }) { Text("İptal") } }
        )
    }

    selectedMember?.let { member ->
        AlertDialog(
            onDismissRequest = { selectedMember = null },
            title = { Text("Üye detayları") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(member.displayName.ifBlank { member.username.ifBlank { member.uid } }, style = MaterialTheme.typography.titleMedium)
                    if (member.username.isNotBlank()) Text("@${member.username}", style = MaterialTheme.typography.bodyMedium)
                    Text("Rol: ${member.role}", style = MaterialTheme.typography.bodyMedium)
                    if (member.joinedAt > 0L) {
                        Text(
                            "Üyelik: ${java.text.SimpleDateFormat("dd.MM.yyyy HH:mm", java.util.Locale.getDefault()).format(java.util.Date(member.joinedAt))}",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    Text("UID: ${member.uid}", style = MaterialTheme.typography.labelSmall)
                }
            },
            confirmButton = {
                TextButton(onClick = { roleMember = member; selectedMember = null }) { Text("Rolü değiştir") }
            },
            dismissButton = { TextButton(onClick = { selectedMember = null }) { Text("Kapat") } }
        )
    }

    roleMember?.let { member ->
        AlertDialog(
            onDismissRequest = { roleMember = null },
            title = { Text("Rol ver") },
            text = {
                LazyColumn {
                    item {
                        TextButton(onClick = {
                            scope.launch { repo.setServerMemberRole(server.id, member.uid, "MEMBER") }
                            roleMember = null
                        }) { Text("MEMBER") }
                    }
                    item {
                        TextButton(onClick = {
                            scope.launch { repo.setServerMemberRole(server.id, member.uid, "ADMIN") }
                            roleMember = null
                        }) { Text("ADMIN") }
                    }
                    items(roles) { role ->
                        TextButton(onClick = {
                            scope.launch { repo.setServerMemberRole(server.id, member.uid, role.id) }
                            roleMember = null
                        }) { Text(role.name) }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { roleMember = null }) { Text("Kapat") } }
        )
    }

    banMember?.let { member ->
        AlertDialog(
            onDismissRequest = { banMember = null },
            title = { Text("Üyeyi banla") },
            text = { Text(member.displayName + " bu sunucudan çıkarılacak ve tekrar katılamayacak.") },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch {
                        when (val r = repo.banServerMember(server.id, member.uid, "")) {
                            is AppResult.Error -> onError(r.error.message)
                            is AppResult.Success -> banMember = null
                        }
                    }
                }) { Text("Banla") }
            },
            dismissButton = { TextButton(onClick = { banMember = null }) { Text("İptal") } }
        )
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Sunucuyu sil") },
            text = { Text("Sunucu, üyeler, roller, kategoriler, kanallar ve ban kayıtları kalıcı olarak silinecek.") },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch {
                        when (val r = repo.deleteCommunityServer(server.id)) {
                            is AppResult.Error -> onError(r.error.message)
                            is AppResult.Success -> { confirmDelete = false; onDeleted() }
                        }
                    }
                }) { Text("Kalıcı olarak sil") }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("İptal") } }
        )
    }

    permissionChannel?.let { channel ->
        ChannelPermissionDialog(server.id, channel, members) { permissionChannel = null }
    }
}
