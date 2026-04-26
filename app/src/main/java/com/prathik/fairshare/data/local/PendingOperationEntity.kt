package com.prathik.fairshare.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Durable local queue entry for a pending write operation.
 *
 * Every financial write action (create/update/delete/restore expense,
 * create/cancel/restore settlement, item assignment) should be stored here
 * before the network call. This enables:
 *
 *   1. Idempotency across retries — same [idempotencyKey] is reused on retry,
 *      preventing duplicate expenses/settlements even if the network drops
 *      between request and response.
 *
 *   2. Offline survival — operations queued while offline are retried when
 *      network returns, even if the app was restarted.
 *
 *   3. Dependency ordering — [dependsOnOperationId] lets a DELETE wait until
 *      its CREATE has synced and [serverResourceId] is known.
 *
 * This is separate from the older [PendingActionEntity] table which has a
 * different schema and is used by unrelated legacy code.
 *
 * Wave 2C: table created, DAO wired. Operations are not yet queued here
 * by default — that happens in Wave 2D when offline flows are implemented.
 */
@Entity(
    tableName = "pending_operations",
    indices = [
        Index(value = ["status"]),
        Index(value = ["userId"]),
        Index(value = ["idempotencyKey"], unique = true),
    ]
)
data class PendingOperationEntity(

    /** Client-generated UUID, stable across retries. */
    @PrimaryKey
    val operationId: String,

    /** ID of the authenticated user who initiated the action. */
    val userId: String,

    /**
     * Operation type, e.g. "CREATE_EXPENSE".
     * Stored as String (not enum) so the DB survives OperationType additions
     * without a migration.
     */
    val operationType: String,

    /** Backend endpoint path, e.g. "/api/expenses". */
    val endpoint: String,

    /** HTTP method: "POST", "PUT", "DELETE". */
    val method: String,

    /**
     * JSON-serialized request body for POST/PUT operations.
     * Null for DELETE (no body needed — endpoint + idempotencyKey header suffice).
     */
    val requestBodyJson: String?,

    /**
     * Stable idempotency key sent to the backend.
     * MUST be reused on every retry — never regenerated.
     * The backend's idempotency_keys table deduplicates on this value.
     */
    val idempotencyKey: String,

    /**
     * Current sync status. See [SyncStatus] for lifecycle.
     * Stored as String for DB compatibility.
     */
    val status: String,

    /** Number of times this operation has been attempted. */
    val retryCount: Int = 0,

    /** Epoch millis when this operation was created locally. */
    val createdAt: Long = System.currentTimeMillis(),

    /** Epoch millis when this operation was last updated. */
    val updatedAt: Long = System.currentTimeMillis(),

    /** Epoch millis of the most recent sync attempt. Null if never attempted. */
    val lastAttemptAt: Long? = null,

    /** Last error message from a failed attempt, for display and debugging. */
    val lastError: String? = null,

    /**
     * If non-null, this operation must wait until the referenced operation
     * has status=SYNCED before being attempted. Used to chain operations
     * where one depends on the server resource ID from another.
     *
     * Example: a DELETE_EXPENSE must wait until the CREATE_EXPENSE that
     * created it (offline) has synced and received a serverResourceId.
     */
    val dependsOnOperationId: String? = null,

    /**
     * Client-assigned local UUID for the resource (used before server ID is known).
     * Allows the UI to display the resource optimistically before sync.
     */
    val localResourceId: String? = null,

    /**
     * Server-assigned UUID returned after a successful sync.
     * Populated after status transitions to SYNCED.
     * Later operations that depend on this one read serverResourceId here.
     */
    val serverResourceId: String? = null,
)