package com.prathik.fairshare.domain.model

/**
 * The scope of a settlement operation.
 * Used instead of a raw String to enforce valid values at compile time.
 *
 * ALL       — settle everything between two users across all groups
 * GROUP     — settle only a specific group's balance
 * NON_GROUP — settle only non-group direct expenses
 * PARTIAL   — settle a custom amount
 */
enum class SettleType {
    ALL,
    GROUP,
    NON_GROUP,
    PARTIAL
}