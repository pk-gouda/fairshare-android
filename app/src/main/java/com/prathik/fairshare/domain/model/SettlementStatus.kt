package com.prathik.fairshare.domain.model

import kotlinx.serialization.Serializable
/**
 * The current state of a settlement record.
 * Note: settlements are immediately COMPLETED in the current backend.
 */
@Serializable
enum class SettlementStatus {
    PENDING,
    COMPLETED,
    CANCELLED,
    LEGACY_UNALLOCATED
}