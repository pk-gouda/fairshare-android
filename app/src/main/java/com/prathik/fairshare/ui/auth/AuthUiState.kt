package com.prathik.fairshare.ui.auth

import com.prathik.fairshare.domain.model.FieldError
import com.prathik.fairshare.domain.model.User

/**
 * Represents the complete UI state for all auth screens.
 *
 * A single sealed class covers all auth states — the active screen
 * observes only the states relevant to it.
 *
 * Idle                      — initial state, nothing happening
 * Loading                   — async operation in progress, show spinner
 * LoginSuccess              — login succeeded, navigate to Groups
 * RegisterSuccess           — register succeeded, navigate to VerifyEmail
 * ForgotPasswordSuccess     — email sent, show success UI
 * VerifyEmailLoading        — deep link received, calling verify API
 * VerifyEmailSuccess        — account activated, show success + navigate to Login
 * VerifyEmailAlreadyVerified— account was already active, show "already verified" UI
 * VerifyEmailError          — token invalid/expired, show error + retry option
 * Error                     — operation failed, show error message
 * ValidationError           — field-level errors, show inline on form fields
 */
sealed class AuthUiState {

    object Idle : AuthUiState()

    object Loading : AuthUiState()

    data class LoginSuccess(
        val user: User,
    ) : AuthUiState()

    data class RegisterSuccess(
        val email: String,
    ) : AuthUiState()

    object ForgotPasswordSuccess : AuthUiState()

    // ✅ Email verification states — driven by deep link flow

    /** Deep link received, POST /api/auth/verify-email in-flight. */
    object VerifyEmailLoading : AuthUiState()

    /** Account successfully activated — show success UI then navigate to Login. */
    object VerifyEmailSuccess : AuthUiState()

    /**
     * Account was already verified (backend returned 409 Conflict).
     * Shown when the user taps the verification link a second time.
     * Displays a friendly "Already verified" message with a sign-in button.
     */
    object VerifyEmailAlreadyVerified : AuthUiState()

    /**
     * Verification failed — token expired, already used, or invalid link.
     * [message] is shown to the user.
     */
    data class VerifyEmailError(
        val message: String,
    ) : AuthUiState()

    data class Error(
        val message: String,
    ) : AuthUiState()

    data class ValidationError(
        val message: String,
        val fieldErrors: List<FieldError> = emptyList(),
    ) : AuthUiState()
}