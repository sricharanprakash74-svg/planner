package com.example.plannerapp.data.template

import com.example.plannerapp.data.DailyCheckinEntity
import com.example.plannerapp.data.PlanEntity
import com.example.plannerapp.data.PlannerDao
import com.example.plannerapp.data.TaskTemplateEntity
import com.google.gson.Gson
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * Service to validate, parse, and ingest incoming PlanTemplateDto instances or JSON payloads
 * into the local Room database as an independent, clean user plan.
 */
class PlanImporter(
    private val gson: Gson = Gson()
) {
    private val dateFormatter = DateTimeFormatter.ISO_LOCAL_DATE

    /**
     * Safely deserializes a JSON string into a PlanTemplateDto.
     */
    fun parseJson(jsonString: String): Result<PlanTemplateDto> {
        return try {
            val dto = gson.fromJson(jsonString, PlanTemplateDto::class.java)
                ?: return Result.failure(IllegalArgumentException("Parsed JSON yielded null template"))
            validate(dto).map { dto }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Validates that the plan template contains valid fields.
     */
    fun validate(dto: PlanTemplateDto): Result<Unit> {
        if (dto.title.isBlank()) {
            return Result.failure(IllegalArgumentException("Plan title cannot be blank"))
        }
        if (dto.targetDurationDays <= 0) {
            return Result.failure(IllegalArgumentException("Target duration must be at least 1 day"))
        }
        return Result.success(Unit)
    }

    /**
     * Ingests a PlanTemplateDto into the local Room database as a fresh personal plan.
     * Generates all daily checkins relative to the target start date.
     *
     * @param template The template data contract to import.
     * @param targetUserId The local user ID who owns the imported plan.
     * @param startDate The user's chosen start date (defaults to today).
     * @param sourcePlanId Optional ID linking back to the origin/public post ID.
     * @param dao The PlannerDao instance to perform atomic database insertion.
     * @return The local planId of the newly created plan.
     */
    suspend fun importToLocalPlan(
        template: PlanTemplateDto,
        targetUserId: Long,
        startDate: LocalDate = LocalDate.now(),
        sourcePlanId: Long? = null,
        dao: PlannerDao
    ): Long {
        validate(template).getOrThrow()

        val durationDays = template.targetDurationDays.coerceAtLeast(1)
        val endDate = startDate.plusDays((durationDays - 1).toLong())

        val planEntity = PlanEntity(
            userId = targetUserId,
            heading = template.title,
            description = template.description,
            startDate = startDate.format(dateFormatter),
            endDate = endDate.format(dateFormatter),
            defaultTaskDurationDays = template.defaultTaskDurationDays.coerceAtLeast(1),
            isPinned = false,
            isPublic = false,
            sourcePlanId = sourcePlanId,
            syncStatus = "LOCAL"
        )

        val templatesWithCheckins = mutableMapOf<TaskTemplateEntity, List<DailyCheckinEntity>>()

        template.tasks.forEach { taskDto ->
            val subtasksJson = gson.toJson(taskDto.subtasks)
            val subtaskCount = taskDto.subtasks.size
            val defaultCompletedSubtasks = gson.toJson(List(subtaskCount) { false })

            val taskTemplateEntity = TaskTemplateEntity(
                planId = 0, // Will be assigned during transaction
                taskDescription = taskDto.taskDescription,
                selectedDays = taskDto.selectedDays.ifBlank { "1,2,3,4,5,6,7" },
                durationDays = taskDto.durationDays.coerceAtLeast(1),
                subtasks = subtasksJson,
                syncStatus = "LOCAL"
            )

            // Parse active days of the week (1 = Monday, 7 = Sunday)
            val activeDaysSet = taskDto.selectedDays
                .split(",")
                .mapNotNull { it.trim().toIntOrNull() }
                .toSet()

            val checkins = mutableListOf<DailyCheckinEntity>()

            // Generate daily check-ins across the duration of the plan
            for (offset in 0 until durationDays) {
                val currentDate = startDate.plusDays(offset.toLong())
                val dayOfWeekValue = currentDate.dayOfWeek.value // 1 (Mon) to 7 (Sun)

                val shouldInclude = if (activeDaysSet.isEmpty()) {
                    true
                } else {
                    activeDaysSet.contains(dayOfWeekValue)
                }

                if (shouldInclude) {
                    checkins.add(
                        DailyCheckinEntity(
                            templateId = 0, // Assigned in transaction
                            exactDate = currentDate.format(dateFormatter),
                            isCompleted = false,
                            completedSubtasks = defaultCompletedSubtasks,
                            syncStatus = "LOCAL"
                        )
                    )
                }
            }

            templatesWithCheckins[taskTemplateEntity] = checkins
        }

        return dao.createFullPlan(planEntity, templatesWithCheckins)
    }
}
