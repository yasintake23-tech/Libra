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
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.tasks.await

class FirebaseAuthRepositoryImpl(
    private val userRepository: UserRepository
) : AuthRepository {

    private val firebaseAuth: FirebaseAuth? by lazy {
        runCatching {
            FirebaseApp.getInstance()
            FirebaseAuth.getInstance()
        }.getOrNull()
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
        val auth = firebaseAuth
            ?: return AppResult.Error(AppError.Auth("Firebase yapılandırması bulunamadı."))

        val firebaseUser = auth.currentUser
            ?: run {
                _currentUser.value = null
                _isAuthenticated.value = false
                return AppResult.Success(null)
            }

        return runCatching { withTimeout(15_000) { loadOrCreateProfile(firebaseUser) } }
            .getOrElse { AppResult.Error(AppError.Auth("Oturum hazırlanırken zaman aşımı oldu.", cause = it)) }
    }

    override fun signInWithGoogleIdToken(
        idToken: String,
        displayName: String?,
        email: String?,
        photoUrl: String?
    ): Flow<AppResult<UserProfile>> = flow {
        val auth = firebaseAuth
            ?: run {
                emit(AppResult.Error(AppError.Auth("Firebase yapılandırması bulunamadı.")))
                return@flow
            }

        try {
            val credential = GoogleAuthProvider.getCredential(idToken, null)
            val authResult = auth.signInWithCredential(credential).await()
            val firebaseUser = authResult.user
                ?: throw IllegalStateException("Firebase kullanıcı hesabı alınamadı.")

            val stored = userRepository.getUserProfile(firebaseUser.uid).first()
            when (stored) {
                is AppResult.Error -> {
                    auth.signOut()
                    emit(AppResult.Error(stored.error))
                }
                is AppResult.Success -> {
                    val existing = stored.data
                    val base = buildProfile(firebaseUser, displayName, email, photoUrl)
                    val profile = existing?.copy(
                        email = if (email.isNullOrBlank()) existing.email else email,
                        profileImageUrl = if (existing.profileImageUrl.isBlank()) {
                            photoUrl.orEmpty().ifBlank { existing.profileImageUrl }
                        } else {
                            existing.profileImageUrl
                        },
                        updatedAt = System.currentTimeMillis()
                    ) ?: base

                    when (val saved = userRepository.createOrUpdateProfile(profile)) {
                        is AppResult.Success -> {
                            _currentUser.value = saved.data
                            _isAuthenticated.value = true
                            emit(AppResult.Success(saved.data))
                        }
                        is AppResult.Error -> {
                            auth.signOut()
                            emit(AppResult.Error(saved.error))
                        }
                    }
                }
            }
        } catch (e: Exception) {
            _currentUser.value = null
            _isAuthenticated.value = false
            emit(
                AppResult.Error(
                    AppError.Auth(
                        "Google ile giriş başarısız: " + (e.localizedMessage ?: "Bilinmeyen hata."),
                        cause = e
                    )
                )
            )
        }
    }

    override suspend fun signInWithEmailPassword(
        email: String,
        password: String
    ): AppResult<UserProfile> {
        val auth = firebaseAuth
            ?: return AppResult.Error(AppError.Auth("Firebase yapılandırması bulunamadı."))

        if (email.isBlank() || password.isBlank()) {
            return AppResult.Error(AppError.Validation("E-posta ve şifre gerekli."))
        }

        return try {
            val result = auth.signInWithEmailAndPassword(email.trim(), password).await()
            val firebaseUser = result.user
                ?: return AppResult.Error(AppError.Auth("Kullanıcı hesabı alınamadı."))
            runCatching { withTimeout(15_000) { loadOrCreateProfile(firebaseUser) } }
                .getOrElse { AppResult.Error(AppError.Auth("Oturum hazırlanırken zaman aşımı oldu.", cause = it)) }
        } catch (e: Exception) {
            AppResult.Error(
                AppError.Auth(
                    "E-posta ile giriş başarısız: " + (e.localizedMessage ?: "E-posta veya şifre hatalı."),
                    cause = e
                )
            )
        }
    }

    override suspend fun createAccountWithEmailPassword(
        email: String,
        password: String
    ): AppResult<UserProfile> {
        val auth = firebaseAuth
            ?: return AppResult.Error(AppError.Auth("Firebase yapılandırması bulunamadı."))

        val normalizedEmail = email.trim()
        if (!normalizedEmail.contains("@") || !normalizedEmail.contains(".")) {
            return AppResult.Error(AppError.Validation("Geçerli bir e-posta adresi gir."))
        }
        if (password.length < 6) {
            return AppResult.Error(AppError.Validation("Şifre en az 6 karakter olmalı."))
        }

        return try {
            val result = auth.createUserWithEmailAndPassword(normalizedEmail, password).await()
            val firebaseUser = result.user
                ?: return AppResult.Error(AppError.Auth("Yeni kullanıcı hesabı oluşturulamadı."))

            runCatching { withTimeout(15_000) { loadOrCreateProfile(firebaseUser) } }
                .getOrElse { AppResult.Error(AppError.Auth("Hesap hazırlanırken zaman aşımı oldu.", cause = it)) }
        } catch (e: Exception) {
            AppResult.Error(
                AppError.Auth(
                    "Hesap oluşturulamadı: " + (e.localizedMessage ?: "Bilinmeyen hata."),
                    cause = e
                )
            )
        }
    }

    override suspend fun signOut(): AppResult<Unit> {
        val auth = firebaseAuth
            ?: return AppResult.Error(AppError.Auth("Firebase yapılandırması bulunamadı."))

        return try {
            auth.signOut()
            _currentUser.value = null
            _isAuthenticated.value = false
            AppResult.Success(Unit)
        } catch (e: Exception) {
            AppResult.Error(AppError.Auth("Oturum kapatılamadı.", cause = e))
        }
    }

    private suspend fun loadOrCreateProfile(firebaseUser: FirebaseUser): AppResult<UserProfile> {
        return try {
            when (val profileResult = userRepository.getUserProfile(firebaseUser.uid).first()) {
                is AppResult.Error -> {
                    _currentUser.value = null
                    _isAuthenticated.value = false
                    AppResult.Error(profileResult.error)
                }
                is AppResult.Success -> {
                    val profile = profileResult.data ?: buildProfile(firebaseUser)
                    val saved = if (profileResult.data == null) {
                        userRepository.createOrUpdateProfile(profile)
                    } else {
                        AppResult.Success(profile)
                    }

                    when (saved) {
                        is AppResult.Success -> {
                            _currentUser.value = saved.data
                            _isAuthenticated.value = true
                            AppResult.Success(saved.data)
                        }
                        is AppResult.Error -> {
                            _currentUser.value = null
                            _isAuthenticated.value = false
                            AppResult.Error(saved.error)
                        }
                    }
                }
            }
        } catch (e: Exception) {
            _currentUser.value = null
            _isAuthenticated.value = false
            AppResult.Error(AppError.Database("Kullanıcı profili alınamadı.", e))
        }
    }

    private fun buildProfile(
        firebaseUser: FirebaseUser,
        displayName: String? = null,
        email: String? = null,
        photoUrl: String? = null
    ): UserProfile {
        val resolvedEmail = email ?: firebaseUser.email.orEmpty()
        val emailLocalPart = resolvedEmail.substringBefore("@")
        val fallbackName = emailLocalPart
            .replace(".", " ")
            .replace("_", " ")
            .trim()
            .ifBlank { "Libra Okuru" }

        val now = System.currentTimeMillis()

        return UserProfile(
            uid = firebaseUser.uid,
            displayName = displayName.orEmpty().ifBlank {
                firebaseUser.displayName.orEmpty().ifBlank { fallbackName }
            },
            username = "",
            email = resolvedEmail,
            profileImageUrl = photoUrl.orEmpty().ifBlank {
                firebaseUser.photoUrl?.toString().orEmpty()
            },
            profileCompleted = false,
            createdAt = now,
            updatedAt = now
        )
    }
}