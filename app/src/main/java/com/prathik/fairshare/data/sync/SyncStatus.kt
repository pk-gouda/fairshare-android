package com.prathik.fairshare.data.sync

/**
 * Lifecycle status of a pending write operation in the local queue.
 *
 * PENDING           → Stored locally, not yet attempted.
 * SYNCING           → Worker is currently sending the request.
 * SYNCED            → Backend confirmed success. Row can be cleaned up.
 * FAILED_RETRYABLE  → Network timeout, 5xx, or other transient error. Worker will retry.
 * FAILED_PERMANENT  → Queued replay got a permanent 4xx/business error. Requires user action.
 * CANCELLED         → Superseded or cancelled by a later operation (e.g. create+delete = noop).
 */
enum class SyncStatus {
    PENDING,
    SYNCING,
    SYNCED,
    FAILED_RETRYABLE,
    FAILED_PERMANENT,
    CANCELLED,
}