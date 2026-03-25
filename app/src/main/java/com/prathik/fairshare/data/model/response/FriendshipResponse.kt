package com.prathik.fairshare.data.model.response

import com.prathik.fairshare.data.model.enums.FriendStatus
import com.prathik.fairshare.data.model.enums.FriendshipType
import kotlinx.serialization.Serializable

/**
 * API response DTO for a friendship relationship record.
 * Maps to FriendshipResponse.java record on the backend.
 * Used for pending requests (sent and received).
 */
@Serializable
data class FriendshipResponse(
    val id: String,
    val requesterId: String,
    val requesterName: String,
    val receiverId: String,
    val receiverName: String,
    val status: FriendStatus,
    val friendshipType: FriendshipType,
    val createdAt: String,
)
