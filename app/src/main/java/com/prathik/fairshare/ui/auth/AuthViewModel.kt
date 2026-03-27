package com.prathik.fairshare.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.prathik.fairshare.domain.model.ApiResult
import com.prathik.fairshare.domain.usecase.auth.ForgotPasswordUseCase
import com.prathik.fairshare.domain.usecase.auth.IsLoggedInUseCase
import com.prathik.fairshare.domain.usecase.auth.LoginUseCase
import com.prathik.fairshare.domain.usecase.auth.LogoutUseCase
import com.prathik.fairshare.domain.usecase.auth.RegisterUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for all auth screens — Login, Register, ForgotPassword, Splash.
 *
 * Holds form field state and emits AuthUiState via StateFlow.
 * All screens share one ViewModel instance scoped to the auth nav graph.
 *
 * Form fields are held here so state survives screen transitions
 * e.g. navigating from Login → Register → back → Login still shows
 * the previously entered email.
 */
@HiltViewModel
class AuthViewModel @Inject constructor(
    private val loginUseCase         : LoginUseCase,
    private val registerUseCase      : RegisterUseCase,
    private val logoutUseCase        : LogoutUseCase,
    private val forgotPasswordUseCase: ForgotPasswordUseCase,
    private val isLoggedInUseCase    : IsLoggedInUseCase,
) : ViewModel() {

    // ── UI State ──────────────────────────────────────────────────────────────
    private val _uiState = MutableStateFlow<AuthUiState>(AuthUiState.Idle)
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    // ── Form fields ───────────────────────────────────────────────────────────
    private val _email           = MutableStateFlow("")
    private val _password        = MutableStateFlow("")
    private val _confirmPassword = MutableStateFlow("")
    private val _fullName        = MutableStateFlow("")
    private val _phone           = MutableStateFlow("")

    val email           : StateFlow<String> = _email.asStateFlow()
    val password        : StateFlow<String> = _password.asStateFlow()
    val confirmPassword : StateFlow<String> = _confirmPassword.asStateFlow()
    val fullName        : StateFlow<String> = _fullName.asStateFlow()
    val phone           : StateFlow<String> = _phone.asStateFlow()

    // ── Event handler ─────────────────────────────────────────────────────────
    fun onEvent(event: AuthUiEvent) {
        when (event) {
            is AuthUiEvent.OnEmailChanged           -> _email.value = event.email
            is AuthUiEvent.OnPasswordChanged        -> _password.value = event.password
            is AuthUiEvent.OnConfirmPasswordChanged -> _confirmPassword.value = event.password
            is AuthUiEvent.OnFullNameChanged        -> _fullName.value = event.fullName
            is AuthUiEvent.OnPhoneChanged           -> _phone.value = event.phone
            is AuthUiEvent.OnLoginClicked           -> login()
            is AuthUiEvent.OnRegisterClicked        -> register()
            is AuthUiEvent.OnForgotPasswordClicked  -> forgotPassword()
            is AuthUiEvent.OnLogoutClicked          -> logout()
            is AuthUiEvent.OnResendVerificationClicked -> resendVerification()
            is AuthUiEvent.OnResetState             -> _uiState.value = AuthUiState.Idle
        }
    }

    // ── Auth operations ───────────────────────────────────────────────────────

    /**
     * Checks if user is already logged in — called from SplashScreen.
     * Returns true if a valid token exists.
     */
    fun isLoggedIn(): Boolean = isLoggedInUseCase()

    private fun login() {
        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            when (val result = loginUseCase(_email.value, _password.value)) {
                is ApiResult.Success -> {
                    _uiState.value = AuthUiState.LoginSuccess(result.data)
                }
                is ApiResult.ValidationError -> {
                    _uiState.value = AuthUiState.ValidationError(
                        message     = result.message,
                        fieldErrors = result.errors,
                    )
                }
                is ApiResult.Unauthorized -> {
                    _uiState.value = AuthUiState.Error("Invalid email or password.")
                }
                is ApiResult.Conflict -> {
                    _uiState.value = AuthUiState.Error(result.message)
                }
                is ApiResult.NetworkError -> {
                    _uiState.value = AuthUiState.Error("No internet connection. Please check your network.")
                }
                else -> {
                    _uiState.value = AuthUiState.Error("Something went wrong. Please try again.")
                }
            }
        }
    }

    private fun register() {
        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            when (val result = registerUseCase(
                email             = _email.value,
                fullName          = _fullName.value,
                password          = _password.value,
                confirmPassword   = _confirmPassword.value,
                phoneNumber       = _phone.value.ifBlank { null },
                preferredCurrency = null,
                language          = null,
            )) {
                is ApiResult.Success -> {
                    _uiState.value = AuthUiState.RegisterSuccess(_email.value)
                }
                is ApiResult.ValidationError -> {
                    _uiState.value = AuthUiState.ValidationError(
                        message     = result.message,
                        fieldErrors = result.errors,
                    )
                }
                is ApiResult.Conflict -> {
                    _uiState.value = AuthUiState.Error(result.message)
                }
                is ApiResult.NetworkError -> {
                    _uiState.value = AuthUiState.Error("No internet connection. Please check your network.")
                }
                else -> {
                    _uiState.value = AuthUiState.Error("Something went wrong. Please try again.")
                }
            }
        }
    }

    private fun forgotPassword() {
        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            when (val result = forgotPasswordUseCase(_email.value)) {
                is ApiResult.Success -> {
                    _uiState.value = AuthUiState.ForgotPasswordSuccess
                }
                is ApiResult.ValidationError -> {
                    _uiState.value = AuthUiState.ValidationError(
                        message     = result.message,
                        fieldErrors = result.errors,
                    )
                }
                is ApiResult.NetworkError -> {
                    _uiState.value = AuthUiState.Error("No internet connection. Please check your network.")
                }
                else -> {
                    _uiState.value = AuthUiState.Error("Something went wrong. Please try again.")
                }
            }
        }
    }

    private fun logout() {
        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            logoutUseCase()
            _uiState.value = AuthUiState.Idle
            clearFields()
        }
    }

    private fun resendVerification() {
        // POST /api/auth/resend-verification — wire when backend endpoint confirmed
        // For now no-op — VerifyEmailScreen shows a manual "resend" button
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    /**
     * Returns the field error message for a given field name.
     * Used by screens to show inline errors on specific fields.
     *
     * Example:
     *   error = viewModel.getFieldError("email")
     */
    fun getFieldError(field: String): String? {
        val state = _uiState.value
        if (state !is AuthUiState.ValidationError) return null
        return state.fieldErrors.firstOrNull { it.field == field }?.message
    }

    private fun clearFields() {
        _email.value           = ""
        _password.value        = ""
        _confirmPassword.value = ""
        _fullName.value        = ""
        _phone.value           = ""
    }
}