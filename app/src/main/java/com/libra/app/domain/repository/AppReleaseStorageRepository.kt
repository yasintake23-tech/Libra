package com.libra.app.domain.repository

import com.libra.app.core.result.AppResult
import kotlinx.coroutines.flow.Flow

interface AppReleaseStorageRepository {
    fun uploadApk(
        fileName: String,
        bytes: ByteArray,
        onProgress: (Int) -> Unit = {}
    ): Flow<AppResult<String>>

    suspend fun deleteApk(objectKey: String): AppResult<Unit>

    suspend fun getSignedApkUrl(objectKey: String): String?
}
