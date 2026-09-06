package com.example.plannerapp.credits

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface CreditDao {

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertTransaction(transaction: CreditTransactionEntity): Long

    @Query("SELECT COALESCE(SUM(amount), 0) FROM credit_transactions WHERE userId = :userId")
    fun getBalance(userId: Long): Flow<Int>

    @Query("SELECT COALESCE(SUM(amount), 0) FROM credit_transactions WHERE userId = :userId")
    suspend fun getBalanceOnce(userId: Long): Int

    @Query("SELECT * FROM credit_transactions WHERE userId = :userId ORDER BY createdAt DESC")
    fun getTransactionHistory(userId: Long): Flow<List<CreditTransactionEntity>>

    // ── Streak Freeze Support ───────────────────────────────────────────────
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFreeze(freeze: StreakFreezeEntity): Long

    @Query("SELECT COUNT(*) FROM streak_freezes WHERE userId = :userId AND isConsumed = 0")
    fun getAvailableFreezes(userId: Long): Flow<Int>

    @Query("SELECT * FROM streak_freezes WHERE userId = :userId AND isConsumed = 0 ORDER BY acquiredAt ASC LIMIT 1")
    suspend fun getNextUsableFreeze(userId: Long): StreakFreezeEntity?

    @Query("UPDATE streak_freezes SET isConsumed = 1, consumedAt = :consumedAt, targetDate = :targetDate WHERE freezeId = :freezeId")
    suspend fun consumeFreeze(freezeId: Long, consumedAt: Long, targetDate: String)

    @Transaction
    suspend fun purchaseStreakFreeze(userId: Long, cost: Int): Boolean {
        val balance = getBalanceOnce(userId)
        if (balance < cost) return false

        insertTransaction(
            CreditTransactionEntity(
                userId = userId,
                amount = -cost,
                transactionType = TransactionType.STREAK_FREEZE_REDEEMED,
                description = "Purchased Streak Freeze Protection"
            )
        )
        insertFreeze(StreakFreezeEntity(userId = userId))
        return true
    }
}
