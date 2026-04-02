package com.prathik.fairshare.domain.repository

import com.prathik.fairshare.domain.model.ApiResult
import com.prathik.fairshare.domain.model.User

/**
 * Contract for user profile operations.
 * Implementation lives in data/repository/impl/UserRepositoryImpl.kt
 */
interface UserRepository {

    /**
     * Fetches the currently authenticated user's profile.
     */
    suspend fun getMyProfile(): ApiResult<User>

    /**
     * Fetches another user's public profile by their ID.
     * Used when viewing group members or friend profiles.
     * Returns [ApiResult.NotFound] if user doesn't exist.
     */
    suspend fun getUserProfile(userId: String): ApiResult<User>

    /**
     * Updates the current user's profile fields.
     * Only non-null fields are updated — null means no change.
     */
    suspend fun updateProfile(
        fullName: String?,
        phoneNumber: String?,
        preferredCurrency: String?,
        language: String?,
        notificationEnabled: Boolean?,
    ): ApiResult<User>

    /**
     * Deactivates the current user's account.
     * Soft delete — account can be reactivated.
     */
    suspend fun deactivateAccount(): ApiResult<Unit>

    /**
     * Reactivates a previously deactivated account.
     */
    suspend fun reactivateAccount(): ApiResult<Unit>

    /**
     * Permanently deletes the current user's account.
     * This is irreversible — all data is removed.
     */
    suspend fun deleteAccount(): ApiResult<Unit>

    /**
     * Returns the current user's friend code.
     */
    suspend fun getFriendCode(): ApiResult<String>

    /**
     * Regenerates the current user's friend code.
     * Old code becomes invalid immediately.
     */
    suspend fun regenerateFriendCode(): ApiResult<String>

    /**
     * Searches for a user by email address.
     * Used by Add Friend screen to find users before sending a request.
     */
    suspend fun searchByEmail(email: String): ApiResult<User>

    /**
     * Requests an email change. Backend sends a verification token to the new email.
     * Returns the token directly in dev (SES not wired yet).
     */
    suspend fun requestEmailChange(newEmail: String): ApiResult<String>

    /**
     * Confirms an email change using the verification token sent to the new email.
     */
    suspend fun verifyEmailChange(token: String): ApiResult<Unit>
}