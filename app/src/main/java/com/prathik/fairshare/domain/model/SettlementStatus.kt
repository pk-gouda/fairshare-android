package com.prathik.fairshare.domain.model

/**
 * The current state of a settlement record.
 * Note: settlements are immediately COMPLETED in the current backend.
 */
enum class SettlementStatus {
    PENDING,
    COMPLETED,
    CANCELLED
}