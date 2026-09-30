package com.example.plannerapp.ui.detail

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.plannerapp.credits.CreditRepository
import com.example.plannerapp.data.PlannerRepository
import com.example.plannerapp.data.UserDao
import com.example.plannerapp.data.social.CloudUser
import com.example.plannerapp.data.social.CommunityPost
import com.example.plannerapp.data.social.PostComment
import com.example.plannerapp.data.social.SocialRepository
import com.example.plannerapp.data.social.VoteType
import com.example.plannerapp.data.template.PlanTemplateDto
import com.example.plannerapp.data.template.TaskTemplateDto
import com.google.gson.Gson
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate

data class PublicPlanDetailUiState(
    val post: CommunityPost? = null,
    val planTemplate: PlanTemplateDto? = null,
    val comments: List<PostComment> = emptyList(),
    val isFollowingCreator: Boolean = false,
    val isFollowingPlan: Boolean = false,
    val isSaved: Boolean = false,
    val isLiked: Boolean = false,
    val isUsing: Boolean = false,
    val joinedLocalPlanId: Long? = null,
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val toastMessage: String? = null
)

class PublicPlanDetailViewModel(
    val planId: String,
    private val socialRepository: SocialRepository,
    private val plannerRepository: PlannerRepository,
    private val userDao: UserDao,
    private val creditRepository: CreditRepository? = null,
    private val gson: Gson = Gson()
) : ViewModel() {

    private val _uiState = MutableStateFlow(PublicPlanDetailUiState())
    val uiState: StateFlow<PublicPlanDetailUiState> = _uiState.asStateFlow()

    init {
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch {
            socialRepository.getPostById(planId).collect { post ->
                if (post != null) {
                    val template = try {
                        gson.fromJson(post.planTemplateJson, PlanTemplateDto::class.java)?.let { parsed ->
                            if (parsed.title.isBlank()) {
                                parsed.copy(
                                    title = post.title,
                                    description = post.description,
                                    targetDurationDays = post.durationDays,
                                    tasks = parsed.tasks
                                )
                            } else {
                                parsed.copy(tasks = parsed.tasks)
                            }
                        }
                    } catch (e: Exception) { null } ?: com.example.plannerapp.data.template.PlanTemplateDto(
                        title = post.title,
                        description = post.description,
                        targetDurationDays = post.durationDays.coerceAtLeast(1),
                        defaultTaskDurationDays = 1,
                        tags = post.tags,
                        category = post.category,
                        author = com.example.plannerapp.data.template.AuthorDto(
                            userId = post.author.userId,
                            displayName = post.author.displayName,
                            avatarUrl = post.author.avatarUrl,
                            isCreator = post.author.isCreator
                        ),
                        tasks = emptyList()
                    )

                    _uiState.update {
                        it.copy(
                            post = post,
                            planTemplate = template,
                            isSaved = post.isSaved,
                            isLiked = post.userVote == VoteType.UP,
                            isLoading = false
                        )
                    }
                } else {
                    _uiState.update { it.copy(isLoading = false) }
                }
            }
        }

        viewModelScope.launch {
            socialRepository.getComments(planId).collect { comments ->
                _uiState.update { it.copy(comments = comments) }
            }
        }

        viewModelScope.launch {
            val user = userDao.getActiveUserOnce()
            val uid = user?.cloudUserId
            if (!uid.isNullOrBlank()) {
                socialRepository.isFollowingPlan(planId, uid).collect { isFollowing ->
                    _uiState.update { it.copy(isFollowingPlan = isFollowing) }
                }
            } else {
                _uiState.update { it.copy(isFollowingPlan = false) }
            }
        }
    }

    fun toggleSave() {
        viewModelScope.launch {
            try {
                val saveResult = socialRepository.toggleSavePost(planId)
                if (saveResult.isSuccess) {
                    val newSaved = saveResult.getOrDefault(false)
                    _uiState.update { it.copy(isSaved = newSaved, toastMessage = if (newSaved) "Plan saved" else "Plan unsaved") }
                } else {
                    _uiState.update { it.copy(errorMessage = saveResult.exceptionOrNull()?.message ?: "Failed to save plan") }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = e.message) }
            }
        }
    }

    fun toggleLike() {
        viewModelScope.launch {
            try {
                val voteResult = socialRepository.votePost(planId, VoteType.UP)
                if (voteResult.isSuccess) {
                    val updated = voteResult.getOrNull()
                    if (updated != null) {
                        _uiState.update { it.copy(post = updated, isLiked = updated.userVote == VoteType.UP) }
                    }
                } else {
                    _uiState.update { it.copy(errorMessage = voteResult.exceptionOrNull()?.message ?: "Failed to like plan") }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = e.message) }
            }
        }
    }

    fun toggleFollowPlan() {
        viewModelScope.launch {
            try {
                val user = userDao.getActiveUserOnce()
                val uid = user?.cloudUserId
                if (uid.isNullOrBlank()) {
                    _uiState.update { it.copy(errorMessage = "Please sign in to follow plans.") }
                    return@launch
                }
                val followResult = socialRepository.toggleFollowPlan(planId, uid)
                if (followResult.isSuccess) {
                    val newFollow = followResult.getOrDefault(false)
                    _uiState.update {
                        it.copy(
                            isFollowingPlan = newFollow,
                            toastMessage = if (newFollow) "Following plan updates" else "Unfollowed plan"
                        )
                    }
                } else {
                    _uiState.update { it.copy(errorMessage = followResult.exceptionOrNull()?.message ?: "Failed to follow plan") }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = e.message) }
            }
        }
    }

    fun toggleFollowCreator() {
        val authorId = uiState.value.post?.author?.userId ?: return
        viewModelScope.launch {
            try {
                val isCurrentlyFollowing = uiState.value.isFollowingCreator
                if (isCurrentlyFollowing) {
                    val res = socialRepository.unfollowCreator(authorId)
                    if (res.isSuccess) {
                        _uiState.update { it.copy(isFollowingCreator = false, toastMessage = "Unfollowed creator") }
                    } else {
                        _uiState.update { it.copy(errorMessage = res.exceptionOrNull()?.message ?: "Failed to unfollow creator") }
                    }
                } else {
                    val res = socialRepository.followCreator(authorId)
                    if (res.isSuccess) {
                        _uiState.update { it.copy(isFollowingCreator = true, toastMessage = "Following creator") }
                    } else {
                        _uiState.update { it.copy(errorMessage = res.exceptionOrNull()?.message ?: "Failed to follow creator") }
                    }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = e.message) }
            }
        }
    }

    fun usePlan(startDate: LocalDate = LocalDate.now()) {
        val currentPost = uiState.value.post ?: return
        val existingTemplate = uiState.value.planTemplate
        val template = if (existingTemplate != null && !existingTemplate.tasks.isNullOrEmpty()) {
            existingTemplate
        } else {
            val parsedFromPost = try {
                gson.fromJson(currentPost.planTemplateJson, PlanTemplateDto::class.java)
            } catch (e: Exception) { null }
            if (parsedFromPost != null && !parsedFromPost.tasks.isNullOrEmpty()) {
                parsedFromPost.copy(
                    title = parsedFromPost.title.ifBlank { currentPost.title.ifBlank { "Imported Plan" } },
                    description = parsedFromPost.description.ifBlank { currentPost.description },
                    targetDurationDays = if (parsedFromPost.targetDurationDays > 0) parsedFromPost.targetDurationDays else currentPost.durationDays.coerceAtLeast(1),
                    tasks = parsedFromPost.tasks
                )
            } else {
                existingTemplate ?: com.example.plannerapp.data.template.PlanTemplateDto(
                    title = currentPost.title.ifBlank { "Imported Plan" },
                    description = currentPost.description,
                    targetDurationDays = currentPost.durationDays.coerceAtLeast(1),
                    defaultTaskDurationDays = 1,
                    tags = currentPost.tags,
                    category = currentPost.category,
                    author = com.example.plannerapp.data.template.AuthorDto(
                        userId = currentPost.author.userId,
                        displayName = currentPost.author.displayName,
                        avatarUrl = currentPost.author.avatarUrl,
                        isCreator = currentPost.author.isCreator
                    ),
                    tasks = emptyList()
                )
            }
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isUsing = true) }
            try {
                val activeUser = userDao.getActiveUserOnce()
                val targetUserId = activeUser?.userId ?: 1L

                val result = plannerRepository.joinCommunityPlan(
                    postId = currentPost.postId,
                    template = template,
                    targetUserId = targetUserId,
                    startDate = startDate,
                    socialRepository = socialRepository,
                    creditRepository = creditRepository
                )

                if (result.isSuccess) {
                    val localPlanId = result.getOrThrow()
                    _uiState.update {
                        it.copy(
                            joinedLocalPlanId = localPlanId,
                            toastMessage = "Plan copied to your local planner with original creator attribution"
                        )
                    }
                } else {
                    _uiState.update { it.copy(errorMessage = result.exceptionOrNull()?.message ?: "Failed to import plan") }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = e.message) }
            } finally {
                _uiState.update { it.copy(isUsing = false) }
            }
        }
    }

    fun addComment(content: String, parentId: String? = null) {
        if (content.isBlank()) return
        viewModelScope.launch {
            try {
                val activeUser = userDao.getActiveUserOnce()
                val cloudId = activeUser?.cloudUserId
                if (cloudId.isNullOrBlank()) {
                    _uiState.update { it.copy(errorMessage = "Please sign in to comment on public plans.") }
                    return@launch
                }
                val author = CloudUser(
                    userId = cloudId,
                    username = activeUser.displayName.replace(" ", "_").lowercase(),
                    displayName = activeUser.displayName,
                    avatarUrl = activeUser.avatarUrl,
                    isCreator = activeUser.isCreator
                )
                val addResult = socialRepository.addComment(
                    postId = planId,
                    author = author,
                    content = content,
                    parentCommentId = parentId
                )
                if (addResult.isFailure) {
                    _uiState.update { it.copy(errorMessage = addResult.exceptionOrNull()?.message ?: "Failed to post comment") }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = e.message) }
            }
        }
    }

    fun reportPlan(reason: String) {
        viewModelScope.launch {
            try {
                val activeUser = userDao.getActiveUserOnce()
                val reporterId = activeUser?.cloudUserId
                if (reporterId.isNullOrBlank()) {
                    _uiState.update { it.copy(errorMessage = "Please sign in to submit a report.") }
                    return@launch
                }
                val reportResult = socialRepository.reportContent(
                    targetId = planId,
                    targetType = "PLAN",
                    reason = reason,
                    reporterUserId = reporterId
                )
                if (reportResult.isSuccess) {
                    _uiState.update { it.copy(toastMessage = "Report submitted. Thank you for keeping the community safe.") }
                } else {
                    _uiState.update { it.copy(errorMessage = reportResult.exceptionOrNull()?.message ?: "Failed to submit report") }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = e.message) }
            }
        }
    }

    fun clearToast() {
        _uiState.update { it.copy(toastMessage = null) }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    fun clearJoinedEvent() {
        _uiState.update { it.copy(joinedLocalPlanId = null) }
    }
}

class PublicPlanDetailViewModelFactory(
    private val planId: String,
    private val socialRepository: SocialRepository,
    private val plannerRepository: PlannerRepository,
    private val userDao: UserDao,
    private val creditRepository: CreditRepository? = null
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(PublicPlanDetailViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return PublicPlanDetailViewModel(planId, socialRepository, plannerRepository, userDao, creditRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
