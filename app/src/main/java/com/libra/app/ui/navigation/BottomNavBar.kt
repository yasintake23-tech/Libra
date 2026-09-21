package com.libra.app.ui.navigation

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp

@Composable
fun LibraBottomBar(selectedTab: BottomNavTab, onTabSelected: (BottomNavTab) -> Unit, modifier: Modifier = Modifier) {
    Surface(modifier = modifier.navigationBarsPadding().shadow(8.dp), color = MaterialTheme.colorScheme.surface) {
        Row(modifier = Modifier.padding(horizontal = 4.dp, vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
            BottomItem(BottomNavTab.HOME, selectedTab, onTabSelected, Modifier.weight(1f))
            BottomItem(BottomNavTab.DISCOVER, selectedTab, onTabSelected, Modifier.weight(1f))
            FloatingActionButton(
                onClick = { onTabSelected(BottomNavTab.WRITE) },
                containerColor = MaterialTheme.colorScheme.onBackground,
                contentColor = MaterialTheme.colorScheme.background,
                shape = CircleShape,
                modifier = Modifier.size(50.dp).testTag("nav_write_fab")
            ) { Icon(Icons.Default.Add, "Yaz") }
            Spacer(Modifier.width(2.dp))
            BottomItem(BottomNavTab.LIBRARY, selectedTab, onTabSelected, Modifier.weight(1f))
            BottomItem(BottomNavTab.PROFILE, selectedTab, onTabSelected, Modifier.weight(1f))
        }
    }
}

@Composable
private fun BottomItem(tab: BottomNavTab, selectedTab: BottomNavTab, onTabSelected: (BottomNavTab) -> Unit, modifier: Modifier) {
    NavigationBarItem(
        selected = selectedTab == tab,
        onClick = { onTabSelected(tab) },
        icon = { Icon(tab.icon, tab.title) },
        label = { Text(tab.title) },
        colors = NavigationBarItemDefaults.colors(
            selectedIconColor = MaterialTheme.colorScheme.onBackground,
            selectedTextColor = MaterialTheme.colorScheme.onBackground,
            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
            indicatorColor = MaterialTheme.colorScheme.surface
        ),
        modifier = modifier.testTag(tab.testTag)
    )
}
