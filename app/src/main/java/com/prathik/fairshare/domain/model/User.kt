package com.prathik.fairshare.domain.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class User(
    val id                 : String,
    val email              : String,
    val fullName           : String,
    val phoneNumber        : String?,
    val profilePictureUrl  : String?,
    val preferredCurrency  : String,
    val language           : String,
    val notificationEnabled: Boolean,
    val isActive           : Boolean,
    val friendCode         : String? = null,
    val timezone           : String  = "UTC",
    // Auth provider — "LOCAL", "GOOGLE", or "APPLE".
    // Populated from UserResponse.toDomain(); defaults to "LOCAL" when read from
    // the Room cache (UserEntity has no authProvider column — avoids a migration).
    // The server enforces the correct check regardless of what the client sends,
    // so a wrong default is never a security issue.
    val authProvider       : String  = "LOCAL",
) : Parcelable