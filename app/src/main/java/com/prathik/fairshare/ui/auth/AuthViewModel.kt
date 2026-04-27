package com.prathik.fairshare.ui.auth

import android.content.Context
import androidx.lifecycle.ViewModel
import com.prathik.fairshare.data.sync.CacheWarmupWorker
import androidx.lifecycle.viewModelScope
import com.prathik.fairshare.domain.model.ApiResult
import com.prathik.fairshare.domain.usecase.auth.ForgotPasswordUseCase
import com.prathik.fairshare.domain.usecase.auth.IsLoggedInUseCase
import com.prathik.fairshare.domain.usecase.auth.LoginUseCase
import com.prathik.fairshare.domain.usecase.auth.LogoutUseCase
import com.prathik.fairshare.domain.usecase.auth.RegisterUseCase
import com.prathik.fairshare.domain.usecase.auth.VerifyEmailUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for all auth screens — Login, Register, ForgotPassword, Splash, VerifyEmail.
 *
 * Holds form field state and emits AuthUiState via StateFlow.
 * All screens share one ViewModel instance scoped to the auth nav graph.
 *
 * Form fields are held here so state survives screen transitions —
 * e.g. navigating from Login → Register → back → Login still shows
 * the previously entered email.
 */
@HiltViewModel
class AuthViewModel @Inject constructor(
    @dagger.hilt.android.qualifiers.ApplicationContext
    private val appContext           : Context,
    private val loginUseCase         : LoginUseCase,
    private val registerUseCase      : RegisterUseCase,
    private val logoutUseCase        : LogoutUseCase,
    private val forgotPasswordUseCase: ForgotPasswordUseCase,
    private val isLoggedInUseCase    : IsLoggedInUseCase,
    private val verifyEmailUseCase   : VerifyEmailUseCase,   // ✅
) : ViewModel() {

    // ── UI State ──────────────────────────────────────────────────────────────
    private val _uiState = MutableStateFlow<AuthUiState>(AuthUiState.Idle)
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    // ── Form fields ───────────────────────────────────────────────────────────
    // confirmPassword removed — not required on registration in FairShare
    private val _email    = MutableStateFlow("")
    private val _password = MutableStateFlow("")
    private val _fullName = MutableStateFlow("")
    private val _phone    = MutableStateFlow("")

    val email   : StateFlow<String> = _email.asStateFlow()
    val password: StateFlow<String> = _password.asStateFlow()
    val fullName: StateFlow<String> = _fullName.asStateFlow()
    val phone   : StateFlow<String> = _phone.asStateFlow()

    // ── Event handler ─────────────────────────────────────────────────────────
    fun onEvent(event: AuthUiEvent) {
        when (event) {
            is AuthUiEvent.OnEmailChanged -> {
                _email.value = event.email
                // Clear inline errors as soon as user starts typing
                if (_uiState.value is AuthUiState.ValidationError) {
                    _uiState.value = AuthUiState.Idle
                }
            }

            is AuthUiEvent.OnPasswordChanged -> {
                _password.value = event.password
                if (_uiState.value is AuthUiState.ValidationError) {
                    _uiState.value = AuthUiState.Idle
                }
            }

            is AuthUiEvent.OnConfirmPasswordChanged -> Unit // no-op — confirm password not used
            is AuthUiEvent.OnFullNameChanged -> {
                _fullName.value = event.fullName
                if (_uiState.value is AuthUiState.ValidationError) {
                    _uiState.value = AuthUiState.Idle
                }
            }

            is AuthUiEvent.OnPhoneChanged -> {
                _phone.value = event.phone
                // Phone is optional — no validation errors to clear
            }

            is AuthUiEvent.OnLoginClicked          -> login()
            is AuthUiEvent.OnRegisterClicked       -> register()
            is AuthUiEvent.OnForgotPasswordClicked -> forgotPassword()
            is AuthUiEvent.OnLogoutClicked         -> logout()
            is AuthUiEvent.OnResendVerificationClicked -> resendVerification()
            is AuthUiEvent.OnResetState            -> _uiState.value = AuthUiState.Idle

            // ✅ Deep link delivered userId + token — call the verify API
            is AuthUiEvent.OnVerifyEmail -> verifyEmail(event.userId, event.token)
        }
    }

    // ── Auth operations ───────────────────────────────────────────────────────

    /**
     * Checks if user is already logged in — called from SplashScreen.
     * Returns true if a valid token exists in EncryptedTokenStore.
     */
    fun isLoggedIn(): Boolean = isLoggedInUseCase()

    private fun login() {
        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            when (val result = loginUseCase(_email.value, _password.value)) {
                is ApiResult.Success -> {
                    // Trigger immediate warmup so offline data is ready after login.
                    CacheWarmupWorker.triggerNow(appContext)
                    _uiState.value = AuthUiState.LoginSuccess(result.data)
                }

                is ApiResult.ValidationError -> {
                    _uiState.value = AuthUiState.ValidationError(
                        message    = result.message,
                        fieldErrors = result.errors,
                    )
                }

                is ApiResult.Unauthorized -> {
                    // Wrong email or password
                    _uiState.value = AuthUiState.Error("Invalid email or password.")
                }

                is ApiResult.Conflict -> {
                    // Account not verified, suspended, or deactivated
                    _uiState.value = AuthUiState.Error(result.message)
                }

                is ApiResult.NetworkError -> {
                    _uiState.value =
                        AuthUiState.Error("No internet connection. Please check your network.")
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
                phoneNumber       = _phone.value.ifBlank { null },
                preferredCurrency = null, // set from LocationHelper on first launch
                language          = null, // set from device locale on first launch
            )) {
                is ApiResult.Success -> {
                    _uiState.value = AuthUiState.RegisterSuccess(_email.value)
                }

                is ApiResult.ValidationError -> {
                    _uiState.value = AuthUiState.ValidationError(
                        message    = result.message,
                        fieldErrors = result.errors,
                    )
                }

                is ApiResult.Conflict -> {
                    // Email already registered
                    _uiState.value = AuthUiState.Error(result.message)
                }

                is ApiResult.NetworkError -> {
                    _uiState.value =
                        AuthUiState.Error("No internet connection. Please check your network.")
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
                        message    = result.message,
                        fieldErrors = result.errors,
                    )
                }

                is ApiResult.NetworkError -> {
                    _uiState.value =
                        AuthUiState.Error("No internet connection. Please check your network.")
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

    /**
     * Called when the deep link delivers userId + token to the app.
     * Calls POST /api/auth/verify-email and emits:
     *   VerifyEmailSuccess — account activated, navigate to Login
     *   VerifyEmailError   — token invalid/expired, show error
     */
    fun verifyEmail(userId: String, token: String) {
        viewModelScope.launch {
            _uiState.value = AuthUiState.VerifyEmailLoading
            when (val result = verifyEmailUseCase(userId, token)) {
                is ApiResult.Success -> {
                    _uiState.value = AuthUiState.VerifyEmailSuccess
                }

                is ApiResult.Unauthorized -> {
                    _uiState.value = AuthUiState.VerifyEmailError(
                        "This verification link is invalid or has expired. Please register again."
                    )
                }

                is ApiResult.Conflict -> {
                    // Account was already verified — show dedicated "already verified" UI
                    // so the user gets clear feedback instead of silently navigating away.
                    _uiState.value = AuthUiState.VerifyEmailAlreadyVerified
                }

                is ApiResult.NetworkError -> {
                    _uiState.value = AuthUiState.VerifyEmailError(
                        "No internet connection. Please check your network and try again."
                    )
                }

                else -> {
                    _uiState.value = AuthUiState.VerifyEmailError(
                        "Verification failed. Please try again or contact support."
                    )
                }
            }
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    /**
     * Returns the field error message for a given field name.
     * Used by screens to show inline errors on specific form fields.
     */
    fun getFieldError(field: String): String? {
        val state = _uiState.value
        if (state !is AuthUiState.ValidationError) return null
        return state.fieldErrors.firstOrNull { it.field == field }?.message
    }

    private fun clearFields() {
        _email.value    = ""
        _password.value = ""
        _fullName.value = ""
        _phone.value    = ""
    }
}