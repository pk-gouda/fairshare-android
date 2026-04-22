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
                is ApiResult.NotFound -> _state.value = SettlementDetailUiState.Deleted
                is ApiResult.NetworkError -> {
                    if (!hasLoadedOnce) _state.value = SettlementDetailUiState.Error("No internet connection.")
                }
                else -> {
                    if (!hasLoadedOnce) _state.value = SettlementDetailUiState.Error("Failed to load settlement.")
                }
            }
        }
    }

    fun deleteSettlement() {
        viewModelScope.launch {
            _actionState.value = SettlementDetailActionState.Loading
            when (settlementRepository.deleteSettlement(settlementId)) {
                is ApiResult.Success -> _actionState.value = SettlementDetailActionState.Deleted
                is ApiResult.NetworkError -> _actionState.value =
                    SettlementDetailActionState.Error("No internet connection.")
                else -> _actionState.value =
                    SettlementDetailActionState.Error("Failed to delete settlement.")
            }
        }
    }

    fun resetActionState() { _actionState.value = SettlementDetailActionState.Idle }

    /** True if current user recorded this settlement (can edit/delete). */
    fun isRecordedByMe(settlement: Settlement) =
        settlement.recordedById == currentUserId
}

sealed class SettlementDetailUiState {
    object Loading : SettlementDetailUiState()
    object Deleted : SettlementDetailUiState()
    data class Success(val settlement: Settlement) : SettlementDetailUiState()
    data class Error(val message: String) : SettlementDetailUiState()
}

sealed class SettlementDetailActionState {
    object Idle    : SettlementDetailActionState()
    object Loading : SettlementDetailActionState()
    object Deleted : SettlementDetailActionState()
    data class Error(val message: String) : SettlementDetailActionState()
}