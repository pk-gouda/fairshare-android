package com.prathik.fairshare.domain.repository

import com.prathik.fairshare.domain.model.Balance

/**
 * Contract for balance-related operations.
 * Implementation lives in data/repository/impl/BalanceRepositoryImpl.kt
 */
interface BalanceRepository {

    /**
     * Fetches all balances for the current user across all groups and direct expenses.
     * Positive amount = other user owes you.
     * Negative amount = you owe other user.
     */
    suspend fun getAllBalances(): Result<List<Balance>>

    /**
     * Fetches the detailed balance breakdown between the current user and one other user.
     * Broken down per group.
     */
    suspend fun getBalanceWithUser(otherUserId: String): Result<Map<String, Any>>

    /**
     * Fetches a summary of total owed and total owing for the current user.
     */
    suspend fun getBalanceSummary(): Result<Map<String, Any>>
}