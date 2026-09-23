package com.libra.app.data.storage

import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageMetadata
import com.libra.app.core.result.AppError
import com.libra.app.core.result.AppResult
import com.libra.app.domain.model.StorageUploadRequest
import com.libra.app.domain.repository.StorageRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.tasks.await

class FirebaseStorageRepositoryImpl : StorageRepository {

    private val storage: FirebaseStorage by lazy {
        FirebaseStorage.getInstance("gs://libra-3bfb9.firebasestorage.app")
    }

    override fun uploadMedia(
        request: StorageUploadRequest
    ): Flow<AppResult<String>> = flow {
        try {
            if (request.bytes.isEmpty()) {
                emit(AppResult.Error(AppError.Storage("Yüklenecek dosya boş.")))
                return@flow
            }
            if (request.bytes.size > 8 * 1024 * 1024) {
                emit(AppResult.Error(AppError.Storage("Dosya 8 MB'dan büyük olamaz.")))
                return@flow
            }

            val directory = request.targetDirectory.trim('/').ifBlank { "uploads" }
            val fileName = request.fileName.substringAfterLast('/').trim().ifBlank { "file" }
            val path = "$directory/$fileName"
            val contentType = request.contentType.ifBlank { "application/octet-stream" }
            val metadata = StorageMetadata.Builder()
                .setContentType(contentType)
                .build()

            val reference = storage.reference.child(path)
            reference.putBytes(request.bytes, metadata).await()

            // Firebase's Android SDK returns a shareable download URL only after
            // the upload task has completed. Use the exact same reference so the
            // URL cannot accidentally point at a different object.
            val downloadUrl = reference.downloadUrl.await().toString()
            emit(AppResult.Success(downloadUrl))
        } catch (e: Exception) {
            emit(AppResult.Error(AppError.Storage(
                "Dosya yüklenemedi: " +
                    (e.localizedMessage ?: "Bilinmeyen hata."),
                e
            )))
        }
    }

    override suspend fun deleteMedia(fileKey: String): AppResult<Unit> {
        return try {
            storage.reference.child(fileKey.trim('/')).delete().await()
            AppResult.Success(Unit)
        } catch (e: Exception) {
            AppResult.Error(AppError.Storage("Dosya silinemedi.", e))
        }
    }

    override fun getPublicCdnUrl(fileKey: String): String = fileKey
}