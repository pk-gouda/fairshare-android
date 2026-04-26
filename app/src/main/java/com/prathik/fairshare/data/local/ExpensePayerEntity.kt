package com.prathik.fairshare.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Room entity for caching payer detail rows for a single expense.
 *
 * Populated when the user opens an expense detail while online
 * (via [ExpenseRepositoryImpl.getExpense] success path).
 *
 * When the device is offline and the parent [ExpenseEntity] exists in cache,
 * these rows allow [EditExpenseViewModel] to reconstruct a full [Expense] domain
 * object with non-empty payers — enabling full financial offline edit instead
 * of falling back to metadata-only mode.
 *
 * Primary key: "${expenseId}_${userId}" — unique per expense+user pair.
 */
@Entity(
    tableName = "expense_payers",
    indices   = [Index(value = ["expenseId"])],
)
data class ExpensePayerEntity(
    @PrimaryKey val id: String,         // "${expenseId}_${userId}"
    val expenseId  : String,
    val userId     : String,
    val fullName   : String,
    val amountPaid : Double,
)