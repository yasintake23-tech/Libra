package com.libra.app.feature.admin

import android.text.format.DateFormat
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material.icons.filled.Tag
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.google.firebase.auth.FirebaseAuth
import com.libra.app.core.result.AppResult
import com.libra.app.domain.model.AdminPermissionSet
import com.libra.app.domain.model.CommunityServer
import com.libra.app.domain.model.ServerBan
import com.libra.app.domain.model.ServerCategory
import com.libra.app.domain.model.ServerChannel
import com.libra.app.domain.model.ServerMember
import com.libra.app.domain.model.UserProfile
import com.libra.app.ui.animation.appleBouncy
import com.libra.app.ui.components.FullScreenImageViewerDialog
import com.libra.app.ui.components.UserAvatar
import java.util.Date

@Composable
fun AdminServersScreen(
    vm: AdminViewModel,
    permissions: AdminPermissionSet,
    initialOwnerFilter: UserProfile? = null,
    onBack: () -> Unit,
    onOpenMemberAdmin: (UserProfile) -> Unit,
    modifier: Modifier = Modifier
) {
    val allServers by vm.servers.collectAsState()
    val serversLoading by vm.serversLoading.collectAsState()
    val busy by vm.serverActionBusy.collectAsState()
    val actionMessage by vm.serverActionMessage.collectAsState()
    val members by vm.members.collectAsState()
    val error by vm.error.collectAsState()

    val currentUid = remember { FirebaseAuth.getInstance().currentUser?.uid.orEmpty() }

    var searchQuery by remember { mutableStateOf("") }
    var ownerFilterUid by remember(initialOwnerFilter?.uid) {
        mutableStateOf(initialOwnerFilter?.uid?.takeIf { it.isNotBlank() })
    }
    var serverFilter by remember { mutableStateOf(AdminServerFilter.ALL) }
    var sortOption by remember { mutableStateOf(AdminServerSortOption.NEWEST) }
    var showSortMenu by remember { mutableStateOf(false) }

    var inspectedServerId by remember { mutableStateOf<String?>(null) }
    var deleteConfirmServer by remember { mutableStateOf<CommunityServer?>(null) }
    var editConfirmServer by remember { mutableStateOf<CommunityServer?>(null) }
    var fullScreenMediaUrl by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        vm.observeServers()
    }

    val activeOwnerProfile = remember(ownerFilterUid, members, initialOwnerFilter) {
        if (ownerFilterUid.isNullOrBlank()) null
        else members.firstOrNull { it.uid == ownerFilterUid } ?: initialOwnerFilter
    }

    val scopedServers = remember(allServers, ownerFilterUid) {
        if (ownerFilterUid.isNullOrBlank()) allServers else allServers.filter { it.ownerId == ownerFilterUid }
    }
    val filteredServers = remember(allServers, searchQuery, ownerFilterUid, serverFilter, sortOption) {
        filterAndSortAdminServers(
            servers = allServers,
            query = searchQuery,
            ownerFilterUid = ownerFilterUid,
            filter = serverFilter,
            sortOption = sortOption
        )
    }

    val mediaServerCount = scopedServers.count { it.avatarUrl.isNotBlank() || it.bannerUrl.isNotBlank() }
    val inviteServerCount = scopedServers.count { it.inviteKey.isNotBlank() }

    val inspectedServer = inspectedServerId?.let { id -> allServers.firstOrNull { it.id == id } }

    if (inspectedServer != null) {
        val ownerProfile = members.firstOrNull { it.uid == inspectedServer.ownerId }
        AdminServerControlCenter(
            server = inspectedServer,
            ownerProfile = ownerProfile,
            members = members,
            vm = vm,
            permissions = permissions,
            currentUid = currentUid,
            onBack = { inspectedServerId = null },
            onOpenMemberAdmin = { target ->
                inspectedServerId = null
                onOpenMemberAdmin(target)
            },
            onFilterOwnerServers = { uid ->
                inspectedServerId = null
                ownerFilterUid = uid
            },
            onEditServer = { editConfirmServer = it },
            onDeleteServer = { deleteConfirmServer = it },
            onOpenFullScreenImage = { fullScreenMediaUrl = it },
            modifier = Modifier.fillMaxSize()
        )
    } else {
        Surface(
            modifier = modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item(key = "header") {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = onBack, modifier = Modifier.appleBouncy(pressedScale = 0.86f, onClick = onBack)) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, "Geri")
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Sunucu İşlemleri",
                                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold)
                            )
                            Text(
                                text = "${filteredServers.size} sunucu • $mediaServerCount görselli • $inviteServerCount davet anahtarlı",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                actionMessage?.let { msg ->
                    item(key = "action-msg") {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            color = MaterialTheme.colorScheme.primaryContainer,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.CheckCircle, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = msg,
                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.weight(1f)
                                )
                                IconButton(onClick = { vm.clearServerActionMessage() }, modifier = Modifier.size(24.dp)) {
                                    Icon(Icons.Default.Close, "Kapat", modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }
                }

                error?.let { err ->
                    item(key = "error-msg") {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            color = MaterialTheme.colorScheme.errorContainer,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.error)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Warning, null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Text(err, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onErrorContainer)
                            }
                        }
                    }
                }

                if (busy) {
                    item(key = "busy-bar") { LinearProgressIndicator(modifier = Modifier.fillMaxWidth()) }
                }

                if (activeOwnerProfile != null) {
                    item(key = "owner-filter-card") {
                        val creationDisabled = activeOwnerProfile.moderation.serverCreationDisabled
                        val messagingDisabled = activeOwnerProfile.moderation.serverMessagingDisabled
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(18.dp),
                            color = MaterialTheme.colorScheme.surface,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    UserAvatar(
                                        photoUrl = activeOwnerProfile.profileImageUrl,
                                        initials = activeOwnerProfile.displayName.take(1).uppercase(),
                                        size = 42.dp
                                    )
                                    Spacer(Modifier.width(10.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "${activeOwnerProfile.displayName.ifBlank { "Üye" }} Sunucuları",
                                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                                        )
                                        Text(
                                            text = activeOwnerProfile.handle,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    TextButton(onClick = { ownerFilterUid = null }) { Text("Tümünü Gör") }
                                }
                                if (permissions.manageBans) {
                                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text("Sunucu Oluşturma Engeli", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold))
                                            Text(
                                                if (creationDisabled) "Üye yeni sunucu kuramıyor" else "Üye sunucu kurabiliyor",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = if (creationDisabled) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        Switch(
                                            checked = creationDisabled,
                                            onCheckedChange = { vm.toggleOwnerServerCreationPermission(activeOwnerProfile.uid, it) }
                                        )
                                    }
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text("Sunucu / Genel Chat Mesaj Engeli", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold))
                                            Text(
                                                if (messagingDisabled) "Üye sunucularda yazamıyor" else "Üye sunucularda yazabiliyor",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = if (messagingDisabled) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        Switch(
                                            checked = messagingDisabled,
                                            onCheckedChange = { vm.toggleUserServerMessagingPermission(activeOwnerProfile.uid, it) }
                                        )
                                    }
                                }
                                OutlinedButton(
                                    onClick = { onOpenMemberAdmin(activeOwnerProfile) },
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(Icons.Default.Person, null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text("Üye Profilini Yönet")
                                }
                            }
                        }
                    }
                }

                item(key = "summary-strip") {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        val metrics = listOf(
                            Triple(AdminServerFilter.ALL, "Tümü", scopedServers.size),
                            Triple(AdminServerFilter.WITH_MEDIA, "Görselli", mediaServerCount),
                            Triple(AdminServerFilter.WITH_INVITE, "Davet Anahtarlı", inviteServerCount)
                        )
                        items(metrics, key = { it.first.name }) { (filter, label, count) ->
                            val selected = serverFilter == filter
                            val shape = RoundedCornerShape(14.dp)
                            Surface(
                                modifier = Modifier
                                    .clip(shape)
                                    .appleBouncy(pressedScale = 0.96f, onClick = {
                                        serverFilter = if (selected && filter != AdminServerFilter.ALL) AdminServerFilter.ALL else filter
                                    }),
                                shape = shape,
                                color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
                                border = BorderStroke(
                                    1.dp,
                                    if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
                                )
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = count.toString(),
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                        color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = label,
                                        style = MaterialTheme.typography.labelMedium,
                                        color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }

                item(key = "search-sort") {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            leadingIcon = { Icon(Icons.Default.Search, null) },
                            trailingIcon = {
                                if (searchQuery.isNotBlank()) {
                                    IconButton(onClick = { searchQuery = "" }) {
                                        Icon(Icons.Default.Close, "Temizle")
                                    }
                                }
                            },
                            placeholder = { Text("Sunucu adı, açıklama, davet kodu veya sahip ID ara…") },
                            shape = RoundedCornerShape(14.dp)
                        )
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            item {
                                Box {
                                    FilterChip(
                                        selected = sortOption != AdminServerSortOption.NEWEST,
                                        onClick = { showSortMenu = true },
                                        leadingIcon = { Icon(Icons.Default.Sort, null, modifier = Modifier.size(16.dp)) },
                                        label = { Text(sortOption.label) }
                                    )
                                    DropdownMenu(
                                        expanded = showSortMenu,
                                        onDismissRequest = { showSortMenu = false },
                                        containerColor = MaterialTheme.colorScheme.surface
                                    ) {
                                        AdminServerSortOption.entries.forEach { option ->
                                            DropdownMenuItem(
                                                text = { Text(option.label) },
                                                onClick = {
                                                    sortOption = option
                                                    showSortMenu = false
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                            if (searchQuery.isNotBlank() || serverFilter != AdminServerFilter.ALL || ownerFilterUid != null) {
                                item {
                                    FilterChip(
                                        selected = false,
                                        onClick = {
                                            searchQuery = ""
                                            serverFilter = AdminServerFilter.ALL
                                            ownerFilterUid = null
                                            sortOption = AdminServerSortOption.NEWEST
                                        },
                                        leadingIcon = { Icon(Icons.Default.Close, null, modifier = Modifier.size(16.dp)) },
                                        label = { Text("Sıfırla") }
                                    )
                                }
                            }
                        }
                    }
                }

                if (serversLoading && allServers.isEmpty()) {
                    item(key = "loading") {
                        Box(Modifier.fillMaxWidth().padding(vertical = 48.dp), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator()
                        }
                    }
                } else if (filteredServers.isEmpty()) {
                    item(key = "empty") {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(18.dp),
                            color = MaterialTheme.colorScheme.surface,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                        ) {
                            Column(
                                modifier = Modifier.fillMaxWidth().padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(Icons.Default.Groups, null, modifier = Modifier.size(34.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("Sunucu bulunamadı", fontWeight = FontWeight.Bold)
                                Text("Arama veya filtre kriterlerini değiştirerek tekrar dene.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                } else {
                    items(filteredServers, key = { it.id }) { server ->
                        val owner = members.firstOrNull { it.uid == server.ownerId }
                        AdminServerCard(
                            server = server,
                            owner = owner,
                            onOpenControl = { inspectedServerId = server.id },
                            onEdit = { editConfirmServer = server },
                            onDelete = { deleteConfirmServer = server },
                            onOpenMedia = { signedUrl -> fullScreenMediaUrl = signedUrl }
                        )
                    }
                }

                item(key = "bottom-spacer") { Spacer(Modifier.height(24.dp)) }
            }
        }
    }

    editConfirmServer?.let { target ->
        var name by remember(target.id) { mutableStateOf(target.name) }
        var description by remember(target.id) { mutableStateOf(target.description) }
        AlertDialog(
            onDismissRequest = { if (!busy) editConfirmServer = null },
            containerColor = MaterialTheme.colorScheme.surface,
            title = { Text("Sunucu Bilgilerini Düzenle", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { if (it.length <= 40) name = it },
                        label = { Text("Sunucu Adı") },
                        singleLine = true,
                        supportingText = { Text("${name.length}/40") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = description,
                        onValueChange = { if (it.length <= 160) description = it },
                        label = { Text("Açıklama") },
                        minLines = 2,
                        supportingText = { Text("${description.length}/160") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        vm.updateServerMetadataAsAdmin(target.id, name, description) { ok ->
                            if (ok) editConfirmServer = null
                        }
                    },
                    enabled = name.trim().length >= 2 && !busy
                ) { Text("Kaydet") }
            },
            dismissButton = {
                TextButton(onClick = { editConfirmServer = null }, enabled = !busy) { Text("İptal") }
            }
        )
    }

    deleteConfirmServer?.let { target ->
        AlertDialog(
            onDismissRequest = { if (!busy) deleteConfirmServer = null },
            containerColor = MaterialTheme.colorScheme.surface,
            title = { Text("Sunucuyu Sil", fontWeight = FontWeight.Bold) },
            text = {
                Text("\"${target.name}\" sunucusu, tüm kanalları, rolleri, üyeleri ve görselleri kalıcı olarak silinecek. Emin misin?")
            },
            confirmButton = {
                Button(
                    onClick = {
                        vm.deleteServerAsAdmin(target) { ok ->
                            if (ok) {
                                if (inspectedServerId == target.id) inspectedServerId = null
                                deleteConfirmServer = null
                            }
                        }
                    },
                    enabled = !busy,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text("Kalıcı Sil") }
            },
            dismissButton = {
                TextButton(onClick = { deleteConfirmServer = null }, enabled = !busy) { Text("İptal") }
            }
        )
    }

    fullScreenMediaUrl?.let { url ->
        FullScreenImageViewerDialog(
            mediaUrl = url,
            onDismiss = { fullScreenMediaUrl = null }
        )
    }
}

@Composable
private fun AdminServerCard(
    server: CommunityServer,
    owner: UserProfile?,
    onOpenControl: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onOpenMedia: (String) -> Unit
) {
    val shape = RoundedCornerShape(18.dp)
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .appleBouncy(pressedScale = 0.985f, onClick = onOpenControl),
        shape = shape,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (server.bannerUrl.isNotBlank()) {
                AdminSignedImage(
                    mediaKey = server.bannerUrl,
                    contentDescription = "Sunucu afişi",
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(110.dp),
                    onImageClick = onOpenMedia
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (server.avatarUrl.isNotBlank()) {
                    AdminSignedImage(
                        mediaKey = server.avatarUrl,
                        contentDescription = server.name,
                        shape = CircleShape,
                        modifier = Modifier.size(46.dp),
                        onImageClick = onOpenMedia
                    )
                } else {
                    Surface(
                        modifier = Modifier.size(46.dp),
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = server.name.take(1).uppercase().ifBlank { "S" },
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
                Spacer(Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = server.name.ifBlank { "İsimsiz Sunucu" },
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "Kurucu: ${owner?.displayName?.ifBlank { owner.handle } ?: server.ownerId.take(12)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                IconButton(onClick = onEdit, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.Edit, "Düzenle", modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
                }
                IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.Delete, "Sil", modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.error)
                }
            }

            if (server.description.isNotBlank()) {
                Text(
                    text = server.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (server.inviteKey.isNotBlank()) {
                        Icon(Icons.Default.Key, null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            text = "libra.sc/${server.inviteKey}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else if (server.createdAt > 0L) {
                        Text(
                            text = DateFormat.format("dd MMM yyyy", Date(server.createdAt)).toString(),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Text(
                    text = "Yönet & Kanallar",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AdminServerControlCenter(
    server: CommunityServer,
    ownerProfile: UserProfile?,
    members: List<UserProfile>,
    vm: AdminViewModel,
    permissions: AdminPermissionSet,
    currentUid: String,
    onBack: () -> Unit,
    onOpenMemberAdmin: (UserProfile) -> Unit,
    onFilterOwnerServers: (String) -> Unit,
    onEditServer: (CommunityServer) -> Unit,
    onDeleteServer: (CommunityServer) -> Unit,
    onOpenFullScreenImage: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val busy by vm.serverActionBusy.collectAsState()
    val serverMembersRes by vm.observeServerMembers(server.id).collectAsState(initial = AppResult.Success(emptyList()))
    val categoriesRes by vm.observeServerCategories(server.id).collectAsState(initial = AppResult.Success(emptyList()))
    val channelsRes by vm.observeServerChannels(server.id).collectAsState(initial = AppResult.Success(emptyList()))
    val bansRes by vm.observeServerBans(server.id).collectAsState(initial = AppResult.Success(emptyList()))

    val serverMembers = (serverMembersRes as? AppResult.Success)?.data.orEmpty()
    val categories = (categoriesRes as? AppResult.Success)?.data.orEmpty()
    val channels = (channelsRes as? AppResult.Success)?.data.orEmpty()
    val bans = (bansRes as? AppResult.Success)?.data.orEmpty()

    var activeTab by remember { mutableIntStateOf(0) }
    var selectedChannelId by remember(server.id, channels.size) {
        mutableStateOf(channels.firstOrNull()?.id.orEmpty())
    }
    LaunchedEffect(channels) {
        if (channels.isNotEmpty() && channels.none { it.id == selectedChannelId }) {
            selectedChannelId = channels.first().id
        }
    }

    val channelMessagesRes by vm.observeServerMessages(server.id, selectedChannelId)
        .collectAsState(initial = AppResult.Success(emptyList()))
    val channelMessages = (channelMessagesRes as? AppResult.Success)?.data.orEmpty().sortedByDescending { it.createdAt }

    var showNotifyDialog by remember { mutableStateOf(false) }
    var showCreateCategoryDialog by remember { mutableStateOf(false) }
    var addChannelCategory by remember { mutableStateOf<ServerCategory?>(null) }
    var banMemberTarget by remember { mutableStateOf<ServerMember?>(null) }
    var deleteCategoryTarget by remember { mutableStateOf<ServerCategory?>(null) }
    var deleteChannelTarget by remember { mutableStateOf<ServerChannel?>(null) }

    Surface(modifier = modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item(key = "top") {
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onBack, modifier = Modifier.appleBouncy(pressedScale = 0.86f, onClick = onBack)) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Geri")
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(server.name, style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold), maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(
                            text = if (server.inviteKey.isNotBlank()) "libra.sc/${server.inviteKey}" else "ID: ${server.id}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = { onEditServer(server) }) {
                        Icon(Icons.Default.Edit, "Düzenle", tint = MaterialTheme.colorScheme.primary)
                    }
                    IconButton(onClick = { onDeleteServer(server) }) {
                        Icon(Icons.Default.Delete, "Sil", tint = MaterialTheme.colorScheme.error)
                    }
                }
            }

            if (busy) {
                item(key = "busy") { LinearProgressIndicator(modifier = Modifier.fillMaxWidth()) }
            }

            // Server Summary & Media Card
            item(key = "overview") {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        if (server.bannerUrl.isNotBlank()) {
                            AdminSignedImage(
                                mediaKey = server.bannerUrl,
                                contentDescription = "Afiş",
                                modifier = Modifier.fillMaxWidth().height(130.dp),
                                onImageClick = onOpenFullScreenImage
                            )
                        }
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            if (server.avatarUrl.isNotBlank()) {
                                AdminSignedImage(
                                    mediaKey = server.avatarUrl,
                                    contentDescription = server.name,
                                    shape = CircleShape,
                                    modifier = Modifier.size(52.dp),
                                    onImageClick = onOpenFullScreenImage
                                )
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    "${serverMembers.size} üye • ${categories.size} kategori • ${channels.size} kanal • ${bans.size} yasak",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                if (server.description.isNotBlank()) {
                                    Text(server.description, style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        }

                        if (server.avatarUrl.isNotBlank() || server.bannerUrl.isNotBlank()) {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                if (server.avatarUrl.isNotBlank()) {
                                    OutlinedButton(
                                        onClick = { vm.clearServerMediaAsAdmin(server, clearAvatar = true, clearBanner = false) },
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier.weight(1f)
                                    ) { Text("Avatarı Kaldır") }
                                }
                                if (server.bannerUrl.isNotBlank()) {
                                    OutlinedButton(
                                        onClick = { vm.clearServerMediaAsAdmin(server, clearAvatar = false, clearBanner = true) },
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier.weight(1f)
                                    ) { Text("Afişi Kaldır") }
                                }
                            }
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(
                                onClick = { onFilterOwnerServers(server.ownerId) },
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.weight(1f)
                            ) { Text("Kurucunun Sunucuları") }
                            if (ownerProfile != null) {
                                OutlinedButton(
                                    onClick = { onOpenMemberAdmin(ownerProfile) },
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.weight(1f)
                                ) { Text("Kurucu Profili") }
                            }
                        }

                        if (ownerProfile != null && permissions.manageBans) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Kurucu Sunucu Oluşturma Engeli", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium))
                                    Text(
                                        if (ownerProfile.moderation.serverCreationDisabled) "Kısıtlı" else "Açık",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (ownerProfile.moderation.serverCreationDisabled) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Switch(
                                    checked = ownerProfile.moderation.serverCreationDisabled,
                                    onCheckedChange = { vm.toggleOwnerServerCreationPermission(ownerProfile.uid, it) }
                                )
                            }
                        }

                        if (permissions.sendFeedback && server.ownerId.isNotBlank()) {
                            OutlinedButton(
                                onClick = { showNotifyDialog = true },
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Default.Notifications, null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Sunucu Sahibine Bildirim Gönder")
                            }
                        }
                    }
                }
            }

            // Tabs: Kanallar | Mesajlar | Üyeler & Yasaklar
            item(key = "tabs") {
                TabRow(selectedTabIndex = activeTab, containerColor = MaterialTheme.colorScheme.surface) {
                    Tab(selected = activeTab == 0, onClick = { activeTab = 0 }, text = { Text("Kanallar (${channels.size})") })
                    Tab(selected = activeTab == 1, onClick = { activeTab = 1 }, text = { Text("Mesajlar (${channelMessages.size})") })
                    Tab(selected = activeTab == 2, onClick = { activeTab = 2 }, text = { Text("Üyeler (${serverMembers.size})") })
                }
            }

            when (activeTab) {
                0 -> {
                    item(key = "add-category-btn") {
                        OutlinedButton(
                            onClick = { showCreateCategoryDialog = true },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Add, null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Yeni Kategori Oluştur")
                        }
                    }

                    if (categories.isEmpty() && channels.isEmpty()) {
                        item(key = "no-channels") {
                            Text(
                                "Bu sunucuda henüz kategori veya kanal bulunmuyor.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    items(categories, key = { "cat-${it.id}" }) { cat ->
                        val catChannels = channels.filter { it.categoryId == cat.id }
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.surface,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                        ) {
                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Folder, null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
                                    Spacer(Modifier.width(8.dp))
                                    Text(cat.name, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                                    IconButton(onClick = { addChannelCategory = cat }, modifier = Modifier.size(30.dp)) {
                                        Icon(Icons.Default.Add, "Kanal Ekle", modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
                                    }
                                    IconButton(onClick = { deleteCategoryTarget = cat }, modifier = Modifier.size(30.dp)) {
                                        Icon(Icons.Default.Delete, "Kategoriyi Sil", modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.error)
                                    }
                                }
                                catChannels.forEach { ch ->
                                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(Icons.Default.Tag, null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Spacer(Modifier.width(6.dp))
                                        Text(ch.name, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                                        TextButton(onClick = {
                                            selectedChannelId = ch.id
                                            activeTab = 1
                                        }) { Text("Mesajlar") }
                                        IconButton(onClick = { deleteChannelTarget = ch }, modifier = Modifier.size(28.dp)) {
                                            Icon(Icons.Default.Delete, "Kanalı Sil", modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.error)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    val uncategorized = channels.filter { ch -> categories.none { it.id == ch.categoryId } }
                    if (uncategorized.isNotEmpty()) {
                        items(uncategorized, key = { "uncat-${it.id}" }) { ch ->
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(14.dp),
                                color = MaterialTheme.colorScheme.surface,
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.Tag, null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text(ch.name, modifier = Modifier.weight(1f))
                                    TextButton(onClick = {
                                        selectedChannelId = ch.id
                                        activeTab = 1
                                    }) { Text("Mesajlar") }
                                    IconButton(onClick = { deleteChannelTarget = ch }, modifier = Modifier.size(28.dp)) {
                                        Icon(Icons.Default.Delete, "Kanalı Sil", modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.error)
                                    }
                                }
                            }
                        }
                    }
                }

                1 -> {
                    if (channels.isNotEmpty()) {
                        item(key = "channel-selector") {
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                channels.forEach { ch ->
                                    FilterChip(
                                        selected = selectedChannelId == ch.id,
                                        onClick = { selectedChannelId = ch.id },
                                        label = { Text("#${ch.name}") }
                                    )
                                }
                            }
                        }
                    }

                    if (channelMessages.isEmpty()) {
                        item(key = "no-msgs") {
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                color = MaterialTheme.colorScheme.surface,
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                            ) {
                                Text(
                                    "Bu kanalda denetlenecek mesaj yok.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(16.dp)
                                )
                            }
                        }
                    } else {
                        items(channelMessages, key = { it.id }) { msg ->
                            val senderMember = members.firstOrNull { it.uid == msg.senderId }
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                color = MaterialTheme.colorScheme.surface,
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                            ) {
                                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        UserAvatar(msg.senderPhotoUrl, msg.senderName.take(1).uppercase(), size = 32.dp)
                                        Spacer(Modifier.width(8.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(msg.senderName.ifBlank { "Üye" }, style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold))
                                            Text(
                                                DateFormat.format("dd MMM HH:mm", Date(msg.createdAt)).toString(),
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        if (senderMember != null) {
                                            IconButton(onClick = { onOpenMemberAdmin(senderMember) }, modifier = Modifier.size(30.dp)) {
                                                Icon(Icons.Default.Person, "Üye Yönetimi", modifier = Modifier.size(16.dp))
                                            }
                                        }
                                        IconButton(
                                            onClick = { vm.deleteServerMessageAsAdmin(server.id, msg.id, selectedChannelId) },
                                            modifier = Modifier.size(30.dp)
                                        ) {
                                            Icon(Icons.Default.Delete, "Mesajı Sil", modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.error)
                                        }
                                    }
                                    if (msg.mediaUrl.isNotBlank()) {
                                        AdminSignedImage(
                                            mediaKey = msg.mediaUrl,
                                            contentDescription = "Sunucu mesajı görseli",
                                            modifier = Modifier.fillMaxWidth().height(150.dp),
                                            onImageClick = onOpenFullScreenImage
                                        )
                                    }
                                    if (msg.text.isNotBlank()) {
                                        Text(msg.text, style = MaterialTheme.typography.bodyMedium)
                                    }
                                }
                            }
                        }
                    }
                }

                else -> {
                    items(serverMembers, key = { "member-${it.uid}" }) { member ->
                        val userProfile = members.firstOrNull { it.uid == member.uid }
                        val isOwner = member.uid == server.ownerId
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.surface,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                UserAvatar(member.photoUrl, member.displayName.take(1).uppercase(), size = 36.dp)
                                Spacer(Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(member.displayName.ifBlank { "Üye" }, fontWeight = FontWeight.Bold)
                                    Text(
                                        text = "${if (member.username.isNotBlank()) "@${member.username} • " else ""}${member.role}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                if (userProfile != null) {
                                    IconButton(onClick = { onOpenMemberAdmin(userProfile) }, modifier = Modifier.size(30.dp)) {
                                        Icon(Icons.Default.Person, "Üye Yönetimi", modifier = Modifier.size(16.dp))
                                    }
                                }
                                if (!isOwner) {
                                    TextButton(onClick = {
                                        val nextRole = if (member.role == "ADMIN") "MEMBER" else "ADMIN"
                                        vm.setServerMemberRoleAsAdmin(server.id, member.uid, nextRole)
                                    }) { Text(if (member.role == "ADMIN") "Üye Yap" else "Admin Yap") }
                                    IconButton(onClick = { banMemberTarget = member }, modifier = Modifier.size(30.dp)) {
                                        Icon(Icons.Default.Block, "Yasakla", modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.error)
                                    }
                                    IconButton(onClick = { vm.removeServerMemberAsAdmin(server.id, member.uid) }, modifier = Modifier.size(30.dp)) {
                                        Icon(Icons.Default.Delete, "Çıkar", modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.error)
                                    }
                                }
                            }
                        }
                    }

                    if (bans.isNotEmpty()) {
                        item(key = "bans-header") {
                            Text("Sunucu Yasakları (${bans.size})", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
                        }
                        items(bans, key = { "ban-${it.uid}" }) { ban ->
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                color = MaterialTheme.colorScheme.errorContainer,
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.error)
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(ban.displayName.ifBlank { ban.uid }, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onErrorContainer)
                                        if (ban.reason.isNotBlank()) {
                                            Text("Sebep: ${ban.reason}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onErrorContainer)
                                        }
                                    }
                                    TextButton(onClick = { vm.unbanServerMemberAsAdmin(server.id, ban.uid) }) {
                                        Text("Yasağı Kaldır")
                                    }
                                }
                            }
                        }
                    }
                }
            }

            item(key = "bottom-spacer") { Spacer(Modifier.height(24.dp)) }
        }
    }

    if (showCreateCategoryDialog) {
        var catName by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showCreateCategoryDialog = false },
            containerColor = MaterialTheme.colorScheme.surface,
            title = { Text("Yeni Kategori", fontWeight = FontWeight.Bold) },
            text = {
                OutlinedTextField(value = catName, onValueChange = { if (it.length <= 40) catName = it }, label = { Text("Kategori Adı") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            },
            confirmButton = {
                Button(
                    onClick = {
                        vm.createServerCategoryAsAdmin(server.id, catName) { ok -> if (ok) showCreateCategoryDialog = false }
                    },
                    enabled = catName.isNotBlank() && !busy
                ) { Text("Oluştur") }
            },
            dismissButton = { TextButton(onClick = { showCreateCategoryDialog = false }) { Text("İptal") } }
        )
    }

    addChannelCategory?.let { category ->
        var chName by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { addChannelCategory = null },
            containerColor = MaterialTheme.colorScheme.surface,
            title = { Text("${category.name} • Yeni Kanal", fontWeight = FontWeight.Bold) },
            text = {
                OutlinedTextField(value = chName, onValueChange = { if (it.length <= 40) chName = it }, label = { Text("Kanal Adı") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            },
            confirmButton = {
                Button(
                    onClick = {
                        vm.createServerChannelAsAdmin(server.id, category.id, chName) { ok -> if (ok) addChannelCategory = null }
                    },
                    enabled = chName.isNotBlank() && !busy
                ) { Text("Oluştur") }
            },
            dismissButton = { TextButton(onClick = { addChannelCategory = null }) { Text("İptal") } }
        )
    }

    deleteCategoryTarget?.let { cat ->
        AlertDialog(
            onDismissRequest = { deleteCategoryTarget = null },
            containerColor = MaterialTheme.colorScheme.surface,
            title = { Text("Kategoriyi Sil", fontWeight = FontWeight.Bold) },
            text = { Text("\"${cat.name}\" kategorisi ve altındaki kanallar silinecek. Devam edilsin mi?") },
            confirmButton = {
                Button(
                    onClick = { vm.deleteServerCategoryAsAdmin(server.id, cat.id) { deleteCategoryTarget = null } },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text("Sil") }
            },
            dismissButton = { TextButton(onClick = { deleteCategoryTarget = null }) { Text("İptal") } }
        )
    }

    deleteChannelTarget?.let { ch ->
        AlertDialog(
            onDismissRequest = { deleteChannelTarget = null },
            containerColor = MaterialTheme.colorScheme.surface,
            title = { Text("Kanalı Sil", fontWeight = FontWeight.Bold) },
            text = { Text("\"#${ch.name}\" kanalı silinecek. Devam edilsin mi?") },
            confirmButton = {
                Button(
                    onClick = { vm.deleteServerChannelAsAdmin(server.id, ch.id) { deleteChannelTarget = null } },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text("Sil") }
            },
            dismissButton = { TextButton(onClick = { deleteChannelTarget = null }) { Text("İptal") } }
        )
    }

    banMemberTarget?.let { target ->
        var reason by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { banMemberTarget = null },
            containerColor = MaterialTheme.colorScheme.surface,
            title = { Text("Sunucudan Yasakla", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("${target.displayName} bu sunucudan yasaklanacak.")
                    OutlinedTextField(value = reason, onValueChange = { reason = it }, label = { Text("Sebep (isteğe bağlı)") }, modifier = Modifier.fillMaxWidth())
                }
            },
            confirmButton = {
                Button(
                    onClick = { vm.banServerMemberAsAdmin(server.id, target.uid, reason) { banMemberTarget = null } },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text("Yasakla") }
            },
            dismissButton = { TextButton(onClick = { banMemberTarget = null }) { Text("İptal") } }
        )
    }

    if (showNotifyDialog) {
        var notifTitle by remember { mutableStateOf("Sunucu Yönetimi Bilgilendirmesi") }
        var notifBody by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showNotifyDialog = false },
            containerColor = MaterialTheme.colorScheme.surface,
            title = { Text("Sunucu Sahibine Bildirim", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = notifTitle, onValueChange = { notifTitle = it }, label = { Text("Başlık") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = notifBody, onValueChange = { notifBody = it }, label = { Text("Mesaj") }, minLines = 3, modifier = Modifier.fillMaxWidth())
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        vm.sendServerModerationNotification(server.ownerId, currentUid, notifTitle, notifBody, server.id) { ok ->
                            if (ok) showNotifyDialog = false
                        }
                    },
                    enabled = notifBody.isNotBlank() && !busy
                ) { Text("Gönder") }
            },
            dismissButton = { TextButton(onClick = { showNotifyDialog = false }) { Text("İptal") } }
        )
    }
}
