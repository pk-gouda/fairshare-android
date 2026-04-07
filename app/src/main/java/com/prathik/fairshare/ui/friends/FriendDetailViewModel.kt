package com.prathik.fairshare.ui.friends

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.prathik.fairshare.domain.model.ApiResult
import com.prathik.fairshare.domain.model.Balance
import com.prathik.fairshare.domain.model.Expense
import com.prathik.fairshare.domain.model.Friend
import com.prathik.fairshare.domain.model.Settlement
import com.prathik.fairshare.domain.repository.BalanceRepository
import com.prathik.fairshare.domain.repository.ExpenseRepository
import com.prathik.fairshare.domain.repository.FriendRepository
import com.prathik.fairshare.domain.repository.SettlementRepository
import com.prathik.fairshare.domain.usecase.balance.GetAllBalancesUseCase
import com.prathik.fairshare.domain.usecase.settlement.GetSettlementHistoryUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class FriendDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val friendRepository: FriendRepository,
    private val expenseRepository: ExpenseRepository,
    private val getAllBalancesUseCase: GetAllBalancesUseCase,
    private val balanceRepository: BalanceRepository,
    private val getSettlementHistoryUseCase: GetSettlementHistoryUseCase,
    private val settlementRepository: SettlementRepository,
) : ViewModel() {

    val friendId: String = checkNotNull(savedStateHandle["friendId"])

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _friend = MutableStateFlow<Friend?>(null)
    val friend: StateFlow<Friend?> = _friend.asStateFlow()

    private val _friendStatus = MutableStateFlow<String?>(null)
    val friendStatus: StateFlow<String?> = _friendStatus.asStateFlow()

    private val _netBalance = MutableStateFlow(0.0)
    val netBalance: StateFlow<Double> = _netBalance.asStateFlow()

    private val _currency = MutableStateFlow("USD")
    val currency: StateFlow<String> = _currency.asStateFlow()

    // Per-group balance breakdown — shown in the balance card, not as expense rows
    private val _groupBalances = MutableStateFlow<List<Balance>>(emptyList())
    val groupBalances: StateFlow<List<Balance>> = _groupBalances.asStateFlow()

    // Settlement history with this friend — shown in the timeline
    private val _settlements = MutableStateFlow<List<Settlement>>(emptyList())
    val settlements: StateFlow<List<Settlement>> = _settlements.asStateFlow()

    private val _expensesState = MutableStateFlow<FriendExpensesState>(FriendExpensesState.Loading)
    val expensesState: StateFlow<FriendExpensesState> = _expensesState.asStateFlow()

    private val _actionState =
        MutableStateFlow<FriendDetailActionState>(FriendDetailActionState.Idle)
    val actionState: StateFlow<FriendDetailActionState> = _actionState.asStateFlow()

    init {
        loadData()
    }

    fun loadData() {
        viewModelScope.launch {
            if (_expensesState.value !is FriendExpensesState.Success) {
                _expensesState.value = FriendExpensesState.Loading
            }
            _isLoading.value = true

            val friendsDeferred  = async { friendRepository.getFriends() }
            val sentDeferred     = async { friendRepository.getSentRequests() }
            val balancesDeferred = async { getAllBalancesUseCase() }
            val breakdownDeferred = async { balanceRepository.getBreakdownWithUser(friendId) }
            val directDeferred      = async { expenseRepository.getDirectExpensesWithFriend(friendId) }
            val settlementsDeferred = async { getSettlementHistoryUseCase(friendId) }

            // Resolve friend
            val allFriends = (friendsDeferred.await() as? ApiResult.Success)?.data ?: emptyList()
            val found = allFriends.find { it.id == friendId }

            if (found != null) {
                _friend.value = found
                _friendStatus.value = when {
                    found.isPlaceholder -> "placeholder"
                    found.isInvited     -> "invited"
                    else                -> null
                }
            } else {
                val sent = (sentDeferred.await() as? ApiResult.Success)
                    ?.data?.find { it.receiverId == friendId }
                if (sent != null) {
                    _friend.value = Friend(sent.receiverId, sent.receiverName, "", null)
                    _friendStatus.value = "pending"
                }
            }

            // Net balance (global, across all groups + direct)
            when (val result = balancesDeferred.await()) {
                is ApiResult.Success -> {
                    val friendBalances = result.data.filter { it.otherUserId == friendId }
                    _netBalance.value = friendBalances.sumOf { it.amount }
                    _currency.value   = friendBalances.firstOrNull()?.currency ?: "USD"
                }
                else -> Unit
            }

            // Per-group breakdown — shown in the balance card
            when (val result = breakdownDeferred.await()) {
                is ApiResult.Success -> _groupBalances.value = result.data
                else -> Unit
            }

            // Only direct (non-group) expenses in the timeline
            val directExpenses =
                (directDeferred.await() as? ApiResult.Success)?.data ?: emptyList()

            // Settlement history with this friend
            when (val result = settlementsDeferred.await()) {
                is ApiResult.Success -> _settlements.value = result.data
                else -> Unit
            }

            _expensesState.value = FriendExpensesState.Success(
                directExpenses.sortedByDescending { it.expenseDate }
            )

            _isLoading.value = false
        }
    }

    fun refreshExpenses() {
        viewModelScope.launch {
            // Refresh net balance
            when (val result = getAllBalancesUseCase()) {
                is ApiResult.Success -> {
                    val friendBalances = result.data.filter { it.otherUserId == friendId }
                    _netBalance.value = friendBalances.sumOf { it.amount }
                    _currency.value   = friendBalances.firstOrNull()?.currency ?: "USD"
                }
                else -> Unit
            }

            // Refresh per-group breakdown
            when (val result = balanceRepository.getBreakdownWithUser(friendId)) {
                is ApiResult.Success -> _groupBalances.value = result.data
                else -> Unit
            }

            // Refresh direct expenses only
            when (val result = expenseRepository.getDirectExpensesWithFriend(friendId)) {
                is ApiResult.Success -> _expensesState.value = FriendExpensesState.Success(
                    result.data.sortedByDescending { it.expenseDate }
                )
                else -> Unit
            }

            // Refresh settlement history
            when (val result = getSettlementHistoryUseCase(friendId)) {
                is ApiResult.Success -> _settlements.value = result.data
                else -> Unit
            }
        }
    }

    fun deleteSettlement(settlementId: String) {
        viewModelScope.launch {
            when (val result = settlementRepository.deleteSettlement(settlementId)) {
                is ApiResult.Success -> {
                    // Remove from local list immediately
                    _settlements.value = _settlements.value.filter { it.id != settlementId }
                    // Refresh balances since delete reverses them
                    refreshExpenses()
                    _actionState.value = FriendDetailActionState.Success("Settlement deleted")
                }
                is ApiResult.NetworkError ->
                    _actionState.value = FriendDetailActionState.Error("No internet connection.")
                else ->
                    _actionState.value = FriendDetailActionState.Error("Failed to delete settlement.")
            }
        }
    }

    fun resetActionState() {
        _actionState.value = FriendDetailActionState.Idle
    }
}

sealed class FriendExpensesState {
    object Loading : FriendExpensesState()
    data class Success(val expenses: List<Expense>) : FriendExpensesState()
    data class Error(val message: String) : FriendExpensesState()
}

sealed class FriendDetailActionState {
    object Idle : FriendDetailActionState()
    data class Success(val message: String) : FriendDetailActionState()
    data class Error(val message: String) : FriendDetailActionState()
}