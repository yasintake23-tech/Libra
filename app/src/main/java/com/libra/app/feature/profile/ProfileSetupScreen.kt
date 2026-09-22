package com.libra.app.feature.profile

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.libra.app.domain.model.UserProfile
import com.libra.app.ui.components.AppButton
import com.libra.app.ui.components.UserAvatar

@Composable
fun ProfileSetupScreen(
    profile: UserProfile,
    onCompleted: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ProfileSetupViewModel = viewModel()
) {
    val state by viewModel.state.collectAsState()
    val context = androidx.compose.ui.platform.LocalContext.current

    val fieldColors = OutlinedTextFieldDefaults.colors(
        focusedTextColor = MaterialTheme.colorScheme.onSurface,
        unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
        disabledTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
        focusedBorderColor = MaterialTheme.colorScheme.primary,
        unfocusedBorderColor = MaterialTheme.colorScheme.outline,
        focusedLabelColor = MaterialTheme.colorScheme.primary,
        unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
        cursorColor = MaterialTheme.colorScheme.primary
    )

    LaunchedEffect(profile.uid) {
        viewModel.initialize(profile)
    }

    val picker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            viewModel.setPhotoUri(uri)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            "Profilini oluşturalım",
            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold)
        )

        Spacer(Modifier.height(8.dp))

        Text(
            "Libra'da seni nasıl tanıyacağımızı seç.",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodyMedium
        )

        Spacer(Modifier.height(24.dp))

        Box(contentAlignment = Alignment.BottomEnd) {
            val selectedUri = state.photoUri

            if (selectedUri != null || state.profile.profileImageUrl.isNotBlank()) {
                val imageModel: Any = selectedUri ?: state.profile.profileImageUrl
                AsyncImage(
                    model = imageModel,
                    contentDescription = "Profil fotoğrafı",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(112.dp)
                        .clip(CircleShape)
                        .border(
                            2.dp,
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.35f),
                            CircleShape
                        )
                )
            } else {
                UserAvatar(
                    photoUrl = "",
                    initials = state.displayName.ifBlank { "L" }.take(2),
                    size = 112.dp
                )
            }

            androidx.compose.material3.IconButton(
                onClick = { picker.launch("image/*") },
                enabled = !state.isSaving,
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary)
            ) {
                Icon(
                    Icons.Default.AddAPhoto,
                    contentDescription = "Fotoğraf seç",
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        Spacer(Modifier.height(8.dp))

        Text(
            "Profil fotoğrafı seç",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary
        )

        Spacer(Modifier.height(22.dp))

        OutlinedTextField(
            value = state.displayName,
            onValueChange = viewModel::setDisplayName,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Takma isim") },
            placeholder = { Text("Mesela Yasin") },
            singleLine = true,
            colors = fieldColors
        )

        Spacer(Modifier.height(12.dp))

        OutlinedTextField(
            value = state.username,
            onValueChange = viewModel::setUsername,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Kullanıcı adı") },
            prefix = { Text("@") },
            placeholder = { Text("kullaniciadi") },
            singleLine = true,
            colors = fieldColors
        )

        when (state.usernameAvailability) {
            UsernameAvailability.CHECKING -> {
                Text(
                    "Kullanıcı adı kontrol ediliyor…",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.fillMaxWidth().padding(top = 6.dp)
                )
            }
            UsernameAvailability.AVAILABLE -> {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        "  Bu kullanıcı adı kullanılabilir.",
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }
            UsernameAvailability.TAKEN -> {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        "  Bu kullanıcı adı zaten alınmış.",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }
            UsernameAvailability.IDLE -> {
                Text(
                    "3-20 karakter: harf, rakam, nokta veya alt çizgi.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.fillMaxWidth().padding(top = 6.dp)
                )
            }
        }

        Spacer(Modifier.height(12.dp))

        OutlinedTextField(
            value = state.bio,
            onValueChange = viewModel::setBio,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Hakkında") },
            placeholder = { Text("Kendinden biraz bahset…") },
            minLines = 4,
            maxLines = 5,
            colors = fieldColors
        )

        Spacer(Modifier.height(10.dp))

        Text(
            "E-posta: " + state.profile.email,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.fillMaxWidth()
        )

        state.errorMessage?.let {
            Spacer(Modifier.height(12.dp))
            Text(
                it,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.fillMaxWidth()
            )
        }

        Spacer(Modifier.height(22.dp))

        AppButton(
            text = "Devam et",
            onClick = {
                viewModel.completeProfile(
                    contentResolver = context.contentResolver,
                    onCompleted = onCompleted
                )
            },
            isLoading = state.isSaving,
            enabled = state.username.matches(Regex("[a-z0-9._]{3,20}")) && state.displayName.trim().length >= 2 && !state.isSaving,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(10.dp))

        Text(
            "Kullanıcı adın Libra'da seni bulmak için kullanılacak.",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.labelSmall
        )
    }
}