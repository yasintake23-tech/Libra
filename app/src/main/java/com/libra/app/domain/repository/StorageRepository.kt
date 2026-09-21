package com.libra.app.domain.repository

import com.libra.app.core.result.AppResult
import com.libra.app.domain.model.StorageUploadRequest
import kotlinx.coroutines.flow.Flow

interface StorageRepository {
    fun uploadMedia(request: StorageUploadRequest): Flow<AppResult<String>>
    suspend fun deleteMedia(fileKey: String): AppResult<Unit>
    fun getPublicCdnUrl(fileKey: String): String
}