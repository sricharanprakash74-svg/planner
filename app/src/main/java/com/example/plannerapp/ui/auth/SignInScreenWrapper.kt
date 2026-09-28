package com.example.plannerapp.ui.auth

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * Feature wrapper for the authentication / sign-in screen, providing a clean architectural boundary
 * and routing target for parallel development.
 */
@Composable
fun SignInScreenWrapper(
    viewModel: AuthViewModel,
    onSignInSuccess: () -> Unit,
    modifier: Modifier = Modifier
) {
    SignInScreen(
        viewModel = viewModel,
        onSignInSuccess = onSignInSuccess,
        modifier = modifier
    )
}
