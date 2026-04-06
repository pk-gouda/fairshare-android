package com.prathik.fairshare.domain.repository

import com.prathik.fairshare.domain.model.ApiResult
import com.prathik.fairshare.domain.model.Balance

/**
 * Contract for balance-related operations.
 * Implementation lives in data/repository/impl/BalanceRepositoryImpl.kt
 */
interface BalanceRepository {

    /**
     * Fetches all balances for the current user across all groups
     * and direct (non-group) expenses.
     *
     * Positive amount = other user owes you (green).
     * Negative amount = you owe other user (orange).
     */
    suspend fun getAllBalances(): ApiResult<List<Balance>>

    /**
     * Fetches per-group balance breakdown between the current user and [otherUserId].
     * Returns one Balance per group where a non-zero balance exists.
     * Each Balance has groupId and groupName populated.
     *
     * Powers: FriendDetail per-group rows, FriendSettings shared groups list.
     */
    suspend fun getBreakdownWithUser(otherUserId: String): ApiResult<List<Balance>>

    /**
     * Fetches the detailed balance breakdown between the current user
     * and one other user, broken down per group.
     * Used by the Settle Up screen to show per-group amounts.
     */
    suspend fun getBalanceWithUser(otherUserId: String): ApiResult<Map<String, Any>>

    /**
     * Fetches a summary of totals for the current user:
     * - Total amount owed to you across all groups
     * - Total amount you owe across all groups
     * - Net balance
     * Used by the Groups Home screen balance hero section.
     */
    suspend fun getBalanceSummary(): ApiResult<Map<String, Any>>
}