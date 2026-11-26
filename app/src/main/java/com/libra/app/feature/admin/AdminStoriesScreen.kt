package com.libra.app.feature.admin

import android.text.format.DateFormat
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Schedule
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.google.firebase.auth.FirebaseAuth
import com.libra.app.domain.model.AdminPermissionSet
import com.libra.app.domain.model.Story
import com.libra.app.domain.model.UserProfile
import com.libra.app.ui.animation.appleBouncy
import com.libra.app.ui.components.FullScreenImageViewerDialog
import com.libra.app.ui.components.UserAvatar
import java.util.Date

@Composable
fun AdminStoriesScreen(
    vm: AdminViewModel,
    permissions: AdminPermissionSet,
    initialOwnerFilter: UserProfile? = null,
    onBack: () -> Unit,
    onOpenMemberAdmin: (UserProfile) -> Unit,
    modifier: Modifier = Modifier
) {
    val allStories by vm.stories.collectAsState()
    val storiesLoading by vm.storiesLoading.collectAsState()
    val busy by vm.storyActionBusy.collectAsState()
    val actionMessage by vm.storyActionMessage.collectAsState()
    val members by vm.members.collectAsState()
    val error by vm.error.collectAsState()

    val currentUid = remember { FirebaseAuth.getInstance().currentUser?.uid.orEmpty() }
    val nowMs = System.currentTimeMillis()

    var searchQuery by remember { mutableStateOf("") }
    var ownerFilterUid by remember(initialOwnerFilter?.uid) {
        mutableStateOf(initialOwnerFilter?.uid?.takeIf { it.isNotBlank() })
    }
    var storyFilter by remember { mutableStateOf(AdminStoryFilter.ALL) }
    var sortOption by remember { mutableStateOf(AdminStorySortOption.NEWEST) }
    var showSortMenu by remember { mutableStateOf(false) }

    var selectionMode by remember { mutableStateOf(false) }
    var selectedStoryIds by remember { mutableStateOf<Set<String>>(emptySet()) }

    var inspectedStoryId by remember { mutableStateOf<String?>(null) }
    var deleteConfirmStory by remember { mutableStateOf<Story?>(null) }
    var showBatchDeleteConfirm by remember { mutableStateOf(false) }
    var fullScreenMediaUrl by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        vm.observeStories()
    }

    val activeOwnerProfile = remember(ownerFilterUid, members, initialOwnerFilter) {
        if (ownerFilterUid.isNullOrBlank()) null
        else members.firstOrNull { it.uid == ownerFilterUid } ?: initialOwnerFilter
    }

    val scopedStories = remember(allStories, ownerFilterUid) {
        if (ownerFilterUid.isNullOrBlank()) allStories else allStories.filter { it.authorId == ownerFilterUid }
    }
    val filteredStories = remember(allStories, searchQuery, ownerFilterUid, storyFilter, sortOption) {
        filterAndSortAdminStories(
            stories = allStories,
            query = searchQuery,
            ownerFilterUid = ownerFilterUid,
            filter = storyFilter,
            sortOption = sortOption
        )
    }

    val activeCount = scopedStories.count { it.expiresAt > nowMs }
    val expiredCount = scopedStories.count { it.expiresAt <= nowMs }
    val mediaCount = scopedStories.count { it.mediaUrl.isNotBlank() }
    val textCount = scopedStories.count { it.mediaUrl.isBlank() }

    val inspectedStory = inspectedStoryId?.let { id -> allStories.firstOrNull { it.id == id } }

    if (inspectedStory != null) {
        val ownerProfile = members.firstOrNull { it.uid == inspectedStory.authorId }
        AdminStoryControlCenter(
            story = inspectedStory,
            ownerProfile = ownerProfile,
            vm = vm,
            permissions = permissions,
            currentUid = currentUid,
            onBack = { inspectedStoryId = null },
            onOpenMemberAdmin = { target ->
                inspectedStoryId = null
                onOpenMemberAdmin(target)
            },
            onFilterAuthorStories = { uid ->
                inspectedStoryId = null
                ownerFilterUid = uid
            },
            onDeleteStory = { deleteConfirmStory = it },
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
                        IconButton(
                            onClick = onBack,
                            modifier = Modifier.appleBouncy(pressedScale = 0.86f, onClick = onBack)
                        ) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, "Geri")
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Hikâye İşlemleri",
                                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            Text(
                                text = "${filteredStories.size} hikâye • $activeCount aktif • $mediaCount görselli",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        IconButton(
                            onClick = {
                                selectionMode = !selectionMode
                                if (!selectionMode) selectedStoryIds = emptySet()
                            },
                            modifier = Modifier.appleBouncy(
                                pressedScale = 0.88f,
                                onClick = {
                                    selectionMode = !selectionMode
                                    if (!selectionMode) selectedStoryIds = emptySet()
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
                                IconButton(onClick = { vm.clearStoryActionMessage() }, modifier = Modifier.size(24.dp)) {
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
                    item(key = "busy-bar") {
                        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                    }
                }

                if (activeOwnerProfile != null) {
                    item(key = "owner-filter-card") {
                        val storyDisabled = activeOwnerProfile.moderation.storyDisabled
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
                                            text = "${activeOwnerProfile.displayName.ifBlank { "Üye" }} Hikâyeleri",
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
                                            Text("Hikâye Paylaşma Engeli", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold))
                                            Text(
                                                if (storyDisabled) "Üye yeni hikâye paylaşamıyor" else "Üye hikâye paylaşabiliyor",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = if (storyDisabled) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        Switch(
                                            checked = storyDisabled,
                                            onCheckedChange = { vm.toggleAuthorStoryPermission(activeOwnerProfile.uid, it) }
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
                            Triple(AdminStoryFilter.ALL, "Tümü", scopedStories.size),
                            Triple(AdminStoryFilter.ACTIVE, "Aktif", activeCount),
                            Triple(AdminStoryFilter.EXPIRED, "Süresi Dolmuş", expiredCount),
                            Triple(AdminStoryFilter.WITH_MEDIA, "Görselli", mediaCount),
                            Triple(AdminStoryFilter.TEXT_ONLY, "Metin", textCount)
                        )
                        items(metrics, key = { it.first.name }) { (filter, label, count) ->
                            val selected = storyFilter == filter
                            val shape = RoundedCornerShape(14.dp)
                            Surface(
                                modifier = Modifier
                                    .clip(shape)
                                    .appleBouncy(pressedScale = 0.96f, onClick = {
                                        storyFilter = if (selected && filter != AdminStoryFilter.ALL) AdminStoryFilter.ALL else filter
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
                            placeholder = { Text("Hikâye metni, yazar adı veya ID ara…") },
                            shape = RoundedCornerShape(14.dp)
                        )

                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            item {
                                Box {
                                    FilterChip(
                                        selected = sortOption != AdminStorySortOption.NEWEST,
                                        onClick = { showSortMenu = true },
                                        leadingIcon = { Icon(Icons.Default.Sort, null, modifier = Modifier.size(16.dp)) },
                                        label = { Text(sortOption.label) }
                                    )
                                    DropdownMenu(
                                        expanded = showSortMenu,
                                        onDismissRequest = { showSortMenu = false },
                                        containerColor = MaterialTheme.colorScheme.surface
                                    ) {
                                        AdminStorySortOption.entries.forEach { option ->
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
                            if (searchQuery.isNotBlank() || storyFilter != AdminStoryFilter.ALL || ownerFilterUid != null) {
                                item {
                                    FilterChip(
                                        selected = false,
                                        onClick = {
                                            searchQuery = ""
                                            storyFilter = AdminStoryFilter.ALL
                                            ownerFilterUid = null
                                            sortOption = AdminStorySortOption.NEWEST
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
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("${selectedStoryIds.size} hikâye seçildi", fontWeight = FontWeight.Bold)
                                    Row {
                                        TextButton(onClick = { selectedStoryIds = filteredStories.map { it.id }.toSet() }) {
                                            Text("Tümünü Seç")
                                        }
                                        TextButton(onClick = { selectedStoryIds = emptySet() }) {
                                            Text("Temizle")
                                        }
                                    }
                                }
                                Button(
                                    onClick = { showBatchDeleteConfirm = true },
                                    enabled = selectedStoryIds.isNotEmpty() && !busy,
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(Icons.Default.Delete, null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text("Seçili Hikâyeleri Sil")
                                }
                            }
                        }
                    }
                }

                if (storiesLoading && allStories.isEmpty()) {
                    item(key = "loading") {
                        Box(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 48.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    }
                } else if (filteredStories.isEmpty()) {
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
                                Icon(Icons.Default.AutoAwesome, null, modifier = Modifier.size(34.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("Hikâye bulunamadı", fontWeight = FontWeight.Bold)
                                Text(
                                    "Arama veya filtre kriterlerini değiştirerek tekrar dene.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                } else {
                    items(filteredStories, key = { it.id }) { story ->
                        val selected = story.id in selectedStoryIds
                        AdminStoryCard(
                            story = story,
                            nowMs = nowMs,
                            selectionMode = selectionMode,
                            selected = selected,
                            onToggleSelect = {
                                selectedStoryIds = if (selected) selectedStoryIds - story.id else selectedStoryIds + story.id
                            },
                            onOpenControl = { inspectedStoryId = story.id },
                            onDelete = { deleteConfirmStory = story },
                            onOpenMedia = { signedUrl -> fullScreenMediaUrl = signedUrl }
                        )
                    }
                }

                item(key = "bottom-spacer") {
                    Spacer(Modifier.height(24.dp))
                }
            }
        }
    }

    deleteConfirmStory?.let { target ->
        AlertDialog(
            onDismissRequest = { if (!busy) deleteConfirmStory = null },
            containerColor = MaterialTheme.colorScheme.surface,
            title = { Text("Hikâyeyi Sil", fontWeight = FontWeight.Bold) },
            text = {
                Text("${target.authorName.ifBlank { "Üye" }} kullanıcısına ait hikâye ve varsa medyası kalıcı olarak silinecek. Devam edilsin mi?")
            },
            confirmButton = {
                Button(
                    onClick = {
                        vm.deleteStoryAsAdmin(target, currentUid) { ok ->
                            if (ok) {
                                if (inspectedStoryId == target.id) inspectedStoryId = null
                                deleteConfirmStory = null
                            }
                        }
                    },
                    enabled = !busy,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text("Sil") }
            },
            dismissButton = {
                TextButton(onClick = { deleteConfirmStory = null }, enabled = !busy) { Text("İptal") }
            }
        )
    }

    if (showBatchDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { if (!busy) showBatchDeleteConfirm = false },
            containerColor = MaterialTheme.colorScheme.surface,
            title = { Text("Toplu Hikâye Silme", fontWeight = FontWeight.Bold) },
            text = { Text("Seçili ${selectedStoryIds.size} hikâye kalıcı olarak silinecek. Emin misin?") },
            confirmButton = {
                Button(
                    onClick = {
                        vm.batchDeleteStories(selectedStoryIds, currentUid) { count ->
                            if (count > 0) {
                                showBatchDeleteConfirm = false
                                selectionMode = false
                                selectedStoryIds = emptySet()
                            }
                        }
                    },
                    enabled = !busy,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text("Hepsini Sil") }
            },
            dismissButton = {
                TextButton(onClick = { showBatchDeleteConfirm = false }, enabled = !busy) { Text("İptal") }
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
private fun AdminStoryCard(
    story: Story,
    nowMs: Long,
    selectionMode: Boolean,
    selected: Boolean,
    onToggleSelect: () -> Unit,
    onOpenControl: () -> Unit,
    onDelete: () -> Unit,
    onOpenMedia: (String) -> Unit
) {
    val isActive = story.expiresAt > nowMs
    val shape = RoundedCornerShape(18.dp)
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .appleBouncy(
                pressedScale = 0.985f,
                onClick = { if (selectionMode) onToggleSelect() else onOpenControl() }
            ),
        shape = shape,
        color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
        border = BorderStroke(
            width = if (selected) 1.5.dp else 1.dp,
            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (selectionMode) {
                    Checkbox(checked = selected, onCheckedChange = { onToggleSelect() })
                }
                UserAvatar(
                    photoUrl = story.authorPhotoUrl,
                    initials = story.authorName.take(1).uppercase(),
                    size = 38.dp
                )
                Spacer(Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = story.authorName.ifBlank { "Libra Üyesi" },
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = DateFormat.format("dd MMM HH:mm", Date(story.createdAt)).toString(),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (isActive) Color(0xFFDCFCE7) else Color(0xFFE5E7EB)
                ) {
                    Text(
                        text = if (isActive) "Aktif" else "Süresi Doldu",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                        color = if (isActive) Color(0xFF166534) else Color(0xFF374151),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
                IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.Delete, "Sil", modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.error)
                }
            }

            if (story.text.isNotBlank()) {
                Text(
                    text = story.text,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
            }

            if (story.mediaUrl.isNotBlank()) {
                AdminSignedImage(
                    mediaKey = story.mediaUrl,
                    contentDescription = "Hikâye görseli",
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(175.dp),
                    onImageClick = onOpenMedia
                )
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Icon(Icons.Default.FavoriteBorder, null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("${story.likesCount}", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Icon(Icons.Default.Schedule, null, modifier = Modifier.size(15.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            text = formatStoryExpiry(story.expiresAt, nowMs),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Text(
                    text = "İncele & Yönet",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
private fun AdminStoryControlCenter(
    story: Story,
    ownerProfile: UserProfile?,
    vm: AdminViewModel,
    permissions: AdminPermissionSet,
    currentUid: String,
    onBack: () -> Unit,
    onOpenMemberAdmin: (UserProfile) -> Unit,
    onFilterAuthorStories: (String) -> Unit,
    onDeleteStory: (Story) -> Unit,
    onOpenFullScreenImage: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val busy by vm.storyActionBusy.collectAsState()
    var showNotifyDialog by remember { mutableStateOf(false) }
    val nowMs = System.currentTimeMillis()
    val isActive = story.expiresAt > nowMs

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
            item(key = "top") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBack, modifier = Modifier.appleBouncy(pressedScale = 0.86f, onClick = onBack)) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Geri")
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Hikâye Denetimi", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold))
                        Text(
                            "ID: ${story.id}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    IconButton(onClick = { onDeleteStory(story) }) {
                        Icon(Icons.Default.Delete, "Sil", tint = MaterialTheme.colorScheme.error)
                    }
                }
            }

            if (busy) {
                item(key = "busy") { LinearProgressIndicator(modifier = Modifier.fillMaxWidth()) }
            }

            item(key = "content-card") {
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
                                photoUrl = story.authorPhotoUrl,
                                initials = story.authorName.take(1).uppercase(),
                                size = 44.dp
                            )
                            Spacer(Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(story.authorName.ifBlank { "Libra Üyesi" }, fontWeight = FontWeight.Bold)
                                Text(
                                    "Paylaşım: ${DateFormat.format("dd MMM yyyy HH:mm", Date(story.createdAt))}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (isActive) Color(0xFFDCFCE7) else Color(0xFFE5E7EB)
                            ) {
                                Text(
                                    text = if (isActive) "Aktif" else "Süresi Doldu",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                    color = if (isActive) Color(0xFF166534) else Color(0xFF374151),
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                        }

                        if (story.mediaUrl.isNotBlank()) {
                            AdminSignedImage(
                                mediaKey = story.mediaUrl,
                                contentDescription = "Hikâye medyası",
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(260.dp),
                                onImageClick = onOpenFullScreenImage
                            )
                        }

                        if (story.text.isNotBlank()) {
                            Text(story.text, style = MaterialTheme.typography.bodyLarge)
                        }

                        Text(
                            text = "Beğeni: ${story.likesCount} • Bitiş: ${DateFormat.format("dd MMM HH:mm", Date(story.expiresAt))}",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Button(
                            onClick = { onDeleteStory(story) },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Delete, null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Hikâyeyi Kalıcı Sil")
                        }
                    }
                }
            }

            item(key = "author-card") {
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
                        Text("Yazar ve Hikâye Yetkileri", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
                        if (ownerProfile != null && permissions.manageBans) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Hikâye Paylaşma Engeli", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium))
                                    Text(
                                        if (ownerProfile.moderation.storyDisabled) "Kısıtlı" else "Açık",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (ownerProfile.moderation.storyDisabled) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Switch(
                                    checked = ownerProfile.moderation.storyDisabled,
                                    onCheckedChange = { vm.toggleAuthorStoryPermission(ownerProfile.uid, it) }
                                )
                            }
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(
                                onClick = { onFilterAuthorStories(story.authorId) },
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.weight(1f)
                            ) { Text("Tüm Hikâyeleri") }
                            if (ownerProfile != null) {
                                OutlinedButton(
                                    onClick = { onOpenMemberAdmin(ownerProfile) },
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Default.Person, null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("Üye Profili")
                                }
                            }
                        }
                        if (permissions.sendFeedback && story.authorId.isNotBlank()) {
                            OutlinedButton(
                                onClick = { showNotifyDialog = true },
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Default.Notifications, null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Üyeye Bildirim Gönder")
                            }
                        }
                    }
                }
            }

            item(key = "bottom-spacer") { Spacer(Modifier.height(24.dp)) }
        }
    }

    if (showNotifyDialog) {
        var notifTitle by remember { mutableStateOf("Hikâye Yönetimi Bilgilendirmesi") }
        var notifBody by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showNotifyDialog = false },
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
                        vm.sendStoryModerationNotification(story.authorId, currentUid, notifTitle, notifBody, story.id) { ok ->
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

private fun formatStoryExpiry(expiresAt: Long, nowMs: Long): String {
    if (expiresAt <= 0L) return "Süre bilinmiyor"
    val diff = expiresAt - nowMs
    if (diff <= 0L) return "24s doldu"
    val totalMinutes = diff / 60_000L
    val hours = totalMinutes / 60L
    val minutes = totalMinutes % 60L
    return if (hours > 0L) "${hours}sa ${minutes}dk kaldı" else "${minutes}dk kaldı"
}
