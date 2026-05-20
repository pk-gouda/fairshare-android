package com.prathik.fairshare.ui.friends

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.prathik.fairshare.domain.model.ApiResult
import com.prathik.fairshare.domain.repository.UserRepository
import com.prathik.fairshare.domain.usecase.user.RegenerateFriendCodeUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class QrCodeViewModel @Inject constructor(
    private val userRepository             : UserRepository,
    private val regenerateFriendCodeUseCase: RegenerateFriendCodeUseCase,
) : ViewModel() {

    private val _friendCode = MutableStateFlow<String?>(null)
    val friendCode: StateFlow<String?> = _friendCode.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _actionState = MutableStateFlow<QrCodeActionState>(QrCodeActionState.Idle)
    val actionState: StateFlow<QrCodeActionState> = _actionState.asStateFlow()

    init { loadFriendCode() }

    fun loadFriendCode() {
        viewModelScope.launch {
            _isLoading.value = true
            // Use the dedicated friend-code endpoint (not profile cache) so that
            // existing users with null/blank friend_code self-heal on first open.
            when (val result = userRepository.getFriendCode()) {
                is ApiResult.Success -> {
                    val code = result.data
                    _friendCode.value = code.ifBlank { null }
                    if (code.isBlank()) {
                        _actionState.value = QrCodeActionState.Error(
                            "Could not generate your friend code. Please try again."
                        )
                    }
                }
                is ApiResult.HttpError -> _actionState.value =
                    QrCodeActionState.Error(result.message ?: "Could not load your QR code")
                else -> _actionState.value =
                    QrCodeActionState.Error("Could not load your QR code")
            }
            _isLoading.value = false
        }
    }

    fun regenerateCode() {
        viewModelScope.launch {
            _isLoading.value = true
            when (val result = regenerateFriendCodeUseCase()) {
                is ApiResult.Success -> {
                    _friendCode.value = result.data
                    _actionState.value = QrCodeActionState.Success("Code changed")
                }
                is ApiResult.HttpError -> _actionState.value = QrCodeActionState.Error(
                    if (result.code == 429)
                        "Too many changes. Please wait a moment and try again."
                    else
                        result.message
                )
                else -> _actionState.value = QrCodeActionState.Error("Failed to change code")
            }
            _isLoading.value = false
        }
    }

    fun retryLoad() { loadFriendCode() }
    fun resetActionState() { _actionState.value = QrCodeActionState.Idle }
}

sealed class QrCodeActionState {
    object Idle : QrCodeActionState()
    data class Success(val message: String) : QrCodeActionState()
    data class Error(val message: String)   : QrCodeActionState()
}