package com.prathik.fairshare.domain.model

import kotlinx.serialization.Serializable
/**
 * The current relationship status between two users.
 */
@Serializable
enum class FriendStatus {
    PENDING,
    ACCEPTED,
    GROUP,
    BLOCKED,
    REPORTED
}