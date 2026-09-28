package com.libra.app.feature.book

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.libra.app.core.di.ServiceLocator
import com.libra.app.core.result.AppResult
import com.libra.app.domain.model.Book
import com.libra.app.domain.model.ReadingProgress
import com.libra.app.domain.model.ShelfType
import kotlinx.coroutines.delay

@Composable
fun BookReaderScreen(book: Book, userId: String, onBack: () -> Unit, modifier: Modifier = Modifier) {
    val chaptersResult by ServiceLocator.bookRepository.getBookChapters(book.id).collectAsState(initial = AppResult.Success(emptyList()))
    val progressResult by ServiceLocator.bookRepository.getReadingProgress(userId, book.id).collectAsState(initial = AppResult.Success(null))
    val chapters = (chaptersResult as? AppResult.Success)?.data.orEmpty().filter { it.isPublished || book.ownerId == userId }
    val savedProgress = (progressResult as? AppResult.Success)?.data
    var selectedChapter by remember { mutableIntStateOf(0) }
    var showChapters by remember { mutableStateOf(false) }
    var saveError by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(chapters, savedProgress) {
        if (chapters.isNotEmpty() && savedProgress != null) {
            val index = chapters.indexOfFirst { it.id == savedProgress.chapterId }
            selectedChapter = if (index >= 0) index else (savedProgress.chapterNumber - 1).coerceIn(0, chapters.lastIndex)
        }
    }

    BackHandler(onBack = onBack)

    if (showChapters) {
        AlertDialog(
            onDismissRequest = { showChapters = false },
            title = { Text("Bölümler") },
            text = {
                LazyColumn {
                    itemsIndexed(chapters) { index, item ->
                        TextButton(
                            onClick = { selectedChapter = index; showChapters = false },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("${item.chapterNumber}. ${item.title}")
                                if (index == selectedChapter) Text("Okunuyor", color = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showChapters = false }) { Text("Kapat") } }
        )
    }

    if (chapters.isEmpty()) {
        Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            if (chaptersResult is AppResult.Error) Text("Bölümler yüklenemedi.") else CircularProgressIndicator()
        }
        return
    }

    val chapter = chapters[selectedChapter]
    val listState = rememberLazyListState()
    var lastSavedIndex by remember(chapter.id) { mutableIntStateOf(-1) }

    LaunchedEffect(chapter.id, savedProgress) {
        if (savedProgress?.chapterId == chapter.id && savedProgress.position > 0) {
            listState.scrollToItem(savedProgress.position)
        } else {
            listState.scrollToItem(0)
        }
    }

    LaunchedEffect(chapter.id, listState.firstVisibleItemIndex) {
        val index = listState.firstVisibleItemIndex
        if (index == lastSavedIndex) return@LaunchedEffect
        lastSavedIndex = index
        delay(900)
        val percent = (((selectedChapter + 1f) / chapters.size) * 100f).toInt().coerceIn(0, 100)
        val progress = ReadingProgress(
            userId = userId,
            bookId = book.id,
            chapterId = chapter.id,
            chapterNumber = chapter.chapterNumber,
            progressPercent = percent,
            position = index
        )
        ServiceLocator.bookRepository.addBookToShelf(userId, book.id, ShelfType.READING)
        when (ServiceLocator.bookRepository.saveReadingProgress(progress)) {
            is AppResult.Success -> saveError = null
            is AppResult.Error -> saveError = "İlerleme kaydedilemedi."
        }
    }

    Column(modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Geri") }
            Column(Modifier.weight(1f)) {
                Text(book.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text("Bölüm ${chapter.chapterNumber} • ${chapter.title}", style = MaterialTheme.typography.labelMedium)
            }
            IconButton(onClick = { showChapters = true }) { Icon(Icons.Default.MenuBook, "Bölümler") }
        }

        LinearProgressIndicator(
            progress = { ((selectedChapter + 1).toFloat() / chapters.size).coerceIn(0f, 1f) },
            modifier = Modifier.fillMaxWidth()
        )

        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Spacer(Modifier.height(12.dp))
                Text(chapter.title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            }
            val blocks = chapter.content.split("\n\n").filter { it.isNotBlank() }
            itemsIndexed(blocks) { _, block ->
                if (block.startsWith("[[IMAGE:") && block.contains("|")) {
                    val url = block.substringAfter("[[IMAGE:").substringBefore("|")
                    AsyncImage(
                        model = ServiceLocator.storageRepository.getPublicCdnUrl(url),
                        contentDescription = "Bölüm görseli",
                        modifier = Modifier.fillMaxWidth().clip(MaterialTheme.shapes.medium)
                    )
                } else {
                    Text(block.replace("**", "").replace("_", ""), style = MaterialTheme.typography.bodyLarge)
                }
            }
            item {
                Spacer(Modifier.height(24.dp))
                if (selectedChapter < chapters.lastIndex) {
                    TextButton(
                        onClick = { selectedChapter++; },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Sonraki bölüme geç") }
                } else {
                    Text("Kitabın sonuna geldin.", modifier = Modifier.padding(bottom = 32.dp))
                }
                saveError?.let { Text(it, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error) }
            }
        }
    }
}
