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
import com.prathik.fairshare.data.model.request.CreateExpenseRequest
import com.prathik.fairshare.data.model.request.UpdateExpenseRequest
import com.prathik.fairshare.domain.repository.ExpenseRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.serialization.json.Json
import java.util.concurrent.TimeUnit

/**
 * WorkManager worker that drains the pending operation queue.
 *
 * Runs only when network is available. On each pass it loads all PENDING
 * and FAILED_RETRYABLE operations (respecting dependency order) and attempts
 * to sync each one to the backend.
 *
 * Wave 2D-4: CREATE_EXPENSE operations are fully wired. The worker
 * deserialises the stored [CreateExpenseRequest] JSON and calls the expense
 * repository using the stable [PendingOperationEntity.idempotencyKey] so the
 * backend deduplicates any duplicate submissions automatically.
 *
 * UPDATE_EXPENSE / DELETE_EXPENSE / RESTORE_EXPENSE operations are enqueued
 * and marked SYNCED inline by their ViewModels while online; the stubs here
 * handle the case where the ViewModel failed and the queue contains a
 * FAILED_RETRYABLE row from a previous session. Full retry logic for those
 * types comes in Wave 2D-2 follow-up.
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
    private val expenseRepository: ExpenseRepository,
    private val json: Json,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val pending   = pendingOperationRepository.getPendingOperations()
        val retryable = pendingOperationRepository.getRetryableOperations()
        val allWork   = pending + retryable

        if (allWork.isEmpty()) return Result.success()

        var anyRetryableFailure = false

        for (operation in allWork) {
            val succeeded = syncOperation(
                operationId      = operation.operationId,
                operationType    = operation.operationType,
                idempotencyKey   = operation.idempotencyKey,
                requestBodyJson  = operation.requestBodyJson,
                endpoint         = operation.endpoint,
            )
            if (!succeeded) anyRetryableFailure = true
        }

        // Clean up old SYNCED / CANCELLED rows on each pass (7-day TTL).
        pendingOperationRepository.cleanupCompleted()

        return if (anyRetryableFailure) Result.retry() else Result.success()
    }

    /**
     * Attempt to sync one pending operation to the backend.
     *
     * String primitives only — avoids KSP ordering issues where the
     * Hilt/AssistedInject processor runs before Room resolves entity types.
     *
     * @return true  → operation succeeded or permanently failed (no worker retry needed).
     *         false → retryable failure occurred; worker should schedule a retry.
     */
    private suspend fun syncOperation(
        operationId    : String,
        operationType  : String,
        idempotencyKey : String,
        requestBodyJson: String?,
        endpoint       : String,
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
                return true // permanent — don't block the worker
            }

            when (type) {
                OperationType.CREATE_EXPENSE -> syncCreateExpense(
                    operationId    = operationId,
                    idempotencyKey = idempotencyKey,
                    requestBodyJson = requestBodyJson,
                )

                OperationType.UPDATE_EXPENSE -> syncUpdateExpense(
                    operationId     = operationId,
                    idempotencyKey  = idempotencyKey,
                    requestBodyJson = requestBodyJson,
                    endpoint        = endpoint,
                )

                OperationType.DELETE_EXPENSE,
                OperationType.RESTORE_EXPENSE -> {
                    // Online-only until Wave 2D-3. Mark permanent so the queue
                    // does not loop — user must retry manually.
                    pendingOperationRepository.markFailed(
                        operationId,
                        "Offline retry for $operationType not yet supported. Please try again when online."
                    )
                    true
                }

                OperationType.CREATE_SETTLEMENT,
                OperationType.UPDATE_SETTLEMENT,
                OperationType.CANCEL_SETTLEMENT,
                OperationType.RESTORE_SETTLEMENT -> {
                    pendingOperationRepository.markFailed(
                        operationId, "Wave 2E not yet implemented for $operationType"
                    )
                    true
                }

                OperationType.ASSIGN_EXPENSE_ITEMS -> {
                    pendingOperationRepository.markFailed(
                        operationId, "Wave 2D not yet implemented for ASSIGN_EXPENSE_ITEMS"
                    )
                    true
                }
            }
        } catch (e: Exception) {
            val error = e.message ?: "Unknown error"
            pendingOperationRepository.markRetryable(operationId, error)
            false
        }
    }

    /**
     * Wave 2D-4: replay a CREATE_EXPENSE pending operation.
     *
     * Deserialises the stored [CreateExpenseRequest] JSON and calls the
     * repository with the original stable [idempotencyKey]. The backend's
     * idempotency_keys table guarantees exactly-once financial mutation even
     * if this runs multiple times.
     */
    private suspend fun syncCreateExpense(
        operationId    : String,
        idempotencyKey : String,
        requestBodyJson: String?,
    ): Boolean {
        if (requestBodyJson == null) {
            pendingOperationRepository.markFailed(
                operationId, "CREATE_EXPENSE pending operation has no requestBodyJson"
            )
            return true
        }

        val request = try {
            json.decodeFromString<CreateExpenseRequest>(requestBodyJson)
        } catch (e: Exception) {
            pendingOperationRepository.markFailed(
                operationId, "Failed to deserialise CREATE_EXPENSE body: ${e.message}"
            )
            return true
        }

        val result = expenseRepository.createExpense(
            groupId          = request.groupId,
            description      = request.description,
            totalAmount      = request.totalAmount,
            currency         = request.currency,
            splitType        = request.splitType,
            category         = request.category,
            notes            = request.notes,
            expenseDate      = request.expenseDate,
            payerData        = request.payerData,
            splitData        = request.splitData,
            receiptId        = request.receiptId,
            idempotencyKey   = idempotencyKey,   // stable key — never regenerated
            remainderPointer = request.remainderPointer,
            itemAssignments  = request.itemAssignments,
            repeatInterval   = request.repeatInterval,
        )

        return when (result) {
            is com.prathik.fairshare.domain.model.ApiResult.Success -> {
                pendingOperationRepository.markSynced(operationId, result.data.id)
                true
            }

            is com.prathik.fairshare.domain.model.ApiResult.NetworkError -> {
                pendingOperationRepository.markRetryable(
                    operationId, result.exception.message ?: "Network error"
                )
                false // signal worker to schedule a retry
            }

            // 409 Conflict — backend already processed this idempotency key with a
            // different body. Treat as permanent; user must recreate from scratch.
            is com.prathik.fairshare.domain.model.ApiResult.Conflict -> {
                pendingOperationRepository.markFailed(operationId, result.message)
                true
            }

            // 401 / 403 — permanent; user must re-authenticate or fix permissions.
            is com.prathik.fairshare.domain.model.ApiResult.Unauthorized,
            is com.prathik.fairshare.domain.model.ApiResult.Forbidden -> {
                pendingOperationRepository.markFailed(
                    operationId,
                    (result as? com.prathik.fairshare.domain.model.ApiResult.Forbidden)?.message
                        ?: "Unauthorized"
                )
                true
            }

            // 400 validation — permanent; stored body is malformed.
            is com.prathik.fairshare.domain.model.ApiResult.ValidationError -> {
                pendingOperationRepository.markFailed(operationId, result.message)
                true
            }

            // 5xx and other transient errors — keep retrying.
            else -> {
                pendingOperationRepository.markRetryable(operationId, "Server error, will retry")
                false
            }
        }
    }

    /**
     * Wave 2D-2: replay an UPDATE_EXPENSE pending operation.
     *
     * The expenseId is stored in [endpoint] as /api/expenses/{expenseId} and also
     * in [PendingOperationEntity.localResourceId]. We read it from the endpoint to
     * keep the function self-contained without needing the full entity.
     *
     * The same stable [idempotencyKey] is sent on every attempt so the backend's
     * idempotency_keys table prevents a double balance reversal/re-application.
     */
    private suspend fun syncUpdateExpense(
        operationId    : String,
        idempotencyKey : String,
        requestBodyJson: String?,
        endpoint       : String,
    ): Boolean {
        if (requestBodyJson == null) {
            pendingOperationRepository.markFailed(
                operationId, "UPDATE_EXPENSE pending operation has no requestBodyJson"
            )
            return true
        }

        // Endpoint format: /api/expenses/{expenseId}
        val expenseId = endpoint.removePrefix("/api/expenses/").trim()
        if (expenseId.isBlank()) {
            pendingOperationRepository.markFailed(
                operationId, "Could not parse expenseId from endpoint: $endpoint"
            )
            return true
        }

        val request = try {
            json.decodeFromString<UpdateExpenseRequest>(requestBodyJson)
        } catch (e: Exception) {
            pendingOperationRepository.markFailed(
                operationId, "Failed to deserialise UPDATE_EXPENSE body: ${e.message}"
            )
            return true
        }

        val result = expenseRepository.updateExpense(
            expenseId      = expenseId,
            description    = request.description,
            totalAmount    = request.totalAmount,
            currency       = request.currency,
            splitType      = request.splitType,
            category       = request.category,
            notes          = request.notes,
            expenseDate    = request.expenseDate,
            payerData      = request.payerData,
            splitData      = request.splitData,
            repeatInterval = request.repeatInterval,
            clearRepeat    = request.clearRepeat,
            idempotencyKey = idempotencyKey,   // stable key — never regenerated
        )

        return when (result) {
            is com.prathik.fairshare.domain.model.ApiResult.Success -> {
                pendingOperationRepository.markSynced(operationId, expenseId)
                true
            }

            is com.prathik.fairshare.domain.model.ApiResult.NetworkError -> {
                pendingOperationRepository.markRetryable(
                    operationId, result.exception.message ?: "Network error"
                )
                false // signal worker to retry
            }

            // 404 — expense was deleted while we were offline. Permanent.
            is com.prathik.fairshare.domain.model.ApiResult.NotFound -> {
                pendingOperationRepository.markFailed(operationId, "Expense no longer exists")
                true
            }

            // 403/401 — permission changed or session expired. Permanent.
            is com.prathik.fairshare.domain.model.ApiResult.Forbidden,
            is com.prathik.fairshare.domain.model.ApiResult.Unauthorized -> {
                pendingOperationRepository.markFailed(
                    operationId,
                    (result as? com.prathik.fairshare.domain.model.ApiResult.Forbidden)?.message
                        ?: "Unauthorized"
                )
                true
            }

            // 400 validation / 409 conflict — stored body is rejected. Permanent.
            is com.prathik.fairshare.domain.model.ApiResult.ValidationError -> {
                pendingOperationRepository.markFailed(operationId, result.message)
                true
            }

            is com.prathik.fairshare.domain.model.ApiResult.Conflict -> {
                pendingOperationRepository.markFailed(operationId, result.message)
                true
            }

            // 5xx and other transient errors — keep retrying.
            else -> {
                pendingOperationRepository.markRetryable(operationId, "Server error, will retry")
                false
            }
        }
    }

    companion object {
        private const val PERIODIC_WORK_NAME   = "fairshare_sync_periodic"
        private const val ONETIME_WORK_NAME    = "fairshare_sync_onetime"
        private const val BACKOFF_DELAY_MILLIS = 30_000L // 30 seconds

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