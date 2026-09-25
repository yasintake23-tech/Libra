package com.libra.app.data.update

import android.content.Context
import android.net.Uri
import android.content.pm.PackageManager
import android.os.Build
import java.security.MessageDigest
import com.amazonaws.HttpMethod
import com.amazonaws.auth.BasicAWSCredentials
import com.amazonaws.regions.Region
import com.amazonaws.regions.Regions
import com.amazonaws.services.s3.AmazonS3Client
import com.amazonaws.services.s3.S3ClientOptions
import com.amazonaws.services.s3.model.DeleteObjectRequest
import com.libra.app.core.result.AppError
import com.libra.app.core.result.AppResult
import com.libra.app.data.storage.CloudflareR2StorageConfig
import com.libra.app.domain.repository.AppReleaseStorageRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.io.InputStream
import java.io.File
import java.io.FileOutputStream
import java.net.URL
import java.util.Date
import java.util.UUID
import org.json.JSONObject
import com.libra.app.domain.model.AppReleaseInfo
import com.libra.app.domain.model.AppUpdate

class CloudflareR2AppReleaseStorageRepositoryImpl(
    private val context: Context
) : AppReleaseStorageRepository {

    private val config get() = CloudflareR2StorageConfig

    override suspend fun inspectApk(uri: Uri): AppResult<AppReleaseInfo> =
        withContext(Dispatchers.IO) {
            if (!config.isConfigured(context)) {
                return@withContext AppResult.Error(AppError.Storage("Cloudflare R2 yapılandırması eksik."))
            }

            val validationDir = File(context.cacheDir, "updates-validation").apply { mkdirs() }
            val validationFile = File(validationDir, "validate-${UUID.randomUUID()}.apk")
            val pm = context.packageManager
            val flags = (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                PackageManager.GET_SIGNING_CERTIFICATES
            } else {
                PackageManager.GET_SIGNATURES
            }) or PackageManager.GET_META_DATA

            try {
                context.contentResolver.openInputStream(uri)?.use { input ->
                    FileOutputStream(validationFile).use { output ->
                        val buffer = ByteArray(64 * 1024)
                        var copied = 0L
                        while (true) {
                            val count = input.read(buffer)
                            if (count < 0) break
                            copied += count
                            if (copied > MAX_APK_BYTES) {
                                return@withContext AppResult.Error(AppError.Storage("APK dosyası 200 MB'dan büyük olamaz."))
                            }
                            output.write(buffer, 0, count)
                        }
                        output.flush()
                    }
                } ?: return@withContext AppResult.Error(AppError.Storage("Seçilen APK dosyası okunamadı."))

                val info = pm.getPackageArchiveInfo(validationFile.absolutePath, flags)
                    ?: return@withContext AppResult.Error(AppError.Storage("Seçilen dosya geçerli bir APK değil."))
                if (info.packageName != context.packageName) {
                    return@withContext AppResult.Error(AppError.Storage("Bu APK Libra uygulamasına ait değil."))
                }

                val releaseId = info.applicationInfo?.metaData
                    ?.getString(RELEASE_ID_META_DATA_KEY)
                    ?.trim()
                    .orEmpty()
                if (releaseId.isBlank() || releaseId == "rel_local") {
                    return@withContext AppResult.Error(
                        AppError.Storage("Bu APK'da otomatik release kimliği bulunamadı. Yeni build oluşturun.")
                    )
                }

                val installed = runCatching {
                    val pi = pm.getPackageInfo(context.packageName, flags)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                        pi.signingInfo?.apkContentsSigners?.map { sha256(it.toByteArray()) }?.toSet().orEmpty()
                    } else {
                        pi.signatures?.map { sha256(it.toByteArray()) }?.toSet().orEmpty()
                    }
                }.getOrElse {
                    return@withContext AppResult.Error(AppError.Storage("Mevcut Libra imza bilgisi okunamadı."))
                }

                val uploaded = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    info.signingInfo?.apkContentsSigners?.map { sha256(it.toByteArray()) }?.toSet().orEmpty()
                } else {
                    info.signatures?.map { sha256(it.toByteArray()) }?.toSet().orEmpty()
                }

                if (installed.isEmpty() || uploaded.isEmpty() || installed.intersect(uploaded).isEmpty()) {
                    return@withContext AppResult.Error(
                        AppError.Storage("APK, yüklü Libra sürümüyle aynı imzalama anahtarını kullanmıyor.")
                    )
                }

                val versionCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    info.longVersionCode
                } else {
                    info.versionCode.toLong()
                }
                if (versionCode <= 0L) {
                    return@withContext AppResult.Error(AppError.Storage("APK versionCode bilgisi geçersiz."))
                }

                AppResult.Success(
                    AppReleaseInfo(
                        releaseId = releaseId,
                        versionCode = versionCode,
                        versionName = info.versionName?.trim().orEmpty()
                    )
                )
            } catch (e: Exception) {
                AppResult.Error(AppError.Storage("APK doğrulanırken dosya okunamadı.", e))
            } finally {
                validationFile.delete()
            }
        }

    override fun uploadApk(
        uri: Uri,
        fileName: String,
        fileSize: Long,
        onProgress: (Int) -> Unit
    ): Flow<AppResult<String>> = flow {
        if (!config.isConfigured(context)) {
            emit(AppResult.Error(AppError.Storage("Cloudflare R2 yapılandırması eksik.")))
            return@flow
        }
        if (fileSize <= 0L) {
            emit(AppResult.Error(AppError.Storage("APK dosyası boş.")))
            return@flow
        }
        if (fileSize > MAX_APK_BYTES) {
            emit(AppResult.Error(AppError.Storage("APK dosyası 200 MB'dan büyük olamaz.")))
            return@flow
        }

        val safeName = fileName.substringAfterLast('/').substringAfterLast('\\')
            .replace(Regex("[\\\\/\\u0000-\\u001F]"), "_")
            .trim()
            .ifBlank { "Libra-release.apk" }
            .take(120)
        when (val validation = validateApk(uri)) {
            is ApkValidation.Invalid -> {
                emit(AppResult.Error(AppError.Storage(validation.message)))
                return@flow
            }
            ApkValidation.Valid -> Unit
        }

        val objectKey = "releases/" + UUID.randomUUID() + "-" + safeName

        try {
            withContext(Dispatchers.IO) {
                onProgress(0)
                uploadWithRetry(uri, objectKey, fileSize, onProgress)
                onProgress(100)
            }
            emit(AppResult.Success(objectKey))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            emit(AppResult.Error(AppError.Storage("APK R2'ye yüklenemedi.", e)))
        }
    }

    override suspend fun deleteApk(objectKey: String): AppResult<Unit> =
        withContext(Dispatchers.IO) {
            val key = objectKey.trim()
            if (!key.startsWith("releases/") || key.length <= "releases/".length) {
                return@withContext AppResult.Error(AppError.Storage("Geçersiz release APK yolu."))
            }
            try {
                createClient().useS3 { client ->
                    client.deleteObject(DeleteObjectRequest(config.bucketName(context), key))
                }
                AppResult.Success(Unit)
            } catch (e: Exception) {
                AppResult.Error(AppError.Storage("Eski APK silinemedi.", e))
            }
        }

    override suspend fun getSignedApkUrl(objectKey: String): String? =
        withContext(Dispatchers.IO) {
            val key = objectKey.trim()
            if (!key.startsWith("releases/")) return@withContext null
            runCatching {
                val expiration = Date(System.currentTimeMillis() + SIGNED_URL_LIFETIME_MS)
                createClient().useS3 { client ->
                    client.generatePresignedUrl(
                        config.bucketName(context),
                        key,
                        expiration,
                        HttpMethod.GET
                    ).toString()
                }
            }.getOrNull()
        }

    override suspend fun publishPublicRelease(release: AppUpdate): AppResult<Unit> =
        withContext(Dispatchers.IO) {
            try {
                val json = JSONObject()
                    .put("releaseId", release.releaseId)
                    .put("versionCode", release.versionCode)
                    .put("versionName", release.versionName)
                    .put("apkObjectKey", release.apkObjectKey)
                    .put("apkSize", release.apkSize)
                    .put("forceUpdate", release.forceUpdate)
                    .put("createdAt", release.createdAt)
                    .put("changelog", org.json.JSONArray().apply {
                        release.changelog.forEach { put(it) }
                    })
                    .toString()
                val expiration = Date(System.currentTimeMillis() + PRESIGNED_URL_LIFETIME_MS)
                val url = createClient().useS3 { client ->
                    client.generatePresignedUrl(config.bucketName(context), PUBLIC_RELEASE_OBJECT_KEY, expiration, HttpMethod.PUT)
                }
                val connection = (url.openConnection() as HttpURLConnection).apply {
                    requestMethod = "PUT"
                    doOutput = true
                    doInput = true
                    useCaches = false
                    instanceFollowRedirects = true
                    connectTimeout = HTTP_TIMEOUT_MS
                    readTimeout = HTTP_TIMEOUT_MS
                    setFixedLengthStreamingMode(json.toByteArray(Charsets.UTF_8).size.toLong())
                    setRequestProperty("Content-Type", "application/json; charset=utf-8")
                }
                try {
                    connection.connect()
                    connection.outputStream.use { it.write(json.toByteArray(Charsets.UTF_8)) }
                    val code = connection.responseCode
                    if (code !in 200..299) error("R2 HTTP ${code}")
                } finally {
                    connection.disconnect()
                }
                AppResult.Success(Unit)
            } catch (e: Exception) {
                AppResult.Error(AppError.Storage("Herkese açık güncelleme bilgisi yayınlanamadı.", e))
            }
        }

    override suspend fun getPublicRelease(): AppResult<AppUpdate?> =
        withContext(Dispatchers.IO) {
            val base = config.publicBaseUrl(context)
            if (!base.startsWith("https://")) {
                return@withContext AppResult.Error(AppError.Storage("R2 public URL yapılandırması eksik."))
            }
            val connection = try {
                (URL("$base/$PUBLIC_RELEASE_OBJECT_KEY").openConnection() as HttpURLConnection).apply {
                    requestMethod = "GET"
                    doInput = true
                    useCaches = false
                    instanceFollowRedirects = true
                    connectTimeout = 10_000
                    readTimeout = 15_000
                }
            } catch (e: Exception) {
                return@withContext AppResult.Error(AppError.Storage("Güncelleme bilgisi okunamadı.", e))
            }
            try {
                connection.connect()
                if (connection.responseCode == HttpURLConnection.HTTP_NOT_FOUND) return@withContext AppResult.Success(null)
                if (connection.responseCode !in 200..299) {
                    return@withContext AppResult.Error(AppError.Storage("Güncelleme bilgisi HTTP ${connection.responseCode}"))
                }
                val json = connection.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
                val obj = JSONObject(json)
                val changes = obj.optJSONArray("changelog")
                val changelog = buildList {
                    if (changes != null) {
                        for (index in 0 until changes.length()) {
                            changes.optString(index).takeIf { it.isNotBlank() }?.let(::add)
                        }
                    }
                }
                AppResult.Success(
                    AppUpdate(
                        releaseId = obj.optString("releaseId"),
                        versionCode = obj.optLong("versionCode"),
                        versionName = obj.optString("versionName"),
                        apkObjectKey = obj.optString("apkObjectKey"),
                        apkSize = obj.optLong("apkSize"),
                        changelog = changelog,
                        forceUpdate = obj.optBoolean("forceUpdate"),
                        createdAt = obj.optLong("createdAt")
                    )
                )
            } catch (e: Exception) {
                AppResult.Error(AppError.Storage("Güncelleme bilgisi çözümlenemedi.", e))
            } finally {
                connection.disconnect()
            }
        }

    private suspend fun validateApk(uri: Uri): ApkValidation =
        when (val result = inspectApk(uri)) {
            is AppResult.Success -> ApkValidation.Valid
            is AppResult.Error -> ApkValidation.Invalid(result.error.message)
        }

    private fun sha256(bytes: ByteArray): String =
        MessageDigest.getInstance("SHA-256")
            .digest(bytes)
            .joinToString("") { "%02X".format(it) }

    private sealed interface ApkValidation {
        data object Valid : ApkValidation
        data class Invalid(val message: String) : ApkValidation
    }

    private fun uploadWithRetry(
        uri: Uri,
        objectKey: String,
        fileSize: Long,
        onProgress: (Int) -> Unit
    ) {
        var lastError: Exception? = null
        repeat(MAX_ATTEMPTS) { attempt ->
            try {
                val expiration = Date(System.currentTimeMillis() + PRESIGNED_URL_LIFETIME_MS)
                val url = createClient().useS3 { client ->
                    client.generatePresignedUrl(
                        config.bucketName(context),
                        objectKey,
                        expiration,
                        HttpMethod.PUT
                    )
                }
                context.contentResolver.openInputStream(uri)?.use { input ->
                    putFixedLength(url, input, fileSize, onProgress)
                } ?: error("APK dosyası açılamadı.")
                return
            } catch (e: Exception) {
                lastError = e
                if (attempt < MAX_ATTEMPTS - 1) Thread.sleep(RETRY_DELAYS_MS[attempt])
            }
        }
        throw lastError ?: IllegalStateException("APK yükleme başarısız.")
    }

    private fun putFixedLength(
        url: URL,
        input: InputStream,
        fileSize: Long,
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
            setFixedLengthStreamingMode(fileSize)
            setRequestProperty("Content-Type", "application/vnd.android.package-archive")
        }

        try {
            connection.connect()
            connection.outputStream.use { output ->
                val buffer = ByteArray(64 * 1024)
                var uploaded = 0L
                while (true) {
                    val count = input.read(buffer)
                    if (count < 0) break
                    output.write(buffer, 0, count)
                    uploaded += count
                    onProgress(((uploaded * 100L) / fileSize).toInt().coerceIn(1, 99))
                }
                if (uploaded != fileSize) {
                    error("APK dosya boyutu değişti veya eksik okundu.")
                }
                output.flush()
            }
            val code = connection.responseCode
            if (code !in 200..299) throw IllegalStateException("R2 HTTP $code")
        } finally {
            connection.disconnect()
        }
    }

    private fun createClient(): AmazonS3Client =
        AmazonS3Client(
            BasicAWSCredentials(
                config.accessKeyId(context),
                config.secretAccessKey(context)
            ),
            Region.getRegion(Regions.US_EAST_1)
        ).also { client ->
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

    private inline fun <T> AmazonS3Client.useS3(block: (AmazonS3Client) -> T): T {
        try { return block(this) } finally { shutdown() }
    }

    private companion object {
        const val MAX_APK_BYTES = 200L * 1024L * 1024L
        const val PUBLIC_RELEASE_OBJECT_KEY = "releases/latest.json"
        const val RELEASE_ID_META_DATA_KEY = "com.libra.app.RELEASE_ID"
        const val MAX_ATTEMPTS = 3
        const val HTTP_TIMEOUT_MS = 60_000
        const val PRESIGNED_URL_LIFETIME_MS = 15L * 60L * 1000L
        const val SIGNED_URL_LIFETIME_MS = 15L * 60L * 1000L
        val RETRY_DELAYS_MS = longArrayOf(1000L, 2500L)
    }
}
