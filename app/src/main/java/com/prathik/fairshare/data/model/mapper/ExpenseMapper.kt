package com.prathik.fairshare.data.model.mapper

import com.prathik.fairshare.data.model.response.ExpenseResponse
import com.prathik.fairshare.domain.model.Expense

/**
 * Maps ExpenseResponse DTO to Expense domain model.
 *
 * payers and splits default to empty lists if null —
 * the backend omits these on list endpoints for performance,
 * they are only populated on single expense fetch.
 *
 * yourBalance sign convention (preserved from backend):
 * Positive = you lent money (green in UI)
 * Negative = you owe money (orange in UI)
 */
fun ExpenseResponse.toDomain(): Expense = Expense(
    id           = id,
    description  = description,
    totalAmount  = totalAmount,
    currency     = currency,
    groupId      = groupId,
    groupName    = groupName,
    addedById    = addedById,
    addedByName  = addedByName,
    splitType    = splitType,
    category     = category,
    notes        = notes,
    expenseDate  = expenseDate,
    isDeleted    = isDeleted,
    payers       = payers?.map { it.toDomain() } ?: emptyList(),
    splits       = splits?.map { it.toDomain() } ?: emptyList(),
    commentCount = commentCount,
    itemCount    = itemCount,
    yourPaid     = yourPaid,
    yourShare    = yourShare,
    yourBalance  = yourBalance,
    createdAt    = createdAt,
    updatedAt    = updatedAt,
)

/**
 * Maps PayerDetail DTO to domain PayerDetail.
 */
fun ExpenseResponse.PayerDetail.toDomain(): Expense.PayerDetail = Expense.PayerDetail(
    userId     = userId,
    fullName   = fullName,
    amountPaid = amountPaid,
)

/**
 * Maps SplitDetail DTO to domain SplitDetail.
 */
fun ExpenseResponse.SplitDetail.toDomain(): Expense.SplitDetail = Expense.SplitDetail(
    userId     = userId,
    fullName   = fullName,
    amountOwed = amountOwed,
    percentage = percentage,
    shares     = shares,
    isSettled  = isSettled,
)
