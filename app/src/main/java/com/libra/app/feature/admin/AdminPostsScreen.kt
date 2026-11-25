package com.libra.app.feature.admin

import android.text.format.DateFormat
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material.icons.filled.Tag
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.InputChip
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
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
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.google.firebase.auth.FirebaseAuth
import com.libra.app.core.result.AppResult
import com.libra.app.domain.model.AdminPermissionSet
import com.libra.app.domain.model.Post
import com.libra.app.domain.model.PostComment
import com.libra.app.domain.model.UserProfile
import com.libra.app.ui.animation.appleBouncy
import com.libra.app.ui.components.FullScreenImageViewerDialog
import com.libra.app.ui.components.UserAvatar
import java.util.Date

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AdminPostsScreen(
    vm: AdminViewModel,
    permissions: AdminPermissionSet,
    initialOwnerFilter: UserProfile? = null,
    onBack: () -> Unit,
    onOpenMemberAdmin: (UserProfile) -> Unit,
    modifier: Modifier = Modifier
) {
    val allPosts by vm.posts.collectAsState()
    val postsLoading by vm.postsLoading.collectAsState()
    val busy by vm.postActionBusy.collectAsState()
    val actionMessage by vm.postActionMessage.collectAsState()
    val members by vm.members.collectAsState()
    val error by vm.error.collectAsState()

    val currentUid = remember { FirebaseAuth.getInstance().currentUser?.uid.orEmpty() }

    var searchQuery by remember { mutableStateOf("") }
    var ownerFilterUid by remember(initialOwnerFilter?.uid) {
        mutableStateOf(initialOwnerFilter?.uid?.takeIf { it.isNotBlank() })
    }
    var postFilter by remember { mutableStateOf(AdminPostFilter.ALL) }
    var tagFilter by remember { mutableStateOf<String?>(null) }
    var sortOption by remember { mutableStateOf(AdminPostSortOption.NEWEST) }

    var showSortMenu by remember { mutableStateOf(false) }
    var showTagExplorer by remember { mutableStateOf(false) }
    var selectionMode by remember { mutableStateOf(false) }
    var selectedPostIds by remember { mutableStateOf<Set<String>>(emptySet()) }

    var inspectedPostId by remember { mutableStateOf<String?>(null) }
    var quickEditPost by remember { mutableStateOf<Post?>(null) }
    var deleteConfirmPost by remember { mutableStateOf<Post?>(null) }
    var showBatchTagDialog by remember { mutableStateOf(false) }
    var showBatchDeleteConfirm by remember { mutableStateOf(false) }
    var fullScreenMediaUrl by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        vm.observePosts()
    }

    val activeOwnerProfile = remember(ownerFilterUid, members, initialOwnerFilter) {
        if (ownerFilterUid.isNullOrBlank()) null
        else members.firstOrNull { it.uid == ownerFilterUid } ?: initialOwnerFilter
    }

    val authorScopedPosts = remember(allPosts, ownerFilterUid) {
        if (ownerFilterUid.isNullOrBlank()) allPosts else allPosts.filter { it.authorId == ownerFilterUid }
    }
    val tagSummaries = remember(authorScopedPosts) {
        summarizeAdminPostTags(authorScopedPosts)
    }
    val filteredPosts = remember(
        allPosts,
        searchQuery,
        ownerFilterUid,
        postFilter,
        tagFilter,
        sortOption
    ) {
        filterAndSortAdminPosts(
            posts = allPosts,
            query = searchQuery,
            ownerFilterUid = ownerFilterUid,
            filter = postFilter,
            tagFilter = tagFilter,
            sortOption = sortOption
        )
    }

    val totalCount = authorScopedPosts.size
    val mediaCount = authorScopedPosts.count { it.mediaUrl.isNotBlank() }
    val textOnlyCount = authorScopedPosts.count { it.mediaUrl.isBlank() }
    val taggedCount = authorScopedPosts.count { it.tags.any { tag -> tag.removePrefix("#").isNotBlank() } }
    val totalComments = authorScopedPosts.sumOf { it.commentsCount }

    val inspectedPost = inspectedPostId?.let { id -> allPosts.firstOrNull { it.id == id } }

    if (inspectedPost != null) {
        val ownerProfile = members.firstOrNull { it.uid == inspectedPost.authorId }
        AdminPostControlCenter(
            post = inspectedPost,
            ownerProfile = ownerProfile,
            members = members,
            vm = vm,
            permissions = permissions,
            currentUid = currentUid,
            onBack = { inspectedPostId = null },
            onOpenMemberAdmin = { target ->
                inspectedPostId = null
                onOpenMemberAdmin(target)
            },
            onFilterAuthorPosts = { uid ->
                inspectedPostId = null
                ownerFilterUid = uid
            },
            onEditPost = { quickEditPost = it },
            onDeletePost = { deleteConfirmPost = it },
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
                // Header
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
                                text = "Gönderi İşlemleri",
                                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            Text(
                                text = "${filteredPosts.size} gönderi • $mediaCount görselli • $totalComments yorum",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        IconButton(
                            onClick = { showTagExplorer = true },
                            modifier = Modifier.appleBouncy(pressedScale = 0.88f, onClick = { showTagExplorer = true })
                        ) {
                            Icon(
                                Icons.Default.Tag,
                                contentDescription = "Etiket Merkezi",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                        IconButton(
                            onClick = {
                                selectionMode = !selectionMode
                                if (!selectionMode) selectedPostIds = emptySet()
                            },
                            modifier = Modifier.appleBouncy(
                                pressedScale = 0.88f,
                                onClick = {
                                    selectionMode = !selectionMode
                                    if (!selectionMode) selectedPostIds = emptySet()
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

                // Feedback / Error Banner
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
                                Icon(
                                    Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = msg,
                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.weight(1f)
                                )
                                IconButton(
                                    onClick = { vm.clearPostActionMessage() },
                                    modifier = Modifier.size(24.dp)
                                ) {
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
                                Text(
                                    text = err,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }

                if (busy) {
                    item(key = "busy-progress") {
                        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                    }
                }

                // Active Author Filter + Direct Member Moderation Controls
                if (activeOwnerProfile != null) {
                    item(key = "owner-filter-card") {
                        val postingDisabled = activeOwnerProfile.moderation.postingDisabled
                        val commentingDisabled = activeOwnerProfile.moderation.commentingDisabled
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
                                            text = "${activeOwnerProfile.displayName.ifBlank { "Üye" }} Gönderileri",
                                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = activeOwnerProfile.handle,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    TextButton(onClick = { ownerFilterUid = null }) {
                                        Text("Tümünü Gör")
                                    }
                                }

                                if (permissions.manageBans) {
                                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text("Gönderi Paylaşma Engeli", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold))
                                            Text(
                                                if (postingDisabled) "Üye yeni gönderi paylaşamıyor" else "Üye gönderi paylaşabiliyor",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = if (postingDisabled) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        Switch(
                                            checked = postingDisabled,
                                            onCheckedChange = { vm.toggleAuthorPostingPermission(activeOwnerProfile.uid, it) }
                                        )
                                    }
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text("Yorum Yapma Engeli", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold))
                                            Text(
                                                if (commentingDisabled) "Üye yorum yazamıyor" else "Üye yorum yazabiliyor",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = if (commentingDisabled) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        Switch(
                                            checked = commentingDisabled,
                                            onCheckedChange = { vm.toggleAuthorCommentingPermission(activeOwnerProfile.uid, it) }
                                        )
                                    }
                                }

                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedButton(
                                        onClick = { onOpenMemberAdmin(activeOwnerProfile) },
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(Icons.Default.Person, null, modifier = Modifier.size(16.dp))
                                        Spacer(Modifier.width(6.dp))
                                        Text("Üye Profili")
                                    }
                                }
                            }
                        }
                    }
                }

                // Filter Summary Strip
                item(key = "summary-strip") {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        val metrics = listOf(
                            Triple(AdminPostFilter.ALL, "Tümü", totalCount),
                            Triple(AdminPostFilter.WITH_MEDIA, "Görselli", mediaCount),
                            Triple(AdminPostFilter.TEXT_ONLY, "Metin", textOnlyCount),
                            Triple(AdminPostFilter.TAGGED, "Etiketli", taggedCount)
                        )
                        items(metrics, key = { it.first.name }) { (filter, label, count) ->
                            val selected = postFilter == filter
                            val shape = RoundedCornerShape(14.dp)
                            Surface(
                                modifier = Modifier
                                    .clip(shape)
                                    .appleBouncy(pressedScale = 0.96f, onClick = {
                                        postFilter = if (selected && filter != AdminPostFilter.ALL) AdminPostFilter.ALL else filter
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

                // Search & Filters
                item(key = "search-and-filters") {
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
                            placeholder = { Text("Başlık, metin, yazar, @kullanıcı, #etiket veya ID ara…") },
                            shape = RoundedCornerShape(14.dp)
                        )

                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            item {
                                FilterChip(
                                    selected = tagFilter != null,
                                    onClick = { showTagExplorer = true },
                                    leadingIcon = { Icon(Icons.Default.Tag, null, modifier = Modifier.size(16.dp)) },
                                    label = {
                                        Text(
                                            if (tagFilter != null) "#$tagFilter"
                                            else "Etiket Merkezi (${tagSummaries.size})"
                                        )
                                    }
                                )
                            }

                            item {
                                Box {
                                    FilterChip(
                                        selected = sortOption != AdminPostSortOption.NEWEST,
                                        onClick = { showSortMenu = true },
                                        leadingIcon = { Icon(Icons.Default.Sort, null, modifier = Modifier.size(16.dp)) },
                                        label = { Text(sortOption.label) }
                                    )
                                    DropdownMenu(
                                        expanded = showSortMenu,
                                        onDismissRequest = { showSortMenu = false },
                                        containerColor = MaterialTheme.colorScheme.surface
                                    ) {
                                        AdminPostSortOption.entries.forEach { option ->
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

                            if (searchQuery.isNotBlank() || postFilter != AdminPostFilter.ALL || tagFilter != null || ownerFilterUid != null) {
                                item {
                                    FilterChip(
                                        selected = false,
                                        onClick = {
                                            searchQuery = ""
                                            postFilter = AdminPostFilter.ALL
                                            tagFilter = null
                                            ownerFilterUid = null
                                            sortOption = AdminPostSortOption.NEWEST
                                        },
                                        leadingIcon = { Icon(Icons.Default.Close, null, modifier = Modifier.size(16.dp)) },
                                        label = { Text("Sıfırla") }
                                    )
                                }
                            }
                        }
                    }
                }

                // Batch Action Bar
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
                                    Text(
                                        text = "${selectedPostIds.size} gönderi seçildi",
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                                    )
                                    Row {
                                        TextButton(onClick = {
                                            selectedPostIds = filteredPosts.map { it.id }.toSet()
                                        }) { Text("Tümünü Seç") }
                                        TextButton(onClick = { selectedPostIds = emptySet() }) {
                                            Text("Temizle")
                                        }
                                    }
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    OutlinedButton(
                                        onClick = { showBatchTagDialog = true },
                                        enabled = selectedPostIds.isNotEmpty() && !busy,
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(Icons.Default.Tag, null, modifier = Modifier.size(16.dp))
                                        Spacer(Modifier.width(6.dp))
                                        Text("Etiket Ekle")
                                    }
                                    Button(
                                        onClick = { showBatchDeleteConfirm = true },
                                        enabled = selectedPostIds.isNotEmpty() && !busy,
                                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(Icons.Default.Delete, null, modifier = Modifier.size(16.dp))
                                        Spacer(Modifier.width(6.dp))
                                        Text("Seçilenleri Sil")
                                    }
                                }
                            }
                        }
                    }
                }

                // Loading / Empty / List
                if (postsLoading && allPosts.isEmpty()) {
                    item(key = "loading") {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 48.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    }
                } else if (filteredPosts.isEmpty()) {
                    item(key = "empty") {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(18.dp),
                            color = MaterialTheme.colorScheme.surface,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(Icons.Default.Tag, null, modifier = Modifier.size(34.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("Gönderi bulunamadı", fontWeight = FontWeight.Bold)
                                Text(
                                    "Arama veya filtre kriterlerini değiştirerek tekrar dene.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                } else {
                    items(filteredPosts, key = { it.id }) { post ->
                        val selected = post.id in selectedPostIds
                        AdminPostCard(
                            post = post,
                            selectionMode = selectionMode,
                            selected = selected,
                            onToggleSelect = {
                                selectedPostIds = if (selected) selectedPostIds - post.id else selectedPostIds + post.id
                            },
                            onOpenControl = { inspectedPostId = post.id },
                            onEditTags = { quickEditPost = post },
                            onDelete = { deleteConfirmPost = post },
                            onTagClick = { tag ->
                                tagFilter = if (tagFilter.equals(tag, ignoreCase = true)) null else tag
                            },
                            onOpenMedia = { fullScreenMediaUrl = post.mediaUrl }
                        )
                    }
                }

                item(key = "bottom-spacer") {
                    Spacer(Modifier.height(24.dp))
                }
            }
        }
    }

    if (showTagExplorer) {
        PostTagExplorerSheet(
            summaries = tagSummaries,
            activeTag = tagFilter,
            onSelectTag = { chosen ->
                tagFilter = chosen
                showTagExplorer = false
            },
            onDismiss = { showTagExplorer = false }
        )
    }

    quickEditPost?.let { target ->
        QuickEditPostDialog(
            post = target,
            busy = busy,
            onDismiss = { quickEditPost = null },
            onSave = { newTitle, newTags ->
                vm.updatePostCategorization(target, newTitle, newTags) { ok ->
                    if (ok) quickEditPost = null
                }
            }
        )
    }

    deleteConfirmPost?.let { target ->
        AlertDialog(
            onDismissRequest = { if (!busy) deleteConfirmPost = null },
            containerColor = MaterialTheme.colorScheme.surface,
            title = { Text("Gönderiyi Sil", fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "\"${target.title.ifBlank { target.text.take(40) }}\" gönderisi ve medyası kalıcı olarak silinecek. Devam edilsin mi?"
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        vm.deletePostAsAdmin(target, currentUid) { ok ->
                            if (ok) {
                                if (inspectedPostId == target.id) inspectedPostId = null
                                deleteConfirmPost = null
                            }
                        }
                    },
                    enabled = !busy,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Sil")
                }
            },
            dismissButton = {
                TextButton(onClick = { deleteConfirmPost = null }, enabled = !busy) {
                    Text("İptal")
                }
            }
        )
    }

    if (showBatchTagDialog) {
        var batchTagInput by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { if (!busy) showBatchTagDialog = false },
            containerColor = MaterialTheme.colorScheme.surface,
            title = { Text("Seçili Gönderilere Etiket Ekle", fontWeight = FontWeight.Bold) },
            text = {
                OutlinedTextField(
                    value = batchTagInput,
                    onValueChange = { batchTagInput = it },
                    singleLine = true,
                    label = { Text("Etiket") },
                    placeholder = { Text("örn: edebiyat") },
                    leadingIcon = { Icon(Icons.Default.Tag, null) },
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        vm.batchAddTagToPosts(selectedPostIds, batchTagInput) { count ->
                            if (count > 0) {
                                showBatchTagDialog = false
                                selectionMode = false
                                selectedPostIds = emptySet()
                            }
                        }
                    },
                    enabled = batchTagInput.trim().removePrefix("#").isNotBlank() && !busy
                ) {
                    Text("Uygula")
                }
            },
            dismissButton = {
                TextButton(onClick = { showBatchTagDialog = false }, enabled = !busy) { Text("İptal") }
            }
        )
    }

    if (showBatchDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { if (!busy) showBatchDeleteConfirm = false },
            containerColor = MaterialTheme.colorScheme.surface,
            title = { Text("Toplu Gönderi Silme", fontWeight = FontWeight.Bold) },
            text = { Text("Seçili ${selectedPostIds.size} gönderi kalıcı olarak silinecek. Emin misin?") },
            confirmButton = {
                Button(
                    onClick = {
                        vm.batchDeletePosts(selectedPostIds, currentUid) { count ->
                            if (count > 0) {
                                showBatchDeleteConfirm = false
                                selectionMode = false
                                selectedPostIds = emptySet()
                            }
                        }
                    },
                    enabled = !busy,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Hepsini Sil")
                }
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AdminPostCard(
    post: Post,
    selectionMode: Boolean,
    selected: Boolean,
    onToggleSelect: () -> Unit,
    onOpenControl: () -> Unit,
    onEditTags: () -> Unit,
    onDelete: () -> Unit,
    onTagClick: (String) -> Unit,
    onOpenMedia: () -> Unit
) {
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
                    Checkbox(
                        checked = selected,
                        onCheckedChange = { onToggleSelect() }
                    )
                }
                UserAvatar(
                    photoUrl = post.authorPhotoUrl,
                    initials = post.authorName.take(1).uppercase(),
                    size = 38.dp
                )
                Spacer(Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = post.authorName.ifBlank { "Libra Üyesi" },
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = buildString {
                            if (post.authorUsername.isNotBlank()) append("@${post.authorUsername}")
                            if (post.createdAt > 0L) {
                                if (isNotEmpty()) append(" • ")
                                append(DateFormat.format("dd MMM HH:mm", Date(post.createdAt)).toString())
                            }
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(onClick = onEditTags, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.Edit, "Etiket/Başlık Düzenle", modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
                }
                IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.Delete, "Gönderiyi Sil", modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.error)
                }
            }

            if (post.title.isNotBlank()) {
                Text(
                    text = post.title,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            if (post.text.isNotBlank()) {
                Text(
                    text = post.text,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 4,
                    overflow = TextOverflow.Ellipsis
                )
            }

            if (post.mediaUrl.isNotBlank()) {
                val imgShape = RoundedCornerShape(14.dp)
                AsyncImage(
                    model = post.mediaUrl,
                    contentDescription = "Gönderi görseli",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp)
                        .clip(imgShape)
                        .clickable(onClick = onOpenMedia)
                )
            }

            if (post.tags.isNotEmpty()) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    post.tags.forEach { rawTag ->
                        val tag = rawTag.trim().removePrefix("#")
                        if (tag.isNotBlank()) {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                border = BorderStroke(0.8.dp, MaterialTheme.colorScheme.outlineVariant),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable { onTagClick(tag) }
                            ) {
                                Text(
                                    text = "#$tag",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            Icons.Default.FavoriteBorder,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "${post.likesCount}",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            Icons.Default.ChatBubbleOutline,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "${post.commentsCount} yorum",
                            style = MaterialTheme.typography.labelMedium,
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AdminPostControlCenter(
    post: Post,
    ownerProfile: UserProfile?,
    members: List<UserProfile>,
    vm: AdminViewModel,
    permissions: AdminPermissionSet,
    currentUid: String,
    onBack: () -> Unit,
    onOpenMemberAdmin: (UserProfile) -> Unit,
    onFilterAuthorPosts: (String) -> Unit,
    onEditPost: (Post) -> Unit,
    onDeletePost: (Post) -> Unit,
    onOpenFullScreenImage: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val commentsResult by vm.observePostComments(post.id).collectAsState(initial = AppResult.Success(emptyList()))
    val comments = (commentsResult as? AppResult.Success)?.data.orEmpty()
    val commentsError = (commentsResult as? AppResult.Error)?.error?.message
    val busy by vm.postActionBusy.collectAsState()

    var showNotifyDialog by remember { mutableStateOf(false) }
    var deleteCommentTarget by remember { mutableStateOf<PostComment?>(null) }

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
            item(key = "detail-top") {
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
                            text = "Gönderi Denetimi",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = "ID: ${post.id}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    IconButton(onClick = { onEditPost(post) }) {
                        Icon(Icons.Default.Edit, "Düzenle", tint = MaterialTheme.colorScheme.primary)
                    }
                    IconButton(onClick = { onDeletePost(post) }) {
                        Icon(Icons.Default.Delete, "Sil", tint = MaterialTheme.colorScheme.error)
                    }
                }
            }

            if (busy) {
                item(key = "detail-busy") {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                }
            }

            // Post Content Card
            item(key = "post-content") {
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
                                photoUrl = post.authorPhotoUrl,
                                initials = post.authorName.take(1).uppercase(),
                                size = 44.dp
                            )
                            Spacer(Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = post.authorName.ifBlank { "Libra Üyesi" },
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                                )
                                Text(
                                    text = buildString {
                                        if (post.authorUsername.isNotBlank()) append("@${post.authorUsername}")
                                        if (post.createdAt > 0L) {
                                            if (isNotEmpty()) append(" • ")
                                            append(DateFormat.format("dd MMM yyyy HH:mm", Date(post.createdAt)).toString())
                                        }
                                    },
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        if (post.title.isNotBlank()) {
                            Text(
                                text = post.title,
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Text(
                            text = post.text,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        if (post.mediaUrl.isNotBlank()) {
                            AsyncImage(
                                model = post.mediaUrl,
                                contentDescription = "Gönderi medyası",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(max = 260.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .clickable { onOpenFullScreenImage(post.mediaUrl) }
                            )
                        }

                        if (post.tags.isNotEmpty()) {
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                post.tags.forEach { tag ->
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = MaterialTheme.colorScheme.surfaceVariant,
                                        border = BorderStroke(0.8.dp, MaterialTheme.colorScheme.outlineVariant)
                                    ) {
                                        Text(
                                            text = "#${tag.removePrefix("#")}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                }
                            }
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(
                                onClick = { onEditPost(post) },
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.Edit, null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Başlık & Etiket")
                            }
                            Button(
                                onClick = { onDeletePost(post) },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.Delete, null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Gönderiyi Sil")
                            }
                        }
                    }
                }
            }

            // Author Moderation Card
            item(key = "author-moderation") {
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
                        Text(
                            text = "Yazar ve Etkileşim Yetkileri",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                        )
                        if (ownerProfile != null && permissions.manageBans) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Gönderi Paylaşma Engeli", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium))
                                    Text(
                                        if (ownerProfile.moderation.postingDisabled) "Kısıtlı" else "Açık",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (ownerProfile.moderation.postingDisabled) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Switch(
                                    checked = ownerProfile.moderation.postingDisabled,
                                    onCheckedChange = { vm.toggleAuthorPostingPermission(ownerProfile.uid, it) }
                                )
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Yorum Yapma Engeli", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium))
                                    Text(
                                        if (ownerProfile.moderation.commentingDisabled) "Kısıtlı" else "Açık",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (ownerProfile.moderation.commentingDisabled) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Switch(
                                    checked = ownerProfile.moderation.commentingDisabled,
                                    onCheckedChange = { vm.toggleAuthorCommentingPermission(ownerProfile.uid, it) }
                                )
                            }
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(
                                onClick = { onFilterAuthorPosts(post.authorId) },
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Tüm Gönderileri")
                            }
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
                        if (permissions.sendFeedback && post.authorId.isNotBlank()) {
                            OutlinedButton(
                                onClick = { showNotifyDialog = true },
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Default.Notifications, null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Yazara Moderasyon Bildirimi Gönder")
                            }
                        }
                    }
                }
            }

            // Comments Header
            item(key = "comments-header") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Yorumlar (${comments.size})",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    if (commentsError != null) {
                        Text(
                            text = commentsError,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }

            if (comments.isEmpty()) {
                item(key = "comments-empty") {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Text(
                            text = "Bu gönderide henüz yorum yok.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }
            } else {
                items(comments, key = { it.id }) { comment ->
                    val commentMember = members.firstOrNull { it.uid == comment.authorId }
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                UserAvatar(
                                    photoUrl = comment.authorPhotoUrl,
                                    initials = comment.authorName.take(1).uppercase(),
                                    size = 32.dp
                                )
                                Spacer(Modifier.width(8.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = comment.authorName.ifBlank { "Libra Üyesi" },
                                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = buildString {
                                            if (comment.authorUsername.isNotBlank()) append("@${comment.authorUsername}")
                                            if (comment.parentCommentId.isNotBlank()) {
                                                if (isNotEmpty()) append(" • ")
                                                append("Yanıt")
                                            }
                                        },
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                if (commentMember != null) {
                                    IconButton(
                                        onClick = { onOpenMemberAdmin(commentMember) },
                                        modifier = Modifier.size(30.dp)
                                    ) {
                                        Icon(Icons.Default.Person, "Üye Yönetimi", modifier = Modifier.size(16.dp))
                                    }
                                }
                                IconButton(
                                    onClick = { deleteCommentTarget = comment },
                                    modifier = Modifier.size(30.dp)
                                ) {
                                    Icon(Icons.Default.Delete, "Yorumu Sil", modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.error)
                                }
                            }
                            Text(
                                text = comment.text,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }

            item(key = "detail-bottom-spacer") {
                Spacer(Modifier.height(24.dp))
            }
        }
    }

    deleteCommentTarget?.let { target ->
        AlertDialog(
            onDismissRequest = { if (!busy) deleteCommentTarget = null },
            containerColor = MaterialTheme.colorScheme.surface,
            title = { Text("Yorumu Sil", fontWeight = FontWeight.Bold) },
            text = { Text("${target.authorName} tarafından yazılan yorum kalıcı olarak silinecek. Devam edilsin mi?") },
            confirmButton = {
                Button(
                    onClick = {
                        vm.deletePostCommentAsAdmin(post.id, target.id, currentUid) { ok ->
                            if (ok) deleteCommentTarget = null
                        }
                    },
                    enabled = !busy,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Sil")
                }
            },
            dismissButton = {
                TextButton(onClick = { deleteCommentTarget = null }, enabled = !busy) { Text("İptal") }
            }
        )
    }

    if (showNotifyDialog) {
        var notifTitle by remember { mutableStateOf("Gönderiniz Hakkında Bilgilendirme") }
        var notifBody by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showNotifyDialog = false },
            containerColor = MaterialTheme.colorScheme.surface,
            title = { Text("Yazara Bildirim Gönder", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = notifTitle,
                        onValueChange = { notifTitle = it },
                        label = { Text("Başlık") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = notifBody,
                        onValueChange = { notifBody = it },
                        label = { Text("Mesaj") },
                        placeholder = { Text("Gönderi veya yorum moderasyonu ile ilgili not…") },
                        minLines = 3,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        vm.sendPostModerationNotification(
                            recipientUid = post.authorId,
                            actorUid = currentUid,
                            title = notifTitle,
                            body = notifBody,
                            postId = post.id
                        ) { ok ->
                            if (ok) showNotifyDialog = false
                        }
                    },
                    enabled = notifBody.isNotBlank() && !busy
                ) {
                    Text("Gönder")
                }
            },
            dismissButton = {
                TextButton(onClick = { showNotifyDialog = false }) { Text("İptal") }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PostTagExplorerSheet(
    summaries: List<AdminPostTagSummary>,
    activeTag: String?,
    onSelectTag: (String?) -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("Etiket Merkezi", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                    Text(
                        "${summaries.size} aktif etiket • Gönderileri konuya göre filtrele",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (activeTag != null) {
                    TextButton(onClick = { onSelectTag(null) }) { Text("Filtreyi Kaldır") }
                }
            }

            if (summaries.isEmpty()) {
                Text(
                    "Henüz etiketlenmiş gönderi bulunmuyor.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 20.dp)
                )
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 380.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(summaries, key = { it.tag }) { item ->
                        val selected = activeTag.equals(item.tag, ignoreCase = true)
                        val shape = RoundedCornerShape(14.dp)
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(shape)
                                .appleBouncy(pressedScale = 0.97f, onClick = {
                                    onSelectTag(if (selected) null else item.tag)
                                }),
                            shape = shape,
                            color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                            border = BorderStroke(
                                1.dp,
                                if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
                            )
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "#${item.tag}",
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                        color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "${item.postCount} gönderi • ${item.mediaPostCount} görselli • ${item.totalLikes} beğeni • ${item.totalComments} yorum",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                if (selected) {
                                    Icon(Icons.Default.CheckCircle, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                                }
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.height(10.dp))
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun QuickEditPostDialog(
    post: Post,
    busy: Boolean,
    onDismiss: () -> Unit,
    onSave: (String, List<String>) -> Unit
) {
    var title by remember(post.id) { mutableStateOf(post.title) }
    var tagInput by remember { mutableStateOf("") }
    val tags = remember(post.id) { mutableStateListOf<String>().apply { addAll(post.tags) } }

    AlertDialog(
        onDismissRequest = { if (!busy) onDismiss() },
        containerColor = MaterialTheme.colorScheme.surface,
        title = { Text("Gönderi Başlık & Etiketleri", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { if (it.length <= 120) title = it },
                    label = { Text("Başlık") },
                    singleLine = true,
                    supportingText = { Text("${title.length}/120") },
                    modifier = Modifier.fillMaxWidth()
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = tagInput,
                        onValueChange = { tagInput = it },
                        label = { Text("Yeni Etiket") },
                        placeholder = { Text("örn: roman") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(
                        onClick = {
                            val clean = tagInput.trim().removePrefix("#")
                            if (clean.isNotBlank() && tags.none { it.equals(clean, ignoreCase = true) } && tags.size < 10) {
                                tags.add(clean)
                                tagInput = ""
                            }
                        }
                    ) {
                        Icon(Icons.Default.Add, "Etiket Ekle")
                    }
                }
                if (tags.isNotEmpty()) {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        tags.forEach { tag ->
                            InputChip(
                                selected = true,
                                onClick = { tags.remove(tag) },
                                label = { Text("#${tag.removePrefix("#")}") },
                                trailingIcon = { Icon(Icons.Default.Close, "Kaldır", modifier = Modifier.size(14.dp)) }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(title, tags.toList()) },
                enabled = !busy
            ) {
                Text("Kaydet")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !busy) { Text("İptal") }
        }
    )
}
