package com.prathik.fairshare.domain.usecase.expense

import com.prathik.fairshare.domain.model.Expense
import com.prathik.fairshare.domain.repository.ExpenseRepository
import javax.inject.Inject

/**
 * Fetches a single expense by ID.
 * Returns failure if the expense doesn't exist or user is not a member of its group.
 */
class GetExpenseUseCase @Inject constructor(
    private val expenseRepository: ExpenseRepository,
) {
    suspend operator fun invoke(expenseId: String): Result<Expense> {
        if (expenseId.isBlank()) {
            return Result.failure(IllegalArgumentException("Expense ID cannot be empty"))
        }
        return expenseRepository.getExpense(expenseId)
    }
}
