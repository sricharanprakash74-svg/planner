package com.example.plannerapp.data.creator

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Entity representing a registered creator with escrow and cash-out balance tracking.
 */
@Entity(
    tableName = "creator_profiles",
    indices = [Index("userId", unique = true)]
)
data class CreatorProfileEntity(
    @PrimaryKey val creatorId: String,
    val userId: String,
    val handle: String,
    val bio: String,
    val isVerified: Boolean,
    val kycStatus: String, // "UNVERIFIED", "PENDING", "VERIFIED", "REJECTED"
    val stripeConnectedAccountId: String?,
    val pendingBalanceCredits: Long,
    val availableBalanceCredits: Long,
    val lifetimeEarnedUsd: Double
)

/**
 * Entity capturing immutable snapshots of creator plans for semantic versioning (v1.0.x vs v2.0 breaking fork).
 */
@Entity(
    tableName = "creator_plan_versions",
    foreignKeys = [
        ForeignKey(
            entity = CreatorProfileEntity::class,
            parentColumns = ["creatorId"],
            childColumns = ["creatorId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("creatorId"), Index("planId")]
)
data class PlanVersionEntity(
    @PrimaryKey val versionId: String,
    val planId: String,
    val creatorId: String,
    val versionTag: String, // e.g. "1.0.0"
    val isBreakingChange: Boolean,
    val title: String,
    val description: String,
    val durationDays: Int,
    val creditPrice: Int,
    val freemiumPreviewDays: Int, // e.g. Days 1-3 free, Day 4 locks
    val pacingMode: String, // "STRICT_DAILY_LOCK", "OPEN_PACING"
    val isMandatoryReflectionEnabled: Boolean,
    val defaultReflectionPrompt: String,
    val enrolledUsersCount: Int,
    val completionRate: Float,
    val publishedAt: Long
)

/**
 * Entity holding cryptographically signed license tokens for offline DRM entitlement verification.
 */
@Entity(
    tableName = "plan_entitlements",
    indices = [Index("planId"), Index("userId")]
)
data class PlanEntitlementEntity(
    @PrimaryKey val entitlementId: String,
    val planId: String,
    val versionId: String,
    val userId: String,
    val licenseSignature: String, // RSA/ECDSA server-signed verification token
    val grantedAt: Long,
    val isActive: Boolean
)

/**
 * Entity implementing the immutable double-entry creator settlement and escrow ledger.
 */
@Entity(
    tableName = "creator_ledger_entries",
    indices = [Index("creatorId"), Index("planId"), Index("status")]
)
data class CreatorLedgerEntity(
    @PrimaryKey val transactionId: String,
    val creatorId: String,
    val planId: String?,
    val planTitle: String?,
    val amountCredits: Long,
    val equivalentUsd: Double,
    val entryType: String, // "PURCHASE_ESCROW", "ESCROW_RELEASE", "BANK_PAYOUT", "REFUND_REVERSAL"
    val status: String, // "PENDING", "SETTLED", "FAILED"
    val escrowReleaseDate: Long?,
    val createdAt: Long
)
