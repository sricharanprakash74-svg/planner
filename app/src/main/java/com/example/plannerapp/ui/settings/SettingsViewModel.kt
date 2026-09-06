package com.example.plannerapp.ui.settings

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.plannerapp.data.UserDao
import com.example.plannerapp.data.UserEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File

class SettingsViewModel(
    private val userDao: UserDao
) : ViewModel() {

    val avatarVersion = MutableStateFlow(System.currentTimeMillis())

    val currentUser: StateFlow<UserEntity?> = userDao.getActiveUser()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    fun updateProfile(displayName: String, avatarUrl: String?) {
        viewModelScope.launch {
            val user = userDao.getActiveUserOnce() ?: return@launch
            userDao.updateProfile(user.userId, displayName, avatarUrl)
        }
    }

    fun updateProfilePicture(context: Context, imageUri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val user = userDao.getActiveUserOnce() ?: return@launch
                val inputStream = context.contentResolver.openInputStream(imageUri) ?: return@launch
                val avatarFile = File(context.filesDir, "avatar_${user.userId}.jpg")
                avatarFile.outputStream().use { output ->
                    inputStream.copyTo(output)
                }
                userDao.updateProfile(user.userId, user.displayName, avatarFile.absolutePath)
                avatarVersion.value = System.currentTimeMillis()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun updateTimezone(timezone: String) {
        viewModelScope.launch {
            val user = userDao.getActiveUserOnce() ?: return@launch
            userDao.updateTimezone(user.userId, timezone)
        }
    }

    fun setCreatorStatus(isCreator: Boolean) {
        viewModelScope.launch {
            val user = userDao.getActiveUserOnce() ?: return@launch
            userDao.updateCreatorStatus(user.userId, isCreator)
        }
    }

    fun deleteAccount(onSuccess: () -> Unit) {
        viewModelScope.launch {
            val user = userDao.getActiveUserOnce()
            if (user != null) {
                userDao.deleteUser(user.userId)
                // Re-create a default guest
                userDao.insertUser(UserEntity(displayName = "Guest"))
            }
            onSuccess()
        }
    }

    fun simulateSignIn() {
        viewModelScope.launch {
            try {
                val user = userDao.getActiveUserOnce()
                if (user != null && user.cloudUserId == null) {
                    val response = com.example.plannerapp.data.NetworkClient.api.login(
                        com.example.plannerapp.data.LoginRequest(
                            firebaseToken = "mock_firebase_uid_123",
                            email = "rahul@gmail.com",
                            displayName = "Rahul Kumar"
                        )
                    )
                    
                    userDao.upgradeToCloudUser(
                        userId = user.userId, 
                        cloudUserId = response.id.toString(), 
                        email = response.email ?: "", 
                        displayName = response.displayName ?: "User"
                    )
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}

class SettingsViewModelFactory(
    private val userDao: UserDao
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SettingsViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return SettingsViewModel(userDao) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
