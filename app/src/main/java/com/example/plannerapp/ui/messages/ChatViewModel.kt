package com.example.plannerapp.ui.messages

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.plannerapp.data.UserDao
import com.example.plannerapp.data.social.DirectMessage
import com.example.plannerapp.data.social.SocialRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class PendingMessage(
    val id: String,
    val content: String,
    val isFailed: Boolean = false,
    val isSending: Boolean = true
)

data class ChatUiState(
    val messages: List<DirectMessage> = emptyList(),
    val currentUserId: String = "",
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val pendingMessages: List<PendingMessage> = emptyList()
)

class ChatViewModel(
    val conversationId: String,
    val recipientUserId: String,
    private val socialRepository: SocialRepository,
    private val userDao: UserDao
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val user = userDao.getActiveUserOnce()
            val uid = user?.cloudUserId ?: user?.userId?.toString() ?: "local_user"
            _uiState.update { it.copy(currentUserId = uid) }

            socialRepository.getMessages(conversationId).collect { msgs ->
                _uiState.update { state ->
                    // Remove pending messages if they now appear in received messages
                    val receivedContents = msgs.map { it.content }.toSet()
                    state.copy(
                        messages = msgs,
                        isLoading = false,
                        pendingMessages = state.pendingMessages.filter { !receivedContents.contains(it.content) }
                    )
                }
            }
        }
    }

    fun sendMessage(content: String) {
        val trimmed = content.trim()
        if (trimmed.isBlank()) return

        val tempId = java.util.UUID.randomUUID().toString()
        val pending = PendingMessage(id = tempId, content = trimmed, isSending = true)
        _uiState.update { it.copy(pendingMessages = it.pendingMessages + pending, errorMessage = null) }

        viewModelScope.launch {
            try {
                val uid = _uiState.value.currentUserId
                socialRepository.sendMessage(
                    conversationId = conversationId,
                    senderId = uid,
                    content = trimmed
                )
                _uiState.update { state ->
                    state.copy(pendingMessages = state.pendingMessages.filter { it.id != tempId })
                }
            } catch (e: Exception) {
                _uiState.update { state ->
                    state.copy(
                        pendingMessages = state.pendingMessages.map {
                            if (it.id == tempId) it.copy(isSending = false, isFailed = true) else it
                        },
                        errorMessage = e.message
                    )
                }
            }
        }
    }

    fun retryMessage(pendingMessage: PendingMessage) {
        _uiState.update { state ->
            state.copy(
                pendingMessages = state.pendingMessages.map {
                    if (it.id == pendingMessage.id) it.copy(isSending = true, isFailed = false) else it
                },
                errorMessage = null
            )
        }
        viewModelScope.launch {
            try {
                val uid = _uiState.value.currentUserId
                socialRepository.sendMessage(
                    conversationId = conversationId,
                    senderId = uid,
                    content = pendingMessage.content
                )
                _uiState.update { state ->
                    state.copy(pendingMessages = state.pendingMessages.filter { it.id != pendingMessage.id })
                }
            } catch (e: Exception) {
                _uiState.update { state ->
                    state.copy(
                        pendingMessages = state.pendingMessages.map {
                            if (it.id == pendingMessage.id) it.copy(isSending = false, isFailed = true) else it
                        },
                        errorMessage = e.message
                    )
                }
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }
}

class ChatViewModelFactory(
    private val conversationId: String,
    private val recipientUserId: String,
    private val socialRepository: SocialRepository,
    private val userDao: UserDao
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ChatViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ChatViewModel(conversationId, recipientUserId, socialRepository, userDao) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
