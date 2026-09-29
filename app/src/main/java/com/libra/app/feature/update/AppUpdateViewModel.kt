package com.libra.app.feature.update

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.libra.app.BuildConfig
import com.libra.app.core.di.ServiceLocator
import com.libra.app.core.result.AppResult
import com.libra.app.domain.model.AppUpdate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

data class AppUpdateUiState(
    val release: AppUpdate? = null,
    val checking: Boolean = false,
    val downloading: Boolean = false,
    val progress: Int = 0,
    val downloadedFile: File? = null,
    val error: String? = null
)

class AppUpdateViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(AppUpdateUiState())
    val uiState: StateFlow<AppUpdateUiState> = _uiState.asStateFlow()

    fun checkForUpdate() {
        if (_uiState.value.checking) return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(checking = true, error = null)
            // R2 is the public source, while Firestore is a backup source.
            // Never let an older Firebase record override a newer R2 release.
            val publicResult = ServiceLocator.appReleaseStorageRepository.getPublicRelease()
            val firebaseResult = ServiceLocator.updateRepository.getActiveRelease()

            val candidates = buildList {
                if (publicResult is AppResult.Success) publicResult.data?.let(::add)
                if (firebaseResult is AppResult.Success) firebaseResult.data?.let(::add)
            }

            if (candidates.isEmpty()) {
                val message = when {
                    publicResult is AppResult.Error -> publicResult.error.message
                    firebaseResult is AppResult.Error -> firebaseResult.error.message
                    else -> "Yayınlanmış güncelleme bulunamadı."
                }
                _uiState.value = _uiState.value.copy(
                    checking = false,
                    error = message
                )
                return@launch
            }

            val release = candidates.maxWithOrNull(
                compareBy<AppUpdate> { it.versionCode }.thenBy { it.createdAt }
            )
            val localReleaseId = BuildConfig.LIBRA_RELEASE_ID.trim()
            val hasUpdate = release != null &&
                release.releaseId.isNotBlank() &&
                localReleaseId.isNotBlank() &&
                release.releaseId != localReleaseId &&
                release.versionCode > BuildConfig.VERSION_CODE.toLong()

            _uiState.value = _uiState.value.copy(
                checking = false,
                release = release?.takeIf { hasUpdate }
            )
        }
    }

    fun download(context: Context) {
        val release = _uiState.value.release ?: return
        if (_uiState.value.downloading) return

        viewModelScope.launch(Dispatchers.IO) {
            _uiState.value = _uiState.value.copy(
                downloading = true, progress = 0, downloadedFile = null, error = null
            )
            val url = ServiceLocator.appReleaseStorageRepository
                .getSignedApkUrl(release.apkObjectKey)

            if (url.isNullOrBlank()) {
                _uiState.value = _uiState.value.copy(
                    downloading = false,
                    error = "Güncelleme dosyasının bağlantısı alınamadı."
                )
                return@launch
            }

            val dir = File(context.cacheDir, "updates").apply { mkdirs() }
            val target = File(dir, "Libra-${release.versionName}.apk")
            val temp = File(dir, "Libra-${release.versionName}.apk.download")

            try {
                downloadFile(url, temp, release.apkSize) { progress ->
                    _uiState.value = _uiState.value.copy(progress = progress)
                }
                if (target.exists()) target.delete()
                if (!temp.renameTo(target)) error("İndirilen APK hazırlanamadı.")
                _uiState.value = _uiState.value.copy(
                    downloading = false, progress = 100, downloadedFile = target
                )
            } catch (e: Exception) {
                temp.delete()
                _uiState.value = _uiState.value.copy(
                    downloading = false,
                    error = e.message ?: "APK indirilemedi."
                )
            }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    fun dismissUpdate() {
        _uiState.value.downloadedFile?.delete()
        _uiState.value = _uiState.value.copy(release = null, downloadedFile = null, progress = 0)
    }

    private fun downloadFile(url: String, target: File, expectedSize: Long, onProgress: (Int) -> Unit) {
        val connection = (URL(url).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            doInput = true
            useCaches = false
            instanceFollowRedirects = true
            connectTimeout = 30_000
            readTimeout = 60_000
        }
        try {
            connection.connect()
            if (connection.responseCode !in 200..299) {
                error("Güncelleme sunucusu HTTP ${connection.responseCode}")
            }
            val total = connection.contentLengthLong.takeIf { it > 0 } ?: expectedSize
            var received = 0L
            connection.inputStream.use { input ->
                target.outputStream().use { output ->
                    val buffer = ByteArray(64 * 1024)
                    while (true) {
                        val count = input.read(buffer)
                        if (count < 0) break
                        output.write(buffer, 0, count)
                        received += count
                        if (total > 0) {
                            onProgress(((received * 100L) / total).toInt().coerceIn(0, 99))
                        }
                    }
                    output.flush()
                }
            }
            if (expectedSize > 0 && received != expectedSize) error("APK eksik indirildi.")
            onProgress(100)
        } finally {
            connection.disconnect()
        }
    }
}
