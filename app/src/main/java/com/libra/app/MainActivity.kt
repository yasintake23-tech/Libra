package com.libra.app

import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.libra.app.notification.LibraNotificationHelper
import com.libra.app.notification.NotificationLaunchPayload
import com.libra.app.ui.navigation.AppNavHost
import com.libra.app.ui.theme.LibraTheme

class MainActivity : ComponentActivity() {
    private var pendingNotificationLaunch by mutableStateOf<NotificationLaunchPayload?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        enableHighRefreshRate()
        LibraNotificationHelper.ensureChannels(this)
        pendingNotificationLaunch = LibraNotificationHelper.extractNotificationPayload(intent)

        setContent {
            val preferences = remember {
                getSharedPreferences("libra_settings", MODE_PRIVATE)
            }
            var darkTheme by remember {
                mutableStateOf(preferences.getBoolean("dark_theme", true))
            }

            LibraTheme(darkTheme = darkTheme) {
                AppNavHost(
                    modifier = Modifier.fillMaxSize(),
                    darkTheme = darkTheme,
                    onDarkThemeChanged = {
                        darkTheme = it
                        preferences.edit().putBoolean("dark_theme", it).apply()
                    },
                    pendingNotificationLaunch = pendingNotificationLaunch,
                    onNotificationLaunchConsumed = {
                        pendingNotificationLaunch = null
                        LibraNotificationHelper.clearNotificationExtras(intent)
                    }
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        pendingNotificationLaunch = LibraNotificationHelper.extractNotificationPayload(intent)
    }

    private fun enableHighRefreshRate() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            runCatching {
                val modes = display?.supportedModes ?: emptyArray()
                val highRefreshMode = modes.maxByOrNull { it.refreshRate }
                if (highRefreshMode != null && highRefreshMode.refreshRate >= 90f) {
                    window.attributes = window.attributes.apply {
                        preferredDisplayModeId = highRefreshMode.modeId
                    }
                }
            }
        }
    }
}
