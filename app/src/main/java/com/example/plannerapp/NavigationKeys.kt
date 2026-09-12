package com.example.plannerapp

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable data object SignIn : NavKey
@Serializable data object Home : NavKey
@Serializable data class Explore(val initialQuery: String = "") : NavKey
@Serializable data object Profile : NavKey
@Serializable data object Settings : NavKey
@Serializable data object CreatePlan : NavKey
@Serializable data class PlanDetail(val planId: Long, val autoOpenAddTask: Boolean = false) : NavKey
@Serializable data object Analytics : NavKey
@Serializable data object SettingsEditProfile : NavKey
@Serializable data object SettingsNotifications : NavKey
@Serializable data object SettingsTimezone : NavKey
@Serializable data object SettingsPublishPlan : NavKey
@Serializable data object SettingsBackup : NavKey
@Serializable data object SettingsExportData : NavKey
@Serializable data object SettingsDeleteAccount : NavKey
@Serializable data class CommunityDiscussion(val postId: String) : NavKey
@Serializable data class CreatorProfile(val userId: String) : NavKey
@Serializable data object SettingsCreatorSetup : NavKey
@Serializable data object SettingsCreatorMonetization : NavKey
@Serializable data object SettingsSavedPlans : NavKey
@Serializable data object SettingsActivityLog : NavKey
@Serializable data object SettingsTimeFocus : NavKey
@Serializable data object SettingsPlanPrivacy : NavKey
@Serializable data object SettingsAppearance : NavKey
@Serializable data object SettingsAccessibility : NavKey
@Serializable data object SettingsHelp : NavKey
@Serializable data object SettingsPrivacyPolicy : NavKey
@Serializable data object SettingsAbout : NavKey
