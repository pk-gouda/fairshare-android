package com.prathik.fairshare.domain.usecase.expense

import com.prathik.fairshare.domain.model.ApiResult
import com.prathik.fairshare.domain.model.Expense
import com.prathik.fairshare.domain.model.ExpenseCategory
import com.prathik.fairshare.domain.model.SplitType
import com.prathik.fairshare.domain.repository.ExpenseRepository
import javax.inject.Inject

/**
 * Updates an existing expense.
 * Only non-null fields are updated.
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
        repeatInterval: String? = null,
        clearRepeat: Boolean? = null,
        idempotencyKey: String? = null,
    ): ApiResult<Expense> {
        if (expenseId.isBlank()) {
            return ApiResult.ValidationError("Expense ID cannot be empty")
        }
        if (description != null && description.isBlank()) {
            return ApiResult.ValidationError("Description cannot be empty")
        }
        if (totalAmount != null && totalAmount <= 0) {
            return ApiResult.ValidationError("Amount must be greater than 0")
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
            splitData      = splitData,
            repeatInterval = repeatInterval,
            clearRepeat    = clearRepeat,
            idempotencyKey = idempotencyKey,
        )
    }
}