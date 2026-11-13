package com.libra.app.ui.navigation

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.libra.app.ui.animation.AppleSprings
import com.libra.app.ui.animation.appleBouncy

/**
 * Floating glass pill navigation dock with round circular 3D buttons.
 * Designed to mirror Apple iOS floating dynamic controls with elastic pull & stretch physics.
 */
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
            .padding(horizontal = 24.dp, vertical = 8.dp)
            .shadow(12.dp, shape = RoundedCornerShape(32.dp)),
        shape = RoundedCornerShape(32.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            BottomItem(tab = BottomNavTab.HOME, selectedTab = selectedTab, onTabSelected = onTabSelected)
            BottomItem(tab = BottomNavTab.DISCOVER, selectedTab = selectedTab, onTabSelected = onTabSelected)
            BottomItem(tab = BottomNavTab.DM, selectedTab = selectedTab, onTabSelected = onTabSelected)
            BottomItem(tab = BottomNavTab.LIBRARY, selectedTab = selectedTab, onTabSelected = onTabSelected)
            BottomItem(tab = BottomNavTab.PROFILE, selectedTab = selectedTab, onTabSelected = onTabSelected)
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

    val iconScale by animateFloatAsState(
        targetValue = if (selected) 1.15f else 1.0f,
        animationSpec = AppleSprings.FloatBouncy,
        label = "tab_icon_scale"
    )

    val buttonBgColor = if (selected) {
        MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)
    } else {
        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.04f)
    }

    val buttonBorderColor = if (selected) {
        MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)
    } else {
        Color.Transparent
    }

    Box(
        modifier = modifier
            .size(46.dp)
            .testTag(tab.testTag)
            .clip(CircleShape)
            .background(buttonBgColor, CircleShape)
            .border(1.dp, buttonBorderColor, CircleShape)
            .appleBouncy(
                pressedScale = 0.85f,
                onClick = {
                    tapToken += 1
                    onTabSelected(tab)
                }
            ),
        contentAlignment = Alignment.Center
    ) {
        LibraAnimatedNavIcon(
            tab = tab,
            selected = selected,
            tapToken = tapToken,
            backgroundColor = MaterialTheme.colorScheme.surface,
            modifier = Modifier
                .size(24.dp)
                .graphicsLayer {
                    scaleX = iconScale
                    scaleY = iconScale
                }
        )
    }
}
