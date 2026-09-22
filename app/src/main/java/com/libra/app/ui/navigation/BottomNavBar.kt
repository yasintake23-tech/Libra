package com.libra.app.ui.navigation

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.weight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.unit.dp

@Composable
fun LibraBottomBar(selectedTab: BottomNavTab, onTabSelected: (BottomNavTab) -> Unit, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .shadow(8.dp),
        color = MaterialTheme.colorScheme.surface
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            BottomItem(BottomNavTab.HOME, selectedTab, onTabSelected)
            BottomItem(BottomNavTab.DISCOVER, selectedTab, onTabSelected)
            BottomItem(BottomNavTab.DM, selectedTab, onTabSelected)
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
            .weight(1f)
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
