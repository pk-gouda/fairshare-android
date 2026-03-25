package com.prathik.fairshare.domain.usecase.expense

import com.prathik.fairshare.domain.repository.ExpenseRepository
import javax.inject.Inject

/**
 * Soft deletes an expense.
 * The expense can be restored later.
 * Only the user who added the expense can delete it.
 */
class DeleteExpenseUseCase @Inject constructor(
    private val expenseRepository: ExpenseRepository,
) {
    suspend operator fun invoke(expenseId: String): Result<Unit> {
        if (expenseId.isBlank()) {
            return Result.failure(IllegalArgumentException("Expense ID cannot be empty"))
        }
        return expenseRepository.deleteExpense(expenseId)
    }
}
