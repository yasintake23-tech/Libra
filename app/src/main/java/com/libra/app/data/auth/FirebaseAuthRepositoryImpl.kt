package com.libra.app.data.auth

import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.libra.app.core.result.AppError
import com.libra.app.core.result.AppResult
import com.libra.app.domain.model.UserProfile
import com.libra.app.domain.repository.AuthRepository
import com.libra.app.domain.repository.UserRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.tasks.await

class FirebaseAuthRepositoryImpl(private val userRepository: UserRepository) : AuthRepository {
    private val firebaseAuth: FirebaseAuth? by lazy {
        runCatching { FirebaseApp.getInstance(); FirebaseAuth.getInstance() }.getOrNull()
    }
    private val _currentUser = MutableStateFlow<UserProfile?>(null)
    override val currentUser: StateFlow<UserProfile?> = _currentUser.asStateFlow()
    private val _isAuthenticated = MutableStateFlow(false)
    override val isAuthenticated: StateFlow<Boolean> = _isAuthenticated.asStateFlow()

    init {
        firebaseAuth?.addAuthStateListener { auth ->
            if (auth.currentUser == null) {
                _currentUser.value = null
                _isAuthenticated.value = false
            }
        }
    }

    override suspend fun checkCurrentSession(): AppResult<UserProfile?> {
        val auth = firebaseAuth ?: return AppResult.Error(AppError.Auth("Firebase yapılandırması bulunamadı. google-services.json ekleyin."))
        val firebaseUser = auth.currentUser ?: run { _currentUser.value = null; _isAuthenticated.value = false; return AppResult.Success(null) }
        return try {
            when (val profileResult = userRepository.getUserProfile(firebaseUser.uid).first()) {
                is AppResult.Error -> AppResult.Error(profileResult.error)
                is AppResult.Success -> {
                    val profile = profileResult.data ?: buildProfile(firebaseUser)
                    if (profileResult.data == null) {
                        when (val saved = userRepository.createOrUpdateProfile(profile)) {
                            is AppResult.Error -> return AppResult.Error(saved.error)
                            is AppResult.Success -> _currentUser.value = saved.data
                        }
                    } else _currentUser.value = profile
                    _isAuthenticated.value = true
                    AppResult.Success(_currentUser.value)
                }
            }
        } catch (e: Exception) {
            AppResult.Error(AppError.Database("Kullanıcı profili alınamadı: ${e.localizedMessage}", e))
        }
    }

    override fun signInWithGoogleIdToken(idToken: String, displayName: String?, email: String?, photoUrl: String?): Flow<AppResult<UserProfile>> = flow {
        val auth = firebaseAuth ?: run { emit(AppResult.Error(AppError.Auth("Firebase yapılandırması bulunamadı. google-services.json ekleyin."))); return@flow }
        try {
            val authResult = auth.signInWithCredential(GoogleAuthProvider.getCredential(idToken, null)).await()
            val firebaseUser = authResult.user ?: throw IllegalStateException("Firebase user is null after sign-in")
            val generatedProfile = buildProfile(firebaseUser, displayName, email, photoUrl)
            when (val stored = userRepository.getUserProfile(firebaseUser.uid).first()) {
                is AppResult.Error -> { auth.signOut(); emit(AppResult.Error(stored.error)) }
                is AppResult.Success -> {
                    val existing = stored.data
                    val profile = existing?.copy(
                        displayName = displayName?.takeIf { it.isNotBlank() } ?: existing.displayName,
                        email = email?.takeIf { it.isNotBlank() } ?: existing.email,
                        profileImageUrl = photoUrl?.takeIf { it.isNotBlank() } ?: existing.profileImageUrl,
                        updatedAt = System.currentTimeMillis()
                    ) ?: generatedProfile
                    when (val saved = userRepository.createOrUpdateProfile(profile)) {
                        is AppResult.Success -> { _currentUser.value = saved.data; _isAuthenticated.value = true; emit(AppResult.Success(saved.data)) }
                        is AppResult.Error -> { auth.signOut(); emit(AppResult.Error(saved.error)) }
                    }
                }
            }
        } catch (e: Exception) {
            _currentUser.value = null; _isAuthenticated.value = false
            emit(AppResult.Error(AppError.Auth("Google ile giriş başarısız: ${e.localizedMessage ?: "Bilinmeyen hata."}", cause = e)))
        }
    }

    override suspend fun signOut(): AppResult<Unit> {
        val auth = firebaseAuth ?: return AppResult.Error(AppError.Auth("Firebase yapılandırması bulunamadı."))
        return try { auth.signOut(); _currentUser.value = null; _isAuthenticated.value = false; AppResult.Success(Unit) }
        catch (e: Exception) { AppResult.Error(AppError.Auth("Oturum kapatılamadı: ${e.localizedMessage}", cause = e)) }
    }

    private fun buildProfile(firebaseUser: FirebaseUser, displayName: String? = null, email: String? = null, photoUrl: String? = null): UserProfile {
        val resolvedEmail = email ?: firebaseUser.email.orEmpty()
        val usernameBase = resolvedEmail.substringBefore("@").lowercase().replace(Regex("[^a-z0-9_]+"), "_").trim('_').ifBlank { "reader_${firebaseUser.uid.take(8)}" }
        val now = System.currentTimeMillis()
        return UserProfile(firebaseUser.uid, displayName ?: firebaseUser.displayName.orEmpty().ifBlank { "Libra Okuru" }, usernameBase, resolvedEmail, photoUrl ?: firebaseUser.photoUrl?.toString().orEmpty(), createdAt = now, updatedAt = now)
    }
}
