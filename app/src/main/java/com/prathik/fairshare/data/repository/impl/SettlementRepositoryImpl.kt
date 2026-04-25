package com.prathik.fairshare.data.repository.impl

import com.prathik.fairshare.data.model.mapper.toDomain
import com.prathik.fairshare.data.model.request.SettleRequest
import com.prathik.fairshare.data.model.request.UpdateSettlementRequest
import com.prathik.fairshare.data.network.api.SettlementApiService
import com.prathik.fairshare.data.network.mapSuccess
import com.prathik.fairshare.data.network.safeApiCall
import com.prathik.fairshare.domain.model.ApiResult
import com.prathik.fairshare.domain.model.Settlement
import com.prathik.fairshare.domain.model.SettleType
import com.prathik.fairshare.domain.repository.SettlementRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SettlementRepositoryImpl @Inject constructor(
    private val settlementService: SettlementApiService,
) : SettlementRepository {

    override suspend fun settle(
        otherUserId: String,
        type: SettleType,
        groupId: String?,
        amount: Double?,
        currency: String?,
        paymentMethod: String?,
        notes: String?,
        payerId: String?,
    ): ApiResult<List<Settlement>> =
        safeApiCall {
            settlementService.settle(
                SettleRequest(
                    otherUserId   = otherUserId,
                    type          = type,
                    groupId       = groupId,
                    amount        = amount,
                    currency      = currency,
                    paymentMethod = paymentMethod,
                    notes         = notes,
                    payerId       = payerId,
                )
            )
        }.mapSuccess { list -> list.map { it.toDomain() } }

    override suspend fun getHistory(otherUserId: String): ApiResult<List<Settlement>> =
        safeApiCall { settlementService.getHistory(otherUserId) }
            .mapSuccess { list -> list.map { it.toDomain() } }

    override suspend fun getPending(): ApiResult<List<Settlement>> =
        safeApiCall { settlementService.getPending() }
            .mapSuccess { list -> list.map { it.toDomain() } }

    override suspend fun getInitiated(): ApiResult<List<Settlement>> =
        safeApiCall { settlementService.getInitiated() }
            .mapSuccess { list -> list.map { it.toDomain() } }

    override suspend fun cancelSettlement(settlementId: String): ApiResult<Settlement> =
        safeApiCall { settlementService.cancelSettlement(settlementId) }
            .mapSuccess { it.toDomain() }

    override suspend fun restoreSettlement(settlementId: String): ApiResult<Settlement> =
        safeApiCall { settlementService.restoreSettlement(settlementId) }
            .mapSuccess { it.toDomain() }

    override suspend fun deleteSettlement(settlementId: String): ApiResult<Unit> {
        val result = safeApiCall { settlementService.deleteSettlement(settlementId) }
        return when (result) {
            is ApiResult.Success -> ApiResult.Success(Unit)
            is ApiResult.NetworkError    -> result
            is ApiResult.HttpError       -> result
            is ApiResult.ValidationError -> result
            is ApiResult.Unauthorized    -> result
            is ApiResult.NotFound        -> result
            is ApiResult.Forbidden       -> result
            is ApiResult.Conflict        -> result
        }
    }

    override suspend fun getSettlementById(settlementId: String): ApiResult<Settlement> =
        safeApiCall { settlementService.getSettlementById(settlementId) }
            .mapSuccess { it.toDomain() }

    override suspend fun updateSettlement(
        settlementId: String,
        amount: Double?,
        notes: String?,
        paymentMethod: String?,
    ): ApiResult<Settlement> =
        safeApiCall {
            settlementService.updateSettlement(
                settlementId,
                UpdateSettlementRequest(
                    amount        = amount,
                    notes         = notes,
                    paymentMethod = paymentMethod,
                )
            )
        }.mapSuccess { it.toDomain() }
}