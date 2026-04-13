package com.prathik.fairshare.domain.usecase.expense

import com.prathik.fairshare.domain.model.ApiResult
import com.prathik.fairshare.domain.model.Expense
import com.prathik.fairshare.domain.model.ExpenseCategory
import com.prathik.fairshare.domain.model.SplitType
import com.prathik.fairshare.domain.repository.ExpenseRepository
import javax.inject.Inject

/**
 * Creates a new expense in a group.
 * Validates description and amount before hitting the network.
 *
 * payerData — null means current user paid the full amount.
 * splitData — null means split equally among all group members.
 */
class CreateExpenseUseCase @Inject constructor(
    private val expenseRepository: ExpenseRepository,
) {
    suspend operator fun invoke(
        groupId: String?,
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
        remainderPointer: Int? = null,
        itemAssignments: Map<String, List<String>>? = null,
    ): ApiResult<Expense> {
        if (description.isBlank()) {
            return ApiResult.ValidationError("Description cannot be empty")
        }
        if (description.trim().length < 2) {
            return ApiResult.ValidationError("Description must be at least 2 characters")
        }
        if (totalAmount <= 0) {
            return ApiResult.ValidationError("Amount must be greater than 0")
        }
        if (currency.isBlank()) {
            return ApiResult.ValidationError("Currency cannot be empty")
        }
        return expenseRepository.createExpense(
            groupId     = groupId,
            description = description.trim(),
            totalAmount = totalAmount,
            currency    = currency,
            splitType   = splitType,
            category    = category,
            notes       = notes?.trim(),
            expenseDate = expenseDate,
            payerData   = payerData,
            splitData        = splitData,
            receiptId        = receiptId,
            remainderPointer = remainderPointer,
            itemAssignments  = itemAssignments,
        )
    }
}