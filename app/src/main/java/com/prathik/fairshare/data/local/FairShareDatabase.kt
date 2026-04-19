package com.prathik.fairshare.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * FairShare Room database.
 *
 * Version history:
 * 1 → 2: Added invited_friends table for locally stored invites/placeholders
 * 2 → 3: Added groupLastActivity column to balances table (BalanceEntity)
 * 3 → 4: (fallbackToDestructiveMigration — schema changes to groups/balances)
 * 4 → 5: Added lastRemainderIndex column to groups table (GroupEntity)
 * 5 → 6: Added defaultCurrency column to groups table (GroupEntity)
 */
@Database(
    entities = [
        GroupEntity::class,
        ExpenseEntity::class,
        BalanceEntity::class,
        UserEntity::class,
        PendingActionEntity::class,
        InvitedFriendEntity::class,
        FriendEntity::class,
    ],
    version = 6,
    exportSchema = false,
)
abstract class FairShareDatabase : RoomDatabase() {
    abstract fun groupDao(): GroupDao
    abstract fun expenseDao(): ExpenseDao
    abstract fun balanceDao(): BalanceDao
    abstract fun userDao(): UserDao
    abstract fun pendingActionDao(): PendingActionDao
    abstract fun invitedFriendDao(): InvitedFriendDao
    abstract fun friendDao(): FriendDao

    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS invited_friends (
                        id TEXT NOT NULL PRIMARY KEY,
                        displayName TEXT NOT NULL,
                        emailOrPhone TEXT NOT NULL,
                        isPlaceholder INTEGER NOT NULL,
                        invitedAt INTEGER NOT NULL DEFAULT 0
                    )
                """.trimIndent()
                )
            }
        }

        // No MIGRATION_2_3 needed — fallbackToDestructiveMigration() handles it.
        // BalanceEntity gained groupLastActivity TEXT NULL column.
        // Room is a cache only — all data lives on the backend, safe to wipe.
    }
}