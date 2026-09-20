package com.example.plannerapp.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.plannerapp.data.social.CloudUser
import com.example.plannerapp.data.social.CommunityPost
import com.example.plannerapp.data.social.PrivacySettings
import com.example.plannerapp.data.social.RelationshipStatus
import com.example.plannerapp.data.social.SocialRepository
import com.example.plannerapp.data.social.VoteType
import com.example.plannerapp.data.UserDao
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private data class LocalProfileState(
    val isBlocked: Boolean = false,
    val errorMessage: String? = null,
    val toastMessage: String? = null
)

private data class CreatorMeta(
    val relationship: RelationshipStatus,
    val canMessage: Boolean,
    val privacySettings: PrivacySettings?,
    val local: LocalProfileState
)

data class CreatorProfileUiState(
    val creator: CloudUser? = null,
    val posts: List<CommunityPost> = emptyList(),
    val isFollowing: Boolean = false,
    val relationship: RelationshipStatus = RelationshipStatus.STRANGER,
    val privacySettings: PrivacySettings? = null,
    val canMessage: Boolean = true,
    val isPrivateAccount: Boolean = false,
    val isLoading: Boolean = true,
    val isBlocked: Boolean = false,
    val errorMessage: String? = null,
    val toastMessage: String? = null
)

class CreatorProfileViewModel(
    private val userId: String,
    private val socialRepository: SocialRepository,
    private val userDao: UserDao? = null
) : ViewModel() {

    private val _localState = MutableStateFlow(LocalProfileState())

    private val metaFlow: Flow<CreatorMeta> = combine(
        socialRepository.getViewerRelationship(userId),
        socialRepository.canMessageUser(userId),
        socialRepository.getPrivacySettings(userId),
        _localState
    ) { relationship, canMessage, privacy, local ->
        CreatorMeta(relationship, canMessage, privacy, local)
    }

    val uiState: StateFlow<CreatorProfileUiState> = combine(
        socialRepository.getUserProfile(userId),
        socialRepository.getPostsByCreator(userId),
        socialRepository.isFollowingCreator(userId),
        metaFlow
    ) { user, posts, isFollowing, meta ->
        val isBlocked = meta.relationship == RelationshipStatus.BLOCKED || meta.local.isBlocked
        val privacy = meta.privacySettings
        val isPrivateAccount = when (privacy?.profileVisibility?.uppercase()) {
            "PRIVATE" -> meta.relationship != RelationshipStatus.OWNER
            "FOLLOWERS_ONLY" -> meta.relationship != RelationshipStatus.FOLLOWING &&
                    meta.relationship != RelationshipStatus.MUTUAL &&
                    meta.relationship != RelationshipStatus.OWNER
            else -> false
        }

        val visiblePosts = if (isPrivateAccount || isBlocked) emptyList() else posts

        CreatorProfileUiState(
            creator = user,
            posts = visiblePosts,
            isFollowing = isFollowing,
            relationship = meta.relationship,
            privacySettings = privacy,
            canMessage = meta.canMessage && !isBlocked && meta.relationship != RelationshipStatus.OWNER,
            isPrivateAccount = isPrivateAccount,
            isLoading = false,
            isBlocked = isBlocked,
            errorMessage = meta.local.errorMessage,
            toastMessage = meta.local.toastMessage
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = CreatorProfileUiState(isLoading = true)
    )

    fun toggleFollow() {
        viewModelScope.launch {
            try {
                val currentFollowing = uiState.value.isFollowing
                if (currentFollowing) {
                    socialRepository.unfollowCreator(userId)
                } else {
                    socialRepository.followCreator(userId)
                }
            } catch (e: Exception) {
                _localState.update { it.copy(errorMessage = e.message ?: "Failed to update follow state") }
            }
        }
    }

    fun onVote(postId: String, voteType: VoteType) {
        viewModelScope.launch {
            try {
                socialRepository.votePost(postId, voteType)
            } catch (e: Exception) {
                _localState.update { it.copy(errorMessage = e.message) }
            }
        }
    }

    fun toggleSave(postId: String) {
        viewModelScope.launch {
            try {
                socialRepository.toggleSavePost(postId)
            } catch (e: Exception) {
                _localState.update { it.copy(errorMessage = e.message) }
            }
        }
    }

    fun reportCreator(reason: String) {
        viewModelScope.launch {
            try {
                val activeUser = userDao?.getActiveUserOnce()
                val currentUserId = activeUser?.cloudUserId ?: activeUser?.userId?.toString() ?: "local_user"
                socialRepository.reportContent(
                    targetId = userId,
                    targetType = "USER",
                    reason = reason,
                    reporterUserId = currentUserId
                )
                _localState.update { it.copy(toastMessage = "Report submitted. Thank you for keeping the community safe.") }
            } catch (e: Exception) {
                _localState.update { it.copy(errorMessage = e.message ?: "Failed to submit report") }
            }
        }
    }

    fun blockCreator(onBlocked: () -> Unit = {}) {
        viewModelScope.launch {
            try {
                val activeUser = userDao?.getActiveUserOnce()
                val currentUserId = activeUser?.cloudUserId ?: activeUser?.userId?.toString() ?: "local_user"
                socialRepository.blockUser(userId, currentUserId)
                _localState.update { it.copy(isBlocked = true, toastMessage = "Creator blocked") }
                onBlocked()
            } catch (e: Exception) {
                _localState.update { it.copy(errorMessage = e.message ?: "Failed to block creator") }
            }
        }
    }

    fun startConversation(onReady: (conversationId: String, recipientUserId: String, recipientName: String) -> Unit) {
        viewModelScope.launch {
            try {
                val activeUser = userDao?.getActiveUserOnce()
                val currentUserId = activeUser?.cloudUserId ?: activeUser?.userId?.toString() ?: "local_user"
                val result = socialRepository.getOrCreateConversation(currentUserId, userId)
                if (result.isSuccess) {
                    val conv = result.getOrThrow()
                    val recipientName = uiState.value.creator?.displayName ?: "User"
                    onReady(conv.id, userId, recipientName)
                } else {
                    _localState.update { it.copy(errorMessage = result.exceptionOrNull()?.message ?: "Failed to open conversation") }
                }
            } catch (e: Exception) {
                _localState.update { it.copy(errorMessage = e.message) }
            }
        }
    }

    fun clearError() {
        _localState.update { it.copy(errorMessage = null) }
    }

    fun clearToast() {
        _localState.update { it.copy(toastMessage = null) }
    }
}

class CreatorProfileViewModelFactory(
    private val userId: String,
    private val socialRepository: SocialRepository,
    private val userDao: UserDao? = null
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(CreatorProfileViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return CreatorProfileViewModel(userId, socialRepository, userDao) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
