package com.prathik.fairshare.ui.friends

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.prathik.fairshare.domain.model.ApiResult
import com.prathik.fairshare.domain.model.errorMessage
import com.prathik.fairshare.domain.model.Balance
import com.prathik.fairshare.domain.model.Expense
import com.prathik.fairshare.domain.model.Friend
import com.prathik.fairshare.domain.model.Settlement
import com.prathik.fairshare.domain.repository.BalanceRepository
import com.prathik.fairshare.data.local.PendingBalanceImpactEntity
import com.prathik.fairshare.data.sync.FairShareSyncManager
import com.prathik.fairshare.data.sync.SyncReason
import com.prathik.fairshare.domain.repository.ExpenseRepository
import com.prathik.fairshare.domain.repository.FriendRepository
import com.prathik.fairshare.domain.repository.ImportRepository
import com.prathik.fairshare.domain.repository.SettlementRepository
import com.prathik.fairshare.domain.usecase.balance.PendingFriendImpactCalculator
import com.prathik.fairshare.domain.usecase.settlement.GetSettlementHistoryUseCase
import com.prathik.fairshare.data.local.EncryptedTokenStore
import com.prathik.fairshare.data.sync.PendingOperationRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class FriendDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val friendRepository: FriendRepository,
    private val expenseRepository: ExpenseRepository,
    private val balanceRepository: BalanceRepository,
    private val getSettlementHistoryUseCase: GetSettlementHistoryUseCase,
    private val settlementRepository: SettlementRepository,
    private val tokenStore: EncryptedTokenStore,
    private val importRepository: ImportRepository,
    private val pendingOperationRepository: PendingOperationRepository,
    private val syncManager               : FairShareSyncManager,
    private val friendImpactCalculator    : PendingFriendImpactCalculator,
) : ViewModel() {

    val friendId: String = checkNotNull(savedStateHandle["friendId"])
    val currentUserId: String? = tokenStore.getUserId()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _friend = MutableStateFlow<Friend?>(null)
    val friend: StateFlow<Friend?> = _friend.asStateFlow()

    private val _friendStatus = MutableStateFlow<String?>(null)
    val friendStatus: StateFlow<String?> = _friendStatus.asStateFlow()

    private val _friends =
        MutableStateFlow<List<com.prathik.fairshare.domain.model.Friend>>(emptyList())
    val friends: StateFlow<List<com.prathik.fairshare.domain.model.Friend>> = _friends.asStateFlow()

    private val _netBalance = MutableStateFlow(0.0)
    val netBalance: StateFlow<Double> = _netBalance.asStateFlow()

    private val _balancesLoadFailed = MutableStateFlow(false)
    val balancesLoadFailed: StateFlow<Boolean> = _balancesLoadFailed.asStateFlow()

    /**
     * True once balances have been loaded from cache or network.
     * UI must not show SETTLED_WITH_HISTORY until this is true.
     */
    private val _balancesLoaded = MutableStateFlow(false)
    val balancesLoaded: StateFlow<Boolean> = _balancesLoaded.asStateFlow()

    val pendingDeleteExpenseIds: StateFlow<Set<String>> =
        pendingOperationRepository.observePendingDeleteResourceIds()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptySet())

    private val _optimisticNetBalance = MutableStateFlow<Double?>(null)
    val optimisticNetBalance: StateFlow<Double?> = _optimisticNetBalance.asStateFlow()

    /** Currency of the optimistic balance; null when delta is not applied (unsafe/mixed). */
    private val _optimisticBalanceCurrency = MutableStateFlow<String?>(null)
    val optimisticBalanceCurrency: StateFlow<String?> = _optimisticBalanceCurrency.asStateFlow()

    private val _hasPendingBalanceSync = MutableStateFlow(false)
    val hasPendingBalanceSync: StateFlow<Boolean> = _hasPendingBalanceSync.asStateFlow()

    private val _currency = MutableStateFlow("USD")
    val currency: StateFlow<String> = _currency.asStateFlow()

    /** Raw per-currency balance entries — used for multi-currency balance bar display. */
    private val _userBalances = MutableStateFlow<List<Balance>>(emptyList())
    val userBalances: StateFlow<List<Balance>> = _userBalances.asStateFlow()

    // Per-group balance breakdown — shown in the balance card, not as expense rows
    private val _groupBalances = MutableStateFlow<List<Balance>>(emptyList())
    val groupBalances: StateFlow<List<Balance>> = _groupBalances.asStateFlow()

    /** Confirmed group balances + pending group impact overlaid. Null = no pending ops active. */
    private val _effectiveGroupBalances = MutableStateFlow<List<Balance>?>(null)
    val effectiveGroupBalances: StateFlow<List<Balance>?> = _effectiveGroupBalances.asStateFlow()

    /** GroupIds whose balance includes at least one active pending local impact. */
    private val _pendingGroupIds = MutableStateFlow<Set<String>>(emptySet())
    val pendingGroupIds: StateFlow<Set<String>> = _pendingGroupIds.asStateFlow()

    // Settlement history with this friend — shown in the timeline
    private val _settlements = MutableStateFlow<List<Settlement>>(emptyList())
    val settlements: StateFlow<List<Settlement>> = _settlements.asStateFlow()

    private val _expensesState = MutableStateFlow<FriendExpensesState>(FriendExpensesState.Loading)
    val expensesState: StateFlow<FriendExpensesState> = _expensesState.asStateFlow()

    private val _actionState =
        MutableStateFlow<FriendDetailActionState>(FriendDetailActionState.Idle)
    val actionState: StateFlow<FriendDetailActionState> = _actionState.asStateFlow()

    private var initialLoadDone = false

    /** True only during a user-initiated pull-to-refresh. Drives the visible indicator. */
    private val _manualRefreshing = MutableStateFlow(false)
    val manualRefreshing: StateFlow<Boolean> = _manualRefreshing.asStateFlow()

    init {
        loadData()
        observeOptimisticBalance()
    }

    /** Render any cached data immediately before starting network calls. */
    private suspend fun renderCachedSnapshot() {
        // Cached friend profile — prevents FsLoadingScreen when Room has the friend
        friendRepository.getCachedFriend(friendId)?.let { cachedFriend ->
            _friend.value = cachedFriend
            _friendStatus.value = when {
                cachedFriend.isPlaceholder -> "placeholder"
                cachedFriend.isInvited -> "invited"
                else -> null
            }
        }
        // Cached direct expenses
        val cachedExpenses = expenseRepository.getCachedDirectExpensesWithFriend(friendId)
        if (cachedExpenses.isNotEmpty()) {
            _expensesState.value = FriendExpensesState.Success(
                cachedExpenses.stableSortedForTimeline()
            )
        }
        // Cached net balance
        val cachedNet = balanceRepository.getCachedNetBalanceWithUser(friendId)
        if (cachedNet.isNotEmpty()) {
            _userBalances.value = cachedNet
            _netBalance.value = cachedNet.sumOf { it.amount }
            _currency.value = cachedNet.firstOrNull()?.currency ?: "USD"
            _balancesLoaded.value = true
            _balancesLoadFailed.value = false
        }
        // Cached group breakdown
        val cachedBreakdown = balanceRepository.getCachedBreakdownWithUser(friendId)
        if (cachedBreakdown.isNotEmpty()) {
            _groupBalances.value = cachedBreakdown
        }
        // Cached direct settlements
        val cachedSettlements = settlementRepository.getCachedDirectSettlements(friendId)
        if (cachedSettlements.isNotEmpty()) {
            _settlements.value = cachedSettlements.filter { it.groupId == null }
        }
    }

    fun loadData() {
        viewModelScope.launch {
            initialLoadDone = false

            // Step 1: Render cached data immediately — no network wait.
            renderCachedSnapshot()

            // Step 2: Set Loading only for states not yet satisfied from cache.
            if (_expensesState.value !is FriendExpensesState.Success) {
                _expensesState.value = FriendExpensesState.Loading
            }
            _isLoading.value = true

            val friendsDeferred = async { friendRepository.getFriends() }
            val sentDeferred = async { friendRepository.getSentRequests() }
            val balancesDeferred = async { balanceRepository.getNetBalanceWithUser(friendId) }
            val breakdownDeferred = async { balanceRepository.getBreakdownWithUser(friendId) }
            val directDeferred = async { expenseRepository.getDirectExpensesWithFriend(friendId) }
            val settlementsDeferred = async { getSettlementHistoryUseCase(friendId) }

            // Resolve friend
            val allFriends = (friendsDeferred.await() as? ApiResult.Success)?.data ?: emptyList()
            // Expose friends list for the "link placeholder" sheet
            _friends.value = allFriends.filter { !it.isPlaceholder }
            val found = allFriends.find { it.id == friendId }

            if (found != null) {
                _friend.value = found
                _friendStatus.value = when {
                    found.isPlaceholder -> "placeholder"
                    found.isInvited -> "invited"
                    else -> null
                }
            } else {
                val sent = (sentDeferred.await() as? ApiResult.Success)
                    ?.data?.find { it.receiverId == friendId }
                if (sent != null) {
                    _friend.value = Friend(sent.receiverId, sent.receiverName, "", null)
                    _friendStatus.value = "pending"
                }
            }

            // Net balance — always fresh from network, never cached.
            // Using getNetBalanceWithUser instead of getAllBalancesUseCase because
            // getAllBalances() serves the Room cache; after a settlement the cached
            // value can be stale until the background refresh completes.
            when (val result = balancesDeferred.await()) {
                is ApiResult.Success -> {
                    _balancesLoadFailed.value = false
                    _balancesLoaded.value = true
                    _userBalances.value = result.data
                    _netBalance.value = result.data.sumOf { it.amount }
                    _currency.value = result.data.firstOrNull()?.currency ?: "USD"
                }
                else -> {
                    if (!_balancesLoaded.value) _balancesLoadFailed.value = true
                }
            }

            // Per-group breakdown — shown in the balance card
            when (val result = breakdownDeferred.await()) {
                is ApiResult.Success -> _groupBalances.value = result.data
                else -> Unit
            }

            // Only direct (non-group) expenses in the timeline
            val directExpenses =
                when (val directResult = directDeferred.await()) {
                    is ApiResult.Success -> directResult.data
                    // Network failed — preserve existing loaded expenses rather than
                    // wiping the list. Cached data from Room fallback or prior load stays.
                    else -> (_expensesState.value as? FriendExpensesState.Success)?.expenses ?: emptyList()
                }

            // Settlement history with this friend
            when (val result = settlementsDeferred.await()) {
                is ApiResult.Success -> {
                    // Group settlements belong in the group timeline only.
                    // Show only non-group (direct) settlements in the friend detail screen.
                    _settlements.value = result.data.filter { it.groupId == null }
                }

                else -> Unit
            }

            _expensesState.value = FriendExpensesState.Success(
                directExpenses.stableSortedForTimeline()
            )

            initialLoadDone = true
            _isLoading.value = false
        }
    }

    fun refreshExpenses(manual: Boolean = false) {
        if (!initialLoadDone) return
        viewModelScope.launch {
            if (manual) _manualRefreshing.value = true
            try {
                // Step 1 — Room-only read: show cached data immediately with no network wait.
                val cachedExpenses = expenseRepository.getCachedDirectExpensesWithFriend(friendId)
                if (cachedExpenses.isNotEmpty()) {
                    _expensesState.value = FriendExpensesState.Success(
                        cachedExpenses.stableSortedForTimeline()
                    )
                }

                // Step 2 — Network sync (same coroutine so indicator stays until done).
                syncManager.syncFriendDetail(friendId, SyncReason.MANUAL_REFRESH)
                when (val result = balanceRepository.getNetBalanceWithUser(friendId)) {
                    is ApiResult.Success -> {
                        _balancesLoadFailed.value = false
                        _balancesLoaded.value = true
                        _userBalances.value = result.data
                        _netBalance.value = result.data.sumOf { it.amount }
                        _currency.value = result.data.firstOrNull()?.currency ?: "USD"
                    }
                    else -> {
                        if (!_balancesLoaded.value && _userBalances.value.isEmpty()) {
                            _balancesLoadFailed.value = true
                        }
                    }
                }
                when (val result = balanceRepository.getBreakdownWithUser(friendId)) {
                    is ApiResult.Success -> _groupBalances.value = result.data
                    else -> Unit
                }
                val refreshedExpenses = expenseRepository.getCachedDirectExpensesWithFriend(friendId)
                if (refreshedExpenses.isNotEmpty() || _expensesState.value is FriendExpensesState.Success) {
                    _expensesState.value = FriendExpensesState.Success(
                        refreshedExpenses.stableSortedForTimeline()
                    )
                }
                when (val result = getSettlementHistoryUseCase(friendId)) {
                    is ApiResult.Success -> _settlements.value = result.data.filter { it.groupId == null }
                    else -> Unit
                }
            } finally {
                if (manual) _manualRefreshing.value = false
            }
        }
    }

    // Per-settlement idempotency keys. Map allows multiple settlements to be
    // retried independently. Key is generated on first attempt, retained across
    // NetworkError, and removed after terminal success or non-retryable failure.
    private val cancelIdempotencyKeys  = mutableMapOf<String, String>()
    private val restoreIdempotencyKeys = mutableMapOf<String, String>()

    fun cancelSettlement(settlementId: String) {
        viewModelScope.launch {
            val key = cancelIdempotencyKeys.getOrPut(settlementId) { UUID.randomUUID().toString() }
            when (val result = settlementRepository.cancelSettlement(settlementId, key)) {
                is ApiResult.Success -> {
                    cancelIdempotencyKeys.remove(settlementId)
                    // Update in-place — row stays visible with CANCELLED status
                    _settlements.value = _settlements.value.map { s ->
                        if (s.id == settlementId) result.data else s
                    }
                    refreshExpenses()
                    _actionState.value = FriendDetailActionState.Success("Settlement cancelled")
                }
                is ApiResult.NetworkError -> {
                    // Retain key for retry
                    _actionState.value = FriendDetailActionState.Error("No internet connection.")
                }
                else -> {
                    cancelIdempotencyKeys.remove(settlementId)
                    _actionState.value = FriendDetailActionState.Error(result.errorMessage() ?: "Failed to cancel settlement.")
                }
            }
        }
    }

    fun restoreSettlement(settlementId: String) {
        viewModelScope.launch {
            val key = restoreIdempotencyKeys.getOrPut(settlementId) { UUID.randomUUID().toString() }
            when (val result = settlementRepository.restoreSettlement(settlementId, key)) {
                is ApiResult.Success -> {
                    restoreIdempotencyKeys.remove(settlementId)
                    _settlements.value = _settlements.value.map { s ->
                        if (s.id == settlementId) result.data else s
                    }
                    refreshExpenses()
                    _actionState.value = FriendDetailActionState.Success("Settlement restored")
                }
                is ApiResult.NetworkError -> {
                    // Retain key for retry
                    _actionState.value = FriendDetailActionState.Error("No internet connection.")
                }
                else -> {
                    restoreIdempotencyKeys.remove(settlementId)
                    _actionState.value = FriendDetailActionState.Error(result.errorMessage() ?: "Failed to restore settlement.")
                }
            }
        }
    }

    /** @deprecated Use cancelSettlement() */
    fun deleteSettlement(settlementId: String) = cancelSettlement(settlementId)

    fun assignFriendPlaceholder(placeholderUserId: String, friendUserId: String) {
        viewModelScope.launch {
            when (importRepository.assignFriendPlaceholder(placeholderUserId, friendUserId)) {
                is ApiResult.Success -> {
                    // Navigate to the real friend's detail screen instead of staying
                    // on the now-deactivated placeholder screen
                    _actionState.value = FriendDetailActionState.LinkedToFriend(friendUserId)
                }

                is ApiResult.NetworkError ->
                    _actionState.value = FriendDetailActionState.Error("No internet connection.")

                else ->
                    _actionState.value = FriendDetailActionState.Error("Failed to link friend.")
            }
        }
    }

    fun resetActionState() {
        _actionState.value = FriendDetailActionState.Idle
    }

    // ── Splitwise import from FriendDetail ────────────────────────────────────

    val currentUserFullName: String get() = tokenStore.getFullName()

    private val _importState = MutableStateFlow<SplitwiseImportState>(SplitwiseImportState.Idle)
    val importState: StateFlow<SplitwiseImportState> = _importState.asStateFlow()

    private val _csvMemberNames = MutableStateFlow<List<String>>(emptyList())
    val csvMemberNames: StateFlow<List<String>> = _csvMemberNames.asStateFlow()

    fun parseCsvNames(csvContent: String) {
        val header = csvContent.lines().firstOrNull {
            it.contains(",") && !it.startsWith("Note") && !it.isBlank()
                    && it.contains("Date", ignoreCase = true)
        } ?: return
        val cols = header.split(",").map { it.trim().removeSurrounding("\"") }
        if (cols.size > 5) {
            _csvMemberNames.value = cols.drop(5)
                .filter { it.isNotBlank() && !it.contains("(removed)", ignoreCase = true) }
        }
    }

    fun clearCsvNames() {
        _csvMemberNames.value = emptyList()
    }

    fun resetImportState() {
        _importState.value = SplitwiseImportState.Idle
    }

    /**
     * Imports a friend-level Splitwise CSV and automatically links the other
     * person's placeholder to this friend's FairShare account — no manual step needed.
     */
    fun importFromSplitwise(csvContent: String, importerCsvName: String) {
        viewModelScope.launch {
            _importState.value = SplitwiseImportState.Loading
            when (val result = importRepository.importFriend(csvContent, importerCsvName)) {
                is ApiResult.Success -> {
                    val data = result.data
                    val expensesCreated = (data["expensesCreated"] as? Int) ?: 0
                    val settlementsCreated = (data["settlementsCreated"] as? Int) ?: 0

                    // Auto-link the other person's placeholder to this friend
                    @Suppress("UNCHECKED_CAST")
                    val members = data["members"] as? List<Map<String, String>> ?: emptyList()
                    val unclaimed = members.firstOrNull { it["status"] == "UNCLAIMED" }
                    val placeholderUserId = unclaimed?.get("placeholderUserId")
                    if (!placeholderUserId.isNullOrBlank()) {
                        importRepository.assignFriendPlaceholder(placeholderUserId, friendId)
                    }

                    _importState.value =
                        SplitwiseImportState.Success(expensesCreated, settlementsCreated)
                    refreshExpenses()
                }

                is ApiResult.NetworkError ->
                    _importState.value = SplitwiseImportState.Error("No internet connection.")

                else ->
                    _importState.value =
                        SplitwiseImportState.Error("Import failed. Please check the CSV and try again.")
            }
        }
    }

    // ── Optimistic balance (Wave 2D-Balance Optimism) ─────────────────────────

    private fun observeOptimisticBalance() {
        viewModelScope.launch {
            pendingOperationRepository.observeActivePendingExpenseOps()
                .collect { ops ->
                    if (ops.isEmpty()) {
                        val wasSync = _hasPendingBalanceSync.value
                        _optimisticNetBalance.value = null
                        _optimisticBalanceCurrency.value = null
                        _effectiveGroupBalances.value = null
                        _pendingGroupIds.value = emptySet()
                        _hasPendingBalanceSync.value = false
                        if (wasSync) refreshExpenses()
                        return@collect
                    }

                    // Separate ops into direct (groupId==null, otherUserId==friendId)
                    // and group (groupId!=null, friendId is a participant).
                    data class OpWithExpense(
                        val op: com.prathik.fairshare.data.local.PendingOperationEntity,
                        val expense: com.prathik.fairshare.domain.model.Expense,
                        val isGroup: Boolean,
                    )

                    val relevant = mutableListOf<OpWithExpense>()
                    for (op in ops) {
                        val resourceId = op.localResourceId ?: op.serverResourceId ?: continue
                        val expense = expenseRepository.getCachedExpense(resourceId) ?: continue
                        when {
                            // Direct expense with this friend
                            expense.groupId == null &&
                                    expenseRepository.getCachedDirectOtherUserId(resourceId) == friendId -> {
                                relevant.add(OpWithExpense(op, expense, false))
                            }
                            // Group expense — include if friendId appears in payer/split rows
                            expense.groupId != null -> {
                                val detail = expenseRepository.getCachedExpenseWithDetail(resourceId)
                                    ?: continue
                                val participants = (detail.payers.map { it.userId } +
                                        detail.splits.map { it.userId }).toSet()
                                if (friendId in participants) {
                                    relevant.add(OpWithExpense(op, detail, true))
                                }
                            }
                        }
                    }

                    if (relevant.isEmpty()) {
                        val wasSync = _hasPendingBalanceSync.value
                        _optimisticNetBalance.value = null
                        _optimisticBalanceCurrency.value = null
                        _effectiveGroupBalances.value = null
                        _pendingGroupIds.value = emptySet()
                        _hasPendingBalanceSync.value = false
                        if (wasSync) refreshExpenses()
                        return@collect
                    }
                    _hasPendingBalanceSync.value = true

                    // Currency safety
                    val pendingCurrencies = relevant.map { it.expense.currency }.toSet()
                    val confirmedCurrencies = _userBalances.value.map { it.currency }.toSet()
                    val displayCurrency = when {
                        confirmedCurrencies.size == 1 -> confirmedCurrencies.single()
                        confirmedCurrencies.isEmpty() && pendingCurrencies.size == 1 ->
                            pendingCurrencies.single()
                        else -> _currency.value
                    }
                    if (pendingCurrencies.size != 1 || pendingCurrencies.single() != displayCurrency) {
                        _optimisticNetBalance.value = null
                        _optimisticBalanceCurrency.value = null
                        _effectiveGroupBalances.value = null
                        _pendingGroupIds.value = emptySet()
                        return@collect
                    }

                    val updateImpacts = pendingOperationRepository.getImpactsForFriend(friendId)
                        .associateBy { it.operationId }

                    val confirmedBalance = _netBalance.value
                    var delta = 0.0

                    for ((op, expense, isGroup) in relevant) {
                        val resourceId = op.localResourceId ?: op.serverResourceId ?: continue
                        if (expense.currency != displayCurrency) continue

                        if (!isGroup) {
                            // Direct expense — use yourBalance as before
                            when (op.operationType) {
                                "CREATE_EXPENSE"  -> delta += expense.yourBalance
                                "DELETE_EXPENSE"  -> delta -= expense.yourBalance
                                "RESTORE_EXPENSE" -> delta += expense.yourBalance
                                "UPDATE_EXPENSE"  -> {
                                    val impact = updateImpacts[op.operationId]
                                    if (impact != null) delta += impact.delta
                                }
                            }
                        } else {
                            // Group expense — calculate friend-level delta from payer/split data
                            val currentUserId = tokenStore.getUserId() ?: continue
                            val friendDeltas = friendImpactCalculator.calculate(
                                payers        = expense.payers,
                                splits        = expense.splits,
                                currentUserId = currentUserId,
                            )
                            val friendDelta = friendDeltas[friendId] ?: 0.0
                            when (op.operationType) {
                                "CREATE_EXPENSE"  -> delta += friendDelta
                                "DELETE_EXPENSE"  -> delta -= friendDelta
                                "RESTORE_EXPENSE" -> delta += friendDelta
                                "UPDATE_EXPENSE"  -> {
                                    // Group UPDATE friend projection requires old AND new payer/split
                                    // context to compute (newFriendImpact - oldFriendImpact) correctly.
                                    // TODO(Wave2F): add PendingExpenseMutationContextEntity to store
                                    // oldPayerData/oldSplitData before offline edit overwrites Room.
                                    // Skipping to avoid showing a wrong number.
                                }
                            }
                        }
                    }

                    _optimisticNetBalance.value = confirmedBalance + delta
                    _optimisticBalanceCurrency.value = displayCurrency

                    // ── Effective group breakdown tiles ──────────────────────────────
                    // Apply pending group deltas to per-group Balance rows so the
                    // group tile in FriendDetail reflects the pending state.
                    val pendingIds = mutableSetOf<String>()
                    val confirmedGroupMap = _groupBalances.value
                        .associateBy { it.groupId }.toMutableMap()

                    for ((op, expense, isGroup) in relevant) {
                        if (!isGroup) continue
                        if (expense.currency != displayCurrency) continue
                        val currentUserId = tokenStore.getUserId() ?: continue
                        val gId = expense.groupId ?: continue
                        val friendDeltas = friendImpactCalculator.calculate(
                            payers        = expense.payers,
                            splits        = expense.splits,
                            currentUserId = currentUserId,
                        )
                        val friendDelta = friendDeltas[friendId] ?: 0.0
                        val signedDelta = when (op.operationType) {
                            "CREATE_EXPENSE", "RESTORE_EXPENSE" -> friendDelta
                            "DELETE_EXPENSE"                    -> -friendDelta
                            // UPDATE: skip group tile projection (needs old/new split data)
                            // TODO(Wave2F): store oldPayerData/oldSplitData for exact delta
                            else -> 0.0
                        }
                        if (signedDelta == 0.0) continue
                        pendingIds.add(gId)
                        val existing = confirmedGroupMap[gId]
                        if (existing != null) {
                            confirmedGroupMap[gId] = existing.copy(
                                amount = existing.amount + signedDelta
                            )
                        } else {
                            // No confirmed row yet — create a temporary pending row
                            confirmedGroupMap[gId] = Balance(
                                userId            = currentUserId ?: "",
                                otherUserId       = friendId,
                                otherUserName     = "",
                                amount            = signedDelta,
                                currency          = displayCurrency,
                                groupId           = gId,
                                groupName         = expense.groupName,
                                groupLastActivity = null,
                            )
                        }
                    }
                    _pendingGroupIds.value = pendingIds
                    _effectiveGroupBalances.value =
                        if (pendingIds.isEmpty()) null
                        else confirmedGroupMap.values.toList()
                }
        }
    }

}

// ── Stable timeline sort helpers ─────────────────────────────────────────────

/**
 * Within the same [expenseDate], sort by [createdAt] (or [updatedAt] as fallback)
 * so same-day expenses appear in consistent add order. Final tie-breaker: [id] ASC.
 */
private fun List<Expense>.stableSortedForTimeline(): List<Expense> =
    sortedWith(
        compareByDescending<Expense> { it.expenseDate }
            .thenByDescending { it.createdAt.ifBlank { it.updatedAt } }
            .thenBy { it.id }
    )

sealed class FriendExpensesState {
    object Loading : FriendExpensesState()
    data class Success(val expenses: List<Expense>) : FriendExpensesState()
    data class Error(val message: String) : FriendExpensesState()
}

sealed class FriendDetailActionState {
    object Idle : FriendDetailActionState()
    data class Success(val message: String) : FriendDetailActionState()
    data class Error(val message: String) : FriendDetailActionState()
    data class LinkedToFriend(val realFriendId: String) : FriendDetailActionState()
}

sealed class SplitwiseImportState {
    object Idle : SplitwiseImportState()
    object Loading : SplitwiseImportState()
    data class Success(val expensesCreated: Int, val settlementsCreated: Int) :
        SplitwiseImportState()

    data class Error(val message: String) : SplitwiseImportState()
}