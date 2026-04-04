package com.prathik.fairshare.domain.repository

import com.prathik.fairshare.domain.model.ApiResult
import com.prathik.fairshare.domain.model.Expense
import com.prathik.fairshare.domain.model.ExpenseCategory
import com.prathik.fairshare.domain.model.SplitType

/**
 * Contract for all expense-related operations.
 * Implementation lives in data/repository/impl/ExpenseRepositoryImpl.kt
 */
interface ExpenseRepository {

    /**
     * Fetches all expenses for a group, ordered by date descending.
     * Returns empty list if the group has no expenses yet.
     */
    suspend fun getGroupExpenses(groupId: String): ApiResult<List<Expense>>

    /**
     * Fetches a single expense by ID.
     * Returns [ApiResult.NotFound] if expense doesn't exist.
     * Returns [ApiResult.Forbidden] if user is not a member of the group.
     */
    suspend fun getExpense(expenseId: String): ApiResult<Expense>

    /**
     * Creates a new expense in a group.
     * payerData — null means current user paid the full amount.
     * splitData — null means split equally among all group members.
     * splitType — null defaults to EQUAL on the backend.
     */
    suspend fun createExpense(
        groupId: String?,
        description: String,
        totalAmount: Double,
        currency: String,
        splitType: SplitType?,
        category: ExpenseCategory?,
        notes: String?,
        expenseDate: String?,
        payerData: Map<String, Double>?,
        splitData: Map<String, Double>?,
        receiptId: String?,
    ): ApiResult<Expense>

    /**
     * Updates an existing expense.
     * Only non-null fields are updated.
     * Returns [ApiResult.Forbidden] if user is not the one who added the expense.
     */
    suspend fun updateExpense(
        expenseId: String,
        description: String?,
        totalAmount: Double?,
        currency: String?,
        splitType: SplitType?,
        category: ExpenseCategory?,
        notes: String?,
        expenseDate: String?,
        payerData: Map<String, Double>?,
        splitData: Map<String, Double>?,
    ): ApiResult<Expense>

    /**
     * Soft deletes an expense — can be restored later.
     * Returns [ApiResult.Forbidden] if user didn't add this expense.
     */
    suspend fun deleteExpense(expenseId: String): ApiResult<Unit>

    /**
     * Restores a previously soft-deleted expense.
     */
    suspend fun restoreExpense(expenseId: String): ApiResult<Expense>

    /**
     * Searches expenses by description keyword across all groups.
     */
    suspend fun searchExpenses(query: String): ApiResult<List<Expense>>

    /**
     * Fetches all recurring expenses for a group.
     */
    suspend fun getRecurringExpenses(groupId: String): ApiResult<List<Expense>>

    /**
     * Stops a recurring expense from auto-generating future entries.
     */
    suspend fun stopRecurring(expenseId: String): ApiResult<Unit>
}