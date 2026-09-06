package com.example.plannerapp.ui.settings

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
fun SettingsScreenWrapper(
    viewModel: SettingsViewModel,
    onBack: () -> Unit,
    onEditProfileClick: () -> Unit,
    onNotificationsClick: () -> Unit,
    onTimezoneClick: () -> Unit,
    onPublishPlanClick: () -> Unit,
    onBackupClick: () -> Unit,
    onExportClick: () -> Unit,
    onDeleteAccountClick: () -> Unit,
    onViewCreatorProfileClick: (String) -> Unit = {},
    onSignInClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    SettingsScreen(
        viewModel = viewModel,
        onBack = onBack,
        onEditProfileClick = onEditProfileClick,
        onNotificationsClick = onNotificationsClick,
        onTimezoneClick = onTimezoneClick,
        onPublishPlanClick = onPublishPlanClick,
        onBackupClick = onBackupClick,
        onExportClick = onExportClick,
        onDeleteAccountClick = onDeleteAccountClick,
        onViewCreatorProfileClick = onViewCreatorProfileClick,
        onSignInClick = onSignInClick,
        modifier = modifier
    )
}
