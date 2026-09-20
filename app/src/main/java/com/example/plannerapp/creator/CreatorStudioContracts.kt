package com.example.plannerapp.creator

import com.example.plannerapp.data.creator.CreatorLedgerEntity
import com.example.plannerapp.data.creator.CreatorProfileEntity
import com.example.plannerapp.data.creator.PlanVersionEntity

sealed interface CreatorStudioUiState {
    data object Loading : CreatorStudioUiState
    data class NotRegistered(val requirementsMet: Boolean) : CreatorStudioUiState
    data class Active(
        val profile: CreatorProfileEntity,
        val publishedPlans: List<PlanVersionEntity>,
        val recentTransactions: List<CreatorLedgerEntity>,
        val telemetry: CreatorTelemetry
    ) : CreatorStudioUiState
    data class Error(val message: String) : CreatorStudioUiState
}

data class CreatorTelemetry(
    val dropOffDay: Int, // e.g., Day 14 has the highest churn
    val dropOffRate: Float,
    val mostChallengingSubtask: String,
    val activeParticipants: Int,
    val completionVelocity: Float
)

sealed interface CreatorStudioEvent {
    data class UpdatePricing(val planId: String, val newCredits: Int, val previewDays: Int) : CreatorStudioEvent
    data class TogglePacingMode(val planId: String, val strictLock: Boolean) : CreatorStudioEvent
    data class ToggleMandatoryReflection(val planId: String, val mandatory: Boolean) : CreatorStudioEvent
    data class CreatePlanPatch(val planId: String, val isBreaking: Boolean, val title: String) : CreatorStudioEvent
    data object RequestPayout : CreatorStudioEvent
    data class RegisterCreator(val handle: String, val bio: String) : CreatorStudioEvent
}
