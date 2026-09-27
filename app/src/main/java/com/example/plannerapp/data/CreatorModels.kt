package com.example.plannerapp.data

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
