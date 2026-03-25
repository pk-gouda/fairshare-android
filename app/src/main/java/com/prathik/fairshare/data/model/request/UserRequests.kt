package com.prathik.fairshare.data.model.request

import kotlinx.serialization.Serializable

/**
 * Request body for PUT /api/users/me
 * All fields are optional — only non-null fields are updated.
 */
@Serializable
data class UpdateProfileRequest(
    val fullName: String? = null,
    val phoneNumber: String? = null,
    val preferredCurrency: String? = null,
    val language: String? = null,
    val notificationEnabled: Boolean? = null,
)
