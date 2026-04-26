package com.prathik.fairshare.ui.expense

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.prathik.fairshare.data.local.EncryptedTokenStore
import com.prathik.fairshare.data.model.mapper.toDomain
import com.prathik.fairshare.data.network.api.ExpenseApiService
import com.prathik.fairshare.data.network.safeApiCall
import com.prathik.fairshare.data.sync.OperationType
import com.prathik.fairshare.data.sync.PendingOperationRepository
import com.prathik.fairshare.data.sync.SyncWorker
import com.prathik.fairshare.domain.model.ApiResult
import com.prathik.fairshare.domain.model.Expense
import com.prathik.fairshare.domain.model.ExpenseChangeLog
import com.prathik.fairshare.domain.model.ExpenseItem
import com.prathik.fairshare.domain.usecase.expense.DeleteExpenseUseCase
import com.prathik.fairshare.domain.usecase.expense.GetExpenseUseCase
import com.prathik.fairshare.domain.usecase.expense.RestoreExpenseUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Wave 2D-3: delete and restore are now queue-backed.
 * - A PendingOperation is enqueued before the network call.
 * - The stable idempotencyKey is sent as the Idempotency-Key header so the backend
 *   cannot apply the balance reversal/re-application twice on retry.
 * - On NetworkError the operation is marked FAILED_RETRYABLE and SyncWorker
 *   will replay it when connectivity returns.
 * - The screen navigates back after queueing; the list will reflect the correct
 *   state after sync completes.
 */
@HiltViewModel
class ExpenseDetailViewModel @Inject constructor(
    private val getExpenseUseCase    : GetExpenseUseCase,
    private val deleteExpenseUseCase  : DeleteExpenseUseCase,
    private val restoreExpenseUseCase : RestoreExpenseUseCase,
    private val expenseApiService    : ExpenseApiService,
    private val tokenStore           : EncryptedTokenStore,
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

    init { loadExpense() }

    private var hasLoadedOnce = false

    fun loadExpense() {
        viewModelScope.launch {
            if (!hasLoadedOnce) _expenseState.value = ExpenseDetailUiState.Loading
            when (val result = getExpenseUseCase(expenseId)) {
                is ApiResult.Success -> {
                    _expenseState.value = ExpenseDetailUiState.Success(result.data)
                    hasLoadedOnce = true
                    if (_items.value.isEmpty()) loadItems()
                    loadChangeLog()
                }
                is ApiResult.NotFound -> _expenseState.value = ExpenseDetailUiState.Deleted
                is ApiResult.NetworkError -> {
                    if (!hasLoadedOnce) _expenseState.value =
                        ExpenseDetailUiState.Error("No internet connection.", true)
                }
                else -> {
                    if (!hasLoadedOnce) _expenseState.value =
                        ExpenseDetailUiState.Error("Failed to load expense.", false)
                }
            }
        }
    }

    fun forceRefresh() {
        hasLoadedOnce = false
        _items.value = emptyList()
        loadExpense()
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
     * Wave 2D-3: delete expense — queue-backed.
     *
     * Enqueues DELETE_EXPENSE before the network call. The stable idempotencyKey
     * is sent as the Idempotency-Key header so the backend cannot reverse balances
     * twice on retry. On NetworkError the operation stays FAILED_RETRYABLE and
     * SyncWorker replays it when network returns.
     */
    fun deleteExpense() {
        val userId = currentUserId ?: run {
            _actionState.value = ExpenseActionState.Error("Not logged in.")
            return
        }

        viewModelScope.launch {
            _actionState.value = ExpenseActionState.Loading

            val enqueued = pendingOperationRepository.enqueue(
                userId           = userId,
                operationType    = OperationType.DELETE_EXPENSE,
                endpoint         = "/api/expenses/$expenseId",
                method           = "DELETE",
                requestBodyJson  = null,         // DELETE has no body
                localResourceId  = expenseId,
                serverResourceId = expenseId,    // resource already exists on server
            )

            when (val result = deleteExpenseUseCase(expenseId, enqueued.idempotencyKey)) {
                is ApiResult.Success -> {
                    pendingOperationRepository.markSynced(enqueued.operationId, expenseId)
                    _actionState.value = ExpenseActionState.Deleted
                }

                is ApiResult.NetworkError -> {
                    pendingOperationRepository.markRetryable(
                        enqueued.operationId, result.exception.message ?: "Network error"
                    )
                    SyncWorker.triggerImmediateSync(appContext)
                    android.util.Log.w("ExpenseDetailVM", "Offline delete queued: ${enqueued.operationId}")
                    _actionState.value = ExpenseActionState.DeletedOffline
                }

                is ApiResult.Forbidden -> {
                    pendingOperationRepository.markFailed(enqueued.operationId, result.message)
                    _actionState.value = ExpenseActionState.Error(result.message)
                }

                is ApiResult.NotFound -> {
                    // Already deleted on server — treat as success locally.
                    pendingOperationRepository.markSynced(enqueued.operationId, expenseId)
                    _actionState.value = ExpenseActionState.Deleted
                }

                else -> {
                    pendingOperationRepository.markFailed(enqueued.operationId, "Failed to delete expense")
                    _actionState.value = ExpenseActionState.Error("Failed to delete expense.")
                }
            }
        }
    }

    /**
     * Wave 2D-3: restore expense — queue-backed.
     *
     * Same idempotency guarantee as delete: the stable key prevents a double
     * balance re-application on retry.
     */
    fun restoreExpense() {
        val userId = currentUserId ?: run {
            _actionState.value = ExpenseActionState.Error("Not logged in.")
            return
        }

        viewModelScope.launch {
            _actionState.value = ExpenseActionState.Loading

            val enqueued = pendingOperationRepository.enqueue(
                userId           = userId,
                operationType    = OperationType.RESTORE_EXPENSE,
                endpoint         = "/api/expenses/$expenseId/restore",
                method           = "POST",
                requestBodyJson  = null,         // restore has no body
                localResourceId  = expenseId,
                serverResourceId = expenseId,    // resource already exists on server
            )

            when (val result = restoreExpenseUseCase(expenseId, enqueued.idempotencyKey)) {
                is ApiResult.Success -> {
                    pendingOperationRepository.markSynced(enqueued.operationId, expenseId)
                    _actionState.value = ExpenseActionState.Restored
                }

                is ApiResult.NetworkError -> {
                    pendingOperationRepository.markRetryable(
                        enqueued.operationId, result.exception.message ?: "Network error"
                    )
                    SyncWorker.triggerImmediateSync(appContext)
                    android.util.Log.w("ExpenseDetailVM", "Offline restore queued: ${enqueued.operationId}")
                    _actionState.value = ExpenseActionState.RestoredOffline
                }

                is ApiResult.Forbidden -> {
                    pendingOperationRepository.markFailed(enqueued.operationId, result.message)
                    _actionState.value = ExpenseActionState.Error(result.message)
                }

                else -> {
                    pendingOperationRepository.markFailed(enqueued.operationId, "Failed to restore expense")
                    _actionState.value = ExpenseActionState.Error("Failed to restore expense.")
                }
            }
        }
    }

    fun resetActionState() { _actionState.value = ExpenseActionState.Idle }
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