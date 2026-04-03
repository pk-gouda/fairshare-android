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
 */
@Database(
    entities = [
        GroupEntity::class,
        ExpenseEntity::class,
        BalanceEntity::class,
        UserEntity::class,
        PendingActionEntity::class,
        InvitedFriendEntity::class,
    ],
    version = 2,
    exportSchema = false,
)
abstract class FairShareDatabase : RoomDatabase() {
    abstract fun groupDao(): GroupDao
    abstract fun expenseDao(): ExpenseDao
    abstract fun balanceDao(): BalanceDao
    abstract fun userDao(): UserDao
    abstract fun pendingActionDao(): PendingActionDao
    abstract fun invitedFriendDao(): InvitedFriendDao

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
    }
}