package com.prathik.fairshare

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import com.prathik.fairshare.data.local.EncryptedTokenStore
import com.prathik.fairshare.data.sync.CacheWarmupWorker
import androidx.work.Configuration
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class FairShareApplication : Application(), Configuration.Provider {

    @Inject lateinit var workerFactory: HiltWorkerFactory
    @Inject lateinit var tokenStore: EncryptedTokenStore

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun onCreate() {
        super.onCreate()
        // Schedule periodic warmup (runs every 6 h while network is available).
        CacheWarmupWorker.schedule(this)
        // If the user is already logged in (app restart / foreground),
        // trigger an immediate one-off warmup so offline data is ready now.
        // Uses KEEP policy — duplicate calls (login + app start) are collapsed.
        if (tokenStore.isLoggedIn()) {
            CacheWarmupWorker.triggerNow(this)
        }
    }
}