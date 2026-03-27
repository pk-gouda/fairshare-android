package com.prathik.fairshare.domain.model

import kotlinx.serialization.Serializable
/**
 * How the friendship was formed.
 */
@Serializable
enum class FriendshipType {
    DIRECT,
    GROUP
}