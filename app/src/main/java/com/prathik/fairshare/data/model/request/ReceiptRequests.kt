package com.prathik.fairshare.data.model.request

import kotlinx.serialization.Serializable

/**
 * Request body for POST /api/receipts/scan
 *
 * imageBase64       — base64-encoded image data of the receipt photo
 * mimeType          — MIME type of the image (e.g. "image/jpeg", "image/png")
 * preferredCurrency — optional hint for the AI to use when parsing amounts
 */
@Serializable
data class ScanReceiptRequest(
    val imageBase64: String,
    val mimeType: String,
    val preferredCurrency: String? = null,
)
