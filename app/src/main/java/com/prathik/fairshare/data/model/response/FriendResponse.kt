package com.prathik.fairshare.data.model.response

import kotlinx.serialization.Serializable

/**
 * API response DTO for a friend's public profile.
 * Maps to FriendResponse.java record on the backend.
 * Used in friend lists, blocked users list, and search results.
 */
@Serializable
data class FriendResponse(
    val id: String,
    val fullName: String,
    val email: String,
    val profilePictureUrl: String? = null,
)
