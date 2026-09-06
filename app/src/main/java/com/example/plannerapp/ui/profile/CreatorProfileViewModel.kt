package com.example.plannerapp.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.plannerapp.data.social.CloudUser
import com.example.plannerapp.data.social.CommunityPost
import com.example.plannerapp.data.social.SocialRepository
import com.example.plannerapp.data.social.VoteType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class CreatorProfileUiState(
    val creator: CloudUser? = null,
    val posts: List<CommunityPost> = emptyList(),
    val isFollowing: Boolean = false,
    val isLoading: Boolean = true,
    val errorMessage: String? = null
)

class CreatorProfileViewModel(
    private val userId: String,
    private val socialRepository: SocialRepository
) : ViewModel() {

    private val _error = MutableStateFlow<String?>(null)

    val uiState: StateFlow<CreatorProfileUiState> = combine(
        socialRepository.getUserProfile(userId),
        socialRepository.getPostsByCreator(userId),
        socialRepository.isFollowingCreator(userId),
        _error
    ) { user, posts, isFollowing, errorMsg ->
        CreatorProfileUiState(
            creator = user,
            posts = posts,
            isFollowing = isFollowing,
            isLoading = false,
            errorMessage = errorMsg
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
                _error.update { e.message ?: "Failed to update follow state" }
            }
        }
    }

    fun onVote(postId: String, voteType: VoteType) {
        viewModelScope.launch {
            try {
                socialRepository.votePost(postId, voteType)
            } catch (e: Exception) {
                _error.update { e.message }
            }
        }
    }

    fun toggleSave(postId: String) {
        viewModelScope.launch {
            try {
                socialRepository.toggleSavePost(postId)
            } catch (e: Exception) {
                _error.update { e.message }
            }
        }
    }

    fun clearError() {
        _error.update { null }
    }
}

class CreatorProfileViewModelFactory(
    private val userId: String,
    private val socialRepository: SocialRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(CreatorProfileViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return CreatorProfileViewModel(userId, socialRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
