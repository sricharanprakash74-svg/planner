package com.example.plannerapp.billing

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

import android.app.Activity

/**
 * UI state for the RevenueCat Paywall screen.
 */
data class BillingUiState(
    val isProActive: Boolean = false,
    val selectedTier: String = BillingConfig.PACKAGE_ANNUAL, // "pro_annual" or "pro_monthly"
    val isPurchasing: Boolean = false,
    val isRestoring: Boolean = false,
    val userFeedbackMessage: String? = null
)

/**
 * ViewModel managing the Dummy Paywall logic and state.
 */
class BillingViewModel(
    private val repository: BillingRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        BillingUiState(isProActive = repository.isProActive.value)
    )
    val uiState: StateFlow<BillingUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            repository.isProActive.collect { isPro ->
                _uiState.update { it.copy(isProActive = isPro) }
            }
        }
    }

    fun selectTier(tier: String) {
        _uiState.update { it.copy(selectedTier = tier) }
    }

    fun purchase(activity: Activity? = null) {
        if (_uiState.value.isPurchasing) return

        _uiState.update { it.copy(isPurchasing = true, userFeedbackMessage = null) }

        val packageToBuy = _uiState.value.selectedTier

        repository.purchase(activity, packageToBuy) { success ->
            _uiState.update {
                it.copy(
                    isPurchasing = false,
                    userFeedbackMessage = if (success) {
                        "Pro Access successfully unlocked!"
                    } else {
                        "Purchase was not completed."
                    }
                )
            }
        }
    }

    fun restorePurchases() {
        if (_uiState.value.isRestoring) return

        _uiState.update { it.copy(isRestoring = true, userFeedbackMessage = null) }

        repository.restorePurchases { success ->
            _uiState.update {
                it.copy(
                    isRestoring = false,
                    userFeedbackMessage = if (success) {
                        "Purchases successfully restored!"
                    } else {
                        "No active subscriptions found to restore."
                    }
                )
            }
        }
    }

    fun clearFeedback() {
        _uiState.update { it.copy(userFeedbackMessage = null) }
    }

    fun resetMockStatus() {
        repository.resetMockStatus()
        _uiState.update { it.copy(isProActive = false, userFeedbackMessage = "Status reset to Free tier.") }
    }
}

/**
 * Factory providing repository dependency to BillingViewModel.
 */
class BillingViewModelFactory(
    private val context: Context
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(BillingViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return BillingViewModel(AppBillingRepository.getInstance(context)) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
