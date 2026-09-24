package com.libra.app.data.storage

import android.content.Context
import android.net.Uri
import com.amazonaws.HttpMethod
import com.amazonaws.auth.BasicAWSCredentials
import com.amazonaws.regions.Region
import com.amazonaws.regions.Regions
import com.amazonaws.services.s3.AmazonS3Client
import com.amazonaws.services.s3.S3ClientOptions
import com.amazonaws.services.s3.model.DeleteObjectRequest
import com.libra.app.core.result.AppError
import com.libra.app.core.result.AppResult
import com.libra.app.domain.model.StorageUploadRequest
import com.libra.app.domain.repository.StorageRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL
import java.util.Date
import java.util.concurrent.ConcurrentHashMap
import java.util.UUID

/**
 * The only media backend used by Libra.
 *
 * Firebase stores the object key, while R2 serves the public object URL.
 * Upload/delete authentication uses the R2 S3-compatible API.
 */
class CloudflareR2StorageRepositoryImpl(
    private val context: Context
) : StorageRepository {

    private val config: CloudflareR2StorageConfig
        get() = CloudflareR2StorageConfig

    override fun uploadMedia(
        request: StorageUploadRequest,
        onProgress: (Int) -> Unit
    ): Flow<AppResult<String>> = flow {
        val validation = validateRequest(request)
        if (validation != null) {
            emit(AppResult.Error(AppError.Storage(validation)))
            return@flow
        }

        val uid = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid
        if (uid.isNullOrBlank()) {
            emit(AppResult.Error(AppError.Auth("Medya yüklemek için giriş yapmalısın.")))
            return@flow
        }

        val ownerDirectory = "users/" + uid
        val targetDirectory = request.targetDirectory.trim('/').ifBlank { ownerDirectory }
        if (targetDirectory != ownerDirectory) {
            emit(AppResult.Error(AppError.Storage("R2 hedef klasörü giriş yapan kullanıcıya ait olmalı.")))
            return@flow
        }

        val fileName = sanitizeFileName(request.fileName)
        val objectKey = ownerDirectory + "/" + UUID.randomUUID() + "-" + fileName

        try {
            withContext(Dispatchers.IO) {
                onProgress(0)
                uploadWithRetry(
                    objectKey = objectKey,
                    bytes = request.bytes,
                    contentType = request.contentType.ifBlank { "application/octet-stream" },
                    onProgress = onProgress
                )
                onProgress(100)
            }
            emit(AppResult.Success(objectKey))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            emit(
                AppResult.Error(
                    AppError.Storage(
                        "R2'ye yükleme başarısız: " +
                            (e.localizedMessage ?: "Bilinmeyen hata."),
                        e
                    )
                )
            )
        }
    }

    override suspend fun deleteMedia(fileKey: String): AppResult<Unit> =
        withContext(Dispatchers.IO) {
            if (!config.isConfigured(context)) {
                return@withContext AppResult.Error(
                    AppError.Storage("Cloudflare R2 yapılandırması eksik.")
                )
            }

            val key = config.objectKeyFromValue(context, fileKey)
                ?: return@withContext AppResult.Error(
                    AppError.Storage("R2 nesne yolu çözümlenemedi.")
                )

            try {
                createClient().useS3 { client ->
                    client.deleteObject(
                        DeleteObjectRequest(config.bucketName(context), key)
                    )
                }
                AppResult.Success(Unit)
            } catch (e: Exception) {
                AppResult.Error(AppError.Storage("R2 medyası silinemedi.", e))
            }
        }

    override fun getPublicCdnUrl(fileKey: String): String {
        val raw = fileKey.trim()
        if (raw.isBlank()) return raw

        val key = config.objectKeyFromValue(context, raw)
        if (key != null && config.isConfigured(context)) {
            // Do not depend on the Cloudflare-managed r2.dev development URL
            // for app media. It is intentionally rate-limited and lacks the
            // caching controls available on a custom domain. A short-lived
            // signed GET also makes image access consistent across Android
            // devices and mobile networks.
            return cachedPresignedGetUrl(key) ?: publicUrlForKey(key)
        }

        val publicBase = config.publicBaseUrl(context)
        if (raw.startsWith(publicBase + "/")) return raw

        // Preserve public custom-domain URLs from older data.
        if (raw.startsWith("https://") || raw.startsWith("http://")) return raw
        return raw
    }

    private fun cachedPresignedGetUrl(key: String): String? {
        val now = System.currentTimeMillis()
        signedUrlCache[key]?.let { cached ->
            if (cached.expiresAt > now) return cached.url
            signedUrlCache.remove(key, cached)
        }

        return runCatching {
            val expiresAt = now + SIGNED_GET_CACHE_MS
            val url = createClient().useS3 { client ->
                client.generatePresignedUrl(
                    config.bucketName(context),
                    key,
                    Date(expiresAt),
                    HttpMethod.GET
                ).toString()
            }
            signedUrlCache[key] = CachedSignedUrl(url, expiresAt - SIGNED_URL_SAFETY_MS)
            url
        }.getOrNull()
    }

    private fun publicUrlForKey(key: String): String =
        config.publicBaseUrl(context) + "/" +
            key.split("/").joinToString("/") { Uri.encode(it) }

    private fun validateRequest(request: StorageUploadRequest): String? {
        if (!config.isConfigured(context)) return "Cloudflare R2 yapılandırması eksik."
        if (request.bytes.isEmpty()) return "Yüklenecek dosya boş."
        if (request.bytes.size > MAX_UPLOAD_BYTES) return "Dosya 8 MB'dan büyük olamaz."
        if (request.fileName.isBlank()) return "Dosya adı boş olamaz."
        if (request.contentType.isBlank()) return "Dosya türü belirlenemedi."
        return null
    }

    private fun sanitizeFileName(value: String): String {
        val cleaned = value
            .substringAfterLast('/')
            .substringAfterLast('\\')
            .replace(Regex("[\\/\\u0000-\\u001F]"), "_")
            .trim()

        return cleaned
            .ifBlank { "file" }
            .take(120)
    }

    private fun uploadWithRetry(
        objectKey: String,
        bytes: ByteArray,
        contentType: String,
        onProgress: (Int) -> Unit
    ) {
        var lastError: Exception? = null

        repeat(MAX_ATTEMPTS) { attempt ->
            try {
                val expiration = Date(System.currentTimeMillis() + PRESIGNED_URL_LIFETIME_MS)
                val presignedUrl = createClient().useS3 { client ->
                    client.generatePresignedUrl(
                        config.bucketName(context),
                        objectKey,
                        expiration,
                        HttpMethod.PUT
                    )
                }

                putFixedLength(presignedUrl, bytes, contentType, onProgress)
                return
            } catch (e: Exception) {
                lastError = e
                if (attempt < MAX_ATTEMPTS - 1) {
                    Thread.sleep(RETRY_DELAYS_MS[attempt])
                }
            }
        }

        throw lastError ?: IllegalStateException("R2 yükleme başarısız.")
    }

    private fun putFixedLength(
        url: URL,
        bytes: ByteArray,
        contentType: String,
        onProgress: (Int) -> Unit
    ) {
        val connection = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "PUT"
            doOutput = true
            doInput = true
            useCaches = false
            instanceFollowRedirects = true
            connectTimeout = HTTP_TIMEOUT_MS
            readTimeout = HTTP_TIMEOUT_MS
            setFixedLengthStreamingMode(bytes.size)
            setRequestProperty("Content-Type", contentType)
        }

        try {
            connection.connect()

            connection.outputStream.use { output ->
                val bufferSize = 64 * 1024
                var offset = 0

                while (offset < bytes.size) {
                    val count = minOf(bufferSize, bytes.size - offset)
                    output.write(bytes, offset, count)
                    offset += count
                    onProgress((offset * 100 / bytes.size).coerceIn(1, 99))
                }
                output.flush()
            }

            val code = connection.responseCode
            if (code !in 200..299) {
                val body = runCatching {
                    connection.errorStream?.bufferedReader()?.use { it.readText() }
                }.getOrNull().orEmpty().take(500)

                throw R2UploadException(
                    code,
                    body.ifBlank { "HTTP " + code }
                )
            }
        } finally {
            connection.disconnect()
        }
    }

    private fun createClient(): AmazonS3Client {
        val credentials = BasicAWSCredentials(
            config.accessKeyId(context),
            config.secretAccessKey(context)
        )

        return AmazonS3Client(
            credentials,
            Region.getRegion(Regions.US_EAST_1)
        ).also { client ->
            // R2 uses the "auto" signing region. The exact jurisdictional
            // endpoint comes from app configuration.
            client.endpoint = config.endpoint(context)
            client.setSignerRegionOverride("auto")
            client.setS3ClientOptions(
                S3ClientOptions.builder()
                    .setPathStyleAccess(true)
                    .disableChunkedEncoding()
                    .setPayloadSigningEnabled(false)
                    .build()
            )
        }
    }

    private inline fun <T> AmazonS3Client.useS3(block: (AmazonS3Client) -> T): T {
        try {
            return block(this)
        } finally {
            shutdown()
        }
    }

    private class R2UploadException(
        val statusCode: Int,
        message: String
    ) : IllegalStateException("R2 HTTP " + statusCode + ": " + message)

    private data class CachedSignedUrl(val url: String, val expiresAt: Long)

    private val signedUrlCache = ConcurrentHashMap<String, CachedSignedUrl>()

    private companion object {
        const val MAX_UPLOAD_BYTES = 8 * 1024 * 1024
        const val MAX_ATTEMPTS = 3
        const val HTTP_TIMEOUT_MS = 30_000
        const val PRESIGNED_URL_LIFETIME_MS = 10L * 60L * 1000L
        const val SIGNED_GET_CACHE_MS = 8L * 60L * 1000L
        const val SIGNED_URL_SAFETY_MS = 30_000L
        val RETRY_DELAYS_MS = longArrayOf(750L, 1750L)
    }
}
