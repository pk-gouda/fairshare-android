package com.prathik.fairshare.data.repository.impl

import com.prathik.fairshare.data.local.BalanceDao
import com.prathik.fairshare.data.local.BalanceEntity
import com.prathik.fairshare.data.model.mapper.toDomain
import com.prathik.fairshare.data.local.EncryptedTokenStore
import com.prathik.fairshare.data.model.response.BalanceSummaryResponse
import com.prathik.fairshare.data.network.api.BalanceApiService
import com.prathik.fairshare.data.network.mapSuccess
import com.prathik.fairshare.data.network.safeApiCall
import com.prathik.fairshare.domain.model.ApiResult
import com.prathik.fairshare.domain.model.Balance
import com.prathik.fairshare.domain.repository.BalanceRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.CoroutineScope
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BalanceRepositoryImpl @Inject constructor(
    private val balanceService: BalanceApiService,
    private val balanceDao: BalanceDao,
    private val tokenStore: EncryptedTokenStore,
) : BalanceRepository {

    override suspend fun getAllBalances(): ApiResult<List<Balance>> {
        val userId = tokenStore.getUserId()

        // Always fetch from network — balance accuracy is critical.
        val result = refreshBalances(userId)
        // Fall back to cache if network fails
        if (result is ApiResult.NetworkError && userId != null) {
            val cached = balanceDao.getByUserId(userId)
            if (cached.isNotEmpty()) return ApiResult.Success(cached.map { it.toDomain() })
        }
        return result
    }

    private suspend fun refreshBalances(userId: String?): ApiResult<List<Balance>> {
        val result = safeApiCall { balanceService.getAllBalances() }
        if (result is ApiResult.Success && userId != null) {
            balanceDao.deleteByUserId(userId)
            balanceDao.insertAll(result.data.map { response ->
                BalanceEntity(
                    userId        = response.userId,
                    otherUserId   = response.otherUserId,
                    otherUserName = response.otherUserName,
                    amount        = response.amount,
                    currency      = response.currency,
                    groupId       = response.groupId ?: "",
                    groupName     = response.groupName,
                )
            })
        }
        return result.mapSuccess { list -> list.map { it.toDomain() } }
    }

    override suspend fun getBreakdownWithUser(otherUserId: String): ApiResult<List<Balance>> {
        val result = safeApiCall { balanceService.getBreakdownWithUser(otherUserId) }
        val userId = tokenStore.getUserId()
        if (result is ApiResult.Success) {
            // Cache group-breakdown rows for this friend with targeted delete (not broad wipe).
            if (userId != null) {
                balanceDao.deleteBreakdownForFriend(userId, otherUserId)
                balanceDao.insertAll(result.data.map { r ->
                    BalanceEntity(
                        userId        = userId,
                        otherUserId   = r.otherUserId,
                        otherUserName = r.otherUserName,
                        amount        = r.amount,
                        currency      = r.currency,
                        groupId       = r.groupId ?: "",
                        groupName     = r.groupName,
                    )
                })
            }
            return result.mapSuccess { list -> list.map { it.toDomain() } }
        }
        // Network failed — return cached group-breakdown rows for this friend.
        if (userId != null) {
            val cached = balanceDao.getByUserId(userId)
                .filter { it.otherUserId == otherUserId && it.groupId.isNotEmpty() }
            if (cached.isNotEmpty()) return ApiResult.Success(cached.map { it.toDomain() })
        }
        return result.mapSuccess { list -> list.map { it.toDomain() } }
    }

    /**
     * Caches the returned net balance rows and falls back to cache on network failure.
     * This ensures FriendDetail balance is readable offline after the first online visit.
     */
    override suspend fun getNetBalanceWithUser(otherUserId: String): ApiResult<List<Balance>> {
        val result = safeApiCall { balanceService.getBalanceWithUser(otherUserId) }
        val userId = tokenStore.getUserId()
        if (result is ApiResult.Success) {
            // Cache the direct friend net-balance rows (groupId = '') with targeted delete.
            if (userId != null) {
                balanceDao.deleteNetBalanceForFriend(userId, otherUserId)
                balanceDao.insertAll(result.data.map { r ->
                    BalanceEntity(
                        userId        = userId,
                        otherUserId   = r.otherUserId,
                        otherUserName = r.otherUserName,
                        amount        = r.amount,
                        currency      = r.currency,
                        groupId       = r.groupId ?: "",
                        groupName     = r.groupName,
                    )
                })
            }
            return result.mapSuccess { list -> list.map { it.toDomain() } }
        }
        // Network failed — return cached net-balance rows for this friend.
        if (userId != null) {
            val cached = balanceDao.getByOtherUserId(userId, otherUserId)
            if (cached.isNotEmpty()) return ApiResult.Success(cached.map { it.toDomain() })
        }
        return result.mapSuccess { list -> list.map { it.toDomain() } }
    }

    override suspend fun getBalanceSummary(): ApiResult<BalanceSummaryResponse> =
        safeApiCall { balanceService.getBalanceSummary() }

    private fun BalanceEntity.toDomain() = Balance(
        userId            = userId,
        otherUserId       = otherUserId,
        otherUserName     = otherUserName,
        amount            = amount,
        currency          = currency,
        groupId           = groupId.ifEmpty { null },
        groupName         = groupName,
        groupLastActivity = groupLastActivity,
    )
}