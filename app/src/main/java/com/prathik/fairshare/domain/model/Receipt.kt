package com.prathik.fairshare.domain.model

data class Receipt(
    val id: String,
    val expenseId: String?,
    val scannedById: String,
    val imageUrl: String?,
    val merchantName: String?,
    val merchantAddress: String?,
    val subtotal: Double?,
    val taxAmount: Double?,
    val tipAmount: Double?,
    val totalAmount: Double,
    val currency: String?,
    val paymentMethod: String?,
    val receiptDate: String?,
    val scanConfidence: String?,
    val itemCount: Int?,
    val createdAt: String,
)