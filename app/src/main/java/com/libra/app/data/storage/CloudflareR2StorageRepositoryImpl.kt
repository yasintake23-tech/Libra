package com.libra.app.data.storage

import com.libra.app.core.result.AppError
import com.libra.app.core.result.AppResult
import com.libra.app.domain.model.StorageUploadRequest
import com.libra.app.domain.repository.StorageRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/**
 * R2 is intentionally accessed through a presigned-upload flow. Long-lived R2 secrets never belong in the APK.
 * Until a backend/presigned endpoint is configured this implementation returns an explicit configuration error.
 */
class CloudflareR2StorageRepositoryImpl(private val publicBaseUrl: String = "") : StorageRepository {
    override fun uploadMedia(request: StorageUploadRequest): Flow<AppResult<String>> = flow {
        if (publicBaseUrl.isBlank()) {
            emit(AppResult.Error(AppError.Storage("Cloudflare R2 presigned upload endpointi yapılandırılmadı.")))
            return@flow
        }
        emit(AppResult.Error(AppError.Storage("R2 upload istemcisi için presigned URL sağlayıcısı henüz bağlanmadı.")))
    }

    override suspend fun deleteMedia(fileKey: String): AppResult<Unit> = AppResult.Error(AppError.Storage("R2 silme işlemi için backend/presigned endpoint yapılandırılmadı."))

    override fun getPublicCdnUrl(fileKey: String): String {
        if (fileKey.startsWith("http://") || fileKey.startsWith("https://")) return fileKey
        return publicBaseUrl.trimEnd('/').let { base -> if (base.isBlank()) fileKey else "$base/${fileKey.trimStart('/')}" }
    }
}
