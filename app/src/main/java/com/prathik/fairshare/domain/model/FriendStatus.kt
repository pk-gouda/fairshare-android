package com.prathik.fairshare.domain.model

import kotlinx.serialization.Serializable
/**
 * The current relationship status between two users.
 *
 * REMOVED is returned by the backend when a direct friendship was actively ended
 * by one party. The friendship row is preserved on the backend for financial history,
 * but the active social relationship is gone. Treat REMOVED like no active friendship:
 * show re-add option, do not show Remove Friend button.
 */
@Serializable
enum class FriendStatus {
    PENDING,
    ACCEPTED,
    GROUP,
    BLOCKED,
    REPORTED,
    REMOVED
}