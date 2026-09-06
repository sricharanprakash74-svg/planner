package com.example.plannerapp.auth

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.compose.auth.ComposeAuth
import io.github.jan.supabase.compose.auth.composeAuth
import io.github.jan.supabase.compose.auth.googleNativeLogin
import io.github.jan.supabase.createSupabaseClient

/**
 * Supabase client configuration and initialization.
 * 
 * Replace placeholders with your actual project credentials:
 * - SUPABASE_URL: Your Supabase project URL (e.g., https://xyz.supabase.co)
 * - SUPABASE_ANON_KEY: Your Supabase anonymous public API key
 * - GOOGLE_SERVER_CLIENT_ID: Your Google Cloud Web Client ID (for Credential Manager ID tokens)
 */
object SupabaseConfig {

    const val SUPABASE_URL = "https://your-project.supabase.co"
    const val SUPABASE_ANON_KEY = "your-anon-key-placeholder"
    const val GOOGLE_SERVER_CLIENT_ID = "your-google-server-client-id.apps.googleusercontent.com"

    val client: SupabaseClient by lazy {
        createSupabaseClient(
            supabaseUrl = SUPABASE_URL,
            supabaseKey = SUPABASE_ANON_KEY
        ) {
            install(Auth)
            install(ComposeAuth) {
                googleNativeLogin(serverClientId = GOOGLE_SERVER_CLIENT_ID)
            }
        }
    }

    val auth: Auth
        get() = client.auth

    val composeAuth: ComposeAuth
        get() = client.composeAuth
}
