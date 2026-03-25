package com.prathik.fairshare.domain.repository

import com.prathik.fairshare.domain.model.ApiResult
import com.prathik.fairshare.domain.model.Receipt

/**
 * Contract for receipt scanning and retrieval operations.
 * Implementation lives in data/repository/impl/ReceiptRepositoryImpl.kt
 */
interface ReceiptRepository {

    /**
     * Scans a receipt image using AI (Gemini) and extracts:
     * merchant name, subtotal, tax, tip, total, line items.
     *
     * [imageBase64] — base64-encoded image data
     * [mimeType]    — "image/jpeg" or "image/png"
     * [preferredCurrency] — optional hint for the AI
     *
     * Returns [ApiResult.ValidationError] if image is unreadable.
     * Returns the scanned receipt with HIGH/MEDIUM/LOW confidence score.
     */
    suspend fun scanReceipt(
        imageBase64: String,
        mimeType: String,
        preferredCurrency: String?,
    ): ApiResult<Receipt>

    /**
     * Fetches a previously scanned receipt by ID.
     * Returns [ApiResult.NotFound] if receipt doesn't exist.
     */
    suspend fun getReceipt(receiptId: String): ApiResult<Receipt>
}