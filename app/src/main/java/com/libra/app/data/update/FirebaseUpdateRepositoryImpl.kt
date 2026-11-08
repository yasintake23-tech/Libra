package com.libra.app.data.update

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Source
import com.libra.app.core.result.AppError
import com.libra.app.core.result.AppResult
import com.libra.app.domain.model.AppUpdate
import com.libra.app.domain.repository.UpdateRepository
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class FirebaseUpdateRepositoryImpl : UpdateRepository {
    private val firestore = FirebaseFirestore.getInstance()
    private val activeRelease = firestore.collection("appUpdates").document("activeRelease")

    override fun observeActiveRelease(): Flow<AppResult<AppUpdate?>> = callbackFlow {
        val registration = activeRelease.addSnapshotListener { snapshot, error ->
            if (error != null) {
                trySend(AppResult.Error(AppError.Database("Güncelleme bilgisi okunamadı.", error)))
                return@addSnapshotListener
            }

            val release = snapshot?.takeIf { it.exists() }
                ?.toObject(AppUpdate::class.java)

            trySend(AppResult.Success(release))
        }
        awaitClose { registration.remove() }
    }

    override suspend fun getActiveRelease(): AppResult<AppUpdate?> = try {
        val snapshot = activeRelease.get(Source.SERVER).await()
        AppResult.Success(
            snapshot.takeIf { it.exists() }?.toObject(AppUpdate::class.java)
        )
    } catch (e: Exception) {
        AppResult.Error(AppError.Database("Güncelleme bilgisi okunamadı.", e))
    }

    override suspend fun publishRelease(release: AppUpdate): AppResult<Unit> = try {
        activeRelease.set(release).await()
        AppResult.Success(Unit)
    } catch (e: Exception) {
        AppResult.Error(AppError.Database("Güncelleme bilgisi yayınlanamadı.", e))
    }
}
