package com.libra.app.domain.repository

import android.net.Uri
import com.libra.app.core.result.AppResult
import kotlinx.coroutines.flow.Flow

interface AppReleaseStorageRepository {
    fun uploadApk(
        uri: Uri,
        fileName: String,
        fileSize: Long,
        onProgress: (Int) -> Unit = {}
    ): Flow<AppResult<String>>

    suspend fun deleteApk(objectKey: String): AppResult<Unit>

    suspend fun getSignedApkUrl(objectKey: String): String?
}
