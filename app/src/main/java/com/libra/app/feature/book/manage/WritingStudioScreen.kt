package com.libra.app.feature.book.manage

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.FormatBold
import androidx.compose.material.icons.filled.FormatItalic
import androidx.compose.material.icons.filled.FormatQuote
import androidx.compose.material.icons.filled.HorizontalRule
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.libra.app.domain.model.Chapter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WritingStudioScreen(
    viewModel: BookManagementViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.state.collectAsState()
    val activeChapter = state.activeStudioChapter
    val chapters = state.chapters
    val snackbarHostState = remember { SnackbarHostState() }

    var chapterTitle by remember(activeChapter?.id) { mutableStateOf(activeChapter?.title.orEmpty()) }
    var chapterContent by remember(activeChapter?.id) { mutableStateOf(activeChapter?.content.orEmpty()) }
    var chapterMenuExpanded by remember { mutableStateOf(false) }

    // Undo / Redo history
    val undoStack = remember(activeChapter?.id) { mutableStateListOf<String>() }
    val redoStack = remember(activeChapter?.id) { mutableStateListOf<String>() }

    val context = LocalContext.current
    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) {
            runCatching {
                val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                if (bytes != null && bytes.isNotEmpty()) {
                    viewModel.uploadStudioChapterImage(
                        bytes = bytes,
                        fileName = "chapter-img-${System.currentTimeMillis()}.jpg",
                        contentType = "image/jpeg"
                    ) { tag ->
                        chapterContent = chapterContent + tag
                        viewModel.updateStudioChapterContent(chapterTitle, chapterContent)
                    }
                }
            }
        }
    }

    LaunchedEffect(chapterTitle, chapterContent) {
        if (activeChapter != null && (chapterTitle != activeChapter.title || chapterContent != activeChapter.content)) {
            viewModel.updateStudioChapterContent(chapterTitle, chapterContent)
        }
    }

    DisposableEffect(activeChapter?.id) {
        onDispose {
            if (activeChapter != null && (chapterTitle != activeChapter.title || chapterContent != activeChapter.content)) {
                viewModel.saveChapterNow(activeChapter.copy(title = chapterTitle, content = chapterContent))
            }
        }
    }

    LaunchedEffect(state.errorMessage, state.successMessage) {
        state.errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessages()
        }
        state.successMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessages()
        }
    }

    BackHandler {
        if (activeChapter != null && (chapterTitle != activeChapter.title || chapterContent != activeChapter.content)) {
            viewModel.saveChapterNow(activeChapter.copy(title = chapterTitle, content = chapterContent))
        }
        onBack()
    }

    val wordCount = remember(chapterContent) {
        chapterContent.trim().split(Regex("\\s+")).count { it.isNotBlank() }
    }
    val charCount = remember(chapterContent) { chapterContent.length }
    val readingMinutes = (wordCount / 200).coerceAtLeast(1)

    val timeFormat = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }
    val lastSavedText = remember(state.lastSavedTimestamp) {
        if (state.lastSavedTimestamp > 0) timeFormat.format(Date(state.lastSavedTimestamp)) else ""
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Box {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { chapterMenuExpanded = true }
                                .padding(horizontal = 6.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "Bölüm ${activeChapter?.chapterNumber ?: 1}",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                            Icon(Icons.Default.ArrowDropDown, contentDescription = "Bölüm Değiştir")
                        }

                        DropdownMenu(
                            expanded = chapterMenuExpanded,
                            onDismissRequest = { chapterMenuExpanded = false }
                        ) {
                            chapters.forEach { chapter ->
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            text = "${chapter.chapterNumber}. ${chapter.title.ifBlank { "İsimsiz Bölüm" }}",
                                            fontWeight = if (chapter.id == activeChapter?.id) FontWeight.Bold else FontWeight.Normal
                                        )
                                    },
                                    onClick = {
                                        chapterMenuExpanded = false
                                        viewModel.selectChapterForStudio(chapter)
                                    }
                                )
                            }
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = {
                        if (activeChapter != null && (chapterTitle != activeChapter.title || chapterContent != activeChapter.content)) {
                            viewModel.saveChapterNow(activeChapter.copy(title = chapterTitle, content = chapterContent))
                        }
                        onBack()
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Geri")
                    }
                },
                actions = {
                    // Auto-saving status indicator
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        if (state.isAutoSaving || state.isSaving) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = "Kaydediliyor...",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        } else if (state.hasUnsavedChanges) {
                            Surface(
                                shape = CircleShape,
                                color = Color(0xFFF57C00),
                                modifier = Modifier.size(8.dp)
                            ) {}
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = "Düzenlendi",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFFF57C00)
                            )
                        } else {
                            Icon(
                                Icons.Default.Check,
                                contentDescription = null,
                                tint = Color(0xFF2E7D32),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                text = if (lastSavedText.isNotBlank()) "Kaydedildi $lastSavedText" else "Kaydedildi",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFF2E7D32)
                            )
                        }
                    }

                    // Manual Save button
                    IconButton(
                        onClick = {
                            activeChapter?.let {
                                viewModel.saveChapterNow(it.copy(title = chapterTitle, content = chapterContent))
                            }
                        },
                        enabled = !state.isSaving && !state.isAutoSaving
                    ) {
                        Icon(Icons.Default.Save, contentDescription = "Kaydet")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .imePadding()
        ) {
            // Metrics bar (Word count, character count, reading time)
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "$wordCount kelime • $charCount karakter",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "~$readingMinutes dk tahmini okuma",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            // Scrollable Writing Canvas
            val canvasScrollState = rememberScrollState()
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(canvasScrollState)
                    .padding(horizontal = 20.dp, vertical = 16.dp)
            ) {
                // Chapter Title Field
                OutlinedTextField(
                    value = chapterTitle,
                    onValueChange = {
                        chapterTitle = it
                    },
                    placeholder = {
                        Text(
                            text = "Bölüm Başlığı",
                            style = MaterialTheme.typography.headlineMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                            )
                        )
                    },
                    textStyle = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    ),
                    singleLine = true,
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(Modifier.height(12.dp))

                // Writing body area
                BasicTextField(
                    value = chapterContent,
                    onValueChange = { newText ->
                        if (newText != chapterContent) {
                            undoStack.add(chapterContent)
                            redoStack.clear()
                            chapterContent = newText
                        }
                    },
                    textStyle = TextStyle(
                        fontSize = 17.sp,
                        lineHeight = 28.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    ),
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                    decorationBox = { innerTextField ->
                        if (chapterContent.isEmpty()) {
                            Text(
                                text = "Buraya yazmaya başla... Wattpad tarzı akıcı ve sürükleyici bir bölüm kurgula.",
                                style = TextStyle(
                                    fontSize = 17.sp,
                                    lineHeight = 28.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                )
                            )
                        }
                        innerTextField()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(600.dp)
                )

                Spacer(Modifier.height(32.dp))
            }

            // Sticky Formatting Toolbar
            Surface(
                modifier = Modifier.fillMaxWidth(),
                tonalElevation = 4.dp,
                color = MaterialTheme.colorScheme.surface
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // Undo
                    IconButton(
                        onClick = {
                            if (undoStack.isNotEmpty()) {
                                val last = undoStack.removeAt(undoStack.lastIndex)
                                redoStack.add(chapterContent)
                                chapterContent = last
                            }
                        },
                        enabled = undoStack.isNotEmpty()
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.Undo,
                            contentDescription = "Geri Al",
                            tint = if (undoStack.isNotEmpty()) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                        )
                    }

                    // Redo
                    IconButton(
                        onClick = {
                            if (redoStack.isNotEmpty()) {
                                val next = redoStack.removeAt(redoStack.lastIndex)
                                undoStack.add(chapterContent)
                                chapterContent = next
                            }
                        },
                        enabled = redoStack.isNotEmpty()
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.Redo,
                            contentDescription = "Yinele",
                            tint = if (redoStack.isNotEmpty()) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                        )
                    }

                    Surface(
                        modifier = Modifier
                            .height(24.dp)
                            .width(1.dp),
                        color = MaterialTheme.colorScheme.outlineVariant
                    ) {}

                    // Bold
                    IconButton(onClick = {
                        undoStack.add(chapterContent)
                        chapterContent = chapterContent + " **kalın metin** "
                    }) {
                        Icon(Icons.Default.FormatBold, contentDescription = "Kalın")
                    }

                    // Italic
                    IconButton(onClick = {
                        undoStack.add(chapterContent)
                        chapterContent = chapterContent + " *italik metin* "
                    }) {
                        Icon(Icons.Default.FormatItalic, contentDescription = "İtalik")
                    }

                    // Quote
                    IconButton(onClick = {
                        undoStack.add(chapterContent)
                        chapterContent = chapterContent + "\n> Alıntı metni\n"
                    }) {
                        Icon(Icons.Default.FormatQuote, contentDescription = "Alıntı")
                    }

                    // Divider
                    IconButton(onClick = {
                        undoStack.add(chapterContent)
                        chapterContent = chapterContent + "\n\n---\n\n"
                    }) {
                        Icon(Icons.Default.HorizontalRule, contentDescription = "Ayraç")
                    }

                    // Insert Image via R2 Direct S3 upload
                    IconButton(onClick = {
                        imagePicker.launch("image/*")
                    }) {
                        Icon(Icons.Default.Image, contentDescription = "Görsel Ekle", tint = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }
    }
}
