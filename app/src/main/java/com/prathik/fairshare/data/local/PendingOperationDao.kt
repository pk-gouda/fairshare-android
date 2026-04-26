package com.prathik.fairshare.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface PendingOperationDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertOperation(operation: PendingOperationEntity)

    @Update
    suspend fun updateOperation(operation: PendingOperationEntity)

    @Query("SELECT * FROM pending_operations WHERE operationId = :operationId")
    suspend fun getById(operationId: String): PendingOperationEntity?

    /**
     * All PENDING operations ready for their first attempt, ordered FIFO.
     * Used by SyncWorker on every sync pass.
     */
    @Query("""
        SELECT * FROM pending_operations
        WHERE status = 'PENDING'
          AND (dependsOnOperationId IS NULL
               OR dependsOnOperationId IN (
                   SELECT operationId FROM pending_operations WHERE status = 'SYNCED'
               ))
        ORDER BY createdAt ASC
    """)
    suspend fun getPendingOperations(): List<PendingOperationEntity>

    /**
     * FAILED_RETRYABLE operations eligible for retry, ordered by oldest update first.
     * SyncWorker checks these on each pass to retry transient failures.
     */
    @Query("""
        SELECT * FROM pending_operations
        WHERE status = 'FAILED_RETRYABLE'
          AND (dependsOnOperationId IS NULL
               OR dependsOnOperationId IN (
                   SELECT operationId FROM pending_operations WHERE status = 'SYNCED'
               ))
        ORDER BY updatedAt ASC
    """)
    suspend fun getRetryableOperations(): List<PendingOperationEntity>

    /**
     * All operations with a specific status, ordered FIFO.
     * Used for monitoring/debugging and by the sync queue UI.
     */
    @Query("SELECT * FROM pending_operations WHERE status = :status ORDER BY createdAt ASC")
    suspend fun getOperationsByStatus(status: String): List<PendingOperationEntity>

    /**
     * Live count of PENDING + FAILED_RETRYABLE operations.
     * Exposed as Flow so the UI can show a sync badge reactively.
     */
    @Query("""
        SELECT COUNT(*) FROM pending_operations
        WHERE status IN ('PENDING', 'FAILED_RETRYABLE', 'SYNCING')
    """)
    fun observePendingCount(): Flow<Int>

    /**
     * Transition an operation to a new status and update timestamp.
     */
    @Query("""
        UPDATE pending_operations
        SET status = :newStatus, updatedAt = :updatedAt, lastError = :lastError
        WHERE operationId = :operationId
    """)
    suspend fun markStatus(
        operationId: String,
        newStatus: String,
        updatedAt: Long = System.currentTimeMillis(),
        lastError: String? = null,
    )

    /**
     * Increment retry count and record last attempt timestamp.
     * Called before each retry attempt.
     */
    @Query("""
        UPDATE pending_operations
        SET retryCount = retryCount + 1,
            lastAttemptAt = :now,
            updatedAt = :now
        WHERE operationId = :operationId
    """)
    suspend fun incrementRetry(operationId: String, now: Long = System.currentTimeMillis())

    /**
     * Store the server-assigned resource ID after a successful sync.
     * Dependent operations read this value to build their requests.
     */
    @Query("""
        UPDATE pending_operations
        SET serverResourceId = :serverResourceId,
            status = 'SYNCED',
            updatedAt = :now
        WHERE operationId = :operationId
    """)
    suspend fun markSynced(
        operationId: String,
        serverResourceId: String,
        now: Long = System.currentTimeMillis(),
    )

    /**
     * Hard-delete a completed/cancelled operation row.
     * Only called during periodic cleanup of old SYNCED/CANCELLED rows.
     */
    @Query("DELETE FROM pending_operations WHERE operationId = :operationId")
    suspend fun deleteOperation(operationId: String)

    /**
     * Clean up old SYNCED and CANCELLED rows older than [cutoffMillis].
     * Called periodically to keep the table small.
     */
    @Query("""
        DELETE FROM pending_operations
        WHERE status IN ('SYNCED', 'CANCELLED')
          AND updatedAt < :cutoffMillis
    """)
    suspend fun cleanupCompleted(cutoffMillis: Long)
}