package com.example.plannerapp.auth

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.compose.auth.ComposeAuth
import io.github.jan.supabase.compose.auth.composeAuth
import io.github.jan.supabase.compose.auth.googleNativeLogin
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.realtime.Realtime
import io.github.jan.supabase.realtime.realtime
import io.github.jan.supabase.serializer.KotlinXSerializer
import kotlinx.serialization.json.Json

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

    val isConfigured: Boolean
        get() = SUPABASE_URL != "https://your-project.supabase.co" &&
                SUPABASE_ANON_KEY != "your-anon-key-placeholder" &&
                SUPABASE_URL.isNotBlank() &&
                SUPABASE_ANON_KEY.isNotBlank()

    val client: SupabaseClient by lazy {
        createSupabaseClient(
            supabaseUrl = SUPABASE_URL,
            supabaseKey = SUPABASE_ANON_KEY
        ) {
            defaultSerializer = KotlinXSerializer(Json {
                ignoreUnknownKeys = true
                isLenient = true
                encodeDefaults = true
                coerceInputValues = true
            })
            install(Auth)
            install(ComposeAuth) {
                googleNativeLogin(serverClientId = GOOGLE_SERVER_CLIENT_ID)
            }
            install(Postgrest)
            install(Realtime)
        }
    }

    val auth: Auth
        get() = client.auth

    val composeAuth: ComposeAuth
        get() = client.composeAuth

    val postgrest: Postgrest
        get() = client.postgrest

    val realtime: Realtime
        get() = client.realtime
}
