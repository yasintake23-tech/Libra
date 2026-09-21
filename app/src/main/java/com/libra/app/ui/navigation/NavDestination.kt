package com.libra.app.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.EditNote
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.PeopleOutline
import androidx.compose.material.icons.outlined.PersonOutline
import androidx.compose.ui.graphics.vector.ImageVector

enum class BottomNavTab(
    val route: String,
    val title: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String
) {
    HOME("tab_home", "Ana Sayfa", Icons.Filled.Home, Icons.Outlined.Home, "nav_tab_home"),
    LIBRARY("tab_library", "Kütüphane", Icons.Filled.Bookmark, Icons.Outlined.BookmarkBorder, "nav_tab_library"),
    WRITE("tab_write", "Yaz", Icons.Filled.EditNote, Icons.Outlined.EditNote, "nav_tab_write"),
    FRIENDS("tab_friends", "Topluluk", Icons.Filled.People, Icons.Outlined.PeopleOutline, "nav_tab_friends"),
    PROFILE("tab_profile", "Profil", Icons.Filled.Person, Icons.Outlined.PersonOutline, "nav_tab_profile")
}
