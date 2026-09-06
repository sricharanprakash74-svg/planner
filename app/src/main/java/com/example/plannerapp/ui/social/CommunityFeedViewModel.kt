package com.example.plannerapp.ui.social

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.plannerapp.data.social.CommunityPost
import com.example.plannerapp.data.social.FeedFilter
import com.example.plannerapp.data.social.SocialRepository
import com.example.plannerapp.data.social.VoteType
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class CommunityFeedUiState(
    val posts: List<CommunityPost> = emptyList(),
    val searchQuery: String = "",
    val selectedFilter: FeedFilter = FeedFilter.TRENDING,
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

@OptIn(ExperimentalCoroutinesApi::class)
class CommunityFeedViewModel(
    private val socialRepository: SocialRepository,
    initialQuery: String = ""
) : ViewModel() {

    private val _searchQuery = MutableStateFlow(initialQuery)
    private val _selectedFilter = MutableStateFlow(FeedFilter.TRENDING)
    private val _errorMessage = MutableStateFlow<String?>(null)

    val uiState: StateFlow<CommunityFeedUiState> = combine(
        _searchQuery,
        _selectedFilter,
        _errorMessage
    ) { query, filter, error ->
        Triple(query, filter, error)
    }.flatMapLatest { (query, filter, error) ->
        socialRepository.getPosts(filter = filter, query = query).map { posts ->
            CommunityFeedUiState(
                posts = posts,
                searchQuery = query,
                selectedFilter = filter,
                isLoading = false,
                errorMessage = error
            )
        }
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
