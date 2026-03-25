package com.prathik.fairshare.data.model.request

import com.prathik.fairshare.data.model.enums.GroupType
import kotlinx.serialization.Serializable

/**
 * Request body for POST /api/groups
 * IMPORTANT: backend field is "description" not "notes" — verified from source.
 */
@Serializable
data class CreateGroupRequest(
    val name: String,
    val type: GroupType,
    val description: String? = null,
)

/**
 * Request body for PUT /api/groups/{groupId}
 * All fields are optional — only non-null fields are updated.
 */
@Serializable
data class UpdateGroupRequest(
    val name: String? = null,
    val description: String? = null,
    val simplifyDebts: Boolean? = null,
)

/**
 * Request body for POST /api/groups/join
 */
@Serializable
data class JoinGroupRequest(
    val inviteCode: String,
)

/**
 * Request body for POST /api/groups/{groupId}/members
 */
@Serializable
data class AddMemberRequest(
    val userId: String,
)
