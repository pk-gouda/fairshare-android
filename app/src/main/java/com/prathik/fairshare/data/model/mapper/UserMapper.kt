package com.prathik.fairshare.data.model.mapper

import com.prathik.fairshare.data.model.response.UserResponse
import com.prathik.fairshare.domain.model.User

/**
 * Maps UserResponse DTO to User domain model.
 *
 * AccountStatus.ACTIVE → isActive = true
 * All other statuses   → isActive = false
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
    isActive            = accountStatus.name == "ACTIVE",
)
