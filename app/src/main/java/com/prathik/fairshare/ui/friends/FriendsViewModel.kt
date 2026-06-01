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
import com.prathik.fairshare.data.local.EncryptedTokenStore
import com.prathik.fairshare.domain.usecase.balance.EffectiveBalanceCalculator
import com.prathik.fairshare.domain.usecase.balance.PendingFriendImpactCalculator
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
    private val friendRepository          : com.prathik.fairshare.domain.repository.FriendRepository,
    private val balanceRepository         : com.prathik.fairshare.domain.repository.BalanceRepository,
    private val effectiveCalculator       : EffectiveBalanceCalculator,
    private val friendImpactCalculator    : PendingFriendImpactCalculator,
    private val tokenStore                : EncryptedTokenStore,
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

    private val _manualRefreshing = MutableStateFlow(false)
    val manualRefreshing: StateFlow<Boolean> = _manualRefreshing.asStateFlow()

    /** True once friends have been loaded from cache or network. */
    private val _friendsLoaded = MutableStateFlow(false)
    val friendsLoaded: StateFlow<Boolean> = _friendsLoaded.asStateFlow()

    /** True when no cached friends exist and network load failed. */
    private val _friendsLoadFailed = MutableStateFlow(false)
    val friendsLoadFailed: StateFlow<Boolean> = _friendsLoadFailed.asStateFlow()

    private var initialLoadDone = false

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
                _friends.value = event.updatedList.stableSortedForHome()
                val msg = if (event.addedCount == 1) "1 friend added" else "${event.addedCount} friends added"
                _actionState.value = FriendsActionState.Success(msg)
            }
        }
    }

    /** Helper: apply cached balance rows to all balance state fields. */
    private fun applyBalances(balances: List<com.prathik.fairshare.domain.model.Balance>) {
        _balanceMap.value = balances
            .groupBy { it.otherUserId }
            .mapValues { (_, list) ->
                list.groupBy { it.currency }
                    .map { (cur, entries) -> Pair(entries.sumOf { it.amount }, cur) }
                    .filter { it.first != 0.0 }
            }
        val byCurrency = balances.groupBy { it.currency }
        _balanceEntries.value = byCurrency.map { (currency, list) ->
            val owedToMe = list.filter { it.amount > 0 }.sumOf { it.amount }
            val youOwe   = list.filter { it.amount < 0 }.sumOf { -it.amount }
            BalanceCurrencyEntry(currency, owedToMe, youOwe, owedToMe - youOwe)
        }.filter { it.owedToMe > 0.0 || it.youOwe > 0.0 }
        _owedToYou.value = balances.filter { it.amount > 0 }.sumOf { it.amount }
        _youOwe.value    = balances.filter { it.amount < 0 }.sumOf { -it.amount }
    }

    fun loadData() {
        viewModelScope.launch {
            initialLoadDone = false

            // Step 1: Render cached friends + balances immediately — no network wait.
            val cachedFriends = friendRepository.getCachedFriends().stableSortedForHome()
            val cachedBalances = balanceRepository.getCachedAllBalances()
            if (cachedFriends.isNotEmpty()) {
                _friends.value = cachedFriends
                _friendsLoaded.value = true
                _friendsLoadFailed.value = false
                if (cachedBalances.isNotEmpty()) {
                    applyBalances(cachedBalances)
                    recomputeEffectiveBalances()
                }
            } else {
                _isLoading.value = true
            }

            // Step 2: Network fetch — updates state only when fresh data is ready.
            when (val result = getFriendsUseCase()) {
                is ApiResult.Success -> {
                    _friends.value = result.data.stableSortedForHome()
                    _friendsLoaded.value = true
                    _friendsLoadFailed.value = false
                }
                is ApiResult.NetworkError -> {
                    if (!_friendsLoaded.value) _friendsLoadFailed.value = true
                }
                else -> {
                    if (!_friendsLoaded.value) _friendsLoadFailed.value = true
                }
            }
            _isLoading.value = false

            // Step 3: Fresh balances from network.
            when (val result = getAllBalancesUseCase()) {
                is ApiResult.Success -> {
                    applyBalances(result.data)
                    recomputeEffectiveBalances()
                }
                else -> Unit
            }

            initialLoadDone = true
        }
    }

    fun refresh(manual: Boolean = false) {
        if (!initialLoadDone) return
        viewModelScope.launch {
            if (manual) _manualRefreshing.value = true
            try {
                val syncResult = syncManager.syncFriendsHome(SyncReason.MANUAL_REFRESH)
                // Gate on sync success: distinguishes empty=settled (ok=true) from
                // empty=unknown (ok=false). On failure, existing state remains visible.
                if (syncResult.friendsOk) {
                    _friends.value = friendRepository.getCachedFriends().stableSortedForHome()
                }
                if (syncResult.balancesOk) {
                    applyBalances(balanceRepository.getCachedAllBalances())
                    recomputeEffectiveBalances()
                }
            } finally {
                if (manual) _manualRefreshing.value = false
            }
        }
    }

    fun onSearchChanged(query: String) { _searchQuery.value = query }

    // ── Stable sort helper ───────────────────────────────────────────────────

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

        // ── Per-friend row effective balances (Wave 2F: includes group expenses) ─
        val currentUserId = tokenStore.getUserId() ?: ""

        // Step 1: direct-expense friend map (existing logic via EffectiveBalanceCalculator)
        val otherUserIdMap: Map<String, String?> = ops.mapNotNull { op ->
            val r = op.localResourceId ?: op.serverResourceId ?: return@mapNotNull null
            r to expenseRepository.getCachedDirectOtherUserId(r)
        }.toMap()
        val directFriends = effectiveCalculator.effectiveFriendBalances(
            confirmedFriendBase      = _balanceMap.value,
            ops                      = ops,
            expenseCache             = cache,
            impacts                  = impacts,
            otherUserIdForExpense    = { resourceId -> otherUserIdMap[resourceId] },
        )

        // Step 2: project GROUP expense pending impacts into per-friend deltas.
        // For each group op, calculate which friends are affected and by how much.
        val groupFriendDeltas = mutableMapOf<String, Double>()  // friendId → accumulated delta
        val groupFriendCurrency = mutableMapOf<String, String>()
        for (op in ops) {
            val r       = op.localResourceId ?: op.serverResourceId ?: continue
            val expense = cache[r] ?: continue
            if (expense.groupId == null) continue  // direct expenses handled above
            // Load payer/split detail for this group expense
            val detail = expenseRepository.getCachedExpenseWithDetail(r) ?: continue
            if (detail.payers.isEmpty() || detail.splits.isEmpty()) continue
            val friendDeltas = friendImpactCalculator.calculate(
                payers        = detail.payers,
                splits        = detail.splits,
                currentUserId = currentUserId,
            )
            val sign = when (op.operationType) {
                "CREATE_EXPENSE", "RESTORE_EXPENSE" -> 1.0
                "DELETE_EXPENSE"                    -> -1.0
                // UPDATE group friend projection requires old AND new payer/split data.
                // Without stored old split context the delta would be wrong.
                // TODO(Wave2F): add PendingExpenseMutationContextEntity with oldPayerData/oldSplitData
                "UPDATE_EXPENSE"                    -> 0.0
                else                                -> 0.0
            }
            if (sign == 0.0) continue
            for ((friendId, d) in friendDeltas) {
                groupFriendDeltas[friendId] = (groupFriendDeltas[friendId] ?: 0.0) + d * sign
                groupFriendCurrency[friendId] = expense.currency
            }
        }

        // Step 3: merge direct + group into final optimistic map
        val mergedFriends = directFriends.toMutableMap()
        for ((friendId, groupDelta) in groupFriendDeltas) {
            val currency = groupFriendCurrency[friendId] ?: continue
            val existing = mergedFriends[friendId]
            if (existing != null) {
                val hasCurrency = existing.any { it.second == currency }
                mergedFriends[friendId] = if (hasCurrency) {
                    // Update the matching currency entry
                    existing.map { (amt, cur) ->
                        if (cur == currency) Pair(amt + groupDelta, cur) else Pair(amt, cur)
                    }
                } else {
                    // Pending currency not in confirmed entries — append rather than drop
                    existing + Pair(groupDelta, currency)
                }
            } else {
                // Friend has no direct effective balance yet; start from currency-specific base.
                val confirmedBase = _balanceMap.value[friendId]
                    ?.firstOrNull { it.second == currency }?.first ?: 0.0
                mergedFriends[friendId] = listOf(Pair(confirmedBase + groupDelta, currency))
            }
        }

        // Track all friends with any pending sync (direct or group)
        val pendingSyncFriends = (directFriends.keys + groupFriendDeltas.keys).toSet()
        _friendsWithPendingSync.value = pendingSyncFriends
        _optimisticFriendBalanceMap.value = mergedFriends

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
/** Active friends first, then invited, then placeholder, then others. Within each: name → email → id. */
private fun List<Friend>.stableSortedForHome(): List<Friend> =
    sortedWith(
        compareBy<Friend> {
            when {
                it.isActive      -> 0
                it.isInvited     -> 1
                it.isPlaceholder -> 2
                else             -> 3
            }
        }
            .thenBy { it.fullName.lowercase() }
            .thenBy { it.email.lowercase() }
            .thenBy { it.id }
    )

sealed class FriendsActionState {
    object Idle : FriendsActionState()
    data class Success(val message: String) : FriendsActionState()
    data class Error(val message: String)   : FriendsActionState()
}