package com.example.plannerapp.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.plannerapp.ui.navigation.AppBottomNavBar
import com.example.plannerapp.ui.navigation.BottomNavTab

typealias BottomNavTab = com.example.plannerapp.ui.navigation.BottomNavTab

@Composable
fun BottomNavBar(
    currentTab: BottomNavTab,
    onTabSelected: (BottomNavTab) -> Unit,
    modifier: Modifier = Modifier
) {
    AppBottomNavBar(
        currentTab = currentTab,
        onTabSelected = onTabSelected,
        modifier = modifier
    )
}
