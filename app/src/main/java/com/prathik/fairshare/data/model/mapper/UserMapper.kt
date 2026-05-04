package com.prathik.fairshare.data.model.mapper

import com.prathik.fairshare.data.model.response.UserResponse
import com.prathik.fairshare.domain.model.User

/**
 * Maps UserResponse DTO to User domain model.
 *
 * authProvider and accountStatus are stored as String in the DTO.
 * isActive = true only when accountStatus == "ACTIVE".
 * Unknown accountStatus values default to inactive for safety.
 */
fun UserResponse.toDomain(): User = User(
    id                  = id,
    email               = email,
    fullName            = fullName,
    phoneNumber         = phoneNumber,
    profilePictureUrl   = profilePictureUrl,
    preferredCurrency   = preferredCurrency,
    language            = language,
    notificationEnabled = notificationEnabled,
    isActive            = accountStatus == "ACTIVE",
    friendCode          = friendCode,
    timezone            = timezone ?: "UTC",
    // authProvider is passed through from the DTO so AccountScreen can determine
    // whether to show a password dialog (LOCAL) or skip it (GOOGLE/APPLE).
    // It is NOT stored in the Room cache — UserEntity has no authProvider column.
    // Defaults to "LOCAL" in the domain model for cache reads (safe fallback: server
    // enforces the correct check regardless of the client value).
    authProvider        = authProvider,
)