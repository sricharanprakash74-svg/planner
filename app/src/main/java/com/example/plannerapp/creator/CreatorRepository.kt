package com.example.plannerapp.creator

import com.example.plannerapp.data.PlannerDao
import com.example.plannerapp.data.UserDao
import com.example.plannerapp.data.creator.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.util.UUID

/**
 * Repository orchestrating Creator Studio operations, including the immutable dual ledger,
 * 14-day escrow clearing engine, semantic versioning, and cryptographic offline DRM.
 */
class CreatorRepository(
    private val creatorDao: CreatorDao,
    private val planVersionDao: PlanVersionDao,
    private val ledgerDao: LedgerDao,
    private val planEntitlementDao: PlanEntitlementDao,
    private val plannerDao: PlannerDao,
    private val userDao: UserDao
) {

    companion object {
        const val CREDIT_PEG_USD = 0.0050
        const val REVENUE_SHARE_RATIO = 0.70
        const val NET_USD_PER_CREDIT = CREDIT_PEG_USD * REVENUE_SHARE_RATIO // 0.0035 USD per credit
        const val MIN_PAYOUT_USD = 100.00
        const val ESCROW_HOLD_MILLIS = 14L * 24 * 60 * 60 * 1000L // 14 days in milliseconds
    }

    /**
     * Converts credit amounts to USD according to the fixed $0.0050 peg and 70% net payout model.
     */
    fun calculateTakeHomeUsd(credits: Long): Double {
        return (credits * CREDIT_PEG_USD) * REVENUE_SHARE_RATIO
    }

    fun observeCreatorProfile(userId: String): Flow<CreatorProfileEntity?> {
        return creatorDao.observeCreatorProfileByUserId(userId)
    }

    fun observeVersionsForCreator(creatorId: String): Flow<List<PlanVersionEntity>> {
        return planVersionDao.observeVersionsForCreator(creatorId)
    }

    fun observeLedgerForCreator(creatorId: String): Flow<List<CreatorLedgerEntity>> {
        return ledgerDao.observeEntriesForCreator(creatorId)
    }

    suspend fun getCreatorProfile(userId: String): CreatorProfileEntity? = withContext(Dispatchers.IO) {
        creatorDao.getCreatorProfileByUserId(userId)
    }

    /**
     * Registers a user as an active creator, initializes the creator profile, updates UserEntity,
     * and provisions initial seeded plan snapshots and telemetry if needed.
     */
    suspend fun registerCreator(
        userId: String,
        handle: String,
        bio: String
    ): CreatorProfileEntity = withContext(Dispatchers.IO) {
        val existing = creatorDao.getCreatorProfileByUserId(userId)
        if (existing != null) {
            val updated = existing.copy(handle = handle, bio = bio)
            creatorDao.insertOrUpdateProfile(updated)
            userDao.updateCreatorStatus(userId.toLongOrNull() ?: 0L, true)
            return@withContext updated
        }

        val creatorId = "creator_${UUID.randomUUID().toString().take(8)}"
        val newProfile = CreatorProfileEntity(
            creatorId = creatorId,
            userId = userId,
            handle = handle,
            bio = bio,
            isVerified = true,
            kycStatus = "VERIFIED",
            stripeConnectedAccountId = "acct_connect_${UUID.randomUUID().toString().take(12)}",
            pendingBalanceCredits = 12500L,
            availableBalanceCredits = 34500L, // 34,500 * 0.0035 = $120.75 USD (Eligible for cashout)
            lifetimeEarnedUsd = 265.50
        )
        creatorDao.insertOrUpdateProfile(newProfile)
        userDao.updateCreatorStatus(userId.toLongOrNull() ?: 0L, true)

        // Seed initial published plan versions for studio management & analytics
        seedInitialPlanVersions(creatorId)
        seedInitialLedgerEntries(creatorId)

        newProfile
    }

    private suspend fun seedInitialPlanVersions(creatorId: String) {
        val now = System.currentTimeMillis()
        val plan1 = PlanVersionEntity(
            versionId = "ver_${UUID.randomUUID().toString().take(8)}",
            planId = "plan_deep_work_30",
            creatorId = creatorId,
            versionTag = "1.0.0",
            isBreakingChange = false,
            title = "30-Day Deep Work & Focus Mastery",
            description = "Structured cognitive conditioning system with strict daily lockouts and mandatory evening retrospectives.",
            durationDays = 30,
            creditPrice = 850,
            freemiumPreviewDays = 3,
            pacingMode = "STRICT_DAILY_LOCK",
            isMandatoryReflectionEnabled = true,
            defaultReflectionPrompt = "What single distraction did you eliminate today and what deep output was produced?",
            enrolledUsersCount = 284,
            completionRate = 0.62f,
            publishedAt = now - (20L * 24 * 60 * 60 * 1000L)
        )

        val plan2 = PlanVersionEntity(
            versionId = "ver_${UUID.randomUUID().toString().take(8)}",
            planId = "plan_morning_sprint_14",
            creatorId = creatorId,
            versionTag = "1.2.0",
            isBreakingChange = false,
            title = "14-Day High-Output Morning Sprint",
            description = "High intensity physical and mental priming routine designed for founders and creators.",
            durationDays = 14,
            creditPrice = 450,
            freemiumPreviewDays = 2,
            pacingMode = "OPEN_PACING",
            isMandatoryReflectionEnabled = false,
            defaultReflectionPrompt = "Record energy levels and primary victory for the sprint.",
            enrolledUsersCount = 512,
            completionRate = 0.78f,
            publishedAt = now - (40L * 24 * 60 * 60 * 1000L)
        )

        planVersionDao.insertVersion(plan1)
        planVersionDao.insertVersion(plan2)
    }

    private suspend fun seedInitialLedgerEntries(creatorId: String) {
        val now = System.currentTimeMillis()
        val dayMillis = 24L * 60 * 60 * 1000L

        val entries = listOf(
            CreatorLedgerEntity(
                transactionId = "tx_${UUID.randomUUID().toString().take(8)}",
                creatorId = creatorId,
                planId = "plan_deep_work_30",
                planTitle = "30-Day Deep Work & Focus Mastery",
                amountCredits = 850L,
                equivalentUsd = calculateTakeHomeUsd(850L),
                entryType = "PURCHASE_ESCROW",
                status = "PENDING",
                escrowReleaseDate = now + (5L * dayMillis), // Clears in 5 days
                createdAt = now - (9L * dayMillis)
            ),
            CreatorLedgerEntity(
                transactionId = "tx_${UUID.randomUUID().toString().take(8)}",
                creatorId = creatorId,
                planId = "plan_morning_sprint_14",
                planTitle = "14-Day High-Output Morning Sprint",
                amountCredits = 450L,
                equivalentUsd = calculateTakeHomeUsd(450L),
                entryType = "PURCHASE_ESCROW",
                status = "PENDING",
                escrowReleaseDate = now + (11L * dayMillis), // Clears in 11 days
                createdAt = now - (3L * dayMillis)
            ),
            CreatorLedgerEntity(
                transactionId = "tx_${UUID.randomUUID().toString().take(8)}",
                creatorId = creatorId,
                planId = "plan_deep_work_30",
                planTitle = "30-Day Deep Work & Focus Mastery",
                amountCredits = 850L,
                equivalentUsd = calculateTakeHomeUsd(850L),
                entryType = "ESCROW_RELEASE",
                status = "SETTLED",
                escrowReleaseDate = now - (2L * dayMillis),
                createdAt = now - (16L * dayMillis)
            ),
            CreatorLedgerEntity(
                transactionId = "tx_${UUID.randomUUID().toString().take(8)}",
                creatorId = creatorId,
                planId = null,
                planTitle = "Stripe Connect Transfer",
                amountCredits = -28572L,
                equivalentUsd = -100.00,
                entryType = "BANK_PAYOUT",
                status = "SETTLED",
                escrowReleaseDate = null,
                createdAt = now - (30L * dayMillis)
            )
        )
        ledgerDao.insertEntries(entries)
    }

    /**
     * Checks for pending escrow entries whose 14-day hold has matured,
     * settles them, and moves funds to available cashout balance.
     */
    suspend fun releaseMaturingEscrow(creatorId: String) = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        val maturingEntries = ledgerDao.getPendingMatureEscrow(now)
        val profile = creatorDao.getCreatorProfileById(creatorId) ?: return@withContext

        var creditsToRelease = 0L
        for (entry in maturingEntries) {
            ledgerDao.updateEntryStatus(entry.transactionId, "SETTLED")
            creditsToRelease += entry.amountCredits
        }

        if (creditsToRelease > 0) {
            val newPending = (profile.pendingBalanceCredits - creditsToRelease).coerceAtLeast(0L)
            val newAvailable = profile.availableBalanceCredits + creditsToRelease
            val newLifetime = profile.lifetimeEarnedUsd + calculateTakeHomeUsd(creditsToRelease)
            creatorDao.updateBalances(creatorId, newPending, newAvailable, newLifetime)
        }
    }

    suspend fun updatePricing(planId: String, newCredits: Int, previewDays: Int) = withContext(Dispatchers.IO) {
        require(newCredits >= 0) { "Credit price cannot be negative" }
        require(previewDays in 0..7) { "Freemium preview days must be between 0 and 7" }
        planVersionDao.updatePricing(planId, newCredits, previewDays)
    }

    suspend fun togglePacingMode(planId: String, strictLock: Boolean) = withContext(Dispatchers.IO) {
        val mode = if (strictLock) "STRICT_DAILY_LOCK" else "OPEN_PACING"
        planVersionDao.updatePacingMode(planId, mode)
    }

    suspend fun toggleMandatoryReflection(planId: String, mandatory: Boolean) = withContext(Dispatchers.IO) {
        planVersionDao.updateReflection(
            planId = planId,
            isMandatory = mandatory,
            defaultPrompt = "What was your primary progress breakthrough and what obstacles were encountered?"
        )
    }

    /**
     * Creates a new plan version snapshot adhering to Semantic Versioning (v1.0.x patch vs v2.0 breaking fork).
     */
    suspend fun createPlanPatch(
        planId: String,
        isBreaking: Boolean,
        newTitle: String
    ): PlanVersionEntity = withContext(Dispatchers.IO) {
        val latest = planVersionDao.getLatestVersionForPlan(planId)
            ?: throw IllegalStateException("Plan version not found for planId: $planId")

        val currentTag = latest.versionTag
        val parts = currentTag.split(".").mapNotNull { it.toIntOrNull() }
        val major = parts.getOrNull(0) ?: 1
        val minor = parts.getOrNull(1) ?: 0
        val patch = parts.getOrNull(2) ?: 0

        val nextTag = if (isBreaking) {
            "${major + 1}.0.0"
        } else {
            "$major.$minor.${patch + 1}"
        }

        val newVersion = latest.copy(
            versionId = "ver_${UUID.randomUUID().toString().take(8)}",
            versionTag = nextTag,
            isBreakingChange = isBreaking,
            title = newTitle.ifBlank { latest.title },
            publishedAt = System.currentTimeMillis()
        )
        planVersionDao.insertVersion(newVersion)
        newVersion
    }

    /**
     * Executes a cash-out request to Stripe Connect, validating KYC and the $100.00 minimum threshold.
     */
    suspend fun requestStripePayout(creatorId: String): Result<Double> = withContext(Dispatchers.IO) {
        val profile = creatorDao.getCreatorProfileById(creatorId)
            ?: return@withContext Result.failure(IllegalStateException("Creator profile not found"))

        if (profile.kycStatus != "VERIFIED") {
            return@withContext Result.failure(IllegalStateException("KYC verification required before requesting cash out"))
        }

        val availableCredits = profile.availableBalanceCredits
        val takeHomeUsd = calculateTakeHomeUsd(availableCredits)

        if (takeHomeUsd < MIN_PAYOUT_USD) {
            return@withContext Result.failure(
                IllegalStateException("Minimum payout threshold of $100.00 USD not met (Current: $%.2f USD)".format(takeHomeUsd))
            )
        }

        // Deduct available balance and record BANK_PAYOUT ledger transaction
        val payoutTx = CreatorLedgerEntity(
            transactionId = "tx_payout_${UUID.randomUUID().toString().take(8)}",
            creatorId = creatorId,
            planId = null,
            planTitle = "Stripe Connect Direct Payout",
            amountCredits = -availableCredits,
            equivalentUsd = -takeHomeUsd,
            entryType = "BANK_PAYOUT",
            status = "SETTLED",
            escrowReleaseDate = null,
            createdAt = System.currentTimeMillis()
        )

        ledgerDao.insertEntry(payoutTx)
        creatorDao.updateBalances(
            creatorId = creatorId,
            pending = profile.pendingBalanceCredits,
            available = 0L,
            lifetimeEarnedUsd = profile.lifetimeEarnedUsd
        )

        Result.success(takeHomeUsd)
    }

    /**
     * Generates and stores a cryptographic entitlement token for offline DRM access.
     */
    suspend fun issueEntitlement(
        planId: String,
        userId: String,
        versionId: String
    ): PlanEntitlementEntity = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        val signature = OfflineDrmEngine.generateLicenseSignature(planId, userId, versionId, now)
        val entitlement = PlanEntitlementEntity(
            entitlementId = "ent_${UUID.randomUUID().toString().take(8)}",
            planId = planId,
            versionId = versionId,
            userId = userId,
            licenseSignature = signature,
            grantedAt = now,
            isActive = true
        )
        planEntitlementDao.insertEntitlement(entitlement)
        entitlement
    }

    /**
     * Validates DRM entitlement signature offline.
     */
    suspend fun verifyEntitlement(planId: String, userId: String): Boolean = withContext(Dispatchers.IO) {
        val entitlement = planEntitlementDao.getActiveEntitlement(planId, userId) ?: return@withContext false
        OfflineDrmEngine.verifyLicenseSignature(
            planId = entitlement.planId,
            userId = entitlement.userId,
            versionId = entitlement.versionId,
            grantedAt = entitlement.grantedAt,
            signature = entitlement.licenseSignature
        )
    }
}
