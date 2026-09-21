package com.libra.app.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.PersonOutline
import androidx.compose.ui.graphics.vector.ImageVector

enum class BottomNavTab(val title: String, val icon: ImageVector, val testTag: String) {
    HOME("Ana Sayfa", Icons.Default.Home, "nav_home"),
    DISCOVER("Keşfet", Icons.Default.Explore, "nav_discover"),
    WRITE("Yaz", Icons.Default.Explore, "nav_write"),
    LIBRARY("Kütüphane", Icons.Default.BookmarkBorder, "nav_library"),
    PROFILE("Profil", Icons.Default.PersonOutline, "nav_profile")
}
