package com.example.plannerapp.data.creator

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface CreatorDao {
    @Query("SELECT * FROM creator_profiles WHERE creatorId = :creatorId LIMIT 1")
    fun observeCreatorProfile(creatorId: String): Flow<CreatorProfileEntity?>

    @Query("SELECT * FROM creator_profiles WHERE userId = :userId LIMIT 1")
    fun observeCreatorProfileByUserId(userId: String): Flow<CreatorProfileEntity?>

    @Query("SELECT * FROM creator_profiles LIMIT 1")
    fun observeActiveCreatorProfile(): Flow<CreatorProfileEntity?>

    @Query("SELECT * FROM creator_profiles WHERE userId = :userId LIMIT 1")
    suspend fun getCreatorProfileByUserId(userId: String): CreatorProfileEntity?

    @Query("SELECT * FROM creator_profiles WHERE creatorId = :creatorId LIMIT 1")
    suspend fun getCreatorProfileById(creatorId: String): CreatorProfileEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateProfile(profile: CreatorProfileEntity)

    @Query("UPDATE creator_profiles SET pendingBalanceCredits = :pending, availableBalanceCredits = :available, lifetimeEarnedUsd = :lifetimeEarnedUsd WHERE creatorId = :creatorId")
    suspend fun updateBalances(creatorId: String, pending: Long, available: Long, lifetimeEarnedUsd: Double)

    @Query("UPDATE creator_profiles SET stripeConnectedAccountId = :stripeId, kycStatus = :kycStatus WHERE creatorId = :creatorId")
    suspend fun updateKycAndStripe(creatorId: String, stripeId: String?, kycStatus: String)
}

@Dao
interface PlanVersionDao {
    @Query("SELECT * FROM creator_plan_versions WHERE creatorId = :creatorId ORDER BY publishedAt DESC")
    fun observeVersionsForCreator(creatorId: String): Flow<List<PlanVersionEntity>>

    @Query("SELECT * FROM creator_plan_versions WHERE planId = :planId ORDER BY publishedAt DESC")
    fun observeVersionsForPlan(planId: String): Flow<List<PlanVersionEntity>>

    @Query("SELECT * FROM creator_plan_versions WHERE versionId = :versionId LIMIT 1")
    fun observeVersion(versionId: String): Flow<PlanVersionEntity?>

    @Query("SELECT * FROM creator_plan_versions WHERE planId = :planId ORDER BY publishedAt DESC LIMIT 1")
    suspend fun getLatestVersionForPlan(planId: String): PlanVersionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVersion(version: PlanVersionEntity)

    @Update
    suspend fun updateVersion(version: PlanVersionEntity)

    @Query("UPDATE creator_plan_versions SET creditPrice = :creditPrice, freemiumPreviewDays = :freemiumPreviewDays WHERE planId = :planId")
    suspend fun updatePricing(planId: String, creditPrice: Int, freemiumPreviewDays: Int)

    @Query("UPDATE creator_plan_versions SET pacingMode = :pacingMode WHERE planId = :planId")
    suspend fun updatePacingMode(planId: String, pacingMode: String)

    @Query("UPDATE creator_plan_versions SET isMandatoryReflectionEnabled = :isMandatory, defaultReflectionPrompt = :defaultPrompt WHERE planId = :planId")
    suspend fun updateReflection(planId: String, isMandatory: Boolean, defaultPrompt: String)
}

@Dao
interface LedgerDao {
    @Query("SELECT * FROM creator_ledger_entries WHERE creatorId = :creatorId ORDER BY createdAt DESC")
    fun observeEntriesForCreator(creatorId: String): Flow<List<CreatorLedgerEntity>>

    @Query("SELECT * FROM creator_ledger_entries WHERE status = 'PENDING' AND escrowReleaseDate IS NOT NULL AND escrowReleaseDate <= :now")
    suspend fun getPendingMatureEscrow(now: Long): List<CreatorLedgerEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEntry(entry: CreatorLedgerEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEntries(entries: List<CreatorLedgerEntity>)

    @Query("UPDATE creator_ledger_entries SET status = :status WHERE transactionId = :transactionId")
    suspend fun updateEntryStatus(transactionId: String, status: String)
}

@Dao
interface PlanEntitlementDao {
    @Query("SELECT * FROM plan_entitlements WHERE planId = :planId AND userId = :userId AND isActive = 1 LIMIT 1")
    suspend fun getActiveEntitlement(planId: String, userId: String): PlanEntitlementEntity?

    @Query("SELECT * FROM plan_entitlements WHERE planId = :planId AND userId = :userId AND isActive = 1 LIMIT 1")
    fun observeActiveEntitlement(planId: String, userId: String): Flow<PlanEntitlementEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEntitlement(entitlement: PlanEntitlementEntity)

    @Query("UPDATE plan_entitlements SET isActive = :isActive WHERE entitlementId = :entitlementId")
    suspend fun updateEntitlementStatus(entitlementId: String, isActive: Boolean)
}
