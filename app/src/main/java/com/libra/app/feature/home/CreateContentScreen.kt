package com.libra.app.feature.home

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.libra.app.core.di.ServiceLocator
import com.libra.app.core.result.AppResult
import com.libra.app.domain.model.StorageUploadRequest
import com.libra.app.domain.model.UserProfile
import com.libra.app.ui.components.UserAvatar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

enum class CreateContentMode { POST, STORY }

@Composable
fun CreateContentScreen(
    mode: CreateContentMode,
    user: UserProfile,
    isPosting: Boolean,
    error: String?,
    onPublishPost: (title: String, text: String, tags: List<String>, mediaUrl: String, mediaType: String) -> Unit,
    onPublishStory: (text: String, mediaUrl: String, mediaType: String) -> Unit = { _, _, _ -> },
    onBack: () -> Unit,
    onClearError: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (mode == CreateContentMode.STORY) {
        StoryComposerScreen(user, isPosting, error, onPublishStory, onBack, onClearError, modifier)
        return
    }

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var title by remember { mutableStateOf("") }
    var text by remember { mutableStateOf("") }
    var tagsInput by remember { mutableStateOf("") }
    var selectedImage by remember { mutableStateOf<android.net.Uri?>(null) }
    var selectedImageName by remember { mutableStateOf("post.jpg") }
    var selectedImageType by remember { mutableStateOf("image/jpeg") }
    var uploadedMediaUrl by remember { mutableStateOf("") }
    var uploadProgress by remember { mutableIntStateOf(0) }
    var uploadError by remember { mutableStateOf<String?>(null) }
    var uploading by remember { mutableStateOf(false) }
    var uploadJob by remember { mutableStateOf<Job?>(null) }

    fun clearSelectedImage() {
        uploadJob?.cancel()
        uploadJob = null
        selectedImage = null
        uploadedMediaUrl = ""
        uploadProgress = 0
        uploading = false
        uploadError = null
    }

    fun startUpload(uri: android.net.Uri) {
        uploadJob?.cancel()
        selectedImage = uri
        uploadedMediaUrl = ""
        uploadProgress = 0
        uploadError = null
        uploading = true
        selectedImageType = context.contentResolver.getType(uri) ?: "image/jpeg"
        selectedImageName = uri.lastPathSegment?.substringAfterLast('/')?.ifBlank { "post.jpg" } ?: "post.jpg"

        uploadJob = scope.launch {
            try {
                val bytes = withContext(Dispatchers.IO) {
                    context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                }
                if (bytes == null || bytes.isEmpty()) throw IllegalStateException("Fotoğraf okunamadı.")
                if (bytes.size > 8 * 1024 * 1024) throw IllegalStateException("Fotoğraf 8 MB'dan küçük olmalı.")
                val result = ServiceLocator.storageRepository.uploadMedia(
                    StorageUploadRequest(
                        fileName = selectedImageName,
                        bytes = bytes,
                        contentType = selectedImageType.ifBlank { "image/jpeg" },
                        targetDirectory = "users/" + ServiceLocator.authRepository.currentUser.value?.uid.orEmpty()
                    ),
                    onProgress = { uploadProgress = it }
                ).first()
                when (result) {
                    is AppResult.Success -> {
                        uploadedMediaUrl = result.data
                        uploadProgress = 100
                    }
                    is AppResult.Error -> uploadError = result.error.message
                }
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Exception) {
                uploadError = e.localizedMessage ?: "Fotoğraf yüklenemedi."
            } finally {
                uploading = false
            }
        }
    }

    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let(::startUpload)
    }

    val tags = remember(tagsInput) {
        tagsInput.split(',', ' ', '\n')
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .map { if (it.startsWith("#") || it.startsWith("@")) it else "#$it" }
            .distinct().take(10)
    }

    val canPublish = text.trim().isNotEmpty() && !isPosting && !uploading &&
        (selectedImage == null || uploadedMediaUrl.isNotBlank())

    Column(modifier = modifier.fillMaxSize().imePadding()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack, enabled = !isPosting && !uploading) { Icon(Icons.Default.ArrowBack, "Geri") }
            Column(Modifier.weight(1f)) {
                Text("Gönderi oluştur", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text("Fikrini, fotoğrafını ve etiketlerini paylaş.", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            TextButton(enabled = canPublish, onClick = {
                onPublishPost(title.trim(), text.trim(), tags, uploadedMediaUrl, if (uploadedMediaUrl.isBlank()) "" else "image")
            }) {
                if (isPosting) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                else Text("Paylaş", fontWeight = FontWeight.Bold)
            }
        }

        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp)) {
            Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Column(Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        UserAvatar(user.profileImageUrl, user.initials, size = 46.dp)
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text(user.displayName.ifBlank { "Sen" }, fontWeight = FontWeight.Bold)
                            Text(user.username.ifBlank { "kullanici" }, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    Spacer(Modifier.height(18.dp))
                    OutlinedTextField(value = title, onValueChange = { if (it.length <= 120) title = it }, Modifier.fillMaxWidth(), singleLine = true, label = { Text("Başlık") }, placeholder = { Text("Gönderine bir başlık ver") }, supportingText = { Text("${title.length}/120") })
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(value = text, onValueChange = { if (it.length <= 2000) text = it }, Modifier.fillMaxWidth().height(190.dp), label = { Text("İçerik") }, placeholder = { Text("Ne düşünüyorsun? Bir hikâye, alıntı, fikir veya güncelleme paylaş…") }, supportingText = { Text("${text.length}/2000") })
                    Spacer(Modifier.height(14.dp))

                    if (selectedImage == null) {
                        Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(MaterialTheme.colorScheme.surfaceVariant).clickable(enabled = !isPosting) { imagePicker.launch("image/*") }.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.AddPhotoAlternate, null, Modifier.size(26.dp))
                            Spacer(Modifier.width(12.dp))
                            Column { Text("Fotoğraf ekle", fontWeight = FontWeight.SemiBold); Text("Seçtiğin anda yüklenmeye başlar.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                        }
                    } else {
                        Box(Modifier.fillMaxWidth().aspectRatio(1.45f).clip(RoundedCornerShape(16.dp))) {
                            AsyncImage(model = selectedImage, contentDescription = "Gönderi fotoğrafı", modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                            IconButton(onClick = ::clearSelectedImage, enabled = !isPosting, modifier = Modifier.align(Alignment.TopEnd).padding(8.dp).background(MaterialTheme.colorScheme.surface.copy(alpha = .88f), RoundedCornerShape(50))) {
                                Icon(Icons.Default.Close, "Fotoğrafı kaldır")
                            }
                            if (uploading || uploadError != null) {
                                Surface(Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(10.dp), shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.surface.copy(alpha = .94f)) {
                                    Column(Modifier.padding(10.dp)) {
                                        Text(if (uploadError != null) "Yükleme başarısız" else "Fotoğraf yükleniyor %$uploadProgress", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.labelMedium)
                                        if (uploadError == null) LinearProgressIndicator(progress = { uploadProgress / 100f }, Modifier.fillMaxWidth().padding(top = 7.dp))
                                        else Text(uploadError!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                                    }
                                }
                            } else if (uploadedMediaUrl.isNotBlank()) {
                                Surface(Modifier.align(Alignment.BottomStart).padding(10.dp), shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.surface.copy(alpha = .94f)) {
                                    Text("✓ Fotoğraf hazır", Modifier.padding(horizontal = 10.dp, vertical = 6.dp), fontWeight = FontWeight.SemiBold)
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(14.dp))
                    OutlinedTextField(value = tagsInput, onValueChange = { if (it.length <= 300) tagsInput = it }, Modifier.fillMaxWidth(), label = { Text("Etiketle") }, placeholder = { Text("#kitap #yazarlık veya @kullanici") }, leadingIcon = { Icon(Icons.Default.Tag, null) }, supportingText = { Text(if (tags.isEmpty()) "Virgül veya boşlukla ayır. En fazla 10 etiket." else tags.joinToString("  ")) }, maxLines = 2)
                }
            }
            Spacer(Modifier.height(12.dp))
            Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = .55f))) {
                Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Image, null, Modifier.size(20.dp)); Spacer(Modifier.width(10.dp))
                    Text("Fotoğraf seçildiği anda yüklenir. Yükleme tamamlanmadan paylaşım yapılamaz.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            if (error != null) { Spacer(Modifier.height(8.dp)); TextButton(onClick = onClearError) { Text(error, color = MaterialTheme.colorScheme.error) } }
            Spacer(Modifier.height(28.dp))
        }
    }
}

@Composable
private fun StoryComposerScreen(
    user: UserProfile,
    isPosting: Boolean,
    error: String?,
    onPublishStory: (String, String, String) -> Unit,
    onBack: () -> Unit,
    onClearError: () -> Unit,
    modifier: Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var text by remember { mutableStateOf("") }
    var selectedImage by remember { mutableStateOf<android.net.Uri?>(null) }
    var imageName by remember { mutableStateOf("story.jpg") }
    var imageType by remember { mutableStateOf("image/jpeg") }
    var uploadedMediaUrl by remember { mutableStateOf("") }
    var uploadProgress by remember { mutableIntStateOf(0) }
    var uploading by remember { mutableStateOf(false) }
    var uploadError by remember { mutableStateOf<String?>(null) }
    var uploadJob by remember { mutableStateOf<Job?>(null) }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        uploadJob?.cancel()
        selectedImage = uri
        uploadedMediaUrl = ""
        uploadProgress = 0
        uploadError = null
        uploading = true
        imageType = context.contentResolver.getType(uri) ?: "image/jpeg"
        imageName = uri.lastPathSegment?.substringAfterLast('/')?.ifBlank { "story.jpg" } ?: "story.jpg"
        uploadJob = scope.launch {
            try {
                val bytes = withContext(Dispatchers.IO) { context.contentResolver.openInputStream(uri)?.use { it.readBytes() } }
                if (bytes == null || bytes.isEmpty()) throw IllegalStateException("Fotoğraf okunamadı.")
                if (bytes.size > 8 * 1024 * 1024) throw IllegalStateException("Fotoğraf 8 MB'dan küçük olmalı.")
                when (val result = ServiceLocator.storageRepository.uploadMedia(
                    StorageUploadRequest(
                        fileName = imageName, bytes = bytes,
                        contentType = imageType.ifBlank { "image/jpeg" },
                        targetDirectory = "users/" + ServiceLocator.authRepository.currentUser.value?.uid.orEmpty()
                    ),
                    onProgress = { uploadProgress = it }
                ).first()) {
                    is AppResult.Success -> { uploadedMediaUrl = result.data; uploadProgress = 100 }
                    is AppResult.Error -> uploadError = result.error.message
                }
            } catch (e: kotlinx.coroutines.CancellationException) { throw e }
            catch (e: Exception) { uploadError = e.localizedMessage ?: "Fotoğraf yüklenemedi." }
            finally { uploading = false }
        }
    }

    val canPublish = (text.isNotBlank() || selectedImage != null) && !isPosting && !uploading &&
        (selectedImage == null || uploadedMediaUrl.isNotBlank())

    Column(modifier.fillMaxSize().imePadding()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack, enabled = !isPosting && !uploading) { Icon(Icons.Default.ArrowBack, "Geri") }
            Column(Modifier.weight(1f)) {
                Text("Hikâye oluştur", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text("24 saat boyunca paylaş.", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            TextButton(enabled = canPublish, onClick = { onPublishStory(text.trim(), uploadedMediaUrl, if (uploadedMediaUrl.isBlank()) "" else "image") }) {
                if (isPosting) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp) else Text("Paylaş", fontWeight = FontWeight.Bold)
            }
        }
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Column(Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        UserAvatar(user.profileImageUrl, user.initials, size = 44.dp); Spacer(Modifier.width(12.dp))
                        Column { Text(user.displayName.ifBlank { "Sen" }, fontWeight = FontWeight.Bold); Text("Hikâyen 24 saat sonra otomatik olarak süresi dolacak.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                    }
                    Spacer(Modifier.height(16.dp))
                    OutlinedTextField(value = text, onValueChange = { if (it.length <= 500) text = it }, Modifier.fillMaxWidth().height(150.dp), label = { Text("Hikâyen") }, placeholder = { Text("Bugün ne paylaşmak istiyorsun?") }, supportingText = { Text("${text.length}/500") })
                    Spacer(Modifier.height(12.dp))
                    if (selectedImage == null) {
                        Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(MaterialTheme.colorScheme.surfaceVariant).clickable(enabled = !isPosting) { picker.launch("image/*") }.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.AddPhotoAlternate, null, Modifier.size(26.dp)); Spacer(Modifier.width(12.dp))
                            Column { Text("Fotoğraf ekle", fontWeight = FontWeight.SemiBold); Text("Seçtiğin anda yüklenmeye başlar.", style = MaterialTheme.typography.bodySmall) }
                        }
                    } else {
                        Box(Modifier.fillMaxWidth().aspectRatio(1.2f).clip(RoundedCornerShape(16.dp))) {
                            AsyncImage(model = selectedImage, contentDescription = "Hikâye fotoğrafı", modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                            IconButton(onClick = { uploadJob?.cancel(); selectedImage = null; uploadedMediaUrl = ""; uploading = false; uploadError = null }, enabled = !isPosting, modifier = Modifier.align(Alignment.TopEnd).padding(8.dp).background(MaterialTheme.colorScheme.surface.copy(alpha = .88f), RoundedCornerShape(50))) { Icon(Icons.Default.Close, "Fotoğrafı kaldır") }
                            if (uploading || uploadError != null) {
                                Surface(Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(10.dp), shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.surface.copy(alpha = .94f)) {
                                    Column(Modifier.padding(10.dp)) {
                                        Text(if (uploadError != null) "Yükleme başarısız" else "Fotoğraf yükleniyor %$uploadProgress", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.labelMedium)
                                        if (uploadError == null) LinearProgressIndicator(progress = { uploadProgress / 100f }, Modifier.fillMaxWidth().padding(top = 7.dp))
                                        else Text(uploadError!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                                    }
                                }
                            } else if (uploadedMediaUrl.isNotBlank()) {
                                Surface(Modifier.align(Alignment.BottomStart).padding(10.dp), shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.surface.copy(alpha = .94f)) { Text("✓ Fotoğraf hazır", Modifier.padding(horizontal = 10.dp, vertical = 6.dp), fontWeight = FontWeight.SemiBold) }
                            }
                        }
                    }
                }
            }
            if (error != null) TextButton(onClick = onClearError) { Text(error, color = MaterialTheme.colorScheme.error) }
        }
    }
}
