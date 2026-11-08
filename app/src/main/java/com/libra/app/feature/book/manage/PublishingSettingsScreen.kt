package com.libra.app.feature.book.manage

import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Publish
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.libra.app.domain.model.BookStatus

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PublishingSettingsScreen(
    viewModel: BookManagementViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.state.collectAsState()
    val book = state.book
    val chapters = state.chapters
    val snackbarHostState = remember { SnackbarHostState() }

    var selectedVisibility by remember(book?.id) { mutableStateOf(book?.visibility ?: "PUBLIC") }
    val contentWarnings = remember(book?.id) {
        mutableStateListOf<String>().apply { addAll(book?.contentWarnings.orEmpty()) }
    }

    var showPublishConfirmDialog by remember { mutableStateOf(false) }
    var showUnpublishConfirmDialog by remember { mutableStateOf(false) }

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

    val hasTitle = book?.title?.trim()?.isNotBlank() == true
    val hasChapters = chapters.isNotEmpty()
    val hasContent = chapters.any { it.content.trim().length >= 30 }
    val canPublish = hasTitle && hasChapters && hasContent
    val isAlreadyPublished = book?.status == BookStatus.PUBLISHED

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Yayınlama & Görünürlük",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Geri")
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
            // Current Status Banner
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isAlreadyPublished) Color(0xFFE8F5E9) else Color(0xFFFFF3E0)
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
                            color = if (isAlreadyPublished) Color(0xFF2E7D32) else Color(0xFFE65100),
                            modifier = Modifier.size(42.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    if (isAlreadyPublished) Icons.Default.CheckCircle else Icons.Default.Publish,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }

                        Spacer(Modifier.width(14.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (isAlreadyPublished) "Kitap Yayında" else "Kitap Taslak Halinde",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = if (isAlreadyPublished) Color(0xFF1B5E20) else Color(0xFFBF360C)
                            )
                            Text(
                                text = if (isAlreadyPublished) {
                                    "Kitabın okurlara açık ve keşfet listelerinde görüntüleniyor."
                                } else {
                                    "Yalnızca sen görebilirsin. Hazır olduğunda yayınlayabilirsin."
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = if (isAlreadyPublished) Color(0xFF2E7D32) else Color(0xFFE65100)
                            )
                        }
                    }
                }
            }

            // Pre-publish validation checklist
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Text(
                            text = "Yayınlama Öncesi Kontrol Listesi",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Spacer(Modifier.height(12.dp))

                        ChecklistItem(
                            title = "Kitap Başlığı Belirlendi",
                            isComplete = hasTitle,
                            note = if (hasTitle) book?.title.orEmpty() else "Başlık eksik"
                        )
                        Spacer(Modifier.height(8.dp))
                        ChecklistItem(
                            title = "En Az Bir Bölüm Eklendi",
                            isComplete = hasChapters,
                            note = "${chapters.size} bölüm mevcut"
                        )
                        Spacer(Modifier.height(8.dp))
                        ChecklistItem(
                            title = "Bölüm İçeriği Yazıldı",
                            isComplete = hasContent,
                            note = if (hasContent) "Bölüm içerikleri dolu" else "Bölüm içeriği boş"
                        )
                    }
                }
            }

            // Publish / Unpublish Action Button
            item {
                if (isAlreadyPublished) {
                    OutlinedButton(
                        onClick = { showUnpublishConfirmDialog = true },
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFC62828))
                    ) {
                        Text("Yayından Kaldır (Taslağa Dönüştür)", fontWeight = FontWeight.Bold)
                    }
                } else {
                    Button(
                        onClick = { showPublishConfirmDialog = true },
                        enabled = canPublish && !state.isPublishing,
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        if (state.isPublishing) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), color = MaterialTheme.colorScheme.onPrimary, strokeWidth = 2.dp)
                            Spacer(Modifier.width(8.dp))
                        }
                        Text("Kitabı ve Bölümleri Yayınla", fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Visibility Options
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Text(
                            text = "Görünürlük Düzeyi",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Spacer(Modifier.height(12.dp))

                        VisibilityOption(
                            title = "Herkese Açık (Public)",
                            description = "Kitap aramalarda, ana sayfada ve keşfet listelerinde görünür.",
                            isSelected = selectedVisibility == "PUBLIC",
                            onSelect = {
                                selectedVisibility = "PUBLIC"
                                viewModel.updateVisibilityAndPrivacy("PUBLIC", contentWarnings.toList())
                            }
                        )

                        Spacer(Modifier.height(8.dp))

                        VisibilityOption(
                            title = "Liste Dışı (Unlisted)",
                            description = "Keşfette listelenmez, yalnızca doğrudan bağlantıya sahip okurlar okuyabilir.",
                            isSelected = selectedVisibility == "UNLISTED",
                            onSelect = {
                                selectedVisibility = "UNLISTED"
                                viewModel.updateVisibilityAndPrivacy("UNLISTED", contentWarnings.toList())
                            }
                        )

                        Spacer(Modifier.height(8.dp))

                        VisibilityOption(
                            title = "Gizli (Private)",
                            description = "Yalnızca yazar kitabı görüntüleyebilir ve okuyabilir.",
                            isSelected = selectedVisibility == "PRIVATE",
                            onSelect = {
                                selectedVisibility = "PRIVATE"
                                viewModel.updateVisibilityAndPrivacy("PRIVATE", contentWarnings.toList())
                            }
                        )
                    }
                }
            }

            // Content Warnings
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Text(
                            text = "İçerik Uyarıları",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = "Okurlarını bilgilendirmek için geçerli içerik temalarını seç:",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(10.dp))

                        val warningOptions = listOf(
                            "Şiddet & Aksiyon",
                            "Korku & Gerilim",
                            "Hassas İçerik / Yetişkin Temalar",
                            "Karanlık & Psikolojik Unsurlar"
                        )

                        warningOptions.forEach { warning ->
                            val isChecked = contentWarnings.contains(warning)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        if (isChecked) contentWarnings.remove(warning) else contentWarnings.add(warning)
                                        viewModel.updateVisibilityAndPrivacy(selectedVisibility, contentWarnings.toList())
                                    }
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = isChecked,
                                    onCheckedChange = { checked ->
                                        if (checked) contentWarnings.add(warning) else contentWarnings.remove(warning)
                                        viewModel.updateVisibilityAndPrivacy(selectedVisibility, contentWarnings.toList())
                                    }
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(warning, style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }
                }
            }

            item {
                Spacer(Modifier.height(32.dp))
            }
        }
    }

    if (showPublishConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showPublishConfirmDialog = false },
            title = { Text("Kitabı Yayınla") },
            text = { Text("Bu işlem kitabı ve tüm uygun bölümlerini yayına alacak. Okurlar kitabını okumaya başlayabilecek. Devam etmek istiyor musun?") },
            confirmButton = {
                Button(
                    onClick = {
                        showPublishConfirmDialog = false
                        viewModel.publishBook()
                    }
                ) {
                    Text("Evet, Yayınla")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showPublishConfirmDialog = false }) {
                    Text("Vazgeç")
                }
            }
        )
    }

    if (showUnpublishConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showUnpublishConfirmDialog = false },
            title = { Text("Yayından Kaldır") },
            text = { Text("Kitap taslak durumuna getirilecek ve okurların erişimine kapatılacak. İstediğin zaman tekrar yayınlayabilirsin.") },
            confirmButton = {
                Button(
                    onClick = {
                        showUnpublishConfirmDialog = false
                        viewModel.unpublishBook()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Yayından Kaldır")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showUnpublishConfirmDialog = false }) {
                    Text("Vazgeç")
                }
            }
        )
    }
}

@Composable
private fun ChecklistItem(
    title: String,
    isComplete: Boolean,
    note: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            if (isComplete) Icons.Default.CheckCircle else Icons.Default.Close,
            contentDescription = null,
            tint = if (isComplete) Color(0xFF2E7D32) else Color(0xFFD32F2F),
            modifier = Modifier.size(20.dp)
        )
        Spacer(Modifier.width(10.dp))
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
            )
            Text(
                text = note,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun VisibilityOption(
    title: String,
    description: String,
    isSelected: Boolean,
    onSelect: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onSelect),
        shape = RoundedCornerShape(12.dp),
        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RadioButton(selected = isSelected, onClick = onSelect)
            Spacer(Modifier.width(8.dp))
            Column {
                Text(title, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold))
                Spacer(Modifier.height(2.dp))
                Text(description, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
