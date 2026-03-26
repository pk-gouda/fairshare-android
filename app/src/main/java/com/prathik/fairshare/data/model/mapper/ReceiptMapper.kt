package com.prathik.fairshare.data.model.mapper

import com.prathik.fairshare.data.model.response.ReceiptResponse
import com.prathik.fairshare.domain.model.Receipt

/**
 * Maps ReceiptResponse DTO to Receipt domain model.
 *
 * scanConfidence values from backend: "HIGH", "MEDIUM", "LOW"
 * These are displayed as badges on the ReceiptScanScreen.
 *
 * subtotal, taxAmount, tipAmount are null when the AI
 * couldn't extract them from the receipt image.
 */
fun ReceiptResponse.toDomain(): Receipt = Receipt(
    id              = id,
    expenseId       = expenseId,
    scannedById     = scannedById,
    imageUrl        = imageUrl,
    merchantName    = merchantName,
    merchantAddress = merchantAddress,
    subtotal        = subtotal,
    taxAmount       = taxAmount,
    tipAmount       = tipAmount,
    totalAmount     = totalAmount,
    currency        = currency,
    paymentMethod   = paymentMethod,
    receiptDate     = receiptDate,
    scanConfidence  = scanConfidence,
    itemCount       = itemCount,
    createdAt       = createdAt,
)
