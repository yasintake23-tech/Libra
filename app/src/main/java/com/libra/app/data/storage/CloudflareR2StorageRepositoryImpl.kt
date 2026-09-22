package com.libra.app.data.storage

import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.libra.app.R
import com.libra.app.core.result.AppError
import com.libra.app.core.result.AppResult
import com.libra.app.domain.model.StorageUploadRequest
import com.libra.app.domain.repository.StorageRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.util.UUID

class CloudflareR2StorageRepositoryImpl(
    private val endpointOverride: String = ""
) : StorageRepository {

    private val endpoint: String by lazy {
        endpointOverride.ifBlank {
            runCatching {
                FirebaseApp.getInstance()
                    .applicationContext
                    .getString(R.string.r2_upload_endpoint)
            }.getOrDefault("")
        }.trimEnd('/')
    }

    val isConfigured: Boolean
        get() = endpoint.isNotBlank()

    override fun uploadMedia(request: StorageUploadRequest): Flow<AppResult<String>> = flow {
        if (!isConfigured) {
            emit(AppResult.Error(AppError.Storage("Cloudflare R2 upload endpointi henüz yapılandırılmadı.")))
            return@flow
        }

        if (request.bytes.isEmpty()) {
            emit(AppResult.Error(AppError.Storage("Yüklenecek dosya boş.")))
            return@flow
        }

        try {
            val user = FirebaseAuth.getInstance().currentUser
                ?: run {
                    emit(AppResult.Error(AppError.Auth("Medya yüklemek için giriş yapmalısın.")))
                    return@flow
                }

            val idToken = user.getIdToken(false).await().token
                ?: run {
                    emit(AppResult.Error(AppError.Auth("Kimlik doğrulama jetonu alınamadı.")))
                    return@flow
                }

            val safeFileName = request.fileName.substringAfterLast('/').ifBlank { "file" }
            val targetDirectory = request.targetDirectory.trim('/').ifBlank { "uploads" }
            val objectKey =
                targetDirectory + "/" + UUID.randomUUID() + "-" + safeFileName

            val response = withContext(Dispatchers.IO) {
                putBytes(
                    url = endpoint + "/upload?path=" +
                        URLEncoder.encode(objectKey, "UTF-8"),
                    idToken = idToken,
                    bytes = request.bytes,
                    contentType = request.contentType.ifBlank {
                        "application/octet-stream"
                    }
                )
            }

            if (response.code !in 200..299) {
                throw IllegalStateException(
                    response.body.ifBlank { "R2 upload başarısız." }
                )
            }

            val json = JSONObject(response.body)
            val url = json.optString("url").ifBlank {
                getPublicCdnUrl(objectKey)
            }

            emit(AppResult.Success(url))
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
        }
    }

    override suspend fun deleteMedia(fileKey: String): AppResult<Unit> {
        if (!isConfigured) {
            return AppResult.Error(AppError.Storage("Cloudflare R2 endpointi yapılandırılmadı."))
        }

        return try {
            val user = FirebaseAuth.getInstance().currentUser
                ?: return AppResult.Error(AppError.Auth("Oturum bulunamadı."))

            val idToken = user.getIdToken(false).await().token
                ?: return AppResult.Error(AppError.Auth("Kimlik doğrulama jetonu alınamadı."))

            val key = fileKey.substringAfter("/media/").trim('/')
            val response = withContext(Dispatchers.IO) {
                request(
                    method = "DELETE",
                    url = endpoint + "/delete?path=" +
                        URLEncoder.encode(key, "UTF-8"),
                    idToken = idToken
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

        return endpoint + "/media/" + fileKey.trim('/')
            .split('/')
            .joinToString("/") { URLEncoder.encode(it, "UTF-8") }
    }

    private fun putBytes(
        url: String,
        idToken: String,
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
            connection.setRequestProperty("Authorization", "Bearer $idToken")
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
        idToken: String
    ): HttpResponse {
        val connection = URL(url).openConnection() as HttpURLConnection
        return try {
            connection.requestMethod = method
            connection.connectTimeout = 20_000
            connection.readTimeout = 30_000
            connection.setRequestProperty("Authorization", "Bearer $idToken")
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
