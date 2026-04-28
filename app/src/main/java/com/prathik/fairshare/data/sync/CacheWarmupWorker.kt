package com.prathik.fairshare.data.sync

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.prathik.fairshare.data.local.EncryptedTokenStore
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.util.concurrent.TimeUnit

/**
 * Periodic background warmup worker.
 * All warmup logic lives in [FairShareSyncManager] — this worker is a thin trigger.
 */
@HiltWorker
class CacheWarmupWorker @AssistedInject constructor(
    @Assisted private val appContext: Context,
    @Assisted params: WorkerParameters,
    private val tokenStore  : EncryptedTokenStore,
    private val syncManager : FairShareSyncManager,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        if (!tokenStore.isLoggedIn()) return Result.success()
        return try {
            syncManager.syncAccountCore(SyncReason.CACHE_WARMUP)
            Result.success()
        } catch (_: Exception) {
            Result.success()
        }
    }

    companion object {

        private const val WORK_NAME_PERIODIC = "fairshare_cache_warmup_periodic"
        private const val WORK_NAME_ONCE     = "fairshare_cache_warmup_once"

        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<CacheWarmupWorker>(6, TimeUnit.HOURS)
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build()
                )
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.MINUTES)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME_PERIODIC,
                ExistingPeriodicWorkPolicy.KEEP,
                request,
            )
        }

        fun triggerNow(context: Context) {
            val request = OneTimeWorkRequestBuilder<CacheWarmupWorker>()
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build()
                )
                .build()

            WorkManager.getInstance(context).enqueueUniqueWork(
                WORK_NAME_ONCE,
                ExistingWorkPolicy.KEEP,
                request,
            )
        }
    }
}