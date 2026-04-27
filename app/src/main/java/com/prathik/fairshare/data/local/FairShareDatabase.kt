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
 * 6 → 7: Added isDeleted/deletedAt to groups cache
 * 7 → 8: Added friendCode/timezone to users cache
 * 8 → 9: Added pending_operations table for Wave 2C durable sync queue
 * 9 → 10: Added expense_payers and expense_splits tables for Wave 2D-2B full offline edit
 * 10 → 11: Added group_members table for offline member caching (Wave 2D-4A)
 */
@Database(
    entities = [
        GroupEntity::class,
        ExpenseEntity::class,
        ExpensePayerEntity::class,
        ExpenseSplitEntity::class,
        BalanceEntity::class,
        UserEntity::class,
        PendingActionEntity::class,
        InvitedFriendEntity::class,
        FriendEntity::class,
        GroupMemberEntity::class,
        NotificationEntity::class,
        PendingOperationEntity::class,
    ],
    version = 14,
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
    abstract fun groupMemberDao(): GroupMemberDao
    abstract fun notificationDao(): NotificationDao
    abstract fun expensePayerDao(): ExpensePayerDao
    abstract fun expenseSplitDao(): ExpenseSplitDao
    abstract fun pendingOperationDao(): PendingOperationDao

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
                db.execSQL(
                    """
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
                    """.trimIndent()
                )
                db.execSQL("DROP TABLE IF EXISTS balances")
                db.execSQL(
                    """
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
                    """.trimIndent()
                )
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

        /** 8 → 9: Add pending_operations table for Wave 2C durable sync queue. */
        val MIGRATION_8_9 = object : Migration(8, 9) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS pending_operations (
                        operationId          TEXT    NOT NULL PRIMARY KEY,
                        userId               TEXT    NOT NULL,
                        operationType        TEXT    NOT NULL,
                        endpoint             TEXT    NOT NULL,
                        method               TEXT    NOT NULL,
                        requestBodyJson      TEXT,
                        idempotencyKey       TEXT    NOT NULL,
                        status               TEXT    NOT NULL,
                        retryCount           INTEGER NOT NULL DEFAULT 0,
                        createdAt            INTEGER NOT NULL,
                        updatedAt            INTEGER NOT NULL,
                        lastAttemptAt        INTEGER,
                        lastError            TEXT,
                        dependsOnOperationId TEXT,
                        localResourceId      TEXT,
                        serverResourceId     TEXT
                    )
                    """.trimIndent()
                )

                db.execSQL(
                    """
                    CREATE INDEX IF NOT EXISTS index_pending_operations_status
                    ON pending_operations(status)
                    """.trimIndent()
                )

                db.execSQL(
                    """
                    CREATE INDEX IF NOT EXISTS index_pending_operations_userId
                    ON pending_operations(userId)
                    """.trimIndent()
                )

                db.execSQL(
                    """
                    CREATE UNIQUE INDEX IF NOT EXISTS index_pending_operations_idempotencyKey
                    ON pending_operations(idempotencyKey)
                    """.trimIndent()
                )
            }
        }

        /** 9 → 10: Add expense_payers and expense_splits for full offline edit cache. */
        val MIGRATION_9_10 = object : Migration(9, 10) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS expense_payers (
                        id         TEXT    NOT NULL PRIMARY KEY,
                        expenseId  TEXT    NOT NULL,
                        userId     TEXT    NOT NULL,
                        fullName   TEXT    NOT NULL,
                        amountPaid REAL    NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    CREATE INDEX IF NOT EXISTS index_expense_payers_expenseId
                    ON expense_payers(expenseId)
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS expense_splits (
                        id         TEXT    NOT NULL PRIMARY KEY,
                        expenseId  TEXT    NOT NULL,
                        userId     TEXT    NOT NULL,
                        fullName   TEXT    NOT NULL,
                        amountOwed REAL    NOT NULL,
                        percentage REAL,
                        shares     INTEGER,
                        isSettled  INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    CREATE INDEX IF NOT EXISTS index_expense_splits_expenseId
                    ON expense_splits(expenseId)
                    """.trimIndent()
                )
            }
        }




        /**
         * 13 → 14: Add cacheScope column to balances; update composite PK to include it.
         *
         * Balances are cache-only. Old rows are discarded rather than copied because
         * the v13 table contains mixed-scope rows (getAllBalances, getNetBalanceWithUser,
         * getBreakdownWithUser, getGroupBalances) that cannot be reliably reclassified.
         * Copying them all as ALL_BALANCES would carry the double-counting bug forward.
         *
         * CacheWarmupCoordinator and repositories will refill scoped rows on next
         * network access. A brief empty-cache state is safer than wrong financial data.
         */
        val MIGRATION_13_14 = object : Migration(13, 14) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Drop old mixed-scope table and recreate clean with cacheScope in PK.
                db.execSQL("DROP TABLE IF EXISTS balances")
                db.execSQL("""
                    CREATE TABLE balances (
                        userId            TEXT    NOT NULL,
                        otherUserId       TEXT    NOT NULL,
                        otherUserName     TEXT    NOT NULL,
                        amount            REAL    NOT NULL,
                        currency          TEXT    NOT NULL,
                        groupId           TEXT    NOT NULL,
                        groupName         TEXT,
                        cacheScope        TEXT    NOT NULL DEFAULT 'ALL_BALANCES',
                        cachedAt          INTEGER NOT NULL DEFAULT 0,
                        groupLastActivity TEXT,
                        PRIMARY KEY (userId, otherUserId, currency, groupId, cacheScope)
                    )
                """.trimIndent())
            }
        }

        /** 12 → 13: Add notifications table for offline Activity restore access. */
        val MIGRATION_12_13 = object : Migration(12, 13) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS notifications (
                        id             TEXT    NOT NULL PRIMARY KEY,
                        title          TEXT    NOT NULL,
                        message        TEXT    NOT NULL,
                        type           TEXT    NOT NULL,
                        referenceId    TEXT,
                        referenceType  TEXT,
                        isRead         INTEGER NOT NULL,
                        createdAt      TEXT    NOT NULL,
                        groupId        TEXT,
                        groupName      TEXT,
                        isGroupDeleted INTEGER NOT NULL,
                        isUserMember   INTEGER NOT NULL,
                        cachedAt       INTEGER NOT NULL DEFAULT 0
                    )
                    """.trimIndent()
                )
            }
        }

        /** 11 → 12: Add otherUserId to expenses for direct friend expense cache (Wave 2D-Final). */
        val MIGRATION_11_12 = object : Migration(11, 12) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE expenses ADD COLUMN otherUserId TEXT")
            }
        }

        /** 10 → 11: Add group_members table for offline member caching. */
        val MIGRATION_10_11 = object : Migration(10, 11) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS group_members (
                        id               TEXT NOT NULL PRIMARY KEY,
                        groupId          TEXT NOT NULL,
                        userId           TEXT NOT NULL,
                        fullName         TEXT NOT NULL,
                        email            TEXT NOT NULL,
                        profilePictureUrl TEXT,
                        joinedAt         TEXT NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    CREATE INDEX IF NOT EXISTS index_group_members_groupId
                    ON group_members(groupId)
                    """.trimIndent()
                )
            }
        }
    }
}