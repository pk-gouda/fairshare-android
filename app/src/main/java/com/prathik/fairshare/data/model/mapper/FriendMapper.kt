package com.prathik.fairshare.data.model.mapper

import com.prathik.fairshare.data.model.response.FriendResponse
import com.prathik.fairshare.data.model.response.FriendshipResponse
import com.prathik.fairshare.domain.model.AccountStatus
import com.prathik.fairshare.domain.model.Friend
import com.prathik.fairshare.domain.model.FriendStatus
import com.prathik.fairshare.domain.model.Friendship
import com.prathik.fairshare.domain.model.FriendshipType

fun FriendResponse.toDomain(): Friend = Friend(
    id                = id,
    fullName          = fullName,
    email             = email ?: "",
    profilePictureUrl = profilePictureUrl,
    accountStatus     = accountStatus,
)

fun FriendshipResponse.toDomain(): Friendship = Friendship(
    id                    = id,
    requesterId           = requesterId,
    requesterName         = requesterName,
    receiverId            = receiverId,
    receiverName          = receiverName,
    receiverEmail         = receiverEmail,
    receiverAccountStatus = receiverAccountStatus,
    status                = status.toFriendStatusSafe(),
    friendshipType        = friendshipType.toFriendshipTypeSafe(),
    createdAt             = createdAt,
)

private fun String.toFriendStatusSafe(): FriendStatus =
    try { FriendStatus.valueOf(this) } catch (e: IllegalArgumentException) { FriendStatus.PENDING }

private fun String.toFriendshipTypeSafe(): FriendshipType =
    try { FriendshipType.valueOf(this) } catch (e: IllegalArgumentException) { FriendshipType.DIRECT }