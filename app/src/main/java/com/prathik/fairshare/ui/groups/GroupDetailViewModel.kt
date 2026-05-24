package com.prathik.fairshare.ui.groups

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.prathik.fairshare.domain.model.ApiResult
import com.prathik.fairshare.domain.model.errorMessage
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
import com.prathik.fairshare.data.local.PendingBalanceImpactEntity
import com.prathik.fairshare.domain.repository.BalanceRepository
import com.prathik.fairshare.data.sync.FairShareSyncManager
import com.prathik.fairshare.data.sync.SyncReason
import com.prathik.fairshare.domain.repository.ExpenseRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class GroupDetailViewModel @Inject constructor(
    private val getGroupUseCase: GetGroupUseCase,
    private val getGroupExpensesUseCase: GetGroupExpensesUseCase,
    private val getGroupBalancesUseCase: GetGroupBalancesUseCase,
    private val getGroupMembersUseCase: GetGroupMembersUseCase,
    private val groupRepository: GroupRepository,
    private val settlementRepository: SettlementRepository,
    private val pendingOperationRepository : PendingOperationRepository,
    private val expenseRepository           : ExpenseRepository,
    private val balanceRepository           : BalanceRepository,
    private val syncManager                 : FairShareSyncManager,
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
        if (_groupState.value is GroupDetailUiState.Loading &&
            _expensesState.value is ExpensesUiState.Loading
        ) return

        viewModelScope.launch {
            // Step 1 — Room-only read: show cached data immediately with no network wait.
            val cachedExpenses = expenseRepository.getCachedGroupExpenses(groupId)
            if (cachedExpenses.isNotEmpty() || _expensesState.value is ExpensesUiState.Success) {
                _expensesState.value = ExpensesUiState.Success(
                    cachedExpenses.sortedByDescending { it.expenseDate }
                )
            }

            // Step 2 — Background network sync: update Room, then re-read.
            launch {
                syncManager.syncGroupDetail(groupId, SyncReason.MANUAL_REFRESH)
                val settlementsDeferred  = async { groupRepository.getGroupSettlements(groupId) }
                val balancesDeferred     = async { getGroupBalancesUseCase(groupId) }

                val refreshedExpenses = expenseRepository.getCachedGroupExpenses(groupId)
                if (refreshedExpenses.isNotEmpty() || _expensesState.value is ExpensesUiState.Success) {
                    _expensesState.value = ExpensesUiState.Success(
                        refreshedExpenses.sortedByDescending { it.expenseDate }
                    )
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
    }

    private val _settlementActionState =
        MutableStateFlow<SettlementActionState>(SettlementActionState.Idle)
    val settlementActionState: StateFlow<SettlementActionState> =
        _settlementActionState.asStateFlow()

    // Per-settlement idempotency keys — same map pattern as FriendDetailViewModel.
    private val cancelIdempotencyKeys  = mutableMapOf<String, String>()
    private val restoreIdempotencyKeys = mutableMapOf<String, String>()

    fun cancelSettlement(settlementId: String) {
        viewModelScope.launch {
            _settlementActionState.value = SettlementActionState.Loading
            val key = cancelIdempotencyKeys.getOrPut(settlementId) { UUID.randomUUID().toString() }
            when (val result = settlementRepository.cancelSettlement(settlementId, key)) {
                is ApiResult.Success -> {
                    cancelIdempotencyKeys.remove(settlementId)
                    // Update in-place so the row remains visible with CANCELLED status.
                    _settlements.value = _settlements.value.map { s ->
                        if (s.id == settlementId) result.data else s
                    }
                    refreshExpenses()
                    _settlementActionState.value = SettlementActionState.Cancelled
                }
                is ApiResult.NetworkError -> {
                    // Retain key for retry
                    _settlementActionState.value = SettlementActionState.Error("No internet connection.")
                }
                else -> {
                    cancelIdempotencyKeys.remove(settlementId)
                    _settlementActionState.value = SettlementActionState.Error(result.errorMessage() ?: "Failed to cancel settlement.")
                }
            }
        }
    }

    fun restoreSettlement(settlementId: String) {
        viewModelScope.launch {
            _settlementActionState.value = SettlementActionState.Loading
            val key = restoreIdempotencyKeys.getOrPut(settlementId) { UUID.randomUUID().toString() }
            when (val result = settlementRepository.restoreSettlement(settlementId, key)) {
                is ApiResult.Success -> {
                    restoreIdempotencyKeys.remove(settlementId)
                    _settlements.value = _settlements.value.map { s ->
                        if (s.id == settlementId) result.data else s
                    }
                    refreshExpenses()
                    _settlementActionState.value = SettlementActionState.Restored
                }
                is ApiResult.NetworkError -> {
                    // Retain key for retry
                    _settlementActionState.value = SettlementActionState.Error("No internet connection.")
                }
                else -> {
                    restoreIdempotencyKeys.remove(settlementId)
                    _settlementActionState.value = SettlementActionState.Error(result.errorMessage() ?: "Failed to restore settlement.")
                }
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
                    // Use cached Room balance as base — never _yourBalance.value which starts
                    // at 0.0 and only loads after a network call. This ensures offline reopen
                    // shows the correct base (e.g. $88.08) not 0 when computing the delta.
                    val confirmedBalance = balanceRepository.getCachedGroupBalance(groupId)
                        ?: _yourBalance.value   // fall back to in-memory if Room also empty

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
                        // Load UPDATE impacts from Option A table for accurate delta.
                        val updateImpacts: Map<String, PendingBalanceImpactEntity> =
                            pendingOperationRepository.getImpactsForGroup(groupId)
                                .associateBy { it.operationId }

                        var delta = 0.0
                        for (op in relevantOps) {
                            val resourceId = op.localResourceId ?: op.serverResourceId ?: continue
                            val expense = expenseRepository.getCachedExpense(resourceId) ?: continue
                            when (op.operationType) {
                                "CREATE_EXPENSE"  -> delta += expense.yourBalance
                                "DELETE_EXPENSE"  -> delta -= expense.yourBalance
                                "RESTORE_EXPENSE" -> delta += expense.yourBalance
                                "UPDATE_EXPENSE"  -> {
                                    // Use persisted impact row — survives navigation + restart.
                                    val impact = updateImpacts[op.operationId]
                                    if (impact != null) delta += impact.delta
                                }
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