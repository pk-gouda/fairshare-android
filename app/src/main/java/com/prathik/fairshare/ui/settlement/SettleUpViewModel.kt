package com.prathik.fairshare.ui.settlement

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.prathik.fairshare.data.local.EncryptedTokenStore
import com.prathik.fairshare.domain.model.ApiResult
import com.prathik.fairshare.domain.model.SettleType
import com.prathik.fairshare.domain.usecase.settlement.SettleUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettleUpViewModel @Inject constructor(
    private val settleUseCase: SettleUseCase,
    private val tokenStore   : EncryptedTokenStore,
    savedStateHandle         : SavedStateHandle,
) : ViewModel() {

    val otherUserId: String = savedStateHandle.get<String>("otherUserId") ?: ""
    val groupId    : String? = savedStateHandle.get<String>("groupId")?.takeIf { it.isNotBlank() }
    val currency   : String = tokenStore.getPreferredCurrency()

    private val _notes = MutableStateFlow("")
    val notes: StateFlow<String> = _notes.asStateFlow()

    private val _amount = MutableStateFlow("")
    val amount: StateFlow<String> = _amount.asStateFlow()

    private val _paymentMethod = MutableStateFlow("Cash")
    val paymentMethod: StateFlow<String> = _paymentMethod.asStateFlow()

    private val _uiState = MutableStateFlow<SettleUpUiState>(SettleUpUiState.Idle)
    val uiState: StateFlow<SettleUpUiState> = _uiState.asStateFlow()

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
            when (val result = settleUseCase(
                otherUserId   = otherUserId,
                type          = type,
                groupId       = groupId,
                amount        = amount,
                currency      = currency,
                paymentMethod = _paymentMethod.value.ifBlank { null },
                notes         = _notes.value.ifBlank { null },
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