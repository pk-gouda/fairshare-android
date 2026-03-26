package com.prathik.fairshare.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

/**
 * FairShare Room database.
 *
 * Contains:
 * - groups       — cached group list for offline-first display
 * - expenses     — cached expense list per group
 * - balances     — cached balance data for hero section
 * - users        — cached current user profile
 * - pending_actions — offline action queue (drained by OfflineSyncManager)
 *
 * When schema changes: increment version and add a Migration.
 */
@Database(
    entities = [
        GroupEntity::class,
        ExpenseEntity::class,
        BalanceEntity::class,
        UserEntity::class,
        PendingActionEntity::class,
    ],
    version = 1,
    exportSchema = false,
)
abstract class FairShareDatabase : RoomDatabase() {
    abstract fun groupDao(): GroupDao
    abstract fun expenseDao(): ExpenseDao
    abstract fun balanceDao(): BalanceDao
    abstract fun userDao(): UserDao
    abstract fun pendingActionDao(): PendingActionDao
}
