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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
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
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
import com.libra.app.domain.model.AdminPermissionSet
import com.libra.app.domain.model.GlobalChatMessage
import com.libra.app.domain.model.UserProfile
import com.libra.app.ui.animation.appleBouncy
import com.libra.app.ui.components.FullScreenImageViewerDialog
import com.libra.app.ui.components.UserAvatar
import java.util.Date

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AdminGlobalChatScreen(
    vm: AdminViewModel,
    permissions: AdminPermissionSet,
    initialOwnerFilter: UserProfile? = null,
    onBack: () -> Unit,
    onOpenMemberAdmin: (UserProfile) -> Unit,
    modifier: Modifier = Modifier
) {
    val allMessages by vm.globalMessages.collectAsState()
    val loading by vm.globalMessagesLoading.collectAsState()
    val busy by vm.globalChatActionBusy.collectAsState()
    val actionMessage by vm.globalChatActionMessage.collectAsState()
    val members by vm.members.collectAsState()
    val error by vm.error.collectAsState()

    val currentUid = remember { FirebaseAuth.getInstance().currentUser?.uid.orEmpty() }

    var searchQuery by remember { mutableStateOf("") }
    var ownerFilterUid by remember(initialOwnerFilter?.uid) {
        mutableStateOf(initialOwnerFilter?.uid?.takeIf { it.isNotBlank() })
    }
    var chatFilter by remember { mutableStateOf(AdminGlobalChatFilter.ALL) }
    var sortOption by remember { mutableStateOf(AdminGlobalChatSortOption.NEWEST) }
    var showSortMenu by remember { mutableStateOf(false) }

    var selectionMode by remember { mutableStateOf(false) }
    var selectedMessageIds by remember { mutableStateOf<Set<String>>(emptySet()) }

    var inspectedMessage by remember { mutableStateOf<GlobalChatMessage?>(null) }
    var deleteConfirmMessage by remember { mutableStateOf<GlobalChatMessage?>(null) }
    var showBatchDeleteConfirm by remember { mutableStateOf(false) }
    var showAnnouncementDialog by remember { mutableStateOf(false) }
    var notifyTargetUid by remember { mutableStateOf<String?>(null) }
    var fullScreenMediaUrl by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        vm.observeGlobalMessages()
    }

    val activeOwnerProfile = remember(ownerFilterUid, members, initialOwnerFilter) {
        if (ownerFilterUid.isNullOrBlank()) null
        else members.firstOrNull { it.uid == ownerFilterUid } ?: initialOwnerFilter
    }

    val scopedMessages = remember(allMessages, ownerFilterUid) {
        if (ownerFilterUid.isNullOrBlank()) allMessages else allMessages.filter { it.senderId == ownerFilterUid }
    }
    val filteredMessages = remember(allMessages, searchQuery, ownerFilterUid, chatFilter, sortOption) {
        filterAndSortAdminGlobalMessages(
            messages = allMessages,
            query = searchQuery,
            ownerFilterUid = ownerFilterUid,
            filter = chatFilter,
            sortOption = sortOption
        )
    }

    val mediaCount = scopedMessages.count { it.mediaUrl.isNotBlank() }
    val sharedCount = scopedMessages.count { it.sharedContent != null }
    val editedCount = scopedMessages.count { it.editedAt != null }
    val reactedCount = scopedMessages.count { it.reactions.isNotEmpty() }

    Surface(modifier = modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item(key = "header") {
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onBack, modifier = Modifier.appleBouncy(pressedScale = 0.86f, onClick = onBack)) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Geri")
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Genel Chat İşlemleri",
                            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = "${filteredMessages.size} mesaj • $mediaCount görselli • $reactedCount tepkili",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(
                        onClick = { showAnnouncementDialog = true },
                        modifier = Modifier.appleBouncy(pressedScale = 0.88f, onClick = { showAnnouncementDialog = true })
                    ) {
                        Icon(Icons.Default.Campaign, "Duyuru Gönder", tint = MaterialTheme.colorScheme.primary)
                    }
                    IconButton(
                        onClick = {
                            selectionMode = !selectionMode
                            if (!selectionMode) selectedMessageIds = emptySet()
                        },
                        modifier = Modifier.appleBouncy(
                            pressedScale = 0.88f,
                            onClick = {
                                selectionMode = !selectionMode
                                if (!selectionMode) selectedMessageIds = emptySet()
                            }
                        )
                    ) {
                        Icon(
                            imageVector = if (selectionMode) Icons.Default.Close else Icons.Default.Checklist,
                            contentDescription = "Toplu İşlem",
                            tint = if (selectionMode) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onBackground
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
                            IconButton(onClick = { vm.clearGlobalChatActionMessage() }, modifier = Modifier.size(24.dp)) {
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
                    val messagingDisabled = activeOwnerProfile.moderation.messagingDisabled
                    val serverMessagingDisabled = activeOwnerProfile.moderation.serverMessagingDisabled
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(18.dp),
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                UserAvatar(
                                    photoUrl = activeOwnerProfile.profileImageUrl,
                                    initials = activeOwnerProfile.displayName.take(1).uppercase(),
                                    size = 42.dp
                                )
                                Spacer(Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("${activeOwnerProfile.displayName.ifBlank { "Üye" }} Mesajları", fontWeight = FontWeight.Bold)
                                    Text(activeOwnerProfile.handle, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                                        Text("Mesaj Atma Engeli", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold))
                                        Text(
                                            if (messagingDisabled) "DM ve Genel Chat kısıtlı" else "Açık",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = if (messagingDisabled) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Switch(
                                        checked = messagingDisabled,
                                        onCheckedChange = { vm.toggleUserGlobalMessagingPermission(activeOwnerProfile.uid, it) }
                                    )
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("Genel Chat / Sunucu Mesaj Engeli", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold))
                                        Text(
                                            if (serverMessagingDisabled) "Genel chat & sunucu mesajları kısıtlı" else "Açık",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = if (serverMessagingDisabled) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Switch(
                                        checked = serverMessagingDisabled,
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
                        Triple(AdminGlobalChatFilter.ALL, "Tümü", scopedMessages.size),
                        Triple(AdminGlobalChatFilter.WITH_MEDIA, "Görselli", mediaCount),
                        Triple(AdminGlobalChatFilter.WITH_SHARED_CONTENT, "Paylaşımlı", sharedCount),
                        Triple(AdminGlobalChatFilter.EDITED, "Düzenlenen", editedCount),
                        Triple(AdminGlobalChatFilter.WITH_REACTIONS, "Tepkili", reactedCount)
                    )
                    items(metrics, key = { it.first.name }) { (filter, label, count) ->
                        val selected = chatFilter == filter
                        val shape = RoundedCornerShape(14.dp)
                        Surface(
                            modifier = Modifier
                                .clip(shape)
                                .appleBouncy(pressedScale = 0.96f, onClick = {
                                    chatFilter = if (selected && filter != AdminGlobalChatFilter.ALL) AdminGlobalChatFilter.ALL else filter
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
                        placeholder = { Text("Mesaj metni, gönderen, @kullanıcı veya ID ara…") },
                        shape = RoundedCornerShape(14.dp)
                    )

                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        item {
                            Box {
                                FilterChip(
                                    selected = sortOption != AdminGlobalChatSortOption.NEWEST,
                                    onClick = { showSortMenu = true },
                                    leadingIcon = { Icon(Icons.Default.Sort, null, modifier = Modifier.size(16.dp)) },
                                    label = { Text(sortOption.label) }
                                )
                                DropdownMenu(
                                    expanded = showSortMenu,
                                    onDismissRequest = { showSortMenu = false },
                                    containerColor = MaterialTheme.colorScheme.surface
                                ) {
                                    AdminGlobalChatSortOption.entries.forEach { option ->
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
                        if (searchQuery.isNotBlank() || chatFilter != AdminGlobalChatFilter.ALL || ownerFilterUid != null) {
                            item {
                                FilterChip(
                                    selected = false,
                                    onClick = {
                                        searchQuery = ""
                                        chatFilter = AdminGlobalChatFilter.ALL
                                        ownerFilterUid = null
                                        sortOption = AdminGlobalChatSortOption.NEWEST
                                    },
                                    leadingIcon = { Icon(Icons.Default.Close, null, modifier = Modifier.size(16.dp)) },
                                    label = { Text("Sıfırla") }
                                )
                            }
                        }
                    }
                }
            }

            if (selectionMode) {
                item(key = "batch-bar") {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("${selectedMessageIds.size} mesaj seçildi", fontWeight = FontWeight.Bold)
                                Row {
                                    TextButton(onClick = { selectedMessageIds = filteredMessages.map { it.id }.toSet() }) { Text("Tümünü Seç") }
                                    TextButton(onClick = { selectedMessageIds = emptySet() }) { Text("Temizle") }
                                }
                            }
                            Button(
                                onClick = { showBatchDeleteConfirm = true },
                                enabled = selectedMessageIds.isNotEmpty() && !busy,
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Default.Delete, null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Seçili Mesajları Sil")
                            }
                        }
                    }
                }
            }

            if (loading && allMessages.isEmpty()) {
                item(key = "loading") {
                    Box(Modifier.fillMaxWidth().padding(vertical = 48.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
            } else if (filteredMessages.isEmpty()) {
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
                            Icon(Icons.Default.ChatBubbleOutline, null, modifier = Modifier.size(34.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("Mesaj bulunamadı", fontWeight = FontWeight.Bold)
                            Text("Arama veya filtre kriterlerini değiştirerek tekrar dene.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            } else {
                items(filteredMessages, key = { it.id }) { msg ->
                    val selected = msg.id in selectedMessageIds
                    AdminGlobalMessageCard(
                        message = msg,
                        selectionMode = selectionMode,
                        selected = selected,
                        onToggleSelect = {
                            selectedMessageIds = if (selected) selectedMessageIds - msg.id else selectedMessageIds + msg.id
                        },
                        onInspect = { inspectedMessage = msg },
                        onDelete = { deleteConfirmMessage = msg },
                        onOpenMedia = { signedUrl -> fullScreenMediaUrl = signedUrl }
                    )
                }
            }

            item(key = "bottom-spacer") { Spacer(Modifier.height(24.dp)) }
        }
    }

    inspectedMessage?.let { msg ->
        val senderProfile = members.firstOrNull { it.uid == msg.senderId }
        AlertDialog(
            onDismissRequest = { inspectedMessage = null },
            containerColor = MaterialTheme.colorScheme.surface,
            title = { Text(msg.senderName.ifBlank { "Mesaj Detayı" }, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = buildString {
                            if (msg.senderUsername.isNotBlank()) append("@${msg.senderUsername} • ")
                            append(DateFormat.format("dd MMM yyyy HH:mm", Date(msg.createdAt)).toString())
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (msg.mediaUrl.isNotBlank()) {
                        AdminSignedImage(
                            mediaKey = msg.mediaUrl,
                            contentDescription = "Mesaj görseli",
                            modifier = Modifier.fillMaxWidth().height(160.dp),
                            onImageClick = { fullScreenMediaUrl = it }
                        )
                    }
                    if (msg.text.isNotBlank()) {
                        Text(msg.text, style = MaterialTheme.typography.bodyMedium)
                    }
                    msg.sharedContent?.let { shared ->
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            border = BorderStroke(0.8.dp, MaterialTheme.colorScheme.outlineVariant),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text("Paylaşılan (${shared.type})", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                                Text(shared.title.ifBlank { shared.id }, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                    if (msg.reactions.isNotEmpty()) {
                        Text(
                            text = "Tepkiler (${msg.reactions.size}): ${msg.reactions.values.distinct().joinToString(" ")}",
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        OutlinedButton(onClick = {
                            inspectedMessage = null
                            ownerFilterUid = msg.senderId
                        }) { Text("Tüm Mesajları") }
                        if (senderProfile != null) {
                            OutlinedButton(onClick = {
                                inspectedMessage = null
                                onOpenMemberAdmin(senderProfile)
                            }) { Text("Üye Profili") }
                        }
                        if (permissions.sendFeedback && msg.senderId.isNotBlank()) {
                            OutlinedButton(onClick = {
                                inspectedMessage = null
                                notifyTargetUid = msg.senderId
                            }) { Text("Bildirim Gönder") }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val target = msg
                        inspectedMessage = null
                        deleteConfirmMessage = target
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text("Mesajı Sil") }
            },
            dismissButton = { TextButton(onClick = { inspectedMessage = null }) { Text("Kapat") } }
        )
    }

    deleteConfirmMessage?.let { target ->
        AlertDialog(
            onDismissRequest = { if (!busy) deleteConfirmMessage = null },
            containerColor = MaterialTheme.colorScheme.surface,
            title = { Text("Mesajı Sil", fontWeight = FontWeight.Bold) },
            text = { Text("${target.senderName} kullanıcısının genel chat mesajı kalıcı olarak silinecek. Devam edilsin mi?") },
            confirmButton = {
                Button(
                    onClick = {
                        vm.deleteGlobalMessageAsAdmin(target.id) { ok ->
                            if (ok) deleteConfirmMessage = null
                        }
                    },
                    enabled = !busy,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text("Sil") }
            },
            dismissButton = { TextButton(onClick = { deleteConfirmMessage = null }, enabled = !busy) { Text("İptal") } }
        )
    }

    if (showBatchDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { if (!busy) showBatchDeleteConfirm = false },
            containerColor = MaterialTheme.colorScheme.surface,
            title = { Text("Toplu Mesaj Silme", fontWeight = FontWeight.Bold) },
            text = { Text("Seçili ${selectedMessageIds.size} mesaj genel chatten silinecek. Emin misin?") },
            confirmButton = {
                Button(
                    onClick = {
                        vm.batchDeleteGlobalMessages(selectedMessageIds) { count ->
                            if (count > 0) {
                                showBatchDeleteConfirm = false
                                selectionMode = false
                                selectedMessageIds = emptySet()
                            }
                        }
                    },
                    enabled = !busy,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text("Hepsini Sil") }
            },
            dismissButton = { TextButton(onClick = { showBatchDeleteConfirm = false }, enabled = !busy) { Text("İptal") } }
        )
    }

    if (showAnnouncementDialog) {
        var announcementText by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { if (!busy) showAnnouncementDialog = false },
            containerColor = MaterialTheme.colorScheme.surface,
            title = { Text("Genel Chate Mesaj Gönder", fontWeight = FontWeight.Bold) },
            text = {
                OutlinedTextField(
                    value = announcementText,
                    onValueChange = { if (it.length <= 1000) announcementText = it },
                    label = { Text("Mesaj / Duyuru") },
                    placeholder = { Text("Tüm üyelere duyuru veya uyarı yaz…") },
                    supportingText = { Text("${announcementText.length}/1000") },
                    minLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        vm.sendGlobalAnnouncementAsAdmin(announcementText) { ok ->
                            if (ok) showAnnouncementDialog = false
                        }
                    },
                    enabled = announcementText.isNotBlank() && !busy
                ) {
                    Icon(Icons.AutoMirrored.Filled.Send, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Gönder")
                }
            },
            dismissButton = { TextButton(onClick = { showAnnouncementDialog = false }, enabled = !busy) { Text("İptal") } }
        )
    }

    notifyTargetUid?.let { recipientUid ->
        var notifTitle by remember { mutableStateOf("Genel Chat Moderasyon Bildirimi") }
        var notifBody by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { notifyTargetUid = null },
            containerColor = MaterialTheme.colorScheme.surface,
            title = { Text("Üyeye Bildirim Gönder", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = notifTitle, onValueChange = { notifTitle = it }, label = { Text("Başlık") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = notifBody, onValueChange = { notifBody = it }, label = { Text("Mesaj") }, minLines = 3, modifier = Modifier.fillMaxWidth())
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        vm.sendGlobalChatModerationNotification(recipientUid, currentUid, notifTitle, notifBody) { ok ->
                            if (ok) notifyTargetUid = null
                        }
                    },
                    enabled = notifBody.isNotBlank() && !busy
                ) { Text("Gönder") }
            },
            dismissButton = { TextButton(onClick = { notifyTargetUid = null }) { Text("İptal") } }
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
private fun AdminGlobalMessageCard(
    message: GlobalChatMessage,
    selectionMode: Boolean,
    selected: Boolean,
    onToggleSelect: () -> Unit,
    onInspect: () -> Unit,
    onDelete: () -> Unit,
    onOpenMedia: (String) -> Unit
) {
    val shape = RoundedCornerShape(18.dp)
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .appleBouncy(
                pressedScale = 0.985f,
                onClick = { if (selectionMode) onToggleSelect() else onInspect() }
            ),
        shape = shape,
        color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
        border = BorderStroke(
            width = if (selected) 1.5.dp else 1.dp,
            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
        )
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                if (selectionMode) {
                    Checkbox(checked = selected, onCheckedChange = { onToggleSelect() })
                }
                UserAvatar(message.senderPhotoUrl, message.senderName.take(1).uppercase(), size = 36.dp)
                Spacer(Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = message.senderName.ifBlank { "Libra Üyesi" },
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = buildString {
                            if (message.senderUsername.isNotBlank()) append("@${message.senderUsername}")
                            if (message.createdAt > 0L) {
                                if (isNotEmpty()) append(" • ")
                                append(DateFormat.format("dd MMM HH:mm", Date(message.createdAt)).toString())
                            }
                            if (message.editedAt != null) append(" • düzenlendi")
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.Delete, "Sil", modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.error)
                }
            }

            if (message.replyToMessageId.isNotBlank()) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Yanıt -> ${message.replyToSenderName.ifBlank { "Mesaj" }}: ${message.replyToText.ifBlank { "Medya" }}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
            }

            message.sharedContent?.let { shared ->
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    border = BorderStroke(0.8.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        Text("Paylaşılan (${shared.type})", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                        Text(shared.title.ifBlank { "Libra içeriği" }, style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold), maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }

            if (message.mediaUrl.isNotBlank()) {
                AdminSignedImage(
                    mediaKey = message.mediaUrl,
                    contentDescription = "Genel chat medyası",
                    modifier = Modifier.fillMaxWidth().height(175.dp),
                    onImageClick = onOpenMedia
                )
            }

            if (message.text.isNotBlank()) {
                Text(
                    text = message.text,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 4,
                    overflow = TextOverflow.Ellipsis
                )
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = if (message.reactions.isNotEmpty()) {
                        "${message.reactions.size} tepki (${message.reactions.values.distinct().joinToString(" ")})"
                    } else {
                        "Tepki yok"
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "İncele & İşlemler",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}
