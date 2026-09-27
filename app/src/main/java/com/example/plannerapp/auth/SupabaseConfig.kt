package com.example.plannerapp.auth

import com.example.plannerapp.BuildConfig
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
 * Credentials are read securely from local.properties via BuildConfig,
 * ensuring no live secrets are ever committed into source control.
 */
object SupabaseConfig {

    val SUPABASE_URL: String = BuildConfig.SUPABASE_URL
    val SUPABASE_ANON_KEY: String = BuildConfig.SUPABASE_ANON_KEY
    val GOOGLE_SERVER_CLIENT_ID: String = BuildConfig.GOOGLE_SERVER_CLIENT_ID

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
