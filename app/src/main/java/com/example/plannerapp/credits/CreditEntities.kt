package com.example.plannerapp.credits

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

enum class TransactionType {
    TASK_COMPLETED,
    STREAK_MILESTONE,
    PLAN_SHARED,
    VIRAL_CLONE_BONUS,
    STREAK_FREEZE_REDEEMED,
    PREMIUM_TIER_UNLOCK
}

@Entity(
    tableName = "credit_transactions",
    indices = [
        Index("userId"),
        Index("createdAt"),
        Index("transactionType")
    ]
)
data class CreditTransactionEntity(
    @PrimaryKey(autoGenerate = true) val transactionId: Long = 0,
    val userId: Long,
    val amount: Int, // Positive for earnings, negative for spends
    val transactionType: TransactionType,
    val referenceId: String? = null, // e.g., checkinId, planId, or shareCode
    val description: String,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "streak_freezes",
    indices = [Index("userId"), Index("consumedAt")]
)
data class StreakFreezeEntity(
    @PrimaryKey(autoGenerate = true) val freezeId: Long = 0,
    val userId: Long,
    val acquiredAt: Long = System.currentTimeMillis(),
    val isConsumed: Boolean = false,
    val consumedAt: Long? = null,
    val targetDate: String? = null // "YYYY-MM-DD" protected by this freeze
)
