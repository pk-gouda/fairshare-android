package com.prathik.fairshare.data.sync

import android.util.Log
import com.prathik.fairshare.data.local.EncryptedTokenStore
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Thin wrapper that delegates to [FairShareSyncManager].
 *
 * Previously owned all warmup logic. That logic now lives in FairShareSyncManager
 * so it is shared with mutation paths and manual refresh calls.
 *
 * Retained so call-sites in MainActivity and AuthViewModel do not need to change
 * in this patch. Call-sites may migrate directly to FairShareSyncManager later.
 */
@Singleton
class CacheWarmupCoordinator @Inject constructor(
    private val tokenStore  : EncryptedTokenStore,
    private val syncManager : FairShareSyncManager,
) {
    private val tag = "CacheWarmup"

    fun warmupIfNeeded() {
        if (!tokenStore.isLoggedIn()) return
        Log.d(tag, "warmupIfNeeded → delegating to FairShareSyncManager")
        syncManager.syncAccountInBackground(SyncReason.APP_FOREGROUND)
    }
}