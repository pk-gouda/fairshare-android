package com.prathik.fairshare.data.model.response

import com.prathik.fairshare.data.model.enums.SettlementStatus
import kotlinx.serialization.Serializable

/**
 * API response DTO for a settlement record.
 * Maps to SettlementResponse.java record on the backend.
 *
 * Note: Settlements are immediately COMPLETED in the current backend —
 * there is no PENDING → confirm step. completedAt will equal settlementDate.
 *
 * groupId and groupName are null for non-group (direct) settlements.
 */
@Serializable
data class SettlementResponse(
    val id: String,
    val payerId: String,
    val payerName: String,
    val receiverId: String,
    val receiverName: String,
    val amount: Double,
    val currency: String,
    val groupId: String? = null,
    val groupName: String? = null,
    val status: SettlementStatus,
    val notes: String? = null,
    val paymentMethod: String? = null,
    val paymentProofImage: String? = null,
    val recordedById: String,
    val recordedByName: String,
    val settlementDate: String,
    val completedAt: String? = null,
    val createdAt: String,
)
