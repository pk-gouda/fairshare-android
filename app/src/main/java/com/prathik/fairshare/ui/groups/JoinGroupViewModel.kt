package com.prathik.fairshare.ui.groups

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.prathik.fairshare.domain.model.ApiResult
import com.prathik.fairshare.domain.usecase.group.JoinGroupUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class JoinGroupViewModel @Inject constructor(
    private val joinGroupUseCase: JoinGroupUseCase,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    // Pre-filled when arriving from a deep link or QR scan
    private val _inviteCode = MutableStateFlow(
        savedStateHandle.get<String>("inviteCode")?.takeIf { it.isNotBlank() } ?: ""
    )
    val inviteCode: StateFlow<String> = _inviteCode.asStateFlow()

    private val _uiState = MutableStateFlow<JoinGroupUiState>(JoinGroupUiState.Idle)
    val uiState: StateFlow<JoinGroupUiState> = _uiState.asStateFlow()

    fun onInviteCodeChanged(value: String) {
        // Strip whitespace — users often copy codes with leading/trailing spaces
        _inviteCode.value = value.trim()
    }

    fun joinGroup() {
        val code = _inviteCode.value.trim()
        if (code.isBlank()) {
            _uiState.value = JoinGroupUiState.Error("Please enter an invite code.")
            return
        }
        viewModelScope.launch {
            _uiState.value = JoinGroupUiState.Loading
            when (val result = joinGroupUseCase(code)) {
                is ApiResult.Success    -> _uiState.value = JoinGroupUiState.Success
                is ApiResult.NotFound   -> _uiState.value = JoinGroupUiState.Error("Invalid invite code. Double-check and try again.")
                is ApiResult.Conflict   -> _uiState.value = JoinGroupUiState.AlreadyMember
                is ApiResult.NetworkError -> _uiState.value = JoinGroupUiState.Error("No internet connection.")
                else                    -> _uiState.value = JoinGroupUiState.Error("Something went wrong. Please try again.")
            }
        }
    }

    fun resetUiState() { _uiState.value = JoinGroupUiState.Idle }
}

sealed class JoinGroupUiState {
    object Idle          : JoinGroupUiState()
    object Loading       : JoinGroupUiState()
    object Success       : JoinGroupUiState()  // navigate to Groups tab; it auto-refreshes
    object AlreadyMember : JoinGroupUiState()
    data class Error(val message: String) : JoinGroupUiState()
}