package com.prathik.fairshare.data.model.mapper

import com.prathik.fairshare.data.model.response.GroupMemberResponse
import com.prathik.fairshare.data.model.response.GroupResponse
import com.prathik.fairshare.domain.model.Group
import com.prathik.fairshare.domain.model.GroupMember
import com.prathik.fairshare.domain.model.GroupType

/**
 * Maps GroupResponse DTO to Group domain model.
 *
 * type is stored as String in the DTO to prevent deserialization crashes
 * when the backend adds new GroupType values.
 * Safe conversion with fallback: unknown type → OTHER.
 */
fun GroupResponse.toDomain(): Group = Group(
    id               = id,
    name             = name,
    type             = type.toGroupTypeSafe(),
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
    createdAt           = createdAt,
    lastRemainderIndex  = lastRemainderIndex,
    defaultCurrency     = defaultCurrency,
)

fun GroupMemberResponse.toDomain(): GroupMember = GroupMember(
    id                = id,
    userId            = userId,
    fullName          = fullName,
    email             = email,
    profilePictureUrl = profilePictureUrl,
    joinedAt          = joinedAt,
)

private fun String.toGroupTypeSafe(): GroupType =
    try { GroupType.valueOf(this) } catch (e: IllegalArgumentException) { GroupType.OTHER }