package com.libra.app.feature.profile

import android.content.ContentResolver
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.libra.app.core.di.ServiceLocator
import com.libra.app.core.result.AppResult
import com.libra.app.domain.model.StorageUploadRequest
import com.libra.app.domain.model.UserProfile
import com.libra.app.domain.repository.AuthRepository
import com.libra.app.domain.repository.StorageRepository
import com.libra.app.domain.repository.UserRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.Locale

enum class UsernameAvailability {
    IDLE,
    CHECKING,
    AVAILABLE,
    TAKEN
}

data class ProfileSetupState(
    val profile: UserProfile = UserProfile(),
    val displayName: String = "",
    val username: String = "",
    val bio: String = "",
    val photoUri: Uri? = null,
    val usernameAvailability: UsernameAvailability = UsernameAvailability.IDLE,
    val isSaving: Boolean = false,
    val errorMessage: String? = null
)

class ProfileSetupViewModel(
    private val authRepository: AuthRepository = ServiceLocator.authRepository,
    private val userRepository: UserRepository = ServiceLocator.userRepository,
    private val storageRepository: StorageRepository = ServiceLocator.storageRepository
) : ViewModel() {

    private val _state = MutableStateFlow(ProfileSetupState())
    val state: StateFlow<ProfileSetupState> = _state.asStateFlow()

    private var usernameCheckJob: Job? = null
    private var initializedUid: String? = null

    fun initialize(profile: UserProfile) {
        if (initializedUid == profile.uid) return

        initializedUid = profile.uid
        _state.value = ProfileSetupState(
            profile = profile,
            displayName = profile.displayName,
            username = normalizeUsername(profile.username),
            bio = profile.bio
        )

        if (isValidUsername(profile.username)) {
            setUsername(profile.username)
        }
    }

    fun setDisplayName(value: String) {
        _state.value = _state.value.copy(
            displayName = value.take(40),
            errorMessage = null
        )
    }

    fun setBio(value: String) {
        _state.value = _state.value.copy(
            bio = value.take(160),
            errorMessage = null
        )
    }

    fun setPhotoUri(uri: Uri?) {
        _state.value = _state.value.copy(
            photoUri = uri,
            errorMessage = null
        )
    }

    fun setUsername(value: String) {
        val normalized = value
            .lowercase(Locale.ROOT)
            .filter {
                (it in 'a'..'z') ||
                    (it in '0'..'9') ||
                    it == '.' ||
                    it == '_'
            }
            .take(20)

        usernameCheckJob?.cancel()

        if (!isValidUsername(normalized)) {
            _state.value = _state.value.copy(
                username = normalized,
                usernameAvailability = UsernameAvailability.IDLE,
                errorMessage = null
            )
            return
        }

        _state.value = _state.value.copy(
            username = normalized,
            usernameAvailability = UsernameAvailability.CHECKING,
            errorMessage = null
        )

        usernameCheckJob = viewModelScope.launch {
            delay(350)

            when (
                val result = userRepository.isUsernameAvailable(
                    normalized,
                    _state.value.profile.uid
                )
            ) {
                is AppResult.Success -> {
                    _state.value = _state.value.copy(
                        usernameAvailability = if (result.data) {
                            UsernameAvailability.AVAILABLE
                        } else {
                            UsernameAvailability.TAKEN
                        }
                    )
                }
                is AppResult.Error -> {
                    _state.value = _state.value.copy(
                        usernameAvailability = UsernameAvailability.IDLE,
                        errorMessage = result.error.message
                    )
                }
            }
        }
    }

    fun completeProfile(
        contentResolver: ContentResolver,
        onCompleted: () -> Unit
    ) {
        val snapshot = _state.value
        val uid = snapshot.profile.uid.ifBlank {
            authRepository.currentUser.value?.uid.orEmpty()
        }

        if (uid.isBlank()) {
            _state.value = snapshot.copy(errorMessage = "Oturum bulunamadı.")
            return
        }

        val displayName = snapshot.displayName.trim()
        val username = normalizeUsername(snapshot.username)

        if (displayName.length < 2) {
            _state.value = snapshot.copy(
                errorMessage = "Takma isim en az 2 karakter olmalı."
            )
            return
        }

        if (!isValidUsername(username)) {
            _state.value = snapshot.copy(
                errorMessage = "Kullanıcı adı 3-20 karakter olmalı; sadece harf, rakam, nokta ve alt çizgi kullanabilirsin."
            )
            return
        }

        viewModelScope.launch {
            _state.value = snapshot.copy(
                isSaving = true,
                errorMessage = null
            )

            var photoUrl = snapshot.profile.profileImageUrl

            val selectedPhoto = snapshot.photoUri
            if (selectedPhoto != null) {
                val bytes = runCatching {
                    contentResolver.openInputStream(selectedPhoto)?.use { it.readBytes() }
                }.getOrNull()

                if (bytes == null) {
                    _state.value = _state.value.copy(
                        isSaving = false,
                        errorMessage = "Seçilen fotoğraf okunamadı."
                    )
                    return@launch
                }

                if (bytes.size > 8 * 1024 * 1024) {
                    _state.value = _state.value.copy(
                        isSaving = false,
                        errorMessage = "Profil fotoğrafı 8 MB'dan küçük olmalı."
                    )
                    return@launch
                }

                val mimeType = contentResolver.getType(selectedPhoto) ?: "image/jpeg"
                val extension = when (mimeType.lowercase(Locale.ROOT)) {
                    "image/png" -> "png"
                    "image/webp" -> "webp"
                    else -> "jpg"
                }

                val uploadRequest = StorageUploadRequest(
                    fileName = "profile." + extension,
                    bytes = bytes,
                    contentType = if (mimeType.startsWith("image/")) {
                        mimeType
                    } else {
                        "image/jpeg"
                    },
                    targetDirectory = "users/" + uid
                )

                when (val upload = storageRepository.uploadMedia(uploadRequest).first()) {
                    is AppResult.Success -> photoUrl = upload.data
                    is AppResult.Error -> {
                        _state.value = _state.value.copy(
                            isSaving = false,
                            errorMessage = upload.error.message
                        )
                        return@launch
                    }
                }
            }

            val finalProfile = snapshot.profile.copy(
                uid = uid,
                displayName = displayName,
                username = username,
                bio = snapshot.bio.trim(),
                profileImageUrl = photoUrl,
                profileCompleted = true,
                updatedAt = System.currentTimeMillis()
            )

            when (val result = userRepository.completeProfile(finalProfile)) {
                is AppResult.Success -> {
                    _state.value = _state.value.copy(
                        profile = result.data,
                        displayName = result.data.displayName,
                        username = result.data.username,
                        bio = result.data.bio,
                        isSaving = false,
                        errorMessage = null
                    )
                    onCompleted()
                }
                is AppResult.Error -> {
                    _state.value = _state.value.copy(
                        isSaving = false,
                        errorMessage = result.error.message
                    )
                }
            }
        }
    }

    private fun normalizeUsername(value: String): String =
        value.trim().lowercase(Locale.ROOT)

    private fun isValidUsername(value: String): Boolean =
        value.matches(Regex("[a-z0-9._]{3,20}"))
}