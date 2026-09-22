package com.libra.app.data.user

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.libra.app.core.result.AppError
import com.libra.app.core.result.AppResult
import com.libra.app.domain.model.UserProfile
import com.libra.app.domain.repository.UserRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.channels.awaitClose
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap

class FirebaseUserRepositoryImpl : UserRepository {

    private val cache = ConcurrentHashMap<String, UserProfile>()

    private val firestore: FirebaseFirestore by lazy {
        FirebaseFirestore.getInstance()
    }

    private val usersRef
        get() = firestore.collection("users")

    private val usernamesRef
        get() = firestore.collection("usernames")

    override fun getUserProfile(uid: String): Flow<AppResult<UserProfile?>> = callbackFlow {
        cache[uid]?.let { trySend(AppResult.Success(it)) }

        var registration: ListenerRegistration? = null

        registration = usersRef.document(uid).addSnapshotListener { snapshot, error ->
            if (error != null) {
                trySend(AppResult.Error(AppError.Database(error.message ?: "Kullanıcı profili okunamadı.", error)))
                return@addSnapshotListener
            }

            if (snapshot == null || !snapshot.exists()) {
                trySend(AppResult.Success(null))
                return@addSnapshotListener
            }

            val profile = runCatching {
                snapshot.toObject(UserProfile::class.java)
            }.getOrNull()

            if (profile == null) {
                trySend(AppResult.Error(AppError.Database("Kullanıcı profili çözümlenemedi.")))
            } else {
                cache[uid] = profile
                trySend(AppResult.Success(profile))
            }
        }

        awaitClose {
            registration?.remove()
        }
    }

    override suspend fun createOrUpdateProfile(profile: UserProfile): AppResult<UserProfile> {
        val updated = profile.copy(updatedAt = System.currentTimeMillis())

        return try {
            usersRef.document(profile.uid).set(updated).await()
            cache[profile.uid] = updated
            AppResult.Success(updated)
        } catch (e: Exception) {
            AppResult.Error(AppError.Database("Kullanıcı profili kaydedilemedi.", e))
        }
    }

    override suspend fun completeProfile(profile: UserProfile): AppResult<UserProfile> {
        val normalizedUsername = normalizeUsername(profile.username)

        if (!isValidUsername(normalizedUsername)) {
            return AppResult.Error(
                AppError.Validation(
                    "Kullanıcı adı 3-20 karakter olmalı; sadece harf, rakam, nokta ve alt çizgi kullanabilirsin."
                )
            )
        }

        if (profile.displayName.trim().length < 2) {
            return AppResult.Error(
                AppError.Validation("Takma isim en az 2 karakter olmalı.")
            )
        }

        return try {
            val usernameDoc = usernamesRef.document(normalizedUsername)

            firestore.runTransaction { transaction ->
                val snapshot = transaction.get(usernameDoc)
                val existingUid = snapshot.getString("uid")

                if (snapshot.exists() && existingUid != profile.uid) {
                    throw UsernameTakenException()
                }

                val createdAt = if (snapshot.exists()) {
                    snapshot.getLong("createdAt") ?: System.currentTimeMillis()
                } else {
                    System.currentTimeMillis()
                }

                transaction.set(
                    usernameDoc,
                    mapOf(
                        "uid" to profile.uid,
                        "username" to normalizedUsername,
                        "createdAt" to createdAt,
                        "updatedAt" to System.currentTimeMillis()
                    )
                )
                null
            }.await()

            val finalProfile = profile.copy(
                username = normalizedUsername,
                displayName = profile.displayName.trim(),
                bio = profile.bio.trim(),
                profileCompleted = true,
                updatedAt = System.currentTimeMillis()
            )

            when (val saved = createOrUpdateProfile(finalProfile)) {
                is AppResult.Success -> saved
                is AppResult.Error -> {
                    runCatching {
                        val current = usernameDoc.get().await()
                        if (current.getString("uid") == profile.uid) {
                            usernameDoc.delete().await()
                        }
                    }
                    saved
                }
            }
        } catch (e: UsernameTakenException) {
            AppResult.Error(AppError.Validation("Bu kullanıcı adı zaten kullanılıyor."))
        } catch (e: Exception) {
            AppResult.Error(
                AppError.Database(
                    "Profil oluşturulamadı: " + (e.localizedMessage ?: "Bilinmeyen hata."),
                    e
                )
            )
        }
    }

    override suspend fun isUsernameAvailable(
        username: String,
        currentUid: String?
    ): AppResult<Boolean> {
        val normalized = normalizeUsername(username)

        if (!isValidUsername(normalized)) {
            return AppResult.Success(false)
        }

        return try {
            val snapshot = usernamesRef.document(normalized).get().await()

            if (!snapshot.exists()) {
                AppResult.Success(true)
            } else {
                AppResult.Success(snapshot.getString("uid") == currentUid)
            }
        } catch (e: Exception) {
            AppResult.Error(AppError.Database("Kullanıcı adı kontrol edilemedi.", e))
        }
    }

    override suspend fun updateBio(uid: String, bio: String): AppResult<Unit> {
        return updateFields(
            uid,
            mapOf(
                "bio" to bio.trim(),
                "updatedAt" to System.currentTimeMillis()
            )
        )
    }

    override suspend fun updateProfilePhoto(uid: String, photoUrl: String): AppResult<Unit> {
        return updateFields(
            uid,
            mapOf(
                "profileImageUrl" to photoUrl,
                "updatedAt" to System.currentTimeMillis()
            )
        )
    }

    override fun searchUsers(query: String): Flow<AppResult<List<UserProfile>>> = callbackFlow {
        val normalized = query.trim().lowercase(Locale.ROOT)

        if (normalized.isBlank()) {
            trySend(AppResult.Success(emptyList()))
            close()
            return@callbackFlow
        }

        try {
            val end = normalized + '\uf8ff'

            val usernameTask = usersRef
                .whereGreaterThanOrEqualTo("username", normalized)
                .whereLessThan("username", end)
                .limit(25)
                .get()

            val displayNameTask = usersRef
                .whereGreaterThanOrEqualTo("displayName", normalized)
                .whereLessThan("displayName", end)
                .limit(25)
                .get()

            val usernameSnapshot = usernameTask.await()
            val displayNameSnapshot = displayNameTask.await()

            val results = (usernameSnapshot.documents + displayNameSnapshot.documents)
                .mapNotNull { document ->
                    runCatching {
                        document.toObject(UserProfile::class.java)
                    }.getOrNull()
                }
                .filter { it.profileCompleted }
                .distinctBy { it.uid }
                .filter { it.username.startsWith(normalized) || it.displayName.lowercase(Locale.ROOT).startsWith(normalized) }
                .take(50)

            results.forEach { cache[it.uid] = it }
            trySend(AppResult.Success(results))
            close()
        } catch (e: Exception) {
            trySend(AppResult.Error(AppError.Database("Kullanıcılar aranamadı.", e)))
            close()
        }

        awaitClose { }
    }

    private suspend fun updateFields(
        uid: String,
        values: Map<String, Any?>
    ): AppResult<Unit> = try {
        usersRef.document(uid).update(values).await()

        val cached = cache[uid]
        if (cached != null) {
            cache[uid] = cached.copy(
                bio = values["bio"] as? String ?: cached.bio,
                profileImageUrl = values["profileImageUrl"] as? String ?: cached.profileImageUrl,
                updatedAt = values["updatedAt"] as? Long ?: cached.updatedAt
            )
        }

        AppResult.Success(Unit)
    } catch (e: Exception) {
        AppResult.Error(AppError.Database("Profil güncellenemedi.", e))
    }

    private fun normalizeUsername(value: String): String =
        value.trim().lowercase(Locale.ROOT)

    private fun isValidUsername(value: String): Boolean =
        value.matches(Regex("[a-z0-9._]{3,20}"))

    private class UsernameTakenException : Exception()
}