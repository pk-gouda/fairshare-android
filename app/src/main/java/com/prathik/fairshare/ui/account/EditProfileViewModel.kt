package com.prathik.fairshare.ui.account

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.prathik.fairshare.domain.model.ApiResult
import com.prathik.fairshare.domain.usecase.user.GetMyProfileUseCase
import com.prathik.fairshare.domain.usecase.user.UpdateProfileUseCase
import com.prathik.fairshare.domain.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class EditProfileViewModel @Inject constructor(
    private val getMyProfileUseCase : GetMyProfileUseCase,
    private val updateProfileUseCase: UpdateProfileUseCase,
    private val userRepository      : UserRepository,
) : ViewModel() {

    // ── Profile fields ────────────────────────────────────────────────────────
    private val _fullName    = MutableStateFlow("")
    val fullName: StateFlow<String> = _fullName.asStateFlow()

    private val _phoneNumber = MutableStateFlow("")
    val phoneNumber: StateFlow<String> = _phoneNumber.asStateFlow()

    private val _email = MutableStateFlow("")
    val email: StateFlow<String> = _email.asStateFlow()

    // ── Inline edit state — which field is being edited ───────────────────────
    private val _editingField = MutableStateFlow<EditingField?>(null)
    val editingField: StateFlow<EditingField?> = _editingField.asStateFlow()

    // ── Email change flow ─────────────────────────────────────────────────────
    private val _emailChangeState = MutableStateFlow<EmailChangeState>(EmailChangeState.Idle)
    val emailChangeState: StateFlow<EmailChangeState> = _emailChangeState.asStateFlow()

    private val _pendingNewEmail = MutableStateFlow("")
    val pendingNewEmail: StateFlow<String> = _pendingNewEmail.asStateFlow()

    private val _verificationToken = MutableStateFlow("")
    val verificationToken: StateFlow<String> = _verificationToken.asStateFlow()

    // ── Loading / action ──────────────────────────────────────────────────────
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _actionState = MutableStateFlow<EditProfileActionState>(EditProfileActionState.Idle)
    val actionState: StateFlow<EditProfileActionState> = _actionState.asStateFlow()

    init { loadProfile() }

    private fun loadProfile() {
        viewModelScope.launch {
            _isLoading.value = true
            when (val result = getMyProfileUseCase()) {
                is ApiResult.Success -> {
                    _fullName.value    = result.data.fullName
                    _phoneNumber.value = result.data.phoneNumber ?: ""
                    _email.value       = result.data.email
                }
                else -> Unit
            }
            _isLoading.value = false
        }
    }

    // ── Field editing ─────────────────────────────────────────────────────────
    fun startEditing(field: EditingField) { _editingField.value = field }
    fun stopEditing()                     { _editingField.value = null }

    fun onFullNameChanged(value: String)  { _fullName.value = value }
    fun onPhoneChanged(value: String)     { _phoneNumber.value = value }
    fun onPendingEmailChanged(value: String) { _pendingNewEmail.value = value }
    fun onVerificationTokenChanged(value: String) { _verificationToken.value = value }

    // ── Save name ─────────────────────────────────────────────────────────────
    fun saveName() {
        val name = _fullName.value.trim()
        if (name.isBlank()) {
            _actionState.value = EditProfileActionState.Error("Name cannot be empty")
            return
        }
        viewModelScope.launch {
            _isLoading.value = true
            when (updateProfileUseCase(fullName = name)) {
                is ApiResult.Success -> {
                    _editingField.value = null
                    _actionState.value  = EditProfileActionState.Success("Name updated")
                }
                else -> _actionState.value = EditProfileActionState.Error("Failed to update name")
            }
            _isLoading.value = false
        }
    }

    // ── Save phone ────────────────────────────────────────────────────────────
    fun savePhone() {
        viewModelScope.launch {
            _isLoading.value = true
            when (updateProfileUseCase(phoneNumber = _phoneNumber.value.trim().ifBlank { null })) {
                is ApiResult.Success -> {
                    _editingField.value = null
                    _actionState.value  = EditProfileActionState.Success("Phone updated")
                }
                else -> _actionState.value = EditProfileActionState.Error("Failed to update phone")
            }
            _isLoading.value = false
        }
    }

    // ── Email change flow ─────────────────────────────────────────────────────
    fun requestEmailChange() {
        val newEmail = _pendingNewEmail.value.trim()
        if (newEmail.isBlank() || !newEmail.contains("@")) {
            _actionState.value = EditProfileActionState.Error("Enter a valid email address")
            return
        }
        viewModelScope.launch {
            _isLoading.value = true
            when (val result = userRepository.requestEmailChange(newEmail)) {
                is ApiResult.Success -> {
                    // Backend returns token directly (SES not wired yet)
                    // In production this would be empty and user checks their email
                    _verificationToken.value = result.data
                    _emailChangeState.value = EmailChangeState.AwaitingVerification
                    _actionState.value = EditProfileActionState.Success("Verification sent to $newEmail")
                }
                is ApiResult.Conflict -> _actionState.value = EditProfileActionState.Error("Email already in use")
                else -> _actionState.value = EditProfileActionState.Error("Failed to send verification")
            }
            _isLoading.value = false
        }
    }

    fun confirmEmailChange() {
        val token = _verificationToken.value.trim()
        if (token.isBlank()) {
            _actionState.value = EditProfileActionState.Error("Enter the verification token")
            return
        }
        viewModelScope.launch {
            _isLoading.value = true
            when (userRepository.verifyEmailChange(token)) {
                is ApiResult.Success -> {
                    _email.value            = _pendingNewEmail.value.trim()
                    _pendingNewEmail.value  = ""
                    _verificationToken.value = ""
                    _emailChangeState.value = EmailChangeState.Idle
                    _editingField.value     = null
                    _actionState.value      = EditProfileActionState.Success("Email updated successfully")
                }
                is ApiResult.Unauthorized -> _actionState.value = EditProfileActionState.Error("Invalid or expired token")
                else -> _actionState.value = EditProfileActionState.Error("Verification failed")
            }
            _isLoading.value = false
        }
    }

    fun cancelEmailChange() {
        _emailChangeState.value  = EmailChangeState.Idle
        _pendingNewEmail.value   = ""
        _verificationToken.value = ""
        _editingField.value      = null
    }

    fun resetActionState() { _actionState.value = EditProfileActionState.Idle }
}

enum class EditingField { NAME, PHONE, EMAIL }

sealed class EmailChangeState {
    object Idle                : EmailChangeState()
    object AwaitingVerification: EmailChangeState()
}

sealed class EditProfileActionState {
    object Idle : EditProfileActionState()
    data class Success(val message: String) : EditProfileActionState()
    data class Error(val message: String)   : EditProfileActionState()
}