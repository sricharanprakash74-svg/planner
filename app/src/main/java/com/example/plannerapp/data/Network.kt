package com.example.plannerapp.data

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST

// ── Auth Models ─────────────────────────────────────
data class LoginRequest(
    val firebaseToken: String,
    val email: String?,
    val displayName: String?
)

data class LoginResponse(
    val id: Long,
    val firebaseUid: String?,
    val email: String?,
    val displayName: String?,
    val createdAt: String
)

// ── Sync Models ─────────────────────────────────────
data class SyncPayload(
    val userId: Long,
    val plans: List<PlanEntity>,
    val templates: List<TaskTemplateEntity>,
    val checkins: List<DailyCheckinEntity>
)

data class SyncResponse(
    val success: Boolean,
    val message: String
)

// ── API Interface ───────────────────────────────────
interface PlannerApiService {
    @POST("/api/auth/login")
    suspend fun login(@Body request: LoginRequest): LoginResponse

    @POST("/api/sync")
    suspend fun syncData(@Body payload: SyncPayload): SyncResponse
}

// ── Retrofit Singleton ──────────────────────────────
object NetworkClient {
    // 10.0.2.2 is the special alias to your host loopback interface (localhost) from the Android emulator
    private const val BASE_URL = "http://10.0.2.2:3000"

    val api: PlannerApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(PlannerApiService::class.java)
    }
}
