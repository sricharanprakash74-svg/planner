package com.example.plannerapp.ui.home

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.plannerapp.data.PlanEntity
import com.example.plannerapp.data.PlannerRepository
import com.example.plannerapp.data.UserDao
import com.example.plannerapp.notifications.ReminderScheduler
import com.example.plannerapp.ui.state.Resource
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter

data class HomeUiState(
    val plans: List<PlanEntity> = emptyList()
)

class HomeViewModel(
    private val repository: PlannerRepository,
    private val userDao: UserDao,
    private val appContext: Context
) : ViewModel() {

    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<Resource<HomeUiState>> = userDao.getActiveUser()
        .flatMapLatest { user ->
            if (user != null) {
                repository.getPlansForUser(user.userId).map { plans ->
                    Resource.Success(HomeUiState(plans = plans)) as Resource<HomeUiState>
                }
            } else {
                MutableStateFlow(Resource.Success(HomeUiState()) as Resource<HomeUiState>)
            }
        }
        .catch { e -> emit(Resource.Error(e.message ?: "Failed to load home data")) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = Resource.Loading
        )

    init {
        viewModelScope.launch {
            if (userDao.getActiveUserOnce() == null) {
                userDao.insertUser(com.example.plannerapp.data.UserEntity(displayName = "Guest"))
            }
        }
    }

    fun createQuickPlan(
        title: String,
        description: String = "",
        durationDays: Int = 30,
        defaultTaskDurationDays: Int = 1,
        reminderEnabled: Boolean = false,
        reminderTime: String? = "08:00",
        onCreated: (Long) -> Unit = {}
    ) {
        viewModelScope.launch {
            try {
                val user = userDao.getActiveUserOnce() ?: return@launch
                val date = LocalDate.now()
                val dateFormatter = DateTimeFormatter.ISO_LOCAL_DATE
                val startStr = date.format(dateFormatter)
                val endStr = date.plusDays((durationDays - 1).coerceAtLeast(0).toLong()).format(dateFormatter)

                val plan = PlanEntity(
                    userId = user.userId,
                    heading = title,
                    description = description,
                    startDate = startStr,
                    endDate = endStr,
                    defaultTaskDurationDays = defaultTaskDurationDays,
                    reminderEnabled = reminderEnabled,
                    reminderTime = reminderTime
                )
                val newPlanId = repository.createPlan(plan)

                // Schedule reminder alarm if enabled
                if (reminderEnabled && !reminderTime.isNullOrBlank()) {
                    ReminderScheduler.schedule(appContext, newPlanId, title, reminderTime)
                }

                onCreated(newPlanId)
            } catch (e: Exception) {
                // Log or handle error
            }
        }
    }

    fun togglePinPlan(planId: Long, isPinned: Boolean) {
        viewModelScope.launch {
            try {
                repository.togglePinPlan(planId, isPinned)
            } catch (e: Exception) {
                // Log or handle error
            }
        }
    }

    fun batchPinPlans(planIds: List<Long>, isPinned: Boolean) {
        viewModelScope.launch {
            try {
                repository.updatePlansPinStatus(planIds, isPinned)
            } catch (e: Exception) {
                // Log or handle error
            }
        }
    }

    fun batchDeletePlans(planIds: List<Long>) {
        viewModelScope.launch {
            try {
                // Cancel any active alarms for deleted plans
                planIds.forEach { planId -> ReminderScheduler.cancel(appContext, planId) }
                repository.deletePlans(planIds)
            } catch (e: Exception) {
                // Log or handle error
            }
        }
    }

    fun updatePlan(
        planId: Long,
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
                // Log or handle error
            }
        }
    }

    fun deletePlan(planId: Long) {
        viewModelScope.launch {
            try {
                ReminderScheduler.cancel(appContext, planId)
                repository.deletePlan(planId)
            } catch (e: Exception) {
                // Log or handle error
            }
        }
    }
}

class HomeViewModelFactory(
    private val repository: PlannerRepository,
    private val userDao: UserDao,
    private val appContext: Context
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(HomeViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return HomeViewModel(repository, userDao, appContext) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
