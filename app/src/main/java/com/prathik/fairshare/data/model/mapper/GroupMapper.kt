package com.prathik.fairshare.data.model.mapper

import com.prathik.fairshare.data.model.response.GroupMemberResponse
import com.prathik.fairshare.data.model.response.GroupResponse
import com.prathik.fairshare.data.model.response.UnclaimedMemberResponse
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

/**
 * Maps an unclaimed-member response into the GroupMember domain model used by the
 * import claim/assign UI.
 *
 * The backend's unclaimed-member object is NOT a group member row — it only carries
 * placeholderUserId + csvName + balance totals. The claim/assign UI reads exactly two
 * fields off each entry: userId (the placeholder ID it sends to claim/assign) and
 * fullName (the display label). We therefore map placeholderUserId → userId and
 * csvName → fullName. The remaining GroupMember fields (id, email, profilePictureUrl,
 * joinedAt) are not applicable to an unclaimed placeholder and are never read for these
 * entries; they are left blank rather than populated with invented values.
 */
fun UnclaimedMemberResponse.toDomain(): GroupMember = GroupMember(
    id                = "",
    userId            = placeholderUserId,
    fullName          = csvName,
    email             = "",
    profilePictureUrl = null,
    joinedAt          = "",
)

private fun String.toGroupTypeSafe(): GroupType =
    try { GroupType.valueOf(this) } catch (e: IllegalArgumentException) { GroupType.OTHER }