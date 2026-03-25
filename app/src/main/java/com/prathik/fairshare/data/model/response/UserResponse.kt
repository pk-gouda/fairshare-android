package com.prathik.fairshare.data.model.response

import com.prathik.fairshare.domain.model.AccountStatus
import com.prathik.fairshare.domain.model.AuthProvider
import kotlinx.serialization.Serializable

/**
 * API response DTO for a user profile.
 * Maps to UserResponse.java record on the backend.
 * Convert to domain model User via UserResponse.toDomain().
 */
@Serializable
data class UserResponse(
    val id: String,
    val email: String,
    val fullName: String,
    val phoneNumber: String? = null,
    val profilePictureUrl: String? = null,
    val authProvider: AuthProvider,
    val accountStatus: AccountStatus,
    val preferredCurrency: String,
    val language: String,
    val notificationEnabled: Boolean,
    val createdAt: String,
)
