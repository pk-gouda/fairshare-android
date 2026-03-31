package com.prathik.fairshare.data.repository.impl

import com.prathik.fairshare.data.local.UserDao
import com.prathik.fairshare.data.local.UserEntity
import com.prathik.fairshare.data.model.mapper.toDomain
import com.prathik.fairshare.data.model.request.UpdateProfileRequest
import com.prathik.fairshare.data.network.api.UserApiService
import com.prathik.fairshare.data.network.mapSuccess
import com.prathik.fairshare.data.network.safeApiCall
import com.prathik.fairshare.domain.model.ApiResult
import com.prathik.fairshare.domain.model.User
import com.prathik.fairshare.domain.repository.UserRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserRepositoryImpl @Inject constructor(
    private val userService: UserApiService,
    private val userDao: UserDao,
) : UserRepository {

    override suspend fun getMyProfile(): ApiResult<User> {
        val result = safeApiCall { userService.getMyProfile() }
        if (result is ApiResult.Success) {
            val user = result.data.toDomain()
            userDao.insert(user.toEntity())
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
    ): ApiResult<User> {
        val result = safeApiCall {
            userService.updateProfile(
                UpdateProfileRequest(
                    fullName = fullName,
                    phoneNumber = phoneNumber,
                    preferredCurrency = preferredCurrency,
                    language = language,
                    notificationEnabled = notificationEnabled,
                )
            )
        }
        if (result is ApiResult.Success) {
            userDao.insert(result.data.toDomain().toEntity())
        }
        return result.mapSuccess { it.toDomain() }
    }

    override suspend fun deactivateAccount(): ApiResult<Unit> =
        safeApiCall { userService.deactivateAccount() }.mapSuccess { }

    override suspend fun reactivateAccount(): ApiResult<Unit> =
        safeApiCall { userService.reactivateAccount() }.mapSuccess { }

    override suspend fun deleteAccount(): ApiResult<Unit> {
        val result = safeApiCall { userService.deleteAccount() }.mapSuccess { }
        if (result is ApiResult.Success) {
            userDao.deleteAll()
        }
        return result
    }

    override suspend fun getFriendCode(): ApiResult<String> =
        safeApiCall { userService.getFriendCode() }
            .mapSuccess { it["friendCode"] ?: "" }

    override suspend fun regenerateFriendCode(): ApiResult<String> =
        safeApiCall { userService.regenerateFriendCode() }
            .mapSuccess { it["friendCode"] ?: "" }

    override suspend fun searchByEmail(email: String): ApiResult<User> =
        safeApiCall { userService.searchByEmail(email) }
            .mapSuccess { it.toDomain() }

    private fun User.toEntity() = UserEntity(
        id = id,
        email = email,
        fullName = fullName,
        phoneNumber = phoneNumber,
        profilePictureUrl = profilePictureUrl,
        preferredCurrency = preferredCurrency,
        language = language,
        notificationEnabled = notificationEnabled,
        isActive = isActive,
    )
}