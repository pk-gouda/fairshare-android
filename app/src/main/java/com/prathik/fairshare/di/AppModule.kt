package com.prathik.fairshare.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import javax.inject.Qualifier
import javax.inject.Singleton

/**
 * Qualifier for a CoroutineScope that lives for the entire application lifetime.
 *
 * Use this instead of a raw CoroutineScope(Dispatchers.IO) anywhere a background
 * job must outlive a single ViewModel or repository call, but must still be
 * managed (not a fire-and-forget leak).
 *
 * SupervisorJob: a failing child job does not cancel the scope or its siblings.
 * Dispatchers.Default: CPU-bound default; individual launches can override with
 *   Dispatchers.IO for blocking I/O work.
 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class ApplicationScope

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    @ApplicationScope
    fun provideApplicationScope(): CoroutineScope =
        CoroutineScope(SupervisorJob() + Dispatchers.Default)
}