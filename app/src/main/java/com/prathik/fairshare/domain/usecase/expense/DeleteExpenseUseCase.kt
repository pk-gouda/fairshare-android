package com.prathik.fairshare.domain.usecase.expense

import com.prathik.fairshare.domain.model.ApiResult
import com.prathik.fairshare.domain.repository.ExpenseRepository
import javax.inject.Inject

/**
 * Soft deletes an expense. Can be restored later.
 * Only the user who added the expense can delete it.
 */
class DeleteExpenseUseCase @Inject constructor(
    private val expenseRepository: ExpenseRepository,
) {
    suspend operator fun invoke(expenseId: String): ApiResult<Unit> {
        if (expenseId.isBlank()) {
            return ApiResult.ValidationError("Expense ID cannot be empty")
        }
        return expenseRepository.deleteExpense(expenseId)
    }
}
