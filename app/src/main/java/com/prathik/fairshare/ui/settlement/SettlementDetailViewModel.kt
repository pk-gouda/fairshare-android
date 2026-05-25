package com.prathik.fairshare.ui.settlement

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.prathik.fairshare.data.local.EncryptedTokenStore
import com.prathik.fairshare.data.model.response.SettlementChangeLogResponse
import com.prathik.fairshare.data.network.api.SettlementApiService
import com.prathik.fairshare.data.network.safeApiCall
import com.prathik.fairshare.domain.model.ApiResult
import com.prathik.fairshare.domain.model.errorMessage
import com.prathik.fairshare.domain.model.Settlement
import com.prathik.fairshare.domain.repository.SettlementRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class SettlementDetailViewModel @Inject constructor(
    private val settlementRepository: SettlementRepository,
    private val settlementApiService: SettlementApiService,
    private val tokenStore: EncryptedTokenStore,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    val settlementId: String = checkNotNull(savedStateHandle["settlementId"])
    val currentUserId: String? = tokenStore.getUserId()

    private val _state = MutableStateFlow<SettlementDetailUiState>(SettlementDetailUiState.Loading)
    val state: StateFlow<SettlementDetailUiState> = _state.asStateFlow()

    private val _actionState = MutableStateFlow<SettlementDetailActionState>(SettlementDetailActionState.Idle)
    val actionState: StateFlow<SettlementDetailActionState> = _actionState.asStateFlow()

    private val _changeLog = MutableStateFlow<List<SettlementChangeLogResponse>>(emptyList())
    val changeLog: StateFlow<List<SettlementChangeLogResponse>> = _changeLog.asStateFlow()

    private var hasLoadedOnce = false

    init { load() }

    fun load(silent: Boolean = false) {
        viewModelScope.launch {
            // Step 1: Render from Room immediately — no network wait.
            val cached = settlementRepository.getCachedSettlement(settlementId)
            if (cached != null) {
                _state.value = SettlementDetailUiState.Success(cached)
                hasLoadedOnce = true
            } else if (!silent && !hasLoadedOnce) {
                _state.value = SettlementDetailUiState.Loading
            }

            // Step 2: Network fetch — updates state only on success.
            when (val result = settlementRepository.getSettlementById(settlementId)) {
                is ApiResult.Success -> {
                    _state.value = SettlementDetailUiState.Success(result.data)
                    hasLoadedOnce = true
                    when (val logs = safeApiCall { settlementApiService.getSettlementChangelog(settlementId) }) {
                        is ApiResult.Success -> _changeLog.value = logs.data
                        else -> Unit
                    }
                }
                is ApiResult.NotFound -> {
                    if (!hasLoadedOnce) _state.value = SettlementDetailUiState.NotFound
                }
                is ApiResult.NetworkError -> {
                    if (!hasLoadedOnce) _state.value = SettlementDetailUiState.Error("No internet connection.")
                }
                else -> {
                    if (!hasLoadedOnce) _state.value = SettlementDetailUiState.Error("Failed to load settlement.")
                }
            }
        }
    }

    /**
     * Silent resume refresh — does not reset hasLoadedOnce, does not flash Loading.
     * Guards against racing the init load.
     */
    fun refreshSilently() {
        if (!hasLoadedOnce && _state.value is SettlementDetailUiState.Loading) return
        load(silent = true)
    }

    /**
     * Soft-cancels a COMPLETED settlement via DELETE endpoint.
     * Backend now returns 200 with CANCELLED status instead of deleting.
     * Screen reloads to show CANCELLED state with Restore option.
     */
    // Stable idempotency keys for cancel/restore on this single settlement.
    // Nullable — generated on first attempt, retained across NetworkError,
    // cleared after terminal success or non-retryable failure.
    private var cancelIdempotencyKey: String? = null
    private var restoreIdempotencyKey: String? = null

    fun cancelSettlement() {
        viewModelScope.launch {
            _actionState.value = SettlementDetailActionState.Loading
            val key = cancelIdempotencyKey ?: UUID.randomUUID().toString().also { cancelIdempotencyKey = it }
            when (val result = settlementRepository.cancelSettlement(settlementId, key)) {
                is ApiResult.Success -> {
                    cancelIdempotencyKey = null   // terminal success — fresh key for any future action
                    _actionState.value = SettlementDetailActionState.Cancelled
                }
                is ApiResult.NetworkError -> {
                    // Retain key — transient failure, retry must send same key
                    _actionState.value = SettlementDetailActionState.Error("No internet connection.")
                }
                is ApiResult.Forbidden -> {
                    cancelIdempotencyKey = null
                    _actionState.value = SettlementDetailActionState.Error("You don't have permission to cancel this settlement.")
                }
                else -> {
                    cancelIdempotencyKey = null
                    _actionState.value = SettlementDetailActionState.Error(result.errorMessage() ?: "Failed to cancel settlement.")
                }
            }
        }
    }

    /**
     * Restores a CANCELLED settlement to COMPLETED.
     * Re-applies existing allocation rows — no new rows created.
     * Screen reloads to show COMPLETED state.
     */
    fun restoreSettlement() {
        viewModelScope.launch {
            _actionState.value = SettlementDetailActionState.Loading
            val key = restoreIdempotencyKey ?: UUID.randomUUID().toString().also { restoreIdempotencyKey = it }
            when (val result = settlementRepository.restoreSettlement(settlementId, key)) {
                is ApiResult.Success -> {
                    restoreIdempotencyKey = null
                    _actionState.value = SettlementDetailActionState.Restored
                }
                is ApiResult.NetworkError -> {
                    // Retain key
                    _actionState.value = SettlementDetailActionState.Error("No internet connection.")
                }
                is ApiResult.Forbidden -> {
                    restoreIdempotencyKey = null
                    _actionState.value = SettlementDetailActionState.Error("You don't have permission to restore this settlement.")
                }
                is ApiResult.Conflict -> {
                    restoreIdempotencyKey = null
                    _actionState.value = SettlementDetailActionState.Error("Settlement is already active.")
                }
                else -> {
                    restoreIdempotencyKey = null
                    _actionState.value = SettlementDetailActionState.Error(result.errorMessage() ?: "Failed to restore settlement.")
                }
            }
        }
    }

    /** @deprecated Use cancelSettlement() — kept for backward compat with any callers */
    fun deleteSettlement() = cancelSettlement()

    fun resetActionState() { _actionState.value = SettlementDetailActionState.Idle }

    /** True if current user is involved (payer, receiver, or recorder). */
    fun isParticipant(settlement: Settlement) =
        settlement.recordedById == currentUserId ||
                settlement.payerId == currentUserId ||
                settlement.receiverId == currentUserId
}

sealed class SettlementDetailUiState {
    object Loading  : SettlementDetailUiState()
    object NotFound : SettlementDetailUiState()
    /** @deprecated kept for backward compat — use NotFound */
    object Deleted  : SettlementDetailUiState()
    data class Success(val settlement: Settlement) : SettlementDetailUiState()
    data class Error(val message: String) : SettlementDetailUiState()
}

sealed class SettlementDetailActionState {
    object Idle      : SettlementDetailActionState()
    object Loading   : SettlementDetailActionState()
    /** Settlement was soft-cancelled — reload screen to show CANCELLED state. */
    object Cancelled : SettlementDetailActionState()
    /** Settlement was restored to COMPLETED — reload screen. */
    object Restored  : SettlementDetailActionState()
    data class Error(val message: String) : SettlementDetailActionState()
}