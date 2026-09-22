package com.libra.app.data.user

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Source
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

    private val followsRef
        get() = firestore.collection("follows")

    override fun getUserProfile(uid: String): Flow<AppResult<UserProfile?>> = callbackFlow {
        var registration: ListenerRegistration? = null

        registration = usersRef.document(uid).addSnapshotListener { snapshot, error ->
            if (error != null) {
                trySend(AppResult.Error(AppError.Database(error.message ?: "Kullanıcı profili okunamadı.", error)))
                return@addSnapshotListener
            }

            if (snapshot == null || !snapshot.exists()) {
                cache.remove(uid)
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

    override suspend fun getUserProfileFresh(uid: String): AppResult<UserProfile?> {
        if (uid.isBlank()) return AppResult.Success(null)

        return try {
            val snapshot = usersRef.document(uid).get(Source.SERVER).await()
            if (!snapshot.exists()) {
                cache.remove(uid)
                AppResult.Success(null)
            } else {
                val profile = snapshot.toObject(UserProfile::class.java)
                if (profile == null) {
                    cache.remove(uid)
                    AppResult.Success(null)
                } else {
                    cache[uid] = profile
                    AppResult.Success(profile)
                }
            }
        } catch (e: Exception) {
            AppResult.Error(AppError.Database("Kullanıcı profili doğrulanamadı.", e))
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
        val normalizedProfile = profile.copy(
            uid = profile.uid.trim(),
            displayName = profile.displayName.trim(),
            username = normalizeUsername(profile.username),
            bio = profile.bio.trim()
        )

        if (normalizedProfile.uid.isBlank()) {
            return AppResult.Error(AppError.Auth("Profil sahibi belirlenemedi."))
        }

        val normalizedUsername = normalizedProfile.username

        if (!isValidUsername(normalizedUsername)) {
            return AppResult.Error(
                AppError.Validation(
                    "Kullanıcı adı 3-20 karakter olmalı; sadece harf, rakam, nokta ve alt çizgi kullanabilirsin."
                )
            )
        }

        if (normalizedProfile.displayName.length < 2) {
            return AppResult.Error(
                AppError.Validation("Takma isim en az 2 karakter olmalı.")
            )
        }

        return try {
            val usernameDoc = usernamesRef.document(normalizedUsername)

            firestore.runTransaction { transaction ->
                val snapshot = transaction.get(usernameDoc)
                val existingUid = snapshot.getString("uid")

                if (snapshot.exists() && existingUid != normalizedProfile.uid) {
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
                        "uid" to normalizedProfile.uid,
                        "username" to normalizedUsername,
                        "createdAt" to createdAt,
                        "updatedAt" to System.currentTimeMillis()
                    )
                )
                null
            }.await()

            val finalProfile = normalizedProfile.copy(
                profileCompleted = true,
                updatedAt = System.currentTimeMillis()
            )

            when (val saved = createOrUpdateProfile(finalProfile)) {
                is AppResult.Success -> saved
                is AppResult.Error -> {
                    runCatching {
                        val current = usernameDoc.get(Source.SERVER).await()
                        if (current.getString("uid") == normalizedProfile.uid) {
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

    override suspend fun getFollowingIds(uid: String): AppResult<Set<String>> {
        if (uid.isBlank()) return AppResult.Success(emptySet())

        return try {
            val snapshot = followsRef
                .whereEqualTo("followerId", uid)
                .limit(1000)
                .get()
                .await()

            val ids = snapshot.documents.mapNotNull { it.getString("followingId") }.distinct()
            val validIds = ids.chunked(10).flatMap { chunk ->
                usersRef.whereIn("__name__", chunk).get(Source.SERVER).await()
                    .documents.map { it.id }
            }.toSet()

            snapshot.documents
                .filter { it.getString("followingId")?.let { id -> id !in validIds } == true }
                .forEach { doc -> runCatching { doc.reference.delete().await() } }

            AppResult.Success(validIds)
        } catch (e: Exception) {
            AppResult.Error(AppError.Database("Takip edilenler yüklenemedi.", e))
        }
    }

    override suspend fun isFollowing(
        followerId: String,
        followingId: String
    ): AppResult<Boolean> {
        if (followerId.isBlank() || followingId.isBlank() || followerId == followingId) {
            return AppResult.Success(false)
        }

        return try {
            AppResult.Success(
                followsRef.document(followerId + "_" + followingId).get().await().exists()
            )
        } catch (e: Exception) {
            AppResult.Error(AppError.Database("Takip durumu okunamadı.", e))
        }
    }

    override suspend fun followUser(
        followerId: String,
        followingId: String
    ): AppResult<Unit> {
        if (followerId.isBlank() || followingId.isBlank()) {
            return AppResult.Error(AppError.Validation("Geçerli bir kullanıcı seçilmedi."))
        }
        if (followerId == followingId) {
            return AppResult.Error(AppError.Validation("Kendini takip edemezsin."))
        }

        return try {
            val ref = followsRef.document(followerId + "_" + followingId)
            if (!ref.get().await().exists()) {
                ref.set(
                    mapOf(
                        "followerId" to followerId,
                        "followingId" to followingId,
                        "createdAt" to System.currentTimeMillis()
                    )
                ).await()
            }
            AppResult.Success(Unit)
        } catch (e: Exception) {
            AppResult.Error(AppError.Database("Takip işlemi tamamlanamadı.", e))
        }
    }

    override suspend fun unfollowUser(
        followerId: String,
        followingId: String
    ): AppResult<Unit> {
        if (followerId.isBlank() || followingId.isBlank()) {
            return AppResult.Error(AppError.Validation("Geçerli bir kullanıcı seçilmedi."))
        }

        return try {
            followsRef.document(followerId + "_" + followingId).delete().await()
            AppResult.Success(Unit)
        } catch (e: Exception) {
            AppResult.Error(AppError.Database("Takipten çıkılamadı.", e))
        }
    }

    override suspend fun getFollowers(uid: String): AppResult<List<UserProfile>> =
        getSocialProfiles(uid, "followingId")

    override suspend fun getFollowing(uid: String): AppResult<List<UserProfile>> =
        getSocialProfiles(uid, "followerId")

    private suspend fun getSocialProfiles(
        uid: String,
        directionField: String
    ): AppResult<List<UserProfile>> {
        if (uid.isBlank()) return AppResult.Success(emptyList())

        return try {
            val snapshot = followsRef
                .whereEqualTo(directionField, uid)
                .limit(1000)
                .get(Source.SERVER)
                .await()

            val otherField = if (directionField == "followingId") "followerId" else "followingId"
            val ids = snapshot.documents.mapNotNull { it.getString(otherField) }.distinct()

            if (ids.isEmpty()) {
                return AppResult.Success(emptyList())
            }

            val profiles = ids.chunked(10).flatMap { chunk ->
                usersRef.whereIn("__name__", chunk).get(Source.SERVER).await()
                    .documents.mapNotNull { document ->
                        runCatching { document.toObject(UserProfile::class.java) }
                            .getOrNull()
                            ?.takeIf { it.profileCompleted && it.uid.isNotBlank() }
                    }
            }

            val existingIds = profiles.map { it.uid }.toSet()
            // Remove stale follow documents that point to users deleted from Firebase.
            snapshot.documents
                .filter { doc -> doc.getString(otherField)?.let { it !in existingIds } == true }
                .forEach { doc -> runCatching { doc.reference.delete().await() } }

            profiles.forEach { cache[it.uid] = it }
            AppResult.Success(
                profiles.sortedBy { it.displayName.lowercase(Locale.ROOT) }
            )
        } catch (e: Exception) {
            AppResult.Error(AppError.Database("Takip listesi yüklenemedi.", e))
        }
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