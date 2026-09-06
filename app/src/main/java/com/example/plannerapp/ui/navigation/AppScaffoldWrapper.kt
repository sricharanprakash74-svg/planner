package com.example.plannerapp.ui.navigation

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
fun AppScaffoldWrapper(
    currentTab: BottomNavTab,
    onTabSelected: (BottomNavTab) -> Unit,
    showBottomBar: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable (PaddingValues) -> Unit
) {
    Scaffold(
        modifier = modifier.safeDrawingPadding(),
        bottomBar = {
            if (showBottomBar) {
                AppBottomNavBar(
                    currentTab = currentTab,
                    onTabSelected = onTabSelected
                )
            }
        },
        content = content
    )
}
