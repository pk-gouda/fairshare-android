package com.prathik.fairshare.data.repository.impl

import com.prathik.fairshare.data.local.UserDao
import com.prathik.fairshare.data.local.UserEntity
import com.prathik.fairshare.data.local.EncryptedTokenStore
import com.prathik.fairshare.data.model.mapper.toDomain
import com.prathik.fairshare.data.model.request.DeactivateAccountRequest
import com.prathik.fairshare.data.model.request.DeleteAccountRequest
import com.prathik.fairshare.data.model.request.UpdateProfileRequest
import com.prathik.fairshare.data.network.api.UserApiService
import com.prathik.fairshare.data.network.mapSuccess
import com.prathik.fairshare.data.network.safeApiCall
import com.prathik.fairshare.di.ApplicationScope
import com.prathik.fairshare.domain.model.ApiResult
import com.prathik.fairshare.domain.model.User
import com.prathik.fairshare.domain.repository.UserRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserRepositoryImpl @Inject constructor(
    private val userService : UserApiService,
    private val userDao     : UserDao,
    private val tokenStore  : EncryptedTokenStore,
    @ApplicationScope private val appScope: CoroutineScope,
) : UserRepository {

    override suspend fun getMyProfile(): ApiResult<User> {
        // Return cached profile immediately if available
        val cached = userDao.getCurrentUser()
        if (cached != null) {
            // Refresh in background so the UI stays snappy.
            //
            // Uses the managed application-level scope (not a raw CoroutineScope)
            // so the job is tracked and can be reasoned about across the app lifetime.
            //
            // Stale-write guard: capture the session userId before launching.
            // After the network call completes, re-read the current session userId.
            // If logout fired (userId = null) or a different user logged in
            // (userId changed), skip the Room insert entirely.
            val sessionUserIdAtLaunch = tokenStore.getUserId()
            appScope.launch(Dispatchers.IO) {
                val result = safeApiCall { userService.getMyProfile() }
                if (result is ApiResult.Success) {
                    val currentSessionUserId = tokenStore.getUserId()
                    // Only cache if:
                    //   1. A session still exists (not logged out), AND
                    //   2. The session still belongs to the same user who
                    //      triggered this refresh (not a different login), AND
                    //   3. The response itself matches that user.
                    if (currentSessionUserId != null
                        && currentSessionUserId == sessionUserIdAtLaunch
                        && currentSessionUserId == result.data.id) {
                        userDao.insert(result.data.toDomain().toEntity())
                    }
                }
            }
            return ApiResult.Success(cached.toDomain())
        }
        // No cache — wait for network
        val result = safeApiCall { userService.getMyProfile() }
        if (result is ApiResult.Success) {
            userDao.insert(result.data.toDomain().toEntity())
        }
        return result.mapSuccess { it.toDomain() }
    }

    override suspend fun getUserProfile(userId: String): ApiResult<User> =
        safeApiCall { userService.getUserProfile(userId) }
            .mapSuccess { it.toDomain() }

    override suspend fun updateProfile(
        fullName: String?,
        phoneNumber: String?,
        preferredCurrency: String?,
        language: String?,
        notificationEnabled: Boolean?,
        timezone: String?,
    ): ApiResult<User> {
        val result = safeApiCall {
            userService.updateProfile(
                UpdateProfileRequest(
                    fullName            = fullName,
                    phoneNumber         = phoneNumber,
                    preferredCurrency   = preferredCurrency,
                    language            = language,
                    notificationEnabled = notificationEnabled,
                    timezone            = timezone,
                )
            )
        }
        if (result is ApiResult.Success) {
            userDao.insert(result.data.toDomain().toEntity())
            // Keep tokenStore in sync so AddExpenseViewModel picks up the new default
            if (preferredCurrency != null) {
                tokenStore.savePreferredCurrency(preferredCurrency)
            }
        }
        return result.mapSuccess { it.toDomain() }
    }

    override suspend fun deactivateAccount(password: String?): ApiResult<Unit> =
        safeApiCall { userService.deactivateAccount(DeactivateAccountRequest(password = password)) }
            .mapSuccess { }

    override suspend fun reactivateAccount(): ApiResult<Unit> =
        safeApiCall { userService.reactivateAccount() }.mapSuccess { }

    override suspend fun deleteAccount(password: String?): ApiResult<Unit> {
        val result = safeApiCall {
            userService.deleteAccount(DeleteAccountRequest(password = password))
        }.mapSuccess { }
        if (result is ApiResult.Success) {
            // Clear the user profile cache. The full session cache (groups, expenses,
            // friends, balances, settlements, etc.) is cleared by logoutUseCase()
            // which AccountViewModel calls immediately after this returns Success.
            // This avoids needing to inject all DAOs here — UserRepositoryImpl only
            // has userDao in its constructor; AuthRepositoryImpl owns the rest.
            userDao.deleteAll()
        }
        return result
    }

    override suspend fun getFriendCode(): ApiResult<String> {
        // Call the dedicated endpoint instead of reading from the profile cache.
        // GET /api/users/me/friend-code generates a code server-side if null/blank,
        // so existing users with no friend_code self-heal on first QR screen open.
        val result = safeApiCall { userService.getMyFriendCode() }
            .mapSuccess { it["friendCode"] ?: "" }
        // If the code came back blank despite a 200, treat as failure so the UI
        // shows a retry instead of rendering an empty/invalid QR code.
        return when {
            result is ApiResult.Success && result.data.isBlank() ->
                ApiResult.HttpError(500, "Could not generate your friend code. Please try again.")
            else -> result
        }
    }

    override suspend fun regenerateFriendCode(): ApiResult<String> =
        safeApiCall { userService.regenerateFriendCode() }
            .mapSuccess { it["friendCode"] ?: "" }

    override suspend fun searchByEmail(email: String): ApiResult<User> =
        safeApiCall { userService.searchByEmail(email) }
            .mapSuccess { it.toDomain() }

    override suspend fun requestEmailChange(newEmail: String, currentPassword: String): ApiResult<String> =
        safeApiCall {
            userService.requestEmailChange(mapOf(
                "newEmail" to newEmail,
                "currentPassword" to currentPassword
            ))
        }.mapSuccess { "" } // Response body ignored — token is emailed, not returned

    override suspend fun verifyEmailChange(token: String): ApiResult<Unit> =
        safeApiCall { userService.verifyEmailChange(token) }.mapSuccess { }

    private fun User.toEntity() = UserEntity(
        id                  = id,
        email               = email,
        fullName            = fullName,
        phoneNumber         = phoneNumber,
        profilePictureUrl   = profilePictureUrl,
        preferredCurrency   = preferredCurrency,
        language            = language,
        notificationEnabled = notificationEnabled,
        isActive            = isActive,
        friendCode          = friendCode,
        timezone            = timezone,
    )

    private fun UserEntity.toDomain() = User(
        id                  = id,
        email               = email,
        fullName            = fullName,
        phoneNumber         = phoneNumber,
        profilePictureUrl   = profilePictureUrl,
        preferredCurrency   = preferredCurrency,
        language            = language,
        notificationEnabled = notificationEnabled,
        isActive            = isActive,
        friendCode          = friendCode,
    )
}