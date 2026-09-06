package com.example.plannerapp.ui.profile

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.plannerapp.data.DailyCheckinEntity
import com.example.plannerapp.data.PlanEntity
import com.example.plannerapp.data.PlannerRepository
import com.example.plannerapp.data.UserDao
import com.example.plannerapp.data.UserEntity
import com.example.plannerapp.ui.state.Resource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File
import java.time.LocalDate

data class ProfileUiState(
    val user: UserEntity? = null,
    val userPlans: List<PlanEntity> = emptyList(),
    val weeklyCheckins: List<DailyCheckinEntity> = emptyList(),
    val streak: Int = 0,
    val consistencyPercentage: Int = 0,
)

class ProfileViewModel(
    private val repository: PlannerRepository,
    private val userDao: UserDao
) : ViewModel() {

    private val _streak = MutableStateFlow(0)
    
    val currentUser: StateFlow<UserEntity?> = userDao.getActiveUser()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<Resource<ProfileUiState>> = combine(
        currentUser,
        currentUser.flatMapLatest { user -> 
            if (user != null) repository.getPlansForUser(user.userId) 
            else MutableStateFlow(emptyList()) 
        },
        currentUser.flatMapLatest { user -> 
            if (user != null) repository.getRecentCheckins(user.userId, 30) 
            else MutableStateFlow(emptyList()) 
        },
        _streak
    ) { user, plans, weeklyCheckins, streak ->
        
        // Calculate total consistency from weeklyCheckins
        val total = weeklyCheckins.size
        val completed = weeklyCheckins.count { it.isCompleted }
        val percentage = if (total > 0) ((completed.toFloat() / total) * 100).toInt() else 0

        Resource.Success(
            ProfileUiState(
                user = user,
                userPlans = plans,
                weeklyCheckins = weeklyCheckins,
                streak = streak,
                consistencyPercentage = percentage
            )
        ) as Resource<ProfileUiState>
    }
    .catch { e -> emit(Resource.Error(e.message ?: "Failed to load profile data")) }
    .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = Resource.Loading
    )

    val avatarVersion = MutableStateFlow(System.currentTimeMillis())

    init {
        viewModelScope.launch {
            try {
                val user = userDao.getActiveUserOnce()
                if (user != null) {
                    _streak.value = repository.getStreak(user.userId, LocalDate.now())
                }
            } catch (e: Exception) {
                // Ignore initial streak load error
            }
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

    fun updateDisplayName(name: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val user = userDao.getActiveUserOnce() ?: return@launch
                userDao.updateProfile(user.userId, name, user.avatarUrl)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}

class ProfileViewModelFactory(
    private val repository: PlannerRepository,
    private val userDao: UserDao
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ProfileViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ProfileViewModel(repository, userDao) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
