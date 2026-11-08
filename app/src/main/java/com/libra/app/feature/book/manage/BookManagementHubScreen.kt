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
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FormatListNumbered
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Message
import androidx.compose.material.icons.filled.Publish
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
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
                    description = "${state.chapters.size} bölüm • Bölüm ekle, sırala, yayınla veya çoğalt.",
                    badge = "${state.chapters.size}",
                    icon = Icons.Default.FormatListNumbered,
                    iconBgColor = Color(0xFFE8F5E9),
                    iconTint = Color(0xFF2E7D32),
                    onClick = { viewModel.navigateToSubPage(ManagementSubPage.CHAPTERS) }
                )
            }

            // 3. Yazma Stüdyosu
            item {
                MenuActionCard(
                    title = "3. Yazma Stüdyosu",
                    description = "Geniş yazma tuvali, canlı kelime sayacı ve otomatik taslak kaydı.",
                    icon = Icons.Default.Edit,
                    iconBgColor = Color(0xFFEDE7F6),
                    iconTint = Color(0xFF512DA8),
                    onClick = { viewModel.navigateToSubPage(ManagementSubPage.STUDIO) }
                )
            }

            // 4. Yayınlama Ayarları
            item {
                MenuActionCard(
                    title = "4. Yayınlama Ayarları",
                    description = if (currentBook.status == BookStatus.PUBLISHED) "Kitap yayında • Güncellemeleri yayınla" else "Kitap taslak • Yayınlama kontrol listesi",
                    icon = Icons.Default.Publish,
                    iconBgColor = Color(0xFFFFF3E0),
                    iconTint = Color(0xFFE65100),
                    onClick = { viewModel.navigateToSubPage(ManagementSubPage.PUBLISHING) }
                )
            }

            // 5. Gizlilik ve Görünürlük
            item {
                MenuActionCard(
                    title = "5. Gizlilik ve Görünürlük",
                    description = "Herkese Açık, Liste Dışı veya Gizli erişim ve içerik uyarıları.",
                    icon = Icons.Default.Lock,
                    iconBgColor = Color(0xFFE1F5FE),
                    iconTint = Color(0xFF0277BD),
                    onClick = { viewModel.navigateToSubPage(ManagementSubPage.PRIVACY) }
                )
            }

            // 6. İstatistikler
            item {
                MenuActionCard(
                    title = "6. İstatistikler",
                    description = "Okuma analitiği, okur etkileşimleri ve toplam okuma süresi.",
                    icon = Icons.Default.Analytics,
                    iconBgColor = Color(0xFFFCE4EC),
                    iconTint = Color(0xFFC2185B),
                    onClick = { viewModel.navigateToSubPage(ManagementSubPage.ANALYTICS) }
                )
            }

            // 7. Kitap Yorumları
            item {
                MenuActionCard(
                    title = "7. Kitap Yorumları",
                    description = "${state.comments.size} okur yorumu • İncele ve yönet.",
                    badge = if (state.comments.isNotEmpty()) "${state.comments.size}" else null,
                    icon = Icons.Default.Message,
                    iconBgColor = Color(0xFFE0F2F1),
                    iconTint = Color(0xFF00796B),
                    onClick = { viewModel.navigateToSubPage(ManagementSubPage.COMMENTS) }
                )
            }

            // 8. Taslaklar ve Sürüm Geçmişi
            item {
                MenuActionCard(
                    title = "8. Taslaklar ve Sürüm Geçmişi",
                    description = "Bulut senkronizasyon durumu ve son kayıt zamanları.",
                    icon = Icons.Default.History,
                    iconBgColor = MaterialTheme.colorScheme.surfaceVariant,
                    iconTint = MaterialTheme.colorScheme.onSurfaceVariant,
                    onClick = { viewModel.navigateToSubPage(ManagementSubPage.DRAFTS) }
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
        }
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
