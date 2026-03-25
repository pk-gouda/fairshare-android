package com.prathik.fairshare.domain.model

data class Friend(
    val id: String,
    val fullName: String,
    val email: String,
    val profilePictureUrl: String?,
)

data class Friendship(
    val id: String,
    val requesterId: String,
    val requesterName: String,
    val receiverId: String,
    val receiverName: String,
    val status: FriendStatus,
    val friendshipType: FriendshipType,
    val createdAt: String,
)