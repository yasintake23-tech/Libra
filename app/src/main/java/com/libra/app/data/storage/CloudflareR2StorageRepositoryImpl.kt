package com.libra.app.data.storage

import android.content.Context
import com.amazonaws.HttpMethodName
import com.amazonaws.auth.BasicAWSCredentials
import com.amazonaws.regions.Region
import com.amazonaws.regions.Regions
import com.amazonaws.services.s3.AmazonS3Client
import com.amazonaws.services.s3.S3ClientOptions
import com.amazonaws.services.s3.model.DeleteObjectRequest
import com.amazonaws.services.s3.model.GeneratePresignedUrlRequest
import com.amazonaws.services.s3.model.ObjectMetadata
import com.amazonaws.services.s3.model.PutObjectRequest
import com.libra.app.core.result.AppError
import com.libra.app.core.result.AppResult
import com.libra.app.domain.model.StorageUploadRequest
import com.libra.app.domain.repository.StorageRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import java.io.ByteArrayInputStream
import java.net.URLEncoder
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
            emit(AppResult.Error(AppError.Storage("Cloudflare R2 S3 erişim bilgileri eksik.")))
            return@flow
        }
        if (request.bytes.isEmpty()) {
            emit(AppResult.Error(AppError.Storage("Yüklenecek dosya boş.")))
            return@flow
        }
        if (request.bytes.size > 8 * 1024 * 1024) {
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
            emit(AppResult.Error(AppError.Storage("R2 hedef klasörü giriş yapan kullanıcıya ait olmalı.")))
            return@flow
        }

        val safeFileName = request.fileName.substringAfterLast('/').trim().ifBlank { "file" }
        val objectKey = targetDirectory + "/" + UUID.randomUUID() + "-" + safeFileName

        try {
            withContext(Dispatchers.IO) {
                val s3 = createClient()
                val metadata = ObjectMetadata().apply {
                    contentType = request.contentType.ifBlank { "application/octet-stream" }
                    contentLength = request.bytes.size.toLong()
                    cacheControl = "public, max-age=31536000, immutable"
                }
                s3.putObject(
                    PutObjectRequest(
                        config.bucketName(context),
                        objectKey,
                        ByteArrayInputStream(request.bytes),
                        metadata
                    )
                )
                onProgress(100)
                s3.shutdown()
            }
            emit(AppResult.Success(objectKey))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            emit(AppResult.Error(AppError.Storage(
                "R2'ye doğrudan yükleme başarısız: " + (e.localizedMessage ?: "Bilinmeyen hata."),
                e
            )))
        }
    }

    override suspend fun deleteMedia(fileKey: String): AppResult<Unit> =
        withContext(Dispatchers.IO) {
            if (!isConfigured) {
                return@withContext AppResult.Error(AppError.Storage("Cloudflare R2 S3 erişim bilgileri eksik."))
            }
            val key = config.objectKeyFromValue(fileKey)
                ?: return@withContext AppResult.Error(AppError.Storage("R2 nesne yolu çözümlenemedi."))
            try {
                val s3 = createClient()
                s3.deleteObject(DeleteObjectRequest(config.bucketName(context), key))
                s3.shutdown()
                AppResult.Success(Unit)
            } catch (e: Exception) {
                AppResult.Error(AppError.Storage("R2 medyası silinemedi.", e))
            }
        }

    override fun getPublicCdnUrl(fileKey: String): String {
        val key = config.objectKeyFromValue(fileKey) ?: return fileKey
        val publicBase = config.publicBaseUrl(context)
        if (publicBase.isNotBlank()) {
            return publicBase + "/" + key.split("/").joinToString("/") {
                URLEncoder.encode(it, "UTF-8").replace("+", "%20")
            }
        }
        if (!isConfigured) return fileKey

        return try {
            val s3 = createClient()
            val expiry = Date(System.currentTimeMillis() + 6L * 24L * 60L * 60L * 1000L)
            val request = GeneratePresignedUrlRequest(config.bucketName(context), key)
                .withMethod(HttpMethodName.GET)
                .withExpiration(expiry)
            val url = s3.generatePresignedUrl(request)
            s3.shutdown()
            url.toString()
        } catch (_: Exception) {
            fileKey
        }
    }

    private fun createClient(): AmazonS3Client {
        val credentials = BasicAWSCredentials(
            config.accessKeyId(context),
            config.secretAccessKey(context)
        )
        val client = AmazonS3Client(credentials, Region.getRegion(Regions.US_EAST_1))
        client.endpoint = config.endpoint(context)
        client.s3ClientOptions = S3ClientOptions.builder()
            .setPathStyleAccess(true)
            .disableChunkedEncoding()
            .build()
        return client
    }
}
