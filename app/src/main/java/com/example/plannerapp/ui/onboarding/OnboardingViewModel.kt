package com.example.plannerapp.ui.onboarding

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.plannerapp.auth.SupabaseConfig
import com.example.plannerapp.data.UserDao
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

enum class UsernameValidationState {
    IDLE,
    CHECKING,
    VALID,
    INVALID
}

class OnboardingViewModel(
    private val userDao: UserDao,
    private val context: Context
) : ViewModel() {

    private val prefs = context.getSharedPreferences("onboarding_prefs", Context.MODE_PRIVATE)

    private val _currentStep = MutableStateFlow(prefs.getInt("current_step", 1))
    val currentStep: StateFlow<Int> = _currentStep.asStateFlow()

    private val _displayName = MutableStateFlow("")
    val displayName: StateFlow<String> = _displayName.asStateFlow()

    private val _username = MutableStateFlow("")
    val username: StateFlow<String> = _username.asStateFlow()

    private val _usernameValidationState = MutableStateFlow(UsernameValidationState.IDLE)
    val usernameValidationState: StateFlow<UsernameValidationState> = _usernameValidationState.asStateFlow()

    private val _usernameErrorMessage = MutableStateFlow<String?>(null)
    val usernameErrorMessage: StateFlow<String?> = _usernameErrorMessage.asStateFlow()

    private val _selectedInterests = MutableStateFlow<Set<String>>(emptySet())
    val selectedInterests: StateFlow<Set<String>> = _selectedInterests.asStateFlow()

    private val _experienceLevel = MutableStateFlow(ExperienceLevel.BEGINNER)
    val experienceLevel: StateFlow<ExperienceLevel> = _experienceLevel.asStateFlow()

    private var usernameValidationJob: Job? = null

    init {
        // Load existing active user info if present
        viewModelScope.launch {
            val activeUser = userDao.getActiveUserOnce()
            if (activeUser != null) {
                if (activeUser.displayName.isNotBlank() && activeUser.displayName != "Guest") {
                    _displayName.value = activeUser.displayName
                }
                if (!activeUser.username.isNullOrBlank()) {
                    _username.value = activeUser.username
                    _usernameValidationState.value = UsernameValidationState.VALID
                } else if (activeUser.categories.isNotBlank() && activeUser.categories != "[]" && !activeUser.categories.startsWith("[")) {
                    val existingUsername = activeUser.categories.removePrefix("@").trim()
                    if (existingUsername.isNotBlank()) {
                        _username.value = existingUsername
                        _usernameValidationState.value = UsernameValidationState.VALID
                    }
                }
                if (activeUser.interests.isNotBlank() && activeUser.interests != "[]") {
                    val cleaned = activeUser.interests
                        .removePrefix("[")
                        .removeSuffix("]")
                        .replace("\"", "")
                    _selectedInterests.value = cleaned
                        .split(",")
                        .map { it.trim() }
                        .filter { it.isNotBlank() }
                        .toSet()
                }
            }
        }
    }

    fun setStep(step: Int) {
        prefs.edit().putInt("current_step", step).apply()
        _currentStep.value = step
    }

    fun setDisplayName(name: String) {
        _displayName.value = name
    }

    fun setUsername(newUsername: String) {
        val sanitized = newUsername.removePrefix("@").trim()
        _username.value = sanitized
        _usernameErrorMessage.value = null

        usernameValidationJob?.cancel()

        if (sanitized.isBlank()) {
            _usernameValidationState.value = UsernameValidationState.IDLE
            return
        }

        if (sanitized.length < 3) {
            _usernameValidationState.value = UsernameValidationState.INVALID
            _usernameErrorMessage.value = "Username must be at least 3 characters"
            return
        }

        if (sanitized.length > 20) {
            _usernameValidationState.value = UsernameValidationState.INVALID
            _usernameErrorMessage.value = "Username cannot exceed 20 characters"
            return
        }

        if (!sanitized.matches(Regex("^[a-zA-Z0-9_]+$"))) {
            _usernameValidationState.value = UsernameValidationState.INVALID
            _usernameErrorMessage.value = "Only letters, numbers, and underscores allowed"
            return
        }

        val reserved = setOf("admin", "root", "planner", "tusknet", "support", "moderator", "official", "help", "system")
        if (reserved.contains(sanitized.lowercase())) {
            _usernameValidationState.value = UsernameValidationState.INVALID
            _usernameErrorMessage.value = "This username is reserved"
            return
        }

        _usernameValidationState.value = UsernameValidationState.CHECKING

        usernameValidationJob = viewModelScope.launch {
            delay(350) // Debounce 350ms
            try {
                if (SupabaseConfig.isConfigured) {
                    val currentUid = SupabaseConfig.auth.currentUserOrNull()?.id
                    val matches = SupabaseConfig.postgrest.from("profiles").select {
                        filter {
                            eq("username", sanitized.lowercase())
                        }
                    }.decodeList<JsonObject>()

                    val isTakenByOther = matches.any { obj ->
                        val id = obj["id"]?.toString()?.trim('"')
                        id != currentUid
                    }

                    if (isTakenByOther) {
                        _usernameValidationState.value = UsernameValidationState.INVALID
                        _usernameErrorMessage.value = "Username is already taken"
                    } else {
                        _usernameValidationState.value = UsernameValidationState.VALID
                        _usernameErrorMessage.value = null
                    }
                } else {
                    _usernameValidationState.value = UsernameValidationState.VALID
                    _usernameErrorMessage.value = null
                }
            } catch (e: Exception) {
                // Offline fallback: optimistically treat as valid
                _usernameValidationState.value = UsernameValidationState.VALID
                _usernameErrorMessage.value = null
            }
        }
    }

    fun toggleInterest(interest: String) {
        val current = _selectedInterests.value.toMutableSet()
        if (current.contains(interest)) {
            current.remove(interest)
        } else if (current.size < 5) {
            current.add(interest)
        }
        _selectedInterests.value = current
    }

    private val _avatarUri = MutableStateFlow<android.net.Uri?>(null)
    val avatarUri: StateFlow<android.net.Uri?> = _avatarUri.asStateFlow()

    fun setAvatarUri(uri: android.net.Uri?) {
        _avatarUri.value = uri
    }

    fun saveProfile(context: android.content.Context? = null, onSuccess: () -> Unit) {
        viewModelScope.launch {
            val user = userDao.getActiveUserOnce()
            val finalDisplayName = _displayName.value.trim().ifBlank { user?.displayName ?: "Planner User" }
            val finalUsername = _username.value.trim().lowercase()

            var localAvatarPath = user?.avatarUrl
            var remoteAvatarUrl: String? = null

            val uri = _avatarUri.value
            if (uri != null && context != null && user != null) {
                try {
                    val inputStream = context.contentResolver.openInputStream(uri)
                    if (inputStream != null) {
                        val avatarFile = java.io.File(context.filesDir, "avatar_${user.userId}.jpg")
                        avatarFile.outputStream().use { output ->
                            inputStream.copyTo(output)
                        }
                        localAvatarPath = avatarFile.absolutePath

                        val uid = SupabaseConfig.auth.currentUserOrNull()?.id ?: user.cloudUserId
                        if (SupabaseConfig.isConfigured && !uid.isNullOrBlank()) {
                            try {
                                val bytes = avatarFile.readBytes()
                                val path = "avatars/${uid}.jpg"
                                SupabaseConfig.storage.from("avatars").upload(
                                    path = path,
                                    data = bytes,
                                    options = { upsert = true }
                                )
                                remoteAvatarUrl = SupabaseConfig.storage.from("avatars").publicUrl(path)
                            } catch (_: Exception) {}
                        }
                    }
                } catch (_: Exception) {}
            }

            if (user != null) {
                userDao.updateProfile(user.userId, finalDisplayName, localAvatarPath)
                userDao.updateOnboardingProfile(
                    userId = user.userId,
                    username = finalUsername,
                    categories = "[]",
                    interests = _selectedInterests.value.joinToString(","),
                    level = _experienceLevel.value.name
                )
            }

            // Remote sync to Supabase profiles
            if (SupabaseConfig.isConfigured) {
                try {
                    val uid = SupabaseConfig.auth.currentUserOrNull()?.id ?: user?.cloudUserId
                    if (uid != null) {
                        val payload = buildJsonObject {
                            put("id", uid)
                            put("username", finalUsername.ifBlank { finalDisplayName.lowercase().replace(" ", "_") })
                            put("display_name", finalDisplayName)
                            if (remoteAvatarUrl != null) {
                                put("avatar_url", remoteAvatarUrl)
                            }
                        }
                        SupabaseConfig.postgrest.from("profiles").upsert(payload)
                    }
                } catch (e: Exception) {
                    // Offline fallback: local state updated successfully
                }
            }
            onSuccess()
        }
    }

    fun saveInterests(onSuccess: () -> Unit) {
        viewModelScope.launch {
            val user = userDao.getActiveUserOnce()
            if (user != null) {
                userDao.updateOnboardingProfile(
                    userId = user.userId,
                    username = _username.value.trim().lowercase(),
                    categories = "[]",
                    interests = _selectedInterests.value.joinToString(","),
                    level = _experienceLevel.value.name
                )
            }

            if (SupabaseConfig.isConfigured) {
                try {
                    val uid = SupabaseConfig.auth.currentUserOrNull()?.id ?: user?.cloudUserId
                    if (uid != null) {
                        val records = _selectedInterests.value.map { interest ->
                            buildJsonObject {
                                put("user_id", uid)
                                put("interest_id", interest.lowercase())
                            }
                        }
                        if (records.isNotEmpty()) {
                            SupabaseConfig.postgrest.from("user_interests").upsert(records)
                        }
                    }
                } catch (e: Exception) {
                    // Table might not exist yet or offline — safe fallback
                }
            }
            onSuccess()
        }
    }

    fun saveNotificationPreference(enabled: Boolean, onDone: () -> Unit) {
        prefs.edit().putBoolean("notifications_enabled", enabled).apply()
        viewModelScope.launch {
            val user = userDao.getActiveUserOnce()
            if (SupabaseConfig.isConfigured) {
                try {
                    val uid = SupabaseConfig.auth.currentUserOrNull()?.id ?: user?.cloudUserId
                    if (uid != null) {
                        val payload = buildJsonObject {
                            put("user_id", uid)
                            put("enabled", enabled)
                            put("plan_reminders", enabled)
                            put("social_updates", enabled)
                            put("streak_reminders", enabled)
                        }
                        SupabaseConfig.postgrest.from("notification_preferences").upsert(payload)
                    }
                } catch (e: Exception) {
                    // Offline or table not configured yet — safe fallback
                }
            }
            onDone()
        }
    }

    fun finishOnboarding(onFinish: () -> Unit) {
        viewModelScope.launch {
            val user = userDao.getActiveUserOnce()
            if (user != null) {
                userDao.markOnboardingComplete(user.userId)
            }
            prefs.edit().remove("current_step").apply()

            if (SupabaseConfig.isConfigured) {
                try {
                    val uid = SupabaseConfig.auth.currentUserOrNull()?.id ?: user?.cloudUserId
                    if (uid != null) {
                        val payload = buildJsonObject {
                            put("onboarding_completed", true)
                        }
                        SupabaseConfig.postgrest.from("profiles").update(payload) {
                            filter { eq("id", uid) }
                        }
                    }
                } catch (e: Exception) {
                    // Offline fallback
                }
            }
            onFinish()
        }
    }
}

class OnboardingViewModelFactory(
    private val userDao: UserDao,
    private val context: Context
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(OnboardingViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return OnboardingViewModel(userDao, context) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
