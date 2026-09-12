package com.example.plannerapp.ui.settings

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
fun SettingsScreenWrapper(
    viewModel: SettingsViewModel,
    onBack: () -> Unit,
    onEditProfileClick: () -> Unit,
    onSavedPlansClick: () -> Unit,
    onActivityLogClick: () -> Unit,
    onNotificationsClick: () -> Unit,
    onTimeFocusClick: () -> Unit,
    onTimezoneClick: () -> Unit,
    onPlanPrivacyClick: () -> Unit,
    onPublishPlanClick: () -> Unit,
    onAppearanceClick: () -> Unit,
    onBackupClick: () -> Unit,
    onExportClick: () -> Unit,
    onAccessibilityClick: () -> Unit,
    onCreatorSetupClick: () -> Unit,
    onCreatorMonetizationClick: () -> Unit = {},
    onSubscriptionClick: () -> Unit = {},
    onHelpClick: () -> Unit,
    onPrivacyPolicyClick: () -> Unit,
    onAboutClick: () -> Unit,
    onSignInClick: () -> Unit,
    onSignOutClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    SettingsScreen(
        viewModel = viewModel,
        onBack = onBack,
        onEditProfileClick = onEditProfileClick,
        onSavedPlansClick = onSavedPlansClick,
        onActivityLogClick = onActivityLogClick,
        onNotificationsClick = onNotificationsClick,
        onTimeFocusClick = onTimeFocusClick,
        onTimezoneClick = onTimezoneClick,
        onPlanPrivacyClick = onPlanPrivacyClick,
        onPublishPlanClick = onPublishPlanClick,
        onAppearanceClick = onAppearanceClick,
        onBackupClick = onBackupClick,
        onExportClick = onExportClick,
        onAccessibilityClick = onAccessibilityClick,
        onCreatorSetupClick = onCreatorSetupClick,
        onCreatorMonetizationClick = onCreatorMonetizationClick,
        onSubscriptionClick = onSubscriptionClick,
        onHelpClick = onHelpClick,
        onPrivacyPolicyClick = onPrivacyPolicyClick,
        onAboutClick = onAboutClick,
        onSignInClick = onSignInClick,
        onSignOutClick = onSignOutClick,
        modifier = modifier
    )
}
