package com.prathik.fairshare.di

import com.prathik.fairshare.data.repository.impl.AuthRepositoryImpl
import com.prathik.fairshare.data.repository.impl.BalanceRepositoryImpl
import com.prathik.fairshare.data.repository.impl.ExpenseRepositoryImpl
import com.prathik.fairshare.data.repository.impl.FriendRepositoryImpl
import com.prathik.fairshare.data.repository.impl.GroupRepositoryImpl
import com.prathik.fairshare.data.repository.impl.ImportRepositoryImpl
import com.prathik.fairshare.data.repository.impl.NotificationRepositoryImpl
import com.prathik.fairshare.data.repository.impl.ReceiptRepositoryImpl
import com.prathik.fairshare.data.repository.impl.SettlementRepositoryImpl
import com.prathik.fairshare.data.repository.impl.UserRepositoryImpl
import com.prathik.fairshare.domain.repository.AuthRepository
import com.prathik.fairshare.domain.repository.BalanceRepository
import com.prathik.fairshare.domain.repository.ExpenseRepository
import com.prathik.fairshare.domain.repository.FriendRepository
import com.prathik.fairshare.domain.repository.GroupRepository
import com.prathik.fairshare.domain.repository.ImportRepository
import com.prathik.fairshare.domain.repository.NotificationRepository
import com.prathik.fairshare.domain.repository.ReceiptRepository
import com.prathik.fairshare.domain.repository.SettlementRepository
import com.prathik.fairshare.domain.repository.UserRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Binds all repository interfaces to their implementations.
 * ViewModels and use cases inject the interface — Hilt provides the impl.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds @Singleton
    abstract fun bindAuthRepository(impl: AuthRepositoryImpl): AuthRepository

    @Binds @Singleton
    abstract fun bindGroupRepository(impl: GroupRepositoryImpl): GroupRepository

    @Binds @Singleton
    abstract fun bindExpenseRepository(impl: ExpenseRepositoryImpl): ExpenseRepository

    @Binds @Singleton
    abstract fun bindBalanceRepository(impl: BalanceRepositoryImpl): BalanceRepository

    @Binds @Singleton
    abstract fun bindSettlementRepository(impl: SettlementRepositoryImpl): SettlementRepository

    @Binds @Singleton
    abstract fun bindFriendRepository(impl: FriendRepositoryImpl): FriendRepository

    @Binds @Singleton
    abstract fun bindNotificationRepository(impl: NotificationRepositoryImpl): NotificationRepository

    @Binds @Singleton
    abstract fun bindUserRepository(impl: UserRepositoryImpl): UserRepository

    @Binds @Singleton
    abstract fun bindReceiptRepository(impl: ReceiptRepositoryImpl): ReceiptRepository

    @Binds @Singleton
    abstract fun bindImportRepository(impl: ImportRepositoryImpl): ImportRepository
}
