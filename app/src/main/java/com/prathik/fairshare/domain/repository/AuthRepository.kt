package com.prathik.fairshare.domain.repository

import com.prathik.fairshare.domain.model.User

/**
 * Contract for all authentication operations.
 * Implementation lives in data/repository/impl/AuthRepositoryImpl.kt
 */
interface AuthRepository {

    /**
     * Authenticates user with email and password.
     * On success, tokens are automatically saved to DataStore.
     * Returns the logged-in [User] or failure with error message.
     */
    suspend fun login(email: String, password: String): Result<User>

    /**
     * Creates a new account. User is set to INVITED status until email is verified.
     * Sends a verification email automatically after registration.
     */
    suspend fun register(
        email: String,
        fullName: String,
        password: String,
        phoneNumber: String?,
        preferredCurrency: String?,
        language: String?,
    ): Result<User>

    /**
     * Verifies the user's email using the token sent to their inbox.
     * On success, account status changes from INVITED to ACTIVE.
     */
    suspend fun verifyEmail(userId: String, token: String): Result<Unit>

    /**
     * Sends a password reset link to the given email.
     * Always returns success even if email doesn't exist (security best practice).
     */
    suspend fun forgotPassword(email: String): Result<Unit>

    /**
     * Resets the password using the token from the reset email.
     */
    suspend fun resetPassword(token: String, newPassword: String): Result<Unit>

    /**
     * Changes password for the currently authenticated user.
     * Requires the current password for verification.
     */
    suspend fun changePassword(currentPassword: String, newPassword: String): Result<Unit>

    /**
     * Uses the stored refresh token to get a new access token.
     * Called automatically by TokenRefreshInterceptor when a 401 is received.
     */
    suspend fun refreshToken(): Result<Unit>

    /**
     * Logs out the current user.
     * Invalidates the refresh token on the server and clears local tokens.
     */
    suspend fun logout(): Result<Unit>

    /**
     * Checks if a valid access token exists in DataStore.
     * Used by the Splash screen to decide whether to go to Login or Home.
     */
    suspend fun isLoggedIn(): Boolean
}