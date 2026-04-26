package com.prathik.fairshare.data.sync

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.util.concurrent.TimeUnit

/**
 * WorkManager worker that drains the pending operation queue.
 *
 * Runs only when network is available. On each pass it loads all PENDING
 * and FAILED_RETRYABLE operations (respecting dependency order) and attempts
 * to sync each one to the backend.
 *
 * Wave 2C: Skeleton only.
 * - Queue loading and status transitions are implemented.
 * - Actual API calls are stubbed with TODO comments per OperationType.
 * - Wave 2D will fill in the real API call implementations.
 *
 * Scheduling:
 * - Periodic: runs every 15 minutes when network is available.
 * - One-shot: triggered immediately when a new operation is enqueued.
 *
 * Idempotency guarantee:
 * - Each operation carries a stable [PendingOperationEntity.idempotencyKey].
 * - The backend's idempotency_keys table deduplicates on this value.
 * - Re-running the worker after a crash cannot create duplicate financial records.
 */
@HiltWorker
class SyncWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    private val pendingOperationRepository: PendingOperationRepository,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val pending   = pendingOperationRepository.getPendingOperations()
        val retryable = pendingOperationRepository.getRetryableOperations()
        val allWork   = pending + retryable

        if (allWork.isEmpty()) return Result.success()

        var anyFailure = false

        for (operation in allWork) {
            val result = syncOperation(
                operationId    = operation.operationId,
                operationType  = operation.operationType,
                idempotencyKey = operation.idempotencyKey,
            )
            if (!result) anyFailure = true
        }

        return if (anyFailure) Result.retry() else Result.success()
    }

    /**
     * Attempt to sync one pending operation to the backend.
     *
     * Takes only String primitives — NOT PendingOperationEntity — to avoid a KSP
     * ordering issue where the Hilt/AssistedInject processor runs before Room has
     * resolved the entity type, causing error.NonExistentClass compile errors.
     *
     * @return true if the operation succeeded or was permanently failed (no retry needed);
     *         false if a retryable error occurred and the worker should retry.
     */
    private suspend fun syncOperation(
        operationId: String,
        operationType: String,
        idempotencyKey: String,
    ): Boolean {
        pendingOperationRepository.markSyncing(operationId)
        pendingOperationRepository.incrementRetry(operationId)

        return try {
            val type = try {
                OperationType.valueOf(operationType)
            } catch (e: IllegalArgumentException) {
                pendingOperationRepository.markFailed(
                    operationId, "Unknown operation type: $operationType"
                )
                return true
            }

            when (type) {
                OperationType.CREATE_EXPENSE -> {
                    // TODO Wave 2D: deserialize requestBodyJson and call
                    // expenseService.createExpense(idempotencyKey = idempotencyKey, ...)
                    pendingOperationRepository.markFailed(operationId, "Wave 2D not yet implemented for CREATE_EXPENSE")
                    true
                }
                OperationType.UPDATE_EXPENSE -> {
                    pendingOperationRepository.markFailed(operationId, "Wave 2D not yet implemented for UPDATE_EXPENSE")
                    true
                }
                OperationType.DELETE_EXPENSE -> {
                    pendingOperationRepository.markFailed(operationId, "Wave 2D not yet implemented for DELETE_EXPENSE")
                    true
                }
                OperationType.RESTORE_EXPENSE -> {
                    pendingOperationRepository.markFailed(operationId, "Wave 2D not yet implemented for RESTORE_EXPENSE")
                    true
                }
                OperationType.CREATE_SETTLEMENT -> {
                    pendingOperationRepository.markFailed(operationId, "Wave 2E not yet implemented for CREATE_SETTLEMENT")
                    true
                }
                OperationType.UPDATE_SETTLEMENT -> {
                    pendingOperationRepository.markFailed(operationId, "Wave 2E not yet implemented for UPDATE_SETTLEMENT")
                    true
                }
                OperationType.CANCEL_SETTLEMENT -> {
                    pendingOperationRepository.markFailed(operationId, "Wave 2E not yet implemented for CANCEL_SETTLEMENT")
                    true
                }
                OperationType.RESTORE_SETTLEMENT -> {
                    pendingOperationRepository.markFailed(operationId, "Wave 2E not yet implemented for RESTORE_SETTLEMENT")
                    true
                }
                OperationType.ASSIGN_EXPENSE_ITEMS -> {
                    pendingOperationRepository.markFailed(operationId, "Wave 2D not yet implemented for ASSIGN_EXPENSE_ITEMS")
                    true
                }
            }
        } catch (e: Exception) {
            val error = e.message ?: "Unknown error"
            pendingOperationRepository.markRetryable(operationId, error)
            false
        }
    }

    companion object {
        private const val PERIODIC_WORK_NAME   = "fairshare_sync_periodic"
        private const val ONETIME_WORK_NAME    = "fairshare_sync_onetime"
        private const val BACKOFF_DELAY_MILLIS = 30_000L // 30 seconds

        /** Network-required constraint shared by both schedule methods. */
        private val networkConstraint = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        /**
         * Schedule periodic background sync (every 15 minutes, network required).
         * Safe to call multiple times — uses [ExistingPeriodicWorkPolicy.KEEP].
         */
        fun schedulePeriodicSync(context: Context) {
            val request = PeriodicWorkRequestBuilder<SyncWorker>(15, TimeUnit.MINUTES)
                .setConstraints(networkConstraint)
                .setBackoffCriteria(
                    BackoffPolicy.EXPONENTIAL,
                    BACKOFF_DELAY_MILLIS,
                    TimeUnit.MILLISECONDS,
                )
                .build()

            WorkManager.getInstance(context)
                .enqueueUniquePeriodicWork(
                    PERIODIC_WORK_NAME,
                    ExistingPeriodicWorkPolicy.KEEP,
                    request,
                )
        }

        /**
         * Trigger an immediate one-shot sync (network required).
         * Call this after enqueueing a new pending operation to minimise latency.
         */
        fun triggerImmediateSync(context: Context) {
            val request = OneTimeWorkRequestBuilder<SyncWorker>()
                .setConstraints(networkConstraint)
                .setBackoffCriteria(
                    BackoffPolicy.EXPONENTIAL,
                    BACKOFF_DELAY_MILLIS,
                    TimeUnit.MILLISECONDS,
                )
                .build()

            WorkManager.getInstance(context)
                .enqueueUniqueWork(
                    ONETIME_WORK_NAME,
                    androidx.work.ExistingWorkPolicy.KEEP,
                    request,
                )
        }
    }
}