package com.example.plannerapp.ui.navigation

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.dp

@Composable
fun AppScaffoldWrapper(
    currentTab: BottomNavTab,
    onTabSelected: (BottomNavTab) -> Unit,
    showBottomBar: Boolean,
    onAddClick: () -> Unit = {},
    avatarUrl: String? = null,
    modifier: Modifier = Modifier,
    content: @Composable (PaddingValues) -> Unit
) {
    var isBottomBarVisible by remember { mutableStateOf(true) }

    val nestedScrollConnection = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (available.y < -6f) {
                    if (isBottomBarVisible) isBottomBarVisible = false
                } else if (available.y > 6f) {
                    if (!isBottomBarVisible) isBottomBarVisible = true
                }
                return Offset.Zero
            }
        }
    }

    val bottomBarOffset by animateDpAsState(
        targetValue = if (isBottomBarVisible) 0.dp else 90.dp,
        animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMediumLow),
        label = "kineticBottomBarOffset"
    )

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .nestedScroll(nestedScrollConnection),
        bottomBar = {
            if (showBottomBar) {
                Box(
                    modifier = Modifier.offset(y = bottomBarOffset)
                ) {
                    AppBottomNavBar(
                        currentTab = currentTab,
                        onTabSelected = onTabSelected,
                        onAddClick = onAddClick,
                        avatarUrl = avatarUrl
                    )
                }
            }
        },
        content = content
    )
}

