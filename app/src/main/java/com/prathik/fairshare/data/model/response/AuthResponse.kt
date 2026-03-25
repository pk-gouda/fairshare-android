package com.prathik.fairshare.data.model.response

import kotlinx.serialization.Serializable

/**
 * API response DTO for successful authentication.
 * Returned by login, register (oauth), and token refresh endpoints.
 *
 * @param accessToken  JWT access token — short-lived (15 minutes)
 * @param refreshToken JWT refresh token — long-lived (7 days)
 * @param tokenType    always "Bearer"
 * @param expiresIn    access token lifetime in seconds (900)
 * @param user         the authenticated user's profile — null on token refresh
 */
@Serializable
data class AuthResponse(
    val accessToken: String,
    val refreshToken: String,
    val tokenType: String,
    val expiresIn: Long,
    val user: UserResponse? = null,
)
