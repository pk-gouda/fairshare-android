package com.prathik.fairshare.data.sync

import com.prathik.fairshare.data.local.PendingOperationDao
import com.prathik.fairshare.data.local.PendingOperationEntity
import kotlinx.coroutines.flow.Flow
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for the pending operation queue.
 *
 * All writes to [PendingOperationDao] go through here so the rest of the
 * app deals with [PendingOperationEntity] without touching DAO directly.
 *
 * Wave 2C: Foundation only. Enqueue methods exist but are not yet called by
 * create/update/delete/restore screens — that wiring happens in Wave 2D.
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
     * Enqueue a new operation. The idempotencyKey is generated once here
     * and stored with the operation — it must never be regenerated on retry.
     *
     * @return The operationId (primary key) of the inserted row.
     */
    suspend fun enqueue(
        userId: String,
        operationType: OperationType,
        endpoint: String,
        method: String,
        requestBodyJson: String?,
        localResourceId: String? = null,
        dependsOnOperationId: String? = null,
    ): String {
        val operationId   = UUID.randomUUID().toString()
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
            dependsOnOperationId = dependsOnOperationId,
        )
        dao.insertOperation(entity)
        return operationId
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