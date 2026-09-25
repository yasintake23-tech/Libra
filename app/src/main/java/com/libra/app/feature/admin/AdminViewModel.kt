package com.libra.app.feature.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.libra.app.core.di.ServiceLocator
import com.libra.app.core.result.AppResult
import com.libra.app.domain.model.AdminRole
import com.libra.app.domain.model.CosmeticRole
import com.libra.app.domain.model.UserModeration
import com.libra.app.domain.model.UserProfile
import com.libra.app.domain.model.AppUpdate
import android.net.Uri
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AdminViewModel : ViewModel() {
    private val repo = ServiceLocator.adminRepository

    private val _members = MutableStateFlow<List<UserProfile>>(emptyList())
    val members: StateFlow<List<UserProfile>> = _members.asStateFlow()
    private val _cosmeticRoles = MutableStateFlow<List<CosmeticRole>>(emptyList())
    val cosmeticRoles: StateFlow<List<CosmeticRole>> = _cosmeticRoles.asStateFlow()
    private val _activeRelease = MutableStateFlow<AppUpdate?>(null)
    val activeRelease: StateFlow<AppUpdate?> = _activeRelease.asStateFlow()
    private val _releaseProgress = MutableStateFlow(0)
    val releaseProgress: StateFlow<Int> = _releaseProgress.asStateFlow()
    private val _releaseBusy = MutableStateFlow(false)
    val releaseBusy: StateFlow<Boolean> = _releaseBusy.asStateFlow()
    private val _releaseStatus = MutableStateFlow<String?>(null)
    val releaseStatus: StateFlow<String?> = _releaseStatus.asStateFlow()
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    init {
        viewModelScope.launch {
            when (val result = ServiceLocator.updateRepository.getActiveRelease()) {
                is AppResult.Success -> _activeRelease.value = result.data
                is AppResult.Error -> _error.value = result.error.message
            }
        }
        viewModelScope.launch {
            repo.observeMembers().collect { if (it is AppResult.Success) _members.value = it.data else if (it is AppResult.Error) _error.value = it.error.message }
        }
        viewModelScope.launch {
            repo.observeCosmeticRoles().collect { if (it is AppResult.Success) _cosmeticRoles.value = it.data else if (it is AppResult.Error) _error.value = it.error.message }
        }
    }

    fun clearError() { _error.value = null }
    fun clearReleaseStatus() { _releaseStatus.value = null }

    fun publishAppRelease(
        uri: Uri, fileName: String, fileSize: Long, releaseId: String,
        versionCode: Long, versionName: String, changelog: List<String>, forceUpdate: Boolean
    ) = viewModelScope.launch {
        _releaseBusy.value = true
        _releaseProgress.value = 0
        _releaseStatus.value = null
        val oldRelease = _activeRelease.value
        try {
            val uploaded = ServiceLocator.appReleaseStorageRepository.uploadApk(
                uri = uri, fileName = fileName, fileSize = fileSize,
                onProgress = { _releaseProgress.value = it }
            ).first()
            if (uploaded !is AppResult.Success) {
                _releaseStatus.value = (uploaded as AppResult.Error).error.message
                return@launch
            }

            val release = AppUpdate(
                releaseId = releaseId, versionCode = versionCode, versionName = versionName,
                apkObjectKey = uploaded.data, apkSize = fileSize, changelog = changelog,
                forceUpdate = forceUpdate, createdAt = System.currentTimeMillis()
            )

            when (val published = ServiceLocator.updateRepository.publishRelease(release)) {
                is AppResult.Error -> {
                    ServiceLocator.appReleaseStorageRepository.deleteApk(uploaded.data)
                    _releaseStatus.value = published.error.message
                    return@launch
                }
                is AppResult.Success -> Unit
            }

            _activeRelease.value = release
            oldRelease?.apkObjectKey
                ?.takeIf { it.isNotBlank() && it != release.apkObjectKey }
                ?.let { oldKey ->
                    when (val deleted = ServiceLocator.appReleaseStorageRepository.deleteApk(oldKey)) {
                        is AppResult.Error -> {
                            _releaseStatus.value = "Yayınlandı. Eski APK silinemedi: " + deleted.error.message
                            return@launch
                        }
                        is AppResult.Success -> Unit
                    }
                }

            _releaseProgress.value = 100
            _releaseStatus.value = "Yeni sürüm başarıyla yayınlandı."
        } catch (e: Exception) {
            _releaseStatus.value = e.localizedMessage ?: "Güncelleme yayınlanamadı."
        } finally {
            _releaseBusy.value = false
        }
    }


    fun saveModeration(uid: String, value: UserModeration, onDone: (Boolean) -> Unit = {}) = viewModelScope.launch {
        val r = repo.updateModeration(uid, value)
        if (r is AppResult.Error) _error.value = r.error.message
        onDone(r is AppResult.Success)
    }

    fun saveProfile(profile: UserProfile, onDone: (Boolean) -> Unit = {}) = viewModelScope.launch {
        val r = repo.updateMemberProfile(profile)
        if (r is AppResult.Error) _error.value = r.error.message
        onDone(r is AppResult.Success)
    }

    suspend fun loadAdminRole(uid: String): AdminRole? =
        (repo.getAdminRole(uid) as? AppResult.Success)?.data

    fun saveAdminRole(uid: String, role: AdminRole?, onDone: (Boolean) -> Unit = {}) = viewModelScope.launch {
        val r = repo.setAdminRole(uid, role)
        if (r is AppResult.Error) _error.value = r.error.message
        onDone(r is AppResult.Success)
    }

    fun saveCosmetic(role: CosmeticRole, onDone: (Boolean) -> Unit = {}) = viewModelScope.launch {
        val r = repo.saveCosmeticRole(role)
        if (r is AppResult.Error) _error.value = r.error.message
        onDone(r is AppResult.Success)
    }

    fun deleteCosmetic(id: String) = viewModelScope.launch {
        val r = repo.deleteCosmeticRole(id)
        if (r is AppResult.Error) _error.value = r.error.message
    }

    fun assignCosmetic(uid: String, roleId: String, assigned: Boolean, onDone: (Boolean) -> Unit = {}) = viewModelScope.launch {
        val r = repo.assignCosmeticRole(uid, roleId, assigned)
        if (r is AppResult.Error) _error.value = r.error.message
        onDone(r is AppResult.Success)
    }
}