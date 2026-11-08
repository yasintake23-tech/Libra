package com.libra.app.feature.update

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.libra.app.domain.model.AppUpdate
import java.io.File
import kotlinx.coroutines.withTimeoutOrNull

private const val UPDATE_ADMIN_PASSWORD = "Libra+8369#"
private const val ADMIN_HOLD_MILLIS = 10_000L

@Composable
fun AppUpdatePrompt(
    release: AppUpdate,
    progress: Int,
    downloading: Boolean,
    error: String?,
    forceUpdate: Boolean,
    onDownload: () -> Unit,
    onDismiss: () -> Unit,
    onClearError: () -> Unit,
    downloadedFile: File?
) {
    val context = LocalContext.current
    var showAdminLogin by remember { mutableStateOf(false) }
    var adminPassword by remember { mutableStateOf("") }
    var adminPasswordError by remember { mutableStateOf(false) }
    var holdStarted by remember { mutableStateOf(false) }
    var adminHoldTriggered by remember { mutableStateOf(false) }

    fun openInstaller(file: File) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
            !context.packageManager.canRequestPackageInstalls()
        ) {
            context.startActivity(
                Intent(
                    Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                    Uri.parse("package:${context.packageName}")
                )
            )
            return
        }
        val uri = FileProvider.getUriForFile(
            context, "${context.packageName}.fileprovider", file
        )
        context.startActivity(
            Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        )
    }

    LaunchedEffect(downloadedFile) {
        downloadedFile?.let { openInstaller(it) }
    }

    AlertDialog(
        onDismissRequest = { if (!forceUpdate && !downloading) onDismiss() },
        title = { Text("Uygulama yeni sürüm geldi") },
        text = {
            Column {
                Text("Libra ${release.versionName} sürümü hazır.")
                if (release.changelog.isNotEmpty()) {
                    Spacer(Modifier.height(10.dp))
                    Text("Yenilikler", style = MaterialTheme.typography.titleSmall)
                    release.changelog.take(6).forEach { Text("• $it") }
                }
                if (downloading) {
                    Spacer(Modifier.height(16.dp))
                    LinearProgressIndicator(
                        progress = { progress / 100f },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(6.dp))
                    Text("%$progress indiriliyor")
                }
                error?.let {
                    Spacer(Modifier.height(10.dp))
                    Text(it, color = MaterialTheme.colorScheme.error)
                }
            }
        },
        confirmButton = {
            when {
                downloadedFile != null -> Button(onClick = { openInstaller(downloadedFile) }) {
                    Text("Kurulumu Aç")
                }
                downloading -> Unit
                else -> {
                    Button(
                        onClick = {
                            if (adminHoldTriggered) {
                                adminHoldTriggered = false
                            } else {
                                onDownload()
                            }
                        },
                        modifier = Modifier.pointerInput(Unit) {
                            awaitEachGesture {
                                val down = awaitFirstDown(requireUnconsumed = false)
                                if (!down.pressed) return@awaitEachGesture

                                holdStarted = true
                                val holdCompleted = withTimeoutOrNull<Unit>(ADMIN_HOLD_MILLIS) {
                                    while (true) {
                                        val event = awaitPointerEvent()
                                        if (event.changes.none { it.pressed }) {
                                            return@withTimeoutOrNull
                                        }
                                    }
                                }

                                holdStarted = false
                                if (holdCompleted == null) {
                                    adminHoldTriggered = true
                                    adminPassword = ""
                                    adminPasswordError = false
                                    showAdminLogin = true
                                }
                            }
                        }
                    ) {
                        Text(if (holdStarted) "10 saniye basılı tut..." else "İndir")
                    }
                }
            }
        },
        dismissButton = {
            if (!forceUpdate && !downloading) {
                TextButton(onClick = onDismiss) { Text("Daha sonra") }
            } else if (error != null) {
                TextButton(onClick = onClearError) { Text("Kapat") }
            }
        }
    )
    if (showAdminLogin) {
        AlertDialog(
            onDismissRequest = { showAdminLogin = false },
            title = { Text("Admin girişi") },
            text = {
                Column {
                    Text("Güncelleme uyarısını kapatmak için admin şifresini gir.")
                    androidx.compose.material3.OutlinedTextField(
                        value = adminPassword,
                        onValueChange = {
                            adminPassword = it
                            adminPasswordError = false
                        },
                        label = { Text("Şifre") },
                        singleLine = true,
                        isError = adminPasswordError
                    )
                    if (adminPasswordError) {
                        Text("Şifre yanlış.", color = MaterialTheme.colorScheme.error)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (adminPassword == UPDATE_ADMIN_PASSWORD) {
                            showAdminLogin = false
                            adminPassword = ""
                            adminPasswordError = false
                            onDismiss()
                        } else {
                            adminPasswordError = true
                        }
                    }
                ) {
                    Text("Giriş")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAdminLogin = false }) {
                    Text("İptal")
                }
            }
        )
    }

}
