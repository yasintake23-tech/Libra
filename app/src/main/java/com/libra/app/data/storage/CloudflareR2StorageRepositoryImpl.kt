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
import java.util.Date
import java.util.UUID

class CloudflareR2StorageRepositoryImpl(
    private val context: Context
) : StorageRepository {

    private val config: CloudflareR2StorageConfig
        get() = CloudflareR2StorageConfig

    val isConfigured: Boolean
        get() = config.isConfigured(context)

    override fun uploadMedia(
        request: StorageUploadRequest,
        onProgress: (Int) -> Unit
    ): Flow<AppResult<String>> = flow {
        if (!isConfigured) {
            emit(
                AppResult.Error(
                    AppError.Storage("Cloudflare R2 S3 ve public URL yapılandırması eksik.")
                )
            )
            return@flow
        }
        if (request.bytes.isEmpty()) {
            emit(AppResult.Error(AppError.Storage("Yüklenecek dosya boş.")))
            return@flow
        }
        if (request.bytes.size > MAX_UPLOAD_BYTES) {
            emit(AppResult.Error(AppError.Storage("Dosya 8 MB'dan büyük olamaz.")))
            return@flow
        }

        val userId = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid
            ?: run {
                emit(AppResult.Error(AppError.Auth("Medya yüklemek için giriş yapmalısın.")))
                return@flow
            }

        val targetDirectory = request.targetDirectory.trim('/').ifBlank { "users/" + userId }
        val ownerDirectory = "users/" + userId
        if (targetDirectory != ownerDirectory) {
            emit(
                AppResult.Error(
                    AppError.Storage("R2 hedef klasörü giriş yapan kullanıcıya ait olmalı.")
                )
            )
            return@flow
        }

        val safeFileName = request.fileName
            .substringAfterLast('/')
            .trim()
            .ifBlank { "file" }
        val objectKey = targetDirectory + "/" + UUID.randomUUID() + "-" + safeFileName

        try {
            withContext(Dispatchers.IO) {
                val s3 = createClient()
                try {
                    // AWS Android SDK v1 can still fall back to aws-chunked streaming
                    // for PutObject on some Android builds. R2 rejects that payload mode.
                    // Generate a normal presigned PUT URL with SigV4, then upload the
                    // fixed-length bytes through HttpURLConnection.
                    val expiration = Date(System.currentTimeMillis() + 15L * 60L * 1000L)
                    val presignedUrl = s3.generatePresignedUrl(
                        config.bucketName(context),
                        objectKey,
                        expiration,
                        HttpMethod.PUT
                    )

                    val connection = (presignedUrl.openConnection() as HttpURLConnection).apply {
                        requestMethod = "PUT"
                        doOutput = true
                        doInput = true
                        useCaches = false
                        connectTimeout = 30_000
                        readTimeout = 30_000
                        setFixedLengthStreamingMode(request.bytes.size)
                        setRequestProperty(
                            "Content-Type",
                            request.contentType.ifBlank { "application/octet-stream" }
                        )
                    }

                    try {
                        connection.connect()
                        connection.outputStream.use { output ->
                            val chunkSize = 64 * 1024
                            var offset = 0
                            while (offset < request.bytes.size) {
                                val count = minOf(chunkSize, request.bytes.size - offset)
                                output.write(request.bytes, offset, count)
                                offset += count
                                onProgress((offset * 100 / request.bytes.size).coerceIn(1, 100))
                            }
                            output.flush()
                        }

                        val responseCode = connection.responseCode
                        if (responseCode !in 200..299) {
                            val responseBody = runCatching {
                                connection.errorStream?.bufferedReader()?.use { it.readText() }
                            }.getOrNull().orEmpty().take(400)
                            throw IllegalStateException(
                                "R2 HTTP $responseCode" +
                                    responseBody.takeIf { it.isNotBlank() }?.let { ": $it" }.orEmpty()
                            )
                        }
                    } finally {
                        connection.disconnect()
                    }
                } finally {
                    s3.shutdown()
                }
                onProgress(100)
            }

            emit(AppResult.Success(objectKey))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            emit(
                AppResult.Error(
                    AppError.Storage(
                        "R2'ye doğrudan yükleme başarısız: " +
                            (e.localizedMessage ?: "Bilinmeyen hata."),
                        e
                    )
                )
            )
        }
    }

    override suspend fun deleteMedia(fileKey: String): AppResult<Unit> =
        withContext(Dispatchers.IO) {
            if (!isConfigured) {
                return@withContext AppResult.Error(
                    AppError.Storage("Cloudflare R2 S3 ve public URL yapılandırması eksik.")
                )
            }

            val key = config.objectKeyFromValue(context, fileKey)
                ?: return@withContext AppResult.Error(
                    AppError.Storage("R2 nesne yolu çözümlenemedi.")
                )

            try {
                val s3 = createClient()
                try {
                    s3.deleteObject(
                        DeleteObjectRequest(config.bucketName(context), key)
                    )
                } finally {
                    s3.shutdown()
                }
                AppResult.Success(Unit)
            } catch (e: Exception) {
                AppResult.Error(AppError.Storage("R2 medyası silinemedi.", e))
            }
        }

    override fun getPublicCdnUrl(fileKey: String): String {
        val raw = fileKey.trim()
        if (raw.isBlank()) return fileKey

        val publicBase = config.publicBaseUrl(context)
        if (publicBase.isNotBlank() && (raw == publicBase || raw.startsWith(publicBase + "/"))) {
            return raw
        }

        val key = config.objectKeyFromValue(context, raw)
        if (key == null) return raw
        if (publicBase.isBlank()) return raw

        return publicBase + "/" + key
            .split("/")
            .joinToString("/") { Uri.encode(it) }
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
            client.endpoint = config.endpoint(context)
            // R2 rejects the AWS SDK v1 streaming payload signature.
            // Use the non-streaming S3 SigV4 mode explicitly.
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

    private companion object {
        const val MAX_UPLOAD_BYTES = 8 * 1024 * 1024
    }
}
