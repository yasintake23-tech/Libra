package com.libra.app.data.storage

import com.libra.app.core.result.AppResult
import com.libra.app.domain.model.StorageUploadRequest
import com.libra.app.domain.repository.StorageRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.first

class HybridStorageRepositoryImpl(
    private val r2: CloudflareR2StorageRepositoryImpl,
    private val fallback: StorageRepository
) : StorageRepository {
    override fun uploadMedia(request: StorageUploadRequest, onProgress: (Int) -> Unit): Flow<AppResult<String>> = flow {
        if (!r2.isConfigured) {
            emit(fallback.uploadMedia(request, onProgress).first())
            return@flow
        }
        when (val r2Result = r2.uploadMedia(request, onProgress).first()) {
            is AppResult.Success -> emit(r2Result)
            is AppResult.Error -> {
                when (val fallbackResult = fallback.uploadMedia(request, onProgress).first()) {
                    is AppResult.Success -> emit(fallbackResult)
                    is AppResult.Error -> emit(
                        AppResult.Error(
                            com.libra.app.core.result.AppError.Storage(
                                "R2 yüklemesi başarısız: ${r2Result.error.message}. " +
                                    "Firebase Storage fallback de başarısız: ${fallbackResult.error.message}",
                                fallbackResult.error
                            )
                        )
                    )
                }
            }
        }
    }

    override suspend fun deleteMedia(fileKey: String): AppResult<Unit> {
        return if (r2.isConfigured) {
            when (val result = r2.deleteMedia(fileKey)) {
                is AppResult.Success -> result
                is AppResult.Error -> fallback.deleteMedia(fileKey)
            }
        } else fallback.deleteMedia(fileKey)
    }

    override fun getPublicCdnUrl(fileKey: String): String =
        if (r2.isConfigured) r2.getPublicCdnUrl(fileKey) else fallback.getPublicCdnUrl(fileKey)
}
