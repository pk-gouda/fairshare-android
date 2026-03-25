package com.prathik.fairshare.domain.model

/**
 * The current relationship status between two users.
 */
enum class FriendStatus {
    PENDING,
    ACCEPTED,
    GROUP,
    BLOCKED,
    REPORTED
}