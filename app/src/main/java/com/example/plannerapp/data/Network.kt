package com.example.plannerapp.data

import com.example.plannerapp.BuildConfig
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.POST
import java.util.concurrent.TimeUnit

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

// ── Creator Monetization Models ─────────────────────
data class PlanSaleItem(
    val id: Int = 0,
    val planTitle: String = "",
    val buyerName: String = "",
    val creditAmount: Int = 0,
    val creatorEarning: Int = 0,
    val createdAt: String = ""
)

data class PayoutHistoryItem(
    val id: Int = 0,
    val referenceId: String = "",
    val creditsDeducted: Int = 0,
    val amountUsd: Double = 0.0,
    val payoutMethod: String = "",
    val payoutAccount: String = "",
    val status: String = "PENDING",
    val createdAt: String = ""
)

data class CreatorDashboardResponse(
    val userId: Long = 0L,
    val earnedCredits: Int = 0,
    val redeemableCredits: Int = 0,
    val estimatedUsd: Double = 0.0,
    val totalPaidUsd: Double = 0.0,
    val payoutMethod: String = "PAYPAL",
    val payoutAccount: String = "",
    val salesCount: Int = 0,
    val minCashOutCredits: Int = 500,
    val minCashOutUsd: Double = 3.50,
    val recentSales: List<PlanSaleItem> = emptyList(),
    val recentPayouts: List<PayoutHistoryItem> = emptyList()
)

data class UpdatePayoutSettingsRequest(
    val userId: Long,
    val payoutMethod: String,
    val payoutAccount: String
)

data class PayoutSettingsResponse(
    val success: Boolean,
    val message: String,
    val payoutMethod: String? = null,
    val payoutAccount: String? = null
)

data class RequestPayoutPayload(
    val userId: Long,
    val creditsToRedeem: Int,
    val payoutMethod: String? = null,
    val payoutAccount: String? = null
)

data class PayoutResponse(
    val success: Boolean,
    val message: String,
    val referenceId: String? = null,
    val amountUsd: Double? = null,
    val creditsDeducted: Int? = null,
    val remainingCredits: Int? = null
)

data class RecordSalePayload(
    val creatorId: Long,
    val buyerId: Long?,
    val buyerName: String,
    val planId: Long,
    val planTitle: String,
    val creditAmount: Int
)

data class RecordSaleResponse(
    val success: Boolean,
    val creatorEarning: Int,
    val saleId: Int
)

// ── API Interface ───────────────────────────────────
interface PlannerApiService {
    @POST("/api/auth/login")
    suspend fun login(@Body request: LoginRequest): LoginResponse

    @POST("/api/sync")
    suspend fun syncData(@Body payload: SyncPayload): SyncResponse

    @GET("/api/creator/{userId}/dashboard")
    suspend fun getCreatorDashboard(@Path("userId") userId: Long): CreatorDashboardResponse

    @POST("/api/creator/payout-settings")
    suspend fun updatePayoutSettings(@Body request: UpdatePayoutSettingsRequest): PayoutSettingsResponse

    @POST("/api/creator/payout")
    suspend fun requestPayout(@Body payload: RequestPayoutPayload): PayoutResponse

    @POST("/api/creator/sell-plan")
    suspend fun recordPlanSale(@Body payload: RecordSalePayload): RecordSaleResponse

    @POST("/api/payments/create-intent")
    suspend fun createPaymentIntent(@Body payload: CreateIntentPayload): CreateIntentResponse

    // ── Secure Proxy Endpoints ──────────────────────────
    @GET("/api/proxy/status")
    suspend fun getProxyStatus(): ProxyStatusResponse

    @POST("/api/proxy/generate")
    suspend fun generateWithProxy(@Body request: ProxyChatRequest): ProxyChatResponse
}

data class CreateIntentPayload(
    val amount: Int,
    val currency: String = "usd",
    val userId: Long
)

data class CreateIntentResponse(
    val clientSecret: String,
    val paymentIntentId: String,
    val error: String? = null
)

// ── Secure Proxy Models ─────────────────────────────
data class ProxyChatMessage(
    val role: String,
    val content: String
)

data class ProxyChatRequest(
    val prompt: String? = null,
    val messages: List<ProxyChatMessage>? = null,
    val model: String? = null,
    val maxTokens: Int? = null,
    val temperature: Double? = null
)

data class ProxyChatResponse(
    val success: Boolean,
    val text: String,
    val model: String? = null,
    val isMock: Boolean = false,
    val error: String? = null,
    val message: String? = null
)

data class ProxyStatusResponse(
    val status: String,
    val mockMode: Boolean,
    val upstreamConfigured: Boolean,
    val model: String,
    val timestamp: String
)

// ── Retrofit Singleton ──────────────────────────────
class ProxyAuthInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): okhttp3.Response {
        val original = chain.request()
        val requestBuilder = original.newBuilder()
            .header("X-App-Secret", BuildConfig.CLIENT_APP_SECRET)
            .header("Accept", "application/json")
        val request = requestBuilder.build()
        return chain.proceed(request)
    }
}

object NetworkClient {
    val okHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .addInterceptor(ProxyAuthInterceptor())
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(45, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    val api: PlannerApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BuildConfig.BACKEND_BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(PlannerApiService::class.java)
    }
}
