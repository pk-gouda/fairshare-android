package com.prathik.fairshare.data.sync

/**
 * Why a sync operation was triggered.
 * Used for structured logging and to tune sync behavior (e.g. throttling).
 */
enum class SyncReason {
    /** First sync after login. */
    LOGIN,
    /** App came back to foreground (MainActivity.onStart). */
    APP_FOREGROUND,
    /** User pulled-to-refresh on a screen. */
    MANUAL_REFRESH,
    /** Immediately after a successful online expense mutation. */
    MUTATION_SUCCESS,
    /** SyncWorker replayed an offline-queued operation successfully. */
    SYNC_WORKER_SUCCESS,
    /** Periodic background warmup (WorkManager). */
    CACHE_WARMUP,
}