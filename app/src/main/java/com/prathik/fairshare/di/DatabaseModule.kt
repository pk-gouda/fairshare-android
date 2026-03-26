package com.prathik.fairshare.di

import android.content.Context
import androidx.room.Room
import com.prathik.fairshare.data.local.BalanceDao
import com.prathik.fairshare.data.local.ExpenseDao
import com.prathik.fairshare.data.local.FairShareDatabase
import com.prathik.fairshare.data.local.GroupDao
import com.prathik.fairshare.data.local.PendingActionDao
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
 * When schema changes: increment version in FairShareDatabase and add Migration.
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
        ).build()

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
}
