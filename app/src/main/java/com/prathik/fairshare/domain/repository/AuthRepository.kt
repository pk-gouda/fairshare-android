package com.prathik.fairshare.domain.repository

import com.prathik.fairshare.domain.model.ApiResult
import com.prathik.fairshare.domain.model.User

/**
 * Contract for all authentication operations.
 * Implementation lives in data/repository/impl/AuthRepositoryImpl.kt
 */
interface AuthRepository {

    /**
     * Authenticates user with email and password.
     * On success, tokens are automatically saved to EncryptedTokenStore.
     * Returns [ApiResult.Unauthorized] if credentials are wrong.
     * Returns [ApiResult.Conflict] if account is suspended or not verified.
     */
    suspend fun login(email: String, password: String): ApiResult<User>

    /**
     * Creates a new account. User starts with INVITED status.
     * Sends a verification email automatically.
     * Returns [ApiResult.Conflict] if email is already registered.
     * Returns [ApiResult.ValidationError] if inputs are invalid.
     */
    suspend fun register(
        email: String,
        fullName: String,
        password: String,
        phoneNumber: String?,
        preferredCurrency: String?,
        language: String?,
    ): ApiResult<User>

    /**
     * Verifies the user's email using the token from the verification email.
     * On success, account status changes from INVITED to ACTIVE.
     */
    suspend fun verifyEmail(userId: String, token: String): ApiResult<Unit>

    /**
     * Sends a password reset link to the given email.
     * Always returns Success even if email doesn't exist (prevents enumeration).
     */
    suspend fun forgotPassword(email: String): ApiResult<Unit>

    /**
     * Resets password using the token from the reset email.
     */
    suspend fun resetPassword(token: String, newPassword: String): ApiResult<Unit>

    /**
     * Changes password for the currently authenticated user.
     * Returns [ApiResult.Unauthorized] if current password is wrong.
     */
    suspend fun changePassword(currentPassword: String, newPassword: String): ApiResult<Unit>

    /**
     * Uses the stored refresh token to get a new access token.
     * Called automatically by TokenRefreshInterceptor on 401.
     */
    suspend fun refreshToken(): ApiResult<Unit>

    /**
     * Logs out — invalidates refresh token on server and clears local tokens.
     */
    suspend fun logout(): ApiResult<Unit>

    /**
     * Checks if a valid access token exists in EncryptedTokenStore.
     * Used by SplashScreen to decide: Login or GroupsHome.
     */
    suspend fun isLoggedIn(): Boolean
}