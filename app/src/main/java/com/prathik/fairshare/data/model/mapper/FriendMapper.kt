package com.prathik.fairshare.data.model.mapper

import com.prathik.fairshare.data.model.response.FriendResponse
import com.prathik.fairshare.data.model.response.FriendshipResponse
import com.prathik.fairshare.domain.model.Friend
import com.prathik.fairshare.domain.model.Friendship

/**
 * Maps FriendResponse DTO to Friend domain model.
 * FriendResponse contains only public profile info —
 * no relationship metadata.
 */
fun FriendResponse.toDomain(): Friend = Friend(
    id                = id,
    fullName          = fullName,
    email             = email,
    profilePictureUrl = profilePictureUrl,
)

/**
 * Maps FriendshipResponse DTO to Friendship domain model.
 * FriendshipResponse contains the full relationship record —
 * who sent the request, status, and how the friendship formed.
 */
fun FriendshipResponse.toDomain(): Friendship = Friendship(
    id             = id,
    requesterId    = requesterId,
    requesterName  = requesterName,
    receiverId     = receiverId,
    receiverName   = receiverName,
    status         = status,
    friendshipType = friendshipType,
    createdAt      = createdAt,
)
