package com.prathik.fairshare.data.local

import androidx.room.Entity

/**
 * Room entity for caching balance data locally.
 * Composite primary key: userId + otherUserId + currency + groupId.
 * groupId defaults to empty string for non-group balances.
 */
@Entity(
    tableName = "balances",
    primaryKeys = ["userId", "otherUserId", "currency", "groupId"],
)
data class BalanceEntity(
    val userId: String,
    val otherUserId: String,
    val otherUserName: String,
    val amount: Double,
    val currency: String,
    val groupId: String = "",
    val groupName: String?,
    val cachedAt: Long = System.currentTimeMillis(),
)
