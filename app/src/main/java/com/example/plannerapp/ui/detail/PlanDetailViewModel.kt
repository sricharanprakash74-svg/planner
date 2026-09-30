package com.example.plannerapp.ui.detail

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.plannerapp.data.DailyCheckinEntity
import com.example.plannerapp.data.DailyTaskView
import com.example.plannerapp.data.JoinedCommunityEntity
import com.example.plannerapp.data.PlanDayCompletionEntity
import com.example.plannerapp.data.PlanEntity
import com.example.plannerapp.data.PlannerRepository
import com.example.plannerapp.credits.CreditRepository
import com.example.plannerapp.data.PlannerDatabase
import com.example.plannerapp.data.TaskTemplateEntity
import com.example.plannerapp.data.social.PlanVersionUpdate
import com.example.plannerapp.data.social.SocialRepository
import com.example.plannerapp.data.template.PlanTemplateDto
import com.example.plannerapp.data.template.TaskTemplateDto
import com.example.plannerapp.notifications.ReminderScheduler
import com.example.plannerapp.ui.state.Resource
import com.example.plannerapp.widget.StreakWidgetUpdater
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

data class PlanDayUiModel(
    val date: LocalDate,
    val dayNumber: Int,
    val dayOfWeek: String,
    val dayOfMonth: String,
    val isToday: Boolean,
    val isSelected: Boolean,
    val isCompleted: Boolean,
    val isLocked: Boolean,
    val hasTasks: Boolean,
    val taskCount: Int,
    val completedTaskCount: Int
)

data class PlanDetailUiState(
    val plan: PlanEntity? = null,
    val selectedDate: LocalDate = LocalDate.now(),
    val planDays: List<PlanDayUiModel> = emptyList(),
    val tasks: List<DailyTaskView> = emptyList(),
    val isDayLocked: Boolean = false,
    val journalNotes: String = "",
    val currentDayNumber: Int = 1,
    val totalDays: Int = 1,
    val overallProgressPercent: Int = 0,
    val joinedCommunity: JoinedCommunityEntity? = null,
    val planUpdate: PlanVersionUpdate? = null
)

class PlanDetailViewModel(
    private val planId: Long,
    private val repository: PlannerRepository,
    private val appContext: Context,
    private val socialRepository: SocialRepository? = null
) : ViewModel() {

    private val _selectedDate = MutableStateFlow(LocalDate.now())
    private val gson = Gson()
    private val dateFormatter = DateTimeFormatter.ISO_LOCAL_DATE
    private val dayOfWeekFormatter = DateTimeFormatter.ofPattern("EEE")
    private val dayOfMonthFormatter = DateTimeFormatter.ofPattern("d")

    val currentPlan: StateFlow<PlanEntity?> = repository.getPlan(planId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    init {
        viewModelScope.launch {
            repository.repairMissingCheckinsForPlan(planId)
        }
    }

    private val allPlanCheckins: Flow<List<DailyCheckinEntity>> = repository.getAllCheckinsForPlan(planId)
    private val allPlanCompletions: Flow<List<PlanDayCompletionEntity>> = repository.getPlanDayCompletions(planId)

    private data class PlanContextData(
        val checkins: List<DailyCheckinEntity>,
        val completions: List<PlanDayCompletionEntity>,
        val joinedCommunity: JoinedCommunityEntity?
    )

    private val planContextFlow: Flow<PlanContextData> = combine(
        allPlanCheckins,
        allPlanCompletions,
        repository.getJoinedCommunityForPlan(planId)
    ) { checkins, completions, joined ->
        PlanContextData(checkins, completions, joined)
    }

    private val prefs = appContext.getSharedPreferences("plan_version_prefs", Context.MODE_PRIVATE)
    private val _dismissedVersions = MutableStateFlow<Set<String>>(emptySet())
    private val _currentLocalVersion = MutableStateFlow(
        prefs.getString("plan_${planId}_version", "1.0.0") ?: "1.0.0"
    )

    @OptIn(ExperimentalCoroutinesApi::class)
    private val planUpdateFlow: Flow<PlanVersionUpdate?> = combine(
        repository.getJoinedCommunityForPlan(planId),
        _currentLocalVersion,
        _dismissedVersions
    ) { joined, version, dismissed ->
        Triple(joined, version, dismissed)
    }.flatMapLatest { (joined, version, dismissed) ->
        if (joined != null && socialRepository != null) {
            socialRepository.checkForPlanUpdate(joined.postId, version).map { update ->
                if (update != null && update.latestVersionTag !in dismissed) update else null
            }
        } else {
            flowOf(null)
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<Resource<PlanDetailUiState>> = combine(
        currentPlan,
        _selectedDate,
        planContextFlow,
        _selectedDate.flatMapLatest { date -> repository.getTasksForPlanAndDate(planId, date) },
        planUpdateFlow
    ) { plan, selectedDate, contextData, tasksForDate, planUpdate ->
        val today = LocalDate.now()

        val parsedStartDate = try {
            if (plan != null && plan.startDate.isNotBlank()) LocalDate.parse(plan.startDate, dateFormatter) else today
        } catch (e: Exception) {
            today
        }

        val parsedEndDate = try {
            if (plan != null && plan.endDate.isNotBlank()) LocalDate.parse(plan.endDate, dateFormatter) else parsedStartDate.plusDays(29)
        } catch (e: Exception) {
            parsedStartDate.plusDays(29)
        }

        val totalDaysCount = (ChronoUnit.DAYS.between(parsedStartDate, parsedEndDate) + 1).coerceAtLeast(1).toInt()
        val currentDayNum = (ChronoUnit.DAYS.between(parsedStartDate, selectedDate) + 1).toInt()

        val allCheckins = contextData.checkins
        val allCompletions = contextData.completions
        val checkinsByDate = allCheckins.groupBy { it.exactDate }
        val completionsByDate = allCompletions.associateBy { it.exactDate }

        val selectedDateStr = selectedDate.format(dateFormatter)
        val currentDayRecord = completionsByDate[selectedDateStr]
        val isCurrentDayLocked = currentDayRecord?.isDayLocked == true
        val currentJournalNotes = currentDayRecord?.journalNotes ?: ""

        // Build days list for this specific plan
        val daysList = (0 until totalDaysCount).map { offset ->
            val date = parsedStartDate.plusDays(offset.toLong())
            val dateStr = date.format(dateFormatter)
            val dayCheckins = checkinsByDate[dateStr] ?: emptyList()
            val dayLockRecord = completionsByDate[dateStr]
            val isLocked = dayLockRecord?.isDayLocked == true
            val total = dayCheckins.size
            val completed = dayCheckins.count { it.isCompleted }

            PlanDayUiModel(
                date = date,
                dayNumber = offset + 1,
                dayOfWeek = date.format(dayOfWeekFormatter).uppercase(),
                dayOfMonth = date.format(dayOfMonthFormatter),
                isToday = date == today,
                isSelected = date == selectedDate,
                isCompleted = (total > 0 && completed == total) || isLocked,
                isLocked = isLocked,
                hasTasks = total > 0,
                taskCount = total,
                completedTaskCount = completed
            )
        }

        val totalPlanCheckins = allCheckins.size
        val totalCompletedPlanCheckins = allCheckins.count { it.isCompleted }
        val overallProgress = if (totalPlanCheckins > 0) {
            ((totalCompletedPlanCheckins.toFloat() / totalPlanCheckins) * 100).toInt()
        } else {
            0
        }

        Resource.Success(
            PlanDetailUiState(
                plan = plan,
                selectedDate = selectedDate,
                planDays = daysList,
                tasks = tasksForDate,
                isDayLocked = isCurrentDayLocked,
                journalNotes = currentJournalNotes,
                currentDayNumber = currentDayNum,
                totalDays = totalDaysCount,
                overallProgressPercent = overallProgress,
                joinedCommunity = contextData.joinedCommunity,
                planUpdate = planUpdate
            )
        ) as Resource<PlanDetailUiState>
    }
    .catch { e -> emit(Resource.Error(e.message ?: "Failed to load plan details")) }
    .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = Resource.Loading
    )

    fun selectDate(date: LocalDate) {
        _selectedDate.value = date
    }

    fun saveJournalNotes(notes: String) {
        viewModelScope.launch {
            try {
                repository.saveJournalNotes(planId, _selectedDate.value, notes)
            } catch (e: Exception) {
                // Handle error
            }
        }
    }

    fun markDayCompleted(completedTasks: Int, totalTasks: Int) {
        viewModelScope.launch {
            try {
                repository.lockDayCompletion(
                    planId = planId,
                    date = _selectedDate.value,
                    completedTasksCount = completedTasks,
                    totalTasksCount = totalTasks
                )
                // Additive hook: reward credit milestones & refresh home screen widget
                val db = PlannerDatabase.getDatabase(appContext)
                val user = db.userDao().getActiveUserOnce()
                if (user != null) {
                    val creditRepo = CreditRepository(db.creditDao())
                    val dateStr = _selectedDate.value.format(dateFormatter)

                    // If all scheduled tasks were completed for the day, award +10 Credits (idempotent)
                    if (completedTasks > 0 && completedTasks == totalTasks) {
                        creditRepo.awardDayCompletion(userId = user.userId, date = dateStr)
                    }

                    // Calculate active streak including this locked day and award milestones (+50 for 7 days, +100 for 30 days)
                    val streak = repository.getStreak(user.userId, _selectedDate.value.plusDays(1))
                    if (streak == 7 || streak == 30) {
                        creditRepo.awardStreakMilestone(userId = user.userId, streakDays = streak)
                    }
                }
                StreakWidgetUpdater.update(appContext)
            } catch (e: Exception) {
                // Handle error
            }
        }
    }

    fun onTaskChecked(checkinId: Long, isCompleted: Boolean) {
        val currentState = (uiState.value as? Resource.Success)?.data
        if (currentState?.isDayLocked == true) return // Prevent changes if day is locked

        viewModelScope.launch {
            try {
                repository.updateTaskStatus(checkinId, isCompleted)

                // If this task checkmark causes all scheduled tasks for today to be completed, award +10 Credits (idempotent)
                if (isCompleted) {
                    val dateStr = _selectedDate.value.format(dateFormatter)
                    val dailyTasks = repository.getTasksForPlanAndDateOnce(planId, dateStr)
                    if (dailyTasks.isNotEmpty() && dailyTasks.all { it.isCompleted }) {
                        val db = PlannerDatabase.getDatabase(appContext)
                        val user = db.userDao().getActiveUserOnce()
                        if (user != null) {
                            val creditRepo = CreditRepository(db.creditDao())
                            creditRepo.awardDayCompletion(userId = user.userId, date = dateStr)
                        }
                    }
                }

                StreakWidgetUpdater.update(appContext)
            } catch (e: Exception) {
                // Handle error
            }
        }
    }

    fun onSubtaskChecked(task: DailyTaskView, subtaskIndex: Int, isCompleted: Boolean) {
        val currentState = (uiState.value as? Resource.Success)?.data
        if (currentState?.isDayLocked == true) return // Prevent changes if day is locked

        viewModelScope.launch {
            try {
                val listType = object : TypeToken<List<Boolean>>() {}.type
                val completedList: MutableList<Boolean> = try {
                    gson.fromJson(task.completedSubtasks, listType) ?: mutableListOf()
                } catch (e: Exception) {
                    mutableListOf()
                }
                
                val subtasksListType = object : TypeToken<List<String>>() {}.type
                val subtasksList: List<String> = try {
                    gson.fromJson(task.subtasks, subtasksListType) ?: emptyList()
                } catch (e: Exception) { emptyList() }

                while (completedList.size < subtasksList.size) {
                    completedList.add(false)
                }

                if (subtaskIndex in completedList.indices) {
                    completedList[subtaskIndex] = isCompleted
                }

                val allCompleted = completedList.isNotEmpty() && completedList.all { it }
                repository.updateCheckinAndSubtasksStatus(
                    checkinId = task.checkinId,
                    isCompleted = allCompleted,
                    completedSubtasks = gson.toJson(completedList)
                )

                // If completing this subtask completes all tasks for today, award +10 Credits (idempotent)
                if (allCompleted) {
                    val dateStr = _selectedDate.value.format(dateFormatter)
                    val dailyTasks = repository.getTasksForPlanAndDateOnce(planId, dateStr)
                    if (dailyTasks.isNotEmpty() && dailyTasks.all { it.isCompleted }) {
                        val db = PlannerDatabase.getDatabase(appContext)
                        val user = db.userDao().getActiveUserOnce()
                        if (user != null) {
                            val creditRepo = CreditRepository(db.creditDao())
                            creditRepo.awardDayCompletion(userId = user.userId, date = dateStr)
                        }
                    }
                }

                StreakWidgetUpdater.update(appContext)
            } catch (e: Exception) {
                // Handle error
            }
        }
    }

    fun addTask(taskDescription: String, durationDays: Int, subtasks: List<String>) {
        viewModelScope.launch {
            try {
                val date = _selectedDate.value
                val activeDays = if (durationDays >= 7) {
                    "1,2,3,4,5,6,7"
                } else {
                    (0 until durationDays).map { date.plusDays(it.toLong()).dayOfWeek.value }.distinct().sorted().joinToString(",")
                }
                val template = TaskTemplateEntity(
                    planId = planId,
                    taskDescription = taskDescription,
                    selectedDays = activeDays,
                    durationDays = durationDays,
                    subtasks = gson.toJson(subtasks)
                )
                
                repository.addTaskToPlan(template, startDate = date, durationDays = durationDays)
            } catch (e: Exception) {
                // Handle error
            }
        }
    }

    fun editTask(templateId: Long, checkinId: Long, newDescription: String, durationDays: Int, newSubtasks: List<String>) {
        viewModelScope.launch {
            try {
                repository.updateTask(templateId, newDescription, durationDays, gson.toJson(newSubtasks))
                repository.updateCheckinAndSubtasksStatus(
                    checkinId = checkinId,
                    isCompleted = false,
                    completedSubtasks = gson.toJson(List(newSubtasks.size) { false })
                )
            } catch (e: Exception) {
                // Handle error
            }
        }
    }

    fun deleteTask(templateId: Long) {
        viewModelScope.launch {
            try {
                repository.deleteTask(templateId)
            } catch (e: Exception) {
                // Handle error
            }
        }
    }

    fun updatePlan(
        heading: String,
        description: String = "",
        startDate: String? = null,
        endDate: String? = null,
        defaultTaskDurationDays: Int = 1,
        reminderEnabled: Boolean = false,
        reminderTime: String? = "08:00"
    ) {
        viewModelScope.launch {
            try {
                repository.updatePlan(planId, heading, description, startDate, endDate, defaultTaskDurationDays, reminderEnabled, reminderTime)

                // Cancel existing alarm, then re-schedule if enabled
                ReminderScheduler.cancel(appContext, planId)
                if (reminderEnabled && !reminderTime.isNullOrBlank()) {
                    ReminderScheduler.schedule(appContext, planId, heading, reminderTime)
                }
            } catch (e: Exception) {
                // Handle error
            }
        }
    }

    fun togglePlanVisibility(isPublic: Boolean, customSocialRepo: com.example.plannerapp.data.social.SocialRepository? = null) {
        viewModelScope.launch {
            try {
                repository.setPlanPublicStatus(planId, isPublic)
                val targetRepo = customSocialRepo ?: this@PlanDetailViewModel.socialRepository
                if (targetRepo != null) {
                    val plan = currentPlan.value ?: repository.getPlan(planId).firstOrNull() ?: return@launch
                    val templates = repository.getTaskTemplatesForPlan(planId)
                    val db = PlannerDatabase.getDatabase(appContext)
                    val user = db.userDao().getActiveUserOnce()

                    val totalDays = try {
                        val start = LocalDate.parse(plan.startDate, dateFormatter)
                        val end = LocalDate.parse(plan.endDate, dateFormatter)
                        ChronoUnit.DAYS.between(start, end).toInt() + 1
                    } catch (e: Exception) { 7 }

                    if (isPublic) {
                        val cloudUid = com.example.plannerapp.auth.SupabaseConfig.auth.currentUserOrNull()?.id
                            ?: user?.cloudUserId
                            ?: return@launch // Requires authenticated cloud user

                        val author = com.example.plannerapp.data.social.CloudUser(
                            userId = cloudUid,
                            username = user?.username ?: user?.displayName?.replace(" ", "_")?.lowercase() ?: "creator",
                            displayName = user?.displayName ?: "Creator",
                            avatarUrl = user?.avatarUrl,
                            isCreator = user?.isCreator == true
                        )

                        val templateDto = com.example.plannerapp.data.template.PlanTemplateDto(
                            title = plan.heading,
                            description = plan.description,
                            targetDurationDays = totalDays,
                            defaultTaskDurationDays = plan.defaultTaskDurationDays,
                            tags = emptyList(),
                            category = "General",
                            author = com.example.plannerapp.data.template.AuthorDto(
                                userId = author.userId,
                                displayName = author.displayName,
                                avatarUrl = author.avatarUrl,
                                isCreator = author.isCreator
                            ),
                            tasks = templates.map { t ->
                                val subtasksList: List<String> = try {
                                    gson.fromJson(t.subtasks, object : TypeToken<List<String>>() {}.type) ?: emptyList()
                                } catch (e: Exception) { emptyList() }
                                com.example.plannerapp.data.template.TaskTemplateDto(
                                    taskDescription = t.taskDescription,
                                    selectedDays = t.selectedDays,
                                    durationDays = t.durationDays,
                                    subtasks = subtasksList
                                )
                            }
                        )

                        val postResult = targetRepo.createPost(
                            author = author,
                            title = plan.heading,
                            description = plan.description,
                            planTemplateJson = gson.toJson(templateDto),
                            durationDays = totalDays,
                            tags = emptyList(),
                            category = "General",
                            visibility = "public"
                        )
                        if (postResult.isSuccess) {
                            val post = postResult.getOrNull()
                            if (post != null) {
                                repository.insertJoinedCommunity(
                                    com.example.plannerapp.data.JoinedCommunityEntity(
                                        localPlanId = planId,
                                        postId = post.postId,
                                        communityTitle = plan.heading,
                                        creatorName = author.displayName
                                    )
                                )
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun adoptPlanUpdate(update: PlanVersionUpdate, onCompleted: (Int) -> Unit = {}) {
        viewModelScope.launch {
            try {
                val plan = currentPlan.value ?: return@launch
                val today = LocalDate.now()
                val parsedEndDate = try {
                    LocalDate.parse(plan.endDate, dateFormatter)
                } catch (e: Exception) { today.plusDays(7) }

                val remainingDays = (ChronoUnit.DAYS.between(today, parsedEndDate) + 1).coerceAtLeast(1).toInt()

                var addedCount = 0
                for (taskDto in update.newTasks) {
                    val template = TaskTemplateEntity(
                        planId = planId,
                        taskDescription = taskDto.taskDescription,
                        selectedDays = taskDto.selectedDays.ifBlank { "1,2,3,4,5,6,7" },
                        durationDays = taskDto.durationDays.coerceAtLeast(1),
                        subtasks = gson.toJson(taskDto.subtasks)
                    )
                    repository.addTaskToPlan(template, startDate = today, durationDays = remainingDays)
                    addedCount++
                }

                prefs.edit().putString("plan_${planId}_version", update.latestVersionTag).apply()
                _currentLocalVersion.value = update.latestVersionTag
                onCompleted(addedCount)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun cloneAsNewPlan(update: PlanVersionUpdate, onPlanCreated: (Long) -> Unit) {
        viewModelScope.launch {
            try {
                val db = PlannerDatabase.getDatabase(appContext)
                val user = db.userDao().getActiveUserOnce() ?: return@launch
                val currentJoined = repository.getJoinedCommunityForPlanOnce(planId)

                if (socialRepository != null && currentJoined != null) {
                    val postFlow = socialRepository.getPostById(currentJoined.postId)
                    val post = postFlow.firstOrNull()
                    if (post != null) {
                        val parsedTemplate = gson.fromJson(post.planTemplateJson, PlanTemplateDto::class.java)
                        if (parsedTemplate != null) {
                            val newPlanId = repository.importPlanTemplate(
                                template = parsedTemplate.copy(
                                    title = "${parsedTemplate.title} (v${update.latestVersionTag})"
                                ),
                                targetUserId = user.userId,
                                startDate = LocalDate.now(),
                                sourcePostId = PlannerRepository.stableStringHash64(currentJoined.postId)
                            ).getOrNull()
                            if (newPlanId != null) {
                                prefs.edit().putString("plan_${newPlanId}_version", update.latestVersionTag).apply()
                                onPlanCreated(newPlanId)
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun dismissPlanUpdate(versionTag: String) {
        _dismissedVersions.value = _dismissedVersions.value + versionTag
    }

    fun deletePlan(onDeleted: () -> Unit) {
        viewModelScope.launch {
            try {
                ReminderScheduler.cancel(appContext, planId)
                repository.deletePlan(planId)
                onDeleted()
            } catch (e: Exception) {
                // Handle error
            }
        }
    }
}

class PlanDetailViewModelFactory(
    private val planId: Long,
    private val repository: PlannerRepository,
    private val appContext: Context,
    private val socialRepository: SocialRepository? = null
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(PlanDetailViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return PlanDetailViewModel(planId, repository, appContext, socialRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

