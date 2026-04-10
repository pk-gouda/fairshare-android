package com.prathik.fairshare.data.repository.impl

import com.prathik.fairshare.data.local.EncryptedTokenStore
import com.prathik.fairshare.data.local.BalanceDao
import com.prathik.fairshare.data.local.ExpenseDao
import com.prathik.fairshare.data.local.GroupDao
import com.prathik.fairshare.data.local.InvitedFriendDao
import com.prathik.fairshare.data.local.PendingActionDao
import com.prathik.fairshare.data.local.UserDao
import com.prathik.fairshare.data.local.UserEntity
import com.prathik.fairshare.data.model.mapper.toDomain
import com.prathik.fairshare.data.model.request.ChangePasswordRequest
import com.prathik.fairshare.data.model.request.ForgotPasswordRequest
import com.prathik.fairshare.data.model.request.LoginRequest
import com.prathik.fairshare.data.model.request.RegisterRequest
import com.prathik.fairshare.data.model.request.ResetPasswordRequest
import com.prathik.fairshare.data.network.api.AuthApiService
import com.prathik.fairshare.data.network.mapSuccess
import com.prathik.fairshare.data.network.safeApiCall
import com.prathik.fairshare.domain.model.ApiResult
import com.prathik.fairshare.domain.model.User
import com.prathik.fairshare.domain.repository.AuthRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepositoryImpl @Inject constructor(
    private val authService: AuthApiService,
    private val tokenStore: EncryptedTokenStore,
    private val userDao: UserDao,
    private val groupDao: GroupDao,
    private val expenseDao: ExpenseDao,
    private val balanceDao: BalanceDao,
    private val invitedFriendDao: InvitedFriendDao,
    private val pendingActionDao: PendingActionDao,
) : AuthRepository {

    override suspend fun login(email: String, password: String): ApiResult<User> {
        val result = safeApiCall {
            authService.login(LoginRequest(email, password))
        }
        if (result is ApiResult.Success) {
            result.data.user?.let { userResponse ->
                tokenStore.saveTokens(
                    accessToken       = result.data.accessToken,
                    refreshToken      = result.data.refreshToken,
                    userId            = userResponse.id,
                    preferredCurrency = userResponse.preferredCurrency ?: "USD",
                    fullName          = userResponse.fullName ?: "",
                )
                cacheUser(userResponse.toDomain())
            }
        }
        return when (result) {
            is ApiResult.Success -> {
                val user = result.data.user
                if (user != null) ApiResult.Success(user.toDomain())
                else ApiResult.Conflict("Server returned success but no user data.")
            }

            else -> @Suppress("UNCHECKED_CAST") (result as ApiResult<User>)
        }
    }

    override suspend fun register(
        email: String,
        fullName: String,
        password: String,
        phoneNumber: String?,
        preferredCurrency: String?,
        language: String?,
    ): ApiResult<User> {
        // Backend register returns UserResponse only — no tokens.
        // User must verify email then login to get tokens.
        val result = safeApiCall {
            authService.register(
                RegisterRequest(
                    email = email,
                    fullName = fullName,
                    password = password,
                    phoneNumber = phoneNumber,
                    preferredCurrency = preferredCurrency,
                    language = language,
                )
            )
        }
        return result.mapSuccess { it.toDomain() }
    }

    override suspend fun verifyEmail(userId: String, token: String): ApiResult<Unit> =
        safeApiCall { authService.verifyEmail(userId, token) }.mapSuccess { }

    override suspend fun forgotPassword(email: String): ApiResult<Unit> =
        safeApiCall {
            authService.forgotPassword(ForgotPasswordRequest(email))
        }.mapSuccess { }

    override suspend fun resetPassword(token: String, newPassword: String): ApiResult<Unit> =
        safeApiCall {
            authService.resetPassword(ResetPasswordRequest(token, newPassword))
        }.mapSuccess { }

    override suspend fun changePassword(
        currentPassword: String,
        newPassword: String,
    ): ApiResult<Unit> =
        safeApiCall {
            authService.changePassword(ChangePasswordRequest(currentPassword, newPassword))
        }.mapSuccess { }

    override suspend fun refreshToken(): ApiResult<Unit> {
        val refreshToken = tokenStore.getRefreshToken() ?: return ApiResult.Unauthorized()
        val result = safeApiCall {
            authService.refreshToken(mapOf("refreshToken" to refreshToken))
        }
        if (result is ApiResult.Success) {
            tokenStore.updateAccessToken(result.data.accessToken)
        }
        return result.mapSuccess { }
    }

    override suspend fun logout(): ApiResult<Unit> {
        val refreshToken = tokenStore.getRefreshToken() ?: ""
        return try {
            safeApiCall { authService.logout(mapOf("refreshToken" to refreshToken)) }
        } finally {
            // Always clear ALL local caches on logout so a subsequent user
            // on the same device never sees the previous user's data.
            tokenStore.clearTokens()
            userDao.deleteAll()
            groupDao.deleteAll()
            expenseDao.deleteAll()
            balanceDao.deleteAll()
            invitedFriendDao.deleteAll()
            pendingActionDao.deleteAll()
        }
    }

    override fun isLoggedIn(): Boolean = tokenStore.isLoggedIn()

    private suspend fun cacheUser(user: User) {
        userDao.insert(
            UserEntity(
                id = user.id,
                email = user.email,
                fullName = user.fullName,
                phoneNumber = user.phoneNumber,
                profilePictureUrl = user.profilePictureUrl,
                preferredCurrency = user.preferredCurrency,
                language = user.language,
                notificationEnabled = user.notificationEnabled,
                isActive = user.isActive,
            )
        )
    }
}