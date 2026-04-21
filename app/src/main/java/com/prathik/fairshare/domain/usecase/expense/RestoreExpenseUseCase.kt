package com.prathik.fairshare.domain.usecase.expense

import com.prathik.fairshare.domain.model.ApiResult
import com.prathik.fairshare.domain.model.Expense
import com.prathik.fairshare.domain.repository.ExpenseRepository
import javax.inject.Inject

class RestoreExpenseUseCase @Inject constructor(
    private val expenseRepository: ExpenseRepository,
) {
    suspend operator fun invoke(expenseId: String): ApiResult<Expense> =
        expenseRepository.restoreExpense(expenseId)
}