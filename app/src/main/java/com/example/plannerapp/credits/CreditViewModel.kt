package com.example.plannerapp.credits

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.plannerapp.data.UserDao
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class CreditUiState(
    val balance: Int = 0,
    val availableFreezes: Int = 0,
    val transactions: List<CreditTransactionEntity> = emptyList(),
    val isBusy: Boolean = false,
    val message: String? = null
)

class CreditViewModel(
    private val repository: CreditRepository,
    private val userDao: UserDao
) : ViewModel() {

    private val _uiState = MutableStateFlow(CreditUiState())
    val uiState: StateFlow<CreditUiState> = _uiState.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val balanceFlow: StateFlow<Int> = userDao.getActiveUser()
        .flatMapLatest { user ->
            if (user != null) repository.observeBalance(user.userId)
            else flowOf(0)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    @OptIn(ExperimentalCoroutinesApi::class)
    val freezesFlow: StateFlow<Int> = userDao.getActiveUser()
        .flatMapLatest { user ->
            if (user != null) repository.observeAvailableFreezes(user.userId)
            else flowOf(0)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    @OptIn(ExperimentalCoroutinesApi::class)
    val transactionsFlow: StateFlow<List<CreditTransactionEntity>> = userDao.getActiveUser()
        .flatMapLatest { user ->
            if (user != null) repository.observeHistory(user.userId)
            else flowOf(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun clearMessage() {
        _uiState.value = _uiState.value.copy(message = null)
    }

    fun purchaseStreakFreeze(cost: Int = 150, onResult: (Boolean, String) -> Unit = { _, _ -> }) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isBusy = true)
            try {
                val user = userDao.getActiveUserOnce()
                if (user == null) {
                    onResult(false, "User profile not found")
                    _uiState.value = _uiState.value.copy(isBusy = false)
                    return@launch
                }

                val success = repository.buyStreakFreeze(user.userId, cost)
                if (success) {
                    onResult(true, "Streak Freeze acquired successfully")
                } else {
                    onResult(false, "Insufficient credits (Need $cost credits)")
                }
            } catch (e: Exception) {
                onResult(false, e.message ?: "Failed to purchase streak freeze")
            } finally {
                _uiState.value = _uiState.value.copy(isBusy = false)
            }
        }
    }

    fun redeemTier(tierName: String, cost: Int, onResult: (Boolean, String) -> Unit = { _, _ -> }) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isBusy = true)
            try {
                val user = userDao.getActiveUserOnce()
                if (user == null) {
                    onResult(false, "User profile not found")
                    _uiState.value = _uiState.value.copy(isBusy = false)
                    return@launch
                }

                val success = repository.redeemTierUpgrade(user.userId, tierName, cost)
                if (success) {
                    onResult(true, "Unlocked $tierName successfully")
                } else {
                    onResult(false, "Insufficient credits (Need $cost credits)")
                }
            } catch (e: Exception) {
                onResult(false, e.message ?: "Failed to unlock tier")
            } finally {
                _uiState.value = _uiState.value.copy(isBusy = false)
            }
        }
    }
}

class CreditViewModelFactory(
    private val repository: CreditRepository,
    private val userDao: UserDao
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(CreditViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return CreditViewModel(repository, userDao) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
