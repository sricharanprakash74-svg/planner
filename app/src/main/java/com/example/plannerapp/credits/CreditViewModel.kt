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

    val activeUserFlow: StateFlow<com.example.plannerapp.data.UserEntity?> = userDao.getActiveUser()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

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

    fun purchaseCredits(creditsAmount: Int, packName: String, onResult: (Boolean, String) -> Unit = { _, _ -> }) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isBusy = true)
            try {
                val user = userDao.getActiveUserOnce()
                if (user == null) {
                    onResult(false, "User profile not found")
                    return@launch
                }
                repository.buyCreditsPack(user.userId, creditsAmount, packName)
                onResult(true, "Added $creditsAmount Credits to your balance")
            } catch (e: Exception) {
                onResult(false, e.message ?: "Failed to complete purchase")
            } finally {
                _uiState.value = _uiState.value.copy(isBusy = false)
            }
        }
    }

    private val _stripeClientSecret = MutableStateFlow<String?>(null)
    val stripeClientSecret: StateFlow<String?> = _stripeClientSecret.asStateFlow()
    
    private var pendingPurchaseCredits = 0
    private var pendingPurchasePackName = ""

    fun startStripePurchase(priceUsd: Double, creditsAmount: Int, packName: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isBusy = true)
            try {
                val user = userDao.getActiveUserOnce()
                if (user == null) {
                    _uiState.value = _uiState.value.copy(isBusy = false, message = "User not found")
                    return@launch
                }
                
                val amountCents = (priceUsd * 100).toInt()
                val response = com.example.plannerapp.data.NetworkClient.api.createPaymentIntent(
                    com.example.plannerapp.data.CreateIntentPayload(
                        amount = amountCents,
                        userId = user.userId
                    )
                )
                
                pendingPurchaseCredits = creditsAmount
                pendingPurchasePackName = packName
                _stripeClientSecret.value = response.clientSecret
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isBusy = false, message = e.message ?: "Failed to initialize payment")
            } finally {
                _uiState.value = _uiState.value.copy(isBusy = false)
            }
        }
    }
    
    fun onStripePaymentSuccess() {
        if (pendingPurchaseCredits > 0) {
            purchaseCredits(pendingPurchaseCredits, pendingPurchasePackName) { success, msg ->
                _uiState.value = _uiState.value.copy(message = msg)
            }
            pendingPurchaseCredits = 0
            pendingPurchasePackName = ""
        }
        _stripeClientSecret.value = null
    }

    fun onStripePaymentCanceledOrFailed(msg: String) {
        pendingPurchaseCredits = 0
        pendingPurchasePackName = ""
        _stripeClientSecret.value = null
        _uiState.value = _uiState.value.copy(message = msg)
    }

    private val _creatorDashboard = MutableStateFlow<com.example.plannerapp.data.CreatorDashboardResponse?>(null)
    val creatorDashboard: StateFlow<com.example.plannerapp.data.CreatorDashboardResponse?> = _creatorDashboard.asStateFlow()

    fun loadCreatorDashboard() {
        viewModelScope.launch {
            val user = userDao.getActiveUserOnce() ?: return@launch
            val res = repository.fetchCreatorDashboard(user.userId)
            if (res.isSuccess) {
                _creatorDashboard.value = res.getOrNull()
            }
        }
    }

    fun updatePayoutSettings(
        method: String,
        account: String,
        onResult: (Boolean, String) -> Unit = { _, _ -> }
    ) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isBusy = true)
            try {
                val user = userDao.getActiveUserOnce()
                if (user == null) {
                    onResult(false, "User not found")
                    return@launch
                }
                val res = repository.updatePayoutSettings(user.userId, method, account)
                if (res.isSuccess) {
                    onResult(true, "Payout settings saved")
                    loadCreatorDashboard()
                } else {
                    onResult(false, res.exceptionOrNull()?.message ?: "Failed to save settings")
                }
            } catch (e: Exception) {
                onResult(false, e.message ?: "Failed to save settings")
            } finally {
                _uiState.value = _uiState.value.copy(isBusy = false)
            }
        }
    }

    fun requestCashOut(
        credits: Int,
        onResult: (Boolean, String) -> Unit = { _, _ -> }
    ) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isBusy = true)
            try {
                val user = userDao.getActiveUserOnce()
                if (user == null) {
                    onResult(false, "User not found")
                    return@launch
                }
                val current = _creatorDashboard.value
                val method = current?.payoutMethod ?: "PAYPAL"
                val account = current?.payoutAccount ?: ""
                if (account.isBlank()) {
                    onResult(false, "Please configure your payout account details first")
                    return@launch
                }
                val res = repository.requestCashOut(user.userId, credits, method, account)
                if (res.isSuccess) {
                    val body = res.getOrNull()
                    if (body?.success == true) {
                        onResult(true, body.message)
                        loadCreatorDashboard()
                    } else {
                        onResult(false, body?.message ?: "Cash out failed")
                    }
                } else {
                    onResult(false, res.exceptionOrNull()?.message ?: "Cash out failed")
                }
            } catch (e: Exception) {
                onResult(false, e.message ?: "Cash out failed")
            } finally {
                _uiState.value = _uiState.value.copy(isBusy = false)
            }
        }
    }

    fun unlockCreatorPlan(
        planTitle: String,
        cost: Int,
        creatorId: Long = 0L,
        planId: Long = 0L,
        onResult: (Boolean, String) -> Unit = { _, _ -> }
    ) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isBusy = true)
            try {
                val user = userDao.getActiveUserOnce()
                if (user == null) {
                    onResult(false, "User profile not found")
                    return@launch
                }
                val success = repository.spendCreditsOnCreatorPlan(
                    userId = user.userId,
                    planTitle = planTitle,
                    cost = cost,
                    creatorId = creatorId,
                    buyerName = user.displayName,
                    planId = planId
                )
                if (success) {
                    onResult(true, "Unlocked $planTitle successfully")
                } else {
                    onResult(false, "Insufficient credits (Need $cost credits)")
                }
            } catch (e: Exception) {
                onResult(false, e.message ?: "Failed to unlock creator plan")
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
