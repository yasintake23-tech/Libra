package com.libra.app.ui.navigation

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
    Surface(
        modifier = modifier.navigationBarsPadding().shadow(8.dp),
        color = MaterialTheme.colorScheme.surface
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            BottomItem(BottomNavTab.HOME, selectedTab, onTabSelected)
            BottomItem(BottomNavTab.DISCOVER, selectedTab, onTabSelected)

            FloatingActionButton(
                onClick = { onTabSelected(BottomNavTab.WRITE) },
                containerColor = MaterialTheme.colorScheme.onBackground,
                contentColor = MaterialTheme.colorScheme.background,
                shape = CircleShape,
                modifier = Modifier.size(50.dp).testTag("nav_write_fab")
            ) {
                Icon(Icons.Default.Add, contentDescription = "Yaz")
            }

            Spacer(Modifier.width(2.dp))
            BottomItem(BottomNavTab.LIBRARY, selectedTab, onTabSelected)
            BottomItem(BottomNavTab.PROFILE, selectedTab, onTabSelected)
        }
    }
}

@Composable
private fun BottomItem(tab: BottomNavTab, selectedTab: BottomNavTab, onTabSelected: (BottomNavTab) -> Unit) {
    val selected = selectedTab == tab
    androidx.compose.foundation.layout.Column(
        modifier = Modifier
            .width(72.dp)
            .clickable { onTabSelected(tab) }
            .padding(vertical = 3.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            tab.icon,
            contentDescription = tab.title,
            tint = if (selected) MaterialTheme.colorScheme.onBackground else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(22.dp)
        )
        Text(
            tab.title,
            style = MaterialTheme.typography.labelSmall,
            color = if (selected) MaterialTheme.colorScheme.onBackground else MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1
        )
    }
}
