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
                            parsed.copy(tasks = (parsed.tasks as? List<TaskTemplateDto>) ?: emptyList())
                        }
                    } catch (e: Exception) { null }

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
            val uid = user?.cloudUserId ?: user?.userId?.toString() ?: "local_user"
            socialRepository.isFollowingPlan(planId, uid).collect { isFollowing ->
                _uiState.update { it.copy(isFollowingPlan = isFollowing) }
            }
        }
    }

    fun toggleSave() {
        viewModelScope.launch {
            try {
                val newSaved = socialRepository.toggleSavePost(planId).getOrDefault(false)
                _uiState.update { it.copy(isSaved = newSaved, toastMessage = if (newSaved) "Plan saved" else "Plan unsaved") }
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = e.message) }
            }
        }
    }

    fun toggleLike() {
        viewModelScope.launch {
            try {
                val updated = socialRepository.votePost(planId, VoteType.UP).getOrNull()
                if (updated != null) {
                    _uiState.update { it.copy(post = updated, isLiked = updated.userVote == VoteType.UP) }
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
                val uid = user?.cloudUserId ?: user?.userId?.toString() ?: "local_user"
                val newFollow = socialRepository.toggleFollowPlan(planId, uid).getOrDefault(false)
                _uiState.update {
                    it.copy(
                        isFollowingPlan = newFollow,
                        toastMessage = if (newFollow) "Subscribed to plan updates" else "Unsubscribed from plan"
                    )
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
                    socialRepository.unfollowCreator(authorId)
                    _uiState.update { it.copy(isFollowingCreator = false, toastMessage = "Unfollowed creator") }
                } else {
                    socialRepository.followCreator(authorId)
                    _uiState.update { it.copy(isFollowingCreator = true, toastMessage = "Following creator") }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = e.message) }
            }
        }
    }

    fun usePlan(startDate: LocalDate = LocalDate.now()) {
        val currentPost = uiState.value.post ?: return
        val template = uiState.value.planTemplate ?: return

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
                val author = CloudUser(
                    userId = activeUser?.cloudUserId ?: activeUser?.userId?.toString() ?: "local_user",
                    username = activeUser?.displayName?.replace(" ", "_")?.lowercase() ?: "user",
                    displayName = activeUser?.displayName ?: "User",
                    avatarUrl = activeUser?.avatarUrl,
                    isCreator = activeUser?.isCreator ?: false
                )
                socialRepository.addComment(
                    postId = planId,
                    author = author,
                    content = content,
                    parentCommentId = parentId
                )
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = e.message) }
            }
        }
    }

    fun reportPlan(reason: String) {
        viewModelScope.launch {
            try {
                val activeUser = userDao.getActiveUserOnce()
                val reporterId = activeUser?.cloudUserId ?: activeUser?.userId?.toString() ?: "local_user"
                socialRepository.reportContent(
                    targetId = planId,
                    targetType = "PLAN",
                    reason = reason,
                    reporterUserId = reporterId
                )
                _uiState.update { it.copy(toastMessage = "Report submitted. Thank you for keeping the community safe.") }
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
