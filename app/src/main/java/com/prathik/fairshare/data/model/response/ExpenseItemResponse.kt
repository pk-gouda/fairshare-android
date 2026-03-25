package com.prathik.fairshare.data.model.response

import kotlinx.serialization.Serializable

/**
 * API response DTO for an individual line item within a receipt.
 * Maps to ExpenseItemResponse.java record on the backend.
 * Used in the Item Assignment screen after receipt scanning.
 *
 * assignedTo is null until items are assigned to group members.
 * taxCode, taxRate, taxAmount, totalWithTax are null for items without tax breakdown.
 */
@Serializable
data class ExpenseItemResponse(
    val id: String,
    val name: String,
    val price: Double,
    val quantity: Int? = null,
    val totalPrice: Double,
    val taxCode: String? = null,
    val taxRate: Double? = null,
    val taxAmount: Double? = null,
    val totalWithTax: Double? = null,
    val assignedTo: List<AssignedUser>? = null,
) {
    /**
     * A user assigned to this receipt line item.
     */
    @Serializable
    data class AssignedUser(
        val userId: String,
        val fullName: String,
    )
}
