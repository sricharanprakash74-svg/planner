package com.example.plannerapp.ui.settings

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * Backward-compatible wrapper delegating to the Cupertino-grade [ProfileCenterScreen].
 */
@Composable
fun EditProfileScreen(
    viewModel: SettingsViewModel,
    onBack: () -> Unit,
    onDeleteAccountClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    ProfileCenterScreen(
        viewModel = viewModel,
        onBack = onBack,
        modifier = modifier
    )
}
