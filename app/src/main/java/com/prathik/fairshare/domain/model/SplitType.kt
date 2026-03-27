package com.prathik.fairshare.domain.model

import kotlinx.serialization.Serializable
/**
 * Determines how an expense is divided among participants.
 *
 * EQUAL      — divided equally among all participants
 * UNEQUAL    — exact amounts per person (must sum to total)
 * PERCENTAGE — percentage per person (must sum to 100%)
 * SHARES     — ratio-based, amounts auto-calculated
 */
@Serializable
enum class SplitType {
    EQUAL,
    UNEQUAL,
    PERCENTAGE,
    SHARES
}