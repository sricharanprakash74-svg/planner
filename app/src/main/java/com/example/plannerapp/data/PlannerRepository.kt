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

    fun getPlan(planId: Long): Flow<PlanEntity?> {
        return dao.getPlanFlow(planId)
    }

    suspend fun updatePlan(
        planId: Long, 
        heading: String, 
        description: String = "", 
        startDate: String? = null, 
        endDate: String? = null, 
        defaultTaskDurationDays: Int = 1,
        reminderEnabled: Boolean = false,
        reminderTime: String? = "08:00"
    ) {
        val today = LocalDate.now().format(dateFormatter)
        val sDate = startDate ?: today
        val eDate = endDate ?: today
        dao.updatePlanDetails(planId, heading, description, sDate, eDate, defaultTaskDurationDays, reminderEnabled, reminderTime)
    }

    suspend fun createPlan(plan: PlanEntity): Long {
        return dao.insertPlan(plan)
    }

    suspend fun togglePinPlan(planId: Long, isPinned: Boolean) {
        dao.updatePlanPinStatus(planId, isPinned)
    }

    suspend fun updatePlansPinStatus(planIds: List<Long>, isPinned: Boolean) {
        dao.updatePlansPinStatus(planIds, isPinned)
    }

    suspend fun deletePlan(planId: Long) {
        dao.deletePlan(planId)
    }

    suspend fun deletePlans(planIds: List<Long>) {
        dao.deletePlans(planIds)
    }

    suspend fun updateTask(templateId: Long, description: String, durationDays: Int, subtasks: String) {
        dao.updateTaskTemplate(templateId, description, durationDays, subtasks)
    }

    suspend fun deleteTask(templateId: Long) {
        dao.deleteCheckinsForTemplate(templateId)
        dao.deleteTaskTemplate(templateId)
    }

    fun getWeeklyCheckins(userId: Long): Flow<List<DailyCheckinEntity>> {
        val today = LocalDate.now()
        val startDate = today.minusDays(6).format(dateFormatter) // Last 7 days
        val endDate = today.format(dateFormatter)
        return dao.getCheckinsBetweenDates(userId, startDate, endDate)
    }

    fun getRecentCheckins(userId: Long, days: Int = 30): Flow<List<DailyCheckinEntity>> {
        val today = LocalDate.now()
        val startDate = today.minusDays((days - 1).coerceAtLeast(0).toLong()).format(dateFormatter)
        val endDate = today.format(dateFormatter)
        return dao.getCheckinsBetweenDates(userId, startDate, endDate)
    }

    fun getAllCheckinsForPlan(planId: Long): Flow<List<DailyCheckinEntity>> {
        return dao.getAllCheckinsForPlan(planId)
    }

    fun getPlanDayCompletions(planId: Long): Flow<List<PlanDayCompletionEntity>> {
        return dao.getPlanDayCompletions(planId)
    }

    fun getDayCompletion(planId: Long, date: LocalDate): Flow<PlanDayCompletionEntity?> {
        return dao.getDayCompletion(planId, date.format(dateFormatter))
    }

    suspend fun saveJournalNotes(planId: Long, date: LocalDate, notes: String) {
        val dateStr = date.format(dateFormatter)
        val completion = PlanDayCompletionEntity(
            planId = planId,
            exactDate = dateStr,
            isDayLocked = false,
            journalNotes = notes,
            completedAt = System.currentTimeMillis()
        )
        dao.insertDayCompletion(completion)
    }

    suspend fun lockDayCompletion(
        planId: Long,
        date: LocalDate,
        completedTasksCount: Int,
        totalTasksCount: Int
    ) {
        val dateStr = date.format(dateFormatter)
        val completion = PlanDayCompletionEntity(
            planId = planId,
            exactDate = dateStr,
            isDayLocked = true,
            completedTasksCount = completedTasksCount,
            totalTasksCount = totalTasksCount,
            completedAt = System.currentTimeMillis()
        )
        dao.insertDayCompletion(completion)
    }

    suspend fun createFullPlan(plan: PlanEntity, templatesWithCheckins: Map<TaskTemplateEntity, List<DailyCheckinEntity>>): Long {
        return dao.createFullPlan(plan, templatesWithCheckins)
    }

    suspend fun addTaskToPlan(template: TaskTemplateEntity, startDate: LocalDate, durationDays: Int) {
        val templateId = dao.insertTaskTemplate(template.copy(durationDays = durationDays))
        val numSubtasks = try {
            val listType = object : com.google.gson.reflect.TypeToken<List<String>>() {}.type
            (com.google.gson.Gson().fromJson<List<String>>(template.subtasks, listType) ?: emptyList()).size
        } catch (e: Exception) { 0 }

        val checkins = (0 until durationDays).map { offset ->
            val date = startDate.plusDays(offset.toLong())
            val dateStr = date.format(dateFormatter)
            DailyCheckinEntity(
                templateId = templateId,
                exactDate = dateStr,
                isCompleted = false,
                completedSubtasks = com.google.gson.Gson().toJson(List(numSubtasks) { false })
            )
        }
        dao.insertDailyCheckins(checkins)
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

    suspend fun exportPlanTemplate(
        planId: Long,
        author: UserEntity,
        tags: List<String> = emptyList(),
        category: String = "Productivity"
    ): Result<com.example.plannerapp.data.template.PlanTemplateDto> {
        val plan = dao.getPlanById(planId) ?: return Result.failure(IllegalArgumentException("Plan not found"))
        val templates = dao.getTemplatesForPlan(planId)
        val exporter = com.example.plannerapp.data.template.PlanExporter()
        return Result.success(exporter.exportPlan(plan, templates, author, tags, category))
    }

    suspend fun importPlanTemplate(
        template: com.example.plannerapp.data.template.PlanTemplateDto,
        targetUserId: Long,
        startDate: LocalDate = LocalDate.now(),
        sourcePostId: Long? = null
    ): Result<Long> {
        return try {
            val importer = com.example.plannerapp.data.template.PlanImporter()
            val planId = importer.importToLocalPlan(template, targetUserId, startDate, sourcePostId, dao)
            Result.success(planId)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun joinCommunityPlan(
        postId: String,
        template: com.example.plannerapp.data.template.PlanTemplateDto,
        targetUserId: Long,
        startDate: LocalDate = LocalDate.now(),
        socialRepository: com.example.plannerapp.data.social.SocialRepository? = null
    ): Result<Long> {
        return try {
            val importer = com.example.plannerapp.data.template.PlanImporter()
            val newLocalPlanId = importer.importToLocalPlan(
                template = template,
                targetUserId = targetUserId,
                startDate = startDate,
                sourcePlanId = postId.hashCode().toLong(),
                dao = dao
            )

            // Record in joined_communities
            dao.insertJoinedCommunity(
                JoinedCommunityEntity(
                    localPlanId = newLocalPlanId,
                    postId = postId,
                    communityTitle = template.title,
                    creatorName = template.author.displayName
                )
            )

            // Increment remote join count
            socialRepository?.incrementJoinCount(postId)

            Result.success(newLocalPlanId)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun getJoinedCommunityForPlan(planId: Long): Flow<JoinedCommunityEntity?> {
        return dao.getJoinedCommunityForPlan(planId)
    }

    suspend fun getJoinedCommunityForPlanOnce(planId: Long): JoinedCommunityEntity? {
        return dao.getJoinedCommunityForPlanOnce(planId)
    }

    suspend fun getJoinedCommunityByPostId(postId: String): JoinedCommunityEntity? {
        return dao.getJoinedCommunityByPostId(postId)
    }

    fun getJoinedCommunityByPostIdFlow(postId: String): Flow<JoinedCommunityEntity?> {
        return dao.getJoinedCommunityByPostIdFlow(postId)
    }

    fun getAllJoinedCommunities(): Flow<List<JoinedCommunityEntity>> {
        return dao.getAllJoinedCommunities()
    }
}
