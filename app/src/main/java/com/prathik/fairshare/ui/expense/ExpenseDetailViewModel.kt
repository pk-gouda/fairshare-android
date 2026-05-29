package com.prathik.fairshare.ui.expense

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.prathik.fairshare.data.local.EncryptedTokenStore
import com.prathik.fairshare.data.model.mapper.toDomain
import com.prathik.fairshare.data.network.api.ExpenseApiService
import com.prathik.fairshare.data.network.safeApiCall
import com.prathik.fairshare.data.local.PendingOperationEntity
import com.prathik.fairshare.domain.repository.ExpenseRepository
import com.prathik.fairshare.data.sync.OperationType
import com.prathik.fairshare.data.sync.ExpenseMutationCacheRefresher
import com.prathik.fairshare.data.sync.PendingOperationRepository
import com.prathik.fairshare.data.sync.SyncWorker
import com.prathik.fairshare.domain.model.ApiResult
import com.prathik.fairshare.domain.model.Expense
import com.prathik.fairshare.domain.model.ExpenseChangeLog
import com.prathik.fairshare.domain.model.ExpenseComment
import com.prathik.fairshare.domain.model.ExpenseItem
import com.prathik.fairshare.domain.usecase.expense.DeleteExpenseUseCase
import com.prathik.fairshare.domain.usecase.expense.GetExpenseUseCase
import com.prathik.fairshare.domain.usecase.expense.RestoreExpenseUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Expense detail ViewModel.
 *
 * Delete and restore use try-first queue semantics:
 * - Generate a stable idempotencyKey before the network call.
 * - If the online request succeeds, no PendingOperation is created.
 * - If the backend returns an HTTP/server/validation error, no PendingOperation is created.
 * - Only on ApiResult.NetworkError is a PendingOperation enqueued with the same
 *   idempotencyKey so SyncWorker replay is deduplicated by the backend.
 */
@HiltViewModel
class ExpenseDetailViewModel @Inject constructor(
    private val getExpenseUseCase    : GetExpenseUseCase,
    private val deleteExpenseUseCase  : DeleteExpenseUseCase,
    private val restoreExpenseUseCase : RestoreExpenseUseCase,
    private val mutationCacheRefresher: ExpenseMutationCacheRefresher,
    private val expenseApiService    : ExpenseApiService,
    private val tokenStore           : EncryptedTokenStore,
    private val expenseRepository        : ExpenseRepository,
    private val pendingOperationRepository: PendingOperationRepository,
    @ApplicationContext private val appContext: Context,
    savedStateHandle                 : SavedStateHandle,
) : ViewModel() {

    val expenseId    : String  = savedStateHandle.get<String>("expenseId") ?: ""
    val currentUserId: String? = tokenStore.getUserId()

    private val _expenseState = MutableStateFlow<ExpenseDetailUiState>(ExpenseDetailUiState.Loading)
    val expenseState: StateFlow<ExpenseDetailUiState> = _expenseState.asStateFlow()

    private val _actionState = MutableStateFlow<ExpenseActionState>(ExpenseActionState.Idle)
    val actionState: StateFlow<ExpenseActionState> = _actionState.asStateFlow()

    private val _items = MutableStateFlow<List<ExpenseItem>>(emptyList())
    val items: StateFlow<List<ExpenseItem>> = _items.asStateFlow()

    private val _itemsLoading = MutableStateFlow(false)
    val itemsLoading: StateFlow<Boolean> = _itemsLoading.asStateFlow()

    private val _changeLog = MutableStateFlow<List<ExpenseChangeLog>>(emptyList())
    val changeLog: StateFlow<List<ExpenseChangeLog>> = _changeLog.asStateFlow()

    private val _changeLogLoading = MutableStateFlow(false)
    val changeLogLoading: StateFlow<Boolean> = _changeLogLoading.asStateFlow()

    // ── Comments ──────────────────────────────────────────────────────────────

    private val _comments = MutableStateFlow<List<ExpenseComment>>(emptyList())
    val comments: StateFlow<List<ExpenseComment>> = _comments.asStateFlow()

    private val _commentsLoading = MutableStateFlow(false)
    val commentsLoading: StateFlow<Boolean> = _commentsLoading.asStateFlow()

    private val _commentsError = MutableStateFlow(false)
    val commentsError: StateFlow<Boolean> = _commentsError.asStateFlow()

    private val _commentText = MutableStateFlow("")
    val commentText: StateFlow<String> = _commentText.asStateFlow()

    private val _commentPosting = MutableStateFlow(false)
    val commentPosting: StateFlow<Boolean> = _commentPosting.asStateFlow()

    private val _commentError = MutableStateFlow<String?>(null)
    val commentError: StateFlow<String?> = _commentError.asStateFlow()

    fun clearCommentError() { _commentError.value = null }

    /**
     * Wave 2D-4: live pending operation for this expense.
     * Null when no active (non-SYNCED/CANCELLED) operation exists.
     * Drives the sync-status banner in ExpenseDetailScreen.
     */
    val pendingOp: StateFlow<PendingOperationEntity?> =
        pendingOperationRepository.observeForExpense(expenseId)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    /**
     * True when this expense is a local-only placeholder that hasn't synced yet
     * (its ID is the localResourceId of an active CREATE_EXPENSE pending op).
     * Edit/Delete/Restore are disabled while true — the placeholder UUID must
     * never be sent to the backend as a real expense ID.
     */
    val isLocalPendingCreate: StateFlow<Boolean> =
        pendingOperationRepository.observeForExpense(expenseId)
            .map { op ->
                // Strict check — only true for a still-unsynced local placeholder:
                // 1. operationType is CREATE_EXPENSE
                // 2. localResourceId == expenseId (this IS the placeholder)
                // 3. serverResourceId == null (backend hasn't confirmed it yet)
                // 4. status is active (PENDING/SYNCING/FAILED_RETRYABLE)
                // The DAO already excludes SYNCED/CANCELLED, so check (1)-(3) only.
                op != null
                        && op.operationType == "CREATE_EXPENSE"
                        && op.localResourceId == expenseId
                        && op.serverResourceId == null
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    /**
     * Wave 2D-4: manual retry.
     * Resets the operation to PENDING (reusing the original idempotencyKey)
     * and triggers an immediate SyncWorker pass.
     */
    fun retryPendingOp(operationId: String) {
        viewModelScope.launch {
            pendingOperationRepository.resetForRetry(operationId)
            SyncWorker.triggerImmediateSync(appContext)
        }
    }

    init { loadExpense(); loadComments() }

    private var hasLoadedOnce = false

    fun loadExpense(silent: Boolean = false) {
        viewModelScope.launch {
            // Step 1: Render from Room immediately — no network wait.
            val cached = expenseRepository.getCachedExpenseWithDetail(expenseId)
                ?: expenseRepository.getCachedExpense(expenseId)
            if (cached != null) {
                _expenseState.value = ExpenseDetailUiState.Success(cached)
                hasLoadedOnce = true
            } else if (!silent && !hasLoadedOnce) {
                // Truly nothing cached — show skeleton, not spinner
                _expenseState.value = ExpenseDetailUiState.Loading
            }

            // Step 2: Network fetch — updates state only on success.
            when (val result = getExpenseUseCase(expenseId)) {
                is ApiResult.Success -> {
                    _expenseState.value = ExpenseDetailUiState.Success(result.data)
                    hasLoadedOnce = true
                    if (_items.value.isEmpty()) loadItems()
                    loadChangeLog()
                }
                is ApiResult.NotFound -> _expenseState.value = ExpenseDetailUiState.Deleted
                is ApiResult.NetworkError -> {
                    // Keep cached data visible. Only show error if truly nothing loaded.
                    if (!hasLoadedOnce) _expenseState.value =
                        ExpenseDetailUiState.Error(
                            "Expense details aren't saved on this device yet. Reconnect to view this expense.",
                            isNetwork = false,
                        )
                }
                else -> {
                    if (!hasLoadedOnce) _expenseState.value =
                        ExpenseDetailUiState.Error("Failed to load expense.", false)
                }
            }
        }
    }

    /**
     * Silent resume refresh — does not reset hasLoadedOnce, does not clear items,
     * does not flash Loading. Called by the RESUMED lifecycle effect.
     */
    fun forceRefresh() {
        // Guard: skip if initial load is still running (hasLoadedOnce not yet set
        // and state is Loading — the init load will handle it).
        if (!hasLoadedOnce && _expenseState.value is ExpenseDetailUiState.Loading) return
        loadExpense(silent = true)
        loadComments()
    }

    fun loadItems() {
        viewModelScope.launch {
            _itemsLoading.value = true
            val result = safeApiCall { expenseApiService.getExpenseItems(expenseId) }
            if (result is ApiResult.Success) {
                _items.value = result.data.map { it.toDomain() }
            }
            _itemsLoading.value = false
        }
    }

    fun loadChangeLog() {
        viewModelScope.launch {
            _changeLogLoading.value = true
            val result = safeApiCall { expenseApiService.getChangeLog(expenseId) }
            if (result is ApiResult.Success) {
                _changeLog.value = result.data.map { entry ->
                    ExpenseChangeLog(
                        changedById   = entry.changedById,
                        changedByName = entry.changedByName,
                        changedAt     = entry.changedAt,
                        changes       = entry.changes.map { fc ->
                            ExpenseChangeLog.FieldChange(
                                fieldName = fc.fieldName,
                                oldValue  = fc.oldValue,
                                newValue  = fc.newValue,
                            )
                        }
                    )
                }
            }
            _changeLogLoading.value = false
        }
    }

    /**
     * Try-first delete: generates a stable idempotencyKey before the network call,
     * only enqueues a DELETE_EXPENSE PendingOperation on ApiResult.NetworkError.
     * Online success and HTTP errors do not create a pending operation so no
     * sync banner appears for foreground failures.
     * The same idempotencyKey is stored in the queue row so SyncWorker replay
     * is deduplicated by the backend.
     */
    fun deleteExpense() {
        val userId = currentUserId ?: run {
            _actionState.value = ExpenseActionState.Error("Not logged in.")
            return
        }

        viewModelScope.launch {
            _actionState.value = ExpenseActionState.Loading

            // Try-first: generate key before call, enqueue only on NetworkError.
            val idempotencyKey = java.util.UUID.randomUUID().toString()

            when (val result = deleteExpenseUseCase(expenseId, idempotencyKey)) {
                is ApiResult.Success -> {
                    // Online success — no pending operation created.
                    val preDelete = (_expenseState.value as? ExpenseDetailUiState.Success)?.expense
                    val gId = preDelete?.groupId
                    val participants = ((preDelete?.payers?.map { it.userId } ?: emptyList()) +
                            (preDelete?.splits?.map { it.userId } ?: emptyList())).toSet()
                    mutationCacheRefresher.refreshAfterDeleteSuccess(
                        expenseId      = expenseId,
                        groupId        = gId,
                        currentUserId  = userId,
                        participantIds = participants,
                    )
                    _actionState.value = ExpenseActionState.Deleted
                }

                is ApiResult.NetworkError -> {
                    val enqueued = pendingOperationRepository.enqueue(
                        userId           = userId,
                        operationType    = OperationType.DELETE_EXPENSE,
                        endpoint         = "/api/expenses/$expenseId",
                        method           = "DELETE",
                        requestBodyJson  = null,
                        localResourceId  = expenseId,
                        serverResourceId = expenseId,
                        idempotencyKey   = idempotencyKey,
                    )
                    pendingOperationRepository.markRetryable(
                        enqueued.operationId, result.exception.message ?: "Network error"
                    )
                    expenseRepository.updateLocalDeletedStatus(expenseId, true)
                    SyncWorker.triggerImmediateSync(appContext)
                    android.util.Log.w("ExpenseDetailVM", "Offline delete queued: ${enqueued.operationId}")
                    _actionState.value = ExpenseActionState.DeletedOffline
                }

                is ApiResult.Forbidden -> {
                    _actionState.value = ExpenseActionState.Error(result.message)
                }

                is ApiResult.NotFound -> {
                    // Already deleted on server — refresh caches, no pending op needed.
                    val preDelete = (_expenseState.value as? ExpenseDetailUiState.Success)?.expense
                    val participants = ((preDelete?.payers?.map { it.userId } ?: emptyList()) +
                            (preDelete?.splits?.map { it.userId } ?: emptyList())).toSet()
                    mutationCacheRefresher.refreshAfterDeleteSuccess(
                        expenseId      = expenseId,
                        groupId        = preDelete?.groupId,
                        currentUserId  = userId,
                        participantIds = participants,
                    )
                    _actionState.value = ExpenseActionState.Deleted
                }

                else -> {
                    _actionState.value = ExpenseActionState.Error("Failed to delete expense.")
                }
            }
        }
    }
    /**
     * Try-first restore: same pattern as deleteExpense — idempotencyKey generated
     * before the call, enqueue only on NetworkError.
     */
    fun restoreExpense() {
        val userId = currentUserId ?: run {
            _actionState.value = ExpenseActionState.Error("Not logged in.")
            return
        }

        viewModelScope.launch {
            _actionState.value = ExpenseActionState.Loading

            val idempotencyKey = java.util.UUID.randomUUID().toString()

            when (val result = restoreExpenseUseCase(expenseId, idempotencyKey)) {
                is ApiResult.Success -> {
                    // Online success — no pending operation was created.
                    val restored = result.data
                    val preRestoreCtx = (_expenseState.value as? ExpenseDetailUiState.Success)?.expense
                    val responseParticipants = ((restored.payers?.map { it.userId } ?: emptyList()) +
                            (restored.splits?.map { it.userId } ?: emptyList())).toSet()
                    val participants = responseParticipants.ifEmpty {
                        ((preRestoreCtx?.payers?.map { it.userId } ?: emptyList()) +
                                (preRestoreCtx?.splits?.map { it.userId } ?: emptyList())).toSet()
                    }
                    mutationCacheRefresher.refreshAfterRestoreSuccess(
                        expense        = restored,
                        groupId        = restored.groupId ?: preRestoreCtx?.groupId,
                        currentUserId  = userId,
                        participantIds = participants,
                    )
                    _actionState.value = ExpenseActionState.Restored
                }

                is ApiResult.NetworkError -> {
                    val enqueued = pendingOperationRepository.enqueue(
                        userId          = userId,
                        operationType   = OperationType.RESTORE_EXPENSE,
                        endpoint        = "/api/expenses/$expenseId/restore",
                        method          = "POST",
                        requestBodyJson = null,
                        localResourceId = expenseId,
                        serverResourceId = expenseId,
                        idempotencyKey  = idempotencyKey,
                    )
                    pendingOperationRepository.markRetryable(
                        enqueued.operationId, result.exception.message ?: "Network error"
                    )
                    expenseRepository.updateLocalDeletedStatus(expenseId, false)
                    SyncWorker.triggerImmediateSync(appContext)
                    android.util.Log.w("ExpenseDetailVM", "Offline restore queued: ${enqueued.operationId}")
                    _actionState.value = ExpenseActionState.RestoredOffline
                }

                is ApiResult.Forbidden -> {
                    _actionState.value = ExpenseActionState.Error(result.message)
                }

                else -> {
                    _actionState.value = ExpenseActionState.Error("Failed to restore expense.")
                }
            }
        }
    }

    fun resetActionState() { _actionState.value = ExpenseActionState.Idle }

    // ── Comment methods ───────────────────────────────────────────────────────

    fun loadComments() {
        viewModelScope.launch {
            _commentsLoading.value = true
            _commentsError.value   = false
            when (val result = expenseRepository.getExpenseComments(expenseId)) {
                is ApiResult.Success    -> {
                    _comments.value      = result.data.sortedBy { it.createdAt }
                    _commentsError.value = false
                }
                else                    -> _commentsError.value = true
            }
            _commentsLoading.value = false
        }
    }

    fun onCommentTextChanged(text: String) {
        _commentText.value = text
    }

    fun postComment() {
        val text = _commentText.value.trim()
        if (text.isBlank() || _commentPosting.value) return
        viewModelScope.launch {
            _commentPosting.value = true
            when (val result = expenseRepository.addExpenseComment(expenseId, text)) {
                is ApiResult.Success -> {
                    _comments.value    = (_comments.value + result.data).sortedBy { it.createdAt }
                    _commentText.value = ""
                }
                is ApiResult.NetworkError -> _commentError.value = "You're offline. Check your connection and try again."
                else                      -> _commentError.value = "Couldn't post comment. Please try again."
            }
            _commentPosting.value = false
        }
    }

    fun deleteComment(commentId: String) {
        viewModelScope.launch {
            when (expenseRepository.deleteExpenseComment(commentId)) {
                is ApiResult.Success -> {
                    _comments.value = _comments.value.filter { it.id != commentId }
                }
                else -> _commentError.value = "Couldn't delete comment. Please try again."
            }
        }
    }
}

sealed class ExpenseDetailUiState {
    object Loading : ExpenseDetailUiState()
    object Deleted : ExpenseDetailUiState()
    data class Success(val expense: Expense) : ExpenseDetailUiState()
    data class Error(val message: String, val isNetwork: Boolean) : ExpenseDetailUiState()
}

sealed class ExpenseActionState {
    object Idle          : ExpenseActionState()
    object Loading       : ExpenseActionState()
    object Deleted       : ExpenseActionState()
    object Restored      : ExpenseActionState()
    /** Wave 2D-3: queued offline — SyncWorker will send DELETE_EXPENSE when network returns. */
    object DeletedOffline  : ExpenseActionState()
    /** Wave 2D-3: queued offline — SyncWorker will send RESTORE_EXPENSE when network returns. */
    object RestoredOffline : ExpenseActionState()
    data class Error(val message: String) : ExpenseActionState()
}