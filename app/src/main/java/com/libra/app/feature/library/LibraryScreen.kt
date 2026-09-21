package com.libra.app.feature.library

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.libra.app.core.state.UiState
import com.libra.app.domain.model.Book
import com.libra.app.domain.model.ShelfType
import com.libra.app.ui.components.EmptyContentView
import com.libra.app.ui.components.ErrorView
import com.libra.app.ui.components.HorizontalBookCard
import com.libra.app.ui.components.LoadingView

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
    Column(
        modifier = modifier
            .fillMaxSize()
            .testTag("library_screen")
    ) {
        // Top Title
        Text(
            text = "Kütüphanem",
            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 8.dp)
        )

        val currentState = (uiState as? UiState.Success)?.data
        val activeShelf = currentState?.selectedShelf ?: ShelfType.READING

        // Search bar
        OutlinedTextField(
            value = currentState?.searchQuery ?: "",
            onValueChange = onSearchChanged,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp)
                .testTag("library_search_input"),
            placeholder = { Text("Kütüphanede ara...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            trailingIcon = {
                if (!currentState?.searchQuery.isNullOrEmpty()) {
                    IconButton(onClick = { onSearchChanged("") }) {
                        Icon(Icons.Default.Close, contentDescription = "Temizle")
                    }
                }
            },
            shape = RoundedCornerShape(12.dp),
            singleLine = true
        )

        // 3 Shelf Tabs: Yazdıklarım, Okuduklarım, Kaydettiklerim
        TabRow(
            selectedTabIndex = ShelfType.values().indexOf(activeShelf),
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.primary
        ) {
            ShelfType.values().forEach { shelf ->
                Tab(
                    selected = activeShelf == shelf,
                    onClick = { onTabSelected(shelf) },
                    text = {
                        Text(
                            text = shelf.titleTr,
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold)
                        )
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Content
        when (uiState) {
            is UiState.Loading -> LoadingView(message = "Kitaplığınız yükleniyor...")
            is UiState.Error -> ErrorView(error = uiState.error, onRetry = onRetry)
            is UiState.Empty -> EmptyLibraryState(activeShelf, onNavigateToWrite)
            is UiState.Success -> {
                val items = currentState?.items ?: emptyList()
                val filtered = if (currentState?.searchQuery.isNullOrBlank()) {
                    items
                } else {
                    items.filter {
                        it.book.title.contains(currentState!!.searchQuery, ignoreCase = true) ||
                        it.book.authorName.contains(currentState.searchQuery, ignoreCase = true)
                    }
                }

                if (filtered.isEmpty()) {
                    EmptyLibraryState(activeShelf, onNavigateToWrite)
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(filtered) { shelfItem ->
                            HorizontalBookCard(
                                book = shelfItem.book,
                                onClick = { onBookClick(shelfItem.book) },
                                extraContent = {
                                    if (shelfItem.shelfType == ShelfType.READING) {
                                        Column {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Text(
                                                    text = "%${shelfItem.progressPercent} tamamlandı",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.primary
                                                )
                                            }
                                            Spacer(modifier = Modifier.height(4.dp))
                                            LinearProgressIndicator(
                                                progress = { shelfItem.progressPercent / 100f },
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .height(4.dp)
                                                    .clip(RoundedCornerShape(2.dp)),
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyLibraryState(
    shelfType: ShelfType,
    onNavigateToWrite: () -> Unit
) {
    val (title, desc) = when (shelfType) {
        ShelfType.MY_WRITINGS -> Pair("Henüz Kitap Yazmadınız", "Kendi kitabınızı yazmaya başlayarak okurlarla buluşun.")
        ShelfType.READING -> Pair("Okuma Listeniz Boş", "Keşfet bölümünden yeni kitaplar bularak okumaya başlayabilirsiniz.")
        ShelfType.SAVED -> Pair("Kaydedilen Kitap Yok", "İleride okumak istediğiniz kitapları kaydedin.")
    }

    EmptyContentView(
        title = title,
        description = desc,
        actionLabel = if (shelfType == ShelfType.MY_WRITINGS) "Yeni Kitap Yaz" else null,
        onActionClick = if (shelfType == ShelfType.MY_WRITINGS) onNavigateToWrite else null
    )
}
