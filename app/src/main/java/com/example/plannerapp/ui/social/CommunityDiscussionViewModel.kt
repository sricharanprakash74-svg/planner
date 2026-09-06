package com.example.plannerapp.ui.social

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.plannerapp.data.PlannerRepository
import com.example.plannerapp.data.UserDao
import com.example.plannerapp.data.social.CloudUser
import com.example.plannerapp.data.social.CommunityPost
import com.example.plannerapp.data.social.PostComment
import com.example.plannerapp.data.social.SocialRepository
import com.example.plannerapp.data.social.VoteType
import com.example.plannerapp.data.template.PlanImporter
import com.example.plannerapp.data.template.PlanTemplateDto
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate

data class CommunityDiscussionUiState(
    val post: CommunityPost? = null,
    val planTemplate: PlanTemplateDto? = null,
    val comments: List<PostComment> = emptyList(),
    val isJoining: Boolean = false,
    val joinedLocalPlanId: Long? = null,
    val localPlanAlreadyJoinedId: Long? = null,
    val replyToComment: PostComment? = null,
    val errorMessage: String? = null
)

class CommunityDiscussionViewModel(
    private val postId: String,
    private val socialRepository: SocialRepository,
    private val plannerRepository: PlannerRepository,
    private val userDao: UserDao
) : ViewModel() {

    private val planImporter = PlanImporter()

    private data class InternalState(
        val isJoining: Boolean = false,
        val joinedLocalPlanId: Long? = null,
        val replyToComment: PostComment? = null,
        val errorMessage: String? = null
    )

    private val _internalState = MutableStateFlow(InternalState())

    val uiState: StateFlow<CommunityDiscussionUiState> = combine(
        socialRepository.getPostById(postId),
        socialRepository.getComments(postId),
        plannerRepository.getJoinedCommunityByPostIdFlow(postId),
        _internalState
    ) { post, comments, existingJoined, internal ->
        val template = post?.let {
            planImporter.parseJson(it.planTemplateJson).getOrNull()
        }

        CommunityDiscussionUiState(
            post = post,
            planTemplate = template,
            comments = comments,
            isJoining = internal.isJoining,
            joinedLocalPlanId = internal.joinedLocalPlanId,
            localPlanAlreadyJoinedId = existingJoined?.localPlanId,
            replyToComment = internal.replyToComment,
            errorMessage = internal.errorMessage
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = CommunityDiscussionUiState()
    )

    fun onVote(voteType: VoteType) {
        viewModelScope.launch {
            try {
                socialRepository.votePost(postId, voteType)
            } catch (e: Exception) {
                _internalState.update { it.copy(errorMessage = e.message) }
            }
        }
    }

    fun onSetReplyTo(comment: PostComment?) {
        _internalState.update { it.copy(replyToComment = comment) }
    }

    fun postComment(content: String) {
        if (content.isBlank()) return
        viewModelScope.launch {
            try {
                val activeUser = userDao.getActiveUserOnce()
                val author = CloudUser(
                    userId = activeUser?.cloudUserId ?: activeUser?.userId?.toString() ?: "local_user",
                    username = activeUser?.displayName?.replace(" ", "_")?.lowercase() ?: "planner_user",
                    displayName = activeUser?.displayName ?: "Guest User",
                    avatarUrl = activeUser?.avatarUrl,
                    isCreator = activeUser?.isCreator ?: false
                )

                val parentId = _internalState.value.replyToComment?.commentId
                socialRepository.addComment(
                    postId = postId,
                    author = author,
                    content = content,
                    parentCommentId = parentId
                )
                _internalState.update { it.copy(replyToComment = null) }
            } catch (e: Exception) {
                _internalState.update { it.copy(errorMessage = e.message) }
            }
        }
    }

    fun joinPlan(startDate: LocalDate = LocalDate.now()) {
        viewModelScope.launch {
            val currentPost = uiState.value.post ?: return@launch
            val template = uiState.value.planTemplate ?: return@launch
            _internalState.update { it.copy(isJoining = true) }

            try {
                val activeUser = userDao.getActiveUserOnce()
                val targetUserId = activeUser?.userId ?: 1L

                val result = plannerRepository.joinCommunityPlan(
                    postId = currentPost.postId,
                    template = template,
                    targetUserId = targetUserId,
                    startDate = startDate,
                    socialRepository = socialRepository
                )

                if (result.isSuccess) {
                    val localPlanId = result.getOrThrow()
                    _internalState.update { it.copy(joinedLocalPlanId = localPlanId) }
                } else {
                    _internalState.update { it.copy(errorMessage = result.exceptionOrNull()?.message ?: "Failed to import plan") }
                }
            } catch (e: Exception) {
                _internalState.update { it.copy(errorMessage = e.message) }
            } finally {
                _internalState.update { it.copy(isJoining = false) }
            }
        }
    }

    fun clearJoinedEvent() {
        _internalState.update { it.copy(joinedLocalPlanId = null) }
    }

    fun clearError() {
        _internalState.update { it.copy(errorMessage = null) }
    }
}

class CommunityDiscussionViewModelFactory(
    private val postId: String,
    private val socialRepository: SocialRepository,
    private val plannerRepository: PlannerRepository,
    private val userDao: UserDao
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(CommunityDiscussionViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return CommunityDiscussionViewModel(postId, socialRepository, plannerRepository, userDao) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
