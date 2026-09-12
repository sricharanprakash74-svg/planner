package com.example.plannerapp.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.plannerapp.data.BadgeDao
import com.example.plannerapp.data.BadgeEntity
import com.example.plannerapp.data.PlanEntity
import com.example.plannerapp.data.PlannerRepository
import com.example.plannerapp.data.UserDao
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

data class ActivityLogItem(
    val type: ActivityType,
    val title: String,
    val subtitle: String,
    val timestampMs: Long
)

enum class ActivityType { TASK_COMPLETED, PLAN_CREATED, BADGE_EARNED }

class ActivityLogViewModel(
    private val repository: PlannerRepository,
    private val userDao: UserDao,
    private val badgeDao: BadgeDao
) : ViewModel() {

    private val _items = MutableStateFlow<List<ActivityLogItem>>(emptyList())
    val items: StateFlow<List<ActivityLogItem>> = _items

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading

    init {
        load()
    }

    private fun load() {
        viewModelScope.launch {
            val user = userDao.getActiveUserOnce() ?: run {
                _isLoading.value = false
                return@launch
            }

            val combined = mutableListOf<ActivityLogItem>()

            // Task completions from the last 30 days
            val checkins = repository.getRecentCheckins(user.userId, 30).first()
            checkins.filter { it.isCompleted && it.completedAt != null }.forEach { c ->
                combined.add(
                    ActivityLogItem(
                        type = ActivityType.TASK_COMPLETED,
                        title = "Completed a task",
                        subtitle = c.exactDate,
                        timestampMs = c.completedAt ?: c.checkinId * 1000L
                    )
                )
            }

            // Plans created
            val plans: List<PlanEntity> = repository.getPlansForUser(user.userId).first()
            plans.forEach { p ->
                combined.add(
                    ActivityLogItem(
                        type = ActivityType.PLAN_CREATED,
                        title = "Created plan: ${p.heading}",
                        subtitle = p.startDate,
                        timestampMs = p.createdAt
                    )
                )
            }

            // Badges
            val badges: List<BadgeEntity> = badgeDao.getBadgesForUser(user.userId).first()
            badges.forEach { b ->
                combined.add(
                    ActivityLogItem(
                        type = ActivityType.BADGE_EARNED,
                        title = "Earned badge: ${b.badgeType.replace('_', ' ').lowercase().replaceFirstChar { it.uppercaseChar() }}",
                        subtitle = "",
                        timestampMs = b.unlockedAt
                    )
                )
            }

            _items.value = combined.sortedByDescending { it.timestampMs }.take(50)
            _isLoading.value = false
        }
    }
}

class ActivityLogViewModelFactory(
    private val repository: PlannerRepository,
    private val userDao: UserDao,
    private val badgeDao: BadgeDao
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ActivityLogViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ActivityLogViewModel(repository, userDao, badgeDao) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
