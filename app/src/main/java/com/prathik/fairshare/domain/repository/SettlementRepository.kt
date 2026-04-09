package com.prathik.fairshare.domain.repository

import com.prathik.fairshare.domain.model.ApiResult
import com.prathik.fairshare.domain.model.Settlement
import com.prathik.fairshare.domain.model.SettleType

/**
 * Contract for settlement-related operations.
 * Implementation lives in data/repository/impl/SettlementRepositoryImpl.kt
 */
interface SettlementRepository {

    /**
     * Settles balances with another user.
     * Settlements are immediately COMPLETED — no confirmation step.
     *
     * [type] determines the scope:
     * - ALL       — settles everything between two users
     * - GROUP     — settles only a specific group (requires groupId)
     * - NON_GROUP — settles only non-group direct expenses
     * - PARTIAL   — settles a custom amount (requires amount + currency)
     *
     * Returns [ApiResult.Conflict] if there is nothing to settle.
     */
    suspend fun settle(
        otherUserId: String,
        type: SettleType,
        groupId: String?,
        amount: Double?,
        currency: String?,
        paymentMethod: String?,
        notes: String?,
        payerId: String? = null,
    ): ApiResult<List<Settlement>>

    /**
     * Fetches the full settlement history between the current user
     * and another user, ordered by date descending.
     */
    suspend fun getHistory(otherUserId: String): ApiResult<List<Settlement>>

    /**
     * Fetches all pending settlements where current user is the receiver.
     */
    suspend fun getPending(): ApiResult<List<Settlement>>

    /**
     * Fetches all settlements initiated by the current user.
     */
    suspend fun getInitiated(): ApiResult<List<Settlement>>

    /**
     * Cancels a settlement.
     * Returns [ApiResult.Forbidden] if user did not initiate the settlement.
     */
    suspend fun cancelSettlement(settlementId: String): ApiResult<Settlement>

    /**
     * Deletes a completed settlement and reverses its balance changes.
     * Returns [ApiResult.Forbidden] if user is not involved.
     * Returns [ApiResult.Conflict] if a participant has left the group.
     */
    suspend fun deleteSettlement(settlementId: String): ApiResult<Unit>

    /**
     * Fetches a single settlement by ID.
     * Only participants (payer or receiver) can view.
     */
    suspend fun getSettlementById(settlementId: String): ApiResult<Settlement>

    /**
     * Updates a settlement's amount, notes, or payment method.
     * Only the person who recorded it can edit.
     */
    suspend fun updateSettlement(
        settlementId: String,
        amount: Double?,
        notes: String?,
        paymentMethod: String?,
    ): ApiResult<Settlement>
}