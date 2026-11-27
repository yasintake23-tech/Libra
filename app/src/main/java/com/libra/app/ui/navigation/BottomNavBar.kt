package com.libra.app.ui.navigation

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.libra.app.ui.animation.AppleSprings
import com.libra.app.ui.animation.appleBouncy

private val DmUnreadBadgeColor = Color(0xFFF43F5E)
private val ServerModeRingColor = Color(0xFF4F46E5)

/**
 * Floating glass pill navigation dock with round circular 3D buttons.
 * Designed to mirror Apple iOS floating dynamic controls with elastic pull & stretch physics,
 * plus a live animated unread private-chat badge on the DM slot.
 */
@Composable
fun LibraBottomBar(
    selectedTab: BottomNavTab,
    onTabSelected: (BottomNavTab) -> Unit,
    modifier: Modifier = Modifier,
    dmUnreadPrivateCount: Int = 0,
    isServerModeActive: Boolean = false
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 24.dp, vertical = 8.dp)
            .shadow(12.dp, shape = RoundedCornerShape(32.dp)),
        shape = RoundedCornerShape(32.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
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
            BottomItem(
                tab = BottomNavTab.DM,
                selectedTab = selectedTab,
                onTabSelected = onTabSelected,
                badgeCount = dmUnreadPrivateCount,
                highlightServerMode = isServerModeActive
            )
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
    modifier: Modifier = Modifier,
    badgeCount: Int = 0,
    highlightServerMode: Boolean = false
) {
    val selected = selectedTab == tab
    var tapToken by remember { mutableIntStateOf(0) }

    val iconScale by animateFloatAsState(
        targetValue = if (selected) 1.15f else 1.0f,
        animationSpec = AppleSprings.FloatBouncy,
        label = "tab_icon_scale"
    )

    val buttonBgColor = when {
        selected && highlightServerMode -> ServerModeRingColor.copy(alpha = 0.18f)
        selected -> MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)
        highlightServerMode -> ServerModeRingColor.copy(alpha = 0.08f)
        else -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.04f)
    }

    val buttonBorderColor = when {
        selected && highlightServerMode -> ServerModeRingColor.copy(alpha = 0.65f)
        selected -> MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)
        highlightServerMode -> ServerModeRingColor.copy(alpha = 0.30f)
        else -> Color.Transparent
    }

    Box(
        modifier = modifier.size(48.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
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

        // Dynamic animated unread private-conversation badge
        AnimatedVisibility(
            visible = badgeCount > 0,
            enter = scaleIn(animationSpec = AppleSprings.FloatBouncy, initialScale = 0.4f) + fadeIn(),
            exit = scaleOut(targetScale = 0.4f) + fadeOut(),
            modifier = Modifier
                .align(Alignment.TopEnd)
                .offset(x = 2.dp, y = (-2).dp)
                .testTag("${tab.testTag}_unread_badge")
        ) {
            Surface(
                shape = CircleShape,
                color = DmUnreadBadgeColor,
                border = BorderStroke(2.dp, MaterialTheme.colorScheme.surface),
                shadowElevation = 3.dp
            ) {
                Box(
                    modifier = Modifier
                        .defaultMinSize(minWidth = 20.dp, minHeight = 20.dp)
                        .padding(horizontal = if (badgeCount >= 10) 5.dp else 4.dp, vertical = 1.dp),
                    contentAlignment = Alignment.Center
                ) {
                    AnimatedContent(
                        targetState = badgeCount.coerceAtMost(100),
                        transitionSpec = {
                            val dir = if (targetState > initialState) -1 else 1
                            (slideInVertically { dir * it / 2 } + fadeIn()) togetherWith
                                (slideOutVertically { -dir * it / 2 } + fadeOut())
                        },
                        label = "dm_badge_count_tick"
                    ) { count ->
                        Text(
                            text = if (count > 99) "99+" else count.toString(),
                            color = Color.White,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.ExtraBold,
                            lineHeight = 11.sp
                        )
                    }
                }
            }
        }
    }
}
