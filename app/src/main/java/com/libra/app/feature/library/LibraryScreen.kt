package com.libra.app.feature.library

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import com.libra.app.ui.animation.AppleSprings
import com.libra.app.ui.animation.appleBouncy
import com.libra.app.ui.animation.appleGlassSurface
import com.libra.app.ui.animation.appleSquircle
import com.libra.app.ui.animation.staggeredVaporEntrance
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.libra.app.core.state.UiState
import com.libra.app.domain.model.Book
import com.libra.app.domain.model.BookStatus
import com.libra.app.domain.model.ShelfType
import com.libra.app.domain.model.UserShelfItem
import com.libra.app.ui.components.BookCover
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun LibraryScreen(
    uiState: UiState<LibraryState>,
    onTabSelected: (ShelfType) -> Unit,
    onSearchChanged: (String) -> Unit,
    onBookClick: (Book, ShelfType) -> Unit,
    onNavigateToWrite: () -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
    onCreateBookClick: () -> Unit = onNavigateToWrite,
    onStartReading: (Book) -> Unit = {},
    onRemoveFromShelf: (Book, ShelfType) -> Unit = { _, _ -> },
    onAddToSaved: (Book) -> Unit = {},
    onNavigateToDiscover: () -> Unit = {}
) {
    val state = (uiState as? UiState.Success)?.data ?: LibraryState()
    val selectedShelf = state.selectedShelf
    val currentShelfState = state.currentShelfState
    val query = state.searchQuery

    // Order of tabs: Written -> Reading -> Saved
    val tabs = remember {
        listOf(ShelfType.MY_WRITINGS, ShelfType.READING, ShelfType.SAVED)
    }
    val tabIndex = tabs.indexOf(selectedShelf).coerceAtLeast(0)

    Column(
        modifier = modifier
            .fillMaxSize()
            .testTag("library_screen")
    ) {
        // --- Top Bar Header ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Kütüphane",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.ExtraBold
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Okudukların, yazdıkların ve kaydettiklerin",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            FilledTonalIconButton(
                onClick = onCreateBookClick,
                colors = IconButtonDefaults.filledTonalIconButtonColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                ),
                modifier = Modifier
                    .appleBouncy(pressedScale = 0.88f, onClick = onCreateBookClick)
                    .testTag("library_create_book_btn")
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Yeni Kitap Oluştur"
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            IconButton(
                onClick = onRetry,
                modifier = Modifier
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    .appleBouncy(pressedScale = 0.88f, onClick = onRetry)
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Yenile",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // --- Search Field ---
        OutlinedTextField(
            value = query,
            onValueChange = onSearchChanged,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            placeholder = {
                Text("Kitap veya yazar ara…", style = MaterialTheme.typography.bodyMedium)
            },
            leadingIcon = {
                Icon(
                    Icons.Default.Search,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            trailingIcon = {
                if (query.isNotEmpty()) {
                    IconButton(onClick = { onSearchChanged("") }) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Temizle",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            },
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surface,
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
            ),
            shape = RoundedCornerShape(16.dp),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(6.dp))

        // --- Tabs: Written | Reading | Saved ---
        TabRow(
            selectedTabIndex = tabIndex,
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.primary,
            indicator = { tabPositions ->
                if (tabIndex in tabPositions.indices) {
                    TabRowDefaults.SecondaryIndicator(
                        Modifier.tabIndicatorOffset(tabPositions[tabIndex]),
                        height = 3.dp,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            },
            divider = {}
        ) {
            tabs.forEach { shelf ->
                val isSelected = shelf == selectedShelf
                val shelfItemsCount = state.shelves[shelf]?.items?.size ?: 0
                val isLoadingShelf = state.shelves[shelf]?.isLoading == true

                Tab(
                    selected = isSelected,
                    onClick = { onTabSelected(shelf) },
                    text = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = shelf.titleTr,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            if (!isLoadingShelf && shelfItemsCount > 0) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Box(
                                    modifier = Modifier
                                        .clip(CircleShape)
                                        .background(
                                            if (isSelected) MaterialTheme.colorScheme.primaryContainer
                                            else MaterialTheme.colorScheme.surfaceVariant
                                        )
                                        .padding(horizontal = 6.dp, vertical = 2.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = shelfItemsCount.toString(),
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer
                                        else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // --- Content Area ---
        when {
            // Global non-success error (e.g. unauthenticated)
            uiState is UiState.Error -> {
                ShelfMessageCard(
                    icon = Icons.Default.ErrorOutline,
                    title = "Kütüphane açılamadı",
                    message = uiState.error.message,
                    action = "Tekrar Dene",
                    onAction = onRetry
                )
            }

            // Global initial loading
            uiState is UiState.Loading -> {
                ShelfLoadingView(title = "Kütüphane hazırlanıyor…")
            }

            // Shelf-specific states
            else -> {
                val items = currentShelfState.items
                val isLoading = currentShelfState.isLoading
                val error = currentShelfState.error

                when {
                    isLoading && items.isEmpty() -> {
                        ShelfLoadingView(title = "${selectedShelf.titleTr} yükleniyor…")
                    }

                    error != null && items.isEmpty() -> {
                        ShelfMessageCard(
                            icon = Icons.Default.ErrorOutline,
                            title = "${selectedShelf.titleTr} yüklenemedi",
                            message = error,
                            action = "Tekrar Dene",
                            onAction = onRetry
                        )
                    }

                    items.isEmpty() -> {
                        ShelfEmptyView(
                            shelf = selectedShelf,
                            onWrite = onCreateBookClick,
                            onDiscover = onNavigateToDiscover
                        )
                    }

                    else -> {
                        val filteredItems = if (query.isBlank()) {
                            items
                        } else {
                            items.filter {
                                it.book.title.contains(query, ignoreCase = true) ||
                                    it.book.authorName.contains(query, ignoreCase = true)
                            }
                        }

                        if (filteredItems.isEmpty()) {
                            ShelfSearchEmptyView(query = query, onClear = { onSearchChanged("") })
                        } else {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                if (selectedShelf == ShelfType.MY_WRITINGS) {
                                    item(key = "library_create_book_banner") {
                                        Card(
                                            onClick = onCreateBookClick,
                                            shape = RoundedCornerShape(16.dp),
                                            colors = CardDefaults.cardColors(
                                                containerColor = MaterialTheme.colorScheme.surface
                                            ),
                                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .testTag("library_my_writings_create_banner")
                                        ) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    modifier = Modifier.weight(1f)
                                                ) {
                                                    Surface(
                                                        shape = CircleShape,
                                                        color = MaterialTheme.colorScheme.primary,
                                                        modifier = Modifier.size(36.dp)
                                                    ) {
                                                        Box(contentAlignment = Alignment.Center) {
                                                            Icon(
                                                                Icons.Default.Add,
                                                                contentDescription = null,
                                                                tint = MaterialTheme.colorScheme.onPrimary,
                                                                modifier = Modifier.size(20.dp)
                                                            )
                                                        }
                                                    }
                                                    Spacer(Modifier.width(12.dp))
                                                    Column {
                                                        Text(
                                                            text = "Yeni Kitap Oluştur",
                                                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                                            color = MaterialTheme.colorScheme.onSurface
                                                        )
                                                        Text(
                                                            text = "Kendi hikayeni yazmaya hemen başla",
                                                            style = MaterialTheme.typography.bodySmall,
                                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                                        )
                                                    }
                                                }
                                                Button(
                                                    onClick = onCreateBookClick,
                                                    shape = RoundedCornerShape(10.dp),
                                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                                    colors = ButtonDefaults.buttonColors(
                                                        containerColor = MaterialTheme.colorScheme.primary,
                                                        contentColor = MaterialTheme.colorScheme.onPrimary
                                                    )
                                                ) {
                                                    Text("Oluştur", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
                                                }
                                            }
                                        }
                                    }
                                }

                                itemsIndexed(filteredItems, key = { _, it -> it.id.ifBlank { "${it.shelfType}_${it.book.id}" } }) { index, item ->
                                    LibraryBookCard(
                                        item = item,
                                        shelfType = selectedShelf,
                                        onClick = { onBookClick(item.book, selectedShelf) },
                                        onStartReading = { onStartReading(item.book) },
                                        onRemove = { onRemoveFromShelf(item.book, selectedShelf) },
                                        index = index
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LibraryBookCard(
    item: UserShelfItem,
    shelfType: ShelfType,
    onClick: () -> Unit,
    onStartReading: () -> Unit,
    onRemove: () -> Unit,
    index: Int = 0
) {
    val book = item.book

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .staggeredVaporEntrance(index = index)
            .appleSquircle(16.dp)
            .appleBouncy(pressedScale = 0.97f, onClick = onClick)
            .testTag("library_book_card_${book.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Book Cover
            BookCover(
                book = book,
                modifier = Modifier
                    .width(68.dp)
                    .aspectRatio(0.68f)
                    .clip(RoundedCornerShape(8.dp))
            )

            Spacer(modifier = Modifier.width(14.dp))

            // Book Details & Actions
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = book.title.ifBlank { "Başlıksız Kitap" },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    if (shelfType != ShelfType.MY_WRITINGS) {
                        IconButton(
                            onClick = onRemove,
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.DeleteOutline,
                                contentDescription = "Kaldır",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                Text(
                    text = book.authorName.ifBlank { "Bilinmeyen Yazar" },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(6.dp))

                when (shelfType) {
                    ShelfType.READING -> {
                        // Progress bar + Percentage
                        val percent = item.progressPercent.coerceIn(0, 100)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "İlerleme",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "%$percent",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        LinearProgressIndicator(
                            progress = percent / 100f,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(5.dp)
                                .clip(RoundedCornerShape(3.dp))
                        )

                        // Last reading activity
                        val lastActivity = formatRelativeTime(if (item.updatedAt > 0) item.updatedAt else item.addedAt)
                        if (lastActivity.isNotBlank()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.History,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                    modifier = Modifier.size(12.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Son okuma: $lastActivity",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            Button(
                                onClick = onStartReading,
                                shape = RoundedCornerShape(12.dp),
                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 4.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary
                                )
                            ) {
                                Icon(
                                    Icons.Default.PlayArrow,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (percent > 0) "Devam Et" else "Oku",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }

                    ShelfType.SAVED -> {
                        // Rating & category
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.Star,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(
                                    text = String.format(Locale.getDefault(), "%.1f", book.rating),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Text(
                                text = "•",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Text(
                                text = book.category.displayName,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            Button(
                                onClick = onClick,
                                shape = RoundedCornerShape(12.dp),
                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 4.dp)
                            ) {
                                Icon(
                                    Icons.Default.AutoStories,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "İncele & Oku",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }

                    ShelfType.MY_WRITINGS -> {
                        // Publication status & metrics
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            val statusText = when (book.status) {
                                BookStatus.PUBLISHED -> "YAYINDA"
                                BookStatus.DRAFT -> "TASLAK"
                                BookStatus.ARCHIVED -> "ARŞİV"
                            }
                            val statusBg = when (book.status) {
                                BookStatus.PUBLISHED -> MaterialTheme.colorScheme.primaryContainer
                                BookStatus.DRAFT -> MaterialTheme.colorScheme.secondaryContainer
                                BookStatus.ARCHIVED -> MaterialTheme.colorScheme.surfaceVariant
                            }
                            val statusColor = when (book.status) {
                                BookStatus.PUBLISHED -> MaterialTheme.colorScheme.onPrimaryContainer
                                BookStatus.DRAFT -> MaterialTheme.colorScheme.onSecondaryContainer
                                BookStatus.ARCHIVED -> MaterialTheme.colorScheme.onSurfaceVariant
                            }

                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(statusBg)
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = statusText,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = statusColor
                                )
                            }

                            Text(
                                text = "${book.readCount} okuma",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            OutlinedButton(
                                onClick = onClick,
                                shape = RoundedCornerShape(12.dp),
                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 4.dp)
                            ) {
                                Icon(
                                    Icons.Default.Edit,
                                    contentDescription = null,
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Düzenle",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ShelfLoadingView(title: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(36.dp),
            color = MaterialTheme.colorScheme.primary,
            strokeWidth = 3.dp
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun ShelfEmptyView(
    shelf: ShelfType,
    onWrite: () -> Unit,
    onDiscover: () -> Unit
) {
    val icon = when (shelf) {
        ShelfType.MY_WRITINGS -> Icons.Default.Add
        ShelfType.READING -> Icons.Default.AutoStories
        ShelfType.SAVED -> Icons.Default.BookmarkBorder
    }
    val title = when (shelf) {
        ShelfType.MY_WRITINGS -> "Henüz bir kitap yazmadın"
        ShelfType.READING -> "Şu anda okuduğun kitap yok"
        ShelfType.SAVED -> "Henüz kaydedilmiş kitabın yok"
    }
    val message = when (shelf) {
        ShelfType.MY_WRITINGS -> "Kendi hikayeni oluştur ve binlerce okurla paylaş."
        ShelfType.READING -> "Keşfet bölümünden yeni kitaplar bulup okumaya başlayabilirsin."
        ShelfType.SAVED -> "Beğendiğin kitapları yer imine ekleyerek daha sonra kolayca bul."
    }
    val actionText = when (shelf) {
        ShelfType.MY_WRITINGS -> "Yeni Kitap Oluştur"
        ShelfType.READING -> "Kitapları Keşfet"
        ShelfType.SAVED -> "Kitapları Keşfet"
    }
    val onAction = when (shelf) {
        ShelfType.MY_WRITINGS -> onWrite
        ShelfType.READING -> onDiscover
        ShelfType.SAVED -> onDiscover
    }

    ShelfMessageCard(
        icon = icon,
        title = title,
        message = message,
        action = actionText,
        onAction = onAction
    )
}

@Composable
private fun ShelfSearchEmptyView(query: String, onClear: () -> Unit) {
    ShelfMessageCard(
        icon = Icons.Default.Search,
        title = "Sonuç bulunamadı",
        message = "\"$query\" aramasıyla eşleşen kitap bulunamadı.",
        action = "Aramayı Temizle",
        onAction = onClear
    )
}

@Composable
private fun ShelfMessageCard(
    icon: ImageVector,
    title: String,
    message: String,
    action: String?,
    onAction: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(36.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(32.dp)
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = message,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
        if (action != null) {
            Spacer(modifier = Modifier.height(18.dp))
            Button(
                onClick = onAction,
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
                modifier = if (action == "Yeni Kitap Oluştur") Modifier.testTag("library_empty_create_book_btn") else Modifier
            ) {
                if (action == "Yeni Kitap Oluştur") {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                }
                Text(action, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

private fun formatRelativeTime(timestamp: Long): String {
    if (timestamp <= 0L) return ""
    val now = System.currentTimeMillis()
    val diff = (now - timestamp).coerceAtLeast(0)
    val minutes = diff / (1000 * 60)
    val hours = diff / (1000 * 60 * 60)
    val days = diff / (1000 * 60 * 60 * 24)
    return when {
        minutes < 1 -> "Az önce"
        minutes < 60 -> "$minutes dk önce"
        hours < 24 -> "$hours saat önce"
        days == 1L -> "Dün"
        days < 30 -> "$days gün önce"
        else -> {
            val sdf = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
            sdf.format(Date(timestamp))
        }
    }
}
