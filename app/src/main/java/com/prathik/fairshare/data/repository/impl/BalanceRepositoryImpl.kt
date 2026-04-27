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
        // Fall back to ALL_BALANCES scope only — never mix with breakdown rows.
        if (result is ApiResult.NetworkError && userId != null) {
            val cached = balanceDao.getAllBalanceRows(userId)
            android.util.Log.d("BalanceCache", "getAllBalances offline fallback: ${cached.size} rows")
            if (cached.isNotEmpty()) return ApiResult.Success(cached.map { it.toDomain() })
        }
        return result
    }

    private suspend fun refreshBalances(userId: String?): ApiResult<List<Balance>> {
        val result = safeApiCall { balanceService.getAllBalances() }
        if (result is ApiResult.Success && userId != null) {
            // UPSERT ONLY — no deleteByUserId.
            // A broad delete wipes group-detail and friend-breakdown rows that
            // getGroupBalances()/getBreakdownWithUser() cached independently.
            // OnConflictStrategy.REPLACE handles updates on the composite PK.
            val rows = result.data.map { response ->
                BalanceEntity(
                    userId        = response.userId,
                    otherUserId   = response.otherUserId,
                    otherUserName = response.otherUserName,
                    amount        = response.amount,
                    currency      = response.currency,
                    groupId       = "",      // forced: ALL_BALANCES = total, never a group row
                    groupName     = null,    // forced: no group association for total rows
                    cacheScope    = BalanceEntity.CacheScope.ALL_BALANCES,
                )
            }
            balanceDao.insertAll(rows)
            android.util.Log.d("BalanceCache", "getAllBalances upserted ${rows.size} rows")
        }
        return result.mapSuccess { list -> list.map { it.toDomain() } }
    }

    override suspend fun getBreakdownWithUser(otherUserId: String): ApiResult<List<Balance>> {
        val result = safeApiCall { balanceService.getBreakdownWithUser(otherUserId) }
        val userId = tokenStore.getUserId()
        if (result is ApiResult.Success) {
            // Cache group-breakdown rows for this friend with targeted delete (not broad wipe).
            if (userId != null) {
                // Targeted delete then upsert for this friend's group rows only.
                // Pin otherUserId to the argument — r.otherUserId may be omitted or vary.
                balanceDao.deleteBreakdownForFriend(userId, otherUserId)
                val rows = result.data.map { r ->
                    BalanceEntity(
                        userId        = userId,
                        otherUserId   = otherUserId,
                        otherUserName = r.otherUserName,
                        amount        = r.amount,
                        currency      = r.currency,
                        groupId       = r.groupId ?: "",
                        groupName     = r.groupName,
                        cacheScope    = BalanceEntity.CacheScope.FRIEND_BREAKDOWN,
                    )
                }
                balanceDao.insertAll(rows)
                android.util.Log.d("BalanceCache", "getBreakdownWithUser cached ${rows.size} rows for $otherUserId")
            }
            return result.mapSuccess { list -> list.map { it.toDomain() } }
        }
        // Network failed — return cached group-breakdown rows for this friend.
        if (userId != null) {
            val cached = balanceDao.getFriendBreakdownRows(userId, otherUserId)
            android.util.Log.d("BalanceCache", "getBreakdownWithUser offline fallback: ${cached.size} rows")
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
                val rows = result.data.map { r ->
                    BalanceEntity(
                        userId        = userId,
                        otherUserId   = otherUserId,
                        otherUserName = r.otherUserName,
                        amount        = r.amount,
                        currency      = r.currency,
                        groupId       = "",      // forced: FRIEND_NET = total, never a group row
                        groupName     = null,    // forced: no group association for total rows
                        cacheScope    = BalanceEntity.CacheScope.FRIEND_NET,
                    )
                }
                balanceDao.insertAll(rows)
                android.util.Log.d("BalanceCache", "getNetBalanceWithUser cached ${rows.size} rows for $otherUserId")
            }
            return result.mapSuccess { list -> list.map { it.toDomain() } }
        }
        // Network failed — return cached net-balance rows for this friend.
        if (userId != null) {
            // Try FRIEND_NET scope first, fall back to ALL_BALANCES scope.
            val cached = balanceDao.getFriendNetRows(userId, otherUserId)
                .ifEmpty { balanceDao.getAllBalanceRows(userId).filter { it.otherUserId == otherUserId } }
            android.util.Log.d("BalanceCache", "getNetBalanceWithUser offline fallback: ${cached.size} rows")
            if (cached.isNotEmpty()) return ApiResult.Success(cached.map { it.toDomain() })
        }
        return result.mapSuccess { list -> list.map { it.toDomain() } }
    }

    override suspend fun getBalanceSummary(): ApiResult<BalanceSummaryResponse> =
        safeApiCall { balanceService.getBalanceSummary() }

    override suspend fun getCachedGroupBalance(groupId: String): Double? {
        val userId = tokenStore.getUserId() ?: return null
        val rows = balanceDao.getGroupBalanceRows(userId, groupId)
        return if (rows.isEmpty()) null else rows.sumOf { it.amount }
    }

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