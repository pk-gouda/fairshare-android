package com.prathik.fairshare.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Option A — durable balance impact store for UPDATE_EXPENSE offline optimism.
 *
 * When an expense is edited offline, the balance overlay must reflect:
 *   effectiveBalance = confirmedCachedBalance + Σ(pending deltas)
 *
 * For CREATE and DELETE, the delta comes directly from the cached expense's
 * yourBalance. For UPDATE, the delta = newYourBalance − oldYourBalance, but
 * the old value is overwritten in Room when we apply the local optimistic edit.
 *
 * This table persists (oldYourBalance, newYourBalance) keyed by operationId so
 * the overlay can compute the correct UPDATE delta across navigation and restarts.
 *
 * Lifecycle:
 * - Row inserted when UPDATE_EXPENSE op is queued offline (after local Room edit).
 * - Row deleted when op reaches SYNCED or CANCELLED/FAILED_PERMANENT.
 * - Overlay observers query this table alongside pending_operations.
 */
@Entity(tableName = "pending_balance_impact")
data class PendingBalanceImpactEntity(
    @PrimaryKey val operationId : String,   // FK → pending_operations.operationId
    val expenseId               : String,
    val groupId                 : String?,
    val otherUserId             : String?,  // non-null for direct friend expenses
    val currency                : String,
    val oldYourBalance          : Double,   // snapshot BEFORE the local edit
    val newYourBalance          : Double,   // snapshot AFTER the local edit
    val createdAt               : Long = System.currentTimeMillis(),
    /** JSON-serialised Set<String> of participant IDs BEFORE the edit. */
    /** Comma-separated original participant IDs captured before offline edit.
     * Used by SyncWorker to refresh removed participants' caches after sync. */
    val oldParticipantIds       : String = "",
) {
    /** The net change this UPDATE brings to the user's balance in this context. */
    val delta: Double get() = newYourBalance - oldYourBalance

    /** Deserialize [oldParticipantIds] back to a Set. */
    fun getOldParticipants(): Set<String> =
        if (oldParticipantIds.isBlank()) emptySet()
        else oldParticipantIds.split(',').filter { it.isNotBlank() }.toSet()
}