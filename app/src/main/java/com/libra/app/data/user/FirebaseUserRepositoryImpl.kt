package com.libra.app.data.user

import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.firestore.FirebaseFirestore
import com.libra.app.core.result.AppError
import com.libra.app.core.result.AppResult
import com.libra.app.domain.model.UserProfile
import com.libra.app.domain.repository.UserRepository
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap

class FirebaseUserRepositoryImpl : UserRepository {

    private val cache = ConcurrentHashMap<String, UserProfile>()

    private val usersRef: DatabaseReference by lazy {
        FirebaseDatabase.getInstance().getReference("users")
    }

    private val firestore: FirebaseFirestore by lazy {
        FirebaseFirestore.getInstance()
    }

    private val usernamesRef
        get() = firestore.collection("usernames")

    override fun getUserProfile(uid: String): Flow<AppResult<UserProfile?>> = callbackFlow {
        cache[uid]?.let { trySend(AppResult.Success(it)) }

        val ref = usersRef.child(uid)
        val listener = object : com.google.firebase.database.ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (!snapshot.exists()) {
                    trySend(AppResult.Success(null))
                    return
                }

                try {
                    val profile = snapshot.getValue(UserProfile::class.java)
                    if (profile == null) {
                        trySend(AppResult.Error(AppError.Database("Kullanıcı profili okunamadı.")))
                    } else {
                        cache[uid] = profile
                        trySend(AppResult.Success(profile))
                    }
                } catch (e: Exception) {
                    trySend(AppResult.Error(AppError.Database("Kullanıcı profili çözümlenemedi.", e)))
                }
            }

            override fun onCancelled(error: DatabaseError) {
                trySend(AppResult.Error(AppError.Database(error.message, error.toException())))
            }
        }

        ref.addValueEventListener(listener)
        awaitClose { ref.removeEventListener(listener) }
    }

    override suspend fun createOrUpdateProfile(profile: UserProfile): AppResult<UserProfile> {
        val updated = profile.copy(updatedAt = System.currentTimeMillis())

        return try {
            usersRef.child(profile.uid).setValue(updated).await()
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
                AppError.Validation("Kullanıcı adı 3-20 karakter olmalı; sadece harf, rakam, nokta ve alt çizgi kullanabilirsin.")
            )
        }

        if (profile.displayName.trim().length < 2) {
            return AppResult.Error(AppError.Validation("Takma isim en az 2 karakter olmalı."))
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

                val data = mapOf(
                    "uid" to profile.uid,
                    "username" to normalizedUsername,
                    "createdAt" to createdAt,
                    "updatedAt" to System.currentTimeMillis()
                )

                transaction.set(usernameDoc, data)
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

    override suspend fun updateBio(uid: String, bio: String): AppResult<Unit> =
        updateFields(
            uid,
            mapOf(
                "bio" to bio.trim(),
                "updatedAt" to System.currentTimeMillis()
            )
        )

    override suspend fun updateProfilePhoto(uid: String, photoUrl: String): AppResult<Unit> =
        updateFields(
            uid,
            mapOf(
                "profileImageUrl" to photoUrl,
                "updatedAt" to System.currentTimeMillis()
            )
        )

    override fun searchUsers(query: String): Flow<AppResult<List<UserProfile>>> = callbackFlow {
        val normalized = query.trim().lowercase(Locale.ROOT)

        if (normalized.isBlank()) {
            trySend(AppResult.Success(emptyList()))
            close()
            return@callbackFlow
        }

        val listener = object : com.google.firebase.database.ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val results = snapshot.children
                    .mapNotNull { child ->
                        runCatching { child.getValue(UserProfile::class.java) }.getOrNull()
                    }
                    .filter { it.profileCompleted }
                    .filter {
                        it.displayName.lowercase(Locale.ROOT).contains(normalized) ||
                            it.username.lowercase(Locale.ROOT).contains(normalized)
                    }
                    .take(50)

                results.forEach { cache[it.uid] = it }
                trySend(AppResult.Success(results))
                close()
            }

            override fun onCancelled(error: DatabaseError) {
                trySend(AppResult.Error(AppError.Database(error.message, error.toException())))
                close()
            }
        }

        usersRef.addListenerForSingleValueEvent(listener)
        awaitClose { }
    }

    private suspend fun updateFields(
        uid: String,
        values: Map<String, Any?>
    ): AppResult<Unit> = try {
        usersRef.child(uid).updateChildren(values).await()
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