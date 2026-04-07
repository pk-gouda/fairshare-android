package com.prathik.fairshare.ui.settlement

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.prathik.fairshare.data.local.EncryptedTokenStore
import com.prathik.fairshare.domain.model.ApiResult
import com.prathik.fairshare.domain.model.SettleType
import com.prathik.fairshare.domain.usecase.balance.GetAllBalancesUseCase
import com.prathik.fairshare.domain.usecase.settlement.SettleUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettleUpViewModel @Inject constructor(
    private val settleUseCase       : SettleUseCase,
    private val getAllBalancesUseCase: GetAllBalancesUseCase,
    private val tokenStore          : EncryptedTokenStore,
    savedStateHandle                : SavedStateHandle,
) : ViewModel() {

    val otherUserId: String  = savedStateHandle.get<String>("otherUserId") ?: ""
    val groupId    : String? = savedStateHandle.get<String>("groupId")?.takeIf { it.isNotBlank() }
    val currency   : String  = tokenStore.getPreferredCurrency()

    private val _notes = MutableStateFlow("")
    val notes: StateFlow<String> = _notes.asStateFlow()

    private val _amount = MutableStateFlow("")
    val amount: StateFlow<String> = _amount.asStateFlow()

    private val _paymentMethod = MutableStateFlow("Cash")
    val paymentMethod: StateFlow<String> = _paymentMethod.asStateFlow()

    private val _uiState = MutableStateFlow<SettleUpUiState>(SettleUpUiState.Idle)
    val uiState: StateFlow<SettleUpUiState> = _uiState.asStateFlow()

    // Direction info — who owes whom and how much
    private val _otherUserName = MutableStateFlow("")
    val otherUserName: StateFlow<String> = _otherUserName.asStateFlow()

    /** Positive = they owe you. Negative = you owe them. */
    private val _balanceAmount = MutableStateFlow(0.0)
    val balanceAmount: StateFlow<Double> = _balanceAmount.asStateFlow()

    private val _balanceCurrency = MutableStateFlow("USD")
    val balanceCurrency: StateFlow<String> = _balanceCurrency.asStateFlow()

    init {
        loadBalance()
    }

    /**
     * Fetches the current balance with [otherUserId] to determine payment direction
     * and pre-fill the "Settle everything" amount label.
     */
    private fun loadBalance() {
        viewModelScope.launch {
            when (val result = getAllBalancesUseCase()) {
                is ApiResult.Success -> {
                    val relevant = if (groupId != null) {
                        result.data.filter { it.otherUserId == otherUserId && it.groupId == groupId }
                            .ifEmpty { result.data.filter { it.otherUserId == otherUserId } }
                    } else {
                        result.data.filter { it.otherUserId == otherUserId }
                    }
                    _otherUserName.value  = relevant.firstOrNull()?.otherUserName ?: ""
                    _balanceAmount.value  = relevant.sumOf { it.amount }
                    _balanceCurrency.value = relevant.firstOrNull()?.currency ?: currency
                }
                else -> Unit
            }
        }
    }

    fun onNotesChanged(value: String)         { _notes.value = value }
    fun onAmountChanged(value: String)        { _amount.value = value }
    fun onPaymentMethodChanged(value: String) { _paymentMethod.value = value }

    fun settleAll() {
        val type = if (groupId != null) SettleType.GROUP else SettleType.ALL
        settle(type = type, amount = null)
    }

    fun settlePartial() {
        val amt = _amount.value.toDoubleOrNull()
        if (amt == null || amt <= 0) {
            _uiState.value = SettleUpUiState.Error("Please enter a valid amount.")
            return
        }
        settle(type = SettleType.PARTIAL, amount = amt)
    }

    private fun settle(type: SettleType, amount: Double?) {
        viewModelScope.launch {
            _uiState.value = SettleUpUiState.Loading

            // When balanceAmount > 0, the other user owes us — they are the payer.
            // When balanceAmount <= 0, we owe them — we are the payer (payerId = null = default).
            val payerId = if (_balanceAmount.value > 0) otherUserId else null

            when (val result = settleUseCase(
                otherUserId   = otherUserId,
                type          = type,
                groupId       = groupId,
                amount        = amount,
                currency      = currency,
                paymentMethod = _paymentMethod.value.ifBlank { null },
                notes         = _notes.value.ifBlank { null },
                payerId       = payerId,
            )) {
                is ApiResult.Success      -> _uiState.value = SettleUpUiState.Success
                is ApiResult.NetworkError -> _uiState.value = SettleUpUiState.Error("No internet connection.")
                else                      -> _uiState.value = SettleUpUiState.Error("Failed to record settlement.")
            }
        }
    }

    fun resetUiState() { _uiState.value = SettleUpUiState.Idle }
}

sealed class SettleUpUiState {
    object Idle    : SettleUpUiState()
    object Loading : SettleUpUiState()
    object Success : SettleUpUiState()
    data class Error(val message: String) : SettleUpUiState()
}