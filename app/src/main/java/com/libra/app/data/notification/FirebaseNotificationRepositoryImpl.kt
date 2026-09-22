package com.libra.app.data.notification

import com.google.firebase.firestore.FirebaseFirestore
import com.libra.app.core.result.AppError
import com.libra.app.core.result.AppResult
import com.libra.app.domain.model.AppNotification
import com.libra.app.domain.repository.NotificationRepository
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class FirebaseNotificationRepositoryImpl : NotificationRepository {
    private val firestore = FirebaseFirestore.getInstance()
    private val ref get() = firestore.collection("notifications")

    override fun observeNotifications(uid: String, limit: Long): Flow<AppResult<List<AppNotification>>> = callbackFlow {
        if (uid.isBlank()) {
            trySend(AppResult.Success(emptyList()))
            close()
            return@callbackFlow
        }
        val registration = ref.whereEqualTo("recipientId", uid)
            .limit(limit)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(AppResult.Error(AppError.Database("Bildirimler yüklenemedi.", error)))
                    return@addSnapshotListener
                }
                val list = snapshot?.documents.orEmpty().mapNotNull { doc ->
                    runCatching { doc.toObject(AppNotification::class.java)?.copy(id = doc.id) }.getOrNull()
                }
                trySend(AppResult.Success(list.sortedByDescending { it.createdAt }))
            }
        awaitClose { registration.remove() }
    }

    override suspend fun create(notification: AppNotification): AppResult<Unit> = try {
        if (notification.recipientId.isBlank() || notification.actorId.isBlank()) {
            return AppResult.Error(AppError.Validation("Geçersiz bildirim."))
        }
        ref.add(notification.copy(id = "")).await()
        AppResult.Success(Unit)
    } catch (e: Exception) {
        AppResult.Error(AppError.Database("Bildirim oluşturulamadı.", e))
    }

    override suspend fun markRead(notificationId: String): AppResult<Unit> {
        if (notificationId.isBlank()) return AppResult.Error(AppError.Validation("Geçersiz bildirim."))
        return try {
            ref.document(notificationId).update("read", true).await()
            AppResult.Success(Unit)
        } catch (e: Exception) {
            AppResult.Error(AppError.Database("Bildirim okunamadı.", e))
        }
    }

    override suspend fun markAllRead(uid: String): AppResult<Unit> {
        if (uid.isBlank()) return AppResult.Error(AppError.Auth("Giriş yapmalısın."))
        return try {
            val snapshot = ref.whereEqualTo("recipientId", uid).limit(100).get().await()
            val batch = firestore.batch()
            snapshot.documents.filter { it.getBoolean("read") != true }.forEach {
                batch.update(it.reference, "read", true)
            }
            batch.commit().await()
            AppResult.Success(Unit)
        } catch (e: Exception) {
            AppResult.Error(AppError.Database("Bildirimler okunamadı.", e))
        }
    }
}
