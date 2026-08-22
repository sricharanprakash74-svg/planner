package com.example.plannerapp.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

// ── User ────────────────────────────────────────────
@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey(autoGenerate = true) val userId: Long = 0,
    val cloudUserId: String? = null,       // null = guest, non-null = authenticated
    val displayName: String = "Guest",
    val email: String? = null,
    val avatarUrl: String? = null,
    val userTimezone: String = java.util.TimeZone.getDefault().id,
    val isCreator: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

// ── Plan ────────────────────────────────────────────
@Entity(
    tableName = "plans",
    foreignKeys = [
        ForeignKey(
            entity = UserEntity::class,
            parentColumns = ["userId"],
            childColumns = ["userId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("userId")]
)
data class PlanEntity(
    @PrimaryKey(autoGenerate = true) val planId: Long = 0,
    val userId: Long = 0,
    val heading: String,
    val description: String = "",
    val startDate: String,                 // "YYYY-MM-DD"
    val endDate: String,                   // "YYYY-MM-DD"
    val isPublic: Boolean = false,         // Creator publishing flag
    val sourcePlanId: Long? = null,        // Tracks cloned-from plan (immutable clone rule)
    val upvoteCount: Int = 0,              // Cached for feed display
    val downloadCount: Int = 0,            // Cached for feed display
    val syncStatus: String = "LOCAL",      // LOCAL | PENDING | SYNCED
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

// ── Task Template ───────────────────────────────────
@Entity(
    tableName = "task_templates",
    foreignKeys = [
        ForeignKey(
            entity = PlanEntity::class,
            parentColumns = ["planId"],
            childColumns = ["planId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("planId")]
)
data class TaskTemplateEntity(
    @PrimaryKey(autoGenerate = true) val templateId: Long = 0,
    val planId: Long,
    val taskDescription: String,
    val selectedDays: String,              // e.g., "1,3,5" for Mon/Wed/Fri
    val subtasks: String = "[]",           // JSON string of List<String>
    val syncStatus: String = "LOCAL",
    val createdAt: Long = System.currentTimeMillis()
)

// ── Daily Check-in (Instance) ───────────────────────
@Entity(
    tableName = "daily_checkins",
    foreignKeys = [
        ForeignKey(
            entity = TaskTemplateEntity::class,
            parentColumns = ["templateId"],
            childColumns = ["templateId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("templateId"), Index("exactDate")]
)
data class DailyCheckinEntity(
    @PrimaryKey(autoGenerate = true) val checkinId: Long = 0,
    val templateId: Long,
    val exactDate: String,                 // "YYYY-MM-DD"
    val isCompleted: Boolean = false,
    val completedSubtasks: String = "[]",  // JSON string of List<Boolean>
    val completedAt: Long? = null,         // Epoch millis when completed
    val timezoneOffset: String = "",       // e.g., "+05:30"
    val syncStatus: String = "LOCAL"       // LOCAL | PENDING | SYNCED
)

// ── Badge (Gamification) ────────────────────────────
@Entity(
    tableName = "badges",
    foreignKeys = [
        ForeignKey(
            entity = UserEntity::class,
            parentColumns = ["userId"],
            childColumns = ["userId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("userId")]
)
data class BadgeEntity(
    @PrimaryKey(autoGenerate = true) val badgeId: Long = 0,
    val userId: Long,
    val badgeType: String,                 // e.g., "STREAK_7", "STREAK_30", "FIRST_PLAN", "CREATOR"
    val unlockedAt: Long = System.currentTimeMillis(),
    val syncStatus: String = "LOCAL"
)

// ── Plan Vote ───────────────────────────────────────
@Entity(
    tableName = "plan_votes",
    foreignKeys = [
        ForeignKey(
            entity = PlanEntity::class,
            parentColumns = ["planId"],
            childColumns = ["planId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = UserEntity::class,
            parentColumns = ["userId"],
            childColumns = ["userId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("planId"), Index("userId"), Index(value = ["planId", "userId"], unique = true)]
)
data class PlanVoteEntity(
    @PrimaryKey(autoGenerate = true) val voteId: Long = 0,
    val planId: Long,
    val userId: Long,
    val voteValue: Int,                    // +1 or -1
    val createdAt: Long = System.currentTimeMillis()
)

// ── Query Result DTOs ───────────────────────────────
data class DailyTaskView(
    val checkinId: Long,
    val taskDescription: String,
    val isCompleted: Boolean,
    val subtasks: String = "[]",
    val completedSubtasks: String = "[]"
)
