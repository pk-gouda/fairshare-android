package com.prathik.fairshare.ui.groups

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.prathik.fairshare.domain.model.ApiResult
import com.prathik.fairshare.domain.model.Balance
import com.prathik.fairshare.domain.model.Group
import com.prathik.fairshare.domain.usecase.balance.GetAllBalancesUseCase
import com.prathik.fairshare.domain.usecase.group.GetGroupBalancesUseCase
import com.prathik.fairshare.data.sync.PendingOperationRepository
import com.prathik.fairshare.data.local.PendingBalanceImpactEntity
import com.prathik.fairshare.domain.repository.BalanceRepository
import com.prathik.fairshare.domain.repository.ExpenseRepository
import com.prathik.fairshare.data.local.PendingOperationEntity
import com.prathik.fairshare.domain.model.Expense
import com.prathik.fairshare.domain.usecase.balance.EffectiveBalanceCalculator
import com.prathik.fairshare.domain.usecase.group.GetGroupsUseCase
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.awaitAll
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import com.prathik.fairshare.domain.model.BalanceCurrencyEntry

/**
 * ViewModel for GroupsHomeScreen.
 *
 * Loads groups and balances in parallel using async/await.
 * Exposes separate state flows so the UI can show partial data
 * while the rest is loading.
 */
@HiltViewModel
class GroupsViewModel @Inject constructor(
    private val getGroupsUseCase            : GetGroupsUseCase,
    private val getAllBalancesUseCase        : GetAllBalancesUseCase,
    private val getGroupBalancesUseCase     : GetGroupBalancesUseCase,
    private val pendingOperationRepository  : PendingOperationRepository,
    private val expenseRepository           : ExpenseRepository,
    private val balanceRepository           : BalanceRepository,
    private val effectiveCalculator         : EffectiveBalanceCalculator,
) : ViewModel() {

    // ── Latest state for reactive recompute ───────────────────────────────
    // Both loadData() and the ops collector update these, then call recomputeEffectiveBalances().
    // This ensures effective summary is always current regardless of which updates first.
    private var latestOps: List<PendingOperationEntity> = emptyList()
    private var latestExpenseCache: Map<String, Expense> = emptyMap()
    private var latestImpacts: Map<String, com.prathik.fairshare.data.local.PendingBalanceImpactEntity> = emptyMap()

    // ── Groups state ──────────────────────────────────────────────────────────
    private val _groupsState = MutableStateFlow<GroupsUiState>(GroupsUiState.Loading)
    val groupsState: StateFlow<GroupsUiState> = _groupsState.asStateFlow()

    // ── Search query ──────────────────────────────────────────────────────────
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    fun onSearchChanged(query: String) {
        _searchQuery.value = query
    }

    // ── Balance summary state ─────────────────────────────────────────────────
    private val _balanceSummary = MutableStateFlow<BalanceSummary?>(null)
    val balanceSummary: StateFlow<BalanceSummary?> = _balanceSummary.asStateFlow()

    // ── Per-group balance: groupId → net amount for current user ──────────────
    // Positive = others owe you in this group, Negative = you owe in this group
    // null key not present = no expenses yet
    // Map<groupId, List<Pair<amount, currency>>> — one entry per currency
    private val _groupBalanceMap = MutableStateFlow<Map<String, List<Pair<Double, String>>>>(emptyMap())
    val groupBalanceMap: StateFlow<Map<String, List<Pair<Double, String>>>> = _groupBalanceMap.asStateFlow()

    /**
     * Per-group optimistic balance — groupId → effective balance including pending delta.
     * Null value for a group means no pending ops affect it (use groupBalanceMap instead).
     */
    private val _optimisticGroupBalanceMap =
        MutableStateFlow<Map<String, List<Pair<Double, String>>>>(emptyMap())
    val optimisticGroupBalanceMap: StateFlow<Map<String, List<Pair<Double, String>>>> =
        _optimisticGroupBalanceMap.asStateFlow()

    /** Set of groupIds that have active pending expense ops. */
    private val _groupsWithPendingSync = MutableStateFlow<Set<String>>(emptySet())
    val groupsWithPendingSync: StateFlow<Set<String>> = _groupsWithPendingSync.asStateFlow()

    /**
     * Effective top-bar summary including pending deltas.
     * Replaces confirmed balanceSummary on GroupsHome while ops are active.
     */
    private val _effectiveSummary = MutableStateFlow<BalanceSummary?>(null)
    val effectiveSummary: StateFlow<BalanceSummary?> = _effectiveSummary.asStateFlow()

    init {
        loadData()
        observeOptimisticGroupBalances()
    }

    /**
     * Loads groups and balances in parallel.
     * Both calls start simultaneously — UI gets data as soon as each completes.
     */
    fun loadData() {
        viewModelScope.launch {
            // Only show loading spinner if we have no data yet
            if (_groupsState.value !is GroupsUiState.Success) {
                _groupsState.value = GroupsUiState.Loading
            }

            // Load groups — instant from Room cache after first launch
            when (val result = getGroupsUseCase()) {
                is ApiResult.Success -> {
                    _groupsState.value = GroupsUiState.Success(result.data)

                    // Load per-group balances and overall summary in background
                    // Groups show immediately — balance cards fill in when ready
                    launch {
                        val groupBalanceResults = result.data.map { group ->
                            async {
                                group.id to when (val r = getGroupBalancesUseCase(group.id)) {
                                    is ApiResult.Success -> {
                                        // One entry per currency — never sum across currencies
                                        r.data.groupBy { it.currency }
                                            .map { (cur, list) -> Pair(list.sumOf { it.amount }, cur) }
                                            .filter { it.first != 0.0 }
                                    }
                                    else -> null
                                }
                            }
                        }.awaitAll()
                        _groupBalanceMap.value = groupBalanceResults
                            .mapNotNull { (id, bal) -> bal?.let { id to it } }
                            .toMap()
                        // Recompute effective group tile balances now that confirmed
                        // group data is ready (may have arrived after pending ops).
                        recomputeEffectiveBalances()
                    }

                    launch {
                        when (val result = getAllBalancesUseCase()) {
                            is ApiResult.Success -> {
                                _balanceSummary.value = calculateSummary(result.data)
                                recomputeEffectiveBalances()
                            }
                            else -> Unit
                        }
                    }
                }
                is ApiResult.NetworkError -> {
                    if (_groupsState.value !is GroupsUiState.Success) {
                        _groupsState.value = GroupsUiState.Error(
                            message = result.message,
                            isNetwork = true,
                        )
                    }
                }
                else -> {
                    if (_groupsState.value !is GroupsUiState.Success) {
                        _groupsState.value = GroupsUiState.Error(
                            message = "Failed to load groups. Please try again.",
                            isNetwork = false,
                        )
                    }
                }
            }
        }
    }

    /**
     * Calculates net balance summary from the list of all balances.
     *
     * Groups balances by currency and computes per-currency net amounts.
     * Matches Splitwise behavior: "₹2,455 + $352" instead of mixing currencies.
     */
    private fun calculateSummary(balances: List<Balance>): BalanceSummary {
        // Group by currency, compute owedToMe and youOwe per currency
        val byCurrency = balances.groupBy { it.currency }
        val entries = byCurrency.map { (currency, list) ->
            val owedToMe = list.filter { it.amount > 0 }.sumOf { it.amount }
            val youOwe   = list.filter { it.amount < 0 }.sumOf { -it.amount }
            BalanceCurrencyEntry(currency, owedToMe, youOwe, owedToMe - youOwe)
        }.filter { it.owedToMe > 0.0 || it.youOwe > 0.0 }

        val totalOwedToMe = entries.sumOf { it.owedToMe }
        val totalYouOwe   = entries.sumOf { it.youOwe }
        return BalanceSummary(
            owedToMe = totalOwedToMe,
            youOwe   = totalYouOwe,
            entries  = entries,
        )
    }

    // ── Optimistic group balance overlay ──────────────────────────────────────

    /**
     * Reacts to active pending expense operations and applies the delta to the
     * confirmed cached group balance for each affected group. This mirrors
     * GroupDetailViewModel but works across ALL groups for GroupsHome tiles.
     */

    private fun observeOptimisticGroupBalances() {
        viewModelScope.launch {
            pendingOperationRepository.observeActivePendingExpenseOps()
                .collect { ops ->
                    // Prefetch all expense data for this op batch.
                    latestExpenseCache = ops.mapNotNull { op ->
                        val r = op.localResourceId ?: op.serverResourceId ?: return@mapNotNull null
                        expenseRepository.getCachedExpense(r)?.let { r to it }
                    }.toMap()
                    latestImpacts = pendingOperationRepository.getAllImpacts()
                        .associateBy { it.operationId }
                    latestOps = ops
                    recomputeEffectiveBalances()
                }
        }
    }

    /**
     * Single recompute entry point called by BOTH loadData() (when confirmed data refreshes)
     * AND the ops collector (when pending ops change). Guarantees the effective overlay is
     * always consistent regardless of which data arrives first.
     */
    private suspend fun recomputeEffectiveBalances() {
        val ops     = latestOps
        val cache   = latestExpenseCache
        val impacts = latestImpacts

        if (ops.isEmpty()) {
            _optimisticGroupBalanceMap.value = emptyMap()
            _groupsWithPendingSync.value = emptySet()
            _effectiveSummary.value = null
            return
        }

        // ── Per-group tile effective balances ─────────────────────────────────
        val confirmedGroupBase: Map<String, Pair<Double, String>> = _groupBalanceMap.value
            .mapNotNull { (gId, entries) ->
                val pair = entries.maxByOrNull { kotlin.math.abs(it.first) } ?: return@mapNotNull null
                gId to Pair(pair.first, pair.second)
            }.toMap()

        val effectiveGroups = effectiveCalculator.effectiveGroupBalances(
            confirmedGroupBase = confirmedGroupBase,
            ops                = ops,
            expenseCache       = cache,
            impacts            = impacts,
        )
        _groupsWithPendingSync.value = effectiveGroups.keys
        _optimisticGroupBalanceMap.value = effectiveGroups
            .mapValues { (_, pair) -> listOf(pair) }

        // ── Global top-bar effective summary ──────────────────────────────────
        val confirmedEntries = _balanceSummary.value?.entries ?: return
        val globalResult = effectiveCalculator.globalEffectiveSummary(
            confirmedEntries = confirmedEntries,
            ops              = ops,
            expenseCache     = cache,
            impacts          = impacts,
        )
        _effectiveSummary.value = globalResult?.let {
            BalanceSummary(owedToMe = it.owedToMe, youOwe = it.youOwe, entries = it.entries)
        }
    }
}

// ── UI State ──────────────────────────────────────────────────────────────────

sealed class GroupsUiState {
    object Loading : GroupsUiState()
    data class Success(val groups: List<Group>) : GroupsUiState()
    data class Error(val message: String, val isNetwork: Boolean) : GroupsUiState()
}

data class BalanceSummary(
    val owedToMe: Double,
    val youOwe: Double,
    val entries: List<BalanceCurrencyEntry> = emptyList(),
) {
    /** True if there is any non-zero balance. */
    val hasBalance: Boolean get() = owedToMe > 0.0 || youOwe > 0.0
    /** Net across all currencies — only valid for single-currency display. */
    val netBalance: Double get() = owedToMe - youOwe
}