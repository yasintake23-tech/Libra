package com.libra.app.feature.book

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.libra.app.core.di.ServiceLocator
import com.libra.app.domain.model.*

@Composable
fun BookDetailScreen(
    book: Book,
    userId: String,
    onBack: () -> Unit,
    onRead: (Book, Chapter?) -> Unit,
    onOpenChapter: (Book, Chapter) -> Unit,
    onOpenAuthor: ((String) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val vm: BookDetailViewModel = viewModel(key = "book-detail-${book.id}")
    val state by vm.state.collectAsState()
    LaunchedEffect(book.id, userId) { vm.load(book, userId) }

    Box(modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        when {
            state.loading && state.chapters.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            state.error != null && state.chapters.isEmpty() -> Column(
                Modifier.fillMaxSize().padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(state.error.orEmpty(), color = MaterialTheme.colorScheme.error)
                Spacer(Modifier.height(12.dp))
                OutlinedButton(onClick = { vm.clear(); vm.load(book, userId) }) { Text("Tekrar dene") }
            }
            else -> BookDetailContent(
                book = state.book ?: book,
                chapters = state.chapters,
                progress = state.progress,
                author = state.author,
                onBack = onBack,
                onRead = onRead,
                onOpenChapter = onOpenChapter,
                onOpenAuthor = onOpenAuthor
            )
        }
    }
}

@Composable
private fun BookDetailContent(
    book: Book,
    chapters: List<Chapter>,
    progress: ReadingProgress?,
    author: UserProfile?,
    onBack: () -> Unit,
    onRead: (Book, Chapter?) -> Unit,
    onOpenChapter: (Book, Chapter) -> Unit,
    onOpenAuthor: ((String) -> Unit)?
) {
    val coverUrl = ServiceLocator.storageRepository.getPublicCdnUrl(book.coverImageUrl)
    val authorPhoto = author?.profileImageUrl?.let { ServiceLocator.storageRepository.getPublicCdnUrl(it) }.orEmpty()
    val progressPercent = progress?.progressPercent?.coerceIn(0, 100) ?: 0
    val continueChapter = progress?.chapterId?.let { id -> chapters.firstOrNull { it.id == id } }
        ?: progress?.chapterNumber?.let { n -> chapters.firstOrNull { it.chapterNumber == n } }
    val primaryChapter = continueChapter ?: chapters.firstOrNull()

    Column(Modifier.fillMaxSize()) {
        Row(
            Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Geri") }
            Text("Kitap", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
        }

        LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 8.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                    AsyncImage(
                        model = coverUrl.takeIf { it.isNotBlank() },
                        contentDescription = book.title,
                        modifier = Modifier.width(190.dp).height(270.dp).clip(RoundedCornerShape(18.dp)),
                        contentScale = ContentScale.Crop
                    )
                    Spacer(Modifier.height(16.dp))
                    Text(book.title, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(6.dp))
                    Row(
                        Modifier.clip(RoundedCornerShape(24.dp))
                            .clickable(enabled = author != null && onOpenAuthor != null) {
                                author?.let { onOpenAuthor?.invoke(it.uid) }
                            }
                            .padding(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (authorPhoto.isNotBlank()) {
                            AsyncImage(authorPhoto, author?.displayName ?: book.authorName, Modifier.size(32.dp).clip(CircleShape), contentScale = ContentScale.Crop)
                        } else {
                            Surface(Modifier.size(32.dp), shape = CircleShape) {
                                Box(contentAlignment = Alignment.Center) { Icon(Icons.Default.Person, null) }
                            }
                        }
                        Spacer(Modifier.width(8.dp))
                        Column {
                            Text(author?.displayName?.ifBlank { book.authorName } ?: book.authorName, fontWeight = FontWeight.SemiBold)
                            author?.username?.takeIf { it.isNotBlank() }?.let { Text("@$it", style = MaterialTheme.typography.labelSmall) }
                        }
                    }
                }
            }
            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StatChip("Bölüm", chapters.size.toString(), Modifier.weight(1f))
                    StatChip("Okunma", book.readCount.toString(), Modifier.weight(1f))
                    StatChip("Beğeni", book.likesCount.toString(), Modifier.weight(1f))
                }
            }
            item {
                Text(book.category.displayName, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                if (book.description.isNotBlank()) {
                    Spacer(Modifier.height(8.dp))
                    Text(book.description, style = MaterialTheme.typography.bodyLarge)
                }
                if (book.discoverySummary.isNotBlank()) {
                    Spacer(Modifier.height(12.dp))
                    Text("Keşfet özeti", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(4.dp))
                    Text(book.discoverySummary, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            item {
                if (progress != null && chapters.isNotEmpty()) {
                    Surface(shape = RoundedCornerShape(16.dp), tonalElevation = 2.dp, modifier = Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(16.dp)) {
                            Text("Okumaya devam et", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.height(4.dp))
                            Text("Bölüm ${continueChapter?.chapterNumber ?: progress.chapterNumber} • %$progressPercent", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(Modifier.height(10.dp))
                            LinearProgressIndicator({ progressPercent / 100f }, Modifier.fillMaxWidth())
                            Spacer(Modifier.height(12.dp))
                            Button(onClick = { onRead(book, primaryChapter) }, Modifier.fillMaxWidth()) {
                                Icon(Icons.Default.MenuBook, null)
                                Spacer(Modifier.width(8.dp))
                                Text("Devam Et")
                            }
                        }
                    }
                } else {
                    Button(
                        enabled = primaryChapter != null,
                        onClick = { primaryChapter?.let { onRead(book, it) } },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.MenuBook, null)
                        Spacer(Modifier.width(8.dp))
                        Text(if (chapters.isEmpty()) "Henüz yayınlanmış bölüm yok" else "İlk Bölümü Oku")
                    }
                }
            }
            item { Text("Bölümler", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold) }
            if (chapters.isEmpty()) {
                item { Text("Bu kitapta henüz yayınlanmış bölüm bulunmuyor.", color = MaterialTheme.colorScheme.onSurfaceVariant) }
            } else {
                items(chapters, key = { it.id }) { chapter ->
                    Surface(
                        Modifier.fillMaxWidth().clickable { onOpenChapter(book, chapter) },
                        shape = RoundedCornerShape(14.dp),
                        tonalElevation = if (continueChapter?.id == chapter.id) 2.dp else 0.dp
                    ) {
                        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                            Surface(Modifier.size(38.dp), shape = CircleShape, color = MaterialTheme.colorScheme.surfaceVariant) {
                                Box(contentAlignment = Alignment.Center) { Text(chapter.chapterNumber.toString(), fontWeight = FontWeight.Bold) }
                            }
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text(chapter.title.ifBlank { "İsimsiz Bölüm" }, fontWeight = if (continueChapter?.id == chapter.id) FontWeight.Bold else FontWeight.Medium)
                                Text("${chapter.wordCount} kelime", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            if (continueChapter?.id == chapter.id) Icon(Icons.Default.BookmarkBorder, "Devam edilen bölüm")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatChip(label: String, value: String, modifier: Modifier = Modifier) {
    Surface(modifier, shape = RoundedCornerShape(14.dp), tonalElevation = 1.dp) {
        Column(Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(value, fontWeight = FontWeight.Bold)
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
