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
     *
     * Note: results are served from Room cache with a background network refresh.
     * Use [getNetBalanceWithUser] for a guaranteed fresh network call.
     */
    suspend fun getAllBalances(): ApiResult<List<Balance>>

    /**
     * Fetches per-group balance breakdown between the current user and [otherUserId].
     * Returns one Balance per group where a non-zero balance exists.
     * Each Balance has groupId and groupName populated.
     *
     * Always hits the network (no Room cache).
     * Powers: FriendDetail per-group rows, FriendSettings shared groups list,
     * SettleUp screen for scoped (group) settlements.
     */
    suspend fun getBreakdownWithUser(otherUserId: String): ApiResult<List<Balance>>

    /**
     * Fetches the net (cross-group) balance between the current user and [otherUserId].
     * Returns one Balance per currency where a non-zero net balance exists.
     * groupId is null on all returned items (these are UserBalance records).
     *
     * Always hits the network (no Room cache) — use this wherever a fresh balance
     * is required (e.g. SettleUp screen for non-group settlements).
     */
    suspend fun getNetBalanceWithUser(otherUserId: String): ApiResult<List<Balance>>

    /**
     * Fetches a summary of totals for the current user:
     * - Total amount owed to you across all groups
     * - Total amount you owe across all groups
     * - Net balance
     * Used by the Groups Home screen balance hero section.
     */
    suspend fun getBalanceSummary(): ApiResult<Map<String, Any>>
}
