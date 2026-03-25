package com.prathik.fairshare.domain.repository

import com.prathik.fairshare.domain.model.Expense
import com.prathik.fairshare.domain.model.ExpenseCategory
import com.prathik.fairshare.domain.model.SplitType

/**
 * Contract for all expense-related operations.
 * Implementation lives in data/repository/impl/ExpenseRepositoryImpl.kt
 */
interface ExpenseRepository {

    /**
     * Fetches all expenses for a group.
     */
    suspend fun getGroupExpenses(groupId: String): Result<List<Expense>>

    /**
     * Fetches a single expense by ID.
     */
    suspend fun getExpense(expenseId: String): Result<Expense>

    /**
     * Creates a new expense.
     * payerData — map of userId to amount paid. Null = current user paid full amount.
     * splitData — map of userId to amount owed. Null = split equally among all members.
     */
    suspend fun createExpense(
        groupId: String,
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
    ): Result<Expense>

    /**
     * Updates an existing expense.
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
    ): Result<Expense>

    /**
     * Soft deletes an expense. Can be restored.
     */
    suspend fun deleteExpense(expenseId: String): Result<Unit>

    /**
     * Restores a previously soft-deleted expense.
     */
    suspend fun restoreExpense(expenseId: String): Result<Expense>

    /**
     * Searches expenses by description keyword.
     */
    suspend fun searchExpenses(query: String): Result<List<Expense>>

    /**
     * Fetches all recurring expenses for a group.
     */
    suspend fun getRecurringExpenses(groupId: String): Result<List<Expense>>

    /**
     * Stops a recurring expense from auto-generating.
     */
    suspend fun stopRecurring(expenseId: String): Result<Unit>
}