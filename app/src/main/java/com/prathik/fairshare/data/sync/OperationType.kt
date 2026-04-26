package com.prathik.fairshare.data.sync

/**
 * Identifies the type of pending write operation stored in the local queue.
 * Used by SyncWorker to route each operation to the correct API call.
 */
enum class OperationType {
    // Expense operations
    CREATE_EXPENSE,
    UPDATE_EXPENSE,
    DELETE_EXPENSE,
    RESTORE_EXPENSE,

    // Settlement operations
    CREATE_SETTLEMENT,
    UPDATE_SETTLEMENT,
    CANCEL_SETTLEMENT,
    RESTORE_SETTLEMENT,

    // Item assignment
    ASSIGN_EXPENSE_ITEMS,
}