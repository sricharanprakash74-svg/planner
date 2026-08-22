package com.example.plannerapp

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable data object Home : NavKey
@Serializable data object Explore : NavKey
@Serializable data object Profile : NavKey
@Serializable data object Settings : NavKey
@Serializable data object CreatePlan : NavKey
@Serializable data class PlanDetail(val planId: Long) : NavKey
@Serializable data object Analytics : NavKey
@Serializable data object SettingsEditProfile : NavKey
@Serializable data object SettingsNotifications : NavKey
@Serializable data object SettingsTimezone : NavKey
@Serializable data object SettingsPublishPlan : NavKey
@Serializable data object SettingsBackup : NavKey
@Serializable data object SettingsExportData : NavKey
@Serializable data object SettingsDeleteAccount : NavKey
