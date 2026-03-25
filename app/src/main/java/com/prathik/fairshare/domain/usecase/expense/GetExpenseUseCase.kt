package com.prathik.fairshare.domain.usecase.expense

import com.prathik.fairshare.domain.model.ApiResult
import com.prathik.fairshare.domain.model.Expense
import com.prathik.fairshare.domain.repository.ExpenseRepository
import javax.inject.Inject

/**
 * Fetches a single expense by ID.
 */
class GetExpenseUseCase @Inject constructor(
    private val expenseRepository: ExpenseRepository,
) {
    suspend operator fun invoke(expenseId: String): ApiResult<Expense> {
        if (expenseId.isBlank()) {
            return ApiResult.ValidationError("Expense ID cannot be empty")
        }
        return expenseRepository.getExpense(expenseId)
    }
}
