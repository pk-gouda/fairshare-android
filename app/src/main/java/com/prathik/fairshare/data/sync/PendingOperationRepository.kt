package com.prathik.fairshare.data.sync

import com.prathik.fairshare.data.local.PendingOperationDao
import com.prathik.fairshare.data.local.PendingOperationEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Result returned by [PendingOperationRepository.enqueue].
 * Both values are generated once at enqueue time and never change.
 *
 * @param operationId   Primary key of the pending_operations row.
 * @param idempotencyKey Stable key to pass to the backend on every attempt.
 */
data class EnqueueResult(
    val operationId: String,
    val idempotencyKey: String,
)

/**
 * Repository for the pending operation queue.
 *
 * All writes to [PendingOperationDao] go through here so the rest of the
 * app deals with [PendingOperationEntity] without touching DAO directly.
 */
@Singleton
class PendingOperationRepository @Inject constructor(
    private val dao: PendingOperationDao,
) {

    // ── Read ──────────────────────────────────────────────────────────────────

    suspend fun getById(operationId: String): PendingOperationEntity? =
        dao.getById(operationId)

    /** Returns PENDING operations whose dependencies (if any) are SYNCED. */
    suspend fun getPendingOperations(): List<PendingOperationEntity> =
        dao.getPendingOperations()

    /** Returns FAILED_RETRYABLE operations eligible for the next retry pass. */
    suspend fun getRetryableOperations(): List<PendingOperationEntity> =
        dao.getRetryableOperations()

    suspend fun getOperationsByStatus(status: SyncStatus): List<PendingOperationEntity> =
        dao.getOperationsByStatus(status.name)

    /** Live count for sync badge in the UI. */
    fun observePendingCount(): Flow<Int> = dao.observePendingCount()

    // ── Write ─────────────────────────────────────────────────────────────────

    /**
     * Enqueue a new operation. Both [EnqueueResult.operationId] and
     * [EnqueueResult.idempotencyKey] are generated once here and stored with
     * the operation — they must never be regenerated on retry.
     *
     * The caller should use [EnqueueResult.idempotencyKey] for the backend
     * request so the backend can deduplicate on retry.
     *
     * @return [EnqueueResult] containing the stable operationId and idempotencyKey.
     */
    suspend fun enqueue(
        userId: String,
        operationType: OperationType,
        endpoint: String,
        method: String,
        requestBodyJson: String?,
        localResourceId: String? = null,
        serverResourceId: String? = null,
        dependsOnOperationId: String? = null,
    ): EnqueueResult {
        val operationId    = UUID.randomUUID().toString()
        val idempotencyKey = UUID.randomUUID().toString()
        val now            = System.currentTimeMillis()

        val entity = PendingOperationEntity(
            operationId          = operationId,
            userId               = userId,
            operationType        = operationType.name,
            endpoint             = endpoint,
            method               = method,
            requestBodyJson      = requestBodyJson,
            idempotencyKey       = idempotencyKey,
            status               = SyncStatus.PENDING.name,
            retryCount           = 0,
            createdAt            = now,
            updatedAt            = now,
            localResourceId      = localResourceId,
            serverResourceId     = serverResourceId,
            dependsOnOperationId = dependsOnOperationId,
        )
        dao.insertOperation(entity)
        return EnqueueResult(operationId = operationId, idempotencyKey = idempotencyKey)
    }

    /** Mark an operation as currently in-flight. */
    suspend fun markSyncing(operationId: String) =
        dao.markStatus(operationId, SyncStatus.SYNCING.name)

    /** Mark an operation as successfully synced and store the server ID. */
    suspend fun markSynced(operationId: String, serverResourceId: String) =
        dao.markSynced(operationId, serverResourceId)

    /** Mark as retryable (network/5xx failures). */
    suspend fun markRetryable(operationId: String, error: String) =
        dao.markStatus(operationId, SyncStatus.FAILED_RETRYABLE.name, lastError = error)

    /** Mark as permanently failed (4xx errors — user action needed). */
    suspend fun markFailed(operationId: String, error: String) =
        dao.markStatus(operationId, SyncStatus.FAILED_PERMANENT.name, lastError = error)

    /** Cancel a pending operation (e.g. a CREATE that was locally deleted before sync). */
    suspend fun markCancelled(operationId: String) =
        dao.markStatus(operationId, SyncStatus.CANCELLED.name)

    /** Record a retry attempt (increments counter, updates lastAttemptAt). */
    suspend fun incrementRetry(operationId: String) =
        dao.incrementRetry(operationId)

    // ── Pending-op visibility (Wave 2D-4) ────────────────────────────────────

    /**
     * Live-watches the active pending operation for a specific expense.
     * Null when the expense has no pending or failed operation (already SYNCED/CANCELLED).
     * Consumed by ExpenseDetailViewModel to drive the sync-status banner.
     */
    fun observeForExpense(expenseId: String): Flow<PendingOperationEntity?> =
        dao.observeForResource(expenseId)

    /**
     * Live set of expense IDs that have at least one active pending operation.
     * Consumed by GroupDetailViewModel so the expense list can show a sync dot
     * on affected rows without one query per row.
     */
    fun observeActivePendingResourceIds(): Flow<Set<String>> =
        dao.observeActiveResourceIds().map { it.toSet() }

    /**
     * Reset a FAILED_RETRYABLE or FAILED_PERMANENT operation back to PENDING
     * so SyncWorker will attempt it again.
     *
     * The [operationId] and its stored [PendingOperationEntity.idempotencyKey]
     * are UNCHANGED — retries always reuse the original key.
     */
    suspend fun resetForRetry(operationId: String) =
        dao.markStatus(operationId, SyncStatus.PENDING.name, lastError = null)

    // ── Cleanup ───────────────────────────────────────────────────────────────

    /**
     * Remove SYNCED and CANCELLED rows older than [olderThanMillis].
     * Call periodically to keep the queue small.
     */
    suspend fun cleanupCompleted(olderThanMillis: Long = 7 * 24 * 60 * 60 * 1000L) {
        val cutoff = System.currentTimeMillis() - olderThanMillis
        dao.cleanupCompleted(cutoff)
    }
}