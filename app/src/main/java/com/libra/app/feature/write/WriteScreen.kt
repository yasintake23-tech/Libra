package com.libra.app.feature.write

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.libra.app.core.state.UiState
import com.libra.app.domain.model.Book
import com.libra.app.domain.model.BookCategory
import com.libra.app.domain.model.BookStatus
import com.libra.app.ui.components.AppButton
import com.libra.app.ui.components.AppButtonVariant
import com.libra.app.ui.components.BookCover

@Composable
fun WriteScreen(
    uiState: UiState<WriteState>,
    onCreateBook: (String, String, String, BookCategory, ByteArray?, String, String) -> Unit,
    onBookClick: (Book) -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showCreate by remember { mutableStateOf(false) }

    Column(modifier = modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(Modifier.weight(1f)) {
                Text("Yazar Stüdyosu", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Text(
                    "Kitaplarını oluştur, bölümlerini yaz ve yayınla.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(onClick = { showCreate = true }) {
                Icon(Icons.Default.Add, "Yeni kitap")
            }
        }

        when (uiState) {
            is UiState.Loading -> {
                Text("Kitapların yükleniyor…", modifier = Modifier.padding(20.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            is UiState.Error -> {
                Column(
                    Modifier.fillMaxWidth().padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(uiState.error.message, color = MaterialTheme.colorScheme.error)
                    TextButton(onClick = onRetry) { Text("Tekrar dene") }
                }
            }
            is UiState.Empty -> AuthorStudioEmpty(onCreate = { showCreate = true })
            is UiState.Success -> {
                val books = uiState.data.myBooks
                val drafts = books.count { it.status == BookStatus.DRAFT }
                val published = books.count { it.status == BookStatus.PUBLISHED }

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        Card(
                            Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(18.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Column(Modifier.padding(16.dp)) {
                                Text("Yazmaya hazır mısın?", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                                Text(
                                    "Yeni kitabını oluştur. Sonrasında yazar editöründen bölümlerini düzenleyebilirsin.",
                                    modifier = Modifier.padding(top = 5.dp),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                AppButton(
                                    "Yeni kitap oluştur",
                                    onClick = { showCreate = true },
                                    variant = AppButtonVariant.PRIMARY,
                                    modifier = Modifier.fillMaxWidth().padding(top = 12.dp).height(44.dp)
                                )
                            }
                        }
                    }

                    item {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            StudioStat("Kitap", books.size, Icons.Default.Book, Modifier.weight(1f))
                            StudioStat("Taslak", drafts, Icons.Default.Description, Modifier.weight(1f))
                            StudioStat("Yayında", published, Icons.Default.MenuBook, Modifier.weight(1f))
                        }
                    }

                    item {
                        Text("Kitaplarım", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }

                    if (books.isEmpty()) {
                        item { AuthorStudioEmpty(onCreate = { showCreate = true }) }
                    } else {
                        items(books, key = { it.id }) { book ->
                            AuthorBookCard(book = book, onOpenEditor = { onBookClick(book) })
                        }
                    }

                    item { Spacer(Modifier.height(20.dp)) }
                }
            }
        }
    }

    if (showCreate) {
        CreateBookDialog(
            onDismiss = { showCreate = false },
            onConfirm = { title, description, discoverySummary, category, coverBytes, fileName, contentType ->
                onCreateBook(title, description, discoverySummary, category, coverBytes, fileName, contentType)
                showCreate = false
            }
        )
    }
}

@Composable
private fun StudioStat(
    label: String,
    value: Int,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier
) {
    Card(modifier, shape = RoundedCornerShape(14.dp)) {
        Column(Modifier.padding(12.dp)) {
            Icon(icon, null, Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value.toString(), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun AuthorBookCard(book: Book, onOpenEditor: () -> Unit) {
    Card(
        Modifier.fillMaxWidth().clickable(onClick = onOpenEditor),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
            BookCover(book, Modifier.width(62.dp).aspectRatio(.69f))
            Column(Modifier.padding(start = 12.dp).weight(1f)) {
                Text(book.title.ifBlank { "Adsız kitap" }, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, maxLines = 1)
                Text(book.category.displayName, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Surface(
                    modifier = Modifier.padding(top = 5.dp),
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Text(
                        if (book.status == BookStatus.PUBLISHED) "Yayında" else "Taslak",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }
            TextButton(onClick = onOpenEditor) {
                Icon(Icons.Default.Edit, null, Modifier.size(17.dp))
                Spacer(Modifier.width(4.dp))
                Text("Editör")
            }
        }
    }
}

@Composable
private fun AuthorStudioEmpty(onCreate: () -> Unit) {
    Column(
        Modifier.fillMaxWidth().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(Icons.Default.MenuBook, null, Modifier.size(42.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Text("Henüz kitabın yok.", modifier = Modifier.padding(top = 8.dp), fontWeight = FontWeight.SemiBold)
        Text(
            "İlk kitabını oluşturduğunda yazar editörü burada açılacak.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        TextButton(onClick = onCreate) { Text("Kitap oluştur") }
    }
}

@Composable
private fun CreateBookDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, String, String, BookCategory, ByteArray?, String, String) -> Unit
) {
    val context = LocalContext.current
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var discoverySummary by remember { mutableStateOf("") }
    var category by remember { mutableStateOf(BookCategory.FICTION) }
    var expanded by remember { mutableStateOf(false) }
    var coverBytes by remember { mutableStateOf<ByteArray?>(null) }
    var coverUri by remember { mutableStateOf<android.net.Uri?>(null) }
    var coverFileName by remember { mutableStateOf("cover.jpg") }
    var coverContentType by remember { mutableStateOf("image/jpeg") }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        runCatching {
            val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                ?: throw IllegalStateException("Kapak okunamadı.")
            if (bytes.size > 8 * 1024 * 1024) throw IllegalStateException("Kapak görseli 8 MB'dan küçük olmalı.")
            coverBytes = bytes
            coverUri = uri
            coverContentType = context.contentResolver.getType(uri) ?: "image/jpeg"
            coverFileName = "book-cover-" + System.currentTimeMillis() + "." +
                coverContentType.substringAfter('/').ifBlank { "jpg" }.take(8)
        }.onFailure {
            coverBytes = null
            coverUri = null
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Yeni kitap oluştur") },
        text = {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth().clickable { picker.launch("image/*") },
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Row(Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                            if (coverUri != null) {
                                AsyncImage(
                                    model = coverUri,
                                    contentDescription = "Kitap kapağı",
                                    modifier = Modifier.size(78.dp).clip(RoundedCornerShape(10.dp))
                                )
                            } else {
                                Box(
                                    Modifier.size(78.dp).clip(RoundedCornerShape(10.dp))
                                        .background(MaterialTheme.colorScheme.surfaceVariant),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.Description, null)
                                }
                            }
                            Column(Modifier.padding(start = 12.dp).weight(1f)) {
                                Text("Kitap kapağı", fontWeight = FontWeight.Bold)
                                Text(
                                    if (coverUri == null) "Kapak fotoğrafı seç" else "Kapağı değiştirmek için dokun",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
                item {
                    OutlinedTextField(
                        value = title,
                        onValueChange = { if (it.length <= 120) title = it },
                        label = { Text("Kitap adı") },
                        supportingText = { Text(title.length.toString() + "/120") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                item {
                    Box(Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = category.displayName,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Kategori") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Box(Modifier.matchParentSize().clickable { expanded = true })
                        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                            BookCategory.values().forEach { item ->
                                DropdownMenuItem(
                                    text = { Text(item.displayName) },
                                    onClick = { category = item; expanded = false }
                                )
                            }
                        }
                    }
                }
                item {
                    OutlinedTextField(
                        value = description,
                        onValueChange = { if (it.length <= 2000) description = it },
                        label = { Text("Kitap açıklaması") },
                        supportingText = { Text(description.length.toString() + "/2000") },
                        minLines = 3,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                item {
                    OutlinedTextField(
                        value = discoverySummary,
                        onValueChange = { if (it.length <= 500) discoverySummary = it },
                        label = { Text("Keşfet özeti") },
                        placeholder = { Text("Kitabını keşfette tanıtacak kısa özet...") },
                        supportingText = { Text(discoverySummary.length.toString() + "/500") },
                        minLines = 3,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = title.isNotBlank(),
                onClick = {
                    onConfirm(
                        title.trim(),
                        description.trim(),
                        discoverySummary.trim(),
                        category,
                        coverBytes,
                        coverFileName,
                        coverContentType
                    )
                }
            ) { Text("Kitabı oluştur") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("İptal") } }
    )
}
