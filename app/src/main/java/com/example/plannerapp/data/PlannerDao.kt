package com.example.plannerapp.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface PlannerDao {

    // ── Plan CRUD ───────────────────────────────────
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlan(plan: PlanEntity): Long

    @Update
    suspend fun updatePlan(plan: PlanEntity)

    @Query("SELECT * FROM plans WHERE userId = :userId ORDER BY isPinned DESC, createdAt DESC")
    fun getPlansForUser(userId: Long): Flow<List<PlanEntity>>

    @Query("SELECT * FROM plans WHERE planId = :planId")
    fun getPlanFlow(planId: Long): Flow<PlanEntity?>

    @Query("SELECT * FROM plans WHERE planId = :planId LIMIT 1")
    suspend fun getPlanById(planId: Long): PlanEntity?

    @Query("UPDATE plans SET heading = :heading, description = :description, startDate = :startDate, endDate = :endDate, defaultTaskDurationDays = :defaultTaskDurationDays, reminderEnabled = :reminderEnabled, reminderTime = :reminderTime, updatedAt = :updatedAt, syncStatus = 'PENDING' WHERE planId = :planId")
    suspend fun updatePlanDetails(
        planId: Long,
        heading: String,
        description: String,
        startDate: String,
        endDate: String,
        defaultTaskDurationDays: Int,
        reminderEnabled: Boolean = false,
        reminderTime: String? = "08:00",
        updatedAt: Long = System.currentTimeMillis()
    )

    @Query("UPDATE plans SET isPinned = :isPinned WHERE planId = :planId")
    suspend fun updatePlanPinStatus(planId: Long, isPinned: Boolean)

    @Query("UPDATE plans SET isPinned = :isPinned WHERE planId IN (:planIds)")
    suspend fun updatePlansPinStatus(planIds: List<Long>, isPinned: Boolean)

    @Query("DELETE FROM plans WHERE planId = :planId")
    suspend fun deletePlan(planId: Long)

    @Query("DELETE FROM plans WHERE planId IN (:planIds)")
    suspend fun deletePlans(planIds: List<Long>)

    @Query("SELECT * FROM plans WHERE reminderEnabled = 1")
    suspend fun getPlansWithRemindersEnabled(): List<PlanEntity>

    // ── Template CRUD ───────────────────────────────
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTaskTemplate(template: TaskTemplateEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTaskTemplates(templates: List<TaskTemplateEntity>): List<Long>

    @Query("UPDATE task_templates SET taskDescription = :taskDescription, durationDays = :durationDays, subtasks = :subtasks, syncStatus = 'PENDING' WHERE templateId = :templateId")
    suspend fun updateTaskTemplate(templateId: Long, taskDescription: String, durationDays: Int, subtasks: String)

    @Query("DELETE FROM task_templates WHERE templateId = :templateId")
    suspend fun deleteTaskTemplate(templateId: Long)

    @Query("DELETE FROM daily_checkins WHERE templateId = :templateId")
    suspend fun deleteCheckinsForTemplate(templateId: Long)

    @Query("SELECT * FROM task_templates WHERE planId = :planId")
    suspend fun getTemplatesForPlan(planId: Long): List<TaskTemplateEntity>

    // ── Checkin CRUD ────────────────────────────────
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDailyCheckins(checkins: List<DailyCheckinEntity>)

    @Query("""
        SELECT c.checkinId, t.templateId, t.taskDescription, c.isCompleted, t.durationDays, t.subtasks, c.completedSubtasks 
        FROM daily_checkins c
        INNER JOIN task_templates t ON c.templateId = t.templateId
        WHERE c.exactDate = :todayDate
    """)
    fun getTasksForDate(todayDate: String): Flow<List<DailyTaskView>>

    @Query("""
        SELECT c.checkinId, t.templateId, t.taskDescription, c.isCompleted, t.durationDays, t.subtasks, c.completedSubtasks 
        FROM daily_checkins c
        INNER JOIN task_templates t ON c.templateId = t.templateId
        WHERE c.exactDate = :exactDate AND t.planId = :planId
    """)
    fun getTasksForPlanAndDate(planId: Long, exactDate: String): Flow<List<DailyTaskView>>

    @Query("""
        UPDATE daily_checkins 
        SET isCompleted = :isCompleted, 
            completedAt = :completedAt, 
            timezoneOffset = :timezoneOffset,
            syncStatus = 'PENDING'
        WHERE checkinId = :checkinId
    """)
    suspend fun updateCheckinStatus(
        checkinId: Long,
        isCompleted: Boolean,
        completedAt: Long?,
        timezoneOffset: String
    )

    @Query("""
        UPDATE daily_checkins 
        SET isCompleted = :isCompleted, 
            completedSubtasks = :completedSubtasks,
            completedAt = :completedAt, 
            timezoneOffset = :timezoneOffset,
            syncStatus = 'PENDING'
        WHERE checkinId = :checkinId
    """)
    suspend fun updateCheckinAndSubtasksStatus(
        checkinId: Long,
        isCompleted: Boolean,
        completedSubtasks: String,
        completedAt: Long?,
        timezoneOffset: String
    )

    // ── Streak Calculation ──────────────────────────
    @Query("""
        SELECT c.* 
        FROM daily_checkins c
        INNER JOIN task_templates t ON c.templateId = t.templateId
        INNER JOIN plans p ON t.planId = p.planId
        WHERE c.exactDate < :currentDate AND p.userId = :userId
        ORDER BY c.exactDate DESC
    """)
    suspend fun getPastCheckins(userId: Long, currentDate: String): List<DailyCheckinEntity>

    // ── Dashboard Stats ─────────────────────────────
    @Query("""
        SELECT c.* 
        FROM daily_checkins c
        INNER JOIN task_templates t ON c.templateId = t.templateId
        INNER JOIN plans p ON t.planId = p.planId
        WHERE p.userId = :userId AND c.exactDate >= :startDate AND c.exactDate <= :endDate
    """)
    fun getCheckinsBetweenDates(userId: Long, startDate: String, endDate: String): Flow<List<DailyCheckinEntity>>

    @Query("""
        SELECT c.* 
        FROM daily_checkins c
        INNER JOIN task_templates t ON c.templateId = t.templateId
        WHERE t.planId = :planId
    """)
    fun getAllCheckinsForPlan(planId: Long): Flow<List<DailyCheckinEntity>>

    // ── Day Completion (Sealed / Locked Day) ─────────
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDayCompletion(completion: PlanDayCompletionEntity)

    @Query("SELECT * FROM plan_day_completions WHERE planId = :planId")
    fun getPlanDayCompletions(planId: Long): Flow<List<PlanDayCompletionEntity>>

    @Query("SELECT * FROM plan_day_completions WHERE planId = :planId AND exactDate = :exactDate LIMIT 1")
    fun getDayCompletion(planId: Long, exactDate: String): Flow<PlanDayCompletionEntity?>

    @Query("UPDATE plan_day_completions SET journalNotes = :notes WHERE planId = :planId AND exactDate = :exactDate")
    suspend fun updateJournalNotes(planId: Long, exactDate: String, notes: String)

    // ── Sync Support ────────────────────────────────
    @Query("SELECT * FROM plans WHERE syncStatus = 'PENDING' OR syncStatus = 'LOCAL'")
    suspend fun getPendingSyncPlans(): List<PlanEntity>

    @Query("SELECT * FROM task_templates WHERE syncStatus = 'PENDING' OR syncStatus = 'LOCAL'")
    suspend fun getPendingSyncTemplates(): List<TaskTemplateEntity>

    @Query("SELECT * FROM daily_checkins WHERE syncStatus = 'PENDING' OR syncStatus = 'LOCAL'")
    suspend fun getPendingSyncCheckins(): List<DailyCheckinEntity>

    @Query("UPDATE daily_checkins SET syncStatus = 'SYNCED' WHERE checkinId IN (:ids)")
    suspend fun markCheckinsSynced(ids: List<Long>)

    @Query("UPDATE task_templates SET syncStatus = 'SYNCED' WHERE templateId IN (:ids)")
    suspend fun markTemplatesSynced(ids: List<Long>)

    @Query("UPDATE plans SET syncStatus = 'SYNCED' WHERE planId IN (:ids)")
    suspend fun markPlansSynced(ids: List<Long>)

    // ── Handshake (Guest → Auth Migration) ──────────
    @Query("UPDATE plans SET userId = :newUserId WHERE userId = :oldUserId")
    suspend fun migrateUserPlans(oldUserId: Long, newUserId: Long)

    // ── Full Plan Creation (Transaction) ────────────
    @Transaction
    suspend fun createFullPlan(
        plan: PlanEntity,
        templatesWithCheckins: Map<TaskTemplateEntity, List<DailyCheckinEntity>>
    ): Long {
        val newPlanId = insertPlan(plan)

        for ((template, checkins) in templatesWithCheckins) {
            val templateToInsert = template.copy(planId = newPlanId)
            val newTemplateId = insertTaskTemplate(templateToInsert)

            val checkinsToInsert = checkins.map { it.copy(templateId = newTemplateId) }
            insertDailyCheckins(checkinsToInsert)
        }
        return newPlanId
    }

    // ── Joined Communities ──────────────────────────
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertJoinedCommunity(joined: JoinedCommunityEntity): Long

    @Query("SELECT * FROM joined_communities WHERE localPlanId = :planId LIMIT 1")
    fun getJoinedCommunityForPlan(planId: Long): Flow<JoinedCommunityEntity?>

    @Query("SELECT * FROM joined_communities WHERE localPlanId = :planId LIMIT 1")
    suspend fun getJoinedCommunityForPlanOnce(planId: Long): JoinedCommunityEntity?

    @Query("SELECT * FROM joined_communities WHERE postId = :postId LIMIT 1")
    suspend fun getJoinedCommunityByPostId(postId: String): JoinedCommunityEntity?

    @Query("SELECT * FROM joined_communities WHERE postId = :postId LIMIT 1")
    fun getJoinedCommunityByPostIdFlow(postId: String): Flow<JoinedCommunityEntity?>

    @Query("SELECT * FROM joined_communities ORDER BY joinedAt DESC")
    fun getAllJoinedCommunities(): Flow<List<JoinedCommunityEntity>>

    @Query("DELETE FROM joined_communities WHERE localPlanId = :planId")
    suspend fun deleteJoinedCommunityForPlan(planId: Long)
}
