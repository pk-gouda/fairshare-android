package com.prathik.fairshare.data.repository.impl

import com.prathik.fairshare.data.local.BalanceDao
import com.prathik.fairshare.data.local.BalanceEntity
import com.prathik.fairshare.data.model.mapper.toDomain
import com.prathik.fairshare.data.local.EncryptedTokenStore
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

    override suspend fun getBreakdownWithUser(otherUserId: String): ApiResult<List<Balance>> =
        safeApiCall { balanceService.getBreakdownWithUser(otherUserId) }
            .mapSuccess { list -> list.map { it.toDomain() } }

    /**
     * Always hits the network — returns UserBalance records (net balance, not per-group).
     * Used by SettleUpViewModel for non-group settlements to guarantee a fresh balance
     * and avoid serving the Room-cached result from getAllBalances().
     */
    override suspend fun getNetBalanceWithUser(otherUserId: String): ApiResult<List<Balance>> =
        safeApiCall { balanceService.getBalanceWithUser(otherUserId) }
            .mapSuccess { list -> list.map { it.toDomain() } }

    override suspend fun getBalanceSummary(): ApiResult<Map<String, Any>> =
        safeApiCall { balanceService.getBalanceSummary() }
            .mapSuccess { it }

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