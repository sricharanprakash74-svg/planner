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
    val joinedCommunity: JoinedCommunityEntity? = null
)

class PlanDetailViewModel(
    private val planId: Long,
    private val repository: PlannerRepository,
    private val appContext: Context
) : ViewModel() {

    private val _selectedDate = MutableStateFlow(LocalDate.now())
    private val gson = Gson()
    private val dateFormatter = DateTimeFormatter.ISO_LOCAL_DATE
    private val dayOfWeekFormatter = DateTimeFormatter.ofPattern("EEE")
    private val dayOfMonthFormatter = DateTimeFormatter.ofPattern("d")

    val currentPlan: StateFlow<PlanEntity?> = repository.getPlan(planId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

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

    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<Resource<PlanDetailUiState>> = combine(
        currentPlan,
        _selectedDate,
        planContextFlow,
        _selectedDate.flatMapLatest { date -> repository.getTasksForPlanAndDate(planId, date) }
    ) { plan, selectedDate, contextData, tasksForDate ->
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
                joinedCommunity = contextData.joinedCommunity
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
                // Additive hook: reward streak bonus & refresh home screen widget
                val db = PlannerDatabase.getDatabase(appContext)
                val user = db.userDao().getActiveUserOnce()
                if (user != null) {
                    val creditRepo = CreditRepository(db.creditDao())
                    creditRepo.awardStreakMilestone(userId = user.userId, streakDays = 1, bonusPoints = 25)
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

                // Additive hook: award credits on completion & refresh home screen widget
                if (isCompleted) {
                    val db = PlannerDatabase.getDatabase(appContext)
                    val user = db.userDao().getActiveUserOnce()
                    if (user != null) {
                        val creditRepo = CreditRepository(db.creditDao())
                        creditRepo.awardTaskCompletion(userId = user.userId, checkinId = checkinId)
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

                if (allCompleted) {
                    val db = PlannerDatabase.getDatabase(appContext)
                    val user = db.userDao().getActiveUserOnce()
                    if (user != null) {
                        val creditRepo = CreditRepository(db.creditDao())
                        creditRepo.awardTaskCompletion(userId = user.userId, checkinId = task.checkinId)
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
                val template = TaskTemplateEntity(
                    planId = planId,
                    taskDescription = taskDescription,
                    selectedDays = date.dayOfWeek.value.toString(),
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
    private val appContext: Context
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(PlanDetailViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return PlanDetailViewModel(planId, repository, appContext) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

