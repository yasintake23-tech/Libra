package com.libra.app.data.storage

import android.content.Context
import android.net.Uri
import com.google.firebase.auth.FirebaseAuth
import com.libra.app.core.result.AppError
import com.libra.app.core.result.AppResult
import com.libra.app.domain.model.StorageUploadRequest
import com.libra.app.domain.repository.StorageRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.UUID

class CloudflareR2StorageRepositoryImpl(
    private val context: Context
) : StorageRepository {

    private val config: CloudflareR2StorageConfig
        get() = CloudflareR2StorageConfig

    val isConfigured: Boolean
        get() = runCatching {
            config.accountId(context).isNotBlank() &&
                config.bucketName(context).isNotBlank() &&
                config.apiToken(context).isNotBlank()
        }.getOrDefault(false)

    override fun uploadMedia(request: StorageUploadRequest): Flow<AppResult<String>> = flow {
        if (!isConfigured) {
            emit(AppResult.Error(AppError.Storage("Cloudflare R2 yapılandırması eksik.")))
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

        val user = FirebaseAuth.getInstance().currentUser
                ?: run {
                    emit(AppResult.Error(AppError.Auth("Medya yüklemek için giriş yapmalısın.")))
                    return@flow
                }

            val safeFileName = request.fileName
                .substringAfterLast('/')
                .trim()
                .ifBlank { "file" }

            val targetDirectory = request.targetDirectory
                .trim('/')
                .ifBlank { "uploads" }

            val ownerDirectory = "users/" + user.uid
            if (targetDirectory != ownerDirectory) {
                emit(
                    AppResult.Error(
                        AppError.Storage(
                            "R2 hedef klasörü giriş yapan kullanıcıya ait olmalı."
                        )
                    )
                )
                return@flow
            }

            val objectKey =
                targetDirectory + "/" + UUID.randomUUID().toString() + "-" + safeFileName

            val response = try {
                withContext(Dispatchers.IO) {
                    putBytes(
                        url = config.objectUrl(context, objectKey),
                        apiToken = config.apiToken(context),
                        bytes = request.bytes,
                        contentType = request.contentType.ifBlank { "application/octet-stream" }
                    )
                }
            } catch (e: CancellationException) {
                // Flow.first() başarılı sonucu aldıktan sonra upstream'i iptal eder.
                // Bu normal iptal durumunu R2 hatası olarak emit etmek Flow
                // transparency ihlaline ve uygulama çökmesine neden olur.
                throw e
            } catch (e: Exception) {
                emit(
                    AppResult.Error(
                        AppError.Storage(
                            "R2 dosya yüklenemedi: " +
                                (e.localizedMessage ?: "Bilinmeyen hata."),
                            e
                        )
                    )
                )
                return@flow
            }

            if (response.code !in 200..299) {
                val message = runCatching {
                    JSONObject(response.body)
                        .optJSONArray("errors")
                        ?.optJSONObject(0)
                        ?.optString("message")
                }.getOrNull().orEmpty()

                emit(
                    AppResult.Error(
                        AppError.Storage(
                            "R2 dosya yüklenemedi: " +
                                message.ifBlank {
                                    response.body.ifBlank { "R2 yükleme başarısız." }
                                }
                        )
                    )
                )
                return@flow
            }

            emit(AppResult.Success(config.objectUrl(context, objectKey)))
    }

    override suspend fun deleteMedia(fileKey: String): AppResult<Unit> {
        if (!isConfigured) {
            return AppResult.Error(AppError.Storage("Cloudflare R2 yapılandırması eksik."))
        }

        return try {
            val key = extractObjectKey(fileKey)
                ?: return AppResult.Error(AppError.Storage("R2 nesne yolu çözümlenemedi."))

            val response = withContext(Dispatchers.IO) {
                request(
                    method = "DELETE",
                    url = config.objectUrl(context, key),
                    apiToken = config.apiToken(context)
                )
            }

            if (response.code in 200..299) {
                AppResult.Success(Unit)
            } else {
                AppResult.Error(
                    AppError.Storage(
                        response.body.ifBlank { "R2 dosyası silinemedi." }
                    )
                )
            }
        } catch (e: Exception) {
            AppResult.Error(AppError.Storage("R2 dosyası silinemedi.", e))
        }
    }

    override fun getPublicCdnUrl(fileKey: String): String {
        if (fileKey.startsWith("http://") || fileKey.startsWith("https://")) {
            return fileKey
        }

        if (!isConfigured) return fileKey

        return config.objectUrl(context, fileKey)
    }

    private fun extractObjectKey(fileKey: String): String? {
        val value = fileKey.trim()
        if (value.isBlank()) return null

        return when {
            CloudflareR2StorageConfig.isObjectApiUrl(value) -> {
                value.substringAfter("/objects/", missingDelimiterValue = "")
                    .takeIf { it.isNotBlank() }
                    ?.let(Uri::decode)
            }

            "/media/" in value -> {
                value.substringAfter("/media/")
                    .trim('/')
                    .takeIf { it.isNotBlank() }
            }

            else -> value.trim('/')
                .takeIf { it.startsWith("users/") }
        }
    }

    private fun putBytes(
        url: String,
        apiToken: String,
        bytes: ByteArray,
        contentType: String
    ): HttpResponse {
        val connection = URL(url).openConnection() as HttpURLConnection
        return try {
            connection.requestMethod = "PUT"
            connection.doOutput = true
            connection.setFixedLengthStreamingMode(bytes.size)
            connection.connectTimeout = 20_000
            connection.readTimeout = 60_000
            connection.setRequestProperty("Authorization", "Bearer " + apiToken)
            connection.setRequestProperty("Content-Type", contentType)
            connection.setRequestProperty("Accept", "application/json")
            connection.outputStream.use { it.write(bytes) }

            HttpResponse(connection.responseCode, readResponse(connection))
        } finally {
            connection.disconnect()
        }
    }

    private fun request(
        method: String,
        url: String,
        apiToken: String
    ): HttpResponse {
        val connection = URL(url).openConnection() as HttpURLConnection
        return try {
            connection.requestMethod = method
            connection.connectTimeout = 20_000
            connection.readTimeout = 30_000
            connection.setRequestProperty("Authorization", "Bearer " + apiToken)
            connection.setRequestProperty("Accept", "application/json")
            HttpResponse(connection.responseCode, readResponse(connection))
        } finally {
            connection.disconnect()
        }
    }

    private fun readResponse(connection: HttpURLConnection): String {
        val stream = if (connection.responseCode in 200..299) {
            connection.inputStream
        } else {
            connection.errorStream ?: connection.inputStream
        }

        return stream.bufferedReader().use { it.readText() }
    }

    private data class HttpResponse(
        val code: Int,
        val body: String
    )
}
