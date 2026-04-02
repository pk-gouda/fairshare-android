package com.prathik.fairshare.ui.account

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.prathik.fairshare.domain.model.ApiResult
import com.prathik.fairshare.domain.usecase.user.GetMyProfileUseCase
import com.prathik.fairshare.domain.usecase.user.UpdateProfileUseCase
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
) : ViewModel() {

    private val _fullName    = MutableStateFlow("")
    val fullName: StateFlow<String> = _fullName.asStateFlow()

    private val _phoneNumber = MutableStateFlow("")
    val phoneNumber: StateFlow<String> = _phoneNumber.asStateFlow()

    private val _email       = MutableStateFlow("")
    val email: StateFlow<String> = _email.asStateFlow()

    private val _isLoading   = MutableStateFlow(false)
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

    fun onFullNameChanged(value: String) { _fullName.value = value }
    fun onPhoneChanged(value: String)    { _phoneNumber.value = value }

    fun saveProfile() {
        val name = _fullName.value.trim()
        if (name.isBlank()) {
            _actionState.value = EditProfileActionState.Error("Name cannot be empty")
            return
        }
        viewModelScope.launch {
            _isLoading.value = true
            when (updateProfileUseCase(
                fullName    = name,
                phoneNumber = _phoneNumber.value.trim().ifBlank { null },
            )) {
                is ApiResult.Success -> _actionState.value = EditProfileActionState.Success("Profile updated")
                else                 -> _actionState.value = EditProfileActionState.Error("Failed to update profile")
            }
            _isLoading.value = false
        }
    }

    fun resetActionState() { _actionState.value = EditProfileActionState.Idle }
}

sealed class EditProfileActionState {
    object Idle : EditProfileActionState()
    data class Success(val message: String) : EditProfileActionState()
    data class Error(val message: String)   : EditProfileActionState()
}