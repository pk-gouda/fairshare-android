package com.prathik.fairshare.data.model.enums

/**
 * The current relationship status between two users.
 *
 * PENDING  — friend request sent, awaiting response
 * ACCEPTED — both users are direct friends
 * GROUP    — users share a group but are not direct friends
 * BLOCKED  — one user has blocked the other
 * REPORTED — one user has reported the other
 */
enum class FriendStatus {
    PENDING,
    ACCEPTED,
    GROUP,
    BLOCKED,
    REPORTED
}