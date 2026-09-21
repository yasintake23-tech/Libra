package com.libra.app.feature.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.PeopleOutline
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.libra.app.core.state.UiState
import com.libra.app.domain.model.Book
import com.libra.app.domain.model.BookCategory
import com.libra.app.domain.model.UserProfile
import com.libra.app.domain.model.UserShelfItem
import com.libra.app.ui.components.BookCover
import com.libra.app.ui.components.HorizontalBookCard
import com.libra.app.ui.components.LoadingView
import com.libra.app.ui.components.SectionHeader
import com.libra.app.ui.components.UserAvatar
import com.libra.app.ui.components.VerticalBookCard

@Composable
fun HomeScreen(
    uiState: UiState<HomeData>,
    onBookClick: (Book) -> Unit,
    onNavigateToWrite: () -> Unit,
    onNavigateToLibrary: () -> Unit,
    onNavigateToProfile: () -> Unit,
    onNavigateToDiscover: () -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    when (uiState) {
        is UiState.Loading -> LoadingView(message = "Kitaplığın hazırlanıyor…")
        is UiState.Error -> HomeError(uiState.error.message, onRetry)
        is UiState.Empty -> LoadingView(message = "Hazırlanıyor…")
        is UiState.Success -> {
            val data = uiState.data
            var selectedCategory by remember { mutableStateOf<BookCategory?>(null) }

            LazyColumn(modifier = modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 18.dp)) {
                item { HomeHeader(data.currentUser, onNavigateToProfile) }
                item { SearchBar(onNavigateToDiscover) }
                item { HeroCard() }
                item { QuickActions(onNavigateToDiscover, onNavigateToWrite, onNavigateToLibrary, onNavigateToDiscover) }

                data.currentlyReading?.let { reading ->
                    item { SectionHeader("Devam Et", actionLabel = "Kütüphane", onActionClick = onNavigateToLibrary) }
                    item { CurrentlyReadingCard(reading, { onBookClick(reading.book) }, Modifier.padding(horizontal = 16.dp)) }
                }

                item { SectionHeader("Senin İçin", actionLabel = "Tümünü Gör", onActionClick = onNavigateToDiscover) }
                item {
                    if (data.featuredBooks.isEmpty()) {
                        EmptyStrip("Yeni kitaplar burada görünecek.")
                    } else {
                        LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            items(data.featuredBooks.take(10), key = { it.id }) { book ->
                                VerticalBookCard(book, { onBookClick(book) })
                            }
                        }
                    }
                }

                item { SectionHeader("Popüler Kategoriler", actionLabel = "Tümü", onActionClick = onNavigateToDiscover) }
                item {
                    CategoryRow(selectedCategory) { category ->
                        selectedCategory = if (selectedCategory == category) null else category
                    }
                }

                item { SectionHeader("Yeni Eklenenler", subtitle = "Libra'daki son yayınlar") }
                val recent = if (selectedCategory == null) data.recentBooks else data.recentBooks.filter { it.category == selectedCategory }
                if (recent.isEmpty()) {
                    item { EmptyStrip("Henüz yayınlanmış bir kitap yok.") }
                } else {
                    items(recent.take(8), key = { it.id }) { book ->
                        HorizontalBookCard(book, { onBookClick(book) }, Modifier.padding(horizontal = 16.dp, vertical = 4.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun HomeHeader(user: UserProfile?, onProfile: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(34.dp).clip(CircleShape).background(MaterialTheme.colorScheme.onBackground), contentAlignment = Alignment.Center) {
                Icon(Icons.Default.MenuBook, null, tint = MaterialTheme.colorScheme.background, modifier = Modifier.size(19.dp))
            }
            Spacer(Modifier.width(10.dp))
            Column {
                Text("Libra", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold, letterSpacing = (-0.4).sp))
                Text("Her hikâyenin bir yeri var.", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        UserAvatar(user?.profileImageUrl, user?.initials ?: "L", size = 40.dp, modifier = Modifier.clickable(onClick = onProfile))
    }
}

@Composable
private fun SearchBar(onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 2.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surface)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Default.Search, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(10.dp))
        Text("Kitap, yazar veya kullanıcı ara…", color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun HeroCard() {
    Box(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp)
            .height(154.dp).clip(RoundedCornerShape(18.dp))
            .background(Brush.linearGradient(listOf(Color(0xFF20262D), Color(0xFF697382))))
    ) {
        Column(modifier = Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.Center) {
            Text("“Daha iyi bir sen,\nher zaman bir kitap uzaklıktadır.”", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Medium, lineHeight = 28.sp), color = Color.White)
            Spacer(Modifier.height(10.dp))
            Text("LIBRA", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = .7f))
        }
    }
}

@Composable
private fun QuickActions(onDiscover: () -> Unit, onWrite: () -> Unit, onLibrary: () -> Unit, onFriends: () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        ActionTile(Icons.Default.Search, "Keşfet", onDiscover)
        ActionTile(Icons.Default.Edit, "Yaz", onWrite)
        ActionTile(Icons.Default.BookmarkBorder, "Kütüphane", onLibrary)
        ActionTile(Icons.Default.PeopleOutline, "Arkadaşlar", onFriends)
    }
}

@Composable
private fun ActionTile(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, onClick: () -> Unit) {
    Card(modifier = Modifier.weight(1f).clickable(onClick = onClick), shape = RoundedCornerShape(13.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(icon, null, modifier = Modifier.size(22.dp))
            Spacer(Modifier.height(5.dp))
            Text(label, style = MaterialTheme.typography.labelSmall)
        }
    }
}

@Composable
private fun CurrentlyReadingCard(item: UserShelfItem, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Card(modifier = modifier.fillMaxWidth().clickable(onClick = onClick), shape = RoundedCornerShape(14.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            BookCover(item.book, Modifier.width(52.dp).aspectRatio(.69f))
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(item.book.title, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold), maxLines = 1)
                Text(item.book.authorName, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(9.dp))
                LinearProgressIndicator(progress = { item.progressPercent / 100f }, modifier = Modifier.fillMaxWidth().height(4.dp).clip(CircleShape))
                Spacer(Modifier.height(4.dp))
                Text("Okuma ilerlemen", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun CategoryRow(selected: BookCategory?, onSelected: (BookCategory) -> Unit) {
    Row(modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        BookCategory.values().take(6).forEach { category ->
            val active = selected == category
            Box(
                modifier = Modifier.clip(RoundedCornerShape(10.dp))
                    .background(if (active) MaterialTheme.colorScheme.onBackground else MaterialTheme.colorScheme.surface)
                    .clickable { onSelected(category) }
                    .padding(horizontal = 15.dp, vertical = 10.dp)
            ) {
                Text(category.displayName, style = MaterialTheme.typography.labelMedium, color = if (active) MaterialTheme.colorScheme.background else MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun EmptyStrip(text: String) {
    Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp).clip(RoundedCornerShape(13.dp)).background(MaterialTheme.colorScheme.surface).padding(18.dp)) {
        Text(text, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun HomeError(message: String, onRetry: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Text("Bir sorun oluştu", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold))
        Spacer(Modifier.height(8.dp))
        Text(message, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        TextButton(onClick = onRetry) { Text("Tekrar dene") }
    }
}
