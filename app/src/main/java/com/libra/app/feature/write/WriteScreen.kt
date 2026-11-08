package com.libra.app.feature.write

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.libra.app.core.state.UiState
import com.libra.app.domain.model.Book
import com.libra.app.domain.model.BookStatus
import com.libra.app.ui.components.AppButton
import com.libra.app.ui.components.AppButtonVariant
import com.libra.app.ui.components.BookCover

@Composable
fun WriteScreen(
    uiState: UiState<WriteState>,
    onCreateBookClick: () -> Unit,
    onBookClick: (Book) -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 14.dp),
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
            IconButton(onClick = onCreateBookClick) {
                Icon(Icons.Default.Add, "Yeni kitap")
            }
        }

        when (uiState) {
            is UiState.Loading -> {
                Text(
                    "Kitapların yükleniyor…",
                    modifier = Modifier.padding(20.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            is UiState.Error -> {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(uiState.error.message, color = MaterialTheme.colorScheme.error)
                    TextButton(onClick = onRetry) { Text("Tekrar dene") }
                }
            }
            is UiState.Empty -> AuthorStudioEmpty(onCreate = onCreateBookClick)
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
                                    "Yeni kitabını oluştur. Kapsamlı editörden bölümlerini düzenleyebilirsin.",
                                    modifier = Modifier.padding(top = 5.dp),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                AppButton(
                                    "Yeni kitap oluştur",
                                    onClick = onCreateBookClick,
                                    variant = AppButtonVariant.PRIMARY,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 12.dp)
                                        .height(44.dp)
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
                        item { AuthorStudioEmpty(onCreate = onCreateBookClick) }
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
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onOpenEditor),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            BookCover(book, Modifier.width(64.dp).aspectRatio(.68f))
            Column(Modifier.padding(start = 14.dp).weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = if (book.status == BookStatus.PUBLISHED) Color(0xFF2E7D32).copy(alpha = 0.15f) else Color(0xFFF57C00).copy(alpha = 0.15f)
                    ) {
                        Text(
                            if (book.status == BookStatus.PUBLISHED) "Yayında" else "Taslak",
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = if (book.status == BookStatus.PUBLISHED) Color(0xFF2E7D32) else Color(0xFFF57C00)
                        )
                    }
                    Text(
                        book.category.displayName,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Spacer(Modifier.height(4.dp))
                Text(book.title.ifBlank { "Adsız Kitap" }, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, maxLines = 1)
                Spacer(Modifier.height(2.dp))
                Text(
                    "${if (book.chapterCount > 0) book.chapterCount else 1} Bölüm • ${book.readCount} Okunma",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Button(
                onClick = onOpenEditor,
                shape = RoundedCornerShape(10.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text("Yönet", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
            }
        }
    }
}

@Composable
private fun AuthorStudioEmpty(onCreate: () -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .padding(24.dp),
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
