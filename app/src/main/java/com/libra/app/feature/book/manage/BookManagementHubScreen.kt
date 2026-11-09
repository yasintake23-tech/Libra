package com.libra.app.feature.book.manage

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FormatListNumbered
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Message
import androidx.compose.material.icons.filled.Publish
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Unarchive
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.libra.app.domain.model.Book
import com.libra.app.domain.model.BookStatus
import com.libra.app.ui.components.BookCover

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookManagementHubScreen(
    book: Book,
    viewModel: BookManagementViewModel,
    onBack: () -> Unit,
    onPreviewBook: (Book) -> Unit,
    onDeleteBook: (Book) -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.state.collectAsState()
    val currentBook = state.book ?: book
    val snackbarHostState = remember { SnackbarHostState() }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var showActionBottomSheet by remember { mutableStateOf(false) }
    var showValidationDialog by remember { mutableStateOf<List<String>?>(null) }
    var showArchivePicker by remember { mutableStateOf(false) }
    var showUnarchivePicker by remember { mutableStateOf(false) }

    LaunchedEffect(book.id) {
        viewModel.initialize(book)
    }

    LaunchedEffect(state.errorMessage, state.successMessage) {
        state.errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessages()
        }
        state.successMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessages()
        }
    }

    BackHandler { onBack() }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Kitap Yönetim Merkezi",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Geri")
                    }
                },
                actions = {
                    OutlinedButton(
                        onClick = { onPreviewBook(currentBook) },
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.MenuBook, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Önizle", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        bottomBar = {
            Surface(
                color = MaterialTheme.colorScheme.background,
                tonalElevation = 3.dp,
                shadowElevation = 8.dp
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    Button(
                        onClick = { showActionBottomSheet = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(Icons.Default.Bolt, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "Kitap İşlemleri / Hızlı Eylemler",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Hero Control Panel Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f),
                                        Color.Transparent
                                    )
                                )
                            )
                            .padding(18.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            BookCover(
                                book = currentBook,
                                modifier = Modifier
                                    .width(88.dp)
                                    .aspectRatio(0.68f),
                                cornerRadius = 12
                            )

                            Spacer(Modifier.width(16.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = if (currentBook.status == BookStatus.PUBLISHED) {
                                            Color(0xFF2E7D32).copy(alpha = 0.15f)
                                        } else {
                                            Color(0xFFF57C00).copy(alpha = 0.15f)
                                        }
                                    ) {
                                        Text(
                                            text = if (currentBook.status == BookStatus.PUBLISHED) "Yayında" else "Taslak",
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                            color = if (currentBook.status == BookStatus.PUBLISHED) Color(0xFF2E7D32) else Color(0xFFF57C00),
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                        )
                                    }

                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                                    ) {
                                        Text(
                                            text = currentBook.category.displayName,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                        )
                                    }
                                }

                                Spacer(Modifier.height(8.dp))

                                Text(
                                    text = currentBook.title.ifBlank { "İsimsiz Kitap" },
                                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )

                                Spacer(Modifier.height(2.dp))

                                Text(
                                    text = currentBook.authorName,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Spacer(Modifier.height(18.dp))

                        // Stats metrics
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            MetricItem(
                                label = "Bölüm",
                                value = "${state.chapters.size}",
                                icon = Icons.Default.FormatListNumbered,
                                modifier = Modifier.weight(1f)
                            )
                            MetricItem(
                                label = "Okunma",
                                value = "${currentBook.readCount}",
                                icon = Icons.Default.Visibility,
                                modifier = Modifier.weight(1f)
                            )
                            MetricItem(
                                label = "Beğeni",
                                value = "${currentBook.likesCount}",
                                icon = Icons.Default.Favorite,
                                modifier = Modifier.weight(1f),
                                iconTint = Color(0xFFE53935)
                            )
                        }
                    }
                }
            }

            // Section Title
            item {
                Text(
                    text = "Kitap Modülleri",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(start = 4.dp, top = 4.dp)
                )
            }

            // 1. Kitap Bilgileri
            item {
                MenuActionCard(
                    title = "1. Kitap Bilgileri",
                    description = "Kitap adı, kapak görseli, kategori, etiketler ve seri bilgileri.",
                    icon = Icons.Default.Info,
                    iconBgColor = MaterialTheme.colorScheme.primaryContainer,
                    iconTint = MaterialTheme.colorScheme.primary,
                    onClick = { viewModel.navigateToSubPage(ManagementSubPage.METADATA) }
                )
            }

            // 2. Bölüm Yönetimi
            item {
                MenuActionCard(
                    title = "2. Bölüm Yönetimi",
                    description = "${state.activeChapters.size} aktif bölüm • ${state.archivedChapters.size} arşiv • Bölüm sırala ve yönet.",
                    badge = "${state.activeChapters.size}",
                    icon = Icons.Default.FormatListNumbered,
                    iconBgColor = Color(0xFFE8F5E9),
                    iconTint = Color(0xFF2E7D32),
                    onClick = { viewModel.navigateToSubPage(ManagementSubPage.CHAPTERS) }
                )
            }

            // 3. Kitap Yazma Araçları
            item {
                MenuActionCard(
                    title = "3. Kitap Yazma Araçları",
                    description = "Yazma Stüdyosu tuvali, taslak geçmişi, canlı kelime sayacı ve bölüm yazımı.",
                    icon = Icons.Default.Edit,
                    iconBgColor = Color(0xFFEDE7F6),
                    iconTint = Color(0xFF512DA8),
                    onClick = { viewModel.navigateToSubPage(ManagementSubPage.STUDIO) }
                )
            }

            // 4. Yayınlama ve Gizlilik
            item {
                MenuActionCard(
                    title = "4. Yayınlama ve Gizlilik",
                    description = if (currentBook.status == BookStatus.PUBLISHED) "Kitap yayında • Ön kontrol listesi ve gizlilik" else "Kitap taslak • Ön kontrol listesi ve gizlilik",
                    badge = if (currentBook.status == BookStatus.PUBLISHED) "Yayında" else "Taslak",
                    icon = Icons.Default.Publish,
                    iconBgColor = Color(0xFFFFF3E0),
                    iconTint = Color(0xFFE65100),
                    onClick = { viewModel.navigateToSubPage(ManagementSubPage.PUBLISHING) }
                )
            }

            // 5. Kitap İstatistikleri
            item {
                MenuActionCard(
                    title = "5. Kitap İstatistikleri",
                    description = "Okuma analitiği, okur yorumları (${state.comments.size}), etkileşimler ve süreler.",
                    icon = Icons.Default.Analytics,
                    iconBgColor = Color(0xFFFCE4EC),
                    iconTint = Color(0xFFC2185B),
                    onClick = { viewModel.navigateToSubPage(ManagementSubPage.ANALYTICS) }
                )
            }

            // Danger Zone
            item {
                Spacer(Modifier.height(8.dp))
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showDeleteConfirmDialog = true },
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .padding(16.dp)
                            .fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.error.copy(alpha = 0.15f),
                            modifier = Modifier.size(40.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(20.dp))
                            }
                        }

                        Spacer(Modifier.width(14.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Kitabı Sil",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.error
                            )
                            Text(
                                text = "Kitabı ve tüm bölümlerini kalıcı olarak kaldırır.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            item {
                Spacer(Modifier.height(40.dp))
            }
        }
    }

    if (showActionBottomSheet) {
        ModalBottomSheet(
            onDismissRequest = { showActionBottomSheet = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp)
            ) {
                Text(
                    text = "Kitap İşlemleri",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                )
                Text(
                    text = "Kitabın genel durumunu yönetmek için bir işlem seçin.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(14.dp))

                ActionSheetItem(
                    title = "1. Tüm Kitabı Kaydet",
                    subtitle = "Tüm bölümleri ve kitap ayarlarını buluta kaydeder.",
                    icon = Icons.Default.Save,
                    tint = MaterialTheme.colorScheme.primary,
                    onClick = {
                        showActionBottomSheet = false
                        viewModel.saveAllBook()
                    }
                )

                ActionSheetItem(
                    title = "2. Kitabı Taslak Olarak Kaydet",
                    subtitle = "Kitabın durumunu taslak (DRAFT) olarak günceller.",
                    icon = Icons.Default.Edit,
                    tint = Color(0xFFF57C00),
                    onClick = {
                        showActionBottomSheet = false
                        viewModel.unpublishBook()
                    }
                )

                ActionSheetItem(
                    title = "3. Kitabı Yayınla",
                    subtitle = "Ön kontrol listesini doğrular ve okurların erişimine açar.",
                    icon = Icons.Default.Publish,
                    tint = Color(0xFF2E7D32),
                    onClick = {
                        showActionBottomSheet = false
                        val missing = viewModel.validatePrePublish()
                        if (missing.isNotEmpty()) {
                            showValidationDialog = missing
                        } else {
                            viewModel.publishBook()
                        }
                    }
                )

                ActionSheetItem(
                    title = "4. Yayından Kaldır",
                    subtitle = "Kitabı yayından çeker, okurlar artık erişemez.",
                    icon = Icons.Default.VisibilityOff,
                    tint = Color(0xFFE65100),
                    onClick = {
                        showActionBottomSheet = false
                        viewModel.unpublishBook()
                    }
                )

                ActionSheetItem(
                    title = "5. Bölümü Arşivle",
                    subtitle = "Aktif bir bölümü yayından kaldırıp arşiv klasörüne alır.",
                    icon = Icons.Default.Archive,
                    tint = Color(0xFF7E57C2),
                    onClick = {
                        showActionBottomSheet = false
                        showArchivePicker = true
                    }
                )

                ActionSheetItem(
                    title = "6. Arşivden Bölümü Geri Yükle",
                    subtitle = "Arşivlenmiş bir bölümü tekrar aktif bölümlere kazandırır.",
                    icon = Icons.Default.Unarchive,
                    tint = Color(0xFF0277BD),
                    onClick = {
                        showActionBottomSheet = false
                        showUnarchivePicker = true
                    }
                )

                ActionSheetItem(
                    title = "7. Kitap Yönetimine Dön",
                    subtitle = "İşlem yapmadan yönetim merkezine geri döner.",
                    icon = Icons.Default.Close,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    onClick = {
                        showActionBottomSheet = false
                    }
                )

                Spacer(Modifier.height(24.dp))
            }
        }
    }

    showValidationDialog?.let { missingList ->
        AlertDialog(
            onDismissRequest = { showValidationDialog = null },
            icon = { Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
            title = { Text("Yayınlama Öncesi Kontrol Listesi") },
            text = {
                Column {
                    Text(
                        text = "Kitabınızı yayınlamak için lütfen eksik maddeleri tamamlayın:",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(Modifier.height(10.dp))
                    missingList.forEach { item ->
                        Text(
                            text = "• $item",
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.error
                        )
                        Spacer(Modifier.height(4.dp))
                    }
                }
            },
            confirmButton = {
                Button(onClick = { showValidationDialog = null }) {
                    Text("Anladım")
                }
            }
        )
    }

    if (showArchivePicker) {
        val activeChapters = state.activeChapters
        AlertDialog(
            onDismissRequest = { showArchivePicker = false },
            icon = { Icon(Icons.Default.Archive, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
            title = { Text("Bölümü Arşivle") },
            text = {
                if (activeChapters.isEmpty()) {
                    Text("Arşivlenebilecek aktif bölüm bulunmuyor.")
                } else {
                    LazyColumn(modifier = Modifier.fillMaxWidth().height(220.dp)) {
                        items(activeChapters.size) { i ->
                            val ch = activeChapters[i]
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        showArchivePicker = false
                                        viewModel.archiveChapter(ch)
                                    }
                                    .padding(vertical = 4.dp),
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "${ch.chapterNumber}. ${ch.title.ifBlank { "Bölüm" }}",
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                        modifier = Modifier.weight(1f)
                                    )
                                    Text(
                                        text = "Arşivle",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showArchivePicker = false }) {
                    Text("Kapat")
                }
            }
        )
    }

    if (showUnarchivePicker) {
        val archivedChapters = state.archivedChapters
        AlertDialog(
            onDismissRequest = { showUnarchivePicker = false },
            icon = { Icon(Icons.Default.Unarchive, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
            title = { Text("Arşivden Geri Yükle") },
            text = {
                if (archivedChapters.isEmpty()) {
                    Text("Arşivde bölüm bulunmuyor.")
                } else {
                    LazyColumn(modifier = Modifier.fillMaxWidth().height(220.dp)) {
                        items(archivedChapters.size) { i ->
                            val ch = archivedChapters[i]
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        showUnarchivePicker = false
                                        viewModel.unarchiveChapter(ch)
                                    }
                                    .padding(vertical = 4.dp),
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "${ch.chapterNumber}. ${ch.title.ifBlank { "Bölüm" }}",
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                        modifier = Modifier.weight(1f)
                                    )
                                    Text(
                                        text = "Geri Yükle",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showUnarchivePicker = false }) {
                    Text("Kapat")
                }
            }
        )
    }

    if (showDeleteConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            title = { Text("Kitabı Sil") },
            text = { Text("\"${currentBook.title}\" adlı kitabı ve tüm bölümlerini kalıcı olarak silmek istediğinden emin misin? Bu işlem geri alınamaz.") },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteConfirmDialog = false
                        onDeleteBook(currentBook)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Evet, Sil")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showDeleteConfirmDialog = false }) {
                    Text("Vazgeç")
                }
            }
        )
    }
}

@Composable
private fun MetricItem(
    label: String,
    value: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    iconTint: Color = MaterialTheme.colorScheme.primary
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(18.dp))
            Spacer(Modifier.height(4.dp))
            Text(text = value, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
            Text(text = label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun MenuActionCard(
    title: String,
    description: String,
    icon: ImageVector,
    iconBgColor: Color,
    iconTint: Color,
    onClick: () -> Unit,
    badge: String? = null,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = CircleShape,
                color = iconBgColor,
                modifier = Modifier.size(44.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(22.dp))
                }
            }

            Spacer(Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    badge?.let {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primaryContainer
                        ) {
                            Text(
                                text = it,
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 1.dp)
                            )
                        }
                    }
                }
                Spacer(Modifier.height(3.dp))
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Icon(
                Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
            )
        }
    }
}

@Composable
private fun ActionSheetItem(
    title: String,
    subtitle: String,
    icon: ImageVector,
    onClick: () -> Unit,
    tint: Color = MaterialTheme.colorScheme.primary,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        color = Color.Transparent
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 12.dp, vertical = 10.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = CircleShape,
                color = tint.copy(alpha = 0.12f),
                modifier = Modifier.size(40.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(20.dp))
                }
            }
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
