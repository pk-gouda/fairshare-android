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
}