package com.libra.app.feature.reader

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.libra.app.core.di.ServiceLocator
import com.libra.app.core.result.AppResult
import com.libra.app.domain.model.*
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

@Composable
fun BookReaderScreen(
    book: Book,
    userId: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    val scope = rememberCoroutineScope()
    var chapters by remember { mutableStateOf<List<Chapter>>(emptyList()) }
    var progress by remember { mutableStateOf<ReadingProgress?>(null) }
    var selectedIndex by remember { mutableStateOf(0) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var saved by remember { mutableStateOf(false) }
    var restoredChapterKey by remember { mutableStateOf("") }

    LaunchedEffect(book.id) {
        ServiceLocator.bookRepository.getBookChapters(book.id).collectLatest { result ->
            when (result) {
                is AppResult.Success -> {
                    chapters = result.data.filter { it.isPublished }.sortedBy { it.chapterNumber }
                    loading = false
                    error = null
                }
                is AppResult.Error -> {
                    loading = false
                    error = result.error.message
                }
            }
        }
    }

    LaunchedEffect(book.id, userId) {
        ServiceLocator.bookRepository.getReadingProgress(userId, book.id).collectLatest { result ->
            if (result is AppResult.Success) {
                progress = result.data
                val index = chapters.indexOfFirst { it.id == result.data?.chapterId }
                if (index >= 0) selectedIndex = index
                saved = result.data != null
            }
        }
    }

    LaunchedEffect(chapters, selectedIndex, progress?.chapterId) {
        val target = chapters.getOrNull(selectedIndex) ?: return@LaunchedEffect
        val restoreKey = book.id + ":" + target.id
        if (restoredChapterKey == restoreKey) return@LaunchedEffect
        val stored = progress
        kotlinx.coroutines.yield()
        scrollState.scrollTo(
            if (stored?.chapterId == target.id) stored.position.coerceAtMost(scrollState.maxValue) else 0
        )
        restoredChapterKey = restoreKey
    }

    LaunchedEffect(selectedIndex, chapters) {
        snapshotFlow { scrollState.value }
            .map { it }
            .distinctUntilChanged()
            .collect { position ->
                val chapter = chapters.getOrNull(selectedIndex) ?: return@collect
                if (scrollState.maxValue <= 0) return@collect
                val percent = ((position.toFloat() / scrollState.maxValue) * 100f).toInt().coerceIn(0, 100)
                if (percent % 5 != 0 && position != scrollState.maxValue) return@collect
                val result = ServiceLocator.bookRepository.saveReadingProgress(
                    ReadingProgress(
                        userId = userId,
                        bookId = book.id,
                        chapterId = chapter.id,
                        chapterNumber = chapter.chapterNumber,
                        progressPercent = percent,
                        position = position
                    )
                )
                if (result is AppResult.Success) {
                    progress = result.data
                    saved = true
                }
            }
    }

    val chapter = chapters.getOrNull(selectedIndex)
    val chapterProgress = if (scrollState.maxValue > 0)
        (scrollState.value.toFloat() / scrollState.maxValue.toFloat()).coerceIn(0f, 1f)
    else 0f

    Box(modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Column(Modifier.fillMaxSize()) {
            Surface(tonalElevation = 2.dp) {
                Row(
                    Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 8.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Geri")
                    }
                    Column(Modifier.weight(1f)) {
                        Text(book.title, maxLines = 1, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text(
                            chapter?.let { "Bölüm \${it.chapterNumber} • \${it.title}" } ?: "Okuyucu",
                            maxLines = 1,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = {
                        scope.launch {
                            if (ServiceLocator.bookRepository.addBookToShelf(userId, book.id, ShelfType.SAVED) is AppResult.Success) {
                                saved = true
                            }
                        }
                    }) {
                        Icon(if (saved) Icons.Filled.Bookmark else Icons.Filled.BookmarkBorder, "Kütüphaneye kaydet")
                    }
                }
            }

            when {
                loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
                error != null -> Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                    Text(error.orEmpty(), color = MaterialTheme.colorScheme.error)
                }
                chapter == null -> Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                    Text("Bu kitapta henüz yayımlanmış bölüm yok.")
                }
                else -> {
                    LinearProgressIndicator(
                        progress = { chapterProgress },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Column(
                        Modifier.fillMaxSize().verticalScroll(scrollState).padding(horizontal = 22.dp, vertical = 28.dp)
                    ) {
                        Text(
                            "Bölüm \${chapter.chapterNumber}",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(chapter.title.ifBlank { "İsimsiz Bölüm" }, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(26.dp))
                        Text(chapter.content, style = MaterialTheme.typography.bodyLarge, lineHeight = MaterialTheme.typography.bodyLarge.lineHeight)
                        Spacer(Modifier.height(100.dp))
                    }
                }
            }
        }

        if (chapter != null) {
            Surface(
                modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().navigationBarsPadding().padding(12.dp),
                shape = RoundedCornerShape(20.dp),
                tonalElevation = 6.dp
            ) {
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        enabled = selectedIndex > 0,
                        onClick = {
                            selectedIndex--
                            scope.launch { scrollState.scrollTo(0) }
                        }
                    ) { Icon(Icons.Filled.KeyboardArrowLeft, "Önceki bölüm") }

                    Text("Bölüm geçişi", style = MaterialTheme.typography.labelLarge)

                    IconButton(
                        enabled = selectedIndex < chapters.lastIndex,
                        onClick = {
                            selectedIndex++
                            scope.launch { scrollState.scrollTo(0) }
                        }
                    ) { Icon(Icons.Filled.KeyboardArrowRight, "Sonraki bölüm") }
                }
            }
        }
    }
}
