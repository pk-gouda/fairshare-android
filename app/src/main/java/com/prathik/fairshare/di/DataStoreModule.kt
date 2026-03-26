package com.prathik.fairshare.di

import android.content.Context
import com.prathik.fairshare.data.local.EncryptedTokenStore
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Provides EncryptedTokenStore as a singleton.
 * Tokens are AES256-GCM encrypted at rest using Android Keystore.
 */
@Module
@InstallIn(SingletonComponent::class)
object DataStoreModule {

    @Provides
    @Singleton
    fun provideEncryptedTokenStore(
        @ApplicationContext context: Context,
    ): EncryptedTokenStore = EncryptedTokenStore(context)
}
