package com.libra.app.feature.profile

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.libra.app.core.di.ServiceLocator
import com.libra.app.domain.model.UserProfile
import com.libra.app.ui.components.AppButton
import com.libra.app.ui.components.UserAvatar

@Composable
fun ProfileEditScreen(
    profile: UserProfile,
    onSaved: (UserProfile) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ProfileEditViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current
    var currentPhotoUrl by remember(profile.profileImageUrl) { mutableStateOf<String?>(null) }

    LaunchedEffect(profile.uid, profile.profileImageUrl) {
        viewModel.initialize(profile)
        currentPhotoUrl = profile.profileImageUrl.takeIf { it.isNotBlank() }?.let {
            ServiceLocator.storageRepository.getSignedMediaUrl(it)
        }
    }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) viewModel.setPhotoUri(uri)
    }

    Column(
        modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(18.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Geri") }
            Text("Profil bilgileri", style = MaterialTheme.typography.headlineSmall)
        }

        Spacer(Modifier.height(18.dp))

        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            when {
                state.photoUri != null -> AsyncImage(
                    model = state.photoUri,
                    contentDescription = "Yeni profil fotoğrafı",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.size(112.dp).clip(CircleShape)
                )
                currentPhotoUrl != null -> AsyncImage(
                    model = currentPhotoUrl,
                    contentDescription = "Profil fotoğrafı",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.size(112.dp).clip(CircleShape)
                )
                else -> UserAvatar("", state.profile.initials, 112.dp)
            }
            IconButton(
                onClick = { picker.launch("image/*") },
                enabled = !state.isSaving,
                modifier = Modifier.align(Alignment.BottomCenter).padding(start = 84.dp)
            ) { Icon(Icons.Default.AddAPhoto, "Profil fotoğrafını değiştir") }
        }

        Spacer(Modifier.height(22.dp))
        OutlinedTextField(
            value = state.displayName,
            onValueChange = viewModel::setDisplayName,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Takma isim") },
            singleLine = true,
            enabled = !state.isSaving
        )
        Text(
            displayNameLimitText(state.profile),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 5.dp)
        )

        Spacer(Modifier.height(14.dp))
        OutlinedTextField(
            value = state.username,
            onValueChange = viewModel::setUsername,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Kullanıcı adı") },
            prefix = { Text("@") },
            singleLine = true,
            enabled = !state.isSaving
        )
        Text(
            usernameLimitText(state.profile),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 5.dp)
        )

        Spacer(Modifier.height(14.dp))
        OutlinedTextField(
            value = state.bio,
            onValueChange = viewModel::setBio,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Biyografi") },
            minLines = 4,
            maxLines = 5,
            enabled = !state.isSaving
        )

        state.errorMessage?.let {
            Spacer(Modifier.height(12.dp))
            Text(it, color = MaterialTheme.colorScheme.error)
        }

        Spacer(Modifier.height(22.dp))
        AppButton(
            text = "Değişiklikleri kaydet",
            onClick = { viewModel.save(context.contentResolver, onSaved) },
            isLoading = state.isSaving,
            enabled = !state.isSaving,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(8.dp))
        TextButton(onClick = onBack, enabled = !state.isSaving, modifier = Modifier.fillMaxWidth()) {
            Text("Vazgeç")
        }
    }
}

private fun displayNameLimitText(profile: UserProfile): String {
    val start = profile.displayNameChangeWindowStart?.time ?: 0L
    val used = if (start > 0L && System.currentTimeMillis() - start < 30L * 24 * 60 * 60 * 1000) {
        profile.displayNameChangesInWindow
    } else 0
    return "Takma isim: 30 günde 3 değişiklik hakkı • Bu dönemde $used/3 kullanıldı."
}

private fun usernameLimitText(profile: UserProfile): String {
    val last = profile.usernameLastChangedAt?.time ?: 0L
    if (last <= 0L) return "Kullanıcı adı: 6 ayda 1 değişiklik hakkı • Şu an değiştirilebilir."
    val remaining = 180L * 24 * 60 * 60 * 1000 - (System.currentTimeMillis() - last)
    if (remaining <= 0L) return "Kullanıcı adı: 6 ayda 1 değişiklik hakkı • Şu an değiştirilebilir."
    val days = (remaining + 86_400_000L - 1L) / 86_400_000L
    return "Kullanıcı adı: tekrar değiştirmek için yaklaşık $days gün beklemelisin."
}
