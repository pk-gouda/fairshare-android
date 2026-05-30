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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Centralized cache/sync coordinator for FairShare.
 *
 * Ownership model:
 *   Backend  = final truth
 *   Room     = local mirror
 *   Pending  = unsynced local actions
 *   UI       = Room confirmed data + pending overlay
 *
 * This class is the single owner of "which cache scopes to refresh and when."
 * It replaces the scattered refresh calls across ViewModels and the now-thin
 * CacheWarmupCoordinator / ExpenseMutationCacheRefresher (which delegate here).
 *
 * Threading:
 * - [backgroundScope] is used for fire-and-forget warmup.
 * - All suspend functions run on the caller's coroutine — safe to call from
 *   ViewModelScope (foreground) or SyncWorker (IO).
 *
 * Error handling:
 * - Each scope refresh is individually wrapped in runCatching.
 * - One failed scope never aborts others.
 */
@Singleton
class FairShareSyncManager @Inject constructor(
    private val tokenStore            : EncryptedTokenStore,
    private val groupRepository       : GroupRepository,
    private val friendRepository      : FriendRepository,
    private val expenseRepository     : ExpenseRepository,
    private val balanceRepository     : BalanceRepository,
    private val notificationRepository: NotificationRepository,
) {
    /** Used only for fire-and-forget background paths (warmup, foreground). */
    private val backgroundScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    // Throttle guard for foreground/warmup runs.
    private val _isRunning   = AtomicBoolean(false)
    private var _lastRunAt   = 0L

    private companion object {
        const val TAG              = "FairShareSync"
        const val MIN_INTERVAL_MS  = 5 * 60 * 1_000L   // 5 min throttle for warmup
        const val EXPENSE_DETAIL_LIMIT = 10             // max detail prefetches per group/friend
        const val NOTIFICATION_PREFETCH_LIMIT = 20
    }

    // ─────────────────────────────────────────────────────────────────────────
    // A. Account-level sync (warmup / foreground / login)
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Fire-and-forget version of [syncAfterExpenseCreate].
     * Runs in [backgroundScope] so it is NOT cancelled when AddExpenseViewModel
     * is cleared after the screen is popped. Safe to call immediately before or
     * after emitting Success from AddExpenseViewModel.
     */
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

    /**
     * Fire-and-forget cache refresh after an online expense update.
     * Runs in [backgroundScope] so it survives ViewModel teardown after
     * the screen navigates back. Mirrors [launchSyncAfterExpenseCreate].
     */
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

    /**
     * Fire-and-forget cache refresh after an online expense delete.
     * Runs in [backgroundScope] so it survives ViewModel teardown after
     * the screen navigates back. Mirrors [launchSyncAfterExpenseCreate].
     */
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

    /**
     * Fire-and-forget cache refresh after an online expense restore.
     * Runs in [backgroundScope] so it survives ViewModel teardown after
     * the screen navigates back. Mirrors [launchSyncAfterExpenseCreate].
     */
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

    /**
     * Fire-and-forget full account sync.
     * Throttled — ignores call if a run started within [MIN_INTERVAL_MS].
     * Safe to call from MainActivity.onStart() or AuthViewModel after login.
     */
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

    /** Full account sync — suspend, runs on caller's coroutine. */
    suspend fun syncAccountCore(reason: SyncReason) {
        Log.d(TAG, "[$reason] syncAccountCore begin")
        coroutineScope {
            launch { syncNotifications(reason) }
            launch { syncGroupsCore(reason) }
            launch { syncFriendsCore(reason) }
            launch { safe("getAllBalances") { balanceRepository.getAllBalances() } }
        }
        Log.d(TAG, "[$reason] syncAccountCore complete")
    }

    // ─────────────────────────────────────────────────────────────────────────
    // B. Tab / screen-level sync (called by ViewModels on manual refresh)
    // ─────────────────────────────────────────────────────────────────────────

    /** GroupsHome pull-to-refresh: global summary + groups + group balances. */
    suspend fun syncGroupsHome(reason: SyncReason) {
        Log.d(TAG, "[$reason] syncGroupsHome")
        coroutineScope {
            launch { safe("getAllBalances") { balanceRepository.getAllBalances() } }
            launch { syncGroupsCore(reason) }
        }
    }

    /** FriendsHome pull-to-refresh: global summary + friends + friend net/breakdown. */
    suspend fun syncFriendsHome(reason: SyncReason) {
        Log.d(TAG, "[$reason] syncFriendsHome")
        coroutineScope {
            launch { safe("getAllBalances") { balanceRepository.getAllBalances() } }
            launch { syncFriendsCore(reason) }
        }
    }

    /** GroupDetail pull-to-refresh: group members, balances, expenses, detail prefetch. */
    suspend fun syncGroupDetail(groupId: String, reason: SyncReason) {
        Log.d(TAG, "[$reason] syncGroupDetail $groupId")
        coroutineScope {
            launch { safe("getAllBalances") { balanceRepository.getAllBalances() } }
            launch { safe("getGroupMembers") { groupRepository.getMembers(groupId) } }
            launch { safe("getGroupBalances") { groupRepository.getGroupBalances(groupId) } }
            launch {
                val result = safe("getGroupExpenses") { expenseRepository.getGroupExpenses(groupId) }
                if (result is ApiResult.Success) {
                    prefetchExpenseDetails(result.data.sortedByDescending { it.expenseDate }.take(EXPENSE_DETAIL_LIMIT))
                }
            }
        }
    }

    /** FriendDetail pull-to-refresh: net balance, breakdown, direct expenses, detail prefetch. */
    suspend fun syncFriendDetail(friendId: String, reason: SyncReason) {
        Log.d(TAG, "[$reason] syncFriendDetail $friendId")
        coroutineScope {
            launch { safe("getAllBalances") { balanceRepository.getAllBalances() } }
            launch { safe("getNetBalance($friendId)") { balanceRepository.getNetBalanceWithUser(friendId) } }
            launch { safe("getBreakdown($friendId)") { balanceRepository.getBreakdownWithUser(friendId) } }
            launch {
                val result = safe("getDirectExpenses($friendId)") { expenseRepository.getDirectExpensesWithFriend(friendId) }
                if (result is ApiResult.Success) {
                    prefetchExpenseDetails(result.data.sortedByDescending { it.expenseDate }.take(EXPENSE_DETAIL_LIMIT))
                }
            }
        }
    }

    /** Fetch and cache a single expense's full detail (payers + splits). */
    suspend fun syncExpenseDetail(expenseId: String, reason: SyncReason) {
        Log.d(TAG, "[$reason] syncExpenseDetail $expenseId")
        safe("getExpense($expenseId)") { expenseRepository.getExpense(expenseId) }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // C. Mutation-driven sync (called after create/update/delete/restore)
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Sync all scopes affected by an expense create.
     * Suspend — awaited by AddExpenseViewModel before emitting Success,
     * ensuring the user can go offline immediately without stale data.
     */
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
            skipExpenseDetail = true,    // expense is deleted on server
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

    // ─────────────────────────────────────────────────────────────────────────
    // Internal helpers
    // ─────────────────────────────────────────────────────────────────────────

    private suspend fun mutationRefresh(
        expenseId        : String,
        groupId          : String?,
        currentUserId    : String,
        participantIds   : Set<String>,
        skipExpenseDetail: Boolean = false,
    ) {
        // 1. Global net balance — both tab top bars.
        safe("getAllBalances") { balanceRepository.getAllBalances() }

        if (groupId != null) {
            // 2. Group tile balance.
            safe("getGroupBalances($groupId)") { groupRepository.getGroupBalances(groupId) }
            // 3. Group expense list.
            safe("getGroupExpenses($groupId)") { expenseRepository.getGroupExpenses(groupId) }
        }

        // 4. Full expense detail so ExpenseDetail opens offline.
        if (!skipExpenseDetail) {
            safe("getExpense($expenseId)") { expenseRepository.getExpense(expenseId) }
        }

        // 5. FRIEND_NET + FRIEND_BREAKDOWN (+ direct expense list for non-group) per participant.
        val friends = participantIds.filter { it != currentUserId }
        Log.d(TAG, "Refreshing ${friends.size} participant friend scopes (groupId=$groupId)")
        coroutineScope {
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

    private suspend fun syncGroupsCore(reason: SyncReason) {
        val result = safe("getMyGroups") { groupRepository.getMyGroups() }
        if (result !is ApiResult.Success) return
        val groups = result.data
        Log.d(TAG, "[$reason] Groups fetched: ${groups.size}")
        coroutineScope {
            for (group in groups) {
                launch {
                    safe("getMembers(${group.id})") { groupRepository.getMembers(group.id) }
                    safe("getGroupBalances(${group.id})") { groupRepository.getGroupBalances(group.id) }
                    val expResult = safe("getGroupExpenses(${group.id})") { expenseRepository.getGroupExpenses(group.id) }
                    if (expResult is ApiResult.Success) {
                        val recent = expResult.data.sortedByDescending { it.expenseDate }.take(EXPENSE_DETAIL_LIMIT)
                        prefetchExpenseDetails(recent)
                    }
                }
            }
        }
    }

    private suspend fun syncFriendsCore(reason: SyncReason) {
        val result = safe("getFriends") { friendRepository.getFriends() }
        if (result !is ApiResult.Success) return
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

    /**
     * Wraps a suspend call in runCatching, logs thrown exceptions.
     * Also logs when the result is a non-Success ApiResult so failed network
     * calls are visible in logs even though ApiResult doesn't throw.
     */
    @Suppress("UNCHECKED_CAST")
    private suspend fun <T> safe(label: String, block: suspend () -> T): T? {
        val result = runCatching { block() }
            .onFailure { Log.w(TAG, "$label threw: ${it.message}") }
            .getOrNull()
        // Log non-Success ApiResult so cache misses are visible.
        if (result is com.prathik.fairshare.domain.model.ApiResult<*> &&
            result !is com.prathik.fairshare.domain.model.ApiResult.Success<*>) {
            Log.w(TAG, "$label returned non-Success: $result")
        }
        return result
    }
}