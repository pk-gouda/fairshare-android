package com.prathik.fairshare.data.model.request

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Request body for DELETE /api/groups/{groupId}.
 * [confirmName] must exactly match the group's name — verified server-side.
 */
@Serializable
data class DeleteGroupRequest(
    @SerialName("confirmName") val confirmName: String,
)
