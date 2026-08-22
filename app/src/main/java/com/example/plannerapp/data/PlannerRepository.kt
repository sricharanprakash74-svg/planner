package com.example.plannerapp.data

import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

class PlannerRepository(private val dao: PlannerDao) {

    private val dateFormatter = DateTimeFormatter.ISO_LOCAL_DATE

    fun getTasksForDate(date: LocalDate): Flow<List<DailyTaskView>> {
        return dao.getTasksForDate(date.format(dateFormatter))
    }

    fun getTasksForPlanAndDate(planId: Long, date: LocalDate): Flow<List<DailyTaskView>> {
        return dao.getTasksForPlanAndDate(planId, date.format(dateFormatter))
    }

    suspend fun updateTaskStatus(checkinId: Long, isCompleted: Boolean) {
        val now = ZonedDateTime.now()
        val offset = now.offset.id  // e.g., "+05:30"
        dao.updateCheckinStatus(
            checkinId = checkinId,
            isCompleted = isCompleted,
            completedAt = if (isCompleted) System.currentTimeMillis() else null,
            timezoneOffset = offset
        )
    }

    suspend fun updateCheckinAndSubtasksStatus(checkinId: Long, isCompleted: Boolean, completedSubtasks: String) {
        val now = ZonedDateTime.now()
        val offset = now.offset.id
        dao.updateCheckinAndSubtasksStatus(
            checkinId = checkinId,
            isCompleted = isCompleted,
            completedSubtasks = completedSubtasks,
            completedAt = if (isCompleted) System.currentTimeMillis() else null,
            timezoneOffset = offset
        )
    }

    suspend fun getStreak(userId: Long, currentDate: LocalDate): Int {
        val pastCheckins = dao.getPastCheckins(userId, currentDate.format(dateFormatter))
        if (pastCheckins.isEmpty()) return 0

        val checkinsByDate = pastCheckins.groupBy { it.exactDate }.toSortedMap(reverseOrder())

        var streak = 0
        for ((_, dailyTasks) in checkinsByDate) {
            val allCompleted = dailyTasks.all { it.isCompleted }
            if (allCompleted && dailyTasks.isNotEmpty()) {
                streak++
            } else {
                break
            }
        }
        return streak
    }

    fun getPlansForUser(userId: Long): Flow<List<PlanEntity>> {
        return dao.getPlansForUser(userId)
    }

    fun getWeeklyCheckins(userId: Long): Flow<List<DailyCheckinEntity>> {
        val today = LocalDate.now()
        val startDate = today.minusDays(6).format(dateFormatter) // Last 7 days
        val endDate = today.format(dateFormatter)
        return dao.getCheckinsBetweenDates(userId, startDate, endDate)
    }

    suspend fun createFullPlan(plan: PlanEntity, templatesWithCheckins: Map<TaskTemplateEntity, List<DailyCheckinEntity>>) {
        dao.createFullPlan(plan, templatesWithCheckins)
    }

    suspend fun addTaskToPlan(template: TaskTemplateEntity, checkin: DailyCheckinEntity) {
        val templateId = dao.insertTaskTemplate(template)
        dao.insertDailyCheckins(listOf(checkin.copy(templateId = templateId)))
    }

    // Temporary function to populate DB so we can see UI working
    suspend fun populateDummyDataIfEmpty(userId: Long) {
        val today = LocalDate.now()
        val todayStr = today.format(dateFormatter)

        val plan = PlanEntity(
            planId = 1,
            userId = userId,
            heading = "Morning Routine",
            description = "Start the day right.",
            startDate = today.minusDays(7).format(dateFormatter),
            endDate = today.plusDays(7).format(dateFormatter)
        )

        val template1 = TaskTemplateEntity(templateId = 1, planId = 1, taskDescription = "Drink Water", selectedDays = "1,2,3,4,5,6,7")
        val template2 = TaskTemplateEntity(templateId = 2, planId = 1, taskDescription = "Read 10 Pages", selectedDays = "1,2,3,4,5,6,7")

        val checkins = mutableListOf<DailyCheckinEntity>()

        var idCounter = 1L
        for (i in -7..7) {
            val date = today.plusDays(i.toLong())
            val dateStr = date.format(dateFormatter)
            val isComplete = i < 0

            checkins.add(DailyCheckinEntity(checkinId = idCounter++, templateId = 1, exactDate = dateStr, isCompleted = isComplete))
            checkins.add(DailyCheckinEntity(checkinId = idCounter++, templateId = 2, exactDate = dateStr, isCompleted = isComplete))
        }

        dao.createFullPlan(
            plan,
            mapOf(
                template1 to checkins.filter { it.templateId == 1L },
                template2 to checkins.filter { it.templateId == 2L }
            )
        )
    }
}
