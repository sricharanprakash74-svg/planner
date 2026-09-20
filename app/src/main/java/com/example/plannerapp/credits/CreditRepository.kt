package com.example.plannerapp.credits

import kotlinx.coroutines.flow.Flow

class CreditRepository(private val creditDao: CreditDao) {

    fun observeBalance(userId: Long): Flow<Int> = creditDao.getBalance(userId)
    fun observeHistory(userId: Long): Flow<List<CreditTransactionEntity>> = creditDao.getTransactionHistory(userId)
    fun observeAvailableFreezes(userId: Long): Flow<Int> = creditDao.getAvailableFreezes(userId)

    // ── Earning Rules ────────────────────────────────────────────────────────
    //
    // All award methods are IDEMPOTENT via hasTransaction(userId, type, referenceId).
    // This means the same event can be safely called multiple times (e.g. if a user
    // unchecks and re-checks a task, or if a sync retries) without double-awarding.

    /**
     * +10 Credits: Complete ALL scheduled tasks for the day.
     *
     * [referenceId] is "DAY_{date}" e.g. "DAY_2026-09-18".
     * Called once from PlanDetailViewModel after any task toggle, when all
     * tasks for the selected date are found to be complete.
     * The idempotency guard prevents repeated awards if the user marks tasks
     * complete, then unchecks one and re-checks it.
     */
    suspend fun awardDayCompletion(userId: Long, date: String) {
        val ref = "DAY_$date"
        if (creditDao.hasTransaction(userId, TransactionType.TASK_COMPLETED, ref)) return
        creditDao.insertTransaction(
            CreditTransactionEntity(
                userId = userId,
                amount = 10,
                transactionType = TransactionType.TASK_COMPLETED,
                referenceId = ref,
                description = "Task completed"
            )
        )
    }

    /**
     * +50 Credits at 7-day streak, +100 Credits at 30-day streak.
     *
     * [streakDays] is the current computed streak length.
     * Only milestones of exactly 7 and 30 are awarded — other day counts are ignored.
     * [referenceId] is "STREAK_{streakDays}" so each milestone fires exactly once
     * per user lifetime (e.g. "STREAK_7", "STREAK_30").
     */
    suspend fun awardStreakMilestone(userId: Long, streakDays: Int) {
        val (bonusPoints, label) = when (streakDays) {
            7  -> 50  to "7-Day Streak Milestone Bonus"
            30 -> 100 to "30-Day Streak Milestone Bonus"
            else -> return // Not a milestone we award
        }
        val ref = "STREAK_$streakDays"
        if (creditDao.hasTransaction(userId, TransactionType.STREAK_MILESTONE, ref)) return
        creditDao.insertTransaction(
            CreditTransactionEntity(
                userId = userId,
                amount = bonusPoints,
                transactionType = TransactionType.STREAK_MILESTONE,
                referenceId = ref,
                description = label
            )
        )
    }

    /**
     * +25 Credits: Share a plan template to the community.
     *
     * [postId] is the unique post ID from the social backend.
     * Idempotency: if the post was already rewarded (e.g. on a retry), no duplicate.
     */
    suspend fun awardPlanShare(userId: Long, postId: String) {
        val ref = "SHARE_$postId"
        if (creditDao.hasTransaction(userId, TransactionType.PLAN_SHARED, ref)) return
        creditDao.insertTransaction(
            CreditTransactionEntity(
                userId = userId,
                amount = 25,
                transactionType = TransactionType.PLAN_SHARED,
                referenceId = ref,
                description = "Plan template shared"
            )
        )
    }

    /**
     * +100 Credits to the CREATOR when another user forks/joins their plan.
     *
     * Per the design spec: "When another user forks your shared plan."
     * Only the original creator earns this bonus — the joiner does not.
     * [referenceId] is "FORK_{joinerUserId}_{postId}" so each unique join-event
     * awards the creator once, but the same joiner cannot trigger it twice.
     */
    suspend fun awardViralCloneBonus(
        creatorUserId: Long,
        joinerUserId: Long,
        postId: String,
        bonusPoints: Int = 100
    ) {
        val ref = "FORK_${joinerUserId}_$postId"
        if (creditDao.hasTransaction(creatorUserId, TransactionType.VIRAL_CLONE_BONUS, ref)) return
        creditDao.insertTransaction(
            CreditTransactionEntity(
                userId = creatorUserId,
                amount = bonusPoints,
                transactionType = TransactionType.VIRAL_CLONE_BONUS,
                referenceId = ref,
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
