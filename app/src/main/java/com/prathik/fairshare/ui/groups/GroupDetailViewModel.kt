package com.prathik.fairshare.ui.groups

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.prathik.fairshare.domain.model.ApiResult
import com.prathik.fairshare.domain.model.Balance
import com.prathik.fairshare.domain.model.Expense
import com.prathik.fairshare.domain.model.Group
import com.prathik.fairshare.domain.model.GroupMember
import com.prathik.fairshare.domain.model.Settlement
import com.prathik.fairshare.domain.repository.GroupRepository
import com.prathik.fairshare.domain.repository.SettlementRepository
import com.prathik.fairshare.domain.usecase.group.GetGroupBalancesUseCase
import com.prathik.fairshare.domain.usecase.expense.GetGroupExpensesUseCase
import com.prathik.fairshare.domain.usecase.group.GetGroupMembersUseCase
import com.prathik.fairshare.domain.usecase.group.GetGroupUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import com.prathik.fairshare.data.sync.PendingOperationRepository
import com.prathik.fairshare.domain.repository.ExpenseRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class GroupDetailViewModel @Inject constructor(
    private val getGroupUseCase: GetGroupUseCase,
    private val getGroupExpensesUseCase: GetGroupExpensesUseCase,
    private val getGroupBalancesUseCase: GetGroupBalancesUseCase,
    private val getGroupMembersUseCase: GetGroupMembersUseCase,
    private val groupRepository: GroupRepository,
    private val settlementRepository: SettlementRepository,
    private val pendingOperationRepository: PendingOperationRepository,
    private val expenseRepository: ExpenseRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    val groupId: String = checkNotNull(savedStateHandle["groupId"])

    private val _groupState = MutableStateFlow<GroupDetailUiState>(GroupDetailUiState.Loading)
    val groupState: StateFlow<GroupDetailUiState> = _groupState.asStateFlow()

    /** Wave 2D-4: IDs of expenses with an active pending operation. */
    val pendingExpenseIds: StateFlow<Set<String>> =
        pendingOperationRepository.observeActivePendingResourceIds()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptySet())

    private val _expensesState = MutableStateFlow<ExpensesUiState>(ExpensesUiState.Loading)
    val expensesState: StateFlow<ExpensesUiState> = _expensesState.asStateFlow()

    private val _settlements = MutableStateFlow<List<Settlement>>(emptyList())
    val settlements: StateFlow<List<Settlement>> = _settlements.asStateFlow()

    private val _balances = MutableStateFlow<List<Balance>>(emptyList())
    val balances: StateFlow<List<Balance>> = _balances.asStateFlow()

    /** True when balances failed to load (network error). Never treat empty as 'all settled'. */
    private val _balancesLoadFailed = MutableStateFlow(false)
    val balancesLoadFailed: StateFlow<Boolean> = _balancesLoadFailed.asStateFlow()

    /** Wave 2D-Final: IDs of expenses with an active DELETE_EXPENSE pending op. */
    val pendingDeleteExpenseIds: StateFlow<Set<String>> =
        pendingOperationRepository.observePendingDeleteResourceIds()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptySet())

    /** Optimistic yourBalance including pending expense changes. Null = use confirmed. */
    private val _optimisticYourBalance = MutableStateFlow<Double?>(null)
    val optimisticYourBalance: StateFlow<Double?> = _optimisticYourBalance.asStateFlow()

    /** Currency of the optimistic balance; null when delta is not applied (unsafe/mixed). */
    private val _optimisticBalanceCurrency = MutableStateFlow<String?>(null)
    val optimisticBalanceCurrency: StateFlow<String?> = _optimisticBalanceCurrency.asStateFlow()

    /** True when displayed balance reflects unsynced pending expense changes. */
    private val _hasPendingBalanceSync = MutableStateFlow(false)
    val hasPendingBalanceSync: StateFlow<Boolean> = _hasPendingBalanceSync.asStateFlow()

    private val _yourBalance = MutableStateFlow(0.0)
    val yourBalance: StateFlow<Double> = _yourBalance.asStateFlow()

    /** All group members — used for "More options" payer/recipient selection. */
    private val _members = MutableStateFlow<List<GroupMember>>(emptyList())
    val members: StateFlow<List<GroupMember>> = _members.asStateFlow()

    // ✅ Fix 1: guard flag so refreshExpenses() skips if loadData() hasn't finished yet.
    // This prevents the RESUMED lifecycle trigger from firing 3 extra requests
    // at the same time as the 5 requests already in-flight from init { loadData() }.
    private var initialLoadDone = false

    init {
        loadData()
        observeOptimisticBalance()
    }

    fun loadData() {
        viewModelScope.launch {
            if (_groupState.value !is GroupDetailUiState.Success)
                _groupState.value = GroupDetailUiState.Loading
            if (_expensesState.value !is ExpensesUiState.Success)
                _expensesState.value = ExpensesUiState.Loading

            val groupDeferred = async { getGroupUseCase(groupId) }
            val expensesDeferred = async { getGroupExpensesUseCase(groupId) }
            val balancesDeferred = async { getGroupBalancesUseCase(groupId) }
            val settlementsDeferred = async { groupRepository.getGroupSettlements(groupId) }
            val membersDeferred = async { getGroupMembersUseCase(groupId) }

            when (val result = groupDeferred.await()) {
                is ApiResult.Success -> _groupState.value = GroupDetailUiState.Success(result.data)
                is ApiResult.NetworkError -> {
                    if (_groupState.value !is GroupDetailUiState.Success)
                        _groupState.value = GroupDetailUiState.Error(result.message, true)
                }

                else -> {
                    if (_groupState.value !is GroupDetailUiState.Success)
                        _groupState.value = GroupDetailUiState.Error("Failed to load group.", false)
                }
            }

            when (val result = expensesDeferred.await()) {
                is ApiResult.Success -> _expensesState.value = ExpensesUiState.Success(result.data)
                is ApiResult.NetworkError -> {
                    if (_expensesState.value !is ExpensesUiState.Success)
                        _expensesState.value = ExpensesUiState.Error(result.message, true)
                }

                else -> {
                    if (_expensesState.value !is ExpensesUiState.Success)
                        _expensesState.value =
                            ExpensesUiState.Error("Failed to load expenses.", false)
                }
            }

            when (val result = settlementsDeferred.await()) {
                is ApiResult.Success -> _settlements.value = result.data
                else -> Unit
            }

            when (val result = balancesDeferred.await()) {
                is ApiResult.Success -> {
                    _balancesLoadFailed.value = false
                    _balances.value = result.data
                    _yourBalance.value = result.data.sumOf { it.amount }
                }

                else -> _balancesLoadFailed.value = true
            }

            when (val result = membersDeferred.await()) {
                is ApiResult.Success -> _members.value = result.data
                else -> Unit
            }

            // ✅ Fix 1: mark initial load complete so RESUMED refreshes are now allowed
            initialLoadDone = true
        }
    }

    fun refreshExpenses() {
        // Only skip if we've never loaded at all (app cold start) — loadData() handles that.
        // Do NOT skip on subsequent resumes — this ensures new expenses appear immediately
        // when returning from AddExpense without waiting for another loadData() cycle.
        if (_groupState.value is GroupDetailUiState.Loading &&
            _expensesState.value is ExpensesUiState.Loading
        ) return

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
                    _balancesLoadFailed.value = false
                    _balances.value = result.data
                    _yourBalance.value = result.data.sumOf { it.amount }
                }

                else -> _balancesLoadFailed.value = true
            }
        }
    }

    private val _settlementActionState =
        MutableStateFlow<SettlementActionState>(SettlementActionState.Idle)
    val settlementActionState: StateFlow<SettlementActionState> =
        _settlementActionState.asStateFlow()

    fun cancelSettlement(settlementId: String) {
        viewModelScope.launch {
            _settlementActionState.value = SettlementActionState.Loading
            when (val result = settlementRepository.cancelSettlement(settlementId)) {
                is ApiResult.Success -> {
                    // Update in-place so the row remains visible with CANCELLED status.
                    _settlements.value = _settlements.value.map { s ->
                        if (s.id == settlementId) result.data else s
                    }
                    refreshExpenses()
                    _settlementActionState.value = SettlementActionState.Cancelled
                }

                is ApiResult.NetworkError -> _settlementActionState.value =
                    SettlementActionState.Error("No internet connection.")

                else -> _settlementActionState.value =
                    SettlementActionState.Error("Failed to cancel settlement.")
            }
        }
    }

    fun restoreSettlement(settlementId: String) {
        viewModelScope.launch {
            _settlementActionState.value = SettlementActionState.Loading
            when (val result = settlementRepository.restoreSettlement(settlementId)) {
                is ApiResult.Success -> {
                    _settlements.value = _settlements.value.map { s ->
                        if (s.id == settlementId) result.data else s
                    }
                    refreshExpenses()
                    _settlementActionState.value = SettlementActionState.Restored
                }

                is ApiResult.NetworkError -> _settlementActionState.value =
                    SettlementActionState.Error("No internet connection.")

                else -> _settlementActionState.value =
                    SettlementActionState.Error("Failed to restore settlement.")
            }
        }
    }

    /** @deprecated Use cancelSettlement() */
    fun deleteSettlement(settlementId: String) = cancelSettlement(settlementId)
    fun resetSettlementActionState() {
        _settlementActionState.value = SettlementActionState.Idle
    }

    // ── Optimistic balance (Wave 2D-Balance Optimism) ─────────────────────────

    private fun observeOptimisticBalance() {
        viewModelScope.launch {
            pendingOperationRepository.observeActivePendingExpenseOps()
                .collect { ops ->
                    val confirmedBalance = _yourBalance.value

                    // Filter to ops whose cached expense belongs to THIS group.
                    val relevantOps = ops.filter { op ->
                        val resourceId =
                            op.localResourceId ?: op.serverResourceId ?: return@filter false
                        val expense =
                            expenseRepository.getCachedExpense(resourceId) ?: return@filter false
                        expense.groupId == groupId
                    }

                    if (relevantOps.isEmpty()) {
                        val wasSync = _hasPendingBalanceSync.value
                        _optimisticYourBalance.value = null
                        _optimisticBalanceCurrency.value = null
                        _hasPendingBalanceSync.value = false
                        // Pending ops cleared → refresh immediately
                        if (wasSync) refreshExpenses()
                        return@collect
                    }
                    _hasPendingBalanceSync.value = true   // scoped: only this group's ops

                    // Currency safety: only apply delta when all pending expenses
                    // are in the same currency as the confirmed balance display.
                    val deltaExpenses = relevantOps.mapNotNull { op ->
                        val resourceId = op.localResourceId ?: op.serverResourceId ?: return@mapNotNull null
                        expenseRepository.getCachedExpense(resourceId)
                    }
                    val pendingCurrencies = deltaExpenses.map { it.currency }.toSet()
                    val displayCurrency = _balances.value.firstOrNull()?.currency
                        ?: pendingCurrencies.singleOrNull()
                        ?: "USD"
                    val currencySafe = pendingCurrencies.size == 1 &&
                            pendingCurrencies.single() == displayCurrency

                    if (!currencySafe) {
                        _optimisticYourBalance.value = null
                        _optimisticBalanceCurrency.value = null
                    } else {
                        var delta = 0.0
                        for (op in relevantOps) {
                            val resourceId = op.localResourceId ?: op.serverResourceId ?: continue
                            val expense = expenseRepository.getCachedExpense(resourceId) ?: continue
                            when (op.operationType) {
                                "CREATE_EXPENSE"  -> delta += expense.yourBalance
                                "DELETE_EXPENSE"  -> delta -= expense.yourBalance
                                "RESTORE_EXPENSE" -> delta += expense.yourBalance
                                // UPDATE: show Pending sync but skip delta (unsafe)
                            }
                        }
                        _optimisticYourBalance.value = confirmedBalance + delta
                        _optimisticBalanceCurrency.value = displayCurrency
                    }
                }
        }
    }
}

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

sealed class TimelineItem(val date: String) {
    data class ExpenseItem(val expense: Expense) : TimelineItem(expense.expenseDate)
    data class SettlementItem(val settlement: Settlement) : TimelineItem(settlement.settlementDate)
}

sealed class SettlementActionState {
    object Idle : SettlementActionState()
    object Loading : SettlementActionState()
    object Cancelled : SettlementActionState()
    object Restored : SettlementActionState()

    /** @deprecated kept for backward compat */
    object Deleted : SettlementActionState()
    data class Error(val message: String) : SettlementActionState()

}