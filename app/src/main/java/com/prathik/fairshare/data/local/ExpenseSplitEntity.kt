package com.prathik.fairshare.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Room entity for caching split detail rows for a single expense.
 *
 * Populated when the user opens an expense detail while online.
 * Mirrors [Expense.SplitDetail] — stores amountOwed, percentage, shares, and
 * isSettled so the edit form can reconstruct exact split values offline.
 *
 * Primary key: "${expenseId}_${userId}" — unique per expense+user pair.
 */
@Entity(
    tableName = "expense_splits",
    indices   = [Index(value = ["expenseId"])],
)
data class ExpenseSplitEntity(
    @PrimaryKey val id: String,         // "${expenseId}_${userId}"
    val expenseId  : String,
    val userId     : String,
    val fullName   : String,
    val amountOwed : Double,
    val percentage : Double?,
    val shares     : Int?,
    val isSettled  : Boolean,
)