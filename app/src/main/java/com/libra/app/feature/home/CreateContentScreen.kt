package com.libra.app.feature.home

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.Edit
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.libra.app.domain.model.UserProfile
import com.libra.app.ui.components.UserAvatar

enum class CreateContentMode { POST, STORY }

@Composable
fun CreateContentScreen(
    mode: CreateContentMode,
    user: UserProfile,
    isPosting: Boolean,
    error: String?,
    onPublishPost: (String) -> Unit,
    onBack: () -> Unit,
    onClearError: () -> Unit,
    modifier: Modifier = Modifier
) {
    var text by remember { mutableStateOf("") }
    val isPost = mode == CreateContentMode.POST

    Column(modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Geri") }
            Icon(if (isPost) Icons.Default.Edit else Icons.Default.AutoStories, null)
            Text(
                if (isPost) "Gönderi oluştur" else "Hikâye oluştur",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(start = 8.dp)
            )
            Spacer(Modifier.weight(1f))
            TextButton(
                enabled = isPost && text.trim().isNotEmpty() && !isPosting,
                onClick = { onPublishPost(text.trim()) }
            ) { Text(if (isPosting) "Paylaşılıyor…" else "Paylaş") }
        }

        Spacer(Modifier.height(18.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            UserAvatar(user.profileImageUrl, user.initials, size = 46.dp)
            Text("  " + user.displayName.ifBlank { "Sen" }, fontWeight = FontWeight.SemiBold)
        }

        Spacer(Modifier.height(16.dp))

        OutlinedTextField(
            value = text,
            onValueChange = { if (it.length <= 1000) text = it },
            modifier = Modifier.fillMaxWidth().weight(1f),
            placeholder = { Text(if (isPost) "Ne paylaşmak istiyorsun?" else "Hikâyene bir şeyler yaz…") },
            supportingText = { Text(text.length.toString() + "/1000") }
        )

        if (!isPost) {
            Text(
                "Hikâye oluşturma ekranı hazır. Yayınlama altyapısını ayrı olarak bağlayacağız.",
                modifier = Modifier.padding(top = 10.dp),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        if (error != null) {
            TextButton(onClick = onClearError) {
                Text(error, color = MaterialTheme.colorScheme.error)
            }
        }
    }
}
