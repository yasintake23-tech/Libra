package com.libra.app.feature.profile

import android.content.ContentResolver
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.libra.app.core.di.ServiceLocator
import com.libra.app.core.result.AppResult
import com.libra.app.domain.model.StorageUploadRequest
import com.libra.app.domain.model.UserProfile
import com.libra.app.domain.repository.StorageRepository
import com.libra.app.domain.repository.UserRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

data class ProfileEditState(
    val profile: UserProfile = UserProfile(),
    val displayName: String = "",
    val username: String = "",
    val bio: String = "",
    val photoUri: Uri? = null,
    val isSaving: Boolean = false,
    val errorMessage: String? = null
)

class ProfileEditViewModel(
    private val userRepository: UserRepository = ServiceLocator.userRepository,
    private val storageRepository: StorageRepository = ServiceLocator.storageRepository
) : ViewModel() {
    private val _state = MutableStateFlow(ProfileEditState())
    val state: StateFlow<ProfileEditState> = _state.asStateFlow()

    fun initialize(profile: UserProfile) {
        if (_state.value.profile.uid == profile.uid && _state.value.profile.uid.isNotBlank()) return
        _state.value = ProfileEditState(profile, profile.displayName, profile.username, profile.bio)
    }

    fun setDisplayName(value: String) { _state.value = _state.value.copy(displayName = value.take(40), errorMessage = null) }
    fun setUsername(value: String) {
        val normalized = value.lowercase(Locale.ROOT)
            .filter { it in 'a'..'z' || it in '0'..'9' || it == '.' || it == '_' }
            .take(20)
        _state.value = _state.value.copy(username = normalized, errorMessage = null)
    }
    fun setBio(value: String) { _state.value = _state.value.copy(bio = value.take(160), errorMessage = null) }
    fun setPhotoUri(uri: Uri?) { _state.value = _state.value.copy(photoUri = uri, errorMessage = null) }

    fun save(contentResolver: ContentResolver, onSaved: (UserProfile) -> Unit) {
        val snapshot = _state.value
        val profile = snapshot.profile
        val displayName = snapshot.displayName.trim()
        val username = snapshot.username.trim().lowercase(Locale.ROOT)

        if (profile.uid.isBlank()) {
            _state.value = snapshot.copy(errorMessage = "Oturum bulunamadı.")
            return
        }
        if (displayName.length < 2) {
            _state.value = snapshot.copy(errorMessage = "Takma isim en az 2 karakter olmalı.")
            return
        }
        if (!username.matches(Regex("[a-z0-9._]{3,20}"))) {
            _state.value = snapshot.copy(errorMessage = "Kullanıcı adı 3-20 karakter olmalı.")
            return
        }

        viewModelScope.launch {
            _state.value = snapshot.copy(isSaving = true, errorMessage = null)
            var uploadedPhotoUrl: String? = null
            try {
                snapshot.photoUri?.let { uri ->
                    val bytes = withContext(Dispatchers.IO) {
                        contentResolver.openInputStream(uri)?.use { it.readBytes() }
                    }
                    if (bytes == null) {
                        _state.value = _state.value.copy(isSaving = false, errorMessage = "Profil fotoğrafı okunamadı.")
                        return@launch
                    }
                    if (bytes.size > 8 * 1024 * 1024) {
                        _state.value = _state.value.copy(isSaving = false, errorMessage = "Profil fotoğrafı 8 MB'dan büyük olamaz.")
                        return@launch
                    }

                    val mime = contentResolver.getType(uri) ?: "image/jpeg"
                    val extension = when (mime.lowercase(Locale.ROOT)) {
                        "image/png" -> "png"
                        "image/webp" -> "webp"
                        "image/heic" -> "heic"
                        "image/heif" -> "heif"
                        else -> "jpg"
                    }
                    val request = StorageUploadRequest(
                        fileName = "profile.$extension",
                        bytes = bytes,
                        contentType = if (mime.startsWith("image/")) mime else "image/jpeg",
                        targetDirectory = "users/${profile.uid}"
                    )

                    when (val upload = storageRepository.uploadMedia(request).first()) {
                        is AppResult.Success -> uploadedPhotoUrl = upload.data
                        is AppResult.Error -> {
                            _state.value = _state.value.copy(
                                isSaving = false,
                                errorMessage = "Profil fotoğrafı yüklenemedi: ${upload.error.message}"
                            )
                            return@launch
                        }
                    }
                }

                val edited = profile.copy(displayName = displayName, username = username, bio = snapshot.bio.trim())
                when (val result = userRepository.updateProfileDetails(edited, uploadedPhotoUrl)) {
                    is AppResult.Success -> {
                        _state.value = ProfileEditState(
                            profile = result.data,
                            displayName = result.data.displayName,
                            username = result.data.username,
                            bio = result.data.bio
                        )
                        onSaved(result.data)
                    }
                    is AppResult.Error -> {
                        uploadedPhotoUrl?.let { runCatching { storageRepository.deleteMedia(it) } }
                        _state.value = _state.value.copy(isSaving = false, errorMessage = result.error.message)
                    }
                }
            } catch (e: Exception) {
                uploadedPhotoUrl?.let { runCatching { storageRepository.deleteMedia(it) } }
                _state.value = _state.value.copy(isSaving = false, errorMessage = e.localizedMessage ?: "Profil güncellenemedi.")
            }
        }
    }
}
