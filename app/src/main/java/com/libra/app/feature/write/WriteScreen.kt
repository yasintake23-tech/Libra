package com.libra.app.feature.write

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
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExposedDropdownMenu
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.unit.dp
import com.libra.app.core.state.UiState
import com.libra.app.domain.model.Book
import com.libra.app.domain.model.BookCategory
import com.libra.app.ui.components.AppButton
import com.libra.app.ui.components.AppButtonVariant
import com.libra.app.ui.components.BookCover

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WriteScreen(
    uiState: UiState<WriteState>,
    onCreateBook: (String, String, BookCategory) -> Unit,
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
            Column {
                Text("Yaz", style = MaterialTheme.typography.headlineSmall.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.Bold))
                Text("Fikirlerini hikâyeye dönüştür.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            IconButton(onClick = { showCreate = true }) { Icon(Icons.Default.Add, "Yeni kitap") }
        }

        Card(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier.width(120.dp).aspectRatio(0.9f).clip(RoundedCornerShape(10.dp)).background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Description, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(30.dp))
                }
                Spacer(Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text("Yeni Kitap Oluştur", style = MaterialTheme.typography.titleMedium.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.Bold))
                    Spacer(Modifier.height(5.dp))
                    Text("Hayalindeki hikâyeyi yazmaya başla.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(10.dp))
                    AppButton("Başla", onClick = { showCreate = true }, variant = AppButtonVariant.PRIMARY, modifier = Modifier.fillMaxWidth().height(42.dp))
                }
            }
        }

        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 14.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("Taslaklarım", style = MaterialTheme.typography.titleMedium.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.Bold))
            Text("Tümünü Gör", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        when (uiState) {
            is UiState.Loading -> Text("Yükleniyor…", modifier = Modifier.padding(20.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
            is UiState.Error -> Column(modifier = Modifier.fillMaxWidth().padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(uiState.error.message, color = MaterialTheme.colorScheme.error)
                TextButton(onClick = onRetry) { Text("Tekrar dene") }
            }
            is UiState.Empty -> EmptyDraft()
            is UiState.Success -> {
                if (uiState.data.myBooks.isEmpty()) EmptyDraft()
                else LazyColumn(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(uiState.data.myBooks, key = { it.id }) { book ->
                        Card(modifier = Modifier.fillMaxWidth().clickable { onBookClick(book) }, shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                            Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                                BookCover(book, Modifier.width(50.dp).aspectRatio(.69f))
                                Spacer(Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(book.title, style = MaterialTheme.typography.titleSmall.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.Bold), maxLines = 1)
                                    Text(book.category.displayName, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Icon(Icons.Default.MoreHoriz, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
        }
    }

    if (showCreate) {
        CreateBookDialog(
            onDismiss = { showCreate = false },
            onConfirm = { title, description, category ->
                onCreateBook(title, description, category)
                showCreate = false
            }
        )
    }
}

@Composable
private fun EmptyDraft() {
    Column(modifier = Modifier.fillMaxWidth().padding(28.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(Icons.Default.Description, null, tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = .55f), modifier = Modifier.size(42.dp))
        Spacer(Modifier.height(10.dp))
        Text("Henüz bir taslağın yok.", fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold)
        Text("Yeni bir kitap başlattığında burada görünecek.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CreateBookDialog(onDismiss: () -> Unit, onConfirm: (String, String, BookCategory) -> Unit) {
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var category by remember { mutableStateOf(BookCategory.FICTION) }
    var expanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Yeni kitap") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(title, { title = it }, label = { Text("Başlık") }, singleLine = true)
                ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = !expanded }) {
                    OutlinedTextField(
                        value = category.displayName,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Kategori") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
                        modifier = Modifier.fillMaxWidth().menuAnchor()
                    )
                    ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                        BookCategory.values().forEach { item ->
                            DropdownMenuItem(text = { Text(item.displayName) }, onClick = { category = item; expanded = false })
                        }
                    }
                }
                OutlinedTextField(description, { description = it }, label = { Text("Kısa açıklama") }, minLines = 3)
            }
        },
        confirmButton = { TextButton(enabled = title.isNotBlank(), onClick = { onConfirm(title.trim(), description.trim(), category) }) { Text("Oluştur") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("İptal") } }
    )
}
