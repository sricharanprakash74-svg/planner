package com.example.plannerapp.ui.messages

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.plannerapp.data.UserDao
import com.example.plannerapp.data.social.Conversation
import com.example.plannerapp.data.social.SocialRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class ConversationsUiState(
    val conversations: List<Conversation> = emptyList(),
    val searchQuery: String = "",
    val isLoading: Boolean = true,
    val errorMessage: String? = null
)

class ConversationsViewModel(
    private val socialRepository: SocialRepository,
    private val userDao: UserDao
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    private val _errorMessage = MutableStateFlow<String?>(null)
    private val _isLoading = MutableStateFlow(true)

    private val _conversationsFlow = flow {
        val user = userDao.getActiveUserOnce()
        val uid = user?.cloudUserId ?: user?.userId?.toString() ?: "local_user"
        emitAll(socialRepository.getConversations(uid))
    }

    val uiState: StateFlow<ConversationsUiState> = combine(
        _conversationsFlow,
        _searchQuery,
        _isLoading,
        _errorMessage
    ) { conversations, query, loading, error ->
        val filtered = if (query.isBlank()) {
            conversations
        } else {
            conversations.filter {
                it.participant?.displayName?.contains(query, ignoreCase = true) == true ||
                it.participant?.username?.contains(query, ignoreCase = true) == true ||
                it.lastMessage?.content?.contains(query, ignoreCase = true) == true
            }
        }
        ConversationsUiState(
            conversations = filtered,
            searchQuery = query,
            isLoading = false,
            errorMessage = error
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = ConversationsUiState(isLoading = true)
    )

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
    }

    fun clearError() {
        _errorMessage.value = null
    }
}

class ConversationsViewModelFactory(
    private val socialRepository: SocialRepository,
    private val userDao: UserDao
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ConversationsViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ConversationsViewModel(socialRepository, userDao) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
