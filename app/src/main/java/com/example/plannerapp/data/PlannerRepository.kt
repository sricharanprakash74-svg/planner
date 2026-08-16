package com.example.plannerapp.data

import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class PlannerRepository(private val dao: PlannerDao) {

    private val dateFormatter = DateTimeFormatter.ISO_LOCAL_DATE

    fun getTasksForDate(date: LocalDate): Flow<List<DailyTaskView>> {
        return dao.getTasksForDate(date.format(dateFormatter))
    }

    suspend fun updateTaskStatus(checkinId: Long, isCompleted: Boolean) {
        dao.updateCheckinStatus(checkinId, isCompleted)
    }

    suspend fun getStreak(currentDate: LocalDate): Int {
        val pastCheckins = dao.getPastCheckins(currentDate.format(dateFormatter))
        if (pastCheckins.isEmpty()) return 0

        // Group by exact date string to calculate daily completion
        val checkinsByDate = pastCheckins.groupBy { it.exactDate }.toSortedMap(reverseOrder())
        
        var streak = 0
        for ((_, dailyTasks) in checkinsByDate) {
            val allCompleted = dailyTasks.all { it.isCompleted }
            if (allCompleted && dailyTasks.isNotEmpty()) {
                streak++
            } else {
                break // Streak broken
            }
        }
        return streak
    }

    // Temporary function to populate DB so we can see UI working
    suspend fun populateDummyDataIfEmpty() {
        val today = LocalDate.now()
        val todayStr = today.format(dateFormatter)
        
        // Very basic check if data exists
        // In reality, you'd check a generic count query
        // But since this is a dummy setup, we will just try to catch it or run it once.
        // For simplicity, we just check if "today" has tasks, if not, we populate.
        // Wait, let's just insert standard data and let REPLACE handle it or just do a simple check.
        
        val plan = PlanEntity(
            planId = 1,
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
            // Make past days 100% complete to show a 7-day streak
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
