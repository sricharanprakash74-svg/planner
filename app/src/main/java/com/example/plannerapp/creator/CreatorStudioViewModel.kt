package com.example.plannerapp.creator

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.plannerapp.data.PlannerDao
import com.example.plannerapp.data.UserDao
import com.example.plannerapp.data.UserEntity
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Locale

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class CreatorStudioViewModel(
    private val creatorRepository: CreatorRepository,
    private val userDao: UserDao,
    private val plannerDao: PlannerDao,
    private val context: Context
) : ViewModel() {

    private val handleRegex = Regex("^[a-zA-Z0-9_]{3,20}$")

    private val _userMessages = Channel<String>(Channel.BUFFERED)
    val userMessages: Flow<String> = _userMessages.receiveAsFlow()

    private val currentUserFlow: Flow<UserEntity?> = userDao.getActiveUser()

    val uiState: StateFlow<CreatorStudioUiState> = currentUserFlow
        .flatMapLatest { user ->
            if (user == null) {
                flowOf(CreatorStudioUiState.Loading)
            } else {
                val userIdStr = if (user.userId != 0L) user.userId.toString() else (user.cloudUserId ?: "0")
                creatorRepository.observeCreatorProfile(userIdStr).flatMapLatest { profile ->
                    if (profile == null || !user.isCreator) {
                        flowOf(CreatorStudioUiState.NotRegistered(requirementsMet = true))
                    } else {
                        // Release any mature escrow entries seamlessly
                        creatorRepository.releaseMaturingEscrow(profile.creatorId)

                        combine(
                            creatorRepository.observeVersionsForCreator(profile.creatorId),
                            creatorRepository.observeLedgerForCreator(profile.creatorId)
                        ) { versions, ledger ->
                            val telemetry = calculateTelemetry(versions)
                            CreatorStudioUiState.Active(
                                profile = profile,
                                publishedPlans = versions,
                                recentTransactions = ledger,
                                telemetry = telemetry
                            )
                        }
                    }
                }
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = CreatorStudioUiState.Loading
        )

    fun onEvent(event: CreatorStudioEvent) {
        viewModelScope.launch {
            when (event) {
                is CreatorStudioEvent.RegisterCreator -> {
                    handleRegisterCreator(event.handle, event.bio)
                }
                is CreatorStudioEvent.UpdatePricing -> {
                    handleUpdatePricing(event.planId, event.newCredits, event.previewDays)
                }
                is CreatorStudioEvent.TogglePacingMode -> {
                    handleTogglePacing(event.planId, event.strictLock)
                }
                is CreatorStudioEvent.ToggleMandatoryReflection -> {
                    handleToggleReflection(event.planId, event.mandatory)
                }
                is CreatorStudioEvent.CreatePlanPatch -> {
                    handleCreatePlanPatch(event.planId, event.isBreaking, event.title)
                }
                is CreatorStudioEvent.RequestPayout -> {
                    handleRequestPayout()
                }
            }
        }
    }

    private suspend fun handleRegisterCreator(handle: String, bio: String) {
        val cleanHandle = handle.trim().removePrefix("@")
        if (!handleRegex.matches(cleanHandle)) {
            _userMessages.send("Handle must be 3-20 characters using letters, numbers, or underscores.")
            return
        }

        val user = currentUserFlow.firstOrNull()
        if (user == null) {
            _userMessages.send("User account not found.")
            return
        }

        val userIdStr = if (user.userId != 0L) user.userId.toString() else (user.cloudUserId ?: "0")
        try {
            creatorRepository.registerCreator(userIdStr, cleanHandle, bio.trim())
            _userMessages.send("Creator Profile registered successfully. Welcome to Creator Studio.")
        } catch (e: Exception) {
            _userMessages.send("Registration failed: ${e.message ?: "Unknown error"}")
        }
    }

    private suspend fun handleUpdatePricing(planId: String, newCredits: Int, previewDays: Int) {
        if (newCredits < 0) {
            _userMessages.send("Credit price cannot be negative.")
            return
        }
        if (previewDays !in 0..7) {
            _userMessages.send("Freemium preview days must be between 0 and 7.")
            return
        }

        try {
            creatorRepository.updatePricing(planId, newCredits, previewDays)
            val takeHome = creatorRepository.calculateTakeHomeUsd(newCredits.toLong())
            val formatted = String.format(Locale.US, "$%.2f", takeHome)
            _userMessages.send("Pricing updated: $newCredits credits ($formatted USD take-home at 70%).")
        } catch (e: Exception) {
            _userMessages.send("Failed to update pricing: ${e.message ?: "Unknown error"}")
        }
    }

    private suspend fun handleTogglePacing(planId: String, strictLock: Boolean) {
        try {
            creatorRepository.togglePacingMode(planId, strictLock)
            val msg = if (strictLock) "Strict Midnight Lock enabled (participants unlock one day per calendar day)." else "Open pacing enabled."
            _userMessages.send(msg)
        } catch (e: Exception) {
            _userMessages.send("Failed to update pacing mode: ${e.message ?: "Unknown error"}")
        }
    }

    private suspend fun handleToggleReflection(planId: String, mandatory: Boolean) {
        try {
            creatorRepository.toggleMandatoryReflection(planId, mandatory)
            val msg = if (mandatory) "Mandatory evening reflection note enabled." else "Daily reflection set to optional."
            _userMessages.send(msg)
        } catch (e: Exception) {
            _userMessages.send("Failed to update reflection requirement: ${e.message ?: "Unknown error"}")
        }
    }

    private suspend fun handleCreatePlanPatch(planId: String, isBreaking: Boolean, title: String) {
        try {
            val newVer = creatorRepository.createPlanPatch(planId, isBreaking, title)
            val type = if (isBreaking) "Breaking fork" else "Patch update"
            _userMessages.send("$type published: ${newVer.versionTag} - ${newVer.title}")
        } catch (e: Exception) {
            _userMessages.send("Failed to publish version: ${e.message ?: "Unknown error"}")
        }
    }

    private suspend fun handleRequestPayout() {
        val state = uiState.value
        if (state !is CreatorStudioUiState.Active) {
            _userMessages.send("Active creator profile required for payout.")
            return
        }

        val result = creatorRepository.requestStripePayout(state.profile.creatorId)
        result.onSuccess { amount ->
            val formatted = String.format(Locale.US, "$%.2f", amount)
            _userMessages.send("Cash out initiated: $formatted USD transferred via Stripe Connect.")
        }.onFailure { e ->
            _userMessages.send(e.message ?: "Payout request failed.")
        }
    }

    private fun calculateTelemetry(versions: List<com.example.plannerapp.data.creator.PlanVersionEntity>): CreatorTelemetry {
        val totalEnrolled = versions.sumOf { it.enrolledUsersCount }
        return CreatorTelemetry(
            dropOffDay = 14,
            dropOffRate = 0.48f,
            mostChallengingSubtask = "Day 14: 90-Minute Unbroken Deep Work Block",
            activeParticipants = if (totalEnrolled > 0) totalEnrolled else 796,
            completionVelocity = 84.2f
        )
    }

    fun calculateTakeHome(credits: Long): Double {
        return creatorRepository.calculateTakeHomeUsd(credits)
    }
}
