package com.libra.app.feature.library

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
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
    val shelf = state?.selectedShelf ?: ShelfType.READING

    Column(modifier = modifier.fillMaxSize().testTag("library_screen")) {
        Text("Kütüphane", style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold), modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 10.dp))
        OutlinedTextField(
            value = state?.searchQuery.orEmpty(),
            onValueChange = onSearchChanged,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            placeholder = { Text("Kütüphanede ara…") },
            leadingIcon = { Icon(Icons.Default.Search, null) },
            trailingIcon = { if (!state?.searchQuery.isNullOrEmpty()) IconButton({ onSearchChanged("") }) { Icon(Icons.Default.Close, "Temizle") } },
            shape = RoundedCornerShape(14.dp),
            singleLine = true
        )
        Spacer(Modifier.height(8.dp))
        TabRow(selectedTabIndex = ShelfType.values().indexOf(shelf)) {
            ShelfType.values().forEach { item ->
                Tab(selected = shelf == item, onClick = { onTabSelected(item) }, text = { Text(item.titleTr) })
            }
        }

        when (uiState) {
            is UiState.Loading -> Text("Yükleniyor…", modifier = Modifier.padding(20.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
            is UiState.Error -> Column(modifier = Modifier.fillMaxWidth().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(uiState.error.message, color = MaterialTheme.colorScheme.error)
                androidx.compose.material3.TextButton(onClick = onRetry) { Text("Tekrar dene") }
            }
            is UiState.Empty -> EmptyLibrary(shelf, onNavigateToWrite)
            is UiState.Success -> {
                val query = state?.searchQuery.orEmpty()
                val items = uiState.data.items.filter { query.isBlank() || it.book.title.contains(query, true) || it.book.authorName.contains(query, true) }
                if (items.isEmpty()) EmptyLibrary(shelf, onNavigateToWrite)
                else LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(items, key = { it.id }) { item ->
                        HorizontalBookCard(item.book, { onBookClick(item.book) })
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyLibrary(shelf: ShelfType, onWrite: () -> Unit) {
    Column(modifier = Modifier.fillMaxWidth().padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(shelf.titleTr, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(4.dp))
        Text("Bu alan şimdilik boş.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        if (shelf == ShelfType.MY_WRITINGS) androidx.compose.material3.TextButton(onClick = onWrite) { Text("Kitap yaz") }
    }
}
