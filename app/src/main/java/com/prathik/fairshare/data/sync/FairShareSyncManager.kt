package com.prathik.fairshare.data.sync

import android.util.Log
import com.prathik.fairshare.data.local.EncryptedTokenStore
import com.prathik.fairshare.domain.model.ApiResult
import com.prathik.fairshare.domain.model.Expense
import com.prathik.fairshare.domain.model.NotificationType
import com.prathik.fairshare.domain.repository.BalanceRepository
import com.prathik.fairshare.domain.repository.ExpenseRepository
import com.prathik.fairshare.domain.repository.FriendRepository
import com.prathik.fairshare.domain.repository.GroupRepository
import com.prathik.fairshare.domain.repository.NotificationRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FairShareSyncManager @Inject constructor(
    private val tokenStore            : EncryptedTokenStore,
    private val groupRepository       : GroupRepository,
    private val friendRepository      : FriendRepository,
    private val expenseRepository     : ExpenseRepository,
    private val balanceRepository     : BalanceRepository,
    private val notificationRepository: NotificationRepository,
) {
    private val backgroundScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val _isRunning   = AtomicBoolean(false)
    private var _lastRunAt   = 0L

    private companion object {
        const val TAG              = "FairShareSync"
        const val MIN_INTERVAL_MS  = 5 * 60 * 1_000L
        const val EXPENSE_DETAIL_LIMIT = 10
        const val NOTIFICATION_PREFETCH_LIMIT = 20
    }

    // ── A. Account-level sync ─────────────────────────────────────────────────

    fun launchSyncAfterExpenseCreate(
        expense       : Expense,
        groupId       : String?,
        currentUserId : String,
        payerIds      : Set<String> = emptySet(),
        splitIds      : Set<String> = emptySet(),
    ) {
        backgroundScope.launch {
            syncAfterExpenseCreate(expense, groupId, currentUserId, payerIds, splitIds)
        }
    }

    fun launchSyncAfterExpenseUpdate(
        expense           : Expense,
        groupId           : String?,
        currentUserId     : String,
        oldParticipantIds : Set<String> = emptySet(),
        newParticipantIds : Set<String> = emptySet(),
    ) {
        backgroundScope.launch {
            syncAfterExpenseUpdate(expense, groupId, currentUserId, oldParticipantIds, newParticipantIds)
        }
    }

    fun launchSyncAfterExpenseDelete(
        expenseId     : String,
        groupId       : String?,
        currentUserId : String,
        participantIds: Set<String> = emptySet(),
    ) {
        backgroundScope.launch {
            syncAfterExpenseDelete(expenseId, groupId, currentUserId, participantIds)
        }
    }

    fun launchSyncAfterExpenseRestore(
        expense       : Expense,
        groupId       : String?,
        currentUserId : String,
        participantIds: Set<String> = emptySet(),
    ) {
        backgroundScope.launch {
            syncAfterExpenseRestore(expense, groupId, currentUserId, participantIds)
        }
    }

    fun syncAccountInBackground(reason: SyncReason) {
        if (!tokenStore.isLoggedIn()) return
        if (reason != SyncReason.LOGIN) {
            val now = System.currentTimeMillis()
            if (now - _lastRunAt < MIN_INTERVAL_MS) return
        }
        if (!_isRunning.compareAndSet(false, true)) return
        _lastRunAt = System.currentTimeMillis()
        backgroundScope.launch {
            try {
                Log.d(TAG, "[$reason] Account sync started")
                syncAccountCore(reason)
                Log.d(TAG, "[$reason] Account sync completed")
            } finally {
                _isRunning.set(false)
            }
        }
    }

    suspend fun syncAccountCore(reason: SyncReason) {
        Log.d(TAG, "[$reason] syncAccountCore begin")
        coroutineScope {
            launch { syncNotifications(reason) }
            launch { syncGroupsCore(reason) }   // return value intentionally ignored here
            launch { syncFriendsCore(reason) }  // return value intentionally ignored here
            launch { safe("getAllBalances") { balanceRepository.getAllBalances() } }
        }
        Log.d(TAG, "[$reason] syncAccountCore complete")
    }

    // ── B. Tab / screen-level sync ────────────────────────────────────────────

    /**
     * GroupsHome pull-to-refresh.
     *
     * Returns [GroupsHomeSyncResult] so the caller can distinguish:
     *   groupsOk=true  + empty cached groups       → user has no groups
     *   groupsOk=false + empty cached groups       → network failed, unknown state
     *   groupBalanceOkByGroupId[id]=true  + empty  → group is settled
     *   groupBalanceOkByGroupId[id]=false           → balance fetch failed, keep previous tile
     */
    suspend fun syncGroupsHome(reason: SyncReason): GroupsHomeSyncResult {
        Log.d(TAG, "[$reason] syncGroupsHome")
        return coroutineScope {
            // getAllBalances runs in parallel with syncGroupsCore
            val balancesDeferred: Deferred<Boolean> = async {
                safe("getAllBalances") { balanceRepository.getAllBalances() } is ApiResult.Success
            }
            val coreDeferred: Deferred<GroupsCoreResult> = async { syncGroupsCore(reason) }
            val coreResult = coreDeferred.await()
            GroupsHomeSyncResult(
                groupsOk              = coreResult.succeeded,
                balancesOk            = balancesDeferred.await(),
                groupBalanceOkByGroupId = coreResult.groupBalanceOkByGroupId,
            )
        }
    }

    /**
     * FriendsHome pull-to-refresh.
     *
     * Returns [FriendsHomeSyncResult] so the caller can distinguish:
     *   friendsOk=true  + empty cache → user has no friends
     *   friendsOk=false               → network failed, keep existing friend list
     */
    suspend fun syncFriendsHome(reason: SyncReason): FriendsHomeSyncResult {
        Log.d(TAG, "[$reason] syncFriendsHome")
        return coroutineScope {
            val balancesDeferred: Deferred<Boolean> = async {
                safe("getAllBalances") { balanceRepository.getAllBalances() } is ApiResult.Success
            }
            val friendsDeferred: Deferred<Boolean> = async { syncFriendsCore(reason) }
            FriendsHomeSyncResult(
                friendsOk  = friendsDeferred.await(),
                balancesOk = balancesDeferred.await(),
            )
        }
    }

    /**
     * GroupDetail pull-to-refresh.
     *
     * Returns [GroupDetailSyncResult] so the caller can distinguish:
     *   groupBalancesOk=true  + empty cache → group is fully settled
     *   groupBalancesOk=false               → balance fetch failed, keep existing state
     */
    suspend fun syncGroupDetail(groupId: String, reason: SyncReason): GroupDetailSyncResult {
        Log.d(TAG, "[$reason] syncGroupDetail $groupId")
        return coroutineScope {
            launch { safe("getAllBalances") { balanceRepository.getAllBalances() } }
            launch { safe("getGroupMembers") { groupRepository.getMembers(groupId) } }
            val groupBalancesDeferred: Deferred<Boolean> = async {
                safe("getGroupBalances") { groupRepository.getGroupBalances(groupId) } is ApiResult.Success
            }
            val expensesDeferred: Deferred<ApiResult<List<Expense>>?> = async {
                @Suppress("UNCHECKED_CAST")
                safe("getGroupExpenses") { expenseRepository.getGroupExpenses(groupId) }
                        as? ApiResult<List<Expense>>
            }
            val expResult = expensesDeferred.await()
            if (expResult is ApiResult.Success) {
                prefetchExpenseDetails(expResult.data.sortedByDescending { it.expenseDate }.take(EXPENSE_DETAIL_LIMIT))
            }
            GroupDetailSyncResult(groupBalancesOk = groupBalancesDeferred.await())
        }
    }

    /**
     * FriendDetail pull-to-refresh.
     *
     * Returns [FriendDetailSyncResult] so the caller can distinguish:
     *   netOk=true  + empty cache → friend is fully settled
     *   netOk=false               → balance fetch failed, keep existing state
     */
    suspend fun syncFriendDetail(friendId: String, reason: SyncReason): FriendDetailSyncResult {
        Log.d(TAG, "[$reason] syncFriendDetail $friendId")
        return coroutineScope {
            launch { safe("getAllBalances") { balanceRepository.getAllBalances() } }
            val netDeferred: Deferred<Boolean> = async {
                safe("getNetBalance($friendId)") { balanceRepository.getNetBalanceWithUser(friendId) } is ApiResult.Success
            }
            val breakdownDeferred: Deferred<Boolean> = async {
                safe("getBreakdown($friendId)") { balanceRepository.getBreakdownWithUser(friendId) } is ApiResult.Success
            }
            val directExpDeferred = async {
                safe("getDirectExpenses($friendId)") { expenseRepository.getDirectExpensesWithFriend(friendId) }
            }
            val expResult = directExpDeferred.await()
            if (expResult is ApiResult.Success) {
                @Suppress("UNCHECKED_CAST")
                val expenses = (expResult as ApiResult.Success<*>).data as? List<Expense>
                expenses?.let {
                    prefetchExpenseDetails(it.sortedByDescending { e -> e.expenseDate }.take(EXPENSE_DETAIL_LIMIT))
                }
            }
            FriendDetailSyncResult(
                netOk       = netDeferred.await(),
                breakdownOk = breakdownDeferred.await(),
            )
        }
    }

    suspend fun syncExpenseDetail(expenseId: String, reason: SyncReason) {
        Log.d(TAG, "[$reason] syncExpenseDetail $expenseId")
        safe("getExpense($expenseId)") { expenseRepository.getExpense(expenseId) }
    }

    // ── C. Mutation-driven sync ───────────────────────────────────────────────

    suspend fun syncAfterExpenseCreate(
        expense       : Expense,
        groupId       : String?,
        currentUserId : String,
        payerIds      : Set<String> = emptySet(),
        splitIds      : Set<String> = emptySet(),
    ) {
        Log.d(TAG, "[MUTATION_SUCCESS] syncAfterExpenseCreate ${expense.id} group=$groupId")
        mutationRefresh(
            expenseId      = expense.id,
            groupId        = groupId,
            currentUserId  = currentUserId,
            participantIds = payerIds + splitIds,
        )
    }

    suspend fun syncAfterExpenseUpdate(
        expense           : Expense,
        groupId           : String?,
        currentUserId     : String,
        oldParticipantIds : Set<String> = emptySet(),
        newParticipantIds : Set<String> = emptySet(),
    ) {
        Log.d(TAG, "[MUTATION_SUCCESS] syncAfterExpenseUpdate ${expense.id} group=$groupId")
        mutationRefresh(
            expenseId      = expense.id,
            groupId        = groupId,
            currentUserId  = currentUserId,
            participantIds = oldParticipantIds + newParticipantIds,
        )
    }

    suspend fun syncAfterExpenseDelete(
        expenseId     : String,
        groupId       : String?,
        currentUserId : String,
        participantIds: Set<String> = emptySet(),
    ) {
        Log.d(TAG, "[MUTATION_SUCCESS] syncAfterExpenseDelete $expenseId group=$groupId")
        mutationRefresh(
            expenseId         = expenseId,
            groupId           = groupId,
            currentUserId     = currentUserId,
            participantIds    = participantIds,
            skipExpenseDetail = true,
        )
    }

    suspend fun syncAfterExpenseRestore(
        expense       : Expense,
        groupId       : String?,
        currentUserId : String,
        participantIds: Set<String> = emptySet(),
    ) {
        Log.d(TAG, "[MUTATION_SUCCESS] syncAfterExpenseRestore ${expense.id} group=$groupId")
        mutationRefresh(
            expenseId      = expense.id,
            groupId        = groupId,
            currentUserId  = currentUserId,
            participantIds = participantIds,
        )
    }

    // ── Internal helpers ──────────────────────────────────────────────────────

    private suspend fun mutationRefresh(
        expenseId        : String,
        groupId          : String?,
        currentUserId    : String,
        participantIds   : Set<String>,
        skipExpenseDetail: Boolean = false,
    ) {
        val friends = participantIds.filter { it != currentUserId }
        Log.d(TAG, "Refreshing ${friends.size} participant friend scopes (groupId=$groupId)")

        // All branches are independent reads — a single coroutineScope lets global
        // balances, group balances, expense detail, and per-participant friend refreshes
        // run in parallel. Each is wrapped in safe() so a failure in one branch
        // does not cancel the others (exceptions caught inside safe()).
        coroutineScope {
            // 1. Global net balance — both tab top bars.
            launch { safe("getAllBalances") { balanceRepository.getAllBalances() } }

            if (groupId != null) {
                // 2. Group tile balance.
                launch { safe("getGroupBalances($groupId)") { groupRepository.getGroupBalances(groupId) } }
                // 3. Group expense list.
                launch { safe("getGroupExpenses($groupId)") { expenseRepository.getGroupExpenses(groupId) } }
            }

            // 4. Full expense detail so ExpenseDetail opens offline.
            if (!skipExpenseDetail) {
                launch { safe("getExpense($expenseId)") { expenseRepository.getExpense(expenseId) } }
            }

            // 5. FRIEND_NET + FRIEND_BREAKDOWN (+ direct expense list for non-group) per participant.
            for (friendId in friends) {
                launch {
                    safe("getNetBalance($friendId)") { balanceRepository.getNetBalanceWithUser(friendId) }
                    safe("getBreakdown($friendId)") { balanceRepository.getBreakdownWithUser(friendId) }
                    // For direct (non-group) expenses, also cache the direct expense list
                    // so the new row appears in FriendDetail offline immediately.
                    if (groupId == null) {
                        safe("getDirectExpenses($friendId)") {
                            expenseRepository.getDirectExpensesWithFriend(friendId)
                        }
                    }
                }
            }
        }
        Log.d(TAG, "mutationRefresh complete for expense=$expenseId")
    }

    /**
     * Fetches groups and per-group members/balances/expenses in parallel.
     * Returns [GroupsCoreResult] with per-group balance success so callers
     * can skip stale-balance updates for groups whose fetch failed.
     *
     * Return value is intentionally ignored by [syncAccountCore].
     */
    private suspend fun syncGroupsCore(reason: SyncReason): GroupsCoreResult {
        val listResult = safe("getMyGroups") { groupRepository.getMyGroups() }
        if (listResult !is ApiResult.Success) {
            return GroupsCoreResult(succeeded = false, groupBalanceOkByGroupId = emptyMap())
        }
        val groups = listResult.data
        Log.d(TAG, "[$reason] Groups fetched: ${groups.size}")

        // Launch one async per group; all groups run in parallel.
        // coroutineScope waits for every child to complete before returning,
        // so groupDeferreds are all completed when we exit the block.
        val groupDeferreds: List<Deferred<Pair<String, Boolean>>> = coroutineScope {
            groups.map { group ->
                async {
                    // Fire members fetch independently — we don't need its result.
                    launch { safe("getMembers(${group.id})") { groupRepository.getMembers(group.id) } }
                    // Track whether group balance refresh succeeded.
                    val balOk = safe("getGroupBalances(${group.id})") {
                        groupRepository.getGroupBalances(group.id)
                    } is ApiResult.Success
                    val expResult = safe("getGroupExpenses(${group.id})") {
                        expenseRepository.getGroupExpenses(group.id)
                    }
                    if (expResult is ApiResult.Success) {
                        val recent = expResult.data.sortedByDescending { it.expenseDate }.take(EXPENSE_DETAIL_LIMIT)
                        prefetchExpenseDetails(recent)
                    }
                    group.id to balOk
                }
            }
        }
        // All deferreds are already complete — await() is instant here.
        val balanceOkByGroupId = groupDeferreds.associate { it.await() }
        return GroupsCoreResult(succeeded = true, groupBalanceOkByGroupId = balanceOkByGroupId)
    }

    /**
     * Fetches friends and per-friend net/breakdown/direct-expenses in parallel.
     * Returns true if getFriends() succeeded, false otherwise.
     *
     * Return value is intentionally ignored by [syncAccountCore].
     */
    private suspend fun syncFriendsCore(reason: SyncReason): Boolean {
        val result = safe("getFriends") { friendRepository.getFriends() }
        if (result !is ApiResult.Success) return false
        val friends = result.data
        Log.d(TAG, "[$reason] Friends fetched: ${friends.size}")
        coroutineScope {
            for (friend in friends) {
                launch {
                    val friendId = friend.id
                    safe("getNetBalance($friendId)") { balanceRepository.getNetBalanceWithUser(friendId) }
                    safe("getBreakdown($friendId)") { balanceRepository.getBreakdownWithUser(friendId) }
                    val expResult = safe("getDirectExpenses($friendId)") { expenseRepository.getDirectExpensesWithFriend(friendId) }
                    if (expResult is ApiResult.Success) {
                        val recent = expResult.data.sortedByDescending { it.expenseDate }.take(EXPENSE_DETAIL_LIMIT)
                        prefetchExpenseDetails(recent)
                    }
                }
            }
        }
        return true
    }

    private suspend fun syncNotifications(reason: SyncReason) {
        val result = safe("getNotifications") { notificationRepository.getAll() }
        if (result !is ApiResult.Success) return
        val notifications = result.data
        Log.d(TAG, "[$reason] Notifications cached: ${notifications.size}")

        val expenseTypes = setOf(
            NotificationType.EXPENSE_ADDED.name,
            NotificationType.EXPENSE_UPDATED.name,
            NotificationType.EXPENSE_DELETED.name,
            NotificationType.EXPENSE_RESTORED.name,
            NotificationType.EXPENSE_AUTO_CREATED.name,
        )
        val refs = notifications
            .filter { it.type.name in expenseTypes && it.referenceId != null }
            .take(NOTIFICATION_PREFETCH_LIMIT)

        var count = 0
        coroutineScope {
            for (n in refs) {
                launch {
                    val id = n.referenceId ?: return@launch
                    if (safe("notif-expense($id)") { expenseRepository.getExpense(id) } is ApiResult.Success) count++
                }
            }
        }
        Log.d(TAG, "[$reason] Notification expense details prefetched: $count")
    }

    private suspend fun prefetchExpenseDetails(expenses: List<Expense>) {
        coroutineScope {
            for (exp in expenses) {
                launch { safe("getExpense(${exp.id})") { expenseRepository.getExpense(exp.id) } }
            }
        }
    }

    /** Unchanged — wraps a suspend call in runCatching, logs failures. */
    @Suppress("UNCHECKED_CAST")
    private suspend fun <T> safe(label: String, block: suspend () -> T): T? {
        val result = runCatching { block() }
            .onFailure { Log.w(TAG, "$label threw: ${it.message}") }
            .getOrNull()
        if (result is com.prathik.fairshare.domain.model.ApiResult<*> &&
            result !is com.prathik.fairshare.domain.model.ApiResult.Success<*>) {
            Log.w(TAG, "$label returned non-Success: $result")
        }
        return result
    }
}

// ── Sync result types ─────────────────────────────────────────────────────────
// Returned by screen-level sync methods so ViewModels can distinguish
// "network succeeded + empty result = settled/zero" from
// "network failed + empty cache = unknown state".
// Callers that do not need the result (e.g. syncAccountCore) may ignore it.

/** Internal result of [FairShareSyncManager.syncGroupsCore]. Not part of public API. */
internal data class GroupsCoreResult(
    val succeeded              : Boolean,
    val groupBalanceOkByGroupId: Map<String, Boolean>,
)

data class GroupsHomeSyncResult(
    val groupsOk              : Boolean,
    val balancesOk            : Boolean,
    /** Per-group flag: true = getGroupBalances succeeded for that group. */
    val groupBalanceOkByGroupId: Map<String, Boolean>,
)

data class FriendsHomeSyncResult(
    val friendsOk  : Boolean,
    val balancesOk : Boolean,
)

data class GroupDetailSyncResult(
    val groupBalancesOk: Boolean,
)

data class FriendDetailSyncResult(
    val netOk       : Boolean,
    val breakdownOk : Boolean,
)