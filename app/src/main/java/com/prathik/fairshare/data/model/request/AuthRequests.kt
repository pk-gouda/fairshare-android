package com.prathik.fairshare.data.model.request

import com.prathik.fairshare.domain.model.AuthProvider
import kotlinx.serialization.Serializable

/**
 * Request body for POST /api/auth/register
 * phoneNumber, preferredCurrency, language are optional.
 */
@Serializable
data class RegisterRequest(
    val email: String,
    val fullName: String,
    val password: String,
    val phoneNumber: String? = null,
    val preferredCurrency: String? = null,
    val language: String? = null,
)

/**
 * Request body for POST /api/auth/login
 */
@Serializable
data class LoginRequest(
    val email: String,
    val password: String,
)

/**
 * Request body for POST /api/auth/oauth
 * Used for Google and Apple sign-in.
 * Creates account if first time, logs in if account already exists.
 */
@Serializable
data class OAuthLoginRequest(
    val email: String,
    val fullName: String,
    val profilePictureUrl: String? = null,
    val authProvider: AuthProvider,
)

/**
 * Request body for POST /api/auth/forgot-password
 */
@Serializable
data class ForgotPasswordRequest(
    val email: String,
)

/**
 * Request body for POST /api/auth/reset-password
 * token comes from the password reset email link.
 */
@Serializable
data class ResetPasswordRequest(
    val token: String,
    val newPassword: String,
)

/**
 * Request body for POST /api/auth/change-password
 * Requires the user to be authenticated.
 */
@Serializable
data class ChangePasswordRequest(
    val currentPassword: String,
    val newPassword: String,
)
