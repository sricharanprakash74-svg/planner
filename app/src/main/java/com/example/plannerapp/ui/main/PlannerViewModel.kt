package com.example.plannerapp.ui.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.plannerapp.data.DailyTaskView
import com.example.plannerapp.data.PlannerRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

data class PlannerUiState(
    val tasks: List<DailyTaskView> = emptyList(),
    val streak: Int = 0,
    val consistencyPercentage: Int = 0,
    val selectedDate: LocalDate = LocalDate.now(),
    val isLoading: Boolean = true
)

class PlannerViewModel(private val repository: PlannerRepository) : ViewModel() {

    private val _selectedDate = MutableStateFlow(LocalDate.now())
    private val _streak = MutableStateFlow(0)
    
    // We observe the tasks flow from the repository for the selected date.
    // If we were switching dates dynamically, we would flatMapLatest this,
    // but for simplicity we will just stick to today or let flatMapLatest handle it.
    // To keep it simple without experimental APIs:
    
    // For now, let's just stick to "Today" for the main view to match the widget logic perfectly.
    val uiState: StateFlow<PlannerUiState> = combine(
        repository.getTasksForDate(LocalDate.now()),
        _streak
    ) { tasks, streak ->
        val total = tasks.size
        val completed = tasks.count { it.isCompleted }
        val percentage = if (total > 0) ((completed.toFloat() / total) * 100).toInt() else 0
        
        PlannerUiState(
            tasks = tasks,
            streak = streak,
            consistencyPercentage = percentage,
            selectedDate = LocalDate.now(),
            isLoading = false
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = PlannerUiState()
    )

    init {
        // Run dummy data generation and calculate initial streak
        viewModelScope.launch {
            repository.populateDummyDataIfEmpty()
            _streak.value = repository.getStreak(LocalDate.now())
        }
    }

    fun onTaskChecked(checkinId: Long, isCompleted: Boolean) {
        viewModelScope.launch {
            repository.updateTaskStatus(checkinId, isCompleted)
            // Recalculate streak after a task is checked, just in case they completed today's tasks
            // Actually, streak logic in repository calculates based on *past* checkins before today.
            // If we want today to count towards the streak, we'd adjust the query. 
            // For now, let's keep it strictly "past consecutive days".
            _streak.value = repository.getStreak(LocalDate.now())
        }
    }
}

class PlannerViewModelFactory(private val repository: PlannerRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(PlannerViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return PlannerViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
