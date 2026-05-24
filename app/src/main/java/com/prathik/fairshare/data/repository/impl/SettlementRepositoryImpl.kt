package com.prathik.fairshare.data.repository.impl

import com.prathik.fairshare.data.model.mapper.toDomain
import com.prathik.fairshare.data.model.request.SettleRequest
import com.prathik.fairshare.data.model.request.UpdateSettlementRequest
import com.prathik.fairshare.data.model.response.SettlementPreviewResponse
import com.prathik.fairshare.data.local.SettlementDao
import com.prathik.fairshare.data.local.EncryptedTokenStore
import com.prathik.fairshare.data.local.toEntity
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
    private val settlementDao    : SettlementDao,
    private val tokenStore       : EncryptedTokenStore,
) : SettlementRepository {

    override suspend fun settle(
        otherUserId   : String,
        type          : SettleType,
        groupId       : String?,
        amount        : Double?,
        currency      : String?,
        paymentMethod : String?,
        notes         : String?,
        payerId       : String?,
        idempotencyKey: String,
    ): ApiResult<List<Settlement>> {
        val result = safeApiCall {
            settlementService.settle(
                idempotencyKey = idempotencyKey,
                request = SettleRequest(
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
        if (result is ApiResult.Success) {
            settlementDao.insertAll(result.data.map { it.toEntity() })
        }
        return result
    }

    override suspend fun previewSettlement(
        otherUserId: String,
        type       : String,
        groupId    : String?,
        amount     : Double?,
        currency   : String?,
    ): ApiResult<SettlementPreviewResponse> =
        safeApiCall {
            settlementService.previewSettlement(
                otherUserId = otherUserId,
                type        = type,
                groupId     = groupId,
                amount      = amount,
                currency    = currency,
            )
        }.mapSuccess { it }

    override suspend fun getCachedDirectSettlements(otherUserId: String): List<com.prathik.fairshare.domain.model.Settlement> {
        val currentUserId = tokenStore.getUserId() ?: return emptyList()
        return settlementDao.getDirectBetween(currentUserId, otherUserId).map { it.toDomain() }
    }

    override suspend fun getHistory(otherUserId: String): ApiResult<List<Settlement>> {
        val currentUserId = tokenStore.getUserId()
        val networkResult = safeApiCall { settlementService.getHistory(otherUserId) }
        val result = networkResult.mapSuccess { list -> list.map { it.toDomain() } }
        if (result is ApiResult.Success && currentUserId != null) {
            // Scoped replace: only wipe settlements between this exact pair.
            // Preserves other friends' cached settlement history.
            settlementDao.deleteDirectBetween(currentUserId, otherUserId)
            settlementDao.insertAll(
                result.data.filter { it.groupId == null }.map { it.toEntity() }
            )
        }
        if (result !is ApiResult.Success && currentUserId != null) {
            val cached = settlementDao.getDirectBetween(currentUserId, otherUserId)
            if (cached.isNotEmpty()) {
                android.util.Log.d("SettlementCache", "getHistory offline: ${cached.size} rows")
                return ApiResult.Success(cached.map { it.toDomain() })
            }
        }
        return result
    }

    override suspend fun getPending(): ApiResult<List<Settlement>> =
        safeApiCall { settlementService.getPending() }
            .mapSuccess { list -> list.map { it.toDomain() } }

    override suspend fun getInitiated(): ApiResult<List<Settlement>> =
        safeApiCall { settlementService.getInitiated() }
            .mapSuccess { list -> list.map { it.toDomain() } }

    override suspend fun cancelSettlement(settlementId: String, idempotencyKey: String): ApiResult<Settlement> {
        val result = safeApiCall { settlementService.cancelSettlement(idempotencyKey, settlementId) }
            .mapSuccess { it.toDomain() }
        if (result is ApiResult.Success) settlementDao.insertAll(listOf(result.data.toEntity()))
        return result
    }

    override suspend fun restoreSettlement(settlementId: String, idempotencyKey: String): ApiResult<Settlement> {
        val result = safeApiCall { settlementService.restoreSettlement(idempotencyKey, settlementId) }
            .mapSuccess { it.toDomain() }
        if (result is ApiResult.Success) settlementDao.insertAll(listOf(result.data.toEntity()))
        return result
    }

    override suspend fun deleteSettlement(settlementId: String): ApiResult<Unit> {
        val result = safeApiCall { settlementService.deleteSettlement(settlementId) }
        if (result is ApiResult.Success) settlementDao.deleteById(settlementId)
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

    override suspend fun getSettlementById(settlementId: String): ApiResult<Settlement> {
        val result = safeApiCall { settlementService.getSettlementById(settlementId) }
            .mapSuccess { it.toDomain() }
        if (result !is ApiResult.Success) {
            val cached = settlementDao.getById(settlementId)
            if (cached != null) {
                android.util.Log.d("SettlementCache", "getSettlementById offline: $settlementId")
                return ApiResult.Success(cached.toDomain())
            }
        }
        return result
    }

    override suspend fun updateSettlement(
        settlementId: String,
        amount: Double?,
        notes: String?,
        paymentMethod: String?,
    ): ApiResult<Settlement> {
        val result = safeApiCall {
            settlementService.updateSettlement(
                settlementId,
                UpdateSettlementRequest(
                    amount        = amount,
                    notes         = notes,
                    paymentMethod = paymentMethod,
                )
            )
        }.mapSuccess { it.toDomain() }
        if (result is ApiResult.Success) settlementDao.insertAll(listOf(result.data.toEntity()))
        return result
    }
}