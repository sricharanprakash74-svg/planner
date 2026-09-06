package com.example.plannerapp.sharing

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "shared_plans",
    indices = [
        Index("shareCode", unique = true),
        Index("originalPlanId"),
        Index("authorUserId")
    ]
)
data class SharedPlanEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val shareCode: String,
    val originalPlanId: Long,
    val authorUserId: Long,
    val planTitle: String,
    val planDescription: String,
    val durationDays: Int,
    val templatePayloadJson: String,
    val cloneCount: Int = 0,
    val createdAt: Long = System.currentTimeMillis()
)
