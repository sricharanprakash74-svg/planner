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
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class CreatePlanViewModel(
    private val repository: PlannerRepository,
    private val userDao: UserDao,
    private val appContext: Context
) : ViewModel() {

    fun createNewPlan(
        name: String,
        description: String,
        startDate: LocalDate,
        endDate: LocalDate,
        tasksInput: List<Pair<String, Set<Int>>>,
        reminderEnabled: Boolean = false,
        reminderTime: String? = "08:00"
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
                    reminderTime = reminderTime
                )

                // 2. Create Templates & Checkins
                val templatesWithCheckins = mutableMapOf<TaskTemplateEntity, List<DailyCheckinEntity>>()

                val totalDays = java.time.temporal.ChronoUnit.DAYS.between(startDate, endDate).toInt()

                tasksInput.forEach { (taskDesc, selectedDays) ->
                    val template = TaskTemplateEntity(
                        planId = 0,
                        taskDescription = taskDesc,
                        selectedDays = selectedDays.joinToString(",")
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

                // 4. Schedule reminder alarm if enabled
                if (reminderEnabled && !reminderTime.isNullOrBlank()) {
                    ReminderScheduler.schedule(appContext, newPlanId, name, reminderTime)
                }
            } catch (e: Exception) {
                // In a real app we'd emit an event to show a Snackbar here
                e.printStackTrace()
            }
        }
    }
}

class CreatePlanViewModelFactory(
    private val repository: PlannerRepository,
    private val userDao: UserDao,
    private val appContext: Context
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(CreatePlanViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return CreatePlanViewModel(repository, userDao, appContext) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
