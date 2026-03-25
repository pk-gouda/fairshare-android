package com.prathik.fairshare.data.model.response

import kotlinx.serialization.Serializable

/**
 * API response DTO for a scanned receipt.
 * Maps to ReceiptResponse.java record on the backend.
 * Returned by POST /api/receipts/scan after AI extraction.
 *
 * scanConfidence indicates AI extraction quality: "HIGH", "MEDIUM", or "LOW".
 * expenseId is null until the receipt is attached to an expense.
 */
@Serializable
data class ReceiptResponse(
    val id: String,
    val expenseId: String? = null,
    val scannedById: String,
    val imageUrl: String? = null,
    val merchantName: String? = null,
    val merchantAddress: String? = null,
    val subtotal: Double? = null,
    val taxAmount: Double? = null,
    val tipAmount: Double? = null,
    val totalAmount: Double,
    val currency: String? = null,
    val paymentMethod: String? = null,
    val receiptDate: String? = null,
    val scanConfidence: String? = null,
    val itemCount: Int? = null,
    val createdAt: String,
)
