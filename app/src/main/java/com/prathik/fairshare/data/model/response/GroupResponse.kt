package com.prathik.fairshare.data.model.response

import com.prathik.fairshare.domain.model.GroupType
import kotlinx.serialization.Serializable

/**
 * API response DTO for a group.
 * Maps to GroupResponse.java record on the backend.
 *
 * Note: tripStartDate and tripEndDate are LocalDate on the backend,
 * serialized as "yyyy-MM-dd" strings (e.g. "2026-03-25").
 */
@Serializable
data class GroupResponse(
    val id: String,
    val name: String,
    val groupImage: String? = null,
    val type: GroupType,
    val createdById: String,
    val createdByName: String,
    val tripStartDate: String? = null,
    val tripEndDate: String? = null,
    val simplifyDebts: Boolean,
    val inviteCode: String,
    val groupNotes: String? = null,
    val lastActivityDate: String? = null,
    val isArchived: Boolean,
    val memberCount: Int,
    val createdAt: String,
)
