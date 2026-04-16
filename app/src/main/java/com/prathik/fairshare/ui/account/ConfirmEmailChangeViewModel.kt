package com.prathik.fairshare.ui.account

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.prathik.fairshare.domain.model.ApiResult
import com.prathik.fairshare.domain.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ConfirmEmailChangeViewModel @Inject constructor(
    private val userRepository: UserRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow<ConfirmEmailChangeUiState>(ConfirmEmailChangeUiState.Loading)
    val uiState: StateFlow<ConfirmEmailChangeUiState> = _uiState.asStateFlow()

    fun confirm(token: String) {
        viewModelScope.launch {
            _uiState.value = ConfirmEmailChangeUiState.Loading
            when (val result = userRepository.verifyEmailChange(token)) {
                is ApiResult.Success     -> _uiState.value = ConfirmEmailChangeUiState.Success
                is ApiResult.Unauthorized -> _uiState.value = ConfirmEmailChangeUiState.Error(
                    "This confirmation link is invalid or has expired. Please request a new email change.")
                else -> _uiState.value = ConfirmEmailChangeUiState.Error(
                    "Something went wrong. Please try again.")
            }
        }
    }

    fun setError(message: String) {
        _uiState.value = ConfirmEmailChangeUiState.Error(message)
    }
}

sealed class ConfirmEmailChangeUiState {
    object Loading : ConfirmEmailChangeUiState()
    object Success : ConfirmEmailChangeUiState()
    data class Error(val message: String) : ConfirmEmailChangeUiState()
}