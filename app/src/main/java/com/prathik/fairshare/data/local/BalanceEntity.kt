package com.prathik.fairshare.data.local

import androidx.room.Entity

/**
 * Room entity for caching balance data locally.
 * Composite primary key: userId + otherUserId + currency + groupId + cacheScope.
 *
 * cacheScope separates rows by origin so that getAllBalances() net rows and
 * getBreakdownWithUser() group rows never overwrite or contaminate each other.
 *
 * Scopes:
 *   ALL_BALANCES   — from getAllBalances() API, represents total relationship balance
 *   FRIEND_NET     — from getNetBalanceWithUser(), total balance with one friend
 *   FRIEND_BREAKDOWN — from getBreakdownWithUser(), per-group component
 *   GROUP_BALANCE  — from getGroupBalances(), per-member balance in a group
 */
@Entity(
    tableName = "balances",
    primaryKeys = ["userId", "otherUserId", "currency", "groupId", "cacheScope"],
)
data class BalanceEntity(
    val userId: String,
    val otherUserId: String,
    val otherUserName: String,
    val amount: Double,
    val currency: String,
    val groupId: String = "",
    val groupName: String?,
    val cacheScope: String = CacheScope.ALL_BALANCES,
    val cachedAt: Long = System.currentTimeMillis(),
    val groupLastActivity: String? = null,
) {
    object CacheScope {
        const val ALL_BALANCES     = "ALL_BALANCES"
        const val FRIEND_NET       = "FRIEND_NET"
        const val FRIEND_BREAKDOWN = "FRIEND_BREAKDOWN"
        const val GROUP_BALANCE    = "GROUP_BALANCE"
    }
}