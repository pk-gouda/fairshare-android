package com.prathik.fairshare.ui.auth

/**
 * All user actions that can occur on auth screens.
 *
 * ViewModels receive events via onEvent(AuthUiEvent) and
 * update the UI state accordingly.
 *
 * Field change events are sent on every keystroke —
 * they update local form state without triggering network calls.
 *
 * Action events (OnLoginClicked etc.) trigger validation
 * and network calls.
 */
sealed class AuthUiEvent {

    // ── Field changes ─────────────────────────────────────────────────────────
    data class OnEmailChanged(val email: String) : AuthUiEvent()
    data class OnPasswordChanged(val password: String) : AuthUiEvent()
    data class OnConfirmPasswordChanged(val password: String) : AuthUiEvent()
    data class OnFullNameChanged(val fullName: String) : AuthUiEvent()
    data class OnPhoneChanged(val phone: String) : AuthUiEvent()

    // ── Auth actions ──────────────────────────────────────────────────────────
    object OnLoginClicked : AuthUiEvent()
    object OnRegisterClicked : AuthUiEvent()
    object OnForgotPasswordClicked : AuthUiEvent()
    object OnLogoutClicked : AuthUiEvent()

    // ── Verification ──────────────────────────────────────────────────────────
    object OnResendVerificationClicked : AuthUiEvent()

    // ✅ Triggered when deep link delivers userId + token to VerifyEmailScreen.
    // ViewModel calls POST /api/auth/verify-email and emits VerifyEmailSuccess
    // or VerifyEmailError based on the result.
    data class OnVerifyEmail(
        val userId: String,
        val token : String,
    ) : AuthUiEvent()

    // ── Reset ─────────────────────────────────────────────────────────────────
    object OnResetState : AuthUiEvent()
}