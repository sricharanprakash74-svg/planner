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

    @Query("SELECT * FROM plans WHERE userId = :userId ORDER BY createdAt DESC")
    fun getPlansForUser(userId: Long): Flow<List<PlanEntity>>

    @Query("SELECT * FROM plans WHERE planId = :planId")
    suspend fun getPlanById(planId: Long): PlanEntity?

    @Query("DELETE FROM plans WHERE planId = :planId")
    suspend fun deletePlan(planId: Long)

    // ── Template CRUD ───────────────────────────────
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTaskTemplate(template: TaskTemplateEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTaskTemplates(templates: List<TaskTemplateEntity>): List<Long>

    @Query("SELECT * FROM task_templates WHERE planId = :planId")
    suspend fun getTemplatesForPlan(planId: Long): List<TaskTemplateEntity>

    // ── Checkin CRUD ────────────────────────────────
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDailyCheckins(checkins: List<DailyCheckinEntity>)

    @Query("""
        SELECT c.checkinId, t.taskDescription, c.isCompleted, t.subtasks, c.completedSubtasks 
        FROM daily_checkins c
        INNER JOIN task_templates t ON c.templateId = t.templateId
        WHERE c.exactDate = :todayDate
    """)
    fun getTasksForDate(todayDate: String): Flow<List<DailyTaskView>>

    @Query("""
        SELECT c.checkinId, t.taskDescription, c.isCompleted, t.subtasks, c.completedSubtasks 
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
    ) {
        val newPlanId = insertPlan(plan)

        for ((template, checkins) in templatesWithCheckins) {
            val templateToInsert = template.copy(planId = newPlanId)
            val newTemplateId = insertTaskTemplate(templateToInsert)

            val checkinsToInsert = checkins.map { it.copy(templateId = newTemplateId) }
            insertDailyCheckins(checkinsToInsert)
        }
    }
}
