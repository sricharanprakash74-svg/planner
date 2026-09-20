package com.example.plannerapp.ui.auth

import android.content.Context
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext

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
    val context = LocalContext.current
    val prefs = context.getSharedPreferences("onboarding_prefs", Context.MODE_PRIVATE)
    
    // Enterprise logic: Persist intro seen state
    var showIntro by remember { 
        mutableStateOf(!prefs.getBoolean("intro_seen", false)) 
    }

    if (showIntro) {
        IntroScreen(
            onNext = { 
                prefs.edit().putBoolean("intro_seen", true).apply()
                showIntro = false 
            },
            onSkip = { 
                prefs.edit().putBoolean("intro_seen", true).apply()
                showIntro = false 
            },
            onSignInClick = {
                prefs.edit().putBoolean("intro_seen", true).apply()
                showIntro = false
            }
        )
    } else {
        SignInScreen(
            viewModel = viewModel,
            onSignInSuccess = onSignInSuccess,
            modifier = modifier
        )
    }
}
