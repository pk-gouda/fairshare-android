package com.prathik.fairshare.data.model.request

import com.prathik.fairshare.data.model.enums.ExpenseCategory
import com.prathik.fairshare.data.model.enums.SplitType
import kotlinx.serialization.Serializable

/**
 * Request body for POST /api/expenses
 *
 * payerData  — map of userId → amountPaid. null = current user paid full amount.
 * splitData  — map of userId → amountOwed. null = split equally among all members.
 * splitType  — null defaults to EQUAL on the backend.
 * idempotencyKey — optional client-generated UUID to prevent duplicate submissions.
 * receiptId  — optional, attach a previously scanned receipt to this expense.
 */
@Serializable
data class CreateExpenseRequest(
    val groupId: String,
    val description: String,
    val totalAmount: Double,
    val currency: String,
    val splitType: SplitType? = null,
    val category: ExpenseCategory? = null,
    val notes: String? = null,
    val expenseDate: String? = null,
    val payerData: Map<String, Double>? = null,
    val splitData: Map<String, Double>? = null,
    val idempotencyKey: String? = null,
    val receiptId: String? = null,
)

/**
 * Request body for PUT /api/expenses/{expenseId}
 * All fields are optional — only non-null fields are updated.
 */
@Serializable
data class UpdateExpenseRequest(
    val description: String? = null,
    val totalAmount: Double? = null,
    val currency: String? = null,
    val splitType: SplitType? = null,
    val category: ExpenseCategory? = null,
    val notes: String? = null,
    val expenseDate: String? = null,
    val payerData: Map<String, Double>? = null,
    val splitData: Map<String, Double>? = null,
    val idempotencyKey: String? = null,
)

/**
 * Request body for POST /api/expenses/{expenseId}/comments
 */
@Serializable
data class AddCommentRequest(
    val comment: String,
)

/**
 * Request body for PUT /api/expenses/{expenseId}/items/assign
 * assignments maps itemId → list of userIds to assign that item to.
 */
@Serializable
data class ItemAssignmentRequest(
    val assignments: Map<String, List<String>>,
)
