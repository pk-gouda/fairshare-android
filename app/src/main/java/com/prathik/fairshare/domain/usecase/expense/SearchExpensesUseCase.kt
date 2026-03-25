package com.prathik.fairshare.domain.usecase.expense

import com.prathik.fairshare.domain.model.ApiResult
import com.prathik.fairshare.domain.model.Expense
import com.prathik.fairshare.domain.repository.ExpenseRepository
import javax.inject.Inject

/**
 * Searches expenses by description keyword across all groups.
 * Requires at least 2 characters to avoid overly broad searches.
 */
class SearchExpensesUseCase @Inject constructor(
    private val expenseRepository: ExpenseRepository,
) {
    suspend operator fun invoke(query: String): ApiResult<List<Expense>> {
        if (query.isBlank()) {
            return ApiResult.ValidationError("Search query cannot be empty")
        }
        if (query.trim().length < 2) {
            return ApiResult.ValidationError("Search query must be at least 2 characters")
        }
        return expenseRepository.searchExpenses(query.trim())
    }
}
