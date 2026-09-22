package com.libra.app.feature.home

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Tag
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import com.libra.app.domain.model.UserProfile
import com.libra.app.ui.components.UserAvatar

enum class CreateContentMode { POST, STORY }

@Composable
fun CreateContentScreen(
    mode: CreateContentMode,
    user: UserProfile,
    isPosting: Boolean,
    error: String?,
    onPublishPost: (
        title: String,
        text: String,
        tags: List<String>,
        imageBytes: ByteArray?,
        imageFileName: String,
        imageContentType: String
    ) -> Unit,
    onBack: () -> Unit,
    onClearError: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (mode == CreateContentMode.STORY) {
        StoryPlaceholderScreen(onBack, modifier)
        return
    }

    val context = LocalContext.current
    var title by remember { mutableStateOf("") }
    var text by remember { mutableStateOf("") }
    var tagsInput by remember { mutableStateOf("") }
    var selectedImage by remember { mutableStateOf<android.net.Uri?>(null) }
    var selectedImageName by remember { mutableStateOf("post.jpg") }
    var selectedImageType by remember { mutableStateOf("image/jpeg") }

    val imagePicker = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        selectedImage = uri
        if (uri != null) {
            context.contentResolver.getType(uri)?.let { selectedImageType = it }
            selectedImageName = uri.lastPathSegment?.substringAfterLast('/')?.ifBlank { "post.jpg" } ?: "post.jpg"
        }
    }

    val tags = remember(tagsInput) {
        tagsInput
            .split(',', ' ', '\n')
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .map { if (it.startsWith("#") || it.startsWith("@")) it else "#$it" }
            .distinct()
            .take(10)
    }

    Column(modifier = modifier.fillMaxSize().imePadding()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack, enabled = !isPosting) {
                Icon(Icons.Default.ArrowBack, "Geri")
            }
            Column(Modifier.weight(1f)) {
                Text("Gönderi oluştur", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text("Fikrini, fotoğrafını ve etiketlerini paylaş.", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            TextButton(
                enabled = text.trim().isNotEmpty() && !isPosting,
                onClick = {
                    val bytes = selectedImage?.let { uri ->
                        runCatching { context.contentResolver.openInputStream(uri)?.use { it.readBytes() } }.getOrNull()
                    }
                    onPublishPost(title.trim(), text.trim(), tags, bytes, selectedImageName, selectedImageType)
                }
            ) {
                if (isPosting) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                else Text("Paylaş", fontWeight = FontWeight.Bold)
            }
        }

        Column(
            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp)
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        UserAvatar(user.profileImageUrl, user.initials, size = 46.dp)
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text(user.displayName.ifBlank { "Sen" }, fontWeight = FontWeight.Bold)
                            Text("${user.username.ifBlank { "kullanici" }}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }

                    Spacer(Modifier.height(18.dp))

                    OutlinedTextField(
                        value = title,
                        onValueChange = { if (it.length <= 120) title = it },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        label = { Text("Başlık") },
                        placeholder = { Text("Gönderine bir başlık ver") },
                        supportingText = { Text("${title.length}/120") }
                    )

                    Spacer(Modifier.height(12.dp))

                    OutlinedTextField(
                        value = text,
                        onValueChange = { if (it.length <= 2000) text = it },
                        modifier = Modifier.fillMaxWidth().height(190.dp),
                        label = { Text("İçerik") },
                        placeholder = { Text("Ne düşünüyorsun? Bir hikâye, alıntı, fikir veya güncelleme paylaş…") },
                        supportingText = { Text("${text.length}/2000") }
                    )

                    Spacer(Modifier.height(14.dp))

                    if (selectedImage == null) {
                        Row(
                            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .clickable(enabled = !isPosting) { imagePicker.launch("image/*") }
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.AddPhotoAlternate, null, Modifier.size(26.dp))
                            Spacer(Modifier.width(12.dp))
                            Column {
                                Text("Fotoğraf ekle", fontWeight = FontWeight.SemiBold)
                                Text("Gönderine bir kapak veya anlık fotoğraf ekle", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    } else {
                        Box(
                            modifier = Modifier.fillMaxWidth().aspectRatio(1.45f).clip(RoundedCornerShape(16.dp))
                        ) {
                            AsyncImage(
                                model = selectedImage,
                                contentDescription = "Gönderi fotoğrafı",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                            IconButton(
                                onClick = { selectedImage = null },
                                modifier = Modifier.align(Alignment.TopEnd).padding(8.dp)
                                    .background(MaterialTheme.colorScheme.surface.copy(alpha = .88f), RoundedCornerShape(50))
                            ) {
                                Icon(Icons.Default.Close, "Fotoğrafı kaldır")
                            }
                        }
                    }

                    Spacer(Modifier.height(14.dp))

                    OutlinedTextField(
                        value = tagsInput,
                        onValueChange = { if (it.length <= 300) tagsInput = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Etiketle") },
                        placeholder = { Text("#kitap #yazarlık veya @kullanici") },
                        leadingIcon = { Icon(Icons.Default.Tag, null) },
                        supportingText = {
                            Text(if (tags.isEmpty()) "Virgül veya boşlukla ayır. En fazla 10 etiket." else tags.joinToString("  "))
                        },
                        maxLines = 2
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = .55f))
            ) {
                Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Image, null, Modifier.size(20.dp))
                    Spacer(Modifier.width(10.dp))
                    Text("Fotoğraflar şimdilik tek görsel olarak paylaşılır. Video gönderisi kapalı.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            if (error != null) {
                Spacer(Modifier.height(8.dp))
                TextButton(onClick = onClearError) { Text(error, color = MaterialTheme.colorScheme.error) }
            }

            Spacer(Modifier.height(28.dp))
        }
    }
}

@Composable
private fun StoryPlaceholderScreen(
    onBack: () -> Unit,
    modifier: Modifier
) {
    Column(modifier.fillMaxSize().padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Geri") }
            Icon(Icons.Default.AutoStories, null)
            Spacer(Modifier.width(8.dp))
            Text("Hikâye oluştur", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(20.dp))
        Text("Hikâye editörü daha sonra bağlanacak. Gönderi editörü artık başlık, fotoğraf ve etiket desteğiyle hazır.", style = MaterialTheme.typography.bodyLarge)
    }
}