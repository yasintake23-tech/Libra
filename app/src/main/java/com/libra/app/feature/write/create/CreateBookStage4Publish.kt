package com.libra.app.feature.write.create

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.libra.app.ui.theme.LibraLavender
import com.libra.app.ui.theme.LibraLavenderBorder
import com.libra.app.ui.theme.LibraLavenderLight
import com.libra.app.ui.theme.LibraLavenderSurface
import com.libra.app.ui.theme.LibraLavenderText
import com.libra.app.ui.animation.appleBouncy

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CreateBookStage4Publish(
    uiState: CreateBookUiState,
    authorName: String,
    onVisibilityChange: (String) -> Unit,
    onSaveDraft: () -> Unit,
    onPublish: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showPublishConfirmDialog by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // --- 1. LIVE BOOK PREVIEW CARD ---
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
                    Text(
                        text = "Kitap Önizlemesi",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = LibraLavenderSurface
                    ) {
                        Text(
                            text = "Canlı Önizleme",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = LibraLavenderText,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }

                Spacer(Modifier.height(14.dp))

                // Realistic Book Showcase Card
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, LibraLavenderBorder.copy(alpha = 0.5f))
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.Top,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        // Cover Mockup
                        Box(
                            modifier = Modifier
                                .width(88.dp)
                                .aspectRatio(0.68f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(LibraLavenderSurface),
                            contentAlignment = Alignment.Center
                        ) {
                            if (uiState.coverUri != null) {
                                AsyncImage(
                                    model = uiState.coverUri,
                                    contentDescription = null,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            } else {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier.padding(6.dp)
                                ) {
                                    Icon(
                                        Icons.Default.AutoStories,
                                        contentDescription = null,
                                        tint = LibraLavenderText,
                                        modifier = Modifier.size(28.dp)
                                    )
                                    Spacer(Modifier.height(4.dp))
                                    Text(
                                        text = uiState.title.ifBlank { "Libra" },
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }

                        // Info Column
                        Column(Modifier.weight(1f)) {
                            Text(
                                text = uiState.title.ifBlank { "Adsız Eser" },
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )

                            Text(
                                text = "Yazar: ${authorName.ifBlank { "Sen" }}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Spacer(Modifier.height(6.dp))

                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = MaterialTheme.colorScheme.surface
                                ) {
                                    Text(
                                        text = uiState.category.displayName,
                                        style = MaterialTheme.typography.labelSmall,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }

                                if (uiState.genre.isNotBlank()) {
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = MaterialTheme.colorScheme.surface
                                    ) {
                                        Text(
                                            text = uiState.genre,
                                            style = MaterialTheme.typography.labelSmall,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }

                                if (uiState.isSeries && uiState.seriesName.isNotBlank()) {
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = LibraLavenderSurface
                                    ) {
                                        Text(
                                            text = "${uiState.seriesName} #${uiState.seriesOrder}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = LibraLavenderText,
                                            fontWeight = FontWeight.SemiBold,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }

                            Spacer(Modifier.height(8.dp))

                            Text(
                                text = "${uiState.chapters.size} Bölüm • ~${uiState.totalWordCount} Kelime",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            if (uiState.discoverySummary.isNotBlank()) {
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    text = "\"${uiState.discoverySummary}\"",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }

        // --- 2. VISIBILITY SETTINGS ---
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
        ) {
            Column(Modifier.padding(16.dp)) {
                Text(
                    text = "Görünürlük Ayarları",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Kitabını kimlerin görebileceğini belirle.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(Modifier.height(12.dp))

                VisibilityOption(
                    title = "Herkese Açık (Önerilen)",
                    description = "Kitap Keşfet sayfasında, aramalarda ve profilinde listelenir. Herkes okuyabilir.",
                    icon = Icons.Default.Public,
                    selected = uiState.visibility == "PUBLIC",
                    onSelect = { onVisibilityChange("PUBLIC") }
                )

                Spacer(Modifier.height(8.dp))

                VisibilityOption(
                    title = "Yalnızca Bağlantıyla",
                    description = "Aramalarda listelenmez. Sadece kitabın doğrudan bağlantısını paylaştığın kişiler okuyabilir.",
                    icon = Icons.Default.Link,
                    selected = uiState.visibility == "UNLISTED",
                    onSelect = { onVisibilityChange("UNLISTED") }
                )

                Spacer(Modifier.height(8.dp))

                VisibilityOption(
                    title = "Gizli / Özel",
                    description = "Yalnızca sen görebilirsin. İstediğin zaman sonradan herkese açık hale getirebilirsin.",
                    icon = Icons.Default.Lock,
                    selected = uiState.visibility == "PRIVATE",
                    onSelect = { onVisibilityChange("PRIVATE") }
                )
            }
        }

        // --- 3. PRE-PUBLISH VALIDATION CHECKLIST ---
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
        ) {
            Column(Modifier.padding(16.dp)) {
                Text(
                    text = "Yayın Öncesi Kontrol Listesi",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Yayınlamadan önce gereksinimlerin durumu.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(Modifier.height(12.dp))

                ValidationRow(
                    label = "Kitap Adı",
                    detail = if (uiState.isTitleValid) "'${uiState.title}' hazır" else "En az 2 karakter olmalıdır",
                    isValid = uiState.isTitleValid,
                    isRequired = true
                )

                ValidationRow(
                    label = "Kategori",
                    detail = uiState.category.displayName,
                    isValid = true,
                    isRequired = true
                )

                ValidationRow(
                    label = "Kitap Kapağı",
                    detail = if (uiState.hasCover) "Özel kapak seçildi" else "Varsayılan kapak kullanılacak",
                    isValid = uiState.hasCover,
                    isRequired = false
                )

                ValidationRow(
                    label = "Bölüm Listesi",
                    detail = "${uiState.chapters.size} bölüm hazır",
                    isValid = uiState.hasChapters,
                    isRequired = true
                )

                ValidationRow(
                    label = "İlk Bölüm İçeriği",
                    detail = if (uiState.isFirstChapterFilled) "Yazıldı (${uiState.chapters.firstOrNull()?.wordCount} kelime)" else "Yayınlamak için 1. bölümün metni gereklidir",
                    isValid = uiState.isFirstChapterFilled,
                    isRequired = true
                )
            }
        }

        // --- 4. ACTION BUTTONS ---
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Publish Button (Primary)
            Button(
                onClick = { showPublishConfirmDialog = true },
                enabled = uiState.canPublish && !uiState.isPublishing,
                colors = ButtonDefaults.buttonColors(containerColor = LibraLavender),
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .appleBouncy(
                        enabled = uiState.canPublish && !uiState.isPublishing,
                        pressedScale = 0.93f,
                        onClick = { showPublishConfirmDialog = true }
                    )
            ) {
                if (uiState.isPublishing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = Color.White
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("Kitap Yayınlanıyor...")
                } else {
                    Icon(Icons.Default.Public, null, Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "Kitabı Yayınla",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Save as Draft Button (Secondary)
            OutlinedButton(
                onClick = onSaveDraft,
                enabled = !uiState.isSavingDraft && !uiState.isPublishing,
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .appleBouncy(
                        enabled = !uiState.isSavingDraft && !uiState.isPublishing,
                        pressedScale = 0.93f,
                        onClick = onSaveDraft
                    )
            ) {
                if (uiState.isSavingDraft) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("Taslak Kaydediliyor...")
                } else {
                    Text(
                        "Taslak Olarak Kaydet",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }

    // Confirmation dialog before live publishing
    if (showPublishConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showPublishConfirmDialog = false },
            icon = { Icon(Icons.Default.Public, null, tint = LibraLavender) },
            title = { Text("Kitabı Yayınlamak Üzeresin") },
            text = {
                Text(
                    "'${uiState.title}' başlıklı eserin okuyuculara açılacak. " +
                        "Yayınladıktan sonra istediğin zaman Yazar Editöründen yeni bölümler ekleyebilir veya güncelleyebilirsin."
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showPublishConfirmDialog = false
                        onPublish()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = LibraLavender)
                ) {
                    Text("Evet, Yayınla")
                }
            },
            dismissButton = {
                TextButton(onClick = { showPublishConfirmDialog = false }) {
                    Text("Vazgeç")
                }
            }
        )
    }
}

@Composable
private fun VisibilityOption(
    title: String,
    description: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    selected: Boolean,
    onSelect: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .appleBouncy(pressedScale = 0.97f, onClick = onSelect),
        color = if (selected) LibraLavenderSurface.copy(alpha = 0.5f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (selected) LibraLavender else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
        ),
        shape = RoundedCornerShape(14.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RadioButton(
                selected = selected,
                onClick = onSelect,
                colors = RadioButtonDefaults.colors(selectedColor = LibraLavender)
            )

            Spacer(Modifier.width(8.dp))

            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(icon, null, Modifier.size(16.dp), tint = if (selected) LibraLavenderText else MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.SemiBold
                    )
                }
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun ValidationRow(
    label: String,
    detail: String,
    isValid: Boolean,
    isRequired: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(22.dp)
                .clip(CircleShape)
                .background(
                    if (isValid) LibraLavenderSurface else if (isRequired) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.surfaceVariant
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (isValid) Icons.Default.Check else if (isRequired) Icons.Default.Close else Icons.Default.Warning,
                contentDescription = null,
                tint = if (isValid) LibraLavenderText else if (isRequired) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(14.dp)
            )
        }

        Spacer(Modifier.width(10.dp))

        Column(Modifier.weight(1f)) {
            Text(text = label, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
            Text(text = detail, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
