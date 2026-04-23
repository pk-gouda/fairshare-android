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
    version = 8,
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

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE balances ADD COLUMN groupLastActivity TEXT")
            }
        }

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("DROP TABLE IF EXISTS groups")
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS groups (
                        id TEXT NOT NULL PRIMARY KEY,
                        name TEXT NOT NULL,
                        type TEXT NOT NULL,
                        groupImage TEXT,
                        createdById TEXT NOT NULL,
                        createdByName TEXT NOT NULL,
                        tripStartDate TEXT,
                        tripEndDate TEXT,
                        simplifyDebts INTEGER NOT NULL,
                        inviteCode TEXT NOT NULL,
                        groupNotes TEXT,
                        lastActivityDate TEXT,
                        isArchived INTEGER NOT NULL,
                        memberCount INTEGER NOT NULL,
                        createdAt TEXT NOT NULL,
                        cachedAt INTEGER NOT NULL DEFAULT 0
                    )
                """.trimIndent())
                db.execSQL("DROP TABLE IF EXISTS balances")
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS balances (
                        id TEXT NOT NULL PRIMARY KEY,
                        userId TEXT NOT NULL,
                        otherUserId TEXT NOT NULL,
                        otherUserName TEXT NOT NULL,
                        amount REAL NOT NULL,
                        currency TEXT NOT NULL,
                        groupId TEXT NOT NULL DEFAULT '',
                        groupName TEXT,
                        cachedAt INTEGER NOT NULL DEFAULT 0,
                        groupLastActivity TEXT
                    )
                """.trimIndent())
            }
        }

        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE groups ADD COLUMN lastRemainderIndex INTEGER NOT NULL DEFAULT 0")
            }
        }

        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE groups ADD COLUMN defaultCurrency TEXT NOT NULL DEFAULT 'USD'")
            }
        }

        /** 6 → 7: Add soft delete fields to groups cache. */
        val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE groups ADD COLUMN isDeleted INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE groups ADD COLUMN deletedAt TEXT")
            }
        }

        /** 7 → 8: Add friendCode and timezone to users cache. */
        val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE users ADD COLUMN friendCode TEXT")
                db.execSQL("ALTER TABLE users ADD COLUMN timezone TEXT NOT NULL DEFAULT 'UTC'")
            }
        }
    }
}