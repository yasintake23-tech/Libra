package com.libra.app.data.user

import com.libra.app.core.result.AppError
import com.libra.app.core.result.AppResult
import com.libra.app.domain.model.UserProfile
import com.libra.app.domain.repository.UserRepository
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.util.concurrent.ConcurrentHashMap

class FirebaseUserRepositoryImpl : UserRepository {
    private val cache = ConcurrentHashMap<String, UserProfile>()
    private val usersRef: DatabaseReference by lazy { FirebaseDatabase.getInstance().getReference("users") }

    override fun getUserProfile(uid: String): Flow<AppResult<UserProfile?>> = callbackFlow {
        cache[uid]?.let { trySend(AppResult.Success(it)) }
        val ref = usersRef.child(uid)
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (!snapshot.exists()) { trySend(AppResult.Success(null)); return }
                try {
                    val profile = snapshot.getValue(UserProfile::class.java)
                    if (profile == null) trySend(AppResult.Error(AppError.Database("Kullanıcı profili okunamadı.")))
                    else { cache[uid] = profile; trySend(AppResult.Success(profile)) }
                } catch (e: Exception) { trySend(AppResult.Error(AppError.Database("Kullanıcı profili çözümlenemedi.", e))) }
            }
            override fun onCancelled(error: DatabaseError) { trySend(AppResult.Error(AppError.Database(error.message, error.toException()))) }
        }
        ref.addValueEventListener(listener)
        awaitClose { ref.removeEventListener(listener) }
    }

    override suspend fun createOrUpdateProfile(profile: UserProfile): AppResult<UserProfile> {
        val updated = profile.copy(updatedAt = System.currentTimeMillis())
        return try { usersRef.child(profile.uid).setValue(updated).await(); cache[profile.uid] = updated; AppResult.Success(updated) }
        catch (e: Exception) { AppResult.Error(AppError.Database("Kullanıcı profili kaydedilemedi: ${e.localizedMessage}", e)) }
    }

    override suspend fun updateBio(uid: String, bio: String): AppResult<Unit> = updateFields(uid, mapOf("bio" to bio, "updatedAt" to System.currentTimeMillis()))
    override suspend fun updateProfilePhoto(uid: String, photoUrl: String): AppResult<Unit> = updateFields(uid, mapOf("profileImageUrl" to photoUrl, "updatedAt" to System.currentTimeMillis()))

    override fun searchUsers(query: String): Flow<AppResult<List<UserProfile>>> = callbackFlow {
        val normalized = query.trim().lowercase()
        if (normalized.isBlank()) { trySend(AppResult.Success(emptyList())); close(); return@callbackFlow }
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val results = snapshot.children.mapNotNull { child -> runCatching { child.getValue(UserProfile::class.java) }.getOrNull() }
                    .filter { it.displayName.lowercase().contains(normalized) || it.username.lowercase().contains(normalized) }
                    .take(50)
                results.forEach { cache[it.uid] = it }
                trySend(AppResult.Success(results)); close()
            }
            override fun onCancelled(error: DatabaseError) { trySend(AppResult.Error(AppError.Database(error.message, error.toException()))); close() }
        }
        usersRef.addListenerForSingleValueEvent(listener)
        awaitClose { }
    }

    private suspend fun updateFields(uid: String, values: Map<String, Any?>): AppResult<Unit> = try {
        usersRef.child(uid).updateChildren(values).await()
        val cached = cache[uid]
        if (cached != null) cache[uid] = cached.copy(
            bio = values["bio"] as? String ?: cached.bio,
            profileImageUrl = values["profileImageUrl"] as? String ?: cached.profileImageUrl,
            updatedAt = values["updatedAt"] as? Long ?: cached.updatedAt
        )
        AppResult.Success(Unit)
    } catch (e: Exception) { AppResult.Error(AppError.Database("Profil güncellenemedi: ${e.localizedMessage}", e)) }
}
