package com.example.plannerapp.ui.notifications

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.plannerapp.data.UserDao
import com.example.plannerapp.data.social.SocialNotification
import com.example.plannerapp.data.social.SocialRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class NotificationsUiState(
    val notifications: List<SocialNotification> = emptyList(),
    val isLoading: Boolean = true,
    val errorMessage: String? = null
)

class NotificationsViewModel(
    private val socialRepository: SocialRepository,
    private val userDao: UserDao
) : ViewModel() {

    private val _uiState = MutableStateFlow(NotificationsUiState())
    val uiState: StateFlow<NotificationsUiState> = _uiState.asStateFlow()

    init {
        loadNotifications()
    }

    private fun loadNotifications() {
        viewModelScope.launch {
            val user = userDao.getActiveUserOnce()
            val uid = user?.cloudUserId ?: user?.userId?.toString() ?: "local_user"
            socialRepository.getNotifications(uid).collect { notifs ->
                _uiState.update { it.copy(notifications = notifs, isLoading = false) }
            }
        }
    }

    fun markAsRead(notificationId: String) {
        viewModelScope.launch {
            socialRepository.markNotificationAsRead(notificationId)
        }
    }

    fun markAllAsRead() {
        viewModelScope.launch {
            val user = userDao.getActiveUserOnce()
            val uid = user?.cloudUserId ?: user?.userId?.toString() ?: "local_user"
            socialRepository.markAllNotificationsAsRead(uid)
        }
    }
}

class NotificationsViewModelFactory(
    private val socialRepository: SocialRepository,
    private val userDao: UserDao
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(NotificationsViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return NotificationsViewModel(socialRepository, userDao) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
