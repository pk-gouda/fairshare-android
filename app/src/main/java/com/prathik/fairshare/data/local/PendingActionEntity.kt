package com.prathik.fairshare.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room entity for the offline action queue.
 * When the device is offline, actions are queued here and
 * replayed in FIFO order when connectivity is restored.
 *
 * OfflineSyncManager (Phase 7) drains this table on reconnect.
 *
 * type values: CREATE_EXPENSE, UPDATE_EXPENSE, DELETE_EXPENSE,
 *              SETTLE, ADD_COMMENT, MARK_NOTIFICATION_READ
 */
@Entity(tableName = "pending_actions")
data class PendingActionEntity(
    @PrimaryKey val id: String,
    val type: String,
    val payloadJson: String,
    val groupId: String?,
    val expenseId: String?,
    val createdAt: Long = System.currentTimeMillis(),
    val retryCount: Int = 0,
    val lastAttemptAt: Long? = null,
)
