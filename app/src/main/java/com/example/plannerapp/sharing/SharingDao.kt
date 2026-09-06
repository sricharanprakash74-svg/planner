package com.example.plannerapp.sharing

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface SharingDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSharedPlan(sharedPlan: SharedPlanEntity): Long

    @Query("SELECT * FROM shared_plans WHERE shareCode = :code LIMIT 1")
    suspend fun getSharedPlanByCode(code: String): SharedPlanEntity?

    @Query("SELECT * FROM shared_plans WHERE authorUserId = :userId ORDER BY createdAt DESC")
    fun getSharedPlansByUser(userId: Long): Flow<List<SharedPlanEntity>>

    @Query("UPDATE shared_plans SET cloneCount = cloneCount + 1 WHERE shareCode = :code")
    suspend fun incrementCloneCount(code: String)
}
