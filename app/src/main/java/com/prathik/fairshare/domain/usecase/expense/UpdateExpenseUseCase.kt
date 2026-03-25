package com.prathik.fairshare.domain.usecase.expense

import com.prathik.fairshare.domain.model.Expense
import com.prathik.fairshare.domain.model.ExpenseCategory
import com.prathik.fairshare.domain.model.SplitType
import com.prathik.fairshare.domain.repository.ExpenseRepository
import javax.inject.Inject

/**
 * Updates an existing expense.
 * Only passes non-null values — null means no change to that field.
 * Only the user who added the expense can update it.
 */
class UpdateExpenseUseCase @Inject constructor(
    private val expenseRepository: ExpenseRepository,
) {
    suspend operator fun invoke(
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
    ): Result<Expense> {
        if (expenseId.isBlank()) {
            return Result.failure(IllegalArgumentException("Expense ID cannot be empty"))
        }
        if (description != null && description.isBlank()) {
            return Result.failure(IllegalArgumentException("Description cannot be empty"))
        }
        if (totalAmount != null && totalAmount <= 0) {
            return Result.failure(IllegalArgumentException("Amount must be greater than 0"))
        }
        return expenseRepository.updateExpense(
            expenseId   = expenseId,
            description = description?.trim(),
            totalAmount = totalAmount,
            currency    = currency,
            splitType   = splitType,
            category    = category,
            notes       = notes?.trim(),
            expenseDate = expenseDate,
            payerData   = payerData,
            splitData   = splitData,
        )
    }
}
