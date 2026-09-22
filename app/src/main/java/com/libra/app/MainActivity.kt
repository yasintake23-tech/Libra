package com.libra.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import com.libra.app.ui.navigation.AppNavHost
import com.libra.app.ui.theme.LibraTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
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
                    }
                )
            }
        }
    }
}
