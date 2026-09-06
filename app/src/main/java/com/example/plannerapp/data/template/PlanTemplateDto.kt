package com.example.plannerapp.data.template

import com.google.gson.annotations.SerializedName
import kotlinx.serialization.Serializable
import java.util.UUID

/**
 * Data contract for serialized, standalone plan templates.
 * Used for exporting local plans to the community feed and importing/forking public plans into local Room DB.
 */
@Serializable
data class PlanTemplateDto(
    @SerializedName("version") val version: Int = 1,
    @SerializedName("templateId") val templateId: String = UUID.randomUUID().toString(),
    @SerializedName("title") val title: String,
    @SerializedName("description") val description: String = "",
    @SerializedName("targetDurationDays") val targetDurationDays: Int = 7,
    @SerializedName("defaultTaskDurationDays") val defaultTaskDurationDays: Int = 1,
    @SerializedName("tags") val tags: List<String> = emptyList(),
    @SerializedName("category") val category: String = "General",
    @SerializedName("author") val author: AuthorDto,
    @SerializedName("tasks") val tasks: List<TaskTemplateDto> = emptyList(),
    @SerializedName("createdAt") val createdAt: Long = System.currentTimeMillis()
)

@Serializable
data class AuthorDto(
    @SerializedName("userId") val userId: String,
    @SerializedName("displayName") val displayName: String,
    @SerializedName("avatarUrl") val avatarUrl: String? = null,
    @SerializedName("isCreator") val isCreator: Boolean = false
)

@Serializable
data class TaskTemplateDto(
    @SerializedName("taskDescription") val taskDescription: String,
    @SerializedName("selectedDays") val selectedDays: String = "1,2,3,4,5,6,7", // Days of week (1=Mon..7=Sun)
    @SerializedName("durationDays") val durationDays: Int = 1,
    @SerializedName("subtasks") val subtasks: List<String> = emptyList(),
    @SerializedName("startDayOffset") val startDayOffset: Int = 0 // 0-indexed relative day offset from plan start
)
