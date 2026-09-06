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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AuthUiState(
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null,
    val isSignUpMode: Boolean = false,
    val isAuthenticated: Boolean = false
)

class AuthViewModel(
    private val userDao: UserDao,
    private val supabase: SupabaseClient = SupabaseConfig.client
) : ViewModel() {

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

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
                val cloudUid = authUser?.id ?: "cloud_${System.currentTimeMillis()}"
                val userEmail = authUser?.email ?: email.trim()
                val displayName = authUser?.userMetadata?.get("full_name")?.toString()
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
                val cloudUid = authUser?.id ?: "cloud_${System.currentTimeMillis()}"
                val userEmail = authUser?.email ?: email.trim()
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
                        val cloudUid = authUser?.id ?: "google_${System.currentTimeMillis()}"
                        val userEmail = authUser?.email ?: "google_user@plannerapp.com"
                        val displayName = authUser?.userMetadata?.get("full_name")?.toString()
                            ?: authUser?.userMetadata?.get("name")?.toString()
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

    fun continueAsGuest(onSuccess: () -> Unit) {
        viewModelScope.launch {
            try {
                val activeUser = userDao.getActiveUserOnce()
                if (activeUser == null) {
                    userDao.insertUser(UserEntity(displayName = "Guest"))
                }
                onSuccess()
            } catch (e: Exception) {
                // If anything fails, still allow guest progression
                onSuccess()
            }
        }
    }

    private suspend fun syncLocalUserWithCloud(cloudUid: String, email: String, displayName: String) {
        val existing = userDao.getActiveUserOnce()
        if (existing != null) {
            userDao.upgradeToCloudUser(
                userId = existing.userId,
                cloudUserId = cloudUid,
                email = email,
                displayName = displayName
            )
        } else {
            userDao.insertUser(
                UserEntity(
                    cloudUserId = cloudUid,
                    email = email,
                    displayName = displayName
                )
            )
        }
    }
}

class AuthViewModelFactory(
    private val userDao: UserDao,
    private val supabase: SupabaseClient = SupabaseConfig.client
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AuthViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return AuthViewModel(userDao, supabase) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
