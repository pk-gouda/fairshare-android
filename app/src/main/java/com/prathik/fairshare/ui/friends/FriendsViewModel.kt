package com.prathik.fairshare.ui.friends

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.prathik.fairshare.domain.model.AccountStatus
import com.prathik.fairshare.domain.model.ApiResult
import com.prathik.fairshare.domain.model.Friend
import com.prathik.fairshare.domain.usecase.balance.GetAllBalancesUseCase
import com.prathik.fairshare.domain.usecase.friend.GetFriendsUseCase
import com.prathik.fairshare.data.local.PendingBalanceImpactEntity
import com.prathik.fairshare.data.local.PendingOperationEntity
import com.prathik.fairshare.data.sync.PendingOperationRepository
import com.prathik.fairshare.domain.model.Expense
import com.prathik.fairshare.data.sync.FairShareSyncManager
import com.prathik.fairshare.data.sync.SyncReason
import com.prathik.fairshare.domain.repository.ExpenseRepository
import com.prathik.fairshare.domain.usecase.balance.EffectiveBalanceCalculator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import com.prathik.fairshare.domain.model.BalanceCurrencyEntry

@HiltViewModel
class FriendsViewModel @Inject constructor(
    private val getFriendsUseCase         : GetFriendsUseCase,
    private val getAllBalancesUseCase      : GetAllBalancesUseCase,
    private val friendEventBus            : FriendEventBus,
    private val pendingOperationRepository: PendingOperationRepository,
    private val expenseRepository         : ExpenseRepository,
    private val effectiveCalculator       : EffectiveBalanceCalculator,
    private val syncManager               : FairShareSyncManager,
) : ViewModel() {

    // ── Latest state for reactive recompute ───────────────────────────────
    private var latestOps: List<PendingOperationEntity> = emptyList()
    private var latestExpenseCache: Map<String, Expense> = emptyMap()
    private var latestImpacts: Map<String, PendingBalanceImpactEntity> = emptyMap()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    // All friends — active, invited, placeholder — all from backend
    private val _friends = MutableStateFlow<List<Friend>>(emptyList())
    val friends: StateFlow<List<Friend>> = _friends.asStateFlow()

    // Map<friendId, List<Pair<amount, currency>>> — one entry per currency
    private val _balanceMap = MutableStateFlow<Map<String, List<Pair<Double, String>>>>(emptyMap())
    val balanceMap: StateFlow<Map<String, List<Pair<Double, String>>>> = _balanceMap.asStateFlow()

    // Multi-currency entries for the net balance bar
    private val _balanceEntries = MutableStateFlow<List<BalanceCurrencyEntry>>(emptyList())
    val balanceEntries: StateFlow<List<BalanceCurrencyEntry>> = _balanceEntries.asStateFlow()

    private val _owedToYou = MutableStateFlow(0.0)
    val owedToYou: StateFlow<Double> = _owedToYou.asStateFlow()

    private val _youOwe = MutableStateFlow(0.0)
    val youOwe: StateFlow<Double> = _youOwe.asStateFlow()

    /** Per-friend optimistic balances — friendId → effective entries including pending delta. */
    private val _optimisticFriendBalanceMap =
        MutableStateFlow<Map<String, List<Pair<Double, String>>>>(emptyMap())
    val optimisticFriendBalanceMap: StateFlow<Map<String, List<Pair<Double, String>>>> =
        _optimisticFriendBalanceMap.asStateFlow()

    /** Friends that have at least one active pending expense op. */
    private val _friendsWithPendingSync = MutableStateFlow<Set<String>>(emptySet())
    val friendsWithPendingSync: StateFlow<Set<String>> = _friendsWithPendingSync.asStateFlow()

    /** Global effective summary — null when no pending ops (fall back to confirmed). */
    private val _effectiveSummary = MutableStateFlow<com.prathik.fairshare.ui.groups.BalanceSummary?>(null)
    val effectiveSummary: StateFlow<com.prathik.fairshare.ui.groups.BalanceSummary?> =
        _effectiveSummary.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _actionState = MutableStateFlow<FriendsActionState>(FriendsActionState.Idle)
    val actionState: StateFlow<FriendsActionState> = _actionState.asStateFlow()

    init {
        loadData()
        observeOptimisticFriendBalances()
        viewModelScope.launch {
            friendEventBus.friendAdded.collect { event ->
                _friends.value = event.updatedList
                val msg = if (event.addedCount == 1) "1 friend added" else "${event.addedCount} friends added"
                _actionState.value = FriendsActionState.Success(msg)
            }
        }
    }

    /** Pull-to-refresh: sync all friend scopes then reload ViewModel state. */
    fun refresh() {
        viewModelScope.launch {
            syncManager.syncFriendsHome(SyncReason.MANUAL_REFRESH)
            loadData()
        }
    }

    fun loadData() {
        viewModelScope.launch {
            if (_friends.value.isEmpty()) _isLoading.value = true

            // Friends first — instant from Room cache after first launch
            when (val result = getFriendsUseCase()) {
                is ApiResult.Success -> _friends.value = result.data
                else -> Unit
            }
            // Stop showing loader as soon as friends are ready
            _isLoading.value = false

            // Balances load independently — updates amounts when ready without blocking display
            launch {
                when (val result = getAllBalancesUseCase()) {
                    is ApiResult.Success -> {
                        val balances = result.data
                        // Per-friend: take the dominant currency (largest abs amount)
                        _balanceMap.value = balances
                            .groupBy { it.otherUserId }
                            .mapValues { (_, list) ->
                                // One Pair per currency — never sum across currencies
                                list.groupBy { it.currency }
                                    .map { (cur, entries) -> Pair(entries.sumOf { it.amount }, cur) }
                                    .filter { it.first != 0.0 }
                            }
                        // Per-currency entries for multi-currency net bar
                        val byCurrency = balances.groupBy { it.currency }
                        _balanceEntries.value = byCurrency.map { (currency, list) ->
                            val owedToMe = list.filter { it.amount > 0 }.sumOf { it.amount }
                            val youOwe   = list.filter { it.amount < 0 }.sumOf { -it.amount }
                            BalanceCurrencyEntry(currency, owedToMe, youOwe, owedToMe - youOwe)
                        }.filter { it.owedToMe > 0.0 || it.youOwe > 0.0 }
                        _owedToYou.value = balances.filter { it.amount > 0 }.sumOf { it.amount }
                        _youOwe.value    = balances.filter { it.amount < 0 }.sumOf { -it.amount }
                        // Recompute effective overlays now that confirmed data is ready.
                        recomputeEffectiveBalances()
                    }
                    else -> Unit
                }
            }
        }
    }

    fun onSearchChanged(query: String) { _searchQuery.value = query }

    // Active friends only — show in main list with balances
    fun filteredActiveFriends(): List<Friend> {
        val q = _searchQuery.value.trim().lowercase()
        return _friends.value
            .filter { it.isActive }
            .let { list ->
                if (q.isBlank()) list
                else list.filter {
                    it.fullName.lowercase().contains(q) ||
                            it.email.lowercase().contains(q)
                }
            }
    }

    // Invited + placeholder — shown separately with status badge
    fun filteredNonActiveFriends(): List<Friend> {
        val q = _searchQuery.value.trim().lowercase()
        return _friends.value
            .filter { it.isPlaceholder || it.isInvited }
            .let { list ->
                if (q.isBlank()) list
                else list.filter { it.fullName.lowercase().contains(q) }
            }
    }

    fun resetActionState() { _actionState.value = FriendsActionState.Idle }


    // ── Optimistic friend balance overlay ─────────────────────────────────────

    private fun observeOptimisticFriendBalances() {
        viewModelScope.launch {
            pendingOperationRepository.observeActivePendingExpenseOps().collect { ops ->
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
     * Single recompute entry called by loadData() (confirmed data) AND ops collector.
     * Guarantees FriendsHome top total always matches GroupsHome top total.
     */
    private suspend fun recomputeEffectiveBalances() {
        val ops     = latestOps
        val cache   = latestExpenseCache
        val impacts = latestImpacts

        if (ops.isEmpty()) {
            _optimisticFriendBalanceMap.value = emptyMap()
            _friendsWithPendingSync.value = emptySet()
            _effectiveSummary.value = null
            return
        }

        // ── Per-friend row effective balances ─────────────────────────────────
        // Pre-build otherUserId map via suspend call — can't call suspend inside the lambda.
        val otherUserIdMap: Map<String, String?> = ops.mapNotNull { op ->
            val r = op.localResourceId ?: op.serverResourceId ?: return@mapNotNull null
            r to expenseRepository.getCachedDirectOtherUserId(r)
        }.toMap()
        val effectiveFriends = effectiveCalculator.effectiveFriendBalances(
            confirmedFriendBase      = _balanceMap.value,
            ops                      = ops,
            expenseCache             = cache,
            impacts                  = impacts,
            otherUserIdForExpense    = { resourceId -> otherUserIdMap[resourceId] },
            // Known limitation (Wave 2F): group expense pending impacts are NOT
            // projected into friend rows here because that requires per-expense
            // split/payer data to compute exact friend-level contributions.
            // Group expense ops DO update the global top-bar effectiveSummary
            // via FairShareSyncManager.mutationRefresh → getAllBalances.
        )
        _friendsWithPendingSync.value = effectiveFriends.keys
        _optimisticFriendBalanceMap.value = effectiveFriends

        // ── Global top-bar effective summary (same formula as GroupsViewModel) ─
        // Always call even when confirmedEntries is empty — calculator handles
        // pending-only currencies. Return early would leave stale effectiveSummary.
        val confirmedEntries = _balanceEntries.value
        val globalResult = effectiveCalculator.globalEffectiveSummary(
            confirmedEntries = confirmedEntries,
            ops              = ops,
            expenseCache     = cache,
            impacts          = impacts,
        )
        _effectiveSummary.value = globalResult?.let {
            com.prathik.fairshare.ui.groups.BalanceSummary(
                owedToMe = it.owedToMe,
                youOwe   = it.youOwe,
                entries  = it.entries,
            )
        }
    }
}
sealed class FriendsActionState {
    object Idle : FriendsActionState()
    data class Success(val message: String) : FriendsActionState()
    data class Error(val message: String)   : FriendsActionState()
}