package com.libra.app.data.storage

import com.libra.app.core.result.AppResult
import com.libra.app.domain.model.StorageUploadRequest
import com.libra.app.domain.repository.StorageRepository
import kotlinx.coroutines.flow.Flow

class HybridStorageRepositoryImpl(
    private val r2: CloudflareR2StorageRepositoryImpl,
    private val fallback: StorageRepository
) : StorageRepository {

    override fun uploadMedia(request: StorageUploadRequest): Flow<AppResult<String>> {
        return if (r2.isConfigured) r2.uploadMedia(request) else fallback.uploadMedia(request)
    }

    override suspend fun deleteMedia(fileKey: String): AppResult<Unit> {
        return if (r2.isConfigured) r2.deleteMedia(fileKey) else fallback.deleteMedia(fileKey)
    }

    override fun getPublicCdnUrl(fileKey: String): String {
        return if (r2.isConfigured) r2.getPublicCdnUrl(fileKey) else fallback.getPublicCdnUrl(fileKey)
    }
}
