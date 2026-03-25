package com.prathik.fairshare.data.model.response

import kotlinx.serialization.Serializable

/**
 * API response DTO for a group member.
 * Maps to GroupMemberResponse.java record on the backend.
 */
@Serializable
data class GroupMemberResponse(
    val id: String,
    val userId: String,
    val fullName: String,
    val email: String,
    val profilePictureUrl: String? = null,
    val joinedAt: String,
)
