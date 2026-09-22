package com.libra.app.data.storage

import android.net.Uri
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageMetadata
import com.libra.app.core.result.AppError
import com.libra.app.core.result.AppResult
import com.libra.app.domain.model.StorageUploadRequest
import com.libra.app.domain.repository.StorageRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.tasks.await
import java.util.UUID

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
            val token = UUID.randomUUID().toString()

            val metadata = StorageMetadata.Builder()
                .setContentType(contentType)
                .setCustomMetadata("firebaseStorageDownloadTokens", token)
                .build()

            val reference = storage.reference.child(path)
            reference.putBytes(request.bytes, metadata).await()

            val bucket = storage.app.options.storageBucket?.trim()
                ?: "libra-3bfb9.firebasestorage.app"
            val encodedPath = Uri.encode(path, "/")
            val downloadUrl =
                "https://firebasestorage.googleapis.com/v0/b/${Uri.encode(bucket)}/o/$encodedPath?alt=media&token=$token"

            var lastError: Exception? = null
            repeat(3) { attempt ->
                try {
                    reference.metadata.await()
                    emit(AppResult.Success(downloadUrl))
                    return@flow
                } catch (e: Exception) {
                    lastError = e
                    if (attempt < 2) delay(500L * (attempt + 1))
                }
            }

            emit(AppResult.Error(AppError.Storage(
                "Dosya yüklendi ancak doğrulanamadı: " +
                    (lastError?.localizedMessage ?: "Bilinmeyen hata."),
                lastError
            )))
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