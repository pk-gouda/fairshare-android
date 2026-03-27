package com.prathik.fairshare.data.model.mapper

import com.prathik.fairshare.data.model.response.FriendResponse
import com.prathik.fairshare.data.model.response.FriendshipResponse
import com.prathik.fairshare.domain.model.Friend
import com.prathik.fairshare.domain.model.FriendStatus
import com.prathik.fairshare.domain.model.Friendship
import com.prathik.fairshare.domain.model.FriendshipType

/**
 * Maps FriendResponse DTO to Friend domain model.
 */
fun FriendResponse.toDomain(): Friend = Friend(
    id                = id,
    fullName          = fullName,
    email             = email,
    profilePictureUrl = profilePictureUrl,
)

/**
 * Maps FriendshipResponse DTO to Friendship domain model.
 *
 * status and friendshipType are stored as String in the DTO.
 * Safe conversion with fallback: unknown status → PENDING, unknown type → DIRECT.
 */
fun FriendshipResponse.toDomain(): Friendship = Friendship(
    id             = id,
    requesterId    = requesterId,
    requesterName  = requesterName,
    receiverId     = receiverId,
    receiverName   = receiverName,
    status         = status.toFriendStatusSafe(),
    friendshipType = friendshipType.toFriendshipTypeSafe(),
    createdAt      = createdAt,
)

private fun String.toFriendStatusSafe(): FriendStatus =
    try { FriendStatus.valueOf(this) } catch (e: IllegalArgumentException) { FriendStatus.PENDING }

private fun String.toFriendshipTypeSafe(): FriendshipType =
    try { FriendshipType.valueOf(this) } catch (e: IllegalArgumentException) { FriendshipType.DIRECT }
