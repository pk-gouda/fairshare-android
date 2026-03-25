package com.prathik.fairshare.domain.usecase.expense

import com.prathik.fairshare.domain.model.Expense
import com.prathik.fairshare.domain.repository.ExpenseRepository
import javax.inject.Inject

/**
 * Fetches all expenses for a group, ordered by date descending.
 * Returns an empty list if the group has no expenses.
 */
class GetGroupExpensesUseCase @Inject constructor(
    private val expenseRepository: ExpenseRepository,
) {
    suspend operator fun invoke(groupId: String): Result<List<Expense>> {
        if (groupId.isBlank()) {
            return Result.failure(IllegalArgumentException("Group ID cannot be empty"))
        }
        return expenseRepository.getGroupExpenses(groupId)
    }
}
