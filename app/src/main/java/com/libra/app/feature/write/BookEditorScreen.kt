package com.libra.app.feature.write

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.platform.LocalContext
import androidx.compose.material.icons.filled.Publish
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.libra.app.domain.model.Book
import com.libra.app.domain.model.BookCategory
import com.libra.app.domain.model.BookStatus
import com.libra.app.domain.model.Chapter

@Composable
fun BookEditorScreen(
    book: Book,
    chapters: List<Chapter>,
    isSaving: Boolean,
    error: String?,
    onLoadChapters: (String) -> Unit,
    onSaveBook: (Book) -> Unit,
    onSaveChapter: (Chapter) -> Unit,
    onDeleteChapter: (Chapter) -> Unit,
    onUploadChapterImage: (ByteArray, String, String, (String) -> Unit) -> Unit,
    onPublish: (Book) -> Unit,
    onClearError: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var title by remember(book.id) { mutableStateOf(book.title) }
    var description by remember(book.id) { mutableStateOf(book.description) }
    var category by remember(book.id) { mutableStateOf(book.category) }
    var selectedChapterId by remember(book.id) { mutableStateOf<String?>(null) }
    var isCreatingChapter by remember(book.id) { mutableStateOf(false) }
    var newChapterNumber by remember(book.id) { mutableStateOf<Int?>(null) }
    var chapterTitle by remember(book.id) { mutableStateOf("") }
    var chapterContent by remember(book.id) { mutableStateOf("") }
    var categoryMenu by remember(book.id) { mutableStateOf(false) }
    var showDeleteConfirm by remember(book.id) { mutableStateOf(false) }
    val context = LocalContext.current
    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            runCatching {
                val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                if (bytes != null && bytes.isNotEmpty()) {
                    onUploadChapterImage(bytes, "chapter-image.jpg", "image/jpeg") { inserted ->
                        chapterContent += inserted
                    }
                }
            }
        }
    }
    LaunchedEffect(book.id) { onLoadChapters(book.id) }

    LaunchedEffect(chapters, selectedChapterId, isCreatingChapter, newChapterNumber) {
        if (isCreatingChapter) {
            val created = newChapterNumber?.let { number ->
                chapters.firstOrNull { it.chapterNumber == number }
            }
            if (created != null) {
                isCreatingChapter = false
                newChapterNumber = null
                selectedChapterId = created.id
                chapterTitle = created.title
                chapterContent = created.content
            }
            return@LaunchedEffect
        }

        val selected = chapters.firstOrNull { it.id == selectedChapterId }
            ?: chapters.firstOrNull()
        if (selected != null && selected.id != selectedChapterId) {
            selectedChapterId = selected.id
            chapterTitle = selected.title
            chapterContent = selected.content
        }
    }

    val selectedChapter = chapters.firstOrNull { it.id == selectedChapterId }
    val nextNumber = (chapters.maxOfOrNull { it.chapterNumber } ?: 0) + 1
    val editorChapterNumber = selectedChapter?.chapterNumber ?: nextNumber

    LaunchedEffect(chapterContent, chapterTitle, selectedChapterId) {
        val current = selectedChapter
        if (current != null && (chapterContent != current.content || chapterTitle != current.title)) {
            kotlinx.coroutines.delay(1200)
            onSaveChapter(current.copy(title = chapterTitle.trim(), content = chapterContent))
        }
    }

    Column(modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack, enabled = !isSaving) {
                Icon(Icons.Default.ArrowBack, "Geri")
            }
            Column(Modifier.weight(1f)) {
                Text(title.ifBlank { "Kitap düzenle" }, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text(
                    if (book.status == BookStatus.PUBLISHED) "Yayınlandı" else "Taslak • ${chapters.size} bölüm",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (isSaving) {
                CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
            } else {
                TextButton(onClick = {
                    onSaveBook(book.copy(title = title.trim(), description = description.trim(), category = category))
                    selectedChapter?.let {
                        onSaveChapter(it.copy(title = chapterTitle.trim(), content = chapterContent))
                    }
                }) {
                    Icon(Icons.Default.Check, null)
                    Spacer(Modifier.width(4.dp))
                    Text("Kaydet")
                }
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Card(
                    Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Book, null, Modifier.size(24.dp))
                            Spacer(Modifier.width(10.dp))
                            Text("Kitap bilgileri", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        }
                        Spacer(Modifier.height(12.dp))
                        OutlinedTextField(
                            value = title,
                            onValueChange = { if (it.length <= 120) title = it },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("Kitap adı") },
                            singleLine = true
                        )
                        Spacer(Modifier.height(10.dp))
                        Box(Modifier.fillMaxWidth()) {
                            OutlinedTextField(
                                value = category.displayName,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Kategori") },
                                modifier = Modifier.fillMaxWidth()
                            )
                            Box(Modifier.matchParentSize().clickable { categoryMenu = true })
                            DropdownMenu(categoryMenu, { categoryMenu = false }) {
                                BookCategory.values().forEach { item ->
                                    DropdownMenuItem(
                                        text = { Text(item.displayName) },
                                        onClick = { category = item; categoryMenu = false }
                                    )
                                }
                            }
                        }
                        Spacer(Modifier.height(10.dp))
                        OutlinedTextField(
                            value = description,
                            onValueChange = { if (it.length <= 2000) description = it },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("Kitap açıklaması") },
                            minLines = 3
                        )
                    }
                }
            }

            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Bölümler", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text("${chapters.size} bölüm", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    TextButton(
                        enabled = !isSaving,
                        onClick = {
                            isCreatingChapter = true
                            newChapterNumber = nextNumber
                            selectedChapterId = null
                            chapterTitle = "$nextNumber. Bölüm"
                            chapterContent = ""
                        }
                    ) {
                        Icon(Icons.Default.Add, null)
                        Spacer(Modifier.width(4.dp))
                        Text("Yeni bölüm")
                    }
                }
            }

            if (chapters.isNotEmpty()) {
                item {
                    Row(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.surfaceVariant).padding(6.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        chapters.forEach { chapter ->
                            Box(
                                Modifier.clip(RoundedCornerShape(9.dp))
                                    .background(if (chapter.id == selectedChapterId) MaterialTheme.colorScheme.onBackground else MaterialTheme.colorScheme.surface)
                                    .clickable {
                                        isCreatingChapter = false
                                        selectedChapterId = chapter.id
                                        chapterTitle = chapter.title
                                        chapterContent = chapter.content
                                    }
                                    .padding(horizontal = 12.dp, vertical = 9.dp)
                            ) {
                                Text(
                                    chapter.chapterNumber.toString(),
                                    color = if (chapter.id == selectedChapterId) MaterialTheme.colorScheme.background else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }

            item {
                Card(
                    Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.MenuBook, null, Modifier.size(22.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(
                                if (selectedChapter != null) "Bölüm " + selectedChapter.chapterNumber else "Yeni bölüm #$editorChapterNumber",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(Modifier.height(12.dp))
                        OutlinedTextField(
                            value = chapterTitle,
                            onValueChange = { if (it.length <= 120) chapterTitle = it },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("Bölüm başlığı") },
                            singleLine = true
                        )
                        Spacer(Modifier.height(10.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            TextButton(onClick = { chapterContent += "\n**" }, enabled = !isSaving) { Text("Kalın") }
                            TextButton(onClick = { chapterContent += "\n_" }, enabled = !isSaving) { Text("İtalik") }
                            TextButton(onClick = { chapterContent += "\n> " }, enabled = !isSaving) { Text("Alıntı") }
                            IconButton(onClick = { imagePicker.launch("image/*") }, enabled = !isSaving) { Icon(Icons.Default.Image, "Görsel ekle") }
                        }
                        OutlinedTextField(
                            value = chapterContent,
                            onValueChange = { if (it.length <= 100000) chapterContent = it },
                            modifier = Modifier.fillMaxWidth().height(420.dp),
                            label = { Text("Bölüm metni") },
                            placeholder = { Text("Hikâyeni burada yaz…") },
                            supportingText = {
                                val words = chapterContent.trim().split(Regex("\\s+")).count { it.isNotBlank() }
                                Text("$words kelime")
                            }
                        )
                        Spacer(Modifier.height(10.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        TextButton(
                            enabled = !isSaving && chapterTitle.isNotBlank() && chapterContent.isNotBlank(),
                            onClick = {
                                val number = selectedChapter?.chapterNumber ?: nextNumber
                                onSaveChapter(
                                    Chapter(
                                        id = selectedChapter?.id.orEmpty(),
                                        bookId = book.id,
                                        chapterNumber = number,
                                        title = chapterTitle.trim(),
                                        content = chapterContent,
                                        isPublished = selectedChapter?.isPublished == true
                                    )
                                )
                                if (selectedChapter != null) {
                                    isCreatingChapter = false
                                }
                            }
                        ) {
                            Icon(Icons.Default.Check, null)
                            Spacer(Modifier.width(4.dp))
                            Text("Bölümü kaydet")
                        }
                        if (selectedChapter != null) {
                            IconButton(onClick = { showDeleteConfirm = true }, enabled = !isSaving) {
                                Icon(Icons.Default.Delete, "Bölümü sil")
                            }
                        }
                        }
                    }
                }
            }

            if (error != null) {
                item {
                    TextButton(onClick = onClearError) {
                        Text(error, color = MaterialTheme.colorScheme.error)
                    }
                }
            }

            item {
                Card(
                    Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Text("Yayın", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(6.dp))
                        Text("Yayınladığında kitap ve bölümler okuyuculara açık hale gelir.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.height(10.dp))
                        TextButton(
                            enabled = !isSaving && chapters.isNotEmpty() && chapters.all { it.content.isNotBlank() },
                            onClick = {
                                onPublish(
                                    book.copy(title = title.trim(), description = description.trim(), category = category)
                                )
                            }
                        ) {
                            Icon(Icons.Default.Publish, null)
                            Spacer(Modifier.width(5.dp))
                            Text(if (book.status == BookStatus.PUBLISHED) "Yayın güncelle" else "Kitabı yayınla")
                        }
                    }
                }
            }

            item { Spacer(Modifier.height(28.dp)) }
        }
    }

    val chapterToDelete = selectedChapter
    if (showDeleteConfirm && chapterToDelete != null) {
        DeleteChapterDialog(
            onDismiss = { showDeleteConfirm = false },
            onConfirm = {
                onDeleteChapter(chapterToDelete)
                showDeleteConfirm = false
                selectedChapterId = null
                chapterTitle = ""
                chapterContent = ""
            }
        )
    }
}


@Composable
private fun DeleteChapterDialog(onDismiss: () -> Unit, onConfirm: () -> Unit) {
    androidx.compose.material3.AlertDialog(onDismissRequest = onDismiss, title = { Text("Bölümü sil?") }, text = { Text("Bu işlem bölümü kalıcı olarak kaldırır.") }, confirmButton = { TextButton(onClick = onConfirm) { Text("Sil") } }, dismissButton = { TextButton(onClick = onDismiss) { Text("İptal") } })
}
