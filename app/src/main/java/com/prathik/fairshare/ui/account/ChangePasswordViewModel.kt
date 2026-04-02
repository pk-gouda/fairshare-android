package com.prathik.fairshare.ui.account

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.prathik.fairshare.domain.model.ApiResult
import com.prathik.fairshare.domain.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ChangePasswordViewModel @Inject constructor(
    private val authRepository: AuthRepository,
) : ViewModel() {

    private val _currentPassword = MutableStateFlow("")
    val currentPassword: StateFlow<String> = _currentPassword.asStateFlow()

    private val _newPassword = MutableStateFlow("")
    val newPassword: StateFlow<String> = _newPassword.asStateFlow()

    private val _confirmPassword = MutableStateFlow("")
    val confirmPassword: StateFlow<String> = _confirmPassword.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _actionState = MutableStateFlow<ChangePasswordActionState>(ChangePasswordActionState.Idle)
    val actionState: StateFlow<ChangePasswordActionState> = _actionState.asStateFlow()

    // Field-level errors
    private val _currentPasswordError = MutableStateFlow<String?>(null)
    val currentPasswordError: StateFlow<String?> = _currentPasswordError.asStateFlow()

    private val _newPasswordError = MutableStateFlow<String?>(null)
    val newPasswordError: StateFlow<String?> = _newPasswordError.asStateFlow()

    private val _confirmPasswordError = MutableStateFlow<String?>(null)
    val confirmPasswordError: StateFlow<String?> = _confirmPasswordError.asStateFlow()

    fun onCurrentPasswordChanged(v: String) { _currentPassword.value = v; _currentPasswordError.value = null }
    fun onNewPasswordChanged(v: String)     { _newPassword.value = v;     _newPasswordError.value = null }
    fun onConfirmPasswordChanged(v: String) { _confirmPassword.value = v; _confirmPasswordError.value = null }

    fun changePassword() {
        // Validate
        var valid = true
        if (_currentPassword.value.isBlank()) {
            _currentPasswordError.value = "Enter your current password"; valid = false
        }
        if (_newPassword.value.length < 8) {
            _newPasswordError.value = "Password must be at least 8 characters"; valid = false
        }
        if (_newPassword.value != _confirmPassword.value) {
            _confirmPasswordError.value = "Passwords don't match"; valid = false
        }
        if (!valid) return

        viewModelScope.launch {
            _isLoading.value = true
            when (authRepository.changePassword(_currentPassword.value, _newPassword.value)) {
                is ApiResult.Success    -> _actionState.value = ChangePasswordActionState.Success("Password updated")
                is ApiResult.Unauthorized -> {
                    _currentPasswordError.value = "Current password is incorrect"
                }
                else -> _actionState.value = ChangePasswordActionState.Error("Failed to update password")
            }
            _isLoading.value = false
        }
    }

    fun resetActionState() { _actionState.value = ChangePasswordActionState.Idle }
}

sealed class ChangePasswordActionState {
    object Idle : ChangePasswordActionState()
    data class Success(val message: String) : ChangePasswordActionState()
    data class Error(val message: String)   : ChangePasswordActionState()
}