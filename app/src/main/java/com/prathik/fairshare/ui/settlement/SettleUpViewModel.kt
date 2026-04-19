package com.prathik.fairshare.ui.settlement

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.prathik.fairshare.data.local.EncryptedTokenStore
import com.prathik.fairshare.domain.model.ApiResult
import com.prathik.fairshare.domain.model.SettleType
import com.prathik.fairshare.domain.repository.BalanceRepository
import com.prathik.fairshare.domain.repository.FriendRepository
import com.prathik.fairshare.domain.usecase.settlement.SettleUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettleUpViewModel @Inject constructor(
    private val settleUseCase    : SettleUseCase,
    private val balanceRepository: BalanceRepository,
    private val friendRepository : FriendRepository,
    private val tokenStore       : EncryptedTokenStore,
    savedStateHandle             : SavedStateHandle,
) : ViewModel() {

    val otherUserId    : String  = savedStateHandle.get<String>("otherUserId") ?: ""
    val groupId        : String? = savedStateHandle.get<String>("groupId")?.takeIf { it.isNotBlank() }
    val overridePayerId  : String? = savedStateHandle.get<String>("payerId")?.takeIf { it.isNotBlank() }
    val overridePayerName: String? = savedStateHandle.get<String>("payerName")?.takeIf { it.isNotBlank() }
    /** Optional currency filter — set when user picks a specific currency entry in the settle sheet. */
    val selectedCurrency : String? = savedStateHandle.get<String>("currency")?.takeIf { it.isNotBlank() }
    val currentUserId  : String? = tokenStore.getUserId()

    private val _notes = MutableStateFlow("")
    val notes: StateFlow<String> = _notes.asStateFlow()

    private val _amount = MutableStateFlow("")
    val amount: StateFlow<String> = _amount.asStateFlow()

    private val _paymentMethod = MutableStateFlow("Cash")
    val paymentMethod: StateFlow<String> = _paymentMethod.asStateFlow()

    private val _uiState = MutableStateFlow<SettleUpUiState>(SettleUpUiState.Idle)
    val uiState: StateFlow<SettleUpUiState> = _uiState.asStateFlow()

    private val _otherUserName = MutableStateFlow("")
    val otherUserName: StateFlow<String> = _otherUserName.asStateFlow()

    private val _payerName = MutableStateFlow("")
    val payerName: StateFlow<String> = _payerName.asStateFlow()

    /** Positive = they owe you. Negative = you owe them. */
    private val _balanceAmount = MutableStateFlow(0.0)
    val balanceAmount: StateFlow<Double> = _balanceAmount.asStateFlow()

    private val _balanceCurrency = MutableStateFlow(tokenStore.getPreferredCurrency())
    val balanceCurrency: StateFlow<String> = _balanceCurrency.asStateFlow()

    init {
        // Pre-populate payer name immediately from nav arg — no async needed
        if (overridePayerName != null) _payerName.value = overridePayerName
        // When direction is manually overridden, start with 0.00 immediately
        if (overridePayerId != null) _balanceAmount.value = 0.0
        loadBalance()
    }

    /**
     * Fetches the current balance with [otherUserId] to determine payment direction
     * and pre-fill the settle amount.
     *
     * Group settle → breakdown endpoint (per-group GroupBalance, filtered to this group).
     * Non-group settle → getNetBalanceWithUser — always a fresh network call.
     *   Why not getAllBalances()? It serves the Room cache and refreshes in the background.
     *   If an expense was just added, the settle screen would show the stale old balance
     *   until the background refresh completes. getNetBalanceWithUser always hits the network.
     */
    private fun loadBalance() {
        viewModelScope.launch {
            if (groupId != null) {
                when (val result = balanceRepository.getBreakdownWithUser(otherUserId)) {
                    is ApiResult.Success -> {
                        val scoped = result.data.filter { it.groupId == groupId }
                            .let { list ->
                                // Filter by currency when user selected a specific entry
                                if (selectedCurrency != null) list.filter { it.currency == selectedCurrency }
                                else list
                            }
                        if (overridePayerId == null) _balanceAmount.value = scoped.sumOf { it.amount }
                        _balanceCurrency.value = scoped.firstOrNull()?.currency
                            ?: tokenStore.getPreferredCurrency()
                        val name = scoped.firstOrNull()?.otherUserName
                        if (!name.isNullOrBlank()) _otherUserName.value = name
                    }
                    else -> Unit
                }
            } else {
                when (val result = balanceRepository.getNetBalanceWithUser(otherUserId)) {
                    is ApiResult.Success -> {
                        val filtered = if (selectedCurrency != null)
                            result.data.filter { it.currency == selectedCurrency }
                        else result.data
                        if (overridePayerId == null) _balanceAmount.value = filtered.sumOf { it.amount }
                        _balanceCurrency.value = filtered.firstOrNull()?.currency
                            ?: tokenStore.getPreferredCurrency()
                        val name = filtered.firstOrNull()?.otherUserName
                        if (!name.isNullOrBlank()) _otherUserName.value = name
                    }
                    else -> Unit
                }
            }
            // Always load names from friends cache — covers payer name and
            // fills otherUserName if balance API didn't return it
            loadNamesFromFriends()
        }
    }

    private suspend fun loadNamesFromFriends() {
        when (val friends = friendRepository.getFriends()) {
            is ApiResult.Success -> {
                // Always load otherUserName (fills in if balance didn't return it)
                val otherName = friends.data.find { it.id == otherUserId }?.fullName
                if (!otherName.isNullOrBlank()) _otherUserName.value = otherName

                // Always load payerName when override is a different user
                if (overridePayerId != null && overridePayerId != currentUserId) {
                    val pName = friends.data.find { it.id == overridePayerId }?.fullName
                    if (!pName.isNullOrBlank()) _payerName.value = pName
                }
            }
            else -> Unit
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

            val payerId = overridePayerId
                ?: if (_balanceAmount.value > 0) otherUserId else null

            // Bug fix: use _balanceCurrency.value, NOT tokenStore.getPreferredCurrency().
            // For PARTIAL type, the BE records the settlement in this currency and applies
            // the balance delta in that exact currency. Sending the user's preferred currency
            // (e.g. USD) when the actual balance is EUR creates a USD settlement that can never
            // be matched — the EUR balance stays unfixed and a phantom USD debt appears on the
            // other side.
            val currency = _balanceCurrency.value

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