package com.example.plannerapp.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.plannerapp.data.PlanEntity
import com.example.plannerapp.data.PlannerRepository
import com.example.plannerapp.data.UserDao
import com.example.plannerapp.data.social.CloudUser
import com.example.plannerapp.data.social.SocialRepository
import com.example.plannerapp.data.template.PlanExporter
import com.google.gson.Gson
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

sealed class PublishStatus {
    object Idle : PublishStatus()
    object Loading : PublishStatus()
    object Success : PublishStatus()
    data class Error(val message: String) : PublishStatus()
}

class PublishPlanViewModel(
    private val repository: PlannerRepository,
    private val userDao: UserDao,
    private val socialRepository: SocialRepository
) : ViewModel() {

    private val _plans = MutableStateFlow<List<PlanEntity>>(emptyList())
    val plans: StateFlow<List<PlanEntity>> = _plans

    private val _publishStatus = MutableStateFlow<PublishStatus>(PublishStatus.Idle)
    val publishStatus: StateFlow<PublishStatus> = _publishStatus

    init {
        viewModelScope.launch {
            val user = userDao.getActiveUserOnce() ?: return@launch
            _plans.value = repository.getPlansForUser(user.userId).first()
        }
    }

    fun publish(
        planId: Long,
        title: String,
        description: String,
        category: String,
        tags: List<String>,
        isPaid: Boolean = false,
        creditCost: Int = 0
    ) {
        viewModelScope.launch {
            _publishStatus.value = PublishStatus.Loading
            try {
                val user = userDao.getActiveUserOnce() ?: throw IllegalStateException("No active user")
                val templates = repository.getTemplatesForPlan(planId)
                val plan = repository.getPlansForUser(user.userId).first().find { it.planId == planId }
                    ?: throw IllegalStateException("Plan not found")

                val exporter = PlanExporter()
                val templateDto = exporter.exportPlan(plan.copy(heading = title, description = description), templates, user, tags, category)
                val templateJson = Gson().toJson(templateDto)

                val cloudUser = CloudUser(
                    userId = user.cloudUserId ?: user.userId.toString(),
                    username = user.displayName.lowercase().replace(" ", "_"),
                    displayName = user.displayName,
                    avatarUrl = user.avatarUrl,
                    isCreator = user.isCreator,
                    bio = "",
                    followerCount = 0,
                    totalMembersJoined = 0
                )

                socialRepository.createPost(
                    author = cloudUser,
                    title = title,
                    description = description,
                    planTemplateJson = templateJson,
                    durationDays = templateDto.targetDurationDays,
                    tags = tags,
                    category = category,
                    isPaid = isPaid,
                    creditCost = creditCost
                )

                _publishStatus.value = PublishStatus.Success
            } catch (e: Exception) {
                _publishStatus.value = PublishStatus.Error(e.message ?: "Publish failed")
            }
        }
    }

    fun resetStatus() { _publishStatus.value = PublishStatus.Idle }
}

class PublishPlanViewModelFactory(
    private val repository: PlannerRepository,
    private val userDao: UserDao,
    private val socialRepository: SocialRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(PublishPlanViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return PublishPlanViewModel(repository, userDao, socialRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
