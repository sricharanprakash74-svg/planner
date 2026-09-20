package com.example.plannerapp.ui.auth

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.plannerapp.auth.SupabaseConfig
import com.example.plannerapp.theme.AppDimens
import com.example.plannerapp.theme.AppDisabledBgLight
import com.example.plannerapp.theme.AppDisabledText
import com.example.plannerapp.theme.AppPrimary
import com.example.plannerapp.theme.AppPrimaryButton
import com.example.plannerapp.theme.AppPrimaryButtonPressed
import com.example.plannerapp.theme.AppTextSecondaryLight
import io.github.jan.supabase.compose.auth.composable.rememberSignInWithGoogle
import io.github.jan.supabase.compose.auth.composeAuth

@Composable
fun SignInScreen(
    viewModel: AuthViewModel,
    onSignInSuccess: () -> Unit,
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
                .padding(horizontal = AppDimens.ScreenHorizontalPadding)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Spacer(modifier = Modifier.height(AppDimens.Space32))

            AuthHeaderSection()

            Spacer(modifier = Modifier.height(AppDimens.SectionVerticalGap))

            SocialAuthSection(
                isLoading = uiState.isLoading,
                onGoogleSignInClick = {
                    focusManager.clearFocus()
                    googleSignInAction.startFlow()
                }
            )

            Spacer(modifier = Modifier.height(AppDimens.SectionVerticalGap))

            // ── Divider: or ───────────────────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                HorizontalDivider(
                    modifier = Modifier.weight(1f),
                    color = MaterialTheme.colorScheme.outline
                )
                Text(
                    text = "or",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = AppDimens.Space16)
                )
                HorizontalDivider(
                    modifier = Modifier.weight(1f),
                    color = MaterialTheme.colorScheme.outline
                )
            }

            Spacer(modifier = Modifier.height(AppDimens.SectionVerticalGap))

            // ── Auth Mode Tabs (Active underline & label: #D4720E, Inactive label: #6B7280) ──
            val selectedTabIndex = if (uiState.isSignUpMode) 0 else 1
            PrimaryTabRow(
                selectedTabIndex = selectedTabIndex,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = AppPrimary,
                modifier = Modifier.fillMaxWidth()
            ) {
                Tab(
                    selected = uiState.isSignUpMode,
                    onClick = { if (!uiState.isSignUpMode) viewModel.toggleAuthMode() },
                    text = {
                        Text(
                            text = "Create account",
                            style = MaterialTheme.typography.labelLarge,
                            color = if (uiState.isSignUpMode) AppPrimary else AppTextSecondaryLight
                        )
                    }
                )
                Tab(
                    selected = !uiState.isSignUpMode,
                    onClick = { if (uiState.isSignUpMode) viewModel.toggleAuthMode() },
                    text = {
                        Text(
                            text = "Sign in",
                            style = MaterialTheme.typography.labelLarge,
                            color = if (!uiState.isSignUpMode) AppPrimary else AppTextSecondaryLight
                        )
                    }
                )
            }

            Spacer(modifier = Modifier.height(AppDimens.Space16))

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

            Spacer(modifier = Modifier.height(AppDimens.Space16))

            // ── Primary Button: #B85E08 (accessible surface), darkens on press to #9E4F04, white text ──
            val buttonSource = remember { MutableInteractionSource() }
            val isButtonPressed by buttonSource.collectIsPressedAsState()
            val buttonBg = if (isButtonPressed) AppPrimaryButtonPressed else AppPrimaryButton

            Button(
                onClick = handleSubmit,
                enabled = !uiState.isLoading,
                interactionSource = buttonSource,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(AppDimens.ButtonHeight),
                shape = RoundedCornerShape(AppDimens.CornerButton),
                colors = ButtonDefaults.buttonColors(
                    containerColor = buttonBg,
                    contentColor = Color.White,
                    disabledContainerColor = AppDisabledBgLight,
                    disabledContentColor = AppDisabledText
                )
            ) {
                if (uiState.isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(22.dp),
                        strokeWidth = 2.dp,
                        color = Color.White
                    )
                } else {
                    Text(
                        text = if (uiState.isSignUpMode) "Create account" else "Sign in",
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }

            Spacer(modifier = Modifier.height(AppDimens.Space12))

            // ── Secondary / Ghost Button: #6B7280 label ──
            TextButton(
                onClick = { viewModel.continueAsGuest(onSuccess = onSignInSuccess) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(AppDimens.ButtonHeight),
                shape = RoundedCornerShape(AppDimens.CornerButton)
            ) {
                Text(
                    text = "Continue as guest",
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Medium),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(AppDimens.Space32))
        }
    }
}
