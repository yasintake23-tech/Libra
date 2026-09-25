package com.libra.app.ui.navigation

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp

@Composable
fun LibraBottomBar(
    selectedTab: BottomNavTab,
    onTabSelected: (BottomNavTab) -> Unit,
    modifier: Modifier = Modifier
) {
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
            BottomItem(BottomNavTab.HOME, selectedTab, onTabSelected, Modifier.weight(1f))
            BottomItem(BottomNavTab.DISCOVER, selectedTab, onTabSelected, Modifier.weight(1f))
            BottomItem(BottomNavTab.DM, selectedTab, onTabSelected, Modifier.weight(1f))
            BottomItem(BottomNavTab.LIBRARY, selectedTab, onTabSelected, Modifier.weight(1f))
            BottomItem(BottomNavTab.PROFILE, selectedTab, onTabSelected, Modifier.weight(1f))
        }
    }
}

@Composable
private fun BottomItem(
    tab: BottomNavTab,
    selectedTab: BottomNavTab,
    onTabSelected: (BottomNavTab) -> Unit,
    modifier: Modifier = Modifier
) {
    val selected = selectedTab == tab
    var tapToken by remember { mutableIntStateOf(0) }

    Column(
        modifier = modifier
            .testTag(tab.testTag)
            .clickable {
                tapToken += 1
                onTabSelected(tab)
            }
            .padding(vertical = 3.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        LibraAnimatedNavIcon(
            tab = tab,
            selected = selected,
            tapToken = tapToken,
            backgroundColor = MaterialTheme.colorScheme.surface,
            modifier = Modifier.size(25.dp)
        )
        Text(
            tab.title,
            style = MaterialTheme.typography.labelSmall,
            color = if (selected) {
                MaterialTheme.colorScheme.onBackground
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
            maxLines = 1
        )
    }
}
