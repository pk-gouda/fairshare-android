package com.prathik.fairshare.data.model.response

import kotlinx.serialization.Serializable

/**
 * API response DTO for a balance between two users.
 * Maps to BalanceResponse.java record on the backend.
 *
 * Sign convention (matches backend exactly):
 * - Positive amount = otherUser owes you (green — "you are owed")
 * - Negative amount = you owe otherUser (orange — "you owe")
 *
 * groupId and groupName are null for non-group (direct) balances.
 */
@Serializable
data class BalanceResponse(
    val userId: String,
    val otherUserId: String,
    val otherUserName: String,
    val amount: Double,
    val currency: String,
    val groupId: String? = null,
    val groupName: String? = null,
)
