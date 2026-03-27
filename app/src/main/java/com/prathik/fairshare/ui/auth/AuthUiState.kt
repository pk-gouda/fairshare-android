package com.prathik.fairshare.ui.auth

import com.prathik.fairshare.domain.model.FieldError
import com.prathik.fairshare.domain.model.User

/**
 * Represents the complete UI state for all auth screens.
 *
 * A single sealed class covers all auth states — the active screen
 * observes only the states relevant to it.
 *
 * Idle        — initial state, nothing happening
 * Loading     — async operation in progress, show spinner
 * LoginSuccess    — login succeeded, navigate to Groups
 * RegisterSuccess — register succeeded, navigate to VerifyEmail
 * ForgotPasswordSuccess — email sent, show success UI
 * Error       — operation failed, show error message
 * ValidationError — field-level errors, show inline on form fields
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

    data class Error(
        val message: String,
    ) : AuthUiState()

    data class ValidationError(
        val message: String,
        val fieldErrors: List<FieldError> = emptyList(),
    ) : AuthUiState()
}