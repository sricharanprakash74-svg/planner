package com.example.plannerapp.ui.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.unit.dp
import com.example.plannerapp.auth.SupabaseConfig
import io.github.jan.supabase.compose.auth.composable.rememberSignInWithGoogle
import io.github.jan.supabase.compose.auth.composeAuth

@Composable
fun SignInScreen(
    viewModel: AuthViewModel,
    onSignInSuccess: () -> Unit,
    onContinueAsGuest: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val focusManager = LocalFocusManager.current

    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var displayName by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }

    // Google Sign In via Credential Manager & Supabase ComposeAuth
    val googleSignInAction = SupabaseConfig.client.composeAuth.rememberSignInWithGoogle(
        onResult = { result ->
            viewModel.handleGoogleSignInResult(result, onSuccess = onSignInSuccess)
        }
    )

    val handleSubmit = {
        focusManager.clearFocus()
        if (uiState.isSignUpMode) {
            viewModel.signUpWithEmail(email, password, displayName, onSignInSuccess)
        } else {
            viewModel.signInWithEmail(email, password, onSignInSuccess)
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Spacer(modifier = Modifier.height(32.dp))

            AuthHeaderSection(
                isSignUpMode = uiState.isSignUpMode,
                onTabSelected = { isSignUp ->
                    if (uiState.isSignUpMode != isSignUp) {
                        viewModel.toggleAuthMode()
                    }
                }
            )

            Spacer(modifier = Modifier.height(24.dp))

            AuthFormSection(
                isSignUpMode = uiState.isSignUpMode,
                displayName = displayName,
                onDisplayNameChange = { displayName = it },
                email = email,
                onEmailChange = {
                    email = it
                    if (uiState.errorMessage != null) viewModel.clearError()
                },
                password = password,
                onPasswordChange = {
                    password = it
                    if (uiState.errorMessage != null) viewModel.clearError()
                },
                passwordVisible = passwordVisible,
                onPasswordVisibilityToggle = { passwordVisible = !passwordVisible },
                errorMessage = uiState.errorMessage,
                onSubmit = handleSubmit
            )

            Spacer(modifier = Modifier.height(24.dp))

            SocialAuthSection(
                isSignUpMode = uiState.isSignUpMode,
                isLoading = uiState.isLoading,
                onPrimarySubmit = handleSubmit,
                onGoogleSignInClick = {
                    focusManager.clearFocus()
                    googleSignInAction.startFlow()
                },
                onContinueAsGuest = {
                    focusManager.clearFocus()
                    viewModel.continueAsGuest(onSuccess = onContinueAsGuest)
                }
            )

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}
