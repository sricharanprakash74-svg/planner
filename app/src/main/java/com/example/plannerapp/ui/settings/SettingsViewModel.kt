package com.example.plannerapp.ui.settings

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.plannerapp.data.UserDao
import com.example.plannerapp.data.UserEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File

class SettingsViewModel(
    private val userDao: UserDao
) : ViewModel() {

    val avatarVersion = MutableStateFlow(System.currentTimeMillis())

    val currentUser: StateFlow<UserEntity?> = userDao.getActiveUser()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    fun updateProfile(displayName: String, avatarUrl: String?) {
        viewModelScope.launch {
            val user = userDao.getActiveUserOnce() ?: return@launch
            userDao.updateProfile(user.userId, displayName, avatarUrl)
        }
    }

    fun updateProfilePicture(context: Context, imageUri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val user = userDao.getActiveUserOnce() ?: return@launch
                val inputStream = context.contentResolver.openInputStream(imageUri) ?: return@launch
                val avatarFile = File(context.filesDir, "avatar_${user.userId}.jpg")
                avatarFile.outputStream().use { output ->
                    inputStream.copyTo(output)
                }
                userDao.updateProfile(user.userId, user.displayName, avatarFile.absolutePath)
                avatarVersion.value = System.currentTimeMillis()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun updateTimezone(timezone: String) {
        viewModelScope.launch {
            val user = userDao.getActiveUserOnce() ?: return@launch
            userDao.updateTimezone(user.userId, timezone)
        }
    }

    fun setCreatorStatus(isCreator: Boolean) {
        viewModelScope.launch {
            val user = userDao.getActiveUserOnce() ?: return@launch
            userDao.updateCreatorStatus(user.userId, isCreator)
        }
    }

    fun deleteAccount(onSuccess: () -> Unit) {
        viewModelScope.launch {
            val user = userDao.getActiveUserOnce()
            if (user != null) {
                userDao.deleteUser(user.userId)
                // Re-create a default guest
                userDao.insertUser(UserEntity(displayName = "Guest"))
            }
            onSuccess()
        }
    }

    fun simulateSignIn() {
        viewModelScope.launch {
            try {
                val user = userDao.getActiveUserOnce()
                if (user != null && user.cloudUserId == null) {
                    val response = com.example.plannerapp.data.NetworkClient.api.login(
                        com.example.plannerapp.data.LoginRequest(
                            firebaseToken = "mock_firebase_uid_123",
                            email = "rahul@gmail.com",
                            displayName = "Rahul Kumar"
                        )
                    )
                    
                    userDao.upgradeToCloudUser(
                        userId = user.userId, 
                        cloudUserId = response.id.toString(), 
                        email = response.email ?: "", 
                        displayName = response.displayName ?: "User"
                    )
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun signOut(onSuccess: () -> Unit) {
        viewModelScope.launch {
            val user = userDao.getActiveUserOnce() ?: return@launch
            val displayName = user.displayName
            userDao.deleteUser(user.userId)
            userDao.insertUser(UserEntity(displayName = displayName))
            onSuccess()
        }
    }

    fun saveCreatorCategories(context: android.content.Context, categories: List<String>) {
        val prefs = context.getSharedPreferences("creator_prefs", android.content.Context.MODE_PRIVATE)
        prefs.edit().putStringSet("categories", categories.toSet()).apply()
    }

    fun loadCreatorCategories(context: android.content.Context): List<String> {
        val prefs = context.getSharedPreferences("creator_prefs", android.content.Context.MODE_PRIVATE)
        return prefs.getStringSet("categories", emptySet())?.sorted() ?: emptyList()
    }

    fun saveCreatorTagline(context: android.content.Context, tagline: String) {
        val prefs = context.getSharedPreferences("creator_prefs", android.content.Context.MODE_PRIVATE)
        prefs.edit().putString("tagline", tagline).apply()
    }

    fun loadCreatorTagline(context: android.content.Context): String {
        val prefs = context.getSharedPreferences("creator_prefs", android.content.Context.MODE_PRIVATE)
        return prefs.getString("tagline", "") ?: ""
    }
    // ── Notification Preferences ────────────────────
    fun saveNotifPrefs(context: android.content.Context, allowNotifications: Boolean, morningReminder: Boolean, eveningReminder: Boolean, streakAlerts: Boolean, communityAlerts: Boolean, vibrate: Boolean, sound: Boolean) {
        val p = context.getSharedPreferences("notif_prefs", android.content.Context.MODE_PRIVATE)
        p.edit()
            .putBoolean("allow", allowNotifications)
            .putBoolean("morning", morningReminder)
            .putBoolean("evening", eveningReminder)
            .putBoolean("streak", streakAlerts)
            .putBoolean("community", communityAlerts)
            .putBoolean("vibrate", vibrate)
            .putBoolean("sound", sound)
            .apply()
    }
    fun loadNotifPrefs(context: android.content.Context): Map<String, Boolean> {
        val p = context.getSharedPreferences("notif_prefs", android.content.Context.MODE_PRIVATE)
        return mapOf(
            "allow" to p.getBoolean("allow", true),
            "morning" to p.getBoolean("morning", true),
            "evening" to p.getBoolean("evening", true),
            "streak" to p.getBoolean("streak", true),
            "community" to p.getBoolean("community", false),
            "vibrate" to p.getBoolean("vibrate", true),
            "sound" to p.getBoolean("sound", true)
        )
    }

    // ── Appearance Preferences ──────────────────────
    fun saveAppearancePrefs(context: android.content.Context, darkMode: Boolean, dynamicColor: Boolean, compactLayout: Boolean) {
        val p = context.getSharedPreferences("appearance_prefs", android.content.Context.MODE_PRIVATE)
        p.edit().putBoolean("dark_mode", darkMode).putBoolean("dynamic_color", dynamicColor).putBoolean("compact", compactLayout).apply()
    }
    fun loadAppearancePrefs(context: android.content.Context): Map<String, Boolean> {
        val p = context.getSharedPreferences("appearance_prefs", android.content.Context.MODE_PRIVATE)
        return mapOf(
            "dark_mode" to p.getBoolean("dark_mode", false),
            "dynamic_color" to p.getBoolean("dynamic_color", true),
            "compact" to p.getBoolean("compact", false)
        )
    }

    // ── Time & Focus Preferences ────────────────────
    fun saveTimeFocusPrefs(context: android.content.Context, focusMode: Boolean, pomodoro: Boolean, durationIndex: Int, dailyPlanning: Boolean) {
        val p = context.getSharedPreferences("timefocus_prefs", android.content.Context.MODE_PRIVATE)
        p.edit().putBoolean("focus_mode", focusMode).putBoolean("pomodoro", pomodoro).putInt("duration_index", durationIndex).putBoolean("daily_planning", dailyPlanning).apply()
    }
    fun loadTimeFocusPrefs(context: android.content.Context): Map<String, Any> {
        val p = context.getSharedPreferences("timefocus_prefs", android.content.Context.MODE_PRIVATE)
        return mapOf(
            "focus_mode" to p.getBoolean("focus_mode", false),
            "pomodoro" to p.getBoolean("pomodoro", true),
            "duration_index" to p.getInt("duration_index", 1),
            "daily_planning" to p.getBoolean("daily_planning", true)
        )
    }

    // ── Accessibility Preferences ───────────────────
    fun saveAccessibilityPrefs(context: android.content.Context, highContrast: Boolean, reducedMotion: Boolean, largerTargets: Boolean, textSizeIndex: Int) {
        val p = context.getSharedPreferences("a11y_prefs", android.content.Context.MODE_PRIVATE)
        p.edit().putBoolean("high_contrast", highContrast).putBoolean("reduced_motion", reducedMotion).putBoolean("larger_targets", largerTargets).putInt("text_size", textSizeIndex).apply()
    }
    fun loadAccessibilityPrefs(context: android.content.Context): Map<String, Any> {
        val p = context.getSharedPreferences("a11y_prefs", android.content.Context.MODE_PRIVATE)
        return mapOf(
            "high_contrast" to p.getBoolean("high_contrast", false),
            "reduced_motion" to p.getBoolean("reduced_motion", false),
            "larger_targets" to p.getBoolean("larger_targets", false),
            "text_size" to p.getInt("text_size", 1)
        )
    }

    // ── Plan Privacy Preferences ────────────────────
    fun savePlanPrivacyPrefs(context: android.content.Context, visibilityIndex: Int, allowComments: Boolean, allowForking: Boolean) {
        val p = context.getSharedPreferences("privacy_prefs", android.content.Context.MODE_PRIVATE)
        p.edit().putInt("visibility", visibilityIndex).putBoolean("allow_comments", allowComments).putBoolean("allow_forking", allowForking).apply()
    }
    fun loadPlanPrivacyPrefs(context: android.content.Context): Map<String, Any> {
        val p = context.getSharedPreferences("privacy_prefs", android.content.Context.MODE_PRIVATE)
        return mapOf(
            "visibility" to p.getInt("visibility", 0),
            "allow_comments" to p.getBoolean("allow_comments", true),
            "allow_forking" to p.getBoolean("allow_forking", true)
        )
    }
}

class SettingsViewModelFactory(
    private val userDao: UserDao
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SettingsViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return SettingsViewModel(userDao) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
