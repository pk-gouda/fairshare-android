package com.prathik.fairshare.ui.groups

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.prathik.fairshare.domain.model.ApiResult
import com.prathik.fairshare.domain.model.Balance
import com.prathik.fairshare.domain.model.Expense
import com.prathik.fairshare.domain.model.Group
import com.prathik.fairshare.domain.model.Settlement
import com.prathik.fairshare.domain.repository.GroupRepository
import com.prathik.fairshare.domain.repository.SettlementRepository
import com.prathik.fairshare.domain.usecase.group.GetGroupBalancesUseCase
import com.prathik.fairshare.domain.usecase.expense.GetGroupExpensesUseCase
import com.prathik.fairshare.domain.usecase.group.GetGroupUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for GroupDetailScreen.
 *
 * Loads group info, expenses, settlements, and balances in parallel.
 * Settlements are merged into the timeline alongside expenses.
 */
@HiltViewModel
class GroupDetailViewModel @Inject constructor(
    private val getGroupUseCase: GetGroupUseCase,
    private val getGroupExpensesUseCase: GetGroupExpensesUseCase,
    private val getGroupBalancesUseCase: GetGroupBalancesUseCase,
    private val groupRepository: GroupRepository,
    private val settlementRepository: SettlementRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    val groupId: String = checkNotNull(savedStateHandle["groupId"])

    // ── Group state ───────────────────────────────────────────────────────────
    private val _groupState = MutableStateFlow<GroupDetailUiState>(GroupDetailUiState.Loading)
    val groupState: StateFlow<GroupDetailUiState> = _groupState.asStateFlow()

    // ── Expenses state ────────────────────────────────────────────────────────
    private val _expensesState = MutableStateFlow<ExpensesUiState>(ExpensesUiState.Loading)
    val expensesState: StateFlow<ExpensesUiState> = _expensesState.asStateFlow()

    // ── Settlements ───────────────────────────────────────────────────────────
    private val _settlements = MutableStateFlow<List<Settlement>>(emptyList())
    val settlements: StateFlow<List<Settlement>> = _settlements.asStateFlow()

    // ── Group balances state ──────────────────────────────────────────────────
    private val _balances = MutableStateFlow<List<Balance>>(emptyList())
    val balances: StateFlow<List<Balance>> = _balances.asStateFlow()

    // ── Your balance in this group ────────────────────────────────────────────
    private val _yourBalance = MutableStateFlow(0.0)
    val yourBalance: StateFlow<Double> = _yourBalance.asStateFlow()

    init {
        loadData()
    }

    /**
     * Loads group, expenses, settlements, and balances in parallel.
     */
    fun loadData() {
        viewModelScope.launch {
            if (_groupState.value !is GroupDetailUiState.Success) {
                _groupState.value = GroupDetailUiState.Loading
            }
            if (_expensesState.value !is ExpensesUiState.Success) {
                _expensesState.value = ExpensesUiState.Loading
            }

            val groupDeferred = async { getGroupUseCase(groupId) }
            val expensesDeferred = async { getGroupExpensesUseCase(groupId) }
            val balancesDeferred = async { getGroupBalancesUseCase(groupId) }
            val settlementsDeferred = async { groupRepository.getGroupSettlements(groupId) }

            // Group
            when (val result = groupDeferred.await()) {
                is ApiResult.Success -> _groupState.value = GroupDetailUiState.Success(result.data)
                is ApiResult.NetworkError -> {
                    if (_groupState.value !is GroupDetailUiState.Success)
                        _groupState.value =
                            GroupDetailUiState.Error(message = result.message, isNetwork = true)
                }
                else -> {
                    if (_groupState.value !is GroupDetailUiState.Success)
                        _groupState.value = GroupDetailUiState.Error(
                            message = "Failed to load group.",
                            isNetwork = false
                        )
                }
            }

            // Expenses
            when (val result = expensesDeferred.await()) {
                is ApiResult.Success -> _expensesState.value = ExpensesUiState.Success(result.data)
                is ApiResult.NetworkError -> {
                    if (_expensesState.value !is ExpensesUiState.Success)
                        _expensesState.value =
                            ExpensesUiState.Error(message = result.message, isNetwork = true)
                }
                else -> {
                    if (_expensesState.value !is ExpensesUiState.Success)
                        _expensesState.value = ExpensesUiState.Error(
                            message = "Failed to load expenses.",
                            isNetwork = false
                        )
                }
            }

            // Settlements
            when (val result = settlementsDeferred.await()) {
                is ApiResult.Success -> _settlements.value = result.data
                else -> Unit
            }

            // Balances
            when (val result = balancesDeferred.await()) {
                is ApiResult.Success -> {
                    _balances.value = result.data
                    _yourBalance.value = result.data.sumOf { it.amount }
                }
                else -> Unit
            }
        }
    }

    /**
     * Refreshes expenses + settlements + balances on screen resume.
     */
    fun refreshExpenses() {
        viewModelScope.launch {
            val expensesDeferred = async { getGroupExpensesUseCase(groupId) }
            val settlementsDeferred = async { groupRepository.getGroupSettlements(groupId) }
            val balancesDeferred = async { getGroupBalancesUseCase(groupId) }

            when (val result = expensesDeferred.await()) {
                is ApiResult.Success -> _expensesState.value = ExpensesUiState.Success(result.data)
                else -> Unit
            }

            when (val result = settlementsDeferred.await()) {
                is ApiResult.Success -> _settlements.value = result.data
                else -> Unit
            }

            when (val result = balancesDeferred.await()) {
                is ApiResult.Success -> {
                    _balances.value = result.data
                    _yourBalance.value = result.data.sumOf { it.amount }
                }
                else -> Unit
            }
        }
    }
    // ── Settlement delete ─────────────────────────────────────────────────────

    private val _settlementActionState = MutableStateFlow<SettlementActionState>(SettlementActionState.Idle)
    val settlementActionState: StateFlow<SettlementActionState> = _settlementActionState.asStateFlow()

    fun deleteSettlement(settlementId: String) {
        viewModelScope.launch {
            _settlementActionState.value = SettlementActionState.Loading
            when (val result = settlementRepository.deleteSettlement(settlementId)) {
                is ApiResult.Success -> {
                    // Remove from local list immediately — don't wait for refresh
                    _settlements.value = _settlements.value.filter { it.id != settlementId }
                    // Also refresh balances since delete reverses them
                    loadData()
                    _settlementActionState.value = SettlementActionState.Deleted
                }
                is ApiResult.NetworkError -> _settlementActionState.value =
                    SettlementActionState.Error("No internet connection.")
                else -> _settlementActionState.value =
                    SettlementActionState.Error("Failed to delete settlement.")
            }
        }
    }

    fun resetSettlementActionState() {
        _settlementActionState.value = SettlementActionState.Idle
    }
}

// ── UI States ─────────────────────────────────────────────────────────────────

sealed class GroupDetailUiState {
    object Loading : GroupDetailUiState()
    data class Success(val group: Group) : GroupDetailUiState()
    data class Error(val message: String, val isNetwork: Boolean) : GroupDetailUiState()
}

sealed class ExpensesUiState {
    object Loading : ExpensesUiState()
    data class Success(val expenses: List<Expense>) : ExpensesUiState()
    data class Error(val message: String, val isNetwork: Boolean) : ExpensesUiState()
}

/**
 * Unified timeline item — either an expense or a settlement.
 * Used to interleave both in chronological order.
 */
sealed class TimelineItem(val date: String) {
    data class ExpenseItem(val expense: Expense) : TimelineItem(expense.expenseDate)
    data class SettlementItem(val settlement: Settlement) : TimelineItem(settlement.settlementDate)
}

sealed class SettlementActionState {
    object Idle    : SettlementActionState()
    object Loading : SettlementActionState()
    object Deleted : SettlementActionState()
    data class Error(val message: String) : SettlementActionState()
}