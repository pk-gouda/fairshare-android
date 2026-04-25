package com.prathik.fairshare.ui.settlement

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.prathik.fairshare.data.local.EncryptedTokenStore
import com.prathik.fairshare.data.model.response.SettlementChangeLogResponse
import com.prathik.fairshare.data.network.api.SettlementApiService
import com.prathik.fairshare.data.network.safeApiCall
import com.prathik.fairshare.domain.model.ApiResult
import com.prathik.fairshare.domain.model.Settlement
import com.prathik.fairshare.domain.repository.SettlementRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
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

    fun load() {
        viewModelScope.launch {
            if (!hasLoadedOnce) _state.value = SettlementDetailUiState.Loading
            when (val result = settlementRepository.getSettlementById(settlementId)) {
                is ApiResult.Success -> {
                    _state.value = SettlementDetailUiState.Success(result.data)
                    hasLoadedOnce = true
                    when (val logs = safeApiCall { settlementApiService.getSettlementChangelog(settlementId) }) {
                        is ApiResult.Success -> _changeLog.value = logs.data
                        else -> Unit
                    }
                }
                is ApiResult.NotFound -> _state.value = SettlementDetailUiState.NotFound
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
     * Soft-cancels a COMPLETED settlement via DELETE endpoint.
     * Backend now returns 200 with CANCELLED status instead of deleting.
     * Screen reloads to show CANCELLED state with Restore option.
     */
    fun cancelSettlement() {
        viewModelScope.launch {
            _actionState.value = SettlementDetailActionState.Loading
            when (settlementRepository.cancelSettlement(settlementId)) {
                is ApiResult.Success -> _actionState.value = SettlementDetailActionState.Cancelled
                is ApiResult.NetworkError -> _actionState.value =
                    SettlementDetailActionState.Error("No internet connection.")
                is ApiResult.Forbidden -> _actionState.value =
                    SettlementDetailActionState.Error("You don't have permission to cancel this settlement.")
                else -> _actionState.value =
                    SettlementDetailActionState.Error("Failed to cancel settlement.")
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
            when (settlementRepository.restoreSettlement(settlementId)) {
                is ApiResult.Success -> _actionState.value = SettlementDetailActionState.Restored
                is ApiResult.NetworkError -> _actionState.value =
                    SettlementDetailActionState.Error("No internet connection.")
                is ApiResult.Forbidden -> _actionState.value =
                    SettlementDetailActionState.Error("You don't have permission to restore this settlement.")
                is ApiResult.Conflict -> _actionState.value =
                    SettlementDetailActionState.Error("Settlement is already active.")
                else -> _actionState.value =
                    SettlementDetailActionState.Error("Failed to restore settlement.")
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