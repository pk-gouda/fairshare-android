package com.prathik.fairshare.data.model.mapper

import com.prathik.fairshare.data.model.response.ExpenseResponse
import com.prathik.fairshare.domain.model.Expense
import com.prathik.fairshare.domain.model.ExpenseCategory
import com.prathik.fairshare.domain.model.SplitType

/**
 * Maps ExpenseResponse DTO to Expense domain model.
 *
 * splitType and category are stored as String in the DTO to prevent
 * deserialization crashes when the backend adds new enum values.
 * Safe conversion with fallback: unknown splitType → EQUAL, unknown category → OTHER.
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
    splitType    = splitType.toSplitTypeSafe(),
    category     = category?.toExpenseCategorySafe(),
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
    receipt        = receipt?.toDomain(),
    repeatInterval = repeatInterval,
    nextRepeatDate = nextRepeatDate,
    isRecurring    = isRecurring,
    isTemplate     = isTemplate,
)

fun ExpenseResponse.ReceiptSummary.toDomain(): Expense.ReceiptSummary = Expense.ReceiptSummary(
    receiptId      = receiptId,
    imageUrl       = imageUrl,
    merchantName   = merchantName,
    totalAmount    = totalAmount,
    scanConfidence = scanConfidence,
    itemCount      = itemCount,
    receiptDate    = receiptDate,
)

fun ExpenseResponse.PayerDetail.toDomain(): Expense.PayerDetail = Expense.PayerDetail(
    userId     = userId,
    fullName   = fullName,
    amountPaid = amountPaid,
)

fun ExpenseResponse.SplitDetail.toDomain(): Expense.SplitDetail = Expense.SplitDetail(
    userId     = userId,
    fullName   = fullName,
    amountOwed = amountOwed,
    percentage = percentage,
    shares     = shares,
    isSettled  = isSettled,
)

private fun String.toSplitTypeSafe(): SplitType =
    try { SplitType.valueOf(this) } catch (e: IllegalArgumentException) { SplitType.EQUAL }

private fun String.toExpenseCategorySafe(): ExpenseCategory =
    try { ExpenseCategory.valueOf(this) } catch (e: IllegalArgumentException) { ExpenseCategory.OTHER }