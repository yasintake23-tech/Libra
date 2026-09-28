package com.libra.app.feature.reader

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.libra.app.domain.model.Book
import com.libra.app.domain.model.Chapter
import com.libra.app.domain.model.ReadingProgress
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookReaderScreen(
    book: Book,
    userId: String,
    initialChapterId: String? = null,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val vm: BookReaderViewModel = viewModel(key = "reader-${book.id}-${userId}")
    val state by vm.state.collectAsState()
    val context = LocalContext.current
    val store = remember { ReaderPreferencesStore(context) }
    var preferences by remember { mutableStateOf(store.read()) }
    var selectedIndex by remember(book.id, initialChapterId) { mutableStateOf(0) }
    var showSettings by remember { mutableStateOf(false) }
    var showChapters by remember { mutableStateOf(false) }
    var restored by remember(book.id, initialChapterId) { mutableStateOf(false) }
    var lastLocalKey by remember { mutableStateOf("") }
    var saveJob by remember { mutableStateOf<Job?>(null) }
    val scrollState = rememberScrollState()
    val scope = rememberCoroutineScope()

    LaunchedEffect(book.id, userId) {
        vm.load(book, userId, store)
    }

    val chapters = state.chapters
    val progress = state.progress

    LaunchedEffect(chapters, initialChapterId, progress?.chapterId) {
        if (chapters.isEmpty() || restored) return@LaunchedEffect
        val preferredId = initialChapterId?.takeIf { id -> chapters.any { it.id == id } }
            ?: progress?.chapterId?.takeIf { id -> chapters.any { it.id == id } }
        val preferredIndex = preferredId?.let { id -> chapters.indexOfFirst { it.id == id } } ?: -1
        selectedIndex = if (preferredIndex >= 0) {
            preferredIndex
        } else {
            chapters.indexOfFirst { it.chapterNumber == progress?.chapterNumber }.takeIf { it >= 0 } ?: 0
        }
        restored = true
    }

    val chapter = chapters.getOrNull(selectedIndex)
    var restorePositionForChapter by remember(chapter?.id, progress?.chapterId) { mutableStateOf(true) }

    LaunchedEffect(chapter?.id, progress?.chapterId, restored) {
        val current = chapter ?: return@LaunchedEffect
        delay(100)
        val shouldRestore = progress?.chapterId == current.id && restorePositionForChapter
        scrollState.scrollTo(if (shouldRestore) progress.position.coerceIn(0, scrollState.maxValue) else 0)
        restorePositionForChapter = false
    }

    fun currentProgress(force: Boolean = false): ReadingProgress? {
        val current = chapter ?: return null
        val max = scrollState.maxValue
        val percent = if (max > 0) {
            ((scrollState.value.toFloat() / max) * 100f).roundToInt().coerceIn(0, 100)
        } else {
            if (current.content.isNotBlank()) 100 else 0
        }
        val key = "${current.id}:${scrollState.value}:$percent"
        if (!force && key == lastLocalKey) return null
        lastLocalKey = key
        return ReadingProgress(
            userId = userId,
            bookId = book.id,
            chapterId = current.id,
            chapterNumber = current.chapterNumber,
            progressPercent = percent,
            position = scrollState.value
        )
    }

    fun saveNow(force: Boolean = false) {
        currentProgress(force)?.let { vm.saveProgress(it, store) }
    }

    LaunchedEffect(chapter?.id) {
        snapshotFlow { scrollState.value }
            .collect { position ->
                if (scrollState.maxValue <= 0 || chapter == null) return@collect
                val percent = ((position.toFloat() / scrollState.maxValue) * 100f).roundToInt()
                if (percent % 5 == 0 || position == scrollState.maxValue) {
                    saveJob?.cancel()
                    saveJob = launch {
                        delay(650)
                        saveNow()
                    }
                }
            }
    }

    DisposableEffect(chapter?.id) {
        onDispose {
            saveJob?.cancel()
            saveNow(force = true)
        }
    }

    val percent = if (scrollState.maxValue > 0) {
        ((scrollState.value.toFloat() / scrollState.maxValue) * 100f).roundToInt().coerceIn(0, 100)
    } else {
        if (chapter?.content?.isNotBlank() == true) 100 else 0
    }

    val background = when (preferences.theme) {
        ReaderTheme.LIGHT -> MaterialTheme.colorScheme.background
        ReaderTheme.DARK -> Color(0xFF111111)
        ReaderTheme.SEPIA -> Color(0xFFF4ECD8)
    }
    val foreground = when (preferences.theme) {
        ReaderTheme.LIGHT -> MaterialTheme.colorScheme.onBackground
        ReaderTheme.DARK -> Color(0xFFF0F0F0)
        ReaderTheme.SEPIA -> Color(0xFF463B2D)
    }
    val surface = when (preferences.theme) {
        ReaderTheme.LIGHT -> MaterialTheme.colorScheme.surface
        ReaderTheme.DARK -> Color(0xFF1B1B1B)
        ReaderTheme.SEPIA -> Color(0xFFEDE2C8)
    }

    Box(modifier.fillMaxSize().background(background)) {
        Column(Modifier.fillMaxSize()) {
            Surface(color = surface) {
                Row(
                    Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 6.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { saveNow(true); onBack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Geri", tint = foreground)
                    }
                    Column(Modifier.weight(1f)) {
                        Text(book.title, maxLines = 1, color = foreground, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text(
                            chapter?.let { "Bölüm ${it.chapterNumber} • ${it.title}" } ?: "Okuyucu",
                            maxLines = 1, color = foreground.copy(alpha = .7f), style = MaterialTheme.typography.labelSmall
                        )
                    }
                    IconButton(onClick = { showChapters = true }) { Icon(Icons.Default.List, "Bölümler", tint = foreground) }
                    IconButton(onClick = { showSettings = true }) { Icon(Icons.Default.Settings, "Okuma ayarları", tint = foreground) }
                }
            }

            if (preferences.showProgress) {
                LinearProgressIndicator({ percent / 100f }, Modifier.fillMaxWidth())
            }

            if (state.progressSyncError != null) {
                Text(
                    "İlerleme senkronize edilemedi. Okuman yerelde korunuyor.",
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.error
                )
            }

            when {
                state.loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
                state.error != null -> Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                    Text(state.error.orEmpty(), color = MaterialTheme.colorScheme.error)
                }
                chapter == null -> Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                    Text("Bu kitapta henüz yayınlanmış bölüm yok.", color = foreground)
                }
                else -> {
                    Column(
                        Modifier.fillMaxWidth().weight(1f).verticalScroll(scrollState),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Column(
                            Modifier.fillMaxWidth().widthIn(max = preferences.contentWidthDp.dp)
                                .padding(horizontal = 22.dp, vertical = 34.dp)
                        ) {
                            Text("Bölüm ${chapter.chapterNumber}", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.height(10.dp))
                            Text(chapter.title.ifBlank { "İsimsiz Bölüm" }, style = MaterialTheme.typography.headlineMedium, color = foreground, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.height(28.dp))
                            Text(
                                chapter.content,
                                style = TextStyle(
                                    fontSize = preferences.fontSizeSp.sp,
                                    lineHeight = (preferences.fontSizeSp * preferences.lineSpacing).sp,
                                    color = foreground
                                )
                            )
                            Spacer(Modifier.height(120.dp))
                        }
                    }

                    Surface(
                        Modifier.fillMaxWidth().navigationBarsPadding(),
                        color = surface,
                        tonalElevation = 5.dp
                    ) {
                        Row(
                            Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 7.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                enabled = selectedIndex > 0,
                                onClick = {
                                    saveNow(true)
                                    restorePositionForChapter = false
                                    selectedIndex--
                                    scope.launch { scrollState.scrollTo(0) }
                                }
                            ) { Icon(Icons.Default.KeyboardArrowLeft, "Önceki bölüm", tint = foreground) }

                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    if (percent >= 100 && selectedIndex == chapters.lastIndex) "Kitabı tamamladın" else "%$percent",
                                    color = foreground, fontWeight = FontWeight.SemiBold
                                )
                                Text("${selectedIndex + 1} / ${chapters.size}", color = foreground.copy(alpha = .7f), style = MaterialTheme.typography.labelSmall)
                            }

                            IconButton(
                                enabled = selectedIndex < chapters.lastIndex,
                                onClick = {
                                    saveNow(true)
                                    restorePositionForChapter = false
                                    selectedIndex++
                                    scope.launch { scrollState.scrollTo(0) }
                                }
                            ) { Icon(Icons.Default.KeyboardArrowRight, "Sonraki bölüm", tint = foreground) }
                        }
                    }
                }
            }
        }
    }

    if (showSettings) {
        ModalBottomSheet(onDismissRequest = { showSettings = false }) {
            Column(Modifier.fillMaxWidth().navigationBarsPadding().padding(20.dp)) {
                Text("Okuma ayarları", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(18.dp))
                Text("Tema", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ReaderTheme.values().forEach { theme ->
                        OutlinedButton(onClick = {
                            preferences = preferences.copy(theme = theme)
                            store.save(preferences)
                        }, Modifier.weight(1f)) {
                            Text(theme.name.lowercase().replaceFirstChar { it.uppercase() })
                        }
                    }
                }
                Spacer(Modifier.height(18.dp))
                Text("Yazı boyutu: ${preferences.fontSizeSp.roundToInt()}sp")
                Slider(preferences.fontSizeSp, { preferences = preferences.copy(fontSizeSp = it); store.save(preferences) }, valueRange = 14f..30f)
                Text("Satır aralığı: ${"%.2f".format(preferences.lineSpacing)}")
                Slider(preferences.lineSpacing, { preferences = preferences.copy(lineSpacing = it); store.save(preferences) }, valueRange = 1.15f..2.1f)
                Text("Metin genişliği: ${preferences.contentWidthDp}dp")
                Slider(preferences.contentWidthDp.toFloat(), { preferences = preferences.copy(contentWidthDp = it.roundToInt()); store.save(preferences) }, valueRange = 320f..900f)
                Row(Modifier.fillMaxWidth().padding(top = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("İlerleme çubuğu", Modifier.weight(1f))
                    Switch(preferences.showProgress, { preferences = preferences.copy(showProgress = it); store.save(preferences) })
                }
                Spacer(Modifier.height(24.dp))
            }
        }
    }

    if (showChapters) {
        ModalBottomSheet(onDismissRequest = { showChapters = false }) {
            Column(Modifier.fillMaxWidth().navigationBarsPadding().padding(18.dp)) {
                Text("Bölümler", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(12.dp))
                chapters.forEachIndexed { index, item ->
                    Row(
                        Modifier.fillMaxWidth().padding(vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("${item.chapterNumber}.", fontWeight = FontWeight.Bold)
                        Spacer(Modifier.width(10.dp))
                        Text(item.title.ifBlank { "İsimsiz Bölüm" }, Modifier.weight(1f))
                        if (index == selectedIndex) Text("Şu an", style = MaterialTheme.typography.labelSmall)
                        else TextButton(onClick = {
                            saveNow(true)
                            restorePositionForChapter = false
                            selectedIndex = index
                            showChapters = false
                            scope.launch { scrollState.scrollTo(0) }
                        }) { Text("Aç") }
                    }
                    HorizontalDivider()
                }
                Spacer(Modifier.height(12.dp))
            }
        }
    }
}
