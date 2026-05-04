package com.prathik.fairshare.data.model.request

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Request body for POST /api/users/me/deactivate.
 *
 * password is null for GOOGLE/APPLE accounts (no stored password).
 * password is required (non-null, non-blank) for LOCAL accounts.
 */
@Serializable
data class DeactivateAccountRequest(
    @SerialName("password") val password: String? = null,
)
