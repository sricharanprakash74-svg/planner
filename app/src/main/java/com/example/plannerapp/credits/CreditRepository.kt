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
