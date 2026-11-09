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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Publish
import androidx.compose.material.icons.filled.Unarchive
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.libra.app.domain.model.Chapter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChapterManagementScreen(
    viewModel: BookManagementViewModel,
    onBack: () -> Unit,
    onOpenStudio: (Chapter) -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.state.collectAsState()
    val chapters = state.chapters
    val snackbarHostState = remember { SnackbarHostState() }
    var chapterToDelete by remember { mutableStateOf<Chapter?>(null) }
    var showNewChapterDialog by remember { mutableStateOf(false) }
    var newChapterTitle by remember { mutableStateOf("") }

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

    var selectedTab by remember { mutableStateOf(0) }
    val activeChapters = state.activeChapters
    val archivedChapters = state.archivedChapters
    val displayedChapters = if (selectedTab == 0) activeChapters else archivedChapters

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Bölüm Yönetimi",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = "${activeChapters.size} aktif • ${archivedChapters.size} arşiv • ${chapters.sumOf { it.wordCount }} toplam kelime",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Geri")
                    }
                },
                actions = {
                    IconButton(onClick = { showNewChapterDialog = true }) {
                        Icon(Icons.Default.Add, contentDescription = "Yeni Bölüm")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showNewChapterDialog = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(Icons.Default.Add, contentDescription = "Yeni Bölüm Ekle")
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = MaterialTheme.colorScheme.surface
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Aktif Bölümler (${activeChapters.size})") }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Arşivdekiler (${archivedChapters.size})") }
                )
            }

            if (state.isLoading && chapters.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (displayedChapters.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = if (selectedTab == 0) "Henüz aktif bölüm bulunmuyor" else "Arşivde bölüm bulunmuyor",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = if (selectedTab == 0) {
                                "Kitabının ilk bölümünü oluşturarak yazmaya başla."
                            } else {
                                "Yayından kaldırdığınız veya saklamak istediğiniz bölümleri arşivleyebilirsiniz."
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (selectedTab == 0) {
                            Spacer(Modifier.height(16.dp))
                            Button(onClick = { showNewChapterDialog = true }) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("İlk Bölümü Oluştur")
                            }
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    itemsIndexed(displayedChapters, key = { _, ch -> ch.id }) { index, chapter ->
                        ChapterManagementCard(
                            chapter = chapter,
                            isFirst = index == 0,
                            isLast = index == displayedChapters.size - 1,
                            onOpenStudio = { onOpenStudio(chapter) },
                            onMoveUp = { viewModel.moveChapterUp(chapter) },
                            onMoveDown = { viewModel.moveChapterDown(chapter) },
                            onTogglePublish = { viewModel.toggleChapterPublishStatus(chapter) },
                            onDuplicate = { viewModel.duplicateChapter(chapter) },
                            onArchive = { viewModel.archiveChapter(chapter) },
                            onUnarchive = { viewModel.unarchiveChapter(chapter) },
                            onDelete = { chapterToDelete = chapter }
                        )
                    }

                    item {
                        Spacer(Modifier.height(72.dp))
                    }
                }
            }
        }
    }

    if (showNewChapterDialog) {
        val nextNumber = (chapters.maxOfOrNull { it.chapterNumber } ?: 0) + 1
        AlertDialog(
            onDismissRequest = { showNewChapterDialog = false },
            title = { Text("Yeni Bölüm Ekle") },
            text = {
                Column {
                    Text(
                        text = "$nextNumber. bölüm için bir başlık belirle (isteğe bağlı):",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(10.dp))
                    androidx.compose.material3.OutlinedTextField(
                        value = newChapterTitle,
                        onValueChange = { newChapterTitle = it },
                        placeholder = { Text("$nextNumber. Bölüm") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val titleToUse = newChapterTitle.trim().ifBlank { "$nextNumber. Bölüm" }
                        showNewChapterDialog = false
                        newChapterTitle = ""
                        viewModel.createNewChapter(titleToUse)
                    }
                ) {
                    Text("Oluştur")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showNewChapterDialog = false; newChapterTitle = "" }) {
                    Text("Vazgeç")
                }
            }
        )
    }

    chapterToDelete?.let { chapter ->
        AlertDialog(
            onDismissRequest = { chapterToDelete = null },
            title = { Text("Bölümü Sil") },
            text = { Text("\"${chapter.title}\" adlı bölümü silmek istediğinden emin misin? Bu işlem geri alınamaz.") },
            confirmButton = {
                Button(
                    onClick = {
                        val toDelete = chapter
                        chapterToDelete = null
                        viewModel.deleteChapter(toDelete)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Sil")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { chapterToDelete = null }) {
                    Text("Vazgeç")
                }
            }
        )
    }
}

@Composable
private fun ChapterManagementCard(
    chapter: Chapter,
    isFirst: Boolean,
    isLast: Boolean,
    onOpenStudio: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onTogglePublish: () -> Unit,
    onDuplicate: () -> Unit,
    onArchive: () -> Unit = {},
    onUnarchive: () -> Unit = {},
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    var menuExpanded by remember { mutableStateOf(false) }
    val dateFormat = remember { SimpleDateFormat("dd MMM yyyy, HH:mm", Locale("tr")) }
    val formattedDate = remember(chapter.updatedAt) {
        if (chapter.updatedAt > 0) dateFormat.format(Date(chapter.updatedAt)) else ""
    }
    val estimatedMinutes = (chapter.wordCount / 200).coerceAtLeast(1)

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onOpenStudio),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(14.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Chapter Number badge
            Surface(
                shape = CircleShape,
                color = if (chapter.isArchived) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f),
                modifier = Modifier.size(38.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = "${chapter.chapterNumber}",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = if (chapter.isArchived) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val badgeColor = when {
                        chapter.isArchived -> Color(0xFF7E57C2)
                        chapter.isPublished -> Color(0xFF2E7D32)
                        else -> Color(0xFFF57C00)
                    }
                    val badgeText = when {
                        chapter.isArchived -> "Arşivde"
                        chapter.isPublished -> "Yayında"
                        else -> "Taslak"
                    }
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = badgeColor.copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = badgeText,
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = badgeColor,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }

                    Text(
                        text = "${chapter.wordCount} kelime • ~$estimatedMinutes dk okuma",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(Modifier.height(4.dp))

                Text(
                    text = chapter.title.ifBlank { "${chapter.chapterNumber}. Bölüm" },
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                if (formattedDate.isNotBlank()) {
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = "Son güncelleme: $formattedDate",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
            }

            // Quick reordering or Quick unarchive
            if (!chapter.isArchived) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    IconButton(
                        onClick = onMoveUp,
                        enabled = !isFirst,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            Icons.Default.ArrowUpward,
                            contentDescription = "Yukarı Taşı",
                            tint = if (!isFirst) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    IconButton(
                        onClick = onMoveDown,
                        enabled = !isLast,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            Icons.Default.ArrowDownward,
                            contentDescription = "Aşağı Taşı",
                            tint = if (!isLast) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            } else {
                TextButton(
                    onClick = onUnarchive,
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Icon(Icons.Default.Unarchive, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Geri Yükle", style = MaterialTheme.typography.labelSmall)
                }
            }

            // Actions dropdown menu
            Box {
                IconButton(onClick = { menuExpanded = true }) {
                    Icon(Icons.Default.MoreVert, contentDescription = "Bölüm Seçenekleri")
                }

                DropdownMenu(
                    expanded = menuExpanded,
                    onDismissRequest = { menuExpanded = false }
                ) {
                    if (chapter.isArchived) {
                        DropdownMenuItem(
                            text = { Text("Arşivden Geri Yükle") },
                            leadingIcon = { Icon(Icons.Default.Unarchive, contentDescription = null) },
                            onClick = {
                                menuExpanded = false
                                onUnarchive()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Yazma Stüdyosunda Aç") },
                            leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) },
                            onClick = {
                                menuExpanded = false
                                onOpenStudio()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Kalıcı Olarak Sil", color = MaterialTheme.colorScheme.error) },
                            leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                            onClick = {
                                menuExpanded = false
                                onDelete()
                            }
                        )
                    } else {
                        DropdownMenuItem(
                            text = { Text("Yazma Stüdyosunda Aç") },
                            leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) },
                            onClick = {
                                menuExpanded = false
                                onOpenStudio()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text(if (chapter.isPublished) "Taslağa Al" else "Yayınla") },
                            leadingIcon = {
                                Icon(
                                    if (chapter.isPublished) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = null
                                )
                            },
                            onClick = {
                                menuExpanded = false
                                onTogglePublish()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Bölümü Çoğalt") },
                            leadingIcon = { Icon(Icons.Default.ContentCopy, contentDescription = null) },
                            onClick = {
                                menuExpanded = false
                                onDuplicate()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Bölümü Arşivle") },
                            leadingIcon = { Icon(Icons.Default.Archive, contentDescription = null) },
                            onClick = {
                                menuExpanded = false
                                onArchive()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Bölümü Sil", color = MaterialTheme.colorScheme.error) },
                            leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                            onClick = {
                                menuExpanded = false
                                onDelete()
                            }
                        )
                    }
                }
            }
        }
    }
}
