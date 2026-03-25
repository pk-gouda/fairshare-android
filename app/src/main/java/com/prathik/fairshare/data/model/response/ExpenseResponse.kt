package com.prathik.fairshare.data.model.response

import com.prathik.fairshare.domain.model.ExpenseCategory
import com.prathik.fairshare.domain.model.SplitType
import kotlinx.serialization.Serializable

/**
 * API response DTO for an expense.
 * Maps to ExpenseResponse.java record on the backend.
 *
 * The yourPaid / yourShare / yourBalance fields are computed server-side
 * for the currently authenticated user:
 * - yourBalance = yourPaid - yourShare
 * - Positive yourBalance = you lent money (show in green)
 * - Negative yourBalance = you owe money (show in orange)
 */
@Serializable
data class ExpenseResponse(
    val id: String,
    val description: String,
    val totalAmount: Double,
    val currency: String,
    val groupId: String? = null,
    val groupName: String? = null,
    val addedById: String,
    val addedByName: String,
    val updatedById: String? = null,
    val updatedByName: String? = null,
    val splitType: SplitType,
    val category: ExpenseCategory? = null,
    val receipt: ReceiptSummary? = null,
    val notes: String? = null,
    val expenseDate: String,
    val isDeleted: Boolean,
    val payers: List<PayerDetail>? = null,
    val splits: List<SplitDetail>? = null,
    val commentCount: Int,
    val itemCount: Int,
    val yourPaid: Double,
    val yourShare: Double,
    val yourBalance: Double,
    val createdAt: String,
    val updatedAt: String,
) {
    /**
     * Who paid for the expense and how much they paid.
     */
    @Serializable
    data class PayerDetail(
        val userId: String,
        val fullName: String,
        val amountPaid: Double,
    )

    /**
     * How much each participant owes for this expense.
     * percentage and shares are only populated for PERCENTAGE and SHARES split types.
     */
    @Serializable
    data class SplitDetail(
        val userId: String,
        val fullName: String,
        val amountOwed: Double,
        val percentage: Double? = null,
        val shares: Int? = null,
        val isSettled: Boolean,
    )

    /**
     * Compact receipt summary embedded in the expense response.
     * Full receipt details fetched separately via ReceiptResponse.
     */
    @Serializable
    data class ReceiptSummary(
        val receiptId: String,
        val imageUrl: String? = null,
        val merchantName: String? = null,
        val totalAmount: Double,
        val scanConfidence: String? = null,
        val itemCount: Int? = null,
        val receiptDate: String? = null,
    )
}
