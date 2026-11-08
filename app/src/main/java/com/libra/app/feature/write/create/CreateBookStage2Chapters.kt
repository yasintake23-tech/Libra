package com.libra.app.feature.write.create

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.libra.app.ui.theme.LibraLavender
import com.libra.app.ui.theme.LibraLavenderBorder
import com.libra.app.ui.theme.LibraLavenderSurface
import com.libra.app.ui.theme.LibraLavenderText

@Composable
fun CreateBookStage2Chapters(
    uiState: CreateBookUiState,
    onAddChapter: (String, String) -> Unit,
    onUpdateChapter: (String, String, String) -> Unit,
    onDeleteChapter: (String) -> Unit,
    onMoveUp: (String) -> Unit,
    onMoveDown: (String) -> Unit,
    onSetEditingChapterId: (String?) -> Unit,
    modifier: Modifier = Modifier
) {
    var chapterToDelete by remember { mutableStateOf<DraftChapter?>(null) }
    var isNewChapterDialogOpen by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Summary & Add Button Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
        ) {
            Column(Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            text = "Bölüm Yönetimi",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${uiState.chapters.size} Bölüm • Toplam ${uiState.totalWordCount} Kelime",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Button(
                        onClick = { isNewChapterDialogOpen = true },
                        colors = ButtonDefaults.buttonColors(containerColor = LibraLavender),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Add, null, Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Bölüm Ekle", style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
        }

        // Chapters List
        if (uiState.chapters.isEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.MenuBook,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(36.dp)
                    )
                    Spacer(Modifier.height(8.dp))
                    Text("Henüz bölüm eklenmedi", fontWeight = FontWeight.Bold)
                    Text(
                        "Yayınlamak için en az bir bölüm eklemelisin.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(12.dp))
                    OutlinedButton(onClick = { isNewChapterDialogOpen = true }) {
                        Text("İlk Bölümü Oluştur")
                    }
                }
            }
        } else {
            uiState.chapters.forEachIndexed { index, chapter ->
                val isEditing = uiState.editingChapterId == chapter.id
                ChapterCard(
                    chapter = chapter,
                    index = index,
                    totalCount = uiState.chapters.size,
                    isEditing = isEditing,
                    onEditClick = { onSetEditingChapterId(if (isEditing) null else chapter.id) },
                    onSaveEdit = { newTitle, newContent ->
                        onUpdateChapter(chapter.id, newTitle, newContent)
                        onSetEditingChapterId(null)
                    },
                    onDelete = { chapterToDelete = chapter },
                    onMoveUp = { onMoveUp(chapter.id) },
                    onMoveDown = { onMoveDown(chapter.id) }
                )
            }
        }
    }

    // New Chapter Creation Dialog
    if (isNewChapterDialogOpen) {
        var newTitle by remember { mutableStateOf("${uiState.chapters.size + 1}. Bölüm") }
        var newContent by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { isNewChapterDialogOpen = false },
            title = { Text("Yeni Bölüm Ekle") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = newTitle,
                        onValueChange = { newTitle = it },
                        label = { Text("Bölüm Başlığı") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = newContent,
                        onValueChange = { newContent = it },
                        label = { Text("Bölüm İçeriği (İsteğe bağlı, sonra da yazılabilir)") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 4,
                        maxLines = 8
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onAddChapter(newTitle.trim(), newContent.trim())
                        isNewChapterDialogOpen = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = LibraLavender)
                ) {
                    Text("Ekle")
                }
            },
            dismissButton = {
                TextButton(onClick = { isNewChapterDialogOpen = false }) {
                    Text("İptal")
                }
            }
        )
    }

    // Delete Confirmation Dialog
    chapterToDelete?.let { chapter ->
        AlertDialog(
            onDismissRequest = { chapterToDelete = null },
            icon = { Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error) },
            title = { Text("Bölümü Sil") },
            text = {
                Text("'${chapter.title}' başlıklı bölümü silmek istediğine emin misin? Bu işlem geri alınamaz.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteChapter(chapter.id)
                        chapterToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Sil")
                }
            },
            dismissButton = {
                TextButton(onClick = { chapterToDelete = null }) {
                    Text("Vazgeç")
                }
            }
        )
    }
}

@Composable
private fun ChapterCard(
    chapter: DraftChapter,
    index: Int,
    totalCount: Int,
    isEditing: Boolean,
    onEditClick: () -> Unit,
    onSaveEdit: (String, String) -> Unit,
    onDelete: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit
) {
    var editTitle by remember(chapter.title, isEditing) { mutableStateOf(chapter.title) }
    var editContent by remember(chapter.content, isEditing) { mutableStateOf(chapter.content) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isEditing) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.surface
        ),
        border = androidx.compose.foundation.BorderStroke(
            if (isEditing) 1.5.dp else 1.dp,
            if (isEditing) LibraLavender else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
        )
    ) {
        Column(Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Chapter Number Badge
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = LibraLavenderSurface
                ) {
                    Text(
                        text = "#${chapter.chapterNumber}",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = LibraLavenderText,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }

                Spacer(Modifier.width(10.dp))

                Column(Modifier.weight(1f)) {
                    Text(
                        text = chapter.title.ifBlank { "İsimsiz Bölüm" },
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${chapter.wordCount} kelime • ${if (chapter.content.isBlank()) "İçerik boş" else "Yazıldı"}",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (chapter.content.isBlank()) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Reorder buttons
                IconButton(
                    onClick = onMoveUp,
                    enabled = index > 0,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(Icons.Default.ArrowUpward, "Yukarı Taşı", Modifier.size(16.dp))
                }

                IconButton(
                    onClick = onMoveDown,
                    enabled = index < totalCount - 1,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(Icons.Default.ArrowDownward, "Aşağı Taşı", Modifier.size(16.dp))
                }

                IconButton(
                    onClick = onEditClick,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = if (isEditing) Icons.Default.Close else Icons.Default.Edit,
                        contentDescription = "Düzenle",
                        Modifier.size(16.dp),
                        tint = if (isEditing) LibraLavenderText else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Sil",
                        Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f)
                    )
                }
            }

            // Inline Editor Area
            AnimatedVisibility(visible = isEditing) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp)
                ) {
                    OutlinedTextField(
                        value = editTitle,
                        onValueChange = { editTitle = it },
                        label = { Text("Bölüm Başlığı") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    )

                    Spacer(Modifier.height(8.dp))

                    OutlinedTextField(
                        value = editContent,
                        onValueChange = { editContent = it },
                        label = { Text("Bölüm Metni") },
                        placeholder = { Text("Hikayeni buraya yazmaya başla...") },
                        minLines = 6,
                        maxLines = 14,
                        supportingText = {
                            val words = if (editContent.isBlank()) 0 else editContent.trim().split("\\s+".toRegex()).count { it.isNotBlank() }
                            Text("$words kelime • ~${(words / 180).coerceAtLeast(1)} dk okuma süresi")
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    )

                    Spacer(Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        Button(
                            onClick = { onSaveEdit(editTitle.trim(), editContent.trim()) },
                            colors = ButtonDefaults.buttonColors(containerColor = LibraLavender),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.Check, null, Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Bölümü Kaydet")
                        }
                    }
                }
            }
        }
    }
}
