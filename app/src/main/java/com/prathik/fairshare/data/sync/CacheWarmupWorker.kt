package com.prathik.fairshare.data.sync

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.prathik.fairshare.data.local.EncryptedTokenStore
import com.prathik.fairshare.domain.model.ApiResult
import com.prathik.fairshare.domain.model.NotificationType
import com.prathik.fairshare.domain.repository.BalanceRepository
import com.prathik.fairshare.domain.repository.ExpenseRepository
import com.prathik.fairshare.domain.repository.FriendRepository
import com.prathik.fairshare.domain.repository.GroupRepository
import com.prathik.fairshare.domain.repository.NotificationRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

/**
 * Wave 2D-Cache Warmup: proactively caches groups, friends, expenses, balances,
 * and Activity notifications while the device is online.
 *
 * All repository methods called here already write to Room on success, so this
 * worker simply needs to trigger them. Each method silently handles its own
 * failure — a fetch error in one group does not abort the rest of the warmup.
 *
 * Limits:
 * - Caches the 10 most recent expenses per group/friend for full detail.
 * - Does not download receipt images, OCR payloads, or import CSVs.
 * - Does not mutate confirmed balances or create pending operations.
 *
 * Scheduling:
 * - Runs once every 6 hours when network is available.
 * - Uses ExistingPeriodicWorkPolicy.KEEP so it does not reset if already queued.
 */
@HiltWorker
class CacheWarmupWorker @AssistedInject constructor(
    @Assisted private val appContext: Context,
    @Assisted params: WorkerParameters,
    private val tokenStore        : EncryptedTokenStore,
    private val groupRepository   : GroupRepository,
    private val friendRepository  : FriendRepository,
    private val expenseRepository : ExpenseRepository,
    private val balanceRepository : BalanceRepository,
    private val notificationRepository: NotificationRepository,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        // Only run if the user is logged in.
        if (!tokenStore.isLoggedIn()) return Result.success()

        try {
            coroutineScope {
                // 1. Activity / notifications — cache all rows; triggers expense prefetch.
                launch { warmupNotifications() }

                // 2. Groups — cache list, then per-group data.
                launch { warmupGroups() }

                // 3. Friends — cache list, then per-friend data.
                launch { warmupFriends() }

                // 4. Overall balances.
                launch { runCatching { balanceRepository.getAllBalances() } }
            }
        } catch (_: Exception) {
            // Warmup failures are non-fatal — return success so WorkManager
            // does not back off or retry aggressively.
        }

        return Result.success()
    }

    private suspend fun warmupNotifications() {
        val result = runCatching { notificationRepository.getAll() }.getOrNull() ?: return
        if (result !is ApiResult.Success) return

        // Prefetch full expense detail for expense-type notification rows.
        // This is the critical path for Activity → ExpenseDetail offline restore.
        val expenseTypes = setOf(
            NotificationType.EXPENSE_ADDED.name,
            NotificationType.EXPENSE_UPDATED.name,
            NotificationType.EXPENSE_DELETED.name,
            NotificationType.EXPENSE_RESTORED.name,
            NotificationType.EXPENSE_AUTO_CREATED.name,
        )
        result.data
            .filter { it.type.name in expenseTypes && it.referenceId != null }
            .take(20) // reasonable cap — newest 20 expense notifications
            .forEach { notification ->
                val expenseId = notification.referenceId ?: return@forEach
                runCatching { expenseRepository.getExpense(expenseId) }
                // Failure is silent — cached if possible, skipped if not.
            }
    }

    private suspend fun warmupGroups() {
        val groupsResult = runCatching { groupRepository.getMyGroups() }.getOrNull() ?: return
        if (groupsResult !is ApiResult.Success) return

        coroutineScope {
            for (group in groupsResult.data) {
                launch {
                    runCatching { groupRepository.getMembers(group.id) }
                    runCatching { groupRepository.getGroupBalances(group.id) }

                    val expensesResult = runCatching {
                        expenseRepository.getGroupExpenses(group.id)
                    }.getOrNull()

                    // Prefetch full detail for the 10 most recent group expenses.
                    if (expensesResult is ApiResult.Success) {
                        expensesResult.data
                            .sortedByDescending { it.expenseDate }
                            .take(10)
                            .forEach { expense ->
                                runCatching { expenseRepository.getExpense(expense.id) }
                            }
                    }
                }
            }
        }
    }

    private suspend fun warmupFriends() {
        val friendsResult = runCatching { friendRepository.getFriends() }.getOrNull() ?: return
        if (friendsResult !is ApiResult.Success) return

        coroutineScope {
            for (friend in friendsResult.data) {
                launch {
                    val friendId = friend.id
                    runCatching { balanceRepository.getNetBalanceWithUser(friendId) }
                    runCatching { balanceRepository.getBreakdownWithUser(friendId) }

                    val expensesResult = runCatching {
                        expenseRepository.getDirectExpensesWithFriend(friendId)
                    }.getOrNull()

                    // Prefetch full detail for the 10 most recent direct expenses.
                    if (expensesResult is ApiResult.Success) {
                        expensesResult.data
                            .sortedByDescending { it.expenseDate }
                            .take(10)
                            .forEach { expense ->
                                runCatching { expenseRepository.getExpense(expense.id) }
                            }
                    }
                }
            }
        }
    }

    companion object {

        private const val WORK_NAME_PERIODIC = "fairshare_cache_warmup_periodic"
        private const val WORK_NAME_ONCE     = "fairshare_cache_warmup_once"

        /**
         * Schedule periodic warmup — call once from [FairShareApplication].
         * Uses KEEP policy so existing scheduled work is not reset.
         */
        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<CacheWarmupWorker>(6, TimeUnit.HOURS)
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build()
                )
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.MINUTES)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME_PERIODIC,
                ExistingPeriodicWorkPolicy.KEEP,
                request,
            )
        }

        /**
         * Trigger a one-off warmup immediately — call after login or on app start
         * when the user is already logged in. Uses KEEP policy so simultaneous
         * calls (login + app start) do not enqueue duplicate workers.
         */
        fun triggerNow(context: Context) {
            val request = androidx.work.OneTimeWorkRequestBuilder<CacheWarmupWorker>()
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build()
                )
                .build()

            WorkManager.getInstance(context).enqueueUniqueWork(
                WORK_NAME_ONCE,
                androidx.work.ExistingWorkPolicy.KEEP,
                request,
            )
        }
    }
}