package com.prathik.fairshare.ui.settlement

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.prathik.fairshare.domain.model.ApiResult
import com.prathik.fairshare.domain.model.Settlement
import com.prathik.fairshare.domain.repository.SettlementRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class EditSettlementViewModel @Inject constructor(
    private val settlementRepository: SettlementRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    val settlementId: String = checkNotNull(savedStateHandle["settlementId"])

    private val _loadState = MutableStateFlow<EditSettlementLoadState>(EditSettlementLoadState.Loading)
    val loadState: StateFlow<EditSettlementLoadState> = _loadState.asStateFlow()

    private val _saveState = MutableStateFlow<EditSettlementSaveState>(EditSettlementSaveState.Idle)
    val saveState: StateFlow<EditSettlementSaveState> = _saveState.asStateFlow()

    // Editable fields
    private val _amount        = MutableStateFlow("")
    val amount: StateFlow<String> = _amount.asStateFlow()

    private val _notes         = MutableStateFlow("")
    val notes: StateFlow<String> = _notes.asStateFlow()

    private val _paymentMethod = MutableStateFlow("Cash")
    val paymentMethod: StateFlow<String> = _paymentMethod.asStateFlow()

    // Original settlement — needed to know currency and original values
    private var originalSettlement: Settlement? = null

    init { load() }

    private fun load() {
        viewModelScope.launch {
            // Step 1: Render from cache immediately — no network wait.
            val cached = settlementRepository.getCachedSettlement(settlementId)
            if (cached != null) {
                originalSettlement   = cached
                _amount.value        = cached.amount.toBigDecimal().stripTrailingZeros().toPlainString()
                _notes.value         = cached.notes ?: ""
                _paymentMethod.value = cached.paymentMethod ?: "Cash"
                _loadState.value     = EditSettlementLoadState.Success(cached)
            }
            // Step 2: Network fetch — updates form only if cache was absent
            // (never overwrites after the form is shown, to avoid losing user edits).
            when (val result = settlementRepository.getSettlementById(settlementId)) {
                is ApiResult.Success -> {
                    val s = result.data
                    if (cached == null) {
                        // First cold load — populate form from network
                        originalSettlement   = s
                        _amount.value        = s.amount.toBigDecimal().stripTrailingZeros().toPlainString()
                        _notes.value         = s.notes ?: ""
                        _paymentMethod.value = s.paymentMethod ?: "Cash"
                    } else {
                        // Cache already populated — only refresh originalSettlement ref silently
                        originalSettlement = s
                    }
                    _loadState.value = EditSettlementLoadState.Success(s)
                }
                is ApiResult.NetworkError -> {
                    if (_loadState.value !is EditSettlementLoadState.Success) {
                        _loadState.value = EditSettlementLoadState.Error("No internet connection.")
                    }
                }
                else -> {
                    if (_loadState.value !is EditSettlementLoadState.Success) {
                        _loadState.value = EditSettlementLoadState.Error("Failed to load settlement.")
                    }
                }
            }
        }
    }

    fun onAmountChanged(value: String)        { _amount.value = value }
    fun onNotesChanged(value: String)         { _notes.value = value }
    fun onPaymentMethodChanged(value: String) { _paymentMethod.value = value }

    fun save() {
        val amt = _amount.value.toDoubleOrNull()
        if (amt == null || amt <= 0) {
            _saveState.value = EditSettlementSaveState.Error("Please enter a valid amount.")
            return
        }
        viewModelScope.launch {
            _saveState.value = EditSettlementSaveState.Loading
            when (val result = settlementRepository.updateSettlement(
                settlementId  = settlementId,
                amount        = amt,
                notes         = _notes.value.ifBlank { null },
                paymentMethod = _paymentMethod.value.ifBlank { null },
            )) {
                is ApiResult.Success -> _saveState.value = EditSettlementSaveState.Saved
                is ApiResult.NetworkError -> _saveState.value =
                    EditSettlementSaveState.Error("No internet connection.")
                else -> _saveState.value =
                    EditSettlementSaveState.Error("Failed to update settlement.")
            }
        }
    }

    fun resetSaveState() { _saveState.value = EditSettlementSaveState.Idle }
}

sealed class EditSettlementLoadState {
    object Loading : EditSettlementLoadState()
    data class Success(val settlement: Settlement) : EditSettlementLoadState()
    data class Error(val message: String) : EditSettlementLoadState()
}

sealed class EditSettlementSaveState {
    object Idle    : EditSettlementSaveState()
    object Loading : EditSettlementSaveState()
    object Saved   : EditSettlementSaveState()
    data class Error(val message: String) : EditSettlementSaveState()
}