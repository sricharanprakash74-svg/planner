package com.example.plannerapp.auth

import com.example.plannerapp.BuildConfig
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
import io.github.jan.supabase.storage.Storage
import io.github.jan.supabase.storage.storage
import kotlinx.serialization.json.Json

/**
 * Supabase client configuration and initialization.
 * 
 * Credentials are read securely at compile-time from local.properties via BuildConfig,
 * ensuring no live secrets are ever committed into source control.
 */
object SupabaseConfig {

    val SUPABASE_URL: String = BuildConfig.SUPABASE_URL
    val SUPABASE_ANON_KEY: String = BuildConfig.SUPABASE_ANON_KEY
    val GOOGLE_SERVER_CLIENT_ID: String = BuildConfig.GOOGLE_SERVER_CLIENT_ID

    val isConfigured: Boolean
        get() = SUPABASE_URL != "https://your-project.supabase.co" &&
                SUPABASE_ANON_KEY != "your-anon-key-placeholder" &&
                !SUPABASE_URL.contains("your-project.supabase.co") && 
                !SUPABASE_ANON_KEY.contains("placeholder") && 
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
            install(Storage)
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

    val storage: Storage
        get() = client.storage
}
