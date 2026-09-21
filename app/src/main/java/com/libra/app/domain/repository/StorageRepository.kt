package com.libra.app.domain.repository

import com.libra.app.core.result.AppResult
import com.libra.app.domain.model.StorageUploadRequest
import kotlinx.coroutines.flow.Flow

/**
 * Storage Abstraction for Cloudflare R2 / S3-compatible asset storage.
 * Keeps UI and business logic independent of specific cloud storage implementations.
 */
interface StorageRepository {
    /**
     * Uploads media bytes (e.g. book covers, user avatars) to Cloudflare R2 bucket.
     * @return Public CDN URL of the uploaded asset.
     */
    fun uploadMedia(request: StorageUploadRequest): Flow<AppResult<String>>

    /**
     * Deletes a file from the bucket by key.
     */
    suspend fun deleteMedia(fileKey: String): AppResult<Unit>

    /**
     * Generates or formats public CDN URL for a given storage key.
     */
    fun getPublicCdnUrl(fileKey: String): String
}
