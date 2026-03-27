package com.prathik.fairshare.domain.model

import kotlinx.serialization.Serializable
/**
 * The type of group — determines the tile gradient and icon
 * shown on the Groups Home screen.
 */
@Serializable
enum class GroupType {
    TRIP,
    HOME,
    COUPLE,
    APARTMENT,
    OFFICE,
    FRIENDS,
    EVENT,
    OTHER
}