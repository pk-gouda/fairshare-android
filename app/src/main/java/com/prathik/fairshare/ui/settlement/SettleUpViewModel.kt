package com.prathik.fairshare.ui.settlement

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.prathik.fairshare.data.local.EncryptedTokenStore
import com.prathik.fairshare.domain.model.ApiResult
import com.prathik.fairshare.domain.model.errorMessage
import com.prathik.fairshare.domain.model.SettleType
import com.prathik.fairshare.domain.repository.BalanceRepository
import com.prathik.fairshare.domain.repository.FriendRepository
import com.prathik.fairshare.domain.usecase.settlement.SettleUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID
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

    /** All currencies in which a balance exists with otherUser (in this group if groupId set). */
    private val _availableCurrencies = MutableStateFlow<List<String>>(emptyList())
    val availableCurrencies: StateFlow<List<String>> = _availableCurrencies.asStateFlow()

    /** The currency currently selected for settlement. Defaults to largest absolute balance. */
    private val _activeCurrency = MutableStateFlow(selectedCurrency ?: tokenStore.getPreferredCurrency())
    val activeCurrency: StateFlow<String> = _activeCurrency.asStateFlow()

    fun setActiveCurrency(currency: String) {
        _activeCurrency.value = currency
        // Recompute balance for newly selected currency
        val balances = _cachedBalances.value
        val entry = balances.filter { it.currency == currency }
        _balanceAmount.value = if (overridePayerId == null) entry.sumOf { it.amount } else 0.0
        _balanceCurrency.value = currency
    }

    /** Raw balance list cached so setActiveCurrency can recompute without a network call. */
    private val _cachedBalances = MutableStateFlow<List<com.prathik.fairshare.domain.model.Balance>>(emptyList())

    /**
     * Idempotency key for the current settlement action.
     *
     * Generated once when [settleAll] or [settlePartial] is first called.
     * The same key is reused on retry (e.g. after a network failure) so the
     * backend can safely deduplicate the request.
     *
     * Reset to a new UUID after:
     * - Successful settlement (action completed, next tap is a new action).
     * - Non-retryable failure (Conflict, ValidationError, etc.) — the user must
     *   adjust input before retrying, so a new key is appropriate.
     *
     * NOT reset on NetworkError — a transient network failure is a retry of the
     * same user-intended action, so the same key must be sent.
     *
     * NOT generated on recomposition — the ViewModel survives config changes,
     * so this field is stable across screen rotations.
     */
    private var settleIdempotencyKey: String = UUID.randomUUID().toString()

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
                        _cachedBalances.value = scoped
                        val currencies = scoped.map { it.currency }.distinct()
                        _availableCurrencies.value = currencies
                        // Pick active currency: selectedCurrency arg > largest absolute balance
                        val active = selectedCurrency?.takeIf { it in currencies }
                            ?: scoped.maxByOrNull { kotlin.math.abs(it.amount) }?.currency
                            ?: tokenStore.getPreferredCurrency()
                        _activeCurrency.value = active
                        _balanceCurrency.value = active
                        val activeBalances = scoped.filter { it.currency == active }
                        if (overridePayerId == null) _balanceAmount.value = activeBalances.sumOf { it.amount }
                        val name = scoped.firstOrNull()?.otherUserName
                        if (!name.isNullOrBlank()) _otherUserName.value = name
                    }
                    else -> Unit
                }
            } else {
                when (val result = balanceRepository.getNetBalanceWithUser(otherUserId)) {
                    is ApiResult.Success -> {
                        _cachedBalances.value = result.data
                        val currencies = result.data.map { it.currency }.distinct()
                        _availableCurrencies.value = currencies
                        val active = selectedCurrency?.takeIf { it in currencies }
                            ?: result.data.maxByOrNull { kotlin.math.abs(it.amount) }?.currency
                            ?: tokenStore.getPreferredCurrency()
                        _activeCurrency.value = active
                        _balanceCurrency.value = active
                        val activeBalances = result.data.filter { it.currency == active }
                        if (overridePayerId == null) _balanceAmount.value = activeBalances.sumOf { it.amount }
                        val name = result.data.firstOrNull()?.otherUserName
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
            val currency = _activeCurrency.value

            when (val result = settleUseCase(
                otherUserId    = otherUserId,
                type           = type,
                groupId        = groupId,
                amount         = amount,
                currency       = currency,
                paymentMethod  = _paymentMethod.value.ifBlank { null },
                notes          = _notes.value.ifBlank { null },
                payerId        = payerId,
                idempotencyKey = settleIdempotencyKey,
            )) {
                is ApiResult.Success -> {
                    // Action completed — generate a fresh key so any future settlement
                    // (e.g. user settles again after coming back) is treated as a new action.
                    settleIdempotencyKey = UUID.randomUUID().toString()
                    _uiState.value = SettleUpUiState.Success
                }
                is ApiResult.NetworkError -> {
                    // Transient failure — retain the same key so a retry sends the same
                    // idempotency key and the backend can deduplicate if needed.
                    _uiState.value = SettleUpUiState.Error("No internet connection.")
                }
                else -> {
                    // Non-retryable failure (Conflict, ValidationError, etc.) — reset key
                    // because the user must change something before the next attempt.
                    settleIdempotencyKey = UUID.randomUUID().toString()
                    _uiState.value = SettleUpUiState.Error(
                        result.errorMessage() ?: "Failed to record settlement."
                    )
                }
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