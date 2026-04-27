package com.prathik.fairshare

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.prathik.fairshare.data.sync.CacheWarmupWorker
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class FairShareApplication : Application(), Configuration.Provider {

    @Inject lateinit var workerFactory: HiltWorkerFactory

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun onCreate() {
        super.onCreate()
        // Schedule periodic background warmup (every 6 h while network is available).
        // Immediate foreground warmup is triggered from MainActivity.onStart() via
        // CacheWarmupCoordinator, which requires no extra lifecycle dependency.
        CacheWarmupWorker.schedule(this)
    }
}