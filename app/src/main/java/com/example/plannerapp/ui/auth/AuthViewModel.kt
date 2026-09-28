package com.example.plannerapp.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.plannerapp.auth.SupabaseConfig
import com.example.plannerapp.data.UserDao
import com.example.plannerapp.data.UserEntity
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.compose.auth.composable.NativeSignInResult
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put

data class AuthUiState(
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null,
    val isSignUpMode: Boolean = true,
    val isAuthenticated: Boolean = false
)

class AuthViewModel(
    private val userDao: UserDao,
    private val supabase: SupabaseClient = SupabaseConfig.client,
    private val appContext: android.content.Context? = null
) : ViewModel() {

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    fun getDraftDisplayName(): String? {
        val prefs = appContext?.getSharedPreferences("onboarding_prefs", android.content.Context.MODE_PRIVATE)
        return prefs?.getString("draft_display_name", null)?.takeIf { it.isNotBlank() }
    }

    fun toggleAuthMode() {
        _uiState.update { 
            it.copy(
                isSignUpMode = !it.isSignUpMode,
                errorMessage = null,
                successMessage = null
            ) 
        }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    fun signInWithEmail(email: String, password: String, onSuccess: () -> Unit) {
        if (email.isBlank() || password.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Please enter both email and password") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                supabase.auth.signInWith(Email) {
                    this.email = email.trim()
                    this.password = password
                }

                val authUser = supabase.auth.currentUserOrNull()
                    ?: throw IllegalStateException("Sign in succeeded but no user session was found.")
                val cloudUid = authUser.id
                val userEmail = authUser.email ?: email.trim()
                val displayName = authUser.userMetadata?.get("full_name")?.toString()
                    ?: userEmail.substringBefore("@").replaceFirstChar { it.uppercase() }

                syncLocalUserWithCloud(cloudUid, userEmail, displayName)

                _uiState.update { 
                    it.copy(
                        isLoading = false, 
                        isAuthenticated = true,
                        errorMessage = null
                    ) 
                }
                onSuccess()
            } catch (e: Exception) {
                _uiState.update { 
                    it.copy(
                        isLoading = false, 
                        errorMessage = e.message ?: "Sign in failed. Please check your credentials."
                    ) 
                }
            }
        }
    }

    fun signUpWithEmail(email: String, password: String, displayName: String, onSuccess: () -> Unit) {
        if (email.isBlank() || password.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Please enter email and password") }
            return
        }
        if (password.length < 6) {
            _uiState.update { it.copy(errorMessage = "Password must be at least 6 characters") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                supabase.auth.signUpWith(Email) {
                    this.email = email.trim()
                    this.password = password
                }

                val authUser = supabase.auth.currentUserOrNull()
                    ?: throw IllegalStateException("Sign up succeeded but user session is not available. Please verify your email or sign in.")
                val cloudUid = authUser.id
                val userEmail = authUser.email ?: email.trim()
                val name = displayName.ifBlank {
                    userEmail.substringBefore("@").replaceFirstChar { it.uppercase() }
                }

                syncLocalUserWithCloud(cloudUid, userEmail, name)

                _uiState.update { 
                    it.copy(
                        isLoading = false, 
                        isAuthenticated = true,
                        errorMessage = null
                    ) 
                }
                onSuccess()
            } catch (e: Exception) {
                _uiState.update { 
                    it.copy(
                        isLoading = false, 
                        errorMessage = e.message ?: "Account creation failed. Please try again."
                    ) 
                }
            }
        }
    }

    fun handleGoogleSignInResult(result: NativeSignInResult, onSuccess: () -> Unit) {
        when (result) {
            is NativeSignInResult.Success -> {
                viewModelScope.launch {
                    _uiState.update { it.copy(isLoading = true, errorMessage = null) }
                    try {
                        val authUser = supabase.auth.currentUserOrNull()
                            ?: throw IllegalStateException("Google sign in succeeded but no user session was found.")
                        val cloudUid = authUser.id
                        val userEmail = authUser.email ?: "google_user@plannerapp.com"
                        val displayName = authUser.userMetadata?.get("full_name")?.toString()
                            ?: authUser.userMetadata?.get("name")?.toString()
                            ?: userEmail.substringBefore("@").replaceFirstChar { it.uppercase() }

                        syncLocalUserWithCloud(cloudUid, userEmail, displayName)

                        _uiState.update { 
                            it.copy(
                                isLoading = false, 
                                isAuthenticated = true,
                                errorMessage = null
                            ) 
                        }
                        onSuccess()
                    } catch (e: Exception) {
                        _uiState.update { 
                            it.copy(
                                isLoading = false, 
                                errorMessage = e.message ?: "Failed to synchronize user account."
                            ) 
                        }
                    }
                }
            }
            is NativeSignInResult.Error -> {
                _uiState.update { 
                    it.copy(
                        isLoading = false, 
                        errorMessage = result.message
                    ) 
                }
            }
            is NativeSignInResult.NetworkError -> {
                _uiState.update { 
                    it.copy(
                        isLoading = false, 
                        errorMessage = "Network error. Please check your internet connection."
                    ) 
                }
            }
            is NativeSignInResult.ClosedByUser -> {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    private suspend fun syncLocalUserWithCloud(cloudUid: String, email: String, displayName: String) {
        val prefs = appContext?.getSharedPreferences("onboarding_prefs", android.content.Context.MODE_PRIVATE)
        val draftUsername = prefs?.getString("draft_username", null)?.takeIf { it.isNotBlank() }
        val draftDisplayName = prefs?.getString("draft_display_name", null)?.takeIf { it.isNotBlank() }
        val draftAvatarPath = prefs?.getString("draft_avatar_path", null)?.takeIf { it.isNotBlank() }
        val draftInterests = prefs?.getString("draft_interests", null)?.takeIf { it.isNotBlank() }
        val draftLevel = prefs?.getString("draft_level", "BEGINNER") ?: "BEGINNER"

        val effectiveDisplayName = draftDisplayName ?: displayName

        val existing = userDao.getActiveUserOnce()
        val targetUserId: Long
        if (existing != null) {
            val oldUserId = existing.userId
            userDao.upgradeToCloudUser(
                userId = oldUserId,
                cloudUserId = cloudUid,
                email = email,
                displayName = effectiveDisplayName
            )
            targetUserId = oldUserId
        } else {
            targetUserId = userDao.insertUser(
                UserEntity(
                    cloudUserId = cloudUid,
                    email = email,
                    displayName = effectiveDisplayName,
                    username = draftUsername,
                    interests = draftInterests ?: "[]",
                    experienceLevel = draftLevel,
                    avatarUrl = draftAvatarPath,
                    onboardingComplete = if (draftUsername != null || draftInterests != null) 1 else 0
                )
            )
        }

        var remoteAvatarUrl: String? = null
        if (!draftAvatarPath.isNullOrBlank() && SupabaseConfig.isConfigured) {
            try {
                val avatarFile = java.io.File(draftAvatarPath)
                if (avatarFile.exists()) {
                    val bytes = avatarFile.readBytes()
                    val path = "avatars/${cloudUid}.jpg"
                    SupabaseConfig.storage.from("avatars").upload(
                        path = path,
                        data = bytes,
                        options = { upsert = true }
                    )
                    remoteAvatarUrl = SupabaseConfig.storage.from("avatars").publicUrl(path)
                }
            } catch (_: Exception) {}
        }

        if (draftUsername != null || draftInterests != null || draftAvatarPath != null) {
            userDao.updateProfile(targetUserId, effectiveDisplayName, draftAvatarPath ?: existing?.avatarUrl)
            userDao.updateOnboardingProfile(
                userId = targetUserId,
                username = draftUsername ?: existing?.username,
                categories = "[]",
                interests = draftInterests ?: existing?.interests ?: "[]",
                level = draftLevel
            )
            userDao.markOnboardingComplete(targetUserId)
        }

        // Check & sync remote profile in Supabase
        if (SupabaseConfig.isConfigured) {
            try {
                val profile = supabase.postgrest.from("profiles").select {
                    filter { eq("id", cloudUid) }
                }.decodeList<JsonObject>().firstOrNull()
                // 1. User's explicit choice in onboarding (draftDisplayName / draftUsername) has highest priority
                // 2. Existing valid cloud profile (ignoring default "Planner User" and "user_xxxx" fallbacks)
                // 3. Fallback to auth name or local entity
                val remoteName = profile?.get("display_name")?.toString()?.trim('"')?.takeIf { 
                    it.isNotBlank() && it != "null" && it != "Planner User" 
                }
                val remoteUsername = profile?.get("username")?.toString()?.trim('"')?.takeIf { 
                    it.isNotBlank() && it != "null" && !it.startsWith("user_") 
                }
                val remoteAvatar = profile?.get("avatar_url")?.toString()?.trim('"')?.takeIf { 
                    it.isNotBlank() && it != "null" 
                }

                val finalDisplayName = draftDisplayName 
                    ?: (if (existing?.displayName.isNullOrBlank() || existing?.displayName == "Planner User") remoteName ?: effectiveDisplayName else existing?.displayName!!)
                val finalUsername = draftUsername 
                    ?: remoteUsername 
                    ?: existing?.username?.takeIf { !it.startsWith("user_") }
                    ?: finalDisplayName.lowercase().replace(" ", "_")
                val finalAvatar = draftAvatarPath ?: remoteAvatar ?: existing?.avatarUrl

                val payload = buildJsonObject {
                    put("id", cloudUid)
                    put("username", finalUsername)
                    put("display_name", finalDisplayName)
                    if (remoteAvatarUrl != null) {
                        put("avatar_url", remoteAvatarUrl)
                    } else if (remoteAvatar != null) {
                        put("avatar_url", remoteAvatar)
                    }
                    put("onboarding_completed", true)
                }
                supabase.postgrest.from("profiles").upsert(payload)

                // Sync user_interests to Supabase
                if (!draftInterests.isNullOrBlank()) {
                    val interestList = draftInterests.split(",").map { it.trim() }.filter { it.isNotBlank() }
                    val records = interestList.map { interest ->
                        buildJsonObject {
                            put("user_id", cloudUid)
                            put("interest_id", interest.lowercase())
                        }
                    }
                    if (records.isNotEmpty()) {
                        supabase.postgrest.from("user_interests").upsert(records)
                    }
                }

                // Update local Room database so it immediately reflects the real user profile
                userDao.updateProfile(targetUserId, finalDisplayName, finalAvatar)
                userDao.updateOnboardingProfile(
                    userId = targetUserId,
                    username = finalUsername,
                    categories = existing?.categories ?: "[]",
                    interests = draftInterests ?: existing?.interests ?: "[]",
                    level = draftLevel
                )
                userDao.markOnboardingComplete(targetUserId)
            } catch (e: Exception) {
                // Offline fallback — safely proceed with local state
                if (draftUsername != null || draftInterests != null) {
                    userDao.markOnboardingComplete(targetUserId)
                }
            }
        } else {
            if (draftUsername != null || draftInterests != null) {
                userDao.markOnboardingComplete(targetUserId)
            }
        }

        // Clean up draft prefs after saving
        prefs?.edit()
            ?.remove("draft_username")
            ?.remove("draft_display_name")
            ?.remove("draft_avatar_path")
            ?.remove("draft_interests")
            ?.remove("draft_level")
            ?.remove("current_step")
            ?.apply()

        // Restore private plans from the cloud (runs on re-install / new device)
        if (SupabaseConfig.isConfigured && appContext != null) {
            try {
                restoreCloudPlans(cloudUid, targetUserId, userDao)
            } catch (_: Exception) {
                // Non-fatal: local state is intact; sync will catch up next online session
            }
            // Drain any locally-queued outbox events now that we are authenticated
            com.example.plannerapp.sync.SupabaseSyncWorker.enqueue(appContext)
        }
    }

    /**
     * Downloads all [user_plans] rows owned by [cloudUid] from Supabase and
     * inserts missing ones into the local Room database.
     *
     * Only plans that have no matching [remoteId] in Room are inserted to avoid
     * duplicates. After insertion the [remoteId] is set immediately so subsequent
     * syncs skip the row.
     */
    private suspend fun restoreCloudPlans(
        cloudUid: String,
        localUserId: Long,
        userDao: UserDao
    ) {
        val remotePlans = supabase.postgrest.from("user_plans").select {
            filter { eq("owner_id", cloudUid) }
        }.decodeList<kotlinx.serialization.json.JsonObject>()

        if (remotePlans.isEmpty()) return

        val db = com.example.plannerapp.data.PlannerDatabase.getDatabase(
            appContext ?: return
        )
        val dao = db.plannerDao()

        for (remotePlan in remotePlans) {
            val remoteId = remotePlan["id"]?.jsonPrimitive?.content ?: continue
            val heading  = remotePlan["heading"]?.jsonPrimitive?.content ?: continue
            val startDate = remotePlan["start_date"]?.jsonPrimitive?.content ?: continue
            val endDate   = remotePlan["end_date"]?.jsonPrimitive?.content ?: startDate
            val description = remotePlan["description"]?.jsonPrimitive?.content ?: ""
            val isPublic = remotePlan["is_public"]?.jsonPrimitive?.content?.toBooleanStrictOrNull() ?: false

            // Skip if already present locally
            val allLocalPlans = dao.getPlansForUser(localUserId)
            val alreadyExists = allLocalPlans.first()
                .any { it.remoteId == remoteId }
            if (alreadyExists) continue

            val newPlan = com.example.plannerapp.data.PlanEntity(
                userId      = localUserId,
                remoteId    = remoteId,
                heading     = heading,
                description = description,
                startDate   = startDate,
                endDate     = endDate,
                isPublic    = isPublic,
                syncStatus  = "SYNCED"
            )
            dao.insertPlan(newPlan)
        }
    }
}

class AuthViewModelFactory(
    private val userDao: UserDao,
    private val supabase: SupabaseClient = SupabaseConfig.client,
    private val appContext: android.content.Context? = null
) : ViewModelProvider.Factory {
    constructor(userDao: UserDao, appContext: android.content.Context?) : this(userDao, SupabaseConfig.client, appContext)

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AuthViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return AuthViewModel(userDao, supabase, appContext) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
