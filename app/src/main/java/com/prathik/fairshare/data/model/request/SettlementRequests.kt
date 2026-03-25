package com.prathik.fairshare.data.model.request

import kotlinx.serialization.Serializable

/**
 * Request body for POST /api/settlements
 *
 * type options:
 * - "ALL"       — settle everything between two users across all groups and direct expenses
 * - "GROUP"     — settle only a specific group's balance (requires groupId)
 * - "NON_GROUP" — settle only non-group direct expenses
 * - "PARTIAL"   — settle a custom amount (requires amount + currency)
 *
 * Note: type is a plain String, not an enum — matches backend SettleRequest exactly.
 * Settlements are immediately COMPLETED — no confirmation step.
 */
@Serializable
data class SettleRequest(
    val otherUserId: String,
    val type: String,
    val groupId: String? = null,
    val amount: Double? = null,
    val currency: String? = null,
    val paymentMethod: String? = null,
    val paymentProofImage: String? = null,
    val notes: String? = null,
)
