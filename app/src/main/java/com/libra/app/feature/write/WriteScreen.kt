package com.libra.app.feature.write

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Description
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.libra.app.core.state.UiState
import com.libra.app.domain.model.Book
import com.libra.app.domain.model.BookCategory
import com.libra.app.domain.model.BookStatus
import com.libra.app.ui.components.AppButton
import com.libra.app.ui.components.AppButtonVariant
import com.libra.app.ui.components.EmptyContentView
import com.libra.app.ui.components.ErrorView
import com.libra.app.ui.components.LoadingView
import com.libra.app.ui.components.SectionHeader

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WriteScreen(
    uiState: UiState<WriteState>,
    onCreateBook: (title: String, desc: String, category: BookCategory) -> Unit,
    onBookClick: (Book) -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showCreateDialog by remember { mutableStateOf(false) }

    Column(modifier = modifier.fillMaxSize().testTag("write_screen")) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("Yazarlık Atölyesi", style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.onBackground)
                Text("Kendi kitabını kurgula ve bölümlerini yönet", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        AppButton(
            text = "Yeni Kitap Oluştur",
            icon = Icons.Default.Add,
            onClick = { showCreateDialog = true },
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
            variant = AppButtonVariant.PRIMARY,
            testTag = "create_new_book_button"
        )

        Spacer(modifier = Modifier.height(12.dp))
        AiWritingAssistantCard(modifier = Modifier.padding(horizontal = 16.dp))
        Spacer(modifier = Modifier.height(16.dp))

        SectionHeader(title = "Taslaklarım ve Eserlerim", subtitle = "Yazım aşamasındaki kitaplarınız")

        when (uiState) {
            is UiState.Loading -> LoadingView(message = "Eserleriniz yükleniyor...")
            is UiState.Error -> ErrorView(error = uiState.error, onRetry = onRetry)
            is UiState.Empty, is UiState.Success -> {
                val books = (uiState as? UiState.Success)?.data?.myBooks ?: emptyList()
                if (books.isEmpty()) {
                    EmptyContentView(title = "Henüz Taslak Oluşturmadınız", description = "Yukarıdaki 'Yeni Kitap Oluştur' butonuna basarak ilk eserinize başlayabilirsiniz.")
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(books) { book -> MyDraftBookCard(book = book, onClick = { onBookClick(book) }) }
                    }
                }
            }
        }
    }

    if (showCreateDialog) {
        CreateBookDialog(
            onDismiss = { showCreateDialog = false },
            onConfirm = { title, desc, cat ->
                onCreateBook(title, desc, cat)
                showCreateDialog = false
            }
        )
    }
}

@Composable
private fun MyDraftBookCard(book: Book, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).testTag("draft_book_${book.id}"),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(modifier = Modifier.padding(14.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Surface(shape = RoundedCornerShape(8.dp), color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f), modifier = Modifier.size(50.dp)) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.Description, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                }
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(book.title, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                Text("${book.category.displayName} • ${book.chapterCount} Bölüm", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Surface(shape = RoundedCornerShape(6.dp), color = if (book.status == BookStatus.PUBLISHED) Color(0xFF22C55E).copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant) {
                Text(if (book.status == BookStatus.PUBLISHED) "Yayında" else "Taslak", style = MaterialTheme.typography.labelSmall.copy(color = if (book.status == BookStatus.PUBLISHED) Color(0xFF16A34A) else MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.SemiBold), modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
            }
        }
    }
}

@Composable
private fun AiWritingAssistantCard(modifier: Modifier = Modifier) {
    Card(modifier = modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))) {
        Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text("AI Yazarlık Asistanı (Mimari Hazır)", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.onSurface)
                Text("Kurgu önerileri, özetleme ve bölüm tamamlama servis katmanında soyutlandı.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CreateBookDialog(onDismiss: () -> Unit, onConfirm: (String, String, BookCategory) -> Unit) {
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf(BookCategory.FICTION) }
    var expanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Yeni Kitap Oluştur", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Kitap Başlığı") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = !expanded }) {
                    OutlinedTextField(value = selectedCategory.displayName, onValueChange = {}, readOnly = true, label = { Text("Kategori") }, trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) }, modifier = Modifier.fillMaxWidth().menuAnchor())
                    ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                        BookCategory.values().forEach { category -> DropdownMenuItem(text = { Text(category.displayName) }, onClick = { selectedCategory = category; expanded = false }) }
                    }
                }
                OutlinedTextField(value = description, onValueChange = { description = it }, label = { Text("Kitap Özeti / Açıklaması") }, modifier = Modifier.fillMaxWidth(), minLines = 3, maxLines = 5)
            }
        },
        confirmButton = { TextButton(onClick = { if (title.isNotBlank()) onConfirm(title, description, selectedCategory) }, enabled = title.isNotBlank()) { Text("Oluştur") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("İptal") } }
    )
}
