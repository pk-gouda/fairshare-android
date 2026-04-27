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
import com.prathik.fairshare.domain.repository.ImportRepository
import com.prathik.fairshare.domain.repository.SettlementRepository
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
        observeOptimisticBalance()
    }

    fun loadData() {
        viewModelScope.launch {
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
                    _userBalances.value = result.data
                    _netBalance.value = result.data.sumOf { it.amount }
                    _currency.value = result.data.firstOrNull()?.currency ?: "USD"
                }

                else -> _balancesLoadFailed.value = true
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
                is ApiResult.Success -> {
                    // Group settlements belong in the group timeline only.
                    // Show only non-group (direct) settlements in the friend detail screen.
                    _settlements.value = result.data.filter { it.groupId == null }
                }

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
            // Refresh net balance — always fresh from network (no cache)
            when (val result = balanceRepository.getNetBalanceWithUser(friendId)) {
                is ApiResult.Success -> {
                    _balancesLoadFailed.value = false
                    _userBalances.value = result.data
                    _netBalance.value = result.data.sumOf { it.amount }
                    _currency.value = result.data.firstOrNull()?.currency ?: "USD"
                }

                else -> _balancesLoadFailed.value = true
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
                is ApiResult.Success -> {
                    _settlements.value = result.data.filter { it.groupId == null }
                }

                else -> Unit
            }
        }
    }

    fun cancelSettlement(settlementId: String) {
        viewModelScope.launch {
            when (val result = settlementRepository.cancelSettlement(settlementId)) {
                is ApiResult.Success -> {
                    // Update in-place — row stays visible with CANCELLED status
                    _settlements.value = _settlements.value.map { s ->
                        if (s.id == settlementId) result.data else s
                    }
                    refreshExpenses()
                    _actionState.value = FriendDetailActionState.Success("Settlement cancelled")
                }

                is ApiResult.NetworkError ->
                    _actionState.value = FriendDetailActionState.Error("No internet connection.")

                else ->
                    _actionState.value =
                        FriendDetailActionState.Error("Failed to cancel settlement.")
            }
        }
    }

    fun restoreSettlement(settlementId: String) {
        viewModelScope.launch {
            when (val result = settlementRepository.restoreSettlement(settlementId)) {
                is ApiResult.Success -> {
                    _settlements.value = _settlements.value.map { s ->
                        if (s.id == settlementId) result.data else s
                    }
                    refreshExpenses()
                    _actionState.value = FriendDetailActionState.Success("Settlement restored")
                }

                is ApiResult.NetworkError ->
                    _actionState.value = FriendDetailActionState.Error("No internet connection.")

                else ->
                    _actionState.value =
                        FriendDetailActionState.Error("Failed to restore settlement.")
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
                    val confirmedBalance = _netBalance.value

                    // Filter to ops whose cached expense belongs to THIS friend.
                    // groupId == null scopes to direct expenses; otherUserId == friendId
                    // scopes to this specific friend. UPDATE is included for Pending sync
                    // label but its delta is not applied (unsafe without parsing JSON).
                    val relevantOps = ops.filter { op ->
                        val resourceId =
                            op.localResourceId ?: op.serverResourceId ?: return@filter false
                        val expense =
                            expenseRepository.getCachedExpense(resourceId) ?: return@filter false
                        if (expense.groupId != null) return@filter false
                        expenseRepository.getCachedDirectOtherUserId(resourceId) == friendId
                    }

                    if (relevantOps.isEmpty()) {
                        val wasSync = _hasPendingBalanceSync.value
                        _optimisticNetBalance.value = null
                        _optimisticBalanceCurrency.value = null
                        _hasPendingBalanceSync.value = false
                        // Pending ops cleared → refresh so confirmed balance fills
                        // the gap immediately rather than waiting for next load.
                        if (wasSync) refreshExpenses()
                        return@collect
                    }
                    _hasPendingBalanceSync.value = true   // scoped: only this friend's ops

                    // Currency safety: only apply optimistic delta when all relevant
                    // expenses share one currency that matches what we display.
                    val deltaExpenses = relevantOps.mapNotNull { op ->
                        val resourceId = op.localResourceId ?: op.serverResourceId ?: return@mapNotNull null
                        expenseRepository.getCachedExpense(resourceId)
                    }
                    val pendingCurrencies = deltaExpenses.map { it.currency }.toSet()
                    val confirmedCurrencies = _userBalances.value.map { it.currency }.toSet()
                    val displayCurrency = when {
                        confirmedCurrencies.size == 1 -> confirmedCurrencies.single()
                        confirmedCurrencies.isEmpty() && pendingCurrencies.size == 1 ->
                            pendingCurrencies.single()
                        else -> _currency.value
                    }
                    val currencySafe = pendingCurrencies.size == 1 &&
                            pendingCurrencies.single() == displayCurrency

                    if (!currencySafe) {
                        _optimisticNetBalance.value = null
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
                        _optimisticNetBalance.value = confirmedBalance + delta
                        _optimisticBalanceCurrency.value = displayCurrency
                    }
                }
        }
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
    data class LinkedToFriend(val realFriendId: String) : FriendDetailActionState()
}

sealed class SplitwiseImportState {
    object Idle : SplitwiseImportState()
    object Loading : SplitwiseImportState()
    data class Success(val expensesCreated: Int, val settlementsCreated: Int) :
        SplitwiseImportState()

    data class Error(val message: String) : SplitwiseImportState()
}