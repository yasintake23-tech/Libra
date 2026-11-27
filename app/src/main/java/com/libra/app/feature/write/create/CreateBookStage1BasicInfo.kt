package com.libra.app.feature.write.create

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.libra.app.domain.model.BookCategory
import com.libra.app.ui.theme.LibraLavender
import com.libra.app.ui.theme.LibraLavenderBorder
import com.libra.app.ui.theme.LibraLavenderLight
import com.libra.app.ui.theme.LibraLavenderSurface
import com.libra.app.ui.theme.LibraLavenderText

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CreateBookStage1BasicInfo(
    uiState: CreateBookUiState,
    onTitleChange: (String) -> Unit,
    onDescriptionChange: (String) -> Unit,
    onDiscoverySummaryChange: (String) -> Unit,
    onCategoryChange: (BookCategory) -> Unit,
    onSetCover: (Uri?, ByteArray?, String, String) -> Unit,
    onRemoveCover: () -> Unit,
    onOpenAiDialog: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var filePickerError by remember { mutableStateOf<String?>(null) }

    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        runCatching {
            val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                ?: throw IllegalStateException("Kapak görseli okunamadı.")
            if (bytes.size > 8 * 1024 * 1024) {
                throw IllegalStateException("Kapak görseli 8 MB'dan küçük olmalı.")
            }
            val mime = context.contentResolver.getType(uri) ?: "image/jpeg"
            val extension = mime.substringAfter('/').ifBlank { "jpg" }.take(8)
            val fileName = "book-cover-" + System.currentTimeMillis() + "." + extension
            onSetCover(uri, bytes, fileName, mime)
            filePickerError = null
        }.onFailure { e ->
            filePickerError = e.localizedMessage ?: "Görsel yüklenemedi."
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // --- 1. COVER IMAGE SELECTION ---
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
                    Column {
                        Text(
                            text = "Kitap Kapağı",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Okuyucuların ilk göreceği görsel.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    if (uiState.coverUri != null) {
                        IconButton(onClick = onRemoveCover) {
                            Icon(Icons.Default.Close, "Kapağı Kaldır", tint = MaterialTheme.colorScheme.error)
                        }
                    }
                }

                Spacer(Modifier.height(14.dp))

                if (uiState.coverUri != null) {
                    // Preview with Cover
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .width(96.dp)
                                .aspectRatio(0.68f)
                                .clip(RoundedCornerShape(12.dp))
                                .border(1.dp, LibraLavenderBorder, RoundedCornerShape(12.dp))
                        ) {
                            AsyncImage(
                                model = uiState.coverUri,
                                contentDescription = "Kitap Kapağı",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        }

                        Column(Modifier.weight(1f)) {
                            Text(
                                text = "Kapak seçildi",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "Değiştirmek için dokun veya yeni görsel seç.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(Modifier.height(8.dp))
                            OutlinedButton(
                                onClick = { imagePicker.launch("image/*") },
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(Icons.Default.Edit, null, Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Değiştir", style = MaterialTheme.typography.labelMedium)
                            }
                        }
                    }
                } else {
                    // Empty state selector
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(LibraLavenderSurface.copy(alpha = 0.45f))
                            .border(1.dp, LibraLavenderBorder, RoundedCornerShape(16.dp))
                            .clickable { imagePicker.launch("image/*") }
                            .padding(20.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.AddPhotoAlternate,
                                contentDescription = null,
                                modifier = Modifier.size(36.dp),
                                tint = LibraLavenderText
                            )
                            Spacer(Modifier.height(8.dp))
                            Text(
                                text = "Cihazdan Kapak Fotoğrafı Seç",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = LibraLavenderText
                            )
                            Text(
                                text = "PNG, JPG veya WEBP (Maks. 8 MB)",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                if (filePickerError != null) {
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = filePickerError!!,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                Spacer(Modifier.height(12.dp))

                // AI Cover Generator Trigger Card (Future Extensibility Hook)
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .clickable { onOpenAiDialog(true) },
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, LibraLavenderBorder.copy(alpha = 0.5f))
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(LibraLavenderSurface),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = LibraLavenderText,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Spacer(Modifier.width(12.dp))

                        Column(Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "Yapay Zeka ile Kapak Tasarla",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(Modifier.width(6.dp))
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = LibraLavenderSurface
                                ) {
                                    Text(
                                        text = "Yakında",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = LibraLavenderText,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            Text(
                                text = "Konusunu yaz, atmosferine uygun kapak üret.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        // --- 2. BOOK TITLE ---
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
        ) {
            Column(Modifier.padding(16.dp)) {
                Text(
                    text = "Kitap Adı *",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Etkileyici ve akılda kalıcı bir başlık.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(Modifier.height(10.dp))

                OutlinedTextField(
                    value = uiState.title,
                    onValueChange = onTitleChange,
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    placeholder = { Text("Örn: Sisler Şehri, Kül ve Yıldızlar...") },
                    supportingText = {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(if (uiState.title.length < 2) "En az 2 karakter" else "Uygun")
                            Text("${uiState.title.length}/120")
                        }
                    },
                    shape = RoundedCornerShape(12.dp)
                )
            }
        }

        // --- 3. CATEGORY SELECTION ---
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
        ) {
            Column(Modifier.padding(16.dp)) {
                Text(
                    text = "Kategori *",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Kitabının ait olduğu ana edebiyat kategorisi.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(Modifier.height(12.dp))

                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    BookCategory.values().forEach { cat ->
                        val isSelected = uiState.category == cat
                        FilterChip(
                            selected = isSelected,
                            onClick = { onCategoryChange(cat) },
                            label = { Text(cat.displayName, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                            leadingIcon = if (isSelected) {
                                { Icon(Icons.Default.Check, null, Modifier.size(16.dp)) }
                            } else null,
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = LibraLavenderSurface,
                                selectedLabelColor = LibraLavenderText,
                                selectedLeadingIconColor = LibraLavenderText
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = isSelected,
                                borderColor = if (isSelected) LibraLavender else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                            ),
                            shape = RoundedCornerShape(10.dp)
                        )
                    }
                }
            }
        }

        // --- 4. BOOK DESCRIPTION & DISCOVERY SUMMARY ---
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
        ) {
            Column(Modifier.padding(16.dp)) {
                Text(
                    text = "Kitap Açıklaması (Sinopsis)",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Arka kapak yazısı. Okurlara hikayenin dünyasını ve çatışmasını anlat.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(Modifier.height(10.dp))

                OutlinedTextField(
                    value = uiState.description,
                    onValueChange = onDescriptionChange,
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 4,
                    maxLines = 8,
                    placeholder = { Text("Hikayeni tanıtan kapsamlı bir özet veya arka kapak yazısı...") },
                    supportingText = { Text("${uiState.description.length}/2000") },
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(Modifier.height(14.dp))

                Text(
                    text = "Keşfet Özeti (Vurucu Kanca)",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Keşfet vitrininde okurların dikkatini çekecek 1-2 cümlelik alıntı.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(Modifier.height(10.dp))

                OutlinedTextField(
                    value = uiState.discoverySummary,
                    onValueChange = onDiscoverySummaryChange,
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2,
                    maxLines = 4,
                    placeholder = { Text("Örn: 'Karanlık çöktüğünde sadece tek bir kural vardı: Asla aynaya bakma.'") },
                    supportingText = { Text("${uiState.discoverySummary.length}/500") },
                    shape = RoundedCornerShape(12.dp)
                )
            }
        }
    }

    // AI Cover Generator Dialog Mockup / Preview
    if (uiState.showAiCoverPromptDialog) {
        var promptText by remember { mutableStateOf(uiState.title + " konulu gizemli bir kitap kapağı") }
        AlertDialog(
            onDismissRequest = { onOpenAiDialog(false) },
            icon = { Icon(Icons.Default.AutoAwesome, null, tint = LibraLavender) },
            title = { Text("Yapay Zeka ile Kapak Tasarımı") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        "Libra AI kapak üretim altyapısı hazırlanıyor. Kitabının atmosferine göre yapay zekaya ipuçları vererek benzersiz bir kapak oluşturabileceksin.",
                        style = MaterialTheme.typography.bodySmall
                    )
                    OutlinedTextField(
                        value = promptText,
                        onValueChange = { promptText = it },
                        label = { Text("Kapak Açıklaması / Tema") },
                        placeholder = { Text("Sisli bir orman, mor yıldızlı gökyüzü...") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 2
                    )
                    Surface(
                        color = LibraLavenderSurface.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            "Bu özellik çok yakında güncellemeyle aktif hale gelecektir. Şimdilik cihazından fotoğraf seçebilirsin.",
                            style = MaterialTheme.typography.labelSmall,
                            color = LibraLavenderText,
                            modifier = Modifier.padding(8.dp)
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { onOpenAiDialog(false) },
                    colors = ButtonDefaults.buttonColors(containerColor = LibraLavender)
                ) {
                    Text("Anladım")
                }
            }
        )
    }
}
