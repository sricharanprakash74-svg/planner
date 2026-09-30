package com.example.plannerapp.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
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

    suspend fun getTasksForPlanAndDateOnce(planId: Long, date: LocalDate): List<DailyTaskView> {
        return dao.getTasksForPlanAndDateOnce(planId, date.format(dateFormatter))
    }

    suspend fun getTasksForPlanAndDateOnce(planId: Long, dateStr: String): List<DailyTaskView> {
        return dao.getTasksForPlanAndDateOnce(planId, dateStr)
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

    suspend fun setPlanPublicStatus(planId: Long, isPublic: Boolean) {
        val plan = dao.getPlanById(planId) ?: return
        dao.updatePlan(plan.copy(isPublic = isPublic, updatedAt = System.currentTimeMillis(), syncStatus = "PENDING"))
    }

    suspend fun getTaskTemplatesForPlan(planId: Long): List<TaskTemplateEntity> {
        return dao.getTemplatesForPlan(planId)
    }

    suspend fun deletePlan(planId: Long) {
        // Soft-delete first so sync worker can queue a DELETE outbox event while remoteId is still readable
        dao.markPlanDeleted(planId)
        dao.deletePlan(planId)
    }

    suspend fun deletePlans(planIds: List<Long>) {
        planIds.forEach { dao.markPlanDeleted(it) }
        dao.deletePlans(planIds)
    }

    suspend fun updateTask(templateId: Long, description: String, durationDays: Int, subtasks: String) {
        dao.updateTaskTemplate(templateId, description, durationDays, subtasks)
    }

    suspend fun deleteTask(templateId: Long) {
        // Soft-delete task first
        dao.markTemplateDeleted(templateId)
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

    suspend fun getPlanById(planId: Long): PlanEntity? = dao.getPlanById(planId)

    suspend fun insertDailyCheckins(checkins: List<DailyCheckinEntity>) = dao.insertDailyCheckins(checkins)

    suspend fun repairMissingCheckinsForPlan(planId: Long) {
        try {
            val plan = dao.getPlanById(planId) ?: return
            val templates = dao.getTemplatesForPlan(planId)
            if (templates.isEmpty()) return

            val start = try { LocalDate.parse(plan.startDate, dateFormatter) } catch (e: Exception) { return }
            val end = try { LocalDate.parse(plan.endDate, dateFormatter) } catch (e: Exception) { start.plusDays(6) }
            val planDaysCount = (java.time.temporal.ChronoUnit.DAYS.between(start, end) + 1).coerceAtLeast(1).toInt()

            val existingCheckins = getAllCheckinsForPlan(planId).first()
            val checkinsByTemplate = existingCheckins.groupBy { it.templateId }

            val newCheckins = mutableListOf<DailyCheckinEntity>()
            val gson = com.google.gson.Gson()

            templates.forEach { template ->
                val templateCheckins = checkinsByTemplate[template.templateId] ?: emptyList()
                val existingDates = templateCheckins.map { it.exactDate }.toSet()

                val numSubtasks = try {
                    val listType = object : com.google.gson.reflect.TypeToken<List<String>>() {}.type
                    (gson.fromJson<List<String>>(template.subtasks, listType) ?: emptyList()).size
                } catch (e: Exception) { 0 }
                val defaultCompletedSubtasks = gson.toJson(List(numSubtasks) { false })

                val taskDuration = template.durationDays.coerceAtLeast(1)
                val activeDaysSet = template.selectedDays
                    .split(",")
                    .mapNotNull { it.trim().toIntOrNull() }
                    .toSet()

                val isFullDuration = taskDuration >= planDaysCount || taskDuration >= 7 || activeDaysSet.size >= 7 || activeDaysSet.isEmpty()
                val isSingleDayBug = activeDaysSet.size == 1 && taskDuration > 1

                for (offset in 0 until planDaysCount) {
                    val date = start.plusDays(offset.toLong())
                    val dateStr = date.format(dateFormatter)

                    if (dateStr in existingDates) continue

                    val shouldAdd = if (isFullDuration || isSingleDayBug) {
                        offset < taskDuration
                    } else if (activeDaysSet.size in 2..6) {
                        activeDaysSet.contains(date.dayOfWeek.value)
                    } else {
                        activeDaysSet.contains(date.dayOfWeek.value)
                    }

                    if (shouldAdd) {
                        newCheckins.add(
                            DailyCheckinEntity(
                                templateId = template.templateId,
                                exactDate = dateStr,
                                isCompleted = false,
                                completedSubtasks = defaultCompletedSubtasks,
                                syncStatus = "LOCAL"
                            )
                        )
                    }
                }

                // If template has NO checkins at all, add for all days up to duration
                if (templateCheckins.isEmpty() && newCheckins.none { it.templateId == template.templateId }) {
                    for (offset in 0 until taskDuration.coerceAtMost(planDaysCount)) {
                        val date = start.plusDays(offset.toLong())
                        val dateStr = date.format(dateFormatter)
                        if (dateStr !in existingDates) {
                            newCheckins.add(
                                DailyCheckinEntity(
                                    templateId = template.templateId,
                                    exactDate = dateStr,
                                    isCompleted = false,
                                    completedSubtasks = defaultCompletedSubtasks,
                                    syncStatus = "LOCAL"
                                )
                            )
                        }
                    }
                }
            }

            if (newCheckins.isNotEmpty()) {
                dao.insertDailyCheckins(newCheckins)
            }
        } catch (e: Exception) {
            // Graceful fallback
        }
    }

    suspend fun joinCommunityPlan(
        postId: String,
        template: com.example.plannerapp.data.template.PlanTemplateDto,
        targetUserId: Long,
        startDate: LocalDate = LocalDate.now(),
        socialRepository: com.example.plannerapp.data.social.SocialRepository? = null,
        creditRepository: com.example.plannerapp.credits.CreditRepository? = null
    ): Result<Long> {
        return try {
            // Enforce one join per user/post: return existing plan if already joined
            val existing = dao.getJoinedCommunityByPostId(postId)
            if (existing != null) {
                val existingPlan = dao.getPlanById(existing.localPlanId)
                if (existingPlan != null && existingPlan.userId == targetUserId) {
                    repairMissingCheckinsForPlan(existingPlan.planId)
                    return Result.success(existing.localPlanId)
                }
            }

            val importer = com.example.plannerapp.data.template.PlanImporter()
            val newLocalPlanId = importer.importToLocalPlan(
                template = template,
                targetUserId = targetUserId,
                startDate = startDate,
                sourcePlanId = stableStringHash64(postId),
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

            // Award viral clone bonus (+100 credits) to creator of shared plan
            val creatorId = template.author.userId.toLongOrNull()
            if (creatorId != null && creatorId > 0 && creatorId != targetUserId) {
                creditRepository?.awardViralCloneBonus(
                    creatorUserId = creatorId,
                    joinerUserId = targetUserId,
                    postId = postId
                )
            }

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

    suspend fun insertJoinedCommunity(joined: JoinedCommunityEntity): Long {
        return dao.insertJoinedCommunity(joined)
    }

    suspend fun getTemplatesForPlan(planId: Long): List<TaskTemplateEntity> {
        return dao.getTemplatesForPlan(planId)
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

    /**
     * Returns a [Flow] of plan completion fraction in [0.0, 1.0] for [planId].
     *
     * This is the single source of truth for the fluid level shown on the
     * FluidProgressCard. It is backed by the Room [daily_checkins] table and
     * therefore behaves identically offline and online:
     *
     *  - Offline: when the user checks off a task locally, Room writes the row,
     *    this Flow emits a new fraction, and the fluid level rises immediately.
     *    No network required.
     *
     *  - Online (after sync): the background sync worker writes the server's
     *    authoritative completion state into the same Room tables. This Flow
     *    emits again and the fluid level reflects the reconciled state — without
     *    any visual jump, because the spring animation absorbs small delta changes.
     *
     * The UI layer (FluidProgressCard) never needs to distinguish between the
     * two modes because it only observes this Flow.
     */
    fun getPlanCompletionFraction(planId: Long): Flow<Float> {
        return dao.getAllCheckinsForPlan(planId)
            .map { checkins ->
                if (checkins.isEmpty()) 0f
                else checkins.count { it.isCompleted }.toFloat() / checkins.size.toFloat()
            }
    }

    companion object {
        /**
         * BUG-11 fix: Derives a collision-resistant 64-bit Long ID from any String (UUID or custom).
         * If the string is a valid UUID, XORs the MSB and LSB. Otherwise, computes a 64-bit FNV-1a hash.
         */
        fun stableStringHash64(input: String): Long {
            return try {
                val uuid = java.util.UUID.fromString(input)
                uuid.mostSignificantBits xor uuid.leastSignificantBits
            } catch (_: Exception) {
                var hash = -0x2b467e45218d6a8bL // 0xcbf29ce484222325L
                for (ch in input) {
                    hash = hash xor ch.code.toLong()
                    hash = hash * 0x100000001b3L
                }
                hash
            }
        }
    }
}
