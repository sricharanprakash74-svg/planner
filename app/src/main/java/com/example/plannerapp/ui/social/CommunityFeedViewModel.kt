package com.example.plannerapp.ui.social

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.plannerapp.data.social.CommunityPost
import com.example.plannerapp.data.social.FeedFilter
import com.example.plannerapp.data.social.SocialRepository
import com.example.plannerapp.data.social.SocialSearchResult
import com.example.plannerapp.data.social.VoteType
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class CommunityFeedUiState(
    val posts: List<CommunityPost> = emptyList(),
    val searchQuery: String = "",
    val searchResults: SocialSearchResult = SocialSearchResult(),
    val isSearching: Boolean = false,
    val selectedFilter: FeedFilter = FeedFilter.TRENDING,
    val followingUserIds: Set<String> = emptySet(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
class CommunityFeedViewModel(
    private val socialRepository: SocialRepository,
    initialQuery: String = ""
) : ViewModel() {

    private val _searchQuery = MutableStateFlow(initialQuery)
    private val _selectedFilter = MutableStateFlow(FeedFilter.TRENDING)
    private val _errorMessage = MutableStateFlow<String?>(null)

    private val debouncedQuery = _searchQuery
        .debounce(250L)
        .distinctUntilChanged()

    private val searchFlow = debouncedQuery.flatMapLatest { query ->
        if (query.isBlank()) {
            flowOf(SocialSearchResult())
        } else {
            socialRepository.search(query)
        }
    }

    private val postsFeedFlow = combine(
        _selectedFilter,
        debouncedQuery
    ) { filter, query ->
        Pair(filter, query)
    }.flatMapLatest { (filter, query) ->
        if (query.isNotBlank()) {
            // When actively searching, avoid executing feed query concurrently
            flowOf(emptyList())
        } else {
            socialRepository.getPosts(filter = filter, query = "")
        }
    }

    private data class FeedParams(
        val query: String,
        val filter: FeedFilter,
        val error: String?
    )

    private val feedParamsFlow = combine(_searchQuery, _selectedFilter, _errorMessage) { query, filter, error ->
        FeedParams(query, filter, error)
    }

    val uiState: StateFlow<CommunityFeedUiState> = combine(
        postsFeedFlow,
        searchFlow,
        socialRepository.getFollowingList(),
        feedParamsFlow
    ) { posts, searchResults, following, params ->
        CommunityFeedUiState(
            posts = posts,
            searchQuery = params.query,
            searchResults = searchResults,
            isSearching = params.query.isNotBlank(),
            selectedFilter = params.filter,
            followingUserIds = following,
            isLoading = false,
            errorMessage = params.error
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = CommunityFeedUiState(searchQuery = initialQuery, isLoading = true)
    )

    fun onSearchQueryChanged(newQuery: String) {
        _searchQuery.value = newQuery
    }

    fun onFilterSelected(filter: FeedFilter) {
        _selectedFilter.value = filter
    }

    fun onVote(postId: String, voteType: VoteType) {
        viewModelScope.launch {
            try {
                socialRepository.votePost(postId, voteType)
            } catch (e: Exception) {
                _errorMessage.value = e.message
            }
        }
    }

    fun onToggleSave(postId: String) {
        viewModelScope.launch {
            try {
                socialRepository.toggleSavePost(postId)
            } catch (e: Exception) {
                _errorMessage.value = e.message
            }
        }
    }

    fun onToggleFollow(userId: String) {
        viewModelScope.launch {
            try {
                val isFollowing = uiState.value.followingUserIds.contains(userId)
                if (isFollowing) {
                    socialRepository.unfollowCreator(userId)
                } else {
                    socialRepository.followCreator(userId)
                }
            } catch (e: Exception) {
                _errorMessage.value = e.message
            }
        }
    }

    fun onReportContent(targetId: String, targetType: String, reason: String, reporterUserId: String = "local_user") {
        viewModelScope.launch {
            try {
                socialRepository.reportContent(targetId, targetType, reason, reporterUserId)
            } catch (e: Exception) {
                _errorMessage.value = e.message
            }
        }
    }

    fun onBlockUser(targetUserId: String, currentUserId: String = "local_user") {
        viewModelScope.launch {
            try {
                socialRepository.blockUser(targetUserId, currentUserId)
            } catch (e: Exception) {
                _errorMessage.value = e.message
            }
        }
    }

    fun clearError() {
        _errorMessage.value = null
    }
}

class CommunityFeedViewModelFactory(
    private val socialRepository: SocialRepository,
    private val initialQuery: String = ""
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(CommunityFeedViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return CommunityFeedViewModel(socialRepository, initialQuery) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

