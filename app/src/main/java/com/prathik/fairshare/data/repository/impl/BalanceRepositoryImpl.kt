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
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BalanceRepositoryImpl @Inject constructor(
    private val balanceService: BalanceApiService,
    private val balanceDao: BalanceDao,
    private val tokenStore: EncryptedTokenStore,
) : BalanceRepository {

    override suspend fun getAllBalances(): ApiResult<List<Balance>> {
        val result = safeApiCall { balanceService.getAllBalances() }
        if (result is ApiResult.Success) {
            val userId = tokenStore.getUserId() ?: return result.mapSuccess { list -> list.map { it.toDomain() } }
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

    override suspend fun getBalanceWithUser(otherUserId: String): ApiResult<Map<String, Any>> =
        safeApiCall { balanceService.getBalanceWithUser(otherUserId) }
            .mapSuccess { list ->
                mapOf("balances" to list.map { it.toDomain() })
            }

    override suspend fun getBalanceSummary(): ApiResult<Map<String, Any>> =
        safeApiCall { balanceService.getBalanceSummary() }
            .mapSuccess { it }
}
