package com.example.plannerapp.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

import com.example.plannerapp.credits.CreditDao
import com.example.plannerapp.credits.CreditTransactionEntity
import com.example.plannerapp.credits.StreakFreezeEntity
import com.example.plannerapp.data.creator.CreatorDao
import com.example.plannerapp.data.creator.CreatorLedgerEntity
import com.example.plannerapp.data.creator.CreatorProfileEntity
import com.example.plannerapp.data.creator.LedgerDao
import com.example.plannerapp.data.creator.PlanEntitlementDao
import com.example.plannerapp.data.creator.PlanEntitlementEntity
import com.example.plannerapp.data.creator.PlanVersionDao
import com.example.plannerapp.data.creator.PlanVersionEntity
import com.example.plannerapp.sharing.SharedPlanEntity
import com.example.plannerapp.sharing.SharingDao

@Database(
    entities = [
        UserEntity::class,
        PlanEntity::class,
        TaskTemplateEntity::class,
        DailyCheckinEntity::class,
        PlanDayCompletionEntity::class,
        BadgeEntity::class,
        PlanVoteEntity::class,
        JoinedCommunityEntity::class,
        CreditTransactionEntity::class,
        StreakFreezeEntity::class,
        SharedPlanEntity::class,
        CreatorProfileEntity::class,
        PlanVersionEntity::class,
        PlanEntitlementEntity::class,
        CreatorLedgerEntity::class,
        SyncOutboxEntity::class
    ],
    version = 12,
    exportSchema = true
)
abstract class PlannerDatabase : RoomDatabase() {

    abstract fun plannerDao(): PlannerDao
    abstract fun userDao(): UserDao
    abstract fun badgeDao(): BadgeDao
    abstract fun creditDao(): CreditDao
    abstract fun sharingDao(): SharingDao
    abstract fun creatorDao(): CreatorDao
    abstract fun planVersionDao(): PlanVersionDao
    abstract fun ledgerDao(): LedgerDao
    abstract fun planEntitlementDao(): PlanEntitlementDao

    companion object {
        val MIGRATION_8_9 = object : androidx.room.migration.Migration(8, 9) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                // Creator monetization tables introduced in version 9
                db.execSQL("CREATE TABLE IF NOT EXISTS creator_profiles (creatorId TEXT NOT NULL, userId TEXT NOT NULL, handle TEXT NOT NULL, bio TEXT NOT NULL, isVerified INTEGER NOT NULL, kycStatus TEXT NOT NULL, stripeConnectedAccountId TEXT, pendingBalanceCredits INTEGER NOT NULL, availableBalanceCredits INTEGER NOT NULL, lifetimeEarnedUsd REAL NOT NULL, PRIMARY KEY(creatorId))")
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_creator_profiles_userId ON creator_profiles (userId)")

                db.execSQL("CREATE TABLE IF NOT EXISTS creator_plan_versions (versionId TEXT NOT NULL, planId TEXT NOT NULL, creatorId TEXT NOT NULL, versionTag TEXT NOT NULL, isBreakingChange INTEGER NOT NULL, title TEXT NOT NULL, description TEXT NOT NULL, durationDays INTEGER NOT NULL, creditPrice INTEGER NOT NULL, freemiumPreviewDays INTEGER NOT NULL, pacingMode TEXT NOT NULL, isMandatoryReflectionEnabled INTEGER NOT NULL, defaultReflectionPrompt TEXT NOT NULL, enrolledUsersCount INTEGER NOT NULL, completionRate REAL NOT NULL, publishedAt INTEGER NOT NULL, PRIMARY KEY(versionId), FOREIGN KEY(creatorId) REFERENCES creator_profiles(creatorId) ON UPDATE NO ACTION ON DELETE CASCADE)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_creator_plan_versions_creatorId ON creator_plan_versions (creatorId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_creator_plan_versions_planId ON creator_plan_versions (planId)")

                db.execSQL("CREATE TABLE IF NOT EXISTS plan_entitlements (entitlementId TEXT NOT NULL, planId TEXT NOT NULL, versionId TEXT NOT NULL, userId TEXT NOT NULL, licenseSignature TEXT NOT NULL, grantedAt INTEGER NOT NULL, isActive INTEGER NOT NULL, PRIMARY KEY(entitlementId))")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_plan_entitlements_planId ON plan_entitlements (planId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_plan_entitlements_userId ON plan_entitlements (userId)")

                db.execSQL("CREATE TABLE IF NOT EXISTS creator_ledger_entries (transactionId TEXT NOT NULL, creatorId TEXT NOT NULL, planId TEXT, planTitle TEXT, amountCredits INTEGER NOT NULL, equivalentUsd REAL NOT NULL, entryType TEXT NOT NULL, status TEXT NOT NULL, escrowReleaseDate INTEGER, createdAt INTEGER NOT NULL, PRIMARY KEY(transactionId))")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_creator_ledger_entries_creatorId ON creator_ledger_entries (creatorId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_creator_ledger_entries_planId ON creator_ledger_entries (planId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_creator_ledger_entries_status ON creator_ledger_entries (status)")
            }
        }

        val MIGRATION_9_10 = object : androidx.room.migration.Migration(9, 10) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE users ADD COLUMN categories TEXT NOT NULL DEFAULT '[]'")
                db.execSQL("ALTER TABLE users ADD COLUMN interests TEXT NOT NULL DEFAULT '[]'")
                db.execSQL("ALTER TABLE users ADD COLUMN experienceLevel TEXT NOT NULL DEFAULT 'BEGINNER'")
                db.execSQL("ALTER TABLE users ADD COLUMN onboardingComplete INTEGER NOT NULL DEFAULT 1")
            }
        }

        val MIGRATION_10_11 = object : androidx.room.migration.Migration(10, 11) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                // 1. User username and onboarding cleanup
                db.execSQL("ALTER TABLE users ADD COLUMN username TEXT DEFAULT NULL")
                db.execSQL("UPDATE users SET username = categories WHERE categories != '[]' AND categories NOT LIKE '[%' AND categories IS NOT NULL")
                db.execSQL("UPDATE users SET categories = '[]' WHERE categories NOT LIKE '[%'")

                // 2. Enforce one join per postId in joined_communities
                db.execSQL("DELETE FROM joined_communities WHERE id NOT IN (SELECT MIN(id) FROM joined_communities GROUP BY postId)")
                db.execSQL("DROP INDEX IF EXISTS index_joined_communities_postId")
                db.execSQL("DROP INDEX IF EXISTS index_joined_communities_localPlanId_postId")
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_joined_communities_postId ON joined_communities (postId)")

                // 3. Credit transactions & streak freezes
                db.execSQL("CREATE TABLE IF NOT EXISTS credit_transactions (transactionId INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, userId INTEGER NOT NULL, amount INTEGER NOT NULL, transactionType TEXT NOT NULL, referenceId TEXT, description TEXT NOT NULL, createdAt INTEGER NOT NULL)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_credit_transactions_userId ON credit_transactions (userId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_credit_transactions_createdAt ON credit_transactions (createdAt)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_credit_transactions_transactionType ON credit_transactions (transactionType)")

                db.execSQL("CREATE TABLE IF NOT EXISTS streak_freezes (freezeId INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, userId INTEGER NOT NULL, acquiredAt INTEGER NOT NULL, isConsumed INTEGER NOT NULL, consumedAt INTEGER, targetDate TEXT)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_streak_freezes_userId ON streak_freezes (userId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_streak_freezes_consumedAt ON streak_freezes (consumedAt)")

                // 4. Shared plans
                db.execSQL("CREATE TABLE IF NOT EXISTS shared_plans (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, shareCode TEXT NOT NULL, originalPlanId INTEGER NOT NULL, authorUserId INTEGER NOT NULL, planTitle TEXT NOT NULL, planDescription TEXT NOT NULL, durationDays INTEGER NOT NULL, templatePayloadJson TEXT NOT NULL, cloneCount INTEGER NOT NULL, createdAt INTEGER NOT NULL)")
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_shared_plans_shareCode ON shared_plans (shareCode)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_shared_plans_originalPlanId ON shared_plans (originalPlanId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_shared_plans_authorUserId ON shared_plans (authorUserId)")

                // 5. Creator monetization & versioning
                db.execSQL("CREATE TABLE IF NOT EXISTS creator_profiles (creatorId TEXT NOT NULL, userId TEXT NOT NULL, handle TEXT NOT NULL, bio TEXT NOT NULL, isVerified INTEGER NOT NULL, kycStatus TEXT NOT NULL, stripeConnectedAccountId TEXT, pendingBalanceCredits INTEGER NOT NULL, availableBalanceCredits INTEGER NOT NULL, lifetimeEarnedUsd REAL NOT NULL, PRIMARY KEY(creatorId))")
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_creator_profiles_userId ON creator_profiles (userId)")

                db.execSQL("CREATE TABLE IF NOT EXISTS creator_plan_versions (versionId TEXT NOT NULL, planId TEXT NOT NULL, creatorId TEXT NOT NULL, versionTag TEXT NOT NULL, isBreakingChange INTEGER NOT NULL, title TEXT NOT NULL, description TEXT NOT NULL, durationDays INTEGER NOT NULL, creditPrice INTEGER NOT NULL, freemiumPreviewDays INTEGER NOT NULL, pacingMode TEXT NOT NULL, isMandatoryReflectionEnabled INTEGER NOT NULL, defaultReflectionPrompt TEXT NOT NULL, enrolledUsersCount INTEGER NOT NULL, completionRate REAL NOT NULL, publishedAt INTEGER NOT NULL, PRIMARY KEY(versionId), FOREIGN KEY(creatorId) REFERENCES creator_profiles(creatorId) ON UPDATE NO ACTION ON DELETE CASCADE)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_creator_plan_versions_creatorId ON creator_plan_versions (creatorId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_creator_plan_versions_planId ON creator_plan_versions (planId)")

                db.execSQL("CREATE TABLE IF NOT EXISTS plan_entitlements (entitlementId TEXT NOT NULL, planId TEXT NOT NULL, versionId TEXT NOT NULL, userId TEXT NOT NULL, licenseSignature TEXT NOT NULL, grantedAt INTEGER NOT NULL, isActive INTEGER NOT NULL, PRIMARY KEY(entitlementId))")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_plan_entitlements_planId ON plan_entitlements (planId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_plan_entitlements_userId ON plan_entitlements (userId)")

                db.execSQL("CREATE TABLE IF NOT EXISTS creator_ledger_entries (transactionId TEXT NOT NULL, creatorId TEXT NOT NULL, planId TEXT, planTitle TEXT, amountCredits INTEGER NOT NULL, equivalentUsd REAL NOT NULL, entryType TEXT NOT NULL, status TEXT NOT NULL, escrowReleaseDate INTEGER, createdAt INTEGER NOT NULL, PRIMARY KEY(transactionId))")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_creator_ledger_entries_creatorId ON creator_ledger_entries (creatorId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_creator_ledger_entries_planId ON creator_ledger_entries (planId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_creator_ledger_entries_status ON creator_ledger_entries (status)")
            }
        }

        val MIGRATION_11_12 = object : androidx.room.migration.Migration(11, 12) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                // 1. Add remoteId (Supabase UUID) to core planner tables
                db.execSQL("ALTER TABLE plans ADD COLUMN remoteId TEXT DEFAULT NULL")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_plans_remoteId ON plans (remoteId)")

                db.execSQL("ALTER TABLE task_templates ADD COLUMN remoteId TEXT DEFAULT NULL")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_task_templates_remoteId ON task_templates (remoteId)")

                db.execSQL("ALTER TABLE daily_checkins ADD COLUMN remoteId TEXT DEFAULT NULL")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_daily_checkins_remoteId ON daily_checkins (remoteId)")

                // 2. Create durable sync outbox table
                db.execSQL(
                    """CREATE TABLE IF NOT EXISTS sync_outbox (
                        outboxId INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        entityType TEXT NOT NULL,
                        localId INTEGER NOT NULL,
                        remoteId TEXT,
                        operation TEXT NOT NULL,
                        payloadJson TEXT,
                        retryCount INTEGER NOT NULL DEFAULT 0,
                        createdAt INTEGER NOT NULL
                    )"""
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS index_sync_outbox_entityType ON sync_outbox (entityType)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_sync_outbox_createdAt ON sync_outbox (createdAt)")
            }
        }

        @Volatile
        private var INSTANCE: PlannerDatabase? = null

        fun getDatabase(context: Context): PlannerDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    PlannerDatabase::class.java,
                    "planner_database"
                )
                .addMigrations(MIGRATION_8_9, MIGRATION_9_10, MIGRATION_10_11, MIGRATION_11_12)
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
