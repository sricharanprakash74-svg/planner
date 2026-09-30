package com.example.plannerapp.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.plannerapp.data.JoinedCommunityEntity
import com.example.plannerapp.data.PlanEntity
import com.example.plannerapp.data.PlannerRepository
import com.example.plannerapp.data.TaskTemplateEntity
import com.example.plannerapp.data.UserDao
import com.example.plannerapp.data.UserEntity
import com.example.plannerapp.data.social.CloudUser
import com.example.plannerapp.data.social.SocialRepository
import com.example.plannerapp.data.template.PlanExporter
import com.google.gson.Gson
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

sealed class PublishStatus {
    object Idle : PublishStatus()
    object Loading : PublishStatus()
    data class Success(val postId: String) : PublishStatus()
    data class Error(val message: String) : PublishStatus()
}

class PublishPlanViewModel(
    private val repository: PlannerRepository,
    private val userDao: UserDao,
    private val socialRepository: SocialRepository,
    private val creditRepository: com.example.plannerapp.credits.CreditRepository? = null
) : ViewModel() {

    val activeUser: Flow<UserEntity?> = userDao.getActiveUser()

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

    suspend fun getTemplatesForPlan(planId: Long): List<TaskTemplateEntity> {
        return repository.getTemplatesForPlan(planId)
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
                val user = userDao.getActiveUserOnce() ?: throw IllegalStateException("No active user session")
                val cloudUid = com.example.plannerapp.auth.SupabaseConfig.auth.currentUserOrNull()?.id
                    ?: user.cloudUserId
                if (cloudUid.isNullOrBlank()) {
                    _publishStatus.value = PublishStatus.Error("Please sign in to publish plans to the online community.")
                    return@launch
                }

                val templates = repository.getTemplatesForPlan(planId)
                if (templates.isEmpty()) {
                    _publishStatus.value = PublishStatus.Error("Cannot publish an empty routine. Please add at least one task to this plan first.")
                    return@launch
                }

                val plan = repository.getPlansForUser(user.userId).first().find { it.planId == planId }
                    ?: throw IllegalStateException("Plan not found")

                val exporter = PlanExporter()
                val checkins = try { repository.getAllCheckinsForPlan(planId).first() } catch (e: Exception) { emptyList() }
                val templateDto = exporter.exportPlan(plan.copy(heading = title, description = description), templates, user, tags, category, checkins)
                val templateJson = Gson().toJson(templateDto)

                val cloudUser = CloudUser(
                    userId = cloudUid,
                    username = user.username ?: user.displayName.lowercase().replace(" ", "_"),
                    displayName = user.displayName,
                    avatarUrl = user.avatarUrl,
                    isCreator = user.isCreator,
                    bio = "",
                    followerCount = 0,
                    totalMembersJoined = 0
                )

                val postResult = socialRepository.createPost(
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

                if (postResult.isSuccess) {
                    val post = postResult.getOrThrow()

                    // Update local Room database so plan reflects its public status
                    repository.setPlanPublicStatus(planId, true)

                    // Link local plan to published community post
                    repository.insertJoinedCommunity(
                        JoinedCommunityEntity(
                            localPlanId = planId,
                            postId = post.postId,
                            communityTitle = title,
                            creatorName = user.displayName
                        )
                    )

                    creditRepository?.awardPlanShare(user.userId, post.postId)
                    _publishStatus.value = PublishStatus.Success(post.postId)
                } else {
                    _publishStatus.value = PublishStatus.Error(postResult.exceptionOrNull()?.message ?: "Publish failed")
                }
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
    private val socialRepository: SocialRepository,
    private val creditRepository: com.example.plannerapp.credits.CreditRepository? = null
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(PublishPlanViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return PublishPlanViewModel(repository, userDao, socialRepository, creditRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
