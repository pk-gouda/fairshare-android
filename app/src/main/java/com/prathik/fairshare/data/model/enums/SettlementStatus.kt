package com.prathik.fairshare.data.model.enums

/**
 * The current state of a settlement record.
 *
 * PENDING   — settlement initiated but not yet confirmed
 * COMPLETED — payment confirmed, balances updated
 * CANCELLED — settlement was cancelled before completion
 *
 * Note: In the current backend implementation, settlements are
 * immediately set to COMPLETED — there is no PENDING confirmation step.
 */
enum class SettlementStatus {
    PENDING,
    COMPLETED,
    CANCELLED
}