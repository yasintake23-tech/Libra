package com.libra.app.feature.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.libra.app.core.di.ServiceLocator
import com.libra.app.core.result.AppResult
import com.libra.app.domain.model.AdminRole
import com.libra.app.domain.model.CosmeticRole
import com.libra.app.domain.model.UserModeration
import com.libra.app.domain.model.UserProfile
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
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    init {
        viewModelScope.launch {
            repo.observeMembers().collect { if (it is AppResult.Success) _members.value = it.data else if (it is AppResult.Error) _error.value = it.error.message }
        }
        viewModelScope.launch {
            repo.observeCosmeticRoles().collect { if (it is AppResult.Success) _cosmeticRoles.value = it.data else if (it is AppResult.Error) _error.value = it.error.message }
        }
    }

    fun clearError() { _error.value = null }

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