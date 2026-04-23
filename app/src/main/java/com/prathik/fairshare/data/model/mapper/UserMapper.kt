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
)