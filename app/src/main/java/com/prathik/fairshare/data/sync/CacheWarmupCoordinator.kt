package com.prathik.fairshare.data.sync

import android.util.Log
import com.prathik.fairshare.data.local.EncryptedTokenStore
import com.prathik.fairshare.domain.model.ApiResult
import com.prathik.fairshare.domain.model.NotificationType
import com.prathik.fairshare.domain.repository.BalanceRepository
import com.prathik.fairshare.domain.repository.ExpenseRepository
import com.prathik.fairshare.domain.repository.FriendRepository
import com.prathik.fairshare.domain.repository.GroupRepository
import com.prathik.fairshare.domain.repository.NotificationRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Wave 2D-Cache Warmup: in-process foreground cache coordinator.
 *
 * Unlike [CacheWarmupWorker] (WorkManager), this runs immediately in the
 * app process without scheduling delays. WorkManager remains for periodic
 * background refresh; this coordinator handles the "user just opened the app"
 * case where we need data cached *now*, not in a few minutes.
 *
 * Thread-safety:
 * - [_isRunning] prevents parallel warmup runs.
 * - [_lastRunAt] throttles re-runs to once every [MIN_INTERVAL_MS] (5 min).
 * - Both are checked atomically before starting.
 *
 * Failure policy:
 * - [SupervisorJob] ensures one failed child coroutine does not cancel others.
 * - Individual fetch errors are caught and logged; warmup continues.
 * - No snackbar or UI error is shown.
 */
@Singleton
class CacheWarmupCoordinator @Inject constructor(
    private val tokenStore          : EncryptedTokenStore,
    private val groupRepository     : GroupRepository,
    private val friendRepository    : FriendRepository,
    private val expenseRepository   : ExpenseRepository,
    private val balanceRepository   : BalanceRepository,
    private val notificationRepository: NotificationRepository,
) {
    private val scope      = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val _isRunning = AtomicBoolean(false)
    private var _lastRunAt = 0L

    companion object {
        private const val TAG            = "CacheWarmup"
        private const val MIN_INTERVAL_MS = 5 * 60 * 1_000L  // 5 minutes
    }

    /**
     * Start warmup if:
     * 1. The user is logged in.
     * 2. A warmup is not already in progress.
     * 3. The last warmup ran more than [MIN_INTERVAL_MS] ago.
     *
     * Safe to call from Application.onCreate(), Activity.onStart(), or
     * ViewModel.init — it will no-op if the conditions are not met.
     */
    fun warmupIfNeeded() {
        if (!tokenStore.isLoggedIn()) return
        val now = System.currentTimeMillis()
        if (now - _lastRunAt < MIN_INTERVAL_MS) return
        if (!_isRunning.compareAndSet(false, true)) return

        _lastRunAt = now
        scope.launch {
            try {
                Log.d(TAG, "Warmup started")
                runWarmup()
                Log.d(TAG, "Warmup completed")
            } finally {
                _isRunning.set(false)
            }
        }
    }

    private suspend fun runWarmup() = coroutineScope {
        // Run all top-level warmup passes concurrently.
        launch { warmupNotifications() }
        launch { warmupGroups() }
        launch { warmupFriends() }
        launch { runCatching { balanceRepository.getAllBalances() } }
    }

    // ── Notifications / Activity ──────────────────────────────────────────────

    private suspend fun warmupNotifications() {
        val result = runCatching { notificationRepository.getAll() }.getOrNull() ?: return
        if (result !is ApiResult.Success) return

        val notifications = result.data
        Log.d(TAG, "Notifications cached: ${notifications.size}")

        // Prefetch full expense detail for expense-type activity rows so the
        // Activity → ExpenseDetail → Restore offline path works immediately.
        val expenseTypes = setOf(
            NotificationType.EXPENSE_ADDED.name,
            NotificationType.EXPENSE_UPDATED.name,
            NotificationType.EXPENSE_DELETED.name,
            NotificationType.EXPENSE_RESTORED.name,
            NotificationType.EXPENSE_AUTO_CREATED.name,
        )
        val expenseRefs = notifications
            .filter { it.type.name in expenseTypes && it.referenceId != null }
            .take(20)

        var prefetchCount = 0
        coroutineScope {
            for (notification in expenseRefs) {
                launch {
                    val id = notification.referenceId ?: return@launch
                    val ok = runCatching { expenseRepository.getExpense(id) }.isSuccess
                    if (ok) prefetchCount++
                }
            }
        }
        Log.d(TAG, "Expense details prefetched from Activity: $prefetchCount")
    }

    // ── Groups ────────────────────────────────────────────────────────────────

    private suspend fun warmupGroups() {
        val groupsResult = runCatching { groupRepository.getMyGroups() }.getOrNull() ?: return
        if (groupsResult !is ApiResult.Success) return

        val groups = groupsResult.data
        Log.d(TAG, "Groups cached: ${groups.size}")

        var detailCount = 0
        coroutineScope {
            for (group in groups) {
                launch {
                    runCatching { groupRepository.getMembers(group.id) }
                    runCatching { groupRepository.getGroupBalances(group.id) }

                    val expResult = runCatching {
                        expenseRepository.getGroupExpenses(group.id)
                    }.getOrNull()

                    if (expResult is ApiResult.Success) {
                        val recent = expResult.data
                            .sortedByDescending { it.expenseDate }
                            .take(10)
                        coroutineScope {
                            for (exp in recent) {
                                launch {
                                    val ok = runCatching {
                                        expenseRepository.getExpense(exp.id)
                                    }.isSuccess
                                    if (ok) detailCount++
                                }
                            }
                        }
                    }
                }
            }
        }
        Log.d(TAG, "Group expense details prefetched: $detailCount")
    }

    // ── Friends ───────────────────────────────────────────────────────────────

    private suspend fun warmupFriends() {
        val friendsResult = runCatching { friendRepository.getFriends() }.getOrNull() ?: return
        if (friendsResult !is ApiResult.Success) return

        val friends = friendsResult.data
        Log.d(TAG, "Friends cached: ${friends.size}")

        var detailCount = 0
        coroutineScope {
            for (friend in friends) {
                launch {
                    val friendId = friend.id
                    runCatching { balanceRepository.getNetBalanceWithUser(friendId) }
                    runCatching { balanceRepository.getBreakdownWithUser(friendId) }

                    val expResult = runCatching {
                        expenseRepository.getDirectExpensesWithFriend(friendId)
                    }.getOrNull()
                    val directCount = if (expResult is com.prathik.fairshare.domain.model.ApiResult.Success)
                        expResult.data.size else 0
                    Log.d(TAG, "Direct expenses fetched for friendId=$friendId count=$directCount")

                    if (expResult is ApiResult.Success) {
                        val recent = expResult.data
                            .sortedByDescending { it.expenseDate }
                            .take(10)
                        coroutineScope {
                            for (exp in recent) {
                                launch {
                                    val ok = runCatching {
                                        expenseRepository.getExpense(exp.id)
                                    }.isSuccess
                                    if (ok) detailCount++
                                }
                            }
                        }
                    }
                }
            }
        }
        Log.d(TAG, "Friend expense details prefetched: $detailCount")
    }
}