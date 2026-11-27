package com.libra.app.feature.admin

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
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FormatListNumbered
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Message
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Publish
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.libra.app.domain.model.AdminPermissionSet
import com.libra.app.domain.model.Book
import com.libra.app.domain.model.BookCategory
import com.libra.app.domain.model.BookStatus
import com.libra.app.domain.model.UserProfile
import com.libra.app.feature.book.manage.ManagementSubPage
import com.libra.app.ui.animation.appleBouncy
import com.libra.app.ui.components.BookCover

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AdminBooksScreen(
    vm: AdminViewModel,
    permissions: AdminPermissionSet,
    initialOwnerFilter: UserProfile? = null,
    onBack: () -> Unit,
    onOpenBookManagement: (Book, ManagementSubPage) -> Unit,
    onPreviewBook: (Book) -> Unit,
    onCreateBook: () -> Unit,
    onOpenMemberAdmin: (UserProfile) -> Unit,
    modifier: Modifier = Modifier
) {
    val allBooks by vm.books.collectAsState()
    val booksLoading by vm.booksLoading.collectAsState()
    val members by vm.members.collectAsState()
    val actionBusy by vm.bookActionBusy.collectAsState()
    val actionMessage by vm.bookActionMessage.collectAsState()
    val error by vm.error.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }

    var query by remember { mutableStateOf("") }
    var ownerFilterUid by remember(initialOwnerFilter?.uid) { mutableStateOf(initialOwnerFilter?.uid) }
    var statusFilter by remember { mutableStateOf(AdminBookStatusFilter.ALL) }
    var categoryFilter by remember { mutableStateOf<BookCategory?>(null) }
    var visibilityFilter by remember { mutableStateOf(AdminBookVisibilityFilter.ALL) }
    var audienceFilter by remember { mutableStateOf(AdminBookAudienceFilter.ALL) }
    var sortOption by remember { mutableStateOf(AdminBookSortOption.RECENTLY_UPDATED) }

    var showSortMenu by remember { mutableStateOf(false) }
    var showVisibilityMenu by remember { mutableStateOf(false) }
    var showAudienceMenu by remember { mutableStateOf(false) }
    var showCategoryHubSheet by remember { mutableStateOf(false) }

    var selectionMode by remember { mutableStateOf(false) }
    var selectedBookIds by remember { mutableStateOf<Set<String>>(emptySet()) }
    var showBatchCategoryDialog by remember { mutableStateOf(false) }
    var showBatchStatusDialog by remember { mutableStateOf(false) }
    var showBatchVisibilityDialog by remember { mutableStateOf(false) }

    var categorizeTargetBook by remember { mutableStateOf<Book?>(null) }
    var deleteTargetBook by remember { mutableStateOf<Book?>(null) }
    var notifyTargetBook by remember { mutableStateOf<Book?>(null) }

    LaunchedEffect(actionMessage, error) {
        actionMessage?.let {
            snackbarHostState.showSnackbar(it)
            vm.clearBookActionMessage()
        }
        error?.let {
            snackbarHostState.showSnackbar(it)
            vm.clearError()
        }
    }

    val membersById = remember(members) { members.associateBy { it.uid } }
    val activeOwnerProfile = remember(ownerFilterUid, membersById, initialOwnerFilter) {
        ownerFilterUid?.let { uid -> membersById[uid] ?: initialOwnerFilter?.takeIf { it.uid == uid } }
    }

    val ownerScopedBooks = remember(allBooks, ownerFilterUid) {
        if (ownerFilterUid.isNullOrBlank()) allBooks else allBooks.filter { it.ownerId == ownerFilterUid }
    }

    val categorySummaries = remember(ownerScopedBooks) {
        summarizeAdminBookCategories(ownerScopedBooks)
    }

    val displayedBooks = remember(
        allBooks,
        query,
        ownerFilterUid,
        statusFilter,
        categoryFilter,
        visibilityFilter,
        audienceFilter,
        sortOption
    ) {
        filterAndSortAdminBooks(
            books = allBooks,
            query = query,
            ownerFilterUid = ownerFilterUid,
            statusFilter = statusFilter,
            categoryFilter = categoryFilter,
            visibilityFilter = visibilityFilter,
            audienceFilter = audienceFilter,
            sortOption = sortOption
        )
    }

    val totalCount = ownerScopedBooks.size
    val publishedCount = ownerScopedBooks.count { it.status == BookStatus.PUBLISHED }
    val draftCount = ownerScopedBooks.count { it.status == BookStatus.DRAFT }
    val archivedCount = ownerScopedBooks.count { it.status == BookStatus.ARCHIVED }
    val restrictedVisCount = ownerScopedBooks.count { !it.visibility.equals("PUBLIC", ignoreCase = true) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            if (selectionMode && selectedBookIds.isNotEmpty()) {
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${selectedBookIds.size} kitap seçildi",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                TextButton(
                                    onClick = { selectedBookIds = displayedBooks.map { it.id }.toSet() }
                                ) { Text("Tümünü Seç") }
                                TextButton(
                                    onClick = { selectedBookIds = emptySet(); selectionMode = false }
                                ) { Text("İptal") }
                            }
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = { showBatchCategoryDialog = true },
                                enabled = !actionBusy,
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp)
                            ) {
                                Text("Kategorize Et", maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                            OutlinedButton(
                                onClick = { showBatchStatusDialog = true },
                                enabled = !actionBusy,
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp)
                            ) {
                                Text("Durum", maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                            OutlinedButton(
                                onClick = { showBatchVisibilityDialog = true },
                                enabled = !actionBusy,
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp)
                            ) {
                                Text("Görünürlük", maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                        }
                    }
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(paddingValues)
        ) {
            // Top Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier.appleBouncy(pressedScale = 0.86f, onClick = onBack)
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Geri")
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (activeOwnerProfile != null) "Üye Kitapları" else "Kitap İşlemleri",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = if (activeOwnerProfile != null) {
                            "${activeOwnerProfile.displayName} (${activeOwnerProfile.handle}) • $totalCount kitap"
                        } else {
                            "$totalCount toplam kitap • $publishedCount yayında • $draftCount taslak"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                IconButton(
                    onClick = { showCategoryHubSheet = true },
                    modifier = Modifier
                        .appleBouncy(pressedScale = 0.86f, onClick = { showCategoryHubSheet = true })
                        .testTag("admin_books_categories_button")
                ) {
                    Icon(Icons.Default.Category, contentDescription = "Kategori Merkezi")
                }

                IconButton(
                    onClick = {
                        selectionMode = !selectionMode
                        if (!selectionMode) selectedBookIds = emptySet()
                    },
                    modifier = Modifier
                        .appleBouncy(pressedScale = 0.86f, onClick = {
                            selectionMode = !selectionMode
                            if (!selectionMode) selectedBookIds = emptySet()
                        })
                        .testTag("admin_books_batch_toggle")
                ) {
                    Icon(
                        Icons.Default.Checklist,
                        contentDescription = "Toplu İşlem",
                        tint = if (selectionMode) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onBackground
                    )
                }

                IconButton(
                    onClick = onCreateBook,
                    modifier = Modifier
                        .appleBouncy(pressedScale = 0.86f, onClick = onCreateBook)
                        .testTag("admin_books_create_button")
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Yeni Kitap Oluştur")
                }
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Member Filter Context + Book Publishing Moderation Card
                if (activeOwnerProfile != null) {
                    item(key = "owner-moderation-card") {
                        val publishingDisabled = activeOwnerProfile.moderation.bookPublishingDisabled
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.surface,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "Kitap Paylaşma Yetkisi",
                                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                                        )
                                        Text(
                                            text = if (publishingDisabled) {
                                                "Bu üyenin yeni kitap yayınlama yetkisi kısıtlı."
                                            } else {
                                                "Üye yeni kitap oluşturabilir ve bölüm yayınlayabilir."
                                            },
                                            style = MaterialTheme.typography.bodySmall,
                                            color = if (publishingDisabled) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    if (permissions.manageBans) {
                                        Switch(
                                            checked = !publishingDisabled,
                                            onCheckedChange = { allowed ->
                                                vm.toggleAuthorBookPublishing(activeOwnerProfile.uid, disabled = !allowed)
                                            }
                                        )
                                    }
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    OutlinedButton(
                                        onClick = { ownerFilterUid = null },
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text("Tüm Kitapları Gör")
                                    }
                                    OutlinedButton(
                                        onClick = { onOpenMemberAdmin(activeOwnerProfile) },
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text("Üye Yönetimi")
                                    }
                                }
                            }
                        }
                    }
                }

                // Search Bar
                item(key = "books-search") {
                    OutlinedTextField(
                        value = query,
                        onValueChange = { query = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("admin_books_search"),
                        placeholder = { Text("Kitap, yazar, tür, etiket (#), seri veya ID ara…") },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                        trailingIcon = {
                            if (query.isNotEmpty()) {
                                IconButton(onClick = { query = "" }) {
                                    Icon(Icons.Default.Close, contentDescription = "Temizle")
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surface,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surface
                        )
                    )
                }

                // Status Overview Strip (Clickable Filters)
                item(key = "status-strip") {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val items = listOf(
                            Triple(AdminBookStatusFilter.ALL, "Tümü", totalCount),
                            Triple(AdminBookStatusFilter.PUBLISHED, "Yayında", publishedCount),
                            Triple(AdminBookStatusFilter.DRAFT, "Taslak", draftCount),
                            Triple(AdminBookStatusFilter.ARCHIVED, "Arşiv", archivedCount),
                            Triple(AdminBookStatusFilter.RESTRICTED_VISIBILITY, "Gizli / Liste Dışı", restrictedVisCount)
                        )
                        items(items, key = { it.first.name }) { (filter, label, count) ->
                            val selected = statusFilter == filter
                            Surface(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(14.dp))
                                    .appleBouncy(pressedScale = 0.94f, onClick = { statusFilter = filter }),
                                shape = RoundedCornerShape(14.dp),
                                color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
                                border = BorderStroke(
                                    1.dp,
                                    if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
                                )
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = label,
                                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                                        color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                                    )
                                    Surface(
                                        shape = CircleShape,
                                        color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.surfaceVariant
                                    ) {
                                        Text(
                                            text = count.toString(),
                                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp),
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Category Chips Strip
                item(key = "category-strip") {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        item(key = "cat-all") {
                            FilterChip(
                                selected = categoryFilter == null,
                                onClick = { categoryFilter = null },
                                label = { Text("Tüm Kategoriler ($totalCount)") }
                            )
                        }
                        items(categorySummaries, key = { it.category.name }) { summary ->
                            FilterChip(
                                selected = categoryFilter == summary.category,
                                onClick = {
                                    categoryFilter = if (categoryFilter == summary.category) null else summary.category
                                },
                                label = { Text("${summary.category.displayName} (${summary.totalCount})") }
                            )
                        }
                    }
                }

                // Advanced Filter & Sort Row
                item(key = "advanced-filters") {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(modifier = Modifier.weight(1f)) {
                            CompactFilterSelector(
                                label = visibilityFilter.label,
                                icon = Icons.Default.Visibility,
                                active = visibilityFilter != AdminBookVisibilityFilter.ALL,
                                onClick = { showVisibilityMenu = true }
                            )
                            DropdownMenu(
                                expanded = showVisibilityMenu,
                                onDismissRequest = { showVisibilityMenu = false }
                            ) {
                                AdminBookVisibilityFilter.entries.forEach { option ->
                                    DropdownMenuItem(
                                        text = { Text(option.label) },
                                        onClick = {
                                            visibilityFilter = option
                                            showVisibilityMenu = false
                                        }
                                    )
                                }
                            }
                        }

                        Box(modifier = Modifier.weight(1f)) {
                            CompactFilterSelector(
                                label = audienceFilter.label,
                                icon = Icons.Default.Person,
                                active = audienceFilter != AdminBookAudienceFilter.ALL,
                                onClick = { showAudienceMenu = true }
                            )
                            DropdownMenu(
                                expanded = showAudienceMenu,
                                onDismissRequest = { showAudienceMenu = false }
                            ) {
                                AdminBookAudienceFilter.entries.forEach { option ->
                                    DropdownMenuItem(
                                        text = { Text(option.label) },
                                        onClick = {
                                            audienceFilter = option
                                            showAudienceMenu = false
                                        }
                                    )
                                }
                            }
                        }

                        Box(modifier = Modifier.weight(1f)) {
                            CompactFilterSelector(
                                label = sortOption.label,
                                icon = Icons.Default.Sort,
                                active = sortOption != AdminBookSortOption.RECENTLY_UPDATED,
                                onClick = { showSortMenu = true }
                            )
                            DropdownMenu(
                                expanded = showSortMenu,
                                onDismissRequest = { showSortMenu = false }
                            ) {
                                AdminBookSortOption.entries.forEach { option ->
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
                }

                // Result Count + Reset Filters
                item(key = "result-header") {
                    val hasAnyFilter = query.isNotBlank() ||
                        statusFilter != AdminBookStatusFilter.ALL ||
                        categoryFilter != null ||
                        visibilityFilter != AdminBookVisibilityFilter.ALL ||
                        audienceFilter != AdminBookAudienceFilter.ALL ||
                        sortOption != AdminBookSortOption.RECENTLY_UPDATED

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${displayedBooks.size} kitap listeleniyor",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (hasAnyFilter) {
                            TextButton(
                                onClick = {
                                    query = ""
                                    statusFilter = AdminBookStatusFilter.ALL
                                    categoryFilter = null
                                    visibilityFilter = AdminBookVisibilityFilter.ALL
                                    audienceFilter = AdminBookAudienceFilter.ALL
                                    sortOption = AdminBookSortOption.RECENTLY_UPDATED
                                },
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Text("Filtreleri Temizle")
                            }
                        }
                    }
                }

                if (booksLoading && allBooks.isEmpty()) {
                    item(key = "books-loading") {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 48.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    }
                } else if (displayedBooks.isEmpty()) {
                    item(key = "books-empty") {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(18.dp),
                            color = MaterialTheme.colorScheme.surface,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(28.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.MenuBook,
                                    contentDescription = null,
                                    modifier = Modifier.size(40.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(Modifier.height(10.dp))
                                Text(
                                    text = "Eşleşen kitap bulunamadı",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                )
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    text = "Arama veya kategori filtrelerini değiştirerek tüm kitapları görebilirsin.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(Modifier.height(12.dp))
                                Button(
                                    onClick = onCreateBook,
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text("Yeni Kitap Oluştur")
                                }
                            }
                        }
                    }
                } else {
                    items(displayedBooks, key = { it.id }) { book ->
                        val ownerProfile = membersById[book.ownerId]
                        val isSelected = book.id in selectedBookIds
                        AdminBookCard(
                            book = book,
                            ownerProfile = ownerProfile,
                            selectionMode = selectionMode,
                            selected = isSelected,
                            onToggleSelect = {
                                selectedBookIds = if (isSelected) selectedBookIds - book.id else selectedBookIds + book.id
                            },
                            onManageClick = { onOpenBookManagement(book, ManagementSubPage.HUB) },
                            onCategorizeClick = { categorizeTargetBook = book },
                            onPreviewClick = { onPreviewBook(book) },
                            onPublishClick = { vm.publishBookAsAdmin(book.id) },
                            onDraftClick = { vm.unpublishBookAsAdmin(book.id) },
                            onArchiveClick = { vm.archiveBookAsAdmin(book.id) },
                            onChangeVisibility = { vis -> vm.updateBookVisibilityAsAdmin(book, vis) },
                            onOpenAuthor = {
                                if (ownerProfile != null) {
                                    onOpenMemberAdmin(ownerProfile)
                                } else {
                                    ownerFilterUid = book.ownerId
                                }
                            },
                            onFilterByAuthor = { ownerFilterUid = book.ownerId },
                            onNotifyAuthor = { notifyTargetBook = book },
                            onDeleteClick = { deleteTargetBook = book }
                        )
                    }
                }
            }
        }
    }

    // Category Distribution & Quick Navigation Sheet
    if (showCategoryHubSheet) {
        ModalBottomSheet(
            onDismissRequest = { showCategoryHubSheet = false },
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 18.dp, vertical = 8.dp)
            ) {
                Text(
                    text = "Kategori Merkezi",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                )
                Text(
                    text = "Kitapların kategorilere göre dağılımı. Filtrelemek için bir kategoriye dokun.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(12.dp))
                LazyColumn(
                    modifier = Modifier.heightIn(max = 440.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(categorySummaries, key = { it.category.name }) { summary ->
                        val isSelected = categoryFilter == summary.category
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .appleBouncy(pressedScale = 0.97f, onClick = {
                                    categoryFilter = if (isSelected) null else summary.category
                                    showCategoryHubSheet = false
                                }),
                            shape = RoundedCornerShape(14.dp),
                            color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                            border = BorderStroke(
                                1.dp,
                                if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
                            )
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = summary.category.displayName,
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "${summary.publishedCount} yayında • ${summary.draftCount} taslak • ${summary.archivedCount} arşiv • ${summary.totalReads} okunma",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = MaterialTheme.colorScheme.surface
                                ) {
                                    Text(
                                        text = "${summary.totalCount}",
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))
            }
        }
    }

    // Quick Categorization & Tagging Sheet
    categorizeTargetBook?.let { targetBook ->
        QuickCategorizeSheet(
            book = targetBook,
            busy = actionBusy,
            onDismiss = { categorizeTargetBook = null },
            onSave = { category, genre, subGenre, tags, audience, language, isSeries, seriesName, seriesOrder ->
                vm.updateBookCategorization(
                    book = targetBook,
                    category = category,
                    genre = genre,
                    subGenre = subGenre,
                    tags = tags,
                    targetAudience = audience,
                    language = language,
                    isSeries = isSeries,
                    seriesName = seriesName,
                    seriesOrder = seriesOrder
                ) { ok ->
                    if (ok) categorizeTargetBook = null
                }
            }
        )
    }

    // Batch Categorize Dialog
    if (showBatchCategoryDialog) {
        AlertDialog(
            onDismissRequest = { showBatchCategoryDialog = false },
            containerColor = MaterialTheme.colorScheme.surface,
            title = { Text("Toplu Kategorize Et (${selectedBookIds.size})", fontWeight = FontWeight.Bold) },
            text = {
                LazyColumn(
                    modifier = Modifier.heightIn(max = 360.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(BookCategory.entries) { category ->
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .appleBouncy(pressedScale = 0.97f, onClick = {
                                    val ids = selectedBookIds
                                    showBatchCategoryDialog = false
                                    vm.batchCategorizeBooks(ids, category) {
                                        selectedBookIds = emptySet()
                                        selectionMode = false
                                    }
                                }),
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                        ) {
                            Text(
                                text = category.displayName,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showBatchCategoryDialog = false }) { Text("Kapat") }
            }
        )
    }

    // Batch Status Dialog
    if (showBatchStatusDialog) {
        AlertDialog(
            onDismissRequest = { showBatchStatusDialog = false },
            containerColor = MaterialTheme.colorScheme.surface,
            title = { Text("Toplu Durum Güncelle (${selectedBookIds.size})", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(
                        BookStatus.PUBLISHED to "Yayına Al (Published)",
                        BookStatus.DRAFT to "Taslağa Al (Draft)",
                        BookStatus.ARCHIVED to "Arşivle (Archived)"
                    ).forEach { (status, label) ->
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .appleBouncy(pressedScale = 0.97f, onClick = {
                                    val ids = selectedBookIds
                                    showBatchStatusDialog = false
                                    vm.batchSetBookStatus(ids, status) {
                                        selectedBookIds = emptySet()
                                        selectionMode = false
                                    }
                                }),
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                        ) {
                            Text(
                                text = label,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showBatchStatusDialog = false }) { Text("Kapat") }
            }
        )
    }

    // Batch Visibility Dialog
    if (showBatchVisibilityDialog) {
        AlertDialog(
            onDismissRequest = { showBatchVisibilityDialog = false },
            containerColor = MaterialTheme.colorScheme.surface,
            title = { Text("Toplu Görünürlük (${selectedBookIds.size})", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(
                        "PUBLIC" to "Herkese Açık (Public)",
                        "UNLISTED" to "Liste Dışı (Unlisted)",
                        "PRIVATE" to "Gizli (Private)"
                    ).forEach { (vis, label) ->
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .appleBouncy(pressedScale = 0.97f, onClick = {
                                    val ids = selectedBookIds
                                    showBatchVisibilityDialog = false
                                    vm.batchSetBookVisibility(ids, vis) {
                                        selectedBookIds = emptySet()
                                        selectionMode = false
                                    }
                                }),
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                        ) {
                            Text(
                                text = label,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showBatchVisibilityDialog = false }) { Text("Kapat") }
            }
        )
    }

    // Notify Author Dialog
    notifyTargetBook?.let { book ->
        AdminBookNoticeDialog(
            book = book,
            onDismiss = { notifyTargetBook = null },
            onSend = { title, body ->
                val actorUid = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid.orEmpty()
                vm.sendBookModerationNotification(
                    recipientUid = book.ownerId,
                    actorUid = actorUid,
                    title = title,
                    body = body,
                    bookId = book.id
                ) { ok ->
                    if (ok) notifyTargetBook = null
                }
            }
        )
    }

    // Delete Confirmation Dialog
    deleteTargetBook?.let { book ->
        AlertDialog(
            onDismissRequest = { deleteTargetBook = null },
            containerColor = MaterialTheme.colorScheme.surface,
            title = { Text("Kitabı Kalıcı Sil", fontWeight = FontWeight.Bold) },
            text = {
                Text("\"${book.title.ifBlank { "İsimsiz Kitap" }}\" kitabını ve tüm bölümlerini kalıcı olarak silmek istediğine emin misin? Bu işlem geri alınamaz.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        val id = book.id
                        deleteTargetBook = null
                        vm.deleteBookAsAdmin(id)
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    )
                ) { Text("Evet, Sil") }
            },
            dismissButton = {
                TextButton(onClick = { deleteTargetBook = null }) { Text("Vazgeç") }
            }
        )
    }
}

@Composable
private fun CompactFilterSelector(
    label: String,
    icon: ImageVector,
    active: Boolean,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(12.dp)
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .appleBouncy(pressedScale = 0.95f, onClick = onClick),
        shape = shape,
        color = if (active) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
        border = BorderStroke(
            1.dp,
            if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
        )
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(14.dp),
                tint = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                color = if (active) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AdminBookCard(
    book: Book,
    ownerProfile: UserProfile?,
    selectionMode: Boolean,
    selected: Boolean,
    onToggleSelect: () -> Unit,
    onManageClick: () -> Unit,
    onCategorizeClick: () -> Unit,
    onPreviewClick: () -> Unit,
    onPublishClick: () -> Unit,
    onDraftClick: () -> Unit,
    onArchiveClick: () -> Unit,
    onChangeVisibility: (String) -> Unit,
    onOpenAuthor: () -> Unit,
    onFilterByAuthor: () -> Unit,
    onNotifyAuthor: () -> Unit,
    onDeleteClick: () -> Unit
) {
    var showMoreMenu by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(18.dp)

    val (statusLabel, statusBg, statusText) = when (book.status) {
        BookStatus.PUBLISHED -> Triple("Yayında", Color(0xFFDCFCE7), Color(0xFF166534))
        BookStatus.DRAFT -> Triple("Taslak", Color(0xFFFEF3C7), Color(0xFF92400E))
        BookStatus.ARCHIVED -> Triple("Arşiv", Color(0xFFE5E7EB), Color(0xFF374151))
    }

    val visibilityLabel = when {
        book.visibility.equals("PRIVATE", ignoreCase = true) -> "Gizli"
        book.visibility.equals("UNLISTED", ignoreCase = true) -> "Liste Dışı"
        else -> "Herkese Açık"
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .appleBouncy(
                pressedScale = 0.98f,
                onClick = if (selectionMode) onToggleSelect else onManageClick
            )
            .testTag("admin_book_card_${book.id}"),
        shape = shape,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(
            if (selected) 2.dp else 1.dp,
            if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
        )
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {
                if (selectionMode) {
                    Checkbox(
                        checked = selected,
                        onCheckedChange = { onToggleSelect() },
                        modifier = Modifier.padding(end = 6.dp)
                    )
                }

                BookCover(
                    book = book,
                    modifier = Modifier
                        .width(68.dp)
                        .aspectRatio(0.68f),
                    cornerRadius = 10
                )

                Spacer(Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Surface(shape = RoundedCornerShape(6.dp), color = statusBg) {
                            Text(
                                text = statusLabel,
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp),
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = statusText
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .clickable(onClick = onCategorizeClick)
                        ) {
                            Text(
                                text = book.category.displayName,
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp),
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }

                        if (!book.visibility.equals("PUBLIC", ignoreCase = true)) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant
                            ) {
                                Text(
                                    text = visibilityLabel,
                                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(6.dp))

                    Text(
                        text = book.title.ifBlank { "İsimsiz Kitap" },
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )

                    val authorLine = buildString {
                        append(book.authorName.ifBlank { ownerProfile?.displayName ?: "Bilinmeyen Yazar" })
                        if (ownerProfile != null) {
                            append(" (${ownerProfile.handle})")
                        }
                    }
                    Text(
                        text = authorLine,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.clickable(onClick = onOpenAuthor)
                    )

                    if (book.genre.isNotBlank() || book.subGenre.isNotBlank() || book.isSeries) {
                        Spacer(Modifier.height(3.dp))
                        val metaLine = buildList {
                            if (book.genre.isNotBlank()) add(book.genre)
                            if (book.subGenre.isNotBlank()) add(book.subGenre)
                            if (book.isSeries && book.seriesName.isNotBlank()) {
                                add("Seri: ${book.seriesName} #${book.seriesOrder.coerceAtLeast(1)}")
                            }
                        }.joinToString(" • ")
                        Text(
                            text = metaLine,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    if (book.tags.isNotEmpty()) {
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = book.tags.take(4).joinToString("  ") { "#$it" },
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Box {
                    IconButton(onClick = { showMoreMenu = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "Hızlı işlemler")
                    }
                    DropdownMenu(
                        expanded = showMoreMenu,
                        onDismissRequest = { showMoreMenu = false }
                    ) {
                        if (book.status != BookStatus.PUBLISHED) {
                            DropdownMenuItem(
                                text = { Text("Yayına Al") },
                                leadingIcon = { Icon(Icons.Default.Publish, null) },
                                onClick = { showMoreMenu = false; onPublishClick() }
                            )
                        }
                        if (book.status != BookStatus.DRAFT) {
                            DropdownMenuItem(
                                text = { Text("Taslağa Al") },
                                leadingIcon = { Icon(Icons.Default.Edit, null) },
                                onClick = { showMoreMenu = false; onDraftClick() }
                            )
                        }
                        if (book.status != BookStatus.ARCHIVED) {
                            DropdownMenuItem(
                                text = { Text("Arşivle") },
                                leadingIcon = { Icon(Icons.Default.Archive, null) },
                                onClick = { showMoreMenu = false; onArchiveClick() }
                            )
                        }
                        HorizontalDivider()
                        DropdownMenuItem(
                            text = { Text("Görünürlük: Herkese Açık") },
                            leadingIcon = { Icon(Icons.Default.Public, null) },
                            onClick = { showMoreMenu = false; onChangeVisibility("PUBLIC") }
                        )
                        DropdownMenuItem(
                            text = { Text("Görünürlük: Liste Dışı") },
                            leadingIcon = { Icon(Icons.Default.VisibilityOff, null) },
                            onClick = { showMoreMenu = false; onChangeVisibility("UNLISTED") }
                        )
                        DropdownMenuItem(
                            text = { Text("Görünürlük: Gizli") },
                            leadingIcon = { Icon(Icons.Default.Lock, null) },
                            onClick = { showMoreMenu = false; onChangeVisibility("PRIVATE") }
                        )
                        HorizontalDivider()
                        DropdownMenuItem(
                            text = { Text("Yazarın Tüm Kitapları") },
                            leadingIcon = { Icon(Icons.AutoMirrored.Filled.MenuBook, null) },
                            onClick = { showMoreMenu = false; onFilterByAuthor() }
                        )
                        DropdownMenuItem(
                            text = { Text("Yazar Üye Yönetimi") },
                            leadingIcon = { Icon(Icons.Default.Person, null) },
                            onClick = { showMoreMenu = false; onOpenAuthor() }
                        )
                        DropdownMenuItem(
                            text = { Text("Yazara Bildirim Gönder") },
                            leadingIcon = { Icon(Icons.Default.Notifications, null) },
                            onClick = { showMoreMenu = false; onNotifyAuthor() }
                        )
                        HorizontalDivider()
                        DropdownMenuItem(
                            text = { Text("Kitabı Sil", color = MaterialTheme.colorScheme.error) },
                            leadingIcon = { Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error) },
                            onClick = { showMoreMenu = false; onDeleteClick() }
                        )
                    }
                }
            }

            Spacer(Modifier.height(10.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Spacer(Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${book.chapterCount} bölüm • ${book.readCount} okuma • ${book.likesCount} beğeni • ${book.commentsCount} yorum",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onManageClick,
                    modifier = Modifier
                        .weight(1.2f)
                        .height(36.dp),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "Yönet",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        maxLines = 1
                    )
                }

                OutlinedButton(
                    onClick = onCategorizeClick,
                    modifier = Modifier
                        .weight(1.2f)
                        .height(36.dp),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "Kategorize",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                        maxLines = 1
                    )
                }

                OutlinedButton(
                    onClick = onPreviewClick,
                    modifier = Modifier
                        .weight(0.9f)
                        .height(36.dp),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "Oku",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                        maxLines = 1
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun QuickCategorizeSheet(
    book: Book,
    busy: Boolean,
    onDismiss: () -> Unit,
    onSave: (
        category: BookCategory,
        genre: String,
        subGenre: String,
        tags: List<String>,
        audience: String,
        language: String,
        isSeries: Boolean,
        seriesName: String,
        seriesOrder: Int
    ) -> Unit
) {
    var selectedCategory by remember(book.id) { mutableStateOf(book.category) }
    var genre by remember(book.id) { mutableStateOf(book.genre) }
    var subGenre by remember(book.id) { mutableStateOf(book.subGenre) }
    var tagInput by remember { mutableStateOf("") }
    val tags = remember(book.id) { mutableStateListOf<String>().apply { addAll(book.tags) } }
    var audience by remember(book.id) { mutableStateOf(book.targetAudience.ifBlank { "ALL" }) }
    var language by remember(book.id) { mutableStateOf(book.language.ifBlank { "tr" }) }
    var isSeries by remember(book.id) { mutableStateOf(book.isSeries) }
    var seriesName by remember(book.id) { mutableStateOf(book.seriesName) }
    var seriesOrder by remember(book.id) { mutableStateOf(book.seriesOrder.coerceAtLeast(1).toString()) }

    ModalBottomSheet(
        onDismissRequest = { if (!busy) onDismiss() },
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Kategorize Et & Sınıflandır",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
            )
            Text(
                text = book.title.ifBlank { "İsimsiz Kitap" },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Text("Ana Kategori", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                BookCategory.entries.forEach { cat ->
                    FilterChip(
                        selected = selectedCategory == cat,
                        onClick = { selectedCategory = cat },
                        label = { Text(cat.displayName) }
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = genre,
                    onValueChange = { genre = it.take(40) },
                    label = { Text("Tür") },
                    placeholder = { Text("Örn: Roman") },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                )
                OutlinedTextField(
                    value = subGenre,
                    onValueChange = { subGenre = it.take(40) },
                    label = { Text("Alt Tür") },
                    placeholder = { Text("Örn: Distopya") },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                )
            }

            Text("Etiketler", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = tagInput,
                    onValueChange = { tagInput = it },
                    placeholder = { Text("Etiket ekle…") },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                )
                Spacer(Modifier.width(8.dp))
                Button(
                    onClick = {
                        val clean = tagInput.trim().removePrefix("#")
                        if (clean.isNotBlank() && !tags.contains(clean)) {
                            tags.add(clean)
                            tagInput = ""
                        }
                    },
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Ekle")
                }
            }

            if (tags.isNotEmpty()) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    tags.forEach { tag ->
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = MaterialTheme.colorScheme.primaryContainer
                        ) {
                            Row(
                                modifier = Modifier.padding(start = 10.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("#$tag", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onPrimaryContainer)
                                IconButton(
                                    onClick = { tags.remove(tag) },
                                    modifier = Modifier.size(20.dp)
                                ) {
                                    Icon(Icons.Default.Close, contentDescription = "Sil", modifier = Modifier.size(12.dp))
                                }
                            }
                        }
                    }
                }
            }

            Text("Hedef Kitle", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(
                    Triple("Herkes İçin", listOf("ALL", "Genel Okur", "Herkes İçin"), "Herkes İçin"),
                    Triple("Genç Yetişkin (13+)", listOf("YA", "Genç Yetişkin", "Genç Yetişkin (13+)"), "Genç Yetişkin (13+)"),
                    Triple("Yetişkin (18+)", listOf("ADULT", "Yetişkin", "Yetişkin (18+)"), "Yetişkin (18+)")
                ).forEach { (value, aliases, label) ->
                    FilterChip(
                        selected = aliases.any { it.equals(audience, ignoreCase = true) },
                        onClick = { audience = value },
                        label = { Text(label) }
                    )
                }
            }

            Text("Dil", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(
                    Triple("Türkçe", "tr", "Türkçe"),
                    Triple("English", "en", "English"),
                    Triple("Deutsch", "de", "Deutsch"),
                    Triple("Français", "fr", "Français"),
                    Triple("Español", "es", "Español"),
                    Triple("Diğer", "other", "Diğer")
                ).forEach { (value, legacyCode, label) ->
                    FilterChip(
                        selected = language.equals(value, ignoreCase = true) ||
                            language.equals(legacyCode, ignoreCase = true),
                        onClick = { language = value },
                        label = { Text(label) }
                    )
                }
            }

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Seri Parçası", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
                        Switch(checked = isSeries, onCheckedChange = { isSeries = it })
                    }
                    if (isSeries) {
                        Spacer(Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = seriesName,
                                onValueChange = { seriesName = it },
                                label = { Text("Seri Adı") },
                                singleLine = true,
                                modifier = Modifier.weight(2f),
                                shape = RoundedCornerShape(10.dp)
                            )
                            OutlinedTextField(
                                value = seriesOrder,
                                onValueChange = { seriesOrder = it.filter(Char::isDigit).take(3) },
                                label = { Text("Sıra") },
                                singleLine = true,
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp)
                            )
                        }
                    }
                }
            }

            Button(
                onClick = {
                    onSave(
                        selectedCategory,
                        genre,
                        subGenre,
                        tags.toList(),
                        audience,
                        language,
                        isSeries,
                        seriesName,
                        seriesOrder.toIntOrNull() ?: 1
                    )
                },
                enabled = !busy,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp)
            ) {
                Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text(if (busy) "Kaydediliyor…" else "Kategorizasyonu Kaydet")
            }

            Spacer(Modifier.height(10.dp))
        }
    }
}

@Composable
fun AdminBookControlCenter(
    book: Book,
    ownerProfile: UserProfile?,
    vm: AdminViewModel,
    permissions: AdminPermissionSet,
    onBack: () -> Unit,
    onOpenSubModule: (ManagementSubPage) -> Unit,
    onPreviewBook: (Book) -> Unit,
    onOpenMemberAdmin: (UserProfile) -> Unit,
    onFilterAuthorBooks: (String) -> Unit,
    onDeleted: () -> Unit,
    modifier: Modifier = Modifier
) {
    val allBooks by vm.books.collectAsState()
    val currentBook = remember(allBooks, book) { allBooks.firstOrNull { it.id == book.id } ?: book }
    val actionBusy by vm.bookActionBusy.collectAsState()
    val actionMessage by vm.bookActionMessage.collectAsState()
    val error by vm.error.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    var showCategorizeSheet by remember { mutableStateOf(false) }
    var showNotifyDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    LaunchedEffect(actionMessage, error) {
        actionMessage?.let {
            snackbarHostState.showSnackbar(it)
            vm.clearBookActionMessage()
        }
        error?.let {
            snackbarHostState.showSnackbar(it)
            vm.clearError()
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(paddingValues)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier.appleBouncy(pressedScale = 0.86f, onClick = onBack)
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Geri")
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Kitap Kontrol Merkezi",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                    )
                    Text(
                        text = currentBook.title.ifBlank { "İsimsiz Kitap" },
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                OutlinedButton(
                    onClick = { onPreviewBook(currentBook) },
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    modifier = Modifier.padding(end = 8.dp)
                ) {
                    Icon(Icons.AutoMirrored.Filled.MenuBook, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Oku / Önizle", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
                }
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Overview Header Card
                item(key = "control-hero") {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                BookCover(
                                    book = currentBook,
                                    modifier = Modifier
                                        .width(84.dp)
                                        .aspectRatio(0.68f),
                                    cornerRadius = 12
                                )
                                Spacer(Modifier.width(14.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = currentBook.title.ifBlank { "İsimsiz Kitap" },
                                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Spacer(Modifier.height(2.dp))
                                    Text(
                                        text = "Yazar: ${currentBook.authorName.ifBlank { ownerProfile?.displayName ?: "Bilinmiyor" }}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(Modifier.height(6.dp))
                                    Text(
                                        text = "${currentBook.category.displayName} • ${currentBook.chapterCount} bölüm • ${currentBook.readCount} okuma • ${currentBook.likesCount} beğeni",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(Modifier.height(4.dp))
                                    Text(
                                        text = "ID: ${currentBook.id}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            Spacer(Modifier.height(14.dp))
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                            Spacer(Modifier.height(12.dp))

                            Text(
                                text = "Hızlı Durum Denetimi",
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                QuickStatusToggleChip(
                                    label = "Yayında",
                                    selected = currentBook.status == BookStatus.PUBLISHED,
                                    enabled = !actionBusy,
                                    onClick = { vm.publishBookAsAdmin(currentBook.id) },
                                    modifier = Modifier.weight(1f)
                                )
                                QuickStatusToggleChip(
                                    label = "Taslak",
                                    selected = currentBook.status == BookStatus.DRAFT,
                                    enabled = !actionBusy,
                                    onClick = { vm.unpublishBookAsAdmin(currentBook.id) },
                                    modifier = Modifier.weight(1f)
                                )
                                QuickStatusToggleChip(
                                    label = "Arşiv",
                                    selected = currentBook.status == BookStatus.ARCHIVED,
                                    enabled = !actionBusy,
                                    onClick = { vm.archiveBookAsAdmin(currentBook.id) },
                                    modifier = Modifier.weight(1f)
                                )
                            }

                            Spacer(Modifier.height(10.dp))
                            Text(
                                text = "Görünürlük",
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(Modifier.height(6.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                listOf("PUBLIC" to "Herkese Açık", "UNLISTED" to "Liste Dışı", "PRIVATE" to "Gizli").forEach { (vis, label) ->
                                    QuickStatusToggleChip(
                                        label = label,
                                        selected = currentBook.visibility.equals(vis, ignoreCase = true),
                                        enabled = !actionBusy,
                                        onClick = { vm.updateBookVisibilityAsAdmin(currentBook, vis) },
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }
                    }
                }

                // Category 1: Metadata & Categorization
                item(key = "cat-metadata") {
                    ControlModuleSection(title = "Temel Bilgiler ve Kategorizasyon") {
                        val audienceLabel = when {
                            currentBook.targetAudience.isBlank() ||
                                currentBook.targetAudience.equals("ALL", ignoreCase = true) ||
                                currentBook.targetAudience.equals("Genel Okur", ignoreCase = true) -> "Herkes İçin"
                            currentBook.targetAudience.equals("YA", ignoreCase = true) -> "Genç Yetişkin (13+)"
                            currentBook.targetAudience.equals("ADULT", ignoreCase = true) -> "Yetişkin (18+)"
                            else -> currentBook.targetAudience
                        }
                        ControlModuleRow(
                            icon = Icons.Default.Category,
                            title = "Hızlı Kategorize Et & Etiketle",
                            subtitle = "${currentBook.category.displayName} • Hedef: $audienceLabel",
                            onClick = { showCategorizeSheet = true }
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        ControlModuleRow(
                            icon = Icons.Default.Info,
                            title = "Kitap Bilgileri & Kapak Editörü",
                            subtitle = "Başlık, açıklama, keşfet özeti, kapak görseli ve seri ayarları",
                            onClick = { onOpenSubModule(ManagementSubPage.METADATA) }
                        )
                    }
                }

                // Category 2: Chapters & Studio
                item(key = "cat-chapters") {
                    ControlModuleSection(title = "İçerik ve Bölüm Araçları") {
                        ControlModuleRow(
                            icon = Icons.Default.FormatListNumbered,
                            title = "Bölüm Yönetimi",
                            subtitle = "Bölümleri ekle, sırala, çoğalt, yayınla veya arşivle",
                            onClick = { onOpenSubModule(ManagementSubPage.CHAPTERS) }
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        ControlModuleRow(
                            icon = Icons.Default.Edit,
                            title = "Yazma Stüdyosu",
                            subtitle = "Bölüm metinlerini ve görsellerini tam editörde düzenle",
                            onClick = { onOpenSubModule(ManagementSubPage.STUDIO) }
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        ControlModuleRow(
                            icon = Icons.Default.History,
                            title = "Taslak & Bulut Senkronizasyonu",
                            subtitle = "Kayıt geçmişi ve Firebase RTDB / R2 senkronizasyon durumu",
                            onClick = { onOpenSubModule(ManagementSubPage.DRAFTS) }
                        )
                    }
                }

                // Category 3: Publishing, Analytics & Moderation
                item(key = "cat-moderation") {
                    ControlModuleSection(title = "Yayın, Analitik ve Okur Moderasyonu") {
                        ControlModuleRow(
                            icon = Icons.Default.Publish,
                            title = "Yayınlama Kontrol Listesi & İçerik Uyarıları",
                            subtitle = "Ön kontrol doğrulaması, görünürlük ve hassas içerik etiketleri",
                            onClick = { onOpenSubModule(ManagementSubPage.PUBLISHING) }
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        ControlModuleRow(
                            icon = Icons.Default.Analytics,
                            title = "Kitap İstatistikleri",
                            subtitle = "Okunma, beğeni, kütüphane kaydı ve bölüm bazlı okuma süreleri",
                            onClick = { onOpenSubModule(ManagementSubPage.ANALYTICS) }
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        ControlModuleRow(
                            icon = Icons.Default.Message,
                            title = "Okur Yorumları Moderasyonu",
                            subtitle = "${currentBook.commentsCount} yorumu incele ve uygunsuz olanları sil",
                            onClick = { onOpenSubModule(ManagementSubPage.COMMENTS) }
                        )
                    }
                }

                // Category 4: Author & Danger Zone
                item(key = "cat-author") {
                    ControlModuleSection(title = "Yazar ve Yönetim İşlemleri") {
                        ControlModuleRow(
                            icon = Icons.AutoMirrored.Filled.MenuBook,
                            title = "Yazarın Diğer Kitapları",
                            subtitle = "${currentBook.authorName.ifBlank { "Yazar" }} tarafından oluşturulan tüm kitaplar",
                            onClick = { onFilterAuthorBooks(currentBook.ownerId) }
                        )
                        if (ownerProfile != null) {
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                            ControlModuleRow(
                                icon = Icons.Default.Person,
                                title = "Yazar Hesabını Yönet",
                                subtitle = "${ownerProfile.displayName} (${ownerProfile.handle}) • Üye işlemleri",
                                onClick = { onOpenMemberAdmin(ownerProfile) }
                            )
                            if (permissions.manageBans) {
                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                                val disabled = ownerProfile.moderation.bookPublishingDisabled
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 14.dp, vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Lock,
                                        contentDescription = null,
                                        tint = if (disabled) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.size(22.dp)
                                    )
                                    Spacer(Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "Yazar Kitap Paylaşma İzni",
                                            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium)
                                        )
                                        Text(
                                            text = if (disabled) "Kısıtlı (Yeni kitap paylaşamaz)" else "Açık",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = if (disabled) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Switch(
                                        checked = !disabled,
                                        onCheckedChange = { allowed ->
                                            vm.toggleAuthorBookPublishing(ownerProfile.uid, disabled = !allowed)
                                        }
                                    )
                                }
                            }
                        }
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        ControlModuleRow(
                            icon = Icons.Default.Notifications,
                            title = "Yazara Bildirim / Uyarı Gönder",
                            subtitle = "Kitap durumu veya kategori düzenlemesi hakkında bilgi ver",
                            onClick = { showNotifyDialog = true }
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        ControlModuleRow(
                            icon = Icons.Default.Delete,
                            title = "Kitabı Kalıcı Sil",
                            subtitle = "Kitap ve tüm bölümlerini veritabanından tamamen kaldırır",
                            tint = MaterialTheme.colorScheme.error,
                            onClick = { showDeleteDialog = true }
                        )
                    }
                }
            }
        }
    }

    if (showCategorizeSheet) {
        QuickCategorizeSheet(
            book = currentBook,
            busy = actionBusy,
            onDismiss = { showCategorizeSheet = false },
            onSave = { category, genre, subGenre, tags, audience, language, isSeries, seriesName, seriesOrder ->
                vm.updateBookCategorization(
                    book = currentBook,
                    category = category,
                    genre = genre,
                    subGenre = subGenre,
                    tags = tags,
                    targetAudience = audience,
                    language = language,
                    isSeries = isSeries,
                    seriesName = seriesName,
                    seriesOrder = seriesOrder
                ) { ok ->
                    if (ok) showCategorizeSheet = false
                }
            }
        )
    }

    if (showNotifyDialog) {
        AdminBookNoticeDialog(
            book = currentBook,
            onDismiss = { showNotifyDialog = false },
            onSend = { title, body ->
                val actorUid = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid.orEmpty()
                vm.sendBookModerationNotification(
                    recipientUid = currentBook.ownerId,
                    actorUid = actorUid,
                    title = title,
                    body = body,
                    bookId = currentBook.id
                ) { ok ->
                    if (ok) showNotifyDialog = false
                }
            }
        )
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            containerColor = MaterialTheme.colorScheme.surface,
            title = { Text("Kitabı Kalıcı Sil", fontWeight = FontWeight.Bold) },
            text = {
                Text("\"${currentBook.title}\" kitabını ve tüm bölümlerini kalıcı olarak silmek istediğine emin misin?")
            },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteDialog = false
                        vm.deleteBookAsAdmin(currentBook.id) { ok ->
                            if (ok) onDeleted()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    )
                ) { Text("Evet, Sil") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text("Vazgeç") }
            }
        )
    }
}

@Composable
private fun QuickStatusToggleChip(
    label: String,
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(10.dp)
    Surface(
        modifier = modifier
            .clip(shape)
            .appleBouncy(enabled = enabled, pressedScale = 0.94f, onClick = onClick),
        shape = shape,
        color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
        border = BorderStroke(
            1.dp,
            if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
        )
    ) {
        Row(
            modifier = Modifier.padding(vertical = 8.dp, horizontal = 6.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (selected) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                    tint = MaterialTheme.colorScheme.onPrimary
                )
                Spacer(Modifier.width(4.dp))
            }
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun ControlModuleSection(
    title: String,
    content: @Composable () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp)
        )
        val shape = RoundedCornerShape(16.dp)
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clip(shape),
            shape = shape,
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                content()
            }
        }
    }
}

@Composable
private fun ControlModuleRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    tint: Color = MaterialTheme.colorScheme.onSurface
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .appleBouncy(pressedScale = 0.97f, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(22.dp)
        )
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                color = tint
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
        Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun AdminBookNoticeDialog(
    book: Book,
    onDismiss: () -> Unit,
    onSend: (title: String, body: String) -> Unit
) {
    var title by remember(book.id) { mutableStateOf("Kitabın hakkında: ${book.title}") }
    var body by remember(book.id) { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        title = { Text("Yazara Bildirim Gönder", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Alıcı: ${book.authorName.ifBlank { book.ownerId }}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it.take(100) },
                    label = { Text("Başlık") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = body,
                    onValueChange = { body = it.take(500) },
                    label = { Text("Mesaj") },
                    placeholder = { Text("Kategori, kapak veya yayın durumu hakkında not…") },
                    minLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onSend(title, body) },
                enabled = body.trim().isNotBlank()
            ) { Text("Gönder") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Vazgeç") }
        }
    )
}
