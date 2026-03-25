package com.prathik.fairshare.data.model.enums

/**
 * The type of group — determines the tile gradient and icon
 * shown on the Groups Home screen.
 *
 * TRIP       — vacation, travel
 * HOME       — roommates, shared house
 * COUPLE     — partners
 * APARTMENT  — rent and utilities
 * OFFICE     — work expenses
 * FRIENDS    — friend group
 * EVENT      — one-time party or event
 * OTHER      — anything else
 */
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