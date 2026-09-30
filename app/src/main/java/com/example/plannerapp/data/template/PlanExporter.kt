package com.example.plannerapp.data.template

import com.example.plannerapp.data.DailyCheckinEntity
import com.example.plannerapp.data.PlanEntity
import com.example.plannerapp.data.TaskTemplateEntity
import com.example.plannerapp.data.UserEntity
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import java.time.LocalDate
import java.time.temporal.ChronoUnit

/**
 * Service to serialize an active local Room plan and its task templates into a standalone PlanTemplateDto.
 */
class PlanExporter(
    private val gson: Gson = GsonBuilder().setPrettyPrinting().create()
) {

    /**
     * Converts a local PlanEntity and its task templates into a PlanTemplateDto.
     */
    fun exportPlan(
        plan: PlanEntity,
        templates: List<TaskTemplateEntity>,
        author: UserEntity,
        tags: List<String> = emptyList(),
        category: String = "Productivity",
        checkins: List<DailyCheckinEntity> = emptyList()
    ): PlanTemplateDto {
        val totalDays = try {
            val start = LocalDate.parse(plan.startDate)
            val end = LocalDate.parse(plan.endDate)
            (ChronoUnit.DAYS.between(start, end) + 1).coerceAtLeast(1).toInt()
        } catch (e: Exception) {
            7
        }

        val authorDto = AuthorDto(
            userId = author.cloudUserId ?: author.userId.toString(),
            displayName = author.displayName,
            avatarUrl = author.avatarUrl,
            isCreator = author.isCreator
        )

        val planStart = try { LocalDate.parse(plan.startDate) } catch (e: Exception) { LocalDate.now() }

        val taskDtos = templates.map { template ->
            val subtasksList: List<String> = try {
                val listType = object : TypeToken<List<String>>() {}.type
                gson.fromJson(template.subtasks, listType) ?: emptyList()
            } catch (e: Exception) {
                emptyList()
            }

            val templateCheckins = checkins.filter { it.templateId == template.templateId }
            val checkinOffsets = templateCheckins.mapNotNull {
                try {
                    val d = LocalDate.parse(it.exactDate)
                    ChronoUnit.DAYS.between(planStart, d).toInt()
                } catch (e: Exception) { null }
            }.sorted()

            val startDayOffset = checkinOffsets.firstOrNull()?.coerceAtLeast(0) ?: 0
            val taskDuration = template.durationDays.coerceAtLeast(checkinOffsets.size).coerceAtLeast(1)

            val activeDaysSet = template.selectedDays
                .split(",")
                .mapNotNull { it.trim().toIntOrNull() }
                .toSet()

            val isFullOrBug = taskDuration >= totalDays || taskDuration >= 7 || (activeDaysSet.size == 1 && taskDuration > 1) || activeDaysSet.isEmpty() || activeDaysSet.size >= 7 || checkinOffsets.size >= totalDays

            val normalizedDays = if (isFullOrBug) {
                "1,2,3,4,5,6,7"
            } else if (activeDaysSet.size in 2..6) {
                template.selectedDays
            } else {
                "1,2,3,4,5,6,7"
            }

            TaskTemplateDto(
                taskDescription = template.taskDescription,
                selectedDays = normalizedDays,
                durationDays = taskDuration,
                subtasks = subtasksList,
                startDayOffset = startDayOffset
            )
        }

        return PlanTemplateDto(
            version = 1,
            title = plan.heading,
            description = plan.description,
            targetDurationDays = totalDays,
            defaultTaskDurationDays = plan.defaultTaskDurationDays,
            tags = tags,
            category = category,
            author = authorDto,
            tasks = taskDtos,
            createdAt = System.currentTimeMillis()
        )
    }

    /**
     * Exports a plan directly to a formatted JSON string.
     */
    fun exportPlanToJson(
        plan: PlanEntity,
        templates: List<TaskTemplateEntity>,
        author: UserEntity,
        tags: List<String> = emptyList(),
        category: String = "Productivity",
        checkins: List<DailyCheckinEntity> = emptyList()
    ): String {
        val dto = exportPlan(plan, templates, author, tags, category, checkins)
        return gson.toJson(dto)
    }

    /**
     * Converts any PlanTemplateDto to JSON string.
     */
    fun toJson(templateDto: PlanTemplateDto): String {
        return gson.toJson(templateDto)
    }
}
