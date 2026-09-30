package com.example.plannerapp.ui.create

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.plannerapp.data.DailyCheckinEntity
import com.example.plannerapp.data.PlanEntity
import com.example.plannerapp.data.PlannerRepository
import com.example.plannerapp.data.TaskTemplateEntity
import com.example.plannerapp.data.UserDao
import com.example.plannerapp.notifications.ReminderScheduler
import com.example.plannerapp.sync.SyncOutboxRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.LocalDate
import com.example.plannerapp.data.social.CloudUser
import com.example.plannerapp.data.social.SocialRepository
import com.example.plannerapp.data.template.AuthorDto
import com.example.plannerapp.data.template.PlanTemplateDto
import com.example.plannerapp.data.template.TaskTemplateDto
import com.google.gson.Gson
import java.time.format.DateTimeFormatter

class CreatePlanViewModel(
    private val repository: PlannerRepository,
    private val userDao: UserDao,
    private val appContext: Context,
    private val socialRepository: SocialRepository? = null,
    private val syncOutbox: SyncOutboxRepository? = null
) : ViewModel() {

    fun createNewPlan(
        name: String,
        description: String,
        startDate: LocalDate,
        endDate: LocalDate,
        tasksInput: List<Pair<String, Set<Int>>>,
        reminderEnabled: Boolean = false,
        reminderTime: String? = "08:00",
        isPublic: Boolean = false,
        category: String = "Productivity",
        tags: List<String> = emptyList()
    ) {
        viewModelScope.launch {
            try {
                val user = userDao.getActiveUserOnce() ?: return@launch
                val dateFormatter = DateTimeFormatter.ISO_LOCAL_DATE

                // 1. Create Plan
                val plan = PlanEntity(
                    userId = user.userId,
                    heading = name,
                    description = description,
                    startDate = startDate.format(dateFormatter),
                    endDate = endDate.format(dateFormatter),
                    reminderEnabled = reminderEnabled,
                    reminderTime = reminderTime,
                    isPublic = isPublic
                )

                // 2. Create Templates & Checkins
                val templatesWithCheckins = mutableMapOf<TaskTemplateEntity, List<DailyCheckinEntity>>()
                val totalDays = java.time.temporal.ChronoUnit.DAYS.between(startDate, endDate).toInt()

                val planDuration = totalDays + 1
                tasksInput.forEach { (taskDesc, selectedDays) ->
                    val template = TaskTemplateEntity(
                        planId = 0,
                        taskDescription = taskDesc,
                        selectedDays = selectedDays.joinToString(","),
                        durationDays = planDuration
                    )

                    val checkins = mutableListOf<DailyCheckinEntity>()
                    for (i in 0..totalDays) {
                        val date = startDate.plusDays(i.toLong())
                        if (selectedDays.contains(date.dayOfWeek.value)) {
                            checkins.add(
                                DailyCheckinEntity(
                                    templateId = 0,
                                    exactDate = date.format(dateFormatter),
                                    isCompleted = false
                                )
                            )
                        }
                    }
                    templatesWithCheckins[template] = checkins
                }

                // 3. Save via Repository
                val newPlanId = repository.createFullPlan(plan, templatesWithCheckins)

                // 4. Enqueue to sync outbox (cloud backup)
                val cloudUid = com.example.plannerapp.auth.SupabaseConfig.auth.currentUserOrNull()?.id
                    ?: user.cloudUserId
                if (cloudUid != null && syncOutbox != null) {
                    val savedPlan = repository.getPlansForUser(user.userId)
                        .first()
                        .find { it.planId == newPlanId }
                    if (savedPlan != null) {
                        syncOutbox.enqueuePlanUpsert(savedPlan, cloudUid)
                        val templates = repository.getTaskTemplatesForPlan(newPlanId)
                        templates.forEach { tmpl ->
                            syncOutbox.enqueueTemplateUpsert(tmpl, null, cloudUid)
                        }
                        val allCheckins = repository.getAllCheckinsForPlan(newPlanId).first()
                        allCheckins.forEach { ci ->
                            syncOutbox.enqueueCheckinUpsert(ci, null, cloudUid)
                        }
                    }
                }

                // 5. If public, publish to SocialRepository
                if (isPublic && socialRepository != null) {
                    // Require authenticated cloud user — do NOT fall back to local integer ID
                    val publishUid = com.example.plannerapp.auth.SupabaseConfig.auth.currentUserOrNull()?.id
                        ?: user.cloudUserId
                    if (publishUid != null) {
                        val author = CloudUser(
                            userId = publishUid,
                            username = user.username ?: user.displayName.replace(" ", "_").lowercase(),
                            displayName = user.displayName,
                            avatarUrl = user.avatarUrl,
                            isCreator = user.isCreator
                        )
                        val effectiveTags = if (tags.isNotEmpty()) tags else listOf(category.lowercase().replace(" & ", "_").replace(" ", "_"))
                        val templateDto = PlanTemplateDto(
                            title = name,
                            description = description,
                            targetDurationDays = planDuration,
                            defaultTaskDurationDays = 1,
                            tags = effectiveTags,
                            category = category,
                            author = AuthorDto(author.userId, author.displayName, author.avatarUrl, author.isCreator),
                            tasks = tasksInput.map { (taskDesc, selectedDays) ->
                                TaskTemplateDto(
                                    taskDescription = taskDesc,
                                    selectedDays = selectedDays.joinToString(","),
                                    durationDays = planDuration,
                                    subtasks = emptyList()
                                )
                            }
                        )
                        val postResult = socialRepository.createPost(
                            author = author,
                            title = name,
                            description = description,
                            planTemplateJson = Gson().toJson(templateDto),
                            durationDays = planDuration,
                            tags = effectiveTags,
                            category = category,
                            visibility = "PUBLIC"
                        )
                        if (postResult.isSuccess) {
                            val post = postResult.getOrNull()
                            if (post != null) {
                                repository.insertJoinedCommunity(
                                    com.example.plannerapp.data.JoinedCommunityEntity(
                                        localPlanId = newPlanId,
                                        postId = post.postId,
                                        communityTitle = name,
                                        creatorName = user.displayName
                                    )
                                )
                            }
                        }
                    }
                }

                // 6. Schedule reminder alarm if enabled
                if (reminderEnabled && !reminderTime.isNullOrBlank()) {
                    ReminderScheduler.schedule(appContext, newPlanId, name, reminderTime)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}

class CreatePlanViewModelFactory(
    private val repository: PlannerRepository,
    private val userDao: UserDao,
    private val appContext: Context,
    private val socialRepository: SocialRepository? = null,
    private val syncOutbox: SyncOutboxRepository? = null
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(CreatePlanViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return CreatePlanViewModel(repository, userDao, appContext, socialRepository, syncOutbox) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
