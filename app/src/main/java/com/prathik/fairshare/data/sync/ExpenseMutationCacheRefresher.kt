package com.prathik.fairshare.data.sync

import com.prathik.fairshare.domain.model.Expense
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Thin wrapper that delegates mutation cache refresh to [FairShareSyncManager].
 *
 * Retained so AddExpenseViewModel, EditExpenseViewModel, and SyncWorker call-sites
 * do not change in this patch. Can be removed once callers migrate directly to
 * FairShareSyncManager.syncAfterExpenseXxx().
 */
@Singleton
class ExpenseMutationCacheRefresher @Inject constructor(
    private val syncManager: FairShareSyncManager,
) {
    suspend fun refreshAfterCreateSuccess(
        expense      : Expense,
        groupId      : String?,
        currentUserId: String,
        payerIds     : Set<String> = emptySet(),
        splitIds     : Set<String> = emptySet(),
    ) = syncManager.syncAfterExpenseCreate(expense, groupId, currentUserId, payerIds, splitIds)

    suspend fun refreshAfterUpdateSuccess(
        expense           : Expense,
        groupId           : String?,
        currentUserId     : String,
        oldParticipantIds : Set<String> = emptySet(),
        newParticipantIds : Set<String> = emptySet(),
    ) = syncManager.syncAfterExpenseUpdate(expense, groupId, currentUserId, oldParticipantIds, newParticipantIds)

    suspend fun refreshAfterDeleteSuccess(
        expenseId     : String,
        groupId       : String?,
        currentUserId : String,
        participantIds: Set<String> = emptySet(),
    ) = syncManager.syncAfterExpenseDelete(expenseId, groupId, currentUserId, participantIds)

    suspend fun refreshAfterRestoreSuccess(
        expense       : Expense,
        groupId       : String?,
        currentUserId : String,
        participantIds: Set<String> = emptySet(),
    ) = syncManager.syncAfterExpenseRestore(expense, groupId, currentUserId, participantIds)

    /**
     * Fire-and-forget: delegates to [FairShareSyncManager.launchSyncAfterExpenseUpdate].
     * Does NOT block — returns immediately. Cache refresh survives ViewModel teardown.
     * Call this AFTER emitting [EditSaveState.Success] so navigation is instant.
     */
    fun launchAfterUpdateSuccess(
        expense           : Expense,
        groupId           : String?,
        currentUserId     : String,
        oldParticipantIds : Set<String> = emptySet(),
        newParticipantIds : Set<String> = emptySet(),
    ) = syncManager.launchSyncAfterExpenseUpdate(
        expense, groupId, currentUserId, oldParticipantIds, newParticipantIds)

    /**
     * Fire-and-forget: delegates to [FairShareSyncManager.launchSyncAfterExpenseDelete].
     * Does NOT block — returns immediately. Cache refresh survives ViewModel teardown.
     * Call this AFTER emitting [ExpenseActionState.Deleted] so navigation is instant.
     */
    fun launchAfterDeleteSuccess(
        expenseId     : String,
        groupId       : String?,
        currentUserId : String,
        participantIds: Set<String> = emptySet(),
    ) = syncManager.launchSyncAfterExpenseDelete(expenseId, groupId, currentUserId, participantIds)

    /**
     * Fire-and-forget: delegates to [FairShareSyncManager.launchSyncAfterExpenseRestore].
     * Does NOT block — returns immediately. Cache refresh survives ViewModel teardown.
     * Call this AFTER emitting [ExpenseActionState.Restored] so navigation is instant.
     */
    fun launchAfterRestoreSuccess(
        expense       : Expense,
        groupId       : String?,
        currentUserId : String,
        participantIds: Set<String> = emptySet(),
    ) = syncManager.launchSyncAfterExpenseRestore(expense, groupId, currentUserId, participantIds)
}