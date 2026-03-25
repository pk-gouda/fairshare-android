package com.prathik.fairshare.domain.repository

import com.prathik.fairshare.domain.model.Settlement

/**
 * Contract for settlement-related operations.
 * Implementation lives in data/repository/impl/SettlementRepositoryImpl.kt
 */
interface SettlementRepository {

    /**
     * Settles balances with another user.
     * type — "ALL", "GROUP", "NON_GROUP", "PARTIAL"
     * Settlements are immediately COMPLETED — no confirmation step.
     */
    suspend fun settle(
        otherUserId: String,
        type: String,
        groupId: String?,
        amount: Double?,
        currency: String?,
        paymentMethod: String?,
        notes: String?,
    ): Result<List<Settlement>>

    /**
     * Fetches the full settlement history with a specific user.
     */
    suspend fun getHistory(otherUserId: String): Result<List<Settlement>>

    /**
     * Fetches all pending settlements where current user is the receiver.
     */
    suspend fun getPending(): Result<List<Settlement>>

    /**
     * Fetches all settlements initiated by the current user.
     */
    suspend fun getInitiated(): Result<List<Settlement>>

    /**
     * Cancels a settlement.
     */
    suspend fun cancelSettlement(settlementId: String): Result<Settlement>

    /**
     * Fetches a breakdown of what is owed between current user and another user.
     * Broken down by group and non-group expenses.
     */
    suspend fun getBreakdown(otherUserId: String): Result<Map<String, Any>>
}