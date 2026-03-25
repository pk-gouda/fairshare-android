package com.prathik.fairshare.data.model.enums

/**
 * Determines how an expense is split among members.
 *
 * EQUAL      — split equally among all participants
 * UNEQUAL    — exact amounts per person (must sum to total)
 * PERCENTAGE — percentage per person (must sum to 100%)
 * SHARES     — ratio-based (e.g. 2 shares vs 1 share, auto-calculates amounts)
 */
enum class SplitType {
    EQUAL,
    UNEQUAL,
    PERCENTAGE,
    SHARES
}