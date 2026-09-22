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
            config.uploadEndpoint(context).isNotBlank()
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
            emit(AppResult.Error(AppError.Storage("R2 hedef klasörü giriş yapan kullanıcıya ait olmalı.")))
            return@flow
        }

        val objectKey = targetDirectory + "/" + UUID.randomUUID().toString() + "-" + safeFileName
        val idToken = try {
            user.getIdToken(false).await().token
        } catch (e: Exception) {
            emit(AppResult.Error(AppError.Auth("Medya yükleme oturumu doğrulanamadı.", e)))
            return@flow
        }

        if (idToken.isNullOrBlank()) {
            emit(AppResult.Error(AppError.Auth("Firebase oturum anahtarı alınamadı.")))
            return@flow
        }

        val response = try {
            withContext(Dispatchers.IO) {
                putBytes(
                    url = config.uploadEndpoint(context) + "/upload?path=" +
                        java.net.URLEncoder.encode(objectKey, "UTF-8").replace("+", "%20"),
                    bearerToken = idToken,
                    bytes = request.bytes,
                    contentType = request.contentType.ifBlank { "application/octet-stream" }
                )
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            emit(AppResult.Error(AppError.Storage(
                "Medya yüklenemedi: " + (e.localizedMessage ?: "Bilinmeyen hata."), e
            )))
            return@flow
        }

        if (response.code !in 200..299) {
            emit(AppResult.Error(AppError.Storage(
                "Medya yüklenemedi: " + response.body.ifBlank { "Sunucu yüklemeyi reddetti." }
            )))
            return@flow
        }

        val publicUrl = runCatching { JSONObject(response.body).optString("url") }.getOrNull().orEmpty()
        if (publicUrl.isBlank()) {
            emit(AppResult.Error(AppError.Storage("Yükleme tamamlandı ancak herkese açık medya adresi alınamadı.")))
            return@flow
        }

        emit(AppResult.Success(publicUrl))
    }

    override suspend fun deleteMedia(fileKey: String): AppResult<Unit> {
        if (!isConfigured) {
            return AppResult.Error(AppError.Storage("Cloudflare R2 Worker yapılandırması eksik."))
        }

        return try {
            val user = FirebaseAuth.getInstance().currentUser
                ?: return AppResult.Error(AppError.Auth("Medya silmek için giriş yapmalısın."))
            val idToken = user.getIdToken(false).await().token
                ?: return AppResult.Error(AppError.Auth("Firebase oturum anahtarı alınamadı."))

            val key = extractObjectKey(fileKey)
                ?: return AppResult.Error(AppError.Storage("Medya yolu çözümlenemedi."))

            val response = withContext(Dispatchers.IO) {
                request(
                    method = "DELETE",
                    url = config.uploadEndpoint(context) + "/delete?path=" +
                        java.net.URLEncoder.encode(key, "UTF-8").replace("+", "%20"),
                    bearerToken = idToken
                )
            }

            if (response.code in 200..299) AppResult.Success(Unit)
            else AppResult.Error(AppError.Storage(response.body.ifBlank { "Medya silinemedi." }))
        } catch (e: Exception) {
            AppResult.Error(AppError.Storage("Medya silinemedi.", e))
        }
    }

    override fun getPublicCdnUrl(fileKey: String): String {
        if (fileKey.startsWith("http://") || fileKey.startsWith("https://")) {
            val key = extractObjectKey(fileKey)
            if (key != null && isConfigured) {
                return config.uploadEndpoint(context) + "/media/" +
                    key.split("/").joinToString("/") { android.net.Uri.encode(it) }
            }
            return fileKey
        }

        if (!isConfigured) return fileKey

        return config.uploadEndpoint(context) + "/media/" +
            fileKey.trim('/').split("/").joinToString("/") { android.net.Uri.encode(it) }
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
        bearerToken: String,
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
            connection.setRequestProperty("Authorization", "Bearer " + bearerToken)
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
