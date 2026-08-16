package com.example.plannerapp.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "plans")
data class PlanEntity(
    @PrimaryKey(autoGenerate = true) val planId: Long = 0,
    val heading: String,
    val description: String,
    val startDate: String, // e.g., "YYYY-MM-DD"
    val endDate: String
)

@Entity(
    tableName = "task_templates",
    foreignKeys = [
        ForeignKey(
            entity = PlanEntity::class,
            parentColumns = ["planId"],
            childColumns = ["planId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("planId")]
)
data class TaskTemplateEntity(
    @PrimaryKey(autoGenerate = true) val templateId: Long = 0,
    val planId: Long,
    val taskDescription: String,
    val selectedDays: String // e.g., "1,3,5" for Mon/Wed/Fri
)

@Entity(
    tableName = "daily_checkins",
    foreignKeys = [
        ForeignKey(
            entity = TaskTemplateEntity::class,
            parentColumns = ["templateId"],
            childColumns = ["templateId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("templateId"), Index("exactDate")]
)
data class DailyCheckinEntity(
    @PrimaryKey(autoGenerate = true) val checkinId: Long = 0,
    val templateId: Long,
    val exactDate: String, // e.g., "YYYY-MM-DD"
    val isCompleted: Boolean = false
)

data class DailyTaskView(
    val checkinId: Long,
    val taskDescription: String,
    val isCompleted: Boolean
)
