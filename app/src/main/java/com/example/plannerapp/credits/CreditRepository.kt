package com.example.plannerapp.credits

import kotlinx.coroutines.flow.Flow

class CreditRepository(private val creditDao: CreditDao) {

    fun observeBalance(userId: Long): Flow<Int> = creditDao.getBalance(userId)
    fun observeHistory(userId: Long): Flow<List<CreditTransactionEntity>> = creditDao.getTransactionHistory(userId)
    fun observeAvailableFreezes(userId: Long): Flow<Int> = creditDao.getAvailableFreezes(userId)

    suspend fun awardTaskCompletion(userId: Long, checkinId: Long, points: Int = 10) {
        creditDao.insertTransaction(
            CreditTransactionEntity(
                userId = userId,
                amount = points,
                transactionType = TransactionType.TASK_COMPLETED,
                referenceId = checkinId.toString(),
                description = "Task completed"
            )
        )
    }

    suspend fun awardStreakMilestone(userId: Long, streakDays: Int, bonusPoints: Int = 50) {
        creditDao.insertTransaction(
            CreditTransactionEntity(
                userId = userId,
                amount = bonusPoints,
                transactionType = TransactionType.STREAK_MILESTONE,
                referenceId = "STREAK_$streakDays",
                description = "$streakDays-Day Streak Milestone Bonus"
            )
        )
    }

    suspend fun awardPlanShare(userId: Long, shareCode: String, bonusPoints: Int = 25) {
        creditDao.insertTransaction(
            CreditTransactionEntity(
                userId = userId,
                amount = bonusPoints,
                transactionType = TransactionType.PLAN_SHARED,
                referenceId = shareCode,
                description = "Plan template shared"
            )
        )
    }

    suspend fun awardViralCloneBonus(recipientUserId: Long, referrerUserId: Long, shareCode: String, bonusPoints: Int = 100) {
        // Reward Recipient
        creditDao.insertTransaction(
            CreditTransactionEntity(
                userId = recipientUserId,
                amount = bonusPoints,
                transactionType = TransactionType.VIRAL_CLONE_BONUS,
                referenceId = shareCode,
                description = "Claimed plan template bonus"
            )
        )
        // Reward Referrer
        creditDao.insertTransaction(
            CreditTransactionEntity(
                userId = referrerUserId,
                amount = bonusPoints,
                transactionType = TransactionType.VIRAL_CLONE_BONUS,
                referenceId = shareCode,
                description = "Peer cloned your shared plan"
            )
        )
    }

    suspend fun buyStreakFreeze(userId: Long, cost: Int = 150): Boolean {
        return creditDao.purchaseStreakFreeze(userId, cost)
    }

    suspend fun redeemTierUpgrade(userId: Long, tierName: String, cost: Int): Boolean {
        val balance = creditDao.getBalanceOnce(userId)
        if (balance < cost) return false

        creditDao.insertTransaction(
            CreditTransactionEntity(
                userId = userId,
                amount = -cost,
                transactionType = TransactionType.PREMIUM_TIER_UNLOCK,
                referenceId = tierName,
                description = "Unlocked $tierName"
            )
        )
        return true
    }

    suspend fun buyCreditsPack(userId: Long, amount: Int, packName: String) {
        creditDao.insertTransaction(
            CreditTransactionEntity(
                userId = userId,
                amount = amount,
                transactionType = TransactionType.PREMIUM_TIER_UNLOCK,
                referenceId = packName,
                description = "Purchased $packName ($amount Credits)"
            )
        )
    }

    suspend fun spendCreditsOnCreatorPlan(
        userId: Long,
        planTitle: String,
        cost: Int,
        creatorId: Long = 0L,
        buyerName: String = "User",
        planId: Long = 0L
    ): Boolean {
        val balance = creditDao.getBalanceOnce(userId)
        if (balance < cost) return false

        creditDao.insertTransaction(
            CreditTransactionEntity(
                userId = userId,
                amount = -cost,
                transactionType = TransactionType.PREMIUM_TIER_UNLOCK,
                referenceId = planTitle,
                description = "Unlocked Creator Plan: $planTitle"
            )
        )

        if (creatorId > 0L) {
            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                try {
                    com.example.plannerapp.data.NetworkClient.api.recordPlanSale(
                        com.example.plannerapp.data.RecordSalePayload(
                            creatorId = creatorId,
                            buyerId = userId,
                            buyerName = buyerName,
                            planId = planId,
                            planTitle = planTitle,
                            creditAmount = cost
                        )
                    )
                } catch (e: Exception) {
                    android.util.Log.w("CreditRepository", "Could not sync sale to backend: ${e.message}")
                }
            }
        }
        return true
    }

    suspend fun fetchCreatorDashboard(userId: Long): Result<com.example.plannerapp.data.CreatorDashboardResponse> {
        return kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            try {
                val resp = com.example.plannerapp.data.NetworkClient.api.getCreatorDashboard(userId)
                Result.success(resp)
            } catch (e: Exception) {
                android.util.Log.w("CreditRepository", "Backend unreachable for creator dashboard: ${e.message}")
                Result.success(
                    com.example.plannerapp.data.CreatorDashboardResponse(
                        userId = userId,
                        earnedCredits = 0,
                        redeemableCredits = 0,
                        estimatedUsd = 0.0,
                        totalPaidUsd = 0.0,
                        payoutMethod = "PAYPAL",
                        payoutAccount = "",
                        salesCount = 0
                    )
                )
            }
        }
    }

    suspend fun updatePayoutSettings(userId: Long, method: String, account: String): Result<com.example.plannerapp.data.PayoutSettingsResponse> {
        return kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            try {
                val resp = com.example.plannerapp.data.NetworkClient.api.updatePayoutSettings(
                    com.example.plannerapp.data.UpdatePayoutSettingsRequest(userId, method, account)
                )
                Result.success(resp)
            } catch (e: Exception) {
                android.util.Log.w("CreditRepository", "Failed to update payout settings: ${e.message}")
                Result.failure(e)
            }
        }
    }

    suspend fun requestCashOut(userId: Long, credits: Int, method: String, account: String): Result<com.example.plannerapp.data.PayoutResponse> {
        return kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            try {
                val resp = com.example.plannerapp.data.NetworkClient.api.requestPayout(
                    com.example.plannerapp.data.RequestPayoutPayload(userId, credits, method, account)
                )
                Result.success(resp)
            } catch (e: Exception) {
                android.util.Log.w("CreditRepository", "Failed to process cash out: ${e.message}")
                Result.failure(e)
            }
        }
    }

    suspend fun consumeStreakFreeze(userId: Long, targetDate: String): Boolean {
        val freeze = creditDao.getNextUsableFreeze(userId) ?: return false
        creditDao.consumeFreeze(
            freezeId = freeze.freezeId,
            consumedAt = System.currentTimeMillis(),
            targetDate = targetDate
        )
        return true
    }
}
