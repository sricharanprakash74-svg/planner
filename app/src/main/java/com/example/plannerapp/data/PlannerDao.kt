package com.example.plannerapp.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface PlannerDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlan(plan: PlanEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTaskTemplate(template: TaskTemplateEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDailyCheckins(checkins: List<DailyCheckinEntity>)

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

    @Query("""
        SELECT c.checkinId, t.taskDescription, c.isCompleted 
        FROM daily_checkins c
        INNER JOIN task_templates t ON c.templateId = t.templateId
        WHERE c.exactDate = :todayDate
    """)
    fun getTasksForDate(todayDate: String): Flow<List<DailyTaskView>>

    @Query("UPDATE daily_checkins SET isCompleted = :isCompleted WHERE checkinId = :checkinId")
    suspend fun updateCheckinStatus(checkinId: Long, isCompleted: Boolean)

    // Used to calculate streak: get all past instances before a specific date, ordered backwards
    @Query("""
        SELECT c.* 
        FROM daily_checkins c
        INNER JOIN task_templates t ON c.templateId = t.templateId
        WHERE c.exactDate < :currentDate
        ORDER BY c.exactDate DESC
    """)
    suspend fun getPastCheckins(currentDate: String): List<DailyCheckinEntity>
}
