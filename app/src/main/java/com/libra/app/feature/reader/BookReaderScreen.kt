package com.libra.app.feature.reader

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.FormatSize
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.libra.app.domain.model.Book
import com.libra.app.domain.model.Chapter
import com.libra.app.domain.model.ReadingProgress
import com.libra.app.ui.animation.appleBouncy
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
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
    val chapters = state.chapters
    val progress = state.progress
    val context = LocalContext.current
    val store = remember { ReaderPreferencesStore(context) }
    var preferences by remember { mutableStateOf(store.read()) }
    var selectedIndex by remember(book.id, initialChapterId) { mutableStateOf(0) }
    var showSettings by remember { mutableStateOf(false) }
    var showChapters by remember { mutableStateOf(false) }
    var restored by remember(book.id, initialChapterId) { mutableStateOf(false) }
    var lastLocalKey by remember { mutableStateOf("") }
    var saveJob by remember { mutableStateOf<Job?>(null) }
    var controlsVisible by remember { mutableStateOf(true) }

    val scrollState = rememberScrollState()
    val scope = rememberCoroutineScope()

    fun currentProgress(force: Boolean = false): ReadingProgress? {
        val current = chapters.getOrNull(selectedIndex) ?: return null
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

    BackHandler {
        when {
            showSettings -> showSettings = false
            showChapters -> showChapters = false
            else -> {
                saveNow(force = true)
                onBack()
            }
        }
    }

    LaunchedEffect(book.id, userId) {
        vm.load(book, userId, store)
    }

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
    var isRestoringPosition by remember(chapter?.id) { mutableStateOf(true) }

    LaunchedEffect(chapter?.id, restored) {
        val current = chapter ?: return@LaunchedEffect
        isRestoringPosition = true
        val shouldRestore = progress?.chapterId == current.id && progress.position > 0
        if (shouldRestore) {
            withTimeoutOrNull(1500L) {
                snapshotFlow { scrollState.maxValue }
                    .filter { max -> max > 0 }
                    .first()
            }
            val target = progress.position.coerceIn(0, scrollState.maxValue)
            scrollState.scrollTo(target)
        } else {
            scrollState.scrollTo(0)
        }
        isRestoringPosition = false
    }

    LaunchedEffect(chapter?.id) {
        snapshotFlow { scrollState.value }
            .collect { position ->
                if (isRestoringPosition || scrollState.maxValue <= 0 || chapter == null) return@collect
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

    // Themes
    val background = when (preferences.theme) {
        ReaderTheme.LIGHT -> Color(0xFFFAFAFA)
        ReaderTheme.DARK -> Color(0xFF121212)
        ReaderTheme.SEPIA -> Color(0xFFFBF0D9)
    }
    val foreground = when (preferences.theme) {
        ReaderTheme.LIGHT -> Color(0xFF212121)
        ReaderTheme.DARK -> Color(0xFFE0E0E0)
        ReaderTheme.SEPIA -> Color(0xFF4A3828)
    }
    val surface = when (preferences.theme) {
        ReaderTheme.LIGHT -> Color(0xFFFFFFFF)
        ReaderTheme.DARK -> Color(0xFF1E1E1E)
        ReaderTheme.SEPIA -> Color(0xFFF3E5C8)
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(background)
    ) {
        // Main reading content area
        Column(modifier = Modifier.fillMaxSize()) {
            when {
                state.loading && chapters.isEmpty() -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    }
                }
                state.error != null && chapters.isEmpty() -> {
                    Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                        Text(state.error.orEmpty(), color = MaterialTheme.colorScheme.error)
                    }
                }
                chapter == null -> {
                    Column(
                        Modifier.fillMaxSize().padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text("Bu kitapta henüz yayınlanmış bölüm yok.", color = foreground, style = MaterialTheme.typography.bodyLarge)
                        Spacer(Modifier.height(16.dp))
                        OutlinedButton(onClick = onBack) {
                            Text("Geri Dön")
                        }
                    }
                }
                else -> {
                    val remainingWords = remember(chapter.wordCount, percent) {
                        ((1f - (percent / 100f)) * chapter.wordCount).roundToInt().coerceAtLeast(0)
                    }
                    val remainingMinutes = (remainingWords / 200).coerceAtLeast(1)

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) {
                                controlsVisible = !controlsVisible
                            }
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .verticalScroll(scrollState),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .widthIn(max = preferences.contentWidthDp.dp)
                                    .padding(
                                        start = 22.dp,
                                        end = 22.dp,
                                        top = if (controlsVisible) 76.dp else 40.dp,
                                        bottom = if (controlsVisible) 96.dp else 40.dp
                                    )
                            ) {
                                // Chapter Header
                                Text(
                                    text = "Bölüm ${chapter.chapterNumber}",
                                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(Modifier.height(6.dp))
                                Text(
                                    text = chapter.title.ifBlank { "${chapter.chapterNumber}. Bölüm" },
                                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                                    color = foreground
                                )
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    text = "${chapter.wordCount} kelime • ~$remainingMinutes dk kaldı",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = foreground.copy(alpha = 0.6f)
                                )

                                Spacer(Modifier.height(28.dp))

                                // Selectable Chapter Text with images support
                                SelectionContainer {
                                    RenderChapterContent(
                                        content = chapter.content,
                                        foreground = foreground,
                                        preferences = preferences
                                    )
                                }

                                Spacer(Modifier.height(48.dp))

                                HorizontalDivider(color = foreground.copy(alpha = 0.15f))

                                Spacer(Modifier.height(24.dp))

                                // Wattpad-style chapter end navigation
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    if (selectedIndex > 0) {
                                        OutlinedButton(
                                            onClick = {
                                                saveNow(true)
                                                selectedIndex--
                                                scope.launch { scrollState.scrollTo(0) }
                                            },
                                            modifier = Modifier
                                                .weight(1f)
                                                .appleBouncy(pressedScale = 0.93f, onClick = {
                                                    saveNow(true)
                                                    selectedIndex--
                                                    scope.launch { scrollState.scrollTo(0) }
                                                }),
                                            shape = RoundedCornerShape(16.dp)
                                        ) {
                                            Icon(Icons.Default.KeyboardArrowLeft, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(Modifier.width(4.dp))
                                            Text("Önceki Bölüm")
                                        }
                                    }

                                    if (selectedIndex < chapters.lastIndex) {
                                        Button(
                                            onClick = {
                                                saveNow(true)
                                                selectedIndex++
                                                scope.launch { scrollState.scrollTo(0) }
                                            },
                                            modifier = Modifier
                                                .weight(1f)
                                                .appleBouncy(pressedScale = 0.93f, onClick = {
                                                    saveNow(true)
                                                    selectedIndex++
                                                    scope.launch { scrollState.scrollTo(0) }
                                                }),
                                            shape = RoundedCornerShape(16.dp),
                                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                                        ) {
                                            Text("Sonraki Bölüm")
                                            Spacer(Modifier.width(4.dp))
                                            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(16.dp))
                                        }
                                    } else {
                                        Card(
                                            modifier = Modifier.fillMaxWidth(),
                                            shape = RoundedCornerShape(16.dp),
                                            colors = CardDefaults.cardColors(containerColor = surface),
                                            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                                        ) {
                                            Column(
                                                modifier = Modifier.padding(20.dp),
                                                horizontalAlignment = Alignment.CenterHorizontally
                                            ) {
                                                Icon(
                                                    Icons.Default.CheckCircle,
                                                    contentDescription = null,
                                                    tint = Color(0xFF2E7D32),
                                                    modifier = Modifier.size(36.dp)
                                                )
                                                Spacer(Modifier.height(8.dp))
                                                Text(
                                                    text = "Tebrikler!",
                                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                                    color = foreground
                                                )
                                                Spacer(Modifier.height(4.dp))
                                                Text(
                                                    text = "Bu kitabın şu ana kadar yayınlanmış tüm bölümlerini bitirdin.",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = foreground.copy(alpha = 0.7f),
                                                    textAlign = TextAlign.Center
                                                )
                                            }
                                        }
                                    }
                                }

                                Spacer(Modifier.height(80.dp))
                            }
                        }
                    }
                }
            }
        }

        // Top App Bar (Toggled with animation)
        AnimatedVisibility(
            visible = controlsVisible,
            enter = fadeIn() + slideInVertically(initialOffsetY = { -it }),
            exit = fadeOut() + slideOutVertically(targetOffsetY = { -it }),
            modifier = Modifier.align(Alignment.TopCenter)
        ) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = surface.copy(alpha = 0.95f),
                tonalElevation = 4.dp
            ) {
                Column {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .statusBarsPadding()
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = {
                                saveNow(true)
                                onBack()
                            },
                            modifier = Modifier.appleBouncy(pressedScale = 0.86f, onClick = {
                                saveNow(true)
                                onBack()
                            })
                        ) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Geri", tint = foreground)
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = book.title,
                                maxLines = 1,
                                color = foreground,
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = chapter?.let { "Bölüm ${it.chapterNumber} • ${it.title}" } ?: "Okuyucu",
                                maxLines = 1,
                                color = foreground.copy(alpha = 0.7f),
                                style = MaterialTheme.typography.labelSmall,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        IconButton(
                            onClick = { showChapters = true },
                            modifier = Modifier.appleBouncy(pressedScale = 0.86f, onClick = { showChapters = true })
                        ) {
                            Icon(Icons.Default.List, contentDescription = "Bölümler", tint = foreground)
                        }

                        IconButton(
                            onClick = { showSettings = true },
                            modifier = Modifier.appleBouncy(pressedScale = 0.86f, onClick = { showSettings = true })
                        ) {
                            Icon(Icons.Default.FormatSize, contentDescription = "Okuma Ayarları", tint = foreground)
                        }
                    }

                    if (preferences.showProgress) {
                        LinearProgressIndicator(
                            progress = { percent / 100f },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(3.dp),
                            color = MaterialTheme.colorScheme.primary,
                            trackColor = foreground.copy(alpha = 0.1f)
                        )
                    }
                }
            }
        }

        // Bottom Bar (Toggled with animation)
        AnimatedVisibility(
            visible = controlsVisible && chapters.isNotEmpty(),
            enter = fadeIn() + slideInVertically(initialOffsetY = { it }),
            exit = fadeOut() + slideOutVertically(targetOffsetY = { it }),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding(),
                color = surface.copy(alpha = 0.95f),
                tonalElevation = 6.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        enabled = selectedIndex > 0,
                        onClick = {
                            saveNow(true)
                            selectedIndex--
                            scope.launch { scrollState.scrollTo(0) }
                        },
                        modifier = Modifier.appleBouncy(
                            enabled = selectedIndex > 0,
                            pressedScale = 0.86f,
                            onClick = {
                                saveNow(true)
                                selectedIndex--
                                scope.launch { scrollState.scrollTo(0) }
                            }
                        )
                    ) {
                        Icon(Icons.Default.KeyboardArrowLeft, contentDescription = "Önceki Bölüm", tint = foreground)
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = if (percent >= 100 && selectedIndex == chapters.lastIndex) "Bölüm Tamamlandı" else "%$percent",
                            color = foreground,
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = "Bölüm ${selectedIndex + 1} / ${chapters.size}",
                            color = foreground.copy(alpha = 0.7f),
                            style = MaterialTheme.typography.labelSmall
                        )
                    }

                    IconButton(
                        enabled = selectedIndex < chapters.lastIndex,
                        onClick = {
                            saveNow(true)
                            selectedIndex++
                            scope.launch { scrollState.scrollTo(0) }
                        },
                        modifier = Modifier.appleBouncy(
                            enabled = selectedIndex < chapters.lastIndex,
                            pressedScale = 0.86f,
                            onClick = {
                                saveNow(true)
                                selectedIndex++
                                scope.launch { scrollState.scrollTo(0) }
                            }
                        )
                    ) {
                        Icon(Icons.Default.KeyboardArrowRight, contentDescription = "Sonraki Bölüm", tint = foreground)
                    }
                }
            }
        }
    }

    // Reading Settings Bottom Sheet
    if (showSettings) {
        ModalBottomSheet(
            onDismissRequest = { showSettings = false }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(24.dp)
            ) {
                Text(
                    text = "Okuma Deneyimi",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                )

                Spacer(Modifier.height(18.dp))

                // Themes
                Text("Tema", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold))
                Spacer(Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    ReaderThemeButton(
                        name = "Açık",
                        bgColor = Color(0xFFFAFAFA),
                        textColor = Color(0xFF212121),
                        isSelected = preferences.theme == ReaderTheme.LIGHT,
                        onClick = {
                            preferences = preferences.copy(theme = ReaderTheme.LIGHT)
                            store.save(preferences)
                        },
                        modifier = Modifier.weight(1f)
                    )
                    ReaderThemeButton(
                        name = "Sepya",
                        bgColor = Color(0xFFFBF0D9),
                        textColor = Color(0xFF4A3828),
                        isSelected = preferences.theme == ReaderTheme.SEPIA,
                        onClick = {
                            preferences = preferences.copy(theme = ReaderTheme.SEPIA)
                            store.save(preferences)
                        },
                        modifier = Modifier.weight(1f)
                    )
                    ReaderThemeButton(
                        name = "Koyu",
                        bgColor = Color(0xFF1E1E1E),
                        textColor = Color(0xFFE0E0E0),
                        isSelected = preferences.theme == ReaderTheme.DARK,
                        onClick = {
                            preferences = preferences.copy(theme = ReaderTheme.DARK)
                            store.save(preferences)
                        },
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(Modifier.height(20.dp))

                // Font Size Slider
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Yazı Boyutu", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold))
                    Text("${preferences.fontSizeSp.roundToInt()} sp", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
                }
                Slider(
                    value = preferences.fontSizeSp,
                    onValueChange = {
                        preferences = preferences.copy(fontSizeSp = it)
                        store.save(preferences)
                    },
                    valueRange = 14f..30f
                )

                Spacer(Modifier.height(12.dp))

                // Line Spacing Slider
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Satır Aralığı", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold))
                    Text(String.format("%.1fx", preferences.lineSpacing), style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
                }
                Slider(
                    value = preferences.lineSpacing,
                    onValueChange = {
                        preferences = preferences.copy(lineSpacing = it)
                        store.save(preferences)
                    },
                    valueRange = 1.2f..2.2f
                )

                Spacer(Modifier.height(12.dp))

                // Progress Bar Switch
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Okuma İlerleme Çubuğu", style = MaterialTheme.typography.bodyMedium)
                    Switch(
                        checked = preferences.showProgress,
                        onCheckedChange = {
                            preferences = preferences.copy(showProgress = it)
                            store.save(preferences)
                        }
                    )
                }

                Spacer(Modifier.height(24.dp))
            }
        }
    }

    // Chapters Directory Bottom Sheet
    if (showChapters) {
        ModalBottomSheet(onDismissRequest = { showChapters = false }) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(20.dp)
            ) {
                Text(
                    text = "Bölümler (${chapters.size})",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                )

                Spacer(Modifier.height(16.dp))

                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    itemsIndexed(chapters, key = { _, it -> it.id }) { index, item ->
                        val isSelected = index == selectedIndex
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    saveNow(true)
                                    selectedIndex = index
                                    showChapters = false
                                    scope.launch { scrollState.scrollTo(0) }
                                },
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f) else Color.Transparent
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(
                                            text = "${item.chapterNumber}",
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                            color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                Spacer(Modifier.width(12.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = item.title.ifBlank { "${item.chapterNumber}. Bölüm" },
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                        )
                                    )
                                    Text(
                                        text = "${item.wordCount} kelime",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                if (isSelected) {
                                    Text(
                                        text = "Okunuyor",
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun RenderChapterContent(
    content: String,
    foreground: Color,
    preferences: ReaderPreferences
) {
    val textStyle = TextStyle(
        fontSize = preferences.fontSizeSp.sp,
        lineHeight = (preferences.fontSizeSp * preferences.lineSpacing).sp,
        color = foreground,
        fontFamily = FontFamily.Serif
    )

    // Parse image tags [[IMAGE:url|caption]] or render standard paragraph text
    val parts = remember(content) {
        val regex = Regex("\\[\\[IMAGE:(.*?)\\|(.*?)\\]\\]")
        val result = mutableListOf<ContentSegment>()
        var lastIdx = 0
        regex.findAll(content).forEach { match ->
            if (match.range.first > lastIdx) {
                result.add(ContentSegment.TextSegment(content.substring(lastIdx, match.range.first)))
            }
            val url = match.groupValues.getOrNull(1).orEmpty()
            val caption = match.groupValues.getOrNull(2).orEmpty()
            result.add(ContentSegment.ImageSegment(url, caption))
            lastIdx = match.range.last + 1
        }
        if (lastIdx < content.length) {
            result.add(ContentSegment.TextSegment(content.substring(lastIdx)))
        }
        result
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        parts.forEach { segment ->
            when (segment) {
                is ContentSegment.TextSegment -> {
                    Text(
                        text = segment.text,
                        style = textStyle,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                is ContentSegment.ImageSegment -> {
                    Spacer(Modifier.height(16.dp))
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        AsyncImage(
                            model = segment.url,
                            contentDescription = segment.caption,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(220.dp),
                            contentScale = ContentScale.Crop
                        )
                    }
                    if (segment.caption.isNotBlank() && segment.caption != "Görsel") {
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = segment.caption,
                            style = MaterialTheme.typography.labelSmall,
                            color = foreground.copy(alpha = 0.6f),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    Spacer(Modifier.height(16.dp))
                }
            }
        }
    }
}

private sealed interface ContentSegment {
    data class TextSegment(val text: String) : ContentSegment
    data class ImageSegment(val url: String, val caption: String) : ContentSegment
}

@Composable
private fun ReaderThemeButton(
    name: String,
    bgColor: Color,
    textColor: Color,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .height(44.dp)
            .appleBouncy(pressedScale = 0.90f, onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        color = bgColor,
        border = if (isSelected) androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else androidx.compose.foundation.BorderStroke(1.dp, Color.Gray.copy(alpha = 0.3f))
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = name,
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                color = textColor
            )
        }
    }
}
