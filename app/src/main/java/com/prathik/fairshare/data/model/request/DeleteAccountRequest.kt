package com.prathik.fairshare.data.model.request

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Request body for DELETE /api/users/me.
 * Same password policy as DeactivateAccountRequest.
 */
@Serializable
data class DeleteAccountRequest(
    @SerialName("password") val password: String? = null,
)
