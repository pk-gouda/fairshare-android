package com.prathik.fairshare.data.model.mapper

import com.prathik.fairshare.data.model.response.GroupMemberResponse
import com.prathik.fairshare.data.model.response.GroupResponse
import com.prathik.fairshare.domain.model.Group
import com.prathik.fairshare.domain.model.GroupMember

/**
 * Maps GroupResponse DTO to Group domain model.
 *
 * tripStartDate and tripEndDate are kept as String here —
 * they are "yyyy-MM-dd" date strings from the backend.
 * DateFormatter in util/ handles display formatting.
 */
fun GroupResponse.toDomain(): Group = Group(
    id               = id,
    name             = name,
    type             = type,
    createdById      = createdById,
    createdByName    = createdByName,
    inviteCode       = inviteCode,
    simplifyDebts    = simplifyDebts,
    isArchived       = isArchived,
    memberCount      = memberCount,
    groupNotes       = groupNotes,
    groupImage       = groupImage,
    lastActivityDate = lastActivityDate,
    tripStartDate    = tripStartDate,
    tripEndDate      = tripEndDate,
    createdAt        = createdAt,
)

/**
 * Maps GroupMemberResponse DTO to GroupMember domain model.
 */
fun GroupMemberResponse.toDomain(): GroupMember = GroupMember(
    id                = id,
    userId            = userId,
    fullName          = fullName,
    email             = email,
    profilePictureUrl = profilePictureUrl,
    joinedAt          = joinedAt,
)
