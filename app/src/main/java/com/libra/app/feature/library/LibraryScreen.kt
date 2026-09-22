package com.libra.app.feature.library

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.libra.app.core.state.UiState
import com.libra.app.domain.model.Book
import com.libra.app.domain.model.ShelfType
import com.libra.app.ui.components.HorizontalBookCard

@Composable
fun LibraryScreen(
    uiState: UiState<LibraryState>,
    onTabSelected: (ShelfType) -> Unit,
    onSearchChanged: (String) -> Unit,
    onBookClick: (Book) -> Unit,
    onNavigateToWrite: () -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state = (uiState as? UiState.Success)?.data
    val selectedShelf = state?.selectedShelf ?: ShelfType.READING
    val query = state?.searchQuery.orEmpty()
    val tabIndex = ShelfType.values().indexOf(selectedShelf).coerceAtLeast(0)

    Column(
        modifier = modifier.fillMaxSize().testTag("library_screen")
    ) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text("Kütüphane", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Text("Okudukların ve okumak istediklerin", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            IconButton(onClick = onRetry) {
                Icon(Icons.Default.Refresh, "Yenile")
            }
        }

        OutlinedTextField(
            value = query,
            onValueChange = onSearchChanged,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            placeholder = { Text("Kitap veya yazar ara…") },
            leadingIcon = { Icon(Icons.Default.Search, null) },
            trailingIcon = {
                if (query.isNotEmpty()) {
                    IconButton(onClick = { onSearchChanged("") }) {
                        Icon(Icons.Default.Close, "Temizle")
                    }
                }
            },
            shape = RoundedCornerShape(18.dp),
            singleLine = true
        )

        TabRow(selectedTabIndex = tabIndex, modifier = Modifier.padding(top = 8.dp)) {
            ShelfType.values().forEach { shelf ->
                Tab(
                    selected = shelf == selectedShelf,
                    onClick = { onTabSelected(shelf) },
                    text = { Text(shelf.titleTr) }
                )
            }
        }

        when (uiState) {
            is UiState.Loading -> {
                LibraryMessage(
                    title = "Kütüphane yükleniyor",
                    message = "Kitapların getiriliyor…",
                    action = null,
                    onAction = {}
                )
            }
            is UiState.Error -> {
                LibraryMessage(
                    title = "Kütüphane açılamadı",
                    message = uiState.error.message,
                    action = "Tekrar dene",
                    onAction = onRetry
                )
            }
            is UiState.Empty -> {
                EmptyLibrary(selectedShelf, onNavigateToWrite)
            }
            is UiState.Success -> {
                val filtered = uiState.data.items.filter {
                    query.isBlank() ||
                        it.book.title.contains(query, ignoreCase = true) ||
                        it.book.authorName.contains(query, ignoreCase = true)
                }

                if (filtered.isEmpty()) {
                    EmptyLibrary(selectedShelf, onNavigateToWrite)
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(filtered, key = { it.id }) { item ->
                            HorizontalBookCard(
                                item.book,
                                { onBookClick(item.book) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LibraryMessage(title: String, message: String, action: String?, onAction: () -> Unit) {
    Column(
        Modifier.fillMaxWidth().padding(36.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
        Text(
            message,
            Modifier.padding(top = 6.dp),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        if (action != null) {
            TextButton(onClick = onAction) { Text(action) }
        }
    }
}

@Composable
private fun EmptyLibrary(shelf: ShelfType, onWrite: () -> Unit) {
    Column(
        Modifier.fillMaxWidth().padding(36.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(shelf.titleTr, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
        Text(
            "Bu raf şu an boş.",
            Modifier.padding(top = 6.dp),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        if (shelf == ShelfType.MY_WRITINGS) {
            TextButton(onClick = onWrite) { Text("İlk kitabını yaz") }
        }
    }
}
