package com.libra.app.feature.messages

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Tag
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.libra.app.core.di.ServiceLocator
import com.libra.app.core.result.AppResult
import com.libra.app.domain.model.CommunityServer
import com.libra.app.domain.model.ServerBan
import com.libra.app.domain.model.ServerCategory
import com.libra.app.domain.model.ServerChannel
import com.libra.app.domain.model.ServerMember
import com.libra.app.domain.model.ServerRoleDefinition
import com.libra.app.ui.animation.appleBouncy
import com.libra.app.ui.components.UserAvatar
import com.libra.app.ui.theme.LocalLibraDarkTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

private val LibraLavender = Color(0xFF2554C7)
private val LibraLavenderSurface = Color(0xFFEAF0FE)
private val LibraLavenderDarkSurface = Color(0xFF19243F)
private val LibraLavenderText = Color(0xFF1E44A6)
private val LibraLavenderDarkText = Color(0xFF9BB8FF)

private enum class ServerSettingsPage(val title: String, val description: String) {
    HOME("Sunucu Stüdyosu", "Kimlik, üyeler, roller ve kanallar"),
    PROFILE("Profil & Görseller", "Ad, açıklama, simge ve kapak görseli"),
    ROLES("Roller & İzinler", "Hiyerarşi ve yetkiler"),
    MEMBERS("Üyeler", "Üyeleri ve rollerini yönet"),
    BANS("Yasaklar", "Sunucudan uzaklaştırılan kullanıcılar"),
    CATEGORIES("Kategoriler", "Kanal gruplarını düzenle ve sırala"),
    CHANNELS("Kanallar", "Kanal sırası, düzenleme ve izinler"),
    MEMBER_DETAIL("Üye Yönetimi", "Rol atama ve moderasyon"),
    ROLE_EDITOR("Rol Düzenleyici", "Rol adı ve yetki matrisi")
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
    val darkTheme = LocalLibraDarkTheme.current
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
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

    // Live profile draft fields
    var nameDraft by remember(server.id, server.name) { mutableStateOf(server.name) }
    var descDraft by remember(server.id, server.description) { mutableStateOf(server.description) }
    var avatarKeyDraft by remember(server.id, server.avatarUrl) { mutableStateOf(server.avatarUrl) }
    var bannerKeyDraft by remember(server.id, server.bannerUrl) { mutableStateOf(server.bannerUrl) }
    var inviteKey by remember(server.id, server.inviteKey) { mutableStateOf(server.inviteKey) }
    var savingProfile by remember { mutableStateOf(false) }
    var uploadingAvatar by remember { mutableStateOf(false) }
    var uploadingBanner by remember { mutableStateOf(false) }
    var uploadPct by remember { mutableIntStateOf(0) }

    // Quick create/edit dialogs inside Categories/Channels pages
    var showAddCategoryDialog by remember { mutableStateOf(false) }
    var addChannelCategory by remember { mutableStateOf<ServerCategory?>(null) }

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

    LaunchedEffect(server.id) {
        if (inviteKey.isBlank()) {
            when (val res = repo.ensureCommunityServerInviteKey(server.id)) {
                is AppResult.Success -> inviteKey = res.data
                is AppResult.Error -> Unit
            }
        }
    }

    fun uploadServerImage(uri: Uri, isBanner: Boolean) {
        if (isBanner) uploadingBanner = true else uploadingAvatar = true
        uploadPct = 0
        actionError = null
        scope.launch {
            try {
                val prepared = prepareChatImageForUpload(
                    context = context,
                    uri = uri,
                    prefix = if (isBanner) "srv_banner" else "srv_avatar"
                )
                val upRes = if (isBanner) {
                    repo.uploadServerBanner(
                        serverId = server.id,
                        fileName = prepared.fileName,
                        bytes = prepared.bytes,
                        contentType = prepared.contentType,
                        onProgress = { pct -> scope.launch(Dispatchers.Main) { uploadPct = pct } }
                    )
                } else {
                    repo.uploadServerAvatar(
                        serverId = server.id,
                        fileName = prepared.fileName,
                        bytes = prepared.bytes,
                        contentType = prepared.contentType,
                        onProgress = { pct -> scope.launch(Dispatchers.Main) { uploadPct = pct } }
                    )
                }
                when (upRes) {
                    is AppResult.Error -> actionError = upRes.error.message
                    is AppResult.Success -> {
                        val oldKey = if (isBanner) bannerKeyDraft else avatarKeyDraft
                        val nextAvatar = if (isBanner) avatarKeyDraft else upRes.data
                        val nextBanner = if (isBanner) upRes.data else bannerKeyDraft
                        when (val saveRes = repo.updateCommunityServerMedia(server.id, nextAvatar, nextBanner)) {
                            is AppResult.Success -> {
                                avatarKeyDraft = nextAvatar
                                bannerKeyDraft = nextBanner
                                if (oldKey.isNotBlank() && oldKey != upRes.data) {
                                    runCatching { repo.deleteServerMedia(server.id, oldKey) }
                                }
                                Toast.makeText(
                                    context,
                                    if (isBanner) "Sunucu kapağı güncellendi." else "Sunucu simgesi güncellendi.",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                            is AppResult.Error -> actionError = saveRes.error.message
                        }
                    }
                }
            } catch (e: Exception) {
                actionError = e.localizedMessage ?: "Görsel yüklenemedi."
            } finally {
                if (isBanner) uploadingBanner = false else uploadingAvatar = false
            }
        }
    }

    val avatarPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        uri?.let { uploadServerImage(it, isBanner = false) }
    }
    val bannerPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        uri?.let { uploadServerImage(it, isBanner = true) }
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
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .navigationBarsPadding()
            ) {
                // Editorial Studio Top Bar
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.surface
                ) {
                    Column {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .statusBarsPadding()
                                .padding(horizontal = 8.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = { back() },
                                modifier = Modifier.appleBouncy(pressedScale = 0.86f, onClick = { back() })
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = if (page == ServerSettingsPage.HOME) "Kapat" else "Geri"
                                )
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = page.title,
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold)
                                )
                                Text(
                                    text = page.description,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            if (page != ServerSettingsPage.HOME) {
                                TextButton(onClick = { goTo(ServerSettingsPage.HOME) }) {
                                    Text(
                                        text = "Özet",
                                        fontWeight = FontWeight.Bold,
                                        color = if (darkTheme) LibraLavenderDarkText else LibraLavenderText
                                    )
                                }
                            }
                        }
                        HorizontalDivider(color = MaterialTheme.colorScheme.outline)
                    }
                }

                actionError?.let { err ->
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.errorContainer
                    ) {
                        Text(
                            text = err,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }

                when (page) {
                    ServerSettingsPage.HOME -> ServerSettingsHome(
                        server = server.copy(
                            name = nameDraft,
                            description = descDraft,
                            avatarUrl = avatarKeyDraft,
                            bannerUrl = bannerKeyDraft
                        ),
                        rolesCount = roles.size,
                        membersCount = members.size,
                        bansCount = bans.size,
                        categoriesCount = categories.size,
                        channelsCount = channels.size,
                        onProfile = { goTo(ServerSettingsPage.PROFILE) },
                        onRoles = { goTo(ServerSettingsPage.ROLES) },
                        onMembers = { goTo(ServerSettingsPage.MEMBERS) },
                        onBans = { goTo(ServerSettingsPage.BANS) },
                        onCategories = { goTo(ServerSettingsPage.CATEGORIES) },
                        onChannels = { goTo(ServerSettingsPage.CHANNELS) },
                        onDelete = { confirmDelete = true }
                    )

                    ServerSettingsPage.PROFILE -> ServerProfileEditorPage(
                        serverId = server.id,
                        name = nameDraft,
                        description = descDraft,
                        avatarKey = avatarKeyDraft,
                        bannerKey = bannerKeyDraft,
                        inviteKey = inviteKey,
                        savingProfile = savingProfile,
                        uploadingAvatar = uploadingAvatar,
                        uploadingBanner = uploadingBanner,
                        uploadPct = uploadPct,
                        onNameChange = { if (it.length <= 40) nameDraft = it },
                        onDescriptionChange = { if (it.length <= 160) descDraft = it },
                        onPickAvatar = {
                            avatarPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                        },
                        onPickBanner = {
                            bannerPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                        },
                        onRemoveAvatar = {
                            if (avatarKeyDraft.isBlank()) return@ServerProfileEditorPage
                            val old = avatarKeyDraft
                            scope.launch {
                                when (val res = repo.updateCommunityServerMedia(server.id, "", bannerKeyDraft)) {
                                    is AppResult.Success -> {
                                        avatarKeyDraft = ""
                                        runCatching { repo.deleteServerMedia(server.id, old) }
                                        Toast.makeText(context, "Sunucu simgesi kaldırıldı.", Toast.LENGTH_SHORT).show()
                                    }
                                    is AppResult.Error -> actionError = res.error.message
                                }
                            }
                        },
                        onRemoveBanner = {
                            if (bannerKeyDraft.isBlank()) return@ServerProfileEditorPage
                            val old = bannerKeyDraft
                            scope.launch {
                                when (val res = repo.updateCommunityServerMedia(server.id, avatarKeyDraft, "")) {
                                    is AppResult.Success -> {
                                        bannerKeyDraft = ""
                                        runCatching { repo.deleteServerMedia(server.id, old) }
                                        Toast.makeText(context, "Sunucu kapağı kaldırıldı.", Toast.LENGTH_SHORT).show()
                                    }
                                    is AppResult.Error -> actionError = res.error.message
                                }
                            }
                        },
                        onSaveInfo = {
                            savingProfile = true
                            actionError = null
                            scope.launch {
                                when (val res = repo.updateCommunityServer(server.id, nameDraft, descDraft)) {
                                    is AppResult.Success -> {
                                        Toast.makeText(context, "Sunucu bilgileri kaydedildi.", Toast.LENGTH_SHORT).show()
                                    }
                                    is AppResult.Error -> actionError = res.error.message
                                }
                                savingProfile = false
                            }
                        },
                        onCopyInvite = {
                            val link = "libra.sc/$inviteKey"
                            clipboard.setText(AnnotatedString(link))
                            Toast.makeText(context, "Davet anahtarı kopyalandı: $link", Toast.LENGTH_SHORT).show()
                        },
                        onShareInvite = {
                            val link = "libra.sc/$inviteKey"
                            val intent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_TEXT, "${nameDraft.ifBlank { server.name }} sunucusuna katıl: $link")
                            }
                            context.startActivity(Intent.createChooser(intent, "Davet Anahtarını Paylaş"))
                        }
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
                            roles = roles,
                            search = memberSearch,
                            onSearchChange = { memberSearch = it },
                            onManage = {
                                selectedMember = it
                                goTo(ServerSettingsPage.MEMBER_DETAIL)
                            }
                        )
                    }

                    ServerSettingsPage.MEMBER_DETAIL -> {
                        val member = selectedMember?.let { live ->
                            members.firstOrNull { it.uid == live.uid } ?: live
                        }
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
                                                selectedMember = member.copy(role = role)
                                                Toast.makeText(context, "Rol güncellendi.", Toast.LENGTH_SHORT).show()
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
                                onBan = { reason ->
                                    scope.launch {
                                        when (val result = repo.banServerMember(server.id, member.uid, reason)) {
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
                        onAddCategory = { showAddCategoryDialog = true },
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
                        categories = categories,
                        members = members,
                        roles = roles,
                        serverId = server.id,
                        onAddChannel = { cat -> addChannelCategory = cat },
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

    if (showAddCategoryDialog) {
        var catName by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showAddCategoryDialog = false },
            containerColor = MaterialTheme.colorScheme.surface,
            title = { Text("Yeni Kategori", fontWeight = FontWeight.Bold) },
            text = {
                OutlinedTextField(
                    value = catName,
                    onValueChange = { if (it.length <= 40) catName = it },
                    label = { Text("Kategori adı") },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp)
                )
            },
            confirmButton = {
                Button(
                    enabled = catName.trim().isNotEmpty(),
                    colors = ButtonDefaults.buttonColors(containerColor = LibraLavender),
                    onClick = {
                        scope.launch {
                            when (val res = repo.createServerCategory(server.id, catName.trim())) {
                                is AppResult.Success -> showAddCategoryDialog = false
                                is AppResult.Error -> actionError = res.error.message
                            }
                        }
                    }
                ) { Text("Oluştur", color = Color.White) }
            },
            dismissButton = { TextButton(onClick = { showAddCategoryDialog = false }) { Text("Vazgeç") } }
        )
    }

    addChannelCategory?.let { cat ->
        var chName by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { addChannelCategory = null },
            containerColor = MaterialTheme.colorScheme.surface,
            title = { Text("${cat.name} • Yeni Kanal", fontWeight = FontWeight.Bold) },
            text = {
                OutlinedTextField(
                    value = chName,
                    onValueChange = { if (it.length <= 40) chName = it },
                    label = { Text("# kanal adı") },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp)
                )
            },
            confirmButton = {
                Button(
                    enabled = chName.trim().isNotEmpty(),
                    colors = ButtonDefaults.buttonColors(containerColor = LibraLavender),
                    onClick = {
                        scope.launch {
                            when (val res = repo.createServerChannel(server.id, cat.id, chName.trim())) {
                                is AppResult.Success -> addChannelCategory = null
                                is AppResult.Error -> actionError = res.error.message
                            }
                        }
                    }
                ) { Text("Oluştur", color = Color.White) }
            },
            dismissButton = { TextButton(onClick = { addChannelCategory = null }) { Text("Vazgeç") } }
        )
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            containerColor = MaterialTheme.colorScheme.surface,
            title = { Text("Sunucuyu Kalıcı Sil", fontWeight = FontWeight.Bold) },
            text = { Text("Sunucu, üyeler, roller, kategoriler, kanallar ve ban kayıtları kalıcı olarak silinecek.") },
            confirmButton = {
                Button(
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    onClick = {
                        scope.launch {
                            when (val result = repo.deleteCommunityServer(server.id)) {
                                is AppResult.Error -> {
                                    actionError = result.error.message
                                    onError(result.error.message)
                                }
                                is AppResult.Success -> {
                                    confirmDelete = false
                                    onDeleted()
                                }
                            }
                        }
                    }
                ) { Text("Kalıcı Olarak Sil") }
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
    onProfile: () -> Unit,
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
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(22.dp))
                    .clickable(onClick = onProfile),
                shape = RoundedCornerShape(22.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                Column {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(104.dp)
                    ) {
                        LibraArchitecturalBanner(
                            seedKey = server.id.ifBlank { server.name },
                            bannerMediaKey = server.bannerUrl,
                            modifier = Modifier.fillMaxSize()
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.verticalGradient(
                                        listOf(Color.Transparent, Color.Black.copy(alpha = 0.55f))
                                    )
                                )
                        )
                        Row(
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            LibraSquircleAvatar(
                                photoUrl = server.avatarUrl,
                                title = server.name,
                                size = 50.dp,
                                cornerRadius = 16.dp,
                                borderColor = Color.White.copy(alpha = 0.85f),
                                borderWidth = 2.dp
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = server.name,
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                                    color = Color.White,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = server.description.ifBlank { "Profil, simge ve kapağı düzenlemek için dokun" },
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White.copy(alpha = 0.85f),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Surface(
                                shape = CircleShape,
                                color = Color.White.copy(alpha = 0.22f)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = "Düzenle",
                                    tint = Color.White,
                                    modifier = Modifier.padding(8.dp).size(16.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        item {
            SettingsNavigationCard(
                icon = Icons.Default.Palette,
                title = "Profil & Görseller",
                description = "Sunucu adı, açıklama, avatar, kapak ve davet anahtarı",
                badgeText = "Düzenle",
                onClick = onProfile
            )
        }
        item {
            SettingsNavigationCard(
                icon = Icons.Default.AdminPanelSettings,
                title = "Roller & İzinler",
                description = "Özel roller oluştur, sırala ve yetkileri belirle",
                badgeText = rolesCount.toString(),
                onClick = onRoles
            )
        }
        item {
            SettingsNavigationCard(
                icon = Icons.Default.Groups,
                title = "Üyeler",
                description = "Üye rolleri, çıkarma ve moderasyon",
                badgeText = membersCount.toString(),
                onClick = onMembers
            )
        }
        item {
            SettingsNavigationCard(
                icon = Icons.Default.FolderOpen,
                title = "Kategoriler",
                description = "Kanal gruplarını ekle, sırala veya kaldır",
                badgeText = categoriesCount.toString(),
                onClick = onCategories
            )
        }
        item {
            SettingsNavigationCard(
                icon = Icons.Default.Tag,
                title = "Kanallar",
                description = "Kanal sırası, silme ve rol/kişi bazlı izinler",
                badgeText = channelsCount.toString(),
                onClick = onChannels
            )
        }
        item {
            SettingsNavigationCard(
                icon = Icons.Default.Block,
                title = "Yasaklar",
                description = "Sunucudan banlanan üyeleri görüntüle ve kaldır",
                badgeText = bansCount.toString(),
                onClick = onBans
            )
        }

        item {
            Spacer(Modifier.height(10.dp))
            Button(
                onClick = onDelete,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            ) {
                Icon(Icons.Default.Delete, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Sunucuyu Kalıcı Olarak Sil", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun ServerProfileEditorPage(
    serverId: String,
    name: String,
    description: String,
    avatarKey: String,
    bannerKey: String,
    inviteKey: String,
    savingProfile: Boolean,
    uploadingAvatar: Boolean,
    uploadingBanner: Boolean,
    uploadPct: Int,
    onNameChange: (String) -> Unit,
    onDescriptionChange: (String) -> Unit,
    onPickAvatar: () -> Unit,
    onPickBanner: () -> Unit,
    onRemoveAvatar: () -> Unit,
    onRemoveBanner: () -> Unit,
    onSaveInfo: () -> Unit,
    onCopyInvite: () -> Unit,
    onShareInvite: () -> Unit
) {
    val darkTheme = LocalLibraDarkTheme.current
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            // Live Banner + Floating Avatar Preview Studio
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(166.dp)
            ) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(126.dp)
                        .clip(RoundedCornerShape(22.dp))
                        .clickable(enabled = !uploadingBanner, onClick = onPickBanner),
                    shape = RoundedCornerShape(22.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        LibraArchitecturalBanner(
                            seedKey = serverId.ifBlank { name },
                            bannerMediaKey = bannerKey,
                            modifier = Modifier.fillMaxSize()
                        )
                        Row(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(10.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = Color.Black.copy(alpha = 0.62f),
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .clickable(enabled = !uploadingBanner, onClick = onPickBanner)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                                ) {
                                    if (uploadingBanner) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(12.dp),
                                            strokeWidth = 1.8.dp,
                                            color = Color.White
                                        )
                                    } else {
                                        Icon(Icons.Default.Image, null, tint = Color.White, modifier = Modifier.size(14.dp))
                                    }
                                    Text(
                                        text = if (uploadingBanner) "%$uploadPct" else "Kapak Değiştir",
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        color = Color.White
                                    )
                                }
                            }
                            if (bannerKey.isNotBlank() && !uploadingBanner) {
                                Surface(
                                    shape = CircleShape,
                                    color = Color.Black.copy(alpha = 0.62f),
                                    modifier = Modifier
                                        .size(30.dp)
                                        .clip(CircleShape)
                                        .clickable(onClick = onRemoveBanner)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(Icons.Default.DeleteOutline, "Kapağı kaldır", tint = Color.White, modifier = Modifier.size(15.dp))
                                    }
                                }
                            }
                        }
                    }
                }

                Box(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(start = 16.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        LibraSquircleAvatar(
                            photoUrl = avatarKey,
                            title = name.ifBlank { "Libra" },
                            size = 76.dp,
                            cornerRadius = 22.dp,
                            borderColor = MaterialTheme.colorScheme.background,
                            borderWidth = 3.dp,
                            onClick = if (!uploadingAvatar) onPickAvatar else null
                        )
                        if (uploadingAvatar) {
                            Surface(
                                shape = RoundedCornerShape(22.dp),
                                color = Color.Black.copy(alpha = 0.55f),
                                modifier = Modifier.size(76.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(22.dp),
                                        strokeWidth = 2.dp,
                                        color = Color.White
                                    )
                                }
                            }
                        }
                    }
                    Surface(
                        shape = CircleShape,
                        color = LibraLavender,
                        border = BorderStroke(2.dp, MaterialTheme.colorScheme.background),
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .size(28.dp)
                            .clip(CircleShape)
                            .clickable(enabled = !uploadingAvatar, onClick = onPickAvatar)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.AddAPhoto, "Simge seç", tint = Color.White, modifier = Modifier.size(14.dp))
                        }
                    }
                }

                if (avatarKey.isNotBlank() && !uploadingAvatar) {
                    TextButton(
                        onClick = onRemoveAvatar,
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(end = 4.dp)
                    ) {
                        Text("Simgeyi Kaldır", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
        }

        item {
            OutlinedTextField(
                value = name,
                onValueChange = onNameChange,
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                label = { Text("Sunucu Adı") }
            )
        }

        item {
            OutlinedTextField(
                value = description,
                onValueChange = onDescriptionChange,
                modifier = Modifier.fillMaxWidth(),
                minLines = 3,
                maxLines = 5,
                shape = RoundedCornerShape(16.dp),
                label = { Text("Açıklama") },
                placeholder = { Text("Topluluğunun konusunu, kültürünü ve kurallarını anlat…") }
            )
        }

        item {
            Button(
                enabled = name.trim().length >= 2 && !savingProfile,
                onClick = onSaveInfo,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = LibraLavender)
            ) {
                if (savingProfile) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = Color.White)
                } else {
                    Icon(Icons.Default.Check, null, tint = Color.White, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Sunucu Bilgilerini Kaydet", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }

        if (inviteKey.isNotBlank()) {
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Davet Anahtarı",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                        )
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            color = if (darkTheme) LibraLavenderDarkSurface else LibraLavenderSurface
                        ) {
                            Text(
                                text = "libra.sc/$inviteKey",
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.ExtraBold),
                                color = if (darkTheme) LibraLavenderDarkText else LibraLavenderText
                            )
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(
                                onClick = onCopyInvite,
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.ContentCopy, null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Kopyala")
                            }
                            Button(
                                onClick = onShareInvite,
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = LibraLavender)
                            ) {
                                Icon(Icons.Default.Share, null, tint = Color.White, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Paylaş", color = Color.White)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsNavigationCard(
    icon: ImageVector,
    title: String,
    description: String,
    badgeText: String,
    onClick: () -> Unit
) {
    val darkTheme = LocalLibraDarkTheme.current
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .appleBouncy(pressedScale = 0.98f, onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Row(
            Modifier.padding(horizontal = 14.dp, vertical = 13.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = if (darkTheme) LibraLavenderDarkSurface else LibraLavenderSurface,
                modifier = Modifier.size(40.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = if (darkTheme) LibraLavenderDarkText else LibraLavenderText,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            Column(
                Modifier
                    .weight(1f)
                    .padding(horizontal = 12.dp)
            ) {
                Text(title, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
                Text(
                    description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surfaceVariant
            ) {
                Text(
                    text = badgeText,
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp)
                )
            }
            Spacer(Modifier.width(4.dp))
            Icon(
                Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
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
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Button(
                onClick = onCreate,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = LibraLavender)
            ) {
                Icon(Icons.Default.Add, null, tint = Color.White, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("Yeni Rol Oluştur", color = Color.White, fontWeight = FontWeight.Bold)
            }
        }
        if (roles.isEmpty()) {
            item {
                Text(
                    text = "Henüz özel rol eklenmedi. Varsayılan ADMIN ve MEMBER rolleri aktif.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }
        }
        itemsIndexed(roles, key = { _, it -> it.id }) { idx, role ->
            Surface(
                Modifier
                    .fillMaxWidth()
                    .longPressReorder(role.id) { onMove(role, it) },
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                Row(
                    Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(role.name, fontWeight = FontWeight.Bold)
                        Text(
                            role.permissions.joinToString(" • ").ifBlank { "İzin tanımlanmamış" },
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    IconButton(enabled = idx > 0, onClick = { onMove(role, -1) }) {
                        Icon(Icons.Default.KeyboardArrowUp, "Yukarı taşı")
                    }
                    IconButton(enabled = idx < roles.lastIndex, onClick = { onMove(role, 1) }) {
                        Icon(Icons.Default.KeyboardArrowDown, "Aşağı taşı")
                    }
                    IconButton(onClick = { onEdit(role) }) { Icon(Icons.Default.Edit, "Düzenle") }
                    IconButton(onClick = { onDelete(role) }) {
                        Icon(Icons.Default.DeleteOutline, "Sil", tint = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }
    }
}

@Composable
private fun ServerMembersPage(
    members: List<ServerMember>,
    roles: List<ServerRoleDefinition>,
    search: String,
    onSearchChange: (String) -> Unit,
    onManage: (ServerMember) -> Unit
) {
    val roleNameMap = remember(roles) { roles.associate { it.id to it.name } }
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
                shape = RoundedCornerShape(16.dp),
                leadingIcon = { Icon(Icons.Default.Search, "Üye ara") },
                placeholder = { Text("Üye ara…") }
            )
        }
        if (members.isEmpty()) {
            item {
                Text(
                    text = "Yönetilecek başka üye bulunamadı.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 12.dp)
                )
            }
        }
        items(members, key = { it.uid }) { member ->
            val roleLabel = roleNameMap[member.role] ?: member.role
            Surface(
                onClick = { onManage(member) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                Row(
                    Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    UserAvatar(
                        photoUrl = member.photoUrl,
                        initials = member.displayName.take(1).uppercase().ifBlank { "U" },
                        size = 40.dp
                    )
                    Column(
                        Modifier
                            .weight(1f)
                            .padding(horizontal = 12.dp)
                    ) {
                        Text(
                            member.displayName.ifBlank { member.username.ifBlank { member.uid } },
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (member.username.isNotBlank()) {
                            Text(
                                "@${member.username}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    LibraRoleChip(
                        label = roleLabel,
                        highlighted = member.role == "ADMIN"
                    )
                    Spacer(Modifier.width(4.dp))
                    Icon(Icons.Default.ChevronRight, "Üyeyi yönet", tint = MaterialTheme.colorScheme.onSurfaceVariant)
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
    onBan: (String) -> Unit
) {
    var showBanDialog by remember { mutableStateOf(false) }
    var banReason by remember { mutableStateOf("") }
    var showKickConfirm by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Surface(
                Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                Row(
                    Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    UserAvatar(
                        photoUrl = member.photoUrl,
                        initials = member.displayName.take(1).uppercase().ifBlank { "U" },
                        size = 54.dp
                    )
                    Column(Modifier.weight(1f)) {
                        Text(
                            member.displayName.ifBlank { member.username.ifBlank { member.uid } },
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold)
                        )
                        if (member.username.isNotBlank()) {
                            Text("@${member.username}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Spacer(Modifier.height(4.dp))
                        LibraRoleChip(label = member.role, highlighted = member.role == "ADMIN")
                    }
                }
            }
        }
        item {
            Text("Rol Ata", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
        }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                RoleChoiceCard("MEMBER", "Standart üye", member.role == "MEMBER") { onRoleChange("MEMBER") }
                RoleChoiceCard("ADMIN", "Yönetici yetkileri", member.role == "ADMIN") { onRoleChange("ADMIN") }
            }
        }
        items(roles, key = { it.id }) { role ->
            RoleChoiceCard(
                name = role.name,
                subtitle = role.permissions.joinToString(", ").ifBlank { "Özel rol" },
                selected = member.role == role.id
            ) { onRoleChange(role.id) }
        }
        item {
            Spacer(Modifier.height(8.dp))
            OutlinedButton(
                onClick = { showKickConfirm = true },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text("Sunucudan Çıkar")
            }
        }
        item {
            Button(
                onClick = { showBanDialog = true },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            ) {
                Text("Sunucudan Banla", fontWeight = FontWeight.Bold)
            }
        }
    }

    if (showKickConfirm) {
        AlertDialog(
            onDismissRequest = { showKickConfirm = false },
            containerColor = MaterialTheme.colorScheme.surface,
            title = { Text("Üyeyi Çıkar", fontWeight = FontWeight.Bold) },
            text = { Text("${member.displayName.ifBlank { "Bu kullanıcı" }} sunucudan çıkarılacak. İsterse davet anahtarıyla tekrar katılabilir.") },
            confirmButton = {
                Button(
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    onClick = { showKickConfirm = false; onKick() }
                ) { Text("Çıkar") }
            },
            dismissButton = { TextButton(onClick = { showKickConfirm = false }) { Text("Vazgeç") } }
        )
    }

    if (showBanDialog) {
        AlertDialog(
            onDismissRequest = { showBanDialog = false },
            containerColor = MaterialTheme.colorScheme.surface,
            title = { Text("Üyeyi Banla", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("${member.displayName.ifBlank { "Bu kullanıcı" }} sunucudan kalıcı olarak engellenecek.")
                    OutlinedTextField(
                        value = banReason,
                        onValueChange = { if (it.length <= 120) banReason = it },
                        label = { Text("Sebep (isteğe bağlı)") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    onClick = { showBanDialog = false; onBan(banReason.trim()) }
                ) { Text("Banla") }
            },
            dismissButton = { TextButton(onClick = { showBanDialog = false }) { Text("Vazgeç") } }
        )
    }
}

@Composable
private fun RoleChoiceCard(name: String, subtitle: String, selected: Boolean, onClick: () -> Unit) {
    val darkTheme = LocalLibraDarkTheme.current
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = if (selected) {
            if (darkTheme) LibraLavenderDarkSurface else LibraLavenderSurface
        } else {
            MaterialTheme.colorScheme.surface
        },
        border = BorderStroke(if (selected) 1.5.dp else 1.dp, if (selected) LibraLavender else MaterialTheme.colorScheme.outline)
    ) {
        Row(
            Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(name, fontWeight = if (selected) FontWeight.ExtraBold else FontWeight.SemiBold)
                Text(subtitle, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            if (selected) {
                Icon(Icons.Default.Check, null, tint = LibraLavender, modifier = Modifier.size(20.dp))
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
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                ) {
                    Text(
                        "Aktif sunucu yasağı bulunmuyor.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(18.dp)
                    )
                }
            }
        }
        items(bans, key = { it.uid }) { ban ->
            Surface(
                Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                Row(
                    Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            ban.displayName.ifBlank { ban.username.ifBlank { ban.uid } },
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            ban.reason.ifBlank { "Sebep belirtilmedi" },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    TextButton(onClick = { onUnban(ban) }) {
                        Text("Yasağı Kaldır", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun ServerCategoriesPage(
    categories: List<ServerCategory>,
    onAddCategory: () -> Unit,
    onDelete: (ServerCategory) -> Unit,
    onMove: (ServerCategory, Int) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            Button(
                onClick = onAddCategory,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = LibraLavender)
            ) {
                Icon(Icons.Default.Add, null, tint = Color.White, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("Yeni Kategori Ekle", color = Color.White, fontWeight = FontWeight.Bold)
            }
        }
        itemsIndexed(categories, key = { _, it -> it.id }) { idx, category ->
            Surface(
                Modifier
                    .fillMaxWidth()
                    .longPressReorder(category.id) { onMove(category, it) },
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                Row(
                    Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(category.name, Modifier.weight(1f), fontWeight = FontWeight.Bold)
                    IconButton(enabled = idx > 0, onClick = { onMove(category, -1) }) {
                        Icon(Icons.Default.KeyboardArrowUp, "Yukarı")
                    }
                    IconButton(enabled = idx < categories.lastIndex, onClick = { onMove(category, 1) }) {
                        Icon(Icons.Default.KeyboardArrowDown, "Aşağı")
                    }
                    IconButton(onClick = { onDelete(category) }) {
                        Icon(Icons.Default.DeleteOutline, "Sil", tint = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }
    }
}

@Composable
private fun ServerChannelsPage(
    channels: List<ServerChannel>,
    categories: List<ServerCategory>,
    members: List<ServerMember>,
    roles: List<ServerRoleDefinition>,
    serverId: String,
    onAddChannel: (ServerCategory) -> Unit,
    onDelete: (ServerChannel) -> Unit,
    onMove: (ServerChannel, Int) -> Unit
) {
    val darkTheme = LocalLibraDarkTheme.current
    var permissionChannel by remember { mutableStateOf<ServerChannel?>(null) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (categories.isNotEmpty()) {
            item {
                Button(
                    onClick = { onAddChannel(categories.first()) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = LibraLavender)
                ) {
                    Icon(Icons.Default.Add, null, tint = Color.White, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Yeni Kanal Ekle", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }
        itemsIndexed(channels, key = { _, it -> it.id }) { idx, channel ->
            Surface(
                Modifier
                    .fillMaxWidth()
                    .longPressReorder(channel.id) { onMove(channel, it) },
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                Row(
                    Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text("# ${channel.name}", fontWeight = FontWeight.Bold)
                        Text(
                            channel.categoryName.ifBlank { "Genel" },
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(enabled = idx > 0, onClick = { onMove(channel, -1) }) {
                        Icon(Icons.Default.KeyboardArrowUp, "Yukarı")
                    }
                    IconButton(enabled = idx < channels.lastIndex, onClick = { onMove(channel, 1) }) {
                        Icon(Icons.Default.KeyboardArrowDown, "Aşağı")
                    }
                    TextButton(onClick = { permissionChannel = channel }) {
                        Icon(
                            Icons.Default.Lock,
                            null,
                            modifier = Modifier.size(15.dp),
                            tint = if (darkTheme) LibraLavenderDarkText else LibraLavenderText
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            "İzinler",
                            fontWeight = FontWeight.Bold,
                            color = if (darkTheme) LibraLavenderDarkText else LibraLavenderText
                        )
                    }
                    IconButton(onClick = { onDelete(channel) }) {
                        Icon(Icons.Default.DeleteOutline, "Sil", tint = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }
    }

    permissionChannel?.let { channel ->
        ChannelPermissionDialog(serverId, channel, members, roles) {
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
        "SEND_MEDIA" to "Fotoğraf / medya gönderebilir",
        "MANAGE_CHANNELS" to "Kanalları yönetebilir",
        "MANAGE_MEMBERS" to "Üyeleri yönetebilir",
        "BAN_MEMBERS" to "Üyeleri banlayabilir"
    )

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            OutlinedTextField(
                value = name,
                onValueChange = { if (it.length <= 30) onNameChange(it) },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Rol Adı") },
                singleLine = true,
                shape = RoundedCornerShape(16.dp)
            )
        }
        item {
            Text("İzinler", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
        }
        items(available) { (permission, label) ->
            Surface(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .clickable { onPermissionChange(permission, permission !in permissions) },
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                Row(
                    Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = permission in permissions,
                        onCheckedChange = { onPermissionChange(permission, it) }
                    )
                    Text(label, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
        item {
            Spacer(Modifier.height(8.dp))
            Button(
                onClick = onSave,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(16.dp),
                enabled = name.trim().isNotEmpty(),
                colors = ButtonDefaults.buttonColors(containerColor = LibraLavender)
            ) {
                Text(
                    if (role == null) "Rolü Oluştur" else "Değişiklikleri Kaydet",
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
