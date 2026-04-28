package com.prathik.fairshare.di

import android.content.Context
import androidx.room.Room
import com.prathik.fairshare.data.local.BalanceDao
import com.prathik.fairshare.data.local.ExpenseDao
import com.prathik.fairshare.data.local.GroupMemberDao
import com.prathik.fairshare.data.local.NotificationDao
import com.prathik.fairshare.data.local.ExpensePayerDao
import com.prathik.fairshare.data.local.ExpenseSplitDao
import com.prathik.fairshare.data.local.FairShareDatabase
import com.prathik.fairshare.data.local.GroupDao
import com.prathik.fairshare.data.local.FriendDao
import com.prathik.fairshare.data.local.InvitedFriendDao
import com.prathik.fairshare.data.local.PendingActionDao
import com.prathik.fairshare.data.local.PendingBalanceImpactDao
import com.prathik.fairshare.data.local.PendingOperationDao
import com.prathik.fairshare.data.local.UserDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Provides the Room database and all DAOs as singletons.
 *
 * Database name: fairshare_db
 * Schema changes during beta: fallbackToDestructiveMigration() wipes and recreates.
 * Before production: replace with proper Migration objects.
 */
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideFairShareDatabase(
        @ApplicationContext context: Context,
    ): FairShareDatabase =
        Room.databaseBuilder(
            context,
            FairShareDatabase::class.java,
            "fairshare_db",
        )
            .addMigrations(
                FairShareDatabase.MIGRATION_1_2,
                FairShareDatabase.MIGRATION_2_3,
                FairShareDatabase.MIGRATION_3_4,
                FairShareDatabase.MIGRATION_4_5,
                FairShareDatabase.MIGRATION_5_6,
                FairShareDatabase.MIGRATION_6_7,
                FairShareDatabase.MIGRATION_7_8,
                FairShareDatabase.MIGRATION_8_9,
                FairShareDatabase.MIGRATION_9_10,
                FairShareDatabase.MIGRATION_10_11,
                FairShareDatabase.MIGRATION_11_12,
                FairShareDatabase.MIGRATION_12_13,
                FairShareDatabase.MIGRATION_13_14,
                FairShareDatabase.MIGRATION_14_15,
                FairShareDatabase.MIGRATION_15_16,
            )
            .build()

    @Provides
    @Singleton
    fun provideGroupDao(database: FairShareDatabase): GroupDao =
        database.groupDao()

    @Provides
    @Singleton
    fun provideExpenseDao(database: FairShareDatabase): ExpenseDao =
        database.expenseDao()

    @Provides
    @Singleton
    fun provideBalanceDao(database: FairShareDatabase): BalanceDao =
        database.balanceDao()

    @Provides
    @Singleton
    fun provideUserDao(database: FairShareDatabase): UserDao =
        database.userDao()

    @Provides
    @Singleton
    fun providePendingActionDao(database: FairShareDatabase): PendingActionDao =
        database.pendingActionDao()

    @Provides
    @Singleton
    fun provideInvitedFriendDao(database: FairShareDatabase): InvitedFriendDao =
        database.invitedFriendDao()

    @Provides
    @Singleton
    fun provideFriendDao(database: FairShareDatabase): FriendDao =
        database.friendDao()

    @Provides
    @Singleton
    fun providePendingOperationDao(database: FairShareDatabase): PendingOperationDao =
        database.pendingOperationDao()

    @Provides
    @Singleton
    fun providePendingBalanceImpactDao(database: FairShareDatabase): PendingBalanceImpactDao =
        database.pendingBalanceImpactDao()

    @Provides
    @Singleton
    fun provideGroupMemberDao(database: FairShareDatabase): GroupMemberDao =
        database.groupMemberDao()


    @Provides
    @Singleton
    fun provideNotificationDao(database: FairShareDatabase): NotificationDao =
        database.notificationDao()

    @Provides
    @Singleton
    fun provideExpensePayerDao(database: FairShareDatabase): ExpensePayerDao =
        database.expensePayerDao()

    @Provides
    @Singleton
    fun provideExpenseSplitDao(database: FairShareDatabase): ExpenseSplitDao =
        database.expenseSplitDao()
}