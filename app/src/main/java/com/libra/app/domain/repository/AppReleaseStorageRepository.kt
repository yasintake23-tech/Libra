package com.libra.app.domain.repository

import android.net.Uri
import com.libra.app.core.result.AppResult
import kotlinx.coroutines.flow.Flow

interface AppReleaseStorageRepository {
    suspend fun inspectApk(uri: Uri): AppResult<com.libra.app.domain.model.AppReleaseInfo>

    fun uploadApk(
        uri: Uri,
        fileName: String,
        fileSize: Long,
        onProgress: (Int) -> Unit = {}
    ): Flow<AppResult<String>>

    suspend fun deleteApk(objectKey: String): AppResult<Unit>

    suspend fun getSignedApkUrl(objectKey: String): String?

    suspend fun publishPublicRelease(release: com.libra.app.domain.model.AppUpdate): AppResult<Unit>

    suspend fun getPublicRelease(): AppResult<com.libra.app.domain.model.AppUpdate?>
}
